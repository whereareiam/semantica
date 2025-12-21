dependencies {
    compileOnly(project(":semantica-api"))
    testImplementation(project(":semantica-api"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "semantica-common"
            pom {
                name.set("semantica-common")
                description.set("Internal runtime for Semantica")
            }
        }
    }
}
