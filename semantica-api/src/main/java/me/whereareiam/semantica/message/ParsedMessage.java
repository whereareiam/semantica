package me.whereareiam.semantica.message;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A message read by a {@link me.whereareiam.semantica.syntax.Syntax}: a list of nodes, parsed once when
 * translations load.
 *
 * <p>{@code You have {count, plural, one {# item} other {# items}} in {@vocabulary.bag}} is a literal, a
 * plural with two branches, another literal and a reference.</p>
 */
@Getter
@EqualsAndHashCode
public final class ParsedMessage {
	/**
	 * Name of the argument {@code #} stands for inside a branch of a plural: the plural's value minus its offset.
	 */
	public static final String COUNT = "#";

	private final List<Node> nodes;

	/**
	 * Creates a message.
	 *
	 * @param nodes its nodes, in order
	 */
	public ParsedMessage(@NotNull List<? extends Node> nodes) {
		this.nodes = List.copyOf(nodes);
	}

	/**
	 * Returns the names of the arguments the message uses anywhere, including inside branches and the
	 * arguments that references take from it. {@link #COUNT} is not included.
	 *
	 * @return the names, in the order they first appear
	 */
	public @NotNull Set<String> arguments() {
		Set<String> names = new LinkedHashSet<>();
		collect(this, names, false);

		return Collections.unmodifiableSet(names);
	}

	/**
	 * Returns the names of the arguments the message always uses, outside every plural and select branch.
	 *
	 * @return the names, in the order they first appear
	 */
	public @NotNull Set<String> requiredArguments() {
		Set<String> names = new LinkedHashSet<>();
		collect(this, names, true);

		return Collections.unmodifiableSet(names);
	}

	/**
	 * Returns the keys the message references, including inside branches.
	 *
	 * @return the keys, in the order they first appear
	 */
	public @NotNull Set<String> references() {
		Set<String> keys = new LinkedHashSet<>();
		references(this, keys);

		return Collections.unmodifiableSet(keys);
	}

	private static void collect(ParsedMessage message, Set<String> names, boolean topLevelOnly) {
		for (Node node : message.nodes) {
			switch (node) {
				case Literal ignored -> {
				}
				case Argument argument -> {
					if (!argument.name().equals(COUNT)) names.add(argument.name());
				}
				case Plural plural -> {
					names.add(plural.name());
					if (!topLevelOnly) plural.branches().values().forEach(branch -> collect(branch, names, false));
				}
				case Select select -> {
					names.add(select.name());
					if (!topLevelOnly) select.branches().values().forEach(branch -> collect(branch, names, false));
				}
				case Reference reference -> names.addAll(reference.renames().values());
			}
		}
	}

	private static void references(ParsedMessage message, Set<String> keys) {
		for (Node node : message.nodes) {
			switch (node) {
				case Reference reference -> keys.add(reference.key());
				case Plural plural -> plural.branches().values().forEach(branch -> references(branch, keys));
				case Select select -> select.branches().values().forEach(branch -> references(branch, keys));
				default -> {
				}
			}
		}
	}

	/**
	 * A part of a message.
	 */
	public sealed interface Node permits Literal, Argument, Plural, Select, Reference {
	}

	/**
	 * Text shown as written, including a format's markup.
	 *
	 * @param text the text
	 */
	public record Literal(@NotNull String text) implements Node {
		public Literal {
			Objects.requireNonNull(text, "text");
		}
	}

	/**
	 * A value inserted into the message: {@code {name}}, {@code {count, number}}, {@code {when, date, short}}.
	 *
	 * @param name   the argument's name; {@link #COUNT} inside a plural branch
	 * @param format how the value is formatted
	 * @param style  the format's style, such as {@code integer}, {@code short} or {@code ::compact-short}, or
	 *               {@code null}
	 */
	public record Argument(@NotNull String name, @NotNull Format format, @Nullable String style) implements Node {
		public Argument {
			Objects.requireNonNull(name, "name");
			Objects.requireNonNull(format, "format");
		}

		/**
		 * How an argument's value is formatted.
		 */
		public enum Format {
			/**
			 * By its type: text as is, numbers and dates in the locale's default style.
			 */
			NONE,

			/**
			 * As a number.
			 */
			NUMBER,

			/**
			 * As a date.
			 */
			DATE,

			/**
			 * As a time.
			 */
			TIME
		}
	}

	/**
	 * A choice by the plural form of a number: {@code {count, plural, one {...} other {...}}}, or by its ordinal
	 * form with {@code selectordinal}.
	 *
	 * @param name     the argument holding the number
	 * @param ordinal  whether ordinal forms ({@code 1st}, {@code 2nd}) are chosen instead of cardinal ones
	 * @param offset   subtracted from the number before a form is chosen and before {@code #} shows it
	 * @param branches the branches by selector: exact values such as {@code =0}, and plural forms such as
	 *                 {@code one}; {@code other} is always present
	 */
	public record Plural(
			@NotNull String name,
			boolean ordinal,
			long offset,
			@NotNull Map<String, ParsedMessage> branches
	) implements Node {
		public Plural {
			Objects.requireNonNull(name, "name");
			branches = Collections.unmodifiableMap(new LinkedHashMap<>(branches));
		}
	}

	/**
	 * A choice by a value: {@code {gender, select, female {...} other {...}}}.
	 *
	 * @param name     the argument holding the value
	 * @param branches the branches by value; {@code other} is always present
	 */
	public record Select(@NotNull String name, @NotNull Map<String, ParsedMessage> branches) implements Node {
		public Select {
			Objects.requireNonNull(name, "name");
			branches = Collections.unmodifiableMap(new LinkedHashMap<>(branches));
		}
	}

	/**
	 * Another message inserted here: {@code {@vocabulary.player}}, or {@code {@vocabulary.player count=online}}
	 * to fill the referenced message's {@code count} from this message's {@code online}.
	 *
	 * @param key     the referenced key
	 * @param renames the referenced message's argument names, mapped to the names of this message whose values
	 *                they take; arguments not named here keep their name
	 */
	public record Reference(@NotNull String key, @NotNull Map<String, String> renames) implements Node {
		public Reference {
			Objects.requireNonNull(key, "key");
			renames = Collections.unmodifiableMap(new LinkedHashMap<>(renames));
		}
	}
}
