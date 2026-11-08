---
version: 0.1.0
---

# Update 0.1.0 - First release

Semantica 0.1.0 is the first release of the shared translation engine: messages in ICU MessageFormat with references, parsed once and checked when they load, merged from layers (files, a jar's resources, bundles over HTTP) and looked up along one locale chain, with the Adventure format for Adventure 4 and 5, a `semantica-cli` for a translations repository's CI, and a specification of the format with conformance cases.

## Upgrading

* Semantica 0.1.0 replaces the unreleased development builds. Translations written with `<p:>`, `<ref:>` or `<if>` are converted once to ICU MessageFormat and `{@key}` references; see the format guide.
