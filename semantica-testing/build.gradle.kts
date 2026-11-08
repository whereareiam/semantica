plugins {
    id("module-java")
    id("packaging-publication")
}

description = "Test helpers for Semantica's consumers: an in-memory source, issue assertions, and the conformance cases of the translation format"

dependencies {
    api(projects.semanticaApi)
}

dependencies {
    testImplementation(projects.semanticaCommon)
    testImplementation(projects.syntaxIcu)
    testImplementation(libs.jackson)
}
