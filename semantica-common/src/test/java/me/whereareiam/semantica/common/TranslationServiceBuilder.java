package me.whereareiam.semantica.common;

import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.common.cache.CacheManager;
import me.whereareiam.semantica.common.cache.CacheStrategy;
import me.whereareiam.semantica.common.cache.TranslationCache;
import me.whereareiam.semantica.common.dependency.DependencyGraph;
import me.whereareiam.semantica.common.dependency.DependencyResolver;
import me.whereareiam.semantica.common.cache.CacheWarmer;
import me.whereareiam.semantica.common.processor.ConditionalProcessor;
import me.whereareiam.semantica.common.processor.PlaceholderProcessor;
import me.whereareiam.semantica.common.processor.ReferenceProcessor;
import me.whereareiam.semantica.common.translation.DefaultTranslationService;
import me.whereareiam.semantica.TagConfiguration;
import me.whereareiam.semantica.locale.LocaleParser;
import me.whereareiam.semantica.translation.base.TranslationProcessor;
import me.whereareiam.semantica.translation.TranslationRegistry;
import me.whereareiam.semantica.translation.TranslationService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Test utility for building TranslationService instances.
 * Simple API without depending on bootstrap module.
 */
@SuppressWarnings("unused")
public class TranslationServiceBuilder<L> {
	private final TranslationRegistry registry;
	private final TranslationLocale defaultLocale;
	private final LocaleParser<L> localeParser;
	private TagConfiguration tagConfiguration = TagConfiguration.defaults();
	private final List<TranslationProcessor> customProcessors = new ArrayList<>();
	private boolean cacheEnabled = true;

	private TranslationServiceBuilder(TranslationRegistry registry, TranslationLocale defaultLocale, LocaleParser<L> localeParser) {
		this.registry = registry;
		this.defaultLocale = defaultLocale;
		this.localeParser = localeParser;
	}

	public static <L> TranslationServiceBuilder<L> create(
			TranslationRegistry registry,
			TranslationLocale defaultLocale,
			LocaleParser<L> localeParser
	) {
		return new TranslationServiceBuilder<>(registry, defaultLocale, localeParser);
	}

	public TranslationServiceBuilder<L> tagConfiguration(TagConfiguration config) {
		this.tagConfiguration = config;
		return this;
	}

	public TranslationServiceBuilder<L> addProcessor(TranslationProcessor processor) {
		this.customProcessors.add(processor);
		return this;
	}

	public TranslationServiceBuilder<L> cacheEnabled(boolean enabled) {
		this.cacheEnabled = enabled;
		return this;
	}

	public TranslationService<L> build() {
		// Create infrastructure
		TagTokens tags = new TagTokens(tagConfiguration);
		CacheStrategy cacheStrategy = new CacheStrategy(tags);
		TranslationCache cache = new TranslationCache(1024, 512, 30, 5);
		CacheManager cacheManager = new CacheManager(cache);

		// Create dependency graph manager
		DependencyGraph dependencyGraph = new DependencyGraph(tags);
		DependencyResolver graphManager = new DependencyResolver(dependencyGraph);

		// Create processors
		List<TranslationProcessor> allProcessors = new ArrayList<>();
		allProcessors.add(new ReferenceProcessor(registry, tags, tagConfiguration.getMaxReferenceDepth()));
		allProcessors.add(new ConditionalProcessor(tags));
		allProcessors.add(new PlaceholderProcessor(tags));
		allProcessors.addAll(customProcessors);
		allProcessors.sort(Comparator.comparingInt(TranslationProcessor::getPriority));

		// Create pre-rendering service
		CacheWarmer cacheWarmer = new CacheWarmer(
				cache,
				cacheStrategy,
				allProcessors,
				new DefaultLocaleResolver(),
				defaultLocale
		);

		// Create service
		return new DefaultTranslationService<>(
				registry,
				defaultLocale,
				cacheManager,
				cacheStrategy,
				cacheWarmer,
				graphManager,
				allProcessors,
				(key, locale) -> key,
				localeParser,
				cacheEnabled
		);
	}
}
