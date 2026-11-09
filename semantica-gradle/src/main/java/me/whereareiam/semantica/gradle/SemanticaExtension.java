package me.whereareiam.semantica.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import java.util.List;

/**
 * The {@code semantica} block of a product's build.
 *
 * <pre>{@code
 * semantica {
 *     defaults("me.whereareiam.identica.message.MessageDefaults")
 * }
 * }</pre>
 */
public abstract class SemanticaExtension {
	/**
	 * Returns the names of the classes implementing
	 * {@code me.whereareiam.semantica.source.file.TranslationDefaults} whose files are exported.
	 *
	 * @return the class names
	 */
	public abstract ListProperty<String> getDefaults();

	/**
	 * Returns the folder of the product's translations, one folder per locale. Defaults to {@code translations}
	 * in the project's folder.
	 *
	 * @return the folder
	 */
	public abstract DirectoryProperty getTranslations();

	/**
	 * Returns the locale the defaults are written in, the folder they are exported to. Defaults to {@code en}.
	 *
	 * @return the locale's tag
	 */
	public abstract Property<String> getLocale();

	/**
	 * Adds classes whose files are exported.
	 *
	 * @param classNames classes implementing {@code me.whereareiam.semantica.source.file.TranslationDefaults}
	 */
	public void defaults(String... classNames) {
		getDefaults().addAll(List.of(classNames));
	}
}
