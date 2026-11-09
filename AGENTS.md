# AGENTS.md

## Semantica At A Glance

Semantica is the **translation engine** shared by Yui (Discord bot), Intercept (Velocity plugin) and Oraylen's Arbor. Messages are ICU MessageFormat plus `{@key}` references. A product keeps its own texts in its own repository and ships them, for strangers too; an organisation's translations repository, edited through Weblate, holds only that organisation's layer (its own products, and its own versions of other products' texts, named by topic, never by plugin), and its CI builds one bundle per product from the key prefixes `semantica.json` gives each product. Semantica merges layers of sources (a server's `languages` folder, an organisation's bundles over HTTP, an add-on's own layer), parses every message once, reports issues instead of failing, swaps an immutable catalog at once, and resolves texts along one locale chain. Text formats render resolved messages: Adventure components through MiniMessage, for Adventure 4 and 5.

Semantica is platform-agnostic: no players, audiences, interactions, plugin registration or service lookups. The project decides a reader's locale and where a text is shown; Discord (markdown, mentions, slash commands, length limits) belongs to Yui. Each plugin owns its own `Semantica` and registers its own `AdventureTranslator` in `GlobalTranslator`.

When changing behavior:

- values a program gives are never read as markup or as part of a message; a format inserts them as data
- a lookup never throws because of translations: report an issue and fall back
- every locale chain ends with the default locale, then the root locale (`Locale.ROOT`, `und`, the folder `all`): messages that are the same in every language; they never count as a translation in the checks, and their formats follow the locale asked for
- the catalog is immutable and replaced in one write; nothing a lookup holds changes under it
- what the format specifies (`docs/content/format/`) and the conformance cases (`semantica-testing/src/main/resources/conformance/`) change together; the TypeScript `feature-i18n` follows them
- markup belongs to each text format, never to the translation format
- no compatibility readers for older formats (`<p:>`, `<ref:>`, `<if>` of Semantica 1): projects convert once

---

## Repository Orientation

### Module Map

- `semantica-api`
  Contracts and models: `Text`, `KeyPrefix` (whole-segment key prefixes), `catalog/Catalog` and `Layer`, `source/Source`, `locale/LocaleChain`, `message/ParsedMessage` and `ResolvedMessage`, `syntax/Syntax`, `issue/Issue` and `Check`, `exception/SyntaxException`. No Jackson, no Configura, no ICU.
- `semantica-common`
  `Semantica` (the instance and its builder) and the engine: `catalog` (`CatalogBuilder`, `ReferenceAnalysis`), `check` (`PluralFormCheck`, `UnknownKeyCheck`, `ArgumentCheck`), `resolution` (`MessageResolver`, `ValueFormatter`). ICU4J for CLDR plural rules and formats.
- `semantica-syntax/syntax-icu` (project `:syntax-icu`)
  `IcuSyntax`: ICU MessageFormat with `{@key}` and `{@key name=other}` references. Depends on the api only.
- `semantica-source/source-file` (project `:source-file`)
  `FileSource.of(Path, Configura)`: the server's `languages` folder, one folder per locale, paths as key prefixes, watching. Plugins write their defaults into that folder themselves (Configura defaults providers); Semantica only reads it.
- `semantica-source/source-http` (project `:source-http`)
  `HttpSource`: an organisation's bundles by product, key prefixes and locales, polled through `manifest.json`; `Manifest` and `Bundle`, also written by `semantica-cli`.
- `semantica-format/common` (project `:semantica-format:common`, artifact `format-common`)
  `Renderer` and `PlainRenderer`. Depends on the api only.
- `semantica-format/type/format-adventure/common` (project `:format-adventure-common`)
  `TagPolicy`, `MarkupCheck`, `MiniMessageTags`, `MinecraftLocales`: what both Adventure adapters share, without Adventure types.
- `semantica-format/type/format-adventure/adventure-v4` (project `:format-adventure-v4`)
  `AdventureRenderer`, `AdventureTags`, `AdventureTranslator`, `TextArgument` for Adventure 4.25 (BungeeCord and Spigot through adventure-platform 4.x), `compileOnly`. Same code as v5 against 4.x: virtual components for named arguments, every MiniMessage tag.
- `semantica-format/type/format-adventure/adventure-v5` (project `:format-adventure-v5`)
  The same classes for Adventure 5.2 (Paper, Velocity, Arbor through its own NeoForge bridge), `compileOnly`. Never subclass `AbstractComponentRenderer` in either: 5.x added an abstract method to it.
- `semantica-testing`
  `MemorySource`, `IssueAssert`, and the conformance cases with `ConformanceTest`.
- `semantica-gradle`
  The Gradle plugin `me.whereareiam.semantica`: `translationsExport` runs a product's `TranslationDefaults` (in `source-file`) on its runtime classpath through `DefaultsExport` and writes `translations/<locale>/`; `translationsCheck`, part of `check`, fails when the committed files differ. Published as `pluginMaven` with its marker.
- `semantica-cli`
  `check` and `bundle` for a translations repository's CI; `RepositorySettings` reads `semantica.json` (`products` and `markup` by key prefix, the longest markup prefix wins). Published with a jar of all dependencies (classifier `all`).
- `build-logic/`
  `module-java` (group `me.whereareiam.semantica`, Java 21 toolchain, Lombok, JetBrains annotations, JUnit, sources and Javadoc jars), `module-api` (`module-java` plus publication, for `semantica-api`), `packaging-publication` (Maven publication through the toolkit).
- `docs/content/`
  `developers/` (using Semantica), `format/` (the translation format specification), `contributing/`.

### Where To Look First

- For contracts and models, start in `semantica-api`.
- For loading, merging and checks, start in `Semantica` and `CatalogBuilder`; for lookups, `MessageResolver`.
- For the message syntax, `IcuParser`; for files, `FileSource` and `TranslationFiles`; for bundles, `HttpSource`.
- For rendering, the format modules; for the translations repository's CI, `semantica-cli`.
- For what the format promises, `docs/content/format/` and the conformance cases.

---

## Architecture Rules

### Module Boundaries

- Keep responsibilities in the narrowest owning module.
- Contracts go into `semantica-api`, behavior into `semantica-common`; a syntax depends on the api only.
- Formats keep their third-party library `compileOnly` when the consumer ships it (Adventure); Configura, Jackson and ICU4J are real dependencies.
- A change to one Adventure adapter is made in the other too, unless it uses what only one version has.
- `Semantica` makes every change to layers and checks under one lock and replaces the catalog in one write; sources are loaded outside the lock.

### API Surfaces

- `semantica-api` is the public API surface, together with the public types of the other modules that projects use (`Semantica`, `IcuSyntax`, `FileSource`, `HttpSource`, `Manifest`, `Bundle`, `Renderer`, `PlainRenderer`, `TagPolicy`, `MarkupCheck`, `MiniMessageTags`, `MinecraftLocales`, and in both Adventure adapters `AdventureRenderer`, `AdventureTags`, `AdventureTranslator`, `TextArgument`; `MemorySource`, `IssueAssert`).
- `CatalogBuilder`, `MessageResolver`, `ValueFormatter` and the engine's checks are public for packaging only; projects do not call them.
- Public API changes are compatibility-sensitive. Public API code has complete Javadocs.

### Safety

- A format inserts values as placeholders (Adventure). Never concatenate a value into markup.
- Caches are bounded or rebuilt with each catalog; nothing grows with the number of lookups.
- Bundles are used only when their SHA-256 matches the manifest.
- What crosses plugin class loaders uses only Adventure and JDK types: `TextArgument` is read through `VirtualComponent`, its simple name and `Map.Entry`, the authority through `Translator` and its name. Never `instanceof` a Semantica class on a value another plugin made.

### Existing Patterns First

- Follow neighboring naming, packaging, and builder patterns before inventing new ones.
- Issue details are read by translators and administrators; keep their wording consistent with the existing ones.

---

## Dependencies

- Use Java 21-compatible code and APIs.
- Follow the version catalog in `gradle/libs.versions.toml` when adding or changing dependencies.
- Add dependencies in the narrowest owning module.
- When a new dependency is required, check the latest available version instead of relying on memory.

---

## Build, Test, And Validation

### Common Commands

- `./gradlew test`
- `./gradlew test build` (what the pull request checks run)
- `./gradlew :semantica-testing:test` (the conformance cases)
- `VERSION=<version> ./gradlew publishToMavenLocal` (to try a build in a project)

The version comes from the `VERSION` environment variable and defaults to `dev`.

### Verification Guidance

- Prefer targeted Gradle tasks for the module you changed before running broad root verification.
- Add or update tests in the closest existing test; a change to the format adds or changes a conformance case.

See `docs/content/contributing/testing/`.

---

## Branching And Pull Requests

### Branch Flow

- Build each feature, fix, or isolated piece of work in its own `feature/...` branch.
- Merge feature work into `dev` through a pull request. Pull requests are merged by rebase: each commit lands on `dev` as written, so a branch holds clean conventional commits and no fixups.
- Releases are cut from `dev`: publishing the release draft tags the current `dev`.

### Pull Request Checks

- Every pull request gets the metadata check (title and label) and the checks: `./gradlew test build`.
- The `dev` ruleset requires these checks, so no pull request merges with a failing or missing check unless an administrator bypasses it.
- Dependabot groups minor and patch updates into one pull request per ecosystem, and `dependabot-auto-merge.yml` turns on auto-merge for it, so it merges once the checks pass; a major update gets a pull request of its own and waits for review. The merge goes through a GitHub App, because a merge by `GITHUB_TOKEN` would not start the workflows of the push to `dev`; its `MERGE_APP_CLIENT_ID` and `MERGE_APP_PRIVATE_KEY` are Dependabot secrets, since a run Dependabot starts gets no Actions secrets. When the checks gain or rename a job, update the required checks of the ruleset.
- Label a pull request with exactly one of `feature`, `change`, `bug`, `dependencies`, or with `skip-changelog`; `major` may accompany a category.
- A run keeps `test-results` as an artifact when a test failed.
- Development builds are started manually and publish a branch-qualified version. A published GitHub release publishes the Maven artifacts.
- Entry workflows only select a trigger and call `reusable-verify.yml` and `reusable-publish.yml`; shared steps come from `whereareiam/devops` and `whereareiam/blockops`.
- The Gradle build cache is on. Pull requests only read caches, so `build-cache.yml` compiles the project and its tests on every push to `dev` and writes the Gradle cache; when the checks start running other tasks before their tests, add those tasks to its command.

### Issues

- A suggestion opens with `suggestion`, the label of an idea not decided on yet. An accepted one trades it for `feature` or `change`; a declined one is closed as not planned.
- A bug report opens with `bug`.

### Release Notes

A merged pull request lands in the release draft as `* <title> ([#<number>](<url>))`, under the category of its label. The draft is written for the developers of projects that use Semantica and for the people who run translations repositories, so the title and the label decide what they read.

#### Titles

- use the strict format `Area: Description`, with a past-tense description of what changed for the reader: `Introduced`, `Changed`, `Fixed`, `Removed`
- describe the effect, not the implementation: `Lookup: Fixed plurals of decimals choosing the wrong form`, not `Lookup: Passed BigDecimal to PluralRules`
- the draft sorts by the exact area, so spell an area exactly as listed below

Areas for behavior projects and translators notice:

`Format`, `Syntax`, `Lookup`, `Layers`, `Checks`, `Files`, `Bundles`, `Adventure`, `CLI`, `Testing`

- reuse an existing area whenever one fits
- add a new area only for something readers recognise by that name, such as a new source or text format, and add it to this list, to the issue template's areas and to the pull request template in the same pull request
- never use class names or internal packages as an area: no `Semantica`, `Common`, `CatalogBuilder`

`API:` is for changes to the contracts projects compile against: types, methods and their meaning in `semantica-api` and the public types of the other modules. The drafter moves `API:` changes out of their category into a closing "For developers" section. A change that is both behavior and API takes the behavior area; mention the API part in the pull request body.

#### Labels

| Label | Use for | Draft section |
| --- | --- | --- |
| `feature` | something new for projects or translators | What’s new |
| `change` | changed behavior or API | Enhancements |
| `bug` | a fix for a bug that shipped in a release | Fixes |
| `dependencies` | a library update | Dependencies |
| `skip-changelog` | anything a reader of the release would not notice | not listed |

Use `skip-changelog` for:

- internal refactors with no effect on projects, translations, or the public API
- build, CI, test, and documentation-only changes
- a fix for a bug that never shipped: when the broken code was introduced after the last release, the fix belongs to the work that introduced it, not to the release notes; compare against the last release tag (`git grep <symbol> <tag>`, `git log <tag>..HEAD -- <path>`) to decide
- changes to something that was itself added after the last release and never shipped

Add `major` next to the category when projects or translations must change on upgrade: an incompatible public API change, a message that the format now reads differently, or a file, bundle or manifest layout that is no longer read. One `major` pull request makes the draft a major version.

#### Release notes file

The drafter rebuilds the draft on every push to `dev`, so text typed into the draft is lost. Everything written by hand, such as the subtitle, the summary, upgrade notes, and security callouts, lives in `.github/release-notes.md` and is changed through pull requests like any other file.

```markdown
---
version: 0.2.0
---

# Update 0.2.0 - Gender Selects

One or two sentences about the release.

## Upgrading

* What a project or a translations repository has to change.
```

- the content replaces the subtitle and summary placeholders of the draft only while `version` matches the version the draft resolves to; notes of a published release therefore never reach the next draft
- when a change needs a note, add it to this file in the same pull request; if the file names an already published version, start the notes of the next release by replacing the whole file and setting `version` to the version the draft shows
- keep the heading as `# Update <version> - <subtitle>`
- bullets in this file are not sorted into the changelog

#### Publishing a release

- fix the changelog part of the draft by editing titles and labels of merged pull requests, then rerun the `Release drafter` workflow
- the subtitle and summary come from `.github/release-notes.md`; the release workflow stops while a placeholder is left
- the summary names what projects and translations repositories must change on upgrade (everything labelled `major`) and calls out security fixes
- publishing the release verifies the tagged commit and publishes the Maven artifacts to `https://registry.whereareiam.me/maven/packages`; nothing is attached to the release and nothing goes to plugin stores

---

## Documentation Rules

### Structure

`docs/content/` is split into three sections, each a root folder (`"root": true` in its `meta.json`) that Scriptorium shows as its own sidebar: `developers/` (Using Semantica), `format/` (Translation format) and `contributing/`. `scriptorium.project.json` configures the site; there is no published site yet.

- **Using Semantica** (`developers/`): setting up an instance, layers and sources, lookups, text formats, the translations repository's CI, and testing a project.
- **Translation format** (`format/`): what every implementation reads alike: syntax, references, files, bundles and the manifest, fallback, limits, issues, conformance. No Java classes and no markup.
- **Contributing** (`contributing/`): building, modules and internal flow, and where tests go.

Rules that keep the sections apart:

- Internal classes (`CatalogBuilder`, `ReferenceAnalysis`, `MessageResolver`, `ValueFormatter`) belong in Contributing.
- A folder exists only for two or more pages; each page is `<name>/index.md`, and each folder with several pages has a `meta.json` that orders them. Only the section folders carry an `icon`.
- Links between pages are relative and point at the page file, such as `../layers/index.md`.

### Content

- When changing behavior projects or translators notice, update the relevant documentation in the same work.
- Documentation describes the **current state**; no "used to" text outside release notes.
- Every code example uses real symbols of the current source.
- Do not document planned modules as existing.

---

## Working Style

- Match the conventions already established in the surrounding module.
- Prefer small, scoped changes over broad cleanup unless cleanup is part of the task.
- Tabs for indentation in Java; Gradle Kotlin DSL files use four spaces.
