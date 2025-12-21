package me.whereareiam.semantica.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.semantica.model.translation.TranslationSource;
import me.whereareiam.semantica.model.translation.entry.TemplateEntry;

import java.util.Collections;
import java.util.Map;

/**
 * Container for all translation data provided by a TranslationProvider.
 * <p>
 * Contains both localized entries (varying by locale) and template entries (non-localized).
 *
 * @param <L> the locale type
 */
@Getter
@RequiredArgsConstructor
public class ProviderResult<L> {
    private final Map<L, Map<String, TranslationSource>> localized;
    private final Map<String, TemplateEntry> templates;

    /**
     * Create ProviderResult with only localized entries.
     *
     * @param localized the localized entries
     * @param <L> the locale type
     * @return ProviderResult instance
     */
    public static <L> ProviderResult<L> localizedOnly(Map<L, Map<String, TranslationSource>> localized) {
        return new ProviderResult<>(localized, Collections.emptyMap());
    }

    /**
     * Create empty ProviderResult.
     *
     * @param <L> the locale type
     * @return empty ProviderResult instance
     */
    public static <L> ProviderResult<L> empty() {
        return new ProviderResult<>(Collections.emptyMap(), Collections.emptyMap());
    }
}
