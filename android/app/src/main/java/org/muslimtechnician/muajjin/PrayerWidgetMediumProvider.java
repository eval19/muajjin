package org.muslimtechnician.muajjin;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

public class PrayerWidgetMediumProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        PrayerWidgetHelper.refreshStateFromTimestamps(context);
        for (int appWidgetId : appWidgetIds) {
            PrayerWidgetHelper.updateMediumWidget(context, appWidgetManager, appWidgetId);
        }
        PrayerWidgetHelper.scheduleNextAlarm(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        PrayerWidgetHelper.updateMediumWidget(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onEnabled(Context context) {
        PrayerWidgetHelper.refreshStateFromTimestamps(context);
        PrayerWidgetHelper.scheduleNextAlarm(context);
    }
}
