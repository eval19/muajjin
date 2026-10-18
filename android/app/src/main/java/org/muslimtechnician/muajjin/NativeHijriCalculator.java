package org.muslimtechnician.muajjin;

import java.util.Calendar;

/*
 * Exact year/month mapping ported from @umalqura/core 0.0.7,
 * dist/index.esm.js:574-787. Copyright (c) 2019 Alaa Masoud, MIT.
 * Full permission notice: assets/native-engine-licenses.txt.
 * Source: https://github.com/umalqura/umalqura
 * The upstream table was generated from the .NET BCL (its original attribution).
 * Unlike upstream epoch subtraction, conversion below uses Gregorian ordinals
 * so timezone/DST changes cannot truncate a fractional day to yesterday.
 */
public final class NativeHijriCalculator {
    private NativeHijriCalculator() {}

    /** Return [day, month1Based, year], retaining adjustment and Maghrib rollover. */
    public static int[] calculate(Calendar localDate, long adjustment, boolean changeAtMaghrib, long now, long maghrib) {
        NativeCalendarMath.requireDate(localDate);
        long shift = adjustment + (changeAtMaghrib && now >= maghrib ? 1 : 0);
        if (shift < Integer.MIN_VALUE || shift > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Hijri date adjustment is outside the supported range");
        }
        Calendar selected = localDate;
        if (shift != 0) {
            selected = (Calendar) localDate.clone();
            selected.add(Calendar.DAY_OF_MONTH, (int) shift);
            NativeCalendarMath.requireDate(selected);
        }
        long ordinal = NativeCalendarMath.ordinal(selected);
        if (ordinal < YEAR_STARTS[0] || ordinal >= YEAR_STARTS[YEAR_STARTS.length - 1]) {
            throw new IllegalArgumentException("Hijri date is outside the supported range 1900-04-30..2077-11-16");
        }
        int lower = 0;
        int upper = MONTH_FLAGS.length;
        while (lower + 1 < upper) {
            int middle = (lower + upper) >>> 1;
            if (ordinal < YEAR_STARTS[middle]) upper = middle;
            else lower = middle;
        }
        int remaining = (int) (ordinal - YEAR_STARTS[lower]);
        int flags = MONTH_FLAGS[lower];
        int month = 1;
        while (remaining >= 29 + (flags & 1)) {
            remaining -= 29 + (flags & 1);
            flags >>= 1;
            month++;
        }
        return new int[] { remaining + 1, month, 1318 + lower };
    }

    // Bit 0 is Muharram: 1 = 30 days, 0 = 29 days. One entry per
    // Hijri year 1318..1500, copied verbatim from the licensed package.
    private static final int[] MONTH_FLAGS = {
        0x02EA, 0x06E9, 0x0ED2, 0x0EA4, 0x0D4A, 0x0A96, 0x0536, 0x0AB5,
        0x0DAA, 0x0BA4, 0x0B49, 0x0A93, 0x052B, 0x0A57, 0x04B6, 0x0AB5,
        0x05AA, 0x0D55, 0x0D2A, 0x0A56, 0x04AE, 0x095D, 0x02EC, 0x06D5,
        0x06AA, 0x0555, 0x04AB, 0x095B, 0x02BA, 0x0575, 0x0BB2, 0x0764,
        0x0749, 0x0655, 0x02AB, 0x055B, 0x0ADA, 0x06D4, 0x0EC9, 0x0D92,
        0x0D25, 0x0A4D, 0x02AD, 0x056D, 0x0B6A, 0x0B52, 0x0AA5, 0x0A4B,
        0x0497, 0x0937, 0x02B6, 0x0575, 0x0D6A, 0x0D52, 0x0A96, 0x092D,
        0x025D, 0x04DD, 0x0ADA, 0x05D4, 0x0DA9, 0x0D52, 0x0AAA, 0x04D6,
        0x09B6, 0x0374, 0x0769, 0x0752, 0x06A5, 0x054B, 0x0AAB, 0x055A,
        0x0AD5, 0x0DD2, 0x0DA4, 0x0D49, 0x0A95, 0x052D, 0x0A5D, 0x055A,
        0x0AD5, 0x06AA, 0x0695, 0x052B, 0x0A57, 0x04AE, 0x0976, 0x056C,
        0x0B55, 0x0AAA, 0x0A55, 0x04AD, 0x095D, 0x02DA, 0x05D9, 0x0DB2,
        0x0BA4, 0x0B4A, 0x0A55, 0x02B5, 0x0575, 0x0B6A, 0x0BD2, 0x0BC4,
        0x0B89, 0x0A95, 0x052D, 0x05AD, 0x0B6A, 0x06D4, 0x0DC9, 0x0D92,
        0x0AA6, 0x0956, 0x02AE, 0x056D, 0x036A, 0x0B55, 0x0AAA, 0x094D,
        0x049D, 0x095D, 0x02BA, 0x05B5, 0x05AA, 0x0D55, 0x0A9A, 0x092E,
        0x026E, 0x055D, 0x0ADA, 0x06D4, 0x06A5, 0x054B, 0x0A97, 0x054E,
        0x0AAE, 0x05AC, 0x0BA9, 0x0D92, 0x0B25, 0x064B, 0x0CAB, 0x055A,
        0x0B55, 0x06D2, 0x0EA5, 0x0E4A, 0x0A95, 0x052D, 0x0AAD, 0x036C,
        0x0759, 0x06D2, 0x0695, 0x052D, 0x0A5B, 0x04BA, 0x09BA, 0x03B4,
        0x0B69, 0x0B52, 0x0AA6, 0x04B6, 0x096D, 0x02EC, 0x06D9, 0x0EB2,
        0x0D54, 0x0D2A, 0x0A56, 0x04AE, 0x096D, 0x0D6A, 0x0B54, 0x0B29,
        0x0A93, 0x052B, 0x0A57, 0x0536, 0x0AB5, 0x06AA, 0x0E93
    };

    // Gregorian starts of the same 183 years, plus the exclusive sentinel
    // 2077-11-17. Packed YYYYMMDD literals are converted in place once to
    // timezone-independent ordinals (no per-conversion dates or table copies).
    private static final int[] YEAR_STARTS = {
        19000430, 19010419, 19020409, 19030330, 19040318, 19050307, 19060224, 19070213,
        19080203, 19090123, 19100112, 19110101, 19111221, 19121209, 19131129, 19141118,
        19151108, 19161027, 19171017, 19181006, 19190925, 19200913, 19210903, 19220823,
        19230813, 19240801, 19250721, 19260710, 19270630, 19280618, 19290608, 19300529,
        19310518, 19320506, 19330425, 19340414, 19350404, 19360324, 19370313, 19380303,
        19390220, 19400209, 19410128, 19420117, 19430107, 19431228, 19441216, 19451205,
        19461124, 19471113, 19481102, 19491022, 19501012, 19511002, 19520920, 19530909,
        19540829, 19550818, 19560807, 19570728, 19580717, 19590707, 19600625, 19610614,
        19620603, 19630524, 19640512, 19650502, 19660421, 19670410, 19680329, 19690319,
        19700308, 19710226, 19720216, 19730204, 19740124, 19750113, 19760102, 19761222,
        19771211, 19781201, 19791120, 19801108, 19811028, 19821018, 19831007, 19840926,
        19850915, 19860905, 19870825, 19880813, 19890802, 19900723, 19910712, 19920701,
        19930621, 19940610, 19950530, 19960518, 19970507, 19980427, 19990417, 20000406,
        20010326, 20020315, 20030304, 20040221, 20050210, 20060131, 20070120, 20080110,
        20081229, 20091218, 20101207, 20111126, 20121115, 20131104, 20141025, 20151014,
        20161002, 20170921, 20180911, 20190831, 20200820, 20210809, 20220730, 20230719,
        20240707, 20250626, 20260616, 20270606, 20280525, 20290514, 20300503, 20310423,
        20320411, 20330401, 20340321, 20350311, 20360228, 20370216, 20380205, 20390126,
        20400115, 20410104, 20411224, 20421214, 20431203, 20441121, 20451110, 20461031,
        20471020, 20481009, 20490928, 20500917, 20510906, 20520826, 20530815, 20540805,
        20550725, 20560714, 20570703, 20580622, 20590611, 20600531, 20610520, 20620510,
        20630430, 20640418, 20650407, 20660327, 20670316, 20680305, 20690223, 20700212,
        20710201, 20720121, 20730109, 20731230, 20741219, 20751209, 20761127, 20771117
    };

    static {
        for (int index = 0; index < YEAR_STARTS.length; index++) {
            int packed = YEAR_STARTS[index];
            YEAR_STARTS[index] = (int) NativeCalendarMath.ordinal(packed / 10000, packed / 100 % 100, packed % 100);
        }
    }
}
