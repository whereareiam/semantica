package me.whereareiam.semantica.source.file;

import me.whereareiam.configura.Configura;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.source.Source;
import me.whereareiam.semantica.testing.IssueAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSourceTest {
	private static final Locale EN = Locale.ENGLISH;
	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

	@TempDir
	Path folder;

	private static void write(Path path, String text) throws IOException {
		Files.createDirectories(path.getParent());
		Files.writeString(path, text, StandardCharsets.UTF_8);
	}

	private void writeSample(Path root) throws IOException {
		write(root.resolve("en/game/menu.json"), """
				{"title": "Menu", "button": {"close": "Close"}, "lore": ["Line one", "Line {two}"]}
				""");
		write(root.resolve("en/common.yml"), """
				unit: "{count, plural, one {# unit} other {# units}}"
				""");
		write(root.resolve("pt-BR/chat.properties"), "greeting=Olá, {name}!\n");
	}

	@Test
	void readsOneFolderPerLocaleWithThePathAsPrefix() throws IOException {
		writeSample(folder);

		Source.Content content = FileSource.of(folder, Configura.builder().build()).load();

		IssueAssert.assertNone(content.getIssues());
		assertEquals("Menu", content.entries(EN).get("game.menu.title").text());
		assertEquals("Close", content.entries(EN).get("game.menu.button.close").text());
		assertEquals("Line one\nLine {two}", content.entries(EN).get("game.menu.lore").text());
		assertEquals("{count, plural, one {# unit} other {# units}}", content.entries(EN).get("common.unit").text());
		assertEquals("Olá, {name}!", content.entries(PT_BR).get("chat.greeting").text());
		assertEquals("en/game/menu.json", content.entries(EN).get("game.menu.title").origin());
	}

	@Test
	void reportsWhatItCannotRead() throws IOException {
		write(folder.resolve("english/a.json"), "{}");
		write(folder.resolve("en/broken.json"), "{\"a\": ");
		write(folder.resolve("en/number.json"), "{\"a\": 5}");
		write(folder.resolve("en/notes.txt"), "hello");
		write(folder.resolve("en/game.json"), "{\"menu\": {\"title\": \"A\"}}");
		write(folder.resolve("en/game/menu.json"), "{\"title\": \"B\"}");
		write(folder.resolve(".reference/en/game.json"), "{\"menu\": {\"title\": \"C\"}}");
		write(folder.resolve("semantica.json"), "{}");

		Source.Content content = FileSource.of(folder, Configura.builder().build()).load();

		IssueAssert.assertReported(content.getIssues(), Issue.Kind.SOURCE, null, null);
		IssueAssert.assertReported(content.getIssues(), Issue.Kind.SOURCE, "number.a", EN);
		IssueAssert.assertReported(content.getIssues(), Issue.Kind.SOURCE, "game.menu.title", EN);
		assertEquals(5, content.getIssues().size(), content.getIssues()::toString);
		assertEquals("B", content.entries(EN).get("game.menu.title").text());
	}

	@Test
	void readsTheRootLocaleFromItsFolder() throws IOException {
		write(folder.resolve("all/identica.yml"), "login:\n  failed: \"{@game.prefix} {@game.account.login-failed}\"\n");
		write(folder.resolve("neutral/identica.yml"), "login:\n  failed: \"named\"\n");

		Source.Content content = FileSource.of(folder, Configura.builder().build()).load();
		assertEquals("{@game.prefix} {@game.account.login-failed}", content.entries(Locale.ROOT).get("identica.login.failed").text());
		assertEquals("all/identica.yml", content.entries(Locale.ROOT).get("identica.login.failed").origin());

		Source.Content renamed = FileSource.of(folder, Configura.builder().build(), "neutral").load();
		assertEquals("named", renamed.entries(Locale.ROOT).get("identica.login.failed").text());
	}

	@Test
	void leavesOutSkippedFoldersWithoutAnIssue() throws IOException {
		writeSample(folder);
		write(folder.resolve("rules/chat.yml"), "patterns: []\n");
		write(folder.resolve("local/en/game/menu.json"), "{\"title\": \"Local\"}");
		write(folder.resolve("local/rules/chat.yml"), "patterns: []\n");

		Source.Content shared = FileSource.builder(folder, Configura.builder().build()).skip("rules", "local").build().load();
		IssueAssert.assertNone(shared.getIssues());
		assertEquals("Menu", shared.entries(EN).get("game.menu.title").text());

		Source.Content local = FileSource.builder(folder.resolve("local"), Configura.builder().build()).skip("rules").build().load();
		IssueAssert.assertNone(local.getIssues());
		assertEquals("Local", local.entries(EN).get("game.menu.title").text());

		IssueAssert.assertReported(FileSource.of(folder, Configura.builder().build()).load().getIssues(), Issue.Kind.SOURCE, null, null);
	}

	@Test
	void doesNotWatchSkippedFolders() throws Exception {
		writeSample(folder);
		write(folder.resolve("rules/chat.yml"), "patterns: []\n");
		CountDownLatch changed = new CountDownLatch(1);

		try (AutoCloseable ignored = FileSource.builder(folder, Configura.builder().build()).skip("rules").build().watch(changed::countDown)) {
			write(folder.resolve("rules/chat.yml"), "patterns: [a]\n");
			assertFalse(changed.await(2, TimeUnit.SECONDS));

			write(folder.resolve("en/game/menu.json"), "{\"title\": \"Changed\"}");
			assertTrue(changed.await(10, TimeUnit.SECONDS));
		}
	}

	@Test
	void aMissingFolderHasNoTranslations() throws IOException {
		Source.Content content = FileSource.of(folder.resolve("absent"), Configura.builder().build()).load();

		assertTrue(content.getLocales().isEmpty());
		assertFalse(Files.exists(folder.resolve("absent")));
	}

	@Test
	void tellsWhenAFileChanges() throws Exception {
		writeSample(folder);
		CountDownLatch changed = new CountDownLatch(1);

		try (AutoCloseable ignored = FileSource.of(folder, Configura.builder().build()).watch(changed::countDown)) {
			write(folder.resolve("en/game/menu.json"), "{\"title\": \"Changed\"}");

			assertTrue(changed.await(10, TimeUnit.SECONDS));
		}
	}

	@Test
	void tellsWhenTheFolderAppears() throws Exception {
		Path late = folder.resolve("languages");
		CountDownLatch changed = new CountDownLatch(1);

		try (AutoCloseable ignored = FileSource.of(late, Configura.builder().build()).watch(changed::countDown)) {
			Files.createDirectories(late);

			assertTrue(changed.await(10, TimeUnit.SECONDS));
		}
	}
}
