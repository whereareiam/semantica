package me.whereareiam.semantica.common.loader;

import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.common.processor.TextProcessor;
import me.whereareiam.semantica.common.util.LocaleUtil;
import me.whereareiam.semantica.model.TextValue;
import me.whereareiam.semantica.model.document.TranslationDocument;
import me.whereareiam.semantica.model.document.TranslationDocumentEntry;
import me.whereareiam.semantica.model.translation.TranslationEntry;
import me.whereareiam.semantica.service.TranslationLoader;
import me.whereareiam.semantica.service.TranslationRegistry;
import me.whereareiam.semantica.type.MessageType;

import java.util.HashMap;
import java.util.Map;

/**
 * Default loader: models -> registry entries.
 */
public class DefaultTranslationLoader implements TranslationLoader {
	private final TranslationRegistry registry;
	private final TextProcessor textProcessor;

	public DefaultTranslationLoader(TranslationRegistry registry) {
		this.registry = registry;
		this.textProcessor = new TextProcessor();
	}

	@Override
	public void load(String keyPrefix, TranslationDocument document) {
		MessageType fileType = document.getType();
		if (document.getItems() == null) return;

		for (Map.Entry<String, TranslationDocumentEntry> entry : document.getItems().entrySet()) {
			String entryKey = entry.getKey();
			TranslationDocumentEntry data = entry.getValue();
			String fullKey = keyPrefix == null || keyPrefix.isBlank() ? entryKey : keyPrefix + "." + entryKey;

			TranslationEntry compiled = toEntry(data, fileType, fullKey);
			registry.register(fullKey, compiled);
		}
	}

	private TranslationEntry toEntry(TranslationDocumentEntry data, MessageType fileType, String key) {
		MessageType type = data.getType();
		if (type == null) type = fileType;
		if (type == null) type = autoDetect(data);

		if (data.getTranslations() != null && !data.getTranslations().isEmpty()) {
			Map<TranslationLocale, String> translations = new HashMap<>();
			for (Map.Entry<String, TextValue> trans : data.getTranslations().entrySet()) {
				var locale = LocaleUtil.parse(trans.getKey());
				var text = textProcessor.process(trans.getValue());
				translations.put(locale, text);
			}
			return new TranslationEntry(type, translations);
		}

		if (data.getText() != null) {
			String text = textProcessor.process(data.getText());
			return new TranslationEntry(type, text);
		}

		throw new IllegalArgumentException("Translation entry must have text or translations: " + key);
	}

	private MessageType autoDetect(TranslationDocumentEntry data) {
		if (data.getTranslations() != null && !data.getTranslations().isEmpty()) return MessageType.MESSAGE;
		if (data.getText() != null) return MessageType.TEMPLATE;
		return MessageType.MESSAGE;
	}
}

