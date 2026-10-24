package org.muslimtechnician.muajjin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PrayerWidgetAlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_PRAYER_TRANSITION = "org.muslimtechnician.muajjin.ACTION_PRAYER_TRANSITION";
    public static final String ACTION_BAR_PROGRESS_REFRESH = "org.muslimtechnician.muajjin.ACTION_BAR_PROGRESS_REFRESH";
    public static final String ACTION_TRANSITION_FALLBACK = "org.muslimtechnician.muajjin.ACTION_TRANSITION_FALLBACK";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent==null) return;
        String action=intent.getAction();
        boolean transition=ACTION_PRAYER_TRANSITION.equals(action),fallback=ACTION_TRANSITION_FALLBACK.equals(action),
            cosmetic=ACTION_BAR_PROGRESS_REFRESH.equals(action)||fallback;
        boolean reconcile=Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)||
            Intent.ACTION_TIME_CHANGED.equals(action)||Intent.ACTION_TIMEZONE_CHANGED.equals(action)||
            Intent.ACTION_DATE_CHANGED.equals(action)||Intent.ACTION_LOCALE_CHANGED.equals(action)||Intent.ACTION_USER_PRESENT.equals(action)||
            Intent.ACTION_USER_UNLOCKED.equals(action)||android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action)||
            "android.app.action.APP_BLOCK_STATE_CHANGED".equals(action)||"android.app.action.NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED".equals(action);
        if(!transition&&!cosmetic&&!reconcile) return;
        PendingResult result=goAsync();Context application=context.getApplicationContext();
        PrayerUpdateCoordinator.execute(()->{
            // A transition or its fallback must re-arm even if it fires just before
            // the target; otherwise the static scheduler cache can leave no alarm.
            try {PrayerUpdateCoordinator.dispatchNow(application,mustForceReschedule(transition,fallback,reconcile));}
            finally {result.finish();}
        });
    }
    static boolean mustForceReschedule(boolean transition,boolean fallback,boolean reconcile) {
        return transition||fallback||reconcile;
    }
}
