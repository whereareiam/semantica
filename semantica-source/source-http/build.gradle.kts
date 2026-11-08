plugins {
    id("module-java")
    id("packaging-publication")
}

description = "Semantica's translations from bundles an organisation's translations repository builds, fetched over HTTP when its manifest changes"

dependencies {
    api(projects.semanticaApi)

    implementation(libs.jackson)
}

dependencies {
    testImplementation(projects.semanticaCommon)
    testImplementation(projects.semanticaTesting)
    testImplementation(projects.syntaxIcu)
}
