package me.whereareiam.semantica.common.processor;

import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.common.service.DefaultFallbackResolver;
import me.whereareiam.semantica.common.util.TagTokens;
import me.whereareiam.semantica.model.translation.TranslationEntry;
import me.whereareiam.semantica.service.FallbackResolver;
import me.whereareiam.semantica.service.TranslationProcessor;
import me.whereareiam.semantica.service.TranslationRegistry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves message references <m:key>.
 */
public class MessageReferenceProcessor implements TranslationProcessor {
	private static final int MAX_DEPTH = 10;
	private final TranslationRegistry registry;
	private final TagTokens tags;
	private final Pattern pattern;

	private final FallbackResolver fallbackResolver = new DefaultFallbackResolver();

	public MessageReferenceProcessor(TranslationRegistry registry, TagTokens tags) {
		this.registry = registry;
		this.tags = tags;
		this.pattern = Pattern.compile("<" + Pattern.quote(tags.messageRefTag.substring(1)) + "([a-zA-Z0-9_.\\-]+)>");
	}

	@Override
	public String process(String text, TranslationLocale locale, Map<String, Object> placeholders) {
		if (text == null || text.isEmpty()) return text;
		if (!text.contains(tags.messageRefTag)) return text;
		return resolve(text, locale, new HashSet<>(), 0);
	}

	private String resolve(String text, TranslationLocale locale, Set<String> path, int depth) {
		if (depth >= MAX_DEPTH) return text;

		Matcher matcher = pattern.matcher(text);
		StringBuilder result = new StringBuilder();

		while (matcher.find()) {
			String key = matcher.group(1);
			if (path.contains(key)) {
				matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
				continue;
			}

			TranslationEntry entry = registry.get(key);
			if (entry == null) {
				matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
				continue;
			}

			String entryText = entry.resolve(locale, fallbackResolver, null, key);
			if (entryText == null || entryText.equals(key)) {
				matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
				continue;
			}

			Set<String> newPath = new HashSet<>(path);
			newPath.add(key);
			String resolved = resolve(entryText, locale, newPath, depth + 1);
			matcher.appendReplacement(result, Matcher.quoteReplacement(resolved));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	@Override
	public int getPriority() {
		return 150;
	}

	@Override
	public String getName() {
		return "references";
	}
}

