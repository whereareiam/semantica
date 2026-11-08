package me.whereareiam.semantica.format.adventure.v5;

import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.common.Semantica;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;
import me.whereareiam.semantica.testing.IssueAssert;
import me.whereareiam.semantica.testing.MemorySource;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static me.whereareiam.semantica.format.adventure.v5.AdventureRendererTest.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextArgumentTest {
	private static final Locale EN = Locale.ENGLISH;

	/**
	 * Loads Semantica's classes again, apart from these, while sharing Adventure and the JDK, as a second plugin's
	 * class loader does.
	 */
	private static URLClassLoader otherCopy() {
		ClassLoader shared = new ClassLoader(TextArgumentTest.class.getClassLoader()) {
			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				if (name.startsWith("me.whereareiam.semantica.")) throw new ClassNotFoundException(name);
				return super.loadClass(name, resolve);
			}
		};
		URL classes = TextArgument.class.getProtectionDomain().getCodeSource().getLocation();

		return new URLClassLoader(new URL[]{classes}, shared);
	}

	@Test
	void readsArgumentsOfAnotherCopyOfSemantica() throws Exception {
		List<Issue> issues = new ArrayList<>();
		Semantica semantica = Semantica.builder()
				.syntax(new IcuSyntax())
				.watch(false)
				.issues(issues::add)
				.layer(Layer.of(new MemorySource("test").put(EN, "greeting", "Hello, {player}! {count, plural, one {# item} other {# items}}")))
				.build();
		AdventureTranslator translator = AdventureTranslator.of(semantica, Key.key("test", "translations"));

		try (URLClassLoader other = otherCopy()) {
			Class<?> foreign = other.loadClass(TextArgument.class.getName());
			assertNotSame(TextArgument.class, foreign);

			ComponentLike player = (ComponentLike) foreign.getMethod("of", String.class, Object.class).invoke(null, "player", "Ann");
			ComponentLike count = (ComponentLike) foreign.getMethod("of", String.class, Object.class).invoke(null, "count", 2);
			Component translated = translator.translate((TranslatableComponent) Component.translatable("greeting", player, count), EN);

			assertEquals("Hello, Ann! 2 items", plain(translated));
			IssueAssert.assertNone(issues);
		}
	}

	@Test
	void readsOnlyTextArguments() {
		assertEquals(Map.entry("count", 3), TextArgument.read(TextArgument.of("count", 3).asComponent()));
		assertNull(TextArgument.read(Component.text("count")));
		assertNull(TextArgument.read(Component.virtual(Void.class, context -> Component.text("x"))));
	}
}
