package me.whereareiam.semantica.catalog;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.message.ParsedMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An immutable snapshot of every translation of a Semantica instance: the merged messages per locale and key,
 * already parsed, and the issues found while building it.
 *
 * <p>Semantica builds a new catalog whenever a layer is added, removed or changes, and replaces the old one
 * at once; a reader holding a catalog keeps a consistent view. The revision grows by one with each catalog.</p>
 */
@Getter
@EqualsAndHashCode
public final class Catalog {
	private final long revision;
	private final Locale defaultLocale;
	private final Map<Locale, Map<String, Entry>> entries;
	private final List<Issue> issues;

	/**
	 * Creates a catalog.
	 *
	 * @param revision      its revision
	 * @param defaultLocale the locale every key should exist in
	 * @param entries       the usable messages per locale and key; messages with errors are left out
	 * @param issues        the issues found while building it
	 */
	public Catalog(
			long revision,
			@NotNull Locale defaultLocale,
			@NotNull Map<Locale, Map<String, Entry>> entries,
			@NotNull List<Issue> issues
	) {
		this.revision = revision;
		this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale");

		Map<Locale, Map<String, Entry>> copy = new LinkedHashMap<>();
		entries.forEach((locale, byKey) -> copy.put(locale, Collections.unmodifiableMap(new LinkedHashMap<>(byKey))));
		this.entries = Collections.unmodifiableMap(copy);
		this.issues = List.copyOf(issues);
	}

	/**
	 * Returns an empty catalog.
	 *
	 * @param defaultLocale the default locale
	 * @return a catalog of revision 0 without messages
	 */
	public static @NotNull Catalog empty(@NotNull Locale defaultLocale) {
		return new Catalog(0, defaultLocale, Map.of(), List.of());
	}

	/**
	 * Returns the locales that have messages.
	 *
	 * @return the locales
	 */
	public @NotNull Set<Locale> getLocales() {
		return entries.keySet();
	}

	/**
	 * Returns the keys of a locale.
	 *
	 * @param locale the locale
	 * @return the keys, empty when the locale has none
	 */
	public @NotNull Set<String> keys(@NotNull Locale locale) {
		return entries.getOrDefault(locale, Map.of()).keySet();
	}

	/**
	 * Returns the messages of a locale.
	 *
	 * @param locale the locale
	 * @return the messages by key, empty when the locale has none
	 */
	public @NotNull Map<String, Entry> entries(@NotNull Locale locale) {
		return entries.getOrDefault(locale, Map.of());
	}

	/**
	 * Finds the message of a key in exactly this locale, without falling back.
	 *
	 * @param locale the locale
	 * @param key    the key
	 * @return the message, or empty
	 */
	public @NotNull Optional<Entry> entry(@NotNull Locale locale, @NotNull String key) {
		return Optional.ofNullable(entries.getOrDefault(locale, Map.of()).get(key));
	}

	/**
	 * A message in a catalog.
	 *
	 * @param key     its key
	 * @param locale  its locale
	 * @param source  the message as written
	 * @param message the message as parsed
	 * @param layer   the name of the layer it came from
	 * @param origin  where it was written, or {@code null}
	 */
	public record Entry(
			@NotNull String key,
			@NotNull Locale locale,
			@NotNull String source,
			@NotNull ParsedMessage message,
			@NotNull String layer,
			@Nullable String origin
	) {
		public Entry {
			Objects.requireNonNull(key, "key");
			Objects.requireNonNull(locale, "locale");
			Objects.requireNonNull(source, "source");
			Objects.requireNonNull(message, "message");
			Objects.requireNonNull(layer, "layer");
		}
	}
}
