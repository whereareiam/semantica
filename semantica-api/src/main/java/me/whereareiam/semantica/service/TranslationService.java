package me.whereareiam.semantica.service;

import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.model.translation.TranslationEntry;
import me.whereareiam.semantica.model.translation.TranslationRequest;

import java.util.Map;
import java.util.Set;

/**
 * Main service for resolving translations.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Resolve text by key/locale (with placeholders)</li>
 *   <li>Expose locale availability per key</li>
 *   <li>Provide cache control via {@link #reload()}</li>
 *   <li>Allow runtime modifications via controlled methods</li>
 * </ul>
 *
 * @param <L> the locale type used by this service
 */
public interface TranslationService<L> {
	/**
	 * Resolve a translation using a request object (key, locale, placeholders).
	 *
	 * @param request translation request
	 * @return resolved text or fallback
	 */
	String resolve(TranslationRequest<L> request);

	/**
	 * Resolve a key for a locale with placeholders.
	 *
	 * @param key          translation key
	 * @param locale       locale instance
	 * @param placeholders placeholder values used by processors
	 * @return resolved text (or fallback)
	 */
	String resolve(String key, L locale, Map<String, Object> placeholders);

	/**
	 * Resolve a key for a locale without placeholders.
	 *
	 * @param key    translation key
	 * @param locale locale instance
	 * @return resolved text (or fallback)
	 */
	String resolve(String key, L locale);

	/**
	 * Check if the key exists in the registry.
	 *
	 * @param key translation key
	 * @return true if present
	 */
	boolean exists(String key);

	/**
	 * Get locales available for a given key.
	 *
	 * @param key translation key
	 * @return set of locales, empty if missing
	 */
	Set<TranslationLocale> getAvailableLocales(String key);

	/**
	 * Clear caches (does not reload entries).
	 */
	void reload();

	/**
	 * Register or replace an entry under the given key.
	 * <p>
	 * Automatically invalidates cache for the key.
	 *
	 * @param key   translation key
	 * @param entry compiled entry
	 */
	void register(String key, TranslationEntry entry);

	/**
	 * Remove an entry by key (no-op if missing).
	 * <p>
	 * Automatically invalidates cache for the key.
	 *
	 * @param key translation key
	 */
	void unregister(String key);

	/**
	 * Get all registered keys.
	 *
	 * @return set of all keys
	 */
	Set<String> getKeys();

	/**
	 * Get keys starting with the provided prefix.
	 *
	 * @param prefix key prefix
	 * @return keys starting with prefix
	 */
	Set<String> getKeysByPrefix(String prefix);
}