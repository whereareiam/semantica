package me.whereareiam.semantica.model;

import lombok.Getter;
import me.whereareiam.semantica.translation.base.TranslationLocale;

import java.util.Map;
import java.util.Objects;

public final class CacheKey {
	@Getter
	private final String key;
	private final TranslationLocale locale;
	private final Map<String, Object> placeholders;

	public CacheKey(String key, TranslationLocale locale, Map<String, Object> placeholders) {
		this.key = key;
		this.locale = locale;
		this.placeholders = placeholders == null ? Map.of() : placeholders;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof CacheKey cacheKey)) return false;
		return Objects.equals(key, cacheKey.key) &&
				Objects.equals(localeKey(locale), localeKey(cacheKey.locale)) &&
				Objects.equals(placeholders, cacheKey.placeholders);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, localeKey(locale), placeholders);
	}

	private String localeKey(TranslationLocale locale) {
		return locale != null ? locale.getLanguage() + "|" + locale.getCountry() + "|" + locale.getVariant() : "";
	}
}

