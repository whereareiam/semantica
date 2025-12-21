package me.whereareiam.semantica.translation;

import me.whereareiam.semantica.model.translation.entry.TranslationEntry;
import me.whereareiam.semantica.model.translation.TranslationRequest;
import me.whereareiam.semantica.model.RebuildMetrics;
import me.whereareiam.semantica.translation.base.TranslationLocale;

import java.util.Map;
import java.util.Set;

/**
 * Main service for resolving translations.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Resolve text by key/locale (with placeholders)</li>
 *   <li>Expose locale availability per key</li>
 *   <li>Provide cache control via {@link #rebuild()}</li>
 *   <li>Allow runtime modifications via controlled methods</li>
 * </ul>
 *
 * @param <L> the locale type used by this service
 */
@SuppressWarnings("unused")
public interface TranslationService<L> {
	/**
	 * Check if the key exists in the registry.
	 *
	 * @param key translation key
	 * @return true if present
	 */
	boolean exists(String key);

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
	 * Register or replace an entry under the given key.
	 * <p>
	 * Automatically invalidates cache for the key and dependent entries.
	 *
	 * @param key   translation key
	 * @param entry compiled entry
	 */
	void register(String key, TranslationEntry entry);

	/**
	 * Register multiple entries at once.
	 * More efficient than multiple single register() calls.
	 *
	 * @param entries map of keys to entries
	 */
	void register(Map<String, TranslationEntry> entries);

	/**
	 * Remove an entry by key (no-op if missing).
	 * <p>
	 * Automatically invalidates cache for the key and dependent entries.
	 *
	 * @param key translation key
	 */
	void unregister(String key);

	/**
	 * Remove all entries with keys starting with the given prefix.
	 * Useful for removing plugin translations at once.
	 *
	 * @param prefix key prefix (e.g., "plugin.music.")
	 * @return number of entries removed
	 */
	int unregisterByPrefix(String prefix);

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
	Set<String> getKeys(String prefix);

	/**
	 * Get locales available for a given key.
	 *
	 * @param key translation key
	 * @return set of locales, empty if missing
	 */
	Set<TranslationLocale> getAvailableLocales(String key);

	/**
	 * Clear all caches (does not reload entries or rebuild dependency graph).
	 * Use {@link #rebuild()} for full recompilation.
	 */
	void clearCache();

	/**
	 * Rebuild dependency graph and re-prerender static entries.
	 * Expensive operation - use sparingly.
	 *
	 * @return statistics about the rebuild (prerendered count, etc.)
	 */
	RebuildMetrics rebuild();
}