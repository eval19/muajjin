package org.muslimtechnician.muajjin;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.StaticLayout;
import android.text.TextPaint;
import java.text.NumberFormat;

/** Presentation fit for the 2x1 Strip widget. Dates and formatted values belong to the snapshot. */
final class WidgetLayout {
    final int variant;
    final float width, height;
    final Context context;
    final PrayerWidgetHelper.WidgetTimelineState state;
    final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    final Paint.FontMetricsInt metrics = new Paint.FontMetricsInt();
    private final Typeface regularTypeface, boldTypeface;
    /** Exactly two widget type scales; Normal (1f) preserves the existing resources. */
    static final float LARGE_TEXT_SCALE = 1.2f;
    boolean compact, tight;
    final boolean summaryOnly = false;
    private boolean largeText;
    private final boolean nonAsciiDigits;
    private float textScale = 1f;
    float minimumHeight;
    float labelLineHeight;
    private final String countdownSample, countdownHourSample, countdownMinuteSample;
    private float regularCountdownWidth, compactCountdownWidth;
    private float regularCountdownHeight, compactCountdownHeight;
    private float largerRegularCountdownWidth, largerCompactCountdownWidth;
    private float largerRegularCountdownHeight, largerCompactCountdownHeight;

    WidgetLayout(Context context, PrayerWidgetHelper.WidgetTimelineState state,
                 int requestedVariant, float width, float height) {
        if (requestedVariant != 0) throw new IllegalArgumentException("Unknown widget variant");
        this.context = context;
        this.state = state;
        this.variant = 0;
        this.width = width;
        this.height = height;
        regularTypeface = WidgetTypeface.regular(context);
        boldTypeface = WidgetTypeface.bold(context);
        paint.setFontFeatureSettings("tnum");
        paint.setTextLocale("rtl".equals(state.presentation.direction)
            ? state.presentation.locale() : state.numericLocale);
        if (state.valid) {
            long seconds = Math.max(0, state.targetTimestamp - state.now) / 1000;
            NumberFormat numbers = NumberFormat.getIntegerInstance(state.numericLocale);
            countdownSample = WidgetLocale.elapsedTime(seconds, numbers);
            countdownHourSample = WidgetLocale.elapsedTime(seconds / 3600 * 3600 + 3599, numbers);
            countdownMinuteSample = WidgetLocale.elapsedTime(3599, numbers);
        } else {
            countdownSample = countdownHourSample = countdownMinuteSample = "—";
        }
        nonAsciiDigits = state.valid && countdownSample.length() > 0 && countdownSample.charAt(0) > '9';
        chooseScale();
    }

    private void chooseScale() {
        largeText = state.presentation.widgetLargeText;
        textScale = 1f;
        compact = false;
        tight = false;
        chooseComposition();
        if (!tight && height > 0 && minimumHeight > height) {
            tight = true;
            chooseComposition();
        }
        if (!compact && ((height > 0 && minimumHeight > height) || !fitsWidth())) {
            compact = true;
            tight = true;
            chooseComposition();
        }
        if (!largeText) return;
        // Enlarge the profile Normal already resolved, so compaction can never
        // make Larger text smaller than Normal. Only spacing may then tighten.
        textScale = LARGE_TEXT_SCALE;
        chooseComposition();
        if (!compact && !fitsWidth()) {
            compact = true;
            tight = true;
            chooseComposition();
        }
        if (!tight && height > 0 && minimumHeight > height) {
            tight = true;
            chooseComposition();
        }
        if (!compact && height > 0 && minimumHeight > height) {
            compact = true;
            chooseComposition();
        }
    }

    int px(float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    float textSize(int regular, int small) {
        return context.getResources().getDimension(compact ? small : regular) * textScale;
    }

    float labelSize() {
        return state.valid
            ? textSize(R.dimen.w_label_size, R.dimen.w_label_size_compact)
            : textSize(R.dimen.w_context_size, R.dimen.w_context_size_compact);
    }

    float countdownSize() {
        float opticalScale = nonAsciiDigits ? 1.12f : 1f;
        return textSize(R.dimen.w_countdown_small_size, R.dimen.w_countdown_small_size_compact) * opticalScale;
    }

    private boolean isTallCell() {
        return !tight && !compact && height >= 106 && height <= 145;
    }

    float horizontalInset() { return 12; }

    float verticalInset() {
        return tight ? (largeText ? 5 : 6) : (isTallCell() ? 12 : 8);
    }

    float progressGap() {
        return tight ? 3 : (isTallCell() ? 10 : 4);
    }

    float contentWidth() {
        return width > 0 ? Math.max(0, width - 2 * horizontalInset()) : 0;
    }

    private void font(float size, boolean bold) {
        paint.setTextSize(size);
        paint.setTypeface(bold ? boldTypeface : regularTypeface);
    }

    float textWidth(String text, float size, boolean bold) {
        font(size, bold);
        return paint.measureText(text) / context.getResources().getDisplayMetrics().density;
    }

    float lineHeight(float size, boolean bold) {
        font(size, bold);
        paint.getFontMetricsInt(metrics);
        return (metrics.bottom - metrics.top) / context.getResources().getDisplayMetrics().density;
    }

    private float textHeight(String text, float size, boolean bold) {
        return textHeight(text, size, bold, -1);
    }

    private float textHeight(String text, float size, boolean bold, float availableWidth) {
        if (text.isEmpty()) return lineHeight(size, bold);
        font(size, bold);
        int measuredWidth = availableWidth >= 0 ? px(availableWidth)
            : (int) Math.ceil(paint.measureText(text)) + px(4);
        StaticLayout.Builder builder = StaticLayout.Builder.obtain(text, 0, text.length(), paint, Math.max(1, measuredWidth))
            .setIncludePad(false);
        if (Build.VERSION.SDK_INT >= 28) builder.setUseLineSpacingFromFallbacks(true);
        return builder.build().getHeight() / context.getResources().getDisplayMetrics().density;
    }

    float countdownWidth() {
        boolean larger = textScale == LARGE_TEXT_SCALE;
        float cached = larger
            ? (compact ? largerCompactCountdownWidth : largerRegularCountdownWidth)
            : (compact ? compactCountdownWidth : regularCountdownWidth);
        if (cached > 0) return cached;
        float size = countdownSize();
        float width = Math.max(textWidth(countdownSample, size, true),
            Math.max(textWidth(countdownHourSample, size, true),
                textWidth(countdownMinuteSample, size, true))) + 2;
        if (larger) {
            if (compact) largerCompactCountdownWidth = width;
            else largerRegularCountdownWidth = width;
        } else if (compact) compactCountdownWidth = width;
        else regularCountdownWidth = width;
        return width;
    }

    float countdownHeight() {
        boolean larger = textScale == LARGE_TEXT_SCALE;
        float cached = larger
            ? (compact ? largerCompactCountdownHeight : largerRegularCountdownHeight)
            : (compact ? compactCountdownHeight : regularCountdownHeight);
        if (cached > 0) return cached;
        float size = countdownSize();
        float height = Math.max(textHeight(countdownSample, size, true),
            Math.max(textHeight(countdownHourSample, size, true),
                textHeight(countdownMinuteSample, size, true)));
        if (larger) {
            if (compact) largerCompactCountdownHeight = height;
            else largerRegularCountdownHeight = height;
        } else if (compact) compactCountdownHeight = height;
        else regularCountdownHeight = height;
        return height;
    }

    private void chooseComposition() {
        labelLineHeight = textHeight(state.label, labelSize(), true,
            !state.valid && width > 0 ? Math.max(0, contentWidth()) : -1);
        minimumHeight = requiredHeight();
    }

    private boolean fitsWidth() {
        return width <= 0 || !state.valid || countdownWidth() <= contentWidth();
    }

    boolean fitsWithinBounds() {
        return (height <= 0 || minimumHeight <= height) && fitsWidth();
    }

    private float requiredHeight() {
        if (!state.valid) {
            // The primary group's 6dp bottom margin already clears the
            // bottom-pinned bar, so only that band is reserved for it.
            return labelLineHeight + 6 + 2 * verticalInset();
        }
        float body = labelLineHeight + (tight ? 0 : 2) + countdownHeight() + progressGap() + 4;
        return body + 2 * verticalInset();
    }

    int layoutId() {
        return R.layout.widget_prayer_strip;
    }
}
