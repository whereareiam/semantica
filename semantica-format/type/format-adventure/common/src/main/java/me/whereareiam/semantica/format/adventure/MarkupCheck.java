package me.whereareiam.semantica.format.adventure;

import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.issue.Check;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.message.ParsedMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the MiniMessage tags of translations: each tag must be one the server's MiniMessage knows and the
 * policy allows, and a translation must use the same tags as the message of the default locale.
 *
 * <p>The Adventure adapters create it with their own tags, through their renderer's {@code check()}.</p>
 */
public final class MarkupCheck implements Check {
	private static final Pattern TAG = Pattern.compile("<(/?)([!?#]?[a-zA-Z][a-zA-Z0-9_\\-]*|#[0-9a-fA-F]{3,8})((?::[^<>]*)?)/?>");

	private final TagPolicy policy;
	private final MiniMessageTags tags;
	private final Predicate<String> keys;

	/**
	 * Creates a check.
	 *
	 * @param policy the allowed groups
	 * @param tags   the tags the server's MiniMessage knows
	 * @param keys   which keys hold MiniMessage
	 */
	public MarkupCheck(@NotNull TagPolicy policy, @NotNull MiniMessageTags tags, @NotNull Predicate<String> keys) {
		this.policy = Objects.requireNonNull(policy, "policy");
		this.tags = Objects.requireNonNull(tags, "tags");
		this.keys = Objects.requireNonNull(keys, "keys");
	}

	@Override
	public @NotNull List<Issue> check(@NotNull Catalog catalog) {
		Locale defaultLocale = catalog.getDefaultLocale();
		List<Issue> issues = new ArrayList<>();

		for (Locale locale : catalog.getLocales()) {
			for (Catalog.Entry entry : catalog.entries(locale).values()) {
				if (!keys.test(entry.key())) continue;

				Set<String> used = tags(entry.message());
				for (String tag : used) {
					TagPolicy.Group group = tags.group(tag);
					if (group != null && policy.allows(group)) continue;

					String detail = group != null
							? "The tag <" + tag + "> is not allowed here and is shown as written"
							: "<" + tag + "> is no MiniMessage tag and is shown as written; write \\<" + tag + "> to show it on purpose";
					issues.add(issue(entry, detail));
				}

				if (locale.equals(defaultLocale) || locale.equals(Locale.ROOT)) continue;
				Optional<Catalog.Entry> original = catalog.entry(defaultLocale, entry.key());
				if (original.isEmpty()) continue;

				Set<String> expected = tags(original.get().message());
				if (expected.equals(used)) continue;

				Set<String> missing = new TreeSet<>(expected);
				missing.removeAll(used);
				Set<String> extra = new TreeSet<>(used);
				extra.removeAll(expected);
				issues.add(issue(entry, "Uses other tags than " + defaultLocale.toLanguageTag()
						+ (missing.isEmpty() ? "" : "; lacks " + missing)
						+ (extra.isEmpty() ? "" : "; adds " + extra)));
			}
		}

		return issues;
	}

	private static Set<String> tags(ParsedMessage message) {
		Set<String> tags = new TreeSet<>();
		collect(message, tags);

		return tags;
	}

	private static void collect(ParsedMessage message, Set<String> tags) {
		for (ParsedMessage.Node node : message.getNodes()) {
			switch (node) {
				case ParsedMessage.Literal literal -> {
					Matcher matcher = TAG.matcher(literal.text());
					while (matcher.find()) {
						if (matcher.start() > 0 && literal.text().charAt(matcher.start() - 1) == '\\') continue;
						tags.add(MiniMessageTags.normalize(matcher.group(2)));
					}
				}
				case ParsedMessage.Plural plural -> plural.branches().values().forEach(branch -> collect(branch, tags));
				case ParsedMessage.Select select -> select.branches().values().forEach(branch -> collect(branch, tags));
				default -> {
				}
			}
		}
	}

	private static Issue issue(Catalog.Entry entry, String detail) {
		return new Issue(Issue.Kind.MARKUP, entry.key(), entry.locale(), entry.layer(), entry.origin(), detail);
	}
}
