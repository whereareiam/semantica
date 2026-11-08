package me.whereareiam.semantica;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The start of keys, whole segments only: {@code realm.vanilla.common} takes {@code realm.vanilla.common} and
 * {@code realm.vanilla.common.join.title}, never {@code realm.vanilla.commons}. A translations repository scopes
 * its products and its markup by key prefixes, and a bundle source keeps only the keys of the prefixes it is given.
 *
 * <pre>{@code
 * KeyPrefix prefix = KeyPrefix.of("realm.vanilla.common");
 * prefix.matches("realm.vanilla.common.join.title"); // true
 * prefix.matches("realm.create.common.join.title");  // false
 * }</pre>
 */
@EqualsAndHashCode
public final class KeyPrefix {
	private static final Pattern VALID = Pattern.compile("[\\p{L}\\p{N}_-]+(\\.[\\p{L}\\p{N}_-]+)*");

	private final String value;

	private KeyPrefix(String value) {
		this.value = value;
	}

	/**
	 * Reads a prefix.
	 *
	 * @param value segments separated by dots, such as {@code game} or {@code realm.vanilla.mode.bedwars}
	 * @return the prefix
	 * @throws IllegalArgumentException when the value is empty, or starts, ends or doubles a dot
	 */
	public static @NotNull KeyPrefix of(@NotNull String value) {
		Objects.requireNonNull(value, "value");
		if (!VALID.matcher(value).matches())
			throw new IllegalArgumentException("'" + value + "' is no key prefix; write segments separated by dots, such as realm.vanilla.common");

		return new KeyPrefix(value);
	}

	/**
	 * Tells whether a key starts with this prefix, by whole segments.
	 *
	 * @param key the key
	 * @return whether the key is the prefix or lies below it
	 */
	public boolean matches(@NotNull String key) {
		return key.equals(value) || key.startsWith(value) && key.length() > value.length() && key.charAt(value.length()) == '.';
	}

	/**
	 * Returns how many segments the prefix has, so the longest of several matching prefixes can be chosen.
	 *
	 * @return the number of segments
	 */
	public int depth() {
		return (int) value.chars().filter(character -> character == '.').count() + 1;
	}

	@Override
	public String toString() {
		return value;
	}
}
