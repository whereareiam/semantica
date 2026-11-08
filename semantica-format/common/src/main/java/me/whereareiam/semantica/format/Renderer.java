package me.whereareiam.semantica.format;

import me.whereareiam.semantica.message.ResolvedMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Turns a resolved message into what a format shows: a component, markdown, plain text.
 *
 * <p>A renderer reads the markup of literal parts, which translators wrote, and inserts value parts, which callers
 * gave, without reading them as markup.</p>
 *
 * @param <O> the rendered form, such as a component or a string
 */
@FunctionalInterface
public interface Renderer<O> {
	/**
	 * Renders a message.
	 *
	 * @param message the resolved message
	 * @return the rendered message
	 */
	@NotNull O render(@NotNull ResolvedMessage message);
}
