plugins {
    id("module-java")
    id("packaging-publication")
}

description = "What Semantica's text formats share: rendering resolved messages, and plain text for logs"

toolkitPublish {
    artifactId.set("format-common")
}

dependencies {
    api(projects.semanticaApi)
}
