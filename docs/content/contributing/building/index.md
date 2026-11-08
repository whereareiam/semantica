---
title: Building
description: Build and test Semantica, and try a build in a product.
---

Semantica builds with the Gradle wrapper and a Java 21 toolchain, which Gradle downloads when it is missing.

```shell
./gradlew test build
```

runs what the pull request checks run: every module's tests, the conformance cases, Javadoc, and the `semantica-cli` jar with its dependencies (`semantica-cli/build/libs/semantica-cli-<version>-all.jar`).

A single module or test:

```shell
./gradlew :semantica-common:test
./gradlew :syntax-icu:test --tests 'me.whereareiam.semantica.syntax.icu.IcuSyntaxTest'
```

## Trying a build in a product

```shell
VERSION=local ./gradlew publishToMavenLocal
```

publishes every module as version `local` to `~/.m2`; a product with `mavenLocal()` among its repositories can then depend on `me.whereareiam.semantica:semantica-common:local`. Without `VERSION`, the version is `dev`.

## Development builds and releases

A development build is started by hand with the `Development build` workflow on a branch; it publishes every module as `<branch>-<commit>`, such as `feature-rewrite-1a2b3c4`, to `https://registry.whereareiam.me/maven/packages`. Publishing a GitHub release publishes the release's version. See `AGENTS.md` for titles, labels and release notes.
