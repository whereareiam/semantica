package me.whereareiam.semantica.common.check;

import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.issue.Check;
import me.whereareiam.semantica.issue.Issue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reports keys that a translation has but the default locale does not: usually a key that was renamed or
 * removed in the default locale and left behind in the translation. Messages of the root locale are no
 * translation and may have keys of their own.
 */
public final class UnknownKeyCheck implements Check {
	@Override
	public @NotNull List<Issue> check(@NotNull Catalog catalog) {
		Locale defaultLocale = catalog.getDefaultLocale();
		Set<String> known = catalog.keys(defaultLocale);

		List<Issue> issues = new ArrayList<>();
		for (Locale locale : catalog.getLocales()) {
			if (locale.equals(defaultLocale) || locale.equals(Locale.ROOT)) continue;

			for (Catalog.Entry entry : catalog.entries(locale).values()) {
				if (known.contains(entry.key())) continue;
				issues.add(new Issue(Issue.Kind.UNKNOWN_KEY, entry.key(), locale, entry.layer(), entry.origin(),
						"The default locale " + defaultLocale.toLanguageTag() + " has no such key"));
			}
		}

		return issues;
	}
}
