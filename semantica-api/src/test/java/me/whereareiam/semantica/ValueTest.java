package me.whereareiam.semantica;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValueTest {
	@Test
	void withMakesTheKindOfEachJavaValue() {
		Instant now = Instant.parse("2026-11-17T10:00:00Z");
		Message message = Message.of("shop.bought")
				.with("item", "Sword")
				.with("count", 3)
				.with("price", new BigDecimal("2.50"))
				.with("at", now)
				.with("paid", Date.from(now))
				.with("gift", true)
				.with("day", Value.choice(DayOfWeek.MONDAY))
				.with("note", Message.of("shop.note"));

		assertEquals(Map.of(
				"item", Value.text("Sword"),
				"count", Value.number(3),
				"price", Value.number(new BigDecimal("2.50")),
				"at", Value.time(now),
				"paid", Value.time(now),
				"gift", Value.flag(true),
				"day", Value.choice(DayOfWeek.MONDAY),
				"note", Value.nested(Message.of("shop.note"))
		), message.getValues());
	}

	@Test
	void showsEachKindAsPlainText() {
		Value.Rendered rendered = () -> "Sword of Dawn";

		assertEquals("Ann", Value.text("Ann").plain());
		assertEquals("2.5", Value.number(2.5).plain());
		assertEquals("2026-11-17T10:00:00Z", Value.time(Instant.parse("2026-11-17T10:00:00Z")).plain());
		assertEquals("false", Value.flag(false).plain());
		assertEquals("MONDAY", Value.choice(DayOfWeek.MONDAY).plain());
		assertEquals("shop.note", Value.nested(Message.of("shop.note")).plain());
		assertEquals("Sword of Dawn", rendered.plain());
	}
}
