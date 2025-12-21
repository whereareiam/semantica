package me.whereareiam.semantica.locale;

import me.whereareiam.semantica.translation.base.TranslationLocale;

/**
 * Converts platform-specific locale objects to TranslationLocale.
 * <p>
 * Similar to Cloud's ArgumentParser, this provides type-safe conversion
 * from any locale type to Semantica's internal representation.
 *
 * @param <L> the platform-specific locale type
 */
public interface LocaleParser<L> {
	/**
	 * Parse/convert a platform-specific locale to TranslationLocale.
	 *
	 * @param locale the platform locale
	 * @return parsed TranslationLocale
	 */
	TranslationLocale parse(L locale);
}