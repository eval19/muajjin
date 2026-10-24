package org.muslimtechnician.muajjin;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** All durable transactions and bounded local renewal run outside the Android UI thread. */
public final class PrayerUpdateCoordinator {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private PrayerUpdateCoordinator() {}
    public static void execute(Runnable operation) { WORKER.execute(operation); }
    public static void dispatch(Context context,boolean force) {
        Context application=context.getApplicationContext();execute(()->dispatchNow(application,force));
    }
    public static void dispatchNow(Context context,boolean force) {
        try {
            AppWidgetManager manager=AppWidgetManager.getInstance(context);
            int[][] ids=PrayerWidgetHelper.widgetIds(context,manager);boolean widgets=false;
            for(int[] variant:ids) if(variant.length>0){widgets=true;break;}
            boolean notification=PrayerNotificationHelper.isConsumerActive(context);
            if(!widgets&&!notification) {
                PrayerWidgetHelper.cancelAlarms(context);PrayerNotificationHelper.cancelLiveUpdateNotification(context);return;
            }
            SharedPreferences prefs=context.getSharedPreferences(PrayerWidgetHelper.PREFS_NAME,Context.MODE_PRIVATE);
            long now=System.currentTimeMillis();TimeZone zone=TimeZone.getDefault();
            PrayerSchedule schedule=NativeScheduleStore.loadSchedule(prefs);
            long lastFailure=prefs.getLong("last_renewal_failure",0L);
            boolean inBackoff=(now-lastFailure)<1800000L;
            if((schedule==null||schedule.needsRenewal(now,zone)) && (!inBackoff || force)) {
                PrayerSchedule.Calculation config=NativeScheduleStore.loadCalculation(prefs);
                if(config!=null) try {
                    PrayerSchedule renewed=PrayerSchedule.renew(now,zone,config);
                    NativeScheduleStore.saveSchedule(prefs,renewed,config,"native:"+zone.getID()+":"+renewed.validFrom);
                    schedule=renewed;
                    if(lastFailure!=0L) prefs.edit().remove("last_renewal_failure").apply();
                } catch(Exception error) {
                    prefs.edit().putLong("last_renewal_failure",now).apply();
                    // A current, validated horizon remains usable after a failed proactive renewal.
                    // Expired/wrong-zone schedules are rejected by timeline selection below.
                    Log.e("MuajjinCoordinator","Offline schedule renewal failed; preserving known-good data",error);
                }
            }
            now=System.currentTimeMillis();
            long lastDispatch=prefs.getLong("last_dispatch_at",0L);
            if(force||now-lastDispatch>=300000L)
                prefs.edit().putLong("last_dispatch_at",now).apply();
            NativePresentation presentation=NativeScheduleStore.loadPresentation(prefs);
            PrayerTimeline timeline=PrayerTimeline.at(schedule,now,zone);
            long[] localEvents=timeline==null?null:schedule.localEvents(now,zone,timeline.day);
            PrayerWidgetHelper.WidgetTimelineState snapshot=new PrayerWidgetHelper.WidgetTimelineState(context,presentation,timeline,now,localEvents);
            if(widgets) PrayerWidgetHelper.updateAllWidgets(context,snapshot,ids);
            if(notification) PrayerNotificationHelper.updateLiveUpdateNotification(context,snapshot,force);
            else PrayerNotificationHelper.cancelLiveUpdateNotification(context);
            PrayerWidgetHelper.scheduleNextAlarm(context,snapshot,force);
        } catch(Exception error) {Log.e("MuajjinCoordinator","Native prayer dispatch failed",error);}
    }
}
