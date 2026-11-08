plugins {
    id("module-java")
    id("packaging-publication")
}

description = "ICU MessageFormat with references to other messages, read into Semantica's parsed messages"

dependencies {
    api(projects.semanticaApi)
}
