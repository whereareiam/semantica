package me.whereareiam.semantica.source.file;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Runs {@link TranslationDefaults} and writes their files as JSON, as the {@code me.whereareiam.semantica} Gradle
 * plugin's tasks do on a project's runtime classpath.
 *
 * <pre>
 * java -cp &lt;runtime classpath&gt; me.whereareiam.semantica.source.file.DefaultsExport &lt;folder&gt; &lt;class&gt;...
 * </pre>
 *
 * <p>Only text is exported: strings, and lists of strings as messages of several lines. Numbers, booleans and
 * other values are settings, not translations, and are left out with a note.</p>
 */
public final class DefaultsExport {
	private static final Pattern PATH = Pattern.compile("[\\p{L}\\p{N}_-]+(/[\\p{L}\\p{N}_-]+)*");
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final List<String> notes = new ArrayList<>();

	/**
	 * Exports the defaults of some classes into a folder.
	 *
	 * @param arguments the folder, then the names of classes implementing {@link TranslationDefaults}
	 * @throws Exception when a class cannot be run or a file cannot be written
	 */
	public static void main(String[] arguments) throws Exception {
		if (arguments.length < 2) throw new IllegalArgumentException("Usage: DefaultsExport <folder> <class>...");

		DefaultsExport export = new DefaultsExport();
		Map<String, JsonNode> files = export.collect(List.of(arguments).subList(1, arguments.length));
		export.write(Path.of(arguments[0]), files);
		export.notes.forEach(System.out::println);
	}

	Map<String, JsonNode> collect(List<String> classNames) throws ReflectiveOperationException {
		Map<String, JsonNode> files = new TreeMap<>();
		Map<String, String> owners = new TreeMap<>();

		for (String className : classNames) {
			Class<?> type = Class.forName(className);
			if (!TranslationDefaults.class.isAssignableFrom(type))
				throw new IllegalArgumentException(className + " does not implement " + TranslationDefaults.class.getName());

			TranslationDefaults defaults = (TranslationDefaults) type.getConstructor().newInstance();
			for (Map.Entry<String, JsonNode> file : defaults.files().entrySet()) {
				if (!PATH.matcher(file.getKey()).matches())
					throw new IllegalArgumentException(className + " names the file '" + file.getKey() + "'; write a path such as identica or identica/commands");

				String previous = owners.put(file.getKey(), className);
				if (previous != null)
					throw new IllegalArgumentException(className + " and " + previous + " both write the file " + file.getKey());

				JsonNode text = text(file.getKey(), file.getValue());
				if (text != null) files.put(file.getKey(), text);
			}
		}

		return files;
	}

	void write(Path folder, Map<String, JsonNode> files) throws IOException {
		DefaultPrettyPrinter printer = new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));
		for (Map.Entry<String, JsonNode> file : files.entrySet()) {
			Path target = folder.resolve(file.getKey() + ".json");
			Files.createDirectories(target.getParent());
			Files.writeString(target, MAPPER.writer(printer).writeValueAsString(file.getValue()) + "\n", StandardCharsets.UTF_8);
		}
	}

	private @Nullable JsonNode text(String path, JsonNode node) {
		if (node.isTextual()) return node;
		if (node.isArray()) return lines(path, (ArrayNode) node);
		if (node.isObject()) return object(path, (ObjectNode) node);
		if (!node.isNull()) notes.add("left out " + path + ": " + node.getNodeType().name().toLowerCase() + ", not text");

		return null;
	}

	private @Nullable JsonNode lines(String path, ArrayNode array) {
		for (JsonNode line : array) {
			if (line.isTextual()) continue;

			notes.add("left out " + path + ": a list of other values than text");
			return null;
		}

		return array;
	}

	private @Nullable JsonNode object(String path, ObjectNode object) {
		ObjectNode kept = MAPPER.createObjectNode();
		for (Map.Entry<String, JsonNode> field : object.properties()) {
			JsonNode text = text(path + "." + field.getKey(), field.getValue());
			if (text != null) kept.set(field.getKey(), text);
		}

		return kept.isEmpty() ? null : kept;
	}
}
