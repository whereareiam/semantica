package me.whereareiam.semantica.processor;

import me.whereareiam.semantica.Message;
import me.whereareiam.semantica.declaration.Default;
import me.whereareiam.semantica.declaration.KeySegment;
import me.whereareiam.semantica.declaration.Segment;
import me.whereareiam.semantica.processor.MessageGroup.GroupMethod;
import me.whereareiam.semantica.processor.MessageGroup.Member;
import me.whereareiam.semantica.processor.MessageGroup.MessageMethod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;

/**
 * Reads a {@code @Messages} interface into a {@link MessageGroup}, reporting every method that does not fit: a return
 * type that is neither a message nor a group, a missing or misplaced {@code @Default}, a segment parameter of another
 * type, two methods of one key, a group that contains itself.
 */
final class GroupReader {
	private final Report report;
	private final Types types;
	private final TypeMirror message;
	private final TypeMirror string;
	private final TypeMirror keySegment;
	private final Deque<TypeElement> path = new ArrayDeque<>();

	GroupReader(ProcessingEnvironment environment, Report report) {
		this.report = report;
		this.types = environment.getTypeUtils();
		this.message = environment.getElementUtils().getTypeElement(Message.class.getName()).asType();
		this.string = environment.getElementUtils().getTypeElement(String.class.getName()).asType();
		this.keySegment = environment.getElementUtils().getTypeElement(KeySegment.class.getName()).asType();
	}

	/**
	 * Reads an interface and every group below it.
	 *
	 * @param type   the interface
	 * @param prefix the prefix of its keys
	 * @return the group; methods with errors are left out
	 */
	MessageGroup read(TypeElement type, String prefix) {
		path.push(type);
		List<Member> members = new ArrayList<>();
		Map<String, ExecutableElement> segments = new HashMap<>();

		for (ExecutableElement method : methods(type)) {
			String segment = Keys.segment(method.getSimpleName().toString());
			ExecutableElement previous = segments.putIfAbsent(segment, method);
			if (previous != null) {
				report.error(method, "Gives the key " + prefix + "." + segment + " as " + previous.getSimpleName() + " does; name one of them otherwise");
				continue;
			}

			member(method, prefix, segment).ifPresent(members::add);
		}

		path.pop();
		return new MessageGroup(type, prefix, List.copyOf(members));
	}

	private Optional<Member> member(ExecutableElement method, String prefix, String segment) {
		if (!method.getTypeParameters().isEmpty()) {
			report.error(method, "A message method takes no type parameters");
			return Optional.empty();
		}

		TypeMirror returned = method.getReturnType();
		if (types.isSameType(returned, message)) return message(method, prefix, segment);
		if (group(returned) != null) return group(method, prefix, segment);

		report.error(method, "Returns neither a Message nor an interface of more messages");
		return Optional.empty();
	}

	private Optional<Member> message(ExecutableElement method, String prefix, String segment) {
		String key = prefix + "." + segment;
		List<? extends VariableElement> keyed = method.getParameters().stream()
				.filter(parameter -> parameter.getAnnotation(Segment.class) != null)
				.toList();
		Default declared = method.getAnnotation(Default.class);

		if (keyed.size() > 1) {
			report.error(method, "Only one parameter may be the key's segment");
			return Optional.empty();
		}

		if (keyed.isEmpty()) return withDefault(method, segment, key, declared);
		return withSegment(method, segment, key, keyed.getFirst(), declared);
	}

	private Optional<Member> withDefault(ExecutableElement method, String segment, String key, Default declared) {
		if (declared == null) {
			report.error(method, "Has no @Default; write the text of " + key + " in the default locale");
			return Optional.empty();
		}

		if (declared.value().length == 0) {
			report.error(method, "@Default holds no line; write the text of " + key);
			return Optional.empty();
		}

		return Optional.of(new MessageMethod(method, segment, key, List.of(declared.value()), null));
	}

	private Optional<Member> withSegment(ExecutableElement method, String segment, String key, VariableElement keyed, Default declared) {
		if (declared != null) {
			report.error(method, "A method with a @Segment parameter has no @Default; the texts of " + key + ".* come from whoever defines the segments");
			return Optional.empty();
		}

		TypeMirror type = keyed.asType();
		if (!types.isSameType(type, string) && !types.isAssignable(type, keySegment)) {
			report.error(keyed, "A @Segment parameter is a String or a KeySegment");
			return Optional.empty();
		}

		return Optional.of(new MessageMethod(method, segment, key, List.of(), keyed));
	}

	private Optional<Member> group(ExecutableElement method, String prefix, String segment) {
		TypeElement type = group(method.getReturnType());
		if (!method.getParameters().isEmpty()) {
			report.error(method, "A method that returns a group takes no parameters");
			return Optional.empty();
		}

		if (path.contains(type)) {
			report.error(method, "The group " + type.getSimpleName() + " contains itself");
			return Optional.empty();
		}

		if (method.getAnnotation(Default.class) != null) {
			report.error(method, "A group has no @Default; its methods have theirs");
			return Optional.empty();
		}

		return Optional.of(new GroupMethod(method, segment, read(type, prefix + "." + segment)));
	}

	private TypeElement group(TypeMirror type) {
		if (type.getKind() != TypeKind.DECLARED) return null;

		DeclaredType declared = (DeclaredType) type;
		TypeElement element = (TypeElement) declared.asElement();
		boolean plain = declared.getTypeArguments().isEmpty();

		return element.getKind() == ElementKind.INTERFACE && plain ? element : null;
	}

	/**
	 * Returns the abstract methods of an interface, its own first in the order written, then those of the interfaces
	 * it extends that it does not declare again.
	 */
	private List<ExecutableElement> methods(TypeElement type) {
		List<ExecutableElement> methods = new ArrayList<>();
		collect(type, methods);

		return methods;
	}

	private void collect(TypeElement type, List<ExecutableElement> methods) {
		for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
			if (!method.getModifiers().contains(Modifier.ABSTRACT)) continue;
			if (methods.stream().noneMatch(known -> sameSignature(known, method))) methods.add(method);
		}

		for (TypeMirror parent : type.getInterfaces())
			collect((TypeElement) types.asElement(parent), methods);
	}

	private boolean sameSignature(ExecutableElement left, ExecutableElement right) {
		if (!left.getSimpleName().equals(right.getSimpleName())) return false;
		if (left.getParameters().size() != right.getParameters().size()) return false;

		for (int index = 0; index < left.getParameters().size(); index++) {
			TypeMirror leftType = types.erasure(left.getParameters().get(index).asType());
			TypeMirror rightType = types.erasure(right.getParameters().get(index).asType());
			if (!types.isSameType(leftType, rightType)) return false;
		}

		return true;
	}
}
