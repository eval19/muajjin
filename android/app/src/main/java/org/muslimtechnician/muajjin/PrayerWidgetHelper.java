package org.muslimtechnician.muajjin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.Locale;

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

    public static void refreshStateFromTimestamps(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();

        long tsFajr = prefs.getLong("ts_fajr", 0);
        long tsSunrise = prefs.getLong("ts_sunrise", 0);
        long tsDhuhr = prefs.getLong("ts_dhuhr", 0);
        long tsAsr = prefs.getLong("ts_asr", 0);
        long tsMaghrib = prefs.getLong("ts_maghrib", 0);
        long tsIsha = prefs.getLong("ts_isha", 0);
        long tsNextFajr = prefs.getLong("ts_next_fajr", 0);

        if (tsFajr == 0 || tsDhuhr == 0) return;

        SharedPreferences.Editor editor = prefs.edit();
        long targetTimestamp;
        long startTimestamp;
        String statusLabel;
        String currentName;
        String currentTime;
        String activePrayerId;

        if (now < tsFajr) {
            // Before Fajr (Night)
            statusLabel = "NEXT SALAT";
            currentName = "Fajr";
            currentTime = prefs.getString("fajr_time", "--:--");
            targetTimestamp = tsFajr;
            startTimestamp = tsIsha > 0 ? tsIsha : (tsFajr - 8 * 3600 * 1000L);
            activePrayerId = "fajr";
        } else if (now < tsSunrise) {
            // Fajr prayer time
            statusLabel = "CURRENT SALAT";
            currentName = "Fajr";
            currentTime = prefs.getString("fajr_time", "--:--");
            targetTimestamp = tsSunrise;
            startTimestamp = tsFajr;
            activePrayerId = "fajr";
        } else if (now < tsDhuhr) {
            // Duha / Ishraq: After Sunrise, before Dhuhr (Non-prayer time)
            statusLabel = "NEXT SALAT";
            currentName = "Dhuhr";
            currentTime = prefs.getString("dhuhr_time", "--:--");
            targetTimestamp = tsDhuhr;
            startTimestamp = tsSunrise;
            activePrayerId = "dhuhr";
        } else if (now < tsAsr) {
            // Dhuhr prayer time
            statusLabel = "CURRENT SALAT";
            currentName = "Dhuhr";
            currentTime = prefs.getString("dhuhr_time", "--:--");
            targetTimestamp = tsAsr;
            startTimestamp = tsDhuhr;
            activePrayerId = "dhuhr";
        } else if (now < tsMaghrib) {
            // Asr prayer time
            statusLabel = "CURRENT SALAT";
            currentName = "Asr";
            currentTime = prefs.getString("asr_time", "--:--");
            targetTimestamp = tsMaghrib;
            startTimestamp = tsAsr;
            activePrayerId = "asr";
        } else if (now < tsIsha) {
            // Maghrib prayer time
            statusLabel = "CURRENT SALAT";
            currentName = "Maghrib";
            currentTime = prefs.getString("maghrib_time", "--:--");
            targetTimestamp = tsIsha;
            startTimestamp = tsMaghrib;
            activePrayerId = "maghrib";
        } else {
            // Isha prayer time
            statusLabel = "CURRENT SALAT";
            currentName = "Isha";
            currentTime = prefs.getString("isha_time", "--:--");
            targetTimestamp = tsNextFajr > now ? tsNextFajr : (tsIsha + 8 * 3600 * 1000L);
            startTimestamp = tsIsha;
            activePrayerId = "isha";
        }

        editor.putString("status_label", statusLabel);
        editor.putString("current_name", currentName);
        editor.putString("current_time", currentTime);
        editor.putLong("target_timestamp", targetTimestamp);
        editor.putLong("start_timestamp", startTimestamp);
        editor.putString("active_prayer_id", activePrayerId);
        editor.apply();
    }

    public static void scheduleNextAlarm(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long target = prefs.getLong("target_timestamp", 0);
        long now = System.currentTimeMillis();

        if (target <= now) {
            refreshStateFromTimestamps(context);
            target = prefs.getLong("target_timestamp", 0);
        }

        if (target <= now) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, PrayerWidgetAlarmReceiver.class);
        intent.setAction(PrayerWidgetAlarmReceiver.ACTION_PRAYER_TRANSITION);
        PendingIntent pi = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pi);
                    } else {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pi);
                    }
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pi);
                }
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, target, pi);
            }
        } catch (SecurityException se) {
            am.set(AlarmManager.RTC_WAKEUP, target, pi);
        }
    }

    private static PendingIntent getOpenAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static String getFormattedCountdown(SharedPreferences prefs) {
        long target = prefs.getLong("target_timestamp", 0);
        if (target > 0) {
            long now = System.currentTimeMillis();
            long diff = target - now;
            if (diff > 0) {
                long totalMins = (diff + 59999) / (1000 * 60);
                long hours = totalMins / 60;
                long mins = totalMins % 60;
                if (hours > 0) {
                    return String.format(Locale.getDefault(), "in %dh %02dm", hours, mins);
                } else {
                    return String.format(Locale.getDefault(), "in %dm", Math.max(1, mins));
                }
            } else {
                return "Now";
            }
        }
        return prefs.getString("time_remaining", "Soon");
    }

    private static int getCalculatedProgress(SharedPreferences prefs) {
        long target = prefs.getLong("target_timestamp", 0);
        long start = prefs.getLong("start_timestamp", 0);
        if (target > start && start > 0) {
            long now = System.currentTimeMillis();
            long total = target - start;
            long elapsed = now - start;
            if (total > 0 && elapsed >= 0) {
                return (int) Math.min(100, Math.max(0, (elapsed * 100) / total));
            }
        }
        return prefs.getInt("progress_percent", 50);
    }

    public static void updateSmallWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_small);

        String statusLabel = prefs.getString("status_label", "NEXT SALAT");
        String currentName = prefs.getString("current_name", "Dhuhr");
        String currentTime = prefs.getString("current_time", "11:43 AM");
        String timeRemaining = getFormattedCountdown(prefs);
        int progress = getCalculatedProgress(prefs);

        views.setTextViewText(R.id.tv_prayer_status, statusLabel);
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

        String location = prefs.getString("location_name", "RIYADH GOVERNORATE");
        String hijri = prefs.getString("hijri_date", "");
        String statusLabel = prefs.getString("status_label", "NEXT SALAT");
        String currentName = prefs.getString("current_name", "Dhuhr");
        String currentTime = prefs.getString("current_time", "11:43 AM");
        String timeRemaining = getFormattedCountdown(prefs);
        int progress = getCalculatedProgress(prefs);

        views.setTextViewText(R.id.tv_medium_location, location);
        views.setTextViewText(R.id.tv_medium_hijri, hijri);
        views.setTextViewText(R.id.tv_medium_status, statusLabel);
        views.setTextViewText(R.id.tv_medium_current_name, currentName);
        views.setTextViewText(R.id.tv_medium_current_time, currentTime);
        views.setTextViewText(R.id.tv_medium_time_remaining, timeRemaining);
        views.setProgressBar(R.id.pb_medium_progress, 100, Math.min(100, Math.max(0, progress)), false);

        views.setOnClickPendingIntent(R.id.widget_medium_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    public static void updateLargeWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_large);

        String location = prefs.getString("location_name", "RIYADH GOVERNORATE");
        String hijri = prefs.getString("hijri_date", "");
        String statusLabel = prefs.getString("status_label", "NEXT SALAT");
        String currentName = prefs.getString("current_name", "Dhuhr");
        String currentTime = prefs.getString("current_time", "11:43 AM");
        String timeRemaining = getFormattedCountdown(prefs);
        int progress = getCalculatedProgress(prefs);
        String activeId = prefs.getString("active_prayer_id", "dhuhr");

        views.setTextViewText(R.id.tv_large_location, location);
        views.setTextViewText(R.id.tv_large_hijri, hijri);
        views.setTextViewText(R.id.tv_large_status, statusLabel);
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

        // Highlight active or upcoming row
        views.setInt(R.id.row_fajr, "setBackgroundResource", "fajr".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_sunrise, "setBackgroundResource", "sunrise".equalsIgnoreCase(activeId) || "shuruq".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_dhuhr, "setBackgroundResource", "dhuhr".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_asr, "setBackgroundResource", "asr".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_maghrib, "setBackgroundResource", "maghrib".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_isha, "setBackgroundResource", "isha".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);

        views.setOnClickPendingIntent(R.id.widget_large_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }
}
