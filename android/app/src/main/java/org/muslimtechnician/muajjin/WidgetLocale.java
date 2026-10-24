package org.muslimtechnician.muajjin;

import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.text.NumberFormat;
import java.util.Locale;

/** Device numeric presentation only; imported words and clock policy have separate owners. */
final class WidgetLocale {
    private WidgetLocale() {}

    static Locale resolve(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleManager manager = context.getSystemService(LocaleManager.class);
            if (manager != null) {
                LocaleList locales = manager.getSystemLocales();
                if (!locales.isEmpty()) return locales.get(0);
            }
        }
        // System resources are not the app's translated/overridden Context configuration.
        LocaleList locales = Resources.getSystem().getConfiguration().getLocales();
        if (!locales.isEmpty()) return locales.get(0);
        // Last resort when Android exposes no system locale: an actual available runtime
        // formatting locale, not a fabricated language or a persisted widget preference.
        return Locale.getDefault(Locale.Category.FORMAT);
    }

    /** Measurement only: mirrors the native Chronometer's MM:SS / H:MM:SS shape. */
    static String elapsedTime(long seconds, NumberFormat numbers) {
        seconds = Math.max(0, seconds);
        long hours = seconds / 3600;
        numbers.setGroupingUsed(false);
        numbers.setMinimumIntegerDigits(1);
        String prefix = hours > 0 ? numbers.format(hours) + ":" : "";
        numbers.setMinimumIntegerDigits(2);
        return prefix + numbers.format((seconds / 60) % 60) + ":" + numbers.format(seconds % 60);
    }
}
