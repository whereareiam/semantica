package me.whereareiam.semantica.source.http;

import com.sun.net.httpserver.HttpServer;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.source.Source;
import me.whereareiam.semantica.testing.IssueAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpSourceTest {
	private static final Locale EN = Locale.ENGLISH;
	private static final Locale RU = Locale.forLanguageTag("ru");
	private static final Locale DE = Locale.GERMAN;

	private final Map<String, byte[]> files = new ConcurrentHashMap<>();
	private final List<String> requests = new CopyOnWriteArrayList<>();
	private HttpServer server;
	private URI base;

	@TempDir
	Path cache;

	@BeforeEach
	void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/bundles/", exchange -> {
			String path = exchange.getRequestURI().getPath().substring("/bundles/".length());
			requests.add(path);
			byte[] body = files.get(path);
			exchange.sendResponseHeaders(body == null ? 404 : 200, body == null ? -1 : body.length);
			if (body != null) try (OutputStream output = exchange.getResponseBody()) {
				output.write(body);
			}
			exchange.close();
		});
		server.start();
		base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/bundles");
	}

	@AfterEach
	void stop() {
		server.stop(0);
	}

	private void publish(String revision, Map<Locale, Map<String, String>> intercept) {
		Map<Locale, String> hashes = new java.util.HashMap<>();
		intercept.forEach((locale, messages) -> {
			byte[] bytes = Bundle.write(messages);
			files.put(Manifest.path("intercept", locale), bytes);
			hashes.put(locale, Bundle.hash(bytes));
		});
		files.put("manifest.json", new Manifest(revision, EN, Map.of("intercept", hashes)).write());
	}

	private HttpSource.Builder source() {
		return HttpSource.builder(base).product("intercept");
	}

	@Test
	void readsTheProductsBundlesForTheEnabledLocales() throws IOException {
		publish("1", Map.of(
				EN, Map.of("game.menu.title", "Menu", "discord.ticket.title", "Ticket"),
				RU, Map.of("game.menu.title", "Меню"),
				DE, Map.of("game.menu.title", "Menü")
		));

		Source.Content content = source().namespaces("game").locales(Set.of(Locale.forLanguageTag("ru-RU"))).build().load();

		IssueAssert.assertNone(content.getIssues());
		assertEquals(Set.of(EN, RU), content.getLocales());
		assertEquals("Меню", content.entries(RU).get("game.menu.title").text());
		assertFalse(content.entries(EN).containsKey("discord.ticket.title"));
		assertFalse(requests.contains("intercept/de.json"));
	}

	@Test
	void downloadsOnlyChangedBundles() throws IOException {
		publish("1", Map.of(EN, Map.of("a", "1"), RU, Map.of("a", "один")));
		HttpSource source = source().build();
		source.load();

		publish("2", Map.of(EN, Map.of("a", "2"), RU, Map.of("a", "один")));
		requests.clear();
		Source.Content content = source.load();

		assertEquals(List.of("manifest.json", "intercept/en.json"), requests);
		assertEquals("2", content.entries(EN).get("a").text());
		assertEquals("один", content.entries(RU).get("a").text());
	}

	@Test
	void rejectsABundleThatDoesNotMatchItsHash() throws IOException {
		publish("1", Map.of(EN, Map.of("a", "1")));
		HttpSource source = source().build();
		source.load();

		publish("2", Map.of(EN, Map.of("a", "2")));
		files.put("intercept/en.json", Bundle.write(Map.of("a", "tampered")));
		Source.Content content = source.load();

		IssueAssert.assertReported(content.getIssues(), Issue.Kind.SOURCE, null, EN);
		assertEquals("1", content.entries(EN).get("a").text());
	}

	@Test
	void startsFromTheCacheWhenTheServerIsDown() throws IOException {
		publish("1", Map.of(EN, Map.of("a", "cached")));
		source().cache(cache).build().load();
		server.stop(0);

		Source.Content content = source().cache(cache).timeout(Duration.ofSeconds(2)).build().load();

		assertEquals("cached", content.entries(EN).get("a").text());
		assertThrows(IOException.class, () -> source().timeout(Duration.ofSeconds(2)).build().load());
	}

	@Test
	void tellsWhenTheRevisionChanges() throws Exception {
		publish("1", Map.of(EN, Map.of("a", "1")));
		HttpSource source = source().interval(Duration.ofMillis(100)).build();
		source.load();

		CountDownLatch changed = new CountDownLatch(1);
		try (AutoCloseable ignored = source.watch(changed::countDown)) {
			assertFalse(changed.await(400, TimeUnit.MILLISECONDS));

			publish("2", Map.of(EN, Map.of("a", "2")));
			assertTrue(changed.await(5, TimeUnit.SECONDS));
		}
	}

	@Test
	void manifestsRoundTrip() throws IOException {
		Manifest manifest = new Manifest("abc", EN, Map.of("yui", Map.of(EN, "00", RU, "11")));

		assertEquals(manifest, Manifest.read(manifest.write()));
		assertThrows(IOException.class, () -> Manifest.read("{\"format\": 2}".getBytes()));
	}
}
