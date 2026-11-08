package me.whereareiam.semantica.exception;

import lombok.Getter;

/**
 * Thrown by a {@link me.whereareiam.semantica.syntax.Syntax} when a message cannot be read.
 *
 * <p>Semantica reports it as an issue of kind {@code syntax} while it loads translations; the message is
 * then left out, and lookups fall back to the next locale.</p>
 */
@Getter
public final class SyntaxException extends RuntimeException {
	private final int position;

	/**
	 * Creates the exception.
	 *
	 * @param message  what is wrong, read by translators and administrators
	 * @param position offset in the message's source where the problem was found, starting at 0
	 */
	public SyntaxException(String message, int position) {
		super(message + " (at " + position + ")");
		this.position = position;
	}
}
