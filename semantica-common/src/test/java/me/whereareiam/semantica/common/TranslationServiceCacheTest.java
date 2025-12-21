package me.whereareiam.semantica.common;

import me.whereareiam.semantica.TranslationLocale;
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
import me.whereareiam.semantica.model.SemanticLocale;
import me.whereareiam.semantica.model.TagConfiguration;
import me.whereareiam.semantica.model.translation.LocalizedEntry;
import me.whereareiam.semantica.service.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationServiceCacheTest {
	private static class CountingProcessor implements TranslationProcessor {
		private final AtomicInteger count = new AtomicInteger();
		@Override
		public String process(String text, TranslationLocale locale, Map<String, Object> context) {
			count.incrementAndGet();
			return text;
		}
		@Override
		public int getPriority() { return 10; }
		@Override
		public String getName() { return "count"; }
	}

	@Test
	void staticMessagesAreCached() {
		TranslationRegistry registry = new DefaultTranslationRegistry();
		registry.register("static.msg", new LocalizedEntry(Map.of(
				SemanticLocale.wrap(Locale.US), "hello"
		)));

		CountingProcessor counter = new CountingProcessor();
		TranslationService<SemanticLocale> service = createService(registry, Locale.US, List.of(counter));

		service.resolve("static.msg", SemanticLocale.wrap(new Locale("en", "US")));
		service.resolve("static.msg", SemanticLocale.wrap(new Locale("en", "US")));

		assertEquals(1, counter.count.get(), "Processor should run once due to caching");
	}

	@Test
	void dynamicMessagesBypassCache() {
		TranslationRegistry registry = new DefaultTranslationRegistry();
		registry.register("dynamic.msg", new LocalizedEntry(Map.of(
				SemanticLocale.wrap(Locale.US), "hello <p:name>"
		)));

		CountingProcessor counter = new CountingProcessor();
		TranslationService<SemanticLocale> service = createService(registry, Locale.US, List.of(counter));

		service.resolve("dynamic.msg", SemanticLocale.wrap(new Locale("en", "US")), Map.of("name", "A"));
		service.resolve("dynamic.msg", SemanticLocale.wrap(new Locale("en", "US")), Map.of("name", "B"));

		assertEquals(2, counter.count.get(), "Processor should run each time for dynamic messages");
	}

	private TranslationService<SemanticLocale> createService(TranslationRegistry registry, Locale locale, List<TranslationProcessor> extraProcessors) {
		TagConfiguration cfg = TagConfiguration.defaults();
		TagTokens tags = new TagTokens(cfg);
		CacheStrategy cacheStrategy = new CacheStrategy(tags);
		TranslationCache cache = new TranslationCache(1024, 512, 30, 5);
		FallbackStrategy fallback = (key, loc) -> key;
		DependencyGraph graph = new DependencyGraph(tags);
		LocaleParser<SemanticLocale> parser = loc -> loc;

		List<TranslationProcessor> processors = new ArrayList<>(List.of(
				new TemplateProcessor(registry, tags),
				new MessageReferenceProcessor(registry, tags),
				new ConditionalProcessor(tags),
				new PlaceholderProcessor(tags)
		));
		processors.addAll(extraProcessors);

		TranslationLocale defaultLocale = SemanticLocale.wrap(locale);
		return new DefaultTranslationService<>(registry, defaultLocale, cache, cacheStrategy, processors, fallback, new DefaultFallbackResolver(), graph, parser, true);
	}
}

