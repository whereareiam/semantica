package me.whereareiam.semantica.source.file;

import me.whereareiam.configura.Configura;
import me.whereareiam.semantica.Message;
import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.common.Semantica;
import me.whereareiam.semantica.source.Source;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;
import me.whereareiam.semantica.testing.IssueAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathSourceTest {
	private static final Locale EN = Locale.ENGLISH;
	private static final Configura CONFIGURA = Configura.builder().build();

	@TempDir
	Path folder;

	@Test
	void generatedMethodsReturnTheirMessages() {
		ShopMessages messages = new GeneratedShopMessages();

		assertEquals(Message.of("shop.bought").with("item", "Sword").with("price", 3), messages.bought("Sword", 3));
		assertEquals(Message.of("shop.cart.empty").with("count", 2L), messages.cart().empty(2));
		assertEquals(Message.of("shop.denied.cooldown"), messages.denied(() -> "cooldown"));
	}

	@Test
	void readsTheDefaultsTheProcessorWrote() throws IOException {
		Source.Content content = ClasspathSource.of(ShopMessages.class, CONFIGURA).load();

		IssueAssert.assertNone(content.getIssues());
		assertEquals("<gold>Shop\n<gray>Open until {closes, time, short}", content.entries(EN).get("shop.title").text());
		assertEquals("Empty", content.entries(EN).get("shop.cart.empty-label").text());
		assertEquals("META-INF/semantica/en/shop.json", content.entries(EN).get("shop.title").origin());
	}

	@Test
	void resolvesTheDefaultsAsTheLowestLayer() throws IOException {
		Files.createDirectories(folder.resolve("en"));
		Files.writeString(folder.resolve("en/shop.json"), "{\"cart\": {\"empty-label\": \"Nothing\"}}", StandardCharsets.UTF_8);
		ShopMessages messages = new GeneratedShopMessages();

		try (Semantica semantica = Semantica.builder()
				.syntax(new IcuSyntax())
				.watch(false)
				.layer(Layer.of(ClasspathSource.of(ShopMessages.class, CONFIGURA), Layer.DEFAULTS))
				.layer(Layer.of(FileSource.of(folder, CONFIGURA), Layer.OVERRIDES))
				.build()) {
			IssueAssert.assertNone(semantica.getCatalog().getIssues());
			assertEquals("Bought Sword for 1 coin", semantica.resolve(messages.bought("Sword", 1), EN).plain());
			assertEquals("Nothing: 2", semantica.resolve(messages.cart().empty(2), EN).plain());
			assertEquals("<gold>Shop", semantica.resolve(messages.title(LocalTime.NOON), EN).plain().lines().findFirst().orElseThrow());
		}
	}

	@Test
	void readsOnlyTheJarThatHoldsTheClass() throws IOException, ClassNotFoundException {
		Path jar = folder.resolve("addon.jar");
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
			copyClass(zip, ShopMessages.class);
			entry(zip, "META-INF/semantica/de/addon.json", "{\"title\": \"Laden\"}");
			entry(zip, "META-INF/semantica/all/addon.json", "{\"prefix\": \"{@addon.title}\"}");
			entry(zip, "META-INF/semantica/notes.txt", "not a locale file");
		}

		try (URLClassLoader loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
			Class<?> owner = loader.loadClass(ShopMessages.class.getName());
			Source.Content content = ClasspathSource.of(owner, CONFIGURA).load();

			IssueAssert.assertNone(content.getIssues());
			assertEquals("Laden", content.entries(Locale.GERMAN).get("addon.title").text());
			assertEquals("{@addon.title}", content.entries(Locale.ROOT).get("addon.prefix").text());
			assertNull(content.entries(EN).get("shop.title"));
		}
	}

	@Test
	void readsAClassServedByAFileSystemOfItsOwn() throws IOException {
		// The JDK serves its own classes from jrt:, as NeoForge serves a mod's from union:.
		Source.Content content = ClasspathSource.of(String.class, CONFIGURA).load();

		IssueAssert.assertNone(content.getIssues());
		assertTrue(content.entries(EN).isEmpty());
	}

	private static void copyClass(ZipOutputStream zip, Class<?> type) throws IOException {
		String name = type.getName().replace('.', '/') + ".class";
		try (InputStream input = type.getResourceAsStream("/" + name)) {
			zip.putNextEntry(new ZipEntry(name));
			input.transferTo(zip);
			zip.closeEntry();
		}
	}

	private static void entry(ZipOutputStream zip, String name, String text) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		OutputStream output = zip;
		output.write(text.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}
}
