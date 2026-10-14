package org.muslimtechnician.muajjin;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;

/** Instrumentation-only host: widget rendering must not launch the WebView
 * and overwrite the native schedule/presentation fixture through its bridge. */
public final class WidgetTestActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(new FrameLayout(this));
    }
}
