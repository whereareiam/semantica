plugins {
    id("module-java")
    id("packaging-publication")
}

description = "Semantica's engine: the Semantica instance, catalogs merged from layers, checks, CLDR plural rules and formatting through ICU4J"

dependencies {
    api(projects.semanticaApi)

    implementation(libs.icu4j)
}

dependencies {
    testImplementation(projects.semanticaTesting)
    testImplementation(projects.syntaxIcu)
}
