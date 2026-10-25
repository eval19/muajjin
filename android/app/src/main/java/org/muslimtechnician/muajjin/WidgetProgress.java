package org.muslimtechnician.muajjin;

/** Sampled presentation stage for the existing interval-progress bar. */
enum WidgetProgress {
    UNAVAILABLE, GREEN, GOLD, AMBER, SOFT_RED;

    /** True when a live interval may display an elapsed fill at all. */
    static boolean available(boolean valid, long start, long target, long now) {
        return valid && target > start && now < target;
    }

    static WidgetProgress at(boolean valid, long start, long target, long now) {
        if (!available(valid, start, target, now)) return UNAVAILABLE;
        if (now < start) return GREEN;

        long duration = target - start;
        long remaining = target - now;
        if (remaining <= Math.min(duration / 10, 5 * 60000L)) return SOFT_RED;
        if (remaining <= Math.min(duration / 4, 15 * 60000L)) return AMBER;
        if (remaining <= Math.min(duration / 2, 30 * 60000L)) return GOLD;
        return GREEN;
    }
}
