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
import me.whereareiam.semantica.common.service.DefaultTranslationService;
import me.whereareiam.semantica.common.util.TagTokens;
import me.whereareiam.semantica.model.TagConfiguration;
import me.whereareiam.semantica.model.Translation;
import me.whereareiam.semantica.model.translation.TranslationEntry;
import me.whereareiam.semantica.service.*;
import me.whereareiam.semantica.type.MessageType;

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
		TagConfiguration tagConfig = config.getTagConfiguration();
		TagTokens tags = new TagTokens(tagConfig);

		SemanticaLogger logger = config.getLogger();
		FallbackStrategy fallback = config.getFallbackStrategy();

		CacheStrategy cacheStrategy = new CacheStrategy(tags);
		TranslationCache cache = new TranslationCache(
				config.getCache().getSemiStaticCacheSize(),
				config.getCache().getDynamicCacheSize(),
				config.getCache().getSemiStaticExpireMinutes(),
				config.getCache().getDynamicExpireMinutes()
		);

		DependencyGraph dependencyGraph = new DependencyGraph(tags);

		List<TranslationProcessor> processors = new ArrayList<>();
		processors.add(new TemplateProcessor(registry, tags));
		processors.add(new MessageReferenceProcessor(registry, tags));
		processors.add(new ConditionalProcessor(tags));
		processors.add(new PlaceholderProcessor(tags));
		processors.addAll(config.getCustomProcessors());

		// Use provided converter or default identity converter
		LocaleConverter<L> localeConverter = config.getLocaleConverter();
		if (localeConverter == null) {
			localeConverter = createIdentityConverter();
		}

		DefaultTranslationService<L> service = new DefaultTranslationService<>(
				registry,
				config.getDefaultLocale(),
				cache,
				cacheStrategy,
				processors,
				fallback,
				dependencyGraph,
				localeConverter
		);

		if (registry instanceof DefaultTranslationRegistry dtr)
			dtr.setOnClear(cache::clear);

		// Load translations if provider is configured
		if (config.getTranslationProvider() != null)
			loadFromProvider(config.getTranslationProvider(), registry, localeConverter);

		if (config.getCache().isPreRenderStatic())
			service.preRenderStatic();

		return service;
	}

	private static <L> LocaleConverter<L> createIdentityConverter() {
		return locale -> {
			if (locale instanceof TranslationLocale tl)
				return tl;

			throw new IllegalArgumentException(
					"Locale type " + locale.getClass().getName() + " is not a TranslationLocale. " +
							"Provide a LocaleConverter in SemanticaConfiguration."
			);
		};
	}

	private static <L> void loadFromProvider(
			TranslationProvider<L> provider,
			TranslationRegistry registry,
			LocaleConverter<L> localeConverter
	) {
		Map<L, Map<String, Translation>> allTranslations = provider.provideAll();
		
		// Group translations by key
		Map<String, Map<TranslationLocale, String>> grouped = new HashMap<>();
		
		allTranslations.forEach((locale, translations) -> {
			TranslationLocale translationLocale = localeConverter.convert(locale);
			translations.forEach((key, translation) -> {
				String text = translation.getText().asString();
				grouped.computeIfAbsent(key, k -> new HashMap<>())
						.put(translationLocale, text);
			});
		});
		
		// Register grouped entries
		grouped.forEach((key, localeMap) -> {
			TranslationEntry entry =
					new TranslationEntry(
							MessageType.MESSAGE,
							localeMap
					);
			registry.register(key, entry);
		});
	}

}

