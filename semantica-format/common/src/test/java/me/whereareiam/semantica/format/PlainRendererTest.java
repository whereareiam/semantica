package me.whereareiam.semantica.format;

import me.whereareiam.semantica.message.ParsedMessage;
import me.whereareiam.semantica.message.ResolvedMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlainRendererTest {
	private final ResolvedMessage message = new ResolvedMessage("k", Locale.ENGLISH, List.of(
			new ResolvedMessage.Literal("<red>Hello</red>, <hover:show_text:'hi'>"),
			new ResolvedMessage.Value("name", "<b>Ann</b>", "<b>Ann</b>", ParsedMessage.Argument.Format.NONE, null),
			new ResolvedMessage.Literal("</hover>! 3 < 4")
	), true);

	@Test
	void removesTagsOnlyFromLiterals() {
		assertEquals("Hello, <b>Ann</b>! 3 < 4", PlainRenderer.withoutTags().render(message));
	}

	@Test
	void keepsLiteralsAsWritten() {
		assertEquals("<red>Hello</red>, <hover:show_text:'hi'><b>Ann</b></hover>! 3 < 4", PlainRenderer.asWritten().render(message));
	}
}
