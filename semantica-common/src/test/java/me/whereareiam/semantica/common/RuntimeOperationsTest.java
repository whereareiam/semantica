package me.whereareiam.semantica.common;

import me.whereareiam.semantica.common.translation.DefaultTranslationRegistry;
import me.whereareiam.semantica.common.translation.DefaultTranslationService;
import me.whereareiam.semantica.model.RebuildMetrics;
import me.whereareiam.semantica.model.SemanticLocale;
import me.whereareiam.semantica.model.translation.entry.TemplateEntry;
import me.whereareiam.semantica.model.translation.entry.TranslationEntry;
import me.whereareiam.semantica.translation.TranslationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeOperationsTest {
	private DefaultTranslationService<SemanticLocale> service;
	private DefaultTranslationRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new DefaultTranslationRegistry();
		service = createService(registry, Locale.US);
	}

	@Test
	void registerInvalidatesDependents() {
		// Setup: A references B
		registry.register("a", new TemplateEntry("<ref:b>"));
		registry.register("b", new TemplateEntry("original"));
		
		// Don't call rebuild - test automatic graph updates
		SemanticLocale locale = SemanticLocale.wrap(Locale.US);

		// First register should auto-rebuild graph
		// Since we just created service, we need initial build
		service.rebuild();
		
		// Resolve and cache A
		String result1 = service.resolve("a", locale);
		assertEquals("original", result1);

		// Update B - should invalidate A's cache AND rebuild graph
		service.register("b", new TemplateEntry("updated"));

		// A should resolve with new value
		String result2 = service.resolve("a", locale);
		assertEquals("updated", result2);
	}

	@Test
	void unregisterInvalidatesDependents() {
		registry.register("template", new TemplateEntry("prefix <ref:value> suffix"));
		registry.register("value", new TemplateEntry("test"));
		service.rebuild();

		SemanticLocale locale = SemanticLocale.wrap(Locale.US);

		// Cache it
		String result1 = service.resolve("template", locale);
		assertEquals("prefix test suffix", result1);

		// Remove dependency
		service.unregister("value");

		// Should resolve with broken reference
		String result2 = service.resolve("template", locale);
		assertTrue(result2.contains("<ref:value>"));
	}

	@Test
	void unregisterByPrefixRemovesMultipleKeys() {
		registry.register("plugin.music.play", new TemplateEntry("Play"));
		registry.register("plugin.music.stop", new TemplateEntry("Stop"));
		registry.register("plugin.music.pause", new TemplateEntry("Pause"));
		registry.register("core.error", new TemplateEntry("Error"));
		service.rebuild();

		int removed = service.unregisterByPrefix("plugin.music.");

		assertEquals(3, removed);
		assertFalse(service.exists("plugin.music.play"));
		assertFalse(service.exists("plugin.music.stop"));
		assertFalse(service.exists("plugin.music.pause"));
		assertTrue(service.exists("core.error"));
	}

	@Test
	void unregisterByPrefixInvalidatesDependents() {
		registry.register("plugin.colors.red", new TemplateEntry("Red"));
		registry.register("core.message", new TemplateEntry("Color: <ref:plugin.colors.red>"));
		service.rebuild();

		SemanticLocale locale = SemanticLocale.wrap(Locale.US);

		// Cache it
		String result1 = service.resolve("core.message", locale);
		assertEquals("Color: Red", result1);

		// Remove plugin
		service.unregisterByPrefix("plugin.colors.");

		// Should invalidate dependent cache
		String result2 = service.resolve("core.message", locale);
		assertTrue(result2.contains("<ref:plugin.colors.red>"));
	}

	@Test
	void registerAllBulkOperation() {
		Map<String, TranslationEntry> entries = new HashMap<>();
		entries.put("a", new TemplateEntry("A"));
		entries.put("b", new TemplateEntry("B"));
		entries.put("c", new TemplateEntry("C"));

		service.register(entries);

		assertTrue(service.exists("a"));
		assertTrue(service.exists("b"));
		assertTrue(service.exists("c"));
	}

	@Test
	void rebuildRecomputesDependencyGraph() {
		// Start with no dependencies
		registry.register("a", new TemplateEntry("static"));
		
		// Initial rebuild
		RebuildMetrics stats1 = service.rebuild();
		assertEquals(1, stats1.getTotalKeys());

		// Update to have dependency
		registry.register("a", new TemplateEntry("<ref:b>"));
		registry.register("b", new TemplateEntry("dynamic"));

		// After rebuild - new graph should reflect changes
		RebuildMetrics stats2 = service.rebuild();

		// Verify by resolving - if graph is correct, reference will resolve
		String result = service.resolve("a", SemanticLocale.wrap(Locale.US));
		assertEquals("dynamic", result);
		assertEquals(2, stats2.getTotalKeys());
	}

	@Test
	void rebuildDetectsCircularDependencies() {
		registry.register("a", new TemplateEntry("<ref:b>"));
		registry.register("b", new TemplateEntry("<ref:a>"));

		RebuildMetrics stats = service.rebuild();

		assertTrue(stats.getCircularDependencies() > 0);
	}

	@Test
	void clearCacheDoesNotAffectRegistry() {
		registry.register("key", new TemplateEntry("value"));
		service.rebuild();

		SemanticLocale locale = SemanticLocale.wrap(Locale.US);
		service.resolve("key", locale); // Cache it

		service.clearCache();

		// Key should still exist
		assertTrue(service.exists("key"));
		// But cache is cleared, so re-resolves
		assertEquals("value", service.resolve("key", locale));
	}

	@Test
	void deepDependencyChainInvalidation() {
		// A -> B -> C -> D
		registry.register("a", new TemplateEntry("<ref:b>"));
		registry.register("b", new TemplateEntry("<ref:c>"));
		registry.register("c", new TemplateEntry("<ref:d>"));
		registry.register("d", new TemplateEntry("leaf"));
		service.rebuild();

		SemanticLocale locale = SemanticLocale.wrap(Locale.US);

		// Cache entire chain
		assertEquals("leaf", service.resolve("a", locale));

		// Update leaf
		service.register("d", new TemplateEntry("updated"));

		// All should reflect change
		assertEquals("updated", service.resolve("a", locale));
		assertEquals("updated", service.resolve("b", locale));
		assertEquals("updated", service.resolve("c", locale));
	}

	private DefaultTranslationService<SemanticLocale> createService(TranslationRegistry registry, Locale locale) {
		return (DefaultTranslationService<SemanticLocale>) TranslationServiceBuilder.<SemanticLocale>create(
				registry,
				SemanticLocale.wrap(locale),
				loc -> loc
		).build();
	}
}
