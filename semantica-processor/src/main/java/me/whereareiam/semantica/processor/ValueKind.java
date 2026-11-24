package me.whereareiam.semantica.processor;

import lombok.RequiredArgsConstructor;
import me.whereareiam.semantica.Value;

/**
 * What a message method's parameter is as a value, and how the implementation passes it on.
 */
@RequiredArgsConstructor
enum ValueKind {
	TEXT("text", "%s.text(%s)"),
	CHARACTERS("text", "%s.text(String.valueOf(%s))"),
	NUMBER("number", "%s.number(%s)"),
	TIME("date or time", "%s.time(%s)"),
	FLAG("boolean", "%s.flag(%s)"),
	CHOICE("enum constant", "%s.choice(%s)"),
	NESTED("message", "%s.nested(%s)"),
	VALUE("value", "%2$s");

	private static final String VALUE_TYPE = Value.class.getName();

	private final String description;
	private final String template;

	/**
	 * Returns the expression that passes a parameter on as a value.
	 *
	 * @param parameter the parameter's name
	 * @return the expression
	 */
	String write(String parameter) {
		return String.format(template, VALUE_TYPE, parameter);
	}

	/**
	 * Tells whether a parameter of this kind may stand where a message asks for a kind; a {@link Value} may be any.
	 *
	 * @param wanted the kind the message asks for
	 * @return whether it fits
	 */
	boolean fits(ValueKind wanted) {
		return this == wanted || this == VALUE;
	}

	@Override
	public String toString() {
		return description;
	}
}
