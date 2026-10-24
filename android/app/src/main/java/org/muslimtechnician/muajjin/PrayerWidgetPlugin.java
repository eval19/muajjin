package org.muslimtechnician.muajjin;

import android.app.Activity;
import android.app.AlarmManager;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
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
    public void getWidgetReliability(PluginCall call) {
        Context context=getContext();if(context==null){call.reject("Context is null");return;}
        try {
            Context application=context.getApplicationContext();
            AlarmManager alarms=(AlarmManager)application.getSystemService(Context.ALARM_SERVICE);
            boolean supported=Build.VERSION.SDK_INT>=Build.VERSION_CODES.S;
            boolean allowed=!supported||(alarms!=null&&alarms.canScheduleExactAlarms());
            boolean unrestricted=false;
            PowerManager power=(PowerManager)application.getSystemService(Context.POWER_SERVICE);
            if(power!=null) try {
                unrestricted=power.isIgnoringBatteryOptimizations(application.getPackageName());
            } catch(RuntimeException ignored) { /* OEM may restrict the query. */ }
            boolean widgets=false;
            AppWidgetManager manager=AppWidgetManager.getInstance(application);
            for(int[] ids:PrayerWidgetHelper.widgetIds(application,manager)) if(ids.length>0){widgets=true;break;}
            SharedPreferences prefs=application.getSharedPreferences(PrayerWidgetHelper.PREFS_NAME,Context.MODE_PRIVATE);
            JSObject result=new JSObject();
            result.put("exactAlarmSupported",supported);
            result.put("exactAlarmAllowed",allowed);
            result.put("batteryUnrestricted",unrestricted);
            result.put("widgetsPresent",widgets);
            result.put("nextTransition",prefs.getLong("next_transition_at",0L));
            result.put("lastDispatch",prefs.getLong("last_dispatch_at",0L));
            call.resolve(result);
        } catch(Exception error){call.reject("Unable to read widget reliability: "+error.getMessage(),error);}
    }

    @PluginMethod
    public void openExactAlarmSettings(PluginCall call) {
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.S){call.resolve();return;}
        Activity activity=getActivity();if(activity==null){call.reject("No activity");return;}
        Intent intent=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:"+activity.getPackageName()));
        if(!startSettings(activity,intent)){call.reject("Unable to open exact alarm settings");return;}
        call.resolve();
    }

    @PluginMethod
    public void openBatteryOptimizationSettings(PluginCall call) {
        Activity activity=getActivity();if(activity==null){call.reject("No activity");return;}
        Intent intent=new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        if(!startSettings(activity,intent)){call.reject("Unable to open battery settings");return;}
        call.resolve();
    }

    private boolean startSettings(Activity activity,Intent intent) {
        try {activity.startActivity(intent);return true;}
        catch(ActivityNotFoundException missing) {
            Intent details=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:"+activity.getPackageName()));
            try {activity.startActivity(details);return true;}
            catch(ActivityNotFoundException ignored) {return false;}
        }
    }

    @PluginMethod
    public void getScheduleStatus(PluginCall call) {
        Context context=getContext();if(context==null){call.reject("Context is null");return;}
        Context application=context.getApplicationContext();
        PrayerUpdateCoordinator.execute(()->{
            try {
                SharedPreferences prefs=application.getSharedPreferences(PrayerWidgetHelper.PREFS_NAME,Context.MODE_PRIVATE);
                PrayerSchedule schedule=NativeScheduleStore.loadSchedule(prefs);
                PrayerSchedule.Calculation config=NativeScheduleStore.loadCalculation(prefs);
                String revision=prefs.getString("prayer_schedule_revision",null);
                JSObject result=new JSObject();
                long now=System.currentTimeMillis();
                java.util.TimeZone zone=java.util.TimeZone.getDefault();
                if(schedule==null||config==null||revision==null||revision.isEmpty()||
                   now<schedule.validFrom||now>=schedule.validUntil||
                   !schedule.timezone.equals(zone.getID())||schedule.dayAt(now,zone)==null) {
                    result.put("exists",false);
                } else {
                    result.put("exists",true);
                    result.put("schema",2);
                    result.put("engine","adhan-4.4.3");
                    result.put("timezonePolicy","device");
                    result.put("timezone",schedule.timezone);
                    result.put("validFrom",schedule.validFrom);
                    result.put("validUntil",schedule.validUntil);
                    result.put("latitude",config.latitude);
                    result.put("longitude",config.longitude);
                    result.put("method",config.method);
                    result.put("madhab",config.madhab);
                    result.put("revision",revision);
                }
                call.resolve(result);
            } catch(Exception error){call.reject("Unable to read schedule status: "+error.getMessage(),error);}
        });
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
                schedule.validateOuterBoundaries(config);
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
