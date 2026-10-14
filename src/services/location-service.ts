import { NativeGeolocation } from '@/plugins/native-geolocation';
import type { Position, PermissionStatus } from '@/plugins/native-geolocation';
import { Capacitor } from '@capacitor/core';
import { z } from 'zod';

export interface LocationResult {
  latitude: number;
  longitude: number;
  city: string;
  country: string;
  method: 'gps' | 'ip' | 'manual';
  addressUnavailable?: boolean;
}

const coordinatesSchema = z.object({
  latitude: z.number().finite().min(-90).max(90),
  longitude: z.number().finite().min(-180).max(180),
});
const addressSchema = z.object({
  city: z.string().max(256).optional(), town: z.string().max(256).optional(),
  village: z.string().max(256).optional(), suburb: z.string().max(256).optional(),
  county: z.string().max(256).optional(), country: z.string().max(256).optional(),
});

function checkCancellation(signal: AbortSignal): void {
  if (signal.aborted) throw new Error('errors.locationCancelled');
}

function gpsError(error: unknown): Error {
  const code = typeof error === 'object' && error !== null && 'code' in error ? error.code : undefined;
  if (code === 1 || code === 'PERMISSION_DENIED') return new Error('errors.locationPermissionDenied');
  if (code === 3 || code === 'LOCATION_TIMEOUT') return new Error('errors.locationTimeout');
  return new Error('errors.locationUnavailable');
}

async function fetchLocationJson(url: string, signal: AbortSignal): Promise<unknown> {
  checkCancellation(signal);
  const controller = new AbortController();
  const cancel = () => controller.abort();
  signal.addEventListener('abort', cancel, { once: true });
  const timeout = setTimeout(cancel, 5000);
  try {
    const response = await fetch(url, { signal: controller.signal });
    if (!response.ok) throw new Error('errors.locationNetworkFailed');
    const result: unknown = await response.json();
    checkCancellation(signal);
    return result;
  } finally {
    clearTimeout(timeout);
    signal.removeEventListener('abort', cancel);
  }
}

async function readGps(signal: AbortSignal): Promise<Position> {
  checkCancellation(signal);
  if (Capacitor.isNativePlatform() && Capacitor.isPluginAvailable('NativeGeolocation')) {
    // The OS permission sheet is not ours to dismiss. An obsolete intent
    // stops waiting and cannot start GPS when the permission answer arrives.
    const permissions = await new Promise<PermissionStatus>((resolve, reject) => {
      const cancel = () => reject(new Error('errors.locationCancelled'));
      signal.addEventListener('abort', cancel, { once: true });
      NativeGeolocation.requestPermissions().then(resolve, reject)
        .finally(() => signal.removeEventListener('abort', cancel));
    });
    checkCancellation(signal);
    if (permissions.location !== 'granted' && permissions.coarseLocation !== 'granted') {
      throw new Error('errors.locationPermissionDenied');
    }
    const requestId = crypto.randomUUID();
    return new Promise<Position>((resolve, reject) => {
      const cancel = () => {
        void NativeGeolocation.cancelPosition({ requestId }).catch(() => {
          console.warn('Native location cancellation failed; its bounded timeout remains active');
        });
        reject(new Error('errors.locationCancelled'));
      };
      signal.addEventListener('abort', cancel, { once: true });
      NativeGeolocation.getCurrentPosition({ requestId, enableHighAccuracy: true, timeout: 15000, maximumAge: 0 })
        .then(resolve, (error: unknown) => reject(gpsError(error)))
        .finally(() => signal.removeEventListener('abort', cancel));
    });
  }
  if (!navigator.geolocation) throw new Error('errors.locationUnavailable');
  return new Promise<Position>((resolve, reject) => {
    let watch: number | undefined;
    let finished = false;
    const finish = (position?: GeolocationPosition, error?: Error) => {
      if (finished) return;
      finished = true;
      if (watch !== undefined) navigator.geolocation.clearWatch(watch);
      signal.removeEventListener('abort', cancel);
      if (position) resolve(position);
      else reject(error);
    };
    const cancel = () => finish(undefined, new Error('errors.locationCancelled'));
    signal.addEventListener('abort', cancel, { once: true });
    watch = navigator.geolocation.watchPosition(
      (position) => finish(position),
      (error) => finish(undefined, gpsError(error)),
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 },
    );
    // Also handle adapters that deliver their first result synchronously.
    if (finished) navigator.geolocation.clearWatch(watch);
  });
}

async function getLocationByGPS(signal: AbortSignal, deadlineExpired: () => boolean): Promise<LocationResult> {
  const position = await readGps(signal);
  checkCancellation(signal);
  const coordinates = coordinatesSchema.parse(position.coords);
  try {
    const data = z.object({ address: addressSchema }).parse(await fetchLocationJson(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${coordinates.latitude}&lon=${coordinates.longitude}&accept-language=en`, signal,
    ));
    const { address } = data;
    const city = address.city || address.town || address.village || address.suburb || address.county || 'Unknown';
    return { latitude: coordinates.latitude, longitude: coordinates.longitude, city, country: address.country || 'Unknown', method: 'gps', addressUnavailable: city === 'Unknown' || !address.country };
  } catch {
    if (!deadlineExpired()) checkCancellation(signal);
    return { latitude: coordinates.latitude, longitude: coordinates.longitude, city: 'Unknown', country: 'Unknown', method: 'gps', addressUnavailable: true };
  }
}

async function getLocationByIP(signal: AbortSignal): Promise<LocationResult> {
  // Endpoint changes require separate approval. Keep both existing providers;
  // HTTPS hosts may block the legacy cleartext request and use the fallback.
  const providers = [
    'http://ip-api.com/json/?fields=status,message,country,countryCode,city,lat,lon',
    'https://ipwhois.app/json/',
  ];
  for (const [index, url] of providers.entries()) {
    try {
      const data = z.object({
        status: z.string().optional(), success: z.boolean().optional(),
        lat: z.number().optional(), lon: z.number().optional(),
        latitude: z.number().optional(), longitude: z.number().optional(),
        city: z.string().max(256).optional(), country: z.string().max(256).optional(),
      }).parse(await fetchLocationJson(url, signal));
      if (index === 0 ? data.status !== 'success' : data.success !== true) continue;
      const coordinates = coordinatesSchema.parse(index === 0
        ? { latitude: data.lat, longitude: data.lon }
        : { latitude: data.latitude, longitude: data.longitude });
      return { latitude: coordinates.latitude, longitude: coordinates.longitude, city: data.city || 'Unknown', country: data.country || 'Unknown', method: 'ip', addressUnavailable: !data.city || !data.country };
    } catch {
      checkCancellation(signal);
    }
  }
  throw new Error('errors.locationNetworkFailed');
}

/** A user-requested, one-shot acquisition. Abort stops GPS and in-flight fetches. */
export async function detectLocation(signal?: AbortSignal): Promise<LocationResult> {
  const controller = new AbortController();
  let timedOut = false;
  const cancel = () => controller.abort();
  if (signal?.aborted) cancel();
  signal?.addEventListener('abort', cancel, { once: true });
  const timeout = setTimeout(() => { timedOut = true; cancel(); }, 25000);
  try {
    try {
      return await getLocationByGPS(controller.signal, () => timedOut);
    } catch (error) {
      checkCancellation(controller.signal);
      try {
        return await getLocationByIP(controller.signal);
      } catch {
        checkCancellation(controller.signal);
        if (error instanceof Error && ['errors.locationPermissionDenied', 'errors.locationTimeout', 'errors.locationUnavailable'].includes(error.message)) throw error;
        throw new Error('errors.locationNetworkFailed');
      }
    }
  } catch (error) {
    if (timedOut) throw new Error('errors.locationTimeout');
    throw error;
  } finally {
    clearTimeout(timeout);
    signal?.removeEventListener('abort', cancel);
  }
}
