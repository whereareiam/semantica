package me.whereareiam.semantica.format.adventure.v4;

import me.whereareiam.semantica.format.Renderer;
import me.whereareiam.semantica.format.adventure.MarkupCheck;
import me.whereareiam.semantica.format.adventure.TagPolicy;
import me.whereareiam.semantica.issue.Check;
import me.whereareiam.semantica.message.ResolvedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Renders resolved messages to Adventure 4 components. Messages are MiniMessage, read with only the tags the
 * {@link TagPolicy} allows; values are inserted as placeholders, so text a player typed is shown as written and
 * never read as markup, and components are inserted as they are.
 *
 * <pre>{@code
 * AdventureRenderer renderer = AdventureRenderer.of(TagPolicy.defaults());
 *
 * Semantica semantica = Semantica.builder()
 *         .syntax(new IcuSyntax())
 *         .layer(Layer.of(FileSource.of(dataFolder.resolve("languages"), configura)))
 *         .check(renderer.check())
 *         .build();
 *
 * Component message = renderer.render(semantica.resolve(Text.of("shop.title"), locale));
 * }</pre>
 */
public final class AdventureRenderer implements Renderer<Component> {
	private static final String PLACEHOLDER = "semantica-value-";

	private final TagPolicy policy;
	private final MiniMessage miniMessage;

	private AdventureRenderer(TagPolicy policy) {
		this.policy = policy;
		this.miniMessage = MiniMessage.builder()
				.tags(AdventureTags.INSTANCE.resolver(policy))
				.build();
	}

	/**
	 * Creates a renderer that applies the tags of a policy.
	 *
	 * @param policy the allowed tags
	 * @return the renderer
	 */
	public static @NotNull AdventureRenderer of(@NotNull TagPolicy policy) {
		return new AdventureRenderer(Objects.requireNonNull(policy, "policy"));
	}

	/**
	 * Returns the policy this renderer applies.
	 *
	 * @return the policy
	 */
	public @NotNull TagPolicy getPolicy() {
		return policy;
	}

	/**
	 * Returns the check of this renderer's tags for every key, to register on Semantica.
	 *
	 * @return the check
	 */
	public @NotNull Check check() {
		return check(key -> true);
	}

	/**
	 * Returns the check of this renderer's tags for some keys, when one Semantica instance also holds texts that
	 * are not MiniMessage.
	 *
	 * @param keys which keys hold MiniMessage
	 * @return the check
	 */
	public @NotNull Check check(@NotNull Predicate<String> keys) {
		return new MarkupCheck(policy, AdventureTags.INSTANCE, keys);
	}

	@Override
	public @NotNull Component render(@NotNull ResolvedMessage message) {
		return render(message.getParts());
	}

	/**
	 * Renders a message as separate lines, such as an item's lore: the message is split at its line breaks, and
	 * each line is read as MiniMessage of its own, so a tag ends at the end of its line.
	 *
	 * @param message the message
	 * @return one component per line
	 */
	public @NotNull List<Component> renderLines(@NotNull ResolvedMessage message) {
		List<List<ResolvedMessage.Part>> lines = new ArrayList<>();
		List<ResolvedMessage.Part> current = new ArrayList<>();
		lines.add(current);

		for (ResolvedMessage.Part part : message.getParts()) {
			if (!(part instanceof ResolvedMessage.Literal literal)) {
				current.add(part);
				continue;
			}

			String[] pieces = literal.text().split("\n", -1);
			for (int index = 0; index < pieces.length; index++) {
				if (index > 0) {
					current = new ArrayList<>();
					lines.add(current);
				}
				if (!pieces[index].isEmpty()) current.add(new ResolvedMessage.Literal(pieces[index]));
			}
		}

		return lines.stream().map(this::render).toList();
	}

	private Component render(List<ResolvedMessage.Part> parts) {
		StringBuilder template = new StringBuilder();
		List<TagResolver> values = new ArrayList<>();

		for (ResolvedMessage.Part part : parts) {
			switch (part) {
				case ResolvedMessage.Literal literal -> template.append(literal.text());
				case ResolvedMessage.Value value -> {
					String name = PLACEHOLDER + values.size();
					values.add(Placeholder.component(name, component(value)));
					template.append('<').append(name).append('>');
				}
			}
		}

		return miniMessage.deserialize(template.toString(), TagResolver.resolver(values));
	}

	private static Component component(ResolvedMessage.Value value) {
		if (value.value() instanceof ComponentLike component) return component.asComponent();
		return Component.text(value.text());
	}
}
