package me.whereareiam.semantica.common.resolution;

import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.RelativeDateTimeFormatter;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import me.whereareiam.semantica.message.ParsedMessage.Argument.Format;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Formats values for a locale with ICU4J and chooses plural forms by CLDR's rules.
 *
 * <p>Formats are built once per locale, format and style and copied for each use, since ICU's formats are
 * not safe to share between threads. The cache holds at most a few thousand formats and starts over when
 * it is full.</p>
 */
public final class ValueFormatter {
	private static final int CACHE_LIMIT = 2048;
	private static final long MINUTE = 60;
	private static final long HOUR = 60 * MINUTE;
	private static final long DAY = 24 * HOUR;
	private static final List<RelativeStep> RELATIVE_STEPS = List.of(
			new RelativeStep(MINUTE, 1, RelativeDateTimeFormatter.RelativeUnit.SECONDS),
			new RelativeStep(HOUR, MINUTE, RelativeDateTimeFormatter.RelativeUnit.MINUTES),
			new RelativeStep(DAY, HOUR, RelativeDateTimeFormatter.RelativeUnit.HOURS),
			new RelativeStep(7 * DAY, DAY, RelativeDateTimeFormatter.RelativeUnit.DAYS),
			new RelativeStep(30 * DAY, 7 * DAY, RelativeDateTimeFormatter.RelativeUnit.WEEKS),
			new RelativeStep(365 * DAY, 30 * DAY, RelativeDateTimeFormatter.RelativeUnit.MONTHS),
			new RelativeStep(Long.MAX_VALUE, 365 * DAY, RelativeDateTimeFormatter.RelativeUnit.YEARS)
	);

	private final Clock clock;
	private final Map<String, Object> formats = new ConcurrentHashMap<>();

	/**
	 * Creates a formatter.
	 *
	 * @param clock the clock relative times are measured from; its zone is the zone dates are shown in
	 */
	public ValueFormatter(@NotNull Clock clock) {
		this.clock = clock;
	}

	/**
	 * Returns the plural form of a number: {@code one}, {@code few}, {@code other}...
	 *
	 * @param locale  the locale whose rules apply
	 * @param number  the number, after the plural's offset
	 * @param ordinal whether ordinal forms are chosen
	 * @return the form
	 */
	public @NotNull String pluralForm(@NotNull Locale locale, double number, boolean ordinal) {
		return rules(locale, ordinal).select(number);
	}

	/**
	 * Returns the plural forms a language uses.
	 *
	 * @param locale  the locale
	 * @param ordinal whether ordinal forms are meant
	 * @return the forms, such as {@code one, few, many, other} for Russian
	 */
	public static @NotNull Set<String> pluralForms(@NotNull Locale locale, boolean ordinal) {
		return rules(locale, ordinal).getKeywords();
	}

	private static PluralRules rules(Locale locale, boolean ordinal) {
		return PluralRules.forLocale(ULocale.forLocale(locale), ordinal ? PluralRules.PluralType.ORDINAL : PluralRules.PluralType.CARDINAL);
	}

	/**
	 * Formats a value. This is where a value of a text, which may be of any type, gets its type: each case below
	 * hands a typed value on.
	 *
	 * @param value  the value
	 * @param format how the message asks for it
	 * @param style  the style, or {@code null}
	 * @param locale the locale
	 * @return the formatted value, or {@code null} when the message asks for no format and the value is of a type
	 * Semantica does not know, such as a component, which the text format renders itself
	 * @throws IllegalArgumentException when the value does not suit the format, or the style is not valid
	 */
	public @Nullable String format(@NotNull Object value, @NotNull Format format, @Nullable String style, @NotNull Locale locale) {
		return switch (value) {
			case Number number when format != Format.DATE && format != Format.TIME -> number(number, style, locale);
			case TemporalAccessor time when format != Format.NUMBER -> date(zoned(time), format, style, locale);
			case Date date when format != Format.NUMBER -> date(date.toInstant().atZone(clock.getZone()), format, style, locale);
			case Calendar calendar when format != Format.NUMBER ->
					date(calendar.toInstant().atZone(calendar.getTimeZone().toZoneId()), format, style, locale);
			case CharSequence text when format == Format.NONE -> text.toString();
			case Boolean bool when format == Format.NONE -> bool.toString();
			case Character character when format == Format.NONE -> character.toString();
			case Enum<?> constant when format == Format.NONE -> constant.name();
			default -> unknown(value, format);
		};
	}

	private static @Nullable String unknown(Object value, Format format) {
		if (format == Format.NONE) return null;
		throw new IllegalArgumentException("Cannot show " + value.getClass().getSimpleName() + " as " + format.name().toLowerCase(Locale.ROOT));
	}

	private String number(Number number, @Nullable String style, Locale locale) {
		if (style != null && style.startsWith("::"))
			return NumberFormatter.forSkeleton(style.substring(2).trim()).locale(locale).format(number).toString();

		NumberFormat prototype = (NumberFormat) cached("number|" + locale.toLanguageTag() + "|" + style, () -> numberFormat(style, ULocale.forLocale(locale)));
		return ((NumberFormat) prototype.clone()).format(number);
	}

	private static NumberFormat numberFormat(@Nullable String style, ULocale locale) {
		if (style == null) return NumberFormat.getInstance(locale);

		return switch (style) {
			case "integer" -> NumberFormat.getIntegerInstance(locale);
			case "percent" -> NumberFormat.getPercentInstance(locale);
			case "currency" -> NumberFormat.getCurrencyInstance(locale);
			default -> new DecimalFormat(style, DecimalFormatSymbols.getInstance(locale));
		};
	}

	private String date(ZonedDateTime time, Format format, @Nullable String style, Locale locale) {
		if ("relative".equals(style)) return relative(time.toInstant(), locale);

		DateFormat prototype = (DateFormat) cached("date|" + format + "|" + locale.toLanguageTag() + "|" + style,
				() -> dateFormat(format, style, ULocale.forLocale(locale)));
		DateFormat copy = (DateFormat) prototype.clone();
		copy.setTimeZone(TimeZone.getTimeZone(time.getZone().getId()));

		return copy.format(Date.from(time.toInstant()));
	}

	private static DateFormat dateFormat(Format format, @Nullable String style, ULocale locale) {
		if (style != null && style.startsWith("::")) return DateFormat.getInstanceForSkeleton(style.substring(2).trim(), locale);

		Integer dateStyle = dateStyle(style);
		if (dateStyle == null) return new SimpleDateFormat(style, locale);

		return switch (format) {
			case DATE -> DateFormat.getDateInstance(dateStyle, locale);
			case TIME -> DateFormat.getTimeInstance(dateStyle, locale);
			case NONE, NUMBER -> DateFormat.getDateTimeInstance(dateStyle, dateStyle, locale);
		};
	}

	private static @Nullable Integer dateStyle(@Nullable String style) {
		if (style == null) return DateFormat.MEDIUM;

		return switch (style) {
			case "short" -> DateFormat.SHORT;
			case "medium" -> DateFormat.MEDIUM;
			case "long" -> DateFormat.LONG;
			case "full" -> DateFormat.FULL;
			default -> null;
		};
	}

	private String relative(Instant instant, Locale locale) {
		long seconds = instant.getEpochSecond() - clock.instant().getEpochSecond();
		long distance = Math.abs(seconds);
		RelativeDateTimeFormatter.Direction direction = seconds < 0
				? RelativeDateTimeFormatter.Direction.LAST
				: RelativeDateTimeFormatter.Direction.NEXT;

		RelativeStep step = RELATIVE_STEPS.stream()
				.filter(candidate -> distance < candidate.below())
				.findFirst()
				.orElse(RELATIVE_STEPS.getLast());

		return RelativeDateTimeFormatter.getInstance(ULocale.forLocale(locale))
				.format(distance / step.seconds(), direction, step.unit());
	}

	private ZonedDateTime zoned(TemporalAccessor time) {
		ZoneId zone = clock.getZone();

		return switch (time) {
			case ZonedDateTime zoned -> zoned;
			case OffsetDateTime offset -> offset.toZonedDateTime();
			case Instant instant -> instant.atZone(zone);
			case LocalDateTime local -> local.atZone(zone);
			case LocalDate date -> date.atStartOfDay(zone);
			case LocalTime local -> local.atDate(LocalDate.now(clock)).atZone(zone);
			default -> throw new IllegalArgumentException("Cannot show " + time.getClass().getSimpleName() + " as a date or time");
		};
	}

	/**
	 * The unit a relative time is shown in while its distance from now is below a bound.
	 *
	 * @param below   the bound, in seconds
	 * @param seconds the seconds in one unit
	 * @param unit    the unit
	 */
	private record RelativeStep(long below, long seconds, RelativeDateTimeFormatter.RelativeUnit unit) {
	}

	private Object cached(String key, Supplier<Object> factory) {
		if (formats.size() > CACHE_LIMIT) formats.clear();
		return formats.computeIfAbsent(key, ignored -> factory.get());
	}
}
