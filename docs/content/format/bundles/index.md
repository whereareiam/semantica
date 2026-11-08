---
title: Bundles
description: The translations repository of an organisation, its semantica.json, and the bundles and manifest its CI builds.
---

Each organisation keeps its translations in one repository, edited through Weblate. Its CI checks every change and builds bundles: one file per product and locale with only the namespaces that product uses. A product downloads the bundles of the locales it enables and polls a small manifest to learn when they change.

## The repository

```text
translations/
  semantica.json
  en/
    game/menu.json
    discord/ticket.json
    common/unit.json
  ru/
    ...
```

The locale folders follow [Files](../files/index.md). `semantica.json` at the root describes the repository:

```json
{
  "defaultLocale": "en",
  "namespaces": {
    "game":    { "format": "adventure", "allow": ["hover"] },
    "discord": {},
    "common":  {}
  },
  "products": {
    "intercept": ["game", "common"],
    "yui":       ["discord", "common"]
  }
}
```

| Field | Meaning |
| --- | --- |
| `defaultLocale` | The locale every key exists in. |
| `namespaces` | Every namespace, the first segment of a key, that the repository holds. A key of a namespace not listed here is a `source` issue. |
| `namespaces.<name>.format` | The text format of its messages, and so which markup is checked: `adventure` (MiniMessage). Without it, the markup is not checked. |
| `namespaces.<name>.allow` | For `adventure`: MiniMessage tag groups allowed beyond the defaults, such as `hover`, `click`, `insertion`. |
| `products` | Each product and the namespaces it receives. |

## Bundles

The CI writes, into a folder it publishes:

```text
bundles/
  manifest.json
  intercept/en.json
  intercept/ru.json
  yui/en.json
  ...
```

A bundle, `<product>/<locale>.json`, is one flat JSON object of the keys of the product's namespaces that the locale has, sorted by key, with the messages as written:

```json
{
  "common.unit": "{count, plural, one {# unit} other {# units}}",
  "game.menu.title": "<gold>Menu</gold>"
}
```

A bundle holds only the messages of its own locale; falling back happens in the product (see [Fallback](../fallback/index.md)). A locale with no message for a product has no bundle for it.

## The manifest

`manifest.json` lists every bundle with the SHA-256 of its bytes, as lowercase hex:

```json
{
  "format": 1,
  "revision": "4f2c1a9",
  "defaultLocale": "en",
  "bundles": {
    "intercept": { "en": "9f86d081884c7d65...", "ru": "2c26b46b68ffc68f..." },
    "yui": { "en": "fcde2b2edba56bf4..." }
  }
}
```

| Field | Meaning |
| --- | --- |
| `format` | The layout of the manifest; `1`. A reader refuses other values. |
| `revision` | Changes whenever any bundle changes, such as the commit the bundles were built from. |
| `defaultLocale` | As in `semantica.json`. |
| `bundles` | The hash of each bundle, by product and locale tag. |

## Reading bundles

A product is configured with the address of the bundles, its product name, the locales it enables and optionally the namespaces it keeps. It:

1. reads `manifest.json`;
2. wants the bundles of its enabled locales, of each one's base language (`pt` for `pt-BR`) and of the default locale, where the manifest has them;
3. downloads a wanted bundle only when its hash differs from the one it holds, and uses it only when the SHA-256 of the downloaded bytes equals the manifest's hash;
4. keeps the bundle it held, and reports a `source` issue, when a download fails or does not match;
5. polls the manifest at an interval and loads again when the revision changes.

A product may keep the last manifest and bundles on disk, checked by the same hashes, to start when the address cannot be reached.
