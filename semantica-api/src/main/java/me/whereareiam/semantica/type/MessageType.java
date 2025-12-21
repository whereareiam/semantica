package me.whereareiam.semantica.type;

/**
 * Type of translation entry.
 */
public enum MessageType {
	/**
	 * A regular message with translations.
	 */
	MESSAGE,

	/**
	 * A template that can be referenced by other messages.
	 */
	TEMPLATE,

	/**
	 * A mixed container of messages and templates.
	 */
	MIXED
}

