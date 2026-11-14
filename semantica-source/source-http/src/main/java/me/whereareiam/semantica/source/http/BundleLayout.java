package me.whereareiam.semantica.source.http;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Where an {@link HttpSource} finds the manifest and the bundles below its base address.
 *
 * <ul>
 *     <li>{@link #paths()}, the default: {@code manifest.json} and {@code <product>/<locale>.json}, the folder
 *     {@code semantica-cli bundle} writes, published as it is.</li>
 *     <li>{@link #digests(String)}: a file depot that serves files by their digest and a moving name for the
 *     manifest. The manifest is read at the name given, and each bundle at {@code sha256-<hash>/<locale>.json},
 *     where {@code <hash>} is the hash the manifest lists for it.</li>
 * </ul>
 *
 * <pre>{@code
 * HttpSource bundles = HttpSource.builder(URI.create("https://files.oraylen.com/translations/"))
 *         .product("intercept")
 *         .layout(BundleLayout.digests("@manifest"))
 *         .build();
 * }</pre>
 *
 * <p>Either way a bundle is used only when the SHA-256 of its bytes equals the manifest's hash, and the cache keeps
 * {@code manifest.json} and {@code <product>/<locale>.json}.</p>
 */
public sealed interface BundleLayout permits BundleLayout.Paths, BundleLayout.Digests {
	/**
	 * Returns the layout {@code semantica-cli bundle} writes: {@code manifest.json} and
	 * {@code <product>/<locale>.json}.
	 *
	 * @return the layout
	 */
	static @NotNull BundleLayout paths() {
		return new Paths();
	}

	/**
	 * Returns the layout of a file depot that serves bundles by their digest: the manifest at a name of its own,
	 * which may answer with a redirect, and each bundle at {@code sha256-<hash>/<locale>.json}.
	 *
	 * @param manifest the manifest's address relative to the base, such as {@code @manifest}
	 * @return the layout
	 * @throws IllegalArgumentException when the name is blank or no relative address
	 */
	static @NotNull BundleLayout digests(@NotNull String manifest) {
		return new Digests(manifest);
	}

	/**
	 * Returns the manifest's address relative to the base.
	 *
	 * @return the address, such as {@code manifest.json}
	 */
	@NotNull String manifest();

	/**
	 * Returns a bundle's address relative to the base.
	 *
	 * @param product the product
	 * @param locale  the locale
	 * @param hash    the bundle's hash in the manifest
	 * @return the address, such as {@code intercept/en.json}
	 * @throws IOException when the layout needs the hash and it is no SHA-256 in lowercase hex
	 */
	@NotNull String bundle(@NotNull String product, @NotNull Locale locale, @NotNull String hash) throws IOException;

	/**
	 * Bundles by product and locale, the folder {@code semantica-cli bundle} writes.
	 */
	record Paths() implements BundleLayout {
		@Override
		public @NotNull String manifest() {
			return "manifest.json";
		}

		@Override
		public @NotNull String bundle(@NotNull String product, @NotNull Locale locale, @NotNull String hash) {
			return Manifest.path(product, locale);
		}
	}

	/**
	 * Bundles by digest, the manifest by a name.
	 *
	 * @param manifest the manifest's address relative to the base
	 */
	record Digests(@NotNull String manifest) implements BundleLayout {
		private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

		public Digests {
			Objects.requireNonNull(manifest, "manifest");
			if (manifest.isBlank() || manifest.startsWith("/") || URI.create(manifest).isAbsolute())
				throw new IllegalArgumentException("'" + manifest + "' is no address relative to the base");
		}

		@Override
		public @NotNull String bundle(@NotNull String product, @NotNull Locale locale, @NotNull String hash) throws IOException {
			if (!SHA_256.matcher(hash).matches()) throw new IOException("The manifest's hash '" + hash + "' is no SHA-256");
			return "sha256-" + hash + "/" + locale.toLanguageTag() + ".json";
		}
	}
}
