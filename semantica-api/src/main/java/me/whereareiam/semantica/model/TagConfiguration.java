package me.whereareiam.semantica.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Configurable tag prefixes used by Semantica’s processors.
 * <p>
 * Defaults mirror Intercept:
 * <ul>
 *   <li>Message references: {@code <m:key>}</li>
 *   <li>Placeholders: {@code <p:name>}</li>
 *   <li>Templates: {@code <tpl:key>}</li>
 *   <li>Conditionals: {@code <if ...>} / {@code <else>}</li>
 * </ul>
 */
@Getter
@Builder(toBuilder = true)
public class TagConfiguration {
	@Builder.Default
	private final String messageRefPrefix = "m";
	@Builder.Default
	private final String placeholderPrefix = "p";
	@Builder.Default
	private final String templatePrefix = "tpl";
	@Builder.Default
	private final String conditionalIf = "if";
	@Builder.Default
	private final String conditionalElse = "else";

	public static TagConfiguration defaults() {
		return TagConfiguration.builder().build();
	}
}

