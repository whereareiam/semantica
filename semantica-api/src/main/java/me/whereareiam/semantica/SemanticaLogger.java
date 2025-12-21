package me.whereareiam.semantica;

/**
 * Simple logger abstraction so consumers can plug their own logging framework.
 * <p>
 * Implementations should be lightweight; all methods are best-effort and may be no-ops.
 */
public interface SemanticaLogger {
	void debug(String message, Object... args);

	void info(String message, Object... args);

	void warn(String message, Object... args);

	void error(String message, Object... args);
}

