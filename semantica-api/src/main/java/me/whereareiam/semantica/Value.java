package me.whereareiam.semantica;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Objects;

/**
 * The value of a message's argument. Each kind is one of what a message can show: text, a number, a date or time, a
 * boolean, an enum constant, another message, or a value a format renders itself, such as an Adventure component.
 * <pre>{@code
 * Message.of("shop.bought")
 *         .with("item", itemName)            // Value.Text
 *         .with("count", 3)                  // Value.Number
 *         .with("at", Instant.now());        // Value.Time
 * }</pre>
 * {@link Message#with} takes the plain Java value and makes the kind; these factories make one where a value is
 * passed on, such as in a map of values.
 */
public sealed interface Value permits Value.Text, Value.Number, Value.Time, Value.Flag, Value.Choice, Value.Nested, Value.Rendered {
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
	 * Returns the value as plain text, as it shows where nothing formats it: a number and a time as Java writes them,
	 * an enum constant by its name, another message by its key.
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
