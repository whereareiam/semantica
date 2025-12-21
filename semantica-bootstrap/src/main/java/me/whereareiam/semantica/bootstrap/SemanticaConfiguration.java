package me.whereareiam.semantica.bootstrap;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import me.whereareiam.semantica.TranslationLocale;
import me.whereareiam.semantica.SemanticaLogger;
import me.whereareiam.semantica.model.SemanticLocale;
import me.whereareiam.semantica.model.TagConfiguration;
import me.whereareiam.semantica.service.FallbackStrategy;
import me.whereareiam.semantica.service.LocaleConverter;
import me.whereareiam.semantica.service.TranslationProcessor;
import me.whereareiam.semantica.service.TranslationProvider;

import java.util.List;
import java.util.Locale;

@Getter
@Builder(toBuilder = true)
public class SemanticaConfiguration<L> {
	@Builder.Default
	private final TranslationLocale defaultLocale = SemanticLocale.wrap(Locale.getDefault());

	@Builder.Default
	private final CacheSettings cache = CacheSettings.defaults();

	@Builder.Default
	private final TagConfiguration tagConfiguration = TagConfiguration.defaults();

	@Singular("processor")
	private final List<TranslationProcessor> customProcessors;

	private final LocaleConverter<L> localeConverter;

	private final TranslationProvider<L> translationProvider;

	@Builder.Default
	private final FallbackStrategy fallbackStrategy = new DefaultFallbackStrategy();

	@Builder.Default
	private final SemanticaLogger logger = new ConsoleSemanticaLogger();

	public static <L> SemanticaConfigurationBuilder<L> builderWithLocale(Locale locale) {
		return SemanticaConfiguration.<L>builder().defaultLocale(SemanticLocale.wrap(locale));
	}

	@Getter
	@Builder(toBuilder = true)
	public static class CacheSettings {
		@Builder.Default
		private final int semiStaticCacheSize = 1024;
		@Builder.Default
		private final int dynamicCacheSize = 512;
		@Builder.Default
		private final long semiStaticExpireMinutes = 30;
		@Builder.Default
		private final long dynamicExpireMinutes = 5;
		@Builder.Default
		private final boolean preRenderStatic = true;

		public static CacheSettings defaults() {
			return CacheSettings.builder().build();
		}
	}
}

