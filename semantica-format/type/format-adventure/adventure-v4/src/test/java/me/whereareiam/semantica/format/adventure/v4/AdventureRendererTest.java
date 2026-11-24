package me.whereareiam.semantica.format.adventure.v4;

import me.whereareiam.semantica.Content;
import me.whereareiam.semantica.Message;
import me.whereareiam.semantica.Value;
import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.common.Semantica;
import me.whereareiam.semantica.format.adventure.TagPolicy;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;
import me.whereareiam.semantica.testing.IssueAssert;
import me.whereareiam.semantica.testing.MemorySource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureRendererTest {
	private static final Locale EN = Locale.ENGLISH;
	private static final Locale RU = Locale.forLanguageTag("ru");

	private final AdventureRenderer renderer = AdventureRenderer.of(TagPolicy.defaults());
	private final Semantica semantica = Semantica.builder()
			.syntax(new IcuSyntax())
			.watch(false)
			.issues(issue -> {
			})
			.check(renderer.check())
			.layer(Layer.of(new MemorySource("test")
					.put(EN, "greeting", "<red>Hello</red>, <bold>{name}</bold>!")
					.put(RU, "greeting", "<red>Привет</red>, <bold>{name}</bold>!")
					.put(EN, "spawn", "<click:run_command:/spawn>Go to spawn</click>")
					.put(EN, "lore", "<gray>First line\n<gold>Second {name}")
					.put(EN, "shadowed", "<shadow:black>Deep</shadow>")
					.put(EN, "page", "<gray>Page {current} {next}")
					.put(EN, "next", "<gold>Next ({page})")))
			.build();

	static String plain(Component component) {
		return PlainTextComponentSerializer.plainText().serialize(component);
	}

	private Component render(Message message, Locale locale) {
		return renderer.render(semantica.resolve(message, locale));
	}

	@Test
	void rendersMiniMessageInTheRequestedLocale() {
		Component component = render(Message.of("greeting").with("name", "Ann"), RU);

		assertEquals("Привет, Ann!", plain(component));
		assertEquals(NamedTextColor.RED, component.children().getFirst().color());
	}

	@Test
	void neverReadsValuesAsMarkup() {
		Component component = render(Message.of("greeting").with("name", "<red>evil</red> <click:run_command:/op me>x"), EN);

		assertEquals("Hello, <red>evil</red> <click:run_command:/op me>x!", plain(component));
	}

	@Test
	void insertsComponentsAsTheyAre() {
		Component component = render(Message.of("greeting").with("name", ComponentValue.of(Component.text("Ann", NamedTextColor.AQUA))), EN);

		assertEquals("Hello, Ann!", plain(component));
		assertTrue(component.toString().contains("aqua"), component::toString);
	}

	@Test
	void showsDisallowedTagsAsWrittenAndReportsThem() {
		assertEquals("<click:run_command:/spawn>Go to spawn</click>", plain(render(Message.of("spawn"), EN)));
		IssueAssert.assertReported(semantica.getCatalog(), Issue.Kind.MARKUP, "spawn");
	}

	@Test
	void appliesAllowedClicks() {
		AdventureRenderer clicks = AdventureRenderer.of(TagPolicy.defaults().allow(TagPolicy.Group.CLICK));
		Component component = clicks.render(semantica.resolve(Message.of("spawn"), EN));

		assertEquals("Go to spawn", plain(component));
		assertNotNull(component.children().isEmpty() ? component.clickEvent() : component.children().getFirst().clickEvent());
	}

	@Test
	void appliesTheTagsOfAdventureFourTwentyFive() {
		assertEquals("Deep", plain(render(Message.of("shadowed"), EN)));
		IssueAssert.assertNone(semantica.getCatalog().getIssues().stream().filter(issue -> "shadowed".equals(issue.getKey())).toList());
	}

	@Test
	void splitsLinesForLore() {
		List<Component> lines = renderer.renderLines(semantica.resolve(Message.of("lore").with("name", "Ann"), EN));

		assertEquals(2, lines.size());
		assertEquals("First line", plain(lines.get(0)));
		assertEquals("Second Ann", plain(lines.get(1)));
	}

	@Test
	void renderingFillsPlaceholdersOfLiteralPartsOnly() {
		Rendering rendering = Rendering.builder()
				.text(text -> text.replace("%player_name%", "Ann"))
				.text(text -> text.replace("Ann", "<aqua>Ann</aqua>"))
				.tags(Placeholder.unparsed("server", "Lobby"))
				.build();
		Content content = Content.of("<gray>%player_name% on <server> said: {said}");

		Component component = renderer.render(semantica.resolve(content, Map.of("said", Value.text("%player_name% <server>")), List.of(EN)), rendering);

		assertEquals("Ann on Lobby said: %player_name% <server>", plain(component));
		assertTrue(component.toString().contains("aqua"), component::toString);
	}

	@Test
	void renderingAppliesToEachLine() {
		Rendering rendering = Rendering.builder().text(text -> text.replace("line", "row")).build();

		List<Component> lines = renderer.renderLines(semantica.resolve(Message.of("lore").with("name", "line"), EN), rendering);

		assertEquals("First row", plain(lines.get(0)));
		assertEquals("Second line", plain(lines.get(1)));
	}

	@Test
	void aLinkedValueIsItsOwnClickableComponentWithTheStyleAroundIt() {
		Value next = Value.linked(Value.nested(Message.of("next").with("page", 2)), Value.Link.runCommand("/help 2"));

		Component component = render(Message.of("page").with("current", 1).with("next", next), EN);

		assertEquals("Page 1 Next (2)", plain(component));
		Component arrow = component.children().getLast();
		assertEquals(ClickEvent.runCommand("/help 2"), arrow.clickEvent());
		assertEquals(NamedTextColor.GRAY, component.color());
		assertEquals("Next (2)", plain(arrow));
	}
}
