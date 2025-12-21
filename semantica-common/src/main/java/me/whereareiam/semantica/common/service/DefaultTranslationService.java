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
import me.whereareiam.semantica.service.FallbackStrategy;
import me.whereareiam.semantica.service.LocaleConverter;
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
	@Getter
	private final DependencyGraph dependencyGraph;
	private final LocaleConverter<L> localeConverter;

	public DefaultTranslationService(
			TranslationRegistry registry,
			TranslationLocale defaultLocale,
			TranslationCache cache,
			CacheStrategy cacheStrategy,
			List<TranslationProcessor> processors,
			FallbackStrategy fallbackStrategy,
			DependencyGraph dependencyGraph,
			LocaleConverter<L> localeConverter
	) {
		this.registry = registry;
		this.defaultLocale = defaultLocale;
		this.cache = cache;
		this.cacheStrategy = cacheStrategy;
		this.processors = new ArrayList<>(processors);
		this.fallbackStrategy = fallbackStrategy;
		this.dependencyGraph = dependencyGraph;
		this.localeConverter = localeConverter;

		this.processors.sort(Comparator.comparingInt(TranslationProcessor::getPriority));
	}

	/**
	 * Pre-render all STATIC entries into cache (optional warm-up).
	 */
	public void preRenderStatic() {
		var entries = registry.getAllEntries();
		for (Map.Entry<String, TranslationEntry> e : entries.entrySet()) {
			String key = e.getKey();
			TranslationEntry entry = e.getValue();

			String sampleText = entry.hasTranslations()
					? entry.getText(defaultLocale)
					: entry.getSingleText();
			if (sampleText == null) continue;

			CacheLevel level = cacheStrategy.classify(sampleText);
			if (level != CacheLevel.STATIC) continue;

			// Pre-render for available locales or default
			if (entry.hasTranslations()) {
				for (TranslationLocale locale : entry.getLocales()) {
					String resolved = resolveInternal(key, locale, Map.of());
					if (resolved != null) {
						cache.put(new CacheKey(key, locale, Map.of()), resolved, CacheLevel.STATIC);
					}
				}
			} else {
				String resolved = resolveInternal(key, defaultLocale, Map.of());
				if (resolved != null) {
					cache.put(new CacheKey(key, defaultLocale, Map.of()), resolved, CacheLevel.STATIC);
				}
			}
		}
	}

	@Override
	public String resolve(TranslationRequest request) {
		TranslationLocale locale = request.getLocale();
		return resolveInternal(request.getKey(), locale, request.getPlaceholders());
	}

	@Override
	public String resolve(String key, L locale, Map<String, Object> placeholders) {
		TranslationLocale translationLocale = localeConverter.convert(locale);
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

		String text = entry.hasTranslations()
				? entry.getText(locale, defaultLocale, key)
				: entry.getSingleText();

		if (text == null) return fallbackStrategy.getFallback(key, locale);

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
	public TranslationRegistry getRegistry() {
		return registry;
	}
}

