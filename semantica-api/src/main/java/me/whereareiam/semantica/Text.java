package me.whereareiam.semantica;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A message to show: the key of a translation and the values its arguments take.
 *
 * <p>A text is immutable; {@link #with(String, Object)} returns a new text. Values may be strings, numbers,
 * dates and instants, booleans, enums, other texts (resolved in the same locale) and whatever a format
 * renders itself, such as an Adventure component.</p>
 *
 * <pre>{@code
 * Text text = Text.of("shop.bought")
 *         .with("item", itemName)
 *         .with("count", 3);
 * }</pre>
 */
@Getter
@EqualsAndHashCode
public final class Text {
	private final String key;
	private final Map<String, Object> values;

	private Text(String key, Map<String, Object> values) {
		this.key = key;
		this.values = values;
	}

	/**
	 * Starts a text without values.
	 *
	 * @param key key of the translation, such as {@code shop.bought}
	 * @return the text
	 * @throws IllegalArgumentException when the key is blank
	 */
	public static @NotNull Text of(@NotNull String key) {
		Objects.requireNonNull(key, "key");
		if (key.isBlank()) throw new IllegalArgumentException("A text needs a key");

		return new Text(key, Map.of());
	}

	/**
	 * Returns a text with one more value, replacing a value of the same name.
	 *
	 * @param name  name of the argument, as written in the message: {@code {count}} is {@code count}
	 * @param value the value
	 * @return a new text; this one is unchanged
	 */
	public @NotNull Text with(@NotNull String name, @NotNull Object value) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(value, "value");

		Map<String, Object> copy = new LinkedHashMap<>(values);
		copy.put(name, value);

		return new Text(key, Collections.unmodifiableMap(copy));
	}

	/**
	 * Returns a text with these values added, replacing values of the same names.
	 *
	 * @param values values by argument name
	 * @return a new text; this one is unchanged
	 */
	public @NotNull Text with(@NotNull Map<String, ?> values) {
		Map<String, Object> copy = new LinkedHashMap<>(this.values);
		values.forEach((name, value) -> copy.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, name)));

		return new Text(key, Collections.unmodifiableMap(copy));
	}

	@Override
	public String toString() {
		return values.isEmpty() ? key : key + values;
	}
}
