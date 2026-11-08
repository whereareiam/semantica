---
title: Overview
description: The translation format shared by Semantica, the web's feature-i18n and every translations repository.
---

Every product of an organisation, the game, the Discord bot, the website and the launcher, reads its texts in one format. Translators edit them in one place, a translations repository per organisation, through Weblate. This section specifies that format, so that every implementation reads a message the same way: Semantica in Java, and `feature-i18n` in TypeScript.

The format covers:

| Part | Page |
| --- | --- |
| How a message is written: arguments, plurals, choices, quoting | [Syntax](./syntax/index.md) |
| How a message reuses another | [References](./references/index.md) |
| How messages are stored: folders, files, keys | [Files](./files/index.md) |
| How a repository is shipped: `semantica.json`, bundles and the manifest | [Bundles](./bundles/index.md) |
| Which locale a message is taken from | [Fallback](./fallback/index.md) |
| How deep and how long a message may grow | [Limits](./limits/index.md) |
| What is reported, and what the reader sees instead | [Issues](./issues/index.md) |
| The cases every implementation must pass | [Conformance](./conformance/index.md) |

## A message at a glance

```json
{
  "bought": "You bought {count, plural, one {# {item}} other {# {item}s}} for {price, number}.",
  "status": "{@vocabulary.server}: {@vocabulary.player count=online} online"
}
```

`{count, plural, ...}` picks a form by the number's plural category in the reader's language, `{price, number}` formats a number for the reader's locale, and `{@vocabulary.server}` inserts another message.

## What the format leaves to text formats

Markup is not part of the format. A message is text with arguments; how its text is styled belongs to where it is shown: MiniMessage tags such as `<gold>` in the game, markdown such as `**bold**` on Discord, plain text on the web. To the format, `<gold>` and `**` are ordinary characters. Each text format says which markup it reads and checks it; Semantica's Adventure format is described in [Adventure](../developers/adventure/index.md), and Discord's markdown belongs to the bot.

Values given by the program are never read as markup or as part of the message: a player named `{@x}` or `<red>` is shown as written.

## Terms

| Term | Meaning |
| --- | --- |
| key | The name of a message, segments separated by dots: `game.menu.title`. |
| namespace | The first segment of a key: `game`. A repository lists its namespaces and which products receive them. |
| locale | A language tag in BCP 47 form: `en`, `pt-BR`, `zh-Hant-TW`. |
| default locale | The locale every key exists in, and the last one tried. Usually `en`. |
| argument | A named value a message inserts or chooses by: `{name}`. |
| text | What a program asks for: a key and the values of its arguments. |
| issue | A problem found in a message, reported by kind; see [Issues](./issues/index.md). |
