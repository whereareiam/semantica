package me.whereareiam.semantica.processor;

import me.whereareiam.semantica.KeyPrefix;
import me.whereareiam.semantica.declaration.Messages;
import me.whereareiam.semantica.message.ParsedMessage;
import me.whereareiam.semantica.processor.MessageGroup.GroupMethod;
import me.whereareiam.semantica.processor.MessageGroup.MessageMethod;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Reads the {@link Messages} interfaces of a compilation, checks their defaults and writes their implementations and
 * default-locale files.
 *
 * <p>The option {@value #LOCALE_OPTION} names the default locale, the folder the files are written to, {@code en}
 * unless set: {@code -Asemantica.locale=en}. A reference in a default to a key under one of the compilation's own
 * prefixes must name a key one of its methods declares; references to other keys are checked when the catalog
 * loads.</p>
 */
public final class MessagesProcessor extends AbstractProcessor {
	/**
	 * The option naming the default locale.
	 */
	public static final String LOCALE_OPTION = "semantica.locale";

	private static final String FOLDER = "META-INF/semantica/";

	private final Map<String, TypeElement> prefixes = new HashMap<>();
	private final Set<String> keys = new HashSet<>();
	private final Set<String> openKeys = new HashSet<>();
	private final List<Referencing> references = new ArrayList<>();
	private Report report;
	private String locale;

	@Override
	public synchronized void init(ProcessingEnvironment environment) {
		super.init(environment);
		this.report = new Report(environment.getMessager());
		this.locale = environment.getOptions().getOrDefault(LOCALE_OPTION, "en");
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(Messages.class.getName());
	}

	@Override
	public Set<String> getSupportedOptions() {
		return Set.of(LOCALE_OPTION);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
		for (Element element : round.getElementsAnnotatedWith(Messages.class)) root(element);
		if (round.processingOver()) checkReferences();

		return true;
	}

	private void root(Element element) {
		if (element.getKind() != ElementKind.INTERFACE) {
			report.error(element, "@Messages declares an interface");
			return;
		}

		TypeElement type = (TypeElement) element;
		String prefix = type.getAnnotation(Messages.class).value();
		if (!fits(type, prefix)) return;

		int errors = report.errors();
		MessageGroup root = new GroupReader(processingEnv, report).read(type, prefix);
		check(root);
		if (report.errors() == errors) write(root);
	}

	private boolean fits(TypeElement type, String prefix) {
		try {
			KeyPrefix.of(prefix);
		} catch (IllegalArgumentException exception) {
			report.error(type, "'" + prefix + "' is no key prefix; write segments of letters, digits, _ and - separated by dots");
			return false;
		}

		if (!type.getTypeParameters().isEmpty() || type.getModifiers().contains(Modifier.PRIVATE)) {
			report.error(type, "A @Messages interface is not private and takes no type parameters");
			return false;
		}

		TypeElement previous = prefixes.putIfAbsent(prefix, type);
		if (previous == null) return true;

		report.error(type, "The prefix " + prefix + " is also the prefix of " + previous.getQualifiedName() + "; each interface has its own");
		return false;
	}

	private void check(MessageGroup group) {
		DefaultCheck defaults = new DefaultCheck(processingEnv, report);
		for (MessageGroup.Member member : group.getMethods()) {
			switch (member) {
				case MessageMethod message when message.keyed() != null -> openKeys.add(message.key());
				case MessageMethod message -> {
					keys.add(message.key());
					ParsedMessage parsed = defaults.check(message);
					if (parsed != null) references.add(new Referencing(message, parsed.references()));
				}
				case GroupMethod nested -> check(nested.group());
			}
		}
	}

	private void checkReferences() {
		for (Referencing referencing : references) {
			for (String key : referencing.keys()) {
				if (!own(key) || keys.contains(key) || open(key)) continue;

				report.error(referencing.method().method(), "The default of " + referencing.method().key() + " references {@" + key
						+ "}, which no method of " + prefix(key) + " declares");
			}
		}
	}

	private boolean own(String key) {
		return prefix(key) != null;
	}

	private String prefix(String key) {
		return prefixes.keySet().stream()
				.filter(prefix -> KeyPrefix.of(prefix).matches(key))
				.findFirst()
				.orElse(null);
	}

	private boolean open(String key) {
		return openKeys.stream().anyMatch(open -> key.startsWith(open + "."));
	}

	private void write(MessageGroup root) {
		TypeElement type = root.getType();
		String packageName = processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
		String className = packageName.isEmpty()
				? ImplementationWriter.name(type)
				: packageName + "." + ImplementationWriter.name(type);
		String file = FOLDER + locale + "/" + DefaultsWriter.path(root.getPrefix());

		try {
			try (Writer writer = processingEnv.getFiler().createSourceFile(className, type).openWriter()) {
				writer.write(new ImplementationWriter().write(root, packageName));
			}

			FileObject resource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", file, type);
			try (Writer writer = resource.openWriter()) {
				writer.write(DefaultsWriter.write(root));
			}
		} catch (IOException exception) {
			report.error(type, "Cannot write " + className + " or " + file + ": " + exception.getMessage());
		}
	}

	/**
	 * The keys a message's default references.
	 *
	 * @param method the message
	 * @param keys   the keys
	 */
	private record Referencing(MessageMethod method, Set<String> keys) {
	}
}
