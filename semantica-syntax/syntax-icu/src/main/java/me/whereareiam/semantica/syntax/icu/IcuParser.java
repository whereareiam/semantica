package me.whereareiam.semantica.syntax.icu;

import me.whereareiam.semantica.exception.SyntaxException;
import me.whereareiam.semantica.message.ParsedMessage;
import me.whereareiam.semantica.message.ParsedMessage.Argument;
import me.whereareiam.semantica.message.ParsedMessage.Literal;
import me.whereareiam.semantica.message.ParsedMessage.Node;
import me.whereareiam.semantica.message.ParsedMessage.Plural;
import me.whereareiam.semantica.message.ParsedMessage.Reference;
import me.whereareiam.semantica.message.ParsedMessage.Select;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads one message; one instance per message.
 */
final class IcuParser {
	private static final Set<String> PLURAL_FORMS = Set.of("zero", "one", "two", "few", "many", "other");

	private final String source;
	private int position;

	IcuParser(String source) {
		this.source = source;
	}

	ParsedMessage parse() {
		ParsedMessage message = message(false);
		if (position < source.length()) throw error("Unexpected '}' without an opening '{'");

		return message;
	}

	private ParsedMessage message(boolean inPlural) {
		List<Node> nodes = new ArrayList<>();
		StringBuilder text = new StringBuilder();

		while (position < source.length()) {
			char current = source.charAt(position);
			if (current == '}') break;

			if (current == '{') {
				flush(text, nodes);
				nodes.add(placeholder());
				continue;
			}

			if (current == '#' && inPlural) {
				flush(text, nodes);
				nodes.add(new Argument(ParsedMessage.COUNT, Argument.Format.NUMBER, null));
				position++;
				continue;
			}

			if (current == '\'') {
				quoted(text, inPlural);
				continue;
			}

			text.append(current);
			position++;
		}

		flush(text, nodes);
		return new ParsedMessage(nodes);
	}

	private void quoted(StringBuilder text, boolean inPlural) {
		if (position + 1 < source.length() && source.charAt(position + 1) == '\'') {
			text.append('\'');
			position += 2;
			return;
		}

		boolean starts = position + 1 < source.length() && isQuotable(source.charAt(position + 1), inPlural);
		if (!starts) {
			text.append('\'');
			position++;
			return;
		}

		position++;
		while (position < source.length()) {
			char current = source.charAt(position);
			if (current == '\'') {
				if (position + 1 < source.length() && source.charAt(position + 1) == '\'') {
					text.append('\'');
					position += 2;
					continue;
				}
				position++;
				return;
			}
			text.append(current);
			position++;
		}
	}

	private static boolean isQuotable(char next, boolean inPlural) {
		return next == '{' || next == '}' || next == '|' || (inPlural && next == '#');
	}

	private Node placeholder() {
		position++;
		whitespace();
		if (peek() == '@') return reference();

		String name = argumentName();
		whitespace();
		if (peek() == '}') {
			position++;
			return new Argument(name, Argument.Format.NONE, null);
		}

		expect(',');
		whitespace();
		String type = identifier("a type such as number, date, time, plural, selectordinal or select");
		whitespace();

		return switch (type) {
			case "number" -> formatted(name, Argument.Format.NUMBER);
			case "date" -> formatted(name, Argument.Format.DATE);
			case "time" -> formatted(name, Argument.Format.TIME);
			case "plural" -> plural(name, false);
			case "selectordinal" -> plural(name, true);
			case "select" -> select(name);
			default -> throw new SyntaxException("Unknown type '" + type + "'", position - type.length());
		};
	}

	private Argument formatted(String name, Argument.Format format) {
		if (peek() == '}') {
			position++;
			return new Argument(name, format, null);
		}

		expect(',');
		int start = position;
		StringBuilder style = new StringBuilder();
		while (position < source.length() && source.charAt(position) != '}') {
			char current = source.charAt(position);
			if (current == '{') throw error("A style cannot contain '{'");
			if (current == '\'') {
				quoted(style, false);
				continue;
			}
			style.append(current);
			position++;
		}
		if (position >= source.length()) throw new SyntaxException("Unclosed argument", start);
		position++;

		String trimmed = style.toString().trim();
		if (trimmed.isEmpty()) throw new SyntaxException("Empty style", start);

		return new Argument(name, format, trimmed);
	}

	private Plural plural(String name, boolean ordinal) {
		expect(',');
		whitespace();

		long offset = 0;
		if (source.startsWith("offset:", position)) {
			position += "offset:".length();
			whitespace();
			offset = number();
			whitespace();
		}

		Map<String, ParsedMessage> branches = new LinkedHashMap<>();
		while (peek() != '}') {
			int start = position;
			branch(pluralSelector(), start, branches, true);
		}
		position++;

		if (!branches.containsKey("other")) throw error("A plural needs an 'other' branch");
		return new Plural(name, ordinal, offset, branches);
	}

	private String pluralSelector() {
		int start = position;
		if (peek() == '=') {
			position++;
			return "=" + number();
		}

		String form = identifier("a plural form or an exact value such as =0");
		if (!PLURAL_FORMS.contains(form)) throw new SyntaxException("Unknown plural form '" + form + "'", start);

		return form;
	}

	private Select select(String name) {
		expect(',');
		whitespace();

		Map<String, ParsedMessage> branches = new LinkedHashMap<>();
		while (peek() != '}') {
			int start = position;
			String selector = selectValue();
			branch(selector, start, branches, false);
		}
		position++;

		if (!branches.containsKey("other")) throw error("A select needs an 'other' branch");
		return new Select(name, branches);
	}

	private void branch(String selector, int start, Map<String, ParsedMessage> branches, boolean inPlural) {
		if (branches.containsKey(selector)) throw new SyntaxException("Branch '" + selector + "' appears twice", start);

		whitespace();
		expect('{');
		ParsedMessage branch = message(inPlural);
		expect('}');
		whitespace();

		branches.put(selector, branch);
	}

	private Reference reference() {
		position++;
		int keyStart = position;
		while (position < source.length() && isKeyCharacter(source.charAt(position))) position++;
		String key = source.substring(keyStart, position);
		if (key.isEmpty() || key.startsWith(".") || key.endsWith(".") || key.contains(".."))
			throw new SyntaxException("A reference needs a key such as {@vocabulary.player}", keyStart);

		Map<String, String> renames = new LinkedHashMap<>();
		while (true) {
			boolean spaced = whitespace();
			if (peek() == '}') break;
			if (!spaced) throw error("Expected a space before a rename such as count=online");

			int renameStart = position;
			String target = argumentName();
			expect('=');
			String from = argumentName();
			if (renames.put(target, from) != null)
				throw new SyntaxException("Argument '" + target + "' is renamed twice", renameStart);
		}
		position++;

		return new Reference(key, renames);
	}

	private static boolean isKeyCharacter(char character) {
		return Character.isLetterOrDigit(character) || character == '_' || character == '-' || character == '.';
	}

	private String argumentName() {
		int start = position;
		if (position < source.length() && Character.isDigit(source.charAt(position))) {
			while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
			return source.substring(start, position);
		}

		while (position < source.length()) {
			char current = source.charAt(position);
			boolean valid = position == start
					? Character.isLetter(current) || current == '_'
					: Character.isLetterOrDigit(current) || current == '_';
			if (!valid) break;
			position++;
		}
		if (position == start) throw error("Expected an argument name");

		return source.substring(start, position);
	}

	private String identifier(String expected) {
		int start = position;
		while (position < source.length() && Character.isLetter(source.charAt(position))) position++;
		if (position == start) throw error("Expected " + expected);

		return source.substring(start, position);
	}

	private String selectValue() {
		int start = position;
		while (position < source.length()) {
			char current = source.charAt(position);
			if (Character.isWhitespace(current) || current == '{' || current == '}') break;
			position++;
		}
		if (position == start) throw error("Expected a value such as 'female' or 'other'");

		return source.substring(start, position);
	}

	private long number() {
		int start = position;
		if (peek() == '-') position++;
		while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
		if (position == start || source.substring(start, position).equals("-")) throw error("Expected a number");

		try {
			return Long.parseLong(source.substring(start, position));
		} catch (NumberFormatException exception) {
			throw new SyntaxException("Number out of range", start);
		}
	}

	private boolean whitespace() {
		int start = position;
		while (position < source.length() && Character.isWhitespace(source.charAt(position))) position++;

		return position > start;
	}

	private char peek() {
		if (position >= source.length()) throw error("Unexpected end of the message; a '}' is missing");
		return source.charAt(position);
	}

	private void expect(char expected) {
		if (peek() != expected) throw error("Expected '" + expected + "'");
		position++;
	}

	private SyntaxException error(String message) {
		return new SyntaxException(message, position);
	}

	private static void flush(StringBuilder text, List<Node> nodes) {
		if (text.isEmpty()) return;

		nodes.add(new Literal(text.toString()));
		text.setLength(0);
	}
}
