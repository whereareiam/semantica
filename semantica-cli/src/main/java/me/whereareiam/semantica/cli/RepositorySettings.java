package me.whereareiam.semantica.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import me.whereareiam.semantica.KeyPrefix;
import me.whereareiam.semantica.format.adventure.TagPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The settings of a translations repository, {@code semantica.json} at its root.
 *
 * <pre>{@code
 * {
 *   "defaultLocale": "en",
 *   "markup": {
 *     "game":  { "format": "adventure", "allow": ["hover"] },
 *     "realm": { "format": "adventure" }
 *   },
 *   "products": {
 *     "create":  ["game", "realm.create.common"],
 *     "bedwars": ["game", "realm.vanilla.common", "realm.vanilla.mode.bedwars"],
 *     "website": ["website"]
 *   }
 * }
 * }</pre>
 */
@Getter
final class RepositorySettings {
	static final String FILE = "semantica.json";

	private final Locale defaultLocale;
	private final List<Markup> markup;
	private final Map<String, List<KeyPrefix>> products;

	private RepositorySettings(Locale defaultLocale, List<Markup> markup, Map<String, List<KeyPrefix>> products) {
		this.defaultLocale = defaultLocale;
		this.markup = markup;
		this.products = products;
	}

	static RepositorySettings read(Path folder) throws IOException {
		Path file = folder.resolve(FILE);
		if (!Files.isRegularFile(file)) throw new SettingsException(file + " is missing");

		JsonNode root = new ObjectMapper().readTree(file.toFile());
		String defaultLocale = root.path("defaultLocale").asText("");
		if (defaultLocale.isBlank()) throw new SettingsException(FILE + " needs a defaultLocale, such as \"en\"");

		List<Markup> markup = new ArrayList<>();
		for (Map.Entry<String, JsonNode> entry : root.path("markup").properties())
			markup.add(markup(prefix(entry.getKey(), "markup"), entry.getValue()));
		markup.sort(Comparator.comparingInt((Markup each) -> each.prefix().depth()).reversed());

		Map<String, List<KeyPrefix>> products = new LinkedHashMap<>();
		for (Map.Entry<String, JsonNode> entry : root.path("products").properties()) {
			List<KeyPrefix> prefixes = new ArrayList<>();
			for (JsonNode prefix : entry.getValue()) prefixes.add(prefix(prefix.asText(), "product '" + entry.getKey() + "'"));
			if (prefixes.isEmpty()) throw new SettingsException("Product '" + entry.getKey() + "' lists no key prefixes");

			products.put(entry.getKey(), List.copyOf(prefixes));
		}
		if (products.isEmpty()) throw new SettingsException(FILE + " lists no products");

		return new RepositorySettings(Locale.forLanguageTag(defaultLocale), List.copyOf(markup), products);
	}

	/**
	 * Returns the markup of a key: that of the longest prefix that matches it.
	 *
	 * @param key the key
	 * @return the markup, or empty when the key's texts are plain
	 */
	Optional<Markup> markup(String key) {
		return markup.stream().filter(each -> each.prefix().matches(key)).findFirst();
	}

	/**
	 * Tells whether a product's bundle carries a key.
	 *
	 * @param key the key
	 * @return whether any product names a prefix of it
	 */
	boolean shipped(String key) {
		return products.values().stream().flatMap(List::stream).anyMatch(prefix -> prefix.matches(key));
	}

	private static KeyPrefix prefix(String value, String where) {
		try {
			return KeyPrefix.of(value);
		} catch (IllegalArgumentException exception) {
			throw new SettingsException("In " + where + ": " + exception.getMessage());
		}
	}

	private static Markup markup(KeyPrefix prefix, JsonNode node) {
		String format = node.path("format").asText("");
		if (!format.equals("adventure"))
			throw new SettingsException("Markup of '" + prefix + "' names the unknown format '" + format + "'; use adventure");

		TagPolicy policy = TagPolicy.defaults();
		for (JsonNode group : node.path("allow")) {
			try {
				policy = policy.allow(TagPolicy.Group.valueOf(group.asText().toUpperCase(Locale.ROOT).replace('-', '_')));
			} catch (IllegalArgumentException exception) {
				throw new SettingsException("Markup of '" + prefix + "' allows the unknown tag group '" + group.asText() + "'");
			}
		}

		return new Markup(prefix, format, policy);
	}

	/**
	 * The text format of the keys below a prefix.
	 *
	 * @param prefix the prefix
	 * @param format the text format, {@code adventure}
	 * @param policy the MiniMessage tags its texts may use
	 */
	record Markup(KeyPrefix prefix, String format, TagPolicy policy) {
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
