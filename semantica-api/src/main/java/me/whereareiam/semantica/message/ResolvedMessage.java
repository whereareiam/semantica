package me.whereareiam.semantica.message;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A message with its branches chosen, its references and nested texts expanded and its values formatted,
 * ready for a format to render.
 *
 * <p>Literal parts are text the translator wrote, including a format's markup; value parts are what the
 * caller gave and are never read as markup.</p>
 */
@Getter
@EqualsAndHashCode
public final class ResolvedMessage {
	private final String key;
	private final Locale locale;
	private final List<Part> parts;
	private final boolean found;

	/**
	 * Creates a resolved message.
	 *
	 * @param key    the key that was resolved
	 * @param locale the locale the message was taken from
	 * @param parts  its parts, in order
	 * @param found  whether a message was found; when not, the parts hold the key
	 */
	public ResolvedMessage(@NotNull String key, @NotNull Locale locale, @NotNull List<? extends Part> parts, boolean found) {
		this.key = Objects.requireNonNull(key, "key");
		this.locale = Objects.requireNonNull(locale, "locale");
		this.parts = List.copyOf(parts);
		this.found = found;
	}

	/**
	 * Returns the message as plain text: literals as written, values as formatted.
	 *
	 * @return the text
	 */
	public @NotNull String plain() {
		StringBuilder text = new StringBuilder();
		for (Part part : parts) text.append(part.text());

		return text.toString();
	}

	@Override
	public String toString() {
		return plain();
	}

	/**
	 * A part of a resolved message.
	 */
	public sealed interface Part permits Literal, Value {
		/**
		 * Returns the part as plain text.
		 *
		 * @return the text
		 */
		@NotNull String text();
	}

	/**
	 * Text the translator wrote.
	 *
	 * @param text the text, possibly with markup of the format
	 */
	public record Literal(@NotNull String text) implements Part {
		public Literal {
			Objects.requireNonNull(text, "text");
		}
	}

	/**
	 * A value the caller gave.
	 *
	 * @param name      the argument's name
	 * @param value     the value as given
	 * @param formatted the value formatted for the locale, or {@code null} when Semantica does not know its
	 *                  type and leaves it to the format, such as a component
	 * @param format    how the message asked for the value to be formatted
	 * @param style     the format's style, or {@code null}
	 */
	public record Value(
			@NotNull String name,
			@NotNull Object value,
			@Nullable String formatted,
			@NotNull ParsedMessage.Argument.Format format,
			@Nullable String style
	) implements Part {
		public Value {
			Objects.requireNonNull(name, "name");
			Objects.requireNonNull(value, "value");
			Objects.requireNonNull(format, "format");
		}

		@Override
		public @NotNull String text() {
			return formatted != null ? formatted : String.valueOf(value);
		}
	}
}
