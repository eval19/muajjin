package org.muslimtechnician.muajjin;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

public class PrayerWidgetSmallProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            PrayerWidgetHelper.updateSquareWidget(context, appWidgetManager, appWidgetId);
        }
        PrayerWidgetHelper.scheduleNextAlarm(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        PrayerWidgetHelper.updateSquareWidget(context, appWidgetManager, appWidgetId);
    }
}
