package me.whereareiam.semantica.locale;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocaleChainTest {
	private static Locale tag(String tag) {
		return Locale.forLanguageTag(tag);
	}

	@Test
	void endsWithTheBaseLanguageAndTheDefault() {
		LocaleChain chain = LocaleChain.builder(Locale.ENGLISH).build();

		assertEquals(List.of(tag("de-AT"), tag("de"), tag("en")), chain.resolve(tag("de-AT")));
		assertEquals(List.of(tag("en-US"), tag("en")), chain.resolve(tag("en-US")));
	}

	@Test
	void triesConfiguredFallbacksBeforeTheBaseLanguage() {
		LocaleChain chain = LocaleChain.builder(Locale.ENGLISH)
				.fallback(tag("pt-BR"), tag("pt-PT"))
				.build();

		assertEquals(List.of(tag("pt-BR"), tag("pt-PT"), tag("pt"), tag("en")), chain.resolve(tag("pt-BR")));
	}

	@Test
	void chainsSeveralRequestedLocalesInOrder() {
		LocaleChain chain = LocaleChain.builder(Locale.ENGLISH).build();

		assertEquals(List.of(tag("uk"), tag("ru-RU"), tag("ru"), tag("en")), chain.resolve(List.of(tag("uk"), tag("ru-RU"))));
	}

	@Test
	void removesScriptAfterRegion() {
		LocaleChain chain = LocaleChain.builder(Locale.ENGLISH).build();

		assertEquals(List.of(tag("sr-Latn-RS"), tag("sr-Latn"), tag("sr"), tag("en")), chain.resolve(tag("sr-Latn-RS")));
	}

	@Test
	void survivesFallbackLoops() {
		LocaleChain chain = LocaleChain.builder(Locale.ENGLISH)
				.fallback(tag("nb"), tag("nn"))
				.fallback(tag("nn"), tag("nb"))
				.build();

		assertEquals(List.of(tag("nb"), tag("nn"), tag("en")), chain.resolve(tag("nb")));
	}
}
