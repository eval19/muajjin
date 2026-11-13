package org.muslimtechnician.muajjin;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrayerWidgetAlarmReceiverTest {
    @Test public void transitionsFallbacksAndReconciliationForceARearm() {
        assertTrue(PrayerWidgetAlarmReceiver.mustForceReschedule(true, false, false));
        assertTrue(PrayerWidgetAlarmReceiver.mustForceReschedule(false, true, false));
        assertTrue(PrayerWidgetAlarmReceiver.mustForceReschedule(false, false, true));
        assertFalse(PrayerWidgetAlarmReceiver.mustForceReschedule(false, false, false));
    }

    @Test public void subSecondEarlyTransitionWaitsPastBoundaryWithoutBlockingLongWindows() {
        long target = 1_800_000_000_000L;
        assertTrue(PrayerWidgetAlarmReceiver.earlyTransitionWaitMs(target, target - 20L) == 20L + PrayerWidgetHelper.TRANSITION_DELIVERY_LEEWAY_MS);
        assertTrue(PrayerWidgetAlarmReceiver.earlyTransitionWaitMs(target, target) == 0L);
        assertTrue(PrayerWidgetAlarmReceiver.earlyTransitionWaitMs(target, target + 50L) == 0L);
        assertTrue(PrayerWidgetAlarmReceiver.earlyTransitionWaitMs(target, target - 5000L) == 0L);
        assertTrue(PrayerWidgetAlarmReceiver.earlyTransitionWaitMs(0L, target) == 0L);
    }
}
