---
title: Texts from configs
description: Let administrators write their own texts in configs, pull in translated ones with references, and check them when the configs load.
---

A config field that holds a text shown to players holds a `Content`: one string in the [message syntax](../../format/syntax/index.md). Administrators write their own text there, pull in a translated one with a reference, or both:

```yaml
announcements:
  welcome: "<gold>Welcome to the server, {player}!"     # their own text
  vote: "{@chirper.vote}"                               # the translated text, in each reader's language
  sale: "<gold>{@chirper.sale} <gray>(ends Sunday)"     # mixed
```

```java
Content welcome = Content.of(config.getString("announcements.welcome"));
```

A config's default names a declared message without a key string, through `Content.of(Message)`, which writes `{@chirper.vote}`:

```java
Content vote = Content.of(messages.vote());
```

The message has no values there: a content takes its values when it is resolved, and `Content.of` refuses a message that has some.

## Resolve it

```java
ResolvedMessage message = semantica.resolve(welcome, Map.of("player", Value.text(name)), List.of(chosenLocale, clientLocale));
Component component = renderer.render(message);
```

A content resolves as a message of the root locale: its references follow the reader's locale chain and see the content's values, so `{player}` fills in inside `chirper.vote` too, and its numbers and plural forms follow the first locale. Semantica parses a content the first time it is resolved and keeps it by its text, so resolving the same content again reads nothing.

A lookup never throws: a content that is no valid message is shown as written and reported as `syntax`; a reference no locale has shows `{@key}` and is reported as `missing-reference`.

## Check it

`semantica.check(content)` returns what is wrong with one content: its syntax error, or each reference no message of the default locale has. Check the configs when they load and reload, and report each issue with the file and the key it came from:

```java
for (Issue issue : semantica.check(welcome))
    logger.warning("announcements.yml announcements.welcome: " + issue.getDetail());
```

The catalog's checks, such as the markup check of a format, do not run on contents.
