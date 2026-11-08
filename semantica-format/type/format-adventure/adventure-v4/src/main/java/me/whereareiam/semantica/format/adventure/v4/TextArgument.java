package me.whereareiam.semantica.format.adventure.v4;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.VirtualComponent;
import net.kyori.adventure.text.VirtualComponentRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * A named argument of a {@link Component#translatable(String, ComponentLike...)} component, read by an
 * {@link AdventureTranslator}. The value keeps its type, so a number still chooses a plural form and a date is
 * formatted for the reader.
 *
 * <pre>{@code
 * player.sendMessage(Component.translatable("shop.bought",
 *         TextArgument.of("item", itemName),
 *         TextArgument.of("count", 3)));
 * }</pre>
 *
 * <p>The translator that reads the argument may come from another copy of Semantica, such as a network's in another
 * plugin's class loader. It recognises an argument by Adventure's {@link VirtualComponent}, by this class's simple
 * name {@code TextArgument}, and by {@link Map.Entry} of the JDK for the name and the value, so plugins may relocate
 * Semantica freely. Values of JDK types, enums and Adventure components cross between copies as they are; a nested
 * {@link me.whereareiam.semantica.Text} of another copy is shown as its key and values.</p>
 */
public final class TextArgument implements VirtualComponentRenderer<Void>, Map.Entry<String, Object> {
	private static final String NAME = TextArgument.class.getSimpleName();

	private final String name;
	private final Object value;

	private TextArgument(String name, Object value) {
		this.name = Objects.requireNonNull(name, "name");
		this.value = Objects.requireNonNull(value, "value");
	}

	/**
	 * Creates a named argument.
	 *
	 * @param name  the argument's name in the message, such as {@code count}
	 * @param value its value: text, a number, a date, a boolean, an enum, a component or a nested text
	 * @return the argument, to pass to {@link Component#translatable(String, ComponentLike...)}
	 */
	public static @NotNull ComponentLike of(@NotNull String name, @NotNull Object value) {
		return Component.virtual(Void.class, new TextArgument(name, value));
	}

	/**
	 * Reads a named argument made by any copy of this class.
	 *
	 * @param value an argument's value, as {@code TranslationArgument.value()} gives it
	 * @return the name and the value, or {@code null} when the value is no named argument
	 */
	static @Nullable Map.Entry<String, Object> read(@Nullable Object value) {
		if (!(value instanceof VirtualComponent virtual)) return null;

		VirtualComponentRenderer<?> renderer = virtual.renderer();
		if (!NAME.equals(renderer.getClass().getSimpleName())) return null;
		if (!(renderer instanceof Map.Entry<?, ?> entry) || !(entry.getKey() instanceof String name) || entry.getValue() == null) return null;

		return Map.entry(name, entry.getValue());
	}

	/**
	 * Returns the argument's name.
	 *
	 * @return the name
	 */
	public @NotNull String getName() {
		return name;
	}

	/**
	 * Returns the argument's name, as a {@link Map.Entry}.
	 *
	 * @return the name
	 */
	@Override
	public @NotNull String getKey() {
		return name;
	}

	/**
	 * Returns the argument's value.
	 *
	 * @return the value
	 */
	@Override
	public @NotNull Object getValue() {
		return value;
	}

	/**
	 * Always fails: an argument is immutable.
	 *
	 * @param value ignored
	 * @return never
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Object setValue(Object value) {
		throw new UnsupportedOperationException("A TextArgument is immutable");
	}

	@Override
	public @NotNull ComponentLike apply(@NotNull Void context) {
		return value instanceof ComponentLike component ? component : Component.text(String.valueOf(value));
	}

	@Override
	public @NotNull String fallbackString() {
		return String.valueOf(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Map.Entry<?, ?> entry && name.equals(entry.getKey()) && value.equals(entry.getValue());
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ value.hashCode();
	}

	@Override
	public String toString() {
		return name + "=" + value;
	}
}
