package me.whereareiam.semantica.common.dependency;

import lombok.RequiredArgsConstructor;
import me.whereareiam.semantica.model.translation.entry.TranslationEntry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages dependency graph building and querying.
 */
@RequiredArgsConstructor
public class DependencyResolver {
	private final DependencyGraph graph;

	/**
	 * Build/rebuild the dependency graph from entries.
	 *
	 * @param entries all translation entries
	 * @return statistics about the graph
	 */
	public GraphMetrics build(Map<String, TranslationEntry> entries) {
		graph.build(entries);

		int circularCount = 0;
		for (String key : entries.keySet())
			if (graph.hasCircularDependency(key))
				circularCount++;

		return new GraphMetrics(entries.size(), circularCount);
	}

	/**
	 * Get all keys that depend on the given key (reverse dependencies).
	 * Includes transitive dependents.
	 */
	public Set<String> getDependents(String key) {
		return graph.getDependents(key);
	}

	/**
	 * Get all keys affected by a change to the given key.
	 * This includes the key itself and all dependents.
	 */
	public Set<String> getAffectedKeys(String key) {
		Set<String> affected = new HashSet<>();
		affected.add(key);
		affected.addAll(getDependents(key));

		return affected;
	}

	/**
	 * Get all keys affected by changes to multiple keys.
	 */
	public Set<String> getAffectedKeys(Set<String> keys) {
		Set<String> affected = new HashSet<>(keys);
		for (String key : keys) {
			affected.addAll(getDependents(key));
		}

		return affected;
	}

	/**
	 * Statistics about the dependency graph.
	 */
	public record GraphMetrics(int totalKeys, int circularDependencies) {
	}
}
