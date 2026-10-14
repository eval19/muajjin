package org.muslimtechnician.muajjin;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class PrayerWidgetStripProvider extends AppWidgetProvider {
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        PendingResult result = goAsync();
        PrayerUpdateCoordinator.execute(result::finish);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        PrayerUpdateCoordinator.dispatch(context, true);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        PrayerUpdateCoordinator.dispatch(context, true);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        PrayerUpdateCoordinator.dispatch(context, true);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        PrayerUpdateCoordinator.dispatch(context, true);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        PrayerUpdateCoordinator.dispatch(context, true);
    }
}
