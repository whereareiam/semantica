package me.whereareiam.semantica.cli;

import me.whereareiam.semantica.source.http.Bundle;
import me.whereareiam.semantica.source.http.Manifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticaCliTest {
	@TempDir
	Path folder;

	private final ByteArrayOutputStream out = new ByteArrayOutputStream();
	private final ByteArrayOutputStream err = new ByteArrayOutputStream();

	private int run(Map<String, String> environment, String... arguments) {
		return new SemanticaCli(new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8), environment)
				.run(arguments);
	}

	private String output() {
		return out.toString(StandardCharsets.UTF_8);
	}

	private void write(String path, String text) throws IOException {
		Path file = folder.resolve(path);
		Files.createDirectories(file.getParent());
		Files.writeString(file, text, StandardCharsets.UTF_8);
	}

	private void writeRepository() throws IOException {
		write("semantica.json", """
				{
				  "defaultLocale": "en",
				  "namespaces": {
					"game": { "format": "adventure" },
					"discord": {},
					"common": {}
				  },
				  "products": {
					"intercept": ["game", "common"],
					"yui": ["discord", "common"]
				  }
				}
				""");
		write("en/game/menu.json", "{\"title\": \"<gold>Menu</gold>\"}");
		write("en/common.json", "{\"unit\": \"{count, plural, one {# unit} other {# units}}\"}");
		write("en/discord/command.json", "{\"ban\": {\"name\": \"ban\", \"description\": \"Ban a member\"}}");
		write("ru/game/menu.json", "{\"title\": \"<gold>Меню</gold>\"}");
		write("ru/common.json", "{\"unit\": \"{count, plural, one {# единица} few {# единицы} many {# единиц} other {# единицы}}\"}");
	}

	@Test
	void passesCleanTranslations() throws IOException {
		writeRepository();

		assertEquals(0, run(Map.of(), "check", folder.toString()), output());
		assertTrue(output().contains("ru: 2/4 keys (50%)"), output());
	}

	@Test
	void failsOnErrorsAndAnnotatesThemOnGitHub() throws IOException {
		writeRepository();
		write("ru/game/menu.json", "{\"title\": \"<click:run_command:/op>Меню</click>\", \"stale\": \"x\"}");
		write("ru/common.json", "{\"unit\": \"{count, plural, one {# единица} other {# единицы}\"}");
		write("de/other/x.json", "{\"a\": \"b\"}");

		assertEquals(1, run(Map.of("GITHUB_ACTIONS", "true"), "check", folder.toString()));

		String output = output();
		assertTrue(output.contains("error: markup ru game.menu.title"), output);
		assertTrue(output.contains("error: unknown-key ru game.menu.stale"), output);
		assertTrue(output.contains("error: syntax ru common.unit"), output);
		assertTrue(output.contains("error: source de other.x.a"), output);
		assertTrue(output.contains("::error file=" + folder.resolve("ru/game/menu.json") + ",title=markup::ru game.menu.title"), output);
	}

	@Test
	void writesBundlesPerProductAndAManifest() throws IOException {
		writeRepository();
		Path output = folder.resolve("../bundles").normalize();

		assertEquals(0, run(Map.of(), "bundle", folder.toString(), output.toString(), "--revision", "abc"), output());

		Manifest manifest = Manifest.read(Files.readAllBytes(output.resolve("manifest.json")));
		assertEquals("abc", manifest.getRevision());
		assertEquals(Locale.ENGLISH, manifest.getDefaultLocale());

		byte[] intercept = Files.readAllBytes(output.resolve("intercept/ru.json"));
		assertEquals(Bundle.hash(intercept), manifest.bundles("intercept").get(Locale.forLanguageTag("ru")));
		assertEquals(Map.of("common.unit", "{count, plural, one {# единица} few {# единицы} many {# единиц} other {# единицы}}",
				"game.menu.title", "<gold>Меню</gold>"), Bundle.read(intercept));

		Map<String, String> yui = Bundle.read(Files.readAllBytes(output.resolve("yui/en.json")));
		assertTrue(yui.containsKey("discord.command.ban.name"));
		assertFalse(yui.containsKey("game.menu.title"));
		assertFalse(Files.exists(output.resolve("yui/ru.json.tmp")));
	}

	@Test
	void refusesToReplaceAFolderThatHoldsOtherFiles() throws IOException {
		writeRepository();

		assertEquals(2, run(Map.of(), "bundle", folder.toString(), folder.toString()));
		assertTrue(Files.exists(folder.resolve("semantica.json")));
	}

	@Test
	void explainsBrokenSettings() throws IOException {
		write("semantica.json", "{\"defaultLocale\": \"en\", \"namespaces\": {\"game\": {}}, \"products\": {\"x\": [\"nope\"]}}");

		assertEquals(2, run(Map.of(), "check", folder.toString()));
		assertTrue(err.toString(StandardCharsets.UTF_8).contains("unknown namespace 'nope'"));
	}
}
