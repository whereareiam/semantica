plugins {
    id("module-java")
    id("packaging-publication")
}

description = "Semantica's Adventure format for Adventure 4.25 and later 4.x (BungeeCord and Spigot through adventure-platform 4.x): components through MiniMessage, named arguments and a GlobalTranslator source"

dependencies {
    api(projects.semanticaCommon)
    api(projects.formatAdventureCommon)

    compileOnly(libs.adventure.v4.api)
    compileOnly(libs.adventure.v4.minimessage)
}

dependencies {
    testImplementation(projects.semanticaTesting)
    testImplementation(projects.syntaxIcu)
    testImplementation(libs.adventure.v4.api)
    testImplementation(libs.adventure.v4.minimessage)
    testImplementation(libs.adventure.v4.plain)
}
