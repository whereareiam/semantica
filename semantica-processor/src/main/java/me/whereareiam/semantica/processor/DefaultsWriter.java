package me.whereareiam.semantica.processor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.whereareiam.semantica.processor.MessageGroup.GroupMethod;
import me.whereareiam.semantica.processor.MessageGroup.Member;
import me.whereareiam.semantica.processor.MessageGroup.MessageMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes the defaults of a {@code @Messages} interface as the JSON file a {@code FileSource} or a
 * {@code ClasspathSource} reads: each method under its key segment, a group as an object, a message of several lines
 * as a list. Methods with a segment parameter have no default and are left out.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DefaultsWriter {
	/**
	 * Returns the path of an interface's file inside the locale folder: its prefix, a folder per segment.
	 *
	 * @param prefix the interface's prefix, such as {@code chirper.vote}
	 * @return the path, such as {@code chirper/vote.json}
	 */
	static String path(String prefix) {
		return prefix.replace('.', '/') + ".json";
	}

	/**
	 * Writes the file of an interface.
	 *
	 * @param root the interface as read
	 * @return the JSON
	 */
	static String write(MessageGroup root) {
		StringBuilder json = new StringBuilder();
		object(json, root, "");

		return json.append('\n').toString();
	}

	private static void object(StringBuilder json, MessageGroup group, String indent) {
		List<String> fields = new ArrayList<>();
		String inner = indent + "  ";
		for (Member member : group.getMethods()) {
			StringBuilder field = new StringBuilder(inner).append(string(member.segment())).append(": ");
			switch (member) {
				case MessageMethod message -> {
					if (message.keyed() != null) continue;
					lines(field, message.lines(), inner);
				}
				case GroupMethod nested -> object(field, nested.group(), inner);
			}
			fields.add(field.toString());
		}

		if (fields.isEmpty()) {
			json.append("{}");
			return;
		}

		json.append("{\n").append(String.join(",\n", fields)).append('\n').append(indent).append('}');
	}

	private static void lines(StringBuilder json, List<String> lines, String indent) {
		if (lines.size() == 1) {
			json.append(string(lines.getFirst()));
			return;
		}

		List<String> written = lines.stream().map(line -> indent + "  " + string(line)).toList();
		json.append("[\n").append(String.join(",\n", written)).append('\n').append(indent).append(']');
	}

	/**
	 * Writes a string as a JSON string.
	 */
	private static String string(String text) {
		StringBuilder string = new StringBuilder("\"");
		for (char character : text.toCharArray()) {
			switch (character) {
				case '"' -> string.append("\\\"");
				case '\\' -> string.append("\\\\");
				case '\n' -> string.append("\\n");
				case '\t' -> string.append("\\t");
				default -> {
					if (character < 0x20) string.append(String.format("\\u%04x", (int) character));
					else string.append(character);
				}
			}
		}

		return string.append('"').toString();
	}
}
