package org.muslimtechnician.muajjin;

import android.content.Context;
import android.content.SharedPreferences;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONObject;

@CapacitorPlugin(name = "PrayerWidget")
public class PrayerWidgetPlugin extends Plugin {
    @PluginMethod
    public void getSystemClockFormat(PluginCall call) {
        JSObject result = new JSObject();
        result.put("timeFormat", android.text.format.DateFormat.is24HourFormat(getContext()) ? "24h" : "12h");
        call.resolve(result);
    }
    @PluginMethod
    public void updateSchedule(PluginCall call) {
        Context context=getContext();if(context==null){call.reject("Context is null");return;}
        Context application=context.getApplicationContext();
        PrayerUpdateCoordinator.execute(()->{
            try {
                String revision=NativePresentation.text(call.getData(),"revision",1024);
                JSONObject calculation=call.getObject("calculation"),wire=call.getObject("schedule");
                if(calculation==null||wire==null) throw new IllegalArgumentException("Calculation and schedule are required");
                PrayerSchedule.Calculation config=NativeScheduleStore.calculation(calculation);
                PrayerSchedule schedule=NativeScheduleStore.schedule(wire);
                java.util.TimeZone zone=java.util.TimeZone.getDefault();
                long now=System.currentTimeMillis();
                if(schedule.validFrom>now || schedule.validUntil<=now)
                    throw new IllegalArgumentException("Schedule must cover the current device time");
                // IANA aliases differ between WebView and Android. Recalculate
                // locally on a zone change rather than reject valid coordinates.
                if(!schedule.timezone.equals(zone.getID()) || schedule.dayAt(now,zone)==null)
                    schedule=PrayerSchedule.renew(now,zone,config);
                SharedPreferences prefs=application.getSharedPreferences(PrayerWidgetHelper.PREFS_NAME,Context.MODE_PRIVATE);
                // Validation is complete before a single durable transaction replaces either value.
                NativeScheduleStore.saveSchedule(prefs,schedule,config,revision);
                JSObject result=new JSObject();result.put("success",true);call.resolve(result);
                PrayerUpdateCoordinator.dispatchNow(application,false);
            } catch(Exception error){call.reject("Unable to save prayer schedule: "+error.getMessage(),error);}
        });
    }
    @PluginMethod
    public void updatePresentation(PluginCall call) {
        Context context=getContext();if(context==null){call.reject("Context is null");return;}
        Context application=context.getApplicationContext();
        PrayerUpdateCoordinator.execute(()->{
            try {
                String revision=NativePresentation.text(call.getData(),"revision",1024);
                JSONObject wire=call.getObject("presentation");if(wire==null)throw new IllegalArgumentException("Presentation is required");
                NativePresentation presentation=NativePresentation.parse(wire);
                SharedPreferences prefs=application.getSharedPreferences(PrayerWidgetHelper.PREFS_NAME,Context.MODE_PRIVATE);
                NativeScheduleStore.savePresentation(prefs,presentation,revision);
                JSObject result=new JSObject();result.put("success",true);call.resolve(result);
                PrayerUpdateCoordinator.dispatchNow(application,false);
            } catch(Exception error){call.reject("Unable to save prayer presentation: "+error.getMessage(),error);}
        });
    }
}
