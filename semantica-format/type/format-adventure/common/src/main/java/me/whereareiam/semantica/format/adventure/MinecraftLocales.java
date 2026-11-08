package me.whereareiam.semantica.format.adventure;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.IllformedLocaleException;
import java.util.Locale;

/**
 * Converts between Minecraft's locale codes, such as {@code en_us}, and {@link Locale}s, such as {@code en-US}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MinecraftLocales {
	/**
	 * Reads a Minecraft locale code.
	 *
	 * @param code the code, such as {@code en_us} or {@code pt_br}
	 * @return the locale, such as {@code en-US}; {@link Locale#ROOT} when the code is no locale
	 */
	public static @NotNull Locale toLocale(@NotNull String code) {
		String[] parts = code.trim().split("[_-]", 2);
		try {
			Locale.Builder builder = new Locale.Builder().setLanguage(parts[0].toLowerCase(Locale.ROOT));
			if (parts.length > 1) builder.setRegion(parts[1].toUpperCase(Locale.ROOT));

			return builder.build();
		} catch (IllformedLocaleException exception) {
			return Locale.ROOT;
		}
	}

	/**
	 * Writes a locale as a Minecraft locale code.
	 *
	 * @param locale the locale, such as {@code en-US}
	 * @return the code, such as {@code en_us}; the language alone when the locale has no region
	 */
	public static @NotNull String toCode(@NotNull Locale locale) {
		String language = locale.getLanguage();
		String region = locale.getCountry();

		return region.isEmpty() ? language : language + "_" + region.toLowerCase(Locale.ROOT);
	}
}
