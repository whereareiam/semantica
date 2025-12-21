package me.whereareiam.semantica.common.processor;

import me.whereareiam.semantica.common.TagTokens;
import me.whereareiam.semantica.TagConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConditionalProcessorTest {
	private final TagTokens tags = new TagTokens(TagConfiguration.defaults());
	private final ConditionalProcessor processor = new ConditionalProcessor(tags);

	@Test
	void resolvesIfElse() {
		String text = "<if enabled==true>YES<else>NO</if>";
		String resolved = processor.process(text, null, Map.of("enabled", "true"));
		assertEquals("YES", resolved);

		resolved = processor.process(text, null, Map.of("enabled", "false"));
		assertEquals("NO", resolved);
	}

	@Test
	void resolvesTruthy() {
		String text = "<if enabled>YES<else>NO</if>";
		String resolved = processor.process(text, null, Map.of("enabled", "value"));
		assertEquals("YES", resolved);
	}
}

