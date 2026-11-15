package me.whereareiam.semantica.common.resolution;

import me.whereareiam.semantica.Content;
import me.whereareiam.semantica.exception.SyntaxException;
import me.whereareiam.semantica.message.ParsedMessage;
import me.whereareiam.semantica.syntax.Syntax;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses the contents of configs once and keeps them by their text, the most recently used first. A content is
 * parsed without the catalog, so what is kept stays valid when the catalog changes.
 */
public final class ContentParser {
	private final Syntax syntax;
	private final Map<String, Parsed> parsed;

	/**
	 * Creates a parser.
	 *
	 * @param syntax the syntax contents are written in
	 * @param limit  how many contents are kept at most
	 */
	public ContentParser(@NotNull Syntax syntax, int limit) {
		this.syntax = syntax;
		this.parsed = new LinkedHashMap<>(16, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Parsed> eldest) {
				return size() > limit;
			}
		};
	}

	/**
	 * Parses a content, or returns it as parsed before.
	 *
	 * @param content the content
	 * @return the parsed message, or the syntax error
	 */
	public @NotNull Parsed parse(@NotNull Content content) {
		String source = content.getSource();
		synchronized (parsed) {
			Parsed known = parsed.get(source);
			if (known != null) return known;
		}

		Parsed result;
		try {
			result = new Parsed(syntax.parse(source), null);
		} catch (SyntaxException exception) {
			result = new Parsed(null, exception.getMessage());
		}

		synchronized (parsed) {
			parsed.put(source, result);
		}
		return result;
	}

	/**
	 * A parsed content: its message, or why it is none.
	 *
	 * @param message the message, or {@code null} when the source is not valid in the syntax
	 * @param error   what is wrong with the source, or {@code null} when it is valid
	 */
	public record Parsed(@Nullable ParsedMessage message, @Nullable String error) {
	}
}
