---
title: Modules
description: How the repository is laid out, which module owns what, and how a message moves through Semantica.
---

## Module map

| Gradle project | Directory | Owns |
| --- | --- | --- |
| `semantica-api` | `semantica-api/` | The contracts and models: `Message`, `Value`, `Content`, the declarations `Messages`, `Default`, `Segment` and `KeySegment`, `Catalog`, `Layer`, `Source`, `LocaleChain`, `ParsedMessage`, `ResolvedMessage`, `Syntax`, `Issue`, `Check`, `SyntaxException`. No dependencies beyond the JDK and annotations. |
| `semantica-common` | `semantica-common/` | `Semantica` and the engine: building catalogs, checks, resolution, formatting. Uses ICU4J. |
| `syntax-icu` | `semantica-syntax/syntax-icu/` | `IcuSyntax`, the parser of ICU MessageFormat with references. Depends on the api only. |
| `source-file` | `semantica-source/source-file/` | `FileSource`: a folder on disk, watched; `ClasspathSource`: the default files of the jar or class folder that holds a class. Both read through Configura 2.1.0 and `TranslationFiles`. |
| `source-http` | `semantica-source/source-http/` | `HttpSource`, the `Manifest` and `Bundle` formats it reads and `semantica-cli` writes, and the `BundleLayout` it finds them by. Uses Jackson. |
| `:semantica-format:common` (artifact `format-common`) | `semantica-format/common/` | `Renderer` and `PlainRenderer`. Depends on the api only. |
| `format-adventure-common` | `semantica-format/type/format-adventure/common/` | `TagPolicy`, `MarkupCheck` (its logic, given a `MiniMessageTags`), `MiniMessageTags`, `MinecraftLocales`. No Adventure types. |
| `format-adventure-v4` | `semantica-format/type/format-adventure/adventure-v4/` | `AdventureRenderer`, `AdventureTags`, `AdventureTranslator` (with `AuthorityLookup` for the authority), `TextArgument`, `ComponentValue` (with `ValueExchange`, the JDK and Adventure types values cross class loaders as) for Adventure 4.25, `compileOnly`. |
| `format-adventure-v5` | `semantica-format/type/format-adventure/adventure-v5/` | The same classes for Adventure 5.2, `compileOnly`. |
| `semantica-processor` | `semantica-processor/` | `MessagesProcessor`, the annotation processor of `@Messages` interfaces: `GroupReader` reads an interface, `DefaultCheck` checks each default with `IcuSyntax`, `ImplementationWriter` and `DefaultsWriter` write `Generated<Interface>` and `META-INF/semantica/<locale>/`. Depends on the api and `syntax-icu`. |
| `semantica-testing` | `semantica-testing/` | `MemorySource`, `IssueAssert`, and the conformance cases under `src/main/resources/conformance/`. |
| `semantica-gradle` | `semantica-gradle/` | The Gradle plugin `me.whereareiam.semantica`: `SemanticaPlugin`, `SemanticaExtension`, the tasks `TranslationsExport` and `TranslationsCheck`. It passes the locale to the processor and copies or compares the files the processor wrote; it depends on nothing of Semantica itself. |
| `semantica-cli` | `semantica-cli/` | `SemanticaCli` with `check` and `bundle`, `RepositorySettings` (`semantica.json`) and `TranslationsCheck`, which judges MiniMessage by `format-adventure-v5` and texts for Minecraft's lang files by `MinecraftCheck`. Published with a jar of all its dependencies. |

`build-logic/` holds the Gradle conventions: `module-java` (group, Java 21 toolchain, Lombok, JetBrains annotations, JUnit, sources and Javadoc jars), `module-api` (`module-java` with publication) and `packaging-publication` (Maven publication through the toolkit).

## Inside `semantica-common`

| Package | Class | Role |
| --- | --- | --- |
| `common` | `Semantica` | Owns the layers, their last content and their watches; rebuilds and replaces the catalog under one lock; resolves texts; reports issues. |
| `common.catalog` | `CatalogBuilder` | Merges layers per key and locale, parses each message once (keeping parsed messages from the last build by their text), leaves out messages with errors and runs the checks. |
| `common.catalog` | `ReferenceAnalysis` | Follows references along each locale's chain and leaves out missing, cyclic and too deep ones. |
| `common.check` | `PluralFormCheck`, `UnknownKeyCheck`, `ArgumentCheck` | The engine's own checks, run on every catalog. |
| `common.resolution` | `MessageResolver` | Walks a parsed message with the message's values: branches, references, nested messages, limits, fallback to the next locale; a content's message as one of the root locale. |
| `common.resolution` | `ContentParser` | Parses contents once and keeps the most recently used by their text, independent of the catalog. |
| `common.resolution` | `ValueFormatter` | Numbers, dates and relative times through ICU4J, and CLDR plural categories. |

## How a message moves through Semantica

1. `Semantica.builder().build()` loads each layer's source, outside the lock, and gives the content to `CatalogBuilder`.
2. `CatalogBuilder` orders the layers by priority and order of adding, takes for each key and locale the highest layer whose message parses, runs `ReferenceAnalysis`, then every `Check`, and returns an immutable `Catalog`.
3. `Semantica` replaces its catalog in one write, reports new issues and calls its listeners.
4. `resolve(message, locales)` takes the current catalog once, asks `LocaleChain` for the chain and gives both to `MessageResolver`, which returns a `ResolvedMessage`.
5. A format renders the `ResolvedMessage`: literals as markup, values as inserted data.

`resolve(content, values, locales)` does the same with the content's message, parsed by `ContentParser` instead of taken from the catalog.

A watched source calls back on its own thread; `Semantica` loads that layer again and goes through steps 2 and 3. Every rebuild clears the record of issues reported while resolving, so they are reported once per catalog.
