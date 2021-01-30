package com.sinlo.core.common.util;

import com.sinlo.core.common.wraparound.Two;
import com.sinlo.sponte.util.Pool;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Predicate;

/**
 * Daty the date util
 * <br/><strike>Daty wants to date with the date</strike>
 *
 * @author sinlo
 */
public class Daty {

    /**
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601#Combined_date_and_time_representations">Combined Date and Time Representations</a>
     */
    public static final DateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private static final Pool.Simple<DateFormat> formats = new Pool.Simple<>();
    /**
     * The default {@link Daty} instance of default timezone
     *
     * @see TimeZone#getDefault()
     */
    public static final Daty DEFAULT = of(TimeZone.getDefault());

    /**
     * The default {@link PolyParser} instance of default timezone, which can parse a
     * variety of common seen date strings
     */
    public static final PolyParser PARSER = PolyParser.builder()
            .full(s -> s.charAt(10) == 'T', ISO8601)
            .when(s -> s.charAt(4) == '-' && (s.length() == 10 || s.charAt(10) == ' '))
            .full(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS"))
            .wide(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
            .brief(new SimpleDateFormat("yyyy-MM-dd"))
            .and()
            .when(s -> s.charAt(4) == '/')
            .full(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSSSS"))
            .wide(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"))
            .brief(new SimpleDateFormat("yyyy/MM/dd"))
            .and()
            .when(s -> s.charAt(2) == '/')
            .full(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSSSSS"))
            .wide(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"))
            .brief(new SimpleDateFormat("dd/MM/yyyy"))
            .and()
            .fallback(null)
            .timeZone(TimeZone.getDefault())
            .build();

    /**
     * The {@link TimeZone} used across this {@link Daty}
     */
    public final TimeZone timeZone;

    private Daty(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Create a new Daty of the given {@code timeZone}
     */
    public static Daty of(TimeZone timeZone) {
        return new Daty(timeZone);
    }

    /**
     * check if the two given date is the sameday
     *
     * @see Daty#same(Date, Date, int)
     */
    public boolean sameday(Date d1, Date d2) {
        return same(d1, d2, Calendar.DAY_OF_MONTH);
    }

    /**
     * check if the two given date is the same regarding the given field
     *
     * @param field could be:
     *              <ul>
     *                  <li>{@link Calendar#ERA}</li>
     *                  <li>{@link Calendar#YEAR}</li>
     *                  <li>{@link Calendar#MONTH}</li>
     *                  <li>{@link Calendar#WEEK_OF_YEAR}, {@link Calendar#WEEK_OF_MONTH}</li>
     *                  <li>{@link Calendar#DATE}, {@link Calendar#DAY_OF_MONTH}, {@link Calendar#DAY_OF_YEAR},
     *                  {@link Calendar#DAY_OF_WEEK}, {@link Calendar#DAY_OF_WEEK_IN_MONTH}</li>
     *                  <li>{@link Calendar#AM_PM}, {@link Calendar#HOUR}, {@link Calendar#HOUR_OF_DAY}</li>
     *                  <li>{@link Calendar#MINUTE}</li>
     *                  <li>{@link Calendar#SECOND}</li>
     *                  <li>{@link Calendar#MILLISECOND}</li>
     *               </ul>
     */
    public boolean same(Date d1, Date d2, int field) {
        Calendar c1 = calendar(d1);
        Calendar c2 = calendar(d2);

        for (int i = Calendar.ERA; i <= field; i++) {
            if (c1.get(i) != c2.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * get day of week
     *
     * @see Daty#get(Date, int)
     */
    public int weekday(Date date) {
        return get(date, Calendar.DAY_OF_WEEK);
    }

    /**
     * get day of year
     *
     * @see Daty#get(Date, int)
     */
    public int day365(Date date) {
        return get(date, Calendar.DAY_OF_YEAR);
    }

    /**
     * @see Calendar#get(int)
     */
    public int get(Date date, int field) {
        Calendar c = calendar(date);
        return c.get(field);
    }

    /**
     * get the first day of week
     *
     * @param offset offset the given date
     */
    public Date dayOneOfWeek(Date date, int offset) {
        Calendar cal = calendar(date);
        cal.add(Calendar.WEEK_OF_YEAR, offset);
        cal.set(Calendar.DAY_OF_WEEK, cal.getActualMinimum(Calendar.DAY_OF_WEEK));
        return cal.getTime();
    }

    /**
     * get the first day of month
     *
     * @param offset offset the given date
     */
    public Date dayOneOfMonth(Date date, int offset) {
        Calendar cal = calendar(date);
        cal.add(Calendar.MONTH, offset);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        return cal.getTime();
    }

    /**
     * get the first day of year
     *
     * @param offset offset the given date
     */
    public Date dayOneOfYear(Date date, int offset) {
        Calendar cal = calendar(date);
        cal.add(Calendar.YEAR, offset);
        cal.set(Calendar.DAY_OF_YEAR, cal.getActualMinimum(Calendar.DAY_OF_YEAR));
        return cal.getTime();
    }

    /**
     * Just during today
     *
     * @see #during(LocalDateTime)
     */
    public <T> Two<LocalDateTime, LocalDateTime> duringToday() {
        return during(new Date());
    }

    /**
     * During the given {@link Date}
     *
     * @see #during(LocalDateTime)
     */
    public <T> Two<LocalDateTime, LocalDateTime> during(Date date) {
        return during(toLocal(date));
    }

    /**
     * @see #during(LocalDateTime, LocalDateTime)
     */
    public <T> Two<LocalDateTime, LocalDateTime> during(Date from, Date to) {
        return during(toLocal(from), toLocal(to));
    }

    /**
     * Similar to {@link #during(LocalDateTime)}, but between the start of the {@code from} and the
     * end of the {@code to}
     */
    public <T> Two<LocalDateTime, LocalDateTime> during(LocalDateTime from, LocalDateTime to) {
        return Two.two(from.toLocalDate().atStartOfDay(), to.toLocalDate().atTime(LocalTime.MAX));
    }

    /**
     * Split a given {@link LocalDateTime} to a date time of the midnight of that day <b>00:00</b>
     * and a date time just before tomorrow's midnight <b>23:59:59.999999999</b>
     */
    public Two<LocalDateTime, LocalDateTime> during(LocalDateTime date) {
        LocalDate local = date.toLocalDate();
        return Two.two(local.atStartOfDay(), local.atTime(LocalTime.MAX));
    }

    /**
     * @see Daty#calendar(Date, TimeZone)
     */
    public Calendar calendar(Date date) {
        return calendar(date, timeZone);
    }

    /**
     * get a calendar instance of the given date and timezone
     */
    public static Calendar calendar(Date date, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(date);
        return calendar;
    }

    /**
     * Get a corresponding {@link DateFormat}
     */
    public static DateFormat formatter(String pattern) {
        return formats.get(pattern, () -> new SimpleDateFormat(pattern));
    }

    /**
     * {@link Date} to {@link LocalDateTime}
     */
    public LocalDateTime toLocal(Date date) {
        return date.toInstant().atZone(timeZone.toZoneId()).toLocalDateTime();
    }

    /**
     * {@link LocalDateTime} to {@link Date}
     */
    public Date fromLocal(LocalDateTime local) {
        return Date.from(local.atZone(timeZone.toZoneId()).toInstant());
    }

    /**
     * A zipped date parser that can parse a variety of supported patterns
     */
    public static class PolyParser {

        private final Map<Predicate<String>, DateFormat> brief;
        private final Map<Predicate<String>, DateFormat> wide;
        private final Map<Predicate<String>, DateFormat> full;
        private final DateFormat fallback;

        private PolyParser(Map<Predicate<String>, DateFormat> brief,
                           Map<Predicate<String>, DateFormat> wide,
                           Map<Predicate<String>, DateFormat> full, DateFormat fallback) {
            this.brief = brief;
            this.wide = wide;
            this.full = full;
            this.fallback = fallback;
        }

        /**
         * Create a {@link Builder}
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Create a builder with all formats copied to it
         */
        public Builder copy() {
            Builder builder = new Builder().fallback(fallback);
            builder.brief.putAll(this.brief);
            builder.wide.putAll(this.wide);
            builder.full.putAll(this.full);
            return builder;
        }

        /**
         * Get a format collection based on the given length of the date string or the date
         * pattern
         */
        public Map<Predicate<String>, DateFormat> collection(int len) {
            switch (len) {
                case 10:
                    return brief;
                case 19:
                    return wide;
                default:
                    return len > 19 ? full : Collections.emptyMap();
            }
        }

        /**
         * Choose a proper {@link DateFormat} supporting the given {@code pattern}
         *
         * @return the proper {@link DateFormat} or the {@code fallback}
         */
        public Optional<DateFormat> choose(String pattern) {
            return Optional.ofNullable(collection(pattern.length())
                    .entrySet().stream()
                    .filter(e -> e.getKey().test(pattern))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(fallback));
        }

        /**
         * Do the parse using a proper parser selected from all supported parsers
         *
         * @throws NoValidParserException if none supporting parser could be found
         */
        public Date parse(String date) {
            if (Strine.isEmpty(date))
                throw new IllegalArgumentException("The input date string is empty");
            try {
                return choose(date)
                        .orElseThrow(() -> new NoValidParserException(date))
                        .parse(date);
            } catch (ParseException e) {
                return Try.toss(e);
            }
        }

        /**
         * The builder responsible for building a proper {@link PolyParser}
         */
        public static class Builder {

            private final Map<Predicate<String>, DateFormat> brief = new HashMap<>();
            private final Map<Predicate<String>, DateFormat> wide = new HashMap<>();
            private final Map<Predicate<String>, DateFormat> full = new HashMap<>();

            private DateFormat fallback;

            private TimeZone timeZone = TimeZone.getDefault();

            private Builder() {
            }

            /**
             * Add a {@link DateFormat} to support a kind of brief date pattern, whose length must be 10
             * (for example: yyyy-MM-dd)
             *
             * @param selector the selector who can predicate if the given pattern can be processed
             *                 by the related {@link DateFormat}
             * @param df       the {@link DateFormat}
             */
            public Builder brief(Predicate<String> selector, DateFormat df) {
                this.brief.put(selector, df);
                return this;
            }

            /**
             * Add a {@link DateFormat} to support a kind of wide date pattern, whose length must be 19
             * (for example: yyyy-MM-dd HH:mm:ss)
             *
             * @param selector the selector who can predicate if the given pattern can be processed
             *                 by the related {@link DateFormat}
             * @param df       the {@link DateFormat}
             */
            public Builder wide(Predicate<String> selector, DateFormat df) {
                this.wide.put(selector, df);
                return this;
            }

            /**
             * Add a {@link DateFormat} to support a kind of full date pattern, whose length must be
             * greater than 19 (for example: yyyy-MM-dd HH:mm:ss.SSSSSS)
             *
             * @param selector the selector who can predicate if the given pattern can be processed
             *                 by the related {@link DateFormat}
             * @param df       the {@link DateFormat}
             */
            public Builder full(Predicate<String> selector, DateFormat df) {
                this.full.put(selector, df);
                return this;
            }

            public When when(Predicate<String> selector) {
                return new When(selector);
            }

            /**
             * Set the timeZone
             */
            public Builder timeZone(TimeZone timeZone) {
                this.timeZone = timeZone;
                return this;
            }

            /**
             * Set the fallback {@link DateFormat}
             */
            public Builder fallback(DateFormat fallback) {
                this.fallback = fallback;
                return this;
            }

            /**
             * Build a {@link PolyParser}
             */
            public PolyParser build() {
                // set timezone for each DateFormat of the formats
                formats.values().forEach(Funny.bind(DateFormat::setTimeZone, timeZone));
                return new PolyParser(brief, wide, full, fallback);
            }

            /**
             * Hold a specific {@code selector} to more easily set {@link DateFormat}s
             */
            public class When {

                private final Predicate<String> selector;

                private When(Predicate<String> selector) {
                    this.selector = selector;
                }

                /**
                 * @see Builder#brief(Predicate, DateFormat)
                 */
                public When brief(DateFormat df) {
                    Builder.this.brief(selector, df);
                    return this;
                }

                /**
                 * @see Builder#wide(Predicate, DateFormat)
                 */
                public When wide(DateFormat df) {
                    Builder.this.wide(selector, df);
                    return this;
                }

                /**
                 * @see Builder#full(Predicate, DateFormat)
                 */
                public When full(DateFormat df) {
                    Builder.this.full(selector, df);
                    return this;
                }

                /**
                 * Back to the {@link Builder}
                 */
                public Builder and() {
                    return Builder.this;
                }
            }
        }

    }

    /**
     * That there's no valid parser available, hence could not parse the given pattern
     */
    public static class NoValidParserException extends RuntimeException {
        public NoValidParserException(String date) {
            super(String.format(
                    "Could not find any valid parser for the given date [ %s ]", date));
        }
    }
}
