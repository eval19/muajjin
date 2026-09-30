package org.muslimtechnician.muajjin;

import android.content.Context;
import android.content.SharedPreferences;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "PrayerWidget")
public class PrayerWidgetPlugin extends Plugin {

    @PluginMethod
    public void updateWidgetData(PluginCall call) {
        Context context = getContext();
        if (context == null) {
            call.reject("Context is null");
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PrayerWidgetHelper.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String statusLabel = call.getString("statusLabel", "NEXT SALAT");
        String currentName = call.getString("currentName", "Dhuhr");
        String currentTime = call.getString("currentTime", "--:--");
        String nextTitle = call.getString("nextTitle", "Starts in");
        String nextName = call.getString("nextName", "Asr");
        String timeRemaining = call.getString("timeRemaining", "");
        Integer progressPercent = call.getInt("progressPercent", 0);
        Long targetTimestamp = null;
        try {
            Double d = call.getDouble("targetTimestamp");
            if (d != null) {
                targetTimestamp = d.longValue();
            }
        } catch (Exception ignored) {}

        String locationName = call.getString("locationName", "RIYADH GOVERNORATE");
        String hijriDate = call.getString("hijriDate", "");
        String activePrayerId = call.getString("activePrayerId", "dhuhr");

        editor.putString("status_label", statusLabel);
        editor.putString("current_name", currentName);
        editor.putString("current_time", currentTime);
        editor.putString("next_title", nextTitle);
        editor.putString("next_name", nextName);
        editor.putString("time_remaining", timeRemaining);
        editor.putInt("progress_percent", progressPercent != null ? progressPercent : 0);
        if (targetTimestamp != null) {
            editor.putLong("target_timestamp", targetTimestamp);
        }
        editor.putString("location_name", locationName);
        editor.putString("hijri_date", hijriDate);
        editor.putString("active_prayer_id", activePrayerId);

        JSObject prayers = call.getObject("prayers");
        if (prayers != null) {
            editor.putString("fajr_time", prayers.optString("fajr", "--:--"));
            editor.putString("sunrise_time", prayers.optString("sunrise", "--:--"));
            editor.putString("dhuhr_time", prayers.optString("dhuhr", "--:--"));
            editor.putString("asr_time", prayers.optString("asr", "--:--"));
            editor.putString("maghrib_time", prayers.optString("maghrib", "--:--"));
            editor.putString("isha_time", prayers.optString("isha", "--:--"));
        }

        JSObject timestamps = call.getObject("prayerTimestamps");
        if (timestamps != null) {
            editor.putLong("ts_fajr", (long) timestamps.optDouble("fajr", 0));
            editor.putLong("ts_sunrise", (long) timestamps.optDouble("sunrise", 0));
            editor.putLong("ts_dhuhr", (long) timestamps.optDouble("dhuhr", 0));
            editor.putLong("ts_asr", (long) timestamps.optDouble("asr", 0));
            editor.putLong("ts_maghrib", (long) timestamps.optDouble("maghrib", 0));
            editor.putLong("ts_isha", (long) timestamps.optDouble("isha", 0));
            editor.putLong("ts_next_fajr", (long) timestamps.optDouble("nextFajr", 0));
        }

        editor.apply();

        // Trigger immediate update on all widgets on the launcher
        PrayerWidgetHelper.updateAllWidgets(context);

        // Schedule precise RTC alarm for the next prayer transition
        PrayerWidgetHelper.scheduleNextAlarm(context);

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }
}
