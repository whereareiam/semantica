package me.whereareiam.semantica.common.dependency;

import me.whereareiam.semantica.common.TagTokens;
import me.whereareiam.semantica.model.SemanticLocale;
import me.whereareiam.semantica.TagConfiguration;
import me.whereareiam.semantica.model.translation.entry.LocalizedEntry;
import me.whereareiam.semantica.model.translation.entry.TranslationEntry;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DependencyGraphTest {
	@Test
	void buildsDependenciesAndDetectsCycles() {
		TagTokens tags = new TagTokens(TagConfiguration.defaults());
		DependencyGraph graph = new DependencyGraph(tags);

		TranslationEntry a = new LocalizedEntry(Map.of(
				SemanticLocale.wrap(Locale.US), "<ref:b>"
		));
		TranslationEntry b = new LocalizedEntry(Map.of(
				SemanticLocale.wrap(Locale.US), "<ref:c>"
		));
		TranslationEntry c = new LocalizedEntry(Map.of(
				SemanticLocale.wrap(Locale.US), "static"
		));

		graph.build(Map.of(
				"a", a,
				"b", b,
				"c", c
		));

		assertEquals(Set.of("b"), graph.getDependencies("a"));
		assertEquals(Set.of("c"), graph.getDependencies("b"));
		assertTrue(graph.getResolutionOrder().containsAll(java.util.List.of("c", "b", "a")));
		assertFalse(graph.hasCircularDependency("a"));

		TranslationEntry d = new LocalizedEntry(Map.of(
				SemanticLocale.wrap(Locale.US), "<ref:e>"
		));
		TranslationEntry e = new LocalizedEntry(Map.of(
				SemanticLocale.wrap(Locale.US), "<ref:d>"
		));
		graph.build(Map.of("d", d, "e", e));
		assertTrue(graph.hasCircularDependency("d"));
	}
}

