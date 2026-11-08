---
title: Conformance
description: The JSON cases every implementation of the format runs, and how to run them.
---

The cases in `semantica-testing/src/main/resources/conformance/` describe the format by example. Semantica runs them in `ConformanceTest`; another implementation, such as the web's `feature-i18n`, runs the same files to show that it reads messages alike.

## A case file

```json
{
  "description": "Plural forms by CLDR's rules, exact values, offsets, # and ordinals.",
  "defaultLocale": "en",
  "fallbacks": { "pt-BR": ["pt-PT"] },
  "limits": { "maxDepth": 3, "maxLength": 20 },
  "messages": {
    "en": { "items": "{count, plural, one {# item} other {# items}}" },
    "ru": { "items": "{count, plural, one {# предмет} few {# предмета} many {# предметов} other {# предмета}}" }
  },
  "cases": [
    { "key": "items", "locale": "ru", "values": { "count": 3 }, "expected": "3 предмета", "resolvedLocale": "ru" },
    { "key": "items", "locale": "en", "values": { "count": "x" }, "expected": "x items", "issues": [ { "kind": "invalid-value", "key": "items" } ] }
  ],
  "issues": [
    { "kind": "missing-plural-form", "locale": "ru", "key": "items" }
  ]
}
```

| Field | Meaning |
| --- | --- |
| `defaultLocale` | The default locale. |
| `fallbacks` | Optional configured fallbacks, by locale. |
| `limits` | Optional limits: `maxDepth`, `maxLength`. Otherwise the [defaults](../limits/index.md). |
| `messages` | The messages, by locale tag and key, as one source. |
| `issues` | Exactly the issues found while loading, by kind, locale and key. |
| `cases[].key`, `cases[].locale` | The text asked for, in one requested locale. |
| `cases[].values` | Values: a JSON number (integral numbers are integers, others decimals), string or boolean, or `{"text": {"key": ..., "values": {...}}}` for a nested text. |
| `cases[].expected` | The resolved message as plain text: the text as written, with values formatted. |
| `cases[].resolvedLocale` | Optional: the locale the message was taken from. |
| `cases[].issues` | Exactly the issues found while resolving this case, by kind and key. Each case starts from freshly loaded messages. |

Issues are compared as sets of their fields listed; their details and order are free.

## Running them

In Semantica:

```shell
./gradlew :semantica-testing:test
```

An implementation in another language reads every `*.json` of the folder and, for each file, loads its messages once to compare the load issues, and once more per case to resolve it. The files are part of the published `semantica-testing` jar under `conformance/`.
