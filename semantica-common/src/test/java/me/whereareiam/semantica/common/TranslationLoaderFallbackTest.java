package me.whereareiam.semantica.common;

import me.whereareiam.semantica.SemanticaLogger;
import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.common.cache.CacheStrategy;
import me.whereareiam.semantica.common.cache.TranslationCache;
import me.whereareiam.semantica.common.loader.DefaultTranslationLoader;
import me.whereareiam.semantica.common.processor.ConditionalProcessor;
import me.whereareiam.semantica.common.processor.MessageReferenceProcessor;
import me.whereareiam.semantica.common.processor.PlaceholderProcessor;
import me.whereareiam.semantica.common.processor.TemplateProcessor;
import me.whereareiam.semantica.common.registry.DefaultTranslationRegistry;
import me.whereareiam.semantica.common.service.DefaultTranslationService;
import me.whereareiam.semantica.common.util.TagTokens;
import me.whereareiam.semantica.model.SemanticLocale;
import me.whereareiam.semantica.model.TagConfiguration;
import me.whereareiam.semantica.model.TextValue;
import me.whereareiam.semantica.model.document.TranslationDocument;
import me.whereareiam.semantica.model.document.TranslationDocumentEntry;
import me.whereareiam.semantica.service.*;
import me.whereareiam.semantica.type.MessageType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationLoaderFallbackTest {
	@Test
	void fallsBackToDefaultLocaleWhenMissing() {
		TranslationDocument doc = new TranslationDocument();
		doc.setType(MessageType.MESSAGE);

		TranslationDocumentEntry entry = new TranslationDocumentEntry();
		entry.setTranslations(Map.of(
				"en_US", new TextValue.StringValue("Hello"),
				"en_GB", new TextValue.StringValue("Hello mate")
		));
		doc.setItems(Map.of("greet", entry));

		TranslationRegistry registry = new DefaultTranslationRegistry();
		TranslationLoader loader = new DefaultTranslationLoader(registry);
		loader.load("", doc);

		TranslationService<SemanticLocale> service = createService(registry, Locale.US);

		String resolved = service.resolve("greet", SemanticLocale.wrap(new Locale("fr", "FR")));
		assertEquals("Hello", resolved);
	}

	private TranslationService<SemanticLocale> createService(TranslationRegistry registry, Locale locale) {
		TagConfiguration cfg = TagConfiguration.defaults();
		TagTokens tags = new TagTokens(cfg);
		CacheStrategy cacheStrategy = new CacheStrategy(tags);
		TranslationCache cache = new TranslationCache(1024, 512, 30, 5);
		FallbackStrategy fallback = (key, loc) -> key;
		DependencyGraph graph = new DependencyGraph(tags);
		LocaleConverter<SemanticLocale> converter = loc -> loc;

		List<TranslationProcessor> processors = List.of(
				new TemplateProcessor(registry, tags),
				new MessageReferenceProcessor(registry, tags),
				new ConditionalProcessor(tags),
				new PlaceholderProcessor(tags)
		);

		TranslationLocale defaultLocale = SemanticLocale.wrap(locale);
		return new DefaultTranslationService<>(registry, defaultLocale, cache, cacheStrategy, processors, fallback, graph, converter);
	}
}

