package me.whereareiam.semantica.format.adventure.v5;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.whereareiam.semantica.Message;
import me.whereareiam.semantica.Value;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * Turns values into the JDK and Adventure types that pass between copies of Semantica, and those types back into
 * values. Adventure's translation arguments and a {@link TextArgument} of another copy hand over a value only as
 * such a type; this is the one place it gets its kind.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ValueExchange {
	/**
	 * Returns the type a value passes between copies as.
	 *
	 * @param value the value
	 * @return a string, a number, a date or time, a boolean, an enum constant, a message, a component, or the joined
	 * or linked value itself, which another copy shows as its text
	 */
	static @NotNull Object write(@NotNull Value value) {
		return switch (value) {
			case Value.Text text -> text.value();
			case Value.Number number -> number.value();
			case Value.Time time -> time.value();
			case Value.Flag flag -> flag.value();
			case Value.Choice choice -> choice.value();
			case Value.Nested nested -> nested.value();
			case Value.Joined joined -> joined;
			case Value.Linked linked -> linked;
			case ComponentValue component -> component.getComponent();
			case Value.Rendered rendered -> rendered.plain();
		};
	}

	/**
	 * Returns the value a passed type stands for. An enum constant that is also a date field, such as a day of the
	 * week, is a choice; a type of no kind, such as a message of another copy, is shown as its text.
	 *
	 * @param passed the type as handed over
	 * @return the value
	 */
	static @NotNull Value read(@NotNull Object passed) {
		return switch (passed) {
			case Value value -> value;
			case String text -> Value.text(text);
			case Number number -> Value.number(number);
			case Boolean flag -> Value.flag(flag);
			case Enum<?> choice -> Value.choice(choice);
			case TemporalAccessor time -> Value.time(time);
			case Date date -> Value.time(date);
			case Message message -> Value.nested(message);
			case ComponentLike component -> ComponentValue.of(component);
			default -> Value.text(String.valueOf(passed));
		};
	}
}
