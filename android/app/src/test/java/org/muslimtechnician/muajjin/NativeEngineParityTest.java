package org.muslimtechnician.muajjin;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/** Exact epoch parity against independently generated, committed JavaScript results. */
public class NativeEngineParityTest {
    private static Path fixtures() {
        String configured = System.getProperty("muajjin.engineFixtures");
        if (configured != null) return Paths.get(configured);
        Path directory = Paths.get("").toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve("tests/native/engine-parity.jsonl");
            if (Files.isRegularFile(candidate)) return candidate;
            directory = directory.getParent();
        }
        throw new AssertionError("Missing committed tests/native/engine-parity.jsonl");
    }

    // The generator's JSONL rows are flat, with primitive fields and one integer
    // array. Read those actual results without Android's mocked org.json classes.
    private static String field(String row, String name) {
        Matcher matcher = Pattern.compile("\"" + name + "\":(\"[^\"]*\"|\\[[^\\]]*\\]|null|true|false|-?[0-9.]+)").matcher(row);
        if (!matcher.find()) throw new AssertionError("Missing fixture field " + name);
        String value = matcher.group(1);
        return value.startsWith("\"") ? value.substring(1, value.length() - 1) : value;
    }

    private static long[] numbers(String value) {
        String[] parts = value.substring(1, value.length() - 1).split(",");
        long[] result = new long[parts.length];
        for (int index = 0; index < parts.length; index++) result[index] = Long.parseLong(parts[index]);
        return result;
    }

    private static Calendar date(String text, String zone) {
        String[] parts = text.split("-");
        GregorianCalendar result = new GregorianCalendar(TimeZone.getTimeZone(zone));
        result.clear();
        result.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]), 12, 0, 0);
        result.setLenient(false);
        result.getTimeInMillis();
        return result;
    }

    @Test public void prayerEpochsMatchAdhan443AcrossEntireFixtureMatrix() throws IOException {
        int count = 0;
        int invalid = 0;
        Set<String> methods = new HashSet<>();
        Set<String> zones = new HashSet<>();
        for (String row : Files.readAllLines(fixtures(), StandardCharsets.UTF_8)) {
            if (!"prayer".equals(field(row, "kind"))) continue;
            String id = field(row, "id");
            String timezone = field(row, "timezone");
            Calendar calendar = date(field(row, "date"), timezone);
            long original = calendar.getTimeInMillis();
            int method = Integer.parseInt(field(row, "method"));
            int madhab = Integer.parseInt(field(row, "madhab"));
            double latitude = Double.parseDouble(field(row, "latitude"));
            double longitude = Double.parseDouble(field(row, "longitude"));
            String expected = field(row, "expected");
            if ("null".equals(expected)) {
                try {
                    NativePrayerCalculator.calculate(calendar, latitude, longitude, method, madhab);
                    fail("Invalid astronomical reference produced invented times: " + id);
                } catch (IllegalStateException astronomicalOutcome) {
                    assertTrue(id, astronomicalOutcome.getMessage().contains("astronomical"));
                }
                invalid++;
            } else {
                assertArrayEquals(id, numbers(expected), NativePrayerCalculator.calculate(calendar, latitude, longitude, method, madhab));
            }
            assertEquals("The calculator must not mutate its caller's Calendar: " + id, original, calendar.getTimeInMillis());
            methods.add(method + "/" + madhab);
            zones.add(timezone);
            count++;
        }
        assertTrue("The full real-reference matrix must remain committed", count >= 5000);
        assertEquals("All supported methods and both madhabs", 24, methods.size());
        assertTrue("Timezone/DST coverage", zones.size() >= 10);
        assertTrue("Polar failures must be exercised, not replaced by fixtures of fake times", invalid >= 100);
    }

    @Test public void hijriDatesMatchExactUpstreamTableWithDstSafeOrdinals() throws IOException {
        int count = 0;
        int outsideRange = 0;
        Set<String> zones = new HashSet<>();
        Set<Integer> years = new HashSet<>();
        for (String row : Files.readAllLines(fixtures(), StandardCharsets.UTF_8)) {
            if (!"hijri".equals(field(row, "kind"))) continue;
            String id = field(row, "id");
            String timezone = field(row, "timezone");
            Calendar calendar = date(field(row, "date"), timezone);
            long original = calendar.getTimeInMillis();
            int adjustment = Integer.parseInt(field(row, "adjustment"));
            boolean changeAtMaghrib = Boolean.parseBoolean(field(row, "changeAtMaghrib"));
            long now = Long.parseLong(field(row, "now"));
            long maghrib = Long.parseLong(field(row, "maghrib"));
            String expected = field(row, "expected");
            if ("null".equals(expected)) {
                try {
                    NativeHijriCalculator.calculate(calendar, adjustment, changeAtMaghrib, now, maghrib);
                    fail("Unsupported Hijri date must not be fabricated: " + id);
                } catch (IllegalArgumentException range) {
                    assertTrue(id, range.getMessage().contains("range"));
                }
                outsideRange++;
            } else {
                long[] reference = numbers(expected);
                assertArrayEquals(id, new int[] { (int) reference[0], (int) reference[1], (int) reference[2] }, NativeHijriCalculator.calculate(calendar, adjustment, changeAtMaghrib, now, maghrib));
                years.add((int) reference[2]);
            }
            assertEquals("Hijri calculation must not mutate caller: " + id, original, calendar.getTimeInMillis());
            zones.add(timezone);
            count++;
        }
        assertTrue("Month boundaries and preference cases must remain", count >= 20000);
        assertEquals("Every supported table year", 183, years.size());
        assertTrue("Local calendar dates in DST and non-DST zones", zones.size() >= 10);
        assertTrue("Before/after table bounds and rollover beyond maximum", outsideRange >= 20);
    }

    @Test public void invalidInputsAreRejectedRatherThanSubstitutingAnotherLocation() {
        Calendar date = date("2024-02-29", "UTC");
        for (double latitude : new double[] { Double.NaN, Double.POSITIVE_INFINITY, -90.01, 90.01 }) {
            assertThrows(IllegalArgumentException.class, () -> NativePrayerCalculator.calculate(date, latitude, 0, 1, 0));
        }
        for (double longitude : new double[] { Double.NaN, Double.NEGATIVE_INFINITY, -180.01, 180.01 }) {
            assertThrows(IllegalArgumentException.class, () -> NativePrayerCalculator.calculate(date, 0, longitude, 1, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> NativePrayerCalculator.calculate(date, 0, 0, 6, 0));
        assertThrows(IllegalArgumentException.class, () -> NativePrayerCalculator.calculate(date, 0, 0, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> NativePrayerCalculator.calculate(null, 0, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> NativeHijriCalculator.calculate(null, 0, false, 0, 0));
    }

    @Test public void calendarDateAndTimezoneAreExplicitNotGlobalDefaults() {
        Calendar selected = date("2024-03-10", "America/New_York");
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"));
            long[] first = NativePrayerCalculator.calculate(selected, 40.7128, -74.006, 11, 1);
            int[] hijri = NativeHijriCalculator.calculate(selected, 1, false, 0, 0);
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Riyadh"));
            assertArrayEquals(first, NativePrayerCalculator.calculate(selected, 40.7128, -74.006, 11, 1));
            assertArrayEquals(hijri, NativeHijriCalculator.calculate(selected, 1, false, 0, 0));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test public void methodAndMadhabStateCannotLeakBetweenCalls() {
        Calendar date = date("2024-06-21", "Europe/Oslo");
        long[] expected = NativePrayerCalculator.calculate(date, 59.9139, 10.7522, 1, 0);
        NativePrayerCalculator.calculate(date, 59.9139, 10.7522, 11, 1);
        NativePrayerCalculator.calculate(date, 59.9139, 10.7522, 7, 0);
        NativePrayerCalculator.calculate(date, 59.9139, 10.7522, 12, 1);
        assertArrayEquals(expected, NativePrayerCalculator.calculate(date, 59.9139, 10.7522, 1, 0));
        assertArrayEquals(NativePrayerCalculator.calculate(date, 59.9139, 10.7522, 12, 1), NativePrayerCalculator.calculate(date, 59.9139, 10.7522, 15, 1));
    }
}
