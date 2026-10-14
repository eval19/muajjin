package org.muslimtechnician.muajjin;

import java.util.TimeZone;

/** One instant and stable prayer IDs, independent of Android and translated labels. */
public final class PrayerTimeline {
    public final PrayerSchedule.Day day;
    public final long now,start,target;
    public final String currentId,nextId;
    public final int progress;
    public final boolean warm;
    private PrayerTimeline(PrayerSchedule.Day day,long now,long start,long target,String currentId,String nextId) {
        this.day=day;this.now=now;this.start=start;this.target=target;this.currentId=currentId;this.nextId=nextId;
        this.progress=(int)Math.min(1000,Math.max(0,(now-start)*1000/(target-start)));
        this.warm=target>now && target-now<=900000L && !"dhuhr".equals(nextId);
    }
    public static PrayerTimeline at(PrayerSchedule schedule,long now,TimeZone zone) {
        if(schedule==null) return null;
        PrayerSchedule.Day d=schedule.dayAt(now,zone);if(d==null) return null;
        if(now<d.fajr) return new PrayerTimeline(d,now,d.previousIsha,d.fajr,"isha","fajr");
        if(now<d.sunrise) return new PrayerTimeline(d,now,d.fajr,d.sunrise,"fajr","sunrise");
        if(now<d.dhuhr) return new PrayerTimeline(d,now,d.sunrise,d.dhuhr,"","dhuhr");
        if(now<d.asr) return new PrayerTimeline(d,now,d.dhuhr,d.asr,"dhuhr","asr");
        if(now<d.maghrib) return new PrayerTimeline(d,now,d.asr,d.maghrib,"asr","maghrib");
        if(now<d.isha) return new PrayerTimeline(d,now,d.maghrib,d.isha,"maghrib","isha");
        return new PrayerTimeline(d,now,d.isha,d.nextFajr,"isha","fajr");
    }
    public long cosmeticTrigger() {
        long interval=Math.max(60000L,Math.min(300000L,(target-start)/120));
        long trigger=now+interval,warmAt=target-900000L;
        if(!"dhuhr".equals(nextId) && warmAt>now && warmAt<trigger) trigger=warmAt;
        return trigger<target ? trigger : 0;
    }
}
