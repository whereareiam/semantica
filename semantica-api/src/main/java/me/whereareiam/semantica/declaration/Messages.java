package me.whereareiam.semantica.declaration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an interface of messages in code: each method is one message, and its key is the prefix, then the
 * method's name in kebab case. The {@code semantica-processor} reads it at compile time.
 *
 * <pre>{@code
 * @Messages("commands")
 * public interface CommandMessages {
 *     @Default("<white>You must wait <gray>{time, number} seconds</gray> before using this command again.")
 *     Message cooldown(double time);                     // commands.cooldown
 *
 *     @Default({"<gold>Socialismus <gray>{version}", "<gray>Reloaded in {millis, number} ms."})
 *     Message reloaded(String version, long millis);     // commands.reloaded, two lines
 *
 *     ReloadMessages reload();                           // a group: commands.reload.*
 * }
 * }</pre>
 *
 * <p>The processor fails the build when a default does not fit its method: a syntax error, an argument without a
 * parameter, a plural or number over a parameter that is no number, a reference to a key under one of the
 * compilation's own prefixes that none of its methods declares. It then writes:</p>
 *
 * <ul>
 *     <li>{@code GeneratedCommandMessages} beside the interface, with a public constructor and no reflection;
 *     {@code cooldown(3.5)} returns {@code Message.of("commands.cooldown").with("time", 3.5)};</li>
 *     <li>the defaults as {@code META-INF/semantica/<locale>/commands.json}, which
 *     {@code ClasspathSource} reads as the lowest layer; the locale is the processor option
 *     {@code semantica.locale}, {@code en} unless set.</li>
 * </ul>
 *
 * <p>A method returns {@link me.whereareiam.semantica.Message}, or another interface without parameters: a group,
 * whose methods take the method's name as one more key segment. A method of a group returns the same kinds.</p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Messages {
	/**
	 * Returns the prefix of every key of the interface.
	 *
	 * @return whole key segments separated by dots, such as {@code commands} or {@code chirper.vote}
	 */
	String value();
}
