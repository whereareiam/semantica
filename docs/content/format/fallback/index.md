---
title: Fallback
description: The order in which locales are tried for a message.
---

A text is asked for in one or more locales: a player's client locale; a Discord user's locale, then their guild's; a locale a user chose first. The message is taken from the first locale of the chain that has the key.

## The chain

For each requested locale, in order:

1. the locale itself;
2. its configured fallbacks, each followed by its own chain;
3. the locale with its variant, region and script removed one at a time: `sr-Latn-RS`, then `sr-Latn`, then `sr`.

Then the default locale. A locale already in the chain is not added again.

| Requested | Configured | Chain |
| --- | --- | --- |
| `en-US` | | `en-US`, `en` |
| `de-AT` | | `de-AT`, `de`, `en` |
| `pt-BR` | `pt-BR` → `pt-PT` | `pt-BR`, `pt-PT`, `pt`, `en` |
| `uk`, then `ru-RU` | | `uk`, `ru-RU`, `ru`, `en` |

A message left out because of an issue counts as missing in its locale, so the next locale of the chain is tried.

## What the chain also decides

- References and nested texts are looked up along the same chain as the text that contains them.
- Plural categories and number and date formats follow the locale the message was taken from, so an English message reached from `de` reads as English throughout.

## No locale has the key

The reader sees the key itself, such as `game.menu.title`, and a `missing-key` issue is reported.
