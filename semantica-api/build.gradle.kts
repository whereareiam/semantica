publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "semantica-api"
            pom {
                name.set("semantica-api")
                description.set("Public API for Semantica")
            }
        }
    }
}

