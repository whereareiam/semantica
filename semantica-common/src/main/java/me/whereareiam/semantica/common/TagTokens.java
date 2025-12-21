package me.whereareiam.semantica.common;

import me.whereareiam.semantica.model.TagConfiguration;

/**
 * Precomputed tag tokens from configuration.
 */
public final class TagTokens {
	public final String referenceTag;
	public final String placeholderTag;
	public final String conditionalIfTag;
	public final String conditionalElseTag;
	public final String conditionalIfCloseTag;

	public TagTokens(TagConfiguration config) {
		this.referenceTag = "<" + config.getReferencePrefix() + ":";
		this.placeholderTag = "<" + config.getPlaceholderPrefix() + ":";
		this.conditionalIfTag = "<" + config.getConditionalIf() + " ";
		this.conditionalElseTag = "<" + config.getConditionalElse() + ">";
		this.conditionalIfCloseTag = "</" + config.getConditionalIf() + ">";
	}
}

