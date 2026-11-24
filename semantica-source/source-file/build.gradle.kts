plugins {
    id("module-java")
    id("packaging-publication")
}

description = "Semantica's translations from folders and from the default texts a jar carries, one file per language, read through Configura"

dependencies {
    api(projects.semanticaApi)
    api(libs.configura)
}

dependencies {
    testImplementation(projects.semanticaCommon)
    testImplementation(projects.semanticaTesting)
    testImplementation(projects.syntaxIcu)

    testAnnotationProcessor(projects.semanticaProcessor)
}

// The processor reads the parameter names of the test's @Messages interfaces, which an incremental build hands it as
// class files; only -parameters keeps them there, as Semantica's Gradle plugin does for products.
tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.add("-parameters")
}
