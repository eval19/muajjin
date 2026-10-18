package org.muslimtechnician.muajjin;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real Android JSON/preferences boundaries; fixtures never touch the user's widget store. */
@RunWith(AndroidJUnit4.class)
public class NativeStorageInstrumentedTest {
    private Context debugContext() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (!context.getPackageName().endsWith(".debug")) throw new SecurityException("Use a debug test application");
        return context;
    }

    private Calendar referenceDate() {
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        date.clear();
        date.set(2024, Calendar.FEBRUARY, 29, 12, 0, 0);
        return date;
    }

    @Test public void astronomicalFailureRejectsStoredScheduleWithoutInventingAnotherLocation() throws Exception {
        Context context = debugContext();
        String name = "native-storage-test-" + UUID.randomUUID();
        SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        try {
            Calendar date = referenceDate();
            PrayerSchedule.Calculation valid = new PrayerSchedule.Calculation(21.3891, 39.8579, 1, 0);
            PrayerSchedule schedule = PrayerSchedule.renew(date.getTimeInMillis(), date.getTimeZone(), valid);
            NativeScheduleStore.saveSchedule(prefs, schedule, valid, "valid");
            assertNotNull(NativeScheduleStore.loadSchedule(prefs));
            PrayerSchedule.Calculation polar = new PrayerSchedule.Calculation(90, 0, 1, 0);
            String polarJson = NativeScheduleStore.calculationJson(polar).toString();
            assertTrue(prefs.edit().putString(NativeScheduleStore.CALCULATION, polarJson).commit());
            assertThrows(IllegalStateException.class, () -> schedule.validateOuterBoundaries(polar));
            assertNull(NativeScheduleStore.loadSchedule(prefs));
            assertEquals(polarJson, prefs.getString(NativeScheduleStore.CALCULATION, null));
            NativeScheduleStore.saveSchedule(prefs, schedule, valid, "repaired");
            PrayerSchedule restored = NativeScheduleStore.loadSchedule(prefs);
            assertEquals(schedule.dayAt(date.getTimeInMillis(), date.getTimeZone()).fajr,
                restored.dayAt(date.getTimeInMillis(), date.getTimeZone()).fajr);
        } finally {
            context.deleteSharedPreferences(name);
        }
    }

    @Test public void wrongPreferenceTypesStayUnavailableAndCanBeRepaired() throws Exception {
        Context context = debugContext();
        String name = "native-storage-test-" + UUID.randomUUID();
        SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        try {
            assertTrue(prefs.edit().putInt(NativeScheduleStore.SCHEDULE, 7)
                .putBoolean(NativeScheduleStore.CALCULATION, false)
                .putLong(NativeScheduleStore.PRESENTATION, 7L)
                .putInt("location_name", 7).putString("unrelated", "keep").commit());
            assertNull(NativeScheduleStore.loadSchedule(prefs));
            assertNull(NativeScheduleStore.loadCalculation(prefs));
            assertEquals("", NativeScheduleStore.loadPresentation(prefs).locationName);
            Calendar date = referenceDate();
            PrayerSchedule.Calculation config = new PrayerSchedule.Calculation(21.3891, 39.8579, 1, 0);
            PrayerSchedule schedule = PrayerSchedule.renew(date.getTimeInMillis(), date.getTimeZone(), config);
            NativeScheduleStore.saveSchedule(prefs, schedule, config, "repaired");
            NativeScheduleStore.savePresentation(prefs, NativePresentation.defaults("QA repaired"), "repaired");
            PrayerSchedule restored = NativeScheduleStore.loadSchedule(prefs);
            long[] events = NativePrayerCalculator.calculate(date, config.latitude, config.longitude, config.method, config.madhab);
            assertEquals(events[0], restored.dayAt(date.getTimeInMillis(), date.getTimeZone()).fajr);
            assertEquals("QA repaired", NativeScheduleStore.loadPresentation(prefs).locationName);
            assertEquals("keep", prefs.getString("unrelated", null));
        } finally {
            context.deleteSharedPreferences(name);
        }
    }

    @Test public void unsupportedHijriShiftDoesNotDiscardOtherPresentationOrPrayerData() throws Exception {
        Context context = debugContext();
        String name = "native-storage-test-" + UUID.randomUUID();
        SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        try {
            Calendar date = referenceDate();
            PrayerSchedule.Calculation config = new PrayerSchedule.Calculation(21.3891, 39.8579, 1, 0);
            PrayerSchedule schedule = PrayerSchedule.renew(date.getTimeInMillis(), date.getTimeZone(), config);
            PrayerTimeline timeline = PrayerTimeline.at(schedule, date.getTimeInMillis(), date.getTimeZone());
            long[] events = NativePrayerCalculator.calculate(date, config.latitude, config.longitude, config.method, config.madhab);
            for (long adjustment : new long[] {2147483648L, -2147483649L, 9007199254740991L, -9007199254740991L}) {
                JSONObject json = NativePresentation.defaults("QA storage").json();
                json.put("language", "ar");json.put("direction", "rtl");json.put("timeFormat", "24h");
                json.put("hijriAdjustment", adjustment);
                NativeScheduleStore.savePresentation(prefs, NativePresentation.parse(json), "shift-" + adjustment);
                NativePresentation restored = NativeScheduleStore.loadPresentation(prefs);
                assertEquals(adjustment, restored.hijriAdjustment);
                assertEquals("QA storage", restored.locationName);
                assertEquals("ar", restored.language);
                assertEquals("rtl", restored.direction);
                assertEquals("24h", restored.timeFormat);
                PrayerWidgetHelper.WidgetTimelineState state = new PrayerWidgetHelper.WidgetTimelineState(
                    context, restored, timeline, date.getTimeInMillis(), events);
                assertEquals("", state.hijriText);
                assertTrue(state.valid);
                assertTrue(state.targetTimestamp > date.getTimeInMillis());
                assertEquals(events[0], state.fajrTs);
            }
            JSONObject unsafe = NativePresentation.defaults("QA storage").json();
            unsafe.put("hijriAdjustment", 9007199254740992L);
            assertThrows(IllegalArgumentException.class, () -> NativePresentation.parse(unsafe));
        } finally {
            context.deleteSharedPreferences(name);
        }
    }
}
