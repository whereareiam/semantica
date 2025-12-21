package me.whereareiam.semantica.translation;

import me.whereareiam.semantica.model.translation.entry.TranslationEntry;

import java.util.Map;
import java.util.Set;

/**
 * Registry for translation entries.
 * <p>
 * Thread-safe implementation is expected on the consumer side; Semantica uses it for lookups and runtime changes.
 */
@SuppressWarnings("unused")
public interface TranslationRegistry {
	/**
	 * @param key translation key
	 * @return true if an entry exists for the key.
	 */
	boolean exists(String key);

	/**
	 * Register or replace an entry under the given key.
	 *
	 * @param key   translation key
	 * @param entry compiled entry
	 */
	void register(String key, TranslationEntry entry);

	/**
	 * Remove an entry by key (no-op if missing).
	 *
	 * @param key translation key
	 */
	void unregister(String key);

	/**
	 * Fetch an entry by key.
	 *
	 * @param key translation key
	 * @return entry or null
	 */
	TranslationEntry get(String key);

	/**
	 * @return all registered keys.
	 */
	Set<String> getKeys();

	/**
	 * @param prefix key prefix
	 * @return keys starting with the provided prefix.
	 */
	Set<String> getKeys(String prefix);

	/**
	 * @return immutable view of all entries.
	 */
	Map<String, TranslationEntry> getAllEntries();

	/**
	 * Remove all entries.
	 */
	void clear();
}