package org.muslimtechnician.muajjin;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class PrayerWidgetLargeProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        PrayerWidgetHelper.refreshStateFromTimestamps(context);
        for (int appWidgetId : appWidgetIds) {
            PrayerWidgetHelper.updateLargeWidget(context, appWidgetManager, appWidgetId);
        }
    }
}
