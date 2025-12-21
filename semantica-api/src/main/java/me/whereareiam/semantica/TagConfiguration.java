package me.whereareiam.semantica;

import lombok.Builder;
import lombok.Getter;

/**
 * Configurable tag prefixes used by Semantica’s processors.
 * <p>
 * Defaults:
 * <ul>
 *   <li>References: {@code <ref:key>} or {@code <ref:key param='value'>}</li>
 *   <li>Placeholders: {@code <p:name>}</li>
 *   <li>Conditionals: {@code <if ...>} / {@code <else>}</li>
 * </ul>
 */
@Getter
@Builder(toBuilder = true)
public class TagConfiguration {
	@Builder.Default
	private final String referencePrefix = "ref";
	@Builder.Default
	private final String placeholderPrefix = "p";
	@Builder.Default
	private final String conditionalIf = "if";
	@Builder.Default
	private final String conditionalElse = "else";
	
	/**
	 * Maximum recursion depth for reference resolution.
	 * Set to -1 to disable depth limit (uses early exit when text stops changing).
	 * Default: 10
	 */
	@Builder.Default
	private final int maxReferenceDepth = 10;

	public static TagConfiguration defaults() {
		return TagConfiguration.builder().build();
	}
}

