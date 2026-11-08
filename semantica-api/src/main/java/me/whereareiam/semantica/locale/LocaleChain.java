package me.whereareiam.semantica.locale;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The order in which locales are tried for a message.
 *
 * <p>For a requested locale the chain is: the locale itself, its configured fallbacks (each followed by
 * theirs), then the locale with its country, script and variant removed one by one, down to its base
 * language, and finally the default locale. Several requested locales, such as a user's preferred locale
 * before the locale of their client, are chained in order before the default.</p>
 *
 * <pre>{@code
 * LocaleChain chain = LocaleChain.builder(Locale.ENGLISH)
 *         .fallback(Locale.forLanguageTag("pt-BR"), Locale.forLanguageTag("pt-PT"))
 *         .build();
 *
 * chain.resolve(Locale.forLanguageTag("pt-BR")); // [pt-BR, pt-PT, pt, en]
 * chain.resolve(Locale.forLanguageTag("en-US")); // [en-US, en]
 * }</pre>
 */
@Getter
@EqualsAndHashCode
public final class LocaleChain {
	private final Locale defaultLocale;
	private final Map<Locale, List<Locale>> fallbacks;

	private LocaleChain(Locale defaultLocale, Map<Locale, List<Locale>> fallbacks) {
		this.defaultLocale = defaultLocale;
		this.fallbacks = fallbacks;
	}

	/**
	 * Starts a chain.
	 *
	 * @param defaultLocale the locale every message exists in, tried last
	 * @return a new builder
	 */
	public static @NotNull Builder builder(@NotNull Locale defaultLocale) {
		return new Builder(defaultLocale);
	}

	/**
	 * Returns the locales to try for one requested locale.
	 *
	 * @param requested the locale asked for
	 * @return the locales, without repetitions, ending with the default locale
	 */
	public @NotNull List<Locale> resolve(@NotNull Locale requested) {
		return resolve(List.of(requested));
	}

	/**
	 * Returns the locales to try for several requested locales, in the order given.
	 *
	 * @param requested the locales asked for, the most wanted first
	 * @return the locales, without repetitions, ending with the default locale
	 */
	public @NotNull List<Locale> resolve(@NotNull List<Locale> requested) {
		Set<Locale> chain = new LinkedHashSet<>();
		for (Locale locale : requested) expand(Objects.requireNonNull(locale, "locale"), chain);
		expand(defaultLocale, chain);

		return List.copyOf(chain);
	}

	private void expand(Locale locale, Set<Locale> chain) {
		if (locale.equals(Locale.ROOT) || !chain.add(locale)) return;

		for (Locale fallback : fallbacks.getOrDefault(locale, List.of())) expand(fallback, chain);
		for (Locale parent : parents(locale)) expand(parent, chain);
	}

	private static List<Locale> parents(Locale locale) {
		List<Locale> parents = new ArrayList<>();
		Locale.Builder builder = new Locale.Builder().setLocale(locale).clearExtensions();
		if (!locale.getVariant().isEmpty()) parents.add(builder.setVariant("").build());
		if (!locale.getCountry().isEmpty()) parents.add(builder.setRegion("").build());
		if (!locale.getScript().isEmpty()) parents.add(builder.setScript("").build());

		return parents;
	}

	/**
	 * Builds a {@link LocaleChain}.
	 */
	public static final class Builder {
		private final Locale defaultLocale;
		private final Map<Locale, List<Locale>> fallbacks = new LinkedHashMap<>();

		private Builder(Locale defaultLocale) {
			this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale");
		}

		/**
		 * Adds fallbacks for a locale, tried after it and before its base language.
		 *
		 * @param locale    the locale
		 * @param fallbacks the locales to try next, in order
		 * @return this builder
		 */
		public @NotNull Builder fallback(@NotNull Locale locale, @NotNull Locale... fallbacks) {
			this.fallbacks.computeIfAbsent(Objects.requireNonNull(locale, "locale"), ignored -> new ArrayList<>())
					.addAll(List.of(fallbacks));
			return this;
		}

		/**
		 * Builds the chain.
		 *
		 * @return the chain
		 */
		public @NotNull LocaleChain build() {
			Map<Locale, List<Locale>> copy = new LinkedHashMap<>();
			fallbacks.forEach((locale, list) -> copy.put(locale, List.copyOf(list)));

			return new LocaleChain(defaultLocale, Collections.unmodifiableMap(copy));
		}
	}
}
