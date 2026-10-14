import { afterEach, expect, spyOn, test } from 'bun:test';
import type { Mock } from 'bun:test';
import { detectLocation } from '../src/services/location-service';

const originalNavigator = Object.getOwnPropertyDescriptor(globalThis, 'navigator');
let fetchSpy: Mock<typeof fetch> | undefined;

afterEach(() => {
  fetchSpy?.mockRestore();
  fetchSpy = undefined;
  if (originalNavigator) Object.defineProperty(globalThis, 'navigator', originalNavigator);
  else Reflect.deleteProperty(globalThis, 'navigator');
});

function installGps(acquire: Geolocation['watchPosition']): number[] {
  const stopped: number[] = [];
  const geolocation: Geolocation = {
    getCurrentPosition: () => { throw new Error('Unexpected unbounded GPS acquisition'); },
    watchPosition: acquire,
    clearWatch: (id) => { stopped.push(id); },
  };
  Object.defineProperty(globalThis, 'navigator', { configurable: true, value: { geolocation } });
  return stopped;
}

const zeroPosition: GeolocationPosition = {
  timestamp: 1790769003000,
  coords: {
    latitude: 0, longitude: 0, accuracy: 10,
    altitude: null, altitudeAccuracy: null, heading: null, speed: null,
  },
};

test('offline address lookup preserves valid zero GPS coordinates and stops acquisition', async () => {
  const stopped = installGps((success) => { success(zeroPosition); return 41; });
  fetchSpy = spyOn(globalThis, 'fetch').mockImplementation(async () => { throw new TypeError('Offline'); });
  expect(await detectLocation()).toEqual({
    latitude: 0, longitude: 0, city: 'Unknown', country: 'Unknown',
    method: 'gps', addressUnavailable: true,
  });
  expect(stopped).toEqual([41]);
});

test('cancelled GPS releases the watch and cannot publish a late position or start network fallback', async () => {
  let deliver: PositionCallback | undefined;
  const stopped = installGps((success) => { deliver = success; return 42; });
  fetchSpy = spyOn(globalThis, 'fetch').mockImplementation(async () => { throw new TypeError('Offline'); });
  const controller = new AbortController();
  const pending = detectLocation(controller.signal);
  controller.abort();
  await expect(pending).rejects.toThrow('errors.locationCancelled');
  deliver?.(zeroPosition);
  await Promise.resolve();
  expect(stopped).toEqual([42]);
  expect(fetchSpy).not.toHaveBeenCalled();
});

test('denied GPS and unavailable IP providers report the permission error, not a fabricated location', async () => {
  const stopped = installGps((_success, failure) => {
    failure?.({ code: 1, message: 'Denied', PERMISSION_DENIED: 1, POSITION_UNAVAILABLE: 2, TIMEOUT: 3 });
    return 43;
  });
  fetchSpy = spyOn(globalThis, 'fetch').mockImplementation(async () => { throw new TypeError('Offline'); });
  await expect(detectLocation()).rejects.toThrow('errors.locationPermissionDenied');
  expect(stopped).toEqual([43]);
});
