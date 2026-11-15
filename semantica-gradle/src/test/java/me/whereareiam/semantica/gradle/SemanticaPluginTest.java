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
 * Applies the plugin to a small product whose texts are declared on a {@code @Messages} interface.
 */
class SemanticaPluginTest {
	private static final String MESSAGES = "src/main/java/com/example/shop/ShopMessages.java";

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
		String classpath = Arrays.stream(System.getProperty("semantica.processorClasspath").split(File.pathSeparator))
				.map(path -> "\"" + path.replace("\\", "\\\\") + "\"")
				.collect(Collectors.joining(", "));

		write("settings.gradle.kts", "rootProject.name = \"shop\"\n");
		write("build.gradle.kts", """
				plugins {
					java
					id("me.whereareiam.semantica")
				}

				dependencies {
					compileOnly(files(%1$s))
					annotationProcessor(files(%1$s))
				}
				""".formatted(classpath));
		write(MESSAGES, """
				package com.example.shop;

				import me.whereareiam.semantica.Message;
				import me.whereareiam.semantica.declaration.Default;
				import me.whereareiam.semantica.declaration.Messages;

				@Messages("shop")
				public interface ShopMessages {
					@Default("<gold>Shop</gold>")
					Message title();

					@Default({"Buy things", "Cheap {price, number}"})
					Message lore(int price);
				}
				""");
	}

	@Test
	void exportsTheDeclaredDefaultsAsTheSourceForTranslators() throws IOException {
		BuildResult result = gradle("translationsExport").build();

		assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava").getOutcome());
		assertEquals(TaskOutcome.SUCCESS, result.task(":translationsExport").getOutcome());
		assertEquals("""
				{
				  "title": "<gold>Shop</gold>",
				  "lore": [
				    "Buy things",
				    "Cheap {price, number}"
				  ]
				}
				""", Files.readString(project.resolve("translations/en/shop.json")));
	}

	@Test
	void writesTheLocaleOfTheExtension() throws IOException {
		write("build.gradle.kts", Files.readString(project.resolve("build.gradle.kts")) + "\nsemantica {\n\tlocale.set(\"de\")\n}\n");

		gradle("translationsExport").build();

		assertTrue(Files.exists(project.resolve("build/classes/java/main/META-INF/semantica/de/shop.json")));
		assertTrue(Files.exists(project.resolve("translations/de/shop.json")));
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
		write(MESSAGES, Files.readString(project.resolve(MESSAGES)).replace("<gold>Shop</gold>", "<gold>Market</gold>"));

		BuildResult result = gradle("check").buildAndFail();

		assertTrue(result.getOutput().contains("shop.json differs"), result.getOutput());
		assertTrue(result.getOutput().contains("Run ./gradlew translationsExport"), result.getOutput());
	}

	@Test
	void exportReplacesFilesThatAreNoLongerDeclared() throws IOException {
		write("translations/en/old.json", "{}");
		write("translations/de/shop.json", "{\"title\": \"Laden\"}");

		BuildResult failed = gradle("translationsCheck").buildAndFail();
		assertTrue(failed.getOutput().contains("old.json is no longer declared"), failed.getOutput());
		assertTrue(failed.getOutput().contains("shop.json is missing"), failed.getOutput());

		gradle("translationsExport").build();

		assertFalse(Files.exists(project.resolve("translations/en/old.json")));
		assertTrue(Files.exists(project.resolve("translations/de/shop.json")));
		gradle("translationsCheck").build();
	}

	@Test
	void failsTheBuildOnADefaultThatDoesNotFitItsMethod() throws IOException {
		write(MESSAGES, Files.readString(project.resolve(MESSAGES)).replace("Cheap {price, number}", "Cheap {cost}"));

		BuildResult result = gradle("compileJava").buildAndFail();

		assertTrue(result.getOutput().contains("{cost} has no parameter"), result.getOutput());
	}
}
