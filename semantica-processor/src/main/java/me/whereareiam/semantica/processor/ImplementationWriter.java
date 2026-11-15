package me.whereareiam.semantica.processor;

import me.whereareiam.semantica.Message;
import me.whereareiam.semantica.processor.MessageGroup.GroupMethod;
import me.whereareiam.semantica.processor.MessageGroup.Member;
import me.whereareiam.semantica.processor.MessageGroup.MessageMethod;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * Writes the source of a {@code @Messages} interface's implementation: {@code Generated<Interface>} beside it, whose
 * methods return messages with constant keys and whose groups are classes nested in it, made once.
 */
final class ImplementationWriter {
	private static final String MESSAGE = Message.class.getName();

	private final List<String> groups = new ArrayList<>();
	private int nextGroup;

	/**
	 * Returns the simple name of the implementation of an interface.
	 *
	 * @param type the interface
	 * @return {@code Generated} and the interface's simple name
	 */
	static String name(TypeElement type) {
		return "Generated" + type.getSimpleName();
	}

	/**
	 * Writes the implementation of a {@code @Messages} interface.
	 *
	 * @param root        the interface as read
	 * @param packageName the interface's package, empty for none
	 * @return the source of the class
	 */
	String write(MessageGroup root, String packageName) {
		String name = name(root.getType());
		StringBuilder source = new StringBuilder();
		if (!packageName.isEmpty()) source.append("package ").append(packageName).append(";\n\n");

		source.append("/**\n * The messages of {@link ").append(root.getType().getQualifiedName()).append("}, keyed under {@code ")
				.append(root.getPrefix()).append("}. Written by the Semantica processor; do not edit.\n */\n")
				.append("@javax.annotation.processing.Generated(\"").append(MessagesProcessor.class.getName()).append("\")\n")
				.append("public final class ").append(name).append(" implements ").append(root.getType().getQualifiedName()).append(" {\n");
		body(source, root, "\t");
		for (String group : groups) source.append('\n').append(group);

		return source.append("}\n").toString();
	}

	private void body(StringBuilder source, MessageGroup group, String indent) {
		List<String> methods = new ArrayList<>();
		for (Member member : group.getMethods()) {
			switch (member) {
				case MessageMethod message -> methods.add(message(message, indent));
				case GroupMethod nested -> {
					String className = group(nested.group());
					source.append(indent).append("private final ").append(nested.group().getType().getQualifiedName()).append(' ')
							.append(nested.method().getSimpleName()).append(" = new ").append(className).append("();\n");
					methods.add(groupGetter(nested, indent));
				}
			}
		}

		boolean fields = group.getMethods().stream().anyMatch(GroupMethod.class::isInstance);
		if (fields) source.append('\n');
		source.append(String.join("\n", methods));
	}

	private String group(MessageGroup group) {
		String className = "Group" + nextGroup++;
		StringBuilder source = new StringBuilder();
		source.append("\t/**\n\t * The messages keyed under {@code ").append(group.getPrefix()).append("}.\n\t */\n")
				.append("\tprivate static final class ").append(className).append(" implements ")
				.append(group.getType().getQualifiedName()).append(" {\n");

		int index = groups.size();
		groups.add("");
		body(source, group, "\t\t");
		groups.set(index, source.append("\t}\n").toString());

		return className;
	}

	private static String groupGetter(GroupMethod nested, String indent) {
		String name = nested.method().getSimpleName().toString();
		return indent + "@Override\n"
				+ indent + "public " + nested.group().getType().getQualifiedName() + " " + name + "() {\n"
				+ indent + "\treturn " + name + ";\n"
				+ indent + "}\n";
	}

	private static String message(MessageMethod message, String indent) {
		ExecutableElement method = message.method();
		StringBuilder body = new StringBuilder(indent).append("\treturn ").append(MESSAGE).append(".of(");
		if (message.keyed() == null) body.append(literal(message.key()));
		else body.append(literal(message.key() + ".")).append(" + ").append(segment(message.keyed()));
		body.append(')');

		for (VariableElement parameter : method.getParameters()) {
			if (parameter.equals(message.keyed())) continue;

			String name = parameter.getSimpleName().toString();
			body.append("\n").append(indent).append("\t\t\t.with(").append(literal(name)).append(", ").append(name).append(')');
		}

		return indent + "@Override\n"
				+ indent + "public " + MESSAGE + " " + method.getSimpleName() + "(" + parameters(method) + ") {\n"
				+ body + ";\n"
				+ indent + "}\n";
	}

	private static String segment(VariableElement parameter) {
		String name = parameter.getSimpleName().toString();
		boolean text = parameter.asType() instanceof DeclaredType declared
				&& ((TypeElement) declared.asElement()).getQualifiedName().contentEquals(String.class.getName());

		return text ? name : name + ".segment()";
	}

	private static String parameters(ExecutableElement method) {
		List<? extends VariableElement> parameters = method.getParameters();
		List<String> written = new ArrayList<>();
		for (int index = 0; index < parameters.size(); index++) {
			VariableElement parameter = parameters.get(index);
			boolean varargs = method.isVarArgs() && index == parameters.size() - 1;
			written.add(type(parameter.asType(), varargs) + " " + parameter.getSimpleName());
		}

		return String.join(", ", written);
	}

	private static String type(TypeMirror type, boolean varargs) {
		if (!varargs) return type.toString();

		return ((ArrayType) type).getComponentType().toString() + "...";
	}

	/**
	 * Writes a string as a Java literal.
	 */
	static String literal(String text) {
		StringBuilder literal = new StringBuilder("\"");
		for (char character : text.toCharArray()) {
			switch (character) {
				case '"' -> literal.append("\\\"");
				case '\\' -> literal.append("\\\\");
				case '\n' -> literal.append("\\n");
				default -> {
					if (character < 0x20) literal.append(String.format("\\u%04x", (int) character));
					else literal.append(character);
				}
			}
		}

		return literal.append('"').toString();
	}
}
