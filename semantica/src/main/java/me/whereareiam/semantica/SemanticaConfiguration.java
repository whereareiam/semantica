package me.whereareiam.semantica;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import me.whereareiam.semantica.locale.LocaleParser;
import me.whereareiam.semantica.model.SemanticLocale;
import me.whereareiam.semantica.predefined.DefaultFallbackStrategy;
import me.whereareiam.semantica.predefined.DefaultSemanticaLogger;
import me.whereareiam.semantica.translation.base.TranslationLocale;
import me.whereareiam.semantica.translation.base.TranslationProcessor;
import me.whereareiam.semantica.translation.base.TranslationProvider;

import java.util.List;
import java.util.Locale;

@Getter
@Builder(toBuilder = true)
@SuppressWarnings("unused")
public class SemanticaConfiguration<L> {
	@Builder.Default
	private final TranslationLocale defaultLocale = SemanticLocale.wrap(Locale.getDefault());

	@Builder.Default
	private final PerformanceSettings performance = PerformanceSettings.defaults();

	@Builder.Default
	private final TagConfiguration tagConfiguration = TagConfiguration.defaults();

	@Singular("processor")
	private final List<TranslationProcessor> customProcessors;

	private final LocaleParser<L> localeParser;

	private final TranslationProvider<L> translationProvider;

	@Builder.Default
	private final FallbackStrategy fallbackStrategy = new DefaultFallbackStrategy();

	@Builder.Default
	private final SemanticaLogger logger = new DefaultSemanticaLogger();

	public static <L> SemanticaConfigurationBuilder<L> builderWithLocale(Locale locale) {
		return SemanticaConfiguration.<L>builder().defaultLocale(SemanticLocale.wrap(locale));
	}

	@Getter
	@Builder(toBuilder = true)
	public static class PerformanceSettings {
		@Builder.Default
		private final CacheSettings cache = CacheSettings.defaults();
		
		@Builder.Default
		private final boolean prerenderStatic = true;
		
		@Builder.Default
		private final boolean buildDependencyGraph = true;
		
		@Builder.Default
		private final boolean logTimings = false;

		public static PerformanceSettings defaults() {
			return PerformanceSettings.builder().build();
		}

		@Getter
		@Builder(toBuilder = true)
		public static class CacheSettings {
			@Builder.Default
			private final boolean enabled = true;
			
			@Builder.Default
			private final int semiStaticSize = 1024;
			
			@Builder.Default
			private final int dynamicSize = 512;
			
			@Builder.Default
			private final int semiStaticExpireMinutes = 30;
			
			@Builder.Default
			private final int dynamicExpireMinutes = 5;

			public static CacheSettings defaults() {
				return CacheSettings.builder().build();
			}
		}
	}
}

