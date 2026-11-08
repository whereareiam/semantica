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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
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
 * <p>One folder, {@value #ROOT_FOLDER} unless named otherwise, holds the messages of the root locale
 * ({@link Locale#ROOT}): messages that are the same in every language, such as one made only of references,
 * {@code "{@game.prefix} {@game.account.login-failed}"}, which every lookup tries last.</p>
 *
 * <p>JSON and YAML are read through Configura, {@code .properties} through the JDK. A list of strings is a
 * message of several lines. Files directly in the folder, folders starting with a dot and folders named by
 * {@link Builder#skip(String...)} are not read.</p>
 */
public final class FileSource implements Source {
	/**
	 * The name of the folder of the root locale, unless another is given.
	 */
	public static final String ROOT_FOLDER = "all";

	private final String name;
	private final Path folder;
	private final String rootFolder;
	private final Set<String> skipped;
	private final Function<InputStream, JsonNode> treeReader;

	private FileSource(Builder builder) {
		this.name = "file:" + builder.folder;
		this.folder = builder.folder;
		this.rootFolder = builder.rootFolder;
		this.skipped = Set.copyOf(builder.skipped);
		this.treeReader = builder.configura::readNode;
	}

	/**
	 * Starts building a source for a folder on disk, to name the root locale's folder or folders that hold no
	 * translations.
	 *
	 * <pre>{@code
	 * FileSource.builder(dataFolder.resolve("translations"), configura)
	 *         .skip("rules", "local")
	 *         .build();
	 * }</pre>
	 *
	 * @param folder    the folder, such as a plugin's {@code languages} folder
	 * @param configura reads the JSON and YAML files
	 * @return a new builder
	 */
	public static @NotNull Builder builder(@NotNull Path folder, @NotNull Configura configura) {
		return new Builder(folder, configura);
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
		return builder(folder, configura).build();
	}

	/**
	 * Reads translations from a folder on disk and watches it, with the root locale's messages in a folder of
	 * another name.
	 *
	 * @param folder     the folder, such as a plugin's {@code languages} folder
	 * @param configura  reads the JSON and YAML files
	 * @param rootFolder the name of the folder of the root locale, such as {@code all}
	 * @return the source
	 */
	public static @NotNull FileSource of(@NotNull Path folder, @NotNull Configura configura, @NotNull String rootFolder) {
		return builder(folder, configura).rootFolder(rootFolder).build();
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
			return new FolderWatch(folder, skipped, onChange);
		} catch (IOException exception) {
			throw new UncheckedIOException("Cannot watch " + folder, exception);
		}
	}

	private void loadFolder(TranslationFiles files) throws IOException {
		if (!Files.isDirectory(folder)) return;

		for (Path localeFolder : sorted(folder)) {
			String folderName = localeFolder.getFileName().toString();
			if (!Files.isDirectory(localeFolder) || folderName.startsWith(".")) continue;

			if (skipped.contains(folderName)) continue;

			Locale locale = folderName.equals(rootFolder) ? Locale.ROOT : files.locale(folderName);
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

	/**
	 * Builds a {@link FileSource}.
	 */
	public static final class Builder {
		private final Path folder;
		private final Configura configura;
		private final Set<String> skipped = new LinkedHashSet<>();
		private String rootFolder = ROOT_FOLDER;

		private Builder(Path folder, Configura configura) {
			this.folder = Objects.requireNonNull(folder, "folder");
			this.configura = Objects.requireNonNull(configura, "configura");
		}

		/**
		 * Names the folder of the root locale. Defaults to {@value #ROOT_FOLDER}.
		 *
		 * @param name the folder's name
		 * @return this builder
		 */
		public @NotNull Builder rootFolder(@NotNull String name) {
			this.rootFolder = Objects.requireNonNull(name, "name");
			return this;
		}

		/**
		 * Leaves out folders directly in the folder that hold no translations, such as files of rules or another
		 * layer's folder. They are neither read, nor reported as no locale, nor watched.
		 *
		 * @param names the folders' names, such as {@code rules}
		 * @return this builder
		 */
		public @NotNull Builder skip(@NotNull String... names) {
			for (String name : names) skipped.add(Objects.requireNonNull(name, "name"));
			return this;
		}

		/**
		 * Builds the source. Nothing is read until it is loaded.
		 *
		 * @return the source
		 */
		public @NotNull FileSource build() {
			return new FileSource(this);
		}
	}

	@Override
	public String toString() {
		return name;
	}
}
