package me.whereareiam.semantica.gradle;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lists the files of a folder of translations.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TranslationFolders {
	/**
	 * Lists the files below a folder by their path inside it.
	 *
	 * @param folder the folder
	 * @return the relative paths with {@code /} between folders, sorted; empty when the folder does not exist
	 */
	static List<String> files(Path folder) {
		if (!Files.isDirectory(folder)) return List.of();

		try (Stream<Path> walk = Files.walk(folder)) {
			return walk.filter(Files::isRegularFile)
					.map(path -> folder.relativize(path).toString().replace(File.separatorChar, '/'))
					.sorted()
					.toList();
		} catch (IOException exception) {
			throw new GradleException("Cannot read " + folder, exception);
		}
	}
}
