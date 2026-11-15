package me.whereareiam.semantica.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesProcessorTest {
	private static final String COMMANDS = """
			package com.example;

			import me.whereareiam.semantica.Message;
			import me.whereareiam.semantica.declaration.Default;
			import me.whereareiam.semantica.declaration.Messages;

			@Messages("commands")
			public interface CommandMessages {
				@Default("<white>You must wait <gray>{time, number} seconds</gray> before using this command again.")
				Message cooldown(double time);

				@Default({"<gold>Socialismus <gray>{version}", "<gray>Reloaded in {millis, number} ms."})
				Message reloaded(String version, long millis);

				@Default("{@commands.reload.done} {count, plural, one {# file} other {# files}}")
				Message staffJoined(int count);

				Reload reload();

				interface Reload {
					@Default("Done")
					Message done();

					Settings settings();
				}

				interface Settings {
					@Default("Settings of {player}")
					Message title(String player);
				}
			}
			""";

	@TempDir
	Path output;

	private Compilation compile(String source, String... options) throws IOException {
		String name = source.contains("interface CommandMessages") ? "CommandMessages" : "Declared";
		return Compilation.of(output, Map.of("com.example." + name, source), options);
	}

	private static String messages(String body) {
		return """
				package com.example;

				import me.whereareiam.semantica.Message;
				import me.whereareiam.semantica.declaration.Default;
				import me.whereareiam.semantica.declaration.KeySegment;
				import me.whereareiam.semantica.declaration.Messages;
				import me.whereareiam.semantica.declaration.Segment;

				@Messages("shop")
				public interface Declared {
				%s
				}
				""".formatted(body);
	}

	@Test
	void writesTheDefaultsOfTheDefaultLocale() throws IOException {
		Compilation compilation = compile(COMMANDS);

		assertTrue(compilation.success(), compilation.errors());
		assertEquals("""
				{
				  "cooldown": "<white>You must wait <gray>{time, number} seconds</gray> before using this command again.",
				  "reloaded": [
				    "<gold>Socialismus <gray>{version}",
				    "<gray>Reloaded in {millis, number} ms."
				  ],
				  "staff-joined": "{@commands.reload.done} {count, plural, one {# file} other {# files}}",
				  "reload": {
				    "done": "Done",
				    "settings": {
				      "title": "Settings of {player}"
				    }
				  }
				}
				""", compilation.file("META-INF/semantica/en/commands.json"));
	}

	@Test
	void writesAnImplementationWithConstantKeys() throws IOException {
		Compilation compilation = compile(COMMANDS);

		String source = compilation.file("com/example/GeneratedCommandMessages.java");
		assertTrue(source.contains("public final class GeneratedCommandMessages implements com.example.CommandMessages"), source);
		assertTrue(source.contains("me.whereareiam.semantica.Message.of(\"commands.cooldown\")\n\t\t\t\t.with(\"time\", time)"), source);
		assertTrue(source.contains("me.whereareiam.semantica.Message.of(\"commands.reload.settings.title\")"), source);
		assertTrue(compilation.exists("com/example/GeneratedCommandMessages.class"));
	}

	@Test
	void writesTheFilesOfTheLocaleGiven() throws IOException {
		Compilation compilation = compile(COMMANDS, "-Asemantica.locale=de");

		assertTrue(compilation.exists("META-INF/semantica/de/commands.json"));
	}

	@Test
	void writesAPrefixOfSeveralSegmentsAsFolders() throws IOException {
		Compilation compilation = compile(messages("@Default(\"Vote!\") Message vote();").replace("\"shop\"", "\"chirper.vote\""));

		assertTrue(compilation.success(), compilation.errors());
		assertTrue(compilation.exists("META-INF/semantica/en/chirper/vote.json"));
	}

	@Test
	void takesASegmentParameterAsTheLastSegmentOfTheKey() throws IOException {
		Compilation compilation = compile(messages("""
				Message denied(@Segment KeySegment reason, String player);

				Message named(@Segment String name);

				@Default("{@shop.denied.cooldown}")
				Message waiting();
				"""));

		assertTrue(compilation.success(), compilation.errors());
		String source = compilation.file("com/example/GeneratedDeclared.java");
		assertTrue(source.contains("Message.of(\"shop.denied.\" + reason.segment())\n\t\t\t\t.with(\"player\", player)"), source);
		assertTrue(source.contains("Message.of(\"shop.named.\" + name)"), source);
		assertEquals("{\n  \"waiting\": \"{@shop.denied.cooldown}\"\n}\n", compilation.file("META-INF/semantica/en/shop.json"));
	}

	@Test
	void failsOnAnArgumentWithoutParameter() throws IOException {
		Compilation compilation = compile(messages("@Default(\"Bought {item}\") Message bought(String name);"));

		assertFalse(compilation.success());
		assertTrue(compilation.errors().contains("{item} has no parameter; the parameters of shop.bought are [name]"), compilation.errors());
		assertFalse(compilation.exists("META-INF/semantica/en/shop.json"));
	}

	@Test
	void warnsOfAParameterNoArgumentUses() throws IOException {
		Compilation compilation = compile(messages("@Default(\"Bought\") Message bought(String item);"));

		assertTrue(compilation.success(), compilation.errors());
		assertTrue(compilation.warnings().contains("The parameter item is used by no argument"), compilation.warnings());
	}

	@Test
	void failsOnAPluralOrNumberOverAnotherType() throws IOException {
		Compilation compilation = compile(messages("""
				@Default("{count, plural, one {# item} other {# items}}") Message items(String count);
				@Default("{price, number}") Message price(boolean price);
				@Default("{when, date}") Message when(java.time.Instant when);
				"""));

		assertTrue(compilation.errors().contains("The plural {count} needs a number, but the parameter is java.lang.String"), compilation.errors());
		assertTrue(compilation.errors().contains("{price, number} cannot format the parameter's type boolean"), compilation.errors());
		assertFalse(compilation.errors().contains("{when"), compilation.errors());
	}

	@Test
	void failsOnASyntaxError() throws IOException {
		Compilation compilation = compile(messages("@Default(\"Bought {item\") Message bought(String item);"));

		assertTrue(compilation.errors().contains("The default of shop.bought is no valid message"), compilation.errors());
	}

	@Test
	void failsOnAReferenceToAnUndeclaredKeyOfItsOwnPrefixes() throws IOException {
		Compilation compilation = compile(messages("""
				@Default("{@shop.missing} {@other.key}") Message bought();
				"""));

		assertTrue(compilation.errors().contains("references {@shop.missing}, which no method of shop declares"), compilation.errors());
		assertFalse(compilation.errors().contains("other.key"), compilation.errors());
	}

	@Test
	void failsOnAMethodWithoutDefault() throws IOException {
		Compilation compilation = compile(messages("Message bought();"));

		assertTrue(compilation.errors().contains("Has no @Default; write the text of shop.bought"), compilation.errors());
	}

	@Test
	void failsOnTwoMethodsOfOneKey() throws IOException {
		Compilation compilation = compile(messages("""
				@Default("a") Message bought();
				@Default("{count}") Message bought(int count);
				"""));

		assertTrue(compilation.errors().contains("Gives the key shop.bought as bought does"), compilation.errors());
	}

	@Test
	void failsOnMisplacedDeclarations() throws IOException {
		Compilation compilation = compile(messages("""
				@Default("a") String name();
				@Default("a") Message denied(@Segment String reason);
				Message wrong(@Segment int reason);
				"""));

		assertTrue(compilation.errors().contains("Returns neither a Message nor an interface of more messages"), compilation.errors());
		assertTrue(compilation.errors().contains("A method with a @Segment parameter has no @Default"), compilation.errors());
		assertTrue(compilation.errors().contains("A @Segment parameter is a String or a KeySegment"), compilation.errors());
	}

	@Test
	void failsOnAGroupThatContainsItself() throws IOException {
		Compilation compilation = compile(messages("Declared again();"));

		assertTrue(compilation.errors().contains("The group Declared contains itself"), compilation.errors());
	}

	@Test
	void failsOnAnInvalidPrefix() throws IOException {
		Compilation compilation = compile(messages("@Default(\"a\") Message a();").replace("\"shop\"", "\"shop..x\""));

		assertTrue(compilation.errors().contains("'shop..x' is no key prefix"), compilation.errors());
	}
}
