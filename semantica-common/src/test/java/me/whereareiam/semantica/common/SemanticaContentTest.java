package me.whereareiam.semantica.common;

import me.whereareiam.semantica.Content;
import me.whereareiam.semantica.Message;
import me.whereareiam.semantica.Value;
import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.message.ResolvedMessage;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;
import me.whereareiam.semantica.testing.IssueAssert;
import me.whereareiam.semantica.testing.MemorySource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contents written in configs: an administrator's own text, translated texts pulled in by reference, and both.
 */
class SemanticaContentTest {
	private static final Locale EN = Locale.ENGLISH;
	private static final Locale DE = Locale.GERMAN;

	private final List<Issue> issues = new ArrayList<>();
	private final Semantica semantica = Semantica.builder()
			.syntax(new IcuSyntax())
			.issues(issues::add)
			.layer(Layer.of(new MemorySource("defaults")
					.put(EN, "chirper.vote", "Vote for us, {player}!")
					.put(DE, "chirper.vote", "Stimm für uns ab, {player}!")
					.put(EN, "chirper.sale", "Sale")))
			.build();

	@Test
	void resolvesAnAdministratorsOwnText() {
		ResolvedMessage message = semantica.resolve(Content.of("<gold>Welcome, {player}!"), Map.of("player", Value.text("Ann")), List.of(EN));

		assertEquals("<gold>Welcome, Ann!", message.plain());
		assertEquals(Locale.ROOT, message.getLocale());
		assertTrue(message.isFound());
		assertTrue(message.getParts().get(1) instanceof ResolvedMessage.Filled);
	}

	@Test
	void resolvesAReferenceInTheReadersLocaleWithTheContentsValues() {
		Content vote = Content.of("{@chirper.vote}");

		assertEquals("Stimm für uns ab, Ann!", semantica.resolve(vote, Map.of("player", Value.text("Ann")), List.of(DE)).plain());
		assertEquals("Vote for us, Ann!", semantica.resolve(vote, Map.of("player", Value.text("Ann")), List.of(EN)).plain());
	}

	@Test
	void mixesOwnTextAndReferences() {
		Content sale = Content.of("<gold>{@chirper.sale} <gray>({days, plural, one {# day} other {# days}} left)");

		assertEquals("<gold>Sale <gray>(3 days left)", semantica.resolve(sale, Map.of("days", Value.number(3)), List.of(EN)).plain());
	}

	@Test
	void showsTheSourceOfAContentThatIsNoValidMessage() {
		ResolvedMessage message = semantica.resolve(Content.of("Broken {player"), Map.of(), List.of(EN));

		assertEquals("Broken {player", message.plain());
		assertFalse(message.isFound());
		IssueAssert.assertReported(issues, Issue.Kind.SYNTAX, "Broken {player", null);
	}

	@Test
	void checkReportsSyntaxErrorsAndUnknownReferences() {
		assertEquals(List.of(), semantica.check(Content.of("<gold>{@chirper.sale}")));

		List<Issue> unknown = semantica.check(Content.of("{@chirper.missing} {@chirper.sale}"));
		assertEquals(1, unknown.size(), unknown::toString);
		assertEquals(Issue.Kind.MISSING_REFERENCE, unknown.getFirst().getKind());
		assertTrue(unknown.getFirst().getDetail().contains("chirper.missing"));

		assertEquals(Issue.Kind.SYNTAX, semantica.check(Content.of("{player")).getFirst().getKind());
	}

	@Test
	void aMessageBecomesAReferenceToItsKey() {
		assertEquals("{@chirper.vote}", Content.of(Message.of("chirper.vote")).getSource());
		assertThrows(IllegalArgumentException.class, () -> Content.of(Message.of("chirper.vote").with("player", "Ann")));
	}
}
