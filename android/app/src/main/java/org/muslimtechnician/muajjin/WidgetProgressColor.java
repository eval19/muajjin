package org.muslimtechnician.muajjin;

/**
 * Pure OKLab interpolation of the theme progress-fill palette.
 *
 * <p>The approved palettes hold twelve opaque anchors at {@code index/11} of the
 * live interval, so there are eleven equal segments, not twelve time buckets.
 * Index 0 and 1 intentionally repeat the app green: the first 1/11 of an
 * interval never changes. Sampling is stateless and allocation-free; the
 * prepared {@link Palette} owns its input and its OKLab coordinates.
 *
 * <p>Conversion follows Björn Ottosson's OKLab description
 * (https://bottosson.github.io/posts/oklab/): sRGB decoding, the standard
 * transfer functions (0.04045 / 0.0031308), the reference matrices and cube
 * roots. Out-of-gamut channels are clamped before 8-bit encoding, and every
 * result is opaque.
 */
final class WidgetProgressColor {
    private WidgetProgressColor() {}

    /** Immutable prepared palette: opaque ARGB anchors plus their OKLab triples. */
    static final class Palette {
        private final int[] colors;
        private final double[] lab;

        private Palette(int[] colors, double[] lab) {
            this.colors = colors;
            this.lab = lab;
        }

        int size() { return colors.length; }

        int colorAt(int index) { return colors[index]; }
    }

    /**
     * Copies {@code colors} (forcing opacity) and prepares its OKLab coordinates
     * once. Later mutation of the caller's array cannot change this palette.
     */
    static Palette prepare(int[] colors) {
        if (colors == null || colors.length < 2) {
            throw new IllegalArgumentException("A progress palette needs at least two colors");
        }
        int[] owned = new int[colors.length];
        double[] lab = new double[colors.length * 3];
        for (int i = 0; i < colors.length; i++) {
            owned[i] = colors[i] | 0xFF000000;
            double[] anchor = toOklab(owned[i]);
            lab[i * 3] = anchor[0];
            lab[i * 3 + 1] = anchor[1];
            lab[i * 3 + 2] = anchor[2];
        }
        return new Palette(owned, lab);
    }

    /**
     * Returns the opaque fill color for the elapsed fraction of a live interval.
     *
     * <p>{@code now <= start} yields the first anchor and {@code now >= target}
     * yields the last, so a defensive exact endpoint never indexes a thirteenth
     * color. Reversed or zero-length intervals yield the first anchor without
     * dividing; callers still hide invalid intervals.
     */
    static int sample(Palette palette, long start, long target, long now) {
        int last = palette.colors.length - 1;
        if (target <= start || now <= start) return palette.colors[0];
        if (now >= target) return palette.colors[last];
        // Double division keeps millisecond precision without overflow.
        double position = ((now - start) / (double) (target - start)) * last;
        int segment = (int) position;
        if (segment >= last) return palette.colors[last];
        double local = position - segment;
        int from = palette.colors[segment];
        int next = palette.colors[segment + 1];
        if (from == next || local <= 0.0) return from;
        int offset = segment * 3;
        return fromOklab(
            palette.lab[offset] + (palette.lab[offset + 3] - palette.lab[offset]) * local,
            palette.lab[offset + 1] + (palette.lab[offset + 4] - palette.lab[offset + 1]) * local,
            palette.lab[offset + 2] + (palette.lab[offset + 5] - palette.lab[offset + 2]) * local);
    }

    /** Converts an opaque 8-bit sRGB color to OKLab; used by preparation and tests. */
    static double[] toOklab(int argb) {
        double r = linear((argb >> 16) & 0xFF);
        double g = linear((argb >> 8) & 0xFF);
        double b = linear(argb & 0xFF);
        double l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
        double m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
        double s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
        return new double[] {
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        };
    }

    /** Converts an OKLab triple back to an opaque 8-bit sRGB color. */
    static int fromOklab(double lightness, double a, double b) {
        double l = lightness + 0.3963377774 * a + 0.2158037573 * b;
        double m = lightness - 0.1055613458 * a - 0.0638541728 * b;
        double s = lightness - 0.0894841775 * a - 1.2914855480 * b;
        l = l * l * l;
        m = m * m * m;
        s = s * s * s;
        double r = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s;
        double g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s;
        double blue = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s;
        return 0xFF000000 | (encode(r) << 16) | (encode(g) << 8) | encode(blue);
    }

    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static int encode(double value) {
        if (value <= 0.0) return 0;
        if (value >= 1.0) return 255;
        double encoded = value <= 0.0031308
            ? 12.92 * value
            : 1.055 * Math.pow(value, 1.0 / 2.4) - 0.055;
        int channel = (int) Math.round(encoded * 255.0);
        return channel < 0 ? 0 : (channel > 255 ? 255 : channel);
    }
}
