package me.whereareiam.semantica.common.check;

import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.common.resolution.ValueFormatter;
import me.whereareiam.semantica.issue.Check;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.message.ParsedMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reports plurals that lack a form their language needs by CLDR's rules, such as {@code few} and {@code many}
 * in Russian. Such a plural still works, through {@code other}, but reads wrong for some numbers.
 */
public final class PluralFormCheck implements Check {
	@Override
	public @NotNull List<Issue> check(@NotNull Catalog catalog) {
		List<Issue> issues = new ArrayList<>();
		for (Locale locale : catalog.getLocales())
			for (Catalog.Entry entry : catalog.entries(locale).values())
				walk(entry, entry.message(), issues);

		return issues;
	}

	private static void walk(Catalog.Entry entry, ParsedMessage message, List<Issue> issues) {
		for (ParsedMessage.Node node : message.getNodes()) {
			switch (node) {
				case ParsedMessage.Plural plural -> {
					Set<String> missing = new TreeSet<>(ValueFormatter.pluralForms(entry.locale(), plural.ordinal()));
					missing.removeAll(plural.branches().keySet());
					if (!missing.isEmpty()) {
						issues.add(new Issue(Issue.Kind.MISSING_PLURAL_FORM, entry.key(), entry.locale(), entry.layer(), entry.origin(),
								"The plural '" + plural.name() + "' lacks " + String.join(", ", missing)));
					}
					plural.branches().values().forEach(branch -> walk(entry, branch, issues));
				}
				case ParsedMessage.Select select -> select.branches().values().forEach(branch -> walk(entry, branch, issues));
				default -> {
				}
			}
		}
	}
}
