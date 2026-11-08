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

Then the default locale, and last the root locale. A locale already in the chain is not added again.

| Requested | Configured | Chain |
| --- | --- | --- |
| `en-US` | | `en-US`, `en`, root |
| `de-AT` | | `de-AT`, `de`, `en`, root |
| `pt-BR` | `pt-BR` → `pt-PT` | `pt-BR`, `pt-PT`, `pt`, `en`, root |
| `uk`, then `ru-RU` | | `uk`, `ru-RU`, `ru`, `en`, root |

A message left out because of an issue counts as missing in its locale, so the next locale of the chain is tried.

## The root locale

The root locale, written `und` (BCP 47's undetermined language, ICU's and CLDR's root), holds messages that are the same in every language, such as a message made only of references:

```json
{ "identica.login.failed": "{@game.prefix} {@game.account.login-failed}" }
```

- It is tried last, so a message of any real locale, including the default locale, wins over it.
- Its references and nested texts are looked up along the chain of the locale asked for, so a Russian reader sees the Russian `game.prefix`.
- Its numbers, dates and plural forms follow the first locale asked for: `{count, plural, ...}` in a root message chooses Russian forms for a Russian reader. Write the branches every language you serve needs, or keep plurals out of root messages.
- A root message is no translation: it may have keys the default locale lacks, and its arguments and markup are not compared with the default locale's.

## What the chain also decides

- References and nested texts are looked up along the same chain as the text that contains them.
- Plural categories and number and date formats follow the locale the message was taken from, so an English message reached from `de` reads as English throughout. A root message follows the locale asked for instead.

## No locale has the key

The reader sees the key itself, such as `game.menu.title`, and a `missing-key` issue is reported.
