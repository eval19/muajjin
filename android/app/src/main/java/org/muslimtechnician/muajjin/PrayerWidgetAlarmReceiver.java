package org.muslimtechnician.muajjin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PrayerWidgetAlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_PRAYER_TRANSITION = "org.muslimtechnician.muajjin.ACTION_PRAYER_TRANSITION";
    public static final String ACTION_BAR_PROGRESS_REFRESH = "org.muslimtechnician.muajjin.ACTION_BAR_PROGRESS_REFRESH";

    @Override
    public void onReceive(Context context, Intent intent) {
        PrayerWidgetHelper.refreshStateFromTimestamps(context);
        PrayerWidgetHelper.updateAllWidgets(context);
        PrayerWidgetHelper.scheduleNextAlarm(context);
    }
}
