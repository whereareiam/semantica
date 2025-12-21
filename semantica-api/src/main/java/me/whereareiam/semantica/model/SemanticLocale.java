package me.whereareiam.semantica.model;

import lombok.Getter;
import me.whereareiam.semantica.TranslationLocale;

import java.util.Locale;

/**
 * Adapter that wraps a {@link java.util.Locale} as a {@link TranslationLocale}.
 * <p>
 * Use {@link #wrap(Locale)} to convert platform locales into Semantica’s abstraction.
 */
@Getter
@SuppressWarnings("unused")
public final class SemanticLocale implements TranslationLocale {
	private final Locale delegate;

	private SemanticLocale(Locale delegate) {
		this.delegate = delegate == null ? Locale.getDefault() : delegate;
	}

	public static SemanticLocale wrap(Locale locale) {
		return new SemanticLocale(locale);
	}

	public Locale unwrap() {
		return delegate;
	}

	@Override
	public String getLanguage() {
		return delegate.getLanguage();
	}

	@Override
	public String getCountry() {
		return delegate.getCountry();
	}

	@Override
	public String getVariant() {
		return delegate.getVariant();
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SemanticLocale that)) return false;
		return delegate.equals(that.delegate);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}
}

