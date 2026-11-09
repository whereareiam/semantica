package me.whereareiam.semantica.source.file;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A product's texts in its default locale, as the files a {@link FileSource} reads: the entry point the
 * {@code me.whereareiam.semantica} Gradle plugin runs to export them for translators.
 *
 * <p>Each file is named by its path inside the locale folder, without extension, which is the prefix of its keys,
 * and holds the tree Configura writes for the product's defaults, so its keys are the keys the product reads.</p>
 *
 * <pre>{@code
 * public final class MessageDefaults implements TranslationDefaults {
 *     @Override
 *     public Map<String, JsonNode> files() {
 *         Configura configura = Configura.builder().build();
 *         return Map.of("identica", configura.mapper().valueToTree(new MessagesDefaults().supply(new Messages())));
 *     }
 * }
 * }</pre>
 *
 * <p>An implementation has a public constructor without parameters and runs without the product running: no
 * server, no injection.</p>
 */
@FunctionalInterface
public interface TranslationDefaults {
	/**
	 * Returns the files of the default locale.
	 *
	 * @return each file's tree by its path, such as {@code identica} for {@code en/identica.json}, or
	 * {@code identica/commands} for {@code en/identica/commands.json}
	 */
	@NotNull Map<String, JsonNode> files();
}
