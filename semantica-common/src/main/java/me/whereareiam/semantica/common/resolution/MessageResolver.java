package me.whereareiam.semantica.common.resolution;

import lombok.RequiredArgsConstructor;
import me.whereareiam.semantica.Message;
import me.whereareiam.semantica.Value;
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
 * Resolves a {@link Message} against a catalog: finds its message along the locale chain, chooses branches,
 * expands references and nested texts and formats values.
 *
 * <p>A message that goes deeper than the depth limit or grows past the size limit is given up on in that
 * locale, reported, and the next locale of the chain is tried; when none is left, the key is shown.</p>
 *
 * <p>A message of the root locale is the same in every language: its references and nested texts follow the
 * chain of the locale asked for, as every message's do, and its numbers, dates and plural forms follow the
 * locale asked for first.</p>
 */
@RequiredArgsConstructor
public final class MessageResolver {
	/**
	 * The layer named in issues of a content written in a config.
	 */
	public static final String CONTENT_LAYER = "content";

	/**
	 * Formats values and chooses plural forms.
	 */
	private final ValueFormatter formatter;

	/**
	 * How deep references and nested texts may go.
	 */
	private final int maxDepth;

	/**
	 * How many characters a resolved message may have.
	 */
	private final int maxLength;

	/**
	 * Resolves a message.
	 *
	 * @param catalog the catalog to take messages from
	 * @param message the message
	 * @param chain   the locales to try, in order
	 * @param issues  receives what went wrong; the result is still usable
	 * @return the resolved message; when no locale has the key, a message holding the key
	 */
	public @NotNull ResolvedMessage resolve(
			@NotNull Catalog catalog,
			@NotNull Message message,
			@NotNull List<Locale> chain,
			@NotNull Consumer<Issue> issues
	) {
		boolean found = false;
		for (Locale locale : chain) {
			Optional<Catalog.Entry> entry = catalog.entry(locale, message.getKey());
			if (entry.isEmpty()) continue;

			found = true;
			Expansion expansion = new Expansion(catalog, chain, issues);
			try {
				expansion.message(entry.get(), message.getValues(), 0);
				return new ResolvedMessage(message.getKey(), locale, expansion.parts, true);
			} catch (LimitExceeded exceeded) {
				issues.accept(issue(exceeded.kind, message.getKey(), entry.get(), exceeded.getMessage()));
			}
		}

		if (!found) {
			issues.accept(new Issue(Issue.Kind.MISSING_KEY, message.getKey(), chain.getFirst(), null, null,
					"No locale of " + chain + " has this key"));
		}
		return new ResolvedMessage(message.getKey(), chain.getFirst(), List.of(new ResolvedMessage.Literal(message.getKey())), false);
	}

	/**
	 * Resolves the message of a content, written in a config rather than a catalog. It resolves as a message of the
	 * root locale: its references follow the chain, its numbers and plural forms the locale asked for first. Its source
	 * stands for the key, in issues and in the result.
	 *
	 * @param catalog the catalog its references take messages from
	 * @param source  the content as written
	 * @param message the content as parsed
	 * @param values  the values of its arguments, which its references see too
	 * @param chain   the locales to try, in order
	 * @param issues  receives what went wrong; the result is still usable
	 * @return the resolved message; when it grows past a limit, a message holding the source
	 */
	public @NotNull ResolvedMessage resolve(
			@NotNull Catalog catalog,
			@NotNull String source,
			@NotNull ParsedMessage message,
			@NotNull Map<String, Value> values,
			@NotNull List<Locale> chain,
			@NotNull Consumer<Issue> issues
	) {
		Catalog.Entry entry = new Catalog.Entry(source, Locale.ROOT, source, message, CONTENT_LAYER, null);
		Expansion expansion = new Expansion(catalog, chain, issues);
		try {
			expansion.message(entry, values, 0);
			return new ResolvedMessage(source, Locale.ROOT, expansion.parts, true);
		} catch (LimitExceeded exceeded) {
			issues.accept(issue(exceeded.kind, source, entry, exceeded.getMessage()));
			return new ResolvedMessage(source, Locale.ROOT, List.of(new ResolvedMessage.Literal(source)), false);
		}
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

		private void message(Catalog.Entry entry, Map<String, Value> values, int depth) {
			if (depth > maxDepth)
				throw new LimitExceeded(Issue.Kind.DEPTH_LIMIT, "References and nested texts go deeper than " + maxDepth);

			nodes(entry, entry.message(), values, null, depth);
			flush();
		}

		private void nodes(Catalog.Entry entry, ParsedMessage message, Map<String, Value> values, @Nullable Count count, int depth) {
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

		private void argument(Catalog.Entry entry, Argument argument, Map<String, Value> values, @Nullable Count count, int depth) {
			boolean isCount = argument.name().equals(ParsedMessage.COUNT);
			Value value = isCount ? (count != null ? count.value() : null) : values.get(argument.name());
			if (value == null) {
				String name = isCount && count != null ? count.name() : argument.name();
				if (!isCount) report(Issue.Kind.MISSING_VALUE, entry, "No value for '" + name + "'");
				append("{" + name + "}");
				return;
			}

			fill(entry, argument, value, depth);
		}

		private void fill(Catalog.Entry entry, Argument argument, Value value, int depth) {
			switch (value) {
				case Value.Nested nested -> nested(entry, nested.value(), depth);
				case Value.Joined joined -> joined.values().forEach(each -> fill(entry, argument, each, depth));
				case Value.Linked linked -> linked(entry, argument, linked, depth);
				default -> formatted(entry, argument, value);
			}
		}

		// The content's parts, resolved in place, then taken out and put back as one linked part.
		private void linked(Catalog.Entry entry, Argument argument, Value.Linked linked, int depth) {
			flush();
			int start = parts.size();
			fill(entry, argument, linked.content(), depth);
			flush();

			List<Part> content = parts.subList(start, parts.size());
			ResolvedMessage.Linked part = new ResolvedMessage.Linked(content, linked.link());
			content.clear();
			parts.add(part);
		}

		private void formatted(Catalog.Entry entry, Argument argument, Value value) {
			String formatted;
			try {
				formatted = formatter.format(value, argument.format(), argument.style(), formatLocale(entry));
			} catch (RuntimeException exception) {
				report(Issue.Kind.INVALID_VALUE, entry, "Cannot format '" + argument.name() + "': " + exception.getMessage());
				formatted = value.plain();
			}

			flush();
			ResolvedMessage.Filled part = new ResolvedMessage.Filled(argument.name(), value, formatted, argument.format(), argument.style());
			grow(part.text().length());
			parts.add(part);
		}

		private void nested(Catalog.Entry entry, Message nested, int depth) {
			for (Locale locale : chain) {
				Optional<Catalog.Entry> target = catalog.entry(locale, nested.getKey());
				if (target.isEmpty()) continue;

				message(target.get(), nested.getValues(), depth + 1);
				return;
			}

			report(Issue.Kind.MISSING_KEY, entry, "No locale of " + chain + " has the nested text '" + nested.getKey() + "'");
			append(nested.getKey());
		}

		private void plural(Catalog.Entry entry, Plural plural, Map<String, Value> values, int depth) {
			Value value = values.get(plural.name());
			if (!(value instanceof Value.Number given)) {
				report(value == null ? Issue.Kind.MISSING_VALUE : Issue.Kind.INVALID_VALUE, entry,
						"The plural '" + plural.name() + "' needs a number");
				nodes(entry, plural.branches().get("other"), values, new Count(plural.name(), value), depth);
				return;
			}

			ParsedMessage branch = plural.branches().get("=" + exact(given.value()));
			Number count = minus(given.value(), plural.offset());
			if (branch == null) {
				String form = formatter.pluralForm(formatLocale(entry), count.doubleValue(), plural.ordinal());
				branch = plural.branches().getOrDefault(form, plural.branches().get("other"));
			}

			nodes(entry, branch, values, new Count(plural.name(), Value.number(count)), depth);
		}

		private void select(Catalog.Entry entry, Select select, Map<String, Value> values, @Nullable Count count, int depth) {
			Value value = values.get(select.name());
			if (value == null) report(Issue.Kind.MISSING_VALUE, entry, "No value for '" + select.name() + "'");

			ParsedMessage branch = value == null ? null : select.branches().get(value.plain());
			if (branch == null && value instanceof Value.Choice choice)
				branch = select.branches().get(choice.value().name().toLowerCase(Locale.ROOT));

			nodes(entry, branch != null ? branch : select.branches().get("other"), values, count, depth);
		}

		private void reference(Catalog.Entry entry, Reference reference, Map<String, Value> values, int depth) {
			for (Locale locale : chain) {
				Optional<Catalog.Entry> target = catalog.entry(locale, reference.key());
				if (target.isEmpty()) continue;

				Map<String, Value> passed = new HashMap<>(values);
				reference.renames().forEach((name, from) -> {
					Value value = values.get(from);
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
	private record Count(String name, @Nullable Value value) {
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
