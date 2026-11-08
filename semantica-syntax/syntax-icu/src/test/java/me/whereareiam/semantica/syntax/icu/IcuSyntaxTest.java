package me.whereareiam.semantica.syntax.icu;

import me.whereareiam.semantica.exception.SyntaxException;
import me.whereareiam.semantica.message.ParsedMessage;
import me.whereareiam.semantica.message.ParsedMessage.Argument;
import me.whereareiam.semantica.message.ParsedMessage.Literal;
import me.whereareiam.semantica.message.ParsedMessage.Plural;
import me.whereareiam.semantica.message.ParsedMessage.Reference;
import me.whereareiam.semantica.message.ParsedMessage.Select;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IcuSyntaxTest {
	private final IcuSyntax syntax = new IcuSyntax();

	@Test
	void readsLiteralsAndArguments() {
		ParsedMessage message = syntax.parse("Hello, {name}! You have {count, number, integer}.");

		assertEquals(List.of(
				new Literal("Hello, "),
				new Argument("name", Argument.Format.NONE, null),
				new Literal("! You have "),
				new Argument("count", Argument.Format.NUMBER, "integer"),
				new Literal(".")
		), message.getNodes());
	}

	@Test
	void readsPluralsWithOffsetsAndCount() {
		ParsedMessage message = syntax.parse("{count, plural, offset:1 =0 {none} one {# other} other {# others}}");

		Plural plural = (Plural) message.getNodes().getFirst();
		assertEquals(1, plural.offset());
		assertEquals(Set.of("=0", "one", "other"), plural.branches().keySet());
		assertEquals(new Argument(ParsedMessage.COUNT, Argument.Format.NUMBER, null), plural.branches().get("one").getNodes().getFirst());
	}

	@Test
	void readsReferencesWithRenames() {
		ParsedMessage message = syntax.parse("{@vocabulary.player count=online}");

		assertEquals(new Reference("vocabulary.player", Map.of("count", "online")), message.getNodes().getFirst());
		assertEquals(Set.of("online"), message.arguments());
		assertEquals(Set.of("vocabulary.player"), message.references());
	}

	@Test
	void readsNestedChoices() {
		ParsedMessage message = syntax.parse("{gender, select, female {{count, plural, one {her item} other {her # items}}} other {{@thing}}}");

		Select select = (Select) message.getNodes().getFirst();
		assertEquals(Set.of("gender", "count"), message.arguments());
		assertEquals(Set.of("gender"), message.requiredArguments());
		assertEquals(Set.of("thing"), message.references());
		assertEquals(Plural.class, select.branches().get("female").getNodes().getFirst().getClass());
	}

	@Test
	void keepsMarkupAsText() {
		assertEquals(List.of(new Literal("<red>Hi</red> <click:run_command:/spawn>go</click>")),
				syntax.parse("<red>Hi</red> <click:run_command:/spawn>go</click>").getNodes());
	}

	@Test
	void quotesWithApostrophes() {
		assertEquals(List.of(new Literal("It's {not} an argument, isn't it")),
				syntax.parse("It's '{not}' an argument, isn''t it").getNodes());
	}

	@Test
	void treatsHashAsTextOutsidePlurals() {
		assertEquals(List.of(new Literal("#1 ")), syntax.parse("#1 ").getNodes());
	}

	@Test
	void rejectsBrokenMessages() {
		SyntaxException unclosed = assertThrows(SyntaxException.class, () -> syntax.parse("Hello {name"));
		assertEquals(11, unclosed.getPosition());

		assertThrows(SyntaxException.class, () -> syntax.parse("{count, plural, one {x}}"));
		assertThrows(SyntaxException.class, () -> syntax.parse("{count, plural, lots {x} other {y}}"));
		assertThrows(SyntaxException.class, () -> syntax.parse("{x, select, a {1} a {2} other {3}}"));
		assertThrows(SyntaxException.class, () -> syntax.parse("{@}"));
		assertThrows(SyntaxException.class, () -> syntax.parse("{@a.b c=}"));
		assertThrows(SyntaxException.class, () -> syntax.parse("{@a.b c=d c=e}"));
		assertThrows(SyntaxException.class, () -> syntax.parse("{x, spellout}"));
		assertThrows(SyntaxException.class, () -> syntax.parse("}"));
	}
}
