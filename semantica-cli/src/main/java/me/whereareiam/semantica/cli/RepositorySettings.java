package me.whereareiam.semantica.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import me.whereareiam.semantica.format.adventure.TagPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The settings of a translations repository, {@code semantica.json} at its root.
 *
 * <pre>{@code
 * {
 *   "defaultLocale": "en",
 *   "namespaces": {
 *     "game":    { "format": "adventure", "allow": ["hover"] },
 *     "discord": {},
 *     "common":  {}
 *   },
 *   "products": {
 *     "intercept": ["game", "common"],
 *     "yui":       ["discord", "common"]
 *   }
 * }
 * }</pre>
 */
@Getter
final class RepositorySettings {
	static final String FILE = "semantica.json";

	private final Locale defaultLocale;
	private final Map<String, Namespace> namespaces;
	private final Map<String, List<String>> products;

	private RepositorySettings(Locale defaultLocale, Map<String, Namespace> namespaces, Map<String, List<String>> products) {
		this.defaultLocale = defaultLocale;
		this.namespaces = namespaces;
		this.products = products;
	}

	static RepositorySettings read(Path folder) throws IOException {
		Path file = folder.resolve(FILE);
		if (!Files.isRegularFile(file)) throw new SettingsException(file + " is missing");

		JsonNode root = new ObjectMapper().readTree(file.toFile());
		String defaultLocale = root.path("defaultLocale").asText("");
		if (defaultLocale.isBlank()) throw new SettingsException(FILE + " needs a defaultLocale, such as \"en\"");

		Map<String, Namespace> namespaces = new LinkedHashMap<>();
		for (Map.Entry<String, JsonNode> entry : root.path("namespaces").properties())
			namespaces.put(entry.getKey(), namespace(entry.getKey(), entry.getValue()));
		if (namespaces.isEmpty()) throw new SettingsException(FILE + " lists no namespaces");

		Map<String, List<String>> products = new LinkedHashMap<>();
		for (Map.Entry<String, JsonNode> entry : root.path("products").properties()) {
			List<String> list = new ArrayList<>();
			for (JsonNode name : entry.getValue()) {
				if (!namespaces.containsKey(name.asText()))
					throw new SettingsException("Product '" + entry.getKey() + "' names the unknown namespace '" + name.asText() + "'");
				list.add(name.asText());
			}
			products.put(entry.getKey(), List.copyOf(list));
		}

		return new RepositorySettings(Locale.forLanguageTag(defaultLocale), namespaces, products);
	}

	private static Namespace namespace(String name, JsonNode node) {
		String format = node.path("format").asText("");
		if (!format.isEmpty() && !format.equals("adventure"))
			throw new SettingsException("Namespace '" + name + "' names the unknown format '" + format + "'; use adventure, or leave it out for plain text");

		TagPolicy policy = TagPolicy.defaults();
		for (JsonNode group : node.path("allow")) {
			try {
				policy = policy.allow(TagPolicy.Group.valueOf(group.asText().toUpperCase(Locale.ROOT).replace('-', '_')));
			} catch (IllegalArgumentException exception) {
				throw new SettingsException("Namespace '" + name + "' allows the unknown tag group '" + group.asText() + "'");
			}
		}

		return new Namespace(format.isEmpty() ? null : format, policy);
	}

	/**
	 * One namespace: the first segment of its keys.
	 *
	 * @param format the text format of its messages, {@code adventure}, or {@code null} for plain text
	 * @param policy the MiniMessage tags it may use when its format is {@code adventure}
	 */
	record Namespace(String format, TagPolicy policy) {
	}

	/**
	 * Thrown when {@code semantica.json} is wrong.
	 */
	static final class SettingsException extends RuntimeException {
		SettingsException(String message) {
			super(message);
		}
	}
}
