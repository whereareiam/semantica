package me.whereareiam.semantica.model.document;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.semantica.type.MessageType;

import java.util.Map;

/**
 * Represents a structured translation document format.
 * <p>
 * This is one way to organize translations, but not the only way.
 * When using {@link me.whereareiam.semantica.service.TranslationProvider},
 * you can use any format you prefer and convert it to the required structure.
 * <p>
 * This model is useful when you want to:
 * <ul>
 *   <li>Organize translations with a specific message type</li>
 *   <li>Include metadata for each translation</li>
 *   <li>Use a consistent structure across projects</li>
 * </ul>
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

