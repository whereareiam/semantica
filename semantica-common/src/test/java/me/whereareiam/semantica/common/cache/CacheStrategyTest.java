package me.whereareiam.semantica.common.cache;

import me.whereareiam.semantica.common.TagTokens;
import me.whereareiam.semantica.TagConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheStrategyTest {
	private final TagTokens tags = new TagTokens(TagConfiguration.defaults());
	private final CacheStrategy strategy = new CacheStrategy(tags);

	@Test
	void classifiesStatic() {
		assertEquals(me.whereareiam.semantica.type.CacheLevel.STATIC, strategy.classify("hello world"));
	}

	@Test
	void classifiesDynamicWhenPlaceholder() {
		assertEquals(me.whereareiam.semantica.type.CacheLevel.DYNAMIC, strategy.classify("hello <p:name>"));
	}

	@Test
	void classifiesSemiStaticWithConditional() {
		assertEquals(me.whereareiam.semantica.type.CacheLevel.SEMI_STATIC, strategy.classify("<if enabled==true>yes</if>"));
	}
}

