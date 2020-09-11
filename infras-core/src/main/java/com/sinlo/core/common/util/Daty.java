package com.sinlo.core.common.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Daty the date util
 *
 * @author sinlo
 */
public class Daty {

    private Daty() {
    }

    private static TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");

    private static final SimpleDateFormat YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * check if the two given date is the sameday
     *
     * @see Daty#same(Date, Date, int)
     */
    public static boolean sameday(Date d1, Date d2) {
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
    public static boolean same(Date d1, Date d2, int field) {
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
    public static int weekday(Date date) {
        return get(date, Calendar.DAY_OF_WEEK);
    }

    /**
     * get day of year
     *
     * @see Daty#get(Date, int)
     */
    public static int day365(Date date) {
        return get(date, Calendar.DAY_OF_YEAR);
    }

    /**
     * @see Calendar#get(int)
     */
    public static int get(Date date, int field) {
        Calendar c = calendar(date);
        return c.get(field);
    }

    /**
     * get the first day of month
     *
     * @param offset offset the given date
     */
    public static Date dayOneOfMonth(Date date, int offset) {
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
    public static Date dayOneOfYear(Date date, int offset) {
        Calendar cal = calendar(date);
        cal.add(Calendar.YEAR, offset);
        cal.add(Calendar.DAY_OF_YEAR, cal.getActualMinimum(Calendar.DAY_OF_YEAR));
        return cal.getTime();
    }

    /**
     * @see Daty#calendar(Date, TimeZone)
     */
    public static Calendar calendar(Date date) {
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
     * use the given {@link TimeZone} globally
     */
    public static void use(TimeZone timeZone) {
        Daty.timeZone = timeZone;
    }
}
