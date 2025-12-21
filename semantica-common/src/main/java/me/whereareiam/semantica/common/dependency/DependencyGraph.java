package me.whereareiam.semantica.common.dependency;

import lombok.Getter;
import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.common.TagTokens;
import me.whereareiam.semantica.model.translation.entry.TranslationEntry;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds dependency graph between translation references.
 */
public class DependencyGraph {
	private final Pattern referencePattern;

	private final Map<String, Set<String>> dependencies = new HashMap<>();
	@Getter
	private List<String> resolutionOrder = List.of();

	public DependencyGraph(TagTokens tags) {
		this.referencePattern = Pattern.compile("<" + Pattern.quote(tags.referenceTag.substring(1)) + "([a-zA-Z0-9_.\\-]+)");
	}

	public void build(Map<String, TranslationEntry> entries) {
		dependencies.clear();
		for (Map.Entry<String, TranslationEntry> entry : entries.entrySet()) {
			String key = entry.getKey();
			String text = anyText(entry.getValue());
			dependencies.put(key, extract(text));
		}
		calcResolutionOrder();
	}

	public Set<String> getDependencies(String key) {
		return dependencies.getOrDefault(key, Set.of());
	}

	public boolean hasCircularDependency(String key) {
		return detectCycle(key, new HashSet<>());
	}

	/**
	 * Get all keys that depend on the given key (reverse dependencies).
	 * Useful for cache invalidation when a key is modified/removed.
	 *
	 * @param key the key to find dependents for
	 * @return set of keys that reference this key
	 */
	public Set<String> getDependents(String key) {
		Set<String> dependents = new HashSet<>();
		for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
			if (entry.getValue().contains(key)) {
				dependents.add(entry.getKey());
				// Recursively add transitive dependents
				dependents.addAll(getDependents(entry.getKey()));
			}
		}
		return dependents;
	}

	private Set<String> extract(String text) {
		Set<String> deps = new HashSet<>();

		Matcher m = referencePattern.matcher(text);
		while (m.find()) deps.add(m.group(1));

		return deps;
	}

	private String anyText(TranslationEntry entry) {
		Set<TranslationLocale> locales = entry.getLocales();
		
		if (locales.isEmpty()) {
			// Template entry - resolve without locale
			String text = entry.resolve(null, (translations, locale, defaultLocale, key) -> "", null, "");
			return text != null && !text.isEmpty() ? text : "";
		}
		
		// Localized entry - get first available translation  
		var locale = locales.iterator().next();
		String text = entry.resolve(locale, (translations, l, d, k) -> {
			// Directly get from map
			return translations.get(locale);
		}, null, "");

		return text != null ? text : "";
	}

	private boolean detectCycle(String key, Set<String> path) {
		if (path.contains(key)) return true;
		Set<String> deps = dependencies.get(key);

		if (deps == null) return false;
		path.add(key);

		for (String dep : deps)
			if (detectCycle(dep, path)) return true;

		path.remove(key);
		return false;
	}

	private void calcResolutionOrder() {
		List<String> order = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		Set<String> visiting = new HashSet<>();
		for (String key : dependencies.keySet())
			if (!visited.contains(key)) topo(key, visited, visiting, order);

		resolutionOrder = order;
	}

	private void topo(String key, Set<String> visited, Set<String> visiting, List<String> order) {
		if (visiting.contains(key) || visited.contains(key)) return;
		visiting.add(key);

		Set<String> deps = dependencies.get(key);
		if (deps != null)
			for (String dep : deps)
				topo(dep, visited, visiting, order);

		visiting.remove(key);
		visited.add(key);
		order.add(key);
	}
}

