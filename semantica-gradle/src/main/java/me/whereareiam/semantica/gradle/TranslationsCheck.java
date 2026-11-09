package me.whereareiam.semantica.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
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
 * Fails when the committed {@code translations/<locale>/} differs from what the product's defaults export now, so
 * translators never work from an outdated source.
 */
@DisableCachingByDefault(because = "A check whose only result is passing or failing")
public abstract class TranslationsCheck extends DefaultsTask {
	/**
	 * Returns the committed folder of the default locale, such as {@code translations/en}.
	 *
	 * @return the folder
	 */
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract DirectoryProperty getLocaleFolder();

	/**
	 * Returns the folder the current export is written to for the comparison.
	 *
	 * @return the folder
	 */
	@OutputDirectory
	public abstract DirectoryProperty getExportFolder();

	/**
	 * Compares the committed texts with a fresh export.
	 */
	@TaskAction
	public void checkTexts() {
		Path committed = getLocaleFolder().get().getAsFile().toPath();
		Path exported = getExportFolder().get().getAsFile().toPath();
		export(exported);

		List<String> problems = new ArrayList<>();
		TreeSet<String> paths = new TreeSet<>(files(committed));
		paths.addAll(files(exported));
		for (String path : paths) {
			boolean inCommitted = Files.exists(committed.resolve(path));
			boolean inExport = Files.exists(exported.resolve(path));
			if (!inCommitted) problems.add(path + " is missing");
			if (!inExport) problems.add(path + " is no longer exported");
			if (inCommitted && inExport && !same(committed.resolve(path), exported.resolve(path))) problems.add(path + " differs");
		}

		if (problems.isEmpty()) return;
		throw new GradleException("The committed texts in " + committed + " are not what the defaults export:\n  "
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
