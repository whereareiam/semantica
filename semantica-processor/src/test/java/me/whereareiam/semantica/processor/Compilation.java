package me.whereareiam.semantica.processor;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Compiles sources with the processor into a folder, keeping what the compiler reported.
 */
final class Compilation {
	private final Path output;
	private final boolean success;
	private final List<Diagnostic<? extends JavaFileObject>> diagnostics;

	private Compilation(Path output, boolean success, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
		this.output = output;
		this.success = success;
		this.diagnostics = diagnostics;
	}

	static Compilation of(Path output, Map<String, String> sources, String... options) throws IOException {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();

		try (StandardJavaFileManager files = compiler.getStandardFileManager(collector, Locale.ROOT, StandardCharsets.UTF_8)) {
			files.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(output));
			files.setLocationFromPaths(StandardLocation.SOURCE_OUTPUT, List.of(output));

			List<String> arguments = new ArrayList<>(List.of("-classpath", System.getProperty("java.class.path"), "-proc:full"));
			arguments.addAll(List.of(options));
			List<JavaFileObject> units = sources.entrySet().stream()
					.map(source -> (JavaFileObject) new Source(source.getKey(), source.getValue()))
					.toList();

			JavaCompiler.CompilationTask task = compiler.getTask(null, files, collector, arguments, null, units);
			task.setProcessors(List.of(new MessagesProcessor()));
			boolean success = task.call();

			return new Compilation(output, success, collector.getDiagnostics());
		}
	}

	boolean success() {
		return success;
	}

	String file(String path) throws IOException {
		return Files.readString(output.resolve(path), StandardCharsets.UTF_8);
	}

	boolean exists(String path) {
		return Files.exists(output.resolve(path));
	}

	String errors() {
		return messages(Diagnostic.Kind.ERROR);
	}

	String warnings() {
		return messages(Diagnostic.Kind.WARNING);
	}

	private String messages(Diagnostic.Kind kind) {
		return diagnostics.stream()
				.filter(diagnostic -> diagnostic.getKind() == kind)
				.map(diagnostic -> diagnostic.getMessage(Locale.ROOT))
				.collect(Collectors.joining("\n"));
	}

	private static final class Source extends SimpleJavaFileObject {
		private final String code;

		private Source(String className, String code) {
			super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.code = code;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}
	}
}
