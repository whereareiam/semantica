package me.whereareiam.semantica.cli;

import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.common.Semantica;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;
import me.whereareiam.semantica.testing.IssueAssert;
import me.whereareiam.semantica.testing.MemorySource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftCheckTest {
	private static final Locale EN = Locale.ENGLISH;
	private static final Locale RU = Locale.forLanguageTag("ru");

	private final Semantica semantica = Semantica.builder()
			.syntax(new IcuSyntax())
			.watch(false)
			.issues(issue -> {
			})
			.layer(Layer.of(new MemorySource("test")
					.put(EN, "item.potion", "Potion")
					.put(EN, "item.potion.effect.water", "Water Bottle")
					.put(EN, "item.apostrophe", "Don't drink it")
					.put(EN, "item.doubled", "It''s gone")
					.put(EN, "item.quoted", "A '{' brace")
					.put(EN, "item.argument", "Hello, {name}")
					.put(EN, "item.plural", "{count, plural, one {# bottle} other {# bottles}}")
					.put(EN, "item.reference", "{@item.potion} of water")
					.put(EN, "item.specifiers", "%s gave %d to %1$s, 100%%")
					.put(EN, "item.unknown", "%f of %5s, %")
					.put(EN, "item.ordered", "%s killed %s")
					.put(RU, "item.ordered", "%2$s убит игроком %1$s")
					.put(EN, "item.typed", "%s has %d")
					.put(RU, "item.typed", "у %s есть %s")
					.put(EN, "chat.raw", "{name}'s '{'")))
			.build();

	private List<Issue> check() {
		return new MinecraftCheck(key -> key.startsWith("item.")).check(semantica.getCatalog());
	}

	private List<Issue> issues(String key, Locale locale) {
		return check().stream().filter(issue -> key.equals(issue.getKey()) && locale.equals(issue.getLocale())).toList();
	}

	@Test
	void acceptsPlainSentencesAndKeysThatAreAlsoPrefixes() {
		assertTrue(issues("item.potion", EN).isEmpty());
		assertTrue(issues("item.potion.effect.water", EN).isEmpty());
		assertTrue(issues("item.apostrophe", EN).isEmpty());
	}

	@Test
	void reportsIcuSyntax() {
		for (String key : List.of("item.argument", "item.plural", "item.reference", "item.quoted")) {
			Issue issue = IssueAssert.assertReported(check(), Issue.Kind.MARKUP, key, EN);
			assertTrue(issue.getDetail().contains("{ and }"), issue::getDetail);
		}

		Issue doubled = IssueAssert.assertReported(check(), Issue.Kind.MARKUP, "item.doubled", EN);
		assertTrue(doubled.getDetail().contains("apostrophes"), doubled::getDetail);
	}

	@Test
	void acceptsJavaSpecifiersAndReportsOtherPercentSequences() {
		assertTrue(issues("item.specifiers", EN).isEmpty());

		List<String> details = issues("item.unknown", EN).stream().map(Issue::getDetail).toList();
		assertEquals(3, details.size(), details::toString);
		assertTrue(details.get(0).startsWith("%f is no format specifier"), details::toString);
		assertTrue(details.get(1).startsWith("%5s is no format specifier"), details::toString);
		assertTrue(details.get(2).startsWith("% is no format specifier"), details::toString);
	}

	@Test
	void comparesTheSpecifiersOfTranslationsWithTheDefaultLocale() {
		assertTrue(issues("item.ordered", RU).isEmpty());

		Issue typed = IssueAssert.assertReported(check(), Issue.Kind.MARKUP, "item.typed", RU);
		assertEquals("Uses other format specifiers than en; lacks [%2$d]; adds [%2$s]", typed.getDetail());
	}

	@Test
	void skipsOtherKeys() {
		assertTrue(check().stream().noneMatch(issue -> "chat.raw".equals(issue.getKey())));
	}
}
