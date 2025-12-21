package me.whereareiam.semantica.common.cache;

import me.whereareiam.semantica.model.CacheKey;
import me.whereareiam.semantica.type.CacheLevel;

import java.util.Set;

/**
 * Manages cache operations and invalidation strategies.
 */
public class CacheManager {
	private final TranslationCache cache;

	public CacheManager(TranslationCache cache) {
		this.cache = cache;
	}

	/**
	 * Get from cache.
	 */
	public String get(CacheKey key, CacheLevel level) {
		return cache.get(key, level);
	}

	/**
	 * Put into cache.
	 */
	public void put(CacheKey key, String text, CacheLevel level) {
		cache.put(key, text, level);
	}

	/**
	 * Clear all caches.
	 */
	public void clearAll() {
		cache.clear();
	}

	/**
	 * Invalidate multiple keys at once.
	 */
	public void invalidateAll(Set<String> keys) {
		cache.invalidateAll(keys);
	}
}
