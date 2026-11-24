package me.whereareiam.semantica.common.catalog;

import lombok.RequiredArgsConstructor;
import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.exception.SyntaxException;
import me.whereareiam.semantica.issue.Check;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.locale.LocaleChain;
import me.whereareiam.semantica.message.ParsedMessage;
import me.whereareiam.semantica.source.Source;
import me.whereareiam.semantica.syntax.Syntax;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Builds catalogs from the content of layers: merges them per key and locale, parses each message once,
 * leaves out messages with errors so lookups fall back, and runs the checks.
 *
 * <p>Parsed messages are kept from one build to the next by their source text, so a rebuild after one layer
 * changed parses only what changed. The kept messages are exactly those of the last build.</p>
 */
@RequiredArgsConstructor
public final class CatalogBuilder {
	/**
	 * Reads the messages.
	 */
	private final Syntax syntax;

	/**
	 * The locale chain references are followed along.
	 */
	private final LocaleChain chain;

	/**
	 * How deep references may go.
	 */
	private final int maxDepth;

	private Map<String, Parse> parsed = Map.of();

	/**
	 * Builds a catalog.
	 *
	 * @param revision the revision of the new catalog
	 * @param layers   the content of each layer, the lowest priority first
	 * @param checks   the checks to run on the merged catalog
	 * @return the catalog with every issue found
	 */
	public synchronized @NotNull Catalog build(long revision, @NotNull List<Layered> layers, @NotNull List<Check> checks) {
		List<Issue> issues = new ArrayList<>();
		Map<String, Parse> used = new HashMap<>();

		Map<Locale, Map<String, Catalog.Entry>> entries = merge(layers, issues, used);
		parsed = used;
		new ReferenceAnalysis(chain, maxDepth).exclude(entries, issues);

		Catalog merged = new Catalog(revision, chain.getDefaultLocale(), entries, List.of());
		for (Check check : checks) issues.addAll(check.check(merged));

		return new Catalog(revision, chain.getDefaultLocale(), entries, issues);
	}

	private Map<Locale, Map<String, Catalog.Entry>> merge(List<Layered> layers, List<Issue> issues, Map<String, Parse> used) {
		Map<Locale, Map<String, List<Candidate>>> candidates = new LinkedHashMap<>();
		for (Layered layer : layers) {
			for (Issue issue : layer.content().getIssues())
				issues.add(issue.getLayer() != null ? issue : withLayer(issue, layer.name()));

			for (Locale locale : layer.content().getLocales()) {
				Map<String, List<Candidate>> byKey = candidates.computeIfAbsent(locale, ignored -> new LinkedHashMap<>());
				layer.content().entries(locale).forEach((key, entry) ->
						byKey.computeIfAbsent(key, ignored -> new ArrayList<>()).addFirst(new Candidate(layer.name(), entry)));
			}
		}

		Map<Locale, Map<String, Catalog.Entry>> entries = new LinkedHashMap<>();
		candidates.forEach((locale, byKey) -> {
			Map<String, Catalog.Entry> chosen = new LinkedHashMap<>();
			byKey.forEach((key, list) -> {
				for (Candidate candidate : list) {
					Parse result = parse(candidate.entry().text(), used);
					if (result instanceof Parse.Parsed(ParsedMessage message)) {
						chosen.put(key, new Catalog.Entry(key, locale, candidate.entry().text(), message,
								candidate.layer(), candidate.entry().origin()));
						break;
					}

					String error = ((Parse.Failed) result).error();
					issues.add(new Issue(Issue.Kind.SYNTAX, key, locale, candidate.layer(), candidate.entry().origin(), error));
				}
			});
			if (!chosen.isEmpty()) entries.put(locale, chosen);
		});

		return entries;
	}

	private Parse parse(String text, Map<String, Parse> used) {
		Parse result = used.get(text);
		if (result == null) result = parsed.get(text);
		if (result == null) result = parse(text);

		used.put(text, result);
		return result;
	}

	private Parse parse(String text) {
		try {
			return new Parse.Parsed(syntax.parse(text));
		} catch (SyntaxException exception) {
			return new Parse.Failed(exception.getMessage());
		}
	}

	private static Issue withLayer(Issue issue, String layer) {
		return new Issue(issue.getKind(), issue.getKey(), issue.getLocale(), layer, issue.getOrigin(), issue.getDetail());
	}

	private record Candidate(String layer, Source.Entry entry) {
	}

	/**
	 * A text as the syntax read it: its message, or why it is not valid.
	 */
	private sealed interface Parse {
		record Parsed(ParsedMessage message) implements Parse {
		}

		record Failed(String error) implements Parse {
		}
	}

	/**
	 * The content one layer gave.
	 *
	 * @param name    the layer's name, for issues
	 * @param content what its source read
	 */
	public record Layered(@NotNull String name, @NotNull Source.Content content) {
		public Layered {
			Objects.requireNonNull(name, "name");
			Objects.requireNonNull(content, "content");
		}
	}
}
