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
    private static final long TRANSITION_FALLBACK_MS = 15000L;
    static final Class<?>[] PROVIDERS = {PrayerWidgetStripProvider.class,PrayerWidgetSmallProvider.class,
        PrayerWidgetBannerProvider.class,PrayerWidgetMediumProvider.class,PrayerWidgetLargeProvider.class,
        PrayerLockSmallProvider.class,PrayerLockMediumProvider.class};
    private static final int[] ROOTS = {R.id.widget_strip_root, R.id.widget_square_root,
        R.id.widget_banner_root, R.id.widget_wide_root, R.id.widget_large_root,
        R.id.widget_lock_small_root, R.id.widget_lock_medium_root};
    private static final int[] LABELS = {R.id.tv_strip_label, R.id.tv_square_label,
        R.id.tv_banner_label, R.id.tv_wide_label, 0, R.id.tv_lock_small_label, R.id.tv_lock_med_label};
    private static final int[] WARM_LABELS = {R.id.tv_strip_label_warm, R.id.tv_square_label_warm,
        R.id.tv_banner_label_warm, R.id.tv_wide_label_warm};
    private static final int[] COUNTDOWNS = {R.id.tv_strip_countdown, R.id.tv_square_countdown,
        R.id.tv_banner_countdown, R.id.tv_wide_countdown, R.id.tv_large_countdown,
        R.id.tv_lock_small_countdown, R.id.tv_lock_med_countdown};
    private static final int[] PROGRESS = {R.id.pb_widget_progress_green, R.id.pb_widget_progress_gold,
        R.id.pb_widget_progress_amber, R.id.pb_widget_progress_soft_red};
    private static final int[] CHIPS = {R.id.chip_fajr, R.id.chip_sunrise, R.id.chip_dhuhr,
        R.id.chip_asr, R.id.chip_maghrib, R.id.chip_isha};
    private static final int[] CHIP_NAMES = {R.id.tv_chip_fajr_name, R.id.tv_chip_sunrise_name,
        R.id.tv_chip_dhuhr_name, R.id.tv_chip_asr_name, R.id.tv_chip_maghrib_name, R.id.tv_chip_isha_name};
    private static final int[] CHIP_TIMES = {R.id.tv_chip_fajr_time, R.id.tv_chip_sunrise_time,
        R.id.tv_chip_dhuhr_time, R.id.tv_chip_asr_time, R.id.tv_chip_maghrib_time, R.id.tv_chip_isha_time};
    private static final int[] TABLE_CELLS = {R.id.row_fajr, R.id.row_sunrise, R.id.row_dhuhr,
        R.id.row_asr, R.id.row_maghrib, R.id.row_isha};
    private static final int[] TABLE_NAMES = {R.id.tv_name_fajr, R.id.tv_name_sunrise, R.id.tv_name_dhuhr,
        R.id.tv_name_asr, R.id.tv_name_maghrib, R.id.tv_name_isha};
    private static final int[] TABLE_TIMES = {R.id.tv_time_fajr, R.id.tv_time_sunrise, R.id.tv_time_dhuhr,
        R.id.tv_time_asr, R.id.tv_time_maghrib, R.id.tv_time_isha};

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
        final Locale numericLocale;
        final WidgetProgress progressStage;
        public WidgetTimelineState(Context context,NativePresentation presentation,PrayerTimeline timeline,long now,long[] localEvents) {
            this(context,presentation,timeline,now,localEvents,WidgetLocale.resolve(context));
        }
        WidgetTimelineState(Context context,NativePresentation presentation,PrayerTimeline timeline,long now,
                            long[] localEvents,Locale numericLocale) {
            this.numericLocale=numericLocale;
            this.presentation=presentation;this.timeline=timeline;this.now=now;valid=timeline!=null;
            java.text.SimpleDateFormat clock=valid?presentation.clockFormatter(context,numericLocale):null;
            targetTimestamp=valid?timeline.target:0;startTimestamp=valid?timeline.start:0;
            chronometerBase=SystemClock.elapsedRealtime()+Math.max(0,targetTimestamp-now);
            isWarm=valid&&timeline.warm;progress=valid?timeline.progress:0;
            progressStage=WidgetProgress.at(valid,startTimestamp,targetTimestamp,now);
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
                    sunsetToday&&presentation.hijriDateChangeAtMaghrib,now,maghribTs),numericLocale);
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


    private static PendingIntent getOpenAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void updateStripWidget(Context context, AppWidgetManager manager, int id, WidgetTimelineState state) {
        updateWidget(context, manager, id, state, 0);
    }
    public static void updateSquareWidget(Context context, AppWidgetManager manager, int id, WidgetTimelineState state) {
        updateWidget(context, manager, id, state, 1);
    }
    public static void updateBannerWidget(Context context, AppWidgetManager manager, int id, WidgetTimelineState state) {
        updateWidget(context, manager, id, state, 2);
    }
    public static void updateWideWidget(Context context, AppWidgetManager manager, int id, WidgetTimelineState state) {
        updateWidget(context, manager, id, state, 3);
    }
    public static void updateLargeWidget(Context context, AppWidgetManager manager, int id, WidgetTimelineState state) {
        updateWidget(context, manager, id, state, 4);
    }
    public static void updateLockSmallWidget(Context context, AppWidgetManager manager, int id, WidgetTimelineState state) {
        updateWidget(context, manager, id, state, 5);
    }
    public static void updateLockMediumWidget(Context context, AppWidgetManager manager, int id, WidgetTimelineState state) {
        updateWidget(context, manager, id, state, 6);
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int id,
                                     WidgetTimelineState state, int variant) {
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
                        layouts.put(size, createWidgetViews(context, state, variant, size.getWidth(), size.getHeight()));
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
            maxHeight > 0 ? maxHeight : minHeight);
        if (maxWidth > 0 && minHeight > 0 && (maxWidth != minWidth || minHeight != maxHeight)) {
            RemoteViews landscape = createWidgetViews(context, state, variant, maxWidth, minHeight);
            manager.updateAppWidget(id, new RemoteViews(landscape, portrait));
        } else {
            manager.updateAppWidget(id, portrait);
        }
    }

    /** The same per-size native presentation used by the host and native visual regressions. */
    static RemoteViews createWidgetViews(Context context, WidgetTimelineState state,
                                         int requestedVariant, float widthDp, float heightDp) {
        WidgetLayout layout = new WidgetLayout(context, state, requestedVariant, widthDp, heightDp);
        if ((layout.variant == 1 || layout.variant == 3) && widthDp > 0 && heightDp > 0
                && !layout.fitsWithinBounds()) {
            WidgetLayout simpler = new WidgetLayout(context, state, layout.variant == 1 ? 0 : 2, widthDp, heightDp);
            if (simpler.fitsWithinBounds()) layout = simpler;
        }
        int variant = layout.variant;
        RemoteViews views = new RemoteViews(context.getPackageName(), layout.layoutId());
        if (variant != 4) {
            int label = variant < 4 && state.isWarm ? WARM_LABELS[variant] : LABELS[variant];
            views.setTextViewText(label, state.label);
            views.setInt(label, "setMaxLines", state.valid ? 1 : 4);
            setSize(views, label, layout.labelSize());
            float labelWidth = layout.textWidth(state.label, layout.labelSize(), true) + 2;
            if (widthDp > 0) labelWidth = Math.min(labelWidth, layout.contentWidth());
            views.setInt(label, "setMinWidth", layout.px(labelWidth));
            if (variant < 4) {
                views.setViewVisibility(LABELS[variant], state.isWarm ? View.GONE : View.VISIBLE);
                views.setViewVisibility(WARM_LABELS[variant], state.isWarm ? View.VISIBLE : View.GONE);
                setSize(views, state.isWarm ? LABELS[variant] : WARM_LABELS[variant], layout.labelSize());
            }
        }
        setupChronometer(views, COUNTDOWNS[variant], state);
        views.setViewVisibility(COUNTDOWNS[variant], state.valid ? View.VISIBLE : View.GONE);
        setSize(views, COUNTDOWNS[variant], layout.countdownSize());
        views.setInt(COUNTDOWNS[variant], "setMinWidth", layout.px(layout.countdownWidth()));
        views.setInt(COUNTDOWNS[variant], "setMinHeight", layout.px(layout.countdownHeight()));
        if (variant < 5) {
            int selected=progressViewId(state.progressStage);
            views.setViewVisibility(R.id.widget_progress_container, selected==View.NO_ID ? View.GONE : View.VISIBLE);
            views.setViewPadding(R.id.widget_progress_container, 0, layout.px(layout.progressGap()), 0, 0);
            for(int progressId:PROGRESS)
                views.setViewVisibility(progressId,progressId==selected ? View.VISIBLE : View.GONE);
            if(selected!=View.NO_ID) views.setProgressBar(selected,1000,state.progress,false);
        }
        switch (variant) {
            case 1:
                bindText(views, R.id.tv_square_next_time, state.nextTime, layout.contextSize());
                bindText(views, R.id.tv_square_footer_left, state.footerText, layout.contextSize());
                bindText(views, R.id.tv_square_footer_right, state.currentStartTime, layout.contextSize());
                views.setViewVisibility(R.id.widget_square_footer, state.valid ? View.VISIBLE : View.GONE);
                break;
            case 2:
                bindText(views, R.id.tv_banner_footer_top, state.footerText, layout.contextSize());
                bindText(views, R.id.tv_banner_footer_bottom, state.contextLine2, layout.contextSize());
                break;
            case 3:
                bindText(views, R.id.tv_wide_footer_top, state.footerText, layout.contextSize());
                bindText(views, R.id.tv_wide_footer_bottom, state.contextLine2, layout.contextSize());
                renderSchedule(context, views, state, layout, false);
                break;
            case 4:
                bindText(views, R.id.tv_large_location, state.presentation.locationName, layout.contextSize());
                bindText(views, R.id.tv_large_hijri, state.hijriText, layout.contextSize());
                bindText(views, R.id.tv_large_current_pill, state.footerText, layout.contextSize());
                bindText(views, R.id.tv_large_summary,
                    state.valid ? state.nextName + " · " + state.nextTime : state.nextName, layout.timeSize());
                views.setInt(R.id.tv_large_summary, "setMaxLines", state.valid ? 1 : 4);
                views.setViewVisibility(R.id.tv_large_current_pill, state.valid ? View.VISIBLE : View.GONE);
                views.setViewVisibility(R.id.widget_primary_group, state.valid ? View.VISIBLE : View.GONE);
                views.setViewVisibility(R.id.widget_schedule_container, layout.summaryOnly ? View.GONE : View.VISIBLE);
                if (!layout.summaryOnly) renderSchedule(context, views, state, layout, true);
                break;
            case 5:
                bindText(views, R.id.tv_lock_small_time,
                    state.valid ? state.presentation.label("at") + " " + state.nextTime : "", layout.contextSize());
                views.setViewVisibility(R.id.tv_lock_small_time, state.valid ? View.VISIBLE : View.GONE);
                break;
            case 6:
                bindText(views, R.id.tv_lock_med_current, state.footerText, layout.contextSize());
                bindText(views, R.id.tv_lock_med_next, state.contextLine2, layout.contextSize());
                break;
            default:
                break;
        }
        if (variant == 2 || variant == 3 || variant == 6)
            views.setViewVisibility(R.id.widget_context_group, state.valid ? View.VISIBLE : View.GONE);
        int horizontal = layout.px(layout.horizontalInset());
        int vertical = layout.px(layout.verticalInset());
        views.setViewPadding(ROOTS[variant], horizontal, vertical, horizontal, vertical);
        views.setOnClickPendingIntent(ROOTS[variant], getOpenAppIntent(context));
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

    private static void bindText(RemoteViews views, int id, String text, float pixels) {
        views.setTextViewText(id, text);
        setSize(views, id, pixels);
    }

    private static void renderSchedule(Context context, RemoteViews views, WidgetTimelineState state,
                                        WidgetLayout layout, boolean table) {
        int[] cells = table ? TABLE_CELLS : CHIPS;
        int[] names = table ? TABLE_NAMES : CHIP_NAMES;
        int[] clocks = table ? TABLE_TIMES : CHIP_TIMES;
        views.setViewPadding(R.id.widget_schedule_container, 0, layout.px(layout.scheduleGap()), 0, 0);
        if (table || layout.columns == 3)
            views.setViewPadding(R.id.widget_schedule_second_row, 0, layout.px(layout.rowGap()), 0, 0);
        int clockMinimum = layout.px(layout.cellHeight() - layout.nameLineHeight - 2 * layout.cellVerticalInset());
        for (int i = 0; i < cells.length; i++) {
            String prayerId = WidgetLayout.PRAYERS[i];
            bindText(views, names[i], state.presentation.label(prayerId), layout.nameSize());
            bindText(views, clocks[i], state.chipClock(prayerId), layout.timeSize());
            views.setInt(names[i], "setMinHeight", layout.px(layout.nameLineHeight));
            views.setInt(clocks[i], "setMaxLines", layout.wrapClocks ? 2 : 1);
            views.setViewPadding(clocks[i], 0, layout.px(layout.cellLabelGap()), 0, 0);
            views.setInt(clocks[i], "setMinHeight", clockMinimum);
            int x = layout.px(layout.cellHorizontalInset());
            int y = layout.px(layout.cellVerticalInset());
            views.setViewPadding(cells[i], x, y, x, y);
            if (table) {
                boolean current = prayerId.equalsIgnoreCase(state.activePrayerId)
                    && (i != 5 || state.ishaTs == state.startTimestamp);
                views.setInt(cells[i], "setBackgroundResource", current ? R.drawable.widget_active_row : 0);
                setTextColorResource(context, views, names[i], current ? R.color.w_accent : R.color.w_text_secondary);
                setTextColorResource(context, views, clocks[i], current ? R.color.w_accent : R.color.w_text_primary);
            } else {
                renderSingleChip(context, views, cells[i], names[i], clocks[i], prayerId, eventTimestamp(state, i), state);
            }
        }
    }

    private static long eventTimestamp(WidgetTimelineState state, int index) {
        switch (index) {
            case 0: return state.fajrTs;
            case 1: return state.sunriseTs;
            case 2: return state.dhuhrTs;
            case 3: return state.asrTs;
            case 4: return state.maghribTs;
            case 5: return state.ishaTs;
            default: throw new IllegalArgumentException("Unknown prayer index");
        }
    }

    private static void renderSingleChip(Context context, RemoteViews views, int chipId, int nameId,
                                         int timeId, String prayerId, long eventTs, WidgetTimelineState state) {
        boolean current = state.valid && prayerId.equals(state.activePrayerId) && eventTs == state.startTimestamp;
        boolean next = state.valid && prayerId.equals(state.nextPrayerId) && eventTs == state.targetTimestamp;
        int background = current ? R.drawable.widget_chip_current
            : next ? R.drawable.widget_chip_next : R.drawable.widget_chip_transparent;
        int color = current ? R.color.w_accent
            : !next && eventTs < state.now && eventTs < state.startTimestamp
                ? R.color.w_text_muted : R.color.w_text_secondary;
        views.setInt(chipId, "setBackgroundResource", background);
        setTextColorResource(context, views, nameId, color);
        setTextColorResource(context, views, timeId, color);
    }

    private static void setTextColorResource(Context context, RemoteViews views, int id, int resource) {
        if (Build.VERSION.SDK_INT >= 31) views.setColor(id, "setTextColor", resource);
        else views.setTextColor(id, context.getColor(resource));
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
            try {
                if(exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,state.targetTimestamp,pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,state.targetTimestamp,pi);
            } catch(SecurityException denied) {
                exact=false;
                Log.w("MuajjinWidget","Exact permission denied; transition delivery is approximate",denied);
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,state.targetTimestamp,pi);
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
