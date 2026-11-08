---
title: References
description: Reusing another message inside a message, and renaming its arguments.
---

A reference inserts another message, so shared words are translated once:

```json
{
  "vocabulary": {
    "server": "Arbor",
    "player": "{count, plural, one {# player} other {# players}}"
  },
  "status": "{@vocabulary.server}: {@vocabulary.player count=online} online"
}
```

`{@vocabulary.server}` inserts the message with the key `vocabulary.server`. The key is written in full; a reference never computes a key from a value.

## Arguments

A referenced message sees the referring message's arguments under their own names. `name=other` renames one: the referenced message's `name` takes the value of the referring message's `other`. In the example, `vocabulary.player` uses `count`, and `status` fills it from `online`:

```text
Text status, online = 3   →   Arbor: 3 players online
```

A rename names an argument on both sides; it never holds a literal value. Several renames are separated by spaces: `{@vocabulary.range from=start to=end}`. `#` of a surrounding plural is not passed on.

## Locales

A reference is looked up along the same locale chain as the text that contains it (see [Fallback](../fallback/index.md)). A German `status` uses the German `vocabulary.player` when there is one, and the English one otherwise.

## Nested texts

A program can do at runtime what a reference does in a file: give a text as the value of an argument. The nested text is resolved in the same locale chain:

```text
"bought": "You bought {item}."         item = text item.sword, count = 2   →   You bought 2 swords.
```

## Problems

| Problem | Issue | What the reader sees |
| --- | --- | --- |
| The key exists in no locale of the chain | `missing-reference` | The message is left out and falls back; a message reached anyway shows `{@key}` |
| References lead back to a message they started from | `reference-cycle` | Every message on or into the cycle is left out and falls back |
| References go deeper than the depth limit | `depth-limit` | The message is left out and falls back; see [Limits](../limits/index.md) |

The cycle and the depth are measured over the references of each locale's chain while translations load.
