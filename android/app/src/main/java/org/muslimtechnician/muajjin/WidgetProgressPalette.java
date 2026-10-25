package org.muslimtechnician.muajjin;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Build;

/**
 * Theme-qualified progress-fill palettes and the tint values delivered with one
 * widget dispatch.
 *
 * <p>Both fixed resource palettes are prepared once per process from
 * {@code res/values} and {@code res/values-night}. Only the night bit of a copy
 * of the current configuration changes; the rest of the host configuration is
 * preserved. Switching themes therefore selects an already prepared palette and
 * never repeats resource work or retains an Activity.
 */
final class WidgetProgressPalette {
    private static final int[] ANCHORS = {
        R.color.w_progress_fill_0, R.color.w_progress_fill_1, R.color.w_progress_fill_2,
        R.color.w_progress_fill_3, R.color.w_progress_fill_4, R.color.w_progress_fill_5,
        R.color.w_progress_fill_6, R.color.w_progress_fill_7, R.color.w_progress_fill_8,
        R.color.w_progress_fill_9, R.color.w_progress_fill_10, R.color.w_progress_fill_11
    };

    private static volatile WidgetProgressColor.Palette lightPalette;
    private static volatile WidgetProgressColor.Palette darkPalette;

    private WidgetProgressPalette() {}

    static WidgetProgressColor.Palette light(Context context) {
        prepare(context);
        return lightPalette;
    }

    static WidgetProgressColor.Palette dark(Context context) {
        prepare(context);
        return darkPalette;
    }

    private static void prepare(Context context) {
        if (lightPalette != null && darkPalette != null) return;
        synchronized (WidgetProgressPalette.class) {
            if (lightPalette != null && darkPalette != null) return;
            Context application = context.getApplicationContext();
            lightPalette = WidgetProgressColor.prepare(colors(application, false));
            darkPalette = WidgetProgressColor.prepare(colors(application, true));
        }
    }

    private static int[] colors(Context context, boolean night) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
            | (night ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        Context selected = context.createConfigurationContext(configuration);
        int[] colors = new int[ANCHORS.length];
        for (int i = 0; i < colors.length; i++) colors[i] = selected.getColor(ANCHORS[i]);
        return colors;
    }

    /**
     * Immutable fill colors and tint lists for one shared widget snapshot.
     *
     * <p>API 31+ only. Each needed theme color is sampled once and the two
     * ColorStateLists are reused by every bar-bearing variant and duplicate
     * widget instance in the dispatch. A null result means the legacy renderer
     * applies or that the live interval is unavailable.
     */
    static final class Fill {
        final int lightColor;
        final int darkColor;
        final ColorStateList lightList;
        final ColorStateList darkList;

        private Fill(int lightColor, int darkColor) {
            this.lightColor = lightColor;
            this.darkColor = darkColor;
            this.lightList = ColorStateList.valueOf(lightColor);
            this.darkList = ColorStateList.valueOf(darkColor);
        }

        static Fill of(Context context, boolean valid, long start, long target, long now) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null;
            if (!WidgetProgress.available(valid, start, target, now)) return null;
            return new Fill(
                WidgetProgressColor.sample(light(context), start, target, now),
                WidgetProgressColor.sample(dark(context), start, target, now));
        }
    }
}
