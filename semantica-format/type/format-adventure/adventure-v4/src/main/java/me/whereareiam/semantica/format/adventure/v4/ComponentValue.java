package me.whereareiam.semantica.format.adventure.v4;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.semantica.Value;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An Adventure component as the value of a message's argument, such as an item's name with its hover. The
 * {@link AdventureRenderer} shows it as it is; formats without components show its text.
 * <pre>{@code
 * Message message = Message.of("shop.bought").with("item", ComponentValue.of(item.displayName()));
 * }</pre>
 */
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ComponentValue implements Value.Rendered {
	private final Component component;

	/**
	 * Returns the value of a component.
	 *
	 * @param component the component
	 * @return the value
	 */
	public static @NotNull ComponentValue of(@NotNull ComponentLike component) {
		return new ComponentValue(Objects.requireNonNull(component, "component").asComponent());
	}

	/**
	 * Returns the text of the component and its children, without styles and without what other components, such
	 * as translatable ones, would show.
	 *
	 * @return the text
	 */
	@Override
	public @NotNull String plain() {
		StringBuilder text = new StringBuilder();
		append(component, text);

		return text.toString();
	}

	private static void append(Component component, StringBuilder text) {
		if (component instanceof TextComponent written) text.append(written.content());
		component.children().forEach(child -> append(child, text));
	}
}
