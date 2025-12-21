package me.whereareiam.semantica.common.processor;

import me.whereareiam.semantica.model.TextValue;

/**
 * Converts TextValue to string.
 */
public class TextProcessor {
	public String process(TextValue value) {
		if (value == null) throw new IllegalArgumentException("Text cannot be null");
		return value.asString();
	}
}

