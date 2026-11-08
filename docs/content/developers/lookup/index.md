---
title: Looking up texts
description: Texts and their values, locales and fallbacks, and what a lookup returns.
---

A `Text` is a key and the values of its arguments. It is immutable; `with` returns a new text:

```java
Text text = Text.of("shop.bought")
        .with("item", itemName)
        .with("count", 3);
```

## Values

| Value | In `{name}` | Also used by |
| --- | --- | --- |
| `String`, any `CharSequence` | as given | `select` |
| `Integer`, `Long`, `Double`, `BigDecimal`, any `Number` | in the locale's number style | `plural`, `selectordinal`, `{x, number}` |
| `Instant`, `ZonedDateTime`, `OffsetDateTime`, `LocalDate`, `LocalDateTime`, `LocalTime`, `Date` | as a date and time | `{x, date}`, `{x, time}` |
| `Boolean` | `true` or `false` | `select` with `true`/`false` branches |
| an enum constant | its name | `select`, by name or lowercase name |
| `Text` | resolved in the same locale | |
| anything else | left to the format, such as a component on Adventure | |

Dates without a zone are shown in the zone of the builder's `clock(...)`, which defaults to the system's.

## Resolving

```java
ResolvedMessage message = semantica.resolve(text, Locale.forLanguageTag("ru"));
ResolvedMessage chosen = semantica.resolve(text, List.of(chosenLocale, clientLocale));

String line = message.plain();
```

The locales are tried along the chain of [Fallback](../../format/fallback/index.md): each requested locale, its configured fallbacks, its base language, then the default locale and last the root locale, whose messages are the same in every language. Configure fallbacks on the builder:

```java
Semantica.builder()
        .defaultLocale(Locale.ENGLISH)
        .fallback(Locale.forLanguageTag("pt-BR"), Locale.forLanguageTag("pt-PT"))
        .fallback(Locale.forLanguageTag("uk"), Locale.forLanguageTag("ru"))
```

A `ResolvedMessage` holds the key, the locale its message came from, whether it was found, and its parts: literals, which translators wrote, and values, which the caller gave. Formats render the two differently, so values are never read as markup. `plain()` joins them as text; `PlainRenderer.withoutTags()` from `format-common` also removes MiniMessage tags, for logs and consoles.

A lookup never throws because of translations. A missing key shows the key; a missing value shows `{name}`; see [Issues](../../format/issues/index.md).

## Limits

```java
Semantica.builder()
        .maxDepth(10)       // references and nested texts inside one another
        .maxLength(32_768)  // characters of a resolved message
```

See [Limits](../../format/limits/index.md).

## Issues

Issues found while loading are on `semantica.getCatalog().getIssues()` and are passed to the builder's `issues(handler)` when they first appear. Issues found while resolving are passed to the handler once for each catalog. The default handler logs through the `System.Logger` named `Semantica`: errors as `ERROR`, warnings as `WARNING`.

A check of your own runs on every catalog:

```java
semantica.check(catalog -> catalog.keys(Locale.ENGLISH).stream()
        .filter(key -> key.length() > 64)
        .map(key -> new Issue(Issue.Kind.PLATFORM_RULE, key, Locale.ENGLISH, null, null, "Keys have at most 64 characters"))
        .toList());
```
