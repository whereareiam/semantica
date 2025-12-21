package me.whereareiam.semantica.translation.base;

import me.whereareiam.semantica.model.translation.TranslationSource;

import java.util.Map;

/**
 * Provides translations from any source.
 * <p>
 * This is a simpler alternative to TranslationLoader that returns all translations
 * at once rather than using a callback pattern. Users can implement this to load
 * translations from any format (files, database, HTTP, etc.).
 * <p>
 * The provider returns a nested map structure: locale -> key -> TranslationSource
 *
 * @param <L> the locale type
 */
public interface TranslationProvider<L> {
	/**
	 * Provide all translations for all locales.
	 *
	 * @return nested map: locale -> translation key -> TranslationSource object
	 */
	Map<L, Map<String, TranslationSource>> provideAll();
}