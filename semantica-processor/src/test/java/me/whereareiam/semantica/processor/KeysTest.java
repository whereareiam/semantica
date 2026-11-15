package me.whereareiam.semantica.processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeysTest {
	@Test
	void writesMethodNamesInKebabCase() {
		assertEquals("cooldown", Keys.segment("cooldown"));
		assertEquals("staff-joined", Keys.segment("staffJoined"));
		assertEquals("open-gui", Keys.segment("openGUI"));
		assertEquals("http-server-down", Keys.segment("httpServerDown"));
		assertEquals("http-server", Keys.segment("HTTPServer"));
	}
}
