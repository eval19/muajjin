package org.muslimtechnician.muajjin;

import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

/** Serialized by PrayerUpdateCoordinator. A failed validation never touches preferences. */
public final class NativeScheduleStore {
    public static final String SCHEDULE="prayer_schedule_json",CALCULATION="prayer_calculation_json",PRESENTATION="prayer_presentation_json";
    private NativeScheduleStore() {}
    public static long epoch(JSONObject json,String key) throws JSONException {
        Object raw=json.get(key);if(!(raw instanceof Number)) throw new IllegalArgumentException("Timestamp must be numeric: "+key);
        double d=((Number)raw).doubleValue();if(!Double.isFinite(d)||d<=0||d>8640000000000000d||d!=Math.rint(d)) throw new IllegalArgumentException("Invalid timestamp: "+key);
        return ((Number)raw).longValue();
    }
    public static int integer(JSONObject json,String key) throws JSONException {
        Object raw=json.get(key);if(!(raw instanceof Number)) throw new IllegalArgumentException("Expected integer: "+key);
        double d=((Number)raw).doubleValue();if(!Double.isFinite(d)||d!=Math.rint(d)||d<Integer.MIN_VALUE||d>Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid integer: "+key);return (int)d;
    }
    private static double number(JSONObject json,String key) throws JSONException {
        Object raw=json.get(key);if(!(raw instanceof Number)) throw new IllegalArgumentException("Expected number: "+key);return ((Number)raw).doubleValue();
    }
    public static PrayerSchedule.Calculation calculation(JSONObject json) throws JSONException {
        if(!"adhan-4.4.3".equals(NativePresentation.text(json,"engine",32))||!"device".equals(NativePresentation.text(json,"timezonePolicy",32))) throw new IllegalArgumentException("Unsupported calculation engine/timezone policy");
        return new PrayerSchedule.Calculation(number(json,"latitude"),number(json,"longitude"),integer(json,"method"),integer(json,"madhab"));
    }
    public static JSONObject calculationJson(PrayerSchedule.Calculation config) throws JSONException {
        JSONObject json=new JSONObject();json.put("engine","adhan-4.4.3");json.put("timezonePolicy","device");json.put("latitude",config.latitude);json.put("longitude",config.longitude);json.put("method",config.method);json.put("madhab",config.madhab);return json;
    }
    public static PrayerSchedule schedule(JSONObject json) throws JSONException {
        if(integer(json,"schema")!=2) throw new IllegalArgumentException("Unsupported schedule schema");
        JSONArray array=json.getJSONArray("days");if(array.length()!=30) throw new IllegalArgumentException("Schedule must contain 30 days");
        PrayerSchedule.Day[] days=new PrayerSchedule.Day[30];for(int i=0;i<30;i++) {
            JSONObject d=array.getJSONObject(i);days[i]=new PrayerSchedule.Day(NativePresentation.text(d,"date",10),epoch(d,"previousIsha"),epoch(d,"fajr"),epoch(d,"sunrise"),epoch(d,"dhuhr"),epoch(d,"asr"),epoch(d,"maghrib"),epoch(d,"isha"),epoch(d,"nextFajr"));
        }
        return new PrayerSchedule(NativePresentation.text(json,"timezone",128),epoch(json,"validFrom"),epoch(json,"validUntil"),days);
    }
    public static JSONObject scheduleJson(PrayerSchedule schedule) throws JSONException {
        JSONObject json=new JSONObject();json.put("schema",2);json.put("timezone",schedule.timezone);json.put("validFrom",schedule.validFrom);json.put("validUntil",schedule.validUntil);
        JSONArray array=new JSONArray();for(PrayerSchedule.Day day:schedule.days()) {
            JSONObject d=new JSONObject();d.put("date",day.date);d.put("previousIsha",day.previousIsha);d.put("fajr",day.fajr);d.put("sunrise",day.sunrise);d.put("dhuhr",day.dhuhr);d.put("asr",day.asr);d.put("maghrib",day.maghrib);d.put("isha",day.isha);d.put("nextFajr",day.nextFajr);array.put(d);
        }json.put("days",array);return json;
    }
    private static String storedString(SharedPreferences prefs,String key) {
        try { return prefs.getString(key,null); }
        catch(ClassCastException error) {
            Log.w("MuajjinSchedule","Invalid saved preference type; recovery required");return null;
        }
    }
    public static PrayerSchedule loadSchedule(SharedPreferences prefs) {
        String stored=storedString(prefs,SCHEDULE);if(stored==null) return null;
        try {
            PrayerSchedule sched=schedule(new JSONObject(stored));
            PrayerSchedule.Calculation config=loadCalculation(prefs);
            if(config==null) {
                Log.w("MuajjinSchedule","Stored schedule missing calculation configuration; recovery required");
                return null;
            }
            sched.validateOuterBoundaries(config);
            return sched;
        } catch(JSONException|IllegalArgumentException|IllegalStateException error) {
            // Old arrays have neither trustworthy first previous-Isha nor final next-Fajr. Do not
            // resurrect their single-day timestamp fallback. A saved calculation renews them below.
            Log.w("MuajjinSchedule","Stored schedule requires migration or recovery");return null;
        }
    }
    public static PrayerSchedule.Calculation loadCalculation(SharedPreferences prefs) {
        String stored=storedString(prefs,CALCULATION);if(stored==null) return null;
        try{return calculation(new JSONObject(stored));}catch(JSONException|IllegalArgumentException error){Log.w("MuajjinSchedule","Invalid saved calculation; open app to restore");return null;}
    }
    public static NativePresentation loadPresentation(SharedPreferences prefs) {
        String stored=storedString(prefs,PRESENTATION);if(stored!=null) try{return NativePresentation.parse(new JSONObject(stored));}catch(JSONException|IllegalArgumentException error){Log.w("MuajjinSchedule","Invalid presentation; using bounded baseline labels");}
        // Preserve the legacy user-provided location, but never reuse a transferred Hijri date.
        String location=storedString(prefs,"location_name");
        return NativePresentation.defaults(location==null?"":location);
    }
    public static void saveSchedule(SharedPreferences prefs,PrayerSchedule schedule,PrayerSchedule.Calculation config,String revision) throws JSONException {
        Map<String,String> values=new LinkedHashMap<>();values.put(SCHEDULE,scheduleJson(schedule).toString());values.put(CALCULATION,calculationJson(config).toString());
        if(values.get(SCHEDULE).equals(storedString(prefs,SCHEDULE))&&
           values.get(CALCULATION).equals(storedString(prefs,CALCULATION))&&
           revision!=null&&revision.equals(storedString(prefs,"prayer_schedule_revision"))) return;
        values.put("prayer_schedule_revision",revision);commit(prefs,values);
    }
    public static void savePresentation(SharedPreferences prefs,NativePresentation presentation,String revision) throws JSONException {
        String json=presentation.json().toString();if(json.equals(storedString(prefs,PRESENTATION))) return;
        Map<String,String> values=new LinkedHashMap<>();values.put(PRESENTATION,json);values.put("prayer_presentation_revision",revision);commit(prefs,values);
    }
    private static void commit(SharedPreferences prefs,Map<String,String> values) {
        Map<String,?> previous=prefs.getAll();SharedPreferences.Editor editor=prefs.edit();
        for(Map.Entry<String,String> entry:values.entrySet()) editor.putString(entry.getKey(),entry.getValue());
        if(editor.commit()) return;
        // SharedPreferences publishes in-memory values even if disk writing fails. Restore only
        // touched keys so a failed ACK cannot replace the process's known-good snapshot.
        SharedPreferences.Editor rollback=prefs.edit();
        for(String key:values.keySet()) {
            Object value=previous.get(key);
            if(value==null) rollback.remove(key);
            else if(value instanceof String) rollback.putString(key,(String)value);
            else if(value instanceof Boolean) rollback.putBoolean(key,(Boolean)value);
            else if(value instanceof Integer) rollback.putInt(key,(Integer)value);
            else if(value instanceof Long) rollback.putLong(key,(Long)value);
            else if(value instanceof Float) rollback.putFloat(key,(Float)value);
            else if(value instanceof java.util.Set<?>) {
                java.util.Set<String> strings=new java.util.HashSet<>();
                for(Object item:(java.util.Set<?>)value) if(item instanceof String) strings.add((String)item);
                rollback.putStringSet(key,strings);
            }
        }
        if(!rollback.commit()) Log.e("MuajjinSchedule","Could not durably restore preferences after failed transaction");
        throw new IllegalStateException("Could not durably save prayer data");
    }
}
