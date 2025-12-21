package me.whereareiam.semantica;

import lombok.Builder;
import lombok.Singular;
import me.whereareiam.semantica.common.dependency.DependencyGraph;
import me.whereareiam.semantica.common.TagTokens;
import me.whereareiam.semantica.common.cache.CacheManager;
import me.whereareiam.semantica.common.cache.CacheStrategy;
import me.whereareiam.semantica.common.cache.TranslationCache;
import me.whereareiam.semantica.common.dependency.DependencyResolver;
import me.whereareiam.semantica.common.cache.CacheWarmer;
import me.whereareiam.semantica.common.processor.ConditionalProcessor;
import me.whereareiam.semantica.common.processor.PlaceholderProcessor;
import me.whereareiam.semantica.common.processor.ReferenceProcessor;
import me.whereareiam.semantica.common.DefaultLocaleResolver;
import me.whereareiam.semantica.common.translation.DefaultTranslationService;
import me.whereareiam.semantica.locale.LocaleParser;
import me.whereareiam.semantica.locale.LocaleResolver;
import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.translation.base.TranslationProcessor;
import me.whereareiam.semantica.translation.TranslationRegistry;
import me.whereareiam.semantica.translation.TranslationService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Factory for building TranslationService instances.
 * Uses Lombok's @Builder for configuration, then creates the service.
 *
 * <p>Usage:
 * <pre>{@code
 * TranslationService<MyLocale> service = TranslationServiceFactory.<MyLocale>configuration()
 *     .registry(registry)
 *     .defaultLocale(locale)
 *     .localeParser(parser)
 *     .processor(myCustomProcessor)  // optional, can call multiple times
 *     .cacheEnabled(true)
 *     .build()
 *     .build();  // first build() creates config, second build() creates service
 * }</pre>
 *
 * @param <L> the locale type
 */
@Builder(builderClassName = "Builder", builderMethodName = "configuration")
public class TranslationServiceFactory<L> {
	// Required
	private final TranslationRegistry registry;
	private final TranslationLocale defaultLocale;
	private final LocaleParser<L> localeParser;

	// Optional with defaults
	@lombok.Builder.Default
	private final FallbackStrategy fallbackStrategy = (key, locale) -> key;

	@lombok.Builder.Default
	private final LocaleResolver localeResolver = new DefaultLocaleResolver();

	@lombok.Builder.Default
	private final TagConfiguration tagConfiguration = TagConfiguration.defaults();
	
	@Singular("processor")
	private final List<TranslationProcessor> processors;

	// Optional - Performance
	@lombok.Builder.Default
	private final CacheSettings cacheSettings = CacheSettings.defaults();

	/**
	 * Cache configuration settings.
	 */
	public static class CacheSettings {
		private final boolean enabled;
		private final int semiStaticSize;
		private final int dynamicSize;
		private final long semiStaticExpireMinutes;
		private final long dynamicExpireMinutes;

		public CacheSettings(boolean enabled, int semiStaticSize, int dynamicSize, long semiStaticExpireMinutes, long dynamicExpireMinutes) {
			this.enabled = enabled;
			this.semiStaticSize = semiStaticSize;
			this.dynamicSize = dynamicSize;
			this.semiStaticExpireMinutes = semiStaticExpireMinutes;
			this.dynamicExpireMinutes = dynamicExpireMinutes;
		}

		public static CacheSettings defaults() {
			return new CacheSettings(true, 1024, 512, 30, 5);
		}

		public boolean enabled() { return enabled; }
		public int semiStaticSize() { return semiStaticSize; }
		public int dynamicSize() { return dynamicSize; }
		public long semiStaticExpireMinutes() { return semiStaticExpireMinutes; }
		public long dynamicExpireMinutes() { return dynamicExpireMinutes; }
	}

	/**
	 * Create a TranslationService from this configuration.
	 */
	public TranslationService<L> build() {
		validate();

		// Create infrastructure
		TagTokens tags = new TagTokens(tagConfiguration);
		CacheStrategy cacheStrategy = new CacheStrategy(tags);
		TranslationCache cache = new TranslationCache(
				cacheSettings.semiStaticSize(),
				cacheSettings.dynamicSize(),
				cacheSettings.semiStaticExpireMinutes(),
				cacheSettings.dynamicExpireMinutes()
		);
		CacheManager cacheManager = new CacheManager(cache);

		// Create dependency graph manager
		DependencyGraph dependencyGraph = new DependencyGraph(tags);
		DependencyResolver graphManager = new DependencyResolver(dependencyGraph);

		// Create processors (built-in + custom)
		List<TranslationProcessor> allProcessors = createProcessors(registry, tags, tagConfiguration);

		// Create pre-rendering service
		CacheWarmer cacheWarmer = new CacheWarmer(
				cache,
				cacheStrategy,
				allProcessors,
				localeResolver,
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
				fallbackStrategy,
				localeParser,
				cacheSettings.enabled()
		);
	}

	private List<TranslationProcessor> createProcessors(TranslationRegistry registry, TagTokens tags, TagConfiguration config) {
		List<TranslationProcessor> allProcessors = new ArrayList<>();

		// Add built-in processors
		allProcessors.add(new ReferenceProcessor(registry, tags, config.getMaxReferenceDepth()));
		allProcessors.add(new ConditionalProcessor(tags));
		allProcessors.add(new PlaceholderProcessor(tags));

		// Add custom processors
		if (processors != null) allProcessors.addAll(processors);

		// Sort by priority
		allProcessors.sort(Comparator.comparingInt(TranslationProcessor::getPriority));

		return allProcessors;
	}

	private void validate() {
		if (registry == null)
			throw new IllegalStateException("TranslationRegistry is required");

		if (defaultLocale == null)
			throw new IllegalStateException("Default locale is required");

		if (localeParser == null)
			throw new IllegalStateException("LocaleParser is required");
	}
}
