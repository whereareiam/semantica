package me.whereareiam.semantica.cli;

import me.whereareiam.configura.Configura;
import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.catalog.Layer;
import me.whereareiam.semantica.common.Semantica;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.format.adventure.v5.AdventureRenderer;
import me.whereareiam.semantica.source.file.FileSource;
import me.whereareiam.semantica.syntax.icu.IcuSyntax;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Loads a translations repository the way Semantica loads translations, with the checks of its namespaces' formats.
 * MiniMessage is judged by the tags of Adventure 5, the newest.
 */
final class TranslationsCheck {
	private final Path folder;
	private final RepositorySettings settings;

	TranslationsCheck(Path folder, RepositorySettings settings) {
		this.folder = folder;
		this.settings = settings;
	}

	Result run() {
		Semantica.Builder builder = Semantica.builder()
				.syntax(new IcuSyntax())
				.defaultLocale(settings.getDefaultLocale())
				.watch(false)
				.issues(issue -> {
				})
				.layer(Layer.of(FileSource.of(folder, Configura.builder().build())));

		settings.getNamespaces().forEach((name, namespace) -> {
			if ("adventure".equals(namespace.format()))
				builder.check(AdventureRenderer.of(namespace.policy()).check(key -> key.startsWith(name + ".")));
		});

		try (Semantica semantica = builder.build()) {
			Catalog catalog = semantica.getCatalog();
			List<Issue> issues = new ArrayList<>(catalog.getIssues());

			for (Locale locale : catalog.getLocales()) {
				for (Catalog.Entry entry : catalog.entries(locale).values()) {
					int dot = entry.key().indexOf('.');
					String namespace = dot < 0 ? entry.key() : entry.key().substring(0, dot);
					if (settings.getNamespaces().containsKey(namespace)) continue;

					issues.add(new Issue(Issue.Kind.SOURCE, entry.key(), locale, null, entry.origin(),
							"The namespace '" + namespace + "' is not listed in " + RepositorySettings.FILE));
				}
			}

			if (!catalog.getLocales().contains(settings.getDefaultLocale())) {
				issues.add(new Issue(Issue.Kind.SOURCE, null, settings.getDefaultLocale(), null, null,
						"The default locale has no folder"));
			}

			return new Result(catalog, issues);
		}
	}

	/**
	 * What the check found.
	 *
	 * @param catalog the loaded translations
	 * @param issues  every issue
	 */
	record Result(Catalog catalog, List<Issue> issues) {
		boolean failed() {
			return issues.stream().anyMatch(issue -> issue.getKind().getSeverity() == Issue.Severity.ERROR);
		}
	}
}
