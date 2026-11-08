package me.whereareiam.semantica.issue;

import me.whereareiam.semantica.catalog.Catalog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Looks at a whole catalog and reports what is wrong with it.
 *
 * <p>Semantica runs its own checks and those registered on it each time it builds a catalog, before the
 * catalog replaces the previous one. Formats provide checks of their own, such as the allowed MiniMessage tags of
 * the Adventure format, and projects may add theirs; the {@code semantica-cli} runs the same checks on a
 * translations repository.</p>
 */
public interface Check {
	/**
	 * Checks a catalog.
	 *
	 * @param catalog the catalog about to be used
	 * @return the issues found, empty when there are none
	 */
	@NotNull List<Issue> check(@NotNull Catalog catalog);
}
