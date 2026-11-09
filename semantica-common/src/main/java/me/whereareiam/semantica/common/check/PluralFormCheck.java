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
 *
 * <p>A plural chosen only by exact values, such as {@code {count, plural, =1 {Build:} other {Builds:}}}, needs no
 * forms as long as none of its branches shows the number: neither {@code #} nor its argument, also not inside a
 * nested choice or passed on to a reference. Once a branch shows the number, every form is needed, since
 * {@code other} would show "3 сборок".</p>
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
					if (!missing.isEmpty() && needsForms(plural)) {
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

	private static boolean needsForms(ParsedMessage.Plural plural) {
		boolean exactOnly = plural.branches().keySet().stream().allMatch(selector -> selector.startsWith("=") || selector.equals("other"));
		if (!exactOnly) return true;

		return plural.branches().values().stream().anyMatch(branch -> showsNumber(branch, plural.name(), true));
	}

	/**
	 * Tells whether a message shows a plural's number: its argument anywhere, or {@code #} where it belongs to that
	 * plural, which is outside every plural nested in it.
	 */
	private static boolean showsNumber(ParsedMessage message, String name, boolean countIsOurs) {
		for (ParsedMessage.Node node : message.getNodes()) {
			boolean shows = switch (node) {
				case ParsedMessage.Literal ignored -> false;
				case ParsedMessage.Argument argument -> argument.name().equals(name) || (countIsOurs && argument.name().equals(ParsedMessage.COUNT));
				case ParsedMessage.Plural nested -> nested.name().equals(name)
						|| nested.branches().values().stream().anyMatch(branch -> showsNumber(branch, name, false));
				case ParsedMessage.Select select -> select.branches().values().stream().anyMatch(branch -> showsNumber(branch, name, countIsOurs));
				case ParsedMessage.Reference reference -> reference.renames().containsValue(name);
			};
			if (shows) return true;
		}

		return false;
	}
}
