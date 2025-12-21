package me.whereareiam.semantica.common.cache;

import me.whereareiam.semantica.model.CacheKey;
import me.whereareiam.semantica.type.CacheLevel;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-level cache for resolved messages.
 */
public class TranslationCache {
	private final Map<me.whereareiam.semantica.model.CacheKey, String> staticCache = new ConcurrentHashMap<>();
	private final Map<me.whereareiam.semantica.model.CacheKey, CacheEntry> semiStaticCache = new ConcurrentHashMap<>();
	private final Map<me.whereareiam.semantica.model.CacheKey, CacheEntry> dynamicCache = new ConcurrentHashMap<>();

	private final int semiStaticMaxSize;
	private final int dynamicMaxSize;
	private final long semiStaticExpireMillis;
	private final long dynamicExpireMillis;

	public TranslationCache(
			int semiStaticMaxSize,
			int dynamicMaxSize,
			long semiStaticExpireMinutes,
			long dynamicExpireMinutes
	) {
		this.semiStaticMaxSize = semiStaticMaxSize;
		this.dynamicMaxSize = dynamicMaxSize;
		this.semiStaticExpireMillis = semiStaticExpireMinutes * 60_000L;
		this.dynamicExpireMillis = dynamicExpireMinutes * 60_000L;
	}

	public String get(CacheKey key, CacheLevel level) {
		return switch (level) {
			case STATIC -> staticCache.get(key);
			case SEMI_STATIC -> getFromTimed(semiStaticCache, key, semiStaticExpireMillis);
			case DYNAMIC -> getFromTimed(dynamicCache, key, dynamicExpireMillis);
		};
	}

	public void put(CacheKey key, String text, CacheLevel level) {
		switch (level) {
			case STATIC -> staticCache.put(key, text);
			case SEMI_STATIC -> putTimed(semiStaticCache, key, text, semiStaticMaxSize);
			case DYNAMIC -> putTimed(dynamicCache, key, text, dynamicMaxSize);
		}
	}

	public void clear() {
		staticCache.clear();
		semiStaticCache.clear();
		dynamicCache.clear();
	}

	public void invalidateAll(Set<String> keys) {
		if (keys == null || keys.isEmpty()) return;
		staticCache.keySet().removeIf(k -> keys.contains(k.getKey()));
		semiStaticCache.keySet().removeIf(k -> keys.contains(k.getKey()));
		dynamicCache.keySet().removeIf(k -> keys.contains(k.getKey()));
	}

	private String getFromTimed(Map<CacheKey, CacheEntry> cache, CacheKey key, long expireMillis) {
		CacheEntry entry = cache.get(key);
		if (entry == null) return null;
		if (System.currentTimeMillis() - entry.timestamp > expireMillis) {
			cache.remove(key);
			return null;
		}
		return entry.text;
	}

	private void putTimed(Map<CacheKey, CacheEntry> cache, CacheKey key, String text, int maxSize) {
		if (cache.size() >= maxSize) {
			int toRemove = Math.max(1, maxSize / 5);
			cache.keySet().stream()
					.limit(toRemove)
					.forEach(cache::remove);
		}

		cache.put(key, new CacheEntry(text, System.currentTimeMillis()));
	}

	private record CacheEntry(String text, long timestamp) {
	}
}

