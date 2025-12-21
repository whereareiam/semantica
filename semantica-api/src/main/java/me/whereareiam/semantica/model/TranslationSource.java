package me.whereareiam.semantica.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Source data for a translation loaded from provider.
 * <p>
 * This represents raw translation data before compilation into entries.
 */
@Getter
@AllArgsConstructor
public class TranslationSource {
	private final TextValue text;
	private final Map<String, String> metadata;

	public TranslationSource(TextValue text) {
		this(text, Collections.emptyMap());
	}
}
