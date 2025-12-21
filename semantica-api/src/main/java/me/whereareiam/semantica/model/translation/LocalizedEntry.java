package me.whereareiam.semantica.model.translation;

import lombok.Getter;
import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.service.FallbackResolver;

import java.util.Map;
import java.util.Set;

/**
 * Localized entry with translations for multiple locales.
 * <p>
 * Used for user-facing messages that vary by language.
 */
@Getter
public final class LocalizedEntry implements TranslationEntry {
	private final Map<TranslationLocale, String> translations;

	public LocalizedEntry(Map<TranslationLocale, String> translations) {
		this.translations = Map.copyOf(translations);
	}

	@Override
	public Set<TranslationLocale> getLocales() {
		return translations.keySet();
	}

	@Override
	public String resolve(
			TranslationLocale locale,
			FallbackResolver fallbackResolver,
			TranslationLocale defaultLocale,
			String key
	) {
		return fallbackResolver.resolve(translations, locale, defaultLocale, key);
	}
}
