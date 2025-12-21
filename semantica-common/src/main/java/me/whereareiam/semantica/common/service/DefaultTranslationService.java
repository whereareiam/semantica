package me.whereareiam.semantica.common.service;

import lombok.Getter;
import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.common.DependencyGraph;
import me.whereareiam.semantica.common.cache.CacheKey;
import me.whereareiam.semantica.common.cache.CacheLevel;
import me.whereareiam.semantica.common.cache.CacheStrategy;
import me.whereareiam.semantica.common.cache.TranslationCache;
import me.whereareiam.semantica.model.translation.TranslationEntry;
import me.whereareiam.semantica.model.translation.TranslationRequest;
import me.whereareiam.semantica.service.FallbackResolver;
import me.whereareiam.semantica.service.FallbackStrategy;
import me.whereareiam.semantica.service.LocaleParser;
import me.whereareiam.semantica.service.TranslationProcessor;
import me.whereareiam.semantica.service.TranslationRegistry;
import me.whereareiam.semantica.service.TranslationService;

import java.util.*;

/**
 * Default implementation: cache + processing pipeline.
 *
 * @param <L> the locale type used by this service
 */
public class DefaultTranslationService<L> implements TranslationService<L> {
	private final TranslationRegistry registry;
	private final TranslationLocale defaultLocale;
	private final TranslationCache cache;
	private final CacheStrategy cacheStrategy;
	private final List<TranslationProcessor> processors;
	private final FallbackStrategy fallbackStrategy;
	private final FallbackResolver fallbackResolver;
	private final boolean cacheEnabled;
	@Getter
	private final DependencyGraph dependencyGraph;
	private final LocaleParser<L> localeParser;

	public DefaultTranslationService(
			TranslationRegistry registry,
			TranslationLocale defaultLocale,
			TranslationCache cache,
			CacheStrategy cacheStrategy,
			List<TranslationProcessor> processors,
			FallbackStrategy fallbackStrategy,
			FallbackResolver fallbackResolver,
			DependencyGraph dependencyGraph,
			LocaleParser<L> localeParser,
			boolean cacheEnabled
	) {
		this.registry = registry;
		this.defaultLocale = defaultLocale;
		this.cache = cache;
		this.cacheStrategy = cacheStrategy;
		this.processors = new ArrayList<>(processors);
		this.fallbackStrategy = fallbackStrategy;
		this.fallbackResolver = fallbackResolver;
		this.dependencyGraph = dependencyGraph;
		this.localeParser = localeParser;
		this.cacheEnabled = cacheEnabled;

		this.processors.sort(Comparator.comparingInt(TranslationProcessor::getPriority));
	}

	/**
	 * Pre-render all STATIC entries into cache (optional warm-up).
	 *
	 * @return number of entries pre-rendered
	 */
	public int preRenderStatic() {
		if (!cacheEnabled) return 0;

		int preRendered = 0;
		var entries = registry.getAllEntries();
		for (Map.Entry<String, TranslationEntry> e : entries.entrySet()) {
			String key = e.getKey();
			TranslationEntry entry = e.getValue();

			Set<TranslationLocale> locales = entry.getLocales();
			if (locales.isEmpty()) {
				// Template entry - pre-render with default locale
				String text = entry.resolve(defaultLocale, fallbackResolver, defaultLocale, key);
				if (text != null) {
					CacheLevel level = cacheStrategy.classify(text);
					if (level == CacheLevel.STATIC) {
						cache.put(new CacheKey(key, defaultLocale, Map.of()), text, CacheLevel.STATIC);
						preRendered++;
					}
				}
				continue;
			}

			// Localized entry - pre-render for each locale
			for (TranslationLocale locale : locales) {
				String text = entry.resolve(locale, fallbackResolver, defaultLocale, key);
				if (text != null) {
					CacheLevel level = cacheStrategy.classify(text);
					if (level == CacheLevel.STATIC) {
						cache.put(new CacheKey(key, locale, Map.of()), processPipeline(text, locale, Map.of()), CacheLevel.STATIC);
						preRendered++;
					}
				}
			}
		}
		return preRendered;
	}

	@Override
	public String resolve(TranslationRequest<L> request) {
		TranslationLocale locale = localeParser.parse(request.getLocale());
		return resolveInternal(request.getKey(), locale, request.getPlaceholders());
	}

	@Override
	public String resolve(String key, L locale, Map<String, Object> placeholders) {
		TranslationLocale translationLocale = localeParser.parse(locale);
		return resolveInternal(key, translationLocale, placeholders);
	}

	@Override
	public String resolve(String key, L locale) {
		return resolve(key, locale, Map.of());
	}

	private String resolveInternal(String key, TranslationLocale locale, Map<String, Object> placeholders) {
		TranslationEntry entry = registry.get(key);
		if (entry == null) {
			return fallbackStrategy.getFallback(key, locale);
		}

		String text = entry.resolve(locale, fallbackResolver, defaultLocale, key);
		if (text == null) return fallbackStrategy.getFallback(key, locale);

		// Check if caching is enabled
		if (!cacheEnabled) {
			return processPipeline(text, locale, placeholders);
		}

		CacheLevel level = cacheStrategy.classify(text);
		if (level != CacheLevel.DYNAMIC) {
			CacheKey cacheKey = new CacheKey(key, locale, placeholders);
			String cached = cache.get(cacheKey, level);
			if (cached != null) return cached;
			String resolved = processPipeline(text, locale, placeholders);
			cache.put(cacheKey, resolved, level);
			return resolved;
		}

		return processPipeline(text, locale, placeholders);
	}

	private String processPipeline(String text, TranslationLocale locale, Map<String, Object> placeholders) {
		String current = text;
		for (TranslationProcessor processor : processors) {
			current = processor.process(current, locale, placeholders);
		}
		return current;
	}

	@Override
	public boolean exists(String key) {
		return registry.exists(key);
	}

	@Override
	public Set<TranslationLocale> getAvailableLocales(String key) {
		TranslationEntry entry = registry.get(key);
		return entry != null ? entry.getLocales() : Set.of();
	}

	@Override
	public void reload() {
		cache.clear();
	}

	@Override
	public void register(String key, TranslationEntry entry) {
		registry.register(key, entry);
		cache.invalidate(key);
	}

	@Override
	public void unregister(String key) {
		registry.unregister(key);
		cache.invalidate(key);
	}

	@Override
	public Set<String> getKeys() {
		return registry.getKeys();
	}

	@Override
	public Set<String> getKeysByPrefix(String prefix) {
		return registry.getKeysByPrefix(prefix);
	}
}

