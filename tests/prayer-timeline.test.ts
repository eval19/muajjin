import { describe, expect, test } from 'bun:test';
import { CalculationMethod, Coordinates, Madhab, PrayerTimes } from 'adhan';
import { DEFAULT_SETTINGS } from '../src/constants/default-settings';
import { calculatePrayerInstants } from '../src/services/prayer-times-local';
import {
  addLocalDays, derivePrayerDay, getCurrentSalat, getNextSalat,
  getPrayerCountdown, getNextFastingEvent, localDateKey,
} from '../src/utils/time-utils';
import { calculateHijriDate } from '../src/utils/hijri-utils';

const settings = { ...DEFAULT_SETTINGS, latitude: 21.3891, longitude: 39.8579 };
const date = new Date(2026, 2, 20, 12);
const methods = [
  [1, CalculationMethod.Karachi], [2, CalculationMethod.NorthAmerica],
  [3, CalculationMethod.MuslimWorldLeague], [4, CalculationMethod.UmmAlQura],
  [5, CalculationMethod.Egyptian], [7, CalculationMethod.Tehran],
  [8, CalculationMethod.Dubai], [9, CalculationMethod.Kuwait],
  [10, CalculationMethod.Qatar], [11, CalculationMethod.Singapore],
  [12, CalculationMethod.MoonsightingCommittee], [15, CalculationMethod.MoonsightingCommittee],
] as const;

describe('canonical prayer instants', () => {
  test.each(methods)('method %i preserves the installed adhan engine instants', (method, factory) => {
    const params = factory();
    params.madhab = Madhab.Shafi;
    const reference = new PrayerTimes(new Coordinates(settings.latitude, settings.longitude), date, params);
    const actual = calculatePrayerInstants(date, { ...settings, method, madhab: 0 });
    for (const id of ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'] as const) {
      expect(actual[id].getTime()).toBe(reference[id].getTime());
    }
  });
  test('absent coordinates retain Makkah defaults, negative coordinates remain valid', () => {
    expect(calculatePrayerInstants(date, { ...settings, latitude: undefined, longitude: undefined }))
      .toEqual(calculatePrayerInstants(date, settings));
    const western = calculatePrayerInstants(date, { ...settings, latitude: -33.9, longitude: -70.7 });
    expect(western.fajr.getTime()).toBeLessThan(western.sunrise.getTime());
  });
  test.each([
    { latitude: Infinity, longitude: 0 }, { latitude: -91, longitude: 0 },
    { latitude: 0, longitude: -181 }, { latitude: null, longitude: 0 },
  ])('invalid coordinates fail instead of changing location: %j', (coordinates) => {
    expect(() => calculatePrayerInstants(date, { ...settings, ...coordinates } as typeof settings)).toThrow();
  });
  test('invalid date, method and polar astronomical failure are explicit', () => {
    expect(() => calculatePrayerInstants(new Date(NaN), settings)).toThrow();
    expect(() => calculatePrayerInstants(date, { ...settings, method: 999 })).toThrow();
    expect(() => calculatePrayerInstants(new Date(2026, 5, 21), { ...settings, latitude: 90, longitude: 0 })).toThrow();
  });
});

describe('dated religious windows', () => {
  test('Isha uses real following-day Fajr and includes the complete preceding end minute', () => {
    const day = derivePrayerDay(date, settings);
    const next = calculatePrayerInstants(addLocalDays(date, 1), settings);
    const isha = day.prayers.find((prayer) => prayer.id === 'isha')!;
    expect(isha.endTimestamp).toBe(next.fajr.getTime() - 1);
    expect(getCurrentSalat(day.timeline, new Date(next.fajr.getTime() - 1))?.id).toBe('isha');
    expect(getCurrentSalat(day.timeline, next.fajr)?.id).toBe('fajr');
  });
  test('pre-Fajr Isha progress starts at yesterday actual Isha, not tonight', () => {
    const day = derivePrayerDay(date, settings);
    const now = new Date(day.instants.fajr.getTime() - 60_000);
    const current = getCurrentSalat(day.timeline, now)!;
    expect(current.id).toBe('isha');
    expect(current.startTimestamp).toBe(calculatePrayerInstants(addLocalDays(date, -1), settings).isha.getTime());
    const countdown = getPrayerCountdown(current, now);
    expect(countdown.progressPercent).toBeGreaterThan(50);
    expect(countdown.remainingMilliseconds).toBe(60_000);
    expect(getNextSalat(day.timeline, now)?.startTimestamp).toBe(day.instants.fajr.getTime());
  });
  test('sunrise ends Fajr with no invented current prayer until Dhuhr', () => {
    const day = derivePrayerDay(date, settings);
    expect(getCurrentSalat(day.timeline, day.instants.sunrise)).toBeNull();
    expect(getNextSalat(day.timeline, day.instants.sunrise)?.id).toBe('dhuhr');
    expect(day.prohibitedTimes[0].startTimestamp).toBe(day.instants.sunrise.getTime());
    expect(day.prohibitedTimes[0].endTimestamp).toBe(day.instants.sunrise.getTime() + 15 * 60_000 - 1);
    expect(day.prohibitedTimes[1].startTimestamp).toBe(day.instants.dhuhr.getTime() - 5 * 60_000);
    expect(day.prohibitedTimes[2].startTimestamp).toBe(day.instants.maghrib.getTime() - 15 * 60_000);
  });
  test('post-midnight Isha Jama’ah belongs to the actual overnight prayer', () => {
    const local = { ...settings, latitude: 40.7, longitude: -74, jamaahTimes: { Isha: '00:30' } };
    const day = derivePrayerDay(date, local);
    const isha = day.prayers.find((prayer) => prayer.id === 'isha')!;
    const jamaah = new Date(isha.jamaahTimestamp!);
    expect(jamaah.getHours()).toBe(0);
    expect(jamaah.getMinutes()).toBe(30);
    expect(jamaah.getTime()).toBeGreaterThan(isha.startTimestamp!);
    expect(getPrayerCountdown(isha, new Date(isha.startTimestamp!)).target).toBe('jamaah');
    expect(getPrayerCountdown(isha, jamaah).target).toBe('end');
  });
  test('after Iftar, fasting countdown targets adjusted tomorrow Fajr', () => {
    const adjusted = { ...settings, suhoorAdjustment: -8, iftarAdjustment: 3 };
    const day = derivePrayerDay(date, adjusted);
    const event = getNextFastingEvent(day.fastingEvents, new Date(day.iftarTimestamp + 1))!;
    expect(event.id).toBe('suhoor');
    expect(event.timestamp).toBe(calculatePrayerInstants(addLocalDays(date, 1), adjusted).fajr.getTime() - 8 * 60_000);
  });
  test('calendar advancement crosses month/year without changing local clock', () => {
    const end = new Date(2026, 11, 31, 12);
    const next = addLocalDays(end, 1);
    expect(localDateKey(next)).toBe('2027-01-01');
    expect(next.getHours()).toBe(12);
  });
});

describe('DST-safe Hijri civil dates', () => {
  test('each time on the same civil date has the same Hijri day', () => {
    for (const date of [new Date(2026, 2, 9), new Date(2026, 10, 2)]) {
      const morning = calculateHijriDate(date);
      const evening = new Date(date); evening.setHours(23, 59);
      expect(calculateHijriDate(evening)).toEqual(morning);
    }
  });
  test('Maghrib rollover and calendar adjustment apply once at the exact instant', () => {
    const maghrib = new Date(2026, 2, 20, 18, 20).getTime();
    expect(calculateHijriDate(new Date(maghrib - 1), 0, maghrib, true))
      .toEqual(calculateHijriDate(new Date(maghrib - 1)));
    expect(calculateHijriDate(new Date(maghrib), 0, maghrib, true))
      .toEqual(calculateHijriDate(new Date(2026, 2, 21)));
    expect(calculateHijriDate(new Date(maghrib), -1, maghrib, true))
      .toEqual(calculateHijriDate(new Date(2026, 2, 20)));
  });
  test('supported range fails honestly outside exact Umm al-Qura data', () => {
    expect(calculateHijriDate(new Date(1900, 3, 30)).date).toBe('01-01-1318');
    expect(() => calculateHijriDate(new Date(1900, 3, 29))).toThrow();
    expect(() => calculateHijriDate(new Date(2077, 10, 17))).toThrow();
  });
});
