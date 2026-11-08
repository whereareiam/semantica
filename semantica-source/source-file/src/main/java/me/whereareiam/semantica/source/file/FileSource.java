package me.whereareiam.semantica.source.file;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.Configura;
import me.whereareiam.semantica.source.Source;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Translations from a folder on disk, watched for changes: a server's {@code languages} folder, which holds the
 * defaults a plugin writes there (such as through Configura's defaults providers) and an administrator's changes.
 *
 * <p>The folder holds one folder per locale, named by language tag, and any files inside it; the path of a file
 * inside its locale folder is the prefix of its keys, and nested objects add to it:</p>
 *
 * <pre>
 * languages/
 *   en/
 *     game/menu.json        {"title": "Menu", "button": {"close": "Close"}}  -> game.menu.title, game.menu.button.close
 *     common.yml            unit: "{count, plural, one {# unit} other {# units}}" -> common.unit
 *   ru/
 *     game/menu.json
 * </pre>
 *
 * <p>JSON and YAML are read through Configura, {@code .properties} through the JDK. A list of strings is a
 * message of several lines. Files directly in the folder and folders starting with a dot are not read.</p>
 */
public final class FileSource implements Source {
	private final String name;
	private final Path folder;
	private final Function<InputStream, JsonNode> treeReader;

	private FileSource(String name, Path folder, Function<InputStream, JsonNode> treeReader) {
		this.name = name;
		this.folder = folder;
		this.treeReader = treeReader;
	}

	/**
	 * Reads translations from a folder on disk and watches it. A folder that does not exist yet has no
	 * translations; it is read once it is created.
	 *
	 * @param folder    the folder, such as a plugin's {@code languages} folder
	 * @param configura reads the JSON and YAML files
	 * @return the source
	 */
	public static @NotNull FileSource of(@NotNull Path folder, @NotNull Configura configura) {
		Objects.requireNonNull(folder, "folder");
		Objects.requireNonNull(configura, "configura");

		return new FileSource("file:" + folder, folder, configura::readNode);
	}

	@Override
	public @NotNull String getName() {
		return name;
	}

	@Override
	public @NotNull Content load() throws IOException {
		TranslationFiles files = new TranslationFiles(name, treeReader);
		loadFolder(files);

		return files.build();
	}

	@Override
	public @NotNull AutoCloseable watch(@NotNull Runnable onChange) {
		try {
			return new FolderWatch(folder, onChange);
		} catch (IOException exception) {
			throw new UncheckedIOException("Cannot watch " + folder, exception);
		}
	}

	private void loadFolder(TranslationFiles files) throws IOException {
		if (!Files.isDirectory(folder)) return;

		for (Path localeFolder : sorted(folder)) {
			String folderName = localeFolder.getFileName().toString();
			if (!Files.isDirectory(localeFolder) || folderName.startsWith(".")) continue;

			Locale locale = files.locale(folderName);
			if (locale == null) continue;

			List<Path> paths;
			try (Stream<Path> walk = Files.walk(localeFolder)) {
				paths = walk.filter(Files::isRegularFile)
						.filter(path -> !hidden(localeFolder.relativize(path)))
						.sorted()
						.toList();
			}

			for (Path path : paths) {
				String relative = slashes(localeFolder.relativize(path));
				files.read(locale, relative, folderName + "/" + relative, () -> Files.newInputStream(path));
			}
		}
	}

	private static List<Path> sorted(Path folder) throws IOException {
		try (Stream<Path> children = Files.list(folder)) {
			return children.sorted().toList();
		}
	}

	private static boolean hidden(Path relative) {
		for (Path part : relative) if (part.toString().startsWith(".")) return true;
		return false;
	}

	private static String slashes(Path relative) {
		List<String> parts = new ArrayList<>();
		for (Path part : relative) parts.add(part.toString());

		return String.join("/", parts);
	}

	@Override
	public String toString() {
		return name;
	}
}
