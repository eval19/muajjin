import { describe, expect, test } from 'bun:test';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { CalculationMethod, Coordinates, Madhab, PrayerTimes } from 'adhan';
import { DEFAULT_SETTINGS } from '../src/constants/default-settings';
import { calculatePrayerInstants } from '../src/services/prayer-times-local';
import {
  addLocalDays, buildPrayerWindows, derivePrayerDay, getCurrentSalat, getNextSalat,
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
  test('high-latitude valid day succeeds despite subsequent polar window collapse', () => {
    const polar = { ...settings, latitude: 60, longitude: 18.96, method: 1, madhab: 0 };
    const day = derivePrayerDay(new Date(2026, 5, 13, 12), polar);
    expect(day.prayers).toHaveLength(5);
    expect(day.prayers[0].id).toBe('fajr');
  });
  test('cross-midnight Jama’ah advances by one day for any midnight-crossing prayer', () => {
    const start = new Date(2026, 5, 13, 23, 30);
    const end = new Date(2026, 5, 14, 2, 30);
    const mockInstants = {
      fajr: new Date(2026, 5, 13, 5),
      sunrise: new Date(2026, 5, 13, 6),
      dhuhr: start,
      asr: end,
      maghrib: new Date(2026, 5, 14, 18),
      isha: new Date(2026, 5, 14, 20),
    };
    const localSettings = { ...settings, jamaahTimes: { Dhuhr: '00:30' } };
    const windows = buildPrayerWindows(mockInstants, new Date(2026, 5, 15, 5), localSettings);
    const dhuhr = windows.find((p) => p.id === 'dhuhr')!;
    const jamaah = new Date(dhuhr.jamaahTimestamp!);
    expect(jamaah.getDate()).toBe(14);
    expect(jamaah.getHours()).toBe(0);
    expect(jamaah.getMinutes()).toBe(30);
    expect(getPrayerCountdown(dhuhr, new Date(2026, 5, 13, 23, 45)).target).toBe('jamaah');
  });
  test('chronological timeline selects prior-day Isha when earlier than today Fajr across timezones', () => {
    const nySettings = {
      ...settings,
      latitude: 40.7,
      longitude: -74,
      method: 1,
      madhab: 0,
    };
    const now = new Date('2026-03-20T00:00:00.000Z');
    const day = derivePrayerDay(now, nySettings);
    for (let i = 1; i < day.timeline.length; i++) {
      expect(day.timeline[i].startTimestamp!).toBeGreaterThanOrEqual(day.timeline[i - 1].startTimestamp!);
    }
    const next = getNextSalat(day.timeline, now);
    expect(next).not.toBeNull();
    expect(next!.id).toBe('isha');
    expect(new Date(next!.startTimestamp!).toISOString()).toBe('2026-03-20T00:39:00.000Z');
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

const fastingArithmeticGroup = 'fasting adjustment arithmetic';
describe(fastingArithmeticGroup, () => {
  // Deleting TZ freezes later changes in pinned Windows Bun. Keep timezone
  // ownership in a child instead of mutating/restoring the shared test process.
  if (process.env.MUAJJIN_FASTING_UTC !== '1') {
    test('all arithmetic cases in an isolated UTC process', () => {
      const result = spawnSync(process.execPath, [
        'test', fileURLToPath(import.meta.url),
        '--test-name-pattern', fastingArithmeticGroup,
      ], {
        env: { ...process.env, TZ: 'UTC', MUAJJIN_FASTING_UTC: '1' },
        encoding: 'utf8', timeout: 4_000,
      });
      if (result.error) throw result.error;
      if (result.status !== 0) throw new Error(result.stderr || result.stdout);
    });
    return;
  }
  if (Intl.DateTimeFormat().resolvedOptions().timeZone !== 'UTC')
    throw new Error('Fasting arithmetic worker did not adopt UTC');
  const reference = new Date('2026-03-20T12:00:00.000Z');
  const calculationSettings = { ...settings, method: 1, madhab: 0 };
  // Existing independent engine-parity.jsonl: makkah/2026-03-20/1/0.
  const fajr = 1773972660000;
  const maghrib = 1774020720000;
  test('fixed reference agrees with the committed independent engine fixture', () => {
    const day = derivePrayerDay(reference, calculationSettings);
    expect(Object.values(day.instants).map((instant) => instant.getTime())).toEqual([
      fajr, 1773977100000, 1773998940000, 1774011180000, maghrib, 1774025160000,
    ]);
  });
  test.each([-143_000_000_000, -144_000_000_000, -121, -10, 0, 10, 121, 143_000_000_000])(
    'preserves exact supported minute semantics: %i',
    (value) => {
      const day = derivePrayerDay(reference, {
        ...calculationSettings, suhoorAdjustment: value, iftarAdjustment: value,
      });
      expect(day.suhoorTimestamp).toBe(fajr + value * 60_000);
      expect(day.iftarTimestamp).toBe(maghrib + value * 60_000);
      expect(day.fastingEvents.every((event) =>
        Number.isFinite(new Date(event.timestamp).getTime()))).toBe(true);
    },
  );
  test.each(['suhoorAdjustment', 'iftarAdjustment'].flatMap((field) =>
    [144_000_000_000, 160_000_000_000, Number.MAX_SAFE_INTEGER,
      -Number.MAX_SAFE_INTEGER, NaN, Infinity, -Infinity, 0.5, '10', null,
    ].map((value) => [field, value] as const)))(
    'isolates unusable %s=%p from valid prayer and fasting siblings',
    (field, value) => {
      const day = derivePrayerDay(reference, { ...calculationSettings, [field]: value });
      const invalidId = field === 'suhoorAdjustment' ? 'suhoor' : 'iftar';
      const siblingId = invalidId === 'suhoor' ? 'iftar' : 'suhoor';
      const failed = invalidId === 'suhoor' ? day.suhoorTimestamp : day.iftarTimestamp;
      expect(Number.isFinite(failed)).toBe(false);
      expect(day.fastingEvents.filter((event) => event.id === invalidId)).toEqual([]);
      expect(getNextFastingEvent(day.fastingEvents, reference)?.id).toBe(siblingId);
      expect(day.suhoorTimestamp === fajr || day.iftarTimestamp === maghrib).toBe(true);
      expect(day.instants.fajr.getTime()).toBe(fajr);
      expect(day.instants.maghrib.getTime()).toBe(maghrib);
      expect(getNextSalat(day.timeline, reference)?.startTimestamp).toBe(1774011180000);
    },
  );
  test('two invalid fasting fields leave the actual prayer timeline available', () => {
    const day = derivePrayerDay(reference, {
      ...calculationSettings, suhoorAdjustment: Infinity, iftarAdjustment: NaN,
    });
    expect(day.fastingEvents).toEqual([]);
    expect(getNextFastingEvent(day.fastingEvents, reference)).toBeNull();
    expect(getNextSalat(day.timeline, reference)?.startTimestamp).toBe(1774011180000);
    expect(day.instants.fajr.getTime()).toBe(fajr);
  });
  test.each([
    [143_970_433_789, 8.64e15],
    [143_970_433_790, NaN],
    [-144_029_566_211, -8.64e15],
    [-144_029_566_212, NaN],
  ])('actual Date endpoint for minute offset %i', (value, expected) => {
    const day = derivePrayerDay(reference, { ...calculationSettings, suhoorAdjustment: value });
    if (Number.isFinite(expected)) expect(day.suhoorTimestamp).toBe(expected);
    else expect(Number.isFinite(day.suhoorTimestamp)).toBe(false);
    expect(day.fastingEvents.every((event) =>
      Number.isFinite(new Date(event.timestamp).getTime()))).toBe(true);
    expect(day.iftarTimestamp).toBe(maghrib);
    expect(getNextFastingEvent(day.fastingEvents, reference)?.id).toBe('iftar');
  });
});
