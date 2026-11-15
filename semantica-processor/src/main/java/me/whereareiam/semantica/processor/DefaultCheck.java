package me.whereareiam.semantica.processor;

import me.whereareiam.semantica.exception.SyntaxException;
import me.whereareiam.semantica.message.ParsedMessage;
import me.whereareiam.semantica.message.ParsedMessage.Argument;
import me.whereareiam.semantica.message.ParsedMessage.Plural;
import me.whereareiam.semantica.message.ParsedMessage.Select;
import me.whereareiam.semantica.processor.MessageGroup.MessageMethod;
import me.whereareiam.semantica.syntax.Syntax;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Checks the default of a message method against its parameters: the text is valid in the message syntax, each of its
 * arguments is a parameter, and a plural or a number takes a number, a date or a time a date. A parameter no argument
 * uses is a warning, since a translation may still use it.
 */
final class DefaultCheck {
	private static final Syntax SYNTAX = new IcuSyntax();

	private final Report report;
	private final Types types;
	private final TypeMirror number;
	private final TypeMirror temporal;
	private final TypeMirror date;

	DefaultCheck(ProcessingEnvironment environment, Report report) {
		this.report = report;
		this.types = environment.getTypeUtils();
		this.number = type(environment, "java.lang.Number");
		this.temporal = type(environment, "java.time.temporal.TemporalAccessor");
		this.date = type(environment, "java.util.Date");
	}

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

		Map<String, TypeMirror> parameters = new LinkedHashMap<>();
		for (VariableElement parameter : method.method().getParameters())
			parameters.put(parameter.getSimpleName().toString(), parameter.asType());

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

	private void types(MessageMethod method, ParsedMessage message, Map<String, TypeMirror> parameters) {
		for (ParsedMessage.Node node : message.getNodes()) {
			switch (node) {
				case Argument argument -> argument(method, argument, parameters.get(argument.name()));
				case Plural plural -> {
					TypeMirror type = parameters.get(plural.name());
					if (type != null && !numeric(type))
						error(method, "The plural {" + plural.name() + "} needs a number, but the parameter is " + type);
					plural.branches().values().forEach(branch -> types(method, branch, parameters));
				}
				case Select select -> select.branches().values().forEach(branch -> types(method, branch, parameters));
				default -> {
				}
			}
		}
	}

	private void argument(MessageMethod method, Argument argument, TypeMirror type) {
		if (type == null) return;

		boolean fits = switch (argument.format()) {
			case NONE -> true;
			case NUMBER -> numeric(type);
			case DATE, TIME -> types.isAssignable(type, temporal) || types.isAssignable(type, date);
		};
		if (!fits) {
			String format = argument.format().name().toLowerCase(Locale.ROOT);
			error(method, "{" + argument.name() + ", " + format + "} cannot format the parameter's type " + type);
		}
	}

	private boolean numeric(TypeMirror type) {
		if (type.getKind().isPrimitive()) return type.getKind() != TypeKind.BOOLEAN && type.getKind() != TypeKind.CHAR;

		return types.isAssignable(type, number);
	}

	private void error(MessageMethod method, String text) {
		report.error(method.method(), text);
	}

	private void warning(MessageMethod method, String text) {
		report.warning(method.method(), text);
	}

	private static TypeMirror type(ProcessingEnvironment environment, String name) {
		return environment.getElementUtils().getTypeElement(name).asType();
	}
}
