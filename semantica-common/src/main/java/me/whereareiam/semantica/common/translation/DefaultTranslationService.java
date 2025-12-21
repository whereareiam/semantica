package me.whereareiam.semantica.common.translation;

import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.model.CacheKey;
import me.whereareiam.semantica.type.CacheLevel;
import me.whereareiam.semantica.common.cache.CacheManager;
import me.whereareiam.semantica.common.cache.CacheStrategy;
import me.whereareiam.semantica.common.dependency.DependencyResolver;
import me.whereareiam.semantica.common.cache.CacheWarmer;
import me.whereareiam.semantica.model.translation.entry.TranslationEntry;
import me.whereareiam.semantica.model.translation.TranslationRequest;
import me.whereareiam.semantica.FallbackStrategy;
import me.whereareiam.semantica.locale.LocaleParser;
import me.whereareiam.semantica.translation.base.TranslationProcessor;
import me.whereareiam.semantica.translation.TranslationRegistry;
import me.whereareiam.semantica.translation.TranslationService;
import me.whereareiam.semantica.model.RebuildMetrics;

import java.util.*;

/**
 * Orchestrator for translation resolution.
 * Delegates to specialized components for caching, pre-rendering, and dependency management.
 *
 * @param <L> the locale type used by this service
 */
public class DefaultTranslationService<L> implements TranslationService<L> {
	// Core components
	private final TranslationRegistry registry;
	private final LocaleParser<L> localeParser;
	private final TranslationLocale defaultLocale;
	private final FallbackStrategy fallbackStrategy;
	
	// Specialized managers
	private final CacheManager cacheManager;
	private final CacheWarmer cacheWarmer;
	private final DependencyResolver dependencyResolver;
	
	// Processing
	private final CacheStrategy cacheStrategy;
	private final List<TranslationProcessor> processors;
	private final boolean cacheEnabled;

	public DefaultTranslationService(
			TranslationRegistry registry,
			TranslationLocale defaultLocale,
			CacheManager cacheManager,
			CacheStrategy cacheStrategy,
			CacheWarmer cacheWarmer,
			DependencyResolver dependencyResolver,
			List<TranslationProcessor> processors,
			FallbackStrategy fallbackStrategy,
			LocaleParser<L> localeParser,
			boolean cacheEnabled
	) {
		this.registry = registry;
		this.defaultLocale = defaultLocale;
		this.cacheManager = cacheManager;
		this.cacheStrategy = cacheStrategy;
		this.cacheWarmer = cacheWarmer;
		this.dependencyResolver = dependencyResolver;
		this.processors = new ArrayList<>(processors);
		this.fallbackStrategy = fallbackStrategy;
		this.localeParser = localeParser;
		this.cacheEnabled = cacheEnabled;

		this.processors.sort(Comparator.comparingInt(TranslationProcessor::getPriority));
	}

	@Override
	public boolean exists(String key) {
		return registry.exists(key);
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

	@Override
	public void register(String key, TranslationEntry entry) {
		registry.register(key, entry);
		
		// Rebuild graph and invalidate affected entries
		dependencyResolver.build(registry.getAllEntries());
		Set<String> affected = dependencyResolver.getAffectedKeys(key);
		cacheManager.invalidateAll(affected);
	}

	@Override
	public void register(Map<String, TranslationEntry> entries) {
		if (entries == null || entries.isEmpty()) return;
		
		// Register all
		for (Map.Entry<String, TranslationEntry> e : entries.entrySet()) {
			registry.register(e.getKey(), e.getValue());
		}
		
		// Single graph rebuild
		dependencyResolver.build(registry.getAllEntries());
		
		// Invalidate all affected
		Set<String> affected = dependencyResolver.getAffectedKeys(entries.keySet());
		cacheManager.invalidateAll(affected);
	}

	@Override
	public void unregister(String key) {
		// Get dependents before removing
		Set<String> dependents = dependencyResolver.getDependents(key);
		
		registry.unregister(key);
		
		// Rebuild graph and invalidate
		dependencyResolver.build(registry.getAllEntries());
		
		Set<String> affected = new HashSet<>();
		affected.add(key);
		affected.addAll(dependents);
		cacheManager.invalidateAll(affected);
	}

	@Override
	public int unregisterByPrefix(String prefix) {
		Set<String> keysToRemove = registry.getKeys(prefix);
		if (keysToRemove.isEmpty()) return 0;
		
		// Get all affected before removal
		Set<String> affected = dependencyResolver.getAffectedKeys(keysToRemove);
		
		// Remove all
		for (String key : keysToRemove) {
			registry.unregister(key);
		}
		
		// Rebuild graph and invalidate
		dependencyResolver.build(registry.getAllEntries());
		cacheManager.invalidateAll(affected);
		
		return keysToRemove.size();
	}

	@Override
	public Set<String> getKeys() {
		return registry.getKeys();
	}

	@Override
	public Set<String> getKeys(String prefix) {
		return registry.getKeys(prefix);
	}

	@Override
	public Set<TranslationLocale> getAvailableLocales(String key) {
		TranslationEntry entry = registry.get(key);
		return entry != null ? entry.getLocales() : Set.of();
	}

	@Override
	public void clearCache() {
		cacheManager.clearAll();
	}

	@Override
	public RebuildMetrics rebuild() {
		long start = System.currentTimeMillis();

		// Clear cache
		cacheManager.clearAll();

		// Rebuild dependency graph
		var entries = registry.getAllEntries();
		var graphStats = dependencyResolver.build(entries);

		// Pre-render static entries
		int preRendered = 0;
		if (cacheEnabled) {
			preRendered = cacheWarmer.preRenderAll(entries);
		}

		long duration = System.currentTimeMillis() - start;
		return new RebuildMetrics(
				graphStats.totalKeys(),
				preRendered,
				graphStats.circularDependencies(),
				duration
		);
	}

	private String resolveInternal(String key, TranslationLocale locale, Map<String, Object> placeholders) {
		// Get entry from registry
		TranslationEntry entry = registry.get(key);
		if (entry == null) {
			return fallbackStrategy.getFallback(key, locale);
		}

		// Resolve raw text with fallback support
		String text = entry.resolve(locale, (translations, loc, defLoc, k) -> {
			// Try fallback locale if needed
			if (translations.isEmpty()) return k;
			String result = translations.get(loc);

			if (result != null) return result;
			if (defLoc != null && translations.containsKey(defLoc))
				return translations.get(defLoc);

			return k;
		}, defaultLocale, key);

		if (text == null) {
			return fallbackStrategy.getFallback(key, locale);
		}

		// Check cache if enabled
		if (!cacheEnabled) {
			return processPipeline(text, locale, placeholders);
		}

		CacheLevel level = cacheStrategy.classify(text);
		if (level != CacheLevel.DYNAMIC) {
			CacheKey cacheKey = new CacheKey(key, locale, placeholders);
			String cached = cacheManager.get(cacheKey, level);
			if (cached != null) return cached;

			String resolved = processPipeline(text, locale, placeholders);
			cacheManager.put(cacheKey, resolved, level);

			return resolved;
		}

		return processPipeline(text, locale, placeholders);
	}

	private String processPipeline(String text, TranslationLocale locale, Map<String, Object> placeholders) {
		String current = text;
		for (TranslationProcessor processor : processors)
			current = processor.process(current, locale, placeholders);

		return current;
	}
}

