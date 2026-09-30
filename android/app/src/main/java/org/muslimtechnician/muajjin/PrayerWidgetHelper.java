package org.muslimtechnician.muajjin;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class PrayerWidgetHelper {
    public static final String PREFS_NAME = "MuajjinPrayerData";

    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        // Update Small Widgets
        ComponentName smallComponent = new ComponentName(context, PrayerWidgetSmallProvider.class);
        int[] smallIds = manager.getAppWidgetIds(smallComponent);
        if (smallIds != null && smallIds.length > 0) {
            for (int id : smallIds) {
                updateSmallWidget(context, manager, id);
            }
        }

        // Update Medium Widgets
        ComponentName mediumComponent = new ComponentName(context, PrayerWidgetMediumProvider.class);
        int[] mediumIds = manager.getAppWidgetIds(mediumComponent);
        if (mediumIds != null && mediumIds.length > 0) {
            for (int id : mediumIds) {
                updateMediumWidget(context, manager, id);
            }
        }

        // Update Large Widgets
        ComponentName largeComponent = new ComponentName(context, PrayerWidgetLargeProvider.class);
        int[] largeIds = manager.getAppWidgetIds(largeComponent);
        if (largeIds != null && largeIds.length > 0) {
            for (int id : largeIds) {
                updateLargeWidget(context, manager, id);
            }
        }
    }

    private static PendingIntent getOpenAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void updateSmallWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_small);

        String currentName = prefs.getString("current_name", "Dhuhr");
        String currentTime = prefs.getString("current_time", "12:30 PM");
        String timeRemaining = prefs.getString("time_remaining", "Next prayer soon");
        int progress = prefs.getInt("progress_percent", 0);

        views.setTextViewText(R.id.tv_prayer_name, currentName);
        views.setTextViewText(R.id.tv_prayer_time, currentTime);
        views.setTextViewText(R.id.tv_time_remaining, timeRemaining);
        views.setProgressBar(R.id.pb_prayer_progress, 100, Math.min(100, Math.max(0, progress)), false);

        views.setOnClickPendingIntent(R.id.widget_small_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    public static void updateMediumWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_medium);

        String location = prefs.getString("location_name", "Muajjin");
        String hijri = prefs.getString("hijri_date", "");
        String currentName = prefs.getString("current_name", "Dhuhr");
        String currentTime = prefs.getString("current_time", "12:30 PM");
        String nextName = prefs.getString("next_name", "Asr");
        String timeRemaining = prefs.getString("time_remaining", "in 45m");
        int progress = prefs.getInt("progress_percent", 0);

        views.setTextViewText(R.id.tv_medium_location, location);
        views.setTextViewText(R.id.tv_medium_hijri, hijri);
        views.setTextViewText(R.id.tv_medium_current_name, currentName);
        views.setTextViewText(R.id.tv_medium_current_time, currentTime);
        views.setTextViewText(R.id.tv_medium_next_title, "Next: " + nextName);
        views.setTextViewText(R.id.tv_medium_time_remaining, timeRemaining);
        views.setProgressBar(R.id.pb_medium_progress, 100, Math.min(100, Math.max(0, progress)), false);

        views.setOnClickPendingIntent(R.id.widget_medium_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    public static void updateLargeWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_large);

        String location = prefs.getString("location_name", "Muajjin");
        String hijri = prefs.getString("hijri_date", "");
        String currentName = prefs.getString("current_name", "Dhuhr");
        String currentTime = prefs.getString("current_time", "12:30 PM");
        String timeRemaining = prefs.getString("time_remaining", "Next prayer soon");
        int progress = prefs.getInt("progress_percent", 0);
        String activeId = prefs.getString("active_prayer_id", "dhuhr");

        views.setTextViewText(R.id.tv_large_location, location);
        views.setTextViewText(R.id.tv_large_hijri, hijri);
        views.setTextViewText(R.id.tv_large_summary, currentName + " · " + currentTime);
        views.setTextViewText(R.id.tv_large_countdown, timeRemaining);
        views.setProgressBar(R.id.pb_large_progress, 100, Math.min(100, Math.max(0, progress)), false);

        // Prayer times list
        views.setTextViewText(R.id.tv_time_fajr, prefs.getString("fajr_time", "--:--"));
        views.setTextViewText(R.id.tv_time_sunrise, prefs.getString("sunrise_time", "--:--"));
        views.setTextViewText(R.id.tv_time_dhuhr, prefs.getString("dhuhr_time", "--:--"));
        views.setTextViewText(R.id.tv_time_asr, prefs.getString("asr_time", "--:--"));
        views.setTextViewText(R.id.tv_time_maghrib, prefs.getString("maghrib_time", "--:--"));
        views.setTextViewText(R.id.tv_time_isha, prefs.getString("isha_time", "--:--"));

        // Highlight active row
        views.setInt(R.id.row_fajr, "setBackgroundResource", "fajr".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_dhuhr, "setBackgroundResource", "dhuhr".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_asr, "setBackgroundResource", "asr".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_maghrib, "setBackgroundResource", "maghrib".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_isha, "setBackgroundResource", "isha".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);

        views.setOnClickPendingIntent(R.id.widget_large_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }
}
