package me.whereareiam.semantica.common.check;

import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.issue.Check;
import me.whereareiam.semantica.issue.Issue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Reports translations whose arguments differ from the message of the default locale: an argument the default
 * locale does not have (it would show as {@code {name}}), or one the default locale always shows and the
 * translation never does. An argument used only in some branches of the default locale may be left out of a
 * translation, since languages need different words per form.
 */
public final class ArgumentCheck implements Check {
	@Override
	public @NotNull List<Issue> check(@NotNull Catalog catalog) {
		Locale defaultLocale = catalog.getDefaultLocale();

		List<Issue> issues = new ArrayList<>();
		for (Locale locale : catalog.getLocales()) {
			if (locale.equals(defaultLocale) || locale.equals(Locale.ROOT)) continue;

			for (Catalog.Entry entry : catalog.entries(locale).values()) {
				Optional<Catalog.Entry> original = catalog.entry(defaultLocale, entry.key());
				if (original.isEmpty()) continue;

				Set<String> unknown = new LinkedHashSet<>(entry.message().arguments());
				unknown.removeAll(original.get().message().arguments());

				Set<String> missing = new LinkedHashSet<>(original.get().message().requiredArguments());
				missing.removeAll(entry.message().arguments());

				if (!unknown.isEmpty())
					issues.add(issue(entry, "Uses " + names(unknown) + ", which the default locale does not"));
				if (!missing.isEmpty())
					issues.add(issue(entry, "Never shows " + names(missing) + ", which the default locale always shows"));
			}
		}

		return issues;
	}

	private static Issue issue(Catalog.Entry entry, String detail) {
		return new Issue(Issue.Kind.ARGUMENT_MISMATCH, entry.key(), entry.locale(), entry.layer(), entry.origin(), detail);
	}

	private static String names(Set<String> names) {
		return "{" + String.join("}, {", names) + "}";
	}
}
