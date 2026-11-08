package me.whereareiam.semantica.format.adventure.v4;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Locale;

/**
 * Finds the authority among {@code GlobalTranslator}'s sources. Adventure copies its sources on each call of
 * {@code sources()}, so the result is kept and looked up again only after an interval: an authority that was added
 * or removed is noticed within that interval.
 */
final class AuthorityLookup {
	private final Key name;
	private final long intervalNanos;

	private volatile @Nullable Translator authority;
	private volatile long checkedAt;
	private volatile boolean checked;

	AuthorityLookup(Key name, Duration interval) {
		this.name = name;
		this.intervalNanos = interval.toNanos();
	}

	/**
	 * Tells whether the authority, if any, answers a key, so another translator leaves it to the authority.
	 *
	 * @param self   the translator asking, never its own authority
	 * @param key    the key
	 * @param locale the locale
	 * @return whether an authority other than {@code self} can translate the key
	 */
	boolean defers(Translator self, String key, Locale locale) {
		Translator found = find(self);
		return found != null && found.canTranslate(key, locale);
	}

	private @Nullable Translator find(Translator self) {
		long now = System.nanoTime();
		if (checked && now - checkedAt < intervalNanos) return authority;

		Translator next = null;
		for (Translator source : GlobalTranslator.translator().sources()) {
			if (source != self && name.equals(source.name())) {
				next = source;
				break;
			}
		}

		authority = next;
		checkedAt = now;
		checked = true;

		return next;
	}
}
