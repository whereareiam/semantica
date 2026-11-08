package me.whereareiam.semantica.format.adventure.v5;

import me.whereareiam.semantica.Text;
import me.whereareiam.semantica.common.Semantica;
import me.whereareiam.semantica.format.adventure.TagPolicy;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * An Adventure translator answering from a Semantica instance, to add to {@code GlobalTranslator}. It translates
 * only keys the instance's default locale has and returns {@code null} for any other, so the next source answers.
 *
 * <pre>{@code
 * GlobalTranslator.translator().addSource(AdventureTranslator.of(semantica, Key.key("shop", "translations")));
 *
 * player.sendMessage(Component.translatable("shop.bought",
 *         TextArgument.of("item", itemName),
 *         TextArgument.of("count", 3)));
 * }</pre>
 *
 * <p>Arguments made with {@link TextArgument} are named; others are numbered from {@code 0}. Adventure's own
 * {@code Argument.string(...)} is not read, since Adventure keeps its name private.</p>
 *
 * <h2>The authority</h2>
 *
 * <p>Adventure asks its sources in no fixed order, so registering first does not answer first. One translator,
 * such as a network's, can register as the authority with {@link #authority(Semantica, AdventureRenderer)}: every
 * other Semantica translator then leaves the keys the authority can translate to it, in every plugin, since
 * Adventure is shared by the server. A translator notices an authority that was added or removed within
 * {@link #AUTHORITY_INTERVAL}.</p>
 *
 * <pre>{@code
 * GlobalTranslator.translator().addSource(AdventureTranslator.authority(semantica, renderer));
 * }</pre>
 */
public final class AdventureTranslator implements Translator {
	/**
	 * The name the authority registers under.
	 */
	public static final Key AUTHORITY = Key.key("semantica", "authority");

	/**
	 * How long a translator keeps what it found out about the authority before looking again.
	 */
	public static final Duration AUTHORITY_INTERVAL = Duration.ofSeconds(5);

	private final Semantica semantica;
	private final Key name;
	private final AdventureRenderer renderer;
	private final @Nullable AuthorityLookup authority;

	private AdventureTranslator(Semantica semantica, Key name, AdventureRenderer renderer, @Nullable AuthorityLookup authority) {
		this.semantica = Objects.requireNonNull(semantica, "semantica");
		this.name = Objects.requireNonNull(name, "name");
		this.renderer = Objects.requireNonNull(renderer, "renderer");
		this.authority = authority;
	}

	/**
	 * Creates a translator that renders with the default tags.
	 *
	 * @param semantica the translations
	 * @param name      the translator's name in {@code GlobalTranslator}, such as {@code shop:translations}
	 * @return the translator
	 */
	public static @NotNull AdventureTranslator of(@NotNull Semantica semantica, @NotNull Key name) {
		return of(semantica, name, AdventureRenderer.of(TagPolicy.defaults()));
	}

	/**
	 * Creates a translator that renders with a renderer of your own.
	 *
	 * @param semantica the translations
	 * @param name      the translator's name in {@code GlobalTranslator}
	 * @param renderer  renders the resolved messages
	 * @return the translator
	 */
	public static @NotNull AdventureTranslator of(@NotNull Semantica semantica, @NotNull Key name, @NotNull AdventureRenderer renderer) {
		return of(semantica, name, renderer, AUTHORITY_INTERVAL);
	}

	static AdventureTranslator of(Semantica semantica, Key name, AdventureRenderer renderer, Duration interval) {
		if (AUTHORITY.equals(name)) throw new IllegalArgumentException("Use authority(...) for the name " + AUTHORITY.asString());
		return new AdventureTranslator(semantica, name, renderer, new AuthorityLookup(AUTHORITY, interval));
	}

	/**
	 * Creates the authority: the translator every other Semantica translator leaves the keys it can translate to,
	 * named {@link #AUTHORITY}. Register one per server.
	 *
	 * @param semantica the translations that take precedence, such as a network's
	 * @param renderer  renders the resolved messages
	 * @return the translator
	 */
	public static @NotNull AdventureTranslator authority(@NotNull Semantica semantica, @NotNull AdventureRenderer renderer) {
		return new AdventureTranslator(semantica, AUTHORITY, renderer, null);
	}

	/**
	 * Tells whether this translator is the authority.
	 *
	 * @return whether it was made by {@link #authority(Semantica, AdventureRenderer)}
	 */
	public boolean isAuthority() {
		return authority == null;
	}

	private boolean defers(String key, Locale locale) {
		return authority != null && authority.defers(this, key, locale);
	}

	@Override
	public @NotNull Key name() {
		return name;
	}

	@Override
	public @NotNull TriState hasAnyTranslations() {
		return TriState.byBoolean(!semantica.getCatalog().getLocales().isEmpty());
	}

	@Override
	public boolean canTranslate(@NotNull String key, @NotNull Locale locale) {
		return semantica.has(key) && !defers(key, locale);
	}

	@Override
	public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
		return null;
	}

	@Override
	public @Nullable Component translate(@NotNull TranslatableComponent component, @NotNull Locale locale) {
		if (!semantica.has(component.key()) || defers(component.key(), locale)) return null;

		Text text = Text.of(component.key());
		List<TranslationArgument> arguments = component.arguments();
		for (int index = 0; index < arguments.size(); index++) {
			Object value = arguments.get(index).value();
			Map.Entry<String, Object> named = TextArgument.read(value);
			if (named != null) {
				text = text.with(named.getKey(), named.getValue()).with(String.valueOf(index), named.getValue());
				continue;
			}
			text = text.with(String.valueOf(index), value);
		}

		Component rendered = renderer.render(semantica.resolve(text, locale));
		if (!component.style().isEmpty()) rendered = rendered.applyFallbackStyle(component.style());

		return rendered.append(component.children());
	}
}
