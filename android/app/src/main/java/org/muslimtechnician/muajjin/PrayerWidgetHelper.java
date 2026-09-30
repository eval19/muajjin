package org.muslimtechnician.muajjin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrayerWidgetHelper {
    public static final String PREFS_NAME = "MuajjinPrayerData";

    public static class WidgetTimelineState {
        public String label = "Asr in";
        public String nextName = "Asr";
        public String nextTime = "15:12";
        public String footerText = "Now: Dhuhr";
        public String contextLine2 = "Asr at 15:12";
        public long targetTimestamp = 0;
        public long startTimestamp = 0;
        public boolean isWarm = false;
        public int progress = 500;
        public String activePrayerId = "dhuhr";

        public long fajrTs, sunriseTs, dhuhrTs, asrTs, maghribTs, ishaTs;
        public String fajrStr = "04:35";
        public String sunriseStr = "05:52";
        public String dhuhrStr = "11:47";
        public String asrStr = "15:12";
        public String maghribStr = "17:42";
        public String ishaStr = "19:12";
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        // 1. Strip Widgets (2x1)
        ComponentName stripComponent = new ComponentName(context, PrayerWidgetStripProvider.class);
        int[] stripIds = manager.getAppWidgetIds(stripComponent);
        if (stripIds != null && stripIds.length > 0) {
            for (int id : stripIds) {
                updateStripWidget(context, manager, id);
            }
        }

        // 2. Square Widgets (2x2)
        ComponentName squareComponent = new ComponentName(context, PrayerWidgetSmallProvider.class);
        int[] squareIds = manager.getAppWidgetIds(squareComponent);
        if (squareIds != null && squareIds.length > 0) {
            for (int id : squareIds) {
                updateSquareWidget(context, manager, id);
            }
        }

        // 3. Banner Widgets (4x1)
        ComponentName bannerComponent = new ComponentName(context, PrayerWidgetBannerProvider.class);
        int[] bannerIds = manager.getAppWidgetIds(bannerComponent);
        if (bannerIds != null && bannerIds.length > 0) {
            for (int id : bannerIds) {
                updateBannerWidget(context, manager, id);
            }
        }

        // 4. Wide Card Widgets (4x2)
        ComponentName wideComponent = new ComponentName(context, PrayerWidgetMediumProvider.class);
        int[] wideIds = manager.getAppWidgetIds(wideComponent);
        if (wideIds != null && wideIds.length > 0) {
            for (int id : wideIds) {
                updateWideWidget(context, manager, id);
            }
        }

        // 5. Full Table Widgets (4x4)
        ComponentName largeComponent = new ComponentName(context, PrayerWidgetLargeProvider.class);
        int[] largeIds = manager.getAppWidgetIds(largeComponent);
        if (largeIds != null && largeIds.length > 0) {
            for (int id : largeIds) {
                updateLargeWidget(context, manager, id);
            }
        }

        // 6. Lock Small Widgets (Keyguard)
        ComponentName lockSmallComponent = new ComponentName(context, PrayerLockSmallProvider.class);
        int[] lockSmallIds = manager.getAppWidgetIds(lockSmallComponent);
        if (lockSmallIds != null && lockSmallIds.length > 0) {
            for (int id : lockSmallIds) {
                updateLockSmallWidget(context, manager, id);
            }
        }

        // 7. Lock Medium Widgets (Keyguard)
        ComponentName lockMedComponent = new ComponentName(context, PrayerLockMediumProvider.class);
        int[] lockMedIds = manager.getAppWidgetIds(lockMedComponent);
        if (lockMedIds != null && lockMedIds.length > 0) {
            for (int id : lockMedIds) {
                updateLockMediumWidget(context, manager, id);
            }
        }
    }

    public static void refreshStateFromTimestamps(Context context) {
        // Reads cache and recalculates current timeline
        getTimelineState(context);
    }

    public static WidgetTimelineState getTimelineState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();

        WidgetTimelineState state = new WidgetTimelineState();

        // 1. Try 30-day cached schedule JSON
        String scheduleJson = prefs.getString("prayer_schedule_json", null);
        boolean parsed = false;
        if (scheduleJson != null && !scheduleJson.isEmpty()) {
            parsed = parseScheduleJson(context, prefs, scheduleJson, now, state);
        }

        // 2. Fallback to single-day stored timestamps
        if (!parsed) {
            parseSingleDayFallback(context, prefs, now, state);
        }

        // 3. Compute Progress (0 to 1000)
        long total = state.targetTimestamp - state.startTimestamp;
        long elapsed = now - state.startTimestamp;
        if (total > 0 && elapsed >= 0) {
            state.progress = (int) Math.min(1000, Math.max(0, (elapsed * 1000) / total));
        } else {
            state.progress = 500;
        }

        // 4. Compute Warm State: Last 15 min before window close (Sunrise, Asr, Maghrib, Isha, Fajr)
        // NOT Dhuhr's start after sunrise
        long diffMs = state.targetTimestamp - now;
        state.isWarm = (diffMs > 0 && diffMs <= 15 * 60 * 1000L && !"dhuhr".equalsIgnoreCase(state.nextName));

        return state;
    }

    private static boolean parseScheduleJson(Context context, SharedPreferences prefs, String jsonStr, long now, WidgetTimelineState state) {
        try {
            JSONArray days = new JSONArray(jsonStr);
            if (days.length() == 0) return false;

            for (int i = 0; i < days.length(); i++) {
                JSONObject day = days.getJSONObject(i);
                long fajr = day.getLong("fajr");
                long sunrise = day.getLong("sunrise");
                long dhuhr = day.getLong("dhuhr");
                long asr = day.getLong("asr");
                long maghrib = day.getLong("maghrib");
                long isha = day.getLong("isha");

                long nextFajr = (i + 1 < days.length()) ? days.getJSONObject(i + 1).getLong("fajr") : (isha + 8 * 3600 * 1000L);

                if (now < nextFajr) {
                    state.fajrTs = fajr;
                    state.sunriseTs = sunrise;
                    state.dhuhrTs = dhuhr;
                    state.asrTs = asr;
                    state.maghribTs = maghrib;
                    state.ishaTs = isha;

                    state.fajrStr = formatClockTime(context, fajr, day.optString("fajrTime", "04:35"));
                    state.sunriseStr = formatClockTime(context, sunrise, day.optString("sunriseTime", "05:52"));
                    state.dhuhrStr = formatClockTime(context, dhuhr, day.optString("dhuhrTime", "11:47"));
                    state.asrStr = formatClockTime(context, asr, day.optString("asrTime", "15:12"));
                    state.maghribStr = formatClockTime(context, maghrib, day.optString("maghribTime", "17:42"));
                    state.ishaStr = formatClockTime(context, isha, day.optString("ishaTime", "19:12"));

                    applyTimelinePeriod(context, now, fajr, sunrise, dhuhr, asr, maghrib, isha, nextFajr, state);
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static void parseSingleDayFallback(Context context, SharedPreferences prefs, long now, WidgetTimelineState state) {
        long fajr = prefs.getLong("ts_fajr", 0);
        long sunrise = prefs.getLong("ts_sunrise", 0);
        long dhuhr = prefs.getLong("ts_dhuhr", 0);
        long asr = prefs.getLong("ts_asr", 0);
        long maghrib = prefs.getLong("ts_maghrib", 0);
        long isha = prefs.getLong("ts_isha", 0);
        long nextFajr = prefs.getLong("ts_next_fajr", isha + 8 * 3600 * 1000L);

        state.fajrTs = fajr;
        state.sunriseTs = sunrise;
        state.dhuhrTs = dhuhr;
        state.asrTs = asr;
        state.maghribTs = maghrib;
        state.ishaTs = isha;

        state.fajrStr = formatClockTime(context, fajr, prefs.getString("fajr_time", "04:35"));
        state.sunriseStr = formatClockTime(context, sunrise, prefs.getString("sunrise_time", "05:52"));
        state.dhuhrStr = formatClockTime(context, dhuhr, prefs.getString("dhuhr_time", "11:47"));
        state.asrStr = formatClockTime(context, asr, prefs.getString("asr_time", "15:12"));
        state.maghribStr = formatClockTime(context, maghrib, prefs.getString("maghrib_time", "17:42"));
        state.ishaStr = formatClockTime(context, isha, prefs.getString("isha_time", "19:12"));

        applyTimelinePeriod(context, now, fajr, sunrise, dhuhr, asr, maghrib, isha, nextFajr, state);
    }

    private static void applyTimelinePeriod(Context context, long now, long fajr, long sunrise, long dhuhr, long asr, long maghrib, long isha, long nextFajr, WidgetTimelineState state) {
        if (now < fajr) {
            state.label = "Fajr in";
            state.nextName = "Fajr";
            state.targetTimestamp = fajr;
            state.startTimestamp = isha > 0 ? isha : (fajr - 8 * 3600 * 1000L);
            state.footerText = "Now: Isha";
            state.nextTime = state.fajrStr;
            state.activePrayerId = "isha";
        } else if (now < sunrise) {
            state.label = "Sunrise in";
            state.nextName = "Sunrise";
            state.targetTimestamp = sunrise;
            state.startTimestamp = fajr;
            state.footerText = "Now: Fajr";
            state.nextTime = state.sunriseStr;
            state.activePrayerId = "fajr";
        } else if (now < dhuhr) {
            state.label = "Dhuhr in";
            state.nextName = "Dhuhr";
            state.targetTimestamp = dhuhr;
            state.startTimestamp = sunrise;
            state.footerText = "After sunrise";
            state.nextTime = state.dhuhrStr;
            state.activePrayerId = ""; // No prayer open between sunrise and dhuhr
        } else if (now < asr) {
            state.label = "Asr in";
            state.nextName = "Asr";
            state.targetTimestamp = asr;
            state.startTimestamp = dhuhr;
            state.footerText = "Now: Dhuhr";
            state.nextTime = state.asrStr;
            state.activePrayerId = "dhuhr";
        } else if (now < maghrib) {
            state.label = "Maghrib in";
            state.nextName = "Maghrib";
            state.targetTimestamp = maghrib;
            state.startTimestamp = asr;
            state.footerText = "Now: Asr";
            state.nextTime = state.maghribStr;
            state.activePrayerId = "asr";
        } else if (now < isha) {
            state.label = "Isha in";
            state.nextName = "Isha";
            state.targetTimestamp = isha;
            state.startTimestamp = maghrib;
            state.footerText = "Now: Maghrib";
            state.nextTime = state.ishaStr;
            state.activePrayerId = "maghrib";
        } else {
            state.label = "Fajr in";
            state.nextName = "Fajr";
            state.targetTimestamp = nextFajr > now ? nextFajr : (isha + 8 * 3600 * 1000L);
            state.startTimestamp = isha;
            state.footerText = "Now: Isha";
            state.nextTime = formatClockTime(context, nextFajr, state.fajrStr);
            state.activePrayerId = "isha";
        }

        state.contextLine2 = state.nextName + " at " + state.nextTime;
    }

    private static String formatClockTime(Context context, long epochMs, String defaultStr) {
        if (epochMs <= 0) return defaultStr;
        try {
            boolean is24 = android.text.format.DateFormat.is24HourFormat(context);
            DateFormat df = new SimpleDateFormat(is24 ? "HH:mm" : "h:mm a", Locale.getDefault());
            return df.format(new Date(epochMs));
        } catch (Exception e) {
            return defaultStr;
        }
    }

    private static void setupChronometer(RemoteViews views, int viewId, long targetTimestamp) {
        long now = System.currentTimeMillis();
        long diff = targetTimestamp - now;
        if (diff > 0) {
            long base = SystemClock.elapsedRealtime() + diff;
            views.setChronometerCountDown(viewId, true);
            views.setChronometer(viewId, base, null, true);
        } else {
            views.setChronometerCountDown(viewId, false);
            views.setTextViewText(viewId, "0:00:00");
        }
    }

    private static boolean isHeightUnderThreshold(AppWidgetManager manager, int appWidgetId, int thresholdDp) {
        if (manager == null) return false;
        Bundle options = manager.getAppWidgetOptions(appWidgetId);
        if (options == null) return false;
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        return (minHeight > 0 && minHeight < thresholdDp);
    }

    private static PendingIntent getOpenAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // 1. Strip (2x1)
    public static void updateStripWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        WidgetTimelineState state = getTimelineState(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_strip);

        if (state.isWarm) {
            views.setViewVisibility(R.id.tv_strip_label, View.GONE);
            views.setViewVisibility(R.id.tv_strip_label_warm, View.VISIBLE);
            views.setTextViewText(R.id.tv_strip_label_warm, state.label);

            views.setViewVisibility(R.id.pb_strip_progress, View.GONE);
            views.setViewVisibility(R.id.pb_strip_progress_warm, View.VISIBLE);
            views.setProgressBar(R.id.pb_strip_progress_warm, 1000, state.progress, false);
        } else {
            views.setViewVisibility(R.id.tv_strip_label_warm, View.GONE);
            views.setViewVisibility(R.id.tv_strip_label, View.VISIBLE);
            views.setTextViewText(R.id.tv_strip_label, state.label);

            views.setViewVisibility(R.id.pb_strip_progress_warm, View.GONE);
            views.setViewVisibility(R.id.pb_strip_progress, View.VISIBLE);
            views.setProgressBar(R.id.pb_strip_progress, 1000, state.progress, false);
        }

        setupChronometer(views, R.id.tv_strip_countdown, state.targetTimestamp);
        views.setOnClickPendingIntent(R.id.widget_strip_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    // 2. Square (2x2)
    public static void updateSquareWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        if (isHeightUnderThreshold(manager, appWidgetId, 100)) {
            updateStripWidget(context, manager, appWidgetId);
            return;
        }

        WidgetTimelineState state = getTimelineState(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_square);

        if (state.isWarm) {
            views.setViewVisibility(R.id.tv_square_label, View.GONE);
            views.setViewVisibility(R.id.tv_square_label_warm, View.VISIBLE);
            views.setTextViewText(R.id.tv_square_label_warm, state.label);

            views.setViewVisibility(R.id.pb_square_progress, View.GONE);
            views.setViewVisibility(R.id.pb_square_progress_warm, View.VISIBLE);
            views.setProgressBar(R.id.pb_square_progress_warm, 1000, state.progress, false);
        } else {
            views.setViewVisibility(R.id.tv_square_label_warm, View.GONE);
            views.setViewVisibility(R.id.tv_square_label, View.VISIBLE);
            views.setTextViewText(R.id.tv_square_label, state.label);

            views.setViewVisibility(R.id.pb_square_progress_warm, View.GONE);
            views.setViewVisibility(R.id.pb_square_progress, View.VISIBLE);
            views.setProgressBar(R.id.pb_square_progress, 1000, state.progress, false);
        }

        setupChronometer(views, R.id.tv_square_countdown, state.targetTimestamp);
        views.setTextViewText(R.id.tv_square_footer_left, state.footerText);
        views.setTextViewText(R.id.tv_square_footer_right, state.nextTime);

        views.setOnClickPendingIntent(R.id.widget_square_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    // 3. Banner (4x1)
    public static void updateBannerWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        WidgetTimelineState state = getTimelineState(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_banner);

        if (state.isWarm) {
            views.setViewVisibility(R.id.tv_banner_label, View.GONE);
            views.setViewVisibility(R.id.tv_banner_label_warm, View.VISIBLE);
            views.setTextViewText(R.id.tv_banner_label_warm, state.label);

            views.setViewVisibility(R.id.pb_banner_progress, View.GONE);
            views.setViewVisibility(R.id.pb_banner_progress_warm, View.VISIBLE);
            views.setProgressBar(R.id.pb_banner_progress_warm, 1000, state.progress, false);
        } else {
            views.setViewVisibility(R.id.tv_banner_label_warm, View.GONE);
            views.setViewVisibility(R.id.tv_banner_label, View.VISIBLE);
            views.setTextViewText(R.id.tv_banner_label, state.label);

            views.setViewVisibility(R.id.pb_banner_progress_warm, View.GONE);
            views.setViewVisibility(R.id.pb_banner_progress, View.VISIBLE);
            views.setProgressBar(R.id.pb_banner_progress, 1000, state.progress, false);
        }

        setupChronometer(views, R.id.tv_banner_countdown, state.targetTimestamp);
        views.setTextViewText(R.id.tv_banner_footer_top, state.footerText);
        views.setTextViewText(R.id.tv_banner_footer_bottom, state.contextLine2);

        views.setOnClickPendingIntent(R.id.widget_banner_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    // 4. Wide Card (4x2)
    public static void updateWideWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        if (isHeightUnderThreshold(manager, appWidgetId, 100)) {
            updateBannerWidget(context, manager, appWidgetId);
            return;
        }

        WidgetTimelineState state = getTimelineState(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_wide);

        if (state.isWarm) {
            views.setViewVisibility(R.id.tv_wide_label, View.GONE);
            views.setViewVisibility(R.id.tv_wide_label_warm, View.VISIBLE);
            views.setTextViewText(R.id.tv_wide_label_warm, state.label);

            views.setViewVisibility(R.id.pb_wide_progress, View.GONE);
            views.setViewVisibility(R.id.pb_wide_progress_warm, View.VISIBLE);
            views.setProgressBar(R.id.pb_wide_progress_warm, 1000, state.progress, false);
        } else {
            views.setViewVisibility(R.id.tv_wide_label_warm, View.GONE);
            views.setViewVisibility(R.id.tv_wide_label, View.VISIBLE);
            views.setTextViewText(R.id.tv_wide_label, state.label);

            views.setViewVisibility(R.id.pb_wide_progress_warm, View.GONE);
            views.setViewVisibility(R.id.pb_wide_progress, View.VISIBLE);
            views.setProgressBar(R.id.pb_wide_progress, 1000, state.progress, false);
        }

        setupChronometer(views, R.id.tv_wide_countdown, state.targetTimestamp);
        views.setTextViewText(R.id.tv_wide_footer_top, state.footerText);
        views.setTextViewText(R.id.tv_wide_footer_bottom, state.contextLine2);

        // Render the 6 Chips
        renderChips(context, views, state);

        views.setOnClickPendingIntent(R.id.widget_wide_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    private static void renderChips(Context context, RemoteViews views, WidgetTimelineState state) {
        renderSingleChip(context, views, R.id.chip_fajr, R.id.tv_chip_fajr_name, R.id.tv_chip_fajr_time,
            "Fajr", state.fajrStr, state.fajrTs, state.startTimestamp, state.targetTimestamp, "fajr".equalsIgnoreCase(state.activePrayerId));

        renderSingleChip(context, views, R.id.chip_sunrise, R.id.tv_chip_sunrise_name, R.id.tv_chip_sunrise_time,
            "Sunrise", state.sunriseStr, state.sunriseTs, state.startTimestamp, state.targetTimestamp, false);

        renderSingleChip(context, views, R.id.chip_dhuhr, R.id.tv_chip_dhuhr_name, R.id.tv_chip_dhuhr_time,
            "Dhuhr", state.dhuhrStr, state.dhuhrTs, state.startTimestamp, state.targetTimestamp, "dhuhr".equalsIgnoreCase(state.activePrayerId));

        renderSingleChip(context, views, R.id.chip_asr, R.id.tv_chip_asr_name, R.id.tv_chip_asr_time,
            "Asr", state.asrStr, state.asrTs, state.startTimestamp, state.targetTimestamp, "asr".equalsIgnoreCase(state.activePrayerId));

        renderSingleChip(context, views, R.id.chip_maghrib, R.id.tv_chip_maghrib_name, R.id.tv_chip_maghrib_time,
            "Maghrib", state.maghribStr, state.maghribTs, state.startTimestamp, state.targetTimestamp, "maghrib".equalsIgnoreCase(state.activePrayerId));

        renderSingleChip(context, views, R.id.chip_isha, R.id.tv_chip_isha_name, R.id.tv_chip_isha_time,
            "Isha", state.ishaStr, state.ishaTs, state.startTimestamp, state.targetTimestamp, "isha".equalsIgnoreCase(state.activePrayerId));
    }

    private static void renderSingleChip(
        Context context,
        RemoteViews views,
        int chipId,
        int nameId,
        int timeId,
        String name,
        String timeStr,
        long eventTs,
        long currentWindowStartTs,
        long nextTargetTs,
        boolean isCurrentPrayer
    ) {
        views.setTextViewText(nameId, name);
        views.setTextViewText(timeId, timeStr);

        long now = System.currentTimeMillis();

        if (isCurrentPrayer) {
            views.setInt(chipId, "setBackgroundResource", R.drawable.widget_chip_current);
            views.setTextColor(nameId, context.getColor(R.color.w_accent));
            views.setTextColor(timeId, context.getColor(R.color.w_accent));
        } else if (eventTs == nextTargetTs || (eventTs > now && eventTs <= nextTargetTs + 1000)) {
            views.setInt(chipId, "setBackgroundResource", R.drawable.widget_chip_next);
            views.setTextColor(nameId, context.getColor(R.color.w_text_secondary));
            views.setTextColor(timeId, context.getColor(R.color.w_text_secondary));
        } else if (eventTs < now && eventTs < currentWindowStartTs) {
            views.setInt(chipId, "setBackgroundResource", R.drawable.widget_chip_transparent);
            views.setTextColor(nameId, context.getColor(R.color.w_text_muted));
            views.setTextColor(timeId, context.getColor(R.color.w_text_muted));
        } else {
            views.setInt(chipId, "setBackgroundResource", R.drawable.widget_chip_transparent);
            views.setTextColor(nameId, context.getColor(R.color.w_text_secondary));
            views.setTextColor(timeId, context.getColor(R.color.w_text_secondary));
        }
    }

    // 5. Preserved Full Schedule Table (4x4)
    public static void updateLargeWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        WidgetTimelineState state = getTimelineState(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_large);

        String location = prefs.getString("location_name", "RIYADH GOVERNORATE");
        String hijri = prefs.getString("hijri_date", "");

        views.setTextViewText(R.id.tv_large_location, location);
        views.setTextViewText(R.id.tv_large_hijri, hijri);
        views.setTextViewText(R.id.tv_large_current_pill, state.footerText);
        views.setTextViewText(R.id.tv_large_summary, state.nextName + " · " + state.nextTime);
        setupChronometer(views, R.id.tv_large_countdown, state.targetTimestamp);
        views.setProgressBar(R.id.pb_large_progress, 1000, state.progress, false);

        views.setTextViewText(R.id.tv_time_fajr, state.fajrStr);
        views.setTextViewText(R.id.tv_time_sunrise, state.sunriseStr);
        views.setTextViewText(R.id.tv_time_dhuhr, state.dhuhrStr);
        views.setTextViewText(R.id.tv_time_asr, state.asrStr);
        views.setTextViewText(R.id.tv_time_maghrib, state.maghribStr);
        views.setTextViewText(R.id.tv_time_isha, state.ishaStr);

        views.setInt(R.id.row_fajr, "setBackgroundResource", "fajr".equalsIgnoreCase(state.activePrayerId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_sunrise, "setBackgroundResource", "sunrise".equalsIgnoreCase(state.activePrayerId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_dhuhr, "setBackgroundResource", "dhuhr".equalsIgnoreCase(state.activePrayerId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_asr, "setBackgroundResource", "asr".equalsIgnoreCase(state.activePrayerId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_maghrib, "setBackgroundResource", "maghrib".equalsIgnoreCase(state.activePrayerId) ? R.drawable.widget_active_row : 0);
        views.setInt(R.id.row_isha, "setBackgroundResource", "isha".equalsIgnoreCase(state.activePrayerId) ? R.drawable.widget_active_row : 0);

        views.setOnClickPendingIntent(R.id.widget_large_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    // 6. Lock Small (Keyguard)
    public static void updateLockSmallWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        WidgetTimelineState state = getTimelineState(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_lock_small);

        views.setTextViewText(R.id.tv_lock_small_label, state.label);
        setupChronometer(views, R.id.tv_lock_small_countdown, state.targetTimestamp);
        views.setTextViewText(R.id.tv_lock_small_time, "at " + state.nextTime);

        views.setOnClickPendingIntent(R.id.widget_lock_small_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    // 7. Lock Medium (Keyguard)
    public static void updateLockMediumWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        WidgetTimelineState state = getTimelineState(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_lock_medium);

        views.setTextViewText(R.id.tv_lock_med_label, state.label);
        setupChronometer(views, R.id.tv_lock_med_countdown, state.targetTimestamp);
        views.setTextViewText(R.id.tv_lock_med_current, state.footerText);
        views.setTextViewText(R.id.tv_lock_med_next, state.contextLine2);

        views.setOnClickPendingIntent(R.id.widget_lock_medium_root, getOpenAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    public static void scheduleNextAlarm(Context context) {
        WidgetTimelineState state = getTimelineState(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        long now = System.currentTimeMillis();

        // 1. Exact boundary alarm (WAKEUP)
        if (state.targetTimestamp > now) {
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
                            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.targetTimestamp, pi);
                        } else {
                            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.targetTimestamp, pi);
                        }
                    } else {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.targetTimestamp, pi);
                    }
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, state.targetTimestamp, pi);
                }
            } catch (SecurityException se) {
                am.set(AlarmManager.RTC_WAKEUP, state.targetTimestamp, pi);
            }
        }

        // 2. Bar refresh & Warm state switch alarm (NON-WAKEUP AlarmManager.RTC)
        long windowLength = state.targetTimestamp - state.startTimestamp;
        if (windowLength <= 0) windowLength = 3600_000L;
        long interval = Math.max(60_000L, Math.min(300_000L, windowLength / 120));

        long nextBarTrigger = now + interval;
        long warmTrigger = state.targetTimestamp - 15 * 60 * 1000L;
        if (warmTrigger > now && warmTrigger < nextBarTrigger && !"dhuhr".equalsIgnoreCase(state.nextName)) {
            nextBarTrigger = warmTrigger;
        }

        if (nextBarTrigger < state.targetTimestamp) {
            Intent barIntent = new Intent(context, PrayerWidgetAlarmReceiver.class);
            barIntent.setAction(PrayerWidgetAlarmReceiver.ACTION_BAR_PROGRESS_REFRESH);
            PendingIntent barPi = PendingIntent.getBroadcast(
                context,
                1,
                barIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            am.set(AlarmManager.RTC, nextBarTrigger, barPi);
        }
    }
}
