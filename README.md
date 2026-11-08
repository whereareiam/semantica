# Semantica

**Semantica** is the translation engine shared by the plugins, mods and bots of an organisation. Messages are written in ICU MessageFormat with references to other messages, edited by translators in one repository, checked when they load, and looked up along one locale chain; text formats render them, such as Adventure components through MiniMessage, and never read the values a program gives as markup.

## A first text

```json
{
  "bought": "<green>You bought {count, plural, one {# {item}} other {# {item}s}}.</green>"
}
```

```java
AdventureRenderer renderer = AdventureRenderer.of(TagPolicy.defaults());
Semantica semantica = Semantica.builder()
        .syntax(new IcuSyntax())
        .defaultLocale(Locale.ENGLISH)
        .layer(Layer.of(FileSource.of(dataFolder.resolve("languages"), configura)))
        .check(renderer.check())
        .build();

GlobalTranslator.translator().addSource(AdventureTranslator.of(semantica, Key.key("shop", "translations"), renderer));
player.sendMessage(Component.translatable("shop.bought", TextArgument.of("item", itemName), TextArgument.of("count", 3)));
```

## Dependency

Semantica is published to `https://registry.whereareiam.me/maven/packages` and needs Java 21:

```kotlin
repositories {
    maven("https://registry.whereareiam.me/maven/packages")
}

dependencies {
    implementation("me.whereareiam.semantica:semantica-common:0.1.0")
    implementation("me.whereareiam.semantica:syntax-icu:0.1.0")
    implementation("me.whereareiam.semantica:source-file:0.1.0")
    implementation("me.whereareiam.semantica:format-adventure-v5:0.1.0")
}
```

## Documentation

The guides are in `docs/content/`: start with [Getting started](docs/content/developers/index.md). The [translation format](docs/content/format/index.md) is specified there too, with conformance cases other implementations run.

## License

Semantica is available under the [MIT License](LICENSE).
