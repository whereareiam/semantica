package me.whereareiam.semantica;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A message to show: the key of a translation and the values its arguments take.
 *
 * <p>A message is immutable; {@link #with(String, Object)} returns a new message. Values may be strings, numbers,
 * dates and instants, booleans, enums, other messages (resolved in the same locale) and whatever a format
 * renders itself, such as an Adventure component.</p>
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
	private final Map<String, Object> values;

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
	 * Returns a message with one more value, replacing a value of the same name.
	 *
	 * @param name  name of the argument, as written in the message: {@code {count}} is {@code count}
	 * @param value the value
	 * @return a new message; this one is unchanged
	 */
	public @NotNull Message with(@NotNull String name, @NotNull Object value) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(value, "value");

		Map<String, Object> copy = new LinkedHashMap<>(values);
		copy.put(name, value);

		return new Message(key, Collections.unmodifiableMap(copy));
	}

	/**
	 * Returns a message with these values added, replacing values of the same names.
	 *
	 * @param values values by argument name
	 * @return a new message; this one is unchanged
	 */
	public @NotNull Message with(@NotNull Map<String, ?> values) {
		Map<String, Object> copy = new LinkedHashMap<>(this.values);
		values.forEach((name, value) -> copy.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, name)));

		return new Message(key, Collections.unmodifiableMap(copy));
	}

	@Override
	public String toString() {
		return values.isEmpty() ? key : key + values;
	}
}
