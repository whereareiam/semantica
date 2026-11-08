package me.whereareiam.semantica.format.adventure.v5;

import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.common.Semantica;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;
import me.whereareiam.semantica.testing.MemorySource;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static me.whereareiam.semantica.format.adventure.v5.AdventureRendererTest.plain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureTranslatorTest {
	private static final Locale EN = Locale.ENGLISH;
	private static final Locale RU = Locale.forLanguageTag("ru");

	private final Semantica semantica = Semantica.builder()
			.syntax(new IcuSyntax())
			.watch(false)
			.issues(issue -> {
			})
			.layer(Layer.of(new MemorySource("test")
					.put(EN, "items", "{count, plural, one {# {item}} other {# {item}s}}")
					.put(RU, "items", "{count, plural, one {# {item}} few {# {item}а} many {# {item}ов} other {# {item}а}}")
					.put(EN, "positional", "{0} gave {1}")))
			.build();

	private final AdventureTranslator translator = AdventureTranslator.of(semantica, Key.key("test", "translations"));

	@Test
	void translatesWithNamedArguments() {
		TranslatableComponent component = (TranslatableComponent) Component.translatable("items",
				TextArgument.of("count", 21), TextArgument.of("item", "меч")).decorate(TextDecoration.BOLD);

		Component translated = translator.translate(component, RU);

		assertEquals("21 меч", plain(translated));
		assertEquals(TextDecoration.State.TRUE, translated.decoration(TextDecoration.BOLD));
	}

	@Test
	void numbersOtherArguments() {
		Component translated = translator.translate(Component.translatable("positional", Component.text("Ann"), Component.text("a sword")), EN);

		assertEquals("Ann gave a sword", plain(translated));
	}

	@Test
	void leavesUnknownKeysToTheNextSource() {
		assertNull(translator.translate(Component.translatable("block.minecraft.stone"), EN));
		assertFalse(translator.canTranslate("block.minecraft.stone", EN));
		assertTrue(translator.canTranslate("items", EN));
		assertEquals(Key.key("test", "translations"), translator.name());
	}
}
