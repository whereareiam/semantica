package me.whereareiam.semantica.source;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.whereareiam.semantica.issue.Issue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Where translations come from: files, a jar's resources, bundles over HTTP, a database.
 *
 * <p>A source reads the messages' sources as text, with where each was written; Semantica parses and checks
 * them. A source that knows when its translations change tells Semantica through {@link #watch(Runnable)},
 * and Semantica loads it again.</p>
 */
public interface Source {
	/**
	 * Names the source in issues and logs, such as {@code file:plugins/Intercept/languages}.
	 *
	 * @return the name
	 */
	@NotNull String getName();

	/**
	 * Reads every translation of the source.
	 *
	 * <p>A problem with a single file or entry is put into the content as an issue, so the rest still loads.
	 * A problem that leaves nothing to read, such as an unreachable server, is thrown; Semantica then keeps
	 * what the source gave last time.</p>
	 *
	 * @return the translations
	 * @throws IOException when the source cannot be read at all
	 */
	@NotNull Content load() throws IOException;

	/**
	 * Starts telling when the translations change. The default source never changes.
	 *
	 * @param onChange called, on any thread, after the source changed; Semantica then calls {@link #load()}
	 * @return closes the watch
	 */
	default @NotNull AutoCloseable watch(@NotNull Runnable onChange) {
		return () -> {
		};
	}

	/**
	 * The source of one message and where it was written.
	 *
	 * @param text   the message as written
	 * @param origin where it was written, such as {@code ru/game/menu.json}, or {@code null}
	 */
	record Entry(@NotNull String text, @Nullable String origin) {
		public Entry {
			Objects.requireNonNull(text, "text");
		}
	}

	/**
	 * What a source read: the messages per locale and key, and the issues of entries it could not read.
	 */
	@Getter
	@EqualsAndHashCode
	final class Content {
		private static final Content EMPTY = new Content(Map.of(), List.of());

		private final Map<Locale, Map<String, Entry>> entries;
		private final List<Issue> issues;

		private Content(Map<Locale, Map<String, Entry>> entries, List<Issue> issues) {
			this.entries = entries;
			this.issues = issues;
		}

		/**
		 * Returns content without messages.
		 *
		 * @return the empty content
		 */
		public static @NotNull Content empty() {
			return EMPTY;
		}

		/**
		 * Starts building content.
		 *
		 * @return a new builder
		 */
		public static @NotNull Builder builder() {
			return new Builder();
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
		 * Returns the messages of a locale.
		 *
		 * @param locale the locale
		 * @return messages by key, empty when the locale has none
		 */
		public @NotNull Map<String, Entry> entries(@NotNull Locale locale) {
			return entries.getOrDefault(locale, Map.of());
		}

		/**
		 * Builds {@link Content}. A key put twice in one locale keeps the last entry.
		 */
		@NoArgsConstructor(access = AccessLevel.PRIVATE)
		public static final class Builder {
			private final Map<Locale, Map<String, Entry>> entries = new LinkedHashMap<>();
			private final List<Issue> issues = new ArrayList<>();

			/**
			 * Adds a message. A message that is the same in every language, such as one made only of references,
			 * belongs to {@link Locale#ROOT}, which every lookup tries last.
			 *
			 * @param locale its locale, or {@link Locale#ROOT}
			 * @param key    its key
			 * @param text   its source
			 * @param origin where it was written, or {@code null}
			 * @return this builder
			 */
			public @NotNull Builder put(@NotNull Locale locale, @NotNull String key, @NotNull String text, @Nullable String origin) {
				Objects.requireNonNull(locale, "locale");
				Objects.requireNonNull(key, "key");
				entries.computeIfAbsent(locale, ignored -> new LinkedHashMap<>()).put(key, new Entry(text, origin));

				return this;
			}

			/**
			 * Adds a problem with an entry that could not be read.
			 *
			 * @param issue the issue
			 * @return this builder
			 */
			public @NotNull Builder issue(@NotNull Issue issue) {
				issues.add(Objects.requireNonNull(issue, "issue"));
				return this;
			}

			/**
			 * Builds the content.
			 *
			 * @return the content
			 */
			public @NotNull Content build() {
				Map<Locale, Map<String, Entry>> copy = new LinkedHashMap<>();
				entries.forEach((locale, byKey) -> copy.put(locale, Collections.unmodifiableMap(new LinkedHashMap<>(byKey))));

				return new Content(Collections.unmodifiableMap(copy), List.copyOf(issues));
			}
		}
	}
}
