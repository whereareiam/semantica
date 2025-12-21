package me.whereareiam.semantica.common;

import me.whereareiam.semantica.common.cache.CacheLevel;
import me.whereareiam.semantica.common.cache.CacheStrategy;
import me.whereareiam.semantica.model.TagConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheStrategyTest {
	private final TagTokens tags = new TagTokens(TagConfiguration.defaults());
	private final CacheStrategy strategy = new CacheStrategy(tags);

	@Test
	void classifiesStatic() {
		assertEquals(CacheLevel.STATIC, strategy.classify("hello world"));
	}

	@Test
	void classifiesDynamicWhenPlaceholder() {
		assertEquals(CacheLevel.DYNAMIC, strategy.classify("hello <p:name>"));
	}

	@Test
	void classifiesSemiStaticWithConditional() {
		assertEquals(CacheLevel.SEMI_STATIC, strategy.classify("<if enabled==true>yes</if>"));
	}
}

