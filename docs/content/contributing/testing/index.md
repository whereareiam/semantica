---
title: Testing
description: Where Semantica's tests live and what each covers.
---

Tests mirror the class they cover, in the same package.

| Test | Covers |
| --- | --- |
| `semantica-testing` `ConformanceTest` | Every case of the [translation format](../../format/conformance/index.md), against `Semantica` and `IcuSyntax`. A change to the format starts here. |
| `syntax-icu` `IcuSyntaxTest` | The parsed tree, quoting and syntax errors. |
| `semantica-api` `LocaleChainTest` | The order of locales. |
| `semantica-common` `SemanticaTest` | Layers, priorities, owners, watching, failing sources, checks, issue reporting, nested texts, dates. |
| `source-file` `FileSourceTest` | Folders, file types, issues, watching. |
| `source-http` `HttpSourceTest` | Manifests, scoped downloads, hashes, the disk cache and polling, against a local HTTP server. |
| `format-common` `PlainRendererTest` | Plain text with and without tags. |
| `format-adventure-common` `MarkupCheckTest` | Tags outside the policy, unknown tags, tags that differ from the default locale, Minecraft's locale codes. |
| `format-adventure-v4` and `-v5` `AdventureRendererTest`, `AdventureTranslatorTest`, `AuthorityLookupTest`, `TextArgumentTest` | Rendering, values never read as MiniMessage, policies, lore, the tags of each version, named and numbered arguments, keys left to the next source, the authority through Adventure's real `GlobalTranslator`, arguments made by a copy of Semantica in another class loader. Each runs against its own Adventure. |
| `source-file` `DefaultsExportTest` | Exporting a product's defaults: only text, files read back by `FileSource`, conflicts. |
| `semantica-gradle` `SemanticaPluginTest` | The plugin on a small product through Gradle TestKit: export, `check`, a changed default, stale and missing files. |
| `semantica-cli` `SemanticaCliTest` | `check` and `bundle` on a repository in a temporary folder. |

When behaviour of the format changes, change its page under `docs/content/format/` and its conformance cases in the same pull request; the TypeScript `feature-i18n` runs the same cases.
