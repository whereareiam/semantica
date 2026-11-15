package me.whereareiam.semantica.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * The {@code semantica} block of a product's build.
 *
 * <pre>{@code
 * semantica {
 *     locale.set("en")
 *     translations.set(layout.projectDirectory.dir("translations"))
 * }
 * }</pre>
 */
public abstract class SemanticaExtension {
	/**
	 * Returns the folder of the product's translations, one folder per locale. Defaults to {@code translations}
	 * in the project's folder.
	 *
	 * @return the folder
	 */
	public abstract DirectoryProperty getTranslations();

	/**
	 * Returns the locale the {@code @Default}s are written in: the processor writes the default files into its folder,
	 * and they are exported to it. Defaults to {@code en}.
	 *
	 * @return the locale's tag
	 */
	public abstract Property<String> getLocale();
}
