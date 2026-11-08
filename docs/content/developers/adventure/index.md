---
title: Adventure
description: Render texts to Adventure components with MiniMessage, check their tags, and answer Adventure's GlobalTranslator, on Adventure 4 and 5.
---

The Adventure format renders resolved messages to components. Messages are MiniMessage; values are inserted as placeholders and never read as MiniMessage. It comes in one adapter per major version of Adventure, with the same classes in each:

| Artifact | Package | Adventure | For |
| --- | --- | --- | --- |
| `format-adventure-v5` | `me.whereareiam.semantica.format.adventure.v5` | 5.2 | Paper, Velocity, and NeoForge mods that bundle Adventure 5 |
| `format-adventure-v4` | `me.whereareiam.semantica.format.adventure.v4` | 4.25 and later 4.x | BungeeCord and Spigot through adventure-platform 4.x |

Both bring `format-adventure-common`, which holds what does not depend on Adventure: `TagPolicy`, `MarkupCheck` and `MinecraftLocales`. Adventure itself comes from the server (`compileOnly` in Semantica).

```kotlin
dependencies {
    implementation("me.whereareiam.semantica:format-adventure-v5:0.1.0")
}
```

## Rendering

```java
AdventureRenderer renderer = AdventureRenderer.of(TagPolicy.defaults());

Component title = renderer.render(semantica.resolve(Text.of("shop.title"), locale));
List<Component> lore = renderer.renderLines(semantica.resolve(Text.of("shop.item.lore").with("price", 20), locale));
```

The project decides the locale: a player's client locale, a locale a player chose, or the server's. Most plugins send translatable components instead and let Adventure ask the translator in each player's locale; see below.

| Value | Inserted as |
| --- | --- |
| text, numbers, dates, booleans, enums | text, formatted, never read as MiniMessage |
| a `Component` or any `ComponentLike` | the component as it is |
| a `Text` | its message, with its own MiniMessage |

`renderLines` splits the message at its line breaks, written as `\n` or as a list of strings in the file; each line is read as MiniMessage of its own, so a tag ends at the end of its line.

## Allowed tags

A translation may only use the tags of the renderer's `TagPolicy`. The defaults allow how text looks: colours, decorations, gradients, rainbows, transitions, shadows, resets and new lines. Tags that make text act, `click`, `hover` and `insertion`, must be added, since a translation could otherwise make a player run a command:

```java
AdventureRenderer renderer = AdventureRenderer.of(TagPolicy.defaults().allow(TagPolicy.Group.HOVER, TagPolicy.Group.CLICK));
```

A tag outside the policy is shown as written. Register the renderer's check so such tags are reported as `markup` issues whenever translations load:

```java
Semantica.builder()
        // ...
        .check(renderer.check())
        .build();
```

It reports tags outside the policy, names that are no MiniMessage tag (such as `<gren>`), and translations whose tags differ from the default locale's message. When one instance also holds texts that are not MiniMessage, limit it: `renderer.check(key -> key.startsWith("game."))`.

## Adventure's GlobalTranslator

Each plugin registers a translator built from its own instance, and sends translatable components with named arguments:

```java
AdventureTranslator translator = AdventureTranslator.of(semantica, Key.key("shop", "translations"), renderer);
GlobalTranslator.translator().addSource(translator);

player.sendMessage(Component.translatable("shop.bought",
        TextArgument.of("item", itemName),
        TextArgument.of("count", 3)));
```

The translator answers only keys its instance's default locale has, and returns `null` for any other, so the next source answers: vanilla keys stay with Minecraft, and another plugin's keys with that plugin.

### The authority

Adventure asks its sources in no fixed order, so registering first does not mean answering first. One translator per server, such as a network's, registers as the authority instead:

```java
GlobalTranslator.translator().addSource(AdventureTranslator.authority(networkSemantica, renderer));
```

It is named `semantica:authority` (`AdventureTranslator.AUTHORITY`). Every other Semantica translator, in any plugin, looks for it among `GlobalTranslator`'s sources and returns `null` for the keys it can translate, so the authority's texts win; keys only a plugin has stay with that plugin. A translator looks again every five seconds (`AUTHORITY_INTERVAL`), so an authority added or removed later is noticed within that time.

`TextArgument.of(name, value)` names an argument and keeps the value's type, so a number still chooses a plural form; it accepts anything `Text` does: text, numbers, dates, booleans, enums, components and nested texts. Other arguments, such as `Component.text("x")`, are numbered from `{0}`. Adventure's own `Argument.string(...)` is not read, since Adventure keeps its name private.

A translator reads `TextArgument`s made by any copy of Semantica, such as a plugin's arguments read by the network's authority in another class loader. It recognises them by Adventure's virtual components, the class's simple name `TextArgument` and the JDK's `Map.Entry`, so plugins may relocate Semantica as they like; only Adventure must be the server's own, unrelocated, as it is for any plugin that sends components. Values of JDK types (text, numbers, dates, booleans), enums and components cross between copies unchanged; a nested `Text` from another copy shows as its key and values, so pass nested texts only to your own translator.

## Minecraft's locale codes

```java
Locale locale = MinecraftLocales.toLocale("pt_br");   // pt-BR
String code = MinecraftLocales.toCode(Locale.US);     // en_us
```
