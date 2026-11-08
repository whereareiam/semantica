---
title: Limits
description: How deep references may go and how long a resolved message may grow.
---

References and nested texts make a message grow at runtime. Two limits keep a broken or hostile message from growing without end. Both are configurable; these are Semantica's defaults.

| Limit | Default | Measured |
| --- | --- | --- |
| depth | 10 | References and nested texts inside one another; a message with no reference has depth 0. |
| length | 32768 characters | The resolved message: its text and its formatted values. |

## Depth

The depth of references is measured when translations load. A message whose references go deeper than the limit is reported as `depth-limit` and left out, and falls back to the next locale. Nested texts are given only at runtime; when they push a message past the limit, the message is given up on in that locale, reported, and the next locale of the chain is tried.

## Length

A message that grows past the length while it is resolved is given up on in that locale and reported as `size-limit`; the next locale of the chain is tried. When no locale is left, the reader sees the key.

## Room where a text is shown

Some places hold limited text, such as a Discord embed's title. Cutting to those lengths is the work of the project that shows the text; it reports a cut as `length-limit`.
