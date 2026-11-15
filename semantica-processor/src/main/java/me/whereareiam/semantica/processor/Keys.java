package me.whereareiam.semantica.processor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * Turns the names of methods into key segments.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Keys {
	/**
	 * Returns the key segment of a method: its name in kebab case, {@code staffJoined} as {@code staff-joined} and
	 * {@code openGUI} as {@code open-gui}.
	 *
	 * @param name the method's name
	 * @return the segment
	 */
	static String segment(String name) {
		StringBuilder segment = new StringBuilder();
		for (int index = 0; index < name.length(); index++) {
			char current = name.charAt(index);
			if (Character.isUpperCase(current) && index > 0 && startsWord(name, index)) segment.append('-');
			segment.append(current);
		}

		return segment.toString().toLowerCase(Locale.ROOT);
	}

	private static boolean startsWord(String name, int index) {
		boolean afterLower = !Character.isUpperCase(name.charAt(index - 1));
		boolean beforeLower = index + 1 < name.length() && Character.isLowerCase(name.charAt(index + 1));

		return afterLower || beforeLower;
	}
}
