package me.whereareiam.semantica.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Applies the plugin to a small product whose texts are a Configura defaults provider, as Identica's are.
 */
class SemanticaPluginTest {
	@TempDir
	Path project;

	private void write(String path, String text) throws IOException {
		Path file = project.resolve(path);
		Files.createDirectories(file.getParent());
		Files.writeString(file, text, StandardCharsets.UTF_8);
	}

	private GradleRunner gradle(String... arguments) {
		return GradleRunner.create()
				.withProjectDir(project.toFile())
				.withPluginClasspath()
				.withArguments(arguments);
	}

	@BeforeEach
	void writeProduct() throws IOException {
		String classpath = Arrays.stream(System.getProperty("semantica.defaultsClasspath").split(File.pathSeparator))
				.map(path -> "\"" + path.replace("\\", "\\\\") + "\"")
				.collect(Collectors.joining(", "));

		write("settings.gradle.kts", "rootProject.name = \"shop\"\n");
		write("build.gradle.kts", """
				plugins {
					java
					id("me.whereareiam.semantica")
				}

				dependencies {
					implementation(files(%s))
				}

				semantica {
					defaults("com.example.shop.ShopDefaults")
				}
				""".formatted(classpath));
		write("src/main/java/com/example/shop/Messages.java", """
				package com.example.shop;

				import java.util.List;

				public class Messages {
					public String title;
					public List<String> lore;
					public int cooldown;
				}
				""");
		write("src/main/java/com/example/shop/MessagesDefaults.java", """
				package com.example.shop;

				import me.whereareiam.configura.merge.defaults.DefaultsProvider;

				import java.util.List;

				public class MessagesDefaults implements DefaultsProvider<Messages> {
					@Override
					public Messages supply(Messages messages) {
						messages.title = "<gold>Shop</gold>";
						messages.lore = List.of("Buy things", "Cheap {price, number}");
						messages.cooldown = 30;
						return messages;
					}
				}
				""");
		write("src/main/java/com/example/shop/ShopDefaults.java", """
				package com.example.shop;

				import com.fasterxml.jackson.databind.JsonNode;
				import me.whereareiam.configura.Configura;
				import me.whereareiam.semantica.source.file.TranslationDefaults;

				import java.util.Map;

				public final class ShopDefaults implements TranslationDefaults {
					@Override
					public Map<String, JsonNode> files() {
						Configura configura = Configura.builder().build();
						return Map.of("shop", configura.mapper().valueToTree(new MessagesDefaults().supply(new Messages())));
					}
				}
				""");
	}

	@Test
	void exportsTheDefaultsAsTheSourceForTranslators() throws IOException {
		BuildResult result = gradle("translationsExport").build();

		assertEquals(TaskOutcome.SUCCESS, result.task(":translationsExport").getOutcome());
		assertEquals("""
				{
				  "title" : "<gold>Shop</gold>",
				  "lore" : [ "Buy things", "Cheap {price, number}" ]
				}
				""", Files.readString(project.resolve("translations/en/shop.json")));
		assertTrue(result.getOutput().contains("left out shop.cooldown"), result.getOutput());
	}

	@Test
	void checkPassesOnACurrentExportAndRunsWithCheck() {
		gradle("translationsExport").build();

		BuildResult result = gradle("check").build();

		assertEquals(TaskOutcome.SUCCESS, result.task(":translationsCheck").getOutcome());
	}

	@Test
	void checkFailsWhenTheDefaultsChanged() throws IOException {
		gradle("translationsExport").build();
		write("src/main/java/com/example/shop/MessagesDefaults.java", Files.readString(project.resolve("src/main/java/com/example/shop/MessagesDefaults.java"))
				.replace("<gold>Shop</gold>", "<gold>Market</gold>"));

		BuildResult result = gradle("check").buildAndFail();

		assertTrue(result.getOutput().contains("shop.json differs"), result.getOutput());
		assertTrue(result.getOutput().contains("Run ./gradlew translationsExport"), result.getOutput());
	}

	@Test
	void exportReplacesFilesThatAreNoLongerExported() throws IOException {
		write("translations/en/old.json", "{}");
		write("translations/de/shop.json", "{\"title\": \"Laden\"}");

		BuildResult failed = gradle("translationsCheck").buildAndFail();
		assertTrue(failed.getOutput().contains("old.json is no longer exported"), failed.getOutput());
		assertTrue(failed.getOutput().contains("shop.json is missing"), failed.getOutput());

		gradle("translationsExport").build();

		assertFalse(Files.exists(project.resolve("translations/en/old.json")));
		assertTrue(Files.exists(project.resolve("translations/de/shop.json")));
		gradle("translationsCheck").build();
	}
}
