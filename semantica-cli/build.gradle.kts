plugins {
    id("module-java")
    alias(libs.plugins.shadow)
    id("packaging-publication")
}

description = "The command a translations repository's CI runs: check translations with Semantica's issues, and build the scoped bundles and their manifest"

dependencies {
    implementation(projects.semanticaCommon)
    implementation(projects.syntaxIcu)
    implementation(projects.sourceFile)
    implementation(projects.sourceHttp)
    implementation(projects.formatAdventureV5)
    implementation(libs.jackson)
    implementation(libs.adventure.v5.minimessage)
}

dependencies {
    testImplementation(projects.semanticaTesting)
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "me.whereareiam.semantica.cli.SemanticaCli")
    }
}

tasks.shadowJar {
    archiveClassifier.set("all")
    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
