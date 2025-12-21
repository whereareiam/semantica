package me.whereareiam.semantica.common.processor;

import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.common.TagTokens;
import me.whereareiam.semantica.translation.base.TranslationProcessor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderProcessor implements TranslationProcessor {
	private final Pattern pattern;
	private final TagTokens tags;

	public PlaceholderProcessor(TagTokens tags) {
		this.tags = tags;
		this.pattern = Pattern.compile("<" + Pattern.quote(tags.placeholderTag.substring(1)) + "([a-zA-Z0-9_\\-]+)>");
	}

	@Override
	public String process(String text, TranslationLocale locale, Map<String, Object> placeholders) {
		if (text == null || text.isEmpty()) return text;
		if (!text.contains(tags.placeholderTag)) return text;

		Matcher matcher = pattern.matcher(text);
		StringBuilder result = new StringBuilder();

		while (matcher.find()) {
			String name = matcher.group(1);
			String replacement;
			if (placeholders != null && placeholders.containsKey(name)) {
				Object value = placeholders.get(name);
				replacement = value != null ? Matcher.quoteReplacement(String.valueOf(value)) : "";
			} else {
				replacement = matcher.group(0);
			}

			matcher.appendReplacement(result, replacement);
		}

		matcher.appendTail(result);

		return result.toString();
	}

	@Override
	public int getPriority() {
		return 400;
	}

	@Override
	public String getName() {
		return "placeholders";
	}
}

