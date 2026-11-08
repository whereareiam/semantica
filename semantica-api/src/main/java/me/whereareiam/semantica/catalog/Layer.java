package me.whereareiam.semantica.catalog;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.whereareiam.semantica.source.Source;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * One source of translations in a Semantica instance, with its priority and, optionally, its owner.
 *
 * <p>Semantica merges layers per key and locale: of all layers that have a key in a locale, the one with the
 * highest priority wins; between equal priorities, the one added last wins. A layer with an owner can be
 * removed with all other layers of that owner in one step, which is how a plugin's translations leave when it
 * stops.</p>
 *
 * <pre>{@code
 * Layer.of(HttpSource.builder(bundles).product("intercept").build(), Layer.SHARED);   // the organisation's bundles
 * Layer.of(FileSource.of(dataFolder.resolve("languages"), configura), Layer.OVERRIDES); // the server's own texts
 * Layer.owned("shop", FileSource.of(shopFolder.resolve("languages"), configura));
 * }</pre>
 */
@Getter
@EqualsAndHashCode
public final class Layer {
	/**
	 * Priority of default translations, the lowest.
	 */
	public static final int DEFAULTS = 0;

	/**
	 * Priority of translations shared by an organisation, such as bundles fetched by an HTTP source.
	 */
	public static final int SHARED = 100;

	/**
	 * Priority of an administrator's own texts, the highest.
	 */
	public static final int OVERRIDES = 200;

	private final @Nullable String owner;
	private final @NotNull Source source;
	private final int priority;

	private Layer(@Nullable String owner, @NotNull Source source, int priority) {
		this.owner = owner;
		this.source = Objects.requireNonNull(source, "source");
		this.priority = priority;
	}

	/**
	 * Creates a layer without owner and with the priority of defaults.
	 *
	 * @param source where its translations come from
	 * @return the layer
	 */
	public static @NotNull Layer of(@NotNull Source source) {
		return new Layer(null, source, DEFAULTS);
	}

	/**
	 * Creates a layer without owner.
	 *
	 * @param source   where its translations come from
	 * @param priority its priority, such as {@link #SHARED} or {@link #OVERRIDES}
	 * @return the layer
	 */
	public static @NotNull Layer of(@NotNull Source source, int priority) {
		return new Layer(null, source, priority);
	}

	/**
	 * Creates a layer that belongs to an owner, with the priority of defaults.
	 *
	 * @param owner  who added it, such as a plugin's id; removing the owner removes the layer
	 * @param source where its translations come from
	 * @return the layer
	 */
	public static @NotNull Layer owned(@NotNull String owner, @NotNull Source source) {
		return owned(owner, source, DEFAULTS);
	}

	/**
	 * Creates a layer that belongs to an owner.
	 *
	 * @param owner    who added it, such as a plugin's id; removing the owner removes the layer
	 * @param source   where its translations come from
	 * @param priority its priority
	 * @return the layer
	 */
	public static @NotNull Layer owned(@NotNull String owner, @NotNull Source source, int priority) {
		Objects.requireNonNull(owner, "owner");
		if (owner.isBlank()) throw new IllegalArgumentException("An owner needs a name");

		return new Layer(owner, source, priority);
	}

	/**
	 * Returns the name issues give for this layer: the owner, or else the source's name.
	 *
	 * @return the name
	 */
	public @NotNull String getName() {
		return owner != null ? owner : source.getName();
	}

	@Override
	public String toString() {
		return getName() + " (" + priority + ")";
	}
}
