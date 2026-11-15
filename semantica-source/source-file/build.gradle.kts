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
