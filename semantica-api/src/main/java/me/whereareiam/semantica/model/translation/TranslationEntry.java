package me.whereareiam.semantica.model.translation;

import lombok.Getter;
import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.type.MessageType;

import java.util.Map;
import java.util.Set;

/**
 * Compiled translation entry ready for resolution and caching.
 * <p>
 * Supports single-text entries (templates) and multi-locale entries (messages).
 */
@Getter
public class TranslationEntry {
	private final MessageType type;
	private final String singleText;
	private final Map<TranslationLocale, String> translations;

	public TranslationEntry(MessageType type, String text) {
		this(type, text, null);
	}

	public TranslationEntry(MessageType type, String text, Map<TranslationLocale, String> translations) {
		this.type = type;
		this.singleText = text;
		this.translations = translations;
	}

	public TranslationEntry(MessageType type, Map<TranslationLocale, String> translations) {
		this(type, null, translations);
	}

	public boolean hasTranslations() {
		return translations != null && !translations.isEmpty();
	}

	public Set<TranslationLocale> getLocales() {
		return translations != null ? translations.keySet() : Set.of();
	}

	public String getText(TranslationLocale locale) {
		if (singleText != null) return singleText;
		if (translations == null) return null;
		return translations.get(locale);
	}

	public String getText(TranslationLocale locale, TranslationLocale defaultLocale, String key) {
		if (singleText != null) return singleText;
		if (translations == null) return key;

		String text = translations.get(locale);
		if (text != null) return text;

		if (locale != null)
			for (TranslationLocale available : translations.keySet())
				if (available != null && available.getLanguage().equals(locale.getLanguage())) {
					text = translations.get(available);
					if (text != null) return text;
				}

		if (defaultLocale != null) {
			text = translations.get(defaultLocale);
			if (text != null) return text;
		}

		return key;
	}
}

