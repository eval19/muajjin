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
}
