package me.whereareiam.semantica.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a translation entry with text value and optional metadata.
 */
@Getter
@AllArgsConstructor
public class Translation {
	private final TextValue text;
	private final Map<String, String> metadata;

	public Translation(TextValue text) {
		this.text = text;
		this.metadata = Collections.emptyMap();
	}
}