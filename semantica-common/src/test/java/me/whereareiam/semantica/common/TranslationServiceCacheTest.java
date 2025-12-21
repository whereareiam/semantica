package me.whereareiam.semantica.common;

import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.common.translation.DefaultTranslationRegistry;
import me.whereareiam.semantica.model.SemanticLocale;
import me.whereareiam.semantica.model.translation.entry.LocalizedEntry;
import me.whereareiam.semantica.translation.base.TranslationProcessor;
import me.whereareiam.semantica.translation.TranslationRegistry;
import me.whereareiam.semantica.translation.TranslationService;
import org.junit.jupiter.api.Test;

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
		TranslationServiceBuilder<SemanticLocale> builder = TranslationServiceBuilder.<SemanticLocale>create(
				registry,
				SemanticLocale.wrap(locale),
				loc -> loc
		);

		for (TranslationProcessor processor : extraProcessors) {
			builder.addProcessor(processor);
		}

		return builder.build();
	}
}

