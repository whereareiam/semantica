package me.whereareiam.semantica.model.translation.entry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.locale.LocaleResolver;

import java.util.Set;

/**
 * Template entry with non-localized text.
 * <p>
 * Used for configuration values, formats, and other text that doesn't vary by locale.
 */
@Getter
@RequiredArgsConstructor
public final class TemplateEntry implements TranslationEntry {
	private final String template;

	@Override
	public Set<TranslationLocale> getLocales() {
		return Set.of();
	}

	@Override
	public String resolve(
			TranslationLocale locale,
			LocaleResolver localeResolver,
			TranslationLocale defaultLocale,
			String key
	) {
		return template;
	}
}
