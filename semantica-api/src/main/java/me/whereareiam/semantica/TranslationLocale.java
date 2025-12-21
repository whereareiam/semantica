package me.whereareiam.semantica;

/**
 * Platform-agnostic locale abstraction.
 * <p>
 * Semantica never depends on {@link java.util.Locale}; instead, callers may:
 * <ul>
 *   <li>Wrap a {@link java.util.Locale} (see {@code SemanticLocale.wrap(Locale)}), or</li>
 *   <li>Provide a custom implementation (e.g., Discord locale codes)</li>
 * </ul>
 */
public interface TranslationLocale {
	/**
	 * Language code of the locale (e.g., {@code en} or {@code en_US}).
	 */
	String getLanguage();

	/**
	 * Country/region code (may be empty).
	 */
	default String getCountry() {
		return "";
	}

	/**
	 * Variant code (may be empty).
	 */
	default String getVariant() {
		return "";
	}
}

