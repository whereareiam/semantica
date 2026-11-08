---
title: Getting started
description: Add Semantica to a plugin, mod or bot and show a first translated text.
---

Semantica is the translation engine of a plugin, mod or bot: it loads messages written in the [translation format](../format/index.md) from a folder or from an organisation's bundles, merges them, checks them once when they load, and resolves texts in a reader's locale. A text format renders the result, such as Adventure components through MiniMessage. Semantica knows nothing about players, servers or bots: the project decides which locale a reader has and where a text is shown.

This page sets up one Semantica instance in a Paper or Velocity plugin and shows a first text. To change Semantica itself, see [Contributing](../contributing/building/index.md).

## Dependencies

Semantica is published to the [public Maven registry](https://registry.whereareiam.me/maven/packages) under the group `me.whereareiam.semantica`, and needs Java 21.

| Artifact | Holds |
| --- | --- |
| `semantica-common` | `Semantica`, the instance; brings `semantica-api` (`Text`, `Layer`, `Issue`, ...) |
| `syntax-icu` | `IcuSyntax`, the message syntax |
| `source-file` | `FileSource`; see [Layers and sources](./layers/index.md) |
| `source-http` | `HttpSource`; see [Layers and sources](./layers/index.md) |
| `format-common` | `Renderer` and `PlainRenderer`, for plain text in logs |
| `format-adventure-v5` | Adventure 5.2 (Paper, Velocity); see [Adventure](./adventure/index.md) |
| `format-adventure-v4` | Adventure 4.25 (BungeeCord, Spigot); see [Adventure](./adventure/index.md) |
| `semantica-testing` | `MemorySource`, `IssueAssert`; see [Testing](./testing/index.md) |
| `semantica-cli` | the command a translations repository's CI runs; see [Translations repository](./repository/index.md) |

```kotlin
repositories {
    maven("https://registry.whereareiam.me/maven/packages")
}

dependencies {
    implementation("me.whereareiam.semantica:semantica-common:0.1.0")
    implementation("me.whereareiam.semantica:syntax-icu:0.1.0")
    implementation("me.whereareiam.semantica:source-file:0.1.0")
    implementation("me.whereareiam.semantica:format-adventure-v5:0.1.0")

    testImplementation("me.whereareiam.semantica:semantica-testing:0.1.0")
}
```

`semantica-common` brings ICU4J for CLDR's plural rules and formats; `source-file` brings Configura 2.1.0. Adventure comes from the server.

## The languages folder

A plugin reads its texts from its `languages` folder, one folder per locale (see [Files](../format/files/index.md)):

```text
plugins/Shop/languages/
  en/shop.json
  ru/shop.json
```

```json
{
  "bought": "<green>You bought {count, plural, one {# {item}} other {# {item}s}}.</green>",
  "empty": "Your basket is empty."
}
```

The plugin writes its English defaults into `languages/en/` itself, such as through Configura's defaults providers, so administrators find every text where they change it. `en/shop.json` holds the keys `shop.bought` and `shop.empty`.

## Build the instance

Each plugin owns one instance. Build it when the plugin starts:

```java
import me.whereareiam.configura.Configura;
import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.common.Semantica;
import me.whereareiam.semantica.format.adventure.TagPolicy;
import me.whereareiam.semantica.format.adventure.v5.AdventureRenderer;
import me.whereareiam.semantica.format.adventure.v5.AdventureTranslator;
import me.whereareiam.semantica.source.file.FileSource;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;

import java.nio.file.Path;
import java.util.Locale;

public final class Translations {
    private final Semantica semantica;
    private final AdventureTranslator translator;

    public Translations(Path dataFolder, Configura configura) {
        AdventureRenderer renderer = AdventureRenderer.of(TagPolicy.defaults());

        semantica = Semantica.builder()
                .syntax(new IcuSyntax())
                .defaultLocale(Locale.ENGLISH)
                .layer(Layer.of(FileSource.of(dataFolder.resolve("languages"), configura)))
                .check(renderer.check())
                .build();

        translator = AdventureTranslator.of(semantica, Key.key("shop", "translations"), renderer);
        GlobalTranslator.translator().addSource(translator);
    }

    public void close() {
        GlobalTranslator.translator().removeSource(translator);
        semantica.close();
    }
}
```

`build()` loads the folder, parses each message once and runs the checks; Semantica watches the folder and loads it again when a file changes. The translator answers Adventure for the keys this instance has.

## Show a text

Send a translatable component with named arguments; Adventure asks the translator in each player's locale:

```java
import me.whereareiam.semantica.format.adventure.v5.TextArgument;
import net.kyori.adventure.text.Component;

player.sendMessage(Component.translatable("shop.bought",
        TextArgument.of("item", itemName),
        TextArgument.of("count", 3)));
```

A Russian client sees the Russian message once `ru/shop.json` has it, in green; others see English. An item name given as a value is shown as written, never read as MiniMessage.

## What success looks like

The log shows no issues at start, and `semantica.getCatalog().getIssues()` is empty. When a message is broken, Semantica logs it through the `System.Logger` named `Semantica`, such as:

```text
syntax ru shop.bought (ru/shop.json): Unexpected end of the message; a '}' is missing (at 41)
```

and Russian readers see the English message until it is fixed. [Issues](../format/issues/index.md) lists every kind; pass `.issues(handler)` to the builder to send them elsewhere.

## Where to go next

- [Layers and sources](./layers/index.md): an organisation's bundles, plugin layers, sources of your own.
- [Looking up texts](./lookup/index.md): values, locales, fallbacks and limits.
- [Adventure](./adventure/index.md): rendering, tags, Adventure 4 and 5.
