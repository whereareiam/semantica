package me.whereareiam.semantica.common.resolution;

import me.whereareiam.semantica.Text;
import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.message.ParsedMessage;
import me.whereareiam.semantica.message.ParsedMessage.Argument;
import me.whereareiam.semantica.message.ParsedMessage.Literal;
import me.whereareiam.semantica.message.ParsedMessage.Plural;
import me.whereareiam.semantica.message.ParsedMessage.Reference;
import me.whereareiam.semantica.message.ParsedMessage.Select;
import me.whereareiam.semantica.message.ResolvedMessage;
import me.whereareiam.semantica.message.ResolvedMessage.Part;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Resolves a {@link Text} against a catalog: finds its message along the locale chain, chooses branches,
 * expands references and nested texts and formats values.
 *
 * <p>A message that goes deeper than the depth limit or grows past the size limit is given up on in that
 * locale, reported, and the next locale of the chain is tried; when none is left, the key is shown.</p>
 *
 * <p>A message of the root locale is the same in every language: its references and nested texts follow the
 * chain of the locale asked for, as every message's do, and its numbers, dates and plural forms follow the
 * locale asked for first.</p>
 */
public final class MessageResolver {
	private final ValueFormatter formatter;
	private final int maxDepth;
	private final int maxLength;

	/**
	 * Creates a resolver.
	 *
	 * @param formatter formats values and chooses plural forms
	 * @param maxDepth  how deep references and nested texts may go
	 * @param maxLength how many characters a resolved message may have
	 */
	public MessageResolver(@NotNull ValueFormatter formatter, int maxDepth, int maxLength) {
		this.formatter = formatter;
		this.maxDepth = maxDepth;
		this.maxLength = maxLength;
	}

	/**
	 * Resolves a text.
	 *
	 * @param catalog the catalog to take messages from
	 * @param text    the text
	 * @param chain   the locales to try, in order
	 * @param issues  receives what went wrong; the result is still usable
	 * @return the resolved message; when no locale has the key, a message holding the key
	 */
	public @NotNull ResolvedMessage resolve(
			@NotNull Catalog catalog,
			@NotNull Text text,
			@NotNull List<Locale> chain,
			@NotNull Consumer<Issue> issues
	) {
		boolean found = false;
		for (Locale locale : chain) {
			Optional<Catalog.Entry> entry = catalog.entry(locale, text.getKey());
			if (entry.isEmpty()) continue;

			found = true;
			Expansion expansion = new Expansion(catalog, chain, issues);
			try {
				expansion.message(entry.get(), text.getValues(), 0);
				return new ResolvedMessage(text.getKey(), locale, expansion.parts, true);
			} catch (LimitExceeded exceeded) {
				issues.accept(issue(exceeded.kind, text.getKey(), entry.get(), exceeded.getMessage()));
			}
		}

		if (!found) {
			issues.accept(new Issue(Issue.Kind.MISSING_KEY, text.getKey(), chain.getFirst(), null, null,
					"No locale of " + chain + " has this key"));
		}
		return new ResolvedMessage(text.getKey(), chain.getFirst(), List.of(new ResolvedMessage.Literal(text.getKey())), false);
	}

	private static Issue issue(Issue.Kind kind, String key, Catalog.Entry entry, String detail) {
		return new Issue(kind, key, entry.locale(), entry.layer(), entry.origin(), detail);
	}

	private static final class LimitExceeded extends RuntimeException {
		private final Issue.Kind kind;

		private LimitExceeded(Issue.Kind kind, String message) {
			super(message, null, false, false);
			this.kind = kind;
		}
	}

	/**
	 * One resolution: the parts written so far and their length.
	 */
	private final class Expansion {
		private final Catalog catalog;
		private final List<Locale> chain;
		private final Consumer<Issue> issues;
		private final List<Part> parts = new ArrayList<>();
		private final StringBuilder literal = new StringBuilder();
		private int length;

		private Expansion(Catalog catalog, List<Locale> chain, Consumer<Issue> issues) {
			this.catalog = catalog;
			this.chain = chain;
			this.issues = issues;
		}

		private void message(Catalog.Entry entry, Map<String, Object> values, int depth) {
			if (depth > maxDepth)
				throw new LimitExceeded(Issue.Kind.DEPTH_LIMIT, "References and nested texts go deeper than " + maxDepth);

			nodes(entry, entry.message(), values, null, depth);
			flush();
		}

		private void nodes(Catalog.Entry entry, ParsedMessage message, Map<String, Object> values, @Nullable Count count, int depth) {
			for (ParsedMessage.Node node : message.getNodes()) {
				switch (node) {
					case Literal text -> append(text.text());
					case Argument argument -> argument(entry, argument, values, count, depth);
					case Plural plural -> plural(entry, plural, values, depth);
					case Select select -> select(entry, select, values, count, depth);
					case Reference reference -> reference(entry, reference, values, depth);
				}
			}
		}

		private void argument(Catalog.Entry entry, Argument argument, Map<String, Object> values, @Nullable Count count, int depth) {
			boolean isCount = argument.name().equals(ParsedMessage.COUNT);
			Object value = isCount ? (count != null ? count.value() : null) : values.get(argument.name());
			if (value == null) {
				String name = isCount && count != null ? count.name() : argument.name();
				if (!isCount) report(Issue.Kind.MISSING_VALUE, entry, "No value for '" + name + "'");
				append("{" + name + "}");
				return;
			}

			if (value instanceof Text nested) {
				nested(entry, nested, depth);
				return;
			}

			String formatted;
			try {
				formatted = formatter.format(value, argument.format(), argument.style(), formatLocale(entry));
			} catch (RuntimeException exception) {
				report(Issue.Kind.INVALID_VALUE, entry, "Cannot format '" + argument.name() + "': " + exception.getMessage());
				formatted = String.valueOf(value);
			}

			flush();
			ResolvedMessage.Value part = new ResolvedMessage.Value(argument.name(), value, formatted, argument.format(), argument.style());
			grow(part.text().length());
			parts.add(part);
		}

		private void nested(Catalog.Entry entry, Text nested, int depth) {
			for (Locale locale : chain) {
				Optional<Catalog.Entry> target = catalog.entry(locale, nested.getKey());
				if (target.isEmpty()) continue;

				message(target.get(), nested.getValues(), depth + 1);
				return;
			}

			report(Issue.Kind.MISSING_KEY, entry, "No locale of " + chain + " has the nested text '" + nested.getKey() + "'");
			append(nested.getKey());
		}

		private void plural(Catalog.Entry entry, Plural plural, Map<String, Object> values, int depth) {
			Object value = values.get(plural.name());
			if (!(value instanceof Number number)) {
				report(value == null ? Issue.Kind.MISSING_VALUE : Issue.Kind.INVALID_VALUE, entry,
						"The plural '" + plural.name() + "' needs a number");
				nodes(entry, plural.branches().get("other"), values, new Count(plural.name(), value), depth);
				return;
			}

			ParsedMessage branch = plural.branches().get("=" + exact(number));
			Number count = minus(number, plural.offset());
			if (branch == null) {
				String form = formatter.pluralForm(formatLocale(entry), count.doubleValue(), plural.ordinal());
				branch = plural.branches().getOrDefault(form, plural.branches().get("other"));
			}

			nodes(entry, branch, values, new Count(plural.name(), count), depth);
		}

		private void select(Catalog.Entry entry, Select select, Map<String, Object> values, @Nullable Count count, int depth) {
			Object value = values.get(select.name());
			if (value == null) report(Issue.Kind.MISSING_VALUE, entry, "No value for '" + select.name() + "'");

			ParsedMessage branch = value == null ? null : select.branches().get(selector(value));
			if (branch == null && value instanceof Enum<?> constant)
				branch = select.branches().get(constant.name().toLowerCase(Locale.ROOT));

			nodes(entry, branch != null ? branch : select.branches().get("other"), values, count, depth);
		}

		private void reference(Catalog.Entry entry, Reference reference, Map<String, Object> values, int depth) {
			for (Locale locale : chain) {
				Optional<Catalog.Entry> target = catalog.entry(locale, reference.key());
				if (target.isEmpty()) continue;

				Map<String, Object> passed = new HashMap<>(values);
				reference.renames().forEach((name, from) -> {
					Object value = values.get(from);
					if (value != null) passed.put(name, value);
					else passed.remove(name);
				});

				if (depth + 1 > maxDepth)
					throw new LimitExceeded(Issue.Kind.DEPTH_LIMIT, "References and nested texts go deeper than " + maxDepth);
				nodes(target.get(), target.get().message(), passed, null, depth + 1);
				return;
			}

			report(Issue.Kind.MISSING_REFERENCE, entry, "No locale of " + chain + " has '" + reference.key() + "'");
			append("{@" + reference.key() + "}");
		}

		/**
		 * Returns the locale a message's numbers, dates and plural forms follow: its own, or for a message of the
		 * root locale, which is the same in every language, the locale asked for first.
		 */
		private Locale formatLocale(Catalog.Entry entry) {
			return entry.locale().equals(Locale.ROOT) ? chain.getFirst() : entry.locale();
		}

		private void report(Issue.Kind kind, Catalog.Entry entry, String detail) {
			issues.accept(issue(kind, entry.key(), entry, detail));
		}

		private void append(String text) {
			grow(text.length());
			literal.append(text);
		}

		private void grow(int added) {
			length += added;
			if (length > maxLength)
				throw new LimitExceeded(Issue.Kind.SIZE_LIMIT, "The resolved message grows past " + maxLength + " characters");
		}

		private void flush() {
			if (literal.isEmpty()) return;

			parts.add(new ResolvedMessage.Literal(literal.toString()));
			literal.setLength(0);
		}
	}

	/**
	 * What {@code #} shows inside a plural branch.
	 *
	 * @param name  the plural's argument
	 * @param value its value after the offset; the value as given when it is no number, or {@code null}
	 */
	private record Count(String name, @Nullable Object value) {
	}

	private static String selector(Object value) {
		if (value instanceof Enum<?> constant) return constant.name();
		return String.valueOf(value);
	}

	private static String exact(Number number) {
		BigDecimal decimal = decimal(number).stripTrailingZeros();
		return decimal.scale() <= 0 ? decimal.toBigInteger().toString() : decimal.toPlainString();
	}

	private static Number minus(Number number, long offset) {
		if (offset == 0) return number;
		if (number instanceof Integer || number instanceof Long || number instanceof Short || number instanceof Byte)
			return number.longValue() - offset;

		return decimal(number).subtract(BigDecimal.valueOf(offset));
	}

	private static BigDecimal decimal(Number number) {
		if (number instanceof BigDecimal decimal) return decimal;
		if (number instanceof Integer || number instanceof Long || number instanceof Short || number instanceof Byte)
			return BigDecimal.valueOf(number.longValue());

		return new BigDecimal(number.toString());
	}
}
