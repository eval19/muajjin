package org.muslimtechnician.muajjin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PrayerWidgetAlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_PRAYER_TRANSITION = "org.muslimtechnician.muajjin.ACTION_PRAYER_TRANSITION";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Precise on-the-second wake up for prayer transition
        PrayerWidgetHelper.refreshStateFromTimestamps(context);
        PrayerWidgetHelper.updateAllWidgets(context);
        PrayerWidgetHelper.scheduleNextAlarm(context);
    }
}
