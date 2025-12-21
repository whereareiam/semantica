package me.whereareiam.semantica;

import me.whereareiam.semantica.translation.base.TranslationLocale;

/**
 * Strategy for missing translations.
 * <p>
 * Default behavior usually returns the key; override to log, format, or supply locale-specific fallbacks.
 */
public interface FallbackStrategy {
	String getFallback(String key, TranslationLocale locale);
}

