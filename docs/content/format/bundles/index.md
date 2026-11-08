---
title: Bundles
description: An organisation's translations repository, its semantica.json, and the bundles and manifest its CI builds.
---

Translations live in two kinds of places:

- **A product's own repository.** A plugin, mod or bot keeps its own texts beside its code and ships them, so anyone who runs it, inside the organisation or not, gets them.
- **An organisation's translations repository.** It holds only the organisation's own layer: the texts of its own products, and its own versions of other products' texts. Texts in it are named by topic, such as `game.chat` or `realm.vanilla.common`, never by plugin. It is edited through Weblate.

The organisation's CI checks every change and builds bundles: one file per product and locale, with only the keys that product uses. A product downloads the bundles of the locales it enables and polls a small manifest to learn when they change.

## The repository

```text
translations/
  semantica.json
  en/
    website/ dashboard/ launcher/ email/
    game/<topic>/                       shared by every realm: account, chat, economy
    realm/<realm>/common/...            one realm: create, vanilla
    realm/<realm>/mode/<mode>/...       a mode inside its realm: bedwars, skywars
  ru/
    ...
```

The locale folders follow [Files](../files/index.md): a file's path is the prefix of its keys. `semantica.json` at the root describes the repository:

```json
{
  "defaultLocale": "en",
  "markup": {
    "game":  { "format": "adventure" },
    "realm": { "format": "adventure", "allow": ["hover"] }
  },
  "products": {
    "create":  ["game", "realm.create.common"],
    "bedwars": ["game", "realm.vanilla.common", "realm.vanilla.mode.bedwars"],
    "website": ["website"]
  }
}
```

| Field | Meaning |
| --- | --- |
| `defaultLocale` | The locale every key exists in. |
| `products` | Each product and the key prefixes its bundles carry. A prefix takes whole segments: `realm.vanilla.common` takes `realm.vanilla.common.join.title`, not `realm.vanilla.commons`. A key no product names a prefix of is a `source` issue, since no bundle would carry it. |
| `markup` | The text format of the keys below a prefix, and so which markup is checked. Where several prefixes match a key, the longest wins. Keys below no prefix are not checked for markup. |
| `markup.<prefix>.format` | `adventure` (MiniMessage). |
| `markup.<prefix>.allow` | MiniMessage tag groups allowed beyond the defaults, such as `hover`, `click`, `insertion`. |

Markup and products are separate because they answer separate questions: how a text is written depends on where it is shown, while which bundle carries it depends on who uses it. A key of `game` is MiniMessage in every product that carries it.

## Bundles

The CI writes, into a folder it publishes:

```text
bundles/
  manifest.json
  bedwars/en.json
  bedwars/ru.json
  create/en.json
  website/en.json
  ...
```

A bundle, `<product>/<locale>.json`, is one flat JSON object of the keys below the product's prefixes that the locale has, sorted by key, with the messages as written:

```json
{
  "game.menu.title": "<gold>Menu</gold>",
  "realm.vanilla.common.join.title": "Welcome to Vanilla",
  "realm.vanilla.mode.bedwars.bed.lost": "<red>Your bed is gone!</red>"
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
    "bedwars": { "en": "9f86d081884c7d65...", "ru": "2c26b46b68ffc68f..." },
    "website": { "en": "fcde2b2edba56bf4..." }
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

A product is configured with the address of the bundles, its product name, the locales it enables and optionally the key prefixes it keeps. It:

1. reads `manifest.json`;
2. wants the bundles of its enabled locales, of each one's base language (`pt` for `pt-BR`) and of the default locale, where the manifest has them;
3. downloads a wanted bundle only when its hash differs from the one it holds, and uses it only when the SHA-256 of the downloaded bytes equals the manifest's hash;
4. keeps the bundle it held, and reports a `source` issue, when a download fails or does not match;
5. polls the manifest at an interval and loads again when the revision changes.

A product may keep the last manifest and bundles on disk, checked by the same hashes, to start when the address cannot be reached.
