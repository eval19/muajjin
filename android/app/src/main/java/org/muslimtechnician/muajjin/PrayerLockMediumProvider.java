package org.muslimtechnician.muajjin;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

public class PrayerLockMediumProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            PrayerWidgetHelper.updateLockMediumWidget(context, appWidgetManager, appWidgetId);
        }
        PrayerWidgetHelper.scheduleNextAlarm(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        PrayerWidgetHelper.updateLockMediumWidget(context, appWidgetManager, appWidgetId);
    }
}
