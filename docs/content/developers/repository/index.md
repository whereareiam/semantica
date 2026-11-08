---
title: Translations repository
description: Check an organisation's translations in CI and build the bundles its products download.
---

An organisation keeps its own layer of translations in one repository: its own products' texts, and its own versions of other products' texts, named by topic. It is laid out as in [Bundles](../../format/bundles/index.md) and edited through Weblate; the products themselves ship their own texts. The repository's CI runs `semantica-cli`, which loads the translations as Semantica does, with the markup checks `semantica.json` names, and writes one bundle per product and locale.

## The command

`semantica-cli` is published as a runnable jar with the classifier `all`:

```shell
curl -fsSLo semantica-cli.jar \
  https://registry.whereareiam.me/maven/packages/me/whereareiam/semantica/semantica-cli/0.1.0/semantica-cli-0.1.0-all.jar

java -jar semantica-cli.jar check translations
java -jar semantica-cli.jar bundle translations dist --revision "$GITHUB_SHA"
```

| Command | Does | Exits with |
| --- | --- | --- |
| `check [folder]` | Prints every issue and the share of keys each locale translates. | 0 when there is no error, 1 when there is, 2 when `semantica.json` is wrong |
| `bundle [folder] [output] [--revision value]` | Checks, then writes `<output>/<product>/<locale>.json` and `<output>/manifest.json`. | as `check` |

The folder defaults to the current folder and the output to `bundles`. The output must be empty or hold earlier bundles; it is replaced as a whole. The revision defaults to `GITHUB_SHA`, and otherwise to a hash of the bundles.

## What `check` reports

Everything the engine reports while loading (see [Issues](../../format/issues/index.md)), plus:

- for keys below a `markup` prefix with the format `adventure`: MiniMessage tags outside the defaults and that prefix's `allow`, judged by Adventure 5's tags, and tags that differ from the default locale;
- keys no product names a prefix of, since no bundle would carry them, and a default locale without a folder.

Errors fail the check; warnings are printed. On GitHub Actions each issue is also an annotation on its file.

```text
error: syntax ru game.menu.title (ru/game/menu.json): Unexpected end of the message; a '}' is missing (at 12)
error: markup de game.shop.buy (de/game/shop.json): Uses other tags than en; lacks [gold]
ru: 412/430 keys (95%)
2 errors, 0 warnings.
```

## A workflow

```yaml
name: Translations

on:
  push:
    branches: [main]
  pull_request:

jobs:
  translations:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: 21
      - run: curl -fsSLo semantica-cli.jar https://registry.whereareiam.me/maven/packages/me/whereareiam/semantica/semantica-cli/0.1.0/semantica-cli-0.1.0-all.jar
      - run: java -jar semantica-cli.jar check .
      - if: github.event_name == 'push'
        run: java -jar semantica-cli.jar bundle . ../bundles
      # publish ../bundles where products read it, such as a bundles branch or a static site
```

Products then read the bundles with an [`HttpSource`](../layers/index.md) pointed at the published folder.
