package me.whereareiam.semantica.source.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The index of an organisation's bundles, {@code manifest.json}: its revision, its default locale and the
 * SHA-256 of every bundle, by product and locale.
 *
 * <pre>{@code
 * {
 *   "format": 1,
 *   "revision": "4f2c1a9",
 *   "defaultLocale": "en",
 *   "bundles": {
 *     "intercept": { "en": "9f86d0...", "ru": "2c26b4..." }
 *   }
 * }
 * }</pre>
 *
 * <p>A client polls the manifest and downloads only the bundles whose hash changed. The {@code semantica-cli}
 * writes it next to the bundles.</p>
 */
@Getter
@EqualsAndHashCode
public final class Manifest {
	/**
	 * The version of the manifest's layout this class reads and writes.
	 */
	public static final int FORMAT = 1;

	private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	private final String revision;
	private final Locale defaultLocale;
	private final Map<String, Map<Locale, String>> bundles;

	/**
	 * Creates a manifest.
	 *
	 * @param revision      changes whenever any bundle changes, such as the commit the bundles were built from
	 * @param defaultLocale the locale every key exists in
	 * @param bundles       the SHA-256 of each bundle's bytes, as lowercase hex, by product and locale
	 */
	public Manifest(@NotNull String revision, @NotNull Locale defaultLocale, @NotNull Map<String, Map<Locale, String>> bundles) {
		this.revision = Objects.requireNonNull(revision, "revision");
		this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale");

		Map<String, Map<Locale, String>> copy = new TreeMap<>();
		bundles.forEach((product, byLocale) -> {
			Map<Locale, String> sorted = new TreeMap<>((left, right) -> left.toLanguageTag().compareTo(right.toLanguageTag()));
			sorted.putAll(byLocale);
			copy.put(product, Collections.unmodifiableMap(sorted));
		});
		this.bundles = Collections.unmodifiableMap(copy);
	}

	/**
	 * Reads a manifest.
	 *
	 * @param bytes the content of {@code manifest.json}
	 * @return the manifest
	 * @throws IOException when it is no manifest, or one of a newer format
	 */
	public static @NotNull Manifest read(byte @NotNull [] bytes) throws IOException {
		JsonNode root = MAPPER.readTree(bytes);
		if (root == null || !root.isObject()) throw new IOException("A manifest is a JSON object");

		int format = root.path("format").asInt(-1);
		if (format != FORMAT) throw new IOException("Unsupported manifest format " + root.path("format") + "; expected " + FORMAT);

		String revision = text(root, "revision");
		Locale defaultLocale = Locale.forLanguageTag(text(root, "defaultLocale"));

		Map<String, Map<Locale, String>> bundles = new TreeMap<>();
		for (Map.Entry<String, JsonNode> product : root.path("bundles").properties()) {
			Map<Locale, String> byLocale = new TreeMap<>((left, right) -> left.toLanguageTag().compareTo(right.toLanguageTag()));
			for (Map.Entry<String, JsonNode> bundle : product.getValue().properties()) {
				if (!bundle.getValue().isTextual()) throw new IOException("The hash of " + product.getKey() + "/" + bundle.getKey() + " is no text");
				byLocale.put(Locale.forLanguageTag(bundle.getKey()), bundle.getValue().textValue());
			}
			bundles.put(product.getKey(), byLocale);
		}

		return new Manifest(revision, defaultLocale, bundles);
	}

	/**
	 * Writes the manifest as JSON, with products and locales sorted.
	 *
	 * @return the content of {@code manifest.json}
	 */
	public byte @NotNull [] write() {
		ObjectNode root = MAPPER.createObjectNode();
		root.put("format", FORMAT);
		root.put("revision", revision);
		root.put("defaultLocale", defaultLocale.toLanguageTag());

		ObjectNode products = root.putObject("bundles");
		bundles.forEach((product, byLocale) -> {
			ObjectNode node = products.putObject(product);
			byLocale.forEach((locale, hash) -> node.put(locale.toLanguageTag(), hash));
		});

		try {
			return MAPPER.writeValueAsBytes(root);
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	/**
	 * Returns the bundles of a product.
	 *
	 * @param product the product, such as {@code intercept}
	 * @return the hash of each bundle by locale, empty when the product has none
	 */
	public @NotNull Map<Locale, String> bundles(@NotNull String product) {
		return bundles.getOrDefault(product, Map.of());
	}

	/**
	 * Returns the path of a bundle relative to the manifest: {@code <product>/<locale>.json}.
	 *
	 * @param product the product
	 * @param locale  the locale
	 * @return the path
	 */
	public static @NotNull String path(@NotNull String product, @NotNull Locale locale) {
		return product + "/" + locale.toLanguageTag() + ".json";
	}

	private static String text(JsonNode root, String field) throws IOException {
		JsonNode node = root.get(field);
		if (node == null || !node.isTextual() || node.textValue().isBlank()) throw new IOException("The manifest needs '" + field + "'");

		return node.textValue();
	}
}
