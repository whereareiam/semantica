package me.whereareiam.semantica.model.translation.entry;

import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.locale.LocaleResolver;

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
	 * @param localeResolver resolver for fallback logic
	 * @param defaultLocale   default locale
	 * @param key             translation key (for fallback)
	 * @return resolved text
	 */
	String resolve(
			TranslationLocale locale,
			LocaleResolver localeResolver,
			TranslationLocale defaultLocale,
			String key
	);
}
