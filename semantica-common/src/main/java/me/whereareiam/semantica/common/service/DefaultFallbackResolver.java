package me.whereareiam.semantica.common.service;

import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.service.FallbackResolver;

import java.util.Map;

/**
 * Default implementation of fallback resolution logic.
 * <p>
 * Resolution order:
 * <ol>
 *   <li>Exact locale match (e.g., en_US)</li>
 *   <li>Language-only match (e.g., en_US → en)</li>
 *   <li>Default locale</li>
 *   <li>Translation key as last resort</li>
 * </ol>
 */
public class DefaultFallbackResolver implements FallbackResolver {
	@Override
	public String resolve(
			Map<TranslationLocale, String> translations,
			TranslationLocale requestedLocale,
			TranslationLocale defaultLocale,
			String key
	) {
		if (translations == null || translations.isEmpty())
			return key;

		// 1. Try exact match
		String text = translations.get(requestedLocale);
		if (text != null) return text;

		// 2. Try language-only fallback
		if (requestedLocale != null) {
			for (TranslationLocale available : translations.keySet()) {
				if (available != null && available.getLanguage().equals(requestedLocale.getLanguage())) {
					text = translations.get(available);
					if (text != null) return text;
				}
			}
		}

		// 3. Try default locale
		if (defaultLocale != null) {
			text = translations.get(defaultLocale);
			if (text != null) return text;
		}

		// 4. Return key as last resort
		return key;
	}
}