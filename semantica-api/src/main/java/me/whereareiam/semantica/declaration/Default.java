package me.whereareiam.semantica.declaration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The text of a message in the default locale, written in the message syntax: its arguments are the method's
 * parameters, by name.
 *
 * <pre>{@code
 * @Default("<white>You must wait <gray>{time, number} seconds</gray>.")
 * Message cooldown(double time);
 *
 * @Default({"<gold>Socialismus <gray>{version}", "<gray>Reloaded in {millis, number} ms."})
 * Message reloaded(String version, long millis);
 * }</pre>
 *
 * <p>Every method of a {@link Messages} interface that returns a message has one, except a method with a
 * {@link Segment}, whose texts come from whoever defines the segments.</p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Default {
	/**
	 * Returns the text: one string, or several for a message of several lines.
	 *
	 * @return the lines of the text, joined by line breaks
	 */
	String[] value();
}
