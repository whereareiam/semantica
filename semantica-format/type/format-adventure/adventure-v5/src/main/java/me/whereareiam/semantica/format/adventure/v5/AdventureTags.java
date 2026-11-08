package me.whereareiam.semantica.format.adventure.v5;

import me.whereareiam.semantica.format.adventure.MiniMessageTags;
import me.whereareiam.semantica.format.adventure.TagPolicy;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * MiniMessage's standard tags in Adventure 5, by group of the {@link TagPolicy}.
 */
public final class AdventureTags implements MiniMessageTags {
	/**
	 * The tags of Adventure 5.
	 */
	public static final AdventureTags INSTANCE = new AdventureTags();

	private final Map<TagPolicy.Group, TagResolver> resolvers = new EnumMap<>(TagPolicy.Group.class);

	private AdventureTags() {
		resolvers.put(TagPolicy.Group.COLOR, StandardTags.color());
		resolvers.put(TagPolicy.Group.DECORATION, StandardTags.decorations());
		resolvers.put(TagPolicy.Group.GRADIENT, StandardTags.gradient());
		resolvers.put(TagPolicy.Group.RAINBOW, StandardTags.rainbow());
		resolvers.put(TagPolicy.Group.TRANSITION, StandardTags.transition());
		resolvers.put(TagPolicy.Group.PRIDE, StandardTags.pride());
		resolvers.put(TagPolicy.Group.SHADOW_COLOR, StandardTags.shadowColor());
		resolvers.put(TagPolicy.Group.RESET, StandardTags.reset());
		resolvers.put(TagPolicy.Group.NEWLINE, StandardTags.newline());
		resolvers.put(TagPolicy.Group.FONT, StandardTags.font());
		resolvers.put(TagPolicy.Group.KEYBIND, StandardTags.keybind());
		resolvers.put(TagPolicy.Group.TRANSLATABLE, TagResolver.resolver(StandardTags.translatable(), StandardTags.translatableFallback()));
		resolvers.put(TagPolicy.Group.SPRITE, TagResolver.resolver(StandardTags.sprite(), StandardTags.sequentialHead()));
		resolvers.put(TagPolicy.Group.SELECTOR, StandardTags.selector());
		resolvers.put(TagPolicy.Group.SCORE, StandardTags.score());
		resolvers.put(TagPolicy.Group.NBT, StandardTags.nbt());
		resolvers.put(TagPolicy.Group.CLICK, StandardTags.clickEvent());
		resolvers.put(TagPolicy.Group.HOVER, StandardTags.hoverEvent());
		resolvers.put(TagPolicy.Group.INSERTION, StandardTags.insertion());
	}

	@Override
	public @Nullable TagPolicy.Group group(@NotNull String name) {
		for (Map.Entry<TagPolicy.Group, TagResolver> entry : resolvers.entrySet())
			if (entry.getValue().has(name)) return entry.getKey();

		return null;
	}

	/**
	 * Returns the resolver of the tags a policy allows.
	 *
	 * @param policy the policy
	 * @return the resolver
	 */
	public @NotNull TagResolver resolver(@NotNull TagPolicy policy) {
		TagResolver.Builder builder = TagResolver.builder();
		resolvers.forEach((group, resolver) -> {
			if (policy.allows(group)) builder.resolver(resolver);
		});

		return builder.build();
	}
}
