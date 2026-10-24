package org.muslimtechnician.muajjin;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.*;

/** Real RemoteViews in the non-exported native host; never writes app preferences.
 * Run once per externally selected light/dark mode. This is not launcher/keyguard proof. */
@RunWith(AndroidJUnit4.class)
public class WidgetPresentationInstrumentedTest {
    private static final String[] PRAYERS = {"fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"};
    private static final String[] VARIANTS = {"strip", "square", "banner", "wide", "table", "lock-small", "lock-medium"};
    private static final int[] ROOTS = {R.id.widget_strip_root, R.id.widget_square_root,
        R.id.widget_banner_root, R.id.widget_wide_root, R.id.widget_large_root,
        R.id.widget_lock_small_root, R.id.widget_lock_medium_root};
    private static final int[] COUNTDOWNS = {R.id.tv_strip_countdown, R.id.tv_square_countdown,
        R.id.tv_banner_countdown, R.id.tv_wide_countdown, R.id.tv_large_countdown,
        R.id.tv_lock_small_countdown, R.id.tv_lock_med_countdown};
    private static final int[] LABELS = {R.id.tv_strip_label, R.id.tv_square_label,
        R.id.tv_banner_label, R.id.tv_wide_label};
    private static final int[] WARM_LABELS = {R.id.tv_strip_label_warm, R.id.tv_square_label_warm,
        R.id.tv_banner_label_warm, R.id.tv_wide_label_warm};
    private static final int[] PROGRESS = {R.id.pb_widget_progress_green,R.id.pb_widget_progress_gold,
        R.id.pb_widget_progress_amber,R.id.pb_widget_progress_soft_red};
    private static final int[] CHIPS = {R.id.chip_fajr, R.id.chip_sunrise, R.id.chip_dhuhr,
        R.id.chip_asr, R.id.chip_maghrib, R.id.chip_isha};
    private static final int[] CHIP_NAMES = {R.id.tv_chip_fajr_name, R.id.tv_chip_sunrise_name,
        R.id.tv_chip_dhuhr_name, R.id.tv_chip_asr_name, R.id.tv_chip_maghrib_name, R.id.tv_chip_isha_name};
    private static final int[] CHIP_TIMES = {R.id.tv_chip_fajr_time, R.id.tv_chip_sunrise_time,
        R.id.tv_chip_dhuhr_time, R.id.tv_chip_asr_time, R.id.tv_chip_maghrib_time, R.id.tv_chip_isha_time};
    private static final int[] ROWS = {R.id.row_fajr, R.id.row_sunrise, R.id.row_dhuhr,
        R.id.row_asr, R.id.row_maghrib, R.id.row_isha};
    private static final int[] ROW_NAMES = {R.id.tv_name_fajr, R.id.tv_name_sunrise, R.id.tv_name_dhuhr,
        R.id.tv_name_asr, R.id.tv_name_maghrib, R.id.tv_name_isha};
    private static final int[] ROW_TIMES = {R.id.tv_time_fajr, R.id.tv_time_sunrise, R.id.tv_time_dhuhr,
        R.id.tv_time_asr, R.id.tv_time_maghrib, R.id.tv_time_isha};
    private static final Profile[] NORMAL = {
        new Profile(0, 160, 96), new Profile(1, 180, 180), new Profile(2, 360, 96),
        new Profile(3, 360, 220), new Profile(4, 360, 320),
        new Profile(5, 180, 96), new Profile(6, 360, 96)
    };

    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final List<String> failures = new ArrayList<>();
    private Context target;
    private Activity activity;
    private FrameLayout surface;
    private PrayerSchedule schedule;
    private PrayerSchedule.Day day;
    private TimeZone zone;
    private boolean capture;
    private int cases;

    private static final class Profile {
        final int variant;
        final float width, height;
        Profile(int variant, float width, float height) {
            this.variant = variant; this.width = width; this.height = height;
        }
        Profile scaled(float scale) {
            // Accessibility may require a larger user-selected host, not smaller sp text.
            return new Profile(variant, (float)Math.ceil(width * scale), (float)Math.ceil(height * scale));
        }
        String name() { return VARIANTS[variant] + "-" + Math.round(width) + "x" + Math.round(height); }
    }

    private final int[] compatibleReapplies = new int[7];

    private static final class Rendered {
        final Context context;
        final Profile profile;
        final FrameLayout host;
        final ViewGroup root;
        final PrayerWidgetHelper.WidgetTimelineState state;
        final int layoutId;
        Rendered(Context context, Profile profile, FrameLayout host, ViewGroup root,
                 PrayerWidgetHelper.WidgetTimelineState state, int layoutId) {
            this.context = context; this.profile = profile; this.host = host;
            this.root = root; this.state = state;
            this.layoutId = layoutId;
        }
    }

    @Test public void actualRemoteViewsKeepContentGeometryAndStateAcrossThePresentationMatrix() throws Exception {
        target = instrumentation.getTargetContext();
        assertTrue("Only the disposable debug application may host synthetic widgets",
            target.getPackageName().endsWith(".debug"));
        Locale originalLocale=Locale.getDefault();
        Locale originalDisplay=Locale.getDefault(Locale.Category.DISPLAY);
        Locale originalFormat=Locale.getDefault(Locale.Category.FORMAT);
        capture = Boolean.parseBoolean(InstrumentationRegistry.getArguments().getString("captureWidgetDesign", "false"));
        prepareSchedule(Calendar.OCTOBER,13);
        activity = instrumentation.startActivitySync(new Intent(target, WidgetTestActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(() -> {
                surface = new FrameLayout(activity);
                activity.setContentView(surface);
                // English family + independently selected narrow/grid and six-column Wide.
                for (Profile profile : NORMAL) runCase(profile, "en", 1f, "midday", true, null);
                runCase(new Profile(3, 600, 220), "en", 1f, "midday", true, null);
                runCase(new Profile(4, 250, 220), "en", 1f, "midday", true, null);
                runCase(new Profile(4,250,220),"en",1f,"midday-long-location",true,null);
                for (Profile profile : NORMAL) runCase(profile, "en", 1f, "midday-24h", false, null);

                // Same attached native hierarchy is reapplied, never replaced with a mock.
                for (Profile profile : NORMAL) runReapplySequence(profile);
                runExtraHeightCase(new Profile(3, 360, 220));
                runExtraHeightCase(new Profile(4, 360, 320));
                runIndependentWideInstances();
                runLegacySizeBoundaries();
                Profile[] minimums = {new Profile(0,120,88), new Profile(1,130,160),
                    new Profile(2,280,96), new Profile(3,288,220), new Profile(4,288,220),
                    new Profile(5,170,96), new Profile(6,280,80)};
                for (String language : new String[]{"en","bn","ar"}) {
                    for (Profile profile : minimums) {
                        runCase(profile,language,1f,"midday",profile.variant==3 || profile.variant==4,null);
                        runCase(profile,language,1f,"invalid",false,null);
                    }
                }
                if (capture) {
                    captureOverview();
                    capturePickerPreviews();
                    captureTransparentBackgrounds();
                }

                // Grouped risk matrix, not a full Cartesian product. uiMode is never overridden.
                for (String language : new String[]{"en", "bn", "ar"}) {
                    for (float scale : new float[]{1f, 1.3f, 1.5f, 2f}) {
                        if (language.equals("en") && scale == 1f) continue;
                        for (Profile profile : NORMAL)
                            runCase(profile.scaled(scale), language, scale, "midday", false, null);
                    }
                }
                // Winter noon has a two-digit PM clock; early winter nights exceed ten hours.
                for(int month : new int[]{Calendar.JANUARY,Calendar.JULY}) {
                    prepareSchedule(month,1);
                    audit("seasonal-clock-check month="+(month+1));
                    for(String language : new String[]{"en","bn","ar"}) {
                        runCase(new Profile(3,288,220),language,1f,"midday",false,null);
                        runCase(new Profile(4,288,220),language,1f,"midday",false,null);
                        if(language.equals("bn")) runLegacyClockFitTransitions();
                    }
                    if(month==Calendar.JANUARY) {
                        runCase(NORMAL[1],"en",1f,"night-start",false,null);
                        runCase(new Profile(0,120,88),"en",1f,"night-start",false,null);
                    }
                }
            });
        } finally {
            if (activity != null) instrumentation.runOnMainSync(() -> {
                if (surface != null) surface.removeAllViews();
                activity.finish();
            });
            Locale.setDefault(originalLocale);
            Locale.setDefault(Locale.Category.DISPLAY,originalDisplay);
            Locale.setDefault(Locale.Category.FORMAT,originalFormat);
            audit("summary cases=" + cases + " failures=" + failures.size()
                + " theme=" + theme(target) + " density=" + target.getResources().getDisplayMetrics().density
                + " captures=" + capture + " host=native-debug not-launcher-or-keyguard");
        }
        assertTrue("Native widget presentation defects (first 24):\n" + joinFailures(), failures.isEmpty());
    }



    private void prepareSchedule(int month,int dayOfMonth) {
        zone = TimeZone.getDefault();
        Calendar date = Calendar.getInstance(zone);
        date.clear(); date.set(2026,month,dayOfMonth,12,0,0);
        // Align the synthetic location's solar day with the device's civil timezone.
        // This uses the actual calculator without changing the process timezone/clock.
        double longitude = Math.max(-180, Math.min(180, zone.getOffset(date.getTimeInMillis()) / 240000d));
        schedule = PrayerSchedule.renew(date.getTimeInMillis(), zone,
            new PrayerSchedule.Calculation(21.3891, longitude, 4, 0));
        String key = PrayerSchedule.dateKey(date);
        day = null;
        for (PrayerSchedule.Day candidate : schedule.days()) if (candidate.date.equals(key)) day = candidate;
        assertNotNull("Synthetic calculator day missing", day);
    }

    private Context configured(String language, float scale) {
        Configuration config = new Configuration(target.getResources().getConfiguration());
        Locale locale = Locale.forLanguageTag(language);
        // Serialized fixture process only. A Context alone cannot change Chronometer's
        // host-process formatter; real device policy is covered in WidgetNumeralInstrumentedTest.
        Locale.setDefault(locale);
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        config.fontScale = scale;
        return target.createConfigurationContext(config);
    }

    private NativePresentation presentation(String language,String fixture) throws Exception {
        String location=fixture.equals("midday-long-location") ? "Riyadh governorate — sample district"
            : language.equals("ar") ? "نموذج" : language.equals("bn") ? "নমুনা" : "Sample";
        JSONObject json = NativePresentation.defaults(location).json();
        json.put("language", language);
        json.put("direction", language.equals("ar") ? "rtl" : "ltr");
        json.put("timeFormat", "12h");
        JSONObject labels = json.getJSONObject("labels");
        if (language.equals("ar")) {
            String[] names = {"الفَجْر", "الشُّرُوق", "الظُّهْر", "العَصْر", "المَغْرِب", "العِشَاء"};
            for (int i = 0; i < PRAYERS.length; i++) labels.put(PRAYERS[i], names[i]);
            labels.put("now", "الآن"); labels.put("current", "الصلاة الحالية");
            labels.put("in", "بعد"); labels.put("at", "عند");
            labels.put("am", "ص"); labels.put("pm", "م");
            labels.put("recovery", "افتح المؤذن لاستعادة إعدادات الصلاة");
        } else if (language.equals("bn")) {
            String[] names = {"ফজর", "সূর্যোদয়", "যোহর", "আসর", "মাগরিব", "ইশা"};
            for (int i = 0; i < PRAYERS.length; i++) labels.put(PRAYERS[i], names[i]);
            labels.put("now", "এখন"); labels.put("current", "বর্তমান সালাত");
            labels.put("in", "বাকি"); labels.put("at", "সময়");
            labels.put("am", "পূর্বাহ্ণ"); labels.put("pm", "অপরাহ্ণ");
            labels.put("recovery", "নামাজের সেটিংস ফেরাতে মুয়াজ্জিন খুলুন");
        }
        return NativePresentation.parse(json);
    }

    private PrayerWidgetHelper.WidgetTimelineState state(Context context, String language, String fixture) throws Exception {
        long now;
        switch (fixture) {
            case "midday":
            case "midday-long-location":
            case "midday-24h": now = day.dhuhr + (day.asr - day.dhuhr) / 3; break;
            case "asr": now = day.asr + (day.maghrib - day.asr) / 3; break;
            case "fajr": now = day.fajr + (day.sunrise - day.fajr) / 3; break;
            case "maghrib": now = day.maghrib + (day.isha - day.maghrib) / 3; break;
            case "isha": now = day.isha + Math.min(30 * 60000L, (day.nextFajr - day.isha) / 3); break;
            case "night-start": now = day.isha + 1000; break;
            case "gold": now = day.asr - 20 * 60000L; break;
            case "amber": now = day.asr - 10 * 60000L; break;
            case "soft-red": now = day.asr - 2 * 60000L; break;
            case "warm": now = day.asr - 5 * 60000L; break;
            case "no-current": now = day.sunrise + (day.dhuhr - day.sunrise) / 2; break;
            case "overnight": now = day.fajr - 3600000L; break;
            case "invalid": now = schedule.validUntil; break;
            default: throw new IllegalArgumentException(fixture);
        }
        PrayerTimeline timeline = PrayerTimeline.at(schedule, now, zone);
        if (fixture.equals("invalid")) assertNull(timeline); else assertNotNull(timeline);
        long[] localEvents = timeline == null ? new long[6] : schedule.localEvents(now, zone, timeline.day);
        NativePresentation display = presentation(language,fixture);
        if (fixture.endsWith("-24h")) {
            JSONObject json = display.json(); json.put("timeFormat", "24h");
            display = NativePresentation.parse(json);
        }
        PrayerWidgetHelper.WidgetTimelineState result = new PrayerWidgetHelper.WidgetTimelineState(
            context, display, timeline, now, localEvents,
            context.getResources().getConfiguration().getLocales().get(0));
        if (fixture.startsWith("midday")) {
            assertEquals("dhuhr", result.activePrayerId); assertEquals("asr", result.nextPrayerId);
            assertTrue("Fixture must exercise an hour-bearing countdown", result.targetTimestamp - now > 3600000L);
        } else if (fixture.equals("warm")) assertTrue(result.isWarm);
        else if(fixture.equals("night-start")) {
            assertEquals("isha",result.activePrayerId);
            assertTrue("Fixture must exercise a two-digit hour countdown",result.targetTimestamp-now>10*3600000L);
        }
        else if (fixture.equals("asr")) assertEquals("asr", result.activePrayerId);
        else if (fixture.equals("fajr") || fixture.equals("maghrib") || fixture.equals("isha"))
            assertEquals(fixture, result.activePrayerId);
        else if (fixture.equals("no-current")) assertEquals("", result.activePrayerId);
        else if (fixture.equals("overnight")) {
            assertEquals("isha", result.activePrayerId); assertEquals("fajr", result.nextPrayerId);
            assertNotEquals("Overnight Isha must not highlight this civil day's future Isha",
                result.startTimestamp, result.ishaTs);
        }
        return result;
    }

    private Rendered render(Profile profile, Context context, PrayerWidgetHelper.WidgetTimelineState state) {
        return render(profile, profile.variant, context, state);
    }

    private Rendered render(Profile request, int expectedVariant, Context context,
                            PrayerWidgetHelper.WidgetTimelineState state) {
        Profile profile = new Profile(expectedVariant, request.width, request.height);
        FrameLayout host = new FrameLayout(context);
        host.setBackgroundColor(profile.variant >= 5 || theme(context).equals("dark") ? 0xff182030 : 0xffe0d6c2);
        RemoteViews remote = PrayerWidgetHelper.createWidgetViews(context, state, request.variant, profile.width, profile.height);
        View applied = remote.apply(context, host);
        assertNotNull("RemoteViews did not inflate", applied);
        host.addView(applied, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        surface.addView(host, new FrameLayout.LayoutParams(px(context, profile.width), px(context, profile.height)));
        layoutHost(host, context, profile);
        ViewGroup root = applied.findViewById(ROOTS[profile.variant]);
        assertNotNull("Wrong/information-reducing composition for " + profile.name(), root);
        return new Rendered(context, profile, host, root, state, remote.getLayoutId());
    }

    private void layoutHost(FrameLayout host, Context context, Profile profile) {
        int width = px(context, profile.width), height = px(context, profile.height);
        ViewGroup.LayoutParams params=host.getLayoutParams();
        if(params.width!=width || params.height!=height) {
            params.width=width; params.height=height; host.setLayoutParams(params);
        }
        host.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        host.layout(0, 0, width, height);
    }

    private Rendered runCase(Profile profile, String language, float scale, String fixture,
                         boolean saveCapture, Rendered previous) {
        return runCase(profile,language,scale,fixture,saveCapture,previous,false);
    }

    private Rendered runCase(Profile profile, String language, float scale, String fixture,
                         boolean saveCapture, Rendered previous,boolean summaryOnly) {
        String name = profile.name() + "-" + language + "-font" + scale + "-" + fixture
            + (previous == null ? "" : "-reapply");
        Rendered rendered = null;
        cases++;
        try {
            Context context = previous == null ? configured(language, scale) : previous.context;
            PrayerWidgetHelper.WidgetTimelineState state = state(context, language, fixture);
            if (previous == null) rendered = render(profile, context, state);
            else {
                RemoteViews remote = PrayerWidgetHelper.createWidgetViews(context, state, profile.variant, profile.width, profile.height);
                ViewGroup root;
                if (remote.getLayoutId() == previous.layoutId) {
                    remote.reapply(context, previous.root);
                    compatibleReapplies[profile.variant]++;
                    root = previous.root;
                } else {
                    // AppWidgetHostView also reinflates when a responsive resource changes.
                    View applied = remote.apply(context, previous.host);
                    previous.host.removeAllViews();
                    previous.host.addView(applied, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    root = applied.findViewById(ROOTS[profile.variant]);
                    assertNotNull("Composition change lost the variant root", root);
                }
                layoutHost(previous.host, context, profile);
                rendered = new Rendered(context, profile, previous.host, root, state, remote.getLayoutId());
            }
            verify(rendered, language, scale, summaryOnly);
        } catch (Throwable error) {
            record(name, error);
        } finally {
            // Capture after assertions too: failures must not make visual evidence unobtainable.
            if (rendered != null) {
                if (capture && (saveCapture || !failures.isEmpty() && failures.get(failures.size() - 1).startsWith(name + ":")))
                    capture(rendered, name);
                if (previous == null) surface.removeView(rendered.host);
            }
        }
        return rendered == null ? previous : rendered;
    }

    private void runReapplySequence(Profile profile) {
        String prefix = profile.name() + "-reapply";
        Rendered rendered = null;
        try {
            Context context = configured("en", 1f);
            rendered = render(profile, context, state(context, "en", "midday"));
            for (String fixture : new String[]{"gold","amber","soft-red","midday","warm","midday",
                    "asr","maghrib","isha","fajr","no-current","overnight","invalid","midday"}) {
                boolean stageCapture=profile.variant<5
                    && (fixture.equals("gold")||fixture.equals("amber")||fixture.equals("soft-red"));
                rendered = runCase(profile, "en", 1f, fixture, stageCapture, rendered);
                // Compare visible presentation against a fresh application of the same state.
                PrayerWidgetHelper.WidgetTimelineState next = state(context, "en", fixture);
                Rendered fresh = null;
                try {
                    fresh = render(profile, context, next);
                    assertEquivalent(rendered.root, fresh.root);
                } finally {
                    if (fresh != null) surface.removeView(fresh.host);
                }
            }
            assertTrue("No compatible native reapply was exercised for " + profile.name(),
                compatibleReapplies[profile.variant] > 0);
            audit("reapply variant=" + VARIANTS[profile.variant] + " compatible=" + compatibleReapplies[profile.variant]);
        } catch (Throwable error) {
            record(prefix, error);
        } finally {
            if (rendered != null) surface.removeView(rendered.host);
        }
    }

    private void verifyProgressPixels(Rendered rendered,ProgressBar progress) {
        int color;
        switch(rendered.state.progressStage) {
            case GOLD:color=R.color.w_progress_gold;break;
            case AMBER:color=R.color.w_progress_amber;break;
            case SOFT_RED:color=R.color.w_progress_soft_red;break;
            default:color=R.color.w_accent;break;
        }
        int fillWidth=progress.getWidth()*rendered.state.progress/1000;
        if(fillWidth<px(rendered.context,4)) return;
        Bitmap bitmap=Bitmap.createBitmap(progress.getWidth(),progress.getHeight(),Bitmap.Config.ARGB_8888);
        try {
            progress.jumpDrawablesToCurrentState();
            progress.draw(new Canvas(bitmap));
            int y=bitmap.getHeight()/2;
            boolean rtl=progress.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL;
            int fillX=rtl?bitmap.getWidth()-1-fillWidth/2:fillWidth/2;
            assertEquals("Native progress fill does not match semantic stage",
                rendered.context.getColor(color),bitmap.getPixel(fillX,y));
            if(bitmap.getWidth()-fillWidth>=px(rendered.context,4))
                assertEquals("Stage recolored or removed the track",rendered.context.getColor(R.color.w_track),
                    bitmap.getPixel(rtl?(bitmap.getWidth()-fillWidth)/2:(fillWidth+bitmap.getWidth())/2,y));
        } finally {bitmap.recycle();}
    }

    private void verify(Rendered rendered, String language, float scale) {
        verify(rendered,language,scale,false);
    }

    private void verify(Rendered rendered, String language, float scale,boolean summaryOnly) {
        Profile p = rendered.profile;
        ViewGroup root = rendered.root;
        PrayerWidgetHelper.WidgetTimelineState s = rendered.state;
        assertEquals("Configured theme was lost", theme(target), theme(root.getContext()));
        assertEquals("Imported layout direction", language.equals("ar") ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR,
            root.getLayoutDirection());
        assertTrue("Whole root lost its open-app click action", root.isClickable() && root.hasOnClickListeners());
        Chronometer clock = root.findViewById(COUNTDOWNS[p.variant]);
        assertNotNull(clock);
        assertEquals("Configuration font scale did not reach actual inflated native views", scale,
            clock.getResources().getConfiguration().fontScale, .001f);
        assertEquals("Configuration locale did not reach actual inflated native views", language,
            clock.getResources().getConfiguration().getLocales().get(0).getLanguage());
        assertTrue("Native countdown direction changed", clock.isCountDown());
        assertEquals("Native countdown base changed", s.chronometerBase, clock.getBase());
        if (s.valid) {
            long displayedSeconds = parseCountdown(clock.getText().toString());
            long expectedSeconds = Math.max(0, (s.chronometerBase - SystemClock.elapsedRealtime() + 999) / 1000);
            assertTrue("Countdown is incomplete or stale: " + clock.getText(), Math.abs(displayedSeconds - expectedSeconds) <= 2);
        } else assertEquals("Stopped recovery countdown", "—", clock.getText().toString());

        if (p.variant < 4) {
            text(root, s.isWarm ? WARM_LABELS[p.variant] : LABELS[p.variant], s.label);
            assertEquals("Old label state survived reapply", View.GONE,
                root.findViewById(s.isWarm ? LABELS[p.variant] : WARM_LABELS[p.variant]).getVisibility());
        }
        if (p.variant < 5) {
            ProgressBar progress=null;
            int visible=0;
            for(int progressId:PROGRESS) {
                ProgressBar candidate=root.findViewById(progressId);
                assertNotNull("Missing shared progress stage",candidate);
                if(candidate.getVisibility()==View.VISIBLE) {progress=candidate;visible++;}
            }
            boolean available=s.progressStage!=WidgetProgress.UNAVAILABLE;
            assertEquals("Exactly one live stage; no recovery stage",available?1:0,visible);
            assertEquals("Recovery must not advertise an inactive progress display",
                available?View.VISIBLE:View.GONE,root.findViewById(R.id.widget_progress_container).getVisibility());
            if(available) {
            assertNotNull(progress);
            assertEquals(1000,progress.getMax());assertEquals(s.progress,progress.getProgress());
            verifyProgressPixels(rendered,progress);
            Rect bar = bounds(root, progress), countdown = bounds(root, clock);
            View primaryView = root.findViewById(R.id.widget_primary_row);
            if (primaryView == null) primaryView = root.findViewById(R.id.widget_primary_group);
            Rect primary = bounds(root, primaryView);
            assertTrue("Progress overlaps the live countdown", bar.top >= countdown.bottom);
            assertTrue("Primary/progress gap expands or overlaps: " + (bar.top - primary.bottom),
                bar.top >= primary.bottom && bar.top - primary.bottom <= px(rendered.context, 16));
            assertEquals("Progress start edge", root.getPaddingLeft(), bar.left);
            assertEquals("Progress end edge", root.getWidth() - root.getPaddingRight(), bar.right);
            assertTrue("Progress geometry changed by state", Math.abs(bar.height() - px(rendered.context, 4)) <= 1);
            }
        }
        switch (p.variant) {
            case 1:
                text(root, R.id.tv_square_next_time, s.nextTime);
                text(root, R.id.tv_square_footer_left, s.footerText);
                text(root, R.id.tv_square_footer_right, s.currentStartTime);
                if (s.valid) assertGap(root, root.findViewById(R.id.widget_progress_container),
                    root.findViewById(R.id.widget_square_footer), 16, rendered.context);
                break;
            case 2:
                text(root, R.id.tv_banner_footer_top, s.footerText);
                text(root, R.id.tv_banner_footer_bottom, s.contextLine2); break;
            case 3:
                text(root, R.id.tv_wide_footer_top, s.footerText);
                text(root, R.id.tv_wide_footer_bottom, s.contextLine2);
                verifySchedule(rendered, CHIPS, CHIP_NAMES, CHIP_TIMES);
                break;
            case 4:
                text(root, R.id.tv_large_location, s.presentation.locationName);
                text(root, R.id.tv_large_hijri, s.hijriText);
                if (s.valid) text(root, R.id.tv_large_current_pill, s.footerText);
                else assertEquals(View.GONE,root.findViewById(R.id.tv_large_current_pill).getVisibility());
                text(root, R.id.tv_large_summary, s.valid ? s.nextName + " · " + s.nextTime : s.nextName);
                assertEquals("Full Table must match the approved information envelope",
                    summaryOnly ? View.GONE : View.VISIBLE,root.findViewById(R.id.widget_schedule_container).getVisibility());
                if(!summaryOnly) {
                    verifySchedule(rendered, ROWS, ROW_NAMES, ROW_TIMES);
                    assertEquals("Table daily grid", 2, scheduleRowCount(root, ROWS));
                }
                break;
            case 5:
                text(root, R.id.tv_lock_small_label, s.label);
                text(root, R.id.tv_lock_small_time, s.valid ? s.presentation.label("at") + " " + s.nextTime : ""); break;
            case 6:
                text(root, R.id.tv_lock_med_label, s.label);
                text(root, R.id.tv_lock_med_current, s.footerText);
                text(root, R.id.tv_lock_med_next, s.contextLine2); break;
            default: break;
        }
        if (s.valid && (p.variant==2 || p.variant==3 || p.variant==4 || p.variant==6))
            verifyColumnSeparation(rendered);
        verifyTextGeometry(root, root);
    }

    private void verifyColumnSeparation(Rendered rendered) {
        ViewGroup root = rendered.root;
        LinearLayout row = root.findViewById(R.id.widget_primary_row);
        if (row.getOrientation()!=LinearLayout.HORIZONTAL) return;
        int variant=rendered.profile.variant;
        int contextId=variant==2 ? R.id.tv_banner_footer_top : variant==3 ? R.id.tv_wide_footer_top
            : variant==4 ? R.id.tv_large_current_pill : R.id.tv_lock_med_current;
        Rect context=bounds(root,root.findViewById(contextId));
        Rect primary=bounds(root,root.findViewById(R.id.widget_primary_group));
        boolean rtl=root.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL;
        int gap=variant==4 ? (rtl ? context.left-primary.right : primary.left-context.right)
            : (rtl ? primary.left-context.right : context.left-primary.right);
        assertTrue("Mirrored columns lose their protected text gap: "+gap,
            gap>=px(rendered.context,4)-1);
    }

    private void verifySchedule(Rendered rendered, int[] cells, int[] names, int[] times) {
        ViewGroup root = rendered.root;
        PrayerWidgetHelper.WidgetTimelineState s = rendered.state;
        int height = root.findViewById(cells[0]).getHeight();
        float nameSize = ((TextView)root.findViewById(names[0])).getTextSize();
        float timeSize = ((TextView)root.findViewById(times[0])).getTextSize();
        int nameStyle = style(root.findViewById(names[0])), timeStyle = style(root.findViewById(times[0]));
        Rect first = bounds(root, root.findViewById(cells[0]));
        View anchor=root.findViewById(s.valid ? R.id.widget_progress_container : R.id.widget_primary_row);
        int progressToCells=first.top-bounds(root,anchor).bottom;
        assertTrue("Primary/progress-to-schedule gap expands or overlaps: "+progressToCells,
            progressToCells>=0 && progressToCells<=px(rendered.context,20)+1);
        int rowOneBottom = 0, rowTwoTop = Integer.MAX_VALUE;
        for (int i = 0; i < cells.length; i++) {
            View cell = root.findViewById(cells[i]);
            text(root, names[i], s.presentation.label(PRAYERS[i]));
            text(root, times[i], s.chipClock(PRAYERS[i]));
            assertTrue("Unequal prayer-cell heights: " + height + "/" + cell.getHeight(), Math.abs(height - cell.getHeight()) <= 1);
            assertEquals("ID-specific prayer-name weight (including Dhuhr)", nameStyle, style(root.findViewById(names[i])));
            assertEquals("ID-specific clock weight", timeStyle, style(root.findViewById(times[i])));
            assertEquals(nameSize, ((TextView)root.findViewById(names[i])).getTextSize(), .01f);
            assertEquals(timeSize, ((TextView)root.findViewById(times[i])).getTextSize(), .01f);
            assertTrue("Active fill stretches into spare host height", cell.getHeight() <= px(rendered.context,
                96 * rendered.context.getResources().getConfiguration().fontScale));
            Rect rect = bounds(root, cell);
            if (Math.abs(rect.top - first.top) <= 1) rowOneBottom = Math.max(rowOneBottom, rect.bottom);
            else rowTwoTop = Math.min(rowTwoTop, rect.top);
            long event = event(s, i);
            boolean current = s.valid && PRAYERS[i].equals(s.activePrayerId) && event == s.startTimestamp;
            boolean next = s.valid && PRAYERS[i].equals(s.nextPrayerId) && event == s.targetTimestamp;
            if (rendered.profile.variant == 3) {
                int expected = current ? R.drawable.widget_chip_current : next ? R.drawable.widget_chip_next : R.drawable.widget_chip_transparent;
                assertArrayEquals("Current/next/passed background: " + PRAYERS[i],
                    drawablePixels(rendered.context.getDrawable(expected)), drawablePixels(cell.getBackground()));
                int color = rendered.context.getColor(current ? R.color.w_accent : next ? R.color.w_text_secondary
                    : event < s.now && event < s.startTimestamp ? R.color.w_text_muted : R.color.w_text_secondary);
                assertEquals("Current/next/passed name color", color, ((TextView)root.findViewById(names[i])).getCurrentTextColor());
                assertEquals("Current/next/passed clock color", color, ((TextView)root.findViewById(times[i])).getCurrentTextColor());
            } else {
                // Table intentionally has only current decoration, never a Wide next outline.
                int[] actual = drawablePixels(cell.getBackground());
                if (current) assertArrayEquals("Active Table decoration", drawablePixels(rendered.context.getDrawable(R.drawable.widget_active_row)), actual);
                else assertTrue("Stale Table active decoration: " + PRAYERS[i], isTransparent(actual));
            }
            for (int j = 0; j < i; j++) {
                Rect other = bounds(root, root.findViewById(cells[j]));
                if (Math.abs(other.top - rect.top) <= 1) {
                    assertTrue("Unequal clock baselines in one schedule row", Math.abs(
                        baseline(root, root.findViewById(times[j])) - baseline(root, root.findViewById(times[i]))) <= 1);
                    assertTrue("Unequal name baselines in one schedule row", Math.abs(
                        baseline(root, root.findViewById(names[j])) - baseline(root, root.findViewById(names[i]))) <= 1);
                }
            }
        }
        if (rowTwoTop != Integer.MAX_VALUE) assertTrue("Schedule rows overlap or drift apart",
            rowTwoTop >= rowOneBottom && rowTwoTop - rowOneBottom <= px(rendered.context, 12));
    }

    private void verifyTextGeometry(ViewGroup root, View view) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view instanceof TextView) {
            TextView text = (TextView)view;
            if (text.getText().length() > 0) {
                String label = resourceName(text) + " '" + text.getText() + "'";
                Layout layout = text.getLayout();
                assertNotNull("No native text layout: " + label, layout);
                assertTrue("Zero-size text: " + label, text.getWidth() > 0 && text.getHeight() > 0);
                int width = text.getWidth() - text.getCompoundPaddingLeft() - text.getCompoundPaddingRight();
                int height = text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom();
                assertTrue("Clipped linebox: " + label + " required=" + layout.getHeight() + " available=" + height,
                    layout.getHeight() <= height + 1);
                assertEquals("Native layout lost trailing text: " + label, text.getText().length(),
                    layout.getLineEnd(layout.getLineCount() - 1));
                for (int line = 0; line < layout.getLineCount(); line++) {
                    assertEquals("Ellipsized critical content: " + label, 0, layout.getEllipsisCount(line));
                    assertTrue("Horizontal text clipping: " + label + " lineWidth=" + layout.getLineWidth(line) + " available=" + width,
                        layout.getLineLeft(line) >= -1 && layout.getLineRight(line) <= width + 1);
                    Rect glyphs = new Rect();
                    String content = text.getText().subSequence(layout.getLineStart(line), layout.getLineEnd(line)).toString();
                    text.getPaint().getTextBounds(content, 0, content.length(), glyphs);
                    int baseline = layout.getLineBaseline(line);
                    assertTrue("Glyph ascenders/descenders clipped: " + label,
                        baseline + glyphs.top >= -1 && baseline + glyphs.bottom <= height + 1);
                }
                View child = text;
                while (child != root) {
                    ViewGroup parent = (ViewGroup)child.getParent();
                    assertTrue("Text ancestor clips content: " + label,
                        child.getLeft() >= -1 && child.getTop() >= -1
                            && child.getRight() <= parent.getWidth() + 1 && child.getBottom() <= parent.getHeight() + 1);
                    child = parent;
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) verifyTextGeometry(root, group.getChildAt(i));
        }
    }

    private void runLegacyClockFitTransitions() {
        Rendered banner=null,table=null;
        try {
            Context context=configured("bn",1f);
            PrayerWidgetHelper.WidgetTimelineState snapshot=state(context,"bn","midday");
            cases++;
            banner=render(new Profile(3,250,220),2,context,snapshot);
            verify(banner,"bn",1f);
            cases++;
            table=render(new Profile(4,288,220),context,snapshot);
            verify(table,"bn",1f);
            for(float width : new float[]{250,288})
                table=runCase(new Profile(4,width,220),"bn",1f,"midday",width==250,table,width==250);
            table=runCase(new Profile(4,250,220),"bn",1f,"midday-24h",false,table,false);
            table=runCase(new Profile(4,250,220),"bn",1f,"midday",false,table,true);
        } catch(Throwable error) {record("legacy-clock-fit-transitions",error);}
        finally {
            if(banner!=null) surface.removeView(banner.host);
            if(table!=null) surface.removeView(table.host);
        }
    }

    private void runLegacySizeBoundaries() {
        try {
            Context context = configured("en",1f);
            PrayerWidgetHelper.WidgetTimelineState snapshot = state(context,"en","midday");
            for (int variant : new int[]{1,3}) {
                float width = variant == 1 ? 130 : 250;
                WidgetLayout compact = new WidgetLayout(context,snapshot,variant,width,110);
                float threshold = compact.minimumHeight;
                for (float height : new float[]{99,threshold-1,threshold,threshold+1}) {
                    cases++;
                    int expectedVariant = height < threshold ? (variant == 1 ? 0 : 2) : variant;
                    Rendered rendered = null;
                    String name = "legacy-"+VARIANTS[variant]+"-"+width+"x"+height;
                    try {
                        rendered = render(new Profile(variant,width,height),expectedVariant,context,snapshot);
                        verify(rendered,"en",1f);
                    } catch(Throwable error) { record(name,error); }
                    finally { if(rendered!=null) surface.removeView(rendered.host); }
                }
            }
        } catch(Exception error) { record("legacy-size-boundaries",error); }
    }

    private void runIndependentWideInstances() {
        Rendered narrow = null, wide = null;
        cases++;
        try {
            Context context = configured("en", 1f);
            PrayerWidgetHelper.WidgetTimelineState s = state(context, "en", "midday");
            narrow = render(new Profile(3, 360, 220), context, s);
            wide = render(new Profile(3, 600, 220), context, s);
            verify(narrow, "en", 1f);
            verify(wide, "en", 1f);
            assertEquals("A second Wide instance changed the narrow composition", 2, scheduleRowCount(narrow.root, CHIPS));
            assertEquals("Wide instance did not keep independent bounds", 1, scheduleRowCount(wide.root, CHIPS));
            assertEquals(((Chronometer)narrow.root.findViewById(COUNTDOWNS[3])).getBase(),
                ((Chronometer)wide.root.findViewById(COUNTDOWNS[3])).getBase());
        } catch (Throwable error) {
            record("independent-wide-instances", error);
        } finally {
            if (capture && narrow != null) capture(narrow, "wide-narrow-grid-simultaneous");
            if (capture && wide != null) capture(wide, "wide-six-columns-simultaneous");
            if (narrow != null) surface.removeView(narrow.host);
            if (wide != null) surface.removeView(wide.host);
        }
    }

    private void runExtraHeightCase(Profile normal) {
        Rendered compact = null, tall = null;
        String name = normal.name() + "-extra-height";
        cases++;
        try {
            Context context = configured("en", 1f);
            PrayerWidgetHelper.WidgetTimelineState s = state(context, "en", "midday");
            compact = render(normal, context, s);
            tall = render(new Profile(normal.variant, normal.width, normal.height + 240), context, s);
            verify(tall, "en", 1f);
            int[] cells = normal.variant == 3 ? CHIPS : ROWS;
            for (int id : cells) {
                int original = compact.root.findViewById(id).getHeight();
                int enlarged = tall.root.findViewById(id).getHeight();
                assertTrue("Extra host height stretches decorated cell: " + original + "/" + enlarged,
                    enlarged <= Math.max(original, px(context, 80)) + 1);
            }
            Rect progress = bounds(tall.root, tall.root.findViewById(R.id.widget_progress_container));
            Rect schedule = bounds(tall.root, tall.root.findViewById(R.id.widget_schedule_container));
            assertTrue("Extra host height opens an internal gulf", schedule.top - progress.bottom <= px(context, 20));
            Rect last = bounds(tall.root, tall.root.findViewById(cells[5]));
            Rect primary = bounds(tall.root, tall.root.findViewById(R.id.widget_primary_row));
            // Surplus belongs outside the coherent composition, not inside a weighted slot.
            int topSurplus = primary.top - tall.root.getPaddingTop();
            int bottomSurplus = tall.root.getHeight() - tall.root.getPaddingBottom() - last.bottom;
            // Table metadata precedes the primary block, so include it in the outer-group bound.
            if (normal.variant == 4) topSurplus = bounds(tall.root,
                tall.root.findViewById(R.id.widget_metadata)).top - tall.root.getPaddingTop();
            assertTrue("Surplus does not surround the content group", Math.abs(topSurplus - bottomSurplus) <= px(context, 12));
        } catch (Throwable error) {
            record(name, error);
        } finally {
            if (tall != null) {
                if (capture) capture(tall, name);
                surface.removeView(tall.host);
            }
            if (compact != null) surface.removeView(compact.host);
        }
    }

    private void assertEquivalent(View previous, View fresh) {
        assertEquals("Reapply visibility differs from fresh render", fresh.getVisibility(), previous.getVisibility());
        if (fresh.getVisibility() != View.VISIBLE) return;
        assertArrayEquals("Reapply retained a stale background", drawablePixels(fresh.getBackground()), drawablePixels(previous.getBackground()));
        assertEquals(fresh.getPaddingLeft(), previous.getPaddingLeft());
        assertEquals(fresh.getPaddingTop(), previous.getPaddingTop());
        assertEquals(fresh.getPaddingRight(), previous.getPaddingRight());
        assertEquals(fresh.getPaddingBottom(), previous.getPaddingBottom());
        if (fresh instanceof TextView) {
            TextView a = (TextView)previous, b = (TextView)fresh;
            // Native running countdowns can legitimately differ by a second across applications.
            if (!(fresh instanceof Chronometer)) assertEquals("Reapply retained stale text", b.getText().toString(), a.getText().toString());
            assertEquals(b.getCurrentTextColor(), a.getCurrentTextColor());
            assertEquals(b.getTextSize(), a.getTextSize(), .01f);
            assertEquals(style(b), style(a));
        }
        if (fresh instanceof ViewGroup) {
            ViewGroup a = (ViewGroup)previous, b = (ViewGroup)fresh;
            assertEquals("State unexpectedly changed hierarchy", b.getChildCount(), a.getChildCount());
            for (int i = 0; i < b.getChildCount(); i++) assertEquivalent(a.getChildAt(i), b.getChildAt(i));
        }
    }

    private void captureTransparentBackgrounds() {
        Context context = configured("en",1f);
        PrayerWidgetHelper.WidgetTimelineState snapshot;
        try { snapshot = state(context,"en","midday"); }
        catch(Exception error) { record("transparent-background-fixture",error); return; }
        Paint wallpaper = new Paint();
        String[] backgrounds = {"light","dark","busy"};
        for(int variant=5;variant<=6;variant++) {
            Rendered rendered = null;
            Bitmap image = null;
            try {
                rendered = render(NORMAL[variant],context,snapshot);
                image = Bitmap.createBitmap(rendered.root.getWidth(),rendered.root.getHeight(),Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(image);
                for(String background : backgrounds) {
                    canvas.drawColor(background.equals("light") ? 0xffe5e7e3 : 0xff182030);
                    if(background.equals("busy")) {
                        int band = px(context,48);
                        wallpaper.setColor(0xffd9dfd5);
                        for(int x=0;x<image.getWidth();x+=2*band)
                            canvas.drawRect(x,0,x+band,image.getHeight(),wallpaper);
                    }
                    rendered.root.draw(canvas);
                    String file = "widget-wallpaper-"+VARIANTS[variant]+"-"+background+"-"+theme(context)+".png";
                    try(FileOutputStream output = new FileOutputStream(new File(target.getCacheDir(),file))) {
                        assertTrue("Wallpaper review PNG encoding failed",image.compress(Bitmap.CompressFormat.PNG,100,output));
                    }
                    audit("transparent-review variant="+VARIANTS[variant]+" background="+background+" file="+file);
                }
            } catch(Throwable error) { record("transparent-background-"+variant,error); }
            finally {
                if(image!=null) image.recycle();
                if(rendered!=null) surface.removeView(rendered.host);
            }
        }
    }

    private void capturePickerPreviews() {
        Context context = configured("en",1f);
        PrayerWidgetHelper.WidgetTimelineState snapshot;
        try { snapshot = state(context,"en","midday"); }
        catch(Exception error) { record("picker-preview-fixture",error); return; }
        for(Profile profile : NORMAL) {
            Rendered rendered = null;
            Bitmap image = null;
            String file = "widget-preview-"+VARIANTS[profile.variant]+"-"+theme(context)+".png";
            try {
                rendered = render(profile,context,snapshot);
                // Static picker artwork only; live widgets retain native text and Chronometers.
                // Transparent keyguard previews need a clearly synthetic sample backdrop.
                View preview = profile.variant >= 5 ? rendered.host : rendered.root;
                image = Bitmap.createBitmap(preview.getWidth(),preview.getHeight(),Bitmap.Config.ARGB_8888);
                preview.draw(new Canvas(image));
                try(FileOutputStream output = new FileOutputStream(new File(target.getCacheDir(),file))) {
                    assertTrue("Picker preview PNG encoding failed",image.compress(Bitmap.CompressFormat.PNG,100,output));
                }
                audit("picker-preview variant="+VARIANTS[profile.variant]+" theme="+theme(context)
                    +" file="+file+" source=native-views");
            } catch(Throwable error) { record("picker-preview-"+profile.variant,error); }
            finally {
                if(image!=null) image.recycle();
                if(rendered!=null) surface.removeView(rendered.host);
            }
        }
    }

    private void captureOverview() {
        Rendered strip = null, square = null;
        Bitmap image = null;
        try {
            Context context = configured("en",1f);
            PrayerWidgetHelper.WidgetTimelineState snapshot = state(context,"en","midday");
            strip = render(NORMAL[0],context,snapshot);
            square = render(NORMAL[1],context,snapshot);
            image = Bitmap.createBitmap(px(context,376),px(context,204),Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(image);
            canvas.drawColor(theme(context).equals("dark") ? 0xff182030 : 0xffe0d6c2);
            canvas.save(); canvas.translate(px(context,12),px(context,12)); strip.host.draw(canvas); canvas.restore();
            canvas.save(); canvas.translate(px(context,184),px(context,12)); square.host.draw(canvas); canvas.restore();
            String file = "widget-design-overview-"+theme(context)+".png";
            try(FileOutputStream output = new FileOutputStream(new File(target.getCacheDir(),file))) {
                assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,output));
            }
            audit("capture overview theme="+theme(context)+" file="+file+" source=native-view-composite");
        } catch(Throwable error) { record("overview-capture",error); }
        finally {
            if(image!=null) image.recycle();
            if(strip!=null) surface.removeView(strip.host);
            if(square!=null) surface.removeView(square.host);
        }
    }

    private void capture(Rendered rendered, String name) {
        Bitmap bitmap = null;
        String filename = "widget-design-" + name.replace('.', '_') + "-" + theme(rendered.context) + ".png";
        try {
            bitmap = Bitmap.createBitmap(rendered.host.getWidth(), rendered.host.getHeight(), Bitmap.Config.ARGB_8888);
            rendered.host.draw(new Canvas(bitmap));
            File file = new File(target.getCacheDir(), filename);
            try (FileOutputStream output = new FileOutputStream(file)) {
                assertTrue("Native PNG encoding failed", bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
            }
            audit("capture variant=" + VARIANTS[rendered.profile.variant] + " size="
                + rendered.profile.width + "x" + rendered.profile.height + "dp pixels=" + bitmap.getWidth() + "x" + bitmap.getHeight()
                + " scale=" + rendered.context.getResources().getConfiguration().fontScale
                + " theme=" + theme(rendered.context) + " file=" + filename);
        } catch (Throwable error) {
            record("capture-" + name, error);
        } finally {
            if (bitmap != null) bitmap.recycle();
        }
    }

    private void record(String name, Throwable error) {
        String detail = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
        if (error.getStackTrace().length > 0) detail += " at " + error.getStackTrace()[0];
        if (detail.length() > 700) detail = detail.substring(0, 700);
        failures.add(name + ": " + detail);
        if (failures.size() <= 24) audit("failure " + name + " " + detail);
    }
    private String joinFailures() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.min(24, failures.size()); i++) result.append(failures.get(i)).append('\n');
        return result.toString();
    }
    private void audit(String line) {
        Bundle status = new Bundle();
        status.putString(Instrumentation.REPORT_KEY_STREAMRESULT, "\nWIDGET_DESIGN " + line + "\n");
        instrumentation.sendStatus(0, status);
    }
    private static String theme(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES ? "dark" : "light";
    }
    private static int px(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
    private static void text(View root, int id, String expected) {
        TextView text = root.findViewById(id);
        assertNotNull("Missing field " + id, text);
        assertEquals("Wrong bound critical text: " + resourceName(text), expected, text.getText().toString());
        if (!expected.isEmpty()) assertEquals("Required field hidden: " + resourceName(text), View.VISIBLE, text.getVisibility());
    }
    private static String resourceName(View view) {
        return view.getId() == View.NO_ID ? view.getClass().getSimpleName() : view.getResources().getResourceEntryName(view.getId());
    }
    private static int style(TextView view) {
        return view.getTypeface() == null ? 0 : view.getTypeface().getStyle();
    }
    private static Rect bounds(ViewGroup root, View view) {
        assertNotNull("Missing geometry subject", view);
        Rect result = new Rect(); view.getDrawingRect(result);
        root.offsetDescendantRectToMyCoords(view, result); return result;
    }
    private static int baseline(ViewGroup root, TextView text) {
        return bounds(root, text).top + text.getBaseline();
    }
    private static void assertGap(ViewGroup root, View before, View after, float maxDp, Context context) {
        Rect a = bounds(root, before), b = bounds(root, after);
        assertTrue("Incoherent group gap: " + (b.top - a.bottom),
            b.top >= a.bottom - 1 && b.top - a.bottom <= px(context, maxDp) + 1);
    }
    private static int scheduleRowCount(ViewGroup root, int[] cells) {
        List<Integer> rows = new ArrayList<>();
        for (int id : cells) {
            int top = bounds(root, root.findViewById(id)).top;
            boolean found = false;
            for (int row : rows) if (Math.abs(row - top) <= 1) found = true;
            if (!found) rows.add(top);
        }
        return rows.size();
    }
    private static long event(PrayerWidgetHelper.WidgetTimelineState s, int index) {
        switch(index) {
            case 0: return s.fajrTs;
            case 1: return s.sunriseTs;
            case 2: return s.dhuhrTs;
            case 3: return s.asrTs;
            case 4: return s.maghribTs;
            case 5: return s.ishaTs;
            default: throw new ArrayIndexOutOfBoundsException(index);
        }
    }
    private static long parseCountdown(String value) {
        String[] fields = value.trim().split(":");
        assertTrue("Native countdown lost digits/colon groups: " + value, fields.length == 2 || fields.length == 3);
        long seconds = 0;
        for (String field : fields) {
            assertFalse("Empty countdown field", field.isEmpty());
            int number = 0;
            for (int i = 0; i < field.length(); i++) {
                int digit = Character.digit(field.charAt(i), 10);
                assertTrue("Non-digit in live countdown: " + value, digit >= 0);
                number = number * 10 + digit;
            }
            seconds = seconds * 60 + number;
        }
        return seconds;
    }
    private static int[] drawablePixels(Drawable drawable) {
        Bitmap image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        if (drawable != null) {
            Rect old = new Rect(drawable.getBounds());
            drawable.setBounds(0, 0, 64, 64); drawable.draw(new Canvas(image)); drawable.setBounds(old);
        }
        int[] pixels = new int[64 * 64]; image.getPixels(pixels, 0, 64, 0, 0, 64, 64); image.recycle();
        return pixels;
    }
    private static boolean isTransparent(int[] pixels) {
        for (int pixel : pixels) if (Color.alpha(pixel) != 0) return false;
        return true;
    }
}
