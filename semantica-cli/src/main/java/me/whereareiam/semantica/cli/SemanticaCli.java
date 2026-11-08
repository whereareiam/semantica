package me.whereareiam.semantica.cli;

import me.whereareiam.semantica.KeyPrefix;
import me.whereareiam.semantica.catalog.Catalog;
import me.whereareiam.semantica.issue.Issue;
import me.whereareiam.semantica.source.http.Bundle;
import me.whereareiam.semantica.source.http.Manifest;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The command a translations repository's CI runs.
 *
 * <pre>
 * java -jar semantica-cli-all.jar check  [folder]
 * java -jar semantica-cli-all.jar bundle [folder] [output] [--revision value]
 * </pre>
 *
 * <p>{@code check} loads the folder as Semantica would, prints every issue, and fails when an issue is an error.
 * {@code bundle} checks first, then writes {@code <output>/<product>/<locale>.json} for each product of
 * {@code semantica.json} and {@code <output>/manifest.json}. The folder defaults to the current folder, the output
 * to {@code bundles}.</p>
 */
public final class SemanticaCli {
	private static final String USAGE = """
			Usage:
			  semantica check  [folder]
			  semantica bundle [folder] [output] [--revision value]
			""";

	private final PrintStream out;
	private final PrintStream err;
	private final Map<String, String> environment;

	SemanticaCli(PrintStream out, PrintStream err, Map<String, String> environment) {
		this.out = out;
		this.err = err;
		this.environment = environment;
	}

	/**
	 * Runs a command and exits with its status: 0 when it succeeded, 1 when translations have errors, 2 when it
	 * was used wrongly.
	 *
	 * @param arguments the command and its arguments
	 */
	public static void main(String[] arguments) {
		System.exit(new SemanticaCli(System.out, System.err, System.getenv()).run(arguments));
	}

	int run(String[] arguments) {
		List<String> positional = new ArrayList<>();
		String revision = null;
		for (int index = 0; index < arguments.length; index++) {
			if (arguments[index].equals("--revision") && index + 1 < arguments.length) revision = arguments[++index];
			else positional.add(arguments[index]);
		}

		if (positional.isEmpty()) {
			err.print(USAGE);
			return 2;
		}

		Path folder = Path.of(positional.size() > 1 ? positional.get(1) : ".");
		try {
			RepositorySettings settings = RepositorySettings.read(folder);
			return switch (positional.getFirst()) {
				case "check" -> check(folder, settings) ? 0 : 1;
				case "bundle" -> bundle(folder, settings, Path.of(positional.size() > 2 ? positional.get(2) : "bundles"), revision);
				default -> {
					err.print(USAGE);
					yield 2;
				}
			};
		} catch (RepositorySettings.SettingsException exception) {
			err.println("error: " + exception.getMessage());
			return 2;
		} catch (IOException exception) {
			err.println("error: " + exception.getMessage());
			return 2;
		}
	}

	private boolean check(Path folder, RepositorySettings settings) {
		TranslationsCheck.Result result = new TranslationsCheck(folder, settings).run();
		boolean annotate = "true".equals(environment.get("GITHUB_ACTIONS"));

		List<Issue> sorted = new ArrayList<>(result.issues());
		sorted.sort(Comparator.comparing((Issue issue) -> issue.getKind().getSeverity())
				.thenComparing(issue -> String.valueOf(issue.getOrigin()))
				.thenComparing(issue -> String.valueOf(issue.getKey())));

		for (Issue issue : sorted) {
			String severity = issue.getKind().getSeverity() == Issue.Severity.ERROR ? "error" : "warning";
			out.println(severity + ": " + issue);
			if (annotate) out.println(annotation(severity, folder, issue));
		}

		coverage(result.catalog(), settings.getDefaultLocale());

		long errors = sorted.stream().filter(issue -> issue.getKind().getSeverity() == Issue.Severity.ERROR).count();
		out.println(errors == 0
				? "Translations are fine (" + (sorted.size() - errors) + " warnings)."
				: errors + " errors, " + (sorted.size() - errors) + " warnings.");

		return !result.failed();
	}

	private void coverage(Catalog catalog, Locale defaultLocale) {
		int total = catalog.keys(defaultLocale).size();
		if (total == 0) return;

		Map<String, Integer> byLocale = new TreeMap<>();
		for (Locale locale : catalog.getLocales()) {
			if (locale.equals(defaultLocale) || locale.equals(Locale.ROOT)) continue;
			int translated = (int) catalog.keys(locale).stream().filter(catalog.keys(defaultLocale)::contains).count();
			byLocale.put(locale.toLanguageTag(), translated);
		}

		byLocale.forEach((locale, translated) ->
				out.println(locale + ": " + translated + "/" + total + " keys (" + (100 * translated / total) + "%)"));
	}

	private int bundle(Path folder, RepositorySettings settings, Path output, String revision) throws IOException {
		if (!check(folder, settings)) {
			err.println("error: not writing bundles while translations have errors");
			return 1;
		}

		Catalog catalog = new TranslationsCheck(folder, settings).run().catalog();
		if (!isEmptyOrBundles(output)) {
			err.println("error: " + output + " holds files other than bundles; choose an empty folder");
			return 2;
		}
		delete(output);

		Map<String, Map<Locale, String>> hashes = new LinkedHashMap<>();
		for (Map.Entry<String, List<KeyPrefix>> product : settings.getProducts().entrySet()) {
			Map<Locale, String> byLocale = new LinkedHashMap<>();
			for (Locale locale : catalog.getLocales()) {
				Map<String, String> messages = new TreeMap<>();
				catalog.entries(locale).values().forEach(entry -> {
					if (product.getValue().stream().anyMatch(prefix -> prefix.matches(entry.key())))
						messages.put(entry.key(), entry.source());
				});
				if (messages.isEmpty()) continue;

				byte[] bytes = Bundle.write(messages);
				Path target = output.resolve(Manifest.path(product.getKey(), locale));
				Files.createDirectories(target.getParent());
				Files.write(target, bytes);
				byLocale.put(locale, Bundle.hash(bytes));
			}
			hashes.put(product.getKey(), byLocale);
		}

		String resolved = revision != null ? revision : environment.getOrDefault("GITHUB_SHA", contentRevision(hashes));
		Files.createDirectories(output);
		Files.write(output.resolve("manifest.json"), new Manifest(resolved, settings.getDefaultLocale(), hashes).write());

		out.println("Wrote bundles of " + hashes.size() + " products to " + output + " (revision " + resolved + ").");
		return 0;
	}

	private static String contentRevision(Map<String, Map<Locale, String>> hashes) {
		StringBuilder all = new StringBuilder();
		new TreeMap<>(hashes).forEach((product, byLocale) -> byLocale.forEach((locale, hash) ->
				all.append(product).append('/').append(locale.toLanguageTag()).append('=').append(hash).append('\n')));

		return Bundle.hash(all.toString().getBytes(StandardCharsets.UTF_8)).substring(0, 12);
	}

	private static String annotation(String severity, Path folder, Issue issue) {
		StringBuilder line = new StringBuilder("::").append(severity).append(' ');
		if (issue.getOrigin() != null) line.append("file=").append(folder.resolve(issue.getOrigin()).normalize()).append(',');
		line.append("title=").append(issue.getKind().getId()).append("::");

		String message = (issue.getLocale() != null ? issue.getLocale().toLanguageTag() + " " : "")
				+ (issue.getKey() != null ? issue.getKey() + ": " : "")
				+ issue.getDetail();
		line.append(message.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A"));

		return line.toString();
	}

	private static boolean isEmptyOrBundles(Path output) throws IOException {
		if (!Files.exists(output)) return true;
		if (Files.exists(output.resolve("manifest.json"))) return true;

		try (Stream<Path> children = Files.list(output)) {
			return children.findAny().isEmpty();
		}
	}

	private static void delete(Path path) throws IOException {
		if (!Files.exists(path)) return;

		try (Stream<Path> walk = Files.walk(path)) {
			for (Path each : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(each);
		}
	}
}
