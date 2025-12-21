package me.whereareiam.semantica.service;

import me.whereareiam.semantica.TranslationLocale;

/**
 * Converts platform-specific locale objects to TranslationLocale.
 *
 * @param <L> the platform-specific locale type
 */
@FunctionalInterface
public interface LocaleConverter<L> {
    /**
     * Convert a platform-specific locale to TranslationLocale.
     *
     * @param locale the platform locale
     * @return wrapped TranslationLocale
     */
    TranslationLocale convert(L locale);
}
