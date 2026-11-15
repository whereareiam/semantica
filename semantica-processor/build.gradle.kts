plugins {
    id("module-java")
    id("packaging-publication")
}

description = "The annotation processor of @Messages interfaces: checks each default against its method, writes the implementation and the default-locale files"

dependencies {
    implementation(projects.semanticaApi)
    implementation(projects.syntaxIcu)
}
