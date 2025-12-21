package me.whereareiam.semantica.service;

import me.whereareiam.semantica.model.document.TranslationDocument;

/**
 * Converts parsed translation models into registry entries.
 * <p>
 * Typical flow: user loads YAML/JSON into {@code TranslationDocument} (e.g., via Configura),
 * then passes the document here with an optional key prefix.
 */
public interface TranslationLoader {
	void load(String keyPrefix, TranslationDocument document);
}

