package me.whereareiam.semantica.model.document;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.semantica.model.TextValue;
import me.whereareiam.semantica.type.MessageType;

import java.util.Map;

/**
 * Single entry from a translation file.
 */
@Getter
@Setter
public class TranslationDocumentEntry {
	private MessageType type;
	private TextValue text;
	private Map<String, TextValue> translations;

	public void setTranslations(Map<String, TextValue> translations) {
		this.translations = translations == null || translations.isEmpty() ? null : translations;
	}
}

