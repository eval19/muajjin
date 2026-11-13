package org.muslimtechnician.muajjin;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.*;

/** Numerical contract of the two approved progress-fill palettes. */
public class WidgetProgressColorTest {
    private static final long START = 1800000000000L;
    /** One 1/11 segment; the whole interval is exactly divisible by 11. */
    private static final long SEGMENT = 600000L;
    private static final long DURATION = SEGMENT * 11;

    private static final int[] LIGHT = {
        0xFF369679, 0xFF369679, 0xFF369679, 0xFF369679, 0xFF369679, 0xFF258E52,
        0xFF4E8E16, 0xFFA67C06, 0xFFCC640B, 0xFFD9550E, 0xFFD9550E, 0xFFD9550E
    };
    private static final int[] DARK = {
        0xFF369679, 0xFF369679, 0xFF369679, 0xFF369679, 0xFF369679, 0xFF4ADE80,
        0xFFA3E635, 0xFFFACC15, 0xFFFBBF24, 0xFFFB923C, 0xFFFB923C, 0xFFF97316
    };

    /** Independent double-precision OKLab midpoints (segment, t = 0.5). */
    private static final int[] LIGHT_MID = {
        0, 0xFF369679, 0xFF369679, 0xFF369679, 0xFF2E9266, 0xFF3D8E3B,
        0xFF808610, 0xFFBA7109, 0xFFD35D0D, 0xFFD9550E, 0xFFD9550E, 0
    };
    private static final int[] DARK_MID = {
        0, 0xFF369679, 0xFF369679, 0xFF369679, 0xFF40B97E, 0xFF7DE263,
        0xFFD3DA28, 0xFFFBC61E, 0xFFFCA933, 0xFFFB923C, 0xFFFA832C, 0
    };
    private static final WidgetProgressColor.Palette LIGHT_PALETTE =
        WidgetProgressColor.prepare(LIGHT);
    private static final WidgetProgressColor.Palette DARK_PALETTE =
        WidgetProgressColor.prepare(DARK);

    private static int light(long now) {
        return WidgetProgressColor.sample(LIGHT_PALETTE, START, START + DURATION, now);
    }

    private static int dark(long now) {
        return WidgetProgressColor.sample(DARK_PALETTE, START, START + DURATION, now);
    }

    @Test public void exactAnchorsKeepOrderAlphaAndWarmOrangeEndpoints() {
        assertEquals(12, LIGHT_PALETTE.size());
        assertEquals(12, DARK_PALETTE.size());
        for (int i = 0; i < 12; i++) {
            assertEquals("light anchor " + i, LIGHT[i], light(START + i * SEGMENT));
            assertEquals("dark anchor " + i, DARK[i], dark(START + i * SEGMENT));
        }
        assertEquals("The approved dark endpoint is exact", 0xFFF97316, dark(START + DURATION));
        assertEquals("The approved light endpoint is exact", 0xFFD9550E, light(START + DURATION));
        assertEquals(0xFF, dark(START) >>> 24);
    }

    @Test public void firstSegmentNeverLeavesTheAppGreen() {
        long[] plateau = {START, START + 1, START + SEGMENT / 2, START + SEGMENT - 1, START + SEGMENT};
        for (long now : plateau) {
            assertEquals("light plateau", 0xFF369679, light(now));
            assertEquals("dark plateau", 0xFF369679, dark(now));
        }
    }

    @Test public void lightGreenHoldRunsToFourOfElevenThenWarmOrange() {
        for (long now : new long[]{START, START + 2 * SEGMENT, START + 4 * SEGMENT - 1,
                START + 4 * SEGMENT}) {
            assertEquals("Light app green must hold through 4/11 of the interval",
                0xFF369679, light(now));
        }
        assertTrue("Light green must begin transitioning after its hold",
            light(START + 4 * SEGMENT + SEGMENT / 2) != 0xFF369679);
        assertEquals("Warm orange must be exact at 9/11", 0xFFD9550E,
            light(START + 9 * SEGMENT));
        assertEquals("Warm orange must hold to the end", 0xFFD9550E,
            light(START + DURATION));
    }

    @Test public void darkGreenHoldMatchesTheLightThemeCheckpoints() {
        for (long now : new long[]{START, START + 2 * SEGMENT, START + 4 * SEGMENT}) {
            assertEquals("Dark app green must hold through 4/11",
                0xFF369679, dark(now));
        }
        assertTrue("Dark must leave its green hold after 4/11",
            dark(START + 4 * SEGMENT + SEGMENT / 2) != 0xFF369679);
        assertEquals("Dark lime at 6/11", 0xFFA3E635,
            dark(START + 6 * SEGMENT));
        assertEquals("Dark yellow at 7/11", 0xFFFACC15,
            dark(START + 7 * SEGMENT));
        assertEquals("Dark warm orange at 9/11", 0xFFFB923C,
            dark(START + 9 * SEGMENT));
        assertEquals("Dark orange endpoint at 11/11", 0xFFF97316,
            dark(START + DURATION));
    }

    @Test public void interiorSamplesMatchIndependentOklabReferences() {
        for (int segment = 1; segment <= 10; segment++) {
            long now = START + segment * SEGMENT + SEGMENT / 2;
            assertColorWithin("light segment " + segment, LIGHT_MID[segment], light(now), 1);
            assertColorWithin("dark segment " + segment, DARK_MID[segment], dark(now), 1);
        }
    }

    @Test public void interiorAnchorTransitionsStayWithinOneChannel() {
        for (int anchor = 1; anchor <= 10; anchor++) {
            long at = START + anchor * SEGMENT;
            assertColorWithin("light before " + anchor, light(at), light(at - 1), 1);
            assertColorWithin("light after " + anchor, light(at), light(at + 1), 1);
            assertColorWithin("dark before " + anchor, dark(at), dark(at - 1), 1);
            assertColorWithin("dark after " + anchor, dark(at), dark(at + 1), 1);
        }
    }

    @Test public void endpointsAndDegenerateIntervalsNeverIndexPastThePalette() {
        assertEquals(LIGHT[0], light(START - 1));
        assertEquals(LIGHT[0], light(START));
        assertEquals(LIGHT[11], light(START + DURATION));
        assertEquals(LIGHT[11], light(START + DURATION + 1));
        assertEquals(DARK[11], dark(START + DURATION + 86_400_000L));
        assertEquals(LIGHT[0], WidgetProgressColor.sample(LIGHT_PALETTE, START, START, START));
        assertEquals(LIGHT[0], WidgetProgressColor.sample(LIGHT_PALETTE, START, START - 3_600_000L, START));
        assertEquals(LIGHT[0], WidgetProgressColor.sample(LIGHT_PALETTE, START, START - 1L,
            START - 7_200_000L));
        assertEquals(DARK[11], WidgetProgressColor.sample(DARK_PALETTE, START, START + 1L,
            Long.MAX_VALUE));
    }

    @Test public void oklabConversionMatchesPublishedAndIndependentReferences() {
        // Ottosson's reference values: pure red is (0.62796, 0.22486, 0.12585);
        // white is (1, 0, 0).
        assertLab(0xFFFF0000, 0.62795536, 0.22486306, 0.12584630);
        assertLab(0xFFFFFFFF, 1.0, 0.0, 0.0);
    }

    @Test public void everyDistinctPaletteColorRoundTripsWithinOneChannel() {
        for (int color : distinct(LIGHT)) assertRoundTrip(color);
        for (int color : distinct(DARK)) assertRoundTrip(color);
    }

    @Test public void samplingIsDeterministicIndependentOfOrderAndPriorSamples() {
        long[] moments = {START, START + SEGMENT, START + DURATION / 3, START + DURATION - 1};
        int[] forward = new int[moments.length];
        for (int i = 0; i < moments.length; i++) forward[i] = light(moments[i]);
        int[] backward = new int[moments.length];
        for (int i = moments.length - 1; i >= 0; i--) backward[i] = light(moments[i]);
        assertArrayEquals(forward, backward);
        // Interleaved dark sampling must not alter light results.
        for (int i = 0; i < moments.length; i++) dark(moments[i]);
        for (int i = 0; i < moments.length; i++)
            assertEquals("light sample " + i, forward[i], light(moments[i]));
    }

    @Test public void theSameFractionMapsAlikeForEveryIntervalLength() {
        long[] durations = {90 * 60_000L, 3 * 3_600_000L, 10 * 3_600_000L, DURATION};
        double[] fractions = {1.0 / 11, 1.5 / 11, 6.0 / 11, 10.5 / 11};
        for (double fraction : fractions) {
            long reference = START + Math.round(DURATION * fraction);
            int lightExpected = light(reference);
            int darkExpected = dark(reference);
            for (long duration : durations) {
                long now = START + Math.round(duration * fraction);
                assertColorWithin("light " + duration + "ms at " + fraction, lightExpected,
                    WidgetProgressColor.sample(LIGHT_PALETTE, START, START + duration, now), 1);
                assertColorWithin("dark " + duration + "ms at " + fraction, darkExpected,
                    WidgetProgressColor.sample(DARK_PALETTE, START, START + duration, now), 1);
            }
        }
    }

    @Test public void overnightIntervalsKeepTheFractionalMappingWithoutCaps() {
        long midnight = 1_798_761_600_000L;
        long start = midnight - 3_600_000L;
        long target = midnight + 6 * 3_600_000L;
        assertEquals(7 * 3_600_000L, target - start);
        int overnight = WidgetProgressColor.sample(DARK_PALETTE, start, target,
            start + 7 * 3_600_000L / 2);
        int daytime = WidgetProgressColor.sample(DARK_PALETTE, START, START + 7 * 3_600_000L,
            START + 7 * 3_600_000L / 2);
        assertEquals("Midnight crossing must not change the mapping", daytime, overnight);
        assertColorWithin("Overnight midpoint", DARK_MID[5], overnight, 1);
    }

    @Test public void aFreshIntervalStartsGreenAndBackwardSamplesDropUrgency() {
        int urgent = dark(START + DURATION - 1);
        assertColorWithin("Just before the target", DARK[11], urgent, 1);
        long secondStart = START + DURATION;
        assertEquals(DARK[0], WidgetProgressColor.sample(DARK_PALETTE, secondStart,
            secondStart + DURATION, secondStart));
        int earlier = dark(START + SEGMENT / 4);
        assertEquals(DARK[0], earlier);
        assertTrue("Backward sampling retained the urgent color", urgent != earlier);
    }

    @Test public void sampledChannelsStayOpaqueAndBounded() {
        for (int theme = 0; theme < 2; theme++) {
            WidgetProgressColor.Palette palette = theme == 0 ? LIGHT_PALETTE : DARK_PALETTE;
            int[] expected = theme == 0 ? LIGHT : DARK;
            for (int step = 0; step <= 1100; step++) {
                long now = START + Math.round(DURATION * (step / 1100.0));
                int color = WidgetProgressColor.sample(palette, START, START + DURATION, now);
                assertEquals("alpha at step " + step, 0xFF, color >>> 24);
            }
            assertColorWithin("final saturated transition", expected[11],
                WidgetProgressColor.sample(palette, START, START + DURATION, START + DURATION - 1), 1);
        }
    }

    @Test public void preparedPalettesOwnTheirInputAndStayIndependent() {
        int[] source = LIGHT.clone();
        WidgetProgressColor.Palette owned = WidgetProgressColor.prepare(source);
        long probe = START + 7 * SEGMENT + SEGMENT / 2;
        int before = WidgetProgressColor.sample(owned, START, START + DURATION, probe);
        source[7] = 0xFF0000FF;
        assertEquals("Later caller mutation changed a prepared palette",
            before, WidgetProgressColor.sample(owned, START, START + DURATION, probe));

        int light = light(START + 6 * SEGMENT);
        int dark = dark(START + 6 * SEGMENT);
        assertEquals(0xFF4E8E16, light);
        assertEquals(0xFFA3E635, dark);
        assertTrue("Concurrent theme samples contaminated each other", light != dark);
    }

    @Test public void bothPalettesKeepThreeToOneTrackContrastAcrossTheInterval() {
        for (int theme = 0; theme < 2; theme++) {
            WidgetProgressColor.Palette palette = theme == 0 ? LIGHT_PALETTE : DARK_PALETTE;
            int track = theme == 0 ? 0xFFEBF0EE : 0xFF29352F;
            double minimum = Double.POSITIVE_INFINITY;
            for (int step = 0; step <= 11000; step++) {
                int fill = WidgetProgressColor.sample(palette, 0L, 11000L, step);
                double a = relativeLuminance(fill), b = relativeLuminance(track);
                double ratio = (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
                minimum = Math.min(minimum, ratio);
            }
            assertTrue("Theme " + theme + " minimum fill/track contrast was " + minimum,
                minimum >= 3.0);
        }
    }

    private static double relativeLuminance(int color) {
        return 0.2126 * linearChannel(color >> 16)
            + 0.7152 * linearChannel(color >> 8)
            + 0.0722 * linearChannel(color);
    }

    private static double linearChannel(int raw) {
        double value = (raw & 0xFF) / 255.0;
        return value <= 0.04045 ? value / 12.92
            : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static void assertLab(int argb, double lightness, double a, double b) {
        double[] lab = WidgetProgressColor.toOklab(argb);
        assertEquals("L " + Integer.toHexString(argb), lightness, lab[0], 1e-5);
        assertEquals("a " + Integer.toHexString(argb), a, lab[1], 1e-5);
        assertEquals("b " + Integer.toHexString(argb), b, lab[2], 1e-5);
    }

    private static void assertRoundTrip(int argb) {
        double[] lab = WidgetProgressColor.toOklab(argb);
        int roundTrip = WidgetProgressColor.fromOklab(lab[0], lab[1], lab[2]);
        assertColorWithin("round trip " + Integer.toHexString(argb), argb, roundTrip, 1);
    }

    private static void assertColorWithin(String message, int expected, int actual, int tolerance) {
        for (int shift : new int[]{16, 8, 0}) {
            int delta = Math.abs(((expected >> shift) & 0xFF) - ((actual >> shift) & 0xFF));
            assertTrue(message + " channel " + shift + " expected "
                    + Integer.toHexString(expected) + " but was " + Integer.toHexString(actual),
                delta <= tolerance);
        }
    }

    private static Set<Integer> distinct(int[] colors) {
        Set<Integer> unique = new LinkedHashSet<>();
        for (int color : colors) unique.add(color);
        return unique;
    }
}
