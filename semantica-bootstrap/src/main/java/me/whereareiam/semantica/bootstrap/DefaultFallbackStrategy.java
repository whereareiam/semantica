package me.whereareiam.semantica.bootstrap;

import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.service.FallbackStrategy;

public class DefaultFallbackStrategy implements FallbackStrategy {
	@Override
	public String getFallback(String key, TranslationLocale locale) {
		return key;
	}
}

