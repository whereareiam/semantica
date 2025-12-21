package me.whereareiam.semantica.common;

import me.whereareiam.semantica.TagConfiguration;

/**
 * Precomputed tag tokens from configuration.
 */
public final class TagTokens {
	public final String referenceTag;
	public final String placeholderTag;
	public final String conditionalIfTag;
	public final String conditionalElseTag;
	public final String conditionalIfCloseTag;
	public final String openDelimiter;
	public final String closeDelimiter;

	public TagTokens(TagConfiguration config) {
		this.openDelimiter = config.getOpenDelimiter();
		this.closeDelimiter = config.getCloseDelimiter();
		this.referenceTag = openDelimiter + config.getReferencePrefix() + ":";
		this.placeholderTag = openDelimiter + config.getPlaceholderPrefix() + ":";
		this.conditionalIfTag = openDelimiter + config.getConditionalIf() + " ";
		this.conditionalElseTag = openDelimiter + config.getConditionalElse() + closeDelimiter;
		this.conditionalIfCloseTag = openDelimiter + "/" + config.getConditionalIf() + closeDelimiter;
	}
}

