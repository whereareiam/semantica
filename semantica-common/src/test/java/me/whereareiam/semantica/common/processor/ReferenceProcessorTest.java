package me.whereareiam.semantica.common.processor;

import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.common.TagTokens;
import me.whereareiam.semantica.model.RebuildMetrics;
import me.whereareiam.semantica.model.SemanticLocale;
import me.whereareiam.semantica.TagConfiguration;
import me.whereareiam.semantica.model.translation.entry.TemplateEntry;
import me.whereareiam.semantica.common.translation.DefaultTranslationRegistry;
import me.whereareiam.semantica.common.TranslationServiceBuilder;
import me.whereareiam.semantica.translation.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceProcessorTest {
	@Test
	void simpleReferenceWorks() {
		DefaultTranslationRegistry registry = new DefaultTranslationRegistry();
		registry.register("a", new TemplateEntry("<ref:b>"));
		registry.register("b", new TemplateEntry("value"));

		TranslationService<SemanticLocale> service = TranslationServiceBuilder.<SemanticLocale>create(
				registry,
				SemanticLocale.wrap(Locale.US),
				loc -> loc
		).build();

		// Build graph
		service.rebuild();

		// Resolve
		String result = service.resolve("a", SemanticLocale.wrap(Locale.US));
		assertEquals("value", result);
	}
	@Test
	void limitsDepthWithDefaultConfiguration() {
		TranslationRegistry registry = new DefaultTranslationRegistry();
		TagConfiguration config = TagConfiguration.defaults();
		TagTokens tags = new TagTokens(config);
		ReferenceProcessor processor = new ReferenceProcessor(registry, tags, config.getMaxReferenceDepth());

		// Create deeply nested references (12 levels)
		registry.register("l0", new TemplateEntry("<ref:l1>"));
		registry.register("l1", new TemplateEntry("<ref:l2>"));
		registry.register("l2", new TemplateEntry("<ref:l3>"));
		registry.register("l3", new TemplateEntry("<ref:l4>"));
		registry.register("l4", new TemplateEntry("<ref:l5>"));
		registry.register("l5", new TemplateEntry("<ref:l6>"));
		registry.register("l6", new TemplateEntry("<ref:l7>"));
		registry.register("l7", new TemplateEntry("<ref:l8>"));
		registry.register("l8", new TemplateEntry("<ref:l9>"));
		registry.register("l9", new TemplateEntry("<ref:l10>"));
		registry.register("l10", new TemplateEntry("<ref:l11>"));
		registry.register("l11", new TemplateEntry("final"));

		TranslationLocale locale = SemanticLocale.wrap(Locale.US);
		String result = processor.process("<ref:l0>", locale, Map.of());

		// Should stop before reaching "final" due to depth limit (default 10)
		assertNotEquals("final", result);
		assertTrue(result.contains("<ref:"));
	}

	@Test
	void unlimitedDepthResolvesUntilStable() {
		TranslationRegistry registry = new DefaultTranslationRegistry();
		TagConfiguration config = TagConfiguration.builder()
				.maxReferenceDepth(-1)
				.build();
		TagTokens tags = new TagTokens(config);
		ReferenceProcessor processor = new ReferenceProcessor(registry, tags, config.getMaxReferenceDepth());

		// Create deeply nested references (15 levels)
		registry.register("l0", new TemplateEntry("<ref:l1>"));
		registry.register("l1", new TemplateEntry("<ref:l2>"));
		registry.register("l2", new TemplateEntry("<ref:l3>"));
		registry.register("l3", new TemplateEntry("<ref:l4>"));
		registry.register("l4", new TemplateEntry("<ref:l5>"));
		registry.register("l5", new TemplateEntry("<ref:l6>"));
		registry.register("l6", new TemplateEntry("<ref:l7>"));
		registry.register("l7", new TemplateEntry("<ref:l8>"));
		registry.register("l8", new TemplateEntry("<ref:l9>"));
		registry.register("l9", new TemplateEntry("<ref:l10>"));
		registry.register("l10", new TemplateEntry("<ref:l11>"));
		registry.register("l11", new TemplateEntry("<ref:l12>"));
		registry.register("l12", new TemplateEntry("<ref:l13>"));
		registry.register("l13", new TemplateEntry("<ref:l14>"));
		registry.register("l14", new TemplateEntry("final"));

		TranslationLocale locale = SemanticLocale.wrap(Locale.US);
		String result = processor.process("<ref:l0>", locale, Map.of());

		// Should resolve completely regardless of depth
		assertEquals("final", result);
	}

	@Test
	void earlyExitWhenNothingChanges() {
		TranslationRegistry registry = new DefaultTranslationRegistry();
		TagConfiguration config = TagConfiguration.builder()
				.maxReferenceDepth(-1)
				.build();
		TagTokens tags = new TagTokens(config);
		ReferenceProcessor processor = new ReferenceProcessor(registry, tags, config.getMaxReferenceDepth());

		// Simple case - should exit after 1 pass
		registry.register("greeting", new TemplateEntry("Hello"));

		TranslationLocale locale = SemanticLocale.wrap(Locale.US);
		String result = processor.process("<ref:greeting>", locale, Map.of());

		assertEquals("Hello", result);
	}

	@Test
	void customDepthLimit() {
		TranslationRegistry registry = new DefaultTranslationRegistry();
		TagConfiguration config = TagConfiguration.builder()
				.maxReferenceDepth(3)
				.build();
		TagTokens tags = new TagTokens(config);
		ReferenceProcessor processor = new ReferenceProcessor(registry, tags, config.getMaxReferenceDepth());

		registry.register("l0", new TemplateEntry("<ref:l1>"));
		registry.register("l1", new TemplateEntry("<ref:l2>"));
		registry.register("l2", new TemplateEntry("<ref:l3>"));
		registry.register("l3", new TemplateEntry("<ref:l4>"));
		registry.register("l4", new TemplateEntry("final"));

		TranslationLocale locale = SemanticLocale.wrap(Locale.US);
		String result = processor.process("<ref:l0>", locale, Map.of());

		// Should stop at depth 3
		assertNotEquals("final", result);
		assertTrue(result.contains("<ref:"));
	}
}
