package com.sinlo.core.common.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Daty the date util
 *
 * @author sinlo
 */
public class Daty {

    public static final String DF_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final Daty DEFAULT = of(DF_PATTERN).build();

    public final TimeZone timeZone;
    public final DateFormat df;

    private Daty(TimeZone timeZone, DateFormat df) {
        this.timeZone = timeZone;
        this.df = df;
    }

    /**
     * A builder starting from {@link Builder#timeZone(TimeZone)}
     */
    public static Builder of(TimeZone timeZone) {
        return new Builder().timeZone(timeZone);
    }

    /**
     * A builder starting from {@link Builder#sdf(String)}
     */
    public static Builder of(String pattern) {
        return new Builder().sdf(pattern);
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
     * get the first day of month
     *
     * @param offset offset the given date
     */
    public Date dayOneOfMonth(Date date, int offset) {
        Calendar cal = calendar(date);
        cal.add(Calendar.MONTH, offset);
        cal.add(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
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
        cal.add(Calendar.DAY_OF_YEAR, cal.getActualMinimum(Calendar.DAY_OF_YEAR));
        return cal.getTime();
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
     * The builder of {@link Daty}
     */
    public static class Builder {
        private DateFormat df;
        private TimeZone timeZone;

        public Builder df(DateFormat df) {
            this.df = df;
            return this;
        }

        /**
         * @see SimpleDateFormat#SimpleDateFormat(String)
         */
        public Builder sdf(String pattern) {
            this.df = new SimpleDateFormat(pattern);
            return this;
        }

        public Builder timeZone(TimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        /**
         * @see TimeZone#getTimeZone(String)
         */
        public Builder timeZone(String timeZone) {
            this.timeZone = TimeZone.getTimeZone(timeZone);
            return this;
        }

        /**
         * @see TimeZone#getTimeZone(ZoneId)
         */
        public Builder timeZone(ZoneId zoneId) {
            this.timeZone = TimeZone.getTimeZone(zoneId);
            return this;
        }

        public Daty build() {
            if (timeZone == null) timeZone(TimeZone.getDefault());
            if (df == null) sdf(DF_PATTERN);
            return new Daty(timeZone, df);
        }
    }
}
