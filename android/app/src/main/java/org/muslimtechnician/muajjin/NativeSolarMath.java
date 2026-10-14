package org.muslimtechnician.muajjin;

/*
 * Port of adhan-js 4.4.3 lib/esm/Astronomical.js, SolarCoordinates.js,
 * SolarTime.js and MathUtils.js. Copyright (c) 2016 Batoul Apps, MIT.
 * Full permission notice: assets/native-engine-licenses.txt.
 * Source: https://github.com/batoulapps/adhan-js/tree/v4.4.3/src
 */
final class NativeSolarMath {
    private NativeSolarMath() {}

    static double radians(double value) { return value * Math.PI / 180.0; }
    static double degrees(double value) { return value * 180.0 / Math.PI; }
    static double normalize(double value, double scale) { return value - scale * Math.floor(value / scale); }
    static double unwind(double value) { return normalize(value, 360.0); }
    static double quadrant(double value) {
        if (value >= -180 && value <= 180) return value;
        return value - 360 * Math.floor(value / 360 + 0.5);
    }

    // Preserve upstream expression ordering; last-second boundaries feed epoch rounding.
    static double julianDay(int year, int month, int day) {
        int y = month > 2 ? year : year - 1;
        int m = month > 2 ? month : month + 12;
        int a = y / 100;
        int b = 2 - a + a / 4;
        return (int) (365.25 * (y + 4716)) + (int) (30.6001 * (m + 1)) + day + b - 1524.5;
    }

    static double meanSolarLongitude(double t) {
        return unwind(280.4664567 + 36000.76983 * t + 0.0003032 * Math.pow(t, 2));
    }
    static double meanLunarLongitude(double t) { return unwind(218.3165 + 481267.8813 * t); }
    static double ascendingLunarNodeLongitude(double t) {
        return unwind(125.04452 - 1934.136261 * t + 0.0020708 * Math.pow(t, 2) + Math.pow(t, 3) / 450000);
    }
    static double meanSolarAnomaly(double t) {
        return unwind(357.52911 + 35999.05029 * t - 0.0001537 * Math.pow(t, 2));
    }
    static double solarEquationOfCenter(double t, double anomaly) {
        double m = radians(anomaly);
        double first = (1.914602 - 0.004817 * t - 0.000014 * Math.pow(t, 2)) * Math.sin(m);
        double second = (0.019993 - 0.000101 * t) * Math.sin(2 * m);
        double third = 0.000289 * Math.sin(3 * m);
        return first + second + third;
    }
    static double apparentSolarLongitude(double t, double longitude) {
        double center = longitude + solarEquationOfCenter(t, meanSolarAnomaly(t));
        double omega = 125.04 - 1934.136 * t;
        return unwind(center - 0.00569 - 0.00478 * Math.sin(radians(omega)));
    }
    static double meanObliquity(double t) {
        return 23.439291 - 0.013004167 * t - 0.0000001639 * Math.pow(t, 2) + 0.0000005036 * Math.pow(t, 3);
    }
    static double apparentObliquity(double t, double obliquity) {
        return obliquity + 0.00256 * Math.cos(radians(125.04 - 1934.136 * t));
    }
    static double meanSiderealTime(double t) {
        double jd = t * 36525 + 2451545.0;
        return unwind(280.46061837 + 360.98564736629 * (jd - 2451545) + 0.000387933 * Math.pow(t, 2) - Math.pow(t, 3) / 38710000);
    }
    static double nutationLongitude(double t, double solarLongitude, double lunarLongitude, double node) {
        double first = -17.2 / 3600 * Math.sin(radians(node));
        double second = 1.32 / 3600 * Math.sin(2 * radians(solarLongitude));
        double third = 0.23 / 3600 * Math.sin(2 * radians(lunarLongitude));
        double fourth = 0.21 / 3600 * Math.sin(2 * radians(node));
        return first - second - third + fourth;
    }
    static double nutationObliquity(double t, double solarLongitude, double lunarLongitude, double node) {
        double first = 9.2 / 3600 * Math.cos(radians(node));
        double second = 0.57 / 3600 * Math.cos(2 * radians(solarLongitude));
        double third = 0.1 / 3600 * Math.cos(2 * radians(lunarLongitude));
        double fourth = 0.09 / 3600 * Math.cos(2 * radians(node));
        return first + second + third - fourth;
    }
    static double altitude(double latitude, double declination, double hourAngle) {
        double first = Math.sin(radians(latitude)) * Math.sin(radians(declination));
        double second = Math.cos(radians(latitude)) * Math.cos(radians(declination)) * Math.cos(radians(hourAngle));
        return degrees(Math.asin(first + second));
    }
    static double interpolate(double y2, double y1, double y3, double n) {
        double a = y2 - y1;
        double b = y3 - y2;
        double c = b - a;
        return y2 + n / 2 * (a + b + n * c);
    }
    static double interpolateAngles(double y2, double y1, double y3, double n) {
        double a = unwind(y2 - y1);
        double b = unwind(y3 - y2);
        double c = b - a;
        return y2 + n / 2 * (a + b + n * c);
    }

    static final class Coordinates {
        final double declination;
        final double rightAscension;
        final double siderealTime;

        Coordinates(double julianDay) {
            double t = (julianDay - 2451545.0) / 36525;
            double l0 = meanSolarLongitude(t);
            double lp = meanLunarLongitude(t);
            double omega = ascendingLunarNodeLongitude(t);
            double lambda = radians(apparentSolarLongitude(t, l0));
            double theta = meanSiderealTime(t);
            double dPsi = nutationLongitude(t, l0, lp, omega);
            double dEpsilon = nutationObliquity(t, l0, lp, omega);
            double epsilon = meanObliquity(t);
            double apparentEpsilon = radians(apparentObliquity(t, epsilon));
            declination = degrees(Math.asin(Math.sin(apparentEpsilon) * Math.sin(lambda)));
            rightAscension = unwind(degrees(Math.atan2(Math.cos(apparentEpsilon) * Math.sin(lambda), Math.cos(lambda))));
            siderealTime = theta + dPsi * 3600 * Math.cos(radians(epsilon + dEpsilon)) / 3600;
        }
    }

    static final class SolarTime {
        final double latitude;
        final double longitude;
        final Coordinates solar;
        final Coordinates previous;
        final Coordinates next;
        final double approximateTransit;
        final double transit;
        final double sunrise;
        final double sunset;

        SolarTime(int year, int month, int day, double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            double jd = julianDay(year, month, day);
            solar = new Coordinates(jd);
            previous = new Coordinates(jd - 1);
            next = new Coordinates(jd + 1);
            approximateTransit = normalize((solar.rightAscension + longitude * -1 - solar.siderealTime) / 360, 1);
            double theta = unwind(solar.siderealTime + 360.985647 * approximateTransit);
            double ascension = unwind(interpolateAngles(solar.rightAscension, previous.rightAscension, next.rightAscension, approximateTransit));
            double h = quadrant(theta - longitude * -1 - ascension);
            transit = (approximateTransit + h / -360) * 24;
            sunrise = hourAngle(-50.0 / 60.0, false);
            sunset = hourAngle(-50.0 / 60.0, true);
        }

        double hourAngle(double angle, boolean afterTransit) {
            double lw = longitude * -1;
            double first = Math.sin(radians(angle)) - Math.sin(radians(latitude)) * Math.sin(radians(solar.declination));
            double second = Math.cos(radians(latitude)) * Math.cos(radians(solar.declination));
            double h0 = degrees(Math.acos(first / second));
            double m = afterTransit ? approximateTransit + h0 / 360 : approximateTransit - h0 / 360;
            double theta = unwind(solar.siderealTime + 360.985647 * m);
            double ascension = unwind(interpolateAngles(solar.rightAscension, previous.rightAscension, next.rightAscension, m));
            double delta = interpolate(solar.declination, previous.declination, next.declination, m);
            double h = theta - lw - ascension;
            double altitude = altitude(latitude, delta, h);
            double third = altitude - angle;
            double fourth = 360 * Math.cos(radians(delta)) * Math.cos(radians(latitude)) * Math.sin(radians(h));
            return (m + third / fourth) * 24;
        }

        double afternoon(int shadowLength) {
            double tangent = Math.abs(latitude - solar.declination);
            double inverse = shadowLength + Math.tan(radians(tangent));
            double angle = degrees(Math.atan(1.0 / inverse));
            return hourAngle(angle, true);
        }
    }

    static int daysSinceSolstice(int dayOfYear, int year, double latitude) {
        boolean leap = NativeCalendarMath.leapYear(year);
        int yearLength = leap ? 366 : 365;
        int days = latitude >= 0 ? dayOfYear + 10 : dayOfYear - (leap ? 173 : 172);
        if (days >= yearLength) days -= yearLength;
        if (days < 0) days += yearLength;
        return days;
    }

    private static double seasonalAdjustment(int days, double a, double b, double c, double d) {
        if (days < 91) return a + (b - a) / 91.0 * days;
        if (days < 137) return b + (c - b) / 46.0 * (days - 91);
        if (days < 183) return c + (d - c) / 46.0 * (days - 137);
        if (days < 229) return d + (c - d) / 46.0 * (days - 183);
        if (days < 275) return c + (b - c) / 46.0 * (days - 229);
        return b + (a - b) / 91.0 * (days - 275);
    }

    static double morningTwilight(double latitude, int dayOfYear, int year, double sunrise) {
        double absoluteLatitude = Math.abs(latitude);
        double a = 75 + 28.65 / 55.0 * absoluteLatitude;
        double b = 75 + 19.44 / 55.0 * absoluteLatitude;
        double c = 75 + 32.74 / 55.0 * absoluteLatitude;
        double d = 75 + 48.1 / 55.0 * absoluteLatitude;
        double adjustment = seasonalAdjustment(daysSinceSolstice(dayOfYear, year, latitude), a, b, c, d);
        return NativePrayerCalculator.addSeconds(sunrise, Math.floor(adjustment * -60.0 + 0.5));
    }

    static double eveningTwilight(double latitude, int dayOfYear, int year, double sunset) {
        // The supported MoonsightingCommittee factory uses Shafaq.General.
        double absoluteLatitude = Math.abs(latitude);
        double a = 75 + 25.6 / 55.0 * absoluteLatitude;
        double b = 75 + 2.05 / 55.0 * absoluteLatitude;
        double c = 75 - 9.21 / 55.0 * absoluteLatitude;
        double d = 75 + 6.14 / 55.0 * absoluteLatitude;
        double adjustment = seasonalAdjustment(daysSinceSolstice(dayOfYear, year, latitude), a, b, c, d);
        return NativePrayerCalculator.addSeconds(sunset, Math.floor(adjustment * 60.0 + 0.5));
    }
}
