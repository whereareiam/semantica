---
title: Exporting defaults
description: Make a product's default texts, declared with @Default in its code, the source translators work from.
---

A product's default texts live in its code, as the `@Default`s of its [declared messages](../messages/index.md), and travel in its jar. Translators work from files instead: `translations/en/` in the product's repository, which Weblate reads. The Gradle plugin `me.whereareiam.semantica` keeps the two the same: it exports the declared defaults into those files, and fails the build when the committed files fall behind the code.

## Set it up

The plugin is published to `https://registry.whereareiam.me/maven/packages`:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://registry.whereareiam.me/maven/packages")
    }
}
```

```kotlin
// build.gradle.kts
plugins {
    java
    id("me.whereareiam.semantica") version "0.2.0"
}

dependencies {
    implementation("me.whereareiam.semantica:source-file:0.2.0")

    annotationProcessor("me.whereareiam.semantica:semantica-processor:0.2.0")
}

semantica {
    locale.set("en")
}
```

The plugin passes `locale` to the processor, which writes the default files into `META-INF/semantica/<locale>/` of the compiled classes. Gradle must run on Java 21.

## Export and check

```shell
./gradlew translationsExport
```

compiles the product and copies the default files into `translations/en/`, replacing everything in it: `@Messages("identica")` gives `translations/en/identica.json`. Commit the result; Weblate takes it from there, and the other locale folders are Weblate's.

```shell
./gradlew check
```

runs `translationsCheck`, which fails when the committed `translations/en/` differs from the default files, naming each file that differs, is missing or is no longer declared:

```text
The committed texts in translations/en are not the declared defaults:
  identica.json differs
Run ./gradlew translationsExport and commit the result.
```

| Setting | Default | Meaning |
| --- | --- | --- |
| `translations` | `translations` | The folder of the product's translations. |
| `locale` | `en` | The locale the `@Default`s are written in, and so the folder the processor writes and the files are exported to. |
