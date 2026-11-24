---
title: Declaring messages
description: Declare a plugin's messages in code with @Messages, check their defaults when it compiles, and ship them in its jar.
---

A plugin declares its messages as methods of an interface. Each method is one message with its English default; the `semantica-processor` checks every default when the plugin compiles, writes the implementation and writes the defaults as files into the jar. No key string appears in the plugin's code, and a message without a default fails the build.

```kotlin
dependencies {
    implementation("me.whereareiam.semantica:semantica-common:0.2.0")
    implementation("me.whereareiam.semantica:source-file:0.2.0")

    annotationProcessor("me.whereareiam.semantica:semantica-processor:0.2.0")
}
```

## Declare them

```java
import me.whereareiam.semantica.Message;
import me.whereareiam.semantica.declaration.Default;
import me.whereareiam.semantica.declaration.Messages;

@Messages("commands")
public interface CommandMessages {
    @Default("<white>You must wait <gray>{time, number} seconds</gray> before using this command again.")
    Message cooldown(double time);

    @Default({"<gold>Socialismus <gray>{version}", "<gray>Reloaded in {millis, number} ms."})
    Message reloaded(String version, long millis);

    @Default("{@commands.prefix} <red>{player} is not online.")
    Message playerOffline(String player);

    @Default("<gray>[<gold>Server</gold>]")
    Message prefix();

    ReloadMessages reload();
}

public interface ReloadMessages {
    @Default("<green>Reloaded.")
    Message done();
}
```

| Declaration | Key |
| --- | --- |
| `@Messages("commands")` on the interface | the prefix of every key below it |
| `Message cooldown(double time)` | `commands.cooldown`, its argument `{time}` |
| `Message playerOffline(String player)` | `commands.player-offline`: method names are written in kebab case |
| `ReloadMessages reload()` | a group: its methods are `commands.reload.*` |

`@Default` holds the text in the default locale, written in the [message syntax](../../format/syntax/index.md); several strings are the lines of one message. The arguments of the text are the method's parameters by name, each passed on as its [kind of value](../lookup/index.md#values), so a number still chooses a plural form and a date is formatted for the reader. A parameter is text (a `String`, any `CharSequence` or a `char`), a number, a date or time, a `boolean`, an enum constant, a `Message` or a `Value`. A group is another interface returned by a method without parameters; its methods return messages or more groups.

## What the build checks

The processor fails the build on:

- a default that is no valid message;
- an argument that is no parameter, such as `{player}` on `cooldown(double time)`;
- a parameter of another type, such as `Object`;
- a `plural`, `selectordinal` or `{x, number}` over a parameter that is no number, and `{x, date}` or `{x, time}` over one that is no date;
- a reference to a key under one of the prefixes the compilation declares that none of its methods declares, such as `{@commands.prefx}`; references to other keys, such as another plugin's, are checked when the catalog loads;
- a method without `@Default`, two methods of one key, a method that returns neither a `Message` nor a group, a group that contains itself.

A parameter no argument uses is a warning, since a translation may still use it.

## Use them

The processor writes `GeneratedCommandMessages` beside the interface: a public class with a constructor without parameters and no reflection, whose methods return the messages with their keys and values.

```java
CommandMessages messages = new GeneratedCommandMessages();

Message cooldown = messages.cooldown(3.5);   // Message.of("commands.cooldown").with("time", 3.5)
player.sendMessage(renderer.render(semantica.resolve(cooldown, locale)));
```

## Ship the defaults

The defaults go into the jar as files of the [file layout](../../format/files/index.md), under `META-INF/semantica/<locale>/`, a file per interface named by its prefix: `META-INF/semantica/en/commands.json`, and `chirper/vote.json` for `@Messages("chirper.vote")`. A message of several lines is a list of strings.

`ClasspathSource` reads them as the lowest layer, below the folder administrators edit:

```java
Semantica semantica = Semantica.builder()
        .syntax(new IcuSyntax())
        .layer(Layer.of(ClasspathSource.of(CommandMessages.class, configura), Layer.DEFAULTS))
        .layer(Layer.of(FileSource.of(dataFolder.resolve("languages"), configura), Layer.OVERRIDES))
        .build();
```

It reads the files of the jar, or the folder of classes, that holds the class given, and of nothing else on its class loader, so an add-on's source never reads its host's files. See [Layers and sources](../layers/index.md).

The default locale is `en`; the processor option `semantica.locale` names another, and the Gradle plugin sets it from `semantica { locale }` and exports the files for translators; see [Exporting defaults](../gradle/index.md).

## Open sets of messages

Some messages come in a set nobody declares in one place, such as one per reason a condition can fail, where modules add reasons. A `@Segment` parameter is the last segment of the key instead of a value:

```java
import me.whereareiam.semantica.declaration.KeySegment;
import me.whereareiam.semantica.declaration.Segment;

@Messages("conditions")
public interface ConditionMessages {
    Message denied(@Segment ConditionReason reason, String player);   // conditions.denied.<reason.segment()>
}

public enum ConditionReason implements KeySegment {
    COOLDOWN, MUTED;

    @Override
    public String segment() {
        return name().toLowerCase(Locale.ROOT);
    }
}
```

The parameter is a `String` or a `KeySegment`. Such a method has no `@Default`: the text of each segment comes from whoever defines the reason, such as a module's own files. A reference to a key below it, such as `{@conditions.denied.cooldown}`, is not checked by the processor.
