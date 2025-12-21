package me.whereareiam.semantica.common.processor;

import me.whereareiam.semantica.common.service.DefaultFallbackResolver;
import me.whereareiam.semantica.common.util.TagTokens;
import me.whereareiam.semantica.model.translation.TranslationEntry;
import me.whereareiam.semantica.service.FallbackResolver;
import me.whereareiam.semantica.service.TranslationRegistry;
import me.whereareiam.semantica.service.TranslationProcessor;
import me.whereareiam.semantica.TranslationLocale;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands templates <tpl:key param='value'>.
 */
public class TemplateProcessor implements TranslationProcessor {
	private final TranslationRegistry registry;
	private final TagTokens tags;
	private final Pattern templatePattern;
	private final Pattern paramPattern = Pattern.compile("([a-zA-Z0-9_\\-]+)='([^']*)'");
	private final FallbackResolver fallbackResolver = new DefaultFallbackResolver();

	public TemplateProcessor(TranslationRegistry registry, TagTokens tags) {
		this.registry = registry;
		this.tags = tags;
		this.templatePattern = Pattern.compile("<" + Pattern.quote(tags.templateTag.substring(1)) + "([a-zA-Z0-9_.\\-]+)(?:\\s+((?:[^>']|'[^']*')*))?>", Pattern.DOTALL);
	}

	@Override
	public String process(String text, TranslationLocale locale, Map<String, Object> placeholders) {
		if (text == null || text.isEmpty()) return text;
		if (!text.contains(tags.templateTag)) return text;

		Matcher matcher = templatePattern.matcher(text);
		StringBuilder result = new StringBuilder();

		while (matcher.find()) {
			String key = matcher.group(1);
			String paramsString = matcher.group(2);

			TranslationEntry entry = registry.get(key);
			if (entry == null) {
				matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
				continue;
			}

			String templateText = entry.resolve(locale, fallbackResolver, null, key);
			if (templateText == null || templateText.equals(key)) {
				matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
				continue;
			}

			if (paramsString != null && !paramsString.isBlank()) {
				Map<String, String> params = parseParams(paramsString);
				for (Map.Entry<String, String> param : params.entrySet()) {
					String placeholder = "<" + tags.placeholderTag.substring(1) + param.getKey() + ">";
					templateText = templateText.replace(placeholder, param.getValue());
				}
			}

			matcher.appendReplacement(result, Matcher.quoteReplacement(templateText));
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
		return 100;
	}

	@Override
	public String getName() {
		return "templates";
	}
}

