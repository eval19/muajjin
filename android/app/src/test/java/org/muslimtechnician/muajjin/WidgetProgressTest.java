package org.muslimtechnician.muajjin;

import org.junit.Test;
import static org.junit.Assert.*;

public class WidgetProgressTest {
    private static final long START = 1800000000000L;

    private WidgetProgress at(long duration, long remaining) {
        long target = START + duration;
        return WidgetProgress.at(true, START, target, target - remaining);
    }

    private void boundary(long duration, long remaining, WidgetProgress before, WidgetProgress after) {
        assertEquals("One millisecond before the threshold", before, at(duration, remaining + 1));
        assertEquals("Threshold equality belongs to the later stage", after, at(duration, remaining));
        assertEquals("One millisecond after the threshold", after, at(duration, remaining - 1));
    }

    @Test public void shortIntervalUsesRelativeThresholdsRatherThanWholeAbsoluteCaps() {
        long duration = 20 * 60000L;
        boundary(duration, 10 * 60000L, WidgetProgress.GREEN, WidgetProgress.GOLD);
        boundary(duration, 5 * 60000L, WidgetProgress.GOLD, WidgetProgress.AMBER);
        boundary(duration, 2 * 60000L, WidgetProgress.AMBER, WidgetProgress.SOFT_RED);
    }

    @Test public void oneHourIntervalCapsTheFinalStageAtFiveMinutes() {
        long duration = 60 * 60000L;
        boundary(duration, 30 * 60000L, WidgetProgress.GREEN, WidgetProgress.GOLD);
        boundary(duration, 15 * 60000L, WidgetProgress.GOLD, WidgetProgress.AMBER);
        boundary(duration, 5 * 60000L, WidgetProgress.AMBER, WidgetProgress.SOFT_RED);
    }

    @Test public void twoHourAndOvernightIntervalsDoNotRemainUrgentForAnHour() {
        for (long duration : new long[]{120 * 60000L, 600 * 60000L}) {
            boundary(duration, 30 * 60000L, WidgetProgress.GREEN, WidgetProgress.GOLD);
            boundary(duration, 15 * 60000L, WidgetProgress.GOLD, WidgetProgress.AMBER);
            boundary(duration, 5 * 60000L, WidgetProgress.AMBER, WidgetProgress.SOFT_RED);
        }
    }

    @Test public void fractionalMillisecondThresholdsUseIntegerDivisionConsistently() {
        boundary(600003L, 300001L, WidgetProgress.GREEN, WidgetProgress.GOLD);
        boundary(600003L, 150000L, WidgetProgress.GOLD, WidgetProgress.AMBER);
        boundary(600003L, 60000L, WidgetProgress.AMBER, WidgetProgress.SOFT_RED);
    }

    @Test public void invalidAndExpiredSamplesDoNotAdvertiseAnActiveStage() {
        assertEquals(WidgetProgress.UNAVAILABLE, WidgetProgress.at(false, START, START + 1000, START));
        assertEquals(WidgetProgress.UNAVAILABLE, WidgetProgress.at(true, START, START, START - 1));
        assertEquals(WidgetProgress.UNAVAILABLE, WidgetProgress.at(true, START, START - 1, START - 2));
        assertEquals(WidgetProgress.UNAVAILABLE, WidgetProgress.at(true, START, START + 1000, START + 1000));
        assertEquals(WidgetProgress.UNAVAILABLE, WidgetProgress.at(true, START, START + 1000, START + 1001));
    }

    @Test public void reschedulingAndClockCorrectionCannotRetainAStaleUrgentStage() {
        long target = START + 2 * 3600000L;
        long now = target - 120000L;
        assertEquals(WidgetProgress.SOFT_RED, WidgetProgress.at(true, START, target, now));
        assertEquals(WidgetProgress.GREEN, WidgetProgress.at(true, START, target + 3600000L, now));
        assertEquals(WidgetProgress.GREEN, WidgetProgress.at(true, START, target, now - 3600000L));
        assertEquals(WidgetProgress.GREEN, WidgetProgress.at(true, target, target + 3600000L, target));
    }

    @Test public void beforeStartSamplesRemainGreenWithoutFabricatingAnInterval() {
        assertEquals(WidgetProgress.GREEN, WidgetProgress.at(true, START, START + 3600000L, START - 1));
    }

    @Test public void exceptionallyShortIntervalsStillHaveDeterministicLiveStages() {
        assertEquals(WidgetProgress.GREEN, WidgetProgress.at(true, START, START + 1, START));
        assertEquals(WidgetProgress.GOLD, WidgetProgress.at(true, START, START + 2, START + 1));
    }
}
