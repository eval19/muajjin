package org.muslimtechnician.muajjin;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** Installed provider metadata is the launcher-visible size and resize contract. */
@RunWith(AndroidJUnit4.class)
public class WidgetProviderInfoInstrumentedTest {
    // Strip, Square, Banner, Wide, Overview, Lock Small, Lock Medium.
    private static final int[][] CELL_HINTS = {
        {110, 40}, {110, 100}, {250, 40}, {250, 100}, {250, 100}, {110, 40}, {250, 40}
    };
    private static final int[][] RESIZE_MINIMUMS = {
        {110, 40}, {110, 100}, {180, 40}, {250, 100}, {250, 100}, {110, 40}, {250, 40}
    };

    @Test public void everyPrayerWidgetDeclaresExpectedFootprintAndResizeBounds() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String packageName = context.getPackageName();
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        Map<String, AppWidgetProviderInfo> installed = new HashMap<>();
        for (AppWidgetProviderInfo info : manager.getInstalledProviders()) {
            if (packageName.equals(info.provider.getPackageName()))
                installed.put(info.provider.getClassName(), info);
        }
        for (int i = 0; i < PrayerWidgetHelper.PROVIDERS.length; i++) {
            Class<?> provider = PrayerWidgetHelper.PROVIDERS[i];
            AppWidgetProviderInfo info = installed.get(provider.getName());
            assertNotNull("Missing installed provider info for " + provider.getName(), info);
            int expectedResize = i < 5
                ? AppWidgetProviderInfo.RESIZE_BOTH
                : AppWidgetProviderInfo.RESIZE_NONE;
            assertEquals(provider.getName() + " resizeMode", expectedResize, info.resizeMode);
            assertEquals(provider.getName() + " minWidth", CELL_HINTS[i][0], info.minWidth);
            assertEquals(provider.getName() + " minHeight", CELL_HINTS[i][1], info.minHeight);
            assertEquals(provider.getName() + " minResizeWidth", RESIZE_MINIMUMS[i][0], info.minResizeWidth);
            assertEquals(provider.getName() + " minResizeHeight", RESIZE_MINIMUMS[i][1], info.minResizeHeight);
        }
    }
}
