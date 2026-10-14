package org.muslimtechnician.muajjin.plugins;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.hardware.GeomagneticField;
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

    private boolean hasLocationPermission() {
        return getContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
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

    private final class PositionRequest implements LocationListener {
        final String id;
        final PluginCall call;
        final boolean highAccuracy;
        final Runnable deadline;
        Location best;
        PositionRequest(String id, PluginCall call) {
            this.id = id; this.call = call;
            highAccuracy = call.getBoolean("enableHighAccuracy", true);
            deadline = () -> finish(best, "Location request timed out", "LOCATION_TIMEOUT");
        }
        void start() {
            try {
                if (locationManager == null) {
                    finish(null, "Location manager unavailable", "POSITION_UNAVAILABLE"); return;
                }
                boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                if (!gps && !network) {
                    finish(null, "Location services are disabled", "POSITION_UNAVAILABLE"); return;
                }
                long maximumAge = call.getLong("maximumAge", 0L);
                if (maximumAge > 0) {
                    long now = System.currentTimeMillis();
                    Location cachedGps = gps ? locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) : null;
                    Location cachedNetwork = network ? locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) : null;
                    if (cachedGps != null && now >= cachedGps.getTime() && now - cachedGps.getTime() <= maximumAge) best = cachedGps;
                    if (cachedNetwork != null && now >= cachedNetwork.getTime() && now - cachedNetwork.getTime() <= maximumAge &&
                        (best == null || cachedNetwork.getTime() > best.getTime())) best = cachedNetwork;
                    if (best != null) { finish(best, null, null); return; }
                }
                if (gps && (highAccuracy || !network)) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this, Looper.getMainLooper());
                if (network) locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this, Looper.getMainLooper());
                mainHandler.postDelayed(deadline, Math.max(1L, Math.min(30000L, call.getLong("timeout", 15000L))));
            } catch (SecurityException error) {
                finish(null, "Location permission denied", "PERMISSION_DENIED");
            } catch (RuntimeException error) {
                finish(null, "Location provider unavailable", "POSITION_UNAVAILABLE");
            }
        }
        @Override public void onLocationChanged(Location location) {
            if (positionRequests.get(id) != this) return;
            if (best == null || location.getAccuracy() < best.getAccuracy() ||
                (location.getAccuracy() == best.getAccuracy() && location.getTime() > best.getTime())) best = location;
            float desiredAccuracy = highAccuracy ? 25f : 100f;
            if (location.hasAccuracy() && (location.getAccuracy() <= desiredAccuracy ||
                (highAccuracy && LocationManager.GPS_PROVIDER.equals(location.getProvider()) && location.getAccuracy() <= 50f)))
                finish(best, null, null);
        }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled(String provider) {}
        @Override public void onProviderDisabled(String provider) {}
        void cleanup() {
            mainHandler.removeCallbacks(deadline);
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

    private Location getBestLastKnownLocation() {
        Location bestLocation = null;

        try {
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (gpsLocation != null) {
                bestLocation = gpsLocation;
            }

            if (networkLocation != null &&
                (bestLocation == null || networkLocation.getTime() > bestLocation.getTime())) {
                bestLocation = networkLocation;
            }
        } catch (SecurityException ignored) {
            return null;
        }

        return bestLocation;
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

        Sensor geomagneticRotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        Sensor rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (
            geomagneticRotationVectorSensor == null &&
            rotationVectorSensor == null &&
            (accelerometerSensor == null || magnetometerSensor == null)
        ) {
            call.reject("Compass sensors are not available");
            return;
        }

        final float[] rotationMatrix = new float[9];
        final float[] remappedMatrix = new float[9];
        final float[] orientation = new float[3];
        final float[] gravityReading = new float[3];
        final float[] magneticReading = new float[3];

        final PluginCall pendingCall = call;
        bridge.saveCall(call);

        final class HeadingListener implements SensorEventListener {
            boolean hasGravity, hasMagnetic, resolved;
            @Override
            public void onSensorChanged(SensorEvent event) {
                boolean canResolve = false;

                if (
                    event.sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR ||
                    event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR
                ) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                    canResolve = true;
                } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, gravityReading, 0, gravityReading.length);
                    hasGravity = true;
                } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, magneticReading, 0, magneticReading.length);
                    hasMagnetic = true;
                }

                if (!canResolve && hasGravity && hasMagnetic) {
                    canResolve = SensorManager.getRotationMatrix(
                        rotationMatrix,
                        null,
                        gravityReading,
                        magneticReading
                    );
                }

                if (!canResolve || resolved) {
                    return;
                }

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

                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    worldAxisForX,
                    worldAxisForY,
                    remappedMatrix
                );
                SensorManager.getOrientation(remappedMatrix, orientation);

                double azimuth = Math.toDegrees(orientation[0]);
                double heading = (azimuth + 360.0) % 360.0;

                Location referenceLocation = getBestLastKnownLocation();
                if (referenceLocation != null) {
                    GeomagneticField geomagneticField = new GeomagneticField(
                        (float) referenceLocation.getLatitude(),
                        (float) referenceLocation.getLongitude(),
                        referenceLocation.hasAltitude() ? (float) referenceLocation.getAltitude() : 0f,
                        referenceLocation.getTime() > 0 ? referenceLocation.getTime() : System.currentTimeMillis()
                    );
                    heading = (heading + geomagneticField.getDeclination() + 360.0) % 360.0;
                }

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
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
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

        if (geomagneticRotationVectorSensor != null) {
            sensorManager.registerListener(
                listener,
                geomagneticRotationVectorSensor,
                SensorManager.SENSOR_DELAY_GAME
            );
        } else if (accelerometerSensor != null && magnetometerSensor != null) {
            sensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(listener, magnetometerSensor, SensorManager.SENSOR_DELAY_GAME);
        } else if (rotationVectorSensor != null) {
            // Last resort only. Rotation vector can be gyro-dominant and may drift,
            // but it is better than hard-failing when no geomagnetic path is available.
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME);
        }

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
}
