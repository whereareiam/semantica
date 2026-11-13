package me.whereareiam.semantica.source.file;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.source.Source;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Turns the files of a translations folder into content: one folder per locale, and the path of a file inside
 * it as the prefix of its keys.
 */
final class TranslationFiles {
	private static final String[] TREE_EXTENSIONS = {".json", ".yml", ".yaml"};

	private final String sourceName;
	private final String keyPrefix;
	private final Function<InputStream, JsonNode> treeReader;
	private final Source.Content.Builder content = Source.Content.builder();
	private final Map<Locale, Map<String, String>> origins = new HashMap<>();

	/**
	 * @param sourceName the source's name, for issues
	 * @param keyPrefix  segments put before every key, or empty for none
	 * @param treeReader reads JSON and YAML files
	 */
	TranslationFiles(String sourceName, String keyPrefix, Function<InputStream, JsonNode> treeReader) {
		this.sourceName = sourceName;
		this.keyPrefix = keyPrefix.isEmpty() ? "" : keyPrefix + ".";
		this.treeReader = treeReader;
	}

	/**
	 * Returns the locale a top folder stands for, or reports it.
	 *
	 * @param folder the folder's name
	 * @return the locale, or {@code null} when the folder is no locale
	 */
	Locale locale(String folder) {
		Locale locale = Locale.forLanguageTag(folder);
		boolean valid = !locale.getLanguage().isEmpty()
				&& locale.getLanguage().length() <= 3
				&& locale.toLanguageTag().equalsIgnoreCase(folder);
		if (valid) return locale;

		issue(null, null, folder, "'" + folder + "' is no locale; name locale folders by language tag, such as en or pt-BR");
		return null;
	}

	/**
	 * Reads one file.
	 *
	 * @param locale   the locale of its folder
	 * @param relative its path inside the locale folder, with {@code /} between folders
	 * @param origin   where it is, for issues
	 * @param stream   opens the file
	 */
	void read(Locale locale, String relative, String origin, StreamOpener stream) {
		String lower = relative.toLowerCase(Locale.ROOT);
		String extension = extension(lower);
		if (extension == null) {
			issue(null, locale, origin, "Unsupported file; use .json, .yml, .yaml or .properties");
			return;
		}

		String prefix = keyPrefix + relative.substring(0, relative.length() - extension.length()).replace('/', '.');
		try (InputStream input = stream.open()) {
			if (extension.equals(".properties")) properties(locale, prefix, origin, input);
			else tree(locale, prefix, origin, treeReader.apply(input));
		} catch (IOException | RuntimeException exception) {
			issue(null, locale, origin, "Cannot read: " + message(exception));
		}
	}

	Source.Content build() {
		return content.build();
	}

	private void properties(Locale locale, String prefix, String origin, InputStream input) throws IOException {
		Properties properties = new Properties();
		properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));

		new TreeMap<>(properties).forEach((key, value) -> put(locale, prefix + "." + key, value.toString(), origin));
	}

	private void tree(Locale locale, String prefix, String origin, JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) return;

		if (node.isObject()) {
			for (Map.Entry<String, JsonNode> field : node.properties())
				tree(locale, prefix + "." + field.getKey(), origin, field.getValue());
			return;
		}

		if (node.isArray()) {
			StringBuilder lines = new StringBuilder();
			for (JsonNode line : node) {
				if (!line.isTextual()) {
					issue(prefix, locale, origin, "A list holds lines of text only");
					return;
				}
				if (!lines.isEmpty()) lines.append('\n');
				lines.append(line.textValue());
			}
			put(locale, prefix, lines.toString(), origin);
			return;
		}

		if (!node.isTextual()) {
			issue(prefix, locale, origin, "A message is text; write it in quotes");
			return;
		}

		put(locale, prefix, node.textValue(), origin);
	}

	private void put(Locale locale, String key, String text, String origin) {
		String previous = origins.computeIfAbsent(locale, ignored -> new HashMap<>()).put(key, origin);
		if (previous != null)
			issue(key, locale, origin, "Also written in " + previous + "; this one is used");

		content.put(locale, key, text, origin);
	}

	private void issue(String key, Locale locale, String origin, String detail) {
		content.issue(new Issue(Issue.Kind.SOURCE, key, locale, sourceName, origin, detail));
	}

	private static String extension(String lower) {
		if (lower.endsWith(".properties")) return ".properties";
		for (String extension : TREE_EXTENSIONS) if (lower.endsWith(extension)) return extension;

		return null;
	}

	private static String message(Throwable exception) {
		Throwable cause = exception;
		while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();

		return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
	}

	@FunctionalInterface
	interface StreamOpener {
		InputStream open() throws IOException;
	}
}
