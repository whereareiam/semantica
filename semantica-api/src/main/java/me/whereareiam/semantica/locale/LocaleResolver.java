package me.whereareiam.semantica.locale;

import me.whereareiam.semantica.translation.base.TranslationLocale;

import java.util.Map;

/**
 * Resolves translation text with fallback logic for missing locales.
 * <p>
 * Extracted from TranslationEntry to separate concerns and improve testability.
 */
public interface LocaleResolver {
	/**
	 * Resolve text for the requested locale from available translations.
	 * <p>
	 * Implementation should handle:
	 * <ul>
	 *   <li>Exact locale match</li>
	 *   <li>Language-only fallback (e.g., en_US → en)</li>
	 *   <li>Default locale fallback</li>
	 *   <li>Key fallback as last resort</li>
	 * </ul>
	 *
	 * @param translations     available translations by locale
	 * @param requestedLocale  requested locale
	 * @param defaultLocale    default/fallback locale
	 * @param key              translation key (used as last resort)
	 * @return resolved text or key
	 */
	String resolve(
			Map<TranslationLocale, String> translations,
			TranslationLocale requestedLocale,
			TranslationLocale defaultLocale,
			String key
	);
}
