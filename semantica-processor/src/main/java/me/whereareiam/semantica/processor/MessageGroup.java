package me.whereareiam.semantica.processor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * A {@code @Messages} interface or a group inside it, as the processor read it: the prefix of its keys and its
 * methods, in the order they are declared.
 */
@Getter
@RequiredArgsConstructor
final class MessageGroup {
	/**
	 * The interface.
	 */
	private final TypeElement type;

	/**
	 * The key segments before its methods' names, such as {@code commands.reload}.
	 */
	private final String prefix;

	/**
	 * Its methods.
	 */
	private final List<Member> methods;

	/**
	 * A method of a group.
	 */
	sealed interface Member permits MessageMethod, GroupMethod {
		/**
		 * Returns the method.
		 *
		 * @return the method
		 */
		ExecutableElement method();

		/**
		 * Returns the key segment of the method, its name in kebab case.
		 *
		 * @return the segment, such as {@code staff-joined}
		 */
		String segment();
	}

	/**
	 * A method that returns a message.
	 *
	 * @param method  the method
	 * @param segment its key segment
	 * @param key     the key of its message; for a method with a segment parameter, the key before that segment
	 * @param lines   the lines of its default, empty for a method with a segment parameter
	 * @param keyed   the parameter that is the key's last segment, or {@code null}
	 */
	record MessageMethod(
			ExecutableElement method,
			String segment,
			String key,
			List<String> lines,
			@Nullable VariableElement keyed
	) implements Member {
	}

	/**
	 * A method that returns a group.
	 *
	 * @param method  the method
	 * @param segment its key segment
	 * @param group   the group
	 */
	record GroupMethod(ExecutableElement method, String segment, MessageGroup group) implements Member {
	}
}
