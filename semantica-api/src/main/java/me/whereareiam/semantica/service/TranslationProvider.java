package me.whereareiam.semantica.service;

import me.whereareiam.semantica.model.Translation;

import java.util.Map;

/**
 * Provides translations from any source.
 * <p>
 * This is a simpler alternative to TranslationLoader that returns all translations
 * at once rather than using a callback pattern. Users can implement this to load
 * translations from any format (files, database, HTTP, etc.).
 * <p>
 * The provider returns a nested map structure: locale -> key -> Translation
 *
 * @param <L> the locale type
 */
@FunctionalInterface
public interface TranslationProvider<L> {
	/**
	 * Provide all translations for all locales.
	 *
	 * @return nested map: locale -> translation key -> Translation object
	 */
	Map<L, Map<String, Translation>> provideAll();
}
