package me.whereareiam.semantica;

import me.whereareiam.semantica.common.translation.DefaultTranslationRegistry;
import me.whereareiam.semantica.locale.LocaleParser;
import me.whereareiam.semantica.model.RebuildMetrics;
import me.whereareiam.semantica.model.translation.TranslationSource;
import me.whereareiam.semantica.model.translation.entry.LocalizedEntry;
import me.whereareiam.semantica.translation.*;
import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.translation.base.TranslationProcessor;
import me.whereareiam.semantica.translation.base.TranslationProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Bootstrap entrypoint for Semantica.
 */
@SuppressWarnings("unused")
public final class Semantica {
	public static <L> TranslationService<L> createService(SemanticaConfiguration<L> config) {
		TranslationRegistry registry = new DefaultTranslationRegistry();
		return createService(config, registry);
	}

	public static <L> TranslationService<L> createService(SemanticaConfiguration<L> config, TranslationRegistry registry) {
		LocaleParser<L> localeParser = config.getLocaleParser();
		if (localeParser == null) {
			throw new IllegalArgumentException("LocaleParser is required. Please provide one via SemanticaConfiguration.builder().localeParser(...)");
		}

		SemanticaLogger logger = config.getLogger();

		// Build service using Lombok-generated builder
		TranslationServiceFactory.Builder<L> factoryBuilder = TranslationServiceFactory.<L>configuration()
				.registry(registry)
				.defaultLocale(config.getDefaultLocale())
				.localeParser(localeParser)
				.fallbackStrategy(config.getFallbackStrategy())
				.tagConfiguration(config.getTagConfiguration())
				.cacheSettings(
						new TranslationServiceFactory.CacheSettings(
								config.getPerformance().getCache().isEnabled(),
								config.getPerformance().getCache().getSemiStaticSize(),
								config.getPerformance().getCache().getDynamicSize(),
								config.getPerformance().getCache().getSemiStaticExpireMinutes(),
								config.getPerformance().getCache().getDynamicExpireMinutes()
						)
				);

		// Add custom processors
		for (TranslationProcessor processor : config.getCustomProcessors())
			factoryBuilder.processor(processor);

		TranslationService<L> service = factoryBuilder.build().build();

		// Setup registry clear callback
		if (registry instanceof DefaultTranslationRegistry dtr)
			dtr.setOnClear(service::clearCache);

		// Load translations if provider is configured
		if (config.getTranslationProvider() != null) {
			long loadStart = config.getPerformance().isLogTimings() ? System.currentTimeMillis() : 0;
			
			loadFromProvider(config.getTranslationProvider(), registry, localeParser);
			
			if (config.getPerformance().isLogTimings()) {
				long loadDuration = System.currentTimeMillis() - loadStart;
				logger.info("Loaded translations in " + loadDuration + " ms");
			}
		}

		// Post-loading optimizations
		boolean shouldOptimize = config.getPerformance().isBuildDependencyGraph() || 
				(config.getPerformance().isPrerenderStatic() && config.getPerformance().getCache().isEnabled());

		if (shouldOptimize) {
			long startTime = config.getPerformance().isLogTimings() ? System.currentTimeMillis() : 0;

			// Rebuild handles both graph building and pre-rendering
			RebuildMetrics stats = service.rebuild();
			
			if (config.getPerformance().isLogTimings()) {
				logger.info("Compiled " + stats.getTotalKeys() + " translations in " + stats.getDurationMs() + " ms");
				logger.info("  - Pre-rendered: " + stats.getPrerenderedEntries());
				logger.info("  - Circular dependencies: " + stats.getCircularDependencies());
			}
		}

		return service;
	}

	private static <L> void loadFromProvider(
			TranslationProvider<L> provider,
			TranslationRegistry registry,
			LocaleParser<L> localeParser
	) {
		Map<L, Map<String, TranslationSource>> allTranslations = provider.provideAll();
		
		// Group translations by key
		Map<String, Map<TranslationLocale, TranslationSource>> grouped = new HashMap<>();
		
		allTranslations.forEach((locale, translations) -> {
			TranslationLocale translationLocale = localeParser.parse(locale);
			translations.forEach((key, source) -> {
				grouped.computeIfAbsent(key, k -> new HashMap<>())
						.put(translationLocale, source);
			});
		});
		
		// Register grouped entries
		grouped.forEach((key, localeMap) -> {
			// Create text map
			Map<TranslationLocale, String> textMap = new HashMap<>();
			localeMap.forEach((locale, source) -> {
				textMap.put(locale, source.getText().asString());
			});
			
			LocalizedEntry entry = new LocalizedEntry(textMap);
			registry.register(key, entry);
		});
	}

}

