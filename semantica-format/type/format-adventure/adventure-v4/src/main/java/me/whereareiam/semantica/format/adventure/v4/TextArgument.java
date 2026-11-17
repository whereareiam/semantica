package me.whereareiam.semantica.format.adventure.v4;

import me.whereareiam.semantica.Message;
import me.whereareiam.semantica.Value;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.VirtualComponent;
import net.kyori.adventure.text.VirtualComponentRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.TemporalAccessor;
import java.util.Date;
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
 * Semantica freely. The entry holds the value as the JDK or Adventure type it was made of, which every copy reads; a
 * nested {@link Message} of another copy is shown as its key.</p>
 */
public final class TextArgument implements VirtualComponentRenderer<Void>, Map.Entry<String, Object> {
	private static final String NAME = TextArgument.class.getSimpleName();

	private final String name;
	private final Value value;

	private TextArgument(String name, Value value) {
		this.name = Objects.requireNonNull(name, "name");
		this.value = Objects.requireNonNull(value, "value");
	}

	/**
	 * Creates a named text argument, shown as it is.
	 *
	 * @param name  the argument's name in the message, such as {@code player}
	 * @param value the text
	 * @return the argument, to pass to {@link Component#translatable(String, ComponentLike...)}
	 */
	public static @NotNull ComponentLike of(@NotNull String name, @NotNull String value) {
		return of(name, Value.text(value));
	}

	/**
	 * Creates a named number argument.
	 *
	 * @param name  the argument's name in the message, such as {@code count}
	 * @param value the number
	 * @return the argument
	 */
	public static @NotNull ComponentLike of(@NotNull String name, @NotNull Number value) {
		return of(name, Value.number(value));
	}

	/**
	 * Creates a named date or time argument.
	 *
	 * @param name  the argument's name in the message
	 * @param value an instant, a zoned, offset or local date-time, a local date or a local time
	 * @return the argument
	 */
	public static @NotNull ComponentLike of(@NotNull String name, @NotNull TemporalAccessor value) {
		return of(name, Value.time(value));
	}

	/**
	 * Creates a named date or time argument.
	 *
	 * @param name  the argument's name in the message
	 * @param value the date, read as an instant
	 * @return the argument
	 */
	public static @NotNull ComponentLike of(@NotNull String name, @NotNull Date value) {
		return of(name, Value.time(value));
	}

	/**
	 * Creates a named boolean argument.
	 *
	 * @param name  the argument's name in the message
	 * @param value the boolean
	 * @return the argument
	 */
	public static @NotNull ComponentLike of(@NotNull String name, boolean value) {
		return of(name, Value.flag(value));
	}

	/**
	 * Creates a named enum argument.
	 *
	 * @param name  the argument's name in the message
	 * @param value the constant
	 * @return the argument
	 */
	public static @NotNull ComponentLike of(@NotNull String name, @NotNull Enum<?> value) {
		return of(name, Value.choice(value));
	}

	/**
	 * Creates a named component argument, shown as it is.
	 *
	 * @param name  the argument's name in the message
	 * @param value the component
	 * @return the argument
	 */
	public static @NotNull ComponentLike of(@NotNull String name, @NotNull ComponentLike value) {
		return of(name, ComponentValue.of(value));
	}

	/**
	 * Creates a named argument of a value.
	 *
	 * @param name  the argument's name in the message
	 * @param value the value
	 * @return the argument
	 */
	public static @NotNull ComponentLike of(@NotNull String name, @NotNull Value value) {
		return Component.virtual(Void.class, new TextArgument(name, value));
	}

	/**
	 * Reads a named argument made by any copy of this class.
	 *
	 * @param argument an argument's value, as {@code TranslationArgument.value()} gives it
	 * @return the name and the value, or {@code null} when the argument is no named argument
	 */
	static @Nullable Named read(@Nullable Object argument) {
		if (!(argument instanceof VirtualComponent virtual)) return null;

		VirtualComponentRenderer<?> renderer = virtual.renderer();
		if (!NAME.equals(renderer.getClass().getSimpleName())) return null;
		if (!(renderer instanceof Map.Entry<?, ?> entry) || !(entry.getKey() instanceof String name) || entry.getValue() == null) return null;

		return new Named(name, ValueExchange.read(entry.getValue()));
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
	 * Returns the argument's value as the JDK or Adventure type every copy of Semantica reads: a string, a number, a
	 * date or time, a boolean, an enum constant, a message or a component.
	 *
	 * @return the value
	 */
	@Override
	public @NotNull Object getValue() {
		return ValueExchange.write(value);
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
		return value instanceof ComponentValue component ? component.getComponent() : Component.text(value.plain());
	}

	@Override
	public @NotNull String fallbackString() {
		return value.plain();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Map.Entry<?, ?> entry && name.equals(entry.getKey()) && getValue().equals(entry.getValue());
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ getValue().hashCode();
	}

	@Override
	public String toString() {
		return name + "=" + value.plain();
	}

	/**
	 * A named argument as read.
	 *
	 * @param name  the argument's name
	 * @param value its value
	 */
	record Named(String name, Value value) {
	}
}
