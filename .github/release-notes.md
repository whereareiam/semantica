---
version: 0.2.0
---

# Update 0.2.0 - Declared Messages

Semantica 0.2.0 lets a project declare its messages in code: `@Messages` interfaces whose `@Default`s are checked when it compiles, implemented without reflection and shipped in its jar for `ClasspathSource`. Configs hold `Content`, an administrator's own text that may pull in translated messages with `{@key}`, and the Adventure renderers take a reader's placeholders for literal parts only.

## Upgrading

* `Text` is now `Message`: replace `me.whereareiam.semantica.Text` and `Text.of(...)` with `me.whereareiam.semantica.Message` and `Message.of(...)`.
* `TranslationDefaults`, `DefaultsExport` and `semantica { defaults(...) }` are removed. Declare the default texts with `@Messages` and `@Default`, add `annotationProcessor("me.whereareiam.semantica:semantica-processor:0.2.0")`, and run `./gradlew translationsExport` once. A declared key is the interface's prefix and the method's name in kebab case; keep the prefixes and names of the keys your translations already use.
