package me.whereareiam.semantica.testing;

import me.whereareiam.semantica.source.Source;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A source kept in memory, for tests: put messages, then call {@link #changed()} to make a watching Semantica
 * instance load them again.
 *
 * <pre>{@code
 * MemorySource source = new MemorySource("test")
 *         .put(Locale.ENGLISH, "greeting", "Hello, {name}!")
 *         .put(Locale.forLanguageTag("de"), "greeting", "Hallo, {name}!");
 *
 * Semantica semantica = Semantica.builder()
 *         .syntax(new IcuSyntax())
 *         .layer(Layer.of(source))
 *         .build();
 * }</pre>
 */
public final class MemorySource implements Source {
	private final String name;
	private final Map<Locale, Map<String, String>> messages = new LinkedHashMap<>();
	private final List<Runnable> watchers = new CopyOnWriteArrayList<>();

	/**
	 * Creates an empty source.
	 *
	 * @param name its name in issues
	 */
	public MemorySource(@NotNull String name) {
		this.name = Objects.requireNonNull(name, "name");
	}

	/**
	 * Puts a message, replacing one of the same locale and key.
	 *
	 * @param locale its locale
	 * @param key    its key
	 * @param text   its source
	 * @return this source
	 */
	public synchronized @NotNull MemorySource put(@NotNull Locale locale, @NotNull String key, @NotNull String text) {
		messages.computeIfAbsent(locale, ignored -> new LinkedHashMap<>()).put(key, text);
		return this;
	}

	/**
	 * Removes a message.
	 *
	 * @param locale its locale
	 * @param key    its key
	 * @return this source
	 */
	public synchronized @NotNull MemorySource remove(@NotNull Locale locale, @NotNull String key) {
		Map<String, String> byKey = messages.get(locale);
		if (byKey != null) byKey.remove(key);

		return this;
	}

	/**
	 * Tells every watcher that the messages changed.
	 */
	public void changed() {
		watchers.forEach(Runnable::run);
	}

	@Override
	public @NotNull String getName() {
		return name;
	}

	@Override
	public synchronized @NotNull Content load() {
		Content.Builder content = Content.builder();
		messages.forEach((locale, byKey) -> byKey.forEach((key, text) -> content.put(locale, key, text, name)));

		return content.build();
	}

	@Override
	public @NotNull AutoCloseable watch(@NotNull Runnable onChange) {
		watchers.add(onChange);
		return () -> watchers.remove(onChange);
	}
}
