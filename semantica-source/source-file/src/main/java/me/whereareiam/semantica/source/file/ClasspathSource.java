package me.whereareiam.semantica.source.file;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.Configura;
import me.whereareiam.semantica.source.Source;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The default texts a jar carries: the files the {@code semantica-processor} writes for its {@code @Messages}
 * interfaces under {@value #FOLDER}, one folder per locale. Add it as the lowest layer, below the folder an
 * administrator edits, so a text nobody translated or changed shows its default:
 *
 * <pre>{@code
 * Semantica semantica = Semantica.builder()
 *         .syntax(new IcuSyntax())
 *         .layer(Layer.of(ClasspathSource.of(CommandMessages.class, configura), Layer.DEFAULTS))
 *         .layer(Layer.of(FileSource.of(dataFolder.resolve("languages"), configura), Layer.OVERRIDES))
 *         .build();
 * }</pre>
 *
 * <p>The files are read from the jar or the folder of classes that holds the class given, and from nowhere else, so
 * a plugin or module whose class loader also sees another's jar reads only its own files. A jar a mod loader serves
 * through a file system of its own, such as NeoForge's {@code union:}, is read through that file system. Their paths are key
 * prefixes as in a {@link FileSource}; the folder {@value FileSource#ROOT_FOLDER} holds the root locale. The
 * texts never change while the jar runs, so the source is not watched.</p>
 */
public final class ClasspathSource implements Source {
	/**
	 * The folder of the default texts inside a jar.
	 */
	public static final String FOLDER = "META-INF/semantica/";

	private final Class<?> owner;
	private final Function<InputStream, JsonNode> treeReader;

	private ClasspathSource(Class<?> owner, Configura configura) {
		this.owner = owner;
		this.treeReader = configura::readNode;
	}

	/**
	 * Reads the default texts of the jar or folder of classes that holds a class.
	 *
	 * @param owner     a class of the jar, such as one of its {@code @Messages} interfaces
	 * @param configura reads the JSON and YAML files
	 * @return the source
	 */
	public static @NotNull ClasspathSource of(@NotNull Class<?> owner, @NotNull Configura configura) {
		return new ClasspathSource(Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(configura, "configura"));
	}

	@Override
	public @NotNull String getName() {
		return "classpath:" + owner.getName();
	}

	@Override
	public @NotNull Content load() throws IOException {
		TranslationFiles files = new TranslationFiles(getName(), "", Collections.emptySet(), treeReader);
		String resource = owner.getName().replace('.', '/') + ".class";
		URI location = location(resource);

		if (location.getScheme().equals("jar")) loadJar(files, jarPath(location, resource));
		else loadFolder(files, root(location, resource).resolve(FOLDER));

		return files.build();
	}

	/**
	 * Returns where the owner's class file was loaded from, such as {@code jar:file:/plugins/x.jar!/a/B.class},
	 * {@code file:/build/classes/java/main/a/B.class}, or a mod loader's own file system, such as NeoForge's
	 * {@code union:}.
	 */
	private URI location(String resource) throws IOException {
		URL url = owner.getResource("/" + resource);
		if (url == null) throw new IOException("Cannot find where " + owner.getName() + " was loaded from");

		try {
			return url.toURI();
		} catch (URISyntaxException exception) {
			throw new IOException("Cannot read " + url, exception);
		}
	}

	private static Path jarPath(URI location, String resource) throws IOException {
		String inner = location.getRawSchemeSpecificPart();
		String jar = inner.substring(0, inner.length() - ("!/" + resource).length());
		try {
			return Path.of(new URI(jar));
		} catch (URISyntaxException exception) {
			throw new IOException("Cannot read " + location, exception);
		}
	}

	/**
	 * Returns the root of the package tree on the file system that serves the class file: a folder of classes, or
	 * the inside of a jar that a mod loader opened as a file system of its own.
	 */
	private Path root(URI location, String resource) throws IOException {
		Path root;
		try {
			root = Path.of(location);
		} catch (IllegalArgumentException | FileSystemNotFoundException | ProviderNotFoundException exception) {
			throw new IOException("Cannot read the texts of " + owner.getName() + " from " + location + "; no file system serves it", exception);
		}

		for (int segment = 0; segment < resource.split("/").length; segment++) root = root.getParent();
		return root;
	}

	private void loadJar(TranslationFiles files, Path jar) throws IOException {
		try (ZipFile zip = new ZipFile(jar.toFile())) {
			List<? extends ZipEntry> entries = zip.stream()
					.filter(entry -> !entry.isDirectory() && entry.getName().startsWith(FOLDER))
					.sorted(Comparator.comparing(ZipEntry::getName))
					.toList();

			for (ZipEntry entry : entries) read(files, entry.getName().substring(FOLDER.length()), () -> zip.getInputStream(entry));
		}
	}

	private void loadFolder(TranslationFiles files, Path folder) throws IOException {
		if (!Files.isDirectory(folder)) return;

		List<Path> paths;
		try (Stream<Path> walk = Files.walk(folder)) {
			paths = walk.filter(Files::isRegularFile).sorted().toList();
		}

		for (Path path : paths) {
			List<String> parts = new ArrayList<>();
			for (Path part : folder.relativize(path)) parts.add(part.toString());
			read(files, String.join("/", parts), () -> Files.newInputStream(path));
		}
	}

	/**
	 * Reads one file by its path below {@value #FOLDER}: its locale folder, then its path inside it.
	 */
	private static void read(TranslationFiles files, String path, TranslationFiles.StreamOpener stream) {
		int slash = path.indexOf('/');
		if (slash < 0 || path.startsWith(".") || path.contains("/.")) return;

		String folder = path.substring(0, slash);
		Locale locale = folder.equals(FileSource.ROOT_FOLDER) ? Locale.ROOT : files.locale(folder);
		if (locale == null) return;

		files.read(locale, path.substring(slash + 1), FOLDER + path, stream);
	}

	@Override
	public String toString() {
		return getName();
	}
}
