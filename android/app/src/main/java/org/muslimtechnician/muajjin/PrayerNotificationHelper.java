package org.muslimtechnician.muajjin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.BidiFormatter;
import android.util.Log;

import java.lang.reflect.Method;

public class PrayerNotificationHelper {
    public static final String CHANNEL_ID = "muajjin_live_update";
    public static final int NOTIFICATION_ID = 1001;

    private static final String TAG = "PrayerNotification";
    private static String lastTitle,lastContent,lastChip,lastDirection,lastLanguage,lastFormat,lastLocation;
    private static int lastProgress;
    private static boolean lastWarm,lastTimed;
    private static long lastTarget;
    private static void clearPostedState() {
        lastTitle=lastContent=lastChip=lastDirection=lastLanguage=lastFormat=lastLocation=null;
    }

    public static boolean isConsumerActive(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PrayerWidgetHelper.PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("live_update_enabled", true)) return false;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || !nm.areNotificationsEnabled()) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
            if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
        }
        return true;
    }

    public static void updateLiveUpdateNotification(
        Context context, PrayerWidgetHelper.WidgetTimelineState state, boolean force
    ) {
        if (!isConsumerActive(context)) {
            cancelLiveUpdateNotification(context);
            return;
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NativePresentation presentation = state.presentation;
        boolean timed = state.valid && state.targetTimestamp > 0;
        BidiFormatter bidi = BidiFormatter.getInstance("rtl".equals(presentation.direction));
        String title = bidi.unicodeWrap(timed
            ? presentation.label("next") + ": " + state.nextName + " "
                + presentation.label("at") + " " + presentation.clock(presentation.clockFormatter(context), state.targetTimestamp)
            : presentation.label("recovery"));
        String content = bidi.unicodeWrap(timed
            ? state.footerText + " · " + state.label
            : presentation.locationName);
        long remainingMinutes = timed ? Math.max(0L, state.targetTimestamp - state.now) / 60000L : 0L;
        String chipText = timed
            ? bidi.unicodeWrap(state.nextName + " " + presentation.remaining(remainingMinutes))
            : "";
        int progress = timed ? Math.max(0, Math.min(100, state.progress / 10)) : 0;
        if (!force && lastTitle != null && title.equals(lastTitle) && content.equals(lastContent)
            && chipText.equals(lastChip) && progress==lastProgress && state.isWarm==lastWarm
            && timed==lastTimed && state.targetTimestamp==lastTarget
            && presentation.direction.equals(lastDirection) && presentation.language.equals(lastLanguage)
            && presentation.timeFormat.equals(lastFormat) && presentation.locationName.equals(lastLocation)) return;

        try {
            ensureNotificationChannel(context, nm);
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
            builder.setSmallIcon(R.mipmap.ic_launcher)
                   .setContentTitle(title)
                   .setContentText(content)
                   .setContentIntent(pi)
                   .setOngoing(true)
                   .setOnlyAlertOnce(true)
                   .setShowWhen(timed)
                   .setUsesChronometer(timed)
                   .setVisibility(Notification.VISIBILITY_PUBLIC);
            if (timed) {
                builder.setWhen(state.targetTimestamp)
                       .setProgress(100, progress, false)
                       .setColor(state.isWarm ? 0xFFFF9800 : 0xFF4CAF50);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    builder.setChronometerCountDown(true);
                }
                applyAndroid16LiveUpdate(nm, builder, chipText);
            }
            nm.notify(NOTIFICATION_ID, builder.build());
            lastTitle=title;lastContent=content;lastChip=chipText;lastProgress=progress;
            lastWarm=state.isWarm;lastTimed=timed;lastTarget=state.targetTimestamp;
            lastDirection=presentation.direction;lastLanguage=presentation.language;
            lastFormat=presentation.timeFormat;lastLocation=presentation.locationName;
        } catch (SecurityException error) {
            clearPostedState();
            if (!isConsumerActive(context)) {
                cancelLiveUpdateNotification(context);
            } else {
                Log.e(TAG, "Notification posting rejected despite an enabled consumer", error);
            }
        } catch (RuntimeException error) {
            clearPostedState();
            Log.e(TAG, "Failed to render or post prayer notification", error);
        }
    }

    public static void cancelLiveUpdateNotification(Context context) {
        clearPostedState();
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try {
                nm.cancel(NOTIFICATION_ID);
            } catch (RuntimeException error) {
                Log.e(TAG, "Failed to cancel prayer notification", error);
            }
        }
    }

    private static void ensureNotificationChannel(Context context, NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Prayer Live Updates",
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Shows live prayer countdown on your lock screen and Super Island");
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                channel.setShowBadge(false);
                channel.setSound(null, null);
                channel.enableVibration(false);
                nm.createNotificationChannel(channel);
            }
        }
    }

    private static void applyAndroid16LiveUpdate(NotificationManager nm, Notification.Builder builder, String chipText) {
        if (Build.VERSION.SDK_INT < 35) return; // Android 15/16+

        try {
            boolean canPromote = true;
            try {
                Method canPostMethod = nm.getClass().getMethod("canPostPromotedNotifications");
                Object result = canPostMethod.invoke(nm);
                if (result instanceof Boolean) {
                    canPromote = (Boolean) result;
                }
            } catch (NoSuchMethodException ignored) {}

            if (canPromote) {
                try {
                    Method promoteMethod = builder.getClass().getMethod("setRequestPromotedOngoing", boolean.class);
                    promoteMethod.invoke(builder, true);
                } catch (NoSuchMethodException ignored) {}

                try {
                    Method chipMethod = builder.getClass().getMethod("setShortCriticalText", CharSequence.class);
                    chipMethod.invoke(builder, chipText);
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            Log.e(TAG, "Failed to apply promoted ongoing prayer notification features", error);
        }
    }
}
