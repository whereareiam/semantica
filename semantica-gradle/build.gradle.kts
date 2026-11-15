plugins {
    `java-gradle-plugin`
    id("module-java")
    id("packaging-publication")
}

description = "The Gradle plugin that passes the default locale to the processor, exports the declared default texts for translators and checks the committed export is current"

gradlePlugin {
    plugins {
        create("semantica") {
            id = "me.whereareiam.semantica"
            implementationClass = "me.whereareiam.semantica.gradle.SemanticaPlugin"
            displayName = "Semantica"
            description = "Exports the declared default texts for translators and checks the committed export is current"
        }
    }
}

// java-gradle-plugin makes the publication pluginMaven and the plugin's marker; the toolkit publishes them.
toolkitPublish {
    name.set("pluginMaven")
    includeComponent.set(false)
}

// The processor and the API it brings, which the test projects compile with.
val processorRuntime = configurations.create("processorRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    processorRuntime(projects.semanticaProcessor)
}

tasks.test {
    inputs.files(processorRuntime)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dsemantica.processorClasspath=" + processorRuntime.files.joinToString(File.pathSeparator))
    })
}
