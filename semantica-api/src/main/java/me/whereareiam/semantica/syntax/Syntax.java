package me.whereareiam.semantica.syntax;

import me.whereareiam.semantica.exception.SyntaxException;
import me.whereareiam.semantica.message.ParsedMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Reads the source of a message into a {@link ParsedMessage}.
 *
 * <p>Semantica parses each message once, when it loads translations, and keeps the tree; resolving a
 * message never reads text again. The standard syntax is ICU MessageFormat with references, in the
 * {@code syntax-icu} module.</p>
 */
public interface Syntax {
	/**
	 * Parses a message.
	 *
	 * @param source the message as written by a translator
	 * @return the parsed message
	 * @throws SyntaxException when the source is not a valid message
	 */
	@NotNull ParsedMessage parse(@NotNull String source);
}
