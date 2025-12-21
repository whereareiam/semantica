package me.whereareiam.semantica.common;

import me.whereareiam.semantica.common.util.TagTokens;
import me.whereareiam.semantica.model.TagConfiguration;
import me.whereareiam.semantica.model.translation.TranslationEntry;
import me.whereareiam.semantica.type.MessageType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DependencyGraphTest {
	@Test
	void buildsDependenciesAndDetectsCycles() {
		TagTokens tags = new TagTokens(TagConfiguration.defaults());
		DependencyGraph graph = new DependencyGraph(tags);

		TranslationEntry a = new TranslationEntry(MessageType.MESSAGE, "<m:b>");
		TranslationEntry b = new TranslationEntry(MessageType.MESSAGE, "<m:c>");
		TranslationEntry c = new TranslationEntry(MessageType.MESSAGE, "static");

		graph.build(Map.of(
				"a", a,
				"b", b,
				"c", c
		));

		assertEquals(Set.of("b"), graph.getDependencies("a"));
		assertEquals(Set.of("c"), graph.getDependencies("b"));
		assertTrue(graph.getResolutionOrder().containsAll(java.util.List.of("c", "b", "a")));
		assertFalse(graph.hasCircularDependency("a"));

		TranslationEntry d = new TranslationEntry(MessageType.MESSAGE, "<m:e>");
		TranslationEntry e = new TranslationEntry(MessageType.MESSAGE, "<m:d>");
		graph.build(Map.of("d", d, "e", e));
		assertTrue(graph.hasCircularDependency("d"));
	}
}

