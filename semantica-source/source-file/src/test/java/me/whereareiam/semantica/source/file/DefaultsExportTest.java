package me.whereareiam.semantica.source.file;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.Configura;
import me.whereareiam.semantica.source.Source;
import me.whereareiam.semantica.testing.IssueAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultsExportTest {
	@TempDir
	Path folder;

	/**
	 * A product's defaults: a model filled the way a Configura defaults provider fills it.
	 */
	public static final class Shop implements TranslationDefaults {
		@Override
		public Map<String, JsonNode> files() {
			Messages messages = new Messages();
			messages.title = "<gold>Shop</gold>";
			messages.lore = List.of("First line", "Second {price}");
			messages.cooldown = 30;
			messages.commands.buy = "Bought {count, plural, one {# item} other {# items}}";

			return Map.of("shop", Configura.builder().build().mapper().valueToTree(messages),
					"shop/admin", Configura.builder().build().mapper().valueToTree(Map.of("reload", "Reloaded")));
		}
	}

	public static final class Messages {
		public String title;
		public List<String> lore;
		public int cooldown;
		public Commands commands = new Commands();
	}

	public static final class Commands {
		public String buy;
	}

	public static final class Twice implements TranslationDefaults {
		@Override
		public Map<String, JsonNode> files() {
			return Map.of("shop", Configura.builder().build().mapper().valueToTree(Map.of("other", "x")));
		}
	}

	@Test
	void writesTheTextAsFilesTheSourceReadsBack() throws Exception {
		DefaultsExport.main(new String[]{folder.resolve("en").toString(), Shop.class.getName()});

		String json = Files.readString(folder.resolve("en/shop.json"));
		assertFalse(json.contains("cooldown"), json);

		Source.Content content = FileSource.of(folder, Configura.builder().build()).load();
		IssueAssert.assertNone(content.getIssues());
		assertEquals("<gold>Shop</gold>", content.entries(Locale.ENGLISH).get("shop.title").text());
		assertEquals("First line\nSecond {price}", content.entries(Locale.ENGLISH).get("shop.lore").text());
		assertEquals("Bought {count, plural, one {# item} other {# items}}", content.entries(Locale.ENGLISH).get("shop.commands.buy").text());
		assertEquals("Reloaded", content.entries(Locale.ENGLISH).get("shop.admin.reload").text());
		assertTrue(json.endsWith("}\n"));
	}

	@Test
	void refusesTwoDefaultsWritingOneFile() {
		assertThrows(IllegalArgumentException.class,
				() -> DefaultsExport.main(new String[]{folder.toString(), Shop.class.getName(), Twice.class.getName()}));
	}

	@Test
	void refusesClassesThatAreNoDefaults() {
		assertThrows(IllegalArgumentException.class,
				() -> DefaultsExport.main(new String[]{folder.toString(), Messages.class.getName()}));
	}
}
