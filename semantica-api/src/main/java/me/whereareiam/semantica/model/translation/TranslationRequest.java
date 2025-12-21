package me.whereareiam.semantica.model.translation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import me.whereareiam.semantica.TranslationLocale;

import java.util.Map;

/**
 * Resolution request with key, locale, and placeholders.
 */
@Getter
@AllArgsConstructor
@Builder
public class TranslationRequest {
	private final String key;
	private final TranslationLocale locale;
	private final Map<String, Object> placeholders;
}

