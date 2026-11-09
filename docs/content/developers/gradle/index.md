---
title: Exporting defaults
description: Make a product's default texts, written as Configura defaults in its code, the source translators work from.
---

A product's default texts live in its code, as Configura defaults providers, so the plugin can write them into a server's `languages` folder. Translators work from files instead: `translations/en/` in the product's repository, which Weblate reads. The Gradle plugin `me.whereareiam.semantica` keeps the two the same: it exports the defaults into those files, and fails the build when the committed files fall behind the code.

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
    id("me.whereareiam.semantica") version "0.1.0"
}

dependencies {
    implementation("me.whereareiam.semantica:source-file:0.1.0")
}

semantica {
    defaults("me.whereareiam.identica.message.MessageDefaults")
}
```

The project needs `source-file` on its runtime classpath, as every product reading a `languages` folder already has. Gradle must run on Java 21.

## Name the defaults

The class named in `defaults(...)` implements `TranslationDefaults` and returns the files of the default locale. Each file is named by its path inside the locale folder, which is the prefix of its keys, and holds the tree Configura writes for the product's defaults:

```java
package me.whereareiam.identica.message;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.Configura;
import me.whereareiam.identica.common.config.defaults.messages.MessagesDefaults;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.semantica.source.file.TranslationDefaults;

import java.util.Map;

public final class MessageDefaults implements TranslationDefaults {
    @Override
    public Map<String, JsonNode> files() {
        Configura configura = Configura.builder().build();
        return Map.of("identica", configura.mapper().valueToTree(new MessagesDefaults().supply(new Messages())));
    }
}
```

Use the same Configura settings the product writes its files with, so the exported keys are the keys it reads. The class needs a public constructor without parameters and must run without the product: no server, no injection.

## Export and check

```shell
./gradlew translationsExport
```

runs the defaults on the runtime classpath and writes `translations/en/identica.json`, replacing everything in `translations/en/`. Only text is exported: strings, and lists of strings as messages of several lines. Numbers, booleans and other settings are left out, and the build prints each one it leaves out. Commit the result; Weblate takes it from there, and the other locale folders are Weblate's.

```shell
./gradlew check
```

runs `translationsCheck`, which exports again into `build/` and fails when the committed `translations/en/` differs, naming each file that differs, is missing or is no longer exported:

```text
The committed texts in translations/en are not what the defaults export:
  identica.json differs
Run ./gradlew translationsExport and commit the result.
```

| Setting | Default | Meaning |
| --- | --- | --- |
| `defaults(...)` | none | The classes implementing `TranslationDefaults`. Several may be named; two may not write the same file. |
| `translations` | `translations` | The folder of the product's translations. |
| `locale` | `en` | The locale the defaults are written in, and so the folder they go to. |
