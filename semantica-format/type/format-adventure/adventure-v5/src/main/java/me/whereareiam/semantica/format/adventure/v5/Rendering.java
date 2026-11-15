package me.whereareiam.semantica.format.adventure.v5;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * What one rendering adds to an {@link AdventureRenderer}, such as the placeholders of the reader: a function over the
 * text of literal parts, and more tags. Both apply to what translators and administrators wrote and never to values,
 * so a {@code %player_name%} an administrator wrote is filled in, and the same text a player typed into a value is
 * shown as typed.
 *
 * <pre>{@code
 * Rendering rendering = Rendering.builder()
 *         .text(text -> PlaceholderAPI.setPlaceholders(player, text))
 *         .tags(Placeholder.unparsed("server", serverName))
 *         .build();
 *
 * Component message = renderer.render(semantica.resolve(content, values, locales), rendering);
 * }</pre>
 */
@Getter
public final class Rendering {
	private static final Rendering NONE = builder().build();

	private final UnaryOperator<String> text;
	private final List<TagResolver> tags;

	private Rendering(Builder builder) {
		this.text = builder.text;
		this.tags = List.copyOf(builder.tags);
	}

	/**
	 * Returns a rendering that adds nothing.
	 *
	 * @return the rendering
	 */
	public static @NotNull Rendering none() {
		return NONE;
	}

	/**
	 * Starts building a rendering.
	 *
	 * @return a new builder
	 */
	public static @NotNull Builder builder() {
		return new Builder();
	}

	/**
	 * Builds a {@link Rendering}.
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Builder {
		private UnaryOperator<String> text = UnaryOperator.identity();
		private final List<TagResolver> tags = new ArrayList<>();

		/**
		 * Applies a function to the text of each literal part before it is read as MiniMessage, after the functions given
		 * before. What it returns is read as MiniMessage with the renderer's tags.
		 *
		 * @param function the function, such as one that fills in PlaceholderAPI's placeholders
		 * @return this builder
		 */
		public @NotNull Builder text(@NotNull UnaryOperator<String> function) {
			Objects.requireNonNull(function, "function");
			UnaryOperator<String> previous = text;
			this.text = literal -> function.apply(previous.apply(literal));

			return this;
		}

		/**
		 * Adds tags that literal parts may use besides the renderer's {@link me.whereareiam.semantica.format.adventure.TagPolicy},
		 * such as MiniPlaceholders' placeholders.
		 *
		 * @param resolvers the tags
		 * @return this builder
		 */
		public @NotNull Builder tags(@NotNull TagResolver... resolvers) {
			for (TagResolver resolver : resolvers) tags.add(Objects.requireNonNull(resolver, "resolver"));
			return this;
		}

		/**
		 * Builds the rendering.
		 *
		 * @return the rendering
		 */
		public @NotNull Rendering build() {
			return new Rendering(this);
		}
	}
}
