package me.whereareiam.semantica.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * What the tasks of the plugin share: running a product's defaults on its runtime classpath and writing their files.
 */
@DisableCachingByDefault(because = "A base of the plugin's tasks, which say for themselves")
public abstract class DefaultsTask extends DefaultTask {
	private static final String RUNNER = "me.whereareiam.semantica.source.file.DefaultsExport";

	/**
	 * Returns the product's runtime classpath, its own classes included.
	 *
	 * @return the classpath
	 */
	@Classpath
	public abstract ConfigurableFileCollection getRuntimeClasspath();

	/**
	 * Returns the classes whose files are exported.
	 *
	 * @return the class names
	 */
	@Input
	public abstract ListProperty<String> getDefaults();

	/**
	 * Returns the Java the defaults run on; the project's toolchain, or Gradle's own Java when absent.
	 *
	 * @return the launcher
	 */
	@Nested
	@Optional
	public abstract Property<JavaLauncher> getLauncher();

	@Inject
	protected abstract ExecOperations getExecOperations();

	/**
	 * Replaces a folder's content with the exported files.
	 *
	 * @param folder the folder
	 */
	protected void export(Path folder) {
		List<String> defaults = getDefaults().get();
		if (defaults.isEmpty())
			throw new GradleException("semantica { defaults(...) } names no class implementing TranslationDefaults");

		clear(folder);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		List<String> arguments = new ArrayList<>();
		arguments.add(folder.toString());
		arguments.addAll(defaults);

		ExecResult result = getExecOperations().javaexec(spec -> {
			spec.classpath(getRuntimeClasspath());
			spec.getMainClass().set(RUNNER);
			spec.args(arguments);
			spec.setStandardOutput(output);
			spec.setIgnoreExitValue(true);
			if (getLauncher().isPresent()) spec.executable(getLauncher().get().getExecutablePath().getAsFile());
		});

		String notes = output.toString().strip();
		if (!notes.isEmpty()) getLogger().lifecycle(notes);
		if (result.getExitValue() != 0)
			throw new GradleException("Running the defaults failed; see the error above. The runtime classpath needs "
					+ "me.whereareiam.semantica:source-file, and each class a public constructor without parameters");
	}

	private static void clear(Path folder) {
		if (!Files.exists(folder)) return;

		try (Stream<Path> walk = Files.walk(folder)) {
			for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
				if (!path.equals(folder)) Files.delete(path);
			}
		} catch (IOException exception) {
			throw new GradleException("Cannot clear " + folder, exception);
		}
	}

	/**
	 * Lists the files below a folder by their path inside it.
	 *
	 * @param folder the folder
	 * @return the relative paths, sorted
	 */
	protected static List<String> files(Path folder) {
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
