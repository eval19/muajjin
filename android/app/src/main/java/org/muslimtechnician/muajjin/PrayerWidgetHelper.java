package org.muslimtechnician.muajjin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

public class PrayerWidgetHelper {
    public static final String PREFS_NAME = "MuajjinPrayerData";

    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        // Update Small Widgets (2x2)
        ComponentName smallComponent = new ComponentName(context, PrayerWidgetSmallProvider.class);
        int[] smallIds = manager.getAppWidgetIds(smallComponent);
        if (smallIds != null && smallIds.length > 0) {
            for (int id : smallIds) {
                updateSmallWidget(context, manager, id);
            }
        }

        // Update Medium Widgets (4x2)
        ComponentName mediumComponent = new ComponentName(context, PrayerWidgetMediumProvider.class);
        int[] mediumIds = manager.getAppWidgetIds(mediumComponent);
        if (mediumIds != null && mediumIds.length > 0) {
            for (int id : mediumIds) {
                updateMediumWidget(context, manager, id);
            }
        }

        // Update Large Widgets (4x3)
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

        String scheduleJson = prefs.getString("prayer_schedule_json", null);
        if (scheduleJson != null && !scheduleJson.isEmpty()) {
            if (processScheduleJson(prefs, scheduleJson, now)) {
                return;
            }
        }

        // Fallback to single-day timestamps if multi-day cache not yet loaded
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
        String currentPill;
        String nextName;
        String nextTime;
        String activePrayerId;

        if (now < tsFajr) {
            currentPill = "● Isha · " + prefs.getString("isha_time", "--:--");
            nextName = "Fajr";
            nextTime = prefs.getString("fajr_time", "--:--");
            targetTimestamp = tsFajr;
            startTimestamp = tsIsha > 0 ? tsIsha : (tsFajr - 8 * 3600 * 1000L);
            activePrayerId = "isha";
        } else if (now < tsSunrise) {
            currentPill = "● Fajr · " + prefs.getString("fajr_time", "--:--");
            nextName = "Sunrise";
            nextTime = prefs.getString("sunrise_time", "--:--");
            targetTimestamp = tsSunrise;
            startTimestamp = tsFajr;
            activePrayerId = "fajr";
        } else if (now < tsDhuhr) {
            currentPill = "☼ Sunrise · " + prefs.getString("sunrise_time", "--:--");
            nextName = "Dhuhr";
            nextTime = prefs.getString("dhuhr_time", "--:--");
            targetTimestamp = tsDhuhr;
            startTimestamp = tsSunrise;
            activePrayerId = "sunrise";
        } else if (now < tsAsr) {
            currentPill = "● Dhuhr · " + prefs.getString("dhuhr_time", "--:--");
            nextName = "Asr";
            nextTime = prefs.getString("asr_time", "--:--");
            targetTimestamp = tsAsr;
            startTimestamp = tsDhuhr;
            activePrayerId = "dhuhr";
        } else if (now < tsMaghrib) {
            currentPill = "● Asr · " + prefs.getString("asr_time", "--:--");
            nextName = "Maghrib";
            nextTime = prefs.getString("maghrib_time", "--:--");
            targetTimestamp = tsMaghrib;
            startTimestamp = tsAsr;
            activePrayerId = "asr";
        } else if (now < tsIsha) {
            currentPill = "● Maghrib · " + prefs.getString("maghrib_time", "--:--");
            nextName = "Isha";
            nextTime = prefs.getString("isha_time", "--:--");
            targetTimestamp = tsIsha;
            startTimestamp = tsMaghrib;
            activePrayerId = "maghrib";
        } else {
            currentPill = "● Isha · " + prefs.getString("isha_time", "--:--");
            nextName = "Fajr";
            nextTime = prefs.getString("fajr_time", "--:--");
            targetTimestamp = tsNextFajr > now ? tsNextFajr : (tsIsha + 8 * 3600 * 1000L);
            startTimestamp = tsIsha;
            activePrayerId = "isha";
        }

        editor.putString("current_pill", currentPill);
        editor.putString("next_name", nextName);
        editor.putString("next_time", nextTime);
        editor.putLong("target_timestamp", targetTimestamp);
        editor.putLong("start_timestamp", startTimestamp);
        editor.putString("active_prayer_id", activePrayerId);
        editor.apply();
    }

    private static boolean processScheduleJson(SharedPreferences prefs, String jsonStr, long now) {
        try {
            JSONArray days = new JSONArray(jsonStr);
            if (days.length() == 0) return false;

            // Find current matching day or interval
            for (int i = 0; i < days.length(); i++) {
                JSONObject day = days.getJSONObject(i);
                long fajr = day.getLong("fajr");
                long sunrise = day.getLong("sunrise");
                long dhuhr = day.getLong("dhuhr");
                long asr = day.getLong("asr");
                long maghrib = day.getLong("maghrib");
                long isha = day.getLong("isha");

                String fajrTime = day.getString("fajrTime");
                String sunriseTime = day.getString("sunriseTime");
                String dhuhrTime = day.getString("dhuhrTime");
                String asrTime = day.getString("asrTime");
                String maghribTime = day.getString("maghribTime");
                String ishaTime = day.getString("ishaTime");

                // Get next day for Isha rollover
                JSONObject nextDay = (i + 1 < days.length()) ? days.getJSONObject(i + 1) : null;
                long nextFajr = (nextDay != null) ? nextDay.getLong("fajr") : (isha + 8 * 3600 * 1000L);
                String nextFajrTime = (nextDay != null) ? nextDay.getString("fajrTime") : fajrTime;

                // Check if current time falls within this day (from Fajr to next Fajr)
                if (now >= fajr && now < nextFajr) {
                    SharedPreferences.Editor editor = prefs.edit();

                    // Update daily list for Large widget
                    editor.putString("fajr_time", fajrTime);
                    editor.putString("sunrise_time", sunriseTime);
                    editor.putString("dhuhr_time", dhuhrTime);
                    editor.putString("asr_time", asrTime);
                    editor.putString("maghrib_time", maghribTime);
                    editor.putString("isha_time", ishaTime);

                    String currentPill;
                    String nextName;
                    String nextTime;
                    long targetTimestamp;
                    long startTimestamp;
                    String activePrayerId;

                    if (now < sunrise) {
                        currentPill = "● Fajr · " + fajrTime;
                        nextName = "Sunrise";
                        nextTime = sunriseTime;
                        targetTimestamp = sunrise;
                        startTimestamp = fajr;
                        activePrayerId = "fajr";
                    } else if (now < dhuhr) {
                        currentPill = "☼ Sunrise · " + sunriseTime;
                        nextName = "Dhuhr";
                        nextTime = dhuhrTime;
                        targetTimestamp = dhuhr;
                        startTimestamp = sunrise;
                        activePrayerId = "sunrise";
                    } else if (now < asr) {
                        currentPill = "● Dhuhr · " + dhuhrTime;
                        nextName = "Asr";
                        nextTime = asrTime;
                        targetTimestamp = asr;
                        startTimestamp = dhuhr;
                        activePrayerId = "dhuhr";
                    } else if (now < maghrib) {
                        currentPill = "● Asr · " + asrTime;
                        nextName = "Maghrib";
                        nextTime = maghribTime;
                        targetTimestamp = maghrib;
                        startTimestamp = asr;
                        activePrayerId = "asr";
                    } else if (now < isha) {
                        currentPill = "● Maghrib · " + maghribTime;
                        nextName = "Isha";
                        nextTime = ishaTime;
                        targetTimestamp = isha;
                        startTimestamp = maghrib;
                        activePrayerId = "maghrib";
                    } else {
                        currentPill = "● Isha · " + ishaTime;
                        nextName = "Fajr";
                        nextTime = nextFajrTime;
                        targetTimestamp = nextFajr;
                        startTimestamp = isha;
                        activePrayerId = "isha";
                    }

                    editor.putString("current_pill", currentPill);
                    editor.putString("next_name", nextName);
                    editor.putString("next_time", nextTime);
                    editor.putLong("target_timestamp", targetTimestamp);
                    editor.putLong("start_timestamp", startTimestamp);
                    editor.putString("active_prayer_id", activePrayerId);
                    editor.apply();
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void scheduleNextAlarm(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long target = prefs.getLong("target_timestamp", 0);
        long now = System.currentTimeMillis();

        if (target <= now + 500) {
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

    private static void setupChronometer(RemoteViews views, int viewId, SharedPreferences prefs) {
        long target = prefs.getLong("target_timestamp", 0);
        long now = System.currentTimeMillis();
        long diff = target - now;

        if (diff > 0) {
            long base = SystemClock.elapsedRealtime() + diff;
            views.setChronometerCountDown(viewId, true);
            views.setChronometer(viewId, base, "in %s", true);
        } else {
            views.setChronometerCountDown(viewId, false);
            views.setTextViewText(viewId, "Now");
        }
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

        String currentPill = prefs.getString("current_pill", "● Dhuhr · 11:43 AM");
        String nextName = prefs.getString("next_name", "Asr");
        String nextTime = prefs.getString("next_time", "03:06 PM");
        int progress = getCalculatedProgress(prefs);

        views.setTextViewText(R.id.tv_prayer_current, currentPill);
        views.setTextViewText(R.id.tv_prayer_status, "NEXT SALAT");
        views.setTextViewText(R.id.tv_prayer_name, nextName);
        views.setTextViewText(R.id.tv_prayer_time, nextTime);
        setupChronometer(views, R.id.tv_time_remaining, prefs);
        views.setProgressBar(R.id.pb_prayer_progress, 100, Math.min(100, Math.max(0, progress)), false);

        views.setOnClickPendingIntent(R.id.widget_small_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    public static void updateMediumWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_medium);

        String location = prefs.getString("location_name", "RIYADH GOVERNORATE");
        String hijri = prefs.getString("hijri_date", "");
        String currentPill = prefs.getString("current_pill", "● NOW: DHUHR · 11:43 AM");
        String nextName = prefs.getString("next_name", "Asr");
        String nextTime = prefs.getString("next_time", "03:06 PM");
        int progress = getCalculatedProgress(prefs);

        views.setTextViewText(R.id.tv_medium_location, location);
        views.setTextViewText(R.id.tv_medium_hijri, hijri);
        views.setTextViewText(R.id.tv_medium_current_pill, currentPill);
        views.setTextViewText(R.id.tv_medium_next_name, nextName);
        views.setTextViewText(R.id.tv_medium_next_time, nextTime);
        setupChronometer(views, R.id.tv_medium_time_remaining, prefs);
        views.setProgressBar(R.id.pb_medium_progress, 100, Math.min(100, Math.max(0, progress)), false);

        views.setOnClickPendingIntent(R.id.widget_medium_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    public static void updateLargeWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_large);

        String location = prefs.getString("location_name", "RIYADH GOVERNORATE");
        String hijri = prefs.getString("hijri_date", "");
        String currentPill = prefs.getString("current_pill", "● NOW: DHUHR · 11:43 AM");
        String nextName = prefs.getString("next_name", "Asr");
        String nextTime = prefs.getString("next_time", "03:06 PM");
        int progress = getCalculatedProgress(prefs);
        String activeId = prefs.getString("active_prayer_id", "dhuhr");

        views.setTextViewText(R.id.tv_large_location, location);
        views.setTextViewText(R.id.tv_large_hijri, hijri);
        views.setTextViewText(R.id.tv_large_current_pill, currentPill);
        views.setTextViewText(R.id.tv_large_summary, "NEXT: " + nextName.toUpperCase() + " · " + nextTime);
        setupChronometer(views, R.id.tv_large_countdown, prefs);
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
        views.setInt(R.id.row_sunrise, "setBackgroundResource", "sunrise".equalsIgnoreCase(activeId) || "shuruq".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_dhuhr, "setBackgroundResource", "dhuhr".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_asr, "setBackgroundResource", "asr".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_maghrib, "setBackgroundResource", "maghrib".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_isha, "setBackgroundResource", "isha".equalsIgnoreCase(activeId) ? R.drawable.widget_active_row : 0);

        views.setOnClickPendingIntent(R.id.widget_large_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }
}
