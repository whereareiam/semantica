package me.whereareiam.semantica;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Text written in a config: an administrator's own message, a translated one pulled in with a reference, or both.
 * It is written in the message syntax, so it holds arguments and references as a translation does:
 *
 * <pre>
 * announcements:
 *   welcome: "&lt;gold&gt;Welcome to the server, {player}!"     # their own text
 *   vote: "{@chirper.vote}"                               # the translated text
 *   sale: "&lt;gold&gt;{@chirper.sale} &lt;gray&gt;(ends Sunday)"     # mixed
 * </pre>
 *
 * <p>A reference sees the values given to the content, so {@code {player}} still fills in inside
 * {@code chirper.vote}, and follows the reader's locale chain. Semantica parses a content once and keeps it by its
 * text; {@code Semantica.check(content)} returns what is wrong with one, for a plugin to report when it loads its
 * configs.</p>
 *
 * <pre>{@code
 * Content welcome = Content.of(config.getString("announcements.welcome"));
 * Content vote = Content.of(messages.vote());                    // writes {@chirper.vote}
 *
 * ResolvedMessage message = semantica.resolve(welcome, Map.of("player", name), List.of(locale));
 * }</pre>
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Content {
	private final String source;

	/**
	 * Takes text as written in a config.
	 *
	 * @param source the text, such as {@code <gold>Welcome, {player}!} or {@code {@chirper.vote}}
	 * @return the content
	 */
	public static @NotNull Content of(@NotNull String source) {
		return new Content(Objects.requireNonNull(source, "source"));
	}

	/**
	 * Refers to a message by its key, {@code {@chirper.vote}}, so a config's default names a declared message without
	 * a key string in code.
	 *
	 * @param message the message, without values: a content takes its values when it is resolved
	 * @return the content
	 * @throws IllegalArgumentException when the message has values, which a reference cannot keep
	 */
	public static @NotNull Content of(@NotNull Message message) {
		Objects.requireNonNull(message, "message");
		if (!message.getValues().isEmpty())
			throw new IllegalArgumentException("A content refers to " + message.getKey() + " by its key and keeps no values; give them when resolving it");

		return new Content("{@" + message.getKey() + "}");
	}

	@Override
	public String toString() {
		return source;
	}
}
