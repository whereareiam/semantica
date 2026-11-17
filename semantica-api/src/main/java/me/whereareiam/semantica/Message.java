package me.whereareiam.semantica;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A message to show: the key of a translation and the values its arguments take.
 *
 * <p>A message is immutable; each {@code with} returns a new message. A value is text, a number, a date or time, a
 * boolean, an enum constant, another message (resolved in the same locale), or a {@link Value} a format renders
 * itself, such as an Adventure component.</p>
 *
 * <pre>{@code
 * Message message = Message.of("shop.bought")
 *         .with("item", itemName)
 *         .with("count", 3);
 * }</pre>
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Message {
	private final String key;
	private final Map<String, Value> values;

	/**
	 * Starts a message without values.
	 *
	 * @param key key of the translation, such as {@code shop.bought}
	 * @return the message
	 * @throws IllegalArgumentException when the key is blank
	 */
	public static @NotNull Message of(@NotNull String key) {
		Objects.requireNonNull(key, "key");
		if (key.isBlank()) throw new IllegalArgumentException("A message needs a key");

		return new Message(key, Map.of());
	}

	/**
	 * Returns a message with one more text value, shown as it is and never read as markup.
	 *
	 * @param name  name of the argument, as written in the message: {@code {player}} is {@code player}
	 * @param value the text
	 * @return a new message; this one is unchanged
	 */
	public @NotNull Message with(@NotNull String name, @NotNull String value) {
		return with(name, Value.text(value));
	}

	/**
	 * Returns a message with one more number, which {@code {count, number}} formats and a plural chooses by.
	 *
	 * @param name  name of the argument
	 * @param value the number
	 * @return a new message; this one is unchanged
	 */
	public @NotNull Message with(@NotNull String name, @NotNull Number value) {
		return with(name, Value.number(value));
	}

	/**
	 * Returns a message with one more date or time.
	 *
	 * @param name  name of the argument
	 * @param value an instant, a zoned, offset or local date-time, a local date or a local time
	 * @return a new message; this one is unchanged
	 */
	public @NotNull Message with(@NotNull String name, @NotNull TemporalAccessor value) {
		return with(name, Value.time(value));
	}

	/**
	 * Returns a message with one more date or time.
	 *
	 * @param name  name of the argument
	 * @param value the date, read as an instant
	 * @return a new message; this one is unchanged
	 */
	public @NotNull Message with(@NotNull String name, @NotNull Date value) {
		return with(name, Value.time(value));
	}

	/**
	 * Returns a message with one more boolean, which a select chooses {@code true} or {@code false} by.
	 *
	 * @param name  name of the argument
	 * @param value the boolean
	 * @return a new message; this one is unchanged
	 */
	public @NotNull Message with(@NotNull String name, boolean value) {
		return with(name, Value.flag(value));
	}

	/**
	 * Returns a message with one more enum constant, which a select chooses by.
	 *
	 * @param name  name of the argument
	 * @param value the constant
	 * @return a new message; this one is unchanged
	 */
	public @NotNull Message with(@NotNull String name, @NotNull Enum<?> value) {
		return with(name, Value.choice(value));
	}

	/**
	 * Returns a message with one more message, resolved in the same locale where the argument stands.
	 *
	 * @param name  name of the argument
	 * @param value the message
	 * @return a new message; this one is unchanged
	 */
	public @NotNull Message with(@NotNull String name, @NotNull Message value) {
		return with(name, Value.nested(value));
	}

	/**
	 * Returns a message with one more value, replacing a value of the same name.
	 *
	 * @param name  name of the argument, as written in the message: {@code {count}} is {@code count}
	 * @param value the value
	 * @return a new message; this one is unchanged
	 */
	public @NotNull Message with(@NotNull String name, @NotNull Value value) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(value, "value");

		Map<String, Value> copy = new LinkedHashMap<>(values);
		copy.put(name, value);

		return new Message(key, Collections.unmodifiableMap(copy));
	}

	/**
	 * Returns a message with these values added, replacing values of the same names.
	 *
	 * @param values values by argument name
	 * @return a new message; this one is unchanged
	 */
	public @NotNull Message with(@NotNull Map<String, ? extends Value> values) {
		Map<String, Value> copy = new LinkedHashMap<>(this.values);
		values.forEach((name, value) -> copy.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, name)));

		return new Message(key, Collections.unmodifiableMap(copy));
	}

	@Override
	public String toString() {
		return values.isEmpty() ? key : key + values;
	}
}
