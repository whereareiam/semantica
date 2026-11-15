package me.whereareiam.semantica.declaration;

import org.jetbrains.annotations.NotNull;

/**
 * A value that names one segment of a key, given to a {@link Segment} parameter, such as the reason a condition
 * failed. An enum of reasons implements it as easily as a class of an open set.
 *
 * <pre>{@code
 * public enum ConditionReason implements KeySegment {
 *     COOLDOWN, MUTED;
 *
 *     @Override
 *     public String segment() {
 *         return name().toLowerCase(Locale.ROOT);
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface KeySegment {
	/**
	 * Returns the segment.
	 *
	 * @return letters, digits, {@code _} and {@code -}, without dots, such as {@code cooldown}
	 */
	@NotNull String segment();
}
