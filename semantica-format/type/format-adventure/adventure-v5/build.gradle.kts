plugins {
    id("module-java")
    id("packaging-publication")
}

description = "Semantica's Adventure format for Adventure 5 (Paper, Velocity): components through MiniMessage, named arguments and a GlobalTranslator source"

dependencies {
    api(projects.semanticaCommon)
    api(projects.formatAdventureCommon)

    compileOnly(libs.adventure.v5.api)
    compileOnly(libs.adventure.v5.minimessage)
}

dependencies {
    testImplementation(projects.semanticaTesting)
    testImplementation(projects.syntaxIcu)
    testImplementation(libs.adventure.v5.api)
    testImplementation(libs.adventure.v5.minimessage)
    testImplementation(libs.adventure.v5.plain)
}
