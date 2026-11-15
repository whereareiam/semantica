package me.whereareiam.semantica.source.file;

import me.whereareiam.semantica.Message;
import me.whereareiam.semantica.declaration.Default;
import me.whereareiam.semantica.declaration.KeySegment;
import me.whereareiam.semantica.declaration.Messages;
import me.whereareiam.semantica.declaration.Segment;

/**
 * Messages declared in code, compiled with the processor, for {@link ClasspathSourceTest}.
 */
@Messages("shop")
public interface ShopMessages {
	@Default("Bought {item} for {price, number} {price, plural, one {coin} other {coins}}")
	Message bought(String item, int price);

	@Default({"<gold>Shop", "<gray>Open until {closes, time, short}"})
	Message title(java.time.LocalTime closes);

	Message denied(@Segment KeySegment reason);

	Cart cart();

	interface Cart {
		@Default("{@shop.cart.empty-label}: {count, number}")
		Message empty(long count);

		@Default("Empty")
		Message emptyLabel();
	}
}
