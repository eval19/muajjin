package org.muslimtechnician.muajjin.plugins;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.hardware.GeomagneticField;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.annotation.Permission;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

@CapacitorPlugin(
    name = "NativeGeolocation",
    permissions = {
        @Permission(strings = { Manifest.permission.ACCESS_FINE_LOCATION }, alias = "location"),
        @Permission(strings = { Manifest.permission.ACCESS_COARSE_LOCATION }, alias = "coarseLocation")
    }
)
public class NativeGeolocation extends Plugin {

    private static final String TAG = "NativeGeolocation";
    private LocationManager locationManager;
    private SensorManager sensorManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, PositionRequest> positionRequests = new HashMap<>();
    private volatile boolean destroyed;
    private volatile boolean paused;
    private final Map<PluginCall, Runnable> headingCleanup = new HashMap<>();
    private final Map<String, Runnable> headingWatches = new HashMap<>();

    @Override
    public void load() {
        locationManager = (LocationManager) getContext().getSystemService(getContext().LOCATION_SERVICE);
        sensorManager = (SensorManager) getContext().getSystemService(getContext().SENSOR_SERVICE);
    }

    @PluginMethod
    public void getCurrentPosition(PluginCall call) {
        mainHandler.post(() -> {
            if (destroyed) { call.reject("Location request cancelled", "LOCATION_CANCELLED"); return; }
            String id = call.getString("requestId", call.getCallbackId());
            if (id.isEmpty() || id.length() > 128 || positionRequests.containsKey(id)) {
                call.reject("Invalid or duplicate location request", "INVALID_REQUEST");
                return;
            }
            PositionRequest request = new PositionRequest(id, call);
            positionRequests.put(id, request);
            bridge.saveCall(call);
            if (!hasLocationPermission()) {
                requestAllPermissions(call, "locationPermsCallback");
            } else {
                request.start();
            }
        });
    }

    private boolean hasFineLocationPermission() {
        return getContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationPermission() {
        return hasFineLocationPermission() ||
            getContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @PermissionCallback
    private void locationPermsCallback(PluginCall call) {
        mainHandler.post(() -> {
            PositionRequest request = positionRequests.get(call.getString("requestId", call.getCallbackId()));
            if (request == null) return; // Cancelled while the permission sheet was open.
            if (!hasLocationPermission()) request.finish(null, "Location permission denied", "PERMISSION_DENIED");
            else request.start();
        });
    }

    @PluginMethod
    public void cancelPosition(PluginCall call) {
        String id = call.getString("requestId");
        mainHandler.post(() -> {
            PositionRequest request = positionRequests.get(id);
            if (request != null) request.finish(null, "Location request cancelled", "LOCATION_CANCELLED");
            call.resolve();
        });
    }

    static final long GRACE_WINDOW_MS = 2500L;
    static final float IMMEDIATE_FINE_ACCURACY_M = 50f;
    static final float GRACE_FINE_ACCURACY_M = 250f;
    static final float COARSE_ACCURACY_M = 3000f;
    static final float FRESH_CACHE_FINE_ACCURACY_M = 100f;
    static final long STALE_THRESHOLD_MS = 120_000L;

    public static boolean isFreshCacheHit(boolean hasAccuracy, float accuracy, long locationTimeMs,
                                          long nowMs, long maximumAgeMs, boolean hasFine, boolean highAccuracy) {
        if (maximumAgeMs <= 0L || !hasAccuracy) return false;
        long age = nowMs - locationTimeMs;
        if (age < 0L || age > maximumAgeMs) return false;
        float maxAccuracy = (hasFine && highAccuracy) ? FRESH_CACHE_FINE_ACCURACY_M : COARSE_ACCURACY_M;
        return accuracy <= maxAccuracy;
    }

    static boolean isFreshCacheHit(Location location, long nowMs, long maximumAgeMs, boolean hasFine, boolean highAccuracy) {
        return location != null && Double.isFinite(location.getLatitude()) && Double.isFinite(location.getLongitude())
            && isFreshCacheHit(location.hasAccuracy(), location.getAccuracy(), location.getTime(),
                nowMs, maximumAgeMs, hasFine, highAccuracy);
    }

    public static boolean shouldPreferCandidate(double candidateLat, double candidateLon,
                                                boolean candidateHasAccuracy, float candidateAccuracy, long candidateTimeMs,
                                                boolean bestHasAccuracy, float bestAccuracy, long bestTimeMs) {
        if (!Double.isFinite(candidateLat) || !Double.isFinite(candidateLon)) return false;
        long timeDelta = candidateTimeMs - bestTimeMs;
        if (timeDelta > STALE_THRESHOLD_MS && (!candidateHasAccuracy || candidateAccuracy <= GRACE_FINE_ACCURACY_M)) {
            return true;
        }
        if (timeDelta < -STALE_THRESHOLD_MS && (!bestHasAccuracy || bestAccuracy <= GRACE_FINE_ACCURACY_M)) {
            return false;
        }
        if (candidateHasAccuracy && !bestHasAccuracy) return true;
        if (!candidateHasAccuracy && bestHasAccuracy) return false;
        if (candidateHasAccuracy && bestHasAccuracy) {
            int cmp = Float.compare(candidateAccuracy, bestAccuracy);
            if (cmp < 0) return true;
            if (cmp > 0) return false;
        }
        return candidateTimeMs >= bestTimeMs;
    }

    static Location selectBetterLocation(Location currentBest, Location candidate) {
        if (candidate == null) return currentBest;
        if (!Double.isFinite(candidate.getLatitude()) || !Double.isFinite(candidate.getLongitude())) return currentBest;
        if (currentBest == null) return candidate;
        return shouldPreferCandidate(
            candidate.getLatitude(), candidate.getLongitude(),
            candidate.hasAccuracy(), candidate.getAccuracy(), candidate.getTime(),
            currentBest.hasAccuracy(), currentBest.getAccuracy(), currentBest.getTime())
            ? candidate : currentBest;
    }

    public static boolean isImmediateFix(boolean hasAccuracy, float accuracy, boolean hasFine, boolean highAccuracy) {
        if (!hasAccuracy) return false;
        float threshold = (hasFine && highAccuracy) ? IMMEDIATE_FINE_ACCURACY_M : COARSE_ACCURACY_M;
        return accuracy <= threshold;
    }

    static boolean isImmediateFix(Location location, boolean hasFine, boolean highAccuracy) {
        return location != null && isImmediateFix(location.hasAccuracy(), location.getAccuracy(), hasFine, highAccuracy);
    }

    public static boolean isGraceEligibleFix(boolean hasAccuracy, float accuracy, boolean hasFine, boolean highAccuracy) {
        return hasFine && highAccuracy && hasAccuracy && accuracy <= GRACE_FINE_ACCURACY_M;
    }

    static boolean isGraceEligibleFix(Location location, boolean hasFine, boolean highAccuracy) {
        return location != null && isGraceEligibleFix(location.hasAccuracy(), location.getAccuracy(), hasFine, highAccuracy);
    }

    private final class PositionRequest implements LocationListener {
        final String id;
        final PluginCall call;
        final boolean highAccuracy;
        final boolean hasFine;
        final Runnable deadline;
        final Runnable graceFinish;
        Location best;
        boolean graceScheduled;
        PositionRequest(String id, PluginCall call) {
            this.id = id; this.call = call;
            this.highAccuracy = call.getBoolean("enableHighAccuracy", true);
            this.hasFine = hasFineLocationPermission();
            this.deadline = () -> finish(best, "Location request timed out", "LOCATION_TIMEOUT");
            this.graceFinish = () -> {
                if (best != null) finish(best, null, null);
            };
        }
        void start() {
            try {
                if (locationManager == null) {
                    finish(null, "Location manager unavailable", "POSITION_UNAVAILABLE"); return;
                }
                boolean gps = hasFine && isProviderUsable(LocationManager.GPS_PROVIDER);
                boolean network = isProviderUsable(LocationManager.NETWORK_PROVIDER);
                boolean fused = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isProviderUsable(LocationManager.FUSED_PROVIDER);
                if (!gps && !network && !fused) {
                    finish(null, "Location services are disabled", "POSITION_UNAVAILABLE"); return;
                }
                long maximumAge = call.getLong("maximumAge", 0L);
                if (maximumAge > 0L) {
                    long now = System.currentTimeMillis();
                    Location cached = null;
                    String[] cacheProviders = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ? new String[]{LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}
                        : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER};
                    for (String provider : cacheProviders) {
                        Location candidate = safeLastKnown(provider);
                        if (isFreshCacheHit(candidate, now, maximumAge, hasFine, highAccuracy)) {
                            cached = selectBetterLocation(cached, candidate);
                        }
                    }
                    if (cached != null) { finish(cached, null, null); return; }
                }
                int registered = 0;
                if (fused) registered += requestProvider(LocationManager.FUSED_PROVIDER);
                if (gps && (highAccuracy || (!network && !fused))) registered += requestProvider(LocationManager.GPS_PROVIDER);
                if (network) registered += requestProvider(LocationManager.NETWORK_PROVIDER);
                if (registered == 0) {
                    finish(null, "Location provider unavailable", "POSITION_UNAVAILABLE"); return;
                }
                mainHandler.postDelayed(deadline, Math.max(1L, Math.min(30000L, call.getLong("timeout", 15000L))));
            } catch (SecurityException error) {
                finish(null, "Location permission denied", "PERMISSION_DENIED");
            } catch (RuntimeException error) {
                finish(null, "Location provider unavailable", "POSITION_UNAVAILABLE");
            }
        }
        private boolean isProviderUsable(String provider) {
            try { return locationManager.isProviderEnabled(provider); }
            catch (IllegalArgumentException | SecurityException ignored) { return false; }
        }
        private Location safeLastKnown(String provider) {
            try { return locationManager.getLastKnownLocation(provider); }
            catch (IllegalArgumentException | SecurityException ignored) { return null; }
        }
        private int requestProvider(String provider) {
            try {
                locationManager.requestLocationUpdates(provider, 0, 0, this, Looper.getMainLooper());
                return 1;
            } catch (IllegalArgumentException | SecurityException ignored) {
                return 0;
            }
        }
        @Override public void onLocationChanged(Location location) {
            if (positionRequests.get(id) != this) return;
            best = selectBetterLocation(best, location);
            if (isImmediateFix(best, hasFine, highAccuracy)) {
                finish(best, null, null);
                return;
            }
            if (!graceScheduled && isGraceEligibleFix(best, hasFine, highAccuracy)) {
                graceScheduled = true;
                mainHandler.postDelayed(graceFinish, GRACE_WINDOW_MS);
            }
        }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled(String provider) {}
        @Override public void onProviderDisabled(String provider) {}
        void cleanup() {
            mainHandler.removeCallbacks(deadline);
            mainHandler.removeCallbacks(graceFinish);
            if (locationManager != null) {
                try { locationManager.removeUpdates(this); }
                catch (SecurityException error) { Log.w(TAG, "Permission revoked during location cleanup"); }
            }
        }
        void finish(Location location, String message, String code) {
            if (!positionRequests.remove(id, this)) return;
            cleanup();
            if (location != null) call.resolve(createPositionResult(location));
            else call.reject(message, code);
            bridge.releaseCall(call);
        }
    }

    @Override
    protected void handleOnDestroy() {
        destroyed = true;
        mainHandler.post(() -> {
            for (PositionRequest request : positionRequests.values()) {
                request.cleanup();
                request.call.reject("Location request cancelled", "LOCATION_CANCELLED");
                bridge.releaseCall(request.call);
            }
            positionRequests.clear();
            cancelHeadings();
        });
        super.handleOnDestroy();
    }

    private void cancelHeadings() {
        java.util.Iterator<Runnable> requests = headingCleanup.values().iterator();
        while (requests.hasNext()) {
            Runnable cancel = requests.next();
            requests.remove();
            cancel.run();
        }
        java.util.Iterator<Runnable> watches = headingWatches.values().iterator();
        while (watches.hasNext()) {
            Runnable cancel = watches.next();
            watches.remove();
            cancel.run();
        }
    }
    @Override protected void handleOnPause() {
        paused = true;
        mainHandler.post(this::cancelHeadings);
        super.handleOnPause();
    }
    @Override protected void handleOnResume() {
        paused = false;
        super.handleOnResume();
    }

    private Location getBestLastKnownLocation() {
        if (locationManager == null) return null;
        Location best = null;
        String[] providers = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ? new String[]{LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}
            : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER};
        for (String provider : providers) {
            try {
                Location candidate = locationManager.getLastKnownLocation(provider);
                best = selectBetterLocation(best, candidate);
            } catch (IllegalArgumentException | SecurityException ignored) {}
        }
        return best;
    }

    private JSObject createPositionResult(Location location) {
        JSObject coords = new JSObject();
        coords.put("latitude", location.getLatitude());
        coords.put("longitude", location.getLongitude());
        coords.put("accuracy", (double) location.getAccuracy());
        if (location.hasAltitude()) {
            coords.put("altitude", (double) location.getAltitude());
        }
        if (location.hasBearing()) {
            coords.put("heading", (double) location.getBearing());
        }
        if (location.hasSpeed()) {
            coords.put("speed", (double) location.getSpeed());
        }

        JSObject result = new JSObject();
        result.put("coords", coords);
        result.put("timestamp", location.getTime());

        return result;
    }


    private static final long HEADING_EMIT_INTERVAL_MS = 50L;
    private static final long DECLINATION_REFRESH_MS = 60_000L;

    private static final class HeadingSensors {
        final Sensor rotation;
        final Sensor accelerometer;
        final Sensor magnetometer;

        HeadingSensors(Sensor rotation, Sensor accelerometer, Sensor magnetometer) {
            this.rotation = rotation;
            this.accelerometer = accelerometer;
            this.magnetometer = magnetometer;
        }
    }

    private HeadingSensors selectHeadingSensors() {
        if (sensorManager == null) return null;
        Sensor geomagneticRotation = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        if (geomagneticRotation != null) return new HeadingSensors(geomagneticRotation, null, null);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (accelerometer != null && magnetometer != null) return new HeadingSensors(null, accelerometer, magnetometer);
        // Last resort only. Rotation vector can be gyro-dominant and may drift,
        // but it is better than hard-failing when no geomagnetic path is available.
        Sensor rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationVector != null) return new HeadingSensors(rotationVector, null, null);
        return null;
    }

    private void registerHeadingSensors(SensorEventListener listener, HeadingSensors sensors, int delay) {
        if (sensors.rotation != null) {
            sensorManager.registerListener(listener, sensors.rotation, delay);
        } else {
            sensorManager.registerListener(listener, sensors.accelerometer, delay);
            sensorManager.registerListener(listener, sensors.magnetometer, delay);
        }
    }

    private float currentDeclination() {
        Location referenceLocation = getBestLastKnownLocation();
        if (referenceLocation == null) return 0f;
        GeomagneticField geomagneticField = new GeomagneticField(
            (float) referenceLocation.getLatitude(),
            (float) referenceLocation.getLongitude(),
            referenceLocation.hasAltitude() ? (float) referenceLocation.getAltitude() : 0f,
            referenceLocation.getTime() > 0 ? referenceLocation.getTime() : System.currentTimeMillis()
        );
        return geomagneticField.getDeclination();
    }

    private boolean updateRotationMatrix(SensorEvent event, float[] rotationMatrix,
                                         float[] gravityReading, float[] magneticReading,
                                         boolean[] sensorFlags) {
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR || type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            return true;
        }
        if (type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravityReading, 0, gravityReading.length);
            sensorFlags[0] = true;
        } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magneticReading, 0, magneticReading.length);
            sensorFlags[1] = true;
        }
        return sensorFlags[0] && sensorFlags[1]
            && SensorManager.getRotationMatrix(rotationMatrix, null, gravityReading, magneticReading);
    }

    private Double resolveHeadingFromMatrix(float[] rotationMatrix, float[] remappedMatrix, float[] orientation, float declination) {
        int worldAxisForX = SensorManager.AXIS_X;
        int worldAxisForY = SensorManager.AXIS_Y;

        if (getActivity() != null && getActivity().getDisplay() != null) {
            int rotation = getActivity().getDisplay().getRotation();
            switch (rotation) {
                case Surface.ROTATION_90:
                    worldAxisForX = SensorManager.AXIS_Y;
                    worldAxisForY = SensorManager.AXIS_MINUS_X;
                    break;
                case Surface.ROTATION_180:
                    worldAxisForX = SensorManager.AXIS_MINUS_X;
                    worldAxisForY = SensorManager.AXIS_MINUS_Y;
                    break;
                case Surface.ROTATION_270:
                    worldAxisForX = SensorManager.AXIS_MINUS_Y;
                    worldAxisForY = SensorManager.AXIS_X;
                    break;
                case Surface.ROTATION_0:
                default:
                    break;
            }
        }

        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForX, worldAxisForY, remappedMatrix);
        SensorManager.getOrientation(remappedMatrix, orientation);
        double azimuth = Math.toDegrees(orientation[0]);
        return (azimuth + declination + 360.0) % 360.0;
    }

    @PluginMethod
    public void getCurrentHeading(PluginCall call) {
        mainHandler.post(() -> readCurrentHeading(call));
    }

    private void readCurrentHeading(PluginCall call) {
        if (destroyed || paused) { call.reject("Compass request cancelled"); return; }
        if (sensorManager == null) {
            call.reject("Sensor manager unavailable");
            return;
        }

        HeadingSensors sensors = selectHeadingSensors();
        if (sensors == null) {
            call.reject("Compass sensors are not available");
            return;
        }

        final float[] rotationMatrix = new float[9];
        final float[] remappedMatrix = new float[9];
        final float[] orientation = new float[3];
        final float[] gravityReading = new float[3];
        final float[] magneticReading = new float[3];

        final boolean[] sensorFlags = new boolean[2];

        final PluginCall pendingCall = call;
        bridge.saveCall(call);

        final class HeadingListener implements SensorEventListener {
            boolean resolved;
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (resolved || !updateRotationMatrix(event, rotationMatrix, gravityReading, magneticReading, sensorFlags)) {
                    return;
                }

                double heading = resolveHeadingFromMatrix(rotationMatrix, remappedMatrix, orientation, currentDeclination());

                resolved = true;
                mainHandler.removeCallbacksAndMessages(pendingCall);
                headingCleanup.remove(pendingCall);
                sensorManager.unregisterListener(this);

                PluginCall savedCall = bridge.getSavedCall(pendingCall.getCallbackId());
                if (savedCall != null) {
                    JSObject result = new JSObject();
                    result.put("heading", heading);
                    savedCall.resolve(result);
                    bridge.releaseCall(savedCall);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        }
        final HeadingListener listener = new HeadingListener();
        headingCleanup.put(pendingCall, () -> {
            if (listener.resolved) return;
            listener.resolved = true;
            mainHandler.removeCallbacksAndMessages(pendingCall);
            headingCleanup.remove(pendingCall);
            sensorManager.unregisterListener(listener);
            pendingCall.reject("Compass request cancelled");
            bridge.releaseCall(pendingCall);
        });

        registerHeadingSensors(listener, sensors, SensorManager.SENSOR_DELAY_GAME);

        mainHandler.postAtTime(() -> {
            if (listener.resolved) return;
            listener.resolved = true;
            headingCleanup.remove(pendingCall);

            sensorManager.unregisterListener(listener);
            PluginCall savedCall = bridge.getSavedCall(pendingCall.getCallbackId());
            if (savedCall != null) {
                savedCall.reject("Compass heading is unavailable");
                bridge.releaseCall(savedCall);
            }
        }, pendingCall, android.os.SystemClock.uptimeMillis() + 4000);
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public void watchHeading(PluginCall call) {
        mainHandler.post(() -> startHeadingWatch(call));
    }

    private void startHeadingWatch(PluginCall call) {
        if (destroyed) { call.reject("Compass request cancelled", "COMPASS_CANCELLED"); return; }
        if (sensorManager == null) {
            call.reject("Sensor manager unavailable");
            return;
        }
        HeadingSensors sensors = selectHeadingSensors();
        if (sensors == null) {
            call.reject("Compass sensors are not available");
            return;
        }

        call.setKeepAlive(true);
        bridge.saveCall(call);
        final String watchId = call.getCallbackId();

        final float[] rotationMatrix = new float[9];
        final float[] remappedMatrix = new float[9];
        final float[] orientation = new float[3];
        final float[] gravityReading = new float[3];
        final float[] magneticReading = new float[3];
        final boolean[] sensorFlags = new boolean[2];

        final class WatchListener implements SensorEventListener {
            boolean stopped, hasEmitted;
            long lastEmitUptimeMs, lastDeclinationUptimeMs;
            float cachedDeclination;

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (stopped || !updateRotationMatrix(event, rotationMatrix, gravityReading, magneticReading, sensorFlags)) {
                    return;
                }

                long uptime = android.os.SystemClock.uptimeMillis();
                if (hasEmitted && (uptime - lastEmitUptimeMs) < HEADING_EMIT_INTERVAL_MS) {
                    return;
                }
                if (!hasEmitted || (uptime - lastDeclinationUptimeMs) >= DECLINATION_REFRESH_MS) {
                    cachedDeclination = currentDeclination();
                    lastDeclinationUptimeMs = uptime;
                }
                if (!hasEmitted) {
                    mainHandler.removeCallbacksAndMessages(watchId);
                }
                hasEmitted = true;
                lastEmitUptimeMs = uptime;

                double heading = resolveHeadingFromMatrix(rotationMatrix, remappedMatrix, orientation, cachedDeclination);
                PluginCall savedCall = bridge.getSavedCall(watchId);
                if (savedCall != null) {
                    JSObject result = new JSObject();
                    result.put("heading", heading);
                    savedCall.resolve(result);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        }

        final WatchListener listener = new WatchListener();
        Runnable stopWatch = () -> {
            if (listener.stopped) return;
            listener.stopped = true;
            mainHandler.removeCallbacksAndMessages(watchId);
            sensorManager.unregisterListener(listener);
            PluginCall savedCall = bridge.getSavedCall(watchId);
            if (savedCall != null) {
                savedCall.setKeepAlive(false);
                bridge.releaseCall(savedCall);
            }
        };
        headingWatches.put(watchId, stopWatch);

        registerHeadingSensors(listener, sensors, SensorManager.SENSOR_DELAY_UI);

        mainHandler.postAtTime(() -> {
            if (listener.stopped || listener.hasEmitted) return;
            headingWatches.remove(watchId);
            listener.stopped = true;
            sensorManager.unregisterListener(listener);
            PluginCall savedCall = bridge.getSavedCall(watchId);
            if (savedCall != null) {
                savedCall.setKeepAlive(false);
                savedCall.reject("Compass heading is unavailable");
                bridge.releaseCall(savedCall);
            }
        }, watchId, android.os.SystemClock.uptimeMillis() + 4000);
    }

    @PluginMethod
    public void clearHeadingWatch(PluginCall call) {
        String id = call.getString("id");
        mainHandler.post(() -> {
            if (id != null) {
                Runnable stop = headingWatches.remove(id);
                if (stop != null) stop.run();
            }
            call.resolve();
        });
    }
}
