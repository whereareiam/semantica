package me.whereareiam.semantica.processor;

import lombok.RequiredArgsConstructor;
import me.whereareiam.semantica.exception.SyntaxException;
import me.whereareiam.semantica.message.ParsedMessage;
import me.whereareiam.semantica.message.ParsedMessage.Argument;
import me.whereareiam.semantica.message.ParsedMessage.Plural;
import me.whereareiam.semantica.message.ParsedMessage.Select;
import me.whereareiam.semantica.processor.MessageGroup.MessageMethod;
import me.whereareiam.semantica.syntax.Syntax;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Checks the default of a message method against its parameters: the text is valid in the message syntax, each of its
 * arguments is a parameter, and a plural or a number takes a number, a date or a time a date. A parameter no argument
 * uses is a warning, since a translation may still use it.
 */
@RequiredArgsConstructor
final class DefaultCheck {
	private static final Syntax SYNTAX = new IcuSyntax();

	private final Report report;

	/**
	 * Checks a method's default.
	 *
	 * @param method the method, with its default
	 * @return the parsed default, or {@code null} when it is no valid message
	 */
	ParsedMessage check(MessageMethod method) {
		ParsedMessage parsed;
		try {
			parsed = SYNTAX.parse(String.join("\n", method.lines()));
		} catch (SyntaxException exception) {
			error(method, "The default of " + method.key() + " is no valid message: " + exception.getMessage());
			return null;
		}

		Map<String, ValueKind> parameters = method.values();

		Set<String> arguments = parsed.arguments();
		for (String argument : arguments) {
			if (!parameters.containsKey(argument))
				error(method, "{" + argument + "} has no parameter; the parameters of " + method.key() + " are " + parameters.keySet());
		}

		for (String parameter : parameters.keySet()) {
			if (!arguments.contains(parameter))
				warning(method, "The parameter " + parameter + " is used by no argument of the default of " + method.key());
		}

		types(method, parsed, parameters);
		return parsed;
	}

	private void types(MessageMethod method, ParsedMessage message, Map<String, ValueKind> parameters) {
		for (ParsedMessage.Node node : message.getNodes()) {
			switch (node) {
				case Argument argument -> argument(method, argument, parameters.get(argument.name()));
				case Plural plural -> {
					ValueKind kind = parameters.get(plural.name());
					if (kind != null && !kind.fits(ValueKind.NUMBER))
						error(method, "The plural {" + plural.name() + "} needs a number, but the parameter is a " + kind);
					plural.branches().values().forEach(branch -> types(method, branch, parameters));
				}
				case Select select -> select.branches().values().forEach(branch -> types(method, branch, parameters));
				default -> {
				}
			}
		}
	}

	private void argument(MessageMethod method, Argument argument, ValueKind kind) {
		if (kind == null) return;

		boolean fits = switch (argument.format()) {
			case NONE -> true;
			case NUMBER -> kind.fits(ValueKind.NUMBER);
			case DATE, TIME -> kind.fits(ValueKind.TIME);
		};
		if (!fits) {
			String format = argument.format().name().toLowerCase(Locale.ROOT);
			error(method, "{" + argument.name() + ", " + format + "} cannot format the parameter, a " + kind);
		}
	}

	private void error(MessageMethod method, String text) {
		report.error(method.method(), text);
	}

	private void warning(MessageMethod method, String text) {
		report.warning(method.method(), text);
	}

}
