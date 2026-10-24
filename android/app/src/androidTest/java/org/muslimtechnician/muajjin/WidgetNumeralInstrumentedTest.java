package org.muslimtechnician.muajjin;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.LocaleManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Actual device configuration, not a translated Context pretending to be the device. */
@RunWith(AndroidJUnit4.class)
public class WidgetNumeralInstrumentedTest {
    private static final String[] PRAYERS = {"fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"};
    private static final int[] COUNTDOWNS = {R.id.tv_strip_countdown,R.id.tv_square_countdown,
        R.id.tv_banner_countdown,R.id.tv_wide_countdown,R.id.tv_large_countdown,
        R.id.tv_lock_small_countdown,R.id.tv_lock_med_countdown};
    private static final int[][] BOUNDS = {{160,96},{180,180},{360,96},{360,220},
        {360,320},{180,96},{360,96}};

    private Locale deviceLocale(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            LocaleManager manager = context.getSystemService(LocaleManager.class);
            if (manager != null && !manager.getSystemLocales().isEmpty()) return manager.getSystemLocales().get(0);
        }
        return Resources.getSystem().getConfiguration().getLocales().get(0);
    }

    private String digits(Locale locale) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(locale);
        formatter.setGroupingUsed(false);
        StringBuilder result = new StringBuilder();
        for (int digit = 0; digit < 10; digit++) result.append(formatter.format(digit));
        return result.toString();
    }

    private void assertDigitSet(String role, String text, Locale locale) {
        String expected = digits(locale);
        int count = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.getType(codePoint) == Character.DECIMAL_DIGIT_NUMBER) {
                assertTrue(role + " uses non-device digit U+" + Integer.toHexString(codePoint)
                    + " for " + locale.toLanguageTag() + ": " + text,
                    expected.indexOf(new String(Character.toChars(codePoint))) >= 0);
                count++;
            }
        }
        assertTrue(role + " has no numeric content", count > 0);
    }

    private NativePresentation presentation(String language) throws Exception {
        JSONObject json = NativePresentation.defaults("Synthetic district 12").json();
        json.put("language", language);
        json.put("timeFormat", "12h");
        if (language.equals("ar")) {
            json.put("direction", "rtl");
            String[] names = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
            for (int i = 0; i < PRAYERS.length; i++) json.getJSONObject("labels").put(PRAYERS[i], names[i]);
            json.getJSONObject("labels").put("am", "ص");
            json.getJSONObject("labels").put("pm", "م");
        }
        else if(language.equals("bn")) {
            String[] names={"ফজর","সূর্যোদয়","যোহর","আসর","মাগরিব","ইশা"};
            for(int i=0;i<PRAYERS.length;i++)json.getJSONObject("labels").put(PRAYERS[i],names[i]);
            json.getJSONObject("labels").put("am","পূর্বাহ্ণ");
            json.getJSONObject("labels").put("pm","অপরাহ্ণ");
        }
        return NativePresentation.parse(json);
    }

    private PrayerWidgetHelper.WidgetTimelineState state(Context context, NativePresentation presentation) {
        return state(context,presentation,null);
    }

    private PrayerWidgetHelper.WidgetTimelineState state(Context context,NativePresentation presentation,Locale numericLocale) {
        TimeZone zone = TimeZone.getDefault();
        Calendar date = Calendar.getInstance(zone);
        date.clear();
        date.set(2026, Calendar.OCTOBER, 24, 12, 0, 0);
        double longitude = Math.max(-180, Math.min(180, zone.getOffset(date.getTimeInMillis()) / 240000d));
        PrayerSchedule schedule = PrayerSchedule.renew(date.getTimeInMillis(), zone,
            new PrayerSchedule.Calculation(21.3891, longitude, 4, 0));
        PrayerSchedule.Day day = schedule.dayAt(date.getTimeInMillis(), zone);
        long now = day.dhuhr + (day.asr - day.dhuhr) / 3;
        PrayerTimeline timeline = PrayerTimeline.at(schedule, now, zone);
        long[] events=schedule.localEvents(now,zone,timeline.day);
        return numericLocale==null
            ?new PrayerWidgetHelper.WidgetTimelineState(context,presentation,timeline,now,events)
            :new PrayerWidgetHelper.WidgetTimelineState(context,presentation,timeline,now,events,numericLocale);
    }

    private String expectedClock(NativePresentation presentation, Locale locale, long epoch) {
        SimpleDateFormat formatter=new SimpleDateFormat(presentation.timeFormat.equals("24h")?"HH:mm":"h:mm a",locale);
        DateFormatSymbols symbols = formatter.getDateFormatSymbols();
        symbols.setAmPmStrings(new String[]{presentation.label("am"), presentation.label("pm")});
        formatter.setDateFormatSymbols(symbols);
        NumberFormat numbers = NumberFormat.getIntegerInstance(locale);
        numbers.setGroupingUsed(false);
        formatter.setNumberFormat(numbers);
        return formatter.format(new Date(epoch));
    }

    private void assertVisibleNumericText(View view,Locale locale) {
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view instanceof TextView && !(view instanceof Chronometer) && view.getId()!=R.id.tv_large_location) {
            String text=((TextView)view).getText().toString();
            if(text.codePoints().anyMatch(value->Character.getType(value)==Character.DECIMAL_DIGIT_NUMBER))
                assertDigitSet("Rendered numeric field",text,locale);
        }
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)assertVisibleNumericText(group.getChildAt(i),locale);
        }
    }

    private boolean containsVisibleText(View view,String text) {
        if(view.getVisibility()!=View.VISIBLE)return false;
        if(view instanceof TextView && ((TextView)view).getText().toString().contains(text))return true;
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)if(containsVisibleText(group.getChildAt(i),text))return true;
        }
        return false;
    }

    @Test public void allVariantsPreserveImportedWordsAcrossIndependentNumberingLocales() throws Exception {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Context target=instrumentation.getTargetContext();
        assertTrue(target.getPackageName().endsWith(".debug"));
        Activity activity=instrumentation.startActivitySync(new Intent(target,WidgetTestActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        String[][] pairs={{"en-US","en"},{"ar-EG","en"},{"en-US","ar"},{"ar-EG","bn"},
            {"bn-BD","en"},{"fa-IR","en"},{"ar-EG-u-nu-latn","ar"},{"ar-MA","ar"}};
        try {
            for(String[] pair:pairs) {
                Locale numbers=Locale.forLanguageTag(pair[0]);
                Configuration configuration=new Configuration(target.getResources().getConfiguration());
                configuration.setLocale(numbers);
                Context context=target.createConfigurationContext(configuration);
                for(String format:new String[]{"12h","24h"}) {
                JSONObject json=presentation(pair[1]).json();json.put("timeFormat",format);
                NativePresentation display=NativePresentation.parse(json);
                PrayerWidgetHelper.WidgetTimelineState snapshot=state(context,display,numbers);
                for(int variant:new int[]{0,1,2,3,4,5,6})instrumentation.runOnMainSync(()->{
                    FrameLayout host=new FrameLayout(activity);
                    activity.setContentView(host);
                    View root=PrayerWidgetHelper.createWidgetViews(context,snapshot,variant,
                        BOUNDS[variant][0],BOUNDS[variant][1]).apply(context,host);
                    host.addView(root);
                    int width=Math.round(BOUNDS[variant][0]*context.getResources().getDisplayMetrics().density);
                    int height=Math.round(BOUNDS[variant][1]*context.getResources().getDisplayMetrics().density);
                    host.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
                    host.layout(0,0,width,height);
                    assertVisibleNumericText(root,numbers);
                    assertEquals("Imported direction follows labels, not numeric script",
                        pair[1].equals("ar")?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR,root.getLayoutDirection());
                    if(variant==4)assertEquals("User location digits must not be transformed","Synthetic district 12",
                        ((TextView)root.findViewById(R.id.tv_large_location)).getText().toString());
                    assertTrue("Imported prayer name must remain visible",containsVisibleText(root,snapshot.nextName));
                    if(variant!=0)assertTrue("Rendered clock must preserve every digit and selected day period",
                        containsVisibleText(root,expectedClock(display,numbers,snapshot.targetTimestamp)));
                });
                }
            }
        } finally {instrumentation.runOnMainSync(activity::finish);}
    }

    @Test public void liveCountdownAndClockFieldsUseDeviceDigitsIndependentlyOfImportedLabels() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        assertTrue("Only the disposable debug app may host the fixture", context.getPackageName().endsWith(".debug"));
        Locale locale = deviceLocale(context);
        String expectedLanguage = InstrumentationRegistry.getArguments().getString("expectedDeviceLanguage");
        if (expectedLanguage != null) assertEquals("Actual device locale was not changed", expectedLanguage, locale.getLanguage());
        Activity activity = instrumentation.startActivitySync(new Intent(context, WidgetTestActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            for (String language : new String[]{"en", "ar", "bn"}) {
                NativePresentation presentation = presentation(language);
                PrayerWidgetHelper.WidgetTimelineState snapshot = state(context, presentation);
                for(int variant:new int[]{0,1,2,3,4,5,6}) {
                AtomicReference<Chronometer> countdown = new AtomicReference<>();
                AtomicReference<String> firstTick = new AtomicReference<>();
                AtomicReference<View> widget = new AtomicReference<>();
                CountDownLatch advanced = new CountDownLatch(1);
                instrumentation.runOnMainSync(() -> {
                    FrameLayout host = new FrameLayout(activity);
                    activity.setContentView(host);
                    RemoteViews remote = PrayerWidgetHelper.createWidgetViews(context,snapshot,variant,
                        BOUNDS[variant][0],BOUNDS[variant][1]);
                    View root = remote.apply(context, host);
                    host.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                    widget.set(root);
                    Chronometer clock = root.findViewById(COUNTDOWNS[variant]);
                    countdown.set(clock);
                    firstTick.set(clock.getText().toString());
                    clock.setOnChronometerTickListener(value -> {
                        if (!value.getText().toString().equals(firstTick.get())) advanced.countDown();
                    });
                });
                assertTrue("Native countdown never advanced", advanced.await(5, TimeUnit.SECONDS));
                instrumentation.runOnMainSync(() -> {
                    Chronometer clock = countdown.get();
                    assertTrue(clock.isCountDown());
                    assertEquals(snapshot.chronometerBase, clock.getBase());
                    assertTrue(clock.getBase() > SystemClock.elapsedRealtime());
                    String live = clock.getText().toString();
                    assertDigitSet("Live native countdown", live, locale);
                    Log.i("MuajjinNumerals", "device=" + locale.toLanguageTag() + " app=" + language
                        + " live=" + live + " next=" + snapshot.nextTime + " digits=" + digits(locale));
                    assertVisibleNumericText(widget.get(),locale);
                    if(variant==1) {
                        TextView nextClock=widget.get().findViewById(R.id.tv_square_next_time);
                        assertEquals("Complete clock must use device numbers and imported day period",
                            expectedClock(presentation,locale,snapshot.targetTimestamp),nextClock.getText().toString());
                    }
                    assertDigitSet("Current prayer clock", snapshot.currentStartTime, locale);
                    assertDigitSet("Hijri day/year", snapshot.hijriText, locale);
                    assertEquals(language.equals("ar") ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR,
                        widget.get().getLayoutDirection());
                    clock.setOnChronometerTickListener(null);
                });
                }
            }
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }
}
