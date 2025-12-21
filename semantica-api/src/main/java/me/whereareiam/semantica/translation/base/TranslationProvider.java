package me.whereareiam.semantica.translation.base;

import me.whereareiam.semantica.model.ProviderResult;

/**
 * Provides translations from any source.
 * <p>
 * Users can implement this to load translations from any format (files, database, HTTP, etc.).
 * The provider returns both localized entries (varying by locale) and template entries (non-localized).
 *
 * @param <L> the locale type
 */
public interface TranslationProvider<L> {
	/**
	 * Provide all translation data including localized entries and templates.
	 *
	 * @return ProviderResult containing both localized and template entries
	 */
	ProviderResult<L> provide();
}