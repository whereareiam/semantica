package me.whereareiam.semantica.issue;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

/**
 * A problem with a translation, found while loading translations or while resolving a text.
 *
 * <p>Semantica never fails a lookup because of an issue: the affected message is left out and the lookup
 * falls back to the next locale of its chain, or to the key itself. Issues are reported to the handler given
 * to Semantica's builder and kept on the {@link me.whereareiam.semantica.catalog.Catalog} they were found in.</p>
 */
@Getter
@EqualsAndHashCode
public final class Issue {
	private final @NotNull Kind kind;
	private final @Nullable String key;
	private final @Nullable Locale locale;
	private final @Nullable String layer;
	private final @Nullable String origin;
	private final @NotNull String detail;

	/**
	 * Creates an issue.
	 *
	 * @param kind   what kind of problem it is
	 * @param key    the key of the affected message, or {@code null} when the issue concerns no single key
	 * @param locale the locale of the affected message, or {@code null}
	 * @param layer  the layer the message came from, or {@code null}
	 * @param origin where the message is written, such as {@code ru/game/menu.json}, or {@code null}
	 * @param detail what is wrong, read by translators and administrators
	 */
	@Builder
	public Issue(
			@NotNull Kind kind,
			@Nullable String key,
			@Nullable Locale locale,
			@Nullable String layer,
			@Nullable String origin,
			@NotNull String detail
	) {
		this.kind = Objects.requireNonNull(kind, "kind");
		this.key = key;
		this.locale = locale;
		this.layer = layer;
		this.origin = origin;
		this.detail = Objects.requireNonNull(detail, "detail");
	}

	/**
	 * Writes the issue as one line for a log: {@code syntax ru game.menu.title (ru/game/menu.json): detail}.
	 *
	 * @return the line
	 */
	@Override
	public String toString() {
		StringBuilder line = new StringBuilder(kind.getId());
		if (locale != null) line.append(' ').append(locale.toLanguageTag());
		if (key != null) line.append(' ').append(key);
		String where = origin != null ? origin : layer;
		if (where != null) line.append(" (").append(where).append(')');

		return line.append(": ").append(detail).toString();
	}

	/**
	 * How serious an issue is. The {@code semantica-cli} check fails on errors; warnings are printed.
	 */
	public enum Severity {
		/**
		 * The message is wrong and is left out, or shows something other than what was written.
		 */
		ERROR,

		/**
		 * The message works, but a reader may see a fallback.
		 */
		WARNING
	}

	/**
	 * The kinds of problems, each with its identifier in the format specification and its severity.
	 */
	@Getter
	@RequiredArgsConstructor
	public enum Kind {
		/**
		 * A file or a remote bundle could not be read, or a folder is no locale.
		 */
		SOURCE("source", Severity.ERROR),

		/**
		 * A message is not valid in the syntax.
		 */
		SYNTAX("syntax", Severity.ERROR),

		/**
		 * A reference names a key that no locale of the chain has.
		 */
		MISSING_REFERENCE("missing-reference", Severity.ERROR),

		/**
		 * References lead back to the message they started from.
		 */
		REFERENCE_CYCLE("reference-cycle", Severity.ERROR),

		/**
		 * References or nested texts go deeper than the configured depth.
		 */
		DEPTH_LIMIT("depth-limit", Severity.ERROR),

		/**
		 * A resolved message grew longer than the configured size.
		 */
		SIZE_LIMIT("size-limit", Severity.ERROR),

		/**
		 * A plural lacks a form that its language needs, such as {@code few} in Russian.
		 */
		MISSING_PLURAL_FORM("missing-plural-form", Severity.ERROR),

		/**
		 * A key exists in a translation but not in the default locale.
		 */
		UNKNOWN_KEY("unknown-key", Severity.ERROR),

		/**
		 * A translation uses other arguments than the message in the default locale.
		 */
		ARGUMENT_MISMATCH("argument-mismatch", Severity.ERROR),

		/**
		 * Markup a format does not allow, or different markup than the message in the default locale.
		 */
		MARKUP("markup", Severity.ERROR),

		/**
		 * A text breaks a rule of the place it is shown in, as a project's own check enforces it, such as the name rules
		 * of slash commands.
		 */
		PLATFORM_RULE("platform-rule", Severity.ERROR),

		/**
		 * A rendered text was longer than the place it is shown in and was cut.
		 */
		LENGTH_LIMIT("length-limit", Severity.WARNING),

		/**
		 * A key was asked for that no locale of the chain has; the key is shown instead.
		 */
		MISSING_KEY("missing-key", Severity.WARNING),

		/**
		 * A message uses an argument the text gives no value for; the placeholder is shown instead.
		 */
		MISSING_VALUE("missing-value", Severity.WARNING),

		/**
		 * A value has a type its argument cannot use, such as text for a plural.
		 */
		INVALID_VALUE("invalid-value", Severity.WARNING);

		private final String id;
		private final Severity severity;
	}
}
