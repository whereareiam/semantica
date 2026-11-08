package me.whereareiam.semantica.syntax.icu;

import me.whereareiam.semantica.message.ParsedMessage;
import me.whereareiam.semantica.syntax.Syntax;
import org.jetbrains.annotations.NotNull;

/**
 * ICU MessageFormat with references, the format of Semantica's translations.
 *
 * <ul>
 *     <li>arguments: {@code {name}}, {@code {count, number}}, {@code {count, number, integer}},
 *     {@code {when, date, short}}, {@code {when, time, ::Hm}};</li>
 *     <li>plurals: {@code {count, plural, =0 {no items} one {# item} other {# items}}}, with an optional
 *     {@code offset:1}, and {@code selectordinal} for ordinal forms;</li>
 *     <li>selects: {@code {gender, select, female {her} male {his} other {their}}};</li>
 *     <li>references: {@code {@vocabulary.player}}, and {@code {@vocabulary.player count=online}} to fill the
 *     referenced message's {@code count} from this message's {@code online};</li>
 *     <li>quoting: {@code ''} is an apostrophe, and an apostrophe before {@code {}, {@code }}, or {@code #} inside
 *     a plural, starts quoted text up to the next single apostrophe.</li>
 * </ul>
 *
 * <p>Markup such as MiniMessage's {@code <red>} is plain text to this syntax; each platform reads its own.</p>
 */
public final class IcuSyntax implements Syntax {
	@Override
	public @NotNull ParsedMessage parse(@NotNull String source) {
		return new IcuParser(source).parse();
	}
}
