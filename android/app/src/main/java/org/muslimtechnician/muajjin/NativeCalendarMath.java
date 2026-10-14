package org.muslimtechnician.muajjin;

import java.util.Calendar;
import java.util.GregorianCalendar;

/** Gregorian local-date arithmetic shared by the two offline engines. */
final class NativeCalendarMath {
    private NativeCalendarMath() {}

    static void requireDate(Calendar date) {
        if (date == null) throw new IllegalArgumentException("A local Calendar date is required");
        if (date.get(Calendar.ERA) != GregorianCalendar.AD || date.get(Calendar.YEAR) < 1 || date.get(Calendar.YEAR) > 9999) {
            throw new IllegalArgumentException("Gregorian date year is outside the supported range 1..9999");
        }
    }

    // Gregorian date ordinal, independent of timezone offsets and DST. This is
    // NOT local-midnight epoch / 86400000, which loses a day during spring DST.
    static long ordinal(int year, int month, int day) {
        int a = (14 - month) / 12;
        long y = year + 4800L - a;
        int m = month + 12 * a - 3;
        long julianDayNumber = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        return julianDayNumber - 2440588;
    }

    static long ordinal(Calendar date) {
        return ordinal(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
    }

    static boolean leapYear(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    static int dayOfYear(int year, int month, int day) {
        return (int) (ordinal(year, month, day) - ordinal(year, 1, 1) + 1);
    }
}
