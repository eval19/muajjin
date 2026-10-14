package org.muslimtechnician.muajjin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.TimeZone;

public final class PrayerWidgetHelper {
    public static final String PREFS_NAME = "MuajjinPrayerData";
    static final Class<?>[] PROVIDERS = {PrayerWidgetStripProvider.class,PrayerWidgetSmallProvider.class,
        PrayerWidgetBannerProvider.class,PrayerWidgetMediumProvider.class,PrayerWidgetLargeProvider.class,
        PrayerLockSmallProvider.class,PrayerLockMediumProvider.class};

    /** Immutable, fully formatted once per dispatch; no renderer reads/parses preferences. */
    public static final class WidgetTimelineState {
        public final long now,targetTimestamp,startTimestamp,chronometerBase;
        public final boolean valid,isWarm;
        public final int progress;
        public final String label,nextName,nextTime,footerText,contextLine2,activePrayerId,nextPrayerId,hijriText,currentStartTime;
        public final long fajrTs,sunriseTs,dhuhrTs,asrTs,maghribTs,ishaTs;
        public final String fajrStr,sunriseStr,dhuhrStr,asrStr,maghribStr,ishaStr;
        public final NativePresentation presentation;
        final PrayerTimeline timeline;
        public WidgetTimelineState(Context context,NativePresentation presentation,PrayerTimeline timeline,long now,long[] localEvents) {
            this.presentation=presentation;this.timeline=timeline;this.now=now;valid=timeline!=null;
            java.text.SimpleDateFormat clock=valid?presentation.clockFormatter(context):null;
            targetTimestamp=valid?timeline.target:0;startTimestamp=valid?timeline.start:0;
            chronometerBase=SystemClock.elapsedRealtime()+Math.max(0,targetTimestamp-now);
            isWarm=valid&&timeline.warm;progress=valid?timeline.progress:0;
            activePrayerId=valid?timeline.currentId:"";nextPrayerId=valid?timeline.nextId:"";
            nextName=valid?presentation.label(nextPrayerId):presentation.label("recovery");
            nextTime=presentation.clock(clock,targetTimestamp);
            label=valid?nextName+" "+presentation.label("in"):presentation.label("recovery");
            footerText=valid?(activePrayerId.isEmpty()?presentation.label("current")+": —":presentation.label("now")+": "+presentation.label(activePrayerId)):presentation.label("recovery");
            contextLine2=valid?nextName+" "+presentation.label("at")+" "+nextTime:"";
            currentStartTime=valid&&!activePrayerId.isEmpty()?presentation.clock(clock,startTimestamp):"";
            fajrTs=valid?localEvents[0]:0;sunriseTs=valid?localEvents[1]:0;dhuhrTs=valid?localEvents[2]:0;
            asrTs=valid?localEvents[3]:0;maghribTs=valid?localEvents[4]:0;ishaTs=valid?localEvents[5]:0;
            fajrStr=presentation.clock(clock,fajrTs);sunriseStr=presentation.clock(clock,sunriseTs);
            dhuhrStr=presentation.clock(clock,dhuhrTs);asrStr=presentation.clock(clock,asrTs);
            maghribStr=presentation.clock(clock,maghribTs);ishaStr=presentation.clock(clock,ishaTs);
            String hijri="";
            try {
                Calendar date=Calendar.getInstance(TimeZone.getDefault());date.setTimeInMillis(now);
                Calendar sunset=(Calendar)date.clone();sunset.setTimeInMillis(maghribTs);
                boolean sunsetToday=valid && date.get(Calendar.YEAR)==sunset.get(Calendar.YEAR) &&
                    date.get(Calendar.DAY_OF_YEAR)==sunset.get(Calendar.DAY_OF_YEAR);
                // Unknown Maghrib cannot safely apply a rollover. The civil date is still fresh.
                hijri=presentation.hijri(NativeHijriCalculator.calculate(date,presentation.hijriAdjustment,
                    sunsetToday&&presentation.hijriDateChangeAtMaghrib,now,maghribTs));
            } catch(IllegalArgumentException error) { Log.w("MuajjinWidget","Hijri date outside supported range",error); }
            hijriText=hijri;
        }
        public String chipClock(String id) {
            switch(id) {
                case "fajr":return fajrStr;case "sunrise":return sunriseStr;case "dhuhr":return dhuhrStr;
                case "asr":return asrStr;case "maghrib":return maghribStr;case "isha":return ishaStr;
                default:return "--:--";
            }
        }
    }

    static int[][] widgetIds(Context context,AppWidgetManager manager) {
        int[][] ids=new int[PROVIDERS.length][];
        for(int i=0;i<ids.length;i++) ids[i]=manager.getAppWidgetIds(new ComponentName(context,PROVIDERS[i]));
        return ids;
    }
    public static void updateAllWidgets(Context context,WidgetTimelineState state,int[][] ids) {
        AppWidgetManager manager=AppWidgetManager.getInstance(context);
        for(int i=0;i<ids.length;i++) for(int id:ids[i]) {
            switch(i) {
                case 0:updateStripWidget(context,manager,id,state);break;
                case 1:updateSquareWidget(context,manager,id,state);break;
                case 2:updateBannerWidget(context,manager,id,state);break;
                case 3:updateWideWidget(context,manager,id,state);break;
                case 4:updateLargeWidget(context,manager,id,state);break;
                case 5:updateLockSmallWidget(context,manager,id,state);break;
                case 6:updateLockMediumWidget(context,manager,id,state);break;
                default:throw new IllegalArgumentException("Unknown widget variant");
            }
        }
    }

    private static void setupChronometer(RemoteViews views,int viewId,WidgetTimelineState state) {
        long diff=state.targetTimestamp-state.now;
        views.setChronometerCountDown(viewId,true);
        views.setChronometer(viewId,state.chronometerBase,null,diff>0);
        if(diff<=0) views.setTextViewText(viewId,"—");
    }

    private static void applyDirection(RemoteViews views,int root,WidgetTimelineState state) {
        views.setInt(root,"setLayoutDirection","rtl".equals(state.presentation.direction)?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
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
    public static void updateStripWidget(Context context, AppWidgetManager manager, int appWidgetId, WidgetTimelineState state) {
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

        setupChronometer(views, R.id.tv_strip_countdown, state);
        views.setOnClickPendingIntent(R.id.widget_strip_root, getOpenAppIntent(context));
        applyDirection(views,R.id.widget_strip_root,state);
        manager.updateAppWidget(appWidgetId, views);
    }

    // 2. Square (2x2)
    public static void updateSquareWidget(Context context, AppWidgetManager manager, int appWidgetId, WidgetTimelineState state) {
        if (isHeightUnderThreshold(manager, appWidgetId, 100)) {
            updateStripWidget(context, manager, appWidgetId, state);
            return;
        }

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

        setupChronometer(views, R.id.tv_square_countdown, state);
        views.setTextViewText(R.id.tv_square_next_time, state.nextTime);
        views.setTextViewText(R.id.tv_square_footer_left, state.footerText);
        views.setTextViewText(R.id.tv_square_footer_right, state.currentStartTime);

        views.setOnClickPendingIntent(R.id.widget_square_root, getOpenAppIntent(context));
        applyDirection(views,R.id.widget_square_root,state);
        manager.updateAppWidget(appWidgetId, views);
    }


    // 3. Banner (4x1)
    public static void updateBannerWidget(Context context, AppWidgetManager manager, int appWidgetId, WidgetTimelineState state) {
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

        setupChronometer(views, R.id.tv_banner_countdown, state);
        views.setTextViewText(R.id.tv_banner_footer_top, state.footerText);
        views.setTextViewText(R.id.tv_banner_footer_bottom, state.contextLine2);

        views.setOnClickPendingIntent(R.id.widget_banner_root, getOpenAppIntent(context));
        applyDirection(views,R.id.widget_banner_root,state);
        manager.updateAppWidget(appWidgetId, views);
    }

    // 4. Wide Card (4x2)
    public static void updateWideWidget(Context context, AppWidgetManager manager, int appWidgetId, WidgetTimelineState state) {
        if (isHeightUnderThreshold(manager, appWidgetId, 100)) {
            updateBannerWidget(context, manager, appWidgetId, state);
            return;
        }

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

        setupChronometer(views, R.id.tv_wide_countdown, state);
        views.setTextViewText(R.id.tv_wide_footer_top, state.footerText);
        views.setTextViewText(R.id.tv_wide_footer_bottom, state.contextLine2);

        // Render the 6 Chips
        renderChips(context, views, state);

        views.setOnClickPendingIntent(R.id.widget_wide_root, getOpenAppIntent(context));
        applyDirection(views,R.id.widget_wide_root,state);
        manager.updateAppWidget(appWidgetId, views);
    }


    private static void renderChips(Context context, RemoteViews views, WidgetTimelineState state) {
        renderSingleChip(context, views, R.id.chip_fajr, R.id.tv_chip_fajr_name, R.id.tv_chip_fajr_time,"fajr",state.fajrTs,state);
        renderSingleChip(context, views, R.id.chip_sunrise, R.id.tv_chip_sunrise_name, R.id.tv_chip_sunrise_time,"sunrise",state.sunriseTs,state);
        renderSingleChip(context, views, R.id.chip_dhuhr, R.id.tv_chip_dhuhr_name, R.id.tv_chip_dhuhr_time,"dhuhr",state.dhuhrTs,state);
        renderSingleChip(context, views, R.id.chip_asr, R.id.tv_chip_asr_name, R.id.tv_chip_asr_time,"asr",state.asrTs,state);
        renderSingleChip(context, views, R.id.chip_maghrib, R.id.tv_chip_maghrib_name, R.id.tv_chip_maghrib_time,"maghrib",state.maghribTs,state);
        renderSingleChip(context, views, R.id.chip_isha, R.id.tv_chip_isha_name, R.id.tv_chip_isha_time,"isha",state.ishaTs,state);
    }

    private static void renderSingleChip(Context context,RemoteViews views,int chipId,int nameId,int timeId,String prayerId,long eventTs,WidgetTimelineState state) {
        views.setTextViewText(nameId, state.presentation.label(prayerId));
        views.setTextViewText(timeId, state.chipClock(prayerId));
        long now=state.now,currentWindowStartTs=state.startTimestamp;
        boolean isCurrentPrayer=state.valid&&prayerId.equals(state.activePrayerId)&&eventTs==state.startTimestamp;

        if (isCurrentPrayer) {
            views.setInt(chipId, "setBackgroundResource", R.drawable.widget_chip_current);
            views.setTextColor(nameId, context.getColor(R.color.w_accent));
            views.setTextColor(timeId, context.getColor(R.color.w_accent));
        } else if (state.valid && prayerId.equals(state.nextPrayerId) && eventTs==state.targetTimestamp) {
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
    public static void updateLargeWidget(Context context, AppWidgetManager manager, int appWidgetId, WidgetTimelineState state) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_large);

        views.setTextViewText(R.id.tv_large_location, state.presentation.locationName);
        views.setTextViewText(R.id.tv_large_hijri, state.hijriText);
        views.setTextViewText(R.id.tv_name_fajr,state.presentation.label("fajr"));
        views.setTextViewText(R.id.tv_name_sunrise,state.presentation.label("sunrise"));
        views.setTextViewText(R.id.tv_name_dhuhr,state.presentation.label("dhuhr"));
        views.setTextViewText(R.id.tv_name_asr,state.presentation.label("asr"));
        views.setTextViewText(R.id.tv_name_maghrib,state.presentation.label("maghrib"));
        views.setTextViewText(R.id.tv_name_isha,state.presentation.label("isha"));
        views.setTextViewText(R.id.tv_large_current_pill, state.footerText);
        views.setTextViewText(R.id.tv_large_summary, state.valid?state.nextName+" · "+state.nextTime:state.nextName);
        setupChronometer(views, R.id.tv_large_countdown, state);
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
        views.setInt(R.id.row_isha, "setBackgroundResource", "isha".equals(state.activePrayerId) && state.ishaTs==state.startTimestamp ? R.drawable.widget_active_row : 0);

        views.setOnClickPendingIntent(R.id.widget_large_root, getOpenAppIntent(context));
        applyDirection(views,R.id.widget_large_root,state);
        manager.updateAppWidget(appWidgetId, views);
    }

    // 6. Lock Small (Keyguard)
    public static void updateLockSmallWidget(Context context, AppWidgetManager manager, int appWidgetId, WidgetTimelineState state) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_lock_small);

        views.setTextViewText(R.id.tv_lock_small_label, state.label);
        setupChronometer(views, R.id.tv_lock_small_countdown, state);
        views.setTextViewText(R.id.tv_lock_small_time, state.valid?state.presentation.label("at")+" "+state.nextTime:"");

        views.setOnClickPendingIntent(R.id.widget_lock_small_root, getOpenAppIntent(context));
        applyDirection(views,R.id.widget_lock_small_root,state);
        manager.updateAppWidget(appWidgetId, views);
    }

    // 7. Lock Medium (Keyguard)
    public static void updateLockMediumWidget(Context context, AppWidgetManager manager, int appWidgetId, WidgetTimelineState state) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_lock_medium);

        views.setTextViewText(R.id.tv_lock_med_label, state.label);
        setupChronometer(views, R.id.tv_lock_med_countdown, state);
        views.setTextViewText(R.id.tv_lock_med_current, state.footerText);
        views.setTextViewText(R.id.tv_lock_med_next, state.contextLine2);

        views.setOnClickPendingIntent(R.id.widget_lock_medium_root, getOpenAppIntent(context));
        applyDirection(views,R.id.widget_lock_medium_root,state);
        manager.updateAppWidget(appWidgetId, views);
    }

    private static long scheduledTransition=0,scheduledCosmetic=0;
    private static boolean scheduledExact=false;
    private static PendingIntent alarmIntent(Context context,int request,String action) {
        Intent intent=new Intent(context,PrayerWidgetAlarmReceiver.class);intent.setAction(action);
        return PendingIntent.getBroadcast(context,request,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static void cancelAlarms(Context context) {
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(am!=null) {
            am.cancel(alarmIntent(context,0,PrayerWidgetAlarmReceiver.ACTION_PRAYER_TRANSITION));
            am.cancel(alarmIntent(context,1,PrayerWidgetAlarmReceiver.ACTION_BAR_PROGRESS_REFRESH));
        }
        scheduledTransition=scheduledCosmetic=0;
    }
    public static void scheduleNextAlarm(Context context,WidgetTimelineState state,boolean force) {
        if(!state.valid) {cancelAlarms(context);return;}
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);if(am==null)return;
        boolean exact=Build.VERSION.SDK_INT<Build.VERSION_CODES.S||am.canScheduleExactAlarms();
        if(force||scheduledTransition!=state.targetTimestamp||scheduledExact!=exact) {
            PendingIntent pi=alarmIntent(context,0,PrayerWidgetAlarmReceiver.ACTION_PRAYER_TRANSITION);
            try {
                if(exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,state.targetTimestamp,pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,state.targetTimestamp,pi);
            } catch(SecurityException denied) {
                exact=false;
                Log.w("MuajjinWidget","Exact permission denied; transition delivery is approximate",denied);
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,state.targetTimestamp,pi);
            }
            scheduledTransition=state.targetTimestamp;scheduledExact=exact;
        }
        long next=state.timeline.cosmeticTrigger();
        PendingIntent bar=alarmIntent(context,1,PrayerWidgetAlarmReceiver.ACTION_BAR_PROGRESS_REFRESH);
        if(next==0) {am.cancel(bar);scheduledCosmetic=0;}
        else if(force||scheduledCosmetic<=state.now||scheduledCosmetic>next) {
            am.set(AlarmManager.RTC,next,bar);scheduledCosmetic=next;
        }
    }
}
