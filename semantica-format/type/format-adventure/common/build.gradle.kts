plugins {
    id("module-java")
    id("packaging-publication")
}

description = "What Semantica's Adventure formats share without Adventure's types: the tag policy, the markup check and Minecraft's locale codes"

dependencies {
    api(projects.semanticaFormat.common)
}

dependencies {
    testImplementation(projects.semanticaCommon)
    testImplementation(projects.semanticaTesting)
    testImplementation(projects.syntaxIcu)
}
