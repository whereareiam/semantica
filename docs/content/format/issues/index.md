---
title: Issues
description: Every problem an implementation reports, when it is found, and what the reader sees instead.
---

A problem never stops a lookup. The affected message is left out or shown in a fallback form, and an issue is reported with its kind, key, locale, where the message came from and what is wrong. Issues found while translations load are kept with the loaded translations and reported when they first appear; issues found while a text is resolved are reported once for each loaded set of translations.

A translations repository's CI fails on errors and prints warnings.

## Found while loading

| Kind | Severity | Found when | The message |
| --- | --- | --- | --- |
| `source` | error | A file or bundle cannot be read, a folder is no locale, a key is written twice, no product in `semantica.json` names a prefix of a key. | Not read, or the other copy is used. |
| `syntax` | error | The message is no valid [syntax](../syntax/index.md). | Left out; a layer below with a valid message for the key is used instead. |
| `missing-reference` | error | A reference names a key no locale of the chain has. | Left out. |
| `reference-cycle` | error | References lead back into a cycle. | Left out. |
| `depth-limit` | error | References go deeper than the [limit](../limits/index.md). | Left out. |
| `missing-plural-form` | error | A plural lacks a category its language needs. | Used; those numbers take `other`. |
| `unknown-key` | error | A translation has a key the default locale lacks. | Used. |
| `argument-mismatch` | error | A translation uses an argument the default locale's message does not, or never shows one the default locale's message shows outside every branch. | Used; an unknown argument shows as `{name}`. |
| `markup` | error | A text format's markup is not allowed, or differs from the default locale's message. | Used; the markup shows as written. |
| `platform-rule` | error | A text breaks a rule of the place it is shown in, as a project's own check enforces it, such as the rules for slash command names. | As the project decides. |

An argument used only inside some branches of the default locale's message may be left out of a translation, since languages need different words per form: Russian often writes `one {одна жизнь}` without `#`.

## Found while resolving

| Kind | Severity | Found when | The reader sees |
| --- | --- | --- | --- |
| `missing-key` | warning | No locale of the chain has the key. | The key. |
| `missing-value` | warning | The text gives no value for an argument. | `{name}`; a plural or select takes `other`. |
| `invalid-value` | warning | A value does not suit its argument, such as text for a plural. | The value as text; a plural takes `other`. |
| `size-limit` | error | The message grows past the length [limit](../limits/index.md). | The next locale's message, or the key. |
| `depth-limit` | error | Nested texts go deeper than the limit. | The next locale's message, or the key. |
| `length-limit` | warning | A project cut the text to fit its place. | The text, cut. |

## Reported fields

| Field | Example |
| --- | --- |
| kind | `syntax` |
| key | `game.menu.title`, or none for a whole file |
| locale | `ru` |
| layer | the source or owner it came from, such as `resources:languages` |
| origin | where it is written, such as `ru/game/menu.json` |
| detail | `Unexpected end of the message; a '}' is missing (at 12)` |
