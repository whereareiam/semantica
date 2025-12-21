package me.whereareiam.semantica.common.cache;

import me.whereareiam.semantica.locale.LocaleResolver;
import me.whereareiam.semantica.model.CacheKey;
import me.whereareiam.semantica.model.translation.entry.TranslationEntry;
import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.translation.base.TranslationProcessor;
import me.whereareiam.semantica.type.CacheLevel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles pre-rendering of static translations into cache for performance.
 */
public class CacheWarmer {
	private final TranslationCache cache;
	private final CacheStrategy cacheStrategy;
	private final List<TranslationProcessor> processors;
	private final LocaleResolver localeResolver;
	private final TranslationLocale defaultLocale;

	public CacheWarmer(
			TranslationCache cache,
			CacheStrategy cacheStrategy,
			List<TranslationProcessor> processors,
			LocaleResolver localeResolver,
			TranslationLocale defaultLocale
	) {
		this.cache = cache;
		this.cacheStrategy = cacheStrategy;
		this.processors = processors;
		this.localeResolver = localeResolver;
		this.defaultLocale = defaultLocale;
	}

	/**
	 * Pre-render all static entries into cache.
	 *
	 * @param entries all translation entries
	 * @return number of entries pre-rendered
	 */
	public int preRenderAll(Map<String, TranslationEntry> entries) {
		int preRendered = 0;

		for (Map.Entry<String, TranslationEntry> e : entries.entrySet()) {
			String key = e.getKey();
			TranslationEntry entry = e.getValue();

			Set<TranslationLocale> locales = entry.getLocales();
			if (locales.isEmpty()) {
				// Template entry - pre-render with default locale
				preRendered += preRenderEntry(key, entry, defaultLocale);
				continue;
			}

			// Localized entry - pre-render for each locale
			for (TranslationLocale locale : locales)
				preRendered += preRenderEntry(key, entry, locale);
		}

		return preRendered;
	}

	private int preRenderEntry(String key, TranslationEntry entry, TranslationLocale locale) {
		// Get raw text
		String rawText = entry.resolve(locale, localeResolver, defaultLocale, key);
		if (rawText == null) return 0;

		// Process through pipeline to resolve references, conditionals, etc.
		String processedText = processPipeline(rawText, locale, Map.of());

		// Classify based on PROCESSED text (after references are resolved)
		CacheLevel level = cacheStrategy.classify(processedText);

		if (level == CacheLevel.STATIC) {
			cache.put(new CacheKey(key, locale, Map.of()), processedText, CacheLevel.STATIC);
			return 1;
		}

		return 0;
	}

	private String processPipeline(String text, TranslationLocale locale, Map<String, Object> placeholders) {
		String current = text;
		for (TranslationProcessor processor : processors)
			current = processor.process(current, locale, placeholders);

		return current;
	}
}
