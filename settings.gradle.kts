rootProject.name = "Semantica"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven("https://registry.whereareiam.me/maven/packages")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://registry.whereareiam.me/maven/packages")
    }
}

include(":semantica-api")
project(":semantica-api").projectDir = file("semantica-api")

include(":semantica-common")
project(":semantica-common").projectDir = file("semantica-common")

include(":syntax-icu")
project(":syntax-icu").projectDir = file("semantica-syntax/syntax-icu")

include(":source-file")
project(":source-file").projectDir = file("semantica-source/source-file")

include(":source-http")
project(":source-http").projectDir = file("semantica-source/source-http")

include(":semantica-format")
project(":semantica-format").projectDir = file("semantica-format")

include(":semantica-format:common")
project(":semantica-format:common").projectDir = file("semantica-format/common")

include(":format-adventure-common")
project(":format-adventure-common").projectDir = file("semantica-format/type/format-adventure/common")

include(":format-adventure-v4")
project(":format-adventure-v4").projectDir = file("semantica-format/type/format-adventure/adventure-v4")

include(":format-adventure-v5")
project(":format-adventure-v5").projectDir = file("semantica-format/type/format-adventure/adventure-v5")

include(":semantica-testing")
project(":semantica-testing").projectDir = file("semantica-testing")

include(":semantica-cli")
project(":semantica-cli").projectDir = file("semantica-cli")
