plugins {
    id("module-java")
    id("packaging-publication")
}

description = "Semantica's translations from folders and a jar's resources, one file per language, read through Configura and watched for changes"

dependencies {
    api(projects.semanticaApi)
    api(libs.configura)
}

dependencies {
    testImplementation(projects.semanticaCommon)
    testImplementation(projects.semanticaTesting)
    testImplementation(projects.syntaxIcu)
}
