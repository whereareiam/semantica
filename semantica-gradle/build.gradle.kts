plugins {
    `java-gradle-plugin`
    id("module-java")
    id("packaging-publication")
}

description = "The Gradle plugin that exports a product's default texts for translators and checks the committed export is current"

gradlePlugin {
    plugins {
        create("semantica") {
            id = "me.whereareiam.semantica"
            implementationClass = "me.whereareiam.semantica.gradle.SemanticaPlugin"
            displayName = "Semantica"
            description = "Exports a product's default texts for translators and checks the committed export is current"
        }
    }
}

// java-gradle-plugin makes the publication pluginMaven and the plugin's marker; the toolkit publishes them.
toolkitPublish {
    name.set("pluginMaven")
    includeComponent.set(false)
}

// What a project using the plugin has on its runtime classpath, for the test projects.
val defaultsRuntime = configurations.create("defaultsRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    defaultsRuntime(projects.sourceFile)
}

tasks.test {
    inputs.files(defaultsRuntime)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dsemantica.defaultsClasspath=" + defaultsRuntime.files.joinToString(File.pathSeparator))
    })
}
