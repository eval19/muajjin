package org.muslimtechnician.muajjin;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Validated local-calendar horizon. Epochs are never reconstructed from display clocks. */
public final class PrayerSchedule {
    public static final int HORIZON_DAYS = 30;
    public final String timezone;
    public final long validFrom, validUntil;
    private final Day[] entries;

    public static final class Calculation {
        public final double latitude, longitude;
        public final int method, madhab;
        public Calculation(double latitude, double longitude, int method, int madhab) {
            if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90 ||
                !Double.isFinite(longitude) || longitude < -180 || longitude > 180 ||
                !(method==1||method==2||method==3||method==4||method==5||method==7||method==8||method==9||method==10||method==11||method==12||method==15) ||
                (madhab != 0 && madhab != 1)) throw new IllegalArgumentException("Invalid calculation settings");
            this.latitude=latitude; this.longitude=longitude; this.method=method; this.madhab=madhab;
        }
    }
    public static final class Day {
        public final String date;
        public final long previousIsha,fajr,sunrise,dhuhr,asr,maghrib,isha,nextFajr;
        public Day(String date,long previousIsha,long fajr,long sunrise,long dhuhr,long asr,long maghrib,long isha,long nextFajr) {
            if (date==null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException("Invalid local date");
            if(previousIsha<=0 || previousIsha>=fajr || fajr>=sunrise || sunrise>=dhuhr ||
                dhuhr>=asr || asr>=maghrib || maghrib>=isha || isha>=nextFajr)
                throw new IllegalArgumentException("Prayer boundaries must be strictly chronological");
            this.date=date;this.previousIsha=previousIsha;this.fajr=fajr;this.sunrise=sunrise;this.dhuhr=dhuhr;
            this.asr=asr;this.maghrib=maghrib;this.isha=isha;this.nextFajr=nextFajr;
        }
    }
    public PrayerSchedule(String timezone,long validFrom,long validUntil,Day[] days) {
        if(timezone==null || !isTimezone(timezone) || days==null || days.length!=HORIZON_DAYS)
            throw new IllegalArgumentException("Schedule requires a valid timezone and 30 local dates");
        TimeZone zone=TimeZone.getTimeZone(timezone); Calendar c=midnight(validFrom,zone);
        if(c.getTimeInMillis()!=validFrom) throw new IllegalArgumentException("validFrom must be local midnight");
        for(int i=0;i<days.length;i++) {
            Day d=days[i]; if(d==null || !dateKey(c).equals(d.date)) throw new IllegalArgumentException("Schedule date coverage is incomplete");
            long dayStart=c.getTimeInMillis()-2*86400000L;
            Calendar tomorrow=(Calendar)c.clone();tomorrow.add(Calendar.DATE,1);
            long dayEnd=c.getTimeInMillis()+3*86400000L;
            if(d.previousIsha<dayStart || d.fajr<dayStart || d.isha>dayEnd || d.nextFajr>dayEnd)
                throw new IllegalArgumentException("Prayer event timestamps do not belong to local date "+d.date);
            if(i>0 && (d.previousIsha!=days[i-1].isha || days[i-1].nextFajr!=d.fajr))
                throw new IllegalArgumentException("Schedule day edges are discontinuous");
            c=tomorrow;
        }
        if(c.getTimeInMillis()!=validUntil) throw new IllegalArgumentException("validUntil must be midnight after 30 local dates");
        this.timezone=timezone;this.validFrom=validFrom;this.validUntil=validUntil;this.entries=days.clone();
    }
    public void validateOuterBoundaries(Calculation config) {
        if(config==null) throw new IllegalArgumentException("Calculation configuration required to validate outer boundaries");
        TimeZone zone=TimeZone.getTimeZone(timezone);
        Calendar prev=midnight(validFrom,zone);prev.add(Calendar.DATE,-1);
        long expectedPreviousIsha=NativePrayerCalculator.calculate(prev,config.latitude,config.longitude,config.method,config.madhab)[5];
        Calendar next=midnight(validUntil,zone);
        long expectedNextFajr=NativePrayerCalculator.calculate(next,config.latitude,config.longitude,config.method,config.madhab)[0];
        if(entries[0].previousIsha!=expectedPreviousIsha || entries[entries.length-1].nextFajr!=expectedNextFajr) {
            throw new IllegalArgumentException("Schedule outer boundary events do not match astronomical calculations");
        }
    }
    public Day[] days() { return entries.clone(); }
    public Day dayAt(long now,TimeZone zone) {
        if(now<validFrom || now>=validUntil || !timezone.equals(zone.getID())) return null;
        String key=dateKey(midnight(now,zone));
        Day matching=null;
        for(Day d:entries) {
            if(now>=d.previousIsha && now<d.nextFajr) {
                if(d.date.equals(key)) return d;
                matching=d;
            }
        }
        return matching;
    }
    public long[] localEvents(long now,TimeZone zone,Day fallback) {
        Calendar start=midnight(now,zone),end=(Calendar)start.clone();end.add(Calendar.DATE,1);
        long from=start.getTimeInMillis(),until=end.getTimeInMillis();
        long[] result={fallback.fajr,fallback.sunrise,fallback.dhuhr,fallback.asr,fallback.maghrib,fallback.isha};
        for(Day d:entries) {
            if(d.fajr>=from&&d.fajr<until) result[0]=d.fajr;
            if(d.sunrise>=from&&d.sunrise<until) result[1]=d.sunrise;
            if(d.dhuhr>=from&&d.dhuhr<until) result[2]=d.dhuhr;
            if(d.asr>=from&&d.asr<until) result[3]=d.asr;
            if(d.maghrib>=from&&d.maghrib<until) result[4]=d.maghrib;
            if(d.isha>=from&&d.isha<until) result[5]=d.isha;
        }
        return result;
    }
    public boolean needsRenewal(long now,TimeZone zone) {
        Calendar threshold=midnight(validUntil,TimeZone.getTimeZone(timezone));threshold.add(Calendar.DATE,-2);
        return !timezone.equals(zone.getID()) || now<validFrom || now>=threshold.getTimeInMillis();
    }
    public static Calendar midnight(long epoch,TimeZone zone) {
        Calendar c=Calendar.getInstance(zone);c.setTimeInMillis(epoch);c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c;
    }
    public static String dateKey(Calendar c) {
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);f.setTimeZone(c.getTimeZone());return f.format(c.getTime());
    }
    private static boolean isTimezone(String id) {
        for(String known:TimeZone.getAvailableIDs()) if(known.equals(id)) return true;
        return false;
    }
    public static PrayerSchedule renew(long now,TimeZone zone,Calculation config) {
        Calendar first=midnight(now,zone);first.add(Calendar.DATE,-2);
        Calendar date=(Calendar)first.clone();date.add(Calendar.DATE,-1);
        long previousIsha=NativePrayerCalculator.calculate(date,config.latitude,config.longitude,config.method,config.madhab)[5];
        date.add(Calendar.DATE,1);
        long[] current=NativePrayerCalculator.calculate(date,config.latitude,config.longitude,config.method,config.madhab);
        Day[] days=new Day[HORIZON_DAYS];
        for(int i=0;i<HORIZON_DAYS;i++) {
            Calendar next=(Calendar)date.clone();next.add(Calendar.DATE,1);
            long[] following=NativePrayerCalculator.calculate(next,config.latitude,config.longitude,config.method,config.madhab);
            days[i]=new Day(dateKey(date),previousIsha,current[0],current[1],current[2],current[3],current[4],current[5],following[0]);
            previousIsha=current[5];current=following;
            date=next;
        }
        return new PrayerSchedule(zone.getID(),first.getTimeInMillis(),date.getTimeInMillis(),days);
    }
}
