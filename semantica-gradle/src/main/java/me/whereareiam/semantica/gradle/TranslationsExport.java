package me.whereareiam.semantica.gradle;

import org.gradle.api.DefaultTask;
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
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Copies the default files the {@code semantica-processor} wrote for the product's {@code @Messages} interfaces into
 * {@code translations/<locale>/}, the source translators work from. The folder is replaced as a whole.
 */
@DisableCachingByDefault(because = "It writes into the project's own committed files")
public abstract class TranslationsExport extends DefaultTask {
	/**
	 * Returns the folder the processor wrote the default locale's files to, such as
	 * {@code build/classes/java/main/META-INF/semantica/en}.
	 *
	 * @return the folder
	 */
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract DirectoryProperty getGenerated();

	/**
	 * Returns the folder of the default locale, such as {@code translations/en}.
	 *
	 * @return the folder
	 */
	@OutputDirectory
	public abstract DirectoryProperty getLocaleFolder();

	/**
	 * Exports the texts.
	 */
	@TaskAction
	public void exportTexts() {
		Path generated = getGenerated().get().getAsFile().toPath();
		Path target = getLocaleFolder().get().getAsFile().toPath();

		try {
			clear(target);
			for (String path : TranslationFolders.files(generated)) {
				Path copy = target.resolve(path);
				Files.createDirectories(copy.getParent());
				Files.copy(generated.resolve(path), copy, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			throw new GradleException("Cannot export the default texts to " + target, exception);
		}

		getLogger().lifecycle("Exported the default texts to {}", target);
	}

	private static void clear(Path folder) throws IOException {
		if (!Files.exists(folder)) return;

		try (Stream<Path> walk = Files.walk(folder)) {
			for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
				if (!path.equals(folder)) Files.delete(path);
			}
		}
	}
}
