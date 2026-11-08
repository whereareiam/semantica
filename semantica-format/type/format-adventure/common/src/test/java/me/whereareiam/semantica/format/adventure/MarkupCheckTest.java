package me.whereareiam.semantica.format.adventure;

import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.common.Semantica;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;
import me.whereareiam.semantica.testing.IssueAssert;
import me.whereareiam.semantica.testing.MemorySource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkupCheckTest {
	private static final Locale EN = Locale.ENGLISH;
	private static final Locale RU = Locale.forLanguageTag("ru");
	private static final MiniMessageTags TAGS = name -> Map.of(
			"red", TagPolicy.Group.COLOR,
			"gold", TagPolicy.Group.COLOR,
			"bold", TagPolicy.Group.DECORATION,
			"click", TagPolicy.Group.CLICK
	).get(name);

	private final Semantica semantica = Semantica.builder()
			.syntax(new IcuSyntax())
			.watch(false)
			.issues(issue -> {
			})
			.layer(Layer.of(new MemorySource("test")
					.put(EN, "fine", "<red>Hi</red> <!bold>{count, plural, one {<gold>#</gold>} other {<gold>#</gold>}}")
					.put(RU, "fine", "<red>Привет</red> <!bold>{count, plural, one {<gold>#</gold>} few {<gold>#</gold>} many {<gold>#</gold>} other {<gold>#</gold>}}")
					.put(EN, "spawn", "<click:run_command:/spawn>Go</click>")
					.put(EN, "typo", "<gren>Typo</gren> \\<escaped>")
					.put(EN, "colored", "<red>Fine</red>")
					.put(RU, "colored", "<gold>Хорошо</gold>")
					.put(EN, "chat.raw", "<gren>not MiniMessage</gren>")))
			.build();

	private List<Issue> check(TagPolicy policy) {
		Catalog catalog = semantica.getCatalog();
		return new MarkupCheck(policy, TAGS, key -> !key.startsWith("chat.")).check(catalog);
	}

	@Test
	void reportsTagsOutsideThePolicyAndUnknownTags() {
		List<Issue> issues = check(TagPolicy.defaults());

		IssueAssert.assertReported(issues, Issue.Kind.MARKUP, "spawn", EN);
		Issue typo = IssueAssert.assertReported(issues, Issue.Kind.MARKUP, "typo", EN);
		assertTrue(typo.getDetail().contains("<gren>"), typo::getDetail);
		assertEquals(1, issues.stream().filter(issue -> "typo".equals(issue.getKey())).count());
	}

	@Test
	void reportsTranslationsWithOtherTags() {
		Issue issue = IssueAssert.assertReported(check(TagPolicy.defaults()), Issue.Kind.MARKUP, "colored", RU);

		assertTrue(issue.getDetail().contains("lacks [red]") && issue.getDetail().contains("adds [gold]"), issue::getDetail);
	}

	@Test
	void acceptsAllowedTagsAndSkipsOtherKeys() {
		List<Issue> issues = check(TagPolicy.defaults().allow(TagPolicy.Group.CLICK));

		assertTrue(issues.stream().noneMatch(issue -> "spawn".equals(issue.getKey()) || "fine".equals(issue.getKey())
				|| "chat.raw".equals(issue.getKey())), issues::toString);
	}

	@Test
	void convertsMinecraftLocaleCodes() {
		assertEquals(Locale.forLanguageTag("pt-BR"), MinecraftLocales.toLocale("pt_br"));
		assertEquals("en_us", MinecraftLocales.toCode(Locale.US));
		assertEquals("de", MinecraftLocales.toCode(Locale.GERMAN));
		assertEquals(Locale.ROOT, MinecraftLocales.toLocale("!!"));
	}
}
