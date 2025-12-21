dependencies {
    api(project(":semantica-api"))
    implementation(project(":semantica-common"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "semantica"
            pom {
                name.set("semantica")
                description.set("Semantica bootstrap: API facade with runtime wiring")
            }
        }
    }
}
