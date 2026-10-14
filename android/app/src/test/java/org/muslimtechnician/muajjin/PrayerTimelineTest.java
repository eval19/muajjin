package org.muslimtechnician.muajjin;

import org.junit.Test;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class PrayerTimelineTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private Calendar date(int day) {
        Calendar c = Calendar.getInstance(UTC); c.clear(); c.set(2026, Calendar.OCTOBER, day); return c;
    }
    private PrayerSchedule schedule() {
        PrayerSchedule.Day[] days = new PrayerSchedule.Day[30];
        for (int i = 0; i < days.length; i++) {
            Calendar c = date(13); c.add(Calendar.DATE, i); long midnight = c.getTimeInMillis();
            days[i] = new PrayerSchedule.Day(PrayerSchedule.dateKey(c), midnight - 4*3600000L,
                midnight+5*3600000L,midnight+6*3600000L,midnight+12*3600000L,
                midnight+15*3600000L,midnight+18*3600000L,midnight+20*3600000L,midnight+29*3600000L);
        }
        Calendar end = date(13); end.add(Calendar.DATE,30);
        return new PrayerSchedule("UTC", date(13).getTimeInMillis(), end.getTimeInMillis(), days);
    }
    @Test public void beforeFajrUsesPreviousIshaNotTodaysIsha() {
        PrayerTimeline t = PrayerTimeline.at(schedule(),date(13).getTimeInMillis()+3600000L,UTC);
        assertEquals("isha", t.currentId); assertEquals("fajr",t.nextId);
        assertEquals(date(13).getTimeInMillis()-4*3600000L,t.start);
        assertTrue(t.progress > 0 && t.progress < 1000);
    }
    @Test public void exactBoundaryStartsNewPeriod() {
        PrayerTimeline t = PrayerTimeline.at(schedule(),date(13).getTimeInMillis()+5*3600000L,UTC);
        assertEquals("fajr",t.currentId); assertEquals("sunrise",t.nextId); assertEquals(0,t.progress);
    }
    @Test public void afterSunriseHasNoOpenPrayerAndNeverWarmDhuhr() {
        PrayerTimeline t = PrayerTimeline.at(schedule(),date(13).getTimeInMillis()+12*3600000L-60000L,UTC);
        assertEquals("",t.currentId); assertEquals("dhuhr",t.nextId); assertFalse(t.warm);
    }
    @Test public void translatedLabelsCannotChangeWarmPeriod() {
        PrayerTimeline t = PrayerTimeline.at(schedule(),date(13).getTimeInMillis()+15*3600000L-60000L,UTC);
        assertTrue(t.warm); assertEquals("asr",t.nextId);
    }
    @Test public void midnightSelectsTodaysRowsWhileIshaWindowCrossesDate() {
        PrayerTimeline t = PrayerTimeline.at(schedule(),date(14).getTimeInMillis(),UTC);
        assertEquals("2026-10-14",t.day.date); assertEquals("isha",t.currentId);
    }
    @Test public void expiredAndWrongTimezoneNeverReusePreviousRows() {
        assertNull(PrayerTimeline.at(schedule(),date(13).getTimeInMillis()-1,UTC));
        assertNull(PrayerTimeline.at(schedule(),schedule().validUntil,UTC));
        assertNull(PrayerTimeline.at(schedule(),date(13).getTimeInMillis(),TimeZone.getTimeZone("Asia/Dhaka")));
    }
    @Test public void renewalThresholdIsTwoCalendarDaysAndIdempotentAfterRenewal() {
        PrayerSchedule s = schedule(); Calendar threshold=date(13); threshold.add(Calendar.DATE,28);
        assertFalse(s.needsRenewal(threshold.getTimeInMillis()-1,UTC));
        assertTrue(s.needsRenewal(threshold.getTimeInMillis(),UTC));
        assertTrue(s.needsRenewal(s.validUntil+40*86400000L,UTC));
        assertTrue(s.needsRenewal(s.validFrom,TimeZone.getTimeZone("Europe/London")));
        assertFalse(s.needsRenewal(s.validFrom,UTC));
    }
    @Test public void missingLastNextFajrAndDiscontinuousEdgesAreRejected() {
        PrayerSchedule s=schedule(); PrayerSchedule.Day[] days=s.days(); PrayerSchedule.Day d=days[29];
        try {
            days[29]=new PrayerSchedule.Day(d.date,d.previousIsha,d.fajr,d.sunrise,d.dhuhr,d.asr,d.maghrib,d.isha,0);
            new PrayerSchedule(s.timezone,s.validFrom,s.validUntil,days); fail("missing boundary accepted");
        }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("chronological")); }
    }
    @Test public void discontinuousEdgesAndShortHorizonAreRejected() {
        PrayerSchedule s=schedule();PrayerSchedule.Day[] days=s.days();PrayerSchedule.Day d=days[1];
        days[1]=new PrayerSchedule.Day(d.date,d.previousIsha-60000L,d.fajr,d.sunrise,d.dhuhr,d.asr,d.maghrib,d.isha,d.nextFajr);
        try {new PrayerSchedule(s.timezone,s.validFrom,s.validUntil,days);fail("discontinuous edge accepted");}
        catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("discontinuous"));}
        try {new PrayerSchedule(s.timezone,s.validFrom,s.validUntil,new PrayerSchedule.Day[29]);fail("short horizon accepted");}
        catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("30"));}
    }
    @Test public void lastNightUsesActualNextFajrAndCosmeticCadenceStaysNonSecond() {
        PrayerSchedule s=schedule();long now=s.validUntil-60000L;PrayerTimeline t=PrayerTimeline.at(s,now,UTC);
        assertEquals(s.days()[29].nextFajr,t.target);assertEquals("fajr",t.nextId);
        assertTrue(t.cosmeticTrigger()-now>=60000L&&t.cosmeticTrigger()-now<=300000L);
    }
    @Test public void localRenewalBeyondOldHorizonProducesRealFollowingDay() {
        PrayerSchedule.Calculation config=new PrayerSchedule.Calculation(21.3891,39.8579,4,0);
        Calendar future=date(13);future.add(Calendar.DATE,65);
        PrayerSchedule renewed=PrayerSchedule.renew(future.getTimeInMillis(),UTC,config);
        assertFalse(renewed.needsRenewal(future.getTimeInMillis(),UTC));
        Calendar following=PrayerSchedule.midnight(renewed.validFrom,UTC);following.add(Calendar.DATE,30);
        long expected=NativePrayerCalculator.calculate(following,config.latitude,config.longitude,config.method,config.madhab)[0];
        assertEquals(expected,renewed.days()[29].nextFajr);
        PrayerTimeline current=PrayerTimeline.at(renewed,future.getTimeInMillis(),UTC);
        assertEquals("isha",current.currentId);
        assertEquals(NativePrayerCalculator.calculate(future,config.latitude,config.longitude,config.method,config.madhab)[0],current.target);
    }
    @Test public void invalidCalculationNeverTurnsIntoDefaultCity() {
        new PrayerSchedule.Calculation(0,0,4,0);
        try { new PrayerSchedule.Calculation(Double.NaN,0,4,0); fail(); } catch (IllegalArgumentException expected) { }
        try { new PrayerSchedule.Calculation(91,0,4,0); fail(); } catch (IllegalArgumentException expected) { }
        try { new PrayerSchedule.Calculation(1,1,6,0); fail(); } catch (IllegalArgumentException expected) { }
    }
    @Test public void horizonValidationUsesCalendarAcrossDst() {
        TimeZone zone=TimeZone.getTimeZone("America/New_York"); Calendar c=Calendar.getInstance(zone);
        c.clear(); c.set(2026,Calendar.OCTOBER,20); long from=c.getTimeInMillis();
        PrayerSchedule.Day[] days=new PrayerSchedule.Day[30];
        for(int i=0;i<30;i++) {
            Calendar prev=(Calendar)c.clone();prev.add(Calendar.DATE,-1);prev.set(Calendar.HOUR_OF_DAY,20);
            Calendar next=(Calendar)c.clone();next.add(Calendar.DATE,1);next.set(Calendar.HOUR_OF_DAY,5);
            long[] events=new long[6];int[] hours={5,6,12,15,18,20};
            for(int j=0;j<6;j++){Calendar e=(Calendar)c.clone();e.set(Calendar.HOUR_OF_DAY,hours[j]);events[j]=e.getTimeInMillis();}
            days[i]=new PrayerSchedule.Day(PrayerSchedule.dateKey(c),prev.getTimeInMillis(),events[0],events[1],events[2],events[3],events[4],events[5],next.getTimeInMillis());c.add(Calendar.DATE,1);
        }
        PrayerSchedule s=new PrayerSchedule(zone.getID(),from,c.getTimeInMillis(),days);
        assertEquals(30,s.days().length);assertEquals(30*86400000L+3600000L,s.validUntil-s.validFrom);
    }
    @Test public void remoteCoordinatesKeepDeviceCivilDatesAndDatedCurrentWindows() {
        for(String id:new String[]{"Pacific/Kiritimati","Etc/GMT+12","America/New_York"}) {
            TimeZone zone=TimeZone.getTimeZone(id);Calendar now=Calendar.getInstance(zone);
            now.clear();now.set(2026,Calendar.OCTOBER,13,12,0,0);
            PrayerSchedule.Calculation config=new PrayerSchedule.Calculation(0,-180,4,0);
            PrayerSchedule s=PrayerSchedule.renew(now.getTimeInMillis(),zone,config);
            long from=PrayerSchedule.midnight(now.getTimeInMillis(),zone).getTimeInMillis();
            Calendar next=PrayerSchedule.midnight(from,zone);next.add(Calendar.DATE,1);
            for(int hour=0;hour<24;hour++) {
                now.set(Calendar.HOUR_OF_DAY,hour);
                PrayerTimeline t=PrayerTimeline.at(s,now.getTimeInMillis(),zone);
                assertTrue(t.start<=now.getTimeInMillis());
                assertTrue(t.target>now.getTimeInMillis());
                long[] local=s.localEvents(now.getTimeInMillis(),zone,t.day);
                for(long event:local) assertTrue(event>=from && event<next.getTimeInMillis());
            }
        }
    }
    @Test(expected = IllegalArgumentException.class)
    public void shiftedEpochTimestampsAreRejectedByConstructor() {
        PrayerSchedule.Day[] days = schedule().days();
        Calendar shifted = date(14);
        shifted.add(Calendar.YEAR, 1);
        long shiftedMidnight = shifted.getTimeInMillis();
        days[0] = new PrayerSchedule.Day("2026-10-13", shiftedMidnight - 4*3600000L,
            shiftedMidnight+5*3600000L, shiftedMidnight+6*3600000L, shiftedMidnight+12*3600000L,
            shiftedMidnight+15*3600000L, shiftedMidnight+18*3600000L, shiftedMidnight+20*3600000L, shiftedMidnight+29*3600000L);
        Calendar end = date(13); end.add(Calendar.DATE, 30);
        new PrayerSchedule("UTC", date(13).getTimeInMillis(), end.getTimeInMillis(), days);
    }
    @Test(expected = Exception.class)
    public void unresolvableAstronomicalRenewalFailsExplicitlyWithoutFabricatingTimes() {
        Calendar now = Calendar.getInstance(UTC);
        now.clear();
        now.set(2026, Calendar.MAY, 22, 12, 0, 0);
        PrayerSchedule.Calculation config = new PrayerSchedule.Calculation(69, 18.96, 1, 0);
        PrayerSchedule.renew(now.getTimeInMillis(), UTC, config);
    }
    @Test(expected = Exception.class)
    public void intermediateAstronomicalCollapseFailsRenewalWithoutFabrication() {
        // May 20, 2026 produces initial valid days but collapses on May 21 at lat 69
        Calendar now = Calendar.getInstance(UTC);
        now.clear();
        now.set(2026, Calendar.MAY, 20, 12, 0, 0);
        PrayerSchedule.Calculation config = new PrayerSchedule.Calculation(69, 18.96, 1, 0);
        PrayerSchedule.renew(now.getTimeInMillis(), UTC, config);
    }
    @Test(expected = IllegalArgumentException.class)
    public void corruptFirstPreviousIshaIsRejectedByConstructor() {
        PrayerSchedule.Day[] days = schedule().days();
        Calendar shifted = date(13);
        shifted.add(Calendar.YEAR, -1);
        long shiftedMidnight = shifted.getTimeInMillis();
        PrayerSchedule.Day d = days[0];
        days[0] = new PrayerSchedule.Day(d.date, shiftedMidnight, d.fajr, d.sunrise, d.dhuhr, d.asr, d.maghrib, d.isha, d.nextFajr);
        Calendar end = date(13); end.add(Calendar.DATE, 30);
        new PrayerSchedule("UTC", date(13).getTimeInMillis(), end.getTimeInMillis(), days);
    }
    @Test(expected = IllegalArgumentException.class)
    public void corruptLastNextFajrIsRejectedByConstructor() {
        PrayerSchedule.Day[] days = schedule().days();
        Calendar shifted = date(13);
        shifted.add(Calendar.YEAR, 1);
        long shiftedMidnight = shifted.getTimeInMillis();
        PrayerSchedule.Day d = days[29];
        days[29] = new PrayerSchedule.Day(d.date, d.previousIsha, d.fajr, d.sunrise, d.dhuhr, d.asr, d.maghrib, d.isha, shiftedMidnight);
        Calendar end = date(13); end.add(Calendar.DATE, 30);
        new PrayerSchedule("UTC", date(13).getTimeInMillis(), end.getTimeInMillis(), days);
    }
    @Test
    public void validEngineDerivedSchedulePassesOuterBoundaryValidation() {
        PrayerSchedule.Calculation config = new PrayerSchedule.Calculation(21.3891, 39.8579, 4, 0);
        PrayerSchedule valid = PrayerSchedule.renew(date(15).getTimeInMillis(), UTC, config);
        valid.validateOuterBoundaries(config);
    }
    @Test(expected = IllegalArgumentException.class)
    public void slightlyShiftedFirstPreviousIshaFailsOuterBoundaryValidation() {
        PrayerSchedule.Calculation config = new PrayerSchedule.Calculation(21.3891, 39.8579, 4, 0);
        PrayerSchedule valid = PrayerSchedule.renew(date(15).getTimeInMillis(), UTC, config);
        PrayerSchedule.Day[] days = valid.days();
        PrayerSchedule.Day d = days[0];
        // Shift previousIsha by 1 hour (still within dayStart and < fajr, passes constructor)
        days[0] = new PrayerSchedule.Day(d.date, d.previousIsha - 3600000L, d.fajr, d.sunrise, d.dhuhr, d.asr, d.maghrib, d.isha, d.nextFajr);
        PrayerSchedule candidate = new PrayerSchedule(valid.timezone, valid.validFrom, valid.validUntil, days);
        candidate.validateOuterBoundaries(config);
    }
    @Test(expected = IllegalArgumentException.class)
    public void slightlyShiftedLastNextFajrFailsOuterBoundaryValidation() {
        PrayerSchedule.Calculation config = new PrayerSchedule.Calculation(21.3891, 39.8579, 4, 0);
        PrayerSchedule valid = PrayerSchedule.renew(date(15).getTimeInMillis(), UTC, config);
        PrayerSchedule.Day[] days = valid.days();
        PrayerSchedule.Day d = days[29];
        // Shift nextFajr by 1 hour (still within dayEnd and > isha, passes constructor; days[0] remains untouched)
        days[29] = new PrayerSchedule.Day(d.date, d.previousIsha, d.fajr, d.sunrise, d.dhuhr, d.asr, d.maghrib, d.isha, d.nextFajr + 3600000L);
        PrayerSchedule candidate = new PrayerSchedule(valid.timezone, valid.validFrom, valid.validUntil, days);
        candidate.validateOuterBoundaries(config);
    }
}
