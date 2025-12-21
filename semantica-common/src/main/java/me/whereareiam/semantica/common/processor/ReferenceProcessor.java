package me.whereareiam.semantica.common.processor;

import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.common.DefaultLocaleResolver;
import me.whereareiam.semantica.common.TagTokens;
import me.whereareiam.semantica.model.translation.entry.TranslationEntry;
import me.whereareiam.semantica.locale.LocaleResolver;
import me.whereareiam.semantica.translation.base.TranslationProcessor;
import me.whereareiam.semantica.translation.TranslationRegistry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves references <ref:key> or <ref:key param='value'>.
 * Combines functionality of message references and templates.
 */
public class ReferenceProcessor implements TranslationProcessor {
	private final TranslationRegistry registry;
	private final TagTokens tags;

	private final Pattern referencePattern;
	private final Pattern paramPattern = Pattern.compile("([a-zA-Z0-9_\\-]+)='([^']*)'");
	private final int maxDepth;

	private final LocaleResolver localeResolver = new DefaultLocaleResolver();

	public ReferenceProcessor(TranslationRegistry registry, TagTokens tags, int maxDepth) {
		this.registry = registry;
		this.tags = tags;
		this.maxDepth = maxDepth;
		String quotedOpen = Pattern.quote(tags.openDelimiter);
		String quotedClose = Pattern.quote(tags.closeDelimiter);
		String quotedPrefix = Pattern.quote(tags.referenceTag.substring(tags.openDelimiter.length()));
		this.referencePattern = Pattern.compile(quotedOpen + quotedPrefix + "([a-zA-Z0-9_.\\-]+)(?:\\s+((?:[^" + Pattern.quote(tags.closeDelimiter.substring(0, 1)) + "']|'[^']*')*))?" + quotedClose, Pattern.DOTALL);
	}

	@Override
	public String process(String text, TranslationLocale locale, Map<String, Object> placeholders) {
		if (text == null || text.isEmpty()) return text;
		if (!text.contains(tags.referenceTag)) return text;
		
		if (maxDepth < 0) {
			return resolveUntilStable(text, locale);
		}

		return resolve(text, locale, new HashSet<>(), 0);
	}

	private String resolveUntilStable(String text, TranslationLocale locale) {
		String current = text;
		String previous;
		Set<String> path = new HashSet<>();
		
		do {
			previous = current;
			current = resolveSinglePass(current, locale, path);
		} while (!current.equals(previous));
		
		return current;
	}

	private String resolve(String text, TranslationLocale locale, Set<String> path, int depth) {
		if (depth >= maxDepth) return text;
		return resolveSinglePass(text, locale, path, depth);
	}

	private String resolveSinglePass(String text, TranslationLocale locale, Set<String> path) {
		return resolveSinglePass(text, locale, path, -1);
	}

	private String resolveSinglePass(String text, TranslationLocale locale, Set<String> path, int depth) {
		Matcher matcher = referencePattern.matcher(text);
		StringBuilder result = new StringBuilder();

		while (matcher.find()) {
			String key = matcher.group(1);
			String paramsString = matcher.group(2);

			if (path.contains(key)) {
				matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
				continue;
			}

			TranslationEntry entry = registry.get(key);
			if (entry == null) {
				matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
				continue;
			}

			String entryText = entry.resolve(locale, localeResolver, null, key);
			if (entryText == null || entryText.equals(key)) {
				matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
				continue;
			}

			if (paramsString != null && !paramsString.isBlank()) {
				Map<String, String> params = parseParams(paramsString);
				for (Map.Entry<String, String> param : params.entrySet()) {
					String placeholder = tags.openDelimiter + tags.placeholderTag.substring(tags.openDelimiter.length()) + param.getKey() + tags.closeDelimiter;
					entryText = entryText.replace(placeholder, param.getValue());
				}
			}

			Set<String> newPath = new HashSet<>(path);
			newPath.add(key);
			
			String resolved;
			if (depth >= 0) {
				resolved = resolve(entryText, locale, newPath, depth + 1);
			} else {
				resolved = entryText;
			}
			
			matcher.appendReplacement(result, Matcher.quoteReplacement(resolved));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private Map<String, String> parseParams(String paramsString) {
		Map<String, String> params = new HashMap<>();
		Matcher paramMatcher = paramPattern.matcher(paramsString);
		while (paramMatcher.find()) {
			params.put(paramMatcher.group(1), paramMatcher.group(2));
		}
		return params;
	}

	@Override
	public int getPriority() {
		return 125;
	}

	@Override
	public String getName() {
		return "references";
	}
}
