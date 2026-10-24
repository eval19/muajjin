package org.muslimtechnician.muajjin;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Notification;
import android.app.NotificationManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.SizeF;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Run on an authorized debug test device with appwidget bind permission.
 * No WebView/network is needed to renew or render the expired schedule. */
@RunWith(AndroidJUnit4.class)
public class PrayerLifecycleInstrumentedTest {
    private static final class ObservedHostView extends AppWidgetHostView {
        private volatile CountDownLatch layoutReady=new CountDownLatch(0);
        private int expectedRoot,expectedRows,expectedWidth;
        ObservedHostView(Context context) { super(context); }
        void expectLayout(int root,int rows,int width) {
            expectedRoot=root;expectedRows=rows;expectedWidth=width;
            layoutReady=new CountDownLatch(1);
        }
        boolean awaitLayout() throws InterruptedException { return layoutReady.await(30,TimeUnit.SECONDS); }
        @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom) {
            super.onLayout(changed,left,top,right,bottom);
            if(expectedRoot==0 || getWidth()!=expectedWidth || findViewById(expectedRoot)==null)return;
            if(expectedRows!=0) {
                View first=findViewById(R.id.chip_fajr),last=findViewById(R.id.chip_isha);
                if(first==null || last==null)return;
                Rect a=new Rect(),b=new Rect();first.getDrawingRect(a);last.getDrawingRect(b);
                offsetDescendantRectToMyCoords(first,a);offsetDescendantRectToMyCoords(last,b);
                if((Math.abs(a.top-b.top)<=1?1:2)!=expectedRows)return;
            }
            layoutReady.countDown();
        }
    }

    private void worker(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        PrayerUpdateCoordinator.execute(() -> {
            try { action.run(); } catch (Throwable error) { failure.set(error); }
            finally { done.countDown(); }
        });
        assertTrue("Native worker did not finish", done.await(30, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
    private String visibleText(View view) {
        if (view.getVisibility() != View.VISIBLE) return "";
        StringBuilder text = new StringBuilder();
        if (view instanceof TextView) text.append(((TextView)view).getText()).append(' ');
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i=0;i<group.getChildCount();i++) text.append(visibleText(group.getChildAt(i)));
        }
        return text.toString();
    }
    private int checkChronometers(View view, long expected) {
        int count=0;
        if (view instanceof Chronometer) {
            Chronometer clock=(Chronometer)view;
            assertTrue(clock.isCountDown());
            assertTrue(Math.abs(expected-(System.currentTimeMillis()+clock.getBase()-SystemClock.elapsedRealtime()))<1000);
            count++;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) count+=checkChronometers(group.getChildAt(i),expected);
        }
        return count;
    }
    private void checkHostSizeDelivery(Instrumentation instrumentation,Context context,AppWidgetManager manager,
                                       AppWidgetHost host,int id,ObservedHostView wide,ScrollView scroll,long deadline) throws Exception {
        float density=context.getResources().getDisplayMetrics().density;
        Rect padding=AppWidgetHostView.getDefaultPaddingForWidget(context,
            new ComponentName(context,PrayerWidgetMediumProvider.class),null);
        int horizontal=padding.left+padding.right,vertical=padding.top+padding.bottom;
        LinearLayout column=(LinearLayout)scroll.getChildAt(0);
        if(Build.VERSION.SDK_INT>=31) {
            // Host API takes outer bounds and subtracts its default padding before provider callbacks.
            // https://developer.android.com/reference/android/appwidget/AppWidgetHostView#updateAppWidgetSize(android.os.Bundle,java.util.List)
            ArrayList<SizeF> sizes=new ArrayList<>(2);
            sizes.add(new SizeF(250+horizontal/density,220+vertical/density));
            sizes.add(new SizeF(600+horizontal/density,220+vertical/density));
            instrumentation.runOnMainSync(() -> wide.updateAppWidgetSize(new Bundle(),sizes));
            for(int width:new int[]{250,600,250}) {
                int pixels=Math.round(width*density)+horizontal;
                instrumentation.runOnMainSync(() -> {
                    wide.expectLayout(R.id.widget_wide_root,width==600?1:2,pixels);
                    wide.setLayoutParams(new LinearLayout.LayoutParams(pixels,Math.round(220*density)+vertical));
                    wide.requestLayout();
                });
                assertTrue("Host never applied its responsive size alternative at "+width+"dp",wide.awaitLayout());
                instrumentation.runOnMainSync(() -> assertTrue("Host alternative lost its real deadline",checkChronometers(wide,deadline)>0));
            }
        }
        Configuration orientation=new Configuration(context.getResources().getConfiguration());
        orientation.orientation=Configuration.ORIENTATION_LANDSCAPE;
        Context landscapeContext=context.createConfigurationContext(orientation);
        AtomicReference<ObservedHostView> landscape=new AtomicReference<>();
        int width=Math.round(600*density)+horizontal,height=Math.round(96*density)+vertical;
        instrumentation.runOnMainSync(() -> {
            ObservedHostView view=(ObservedHostView)host.createView(landscapeContext,id,manager.getAppWidgetInfo(id));
            landscape.set(view);view.expectLayout(R.id.widget_banner_root,0,width);
            column.addView(view,new LinearLayout.LayoutParams(width,height));
            Bundle range=manager.getAppWidgetOptions(id);
            if(Build.VERSION.SDK_INT>=31) range.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,null);
            range.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,250);
            range.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,600);
            range.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,96);
            range.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,220);
            manager.updateAppWidgetOptions(id,range);
        });
        assertTrue("Provider callback did not deliver the distinct legacy landscape alternative",landscape.get().awaitLayout());
        instrumentation.runOnMainSync(() -> {
            assertNotNull(landscape.get().findViewById(R.id.tv_banner_countdown));
            assertTrue("Legacy orientation alternative lost its real deadline",checkChronometers(landscape.get(),deadline)>0);
            column.removeView(landscape.get());
        });
    }


    @Test public void persistedHijriAdjustmentsKeepTheSameDatesAsTheWebCalculator() throws Exception {
        Calendar date=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        date.clear();date.set(2026,Calendar.OCTOBER,13,12,0,0);
        int[] adjustments={-120,120};
        int[][] expected={{29,12,1447},{3,9,1448}};
        String[] labels={"29 Dhu al-Hijjah 1447","3 Ramadan 1448"};
        for(int i=0;i<adjustments.length;i++) {
            org.json.JSONObject saved=NativePresentation.defaults("Makkah").json();
            saved.put("hijriAdjustment",adjustments[i]);
            NativePresentation presentation=NativePresentation.parse(saved);
            int[] actual=NativeHijriCalculator.calculate(date,presentation.hijriAdjustment,
                presentation.hijriDateChangeAtMaghrib,date.getTimeInMillis(),Long.MAX_VALUE);
            assertArrayEquals(expected[i],actual);
            assertEquals(labels[i],presentation.hijri(actual));
        }
    }

    @Test public void expiredOfflineScheduleRenewsAndEveryWidgetDisplaysRealNextPrayer() throws Exception {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Context context=instrumentation.getTargetContext();
        if(!context.getPackageName().endsWith(".debug")) throw new SecurityException("Use a debug test application");
        SharedPreferences prefs=context.getSharedPreferences(PrayerWidgetHelper.PREFS_NAME,Context.MODE_PRIVATE);
        Map<String,?> original=prefs.getAll();
        AppWidgetManager manager=AppWidgetManager.getInstance(context);
        AppWidgetHost host=new AppWidgetHost(context,480271) {
            @Override protected AppWidgetHostView onCreateView(Context viewContext,int id,AppWidgetProviderInfo info) {
                return new ObservedHostView(viewContext);
            }
        };
        AppWidgetHostView[] views=new AppWidgetHostView[PrayerWidgetHelper.PROVIDERS.length+1];
        int[] ids=new int[views.length];
        Activity activity=null;
        try {
            host.startListening();
            for(int i=0;i<ids.length;i++) {
                ids[i]=host.allocateAppWidgetId();
                int provider=i%PrayerWidgetHelper.PROVIDERS.length;
                int width=provider==4?250:360;
                Bundle options=new Bundle();
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,width);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,width);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,220);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,220);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,provider>=5?AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD:AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
                assertTrue("Authorize test-host widget binding",manager.bindAppWidgetIdIfAllowed(ids[i],new ComponentName(context,PrayerWidgetHelper.PROVIDERS[provider]),options));
            }
            TimeZone zone=TimeZone.getDefault();
            long now=System.currentTimeMillis();
            Calendar old=Calendar.getInstance(zone);old.setTimeInMillis(now);old.add(Calendar.DATE,-65);
            PrayerSchedule.Calculation config=new PrayerSchedule.Calculation(21.3891,39.8579,1,0);
            worker(() -> {
                try {
                    NativeScheduleStore.saveSchedule(prefs,PrayerSchedule.renew(old.getTimeInMillis(),zone,config),config,"test-expired");
                    org.json.JSONObject display=NativePresentation.defaults("Makkah").json();display.put("timeFormat","24h");
                    NativeScheduleStore.savePresentation(prefs,NativePresentation.parse(display),"test-display");
                    if(!prefs.edit().putBoolean("live_update_enabled",true).commit()) throw new IllegalStateException("Cannot configure notification fixture");
                    PrayerUpdateCoordinator.dispatchNow(context,true);
                } catch(Exception error) { throw new IllegalStateException(error); }
            });
            PrayerSchedule renewed=NativeScheduleStore.loadSchedule(prefs);
            assertTrue(renewed.validFrom<=now && renewed.validUntil>now);
            // Independent next-event selection from the engine, not the renderer/timeline.
            String[] names={"Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"};
            long next=Long.MAX_VALUE;String nextName="";
            for(int offset=-1;offset<=2;offset++) {
                Calendar date=Calendar.getInstance(zone);date.setTimeInMillis(now);date.add(Calendar.DATE,offset);
                long[] events=NativePrayerCalculator.calculate(date,config.latitude,config.longitude,config.method,config.madhab);
                for(int i=0;i<events.length;i++) if(events[i]>now && events[i]<next){next=events[i];nextName=names[i];}
            }
            final long expected=next;final String expectedName=nextName;
            instrumentation.runOnMainSync(() -> {
                for(int i=0;i<ids.length;i++) {
                    views[i]=host.createView(context,ids[i],manager.getAppWidgetInfo(ids[i]));
                    assertTrue(visibleText(views[i]),visibleText(views[i]).contains(expectedName));
                    assertTrue("Visible countdown missing",checkChronometers(views[i],expected)>0);
                }
            });
            assertTrue("Authorize debug notification permission",PrayerNotificationHelper.isConsumerActive(context));
            if(PrayerNotificationHelper.isConsumerActive(context)) {
                NotificationManager notifications=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                boolean found=false;
                for(android.service.notification.StatusBarNotification posted:notifications.getActiveNotifications()) if(posted.getId()==PrayerNotificationHelper.NOTIFICATION_ID) {
                    Notification notification=posted.getNotification();
                    found=true;
                    assertEquals(expected,notification.when);
                    assertTrue(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN));
                    assertEquals(100,notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX));
                    int progress=notification.extras.getInt(Notification.EXTRA_PROGRESS);
                    assertTrue(progress>=0 && progress<=100);
                }
                assertTrue("Enabled prayer notification was not posted",found);
            }
            // Resizing may select a compact layout; both layouts must retain the
            // same real deadline and the imported language, including duplicates.
            String[] translated={"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"};
            String[] prayerIds={"fajr","sunrise","dhuhr","asr","maghrib","isha"};
            String localizedNext=translated[java.util.Arrays.asList(names).indexOf(expectedName)];
            worker(() -> {
                try {
                    org.json.JSONObject display=NativePresentation.defaults("اختبار دون اتصال").json();
                    display.put("language","ar");display.put("direction","rtl");display.put("timeFormat","12h");
                    for(int i=0;i<prayerIds.length;i++) display.getJSONObject("labels").put(prayerIds[i],translated[i]);
                    NativeScheduleStore.savePresentation(prefs,NativePresentation.parse(display),"test-rtl");
                } catch(Exception error) { throw new IllegalStateException(error); }
            });
            int[] roots={R.id.widget_strip_root,R.id.widget_square_root,R.id.widget_banner_root,R.id.widget_wide_root,R.id.widget_large_root,R.id.widget_lock_small_root,R.id.widget_lock_medium_root};
            for(int height:new int[]{80,220}) {
                for(int id:ids) {
                    Bundle resized=manager.getAppWidgetOptions(id);
                    resized.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,height);
                    resized.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,height);
                    manager.updateAppWidgetOptions(id,resized);
                }
                worker(() -> PrayerUpdateCoordinator.dispatchNow(context,true));
                instrumentation.runOnMainSync(() -> {
                    for(int i=0;i<ids.length;i++) {
                        views[i]=host.createView(context,ids[i],manager.getAppWidgetInfo(ids[i]));
                        assertTrue(visibleText(views[i]),visibleText(views[i]).contains(localizedNext));
                        assertTrue("Resize lost the real countdown",checkChronometers(views[i],expected)>0);
                        View content=null;
                        for(int root:roots) if(views[i].findViewById(root)!=null) content=views[i].findViewById(root);
                        assertNotNull("No rendered widget content",content);
                        assertEquals("Imported RTL was lost on resize",View.LAYOUT_DIRECTION_RTL,content.getLayoutDirection());
                    }
                });
            }
            // The debug-only host cannot synchronize WebView settings over the fixture.
            activity=instrumentation.startActivitySync(new Intent(context,WidgetTestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Activity surface=activity;
            java.util.concurrent.atomic.AtomicReference<ScrollView> overlay=new java.util.concurrent.atomic.AtomicReference<>();
            instrumentation.runOnMainSync(() -> {
                ScrollView scroll=new ScrollView(surface);LinearLayout column=new LinearLayout(surface);column.setOrientation(LinearLayout.VERTICAL);
                scroll.setBackgroundColor(0xff182030);scroll.addView(column);overlay.set(scroll);
                for(int i=0;i<views.length;i++) {
                    TextView title=new TextView(surface);title.setText(PrayerWidgetHelper.PROVIDERS[i%PrayerWidgetHelper.PROVIDERS.length].getSimpleName());title.setTextColor(0xffffffff);column.addView(title);
                    float density=surface.getResources().getDisplayMetrics().density;
                    int width=i==4?(int)(250*density):ViewGroup.LayoutParams.MATCH_PARENT;
                    column.addView(views[i],new LinearLayout.LayoutParams(width,(int)(220*density)));
                }
                ((ViewGroup)surface.findViewById(android.R.id.content)).addView(scroll);
            });
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                int[] labels={R.id.tv_name_fajr,R.id.tv_name_sunrise,R.id.tv_name_dhuhr,R.id.tv_name_asr,R.id.tv_name_maghrib,R.id.tv_name_isha};
                for(int id:labels) {
                    TextView text=views[4].findViewById(id);
                    View row=(View)text.getParent();
                    assertTrue("Prayer glyphs clipped: top="+text.getTop()+" height="+text.getHeight()+" row="+row.getHeight(),
                        text.getHeight()>0 && text.getTop()>=0 && text.getTop()+text.getHeight()<=row.getHeight());
                    assertTrue("Prayer glyph layout clipped: layout="+text.getLayout().getHeight()+" view="+text.getHeight(),
                        text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
                    assertEquals("Minimum-width widget truncates a supported prayer label",0,text.getLayout().getEllipsisCount(0));
                }
            });
            instrumentation.runOnMainSync(() -> {
                int[] clocks={R.id.tv_chip_fajr_time,R.id.tv_chip_sunrise_time,R.id.tv_chip_dhuhr_time,R.id.tv_chip_asr_time,R.id.tv_chip_maghrib_time,R.id.tv_chip_isha_time};
                for(int id:clocks) {
                    TextView text=views[3].findViewById(id);
                    for(int line=0;line<text.getLayout().getLineCount();line++)
                        assertEquals("Wide widget hides part of the selected clock: "+text.getText(),0,text.getLayout().getEllipsisCount(line));
                }
            });
            ScrollView scroll=overlay.get();
            boolean capture=Boolean.parseBoolean(InstrumentationRegistry.getArguments().getString("captureSurfaces","false"));
            for(int page=0;capture && page<views.length;page++) {
                final int pageIndex=page;
                instrumentation.runOnMainSync(() -> scroll.scrollTo(0,Math.max(0,views[pageIndex].getTop()-24)));
                instrumentation.getUiAutomation().waitForIdle(150,2000);
                Bitmap image=instrumentation.getUiAutomation().takeScreenshot();
                try(FileOutputStream output=new FileOutputStream(new File(context.getCacheDir(),"widget-smoke-"+page+".png"))) {image.compress(Bitmap.CompressFormat.PNG,100,output);}
                image.recycle();
            }
            instrumentation.runOnMainSync(() -> {
                float density=context.getResources().getDisplayMetrics().density;
                StringBuilder defects=new StringBuilder();
                TextView squareClock=views[1].findViewById(R.id.tv_square_countdown);
                View squareProgress=views[1].findViewById(R.id.pb_square_progress);
                if(squareProgress==null || squareProgress.getVisibility()!=View.VISIBLE)
                    squareProgress=views[1].findViewById(R.id.pb_square_progress_warm);
                android.graphics.Rect clockBounds=new android.graphics.Rect();
                android.graphics.Rect progressBounds=new android.graphics.Rect();
                squareClock.getDrawingRect(clockBounds);
                squareProgress.getDrawingRect(progressBounds);
                views[1].offsetDescendantRectToMyCoords(squareClock,clockBounds);
                views[1].offsetDescendantRectToMyCoords(squareProgress,progressBounds);
                int squareGap=progressBounds.top-clockBounds.bottom;
                if(squareGap>20*density) defects.append("Square separates countdown/progress by ").append(squareGap/density).append("dp; ");
                int[] cells={R.id.chip_fajr,R.id.chip_sunrise,R.id.chip_dhuhr,R.id.chip_asr,R.id.chip_maghrib,R.id.chip_isha};
                int firstHeight=views[3].findViewById(cells[0]).getHeight();
                for(int cell:cells) {
                    View item=views[3].findViewById(cell);
                    if(Math.abs(item.getHeight()-firstHeight)>1)
                        defects.append("Wide prayer cells have unequal heights: ").append(firstHeight).append("/").append(item.getHeight()).append("; ");
                }
                int[] tableCells={R.id.row_fajr,R.id.row_sunrise,R.id.row_dhuhr,R.id.row_asr,R.id.row_maghrib,R.id.row_isha};
                for(int cell:tableCells) {
                    View item=views[4].findViewById(cell);
                    if(item.getHeight()>80*density*context.getResources().getConfiguration().fontScale)
                        defects.append("Table decoration stretches to ").append(item.getHeight()/density).append("dp; ");
                }
                assertEquals(defects.toString(),"",defects.toString());
            });
            checkHostSizeDelivery(instrumentation,context,manager,host,ids[3],(ObservedHostView)views[3],scroll,expected);
        } finally {
            if(activity!=null) {
                Activity closing=activity;
                instrumentation.runOnMainSync(closing::finish);
            }
            host.stopListening();host.deleteHost();
            worker(() -> {
                SharedPreferences.Editor restore=prefs.edit().clear();
                for(Map.Entry<String,?> entry:original.entrySet()) {
                    Object value=entry.getValue();String key=entry.getKey();
                    if(value instanceof String)restore.putString(key,(String)value);
                    else if(value instanceof Boolean)restore.putBoolean(key,(Boolean)value);
                    else if(value instanceof Integer)restore.putInt(key,(Integer)value);
                    else if(value instanceof Long)restore.putLong(key,(Long)value);
                    else if(value instanceof Float)restore.putFloat(key,(Float)value);
                    else if(value instanceof java.util.Set<?>) {
                        java.util.Set<String> strings=new java.util.HashSet<>();
                        for(Object item:(java.util.Set<?>)value) if(item instanceof String) strings.add((String)item);
                        restore.putStringSet(key,strings);
                    }
                }
                if(!restore.commit()) throw new IllegalStateException("Could not restore native test preferences");
                PrayerUpdateCoordinator.dispatchNow(context,true);
            });
        }
    }
}
