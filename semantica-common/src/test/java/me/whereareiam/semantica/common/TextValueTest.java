package me.whereareiam.semantica.common;

import me.whereareiam.semantica.model.TextValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextValueTest {
	@Test
	void listValueJoinsWithNewlines() {
		TextValue value = new TextValue.ListValue(List.of("a", "b", "c"));
		assertEquals("a\nb\nc", value.asString());
	}
}

