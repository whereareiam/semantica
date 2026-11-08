package me.whereareiam.semantica.source.http;

import me.whereareiam.semantica.KeyPrefix;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.source.Source;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Translations from the bundles an organisation's translations repository builds, fetched over HTTP.
 *
 * <p>The source reads {@code manifest.json} at its base address and downloads {@code <product>/<locale>.json}
 * for its product and the locales it is given, plus the manifest's default locale and the base language of
 * each locale. A bundle is downloaded again only when its hash in the manifest changed, and its bytes must match
 * that hash. When watched, the source polls the manifest and reports a change when its revision changes.</p>
 *
 * <p>With a cache folder, the source keeps the last bundles on disk and starts from them when the server cannot
 * be reached.</p>
 *
 * <pre>{@code
 * HttpSource bundles = HttpSource.builder(URI.create("https://translations.oraylen.com/bundles/"))
 *         .product("intercept")
 *         .prefixes("game", "realm.vanilla")
 *         .locales(Set.of(Locale.ENGLISH, Locale.forLanguageTag("ru")))
 *         .cache(dataFolder.resolve("cache/translations"))
 *         .build();
 *
 * Layer.of(bundles, Layer.SHARED);
 * }</pre>
 */
public final class HttpSource implements Source {
	private static final System.Logger LOGGER = System.getLogger("Semantica");

	private final URI base;
	private final String product;
	private final List<KeyPrefix> prefixes;
	private final Set<Locale> locales;
	private final Duration interval;
	private final Duration timeout;
	private final Path cache;
	private final HttpClient client;

	private final Object lock = new Object();
	private Manifest manifest;
	private final Map<Locale, Downloaded> bundles = new HashMap<>();

	private HttpSource(Builder builder) {
		this.base = builder.base;
		this.product = builder.product;
		this.prefixes = List.copyOf(builder.prefixes);
		this.locales = builder.locales == null ? null : Set.copyOf(builder.locales);
		this.interval = builder.interval;
		this.timeout = builder.timeout;
		this.cache = builder.cache;
		this.client = builder.client != null ? builder.client : HttpClient.newBuilder()
				.connectTimeout(timeout)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	/**
	 * Starts building a source.
	 *
	 * @param base the address the manifest and the bundles are under, such as
	 *             {@code https://translations.oraylen.com/bundles/}
	 * @return a new builder
	 */
	public static @NotNull Builder builder(@NotNull URI base) {
		return new Builder(base);
	}

	@Override
	public @NotNull String getName() {
		return "http:" + base + " " + product;
	}

	@Override
	public @NotNull Content load() throws IOException {
		synchronized (lock) {
			Manifest current;
			try {
				current = Manifest.read(get("manifest.json"));
			} catch (IOException exception) {
				if (manifest == null && cache != null && Files.exists(cache.resolve("manifest.json"))) {
					LOGGER.log(System.Logger.Level.WARNING, "Cannot reach " + base + ", starting from the cached bundles: " + exception.getMessage());
					return fromCache();
				}
				throw exception;
			}

			Content.Builder content = Content.builder();
			Map<Locale, String> available = current.bundles(product);
			if (available.isEmpty()) {
				content.issue(issue(null, "The manifest has no bundles for '" + product + "'"));
			}

			Map<Locale, Downloaded> next = new HashMap<>();
			for (Locale locale : wanted(current, available.keySet())) {
				String hash = available.get(locale);
				Downloaded known = bundles.get(locale);
				if (known != null && known.hash().equals(hash)) {
					next.put(locale, known);
					continue;
				}

				try {
					byte[] bytes = get(Manifest.path(product, locale));
					String actual = Bundle.hash(bytes);
					if (!actual.equals(hash))
						throw new IOException("Its SHA-256 is " + actual + ", but the manifest says " + hash);

					next.put(locale, new Downloaded(hash, Bundle.read(bytes)));
					store(Manifest.path(product, locale), bytes);
				} catch (IOException exception) {
					content.issue(issue(locale, "Cannot use " + Manifest.path(product, locale) + ": " + exception.getMessage()));
					if (known != null) next.put(locale, known);
				}
			}

			manifest = current;
			bundles.clear();
			bundles.putAll(next);
			store("manifest.json", current.write());

			return fill(content);
		}
	}

	@Override
	public @NotNull AutoCloseable watch(@NotNull Runnable onChange) {
		ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(task ->
				Thread.ofPlatform().daemon().name("Semantica poll " + product).unstarted(task));

		poller.scheduleWithFixedDelay(() -> {
			try {
				Manifest current = Manifest.read(get("manifest.json"));
				String known;
				synchronized (lock) {
					known = manifest != null ? manifest.getRevision() : null;
				}
				if (!current.getRevision().equals(known)) onChange.run();
			} catch (IOException exception) {
				LOGGER.log(System.Logger.Level.DEBUG, "Cannot poll " + base + ": " + exception.getMessage());
			} catch (RuntimeException exception) {
				LOGGER.log(System.Logger.Level.WARNING, "Cannot poll " + base, exception);
			}
		}, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);

		return poller::shutdownNow;
	}

	private Set<Locale> wanted(Manifest current, Set<Locale> available) {
		if (locales == null) return available;

		Set<Locale> wanted = new LinkedHashSet<>();
		for (Locale locale : locales) {
			wanted.add(locale);
			wanted.add(Locale.forLanguageTag(locale.getLanguage()));
		}
		wanted.add(current.getDefaultLocale());
		wanted.retainAll(available);

		return wanted;
	}

	private Content fill(Content.Builder content) {
		bundles.forEach((locale, bundle) -> bundle.messages().forEach((key, text) -> {
			if (kept(key)) content.put(locale, key, text, Manifest.path(product, locale));
		}));

		return content.build();
	}

	private boolean kept(String key) {
		return prefixes.isEmpty() || prefixes.stream().anyMatch(prefix -> prefix.matches(key));
	}

	private Content fromCache() throws IOException {
		Manifest cached = Manifest.read(Files.readAllBytes(cache.resolve("manifest.json")));
		Content.Builder content = Content.builder();

		for (Map.Entry<Locale, String> bundle : cached.bundles(product).entrySet()) {
			if (!wanted(cached, cached.bundles(product).keySet()).contains(bundle.getKey())) continue;

			Path file = cache.resolve(Manifest.path(product, bundle.getKey()));
			if (!Files.exists(file)) continue;

			byte[] bytes = Files.readAllBytes(file);
			if (!Bundle.hash(bytes).equals(bundle.getValue())) {
				content.issue(issue(bundle.getKey(), "The cached " + file.getFileName() + " does not match the cached manifest"));
				continue;
			}
			bundles.put(bundle.getKey(), new Downloaded(bundle.getValue(), Bundle.read(bytes)));
		}
		manifest = cached;

		return fill(content);
	}

	private byte[] get(String path) throws IOException {
		HttpRequest request = HttpRequest.newBuilder(base.resolve(path))
				.timeout(timeout)
				.header("Accept", "application/json")
				.GET()
				.build();

		HttpResponse<byte[]> response;
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while fetching " + request.uri(), exception);
		}

		if (response.statusCode() != 200) throw new IOException(request.uri() + " answered " + response.statusCode());
		return response.body();
	}

	private void store(String path, byte[] bytes) {
		if (cache == null) return;

		try {
			Path target = cache.resolve(path);
			Files.createDirectories(target.getParent());
			Path temporary = Files.createTempFile(target.getParent(), ".download-", ".json");
			Files.write(temporary, bytes);
			Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException exception) {
			LOGGER.log(System.Logger.Level.WARNING, "Cannot cache " + path + " in " + cache, exception);
		}
	}

	private Issue issue(@Nullable Locale locale, String detail) {
		return new Issue(Issue.Kind.SOURCE, null, locale, getName(), null, detail);
	}

	private record Downloaded(String hash, Map<String, String> messages) {
	}

	/**
	 * Builds an {@link HttpSource}.
	 */
	public static final class Builder {
		private final URI base;
		private String product;
		private final List<KeyPrefix> prefixes = new ArrayList<>();
		private Set<Locale> locales;
		private Duration interval = Duration.ofMinutes(5);
		private Duration timeout = Duration.ofSeconds(15);
		private Path cache;
		private HttpClient client;

		private Builder(URI base) {
			String text = Objects.requireNonNull(base, "base").toString();
			this.base = URI.create(text.endsWith("/") ? text : text + "/");
		}

		/**
		 * Sets the product whose bundles are read, as named in the translations repository's
		 * {@code semantica.json}. Required.
		 *
		 * @param product the product, such as {@code intercept}
		 * @return this builder
		 */
		public @NotNull Builder product(@NotNull String product) {
			this.product = Objects.requireNonNull(product, "product");
			return this;
		}

		/**
		 * Keeps only keys below these prefixes, by whole segments. Without prefixes, every key of the product's
		 * bundles is kept.
		 *
		 * @param prefixes the prefixes, such as {@code game} or {@code realm.vanilla}
		 * @return this builder
		 * @throws IllegalArgumentException when a value is no key prefix
		 */
		public @NotNull Builder prefixes(@NotNull String... prefixes) {
			for (String prefix : prefixes) this.prefixes.add(KeyPrefix.of(prefix));
			return this;
		}

		/**
		 * Downloads only bundles of these locales, their base languages and the default locale. Without locales,
		 * every bundle of the product is downloaded.
		 *
		 * @param locales the enabled locales
		 * @return this builder
		 */
		public @NotNull Builder locales(@NotNull Set<Locale> locales) {
			this.locales = Set.copyOf(locales);
			return this;
		}

		/**
		 * Sets how often the manifest is polled while the source is watched. Defaults to five minutes.
		 *
		 * @param interval the interval
		 * @return this builder
		 */
		public @NotNull Builder interval(@NotNull Duration interval) {
			if (interval.isNegative() || interval.isZero()) throw new IllegalArgumentException("The interval must be positive");
			this.interval = interval;
			return this;
		}

		/**
		 * Sets how long a request may take. Defaults to fifteen seconds.
		 *
		 * @param timeout the timeout
		 * @return this builder
		 */
		public @NotNull Builder timeout(@NotNull Duration timeout) {
			this.timeout = Objects.requireNonNull(timeout, "timeout");
			return this;
		}

		/**
		 * Keeps the last manifest and bundles in a folder, to start from when the server cannot be reached.
		 *
		 * @param folder the folder, such as {@code cache/translations} in a plugin's data folder
		 * @return this builder
		 */
		public @NotNull Builder cache(@NotNull Path folder) {
			this.cache = Objects.requireNonNull(folder, "folder");
			return this;
		}

		/**
		 * Sets the HTTP client. Defaults to a client of its own.
		 *
		 * @param client the client
		 * @return this builder
		 */
		public @NotNull Builder client(@NotNull HttpClient client) {
			this.client = Objects.requireNonNull(client, "client");
			return this;
		}

		/**
		 * Builds the source. Nothing is fetched until it is loaded.
		 *
		 * @return the source
		 */
		public @NotNull HttpSource build() {
			if (product == null) throw new IllegalStateException("An HTTP source needs a product");
			return new HttpSource(this);
		}
	}
}
