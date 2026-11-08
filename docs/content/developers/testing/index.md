---
title: Testing
description: Test a product's translations and its use of Semantica.
---

`semantica-testing` has a source kept in memory and assertions on issues, for tests of any framework:

```kotlin
dependencies {
    testImplementation("me.whereareiam.semantica:semantica-testing:0.1.0")
}
```

## Messages in memory

```java
MemorySource source = new MemorySource("test")
        .put(Locale.ENGLISH, "shop.bought", "You bought {count, plural, one {# item} other {# items}}")
        .put(Locale.forLanguageTag("ru"), "shop.bought", "Вы купили {count, plural, one {# предмет} few {# предмета} many {# предметов} other {# предмета}}");

Semantica semantica = Semantica.builder()
        .syntax(new IcuSyntax())
        .watch(true)
        .layer(Layer.of(source))
        .build();

assertEquals("Вы купили 3 предмета", semantica.resolve(Text.of("shop.bought").with("count", 3), Locale.forLanguageTag("ru")).plain());

source.put(Locale.ENGLISH, "shop.bought", "Bought {count}").changed();   // a watching instance loads it again
```

## Checking the shipped translations

Load the texts the project ships and assert that they have no issues, so a broken message fails the build instead of a player's screen:

```java
@Test
void shippedTranslationsHaveNoIssues() {
    Semantica semantica = Semantica.builder()
            .syntax(new IcuSyntax())
            .watch(false)
            .layer(Layer.of(FileSource.of(Path.of("src/main/resources/languages"), Configura.builder().build())))
            .check(AdventureRenderer.of(TagPolicy.defaults()).check())
            .build();

    IssueAssert.assertNone(semantica.getCatalog());
}
```

`IssueAssert.assertReported(catalog, Issue.Kind.REFERENCE_CYCLE, "key")` asserts the opposite; each assertion lists the issues there are when it fails.
