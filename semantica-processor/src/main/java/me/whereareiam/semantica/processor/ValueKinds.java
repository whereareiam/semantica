package me.whereareiam.semantica.processor;

import me.whereareiam.semantica.Message;
import me.whereareiam.semantica.Value;

import java.util.List;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Tells the value kind of a parameter's type, by the first type of a list it is assignable to. An enum constant that
 * is also a date field, such as a day of the week, is a choice.
 */
final class ValueKinds {
	private final Types types;
	private final List<Rule> rules;

	ValueKinds(ProcessingEnvironment environment) {
		Elements elements = environment.getElementUtils();
		this.types = environment.getTypeUtils();
		this.rules = List.of(
				new Rule(type(elements, Value.class.getName()), ValueKind.VALUE),
				new Rule(type(elements, Message.class.getName()), ValueKind.NESTED),
				new Rule(type(elements, "java.lang.String"), ValueKind.TEXT),
				new Rule(type(elements, "java.lang.CharSequence"), ValueKind.CHARACTERS),
				new Rule(type(elements, "java.lang.Character"), ValueKind.CHARACTERS),
				new Rule(type(elements, "java.lang.Boolean"), ValueKind.FLAG),
				new Rule(type(elements, "java.lang.Number"), ValueKind.NUMBER),
				new Rule(types.erasure(type(elements, "java.lang.Enum")), ValueKind.CHOICE),
				new Rule(type(elements, "java.time.temporal.TemporalAccessor"), ValueKind.TIME),
				new Rule(type(elements, "java.util.Date"), ValueKind.TIME)
		);
	}

	/**
	 * Returns the kind of a parameter's type.
	 *
	 * @param type the type
	 * @return the kind, or nothing for a type that is no value
	 */
	Optional<ValueKind> of(TypeMirror type) {
		if (type.getKind().isPrimitive()) return Optional.of(primitive(type.getKind()));
		if (type.getKind() != TypeKind.DECLARED) return Optional.empty();

		TypeMirror erased = types.erasure(type);
		return rules.stream()
				.filter(rule -> types.isAssignable(erased, rule.type()))
				.map(Rule::kind)
				.findFirst();
	}

	private static ValueKind primitive(TypeKind kind) {
		return switch (kind) {
			case BOOLEAN -> ValueKind.FLAG;
			case CHAR -> ValueKind.CHARACTERS;
			default -> ValueKind.NUMBER;
		};
	}

	private static TypeMirror type(Elements elements, String name) {
		return elements.getTypeElement(name).asType();
	}

	/**
	 * A parameter assignable to a type is of a kind.
	 *
	 * @param type the type
	 * @param kind the kind
	 */
	private record Rule(TypeMirror type, ValueKind kind) {
	}
}
