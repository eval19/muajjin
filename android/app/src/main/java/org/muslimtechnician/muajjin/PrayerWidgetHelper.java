package org.muslimtechnician.muajjin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.util.Log;
import android.util.SizeF;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;

public final class PrayerWidgetHelper {
    public static final String PREFS_NAME = "MuajjinPrayerData";
    private static final long TRANSITION_FALLBACK_MS = 2000L;
    static final long TRANSITION_DELIVERY_LEEWAY_MS = 50L;
    static final Class<?>[] PROVIDERS = {PrayerWidgetStripProvider.class};
    private static final int[] ROOTS = {R.id.widget_strip_root};
    private static final int[] LABELS = {R.id.tv_strip_label};
    private static final int[] WARM_LABELS = {R.id.tv_strip_label_warm};
    private static final int[] COUNTDOWNS = {R.id.tv_strip_countdown};
    private static final int[] PROGRESS = {R.id.pb_widget_progress_green, R.id.pb_widget_progress_gold,
        R.id.pb_widget_progress_amber, R.id.pb_widget_progress_soft_red};

    /** Immutable, fully formatted once per dispatch; no renderer reads/parses preferences. */
    public static final class WidgetTimelineState {
        public final long now,targetTimestamp,startTimestamp,chronometerBase;
        public final boolean valid,isWarm;
        public final int progress;
        public final String label,nextName,nextTime,footerText,contextLine2,summaryText,lockTime,activePrayerId,nextPrayerId,hijriText,currentStartTime;
        public final long fajrTs,sunriseTs,dhuhrTs,asrTs,maghribTs,ishaTs;
        public final String fajrStr,sunriseStr,dhuhrStr,asrStr,maghribStr,ishaStr;
        public final String[] forecastIds,forecastTimes;
        public final long[] forecastTimestamps;
        public final NativePresentation presentation;
        final PrayerTimeline timeline;
        final Locale numericLocale;
        final WidgetProgress progressStage;
        public WidgetTimelineState(Context context,NativePresentation presentation,PrayerTimeline timeline,long now,long[] localEvents) {
            this(context,presentation,timeline,now,localEvents,WidgetLocale.resolve(context),null);
        }
        WidgetTimelineState(Context context,NativePresentation presentation,PrayerTimeline timeline,long now,
                            long[] localEvents,PrayerSchedule.Upcoming upcoming) {
            this(context,presentation,timeline,now,localEvents,WidgetLocale.resolve(context),upcoming);
        }
        WidgetTimelineState(Context context,NativePresentation presentation,PrayerTimeline timeline,long now,
                            long[] localEvents,Locale numericLocale) {
            this(context,presentation,timeline,now,localEvents,numericLocale,null);
        }
        WidgetTimelineState(Context context,NativePresentation presentation,PrayerTimeline timeline,long now,
                            long[] localEvents,Locale numericLocale,PrayerSchedule.Upcoming upcoming) {
            this.numericLocale=numericLocale;
            this.presentation=presentation;this.timeline=timeline;this.now=now;valid=timeline!=null;
            java.text.SimpleDateFormat clock=valid?presentation.clockFormatter(context,numericLocale):null;
            targetTimestamp=valid?timeline.target:0;startTimestamp=valid?timeline.start:0;
            chronometerBase=SystemClock.elapsedRealtime()+Math.max(0,targetTimestamp-now);
            isWarm=valid&&timeline.warm;progress=valid?timeline.progress:0;
            progressStage=WidgetProgress.at(valid,startTimestamp,targetTimestamp,now);
            activePrayerId=valid?timeline.currentId:"";nextPrayerId=valid?timeline.nextId:"";
            nextName=valid?presentation.label(nextPrayerId):presentation.label("recovery");
            nextTime=formatWidgetClock(clock,targetTimestamp);
            // Keep the clock as an independent LTR run when it is embedded in an
            // Arabic sentence; otherwise bidi reordering can move the AM/PM word.
            boolean rtl="rtl".equals(presentation.direction);
            String embeddedTime=rtl
                ? BidiFormatter.getInstance(true).unicodeWrap(nextTime, TextDirectionHeuristics.LTR)
                : nextTime;
            label=valid?nextName+" "+presentation.label("in"):presentation.label("recovery");
            footerText=valid?(activePrayerId.isEmpty()?presentation.label("current")+": —":presentation.label("now")+": "+presentation.label(activePrayerId)):presentation.label("recovery");
            contextLine2=valid?nextName+" "+presentation.label("at")+" "+embeddedTime:"";
            summaryText=valid?nextName+" · "+embeddedTime:nextName;
            lockTime=valid?presentation.label("at")+" "+embeddedTime:"";
            currentStartTime=valid&&!activePrayerId.isEmpty()?formatWidgetClock(clock,startTimestamp):"";
            fajrTs=valid?localEvents[0]:0;sunriseTs=valid?localEvents[1]:0;dhuhrTs=valid?localEvents[2]:0;
            asrTs=valid?localEvents[3]:0;maghribTs=valid?localEvents[4]:0;ishaTs=valid?localEvents[5]:0;
            fajrStr=formatWidgetClock(clock,fajrTs);sunriseStr=formatWidgetClock(clock,sunriseTs);
            dhuhrStr=formatWidgetClock(clock,dhuhrTs);asrStr=formatWidgetClock(clock,asrTs);
            maghribStr=formatWidgetClock(clock,maghribTs);ishaStr=formatWidgetClock(clock,ishaTs);
            String hijri="";
            try {
                Calendar date=Calendar.getInstance(TimeZone.getDefault());date.setTimeInMillis(now);
                Calendar sunset=(Calendar)date.clone();sunset.setTimeInMillis(maghribTs);
                boolean sunsetToday=valid && date.get(Calendar.YEAR)==sunset.get(Calendar.YEAR) &&
                    date.get(Calendar.DAY_OF_YEAR)==sunset.get(Calendar.DAY_OF_YEAR);
                // Unknown Maghrib cannot safely apply a rollover. The civil date is still fresh.
                hijri=presentation.hijri(NativeHijriCalculator.calculate(date,presentation.hijriAdjustment,
                    sunsetToday&&presentation.hijriDateChangeAtMaghrib,now,maghribTs),numericLocale);
            } catch(IllegalArgumentException error) { Log.w("MuajjinWidget","Hijri date outside supported range",error); }
            hijriText=hijri;
            forecastIds=new String[4];
            forecastTimestamps=new long[4];
            forecastTimes=new String[4];
            if(valid) {
                if(upcoming!=null && upcoming.ids.length==4 && upcoming.timestamps[0]>0) {
                    for(int i=0;i<4;i++) {
                        forecastIds[i]=upcoming.ids[i];
                        forecastTimestamps[i]=upcoming.timestamps[i];
                        forecastTimes[i]=formatWidgetClock(clock,upcoming.timestamps[i]);
                    }
                } else {
                    String[] order={"fajr","sunrise","dhuhr","asr","maghrib","isha"};
                    long[] dayTimes={timeline.day.fajr,timeline.day.sunrise,timeline.day.dhuhr,
                        timeline.day.asr,timeline.day.maghrib,timeline.day.isha};
                    int count=0;
                    for(int i=0;i<dayTimes.length && count<4;i++) {
                        if(dayTimes[i]>now) {
                            forecastIds[count]=order[i];
                            forecastTimestamps[count]=dayTimes[i];
                            forecastTimes[count]=formatWidgetClock(clock,dayTimes[i]);
                            count++;
                        }
                    }
                    for(int i=0;i<order.length && count<4;i++) {
                        long ts=(i==0 && timeline.day.nextFajr>now) ? timeline.day.nextFajr : 0L;
                        forecastIds[count]=order[i];
                        forecastTimestamps[count]=ts;
                        forecastTimes[count]=formatWidgetClock(clock,ts);
                        count++;
                    }
                }
            } else {
                for(int i=0;i<4;i++) {
                    forecastIds[i]="";
                    forecastTimestamps[i]=0L;
                    forecastTimes[i]="--:--";
                }
            }
        }
        private String formatWidgetClock(java.text.SimpleDateFormat formatter,long timestamp) {
            String text=presentation.clock(formatter,timestamp);
            if(!"rtl".equals(presentation.direction)) return text;
            int separator=text.indexOf(' ');
            // Arabic-Indic digits are weak bidi characters. A strong LTR boundary
            // keeps the translated day period after the numeric clock in every field.
            return separator<0?text:text.substring(0,separator)+"\u200E"+text.substring(separator);
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
        context=themeContext(context,state.presentation.theme);
        AppWidgetManager manager=AppWidgetManager.getInstance(context);
        WidgetProgressPalette.Fill fill=ids.length>0&&ids[0].length>0?progressFill(context,state):null;
        for(int i=0;i<ids.length;i++) for(int id:ids[i])
            updateWidget(context,manager,id,state,i,fill);
    }

    private static WidgetProgressPalette.Fill progressFill(Context context,WidgetTimelineState state) {
        return WidgetProgressPalette.Fill.of(context,state.valid,state.startTimestamp,
            state.targetTimestamp,state.now);
    }

    private static void setupChronometer(RemoteViews views,int viewId,WidgetTimelineState state) {
        long diff=state.targetTimestamp-state.now;
        views.setChronometerCountDown(viewId,true);
        views.setChronometer(viewId,state.chronometerBase,null,diff>=1000L);
        if(diff<=0) views.setTextViewText(viewId,"—");
        else if(diff<1000L) {
            java.text.NumberFormat numbers=java.text.NumberFormat.getIntegerInstance(state.numericLocale);
            views.setTextViewText(viewId,WidgetLocale.elapsedTime(0,numbers));
        }
    }

    private static void applyDirection(RemoteViews views,int root,WidgetTimelineState state) {
        views.setInt(root,"setLayoutDirection","rtl".equals(state.presentation.direction)?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
    }


    private static PendingIntent getOpenAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void updateStripWidget(Context context, AppWidgetManager manager, int id, WidgetTimelineState state) {
        updateWidget(context, manager, id, state, 0, progressFill(context, state));
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int id,
                                     WidgetTimelineState state, int variant, WidgetProgressPalette.Fill fill) {
        Bundle options = manager.getAppWidgetOptions(id);
        if (options == null) options = Bundle.EMPTY;
        if (Build.VERSION.SDK_INT >= 31) {
            ArrayList<SizeF> sizes = Build.VERSION.SDK_INT >= 33
                ? options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF.class)
                : options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            // RemoteViews supports at most sixteen size alternatives.
            if (sizes != null && !sizes.isEmpty() && sizes.size() <= 16) {
                Map<SizeF, RemoteViews> layouts = new LinkedHashMap<>();
                for (SizeF size : sizes) {
                    if (size != null && size.getWidth() > 0 && size.getHeight() > 0)
                        layouts.put(size, createWidgetViews(context, state, variant, size.getWidth(),
                            size.getHeight(), fill));
                }
                if (!layouts.isEmpty()) {
                    manager.updateAppWidget(id, new RemoteViews(layouts));
                    return;
                }
            }
        }
        float minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        float maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
        float minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        float maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        RemoteViews portrait = createWidgetViews(context, state, variant, minWidth,
            maxHeight > 0 ? maxHeight : minHeight, fill);
        if (maxWidth > 0 && minHeight > 0 && (maxWidth != minWidth || minHeight != maxHeight)) {
            RemoteViews landscape = createWidgetViews(context, state, variant, maxWidth, minHeight, fill);
            manager.updateAppWidget(id, new RemoteViews(landscape, portrait));
        } else {
            manager.updateAppWidget(id, portrait);
        }
    }

    /** The same per-size native presentation used by the host and native visual regressions. */
    static RemoteViews createWidgetViews(Context context, WidgetTimelineState state,
                                         int requestedVariant, float widthDp, float heightDp) {
        return createWidgetViews(context, state, requestedVariant, widthDp, heightDp,
            progressFill(context, state));
    }

    static RemoteViews createWidgetViews(Context context, WidgetTimelineState state,
                                         int requestedVariant, float widthDp, float heightDp,
                                         WidgetProgressPalette.Fill fill) {
        String theme = state.presentation.theme;
        context = themeContext(context, theme);
        WidgetLayout layout = new WidgetLayout(context, state, 0, widthDp, heightDp);
        int variant = layout.variant;
        RemoteViews views = new RemoteViews(context.getPackageName(), layout.layoutId());
        int label = state.isWarm ? WARM_LABELS[variant] : LABELS[variant];
        views.setTextViewText(label, state.label);
        views.setInt(label, "setMaxLines", state.valid ? 1 : 4);
        setSize(views, label, layout.labelSize());
        float labelWidth = layout.textWidth(state.label, layout.labelSize(), true) + 2;
        if (widthDp > 0) labelWidth = Math.min(labelWidth, layout.contentWidth());
        views.setInt(label, "setMinWidth", layout.px(labelWidth));
        views.setViewVisibility(LABELS[variant], state.isWarm ? View.GONE : View.VISIBLE);
        views.setViewVisibility(WARM_LABELS[variant], state.isWarm ? View.VISIBLE : View.GONE);
        setSize(views, state.isWarm ? LABELS[variant] : WARM_LABELS[variant], layout.labelSize());
        setupChronometer(views, COUNTDOWNS[variant], state);
        views.setViewVisibility(COUNTDOWNS[variant], state.valid ? View.VISIBLE : View.GONE);
        setSize(views, COUNTDOWNS[variant], layout.countdownSize());
        views.setInt(COUNTDOWNS[variant], "setMinWidth", layout.px(layout.countdownWidth()));
        views.setInt(COUNTDOWNS[variant], "setMinHeight", layout.px(layout.countdownHeight()));
        views.setViewPadding(R.id.widget_progress_container, 0, layout.px(layout.progressGap()), 0, 0);
        if (Build.VERSION.SDK_INT >= 31) {
            // System mode keeps both palettes host-resolvable. Forced modes
            // send the same palette for either launcher configuration.
            boolean live=fill!=null;
            views.setViewVisibility(R.id.widget_progress_container, live ? View.VISIBLE : View.GONE);
            for(int progressId:PROGRESS)
                views.setViewVisibility(progressId,
                    progressId==R.id.pb_widget_progress_green&&live ? View.VISIBLE : View.GONE);
            if(live) {
                views.setProgressBar(R.id.pb_widget_progress_green,1000,state.progress,false);
                views.setColorStateList(R.id.pb_widget_progress_green,"setProgressTintList",
                    "dark".equals(theme) ? fill.darkList : fill.lightList,
                    "light".equals(theme) ? fill.lightList : fill.darkList);
                views.setColorStateList(R.id.pb_widget_progress_green, "setProgressBackgroundTintList",
                    themeResource(theme, R.color.w_track, R.color.w_light_track, R.color.w_dark_track));
            }
        } else {
            // Before API 31 RemoteViews cannot send ColorStateList setters.
            // XML tint resources preserve both forced and system modes.
            RemoteViews bars = new RemoteViews(context.getPackageName(), themeResource(theme,
                R.layout.widget_progress_system, R.layout.widget_progress_light, R.layout.widget_progress_dark));
            views.removeAllViews(R.id.widget_progress_container);
            views.addView(R.id.widget_progress_container, bars);
            int selected=progressViewId(state.progressStage);
            views.setViewVisibility(R.id.widget_progress_container, selected==View.NO_ID ? View.GONE : View.VISIBLE);
            for(int progressId:PROGRESS)
                views.setViewVisibility(progressId,progressId==selected ? View.VISIBLE : View.GONE);
            if(selected!=View.NO_ID) views.setProgressBar(selected,1000,state.progress,false);
        }
        views.setViewPadding(R.id.tv_strip_countdown, 0, layout.px(layout.tight ? 0 : 2), 0, 0);
        int horizontal = layout.px(layout.horizontalInset());
        int vertical = layout.px(layout.verticalInset());
        views.setViewPadding(ROOTS[variant], horizontal, vertical, horizontal, vertical);
        views.setOnClickPendingIntent(ROOTS[variant], getOpenAppIntent(context));
        applyTheme(context, views, state, variant);
        applyDirection(views, ROOTS[variant], state);
        return views;
    }

    private static int progressViewId(WidgetProgress stage) {
        switch(stage) {
            case GREEN:return R.id.pb_widget_progress_green;
            case GOLD:return R.id.pb_widget_progress_gold;
            case AMBER:return R.id.pb_widget_progress_amber;
            case SOFT_RED:return R.id.pb_widget_progress_soft_red;
            default:return View.NO_ID;
        }
    }

    private static void setSize(RemoteViews views, int id, float pixels) {
        views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_PX, pixels);
    }

    private static void setTextColorResource(Context context, RemoteViews views, int id, int resource, String theme) {
        if (Build.VERSION.SDK_INT >= 31 && "system".equals(theme))
            views.setColor(id, "setTextColor", resource);
        else views.setTextColor(id, context.getColor(resource));
    }

    private static Context themeContext(Context context, String theme) {
        if ("system".equals(theme)) return context;
        int night = "dark".equals(theme) ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
        Configuration current = context.getResources().getConfiguration();
        if ((current.uiMode & Configuration.UI_MODE_NIGHT_MASK) == night) return context;
        Configuration selected = new Configuration(current);
        selected.uiMode = (selected.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
        return context.createConfigurationContext(selected);
    }

    private static String effectiveTheme(Context context, String theme) {
        if (Build.VERSION.SDK_INT >= 31 || !"system".equals(theme)) return theme;
        boolean night = (context.getResources().getConfiguration().uiMode
            & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return night ? "dark" : "light";
    }

    private static int themeResource(String theme, int system, int light, int dark) {
        return "light".equals(theme) ? light : "dark".equals(theme) ? dark : system;
    }

    private static void applyTheme(Context context, RemoteViews views, WidgetTimelineState state, int variant) {
        String theme = state.presentation.theme;
        String effectiveTheme = effectiveTheme(context, theme);
        views.setInt(ROOTS[variant], "setBackgroundResource", themeResource(effectiveTheme,
            R.drawable.widget_card_bg, R.drawable.widget_card_bg_light, R.drawable.widget_card_bg_dark));
        setTextColorResource(context, views, COUNTDOWNS[variant], R.color.w_text_primary, theme);
        setTextColorResource(context, views, LABELS[variant], R.color.w_accent, theme);
        setTextColorResource(context, views, WARM_LABELS[variant], R.color.w_warm_text, theme);
    }

    private static long scheduledTransition=0,scheduledCosmetic=0,scheduledFallback=0;
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
            am.cancel(alarmIntent(context,2,PrayerWidgetAlarmReceiver.ACTION_TRANSITION_FALLBACK));
        }
        scheduledTransition=scheduledCosmetic=scheduledFallback=0;
        context.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE).edit().remove("next_transition_at").apply();
    }
    public static void scheduleNextAlarm(Context context,WidgetTimelineState state,boolean force) {
        if(!state.valid) {cancelAlarms(context);return;}
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);if(am==null)return;
        boolean exact=Build.VERSION.SDK_INT<Build.VERSION_CODES.S||am.canScheduleExactAlarms();
        if(force||scheduledTransition!=state.targetTimestamp||scheduledExact!=exact) {
            PendingIntent pi=alarmIntent(context,0,PrayerWidgetAlarmReceiver.ACTION_PRAYER_TRANSITION);
            long triggerAt=state.targetTimestamp+TRANSITION_DELIVERY_LEEWAY_MS;
            try {
                if(exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,triggerAt,pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,triggerAt,pi);
            } catch(SecurityException denied) {
                exact=false;
                Log.w("MuajjinWidget","Exact permission denied; transition delivery is approximate",denied);
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,triggerAt,pi);
            }
            scheduledTransition=state.targetTimestamp;scheduledExact=exact;
            context.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE).edit()
                .putLong("next_transition_at",state.targetTimestamp).apply();
        }
        // One non-wakeup recovery refresh after the boundary. A successful
        // transition re-schedules a new fallback and replaces this one.
        long fallbackAt=state.targetTimestamp+TRANSITION_FALLBACK_MS;
        PendingIntent fallback=alarmIntent(context,2,PrayerWidgetAlarmReceiver.ACTION_TRANSITION_FALLBACK);
        if(force||scheduledFallback!=fallbackAt) {
            am.set(AlarmManager.RTC,fallbackAt,fallback);
            scheduledFallback=fallbackAt;
        }
        long next=state.timeline.cosmeticTrigger();
        PendingIntent bar=alarmIntent(context,1,PrayerWidgetAlarmReceiver.ACTION_BAR_PROGRESS_REFRESH);
        if(next==0) {am.cancel(bar);scheduledCosmetic=0;}
        else if(force||scheduledCosmetic<=state.now||scheduledCosmetic>next) {
            am.set(AlarmManager.RTC,next,bar);scheduledCosmetic=next;
        }
    }
}
