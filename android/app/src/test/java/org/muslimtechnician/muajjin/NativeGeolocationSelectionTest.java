package org.muslimtechnician.muajjin;


import org.muslimtechnician.muajjin.plugins.NativeGeolocation;
import org.junit.Test;
import static org.junit.Assert.*;

public class NativeGeolocationSelectionTest {
    private static final long NOW = 1_800_000_000_000L;

    @Test public void freshAccurateCacheHitsImmediatelyAndStaleCacheIsRejected() {
        assertTrue(NativeGeolocation.isFreshCacheHit(true, 45f, NOW - 15_000L, NOW, 30_000L, true, true));
        assertFalse(NativeGeolocation.isFreshCacheHit(true, 45f, NOW - 45_000L, NOW, 30_000L, true, true));
        assertFalse(NativeGeolocation.isFreshCacheHit(true, 180f, NOW - 5_000L, NOW, 30_000L, true, true));
        assertTrue(NativeGeolocation.isFreshCacheHit(true, 1200f, NOW - 10_000L, NOW, 30_000L, false, true));
    }

    @Test public void coarseOnlyPermissionAcceptsImmediateApproximateFixWithoutWaiting() {
        assertTrue(NativeGeolocation.isImmediateFix(true, 1500f, false, true));
        assertFalse(NativeGeolocation.isImmediateFix(true, 120f, true, true));
        assertTrue(NativeGeolocation.isImmediateFix(true, 40f, true, true));
        assertTrue(NativeGeolocation.isGraceEligibleFix(true, 120f, true, true));
        assertFalse(NativeGeolocation.isGraceEligibleFix(true, 400f, true, true));
    }

    @Test public void betterAccuracyOrMuchNewerFixReplacesOlderBest() {
        assertTrue(NativeGeolocation.shouldPreferCandidate(21.4, 39.8, true, 12f, NOW, true, 80f, NOW - 1_000L));
        assertFalse(NativeGeolocation.shouldPreferCandidate(21.4, 39.8, true, 90f, NOW, true, 15f, NOW - 5_000L));
        assertTrue(NativeGeolocation.shouldPreferCandidate(21.4, 39.8, true, 90f, NOW, true, 15f, NOW - 180_000L));
        assertFalse(NativeGeolocation.shouldPreferCandidate(Double.NaN, 39.8, true, 5f, NOW, true, 50f, NOW));
    }
}
