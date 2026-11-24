package me.whereareiam.semantica;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * The value of a message's argument. Each kind is one of what a message can show: text, a number, a date or time, a
 * boolean, an enum constant, another message, several values one after another, a value that does something when it
 * is clicked, or a value a format renders itself, such as an Adventure component.
 * <pre>{@code
 * Message.of("shop.bought")
 *         .with("item", itemName)            // Value.Text
 *         .with("count", 3)                  // Value.Number
 *         .with("at", Instant.now());        // Value.Time
 * }</pre>
 * {@link Message#with} takes the plain Java value and makes the kind; these factories make one where a value is
 * passed on, such as in a map of values.
 */
public sealed interface Value permits Value.Text, Value.Number, Value.Time, Value.Flag, Value.Choice, Value.Nested, Value.Joined,
		Value.Linked, Value.Rendered {
	/**
	 * Returns a text value, shown as it is and never read as markup.
	 *
	 * @param text the text
	 * @return the value
	 */
	static @NotNull Text text(@NotNull String text) {
		return new Text(text);
	}

	/**
	 * Returns a number, which {@code {count, number}} formats and a plural chooses its form by.
	 *
	 * @param number the number
	 * @return the value
	 */
	static @NotNull Number number(java.lang.@NotNull Number number) {
		return new Number(number);
	}

	/**
	 * Returns a date or time, which {@code {at, date}} and {@code {at, time}} format.
	 *
	 * @param time an instant, a zoned, offset or local date-time, a local date or a local time
	 * @return the value
	 */
	static @NotNull Time time(@NotNull TemporalAccessor time) {
		return new Time(time);
	}

	/**
	 * Returns a date or time.
	 *
	 * @param date the date, read as an {@link Instant}
	 * @return the value
	 */
	static @NotNull Time time(@NotNull Date date) {
		return new Time(Objects.requireNonNull(date, "date").toInstant());
	}

	/**
	 * Returns a boolean, which a select chooses {@code true} or {@code false} by.
	 *
	 * @param flag the boolean
	 * @return the value
	 */
	static @NotNull Flag flag(boolean flag) {
		return new Flag(flag);
	}

	/**
	 * Returns an enum constant, which a select chooses its branch by: the constant's name, or its name in lower case.
	 *
	 * @param choice the constant
	 * @return the value
	 */
	static @NotNull Choice choice(@NotNull Enum<?> choice) {
		return new Choice(choice);
	}

	/**
	 * Returns another message, resolved in the same locale where the argument stands.
	 *
	 * @param message the message
	 * @return the value
	 */
	static @NotNull Nested nested(@NotNull Message message) {
		return new Nested(message);
	}

	/**
	 * Returns several values shown one after another where the argument stands, each as it would show there alone,
	 * such as a command's arguments, each its own message:
	 * <pre>{@code
	 * Value arguments = Value.joined(List.of(
	 *         Value.text(" "), Value.nested(messages.argument("player")),
	 *         Value.text(" "), Value.nested(messages.optionalArgument("page"))));
	 * }</pre>
	 *
	 * @param values the values, in order; none shows nothing
	 * @return the value
	 */
	static @NotNull Joined joined(@NotNull List<? extends Value> values) {
		return new Joined(List.copyOf(values));
	}

	/**
	 * Returns a value that does something when it is clicked, such as a page's arrow that runs the command of the
	 * page. Formats that cannot click show the content alone.
	 * <pre>{@code
	 * Value next = Value.linked(Value.nested(messages.next(3)), Value.Link.runCommand("/arbor help 3"));
	 * }</pre>
	 *
	 * @param content what is shown, such as another message
	 * @param link    what a click does
	 * @return the value
	 */
	static @NotNull Linked linked(@NotNull Value content, @NotNull Link link) {
		return new Linked(content, link);
	}

	/**
	 * Returns the value as plain text, as it shows where nothing formats it: a number and a time as Java writes them,
	 * an enum constant by its name, another message by its key, joined values one after another and a linked value by
	 * its content.
	 *
	 * @return the text
	 */
	default @NotNull String plain() {
		return switch (this) {
			case Text text -> text.value();
			case Number number -> number.value().toString();
			case Time time -> time.value().toString();
			case Flag flag -> Boolean.toString(flag.value());
			case Choice choice -> choice.value().name();
			case Nested nested -> nested.value().getKey();
			case Joined joined -> {
				StringBuilder text = new StringBuilder();
				joined.values().forEach(value -> text.append(value.plain()));
				yield text.toString();
			}
			case Linked linked -> linked.content().plain();
			case Rendered rendered -> rendered.plain();
		};
	}

	/**
	 * Text, shown as it is.
	 *
	 * @param value the text
	 */
	record Text(@NotNull String value) implements Value {
		public Text {
			Objects.requireNonNull(value, "value");
		}
	}

	/**
	 * A number.
	 *
	 * @param value the number
	 */
	record Number(java.lang.@NotNull Number value) implements Value {
		public Number {
			Objects.requireNonNull(value, "value");
		}
	}

	/**
	 * A date or time.
	 *
	 * @param value an instant, a zoned, offset or local date-time, a local date or a local time
	 */
	record Time(@NotNull TemporalAccessor value) implements Value {
		public Time {
			Objects.requireNonNull(value, "value");
		}
	}

	/**
	 * A boolean.
	 *
	 * @param value the boolean
	 */
	record Flag(boolean value) implements Value {
	}

	/**
	 * An enum constant.
	 *
	 * @param value the constant
	 */
	record Choice(@NotNull Enum<?> value) implements Value {
		public Choice {
			Objects.requireNonNull(value, "value");
		}
	}

	/**
	 * Another message.
	 *
	 * @param value the message
	 */
	record Nested(@NotNull Message value) implements Value {
		public Nested {
			Objects.requireNonNull(value, "value");
		}
	}

	/**
	 * Several values, shown one after another.
	 *
	 * @param values the values, in order
	 */
	record Joined(@NotNull List<Value> values) implements Value {
		public Joined {
			values = List.copyOf(values);
		}
	}

	/**
	 * A value that does something when it is clicked.
	 *
	 * @param content what is shown
	 * @param link    what a click does
	 */
	record Linked(@NotNull Value content, @NotNull Link link) implements Value {
		public Linked {
			Objects.requireNonNull(content, "content");
			Objects.requireNonNull(link, "link");
		}
	}

	/**
	 * What clicking a {@link Linked} value does: an action and its target, such as a command or an address. The
	 * program chooses it, never a translation, so a translator cannot make a reader run a command.
	 *
	 * @param action what is done
	 * @param target the command, the address or the text, as the action needs
	 */
	record Link(@NotNull Action action, @NotNull String target) {
		public Link {
			Objects.requireNonNull(action, "action");
			Objects.requireNonNull(target, "target");
		}

		/**
		 * Returns a link that runs a command as the reader, such as {@code /arbor help 2}.
		 *
		 * @param command the command
		 * @return the link
		 */
		public static @NotNull Link runCommand(@NotNull String command) {
			return new Link(Action.RUN_COMMAND, command);
		}

		/**
		 * Returns a link that writes a command into the reader's chat for them to complete and send.
		 *
		 * @param command the command
		 * @return the link
		 */
		public static @NotNull Link suggestCommand(@NotNull String command) {
			return new Link(Action.SUGGEST_COMMAND, command);
		}

		/**
		 * Returns a link that opens an address.
		 *
		 * @param url the address
		 * @return the link
		 */
		public static @NotNull Link openUrl(@NotNull String url) {
			return new Link(Action.OPEN_URL, url);
		}

		/**
		 * Returns a link that copies a text to the reader's clipboard.
		 *
		 * @param text the text
		 * @return the link
		 */
		public static @NotNull Link copy(@NotNull String text) {
			return new Link(Action.COPY, text);
		}

		/**
		 * What a link does.
		 */
		public enum Action {
			/**
			 * Runs a command as the reader.
			 */
			RUN_COMMAND,

			/**
			 * Writes a command into the reader's chat.
			 */
			SUGGEST_COMMAND,

			/**
			 * Opens an address.
			 */
			OPEN_URL,

			/**
			 * Copies a text to the clipboard.
			 */
			COPY
		}
	}

	/**
	 * A value a format renders itself, such as an Adventure component; a format offers its own kind. Semantica
	 * neither formats nor selects by it, and a format that does not know the kind shows {@link #plain()}.
	 */
	non-sealed interface Rendered extends Value {
		/**
		 * Returns the value as plain text, for formats and places that show no more than text.
		 *
		 * @return the text
		 */
		@Override
		@NotNull String plain();
	}
}
