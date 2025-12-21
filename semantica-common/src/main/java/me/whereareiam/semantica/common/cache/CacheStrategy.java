package me.whereareiam.semantica.common.cache;

import me.whereareiam.semantica.common.TagTokens;
import me.whereareiam.semantica.type.CacheLevel;

/**
 * Determines which cache level to use based on text content.
 */
public class CacheStrategy {
	private final TagTokens tags;

	public CacheStrategy(TagTokens tags) {
		this.tags = tags;
	}

	public CacheLevel classify(String text) {
		if (text == null || text.isEmpty()) return CacheLevel.STATIC;

		if (contains(text, tags.placeholderTag)) return CacheLevel.DYNAMIC;
		if (contains(text, tags.conditionalIfTag)) return CacheLevel.SEMI_STATIC;
		return CacheLevel.STATIC;
	}

	private boolean contains(String text, String needle) {
		return text.contains(needle);
	}
}

