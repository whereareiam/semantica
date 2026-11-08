package me.whereareiam.semantica.testing;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.issue.Issue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Assertions on issues for tests of any framework: each throws an {@link AssertionError} listing the issues
 * there are.
 *
 * <pre>{@code
 * IssueAssert.assertNone(semantica.getCatalog());
 * IssueAssert.assertReported(semantica.getCatalog(), Issue.Kind.REFERENCE_CYCLE, "a");
 * }</pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IssueAssert {
	/**
	 * Asserts that a catalog has no issues.
	 *
	 * @param catalog the catalog
	 */
	public static void assertNone(@NotNull Catalog catalog) {
		assertNone(catalog.getIssues());
	}

	/**
	 * Asserts that there are no issues.
	 *
	 * @param issues the issues
	 */
	public static void assertNone(@NotNull Collection<Issue> issues) {
		if (!issues.isEmpty()) throw new AssertionError("Expected no issues, but got:\n" + list(issues));
	}

	/**
	 * Asserts that a catalog has an issue of a kind for a key.
	 *
	 * @param catalog the catalog
	 * @param kind    the kind
	 * @param key     the key, or {@code null} for an issue without key
	 * @return the issue
	 */
	public static @NotNull Issue assertReported(@NotNull Catalog catalog, @NotNull Issue.Kind kind, @Nullable String key) {
		return assertReported(catalog.getIssues(), kind, key, null);
	}

	/**
	 * Asserts that there is an issue of a kind for a key, and in a locale when one is given.
	 *
	 * @param issues the issues
	 * @param kind   the kind
	 * @param key    the key, or {@code null} for an issue without key
	 * @param locale the locale, or {@code null} for any locale
	 * @return the first such issue
	 */
	public static @NotNull Issue assertReported(
			@NotNull Collection<Issue> issues,
			@NotNull Issue.Kind kind,
			@Nullable String key,
			@Nullable Locale locale
	) {
		return issues.stream()
				.filter(issue -> issue.getKind() == kind && Objects.equals(issue.getKey(), key))
				.filter(issue -> locale == null || locale.equals(issue.getLocale()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected " + kind.getId() + " for " + key
						+ (locale != null ? " in " + locale.toLanguageTag() : "") + ", but got:\n" + list(issues)));
	}

	/**
	 * Asserts that there is no issue of a kind.
	 *
	 * @param issues the issues
	 * @param kind   the kind
	 */
	public static void assertNotReported(@NotNull Collection<Issue> issues, @NotNull Issue.Kind kind) {
		List<Issue> found = issues.stream().filter(issue -> issue.getKind() == kind).toList();
		if (!found.isEmpty()) throw new AssertionError("Expected no " + kind.getId() + ", but got:\n" + list(found));
	}

	private static String list(Collection<Issue> issues) {
		if (issues.isEmpty()) return "  (none)";
		return issues.stream().map(issue -> "  " + issue).collect(Collectors.joining("\n"));
	}
}
