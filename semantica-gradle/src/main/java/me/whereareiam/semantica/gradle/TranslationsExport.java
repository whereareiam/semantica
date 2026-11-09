package me.whereareiam.semantica.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/**
 * Writes the product's default texts into {@code translations/<locale>/}, the source translators work from. The
 * folder is replaced as a whole.
 */
@DisableCachingByDefault(because = "It writes into the project's own committed files")
public abstract class TranslationsExport extends DefaultsTask {
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
		export(getLocaleFolder().get().getAsFile().toPath());
		getLogger().lifecycle("Exported the default texts to {}", getLocaleFolder().get().getAsFile());
	}
}
