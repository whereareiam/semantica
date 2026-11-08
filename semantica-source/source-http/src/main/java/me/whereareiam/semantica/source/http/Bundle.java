package me.whereareiam.semantica.source.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A bundle: the messages of one product in one locale, as one flat JSON object of keys and messages, sorted
 * by key.
 *
 * <pre>{@code
 * {
 *   "common.unit": "{count, plural, one {# unit} other {# units}}",
 *   "game.menu.title": "Menu"
 * }
 * }</pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Bundle {
	private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	/**
	 * Writes messages as a bundle.
	 *
	 * @param messages the messages by key
	 * @return the bundle's bytes, the same for the same messages
	 */
	public static byte @NotNull [] write(@NotNull Map<String, String> messages) {
		ObjectNode root = MAPPER.createObjectNode();
		new TreeMap<>(messages).forEach(root::put);

		try {
			return MAPPER.writeValueAsBytes(root);
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	/**
	 * Reads a bundle.
	 *
	 * @param bytes the bundle's bytes
	 * @return the messages by key
	 * @throws IOException when it is no bundle
	 */
	public static @NotNull Map<String, String> read(byte @NotNull [] bytes) throws IOException {
		JsonNode root = MAPPER.readTree(bytes);
		if (root == null || !root.isObject()) throw new IOException("A bundle is a JSON object of keys and messages");

		Map<String, String> messages = new LinkedHashMap<>();
		for (Map.Entry<String, JsonNode> entry : root.properties()) {
			if (!entry.getValue().isTextual()) throw new IOException("The message of '" + entry.getKey() + "' is no text");
			messages.put(entry.getKey(), entry.getValue().textValue());
		}

		return messages;
	}

	/**
	 * Returns the SHA-256 of bytes as lowercase hex, as the manifest lists it.
	 *
	 * @param bytes the bytes
	 * @return the hash
	 */
	public static @NotNull String hash(byte @NotNull [] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
