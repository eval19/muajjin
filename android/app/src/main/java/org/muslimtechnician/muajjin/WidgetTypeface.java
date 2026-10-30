package org.muslimtechnician.muajjin;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;

/** Native widget typography. API 24-25 keep the platform face; 26+ use the bundled family. */
final class WidgetTypeface {
    private WidgetTypeface() {}

    static Typeface regular(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Typeface family = context.getResources().getFont(R.font.ibm_plex_sans_arabic);
            if (family != null) return family;
        }
        return Typeface.DEFAULT;
    }

    static Typeface bold(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Typeface bold = context.getResources().getFont(R.font.ibm_plex_sans_arabic_bold);
            if (bold != null) return bold;
        }
        return Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
    }
}
