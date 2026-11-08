package me.whereareiam.semantica.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.semantica.Text;
import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.common.Semantica;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.message.ResolvedMessage;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runs every conformance case of the translation format against the engine and the ICU syntax.
 */
class ConformanceTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@TestFactory
	Stream<DynamicNode> conformance() throws IOException, URISyntaxException {
		Path folder = Path.of(ConformanceTest.class.getClassLoader().getResource("conformance").toURI());
		List<Path> files;
		try (Stream<Path> listed = Files.list(folder)) {
			files = listed.filter(path -> path.toString().endsWith(".json")).sorted().toList();
		}

		List<DynamicNode> containers = new ArrayList<>();
		for (Path file : files) {
			JsonNode suite = MAPPER.readTree(file.toFile());
			List<DynamicNode> tests = new ArrayList<>();
			tests.add(DynamicTest.dynamicTest("load issues", () -> loadIssues(suite)));

			int index = 0;
			for (JsonNode test : suite.path("cases")) {
				String name = (index++) + ": " + test.get("key").asText() + " in " + test.get("locale").asText()
						+ " with " + test.path("values");
				tests.add(DynamicTest.dynamicTest(name, () -> resolve(suite, test)));
			}

			containers.add(DynamicContainer.dynamicContainer(file.getFileName().toString(), tests));
		}

		return containers.stream();
	}

	private static void loadIssues(JsonNode suite) {
		List<Issue> reported = new ArrayList<>();
		try (Semantica semantica = semantica(suite, reported)) {
			Set<String> actual = new TreeSet<>();
			for (Issue issue : semantica.getCatalog().getIssues())
				actual.add(issue.getKind().getId() + " " + issue.getLocale().toLanguageTag() + " " + issue.getKey());

			Set<String> expected = new TreeSet<>();
			for (JsonNode issue : suite.path("issues"))
				expected.add(issue.get("kind").asText() + " " + issue.get("locale").asText() + " " + issue.get("key").asText());

			assertEquals(expected, actual, () -> "Load issues: " + semantica.getCatalog().getIssues());
		}
	}

	private static void resolve(JsonNode suite, JsonNode test) {
		List<Issue> reported = new ArrayList<>();
		try (Semantica semantica = semantica(suite, reported)) {
			reported.clear();

			Text text = text(test.get("key").asText(), test.path("values"));
			ResolvedMessage message = semantica.resolve(text, Locale.forLanguageTag(test.get("locale").asText()));
			assertEquals(test.get("expected").asText(), message.plain());

			if (test.has("resolvedLocale"))
				assertEquals(test.get("resolvedLocale").asText(), message.getLocale().toLanguageTag());

			Set<String> expected = new TreeSet<>();
			for (JsonNode issue : test.path("issues")) expected.add(issue.get("kind").asText() + " " + issue.get("key").asText());

			Set<String> actual = new TreeSet<>();
			for (Issue issue : reported) actual.add(issue.getKind().getId() + " " + issue.getKey());

			assertEquals(expected, actual, () -> "Issues while resolving: " + reported);
		}
	}

	private static Semantica semantica(JsonNode suite, List<Issue> reported) {
		MemorySource source = new MemorySource("conformance");
		for (Map.Entry<String, JsonNode> locale : suite.path("messages").properties()) {
			for (Map.Entry<String, JsonNode> message : locale.getValue().properties())
				source.put(Locale.forLanguageTag(locale.getKey()), message.getKey(), message.getValue().asText());
		}

		Semantica.Builder builder = Semantica.builder()
				.syntax(new IcuSyntax())
				.defaultLocale(Locale.forLanguageTag(suite.get("defaultLocale").asText()))
				.layer(Layer.of(source))
				.issues(reported::add)
				.watch(false);

		for (Map.Entry<String, JsonNode> fallback : suite.path("fallbacks").properties()) {
			List<Locale> targets = new ArrayList<>();
			fallback.getValue().forEach(target -> targets.add(Locale.forLanguageTag(target.asText())));
			builder.fallback(Locale.forLanguageTag(fallback.getKey()), targets.toArray(Locale[]::new));
		}

		JsonNode limits = suite.path("limits");
		if (limits.has("maxDepth")) builder.maxDepth(limits.get("maxDepth").asInt());
		if (limits.has("maxLength")) builder.maxLength(limits.get("maxLength").asInt());

		return builder.build();
	}

	private static Text text(String key, JsonNode values) {
		Map<String, Object> converted = new LinkedHashMap<>();
		for (Map.Entry<String, JsonNode> field : values.properties())
			converted.put(field.getKey(), value(field.getValue()));

		return Text.of(key).with(converted);
	}

	private static Object value(JsonNode node) {
		if (node.isIntegralNumber()) return node.longValue();
		if (node.isNumber()) return node.decimalValue();
		if (node.isBoolean()) return node.booleanValue();
		if (node.isObject() && node.has("text")) {
			JsonNode text = node.get("text");
			return text(text.get("key").asText(), text.path("values"));
		}

		return node.asText();
	}
}
