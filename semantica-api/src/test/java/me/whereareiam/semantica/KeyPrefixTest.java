package me.whereareiam.semantica;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyPrefixTest {
	@Test
	void matchesWholeSegmentsOnly() {
		KeyPrefix prefix = KeyPrefix.of("realm.vanilla.common");

		assertTrue(prefix.matches("realm.vanilla.common"));
		assertTrue(prefix.matches("realm.vanilla.common.join.title"));
		assertFalse(prefix.matches("realm.vanilla.commons"));
		assertFalse(prefix.matches("realm.vanilla"));
		assertFalse(prefix.matches("realm.create.common.join.title"));
		assertEquals(3, prefix.depth());
	}

	@Test
	void rejectsWhatIsNoPrefix() {
		assertThrows(IllegalArgumentException.class, () -> KeyPrefix.of(""));
		assertThrows(IllegalArgumentException.class, () -> KeyPrefix.of(".game"));
		assertThrows(IllegalArgumentException.class, () -> KeyPrefix.of("game."));
		assertThrows(IllegalArgumentException.class, () -> KeyPrefix.of("realm..vanilla"));
	}
}
