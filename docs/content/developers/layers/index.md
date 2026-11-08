---
title: Layers and sources
description: Where translations come from, which wins, and how plugins add and remove their own.
---

A Semantica instance merges layers. Each layer has a source, which reads translations, a priority and optionally an owner. For each key and locale, the layer with the highest priority that has the key wins; between equal priorities, the layer added last wins.

| Priority | Constant | For |
| --- | --- | --- |
| 0 | `Layer.DEFAULTS` | Default texts. |
| 100 | `Layer.SHARED` | An organisation's bundles. |
| 200 | `Layer.OVERRIDES` | The server's own texts, such as its languages folder when bundles are also used. |

A message with a syntax error does not win: the next layer with a valid message for the key is used, and the error is reported.

## The languages folder

`FileSource` reads the layout of [Files](../../format/files/index.md) from a folder on disk, through a Configura 2.1.0 instance, and watches it:

```java
FileSource languages = FileSource.of(dataFolder.resolve("languages"), configura);
```

Saving files reloads the layer once things have been quiet for a moment. A folder that does not exist has no translations and is read once it is created.

The folder holds the plugin's own texts: the plugin writes its English defaults into `languages/en/`, such as through Configura's defaults providers, and administrators change them there or add other languages. Semantica only reads the folder.

## An organisation's bundles

`HttpSource` downloads the bundles an organisation's translations repository builds (see [Bundles](../../format/bundles/index.md)):

```java
import me.whereareiam.semantica.source.http.HttpSource;

HttpSource bundles = HttpSource.builder(URI.create("https://translations.oraylen.com/bundles/"))
        .product("intercept")
        .prefixes("game", "realm.vanilla")
        .locales(Set.of(Locale.ENGLISH, Locale.forLanguageTag("ru"), Locale.forLanguageTag("de")))
        .interval(Duration.ofMinutes(5))
        .cache(dataFolder.resolve("cache/translations"))
        .build();

Semantica semantica = Semantica.builder()
        .syntax(new IcuSyntax())
        .layer(Layer.of(bundles, Layer.SHARED))
        .layer(Layer.of(FileSource.of(dataFolder.resolve("languages"), configura), Layer.OVERRIDES))
        .build();
```

The source downloads the product's bundles for the enabled locales, their base languages and the default locale, checks each against the manifest's SHA-256, and polls the manifest; when its revision changes, Semantica loads the layer again and only changed bundles are downloaded. With `cache`, the last bundles are kept on disk and used when the server cannot be reached at start.

When the server cannot be reached later, Semantica keeps the bundles it has and reports a `source` issue.

## Layers of plugins

A product that loads add-ons of its own can give each add-on an owned layer, removed with the add-on:

```java
semantica.add(Layer.owned("shop", FileSource.of(addonsFolder.resolve("shop/languages"), configura)));

// the add-on stops
semantica.remove("shop");

// the add-on reloads: its layers are replaced at once, never missing in between
semantica.replace("shop", Layer.owned("shop", FileSource.of(addonsFolder.resolve("shop/languages"), configura)));
```

`add`, `remove` and `replace` build one new catalog each and replace the old one at once. `add` takes several layers in one call.

## A source of your own

A source reads messages as text, with where each was written, and may tell Semantica when they change:

```java
import me.whereareiam.semantica.source.Source;

public final class DatabaseSource implements Source {
    @Override
    public @NotNull String getName() {
        return "database";
    }

    @Override
    public @NotNull Content load() throws IOException {
        Content.Builder content = Content.builder();
        for (Row row : database.overrides())
            content.put(Locale.forLanguageTag(row.locale()), row.key(), row.text(), "database#" + row.id());

        return content.build();
    }

    @Override
    public @NotNull AutoCloseable watch(@NotNull Runnable onChange) {
        return database.onOverridesChanged(onChange);
    }
}
```

A problem with a single entry goes into the content as an issue (`content.issue(...)`), so the rest still loads. An exception from `load()` means nothing could be read; Semantica keeps what the source gave last time and reports a `source` issue.

## Reloading and watching

- `semantica.reload()` loads every layer again, such as for a reload command.
- `onChange(listener)` is called with each new catalog.
- `Semantica.builder().watch(false)` turns watching off, such as in tests.
- `close()` stops watching. Lookups keep working on the last catalog.
