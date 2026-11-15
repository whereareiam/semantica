package me.whereareiam.semantica.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.List;

/**
 * The plugin {@code me.whereareiam.semantica}: makes a product's default texts, declared with {@code @Default} on its
 * {@code @Messages} interfaces, the source translators work from.
 *
 * <ul>
 *     <li>{@code compileJava} passes {@code semantica { locale }} to the {@code semantica-processor}, which writes the
 *     default files into {@code META-INF/semantica/<locale>/} of the classes;</li>
 *     <li>{@code translationsExport} copies them into {@code translations/<locale>/};</li>
 *     <li>{@code translationsCheck}, part of {@code check}, fails when the committed files differ from them.</li>
 * </ul>
 *
 * <p>The processor itself is a dependency of the project: {@code annotationProcessor("me.whereareiam.semantica:semantica-processor:<version>")}.</p>
 */
public final class SemanticaPlugin implements Plugin<Project> {
	private static final String GROUP = "translations";

	@Override
	public void apply(Project project) {
		SemanticaExtension extension = project.getExtensions().create("semantica", SemanticaExtension.class);
		extension.getTranslations().convention(project.getLayout().getProjectDirectory().dir("translations"));
		extension.getLocale().convention("en");

		project.getPlugins().withType(JavaPlugin.class, ignored -> register(project, extension));
	}

	private static void register(Project project, SemanticaExtension extension) {
		TaskProvider<JavaCompile> compile = project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile.class);
		compile.configure(task -> task.getOptions().getCompilerArgumentProviders()
				.add(() -> List.of("-Asemantica.locale=" + extension.getLocale().get())));

		Provider<Directory> generated = compile.flatMap(task -> task.getDestinationDirectory()
				.dir(extension.getLocale().map(locale -> "META-INF/semantica/" + locale)));
		Provider<Directory> localeFolder = extension.getTranslations().dir(extension.getLocale());

		project.getTasks().register("translationsExport", TranslationsExport.class, task -> {
			task.setGroup(GROUP);
			task.setDescription("Copies the declared default texts into translations/<locale>/ for translators.");
			task.getGenerated().set(generated);
			task.getLocaleFolder().set(localeFolder);
		});

		TaskProvider<TranslationsCheck> check = project.getTasks().register("translationsCheck", TranslationsCheck.class, task -> {
			task.setGroup(GROUP);
			task.setDescription("Fails when the committed translations/<locale>/ differs from the declared default texts.");
			task.getGenerated().set(generated);
			task.getLocaleFolder().set(localeFolder);
		});

		project.getTasks().named("check", task -> task.dependsOn(check));
	}
}
