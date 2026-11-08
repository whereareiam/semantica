package me.whereareiam.semantica.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.whereareiam.semantica.Text;
import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.common.catalog.CatalogBuilder;
import me.whereareiam.semantica.common.check.ArgumentCheck;
import me.whereareiam.semantica.common.check.PluralFormCheck;
import me.whereareiam.semantica.common.check.UnknownKeyCheck;
import me.whereareiam.semantica.common.resolution.MessageResolver;
import me.whereareiam.semantica.common.resolution.ValueFormatter;
import me.whereareiam.semantica.issue.Check;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.locale.LocaleChain;
import me.whereareiam.semantica.message.ResolvedMessage;
import me.whereareiam.semantica.source.Source;
import me.whereareiam.semantica.syntax.Syntax;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The translations of one organisation or product: its layers merged into one catalog, and the lookups
 * against it.
 *
 * <p>Semantica loads every layer when it is built, parses each message once and checks the result. A layer
 * added, removed or changed later produces a new catalog, which replaces the old one at once; lookups running
 * meanwhile finish on the catalog they started with. Problems never fail a lookup: they are reported as
 * {@link Issue}s, the affected message is left out and the lookup falls back along the locale chain.</p>
 *
 * <pre>{@code
 * Semantica semantica = Semantica.builder()
 *         .syntax(new IcuSyntax())
 *         .defaultLocale(Locale.ENGLISH)
 *         .layer(Layer.of(FileSource.of(dataFolder.resolve("languages"), configura)))
 *         .build();
 *
 * String line = semantica.resolve(Text.of("shop.bought").with("count", 3), Locale.forLanguageTag("ru")).plain();
 * }</pre>
 */
public final class Semantica implements AutoCloseable {
	private static final System.Logger LOGGER = System.getLogger("Semantica");
	private static final int REPORTED_LIMIT = 10_000;

	private final LocaleChain chain;
	private final CatalogBuilder catalogBuilder;
	private final MessageResolver resolver;
	private final Consumer<Issue> issueHandler;
	private final boolean watch;

	private final Object lock = new Object();
	private final Map<Layer, Slot> slots = new LinkedHashMap<>();
	private final List<Check> checks = new ArrayList<>();
	private final List<Consumer<Catalog>> listeners = new CopyOnWriteArrayList<>();
	private final Set<Issue> reported = ConcurrentHashMap.newKeySet();
	private long nextOrder;
	private boolean closed;

	private volatile Catalog catalog;

	private Semantica(Builder builder, LocaleChain chain) {
		this.chain = chain;
		this.catalogBuilder = new CatalogBuilder(builder.syntax, chain, builder.maxDepth);
		this.resolver = new MessageResolver(new ValueFormatter(builder.clock), builder.maxDepth, builder.maxLength);
		this.issueHandler = builder.issueHandler;
		this.watch = builder.watch;
		this.catalog = Catalog.empty(chain.getDefaultLocale());

		checks.add(new PluralFormCheck());
		checks.add(new UnknownKeyCheck());
		checks.add(new ArgumentCheck());
		checks.addAll(builder.checks);
	}

	/**
	 * Starts building an instance.
	 *
	 * @return a new builder
	 */
	public static @NotNull Builder builder() {
		return new Builder();
	}

	/**
	 * Resolves a text in a locale, falling back along the locale chain.
	 *
	 * @param text   the text
	 * @param locale the locale asked for
	 * @return the resolved message; when no locale of the chain has the key, a message holding the key
	 */
	public @NotNull ResolvedMessage resolve(@NotNull Text text, @NotNull Locale locale) {
		return resolve(text, List.of(locale));
	}

	/**
	 * Resolves a text for several wanted locales, such as a user's chosen locale before their client's.
	 *
	 * @param text    the text
	 * @param locales the locales asked for, the most wanted first; may be empty for the default locale
	 * @return the resolved message; when no locale of the chain has the key, a message holding the key
	 */
	public @NotNull ResolvedMessage resolve(@NotNull Text text, @NotNull List<Locale> locales) {
		Objects.requireNonNull(text, "text");
		Catalog current = catalog;

		return resolver.resolve(current, text, chain.resolve(locales), this::report);
	}

	/**
	 * Tells whether the default locale has a key.
	 *
	 * @param key the key
	 * @return whether a message exists for it
	 */
	public boolean has(@NotNull String key) {
		return catalog.entry(chain.getDefaultLocale(), key).isPresent();
	}

	/**
	 * Returns the current catalog.
	 *
	 * @return the catalog lookups use now
	 */
	public @NotNull Catalog getCatalog() {
		return catalog;
	}

	/**
	 * Returns the locale chain lookups follow.
	 *
	 * @return the chain
	 */
	public @NotNull LocaleChain getChain() {
		return chain;
	}

	/**
	 * Returns the locale every key should exist in.
	 *
	 * @return the default locale
	 */
	public @NotNull Locale getDefaultLocale() {
		return chain.getDefaultLocale();
	}

	/**
	 * Loads every layer again and replaces the catalog.
	 *
	 * @return the new catalog
	 */
	public @NotNull Catalog reload() {
		List<Layer> layers;
		synchronized (lock) {
			layers = List.copyOf(slots.keySet());
		}

		Map<Layer, Source.Content> loaded = new LinkedHashMap<>();
		for (Layer layer : layers) loaded.put(layer, load(layer));

		synchronized (lock) {
			loaded.forEach((layer, content) -> {
				Slot slot = slots.get(layer);
				if (slot != null) slot.content = content;
			});
			return rebuild();
		}
	}

	/**
	 * Adds layers in one step: they are loaded, and one new catalog replaces the old one.
	 *
	 * @param layers the layers
	 * @return the new catalog
	 */
	public @NotNull Catalog add(@NotNull Layer... layers) {
		return replace(null, layers);
	}

	/**
	 * Removes every layer of an owner in one step.
	 *
	 * @param owner the owner given to {@link Layer#owned(String, Source)}
	 * @return the new catalog
	 */
	public @NotNull Catalog remove(@NotNull String owner) {
		return replace(Objects.requireNonNull(owner, "owner"));
	}

	/**
	 * Replaces every layer of an owner with new layers in one step, such as when a plugin reloads; lookups
	 * never see the owner's translations missing.
	 *
	 * @param owner  the owner whose layers are removed, or {@code null} to remove none
	 * @param layers the layers to add
	 * @return the new catalog
	 */
	public @NotNull Catalog replace(@Nullable String owner, @NotNull Layer... layers) {
		Map<Layer, Source.Content> loaded = new LinkedHashMap<>();
		for (Layer layer : layers) loaded.put(Objects.requireNonNull(layer, "layer"), load(layer));

		List<AutoCloseable> stopped = new ArrayList<>();
		Catalog result;
		synchronized (lock) {
			if (closed) throw new IllegalStateException("Semantica is closed");

			if (owner != null) {
				slots.entrySet().removeIf(slot -> {
					if (!owner.equals(slot.getKey().getOwner())) return false;
					stopped.add(slot.getValue().watch);
					return true;
				});
			}

			loaded.forEach((layer, content) -> {
				Slot previous = slots.remove(layer);
				if (previous != null) stopped.add(previous.watch);
				slots.put(layer, new Slot(nextOrder++, content));
			});
			result = rebuild();
		}

		stopped.forEach(Semantica::closeQuietly);
		if (watch) loaded.keySet().forEach(this::watch);

		return result;
	}

	/**
	 * Adds a check, run on the current catalog now and on every catalog after it.
	 *
	 * @param check the check
	 * @return the new catalog
	 */
	public @NotNull Catalog check(@NotNull Check check) {
		synchronized (lock) {
			checks.add(Objects.requireNonNull(check, "check"));
			return rebuild();
		}
	}

	/**
	 * Calls a listener with every new catalog, after it replaced the previous one.
	 *
	 * @param listener the listener, called on the thread that changed the catalog
	 * @return removes the listener
	 */
	public @NotNull AutoCloseable onChange(@NotNull Consumer<Catalog> listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
		return () -> listeners.remove(listener);
	}

	/**
	 * Reports an issue found while using a text, such as a project cutting a text that was too long for where it is shown. Each
	 * issue is reported once per catalog.
	 *
	 * @param issue the issue
	 */
	public void report(@NotNull Issue issue) {
		if (reported.size() >= REPORTED_LIMIT) return;
		if (reported.add(issue)) issueHandler.accept(issue);
	}

	/**
	 * Stops watching every layer. Lookups keep working on the last catalog.
	 */
	@Override
	public void close() {
		List<AutoCloseable> watches = new ArrayList<>();
		synchronized (lock) {
			if (closed) return;

			closed = true;
			slots.values().forEach(slot -> watches.add(slot.watch));
		}

		watches.forEach(Semantica::closeQuietly);
	}

	private void watch(Layer layer) {
		AutoCloseable handle;
		try {
			handle = layer.getSource().watch(() -> reload(layer));
		} catch (RuntimeException exception) {
			report(new Issue(Issue.Kind.SOURCE, null, null, layer.getName(), null, "Cannot watch for changes: " + exception));
			return;
		}

		synchronized (lock) {
			Slot slot = slots.get(layer);
			if (slot != null && !closed) {
				slot.watch = handle;
				return;
			}
		}
		closeQuietly(handle);
	}

	private void reload(Layer layer) {
		Source.Content content = load(layer);
		synchronized (lock) {
			Slot slot = slots.get(layer);
			if (slot == null || closed) return;

			slot.content = content;
			rebuild();
		}
	}

	private Source.Content load(Layer layer) {
		try {
			return layer.getSource().load();
		} catch (Exception exception) {
			report(new Issue(Issue.Kind.SOURCE, null, null, layer.getName(), null, "Cannot load: " + exception));

			synchronized (lock) {
				Slot slot = slots.get(layer);
				return slot != null ? slot.content : Source.Content.empty();
			}
		}
	}

	private Catalog rebuild() {
		List<CatalogBuilder.Layered> ordered = slots.entrySet().stream()
				.sorted(Comparator.<Map.Entry<Layer, Slot>>comparingInt(slot -> slot.getKey().getPriority())
						.thenComparingLong(slot -> slot.getValue().order))
				.map(slot -> new CatalogBuilder.Layered(slot.getKey().getName(), slot.getValue().content))
				.toList();

		Catalog previous = catalog;
		Catalog next = catalogBuilder.build(previous.getRevision() + 1, ordered, List.copyOf(checks));
		catalog = next;

		reported.clear();
		Set<Issue> known = new HashSet<>(previous.getIssues());
		for (Issue issue : next.getIssues())
			if (!known.contains(issue)) issueHandler.accept(issue);
		for (Consumer<Catalog> listener : listeners) listener.accept(next);

		return next;
	}

	private static void closeQuietly(AutoCloseable closeable) {
		if (closeable == null) return;

		try {
			closeable.close();
		} catch (Exception exception) {
			LOGGER.log(System.Logger.Level.WARNING, "Cannot stop watching a source", exception);
		}
	}

	private static void log(Issue issue) {
		System.Logger.Level level = issue.getKind().getSeverity() == Issue.Severity.ERROR
				? System.Logger.Level.ERROR
				: System.Logger.Level.WARNING;
		LOGGER.log(level, issue.toString());
	}

	private static final class Slot {
		private final long order;
		private Source.Content content;
		private AutoCloseable watch;

		private Slot(long order, Source.Content content) {
			this.order = order;
			this.content = content;
		}
	}

	/**
	 * Builds a {@link Semantica} instance.
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Builder {
		private Syntax syntax;
		private Locale defaultLocale = Locale.ENGLISH;
		private final Map<Locale, List<Locale>> fallbacks = new LinkedHashMap<>();
		private final List<Layer> layers = new ArrayList<>();
		private final List<Check> checks = new ArrayList<>();
		private Consumer<Issue> issueHandler = Semantica::log;
		private Clock clock = Clock.systemDefaultZone();
		private int maxDepth = 10;
		private int maxLength = 32_768;
		private boolean watch = true;

		/**
		 * Sets the syntax messages are written in, such as {@code new IcuSyntax()}. Required.
		 *
		 * @param syntax the syntax
		 * @return this builder
		 */
		public @NotNull Builder syntax(@NotNull Syntax syntax) {
			this.syntax = Objects.requireNonNull(syntax, "syntax");
			return this;
		}

		/**
		 * Sets the locale every key exists in, tried last. Defaults to English.
		 *
		 * @param locale the default locale
		 * @return this builder
		 */
		public @NotNull Builder defaultLocale(@NotNull Locale locale) {
			this.defaultLocale = Objects.requireNonNull(locale, "locale");
			return this;
		}

		/**
		 * Adds fallbacks for a locale, tried after it and before its base language.
		 *
		 * @param locale    the locale, such as {@code pt-BR}
		 * @param fallbacks the locales to try next, such as {@code pt-PT}
		 * @return this builder
		 */
		public @NotNull Builder fallback(@NotNull Locale locale, @NotNull Locale... fallbacks) {
			this.fallbacks.computeIfAbsent(Objects.requireNonNull(locale, "locale"), ignored -> new ArrayList<>())
					.addAll(List.of(fallbacks));
			return this;
		}

		/**
		 * Adds a layer.
		 *
		 * @param layer the layer
		 * @return this builder
		 */
		public @NotNull Builder layer(@NotNull Layer layer) {
			layers.add(Objects.requireNonNull(layer, "layer"));
			return this;
		}

		/**
		 * Adds a check run on every catalog, next to Semantica's own.
		 *
		 * @param check the check
		 * @return this builder
		 */
		public @NotNull Builder check(@NotNull Check check) {
			checks.add(Objects.requireNonNull(check, "check"));
			return this;
		}

		/**
		 * Sets where issues go. Defaults to the {@link System.Logger} named {@code Semantica}. Issues found while
		 * loading are reported when they first appear; issues found while resolving, once per catalog.
		 *
		 * @param handler receives issues, on the thread that found them
		 * @return this builder
		 */
		public @NotNull Builder issues(@NotNull Consumer<Issue> handler) {
			this.issueHandler = Objects.requireNonNull(handler, "handler");
			return this;
		}

		/**
		 * Sets the clock relative times are measured from; its zone is the zone dates are shown in. Defaults to
		 * the system clock and zone.
		 *
		 * @param clock the clock
		 * @return this builder
		 */
		public @NotNull Builder clock(@NotNull Clock clock) {
			this.clock = Objects.requireNonNull(clock, "clock");
			return this;
		}

		/**
		 * Sets how deep references and nested texts may go. Defaults to 10.
		 *
		 * @param maxDepth the depth, at least 1
		 * @return this builder
		 */
		public @NotNull Builder maxDepth(int maxDepth) {
			if (maxDepth < 1) throw new IllegalArgumentException("maxDepth must be at least 1");
			this.maxDepth = maxDepth;
			return this;
		}

		/**
		 * Sets how many characters a resolved message may have. Defaults to 32768.
		 *
		 * @param maxLength the length, at least 1
		 * @return this builder
		 */
		public @NotNull Builder maxLength(int maxLength) {
			if (maxLength < 1) throw new IllegalArgumentException("maxLength must be at least 1");
			this.maxLength = maxLength;
			return this;
		}

		/**
		 * Sets whether layers are watched and loaded again when their source changes. Defaults to {@code true}.
		 *
		 * @param watch whether to watch
		 * @return this builder
		 */
		public @NotNull Builder watch(boolean watch) {
			this.watch = watch;
			return this;
		}

		/**
		 * Builds the instance and loads every layer.
		 *
		 * @return the instance
		 * @throws IllegalStateException when no syntax was set
		 */
		public @NotNull Semantica build() {
			if (syntax == null) throw new IllegalStateException("Semantica needs a syntax, such as new IcuSyntax()");
			LocaleChain.Builder chain = LocaleChain.builder(defaultLocale);
			fallbacks.forEach((locale, list) -> chain.fallback(locale, list.toArray(Locale[]::new)));

			Semantica semantica = new Semantica(this, chain.build());
			semantica.add(layers.toArray(Layer[]::new));

			return semantica;
		}
	}
}
