package me.whereareiam.semantica.common.util;

import me.whereareiam.semantica.model.SemanticLocale;
import me.whereareiam.semantica.TranslationLocale;

import java.util.Locale;

public final class LocaleUtil {
	private LocaleUtil() {
	}

	public static TranslationLocale parse(String raw) {
		if (raw == null || raw.isBlank()) return SemanticLocale.wrap(Locale.getDefault());

		if (raw.contains("_")) {
			String[] parts = raw.split("_", 3);
			if (parts.length == 2) return SemanticLocale.wrap(new Locale(parts[0], parts[1]));
			if (parts.length == 3) return SemanticLocale.wrap(new Locale(parts[0], parts[1], parts[2]));
		}

		return SemanticLocale.wrap(new Locale(raw));
	}
}

