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
	private final Map<String, String> names = new ConcurrentHashMap<>();
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
			if (names.containsKey(path)) {
				exchange.getResponseHeaders().add("Location", names.get(path));
				exchange.sendResponseHeaders(302, -1);
				exchange.close();
				return;
			}

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

	private void publishDigests(String revision, Map<Locale, Map<String, String>> intercept) {
		Map<Locale, String> hashes = new java.util.HashMap<>();
		intercept.forEach((locale, messages) -> {
			byte[] bytes = Bundle.write(messages);
			hashes.put(locale, Bundle.hash(bytes));
			files.put("sha256-" + Bundle.hash(bytes) + "/" + locale.toLanguageTag() + ".json", bytes);
		});

		byte[] manifest = new Manifest(revision, EN, Map.of("intercept", hashes)).write();
		files.put("sha256-" + Bundle.hash(manifest) + "/manifest.json", manifest);
		names.put("@manifest", "sha256-" + Bundle.hash(manifest) + "/manifest.json");
	}

	private HttpSource.Builder source() {
		return HttpSource.builder(base).product("intercept");
	}

	@Test
	void readsTheProductsBundlesForTheEnabledLocales() throws IOException {
		publish("1", Map.of(
				EN, Map.of("game.menu.title", "Menu", "gamemode.title", "Mode", "discord.ticket.title", "Ticket"),
				RU, Map.of("game.menu.title", "Меню"),
				DE, Map.of("game.menu.title", "Menü"),
				Locale.ROOT, Map.of("game.menu.prefix", "{@game.menu.title}:")
		));

		Source.Content content = source().prefixes("game").locales(Set.of(Locale.forLanguageTag("ru-RU"))).build().load();

		IssueAssert.assertNone(content.getIssues());
		assertEquals(Set.of(EN, RU, Locale.ROOT), content.getLocales());
		assertTrue(requests.contains("intercept/und.json"));
		assertEquals("Меню", content.entries(RU).get("game.menu.title").text());
		assertFalse(content.entries(EN).containsKey("discord.ticket.title"));
		assertFalse(content.entries(EN).containsKey("gamemode.title"));
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
	void readsBundlesByDigestWhenAskedTo() throws IOException {
		publishDigests("1", Map.of(EN, Map.of("a", "1"), RU, Map.of("a", "один")));
		String english = Bundle.hash(Bundle.write(Map.of("a", "1")));

		Source.Content content = source().layout(BundleLayout.digests("@manifest")).locales(Set.of(EN)).build().load();

		IssueAssert.assertNone(content.getIssues());
		assertEquals("1", content.entries(EN).get("a").text());
		assertEquals("@manifest", requests.getFirst());
		assertTrue(requests.contains("sha256-" + english + "/en.json"), requests::toString);
		assertTrue(requests.stream().noneMatch(path -> path.startsWith("intercept/") || path.endsWith("/ru.json")), requests::toString);
	}

	@Test
	void rejectsADigestBundleThatDoesNotMatchItsHash() throws IOException {
		publishDigests("1", Map.of(EN, Map.of("a", "1")));
		files.put("sha256-" + Bundle.hash(Bundle.write(Map.of("a", "1"))) + "/en.json", Bundle.write(Map.of("a", "tampered")));

		Source.Content content = source().layout(BundleLayout.digests("@manifest")).build().load();

		IssueAssert.assertReported(content.getIssues(), Issue.Kind.SOURCE, null, EN);
		assertTrue(content.entries(EN).isEmpty());
	}

	@Test
	void refusesAHashThatIsNoAddressOfADigest() throws IOException {
		byte[] manifest = new Manifest("1", EN, Map.of("intercept", Map.of(EN, "../intercept/en"))).write();
		files.put("manifest.json", manifest);
		requests.clear();

		Source.Content content = source().layout(BundleLayout.digests("manifest.json")).build().load();

		Issue issue = IssueAssert.assertReported(content.getIssues(), Issue.Kind.SOURCE, null, EN);
		assertTrue(issue.getDetail().contains("is no SHA-256"), issue::getDetail);
		assertEquals(List.of("manifest.json"), requests);
	}

	@Test
	void cachesDigestBundlesAsByPath() throws IOException {
		publishDigests("1", Map.of(EN, Map.of("a", "cached")));
		source().layout(BundleLayout.digests("@manifest")).cache(cache).build().load();
		server.stop(0);

		assertTrue(cache.resolve("manifest.json").toFile().isFile());
		assertTrue(cache.resolve("intercept/en.json").toFile().isFile());

		Source.Content content = source().layout(BundleLayout.digests("@manifest")).cache(cache).timeout(Duration.ofSeconds(2)).build().load();
		assertEquals("cached", content.entries(EN).get("a").text());
	}

	@Test
	void takesOnlyARelativeAddressForTheManifest() {
		assertThrows(IllegalArgumentException.class, () -> BundleLayout.digests("https://files.example/@manifest"));
		assertThrows(IllegalArgumentException.class, () -> BundleLayout.digests("/translations/@manifest"));
		assertThrows(IllegalArgumentException.class, () -> BundleLayout.digests(" "));
		assertEquals("manifest.json", BundleLayout.paths().manifest());
	}

	@Test
	void manifestsRoundTrip() throws IOException {
		Manifest manifest = new Manifest("abc", EN, Map.of("yui", Map.of(EN, "00", RU, "11")));

		assertEquals(manifest, Manifest.read(manifest.write()));
		assertThrows(IOException.class, () -> Manifest.read("{\"format\": 2}".getBytes()));
	}
}
