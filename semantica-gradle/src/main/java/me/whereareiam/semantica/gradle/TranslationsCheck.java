package me.whereareiam.semantica.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Fails when the committed {@code translations/<locale>/} differs from the default files the processor writes now, so
 * translators never work from an outdated source.
 */
@DisableCachingByDefault(because = "A check whose only result is passing or failing")
public abstract class TranslationsCheck extends DefaultTask {
	/**
	 * Returns the folder the processor wrote the default locale's files to.
	 *
	 * @return the folder
	 */
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract DirectoryProperty getGenerated();

	/**
	 * Returns the committed folder of the default locale, such as {@code translations/en}.
	 *
	 * @return the folder
	 */
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract DirectoryProperty getLocaleFolder();

	/**
	 * Compares the committed texts with the generated ones.
	 */
	@TaskAction
	public void checkTexts() {
		Path committed = getLocaleFolder().get().getAsFile().toPath();
		Path generated = getGenerated().get().getAsFile().toPath();

		List<String> problems = new ArrayList<>();
		TreeSet<String> paths = new TreeSet<>(TranslationFolders.files(committed));
		paths.addAll(TranslationFolders.files(generated));
		for (String path : paths) {
			boolean inCommitted = Files.exists(committed.resolve(path));
			boolean inGenerated = Files.exists(generated.resolve(path));
			if (!inCommitted) problems.add(path + " is missing");
			if (!inGenerated) problems.add(path + " is no longer declared");
			if (inCommitted && inGenerated && !same(committed.resolve(path), generated.resolve(path))) problems.add(path + " differs");
		}

		if (problems.isEmpty()) return;
		throw new GradleException("The committed texts in " + committed + " are not the declared defaults:\n  "
				+ String.join("\n  ", problems) + "\nRun ./gradlew translationsExport and commit the result.");
	}

	private static boolean same(Path left, Path right) {
		try {
			return Files.mismatch(left, right) == -1;
		} catch (IOException exception) {
			throw new GradleException("Cannot compare " + left + " with " + right, exception);
		}
	}
}
