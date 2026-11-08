package me.whereareiam.semantica.format.adventure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The standard tags one version of MiniMessage knows, by group. Each Adventure adapter provides its own, so the
 * {@link MarkupCheck} judges tags as the server's Adventure will.
 */
public interface MiniMessageTags {
	/**
	 * Returns the group a tag belongs to.
	 *
	 * @param name the tag's name, lowercase and without {@code !}, such as {@code red} or {@code #ff0000}
	 * @return the group, or {@code null} when this MiniMessage has no such tag
	 */
	@Nullable TagPolicy.Group group(@NotNull String name);

	/**
	 * Reduces a tag's name as written to the name tags are known by: lowercase, without a leading {@code !}.
	 *
	 * @param name the name as written, such as {@code !BOLD}
	 * @return the name, such as {@code bold}
	 */
	static @NotNull String normalize(@NotNull String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		return lower.startsWith("!") ? lower.substring(1) : lower;
	}
}
