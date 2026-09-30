package org.muslimtechnician.muajjin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.lang.reflect.Method;

public class PrayerNotificationHelper {
    public static final String CHANNEL_ID = "muajjin_live_update";
    public static final int NOTIFICATION_ID = 1001;

    public static void updateLiveUpdateNotification(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PrayerWidgetHelper.PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("live_update_enabled", true);
        if (!enabled) {
            cancelLiveUpdateNotification(context);
            return;
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        // Check if notifications are permitted on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!nm.areNotificationsEnabled()) {
                return;
            }
        }

        ensureNotificationChannel(context, nm);

        PrayerWidgetHelper.WidgetTimelineState state = PrayerWidgetHelper.getTimelineState(context);
        if (state.targetTimestamp <= 0) return;

        long now = System.currentTimeMillis();
        long diffMs = Math.max(0, state.targetTimestamp - now);
        int remainingMins = (int) (diffMs / 60000L);
        String chipText = state.nextName + " " + (remainingMins > 60 ? (remainingMins / 60) + "h " + (remainingMins % 60) + "m" : remainingMins + "m");

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Next: " + state.nextName + " at " + state.nextTime;
        String content = state.footerText + " · " + state.label;

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        builder.setSmallIcon(R.mipmap.ic_launcher)
               .setContentTitle(title)
               .setContentText(content)
               .setContentIntent(pi)
               .setOngoing(true)
               .setOnlyAlertOnce(true)
               .setShowWhen(true)
               .setWhen(state.targetTimestamp)
               .setUsesChronometer(true)
               .setVisibility(Notification.VISIBILITY_PUBLIC);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setChronometerCountDown(true);
        }

        // Try Android 16 / HyperOS 3 Super Island Promoted Ongoing Notification
        applyAndroid16LiveUpdate(context, nm, builder, chipText);

        try {
            nm.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception ignored) {}
    }

    public static void cancelLiveUpdateNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try {
                nm.cancel(NOTIFICATION_ID);
            } catch (Exception ignored) {}
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

    private static void applyAndroid16LiveUpdate(Context context, NotificationManager nm, Notification.Builder builder, String chipText) {
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
        } catch (Exception ignored) {}
    }
}
