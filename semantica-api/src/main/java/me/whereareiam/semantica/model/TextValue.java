package me.whereareiam.semantica.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Type-safe representation of text content that can be either:
 * <ul>
 *   <li>A single string, or</li>
 *   <li>A list of strings (joined with {@code \n} for processing)</li>
 * </ul>
 * This mirrors the flexible YAML/JSON shapes used in Intercept.
 */
@NoArgsConstructor
@SuppressWarnings("unused")
public abstract class TextValue {
	/**
	 * @return content as a single string (list values are joined with newlines).
	 */
	public abstract String asString();

	/**
	 * String variant.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class StringValue extends TextValue {
		private String value;

		@Override
		public String asString() {
			return value;
		}

		@Override
		public String toString() {
			return value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof StringValue that)) return false;
			return Objects.equals(value, that.value);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(value);
		}
	}

	/**
	 * List variant.
	 */
	@Getter
	@Setter
	public static class ListValue extends TextValue {
		private List<String> value;

		public ListValue() {
			this.value = new ArrayList<>();
		}

		public ListValue(List<String> value) {
			this.value = Objects.requireNonNull(value, "value");
		}

		@Override
		public String asString() {
			return String.join("\n", value);
		}

		@Override
		public String toString() {
			return asString();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof ListValue that)) return false;
			return Objects.equals(value, that.value);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(value);
		}
	}
}

