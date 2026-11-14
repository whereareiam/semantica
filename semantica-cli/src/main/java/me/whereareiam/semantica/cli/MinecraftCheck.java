package me.whereareiam.semantica.cli;

import lombok.RequiredArgsConstructor;
import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.issue.Check;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.message.ParsedMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks texts that a launcher writes into Minecraft's {@code lang/<locale>.json}, where they are shown as written
 * and the client fills only the specifiers its {@code TranslatableContents} knows: a text holds no ICU syntax, each
 * {@code %} starts {@code %s}, {@code %1$s}, {@code %2$s} and so on, or {@code %%}, and a translation fills the same
 * specifiers as the message of the default locale.
 *
 * <p>A bundle carries a message as written, and so does the launcher, so the check reads what was written rather
 * than what ICU parsed: an apostrophe such as {@code Don't} reads alike in both, while {@code ''}, a quoted
 * {@code '{'} or any brace would differ.</p>
 */
@RequiredArgsConstructor
final class MinecraftCheck implements Check {
	private static final Pattern PERCENT = Pattern.compile("%(?:([1-9][0-9]{0,8})\\$)?(s)|%%|%[-#+0(<.0-9$]{0,8}[a-zA-Z]?");

	private final Predicate<String> keys;

	@Override
	public @NotNull List<Issue> check(@NotNull Catalog catalog) {
		List<Issue> issues = new ArrayList<>();

		for (Locale locale : catalog.getLocales()) {
			for (Catalog.Entry entry : catalog.entries(locale).values()) {
				if (!keys.test(entry.key())) continue;

				icu(entry).ifPresent(detail -> issues.add(issue(entry, detail)));
				Percents percents = Percents.of(entry.source());
				for (String unknown : new LinkedHashSet<>(percents.unknown())) {
					issues.add(issue(entry, unknown + " is no format specifier Minecraft fills; use %s, %1$s, %2$s"
							+ " and so on, or %% for a percent sign"));
				}

				if (locale.equals(catalog.getDefaultLocale()) || locale.equals(Locale.ROOT)) continue;
				catalog.entry(catalog.getDefaultLocale(), entry.key())
						.flatMap(original -> compare(Percents.of(original.source()), percents, catalog.getDefaultLocale()))
						.ifPresent(detail -> issues.add(issue(entry, detail)));
			}
		}

		return issues;
	}

	private static Optional<String> icu(Catalog.Entry entry) {
		String source = entry.source();
		if (source.indexOf('{') >= 0 || source.indexOf('}') >= 0)
			return Optional.of("Minecraft shows { and } as written, so these texts take no arguments, plurals, selects"
					+ " or references; write %s for a value");
		if (!source.equals(text(entry.message())))
			return Optional.of("ICU reads the apostrophes here as quoting, while Minecraft shows them as written;"
					+ " write a single ' instead of '' or quoted text");

		return Optional.empty();
	}

	private static String text(ParsedMessage message) {
		StringBuilder text = new StringBuilder();
		for (ParsedMessage.Node node : message.getNodes()) {
			if (node instanceof ParsedMessage.Literal literal) text.append(literal.text());
		}

		return text.toString();
	}

	private static Optional<String> compare(Percents expected, Percents used, Locale defaultLocale) {
		if (expected.specifiers().equals(used.specifiers())) return Optional.empty();

		List<String> missing = new ArrayList<>(expected.specifiers());
		used.specifiers().forEach(missing::remove);
		List<String> extra = new ArrayList<>(used.specifiers());
		expected.specifiers().forEach(extra::remove);

		return Optional.of("Uses other format specifiers than " + defaultLocale.toLanguageTag()
				+ (missing.isEmpty() ? "" : "; lacks " + missing)
				+ (extra.isEmpty() ? "" : "; adds " + extra));
	}

	private static Issue issue(Catalog.Entry entry, String detail) {
		return new Issue(Issue.Kind.MARKUP, entry.key(), entry.locale(), entry.layer(), entry.origin(), detail);
	}

	/**
	 * The {@code %} sequences of a text.
	 *
	 * @param specifiers the specifiers, each with the position of the value it takes ({@code %s %s} as
	 *                   {@code %1$s} and {@code %2$s}), sorted
	 * @param unknown    the sequences that are no specifier Minecraft fills, in order
	 */
	private record Percents(List<String> specifiers, List<String> unknown) {
		static Percents of(String text) {
			List<String> specifiers = new ArrayList<>();
			List<String> unknown = new ArrayList<>();
			int ordinary = 0;

			Matcher matcher = PERCENT.matcher(text);
			while (matcher.find()) {
				if (matcher.group().equals("%%")) continue;
				if (matcher.group(2) == null) {
					unknown.add(matcher.group());
					continue;
				}

				int position = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : ++ordinary;
				specifiers.add("%" + position + "$" + matcher.group(2));
			}
			specifiers.sort(null);

			return new Percents(specifiers, unknown);
		}
	}
}
