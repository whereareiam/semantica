package me.whereareiam.semantica.format;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.whereareiam.semantica.message.ResolvedMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Renders messages as plain text for logs and consoles: values as formatted, and tags such as MiniMessage's
 * {@code <red>} removed from literal parts when asked to.
 *
 * <pre>{@code
 * PlainRenderer plain = PlainRenderer.withoutTags();
 * logger.info(plain.render(semantica.resolve(Message.of("server.started"), Locale.ENGLISH)));
 * }</pre>
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlainRenderer implements Renderer<String> {
	private static final Pattern TAG = Pattern.compile("</?[!?#]?[a-z0-9_:\\-.]+(?::[^<>]*)?/?>", Pattern.CASE_INSENSITIVE);
	private static final PlainRenderer AS_WRITTEN = new PlainRenderer(false);
	private static final PlainRenderer WITHOUT_TAGS = new PlainRenderer(true);

	private final boolean stripTags;

	/**
	 * Returns a renderer that keeps literal parts as written.
	 *
	 * @return the renderer
	 */
	public static @NotNull PlainRenderer asWritten() {
		return AS_WRITTEN;
	}

	/**
	 * Returns a renderer that removes tags written as {@code <name>}, {@code <name:argument>} or {@code </name>}
	 * from literal parts. Values are never changed.
	 *
	 * @return the renderer
	 */
	public static @NotNull PlainRenderer withoutTags() {
		return WITHOUT_TAGS;
	}

	@Override
	public @NotNull String render(@NotNull ResolvedMessage message) {
		StringBuilder text = new StringBuilder();
		append(message.getParts(), text);

		return text.toString();
	}

	// A linked value shows its content.
	private void append(List<ResolvedMessage.Part> parts, StringBuilder text) {
		for (ResolvedMessage.Part part : parts) {
			switch (part) {
				case ResolvedMessage.Literal literal when stripTags -> text.append(TAG.matcher(literal.text()).replaceAll(""));
				case ResolvedMessage.Linked linked -> append(linked.parts(), text);
				default -> text.append(part.text());
			}
		}
	}
}
