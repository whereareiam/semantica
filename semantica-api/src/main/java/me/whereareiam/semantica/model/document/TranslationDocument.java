package me.whereareiam.semantica.model.document;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.semantica.type.MessageType;

import java.util.Map;

/**
 * Parsed data from translation file.
 */
@Getter
@Setter
public class TranslationDocument {
	private MessageType type;
	private Map<String, TranslationDocumentEntry> items;

	public void setItems(Map<String, TranslationDocumentEntry> items) {
		this.items = items == null || items.isEmpty() ? null : items;
	}
}

