package org.muslimtechnician.muajjin;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import java.text.NumberFormat;

/** Presentation fit only. Dates, formatted values and prayer state belong to the snapshot. */
final class WidgetLayout {
    static final String[] PRAYERS = {"fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"};
    final int variant;
    final float width, height;
    final Context context;
    final PrayerWidgetHelper.WidgetTimelineState state;
    final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    final Paint.FontMetricsInt metrics = new Paint.FontMetricsInt();
    private final Typeface regularTypeface, boldTypeface;
    /** Exactly two widget type scales; Normal (1f) preserves the existing resources. */
    static final float LARGE_TEXT_SCALE = 1.2f;
    /** A six-cell single row is dense; keep complete names at the cost of less padding. */
    private static final float SIX_COLUMN_INSET = 1f;
    boolean compact, tight, stacked, metadataStacked, wrapClocks, summaryOnly, dropLabel;
    private boolean largeText;
    private float textScale = 1f;
    int columns = 3;
    float minimumHeight;
    float nameLineHeight, clockLineHeight, labelLineHeight, contextLineHeight;
    float contextBlockHeight, compactCellInset;
    int scheduleTextPixels;
    private final String countdownSample, countdownHourSample, countdownMinuteSample;
    private float regularCountdownWidth, compactCountdownWidth;
    private float regularCountdownHeight, compactCountdownHeight;

    WidgetLayout(Context context, PrayerWidgetHelper.WidgetTimelineState state,
                 int requestedVariant, float width, float height) {
        this.context = context;
        this.state = state;
        this.width = width;
        this.height = height;
        regularTypeface = WidgetTypeface.regular(context);
        boldTypeface = WidgetTypeface.bold(context);
        variant = height > 0 && height < 100 && requestedVariant == 1 ? 0
            : height > 0 && height < 100 && requestedVariant == 3 ? 2 : requestedVariant;
        paint.setFontFeatureSettings("tnum");
        paint.setTextLocale("rtl".equals(state.presentation.direction)
            ? state.presentation.locale() : state.numericLocale);
        if(state.valid) {
            long seconds=Math.max(0,state.targetTimestamp-state.now)/1000;
            NumberFormat numbers=NumberFormat.getIntegerInstance(state.numericLocale);
            countdownSample=WidgetLocale.elapsedTime(seconds,numbers);
            countdownHourSample=WidgetLocale.elapsedTime(seconds/3600*3600+3599,numbers);
            countdownMinuteSample=WidgetLocale.elapsedTime(3599,numbers);
        } else {
            countdownSample=countdownHourSample=countdownMinuteSample="—";
        }
        chooseScale();
        if (variant == 3 && !stacked && columns == 3 && height > 0 && minimumHeight > height
                && canWrapSixClocks()) {
            columns = 6;
            wrapClocks = true;
            minimumHeight = requiredHeight();
        }
        if (variant == 4 && !fitsWithinBounds()) {
            summaryOnly = true;
            chooseScale();
        }
    }

    private void chooseScale() {
        // Keyguard widgets are fixed one-row surfaces; their regular type scale
        // cannot absorb a custom font's line height. Use the compact scale that
        // the 2x1/4x1 hosts were designed around. Keyguard hosts also keep the
        // Normal scale: a larger three-line stack cannot fit without dropping a
        // value, so Larger applies to the five home-screen variants.
        largeText = state.presentation.widgetLargeText && variant <= 4;
        textScale = 1f;
        compact = variant == 5 || variant == 6;
        tight = compact;
        chooseComposition();
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
        if (!tight && height > 0 && minimumHeight > height) {
            tight = true;
            chooseComposition();
        }
        if (variant == 0 && !compact && height > 0) {
            // Keep the promised Larger scale: only spacing may tighten here.
            float hero = height - 2 * verticalInset() - progressGap() - 4;
            if (countdownMinHeight() > hero) tight = true;
        }
    }

    float dimension(int resource) {
        return context.getResources().getDimension(resource)
            / context.getResources().getDisplayMetrics().density;
    }

    int px(float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    float textSize(int regular, int small) {
        return context.getResources().getDimension(compact ? small : regular) * textScale;
    }

    float labelSize() { return state.valid ? textSize(R.dimen.w_label_size, R.dimen.w_label_size_compact) : contextSize(); }
    float contextSize() { return textSize(R.dimen.w_context_size, R.dimen.w_context_size_compact); }
    float nameSize() { return textSize(R.dimen.w_schedule_name_size, R.dimen.w_schedule_name_size_compact); }
    float timeSize() { return textSize(R.dimen.w_schedule_time_size, R.dimen.w_schedule_time_size_compact); }
    float countdownSize() {
        if (variant == 0 || variant == 2 || variant == 5)
            return textSize(R.dimen.w_countdown_small_size, R.dimen.w_countdown_small_size_compact);
        if (variant == 4 || variant == 6)
            return textSize(R.dimen.w_countdown_table_size, R.dimen.w_countdown_table_size_compact);
        return textSize(R.dimen.w_countdown_size, R.dimen.w_countdown_size_compact);
    }

    float horizontalInset() { return variant == 0 || tight ? 12 : 16; }
    float verticalInset() { return variant == 0 || tight ? 8 : 16; }
    float cellHorizontalInset() {
        if (columns == 6) return sixColumnInset();
        return tight ? compactCellInset : 8;
    }
    private float sixColumnInset() {
        return Math.min(tight ? compactCellInset : 8f, SIX_COLUMN_INSET);
    }
    float cellVerticalInset() { return tight ? 6 : 8; }
    float cellLabelGap() { return tight ? 2 : 4; }
    float progressGap() { return variant == 0 || variant == 2 || tight ? 8 : 12; }
    float scheduleGap() { return tight ? 8 : 16; }
    float columnGap() { return dimension(R.dimen.w_gap_cells); }
    float rowGap() { return tight ? 6 : 8; }
    float contentWidth() { return width > 0 ? Math.max(0, width - 2 * horizontalInset()) : 0; }

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
            : (int)Math.ceil(paint.measureText(text)) + px(4);
        StaticLayout.Builder builder = StaticLayout.Builder.obtain(text, 0, text.length(), paint, Math.max(1, measuredWidth))
            .setIncludePad(false);
        if (Build.VERSION.SDK_INT >= 28) builder.setUseLineSpacingFromFallbacks(true);
        return builder.build().getHeight() / context.getResources().getDisplayMetrics().density;
    }

    float countdownWidth() {
        float cached=compact?compactCountdownWidth:regularCountdownWidth;
        if(cached>0) return cached;
        float size=countdownSize();
        float width=Math.max(textWidth(countdownSample,size,true),
            Math.max(textWidth(countdownHourSample,size,true),textWidth(countdownMinuteSample,size,true)))+2;
        if(compact) compactCountdownWidth=width;else regularCountdownWidth=width;
        return width;
    }

    float countdownHeight() {
        float cached=compact?compactCountdownHeight:regularCountdownHeight;
        if(cached>0) return cached;
        float size=countdownSize();
        // Arabic and Latin fallbacks report different line boxes; reserve the taller.
        float height=0;
        for (java.util.Locale locale : new java.util.Locale[]{state.presentation.locale(), state.numericLocale}) {
            paint.setTextLocale(locale);
            height=Math.max(height, Math.max(textHeight(countdownSample,size,true),
                Math.max(textHeight(countdownHourSample,size,true),textHeight(countdownMinuteSample,size,true))));
        }
        if(compact) compactCountdownHeight=height;else regularCountdownHeight=height;
        return height;
    }

    /** Complete glyph box; font metrics alone under-report fallback digit descenders. */
    float countdownGlyphHeight() {
        float size = countdownSize();
        font(size, true);
        android.graphics.Rect bounds = new android.graphics.Rect();
        float height = 0;
        for (java.util.Locale locale : new java.util.Locale[]{state.presentation.locale(), state.numericLocale}) {
            paint.setTextLocale(locale);
            for (String sample : new String[]{countdownSample, countdownHourSample, countdownMinuteSample}) {
                paint.getTextBounds(sample, 0, sample.length(), bounds);
                height = Math.max(height, bounds.height() / context.getResources().getDisplayMetrics().density + 2);
            }
        }
        return height;
    }

    /** Height reserved for the timer: the larger of layout and glyph boxes. */
    float countdownMinHeight() {
        return Math.max(countdownHeight(), countdownGlyphHeight());
    }

    private float heroWidth() {
        return Math.max(countdownWidth(), textWidth(state.label, labelSize(), true) + 2);
    }

    private float contextWidth() {
        if (variant == 4)
            return Math.max(textWidth(state.footerText, contextSize(), false),
                textWidth(state.summaryText, timeSize(), true));
        if (!state.valid) return 0;
        return Math.max(textWidth(state.footerText, contextSize(), false),
            textWidth(state.contextLine2, contextSize(), false));
    }

    private void chooseComposition() {
        labelLineHeight = textHeight(state.label, labelSize(), true);
        contextLineHeight = Math.max(textHeight(state.footerText, contextSize(), false),
            textHeight(state.contextLine2, contextSize(), false));
        compactCellInset = 4;
        if ((variant == 3 || variant == 4) && !summaryOnly) {
            nameLineHeight = lineHeight(nameSize(), false);
            clockLineHeight = lineHeight(timeSize(), true);
            float widest = 0;
            for (String id : PRAYERS) {
                nameLineHeight = Math.max(nameLineHeight, textHeight(state.presentation.label(id), nameSize(), false));
                clockLineHeight = Math.max(clockLineHeight, textHeight(state.chipClock(id), timeSize(), true));
                widest = Math.max(widest, textWidth(state.presentation.label(id), nameSize(), false));
                widest = Math.max(widest, textWidth(state.chipClock(id), timeSize(), true));
            }
            // Weighted rows allocate whole pixels; do not round a fractional text width down.
            scheduleTextPixels = (int)Math.ceil(widest * context.getResources().getDisplayMetrics().density) + 1;
        }
        float available = contentWidth();
        stacked = !state.valid && variant != 0 && variant != 5;
        metadataStacked = stacked && variant == 4;
        if (width > 0) {
            if (variant == 1) {
                float header = textWidth(state.label, labelSize(), true)
                    + textWidth(state.nextTime, contextSize(), false) + 10;
                float footer = textWidth(state.footerText, contextSize(), false)
                    + textWidth(state.currentStartTime, contextSize(), false) + 8;
                stacked = Math.max(header, footer) > available;
            } else if (variant == 2 || variant == 3 || variant == 6) {
                stacked = !state.valid || heroWidth() + contextWidth() + 8 > available;
            } else if (variant == 4) {
                float metadata = textWidth(state.presentation.locationName, contextSize(), true)
                    + textWidth(state.hijriText, contextSize(), false) + 8;
                stacked = contextWidth() + countdownWidth() + 8 > available;
                metadataStacked = stacked || metadata > available;
            }
        }
        if ((variant == 3 || variant == 4) && tight && width > 0) {
            int fittingInset = (cellWidthPixels(3) - scheduleTextPixels) / 2;
            compactCellInset = Math.max(px(1), Math.min(px(4), fittingInset))
                / context.getResources().getDisplayMetrics().density;
        }
        if (!state.valid && width > 0) {
            float allocated = variant == 1 && !stacked
                ? available - textWidth(state.nextTime, contextSize(), false) - 8 : available;
            labelLineHeight = textHeight(state.label, labelSize(), true, Math.max(0, allocated));
        }
        float contextAllocation = width <= 0 ? -1 : Math.max(0, stacked ? available : available - heroWidth() - 8);
        contextBlockHeight = textHeight(state.footerText, contextSize(), false, contextAllocation) + 4
            + textHeight(state.contextLine2, contextSize(), false, contextAllocation);
        columns = variant == 3 && !stacked && state.valid && width > 0 && sixColumnsFit() ? 6 : 3;
        minimumHeight = requiredHeight();
        if (variant == 0 && height > 0 && state.valid) {
            // The 2x1 keeps its countdown and progress line. The prayer label is
            // the only element that may give way when the host is too short.
            float hero = height - 2 * verticalInset() - progressGap() - 4;
            // Reserve a conservative timer line box so the label never squeezes it.
            dropLabel = labelLineHeight + 4 + countdownSize() * 1.6f > hero + 1;
        }
    }

    private int cellWidthPixels(int count) {
        return Math.max(0, (px(contentWidth()) - (count - 1) * px(columnGap())) / count);
    }

    private int requiredCellWidthPixels() {
        return scheduleTextPixels + 2 * px(cellHorizontalInset());
    }

    private boolean sixColumnsFit() {
        return cellWidthPixels(6) >= requiredCellWidthPixels();
    }

    private boolean fitsWidth() {
        if (width <= 0) return true;
        if (state.valid && countdownWidth() > contentWidth()) return false;
        if (summaryOnly || (variant != 3 && variant != 4)) return true;
        return wrapClocks ? canWrapSixClocks()
            : cellWidthPixels(columns) >= requiredCellWidthPixels();
    }

    boolean fitsWithinBounds() {
        return (height <= 0 || minimumHeight <= height) && fitsWidth();
    }

    private boolean canWrapSixClocks() {
        if (width <= 0) return false;
        int cellWidth = cellWidthPixels(6) - 2 * px(sixColumnInset()) - 1;
        if (cellWidth <= 0) return false;
        float density = context.getResources().getDisplayMetrics().density;
        for (String id : PRAYERS) {
            if (Math.ceil(textWidth(state.presentation.label(id), nameSize(), false) * density) > cellWidth) return false;
            String clock = state.chipClock(id);
            font(timeSize(), true);
            StaticLayout lines = StaticLayout.Builder.obtain(clock, 0, clock.length(), paint, cellWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build();
            if (lines.getLineCount() > 2) return false;
            for (int line = 0; line < lines.getLineCount(); line++)
                if (lines.getLineWidth(line) > cellWidth) return false;
        }
        return true;
    }

    float cellHeight() {
        float text = nameLineHeight + cellLabelGap()
            + clockLineHeight * (wrapClocks ? 2 : 1) + 2 * cellVerticalInset();
        return Math.max(tight ? 48 : 64, text);
    }

    private float tableMetadataHeight() {
        float location = textHeight(state.presentation.locationName, contextSize(), true);
        float hijri = textHeight(state.hijriText, contextSize(), false);
        return metadataStacked ? location + 4 + hijri : Math.max(location, hijri);
    }

    private float tableSummaryHeight() {
        float allocated = width <= 0 ? -1 : Math.max(0, contentWidth()
            - (state.valid && !stacked ? countdownWidth() + 8 : 0));
        String summary = state.summaryText;
        return (state.valid ? textHeight(state.footerText, contextSize(), false, allocated) : 0)
            + 4 + textHeight(summary, timeSize(), true, allocated);
    }

    private float requiredHeight() {
        float label = labelLineHeight;
        if (!state.valid) {
            float recovery = label;
            if (variant == 1)
                recovery = stacked ? label + 4 + lineHeight(contextSize(), false)
                    : Math.max(label, lineHeight(contextSize(), false));
            else if (variant == 4)
                recovery = tableMetadataHeight() + 8 + tableSummaryHeight();
            if (variant == 3 || (variant == 4 && !summaryOnly))
                recovery += scheduleGap() + 2 * cellHeight() + rowGap();
            return recovery + 2 * verticalInset();
        }
        float digits = countdownHeight();
        float contextLine = contextLineHeight;
        float primary = label + 4 + digits;
        float contextBlock = contextBlockHeight;
        float body;
        switch (variant) {
            case 0:
                body = primary + progressGap() + 4;
                break;
            case 1:
                body = (stacked ? label + contextLine + 4 : Math.max(label, contextLine))
                    + 4 + digits + progressGap() + 4 + 8
                    + (stacked ? 2 * contextLine + 4 : contextLine);
                break;
            case 2:
            case 3:
            case 6:
                body = stacked ? primary + (state.valid ? 4 + contextBlock : 0) : Math.max(primary, contextBlock);
                if (variant != 6) body += progressGap() + 4;
                if (variant == 3)
                    body += scheduleGap() + (columns == 6 ? cellHeight() : 2 * cellHeight() + rowGap());
                break;
            case 4:
                float summary = tableSummaryHeight();
                body = tableMetadataHeight() + 8
                    + (stacked ? summary + 4 + digits : Math.max(summary, digits))
                    + progressGap() + 4
                    + (summaryOnly ? 0 : scheduleGap() + 2 * cellHeight() + rowGap());
                break;
            case 5:
                body = primary + 4 + contextLine;
                break;
            default:
                throw new IllegalArgumentException("Unknown widget variant");
        }
        return body + 2 * verticalInset();
    }

    int layoutId() {
        switch (variant) {
            case 0: return R.layout.widget_prayer_strip;
            case 1: return stacked ? R.layout.widget_prayer_square_stacked : R.layout.widget_prayer_square;
            case 2: return stacked ? R.layout.widget_prayer_banner_stacked : R.layout.widget_prayer_banner;
            case 3: return columns == 6 ? R.layout.widget_prayer_wide
                : stacked ? R.layout.widget_prayer_wide_grid_stacked : R.layout.widget_prayer_wide_grid;
            case 4: return stacked ? R.layout.widget_prayer_large_stacked
                : metadataStacked ? R.layout.widget_prayer_large_metadata_stacked : R.layout.widget_prayer_large;
            case 5: return R.layout.widget_lock_small;
            case 6: return stacked ? R.layout.widget_lock_medium_stacked : R.layout.widget_lock_medium;
            default: throw new IllegalArgumentException("Unknown widget variant");
        }
    }
}
