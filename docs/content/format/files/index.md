---
title: Files
description: How a folder of translations is laid out, and how a file's path becomes the prefix of its keys.
---

Translations are stored in a folder with one folder per locale. Each file holds messages of one language, which is what Weblate edits; the path of a file inside its locale folder is the prefix of its keys.

```text
languages/
  en/
    game/menu.json          → game.menu.*
    common.json             → common.*
  ru/
    game/menu.json
    common.json
```

## Locale folders

A locale folder is named by its language tag in BCP 47 form: `en`, `ru`, `pt-BR`, `zh-Hant-TW`. A language has two or three letters. A folder that is no locale is reported as a `source` issue and not read.

The folder `all` holds the messages of the root locale: messages that are the same in every language (see [Fallback](../fallback/index.md)). An implementation may let the name be changed; Semantica's `FileSource.of(folder, configura, rootFolder)` does. In bundles and the manifest, the root locale is written `und`. Files directly in the folder, such as `semantica.json` or a README, are not read, and neither is any folder or file whose name starts with a dot.

## Keys

The path of a file inside its locale folder, without its extension and with `/` replaced by `.`, is the prefix of its keys. Inside the file, nested objects add their names. For `en/game/menu.json`:

```json
{
  "title": "Menu",
  "button": {
    "close": "Close"
  }
}
```

| Key | Message |
| --- | --- |
| `game.menu.title` | `Menu` |
| `game.menu.button.close` | `Close` |

Write a key's segments with letters, digits, `_` and `-`; a [reference](../references/index.md) can name no other key. A name with a dot inside a file, such as `"button.close"`, is read the same as the nested form.

A key written twice in one locale, in two files or in one, is reported as a `source` issue; the file read last, by path, wins.

## File types

| Extension | Content |
| --- | --- |
| `.json` | An object of messages and nested objects. |
| `.yml`, `.yaml` | The same in YAML. |
| `.properties` | `key=message` lines in UTF-8; keys are added to the prefix as written. |

A message is a string. A list of strings is a message of several lines, joined with a line break, which a format may show as separate lines, such as an item's lore. Numbers, booleans and lists of other values are reported as `source` issues. Any other file in a locale folder is reported as a `source` issue.

## Defaults

A product keeps its own texts in its own repository and ships them, so anyone who runs it gets them. A plugin writes its default locale's files into the server's folder, such as through Configura's defaults providers, and administrators change them or add other locales there. An organisation adds its own layer on top: its own products' texts and its own versions of other products' texts, from its [translations repository](../bundles/index.md) as bundles.
