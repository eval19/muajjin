package org.muslimtechnician.muajjin;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class NativePresentation {
    private static final String[] KEYS={"fajr","sunrise","dhuhr","asr","maghrib","isha","current","next","now","in","at","startsIn","am","pm","remainingHours","remainingMinutes","recovery"};
    private static final String[] ENGLISH={"Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha","Current Salat","Next Salat","Now","in","at","Starts in","AM","PM","{{hours}}h {{minutes}}m","{{minutes}}m","Open Muajjin to restore prayer settings"};
    public final String language,direction,timeFormat,locationName;
    public final long hijriAdjustment;
    public final boolean hijriDateChangeAtMaghrib;
    private final Map<String,String> labels;
    private final String[] months;
    private NativePresentation(String language,String direction,String timeFormat,String location,long adjustment,boolean rollover,Map<String,String> labels,String[] months) {
        this.language=language;this.direction=direction;this.timeFormat=timeFormat;this.locationName=location;
        this.hijriAdjustment=adjustment;this.hijriDateChangeAtMaghrib=rollover;
        this.labels=Collections.unmodifiableMap(new LinkedHashMap<>(labels));this.months=months.clone();
    }
    public static NativePresentation defaults(String location) {
        Map<String,String> labels=new LinkedHashMap<>();for(int i=0;i<KEYS.length;i++) labels.put(KEYS[i],ENGLISH[i]);
        return new NativePresentation("en","ltr","system",location,0,true,labels,new String[]{"Muharram","Safar","Rabi al-Awwal","Rabi al-Thani","Jumada al-Awwal","Jumada al-Thani","Rajab","Sha'ban","Ramadan","Shawwal","Dhu al-Qi'dah","Dhu al-Hijjah"});
    }
    public static NativePresentation parse(JSONObject json) throws JSONException {
        if(NativeScheduleStore.integer(json,"schema")!=2) throw new IllegalArgumentException("Unsupported presentation schema");
        String language=text(json,"language",64),direction=text(json,"direction",3),format=text(json,"timeFormat",6);
        if(!direction.equals("ltr")&&!direction.equals("rtl")) throw new IllegalArgumentException("Invalid presentation direction");
        if(!format.equals("system")&&!format.equals("12h")&&!format.equals("24h")) throw new IllegalArgumentException("Invalid time format");
        String location=text(json,"locationName",256,true);
        Object rawAdjustment=json.get("hijriAdjustment");
        if(!(rawAdjustment instanceof Number)) throw new IllegalArgumentException("Hijri adjustment must be numeric");
        double numericAdjustment=((Number)rawAdjustment).doubleValue();
        if(!Double.isFinite(numericAdjustment)||numericAdjustment!=Math.rint(numericAdjustment)||Math.abs(numericAdjustment)>9007199254740991d)
            throw new IllegalArgumentException("Hijri adjustment must be an exact JavaScript integer");
        long adjustment=((Number)rawAdjustment).longValue();
        Object rollover=json.get("hijriDateChangeAtMaghrib");if(!(rollover instanceof Boolean)) throw new IllegalArgumentException("Hijri rollover must be boolean");
        JSONObject incoming=json.getJSONObject("labels");Map<String,String> labels=new LinkedHashMap<>();
        for(String key:KEYS) labels.put(key,text(incoming,key,8192));
        if(!labels.get("remainingHours").contains("{{hours}}")||!labels.get("remainingHours").contains("{{minutes}}")||!labels.get("remainingMinutes").contains("{{minutes}}"))
            throw new IllegalArgumentException("Missing remaining-time placeholders");
        JSONArray array=json.getJSONArray("months");if(array.length()!=12) throw new IllegalArgumentException("Exactly 12 Hijri months required");
        String[] months=new String[12];for(int i=0;i<12;i++) {Object value=array.get(i);if(!(value instanceof String)||((String)value).trim().isEmpty()||((String)value).length()>8192) throw new IllegalArgumentException("Invalid Hijri month");months[i]=(String)value;}
        return new NativePresentation(language,direction,format,location,adjustment,(Boolean)rollover,labels,months);
    }
    static String text(JSONObject json,String key,int max) throws JSONException {return text(json,key,max,false);}
    static String text(JSONObject json,String key,int max,boolean empty) throws JSONException {
        Object value=json.get(key);if(!(value instanceof String)||(!empty&&((String)value).trim().isEmpty())||((String)value).length()>max)
            throw new IllegalArgumentException("Invalid text: "+key);return (String)value;
    }
    public String label(String key) {String label=labels.get(key);return label==null?"":label;}
    public Locale locale() {Locale result=Locale.forLanguageTag(language.replace('_','-'));return result.getLanguage().isEmpty()?Locale.getDefault():result;}
    public SimpleDateFormat clockFormatter(Context context) {
        boolean is24=timeFormat.equals("24h")||(timeFormat.equals("system")&&android.text.format.DateFormat.is24HourFormat(context));
        SimpleDateFormat formatter=new SimpleDateFormat(is24?"HH:mm":"h:mm a",locale());
        DateFormatSymbols symbols=formatter.getDateFormatSymbols();symbols.setAmPmStrings(new String[]{label("am"),label("pm")});formatter.setDateFormatSymbols(symbols);
        return formatter;
    }
    public String clock(SimpleDateFormat formatter,long epoch) {
        return epoch<=0 ? "--:--" : formatter.format(new Date(epoch));
    }
    public String remaining(long minutes) {
        NumberFormat f=NumberFormat.getIntegerInstance(locale());f.setGroupingUsed(false);
        return (minutes>60?label("remainingHours"):label("remainingMinutes")).replace("{{hours}}",f.format(minutes/60)).replace("{{minutes}}",f.format(minutes>60?minutes%60:minutes));
    }
    public String hijri(int[] date) {NumberFormat f=NumberFormat.getIntegerInstance(locale());f.setGroupingUsed(false);return f.format(date[0])+" "+months[date[1]-1]+" "+f.format(date[2]);}
    public JSONObject json() throws JSONException {
        JSONObject json=new JSONObject();json.put("schema",2);json.put("language",language);json.put("direction",direction);json.put("timeFormat",timeFormat);json.put("locationName",locationName);
        json.put("hijriAdjustment",hijriAdjustment);json.put("hijriDateChangeAtMaghrib",hijriDateChangeAtMaghrib);
        json.put("labels",new JSONObject(labels));json.put("months",new JSONArray(months));return json;
    }
}
