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
| `semantica-common` `SemanticaTest` | Layers, priorities, owners, watching, failing sources, checks, issue reporting, nested messages, dates. |
| `semantica-common` `SemanticaContentTest` | Contents: own text, references in the reader's locale with the content's values, syntax errors, `check`. |
| `source-file` `FileSourceTest` | Folders, file types, issues, watching. |
| `source-file` `ClasspathSourceTest` | A `@Messages` interface of the test sources compiled with the processor: its generated class, its default files read from the class folder and resolved below a folder, and a jar read without the files of others. |
| `semantica-processor` `MessagesProcessorTest`, `KeysTest` | Sources compiled in the test with the processor: the implementation, the default files and their locale, groups, segments, and every error and warning of a declaration; method names as key segments. |
| `source-http` `HttpSourceTest` | Manifests, scoped downloads, hashes, the disk cache and polling, against a local HTTP server. |
| `format-common` `PlainRendererTest` | Plain text with and without tags. |
| `format-adventure-common` `MarkupCheckTest` | Tags outside the policy, unknown tags, tags that differ from the default locale, Minecraft's locale codes. |
| `format-adventure-v4` and `-v5` `AdventureRendererTest`, `AdventureTranslatorTest`, `AuthorityLookupTest`, `TextArgumentTest` | Rendering, values never read as MiniMessage, policies, lore, a `Rendering`'s text functions and tags on literal parts only, the tags of each version, named and numbered arguments, keys left to the next source, the authority through Adventure's real `GlobalTranslator`, arguments made by a copy of Semantica in another class loader. Each runs against its own Adventure. |
| `semantica-gradle` `SemanticaPluginTest` | The plugin on a small product with a `@Messages` interface through Gradle TestKit: export, the locale, `check`, a changed default, stale and missing files, a default that fails the compilation. |
| `semantica-cli` `SemanticaCliTest` | `check` and `bundle` on a repository in a temporary folder. |

When behaviour of the format changes, change its page under `docs/content/format/` and its conformance cases in the same pull request; the TypeScript `feature-i18n` runs the same cases.
