package me.whereareiam.semantica.translation.base;

import java.util.Map;

/**
 * Extensible text processor executed in the resolution pipeline.
 * <p>
 * Lower {@link #getPriority()} runs earlier; processors should be pure and side-effect free.
 */
public interface TranslationProcessor {
	/**
	 * @param text         input text
	 * @param locale       locale
	 * @param context      placeholders/context map
	 * @return processed text
	 */
	String process(String text, TranslationLocale locale, Map<String, Object> context);

	/**
	 * Lower value = earlier execution.
	 */
	int getPriority();

	/**
	 * Name for debugging.
	 */
	String getName();
}

