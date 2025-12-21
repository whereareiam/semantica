package me.whereareiam.semantica.common.util;

import me.whereareiam.semantica.model.TagConfiguration;

/**
 * Precomputed tag tokens from configuration.
 */
public final class TagTokens {
	public final String messageRefTag;
	public final String placeholderTag;
	public final String templateTag;
	public final String conditionalIfTag;
	public final String conditionalElseTag;
	public final String conditionalIfCloseTag;

	public TagTokens(TagConfiguration config) {
		this.messageRefTag = "<" + config.getMessageRefPrefix() + ":";
		this.placeholderTag = "<" + config.getPlaceholderPrefix() + ":";
		this.templateTag = "<" + config.getTemplatePrefix() + ":";
		this.conditionalIfTag = "<" + config.getConditionalIf() + " ";
		this.conditionalElseTag = "<" + config.getConditionalElse() + ">";
		this.conditionalIfCloseTag = "</" + config.getConditionalIf() + ">";
	}
}

