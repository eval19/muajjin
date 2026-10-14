package org.muslimtechnician.muajjin;

import java.util.Calendar;

/*
 * Port of adhan-js 4.4.3 lib/esm/PrayerTimes.js, CalculationMethod.js,
 * CalculationParameters.js, DateUtils.js, TimeComponents.js and Madhab.js.
 * Copyright (c) 2016 Batoul Apps. MIT permission notice is packaged in
 * assets/native-engine-licenses.txt. Provenance: tests/native/PROVENANCE.txt.
 * https://github.com/batoulapps/adhan-js/tree/v4.4.3/src
 */
public final class NativePrayerCalculator {
    private NativePrayerCalculator() {}

    /**
     * Calculate actual epoch milliseconds [fajr, sunrise, dhuhr, asr, maghrib, isha].
     * The supplied Calendar's local year/month/day and timezone define the date;
     * coordinates do not silently select another timezone. Caller is not mutated.
     *
     * @throws IllegalArgumentException for invalid date/coordinates/method/madhab
     * @throws IllegalStateException for unresolved astronomical prayer outcomes
     */
    public static long[] calculate(Calendar date, double latitude, double longitude, int method, int madhab) {
        NativeCalendarMath.requireDate(date);
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Prayer coordinates must be finite and within latitude/longitude ranges");
        }
        if (madhab != 0 && madhab != 1) {
            throw new IllegalArgumentException("Unsupported madhab; expected 0 (Shafi) or 1 (Hanafi)");
        }
        Method parameters = Method.forId(method);
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH) + 1;
        int day = date.get(Calendar.DAY_OF_MONTH);
        long midnightUtc = NativeCalendarMath.ordinal(year, month, day) * 86400000L;
        NativeSolarMath.SolarTime solar = new NativeSolarMath.SolarTime(year, month, day, latitude, longitude);
        double dhuhr = instant(midnightUtc, solar.transit);
        double sunrise = instant(midnightUtc, solar.sunrise);
        double sunset = instant(midnightUtc, solar.sunset);
        // Calendar addition mirrors upstream dateByAddingDays, even across DST
        // or an entirely skipped local date (e.g. Pacific/Apia in December 2011).
        Calendar tomorrow = (Calendar) date.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        NativeCalendarMath.requireDate(tomorrow);
        NativeSolarMath.SolarTime tomorrowSolar = new NativeSolarMath.SolarTime(
                tomorrow.get(Calendar.YEAR), tomorrow.get(Calendar.MONTH) + 1,
                tomorrow.get(Calendar.DAY_OF_MONTH), latitude, longitude);
        double tomorrowSunrise = instant(NativeCalendarMath.ordinal(tomorrow) * 86400000L, tomorrowSolar.sunrise);
        double night = (tomorrowSunrise - sunset) / 1000;
        double asr = instant(midnightUtc, solar.afternoon(madhab == 1 ? 2 : 1));
        double fajr = instant(midnightUtc, solar.hourAngle(-parameters.fajrAngle, false));
        boolean moonsighting = parameters == Method.MOONSIGHTING;
        if (moonsighting && latitude >= 55) fajr = addSeconds(sunrise, -night / 7);
        int dayOfYear = NativeCalendarMath.dayOfYear(year, month, day);
        double safeFajr = moonsighting
                ? NativeSolarMath.morningTwilight(latitude, dayOfYear, year, sunrise)
                : addSeconds(sunrise, -0.5 * night);
        if (Double.isNaN(fajr) || safeFajr > fajr) fajr = safeFajr;
        double isha;
        if (parameters.ishaInterval > 0) {
            isha = addSeconds(sunset, parameters.ishaInterval * 60);
        } else {
            isha = instant(midnightUtc, solar.hourAngle(-parameters.ishaAngle, true));
            if (moonsighting && latitude >= 55) isha = addSeconds(sunset, night / 7);
            double safeIsha = moonsighting
                    ? NativeSolarMath.eveningTwilight(latitude, dayOfYear, year, sunset)
                    : addSeconds(sunset, 0.5 * night);
            if (Double.isNaN(isha) || safeIsha < isha) isha = safeIsha;
        }
        double maghrib = sunset;
        if (parameters.maghribAngle != 0) {
            double angleBased = instant(midnightUtc, solar.hourAngle(-parameters.maghribAngle, true));
            if (sunset < angleBased && isha > angleBased) maghrib = angleBased;
        }
        // Supported factories use MiddleOfTheNight, unresolved polar circles,
        // Shafaq.General and zero user adjustments, exactly as the web app does.
        long[] result = new long[] {
            rounded(fajr, parameters.roundUp),
            rounded(addSeconds(sunrise, parameters.sunriseAdjustment * 60), parameters.roundUp),
            rounded(addSeconds(dhuhr, parameters.dhuhrAdjustment * 60), parameters.roundUp),
            rounded(addSeconds(asr, parameters.asrAdjustment * 60), parameters.roundUp),
            rounded(addSeconds(maghrib, parameters.maghribAdjustment * 60), parameters.roundUp),
            rounded(isha, parameters.roundUp)
        };
        return result;
    }

    // TimeComponents floors hours, minutes and seconds separately. Rounding
    // decimal hours directly to milliseconds would disagree with adhan-js.
    private static double instant(long midnightUtc, double hoursValue) {
        if (!Double.isFinite(hoursValue)) return Double.NaN;
        double hours = Math.floor(hoursValue);
        double minutes = Math.floor((hoursValue - hours) * 60);
        double seconds = Math.floor((hoursValue - (hours + minutes / 60)) * 60 * 60);
        return midnightUtc + hours * 3600000 + minutes * 60000 + seconds * 1000;
    }

    static double addSeconds(double epoch, double seconds) {
        double value = epoch + seconds * 1000;
        // JavaScript Date TimeClip truncates fractional milliseconds toward zero.
        return Double.isFinite(value) ? (double) (long) value : Double.NaN;
    }

    private static long rounded(double epoch, boolean roundUp) {
        if (!Double.isFinite(epoch)) {
            throw new IllegalStateException("Unresolved astronomical prayer outcome for the requested date and coordinates");
        }
        long millis = (long) epoch;
        int seconds = (int) Math.floorMod(Math.floorDiv(millis, 1000), 60);
        // Upstream UP advances to the next minute even at exactly :00, and
        // preserves milliseconds from high-latitude fractional-night fallbacks.
        int offset = roundUp ? 60 - seconds : seconds >= 30 ? 60 - seconds : -seconds;
        return millis + offset * 1000L;
    }

    // Immutable factory parameters, so neither method nor madhab state leaks.
    private enum Method {
        KARACHI(18, 18, 0, 0, 0, 1, 0, 0, false),
        NORTH_AMERICA(15, 15, 0, 0, 0, 1, 0, 0, false),
        MUSLIM_WORLD_LEAGUE(18, 17, 0, 0, 0, 1, 0, 0, false),
        UMM_AL_QURA(18.5, 0, 90, 0, 0, 0, 0, 0, false),
        EGYPTIAN(19.5, 17.5, 0, 0, 0, 1, 0, 0, false),
        TEHRAN(17.7, 14, 0, 4.5, 0, 0, 0, 0, false),
        DUBAI(18.2, 18.2, 0, 0, -3, 3, 3, 3, false),
        KUWAIT(18, 17.5, 0, 0, 0, 0, 0, 0, false),
        QATAR(18, 0, 90, 0, 0, 0, 0, 0, false),
        SINGAPORE(20, 18, 0, 0, 0, 1, 0, 0, true),
        MOONSIGHTING(18, 18, 0, 0, 0, 5, 0, 3, false);

        final double fajrAngle;
        final double ishaAngle;
        final int ishaInterval;
        final double maghribAngle;
        final int sunriseAdjustment;
        final int dhuhrAdjustment;
        final int asrAdjustment;
        final int maghribAdjustment;
        final boolean roundUp;

        Method(double fajrAngle, double ishaAngle, int ishaInterval, double maghribAngle,
                int sunriseAdjustment, int dhuhrAdjustment, int asrAdjustment, int maghribAdjustment, boolean roundUp) {
            this.fajrAngle = fajrAngle;
            this.ishaAngle = ishaAngle;
            this.ishaInterval = ishaInterval;
            this.maghribAngle = maghribAngle;
            this.sunriseAdjustment = sunriseAdjustment;
            this.dhuhrAdjustment = dhuhrAdjustment;
            this.asrAdjustment = asrAdjustment;
            this.maghribAdjustment = maghribAdjustment;
            this.roundUp = roundUp;
        }

        static Method forId(int id) {
            switch (id) {
                case 1: return KARACHI;
                case 2: return NORTH_AMERICA;
                case 3: return MUSLIM_WORLD_LEAGUE;
                case 4: return UMM_AL_QURA;
                case 5: return EGYPTIAN;
                case 7: return TEHRAN;
                case 8: return DUBAI;
                case 9: return KUWAIT;
                case 10: return QATAR;
                case 11: return SINGAPORE;
                case 12:
                case 15: return MOONSIGHTING;
                default: throw new IllegalArgumentException("Unsupported prayer calculation method: " + id);
            }
        }
    }
}
