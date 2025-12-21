package me.whereareiam.semantica;

public class ConsoleSemanticaLogger implements SemanticaLogger {
	@Override
	public void debug(String message, Object... args) {
		System.out.println(prefix("DEBUG") + format(message, args));
	}

	@Override
	public void info(String message, Object... args) {
		System.out.println(prefix("INFO") + format(message, args));
	}

	@Override
	public void warn(String message, Object... args) {
		System.out.println(prefix("WARN") + format(message, args));
	}

	@Override
	public void error(String message, Object... args) {
		System.err.println(prefix("ERROR") + format(message, args));
	}

	private String prefix(String level) {
		return "[Semantica " + level + "] ";
	}

	private String format(String message, Object... args) {
		try {
			return args == null || args.length == 0 ? message : String.format(message, args);
		} catch (Exception ex) {
			return message;
		}
	}
}

