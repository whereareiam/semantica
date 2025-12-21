package me.whereareiam.semantica.model.translation;

import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.service.FallbackResolver;

import java.util.Set;

/**
 * Compiled translation entry ready for resolution.
 * <p>
 * Sealed interface with two implementations:
 * <ul>
 *   <li>{@link TemplateEntry} - Non-localized template text</li>
 *   <li>{@link LocalizedEntry} - Localized messages with multiple translations</li>
 * </ul>
 */
public sealed interface TranslationEntry permits TemplateEntry, LocalizedEntry {
	/**
	 * Get available locales for this entry.
	 *
	 * @return set of locales (empty for templates)
	 */
	Set<TranslationLocale> getLocales();

	/**
	 * Resolve text for the given locale.
	 *
	 * @param locale          requested locale
	 * @param fallbackResolver resolver for fallback logic
	 * @param defaultLocale   default locale
	 * @param key             translation key (for fallback)
	 * @return resolved text
	 */
	String resolve(
			TranslationLocale locale,
			FallbackResolver fallbackResolver,
			TranslationLocale defaultLocale,
			String key
	);
}
