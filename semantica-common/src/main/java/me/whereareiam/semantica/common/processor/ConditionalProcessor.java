package me.whereareiam.semantica.common.processor;

import me.whereareiam.semantica.common.util.TagTokens;
import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.service.TranslationProcessor;

import java.util.Map;

/**
 * Evaluates conditional blocks: <if condition>...<else>...</if>
 */
public class ConditionalProcessor implements TranslationProcessor {
	private final TagTokens tags;

	public ConditionalProcessor(TagTokens tags) {
		this.tags = tags;
	}

	@Override
	public String process(String text, TranslationLocale locale, Map<String, Object> placeholders) {
		if (text == null || text.isEmpty()) return text;
		if (!text.contains(tags.conditionalIfTag)) return text;

		String result = text;
		int safety = 0;
		while (result.contains(tags.conditionalIfTag) && safety++ < 10) {
			String next = processOnce(result, placeholders);
			if (next.equals(result)) break;
			result = next;
		}
		return result;
	}

	private String processOnce(String text, Map<String, Object> placeholders) {
		int ifStart = text.indexOf(tags.conditionalIfTag);
		if (ifStart == -1) return text;

		int conditionEnd = text.indexOf('>', ifStart);
		if (conditionEnd == -1) return text;

		String condition = text.substring(ifStart + tags.conditionalIfTag.length(), conditionEnd).trim();

		int contentStart = conditionEnd + 1;
		int elsePos = -1;
		int depth = 1;
		int pos = contentStart;
		while (pos < text.length() && depth > 0) {
			if (text.startsWith(tags.conditionalIfTag, pos)) {
				depth++;
				pos += tags.conditionalIfTag.length();
				continue;
			}
			if (text.startsWith(tags.conditionalElseTag, pos) && depth == 1 && elsePos == -1) {
				elsePos = pos;
				pos += tags.conditionalElseTag.length();
				continue;
			}
			if (text.startsWith(tags.conditionalIfCloseTag, pos)) {
				depth--;
				if (depth == 0) break;
				pos += tags.conditionalIfCloseTag.length();
				continue;
			}
			pos++;
		}
		if (depth != 0) return text;
		int ifEnd = pos;

		String ifBlock;
		String elseBlock = null;
		if (elsePos != -1) {
			ifBlock = text.substring(contentStart, elsePos);
			elseBlock = text.substring(elsePos + tags.conditionalElseTag.length(), ifEnd);
		} else {
			ifBlock = text.substring(contentStart, ifEnd);
		}

		boolean result = evaluate(condition, placeholders);
		String replacement = result ? ifBlock : (elseBlock != null ? elseBlock : "");

		return text.substring(0, ifStart) + replacement + text.substring(ifEnd + tags.conditionalIfCloseTag.length());
	}

	private boolean evaluate(String condition, Map<String, Object> placeholders) {
		if (condition.isEmpty()) return false;

		if (condition.contains("==")) {
			String[] parts = condition.split("==", 2);
			String left = parts[0].trim();
			String right = parts[1].trim();
			Object value = placeholders != null ? placeholders.get(left) : null;
			return value != null && right.equals(String.valueOf(value));
		}
		if (condition.contains("!=")) {
			String[] parts = condition.split("!=", 2);
			String left = parts[0].trim();
			String right = parts[1].trim();
			Object value = placeholders != null ? placeholders.get(left) : null;
			return value == null || !right.equals(String.valueOf(value));
		}

		// truthy check
		Object value = placeholders != null ? placeholders.get(condition) : null;
		if (value == null) return false;
		String str = String.valueOf(value);
		return !(str.equalsIgnoreCase("false") || str.equals("0") || str.isBlank());
	}

	@Override
	public int getPriority() {
		return 300;
	}

	@Override
	public String getName() {
		return "conditionals";
	}
}

