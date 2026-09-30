package org.muslimtechnician.muajjin;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class PrayerWidgetSmallProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            PrayerWidgetHelper.updateSmallWidget(context, appWidgetManager, appWidgetId);
        }
    }
}
