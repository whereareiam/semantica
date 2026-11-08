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
import java.util.Set;

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
				  "markup": {
					"game": { "format": "adventure" },
					"realm": { "format": "adventure" },
					"realm.vanilla.mode.bedwars": { "format": "adventure", "allow": ["hover"] }
				  },
				  "products": {
					"create": ["game", "realm.create.common"],
					"bedwars": ["game", "realm.vanilla.common", "realm.vanilla.mode.bedwars"],
					"website": ["website"]
				  }
				}
				""");
		write("en/game/menu.json", "{\"title\": \"<gold>Menu</gold>\"}");
		write("en/realm/create/common/join.json", "{\"title\": \"Welcome to Create\"}");
		write("en/realm/vanilla/common/join.json", "{\"title\": \"Welcome to Vanilla\"}");
		write("en/realm/vanilla/mode/bedwars/bed.json", "{\"broken\": \"<hover:show_text:'{team}'>Your bed</hover> is gone, {team}\"}");
		write("en/website/home.json", "{\"title\": \"<gren> is just text here\", \"unit\": \"{count, plural, one {# unit} other {# units}}\"}");
		write("ru/game/menu.json", "{\"title\": \"<gold>Меню</gold>\"}");
		write("all/game/link.json", "{\"menu\": \"{@game.menu.title}!\"}");
		write("ru/website/home.json", "{\"unit\": \"{count, plural, one {# единица} few {# единицы} many {# единиц} other {# единицы}}\"}");
	}

	@Test
	void passesCleanTranslations() throws IOException {
		writeRepository();

		assertEquals(0, run(Map.of(), "check", folder.toString()), output());
		assertTrue(output().contains("ru: 2/6 keys (33%)") && !output().contains("und:"), output());
	}

	@Test
	void failsOnErrorsAndAnnotatesThemOnGitHub() throws IOException {
		writeRepository();
		write("ru/game/menu.json", "{\"title\": \"<click:run_command:/op>Меню</click>\", \"stale\": \"x\"}");
		write("ru/realm/vanilla/mode/bedwars/bed.json", "{\"broken\": \"<hover:show_text:'{team}'>Кровать</hover> {team\"}");
		write("de/other/x.json", "{\"a\": \"b\"}");

		assertEquals(1, run(Map.of("GITHUB_ACTIONS", "true"), "check", folder.toString()));

		String output = output();
		assertTrue(output.contains("error: markup ru game.menu.title"), output);
		assertTrue(output.contains("error: unknown-key ru game.menu.stale"), output);
		assertTrue(output.contains("error: syntax ru realm.vanilla.mode.bedwars.bed.broken"), output);
		assertTrue(output.contains("error: source de other.x.a"), output);
		assertTrue(output.contains("no bundle carries it"), output);
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

		Map<String, String> bedwars = Bundle.read(Files.readAllBytes(output.resolve("bedwars/en.json")));
		assertEquals(Bundle.hash(Files.readAllBytes(output.resolve("bedwars/en.json"))), manifest.bundles("bedwars").get(Locale.ENGLISH));
		assertEquals(Set.of("game.menu.title", "realm.vanilla.common.join.title", "realm.vanilla.mode.bedwars.bed.broken"), bedwars.keySet());

		Map<String, String> create = Bundle.read(Files.readAllBytes(output.resolve("create/en.json")));
		assertEquals(Set.of("game.menu.title", "realm.create.common.join.title"), create.keySet());

		assertEquals(Map.of("game.menu.title", "<gold>Меню</gold>"), Bundle.read(Files.readAllBytes(output.resolve("create/ru.json"))));
		assertTrue(Files.exists(output.resolve("website/ru.json")));
		assertEquals(Map.of("game.link.menu", "{@game.menu.title}!"), Bundle.read(Files.readAllBytes(output.resolve("bedwars/und.json"))));
		assertFalse(Files.exists(output.resolve("bedwars/de.json")));
	}

	@Test
	void refusesToReplaceAFolderThatHoldsOtherFiles() throws IOException {
		writeRepository();

		assertEquals(2, run(Map.of(), "bundle", folder.toString(), folder.toString()));
		assertTrue(Files.exists(folder.resolve("semantica.json")));
	}

	@Test
	void explainsBrokenSettings() throws IOException {
		write("semantica.json", "{\"defaultLocale\": \"en\", \"products\": {\"bedwars\": [\"realm..vanilla\"]}}");

		assertEquals(2, run(Map.of(), "check", folder.toString()));
		assertTrue(err.toString(StandardCharsets.UTF_8).contains("In product 'bedwars': 'realm..vanilla' is no key prefix"), err::toString);
	}
}
