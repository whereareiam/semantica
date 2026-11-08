package me.whereareiam.semantica.format.adventure.v5;

import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.common.Semantica;
import me.whereareiam.semantica.format.adventure.TagPolicy;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;
import me.whereareiam.semantica.testing.MemorySource;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static me.whereareiam.semantica.format.adventure.v5.AdventureRendererTest.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorityLookupTest {
	private static final Locale EN = Locale.ENGLISH;
	private static final AdventureRenderer RENDERER = AdventureRenderer.of(TagPolicy.defaults());

	private final List<AdventureTranslator> registered = new ArrayList<>();

	private static Semantica semantica(String... keysAndTexts) {
		MemorySource source = new MemorySource("test");
		for (int index = 0; index < keysAndTexts.length; index += 2) source.put(EN, keysAndTexts[index], keysAndTexts[index + 1]);

		return Semantica.builder()
				.syntax(new IcuSyntax())
				.watch(false)
				.issues(issue -> {
				})
				.layer(Layer.of(source))
				.build();
	}

	private AdventureTranslator register(AdventureTranslator translator) {
		GlobalTranslator.translator().addSource(translator);
		registered.add(translator);
		return translator;
	}

	@AfterEach
	void unregister() {
		registered.forEach(GlobalTranslator.translator()::removeSource);
	}

	private static Component translate(AdventureTranslator translator, String key) {
		return translator.translate((TranslatableComponent) Component.translatable(key), EN);
	}

	@Test
	void otherTranslatorsLeaveTheAuthoritysKeysToIt() {
		AdventureTranslator shop = register(AdventureTranslator.of(semantica("shop.title", "Shop", "shop.only", "Only the shop"),
				Key.key("shop", "translations"), RENDERER, Duration.ZERO));
		AdventureTranslator network = register(AdventureTranslator.authority(semantica("shop.title", "Network shop"), RENDERER));

		assertNull(translate(shop, "shop.title"));
		assertFalse(shop.canTranslate("shop.title", EN));
		assertEquals("Network shop", plain(translate(network, "shop.title")));
		assertEquals("Network shop", plain(GlobalTranslator.render(Component.translatable("shop.title"), EN)));

		assertEquals("Only the shop", plain(translate(shop, "shop.only")));
		assertTrue(network.isAuthority());
		assertFalse(shop.isAuthority());
	}

	@Test
	void answersAgainOnceTheAuthorityIsRemoved() {
		AdventureTranslator shop = register(AdventureTranslator.of(semantica("shop.title", "Shop"), Key.key("shop", "translations"), RENDERER, Duration.ZERO));
		AdventureTranslator network = register(AdventureTranslator.authority(semantica("shop.title", "Network shop"), RENDERER));
		assertNull(translate(shop, "shop.title"));

		GlobalTranslator.translator().removeSource(network);

		assertEquals("Shop", plain(translate(shop, "shop.title")));
	}

	@Test
	void keepsWhatItFoundUntilTheIntervalPasses() {
		AdventureTranslator shop = register(AdventureTranslator.of(semantica("shop.title", "Shop"), Key.key("shop", "translations"), RENDERER, Duration.ofHours(1)));
		assertEquals("Shop", plain(translate(shop, "shop.title")));

		register(AdventureTranslator.authority(semantica("shop.title", "Network shop"), RENDERER));

		assertEquals("Shop", plain(translate(shop, "shop.title")));
	}

	@Test
	void reservesTheAuthoritysName() {
		assertThrows(IllegalArgumentException.class,
				() -> AdventureTranslator.of(semantica("a", "b"), AdventureTranslator.AUTHORITY, RENDERER));
	}
}
