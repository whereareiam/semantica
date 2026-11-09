package me.whereareiam.semantica.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.toolchain.JavaToolchainService;

/**
 * The plugin {@code me.whereareiam.semantica}: makes a product's default texts, written as Configura defaults in
 * its code, the source translators work from.
 *
 * <ul>
 *     <li>{@code translationsExport} runs the classes named in {@code semantica { defaults(...) }} on the runtime
 *     classpath and writes their files into {@code translations/<locale>/};</li>
 *     <li>{@code translationsCheck}, part of {@code check}, fails when the committed files differ from a fresh
 *     export.</li>
 * </ul>
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
		JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
		SourceSet main = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		JavaToolchainService toolchains = project.getExtensions().getByType(JavaToolchainService.class);
		Provider<Directory> localeFolder = extension.getTranslations().dir(extension.getLocale());

		project.getTasks().register("translationsExport", TranslationsExport.class, task -> {
			task.setGroup(GROUP);
			task.setDescription("Writes the default texts into translations/<locale>/ for translators.");
			task.getRuntimeClasspath().from(main.getRuntimeClasspath());
			task.getDefaults().set(extension.getDefaults());
			task.getLauncher().set(toolchains.launcherFor(java.getToolchain()));
			task.getLocaleFolder().set(localeFolder);
		});

		TaskProvider<TranslationsCheck> check = project.getTasks().register("translationsCheck", TranslationsCheck.class, task -> {
			task.setGroup(GROUP);
			task.setDescription("Fails when the committed translations/<locale>/ differs from the default texts.");
			task.getRuntimeClasspath().from(main.getRuntimeClasspath());
			task.getDefaults().set(extension.getDefaults());
			task.getLauncher().set(toolchains.launcherFor(java.getToolchain()));
			task.getLocaleFolder().set(localeFolder);
			task.getExportFolder().set(project.getLayout().getBuildDirectory().dir("semantica/translations-check"));
		});

		project.getTasks().named("check", task -> task.dependsOn(check));
	}
}
