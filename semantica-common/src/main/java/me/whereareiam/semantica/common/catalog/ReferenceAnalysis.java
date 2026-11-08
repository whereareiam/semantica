package me.whereareiam.semantica.common.catalog;

import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.locale.LocaleChain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Follows the references of every message along the locale chain of its locale, and leaves out messages whose
 * references are missing, lead into a cycle or go deeper than allowed.
 */
final class ReferenceAnalysis {
	private static final int CYCLE = -1;

	private final LocaleChain chain;
	private final int maxDepth;

	ReferenceAnalysis(LocaleChain chain, int maxDepth) {
		this.chain = chain;
		this.maxDepth = maxDepth;
	}

	void exclude(Map<Locale, Map<String, Catalog.Entry>> entries, List<Issue> issues) {
		Map<Locale, List<String>> excluded = new HashMap<>();

		for (Map.Entry<Locale, Map<String, Catalog.Entry>> locale : entries.entrySet()) {
			List<Locale> locales = chain.resolve(locale.getKey());
			Lookup lookup = key -> {
				for (Locale candidate : locales) {
					Catalog.Entry entry = entries.getOrDefault(candidate, Map.of()).get(key);
					if (entry != null) return entry;
				}
				return null;
			};

			Map<String, Integer> depths = new HashMap<>();
			for (Catalog.Entry entry : locale.getValue().values()) {
				Issue issue = check(entry, lookup, depths);
				if (issue == null) continue;

				issues.add(issue);
				excluded.computeIfAbsent(locale.getKey(), ignored -> new ArrayList<>()).add(entry.key());
			}
		}

		excluded.forEach((locale, keys) -> {
			Map<String, Catalog.Entry> byKey = entries.get(locale);
			keys.forEach(byKey::remove);
			if (byKey.isEmpty()) entries.remove(locale);
		});
	}

	private Issue check(Catalog.Entry entry, Lookup lookup, Map<String, Integer> depths) {
		Set<String> references = entry.message().references();
		if (references.isEmpty()) return null;

		for (String reference : references)
			if (lookup.find(reference) == null)
				return issue(entry, Issue.Kind.MISSING_REFERENCE, "References '" + reference + "', which no locale of its chain has");

		int depth = depth(entry.key(), lookup, depths, new LinkedHashSet<>());
		if (depth == CYCLE)
			return issue(entry, Issue.Kind.REFERENCE_CYCLE, "Its references lead back into a cycle");
		if (depth > maxDepth)
			return issue(entry, Issue.Kind.DEPTH_LIMIT, "Its references go " + depth + " deep, more than " + maxDepth);

		return null;
	}

	private int depth(String key, Lookup lookup, Map<String, Integer> depths, Set<String> path) {
		Integer known = depths.get(key);
		if (known != null) return known;
		if (!path.add(key)) return CYCLE;

		int depth = 0;
		Catalog.Entry entry = lookup.find(key);
		if (entry != null) {
			for (String reference : entry.message().references()) {
				if (lookup.find(reference) == null) continue;

				int below = depth(reference, lookup, depths, path);
				if (below == CYCLE) {
					depth = CYCLE;
					break;
				}
				depth = Math.max(depth, below + 1);
			}
		}

		path.remove(key);
		depths.put(key, depth);

		return depth;
	}

	private static Issue issue(Catalog.Entry entry, Issue.Kind kind, String detail) {
		return new Issue(kind, entry.key(), entry.locale(), entry.layer(), entry.origin(), detail);
	}

	@FunctionalInterface
	private interface Lookup {
		Catalog.Entry find(String key);
	}
}
