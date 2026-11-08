package me.whereareiam.semantica.common;

import me.whereareiam.semantica.Text;
import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.source.Source;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;
import me.whereareiam.semantica.testing.IssueAssert;
import me.whereareiam.semantica.testing.MemorySource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticaTest {
	private static final Locale EN = Locale.ENGLISH;
	private static final Locale DE = Locale.GERMAN;

	private final List<Issue> issues = new ArrayList<>();

	private Semantica.Builder builder() {
		return Semantica.builder()
				.syntax(new IcuSyntax())
				.issues(issues::add);
	}

	@Test
	void higherPrioritiesWinPerKey() {
		MemorySource defaults = new MemorySource("defaults").put(EN, "a", "default a").put(EN, "b", "default b");
		MemorySource overrides = new MemorySource("overrides").put(EN, "b", "override b");

		Semantica semantica = builder()
				.layer(Layer.of(overrides, Layer.OVERRIDES))
				.layer(Layer.of(defaults))
				.build();

		assertEquals("default a", semantica.resolve(Text.of("a"), EN).plain());
		assertEquals("override b", semantica.resolve(Text.of("b"), EN).plain());
		assertEquals("overrides", semantica.getCatalog().entry(EN, "b").orElseThrow().layer());
	}

	@Test
	void aBrokenOverrideFallsBackToTheLayerBelow() {
		MemorySource defaults = new MemorySource("defaults").put(EN, "a", "fine {name}");
		MemorySource overrides = new MemorySource("overrides").put(EN, "a", "broken {name");

		Semantica semantica = builder()
				.layer(Layer.of(defaults))
				.layer(Layer.of(overrides, Layer.OVERRIDES))
				.build();

		assertEquals("fine Ann", semantica.resolve(Text.of("a").with("name", "Ann"), EN).plain());
		Issue issue = IssueAssert.assertReported(semantica.getCatalog(), Issue.Kind.SYNTAX, "a");
		assertEquals("overrides", issue.getLayer());
	}

	@Test
	void ownedLayersLeaveInOneStep() {
		Semantica semantica = builder()
				.layer(Layer.of(new MemorySource("base").put(EN, "a", "base")))
				.build();

		semantica.add(
				Layer.owned("shop", new MemorySource("shop").put(EN, "shop.title", "Shop")),
				Layer.owned("shop", new MemorySource("shop extra").put(EN, "shop.extra", "Extra"))
		);
		assertTrue(semantica.has("shop.title"));
		assertTrue(semantica.has("shop.extra"));

		long revision = semantica.getCatalog().getRevision();
		semantica.remove("shop");

		assertFalse(semantica.has("shop.title"));
		assertFalse(semantica.has("shop.extra"));
		assertTrue(semantica.has("a"));
		assertEquals(revision + 1, semantica.getCatalog().getRevision());
	}

	@Test
	void replaceSwapsAnOwnersLayersAtOnce() {
		Semantica semantica = builder().build();
		semantica.add(Layer.owned("shop", new MemorySource("v1").put(EN, "shop.title", "Shop v1")));

		List<Catalog> seen = new ArrayList<>();
		semantica.onChange(seen::add);
		semantica.replace("shop", Layer.owned("shop", new MemorySource("v2").put(EN, "shop.title", "Shop v2")));

		assertEquals(1, seen.size());
		assertEquals("Shop v2", semantica.resolve(Text.of("shop.title"), EN).plain());
	}

	@Test
	void watchedSourcesReplaceTheCatalog() {
		MemorySource source = new MemorySource("live").put(EN, "a", "first");
		Semantica semantica = builder().layer(Layer.of(source)).build();
		Catalog before = semantica.getCatalog();

		source.put(EN, "a", "second").changed();

		assertEquals("second", semantica.resolve(Text.of("a"), EN).plain());
		assertEquals("first", before.entry(EN, "a").orElseThrow().source());
		semantica.close();

		source.put(EN, "a", "third").changed();
		assertEquals("second", semantica.resolve(Text.of("a"), EN).plain());
	}

	@Test
	void aFailingSourceKeepsWhatItGaveLastTime() {
		AtomicBoolean failing = new AtomicBoolean();
		Source source = new Source() {
			@Override
			public @NotNull String getName() {
				return "flaky";
			}

			@Override
			public @NotNull Content load() throws IOException {
				if (failing.get()) throw new IOException("unreachable");
				return Content.builder().put(EN, "a", "kept", null).build();
			}
		};

		Semantica semantica = builder().layer(Layer.of(source)).build();
		failing.set(true);
		semantica.reload();

		assertEquals("kept", semantica.resolve(Text.of("a"), EN).plain());
		IssueAssert.assertReported(issues, Issue.Kind.SOURCE, null, null);
	}

	@Test
	void registeredChecksRunOnTheCurrentCatalog() {
		Semantica semantica = builder().layer(Layer.of(new MemorySource("m").put(EN, "a", "x"))).build();

		Catalog catalog = semantica.check(current -> List.of(new Issue(Issue.Kind.MARKUP, "a", EN, null, null, "test")));

		IssueAssert.assertReported(catalog, Issue.Kind.MARKUP, "a");
		IssueAssert.assertReported(issues, Issue.Kind.MARKUP, "a", EN);
	}

	@Test
	void runtimeIssuesAreReportedOncePerCatalog() {
		Semantica semantica = builder().build();
		semantica.resolve(Text.of("nothing"), EN);
		semantica.resolve(Text.of("nothing"), EN);

		assertEquals(1, issues.stream().filter(issue -> issue.getKind() == Issue.Kind.MISSING_KEY).count());
	}

	@Test
	void nestedTextsResolveInTheSameLocale() {
		Semantica semantica = builder()
				.layer(Layer.of(new MemorySource("m")
						.put(EN, "item.sword", "sword")
						.put(DE, "item.sword", "Schwert")
						.put(EN, "bought", "You bought a {item}")
						.put(DE, "bought", "Du hast ein {item} gekauft")))
				.build();

		assertEquals("Du hast ein Schwert gekauft",
				semantica.resolve(Text.of("bought").with("item", Text.of("item.sword")), DE).plain());
	}

	@Test
	void formatsDatesInTheClocksZone() {
		Clock clock = Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);
		Semantica semantica = builder()
				.clock(clock)
				.layer(Layer.of(new MemorySource("m")
						.put(EN, "day", "{when, date, long}")
						.put(EN, "ago", "{when, time, relative}")))
				.build();

		assertEquals("November 8, 2026", semantica.resolve(Text.of("day").with("when", clock.instant()), EN).plain());
		assertEquals("3 hours ago",
				semantica.resolve(Text.of("ago").with("when", clock.instant().minusSeconds(3 * 3600)), EN).plain());
	}

	@Test
	void valuesPlatformsRenderStayUnformatted() {
		Object component = new Object() {
			@Override
			public String toString() {
				return "component";
			}
		};
		Semantica semantica = builder().layer(Layer.of(new MemorySource("m").put(EN, "a", "<{thing}>"))).build();

		var message = semantica.resolve(Text.of("a").with("thing", component), EN);
		assertEquals("<component>", message.plain());
		assertSame(component, ((me.whereareiam.semantica.message.ResolvedMessage.Value) message.getParts().get(1)).value());
	}
}
