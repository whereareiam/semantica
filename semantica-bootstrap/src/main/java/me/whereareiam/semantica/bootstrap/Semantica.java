package me.whereareiam.semantica.bootstrap;

import me.whereareiam.semantica.SemanticaLogger;
import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.common.DependencyGraph;
import me.whereareiam.semantica.common.cache.CacheStrategy;
import me.whereareiam.semantica.common.cache.TranslationCache;
import me.whereareiam.semantica.common.processor.ConditionalProcessor;
import me.whereareiam.semantica.common.processor.MessageReferenceProcessor;
import me.whereareiam.semantica.common.processor.PlaceholderProcessor;
import me.whereareiam.semantica.common.processor.TemplateProcessor;
import me.whereareiam.semantica.common.registry.DefaultTranslationRegistry;
import me.whereareiam.semantica.common.service.DefaultFallbackResolver;
import me.whereareiam.semantica.common.service.DefaultTranslationService;
import me.whereareiam.semantica.common.util.TagTokens;
import me.whereareiam.semantica.model.TagConfiguration;
import me.whereareiam.semantica.model.TranslationSource;
import me.whereareiam.semantica.model.translation.LocalizedEntry;
import me.whereareiam.semantica.service.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

		TagConfiguration tagConfig = config.getTagConfiguration();
		TagTokens tags = new TagTokens(tagConfig);

		SemanticaLogger logger = config.getLogger();
		FallbackStrategy fallback = config.getFallbackStrategy();
		FallbackResolver fallbackResolver = new DefaultFallbackResolver();

		CacheStrategy cacheStrategy = new CacheStrategy(tags);
		TranslationCache cache = new TranslationCache(
				config.getPerformance().getCache().getSemiStaticSize(),
				config.getPerformance().getCache().getDynamicSize(),
				config.getPerformance().getCache().getSemiStaticExpireMinutes(),
				config.getPerformance().getCache().getDynamicExpireMinutes()
		);

		DependencyGraph dependencyGraph = new DependencyGraph(tags);

		List<TranslationProcessor> processors = new ArrayList<>();
		processors.add(new TemplateProcessor(registry, tags));
		processors.add(new MessageReferenceProcessor(registry, tags));
		processors.add(new ConditionalProcessor(tags));
		processors.add(new PlaceholderProcessor(tags));
		processors.addAll(config.getCustomProcessors());

		DefaultTranslationService<L> service = new DefaultTranslationService<>(
				registry,
				config.getDefaultLocale(),
				cache,
				cacheStrategy,
				processors,
				fallback,
				fallbackResolver,
				dependencyGraph,
				localeParser,
				config.getPerformance().getCache().isEnabled()
		);

		if (registry instanceof DefaultTranslationRegistry dtr)
			dtr.setOnClear(cache::clear);

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
		boolean shouldBuildGraph = config.getPerformance().isBuildDependencyGraph();
		boolean shouldPrerender = config.getPerformance().isPrerenderStatic() && config.getPerformance().getCache().isEnabled();

		if (shouldBuildGraph || shouldPrerender) {
			long startTime = config.getPerformance().isLogTimings() ? System.currentTimeMillis() : 0;

			if (shouldBuildGraph) {
				dependencyGraph.build(registry.getAllEntries());
				
				// Log circular dependencies
				for (String key : registry.getKeys()) {
					if (dependencyGraph.hasCircularDependency(key)) {
						logger.warn("Circular dependency detected for key: " + key);
					}
				}
			}

			if (shouldPrerender) {
				int preRendered = service.preRenderStatic();
				if (config.getPerformance().isLogTimings()) {
					logger.info("Pre-rendered " + preRendered + " static translations");
				}
			}

			if (config.getPerformance().isLogTimings()) {
				long duration = System.currentTimeMillis() - startTime;
				logger.info("Finished compilation in " + duration + " ms");
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

