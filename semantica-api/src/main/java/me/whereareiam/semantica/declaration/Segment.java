package me.whereareiam.semantica.declaration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes a parameter the last segment of the key instead of a value, for a set of messages that stays open, such as
 * one per reason a condition can fail:
 *
 * <pre>{@code
 * @Messages("conditions")
 * public interface ConditionMessages {
 *     Message denied(@Segment ConditionReason reason, String player);   // conditions.denied.<reason.segment()>
 * }
 * }</pre>
 *
 * <p>The parameter is a {@link String} or a {@link KeySegment}. The method has no {@link Default}: the texts of the
 * segments come from whoever defines them, such as a module's own files. One parameter of a method may be a
 * segment.</p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface Segment {
}
