package me.whereareiam.semantica.predefined;

import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.FallbackStrategy;

public class DefaultFallbackStrategy implements FallbackStrategy {
	@Override
	public String getFallback(String key, TranslationLocale locale) {
		return key;
	}
}

