package me.whereareiam.semantica.model.translation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.service.FallbackResolver;

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
			FallbackResolver fallbackResolver,
			TranslationLocale defaultLocale,
			String key
	) {
		return template;
	}
}
