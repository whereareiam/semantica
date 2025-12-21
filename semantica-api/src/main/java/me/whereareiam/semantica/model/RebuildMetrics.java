package me.whereareiam.semantica.model;

import lombok.Value;

/**
 * Statistics returned after rebuilding the translation service.
 */
@Value
public class RebuildMetrics {
	/**
	 * Total number of translation keys
	 */
	int totalKeys;
	
	/**
	 * Number of static entries pre-rendered into cache
	 */
	int prerenderedEntries;
	
	/**
	 * Number of keys with circular dependencies detected
	 */
	int circularDependencies;
	
	/**
	 * Time taken to rebuild in milliseconds
	 */
	long durationMs;
}
