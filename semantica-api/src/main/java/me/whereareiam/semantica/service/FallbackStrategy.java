package me.whereareiam.semantica.service;

import me.whereareiam.semantica.TranslationLocale;

/**
 * Strategy for missing translations.
 * <p>
 * Default behavior usually returns the key; override to log, format, or supply locale-specific fallbacks.
 */
public interface FallbackStrategy {
	String getFallback(String key, TranslationLocale locale);
}

