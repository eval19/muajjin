import { describe, expect, test } from 'bun:test';
import { calculatePrayerTimesLocally } from '../src/services/prayer-times-local';
import { DEFAULT_SETTINGS } from '../src/constants/default-settings';

// A zero coordinate must not silently select a different location.
describe('local prayer coordinates', () => {
  const date = new Date(2026, 2, 20, 12);
  test('equator and prime meridian retain their requested location', () => {
    const atZero = calculatePrayerTimesLocally(date, {
      ...DEFAULT_SETTINGS, latitude: 0, longitude: 0,
    });
    const nearZero = calculatePrayerTimesLocally(date, {
      ...DEFAULT_SETTINGS, latitude: 0.000001, longitude: 0.000001,
    });
    expect(atZero).toEqual(nearZero);
  });
  test.each([
    { latitude: Number.NaN, longitude: 10 },
    { latitude: 91, longitude: 10 },
    { latitude: 10, longitude: 181 },
  ])('rejects invalid calculation coordinates %j', (coordinates) => {
    expect(() => calculatePrayerTimesLocally(date, {
      ...DEFAULT_SETTINGS, ...coordinates,
    })).toThrow();
  });
  test('Hanafi parameters do not leak into later Shafi calculations', () => {
    const shafi = calculatePrayerTimesLocally(date, {
      ...DEFAULT_SETTINGS, madhab: 0,
    });
    const hanafi = calculatePrayerTimesLocally(date, {
      ...DEFAULT_SETTINGS, madhab: 1,
    });
    expect(hanafi.Asr).not.toBe(shafi.Asr);
    expect(calculatePrayerTimesLocally(date, {
      ...DEFAULT_SETTINGS, madhab: 0,
    })).toEqual(shafi);
  });
});
