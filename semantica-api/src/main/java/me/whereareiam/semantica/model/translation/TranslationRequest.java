package me.whereareiam.semantica.model.translation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Resolution request with key, locale, and placeholders.
 *
 * @param <L> the locale type
 */
@Getter
@AllArgsConstructor
@Builder
public class TranslationRequest<L> {
	private final String key;
	private final L locale;
	private final Map<String, Object> placeholders;
}