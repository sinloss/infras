package com.sinlo.core.common.util;

import com.sinlo.core.common.wraparound.Two;
import com.sinlo.sponte.util.Pool;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Daty the date util
 * <br/><strike>Daty wants to date with the date</strike>
 *
 * @author sinlo
 */
public class Daty {

    private static final Pool.Simple<DateFormat> formats = new Pool.Simple<>();
    public static final Daty DEFAULT = of(TimeZone.getDefault());

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
     */
    public <T> Two<LocalDateTime, LocalDateTime> during(Date date) {
        return during(toLocal(date));
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

}
