import { afterAll, beforeAll, describe, expect, test } from 'bun:test';
import { spawnSync } from 'node:child_process';
import { createWidgetSync, buildWidgetPresentation, buildWidgetSchedule } from '../src/services/widget-sync-service';
import { DEFAULT_SETTINGS } from '../src/constants/default-settings';
import type { PrayerSchedulePayload, WidgetPresentation, PrayerWidgetPluginInterface } from '../src/services/widget-sync-service';
import { derivePrayerDay } from '../src/utils/time-utils';
import type { UserSettings } from '../src/types';

const now = () => new Date('2026-10-13T12:00:00.000Z');
const translate = (key: string) => key;
const presentation: WidgetPresentation = buildWidgetPresentation(DEFAULT_SETTINGS, translate, 'en', 'ltr');
const settings = { ...DEFAULT_SETTINGS, latitude: 21.3891, longitude: 39.8579, madhab: 0 };

const originalTimezone = process.env.TZ;
beforeAll(() => { process.env.TZ = 'UTC'; });
afterAll(() => {
  if (originalTimezone === undefined) delete process.env.TZ;
  else process.env.TZ = originalTimezone;
});

function nativeStatus(
  calculationSettings: UserSettings = settings,
  anchor = new Date('2026-10-12T12:00:00.000Z'),
) {
  const payload = buildWidgetSchedule(calculationSettings, anchor);
  return {
    exists: true,
    ...payload.calculation,
    schema: payload.schedule.schema,
    timezone: payload.schedule.timezone,
    validFrom: payload.schedule.validFrom,
    validUntil: payload.schedule.validUntil,
    revision: payload.revision,
  };
}

// Load production only after wrapping the real engine factory, in an isolated
// process. This counts calculation work independently of bridge transfers.
const workProbe = `
import { CalculationMethod } from 'adhan';
let calculations = 0;
const karachi = CalculationMethod.Karachi;
CalculationMethod.Karachi = () => { calculations++; return karachi(); };
// Dynamic imports are intentional: the production factory table captures these functions.
const { createWidgetSync, buildWidgetSchedule, buildWidgetPresentation } =
  await import('./src/services/widget-sync-service.ts');
const { DEFAULT_SETTINGS } = await import('./src/constants/default-settings.ts');
const settings = { ...DEFAULT_SETTINGS, latitude: 21.3891, longitude: 39.8579, method: 1, madhab: 0 };
const now = new Date('2026-10-13T12:00:00.000Z');
const payload = buildWidgetSchedule(settings, new Date('2026-10-12T12:00:00.000Z'));
const baselineCalculations = calculations;
const status = {
  exists: true, ...payload.calculation, schema: 2,
  timezone: process.env.MUAJJIN_QA_ZONE,
  validFrom: payload.schedule.validFrom, validUntil: payload.schedule.validUntil,
  revision: payload.revision,
};
const presentation = buildWidgetPresentation(settings, key => key, 'en', 'ltr');
const mode = process.env.MUAJJIN_QA_MODE;
let statusQueries = 0, scheduleTransfers = 0, presentationTransfers = 0;
const snapshot = () => ({ calculations, statusQueries, scheduleTransfers, presentationTransfers });
const makeSync = () => createWidgetSync({
  getScheduleStatus: async () => {
    statusQueries++;
    if (mode === 'retry' && statusQueries === 1) throw new Error('QA status unavailable');
    return mode === 'ticks' ? { exists: false }
      : mode === 'corrupt' ? { ...status, validUntil: 8_650_000_000_000_000 } : status;
  },
  updateSchedule: async () => {
    scheduleTransfers++;
    if (mode === 'retry' && scheduleTransfers === 1) throw new Error('QA write denied');
    return { success: true };
  },
  updatePresentation: async () => { presentationTransfers++; return { success: true }; },
}, () => mode !== 'web', () => {
  if (mode === 'web') throw new Error('Web requested native preparation');
  return now;
});
calculations = 0;
let initial;
let firstRejected;
if (mode === 'retry' || mode === 'ticks') {
  const sync = makeSync();
  try { await sync({ settings, presentation }); firstRejected = false; }
  catch { firstRejected = true; }
  initial = snapshot();
  calculations = 0;
  if (mode === 'retry') await sync({ settings, presentation });
  else {
    for (let tick = 0; tick < 20; tick++) await sync({ settings, presentation });
    await sync({ settings, presentation: { ...presentation, timeFormat: '12h' } });
  }
} else {
  for (let restart = 0; restart < (mode === 'web' || mode === 'corrupt' ? 1 : 3); restart++)
    await makeSync()({ settings, presentation });
}
const offset = timeZone => new Intl.DateTimeFormat('en-US', {
  timeZone, timeZoneName: 'longOffset',
}).formatToParts(now).find(part => part.type === 'timeZoneName').value;
const sameOffsetNow = offset(Intl.DateTimeFormat().resolvedOptions().timeZone) === offset(status.timezone);
console.log(JSON.stringify({ baselineCalculations, sameOffsetNow, initial, firstRejected, final: snapshot() }));
`;

describe('native schedule delivery', () => {
  test('ordinary web does not prepare or deliver a schedule', async () => {
    const adapter: PrayerWidgetPluginInterface = {
      updateSchedule: async () => { throw new Error('Unsupported bridge was called'); },
      updatePresentation: async () => { throw new Error('Unsupported bridge was called'); },
    };
    const sync = createWidgetSync(adapter, () => false, () => { throw new Error('Native schedule preparation in web mode'); });
    await sync({ settings, presentation });
  });

  test('repeated ticks and presentation edits retain the same physical schedule', async () => {
    const schedules: PrayerSchedulePayload[] = [];
    const display: WidgetPresentation[] = [];
    const sync = createWidgetSync({
      updateSchedule: async (payload) => { schedules.push(payload); return { success: true }; },
      updatePresentation: async (payload) => { display.push(payload.presentation); return { success: true }; },
    }, () => true, now);
    await sync({ settings, presentation });
    const first = schedules[0];
    for (let i = 0; i < 20; i++) await sync({ settings, presentation });
    await sync({ settings, presentation: { ...presentation, timeFormat: '12h' } });
    await sync({
      settings,
      presentation: { ...presentation, language: 'ar', direction: 'rtl' },
    });
    const languageSwitch = display[display.length - 1];
    expect(languageSwitch.language).toBe('ar');
    expect(languageSwitch.direction).toBe('rtl');
    expect(schedules).toEqual([first]);
    expect(first.schedule.days[29].nextFajr).toBeGreaterThan(first.schedule.days[29].isha);
    expect(first.schedule.days[0].previousIsha).toBeLessThan(first.schedule.days[0].fajr);
  });

  test('widget text size changes presentation only, never the calculated schedule', async () => {
    const schedules: PrayerSchedulePayload[] = [];
    const display: WidgetPresentation[] = [];
    const sync = createWidgetSync({
      updateSchedule: async (payload) => { schedules.push(payload); return { success: true }; },
      updatePresentation: async (payload) => { display.push(payload.presentation); return { success: true }; },
    }, () => true, now);
    expect(buildWidgetPresentation(DEFAULT_SETTINGS, translate, 'en', 'ltr').widgetLargeText).toBe(false);
    await sync({ settings, presentation });
    const larger = buildWidgetPresentation({ ...settings, widgetLargeText: true }, translate, 'en', 'ltr');
    expect(larger.widgetLargeText).toBe(true);
    await sync({ settings: { ...settings, widgetLargeText: true }, presentation: larger });
    expect(display[display.length - 1].widgetLargeText).toBe(true);
    await sync({
      settings: { ...settings, widgetLargeText: false },
      presentation: buildWidgetPresentation(settings, translate, 'en', 'ltr'),
    });
    expect(display[display.length - 1].widgetLargeText).toBe(false);
    expect(schedules).toHaveLength(1);
  });

  test('a damaged coordinate cannot reuse an acknowledged default-location schedule', async () => {
    const sync = createWidgetSync({
      updateSchedule: async () => ({ success: true }),
      updatePresentation: async () => ({ success: true }),
    }, () => true, now);
    await sync({ settings, presentation });
    await expect(sync({ settings: { ...settings, latitude: null }, presentation })).rejects.toThrow();
  });

  test('failed acknowledgment remains retryable rather than claiming success', async () => {
    let accepts = false;
    const accepted: PrayerSchedulePayload[] = [];
    const sync = createWidgetSync({
      updateSchedule: async (payload) => {
        if (!accepts) throw new Error('Durable storage failed');
        accepted.push(payload);
        return { success: true };
      },
      updatePresentation: async () => ({ success: true }),
    }, () => true, now);
    await expect(sync({ settings, presentation })).rejects.toBeInstanceOf(Error);
    accepts = true;
    await sync({ settings, presentation });
    expect(accepted.map((item) => item.calculation.latitude)).toEqual([settings.latitude]);
  });

  test('a late old request cannot become the final settings revision', async () => {
    let release: () => void = () => { throw new Error('No pending first request'); };
    const barrier = new Promise<void>((resolve) => { release = resolve; });
    const delivered: PrayerSchedulePayload[] = [];
    const sync = createWidgetSync({
      updateSchedule: async (payload) => {
        delivered.push(payload);
        if (delivered.length === 1) await barrier;
        return { success: true };
      },
      updatePresentation: async () => ({ success: true }),
    }, () => true, now);
    const first = sync({ settings, presentation });
    const newer = { ...settings, latitude: 23.8103, longitude: 90.4125, madhab: 1 };
    const second = sync({ settings: newer, presentation });
    release();
    await Promise.all([first, second]);
    expect(delivered.map((item) => [item.calculation.latitude, item.calculation.madhab])).toEqual([[21.3891, 0], [23.8103, 1]]);
    const last = delivered[1];
    expect(last.schedule.days[0].asr).not.toBe(delivered[0].schedule.days[0].asr);
  });

  test('a newer calculation during cache-hit finalization reaches durable state', async () => {
    let committed: PrayerSchedulePayload | undefined;
    const sync = createWidgetSync({
      updateSchedule: async (payload) => { committed = payload; return { success: true }; },
      updatePresentation: async () => ({ success: true }),
    }, () => true, now);
    await sync({ settings, presentation });
    const unchanged = sync({ settings, presentation });
    const newest = { ...settings, madhab: 1 };
    const changed = sync({ settings: newest, presentation });
    await Promise.all([unchanged, changed]);
    const day = committed!.schedule.days.find((entry) => entry.date === '2026-10-13')!;
    const expected = derivePrayerDay(now(), newest).prayers.find((prayer) => prayer.id === 'asr')!;
    expect(day.asr).toBe(expected.startTimestamp!);
  });
  test('fresh JS synchronizer reuses an engine-derived native horizon without schedule transfer', async () => {
    let scheduleGenerated = false;
    let presentationDelivered = false;
    const currentTime = now();
    const sync = createWidgetSync({
      getScheduleStatus: async () => nativeStatus(),
      updateSchedule: async () => {
        scheduleGenerated = true;
        return { success: true };
      },
      updatePresentation: async () => {
        presentationDelivered = true;
        return { success: true };
      },
    }, () => true, () => currentTime);

    await sync({ settings, presentation });
    expect(scheduleGenerated).toBe(false);
    expect(presentationDelivered).toBe(true);
  });

  test('near-expiry native schedule triggers renewal', async () => {
    let scheduleGenerated = false;
    const currentTime = now();
    const sync = createWidgetSync({
      getScheduleStatus: async () => nativeStatus(settings, new Date('2026-09-16T12:00:00.000Z')),
      updateSchedule: async () => {
        scheduleGenerated = true;
        return { success: true };
      },
      updatePresentation: async () => ({ success: true }),
    }, () => true, () => currentTime);

    await sync({ settings, presentation });
    expect(scheduleGenerated).toBe(true);
  });

  test('failed status query does not permanently lock out subsequent retry', async () => {
    let queryAttempts = 0;
    let scheduleUpdates = 0;
    let presentationUpdates = 0;
    const currentTime = now();
    const sync = createWidgetSync({
      getScheduleStatus: async () => {
        queryAttempts++;
        if (queryAttempts === 1) throw new Error('Transient bridge error');
        return nativeStatus();
      },
      updateSchedule: async () => {
        scheduleUpdates++;
        if (scheduleUpdates === 1) throw new Error('Schedule transfer failed');
        return { success: true };
      },
      updatePresentation: async () => {
        presentationUpdates++;
        return { success: true };
      },
    }, () => true, () => currentTime);

    // First attempt: status query throws, fallback updateSchedule throws -> sync throws
    await expect(sync({ settings, presentation })).rejects.toThrow();
    expect(queryAttempts).toBe(1);
    expect(scheduleUpdates).toBe(1);

    // Second attempt on the same synchronizer: query is retried, native status recovered!
    // Reuses native schedule without invoking updateSchedule a second time!
    await sync({ settings, presentation });
    expect(queryAttempts).toBe(2);
    expect(scheduleUpdates).toBe(1);
    expect(presentationUpdates).toBe(1);
  });

  test.each([
    ['unrepresentable end', 'validUntil', 8_650_000_000_000_000],
    ['truthy non-boolean discriminator', 'exists', 'true'],
    ['missing revision only', 'revision', undefined],
    ...['validFrom', 'validUntil'].flatMap((field) => Object.entries({
      missing: undefined, null: null, text: '1791849600000',
      nan: NaN, infinity: Infinity, negativeInfinity: -Infinity,
      zero: 0, fractional: 1.5, unsafe: Number.MAX_SAFE_INTEGER + 1,
      outsideDate: 8.64e15 + 1, negative: -1,
    }).map(([label, value]) => [`${field} ${label}`, field, value] as const)),
    ...['exists', 'schema', 'engine', 'timezonePolicy', 'timezone',
      'latitude', 'longitude', 'method', 'madhab'].map((field) =>
      [`missing ${field}`, field, undefined] as const),
    ['false discriminator', 'exists', false],
    ['numeric discriminator', 'exists', 1],
    ['unsupported schema', 'schema', 3],
    ['wrong schema type', 'schema', '2'],
    ['unsupported engine', 'engine', 'adhan-other'],
    ['unsupported timezone policy', 'timezonePolicy', 'location'],
    ['unknown timezone', 'timezone', 'Not/A_Timezone'],
    ['empty timezone', 'timezone', ''],
    ['wrong timezone type', 'timezone', 0],
    ['null latitude', 'latitude', null],
    ['latitude wrong type', 'latitude', '21.3891'],
    ['latitude not finite', 'latitude', Infinity],
    ['latitude out of range', 'latitude', 91],
    ['longitude not finite', 'longitude', NaN],
    ['longitude wrong type', 'longitude', '39.8579'],
    ['longitude out of range', 'longitude', -181],
    ['different latitude', 'latitude', 0],
    ['different longitude', 'longitude', 0],
    ['unsupported method', 'method', 6],
    ['fractional method', 'method', 1.5],
    ['wrong method type', 'method', '1'],
    ['different supported method', 'method', 2],
    ['unsupported madhab', 'madhab', 2],
    ['fractional madhab', 'madhab', 0.5],
    ['wrong madhab type', 'madhab', '0'],
    ['different supported madhab', 'madhab', 1],
    ['empty revision', 'revision', ''],
    ['null revision', 'revision', null],
    ['wrong revision type', 'revision', 1],
    ['oversized revision', 'revision', 'x'.repeat(1025)],
    ['oversized timezone', 'timezone', 'x'.repeat(129)],
    ['reversed bounds', 'validUntil', Date.parse('2026-10-09T00:00:00.000Z')],
  ] as const)('rejects %s in otherwise reusable native status', async (_name, field, value) => {
    const status = { ...nativeStatus(), [field]: value };
    const committed: PrayerSchedulePayload[] = [];
    const sync = createWidgetSync({
      getScheduleStatus: async () => status,
      updateSchedule: async (payload) => { committed.push(payload); return { success: true }; },
      updatePresentation: async () => ({ success: true }),
    }, () => true, now);
    await sync({ settings, presentation });
    expect(committed).toHaveLength(1);
    expect(committed[0].calculation).toEqual({
      engine: 'adhan-4.4.3', timezonePolicy: 'device',
      latitude: settings.latitude, longitude: settings.longitude,
      method: settings.method, madhab: settings.madhab,
    });
    expect(committed[0].schedule.validFrom).toBeLessThanOrEqual(now().getTime());
    expect(committed[0].schedule.validUntil).toBeGreaterThan(now().getTime());
  });
  test.each([
    ['UTC retained', 'UTC', 'UTC', 'reuse', 0, 0],
    ['UTC / Etc/UTC', 'UTC', 'Etc/UTC', 'reuse', 0, 0],
    ['New York / US/Eastern', 'America/New_York', 'US/Eastern', 'reuse', 0, 0],
    ['Kolkata / Calcutta', 'Asia/Calcutta', 'Asia/Kolkata', 'reuse', 0, 0],
    ['same offset, different history', 'UTC', 'Africa/Abidjan', 'reuse', 96, 3],
    ['same offset, different DST rules', 'America/Phoenix', 'America/Los_Angeles', 'reuse', 96, 3],
    ['ordinary web', 'UTC', 'UTC', 'web', 0, 0],
    ['stable ticks and presentation edit', 'UTC', 'UTC', 'ticks', 0, 1],
    ['same synchronizer retries status after failed fallback', 'UTC', 'UTC', 'retry', 0, 1],
    ['unrepresentable status repairs a real month', 'UTC', 'UTC', 'corrupt', 32, 1],
  ] as const)('real engine work: %s', (_name, timezone, nativeZone, mode, calculations, transfers) => {
    const child = spawnSync(process.execPath, ['--eval', workProbe], {
      cwd: new URL('..', import.meta.url),
      env: { ...process.env, TZ: timezone, MUAJJIN_QA_ZONE: nativeZone, MUAJJIN_QA_MODE: mode },
      encoding: 'utf8', timeout: 3_000,
    });
    if (child.status !== 0) throw new Error(child.stderr || `Work probe exited ${child.status}`);
    const observed: unknown = JSON.parse(child.stdout);
    expect(observed).toMatchObject({
      baselineCalculations: 32,
      sameOffsetNow: true,
      final: {
        calculations,
        statusQueries: mode === 'web' ? 0 : mode === 'reuse' ? 3 : mode === 'retry' ? 2 : 1,
        scheduleTransfers: transfers,
        presentationTransfers: mode === 'web' ? 0 : mode === 'reuse' ? 3 : mode === 'ticks' ? 2 : 1,
      },
    });
    if (mode === 'retry' || mode === 'ticks') {
      expect(observed).toMatchObject({
        firstRejected: mode === 'retry',
        initial: { calculations: 32, statusQueries: 1, scheduleTransfers: 1, presentationTransfers: mode === 'retry' ? 0 : 1 },
      });
    }
  });
  test.each(['available', 'failed', 'malformed'] as const)(
    'reconsiders the clock after %s status wait',
    async (outcome) => {
      const retained = nativeStatus();
      const barrier = Promise.withResolvers<void>();
      let current = now();
      const committed: PrayerSchedulePayload[] = [];
      const sync = createWidgetSync({
        getScheduleStatus: async () => {
          await barrier.promise;
          if (outcome === 'failed') throw new Error('QA status failed after suspension');
          return outcome === 'malformed' ? null : retained;
        },
        updateSchedule: async (payload) => { committed.push(payload); return { success: true }; },
        updatePresentation: async () => ({ success: true }),
      }, () => true, () => current);
      const pending = sync({ settings, presentation });
      current = new Date('2026-11-20T12:00:00.000Z');
      barrier.resolve();
      await pending;
      expect(committed.map((payload) => payload.schedule.validFrom)).toEqual([
        Date.parse('2026-11-18T00:00:00.000Z'),
      ]);
    },
  );

  test.each(['clock', 'timezone'] as const)(
    'reconsiders %s after schedule acknowledgment',
    async (condition) => {
      const entered = Promise.withResolvers<void>();
      const barrier = Promise.withResolvers<void>();
      let current = now();
      const committed: PrayerSchedulePayload[] = [];
      const sync = createWidgetSync({
        updateSchedule: async (payload) => {
          committed.push(payload);
          if (committed.length === 1) { entered.resolve(); await barrier.promise; }
          return { success: true };
        },
        updatePresentation: async () => ({ success: true }),
      }, () => true, () => current);
      try {
        const pending = sync({ settings, presentation });
        await entered.promise;
        if (condition === 'clock') current = new Date('2026-11-20T12:00:00.000Z');
        else process.env.TZ = 'Asia/Dhaka';
        barrier.resolve();
        await pending;
        expect(committed).toHaveLength(2);
        expect(committed[1].schedule.timezone).toBe(condition === 'clock' ? 'UTC' : 'Asia/Dhaka');
        expect(committed[1].schedule.validUntil).toBeGreaterThan(current.getTime());
        await sync({ settings, presentation });
        expect(committed).toHaveLength(2);
      } finally { process.env.TZ = 'UTC'; barrier.resolve(); }
    },
  );

  test.each(['clock', 'timezone'] as const)(
    'reconsiders %s after presentation acknowledgment',
    async (condition) => {
      const retained = nativeStatus();
      const entered = Promise.withResolvers<void>();
      const barrier = Promise.withResolvers<void>();
      let current = now();
      const committed: PrayerSchedulePayload[] = [];
      let presentations = 0;
      const sync = createWidgetSync({
        getScheduleStatus: async () => retained,
        updateSchedule: async (payload) => { committed.push(payload); return { success: true }; },
        updatePresentation: async () => { presentations++; entered.resolve(); await barrier.promise; return { success: true }; },
      }, () => true, () => current);
      try {
        const pending = sync({ settings, presentation });
        await entered.promise;
        if (condition === 'clock') current = new Date('2026-11-20T12:00:00.000Z');
        else process.env.TZ = 'Asia/Dhaka';
        barrier.resolve();
        await pending;
        expect(committed).toHaveLength(1);
        expect(committed[0].schedule.timezone).toBe(condition === 'clock' ? 'UTC' : 'Asia/Dhaka');
        expect(committed[0].schedule.validUntil).toBeGreaterThan(current.getTime());
        expect(presentations).toBe(1);
      } finally { process.env.TZ = 'UTC'; barrier.resolve(); }
    },
  );
  test.each([null, undefined, [], true, 0, 'status'].map((status) => [status] as const))(
    'repairs non-object native status %p',
    async (status) => {
      const committed: PrayerSchedulePayload[] = [];
      const sync = createWidgetSync({
        getScheduleStatus: async () => status,
        updateSchedule: async (payload) => { committed.push(payload); return { success: true }; },
        updatePresentation: async () => ({ success: true }),
      }, () => true, now);
      await sync({ settings, presentation });
      expect(committed).toHaveLength(1);
      expect(committed[0].schedule.validFrom).toBeLessThanOrEqual(now().getTime());
      expect(committed[0].schedule.validUntil).toBeGreaterThan(now().getTime());
    },
  );

  test.each([
    ['at coverage start', 'validFrom', 0, 0],
    ['before coverage', 'validFrom', -1, 1],
    ['at expiration', 'validUntil', 0, 1],
    ['just before expiration', 'validUntil', -1, 1],
    ['at renewal threshold', 'validUntil', -2 * 86400000, 1],
    ['just before renewal threshold', 'validUntil', -2 * 86400000 - 1, 0],
  ] as const)('native coverage %s', async (_name, boundary, offset, transfers) => {
    const retained = nativeStatus();
    const current = new Date(retained[boundary] + offset);
    const committed: PrayerSchedulePayload[] = [];
    const sync = createWidgetSync({
      getScheduleStatus: async () => retained,
      updateSchedule: async (payload) => { committed.push(payload); return { success: true }; },
      updatePresentation: async () => ({ success: true }),
    }, () => true, () => current);
    await sync({ settings, presentation });
    expect(committed).toHaveLength(transfers);
    if (transfers) {
      expect(committed[0].schedule.validFrom).toBeLessThanOrEqual(current.getTime());
      expect(committed[0].schedule.validUntil).toBeGreaterThan(current.getTime());
    }
  });

  test.each([
    ['validFrom', 1], ['validUntil', 8.64e15],
  ] as const)('metadata %s accepts the native numeric endpoint', async (field, value) => {
    const status = { ...nativeStatus(), [field]: value };
    const committed: PrayerSchedulePayload[] = [];
    const sync = createWidgetSync({
      getScheduleStatus: async () => status,
      updateSchedule: async (payload) => { committed.push(payload); return { success: true }; },
      updatePresentation: async () => ({ success: true }),
    }, () => true, now);
    await sync({ settings, presentation });
    expect(committed).toEqual([]);
  });

  test.each([1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 15].flatMap(
    (method) => [0, 1].map((madhab) => [method, madhab] as const),
  ))('reuses method %i / madhab %i at zero coordinates', async (method, madhab) => {
    const request = { ...settings, method, madhab, latitude: 0, longitude: 0 };
    const retained = nativeStatus(request);
    const committed: PrayerSchedulePayload[] = [];
    const sync = createWidgetSync({
      getScheduleStatus: async () => retained,
      updateSchedule: async (payload) => { committed.push(payload); return { success: true }; },
      updatePresentation: async () => ({ success: true }),
    }, () => true, now);
    await sync({ settings: request, presentation });
    expect(committed).toEqual([]);
  });

  test.each([
    ['latitude', 91], ['longitude', -181], ['method', 6],
    ['method', 1.5], ['madhab', 2], ['madhab', null],
  ] as const)('matching invalid %s=%p cannot certify success', async (field, value) => {
    const status = { ...nativeStatus(), [field]: value };
    const request = { ...settings, [field]: value };
    const committed: PrayerSchedulePayload[] = [];
    const presented: WidgetPresentation[] = [];
    const sync = createWidgetSync({
      getScheduleStatus: async () => status,
      updateSchedule: async (payload) => { committed.push(payload); return { success: true }; },
      updatePresentation: async (payload) => { presented.push(payload.presentation); return { success: true }; },
    }, () => true, now);
    await expect(sync({ settings: request, presentation })).rejects.toBeInstanceOf(Error);
    expect(committed).toEqual([]);
    expect(presented).toEqual([]);
  });
  test.each(['available', 'failed', 'malformed'] as const)(
    'reconsiders timezone after %s status wait',
    async (outcome) => {
      const retained = nativeStatus();
      const barrier = Promise.withResolvers<void>();
      const committed: PrayerSchedulePayload[] = [];
      const sync = createWidgetSync({
        getScheduleStatus: async () => {
          await barrier.promise;
          if (outcome === 'failed') throw new Error('QA status unavailable');
          return outcome === 'malformed' ? null : retained;
        },
        updateSchedule: async (payload) => { committed.push(payload); return { success: true }; },
        updatePresentation: async () => ({ success: true }),
      }, () => true, now);
      try {
        const pending = sync({ settings, presentation });
        process.env.TZ = 'Asia/Dhaka';
        barrier.resolve();
        await pending;
        await sync({ settings, presentation });
        expect(committed.map((payload) => payload.schedule.timezone)).toEqual(['Asia/Dhaka']);
      } finally { process.env.TZ = 'UTC'; barrier.resolve(); }
    },
  );

  test.each(['status', 'schedule', 'presentation'] as const)(
    'newer settings win while %s is pending',
    async (phase) => {
      const retained = nativeStatus();
      const entered = Promise.withResolvers<void>();
      const barrier = Promise.withResolvers<void>();
      let held = false;
      const committed: PrayerSchedulePayload[] = [];
      const presented: WidgetPresentation[] = [];
      const sync = createWidgetSync({
        getScheduleStatus: async () => {
          if (phase === 'status' && !held) {
            held = true; entered.resolve(); await barrier.promise;
          }
          return phase === 'schedule' ? { exists: false } : retained;
        },
        updateSchedule: async (payload) => {
          if (phase === 'schedule' && !held) {
            held = true; entered.resolve(); await barrier.promise;
          }
          committed.push(payload);
          return { success: true };
        },
        updatePresentation: async (payload) => {
          if (phase === 'presentation' && !held) {
            held = true; entered.resolve(); await barrier.promise;
          }
          presented.push(payload.presentation);
          return { success: true };
        },
      }, () => true, now);
      const first = sync({ settings, presentation });
      await entered.promise;
      const latest = { ...settings, madhab: 1 };
      const latestPresentation: WidgetPresentation = { ...presentation, timeFormat: '12h' };
      const second = sync({ settings: latest, presentation: latestPresentation });
      barrier.resolve();
      await Promise.all([first, second]);
      expect(committed).toHaveLength(phase === 'schedule' ? 2 : 1);
      expect(committed.at(-1)?.calculation.madhab).toBe(1);
      expect(presented.at(-1)?.timeFormat).toBe('12h');
    },
  );

  test('matching negative coordinates remain reusable', async () => {
    const request = { ...settings, latitude: -23.55, longitude: -46.63 };
    const retained = nativeStatus(request);
    const committed: PrayerSchedulePayload[] = [];
    const sync = createWidgetSync({
      getScheduleStatus: async () => retained,
      updateSchedule: async (payload) => { committed.push(payload); return { success: true }; },
      updatePresentation: async () => ({ success: true }),
    }, () => true, now);
    await sync({ settings: request, presentation });
    expect(committed).toEqual([]);
  });

  test.each(['schedule', 'presentation'] as const)(
    'negative %s acknowledgment preserves retryability',
    async (phase) => {
      let denied = true;
      const committed: PrayerSchedulePayload[] = [];
      const presented: WidgetPresentation[] = [];
      const sync = createWidgetSync({
        updateSchedule: async (payload) => {
          if (phase === 'schedule' && denied) return { success: false };
          committed.push(payload);
          return { success: true };
        },
        updatePresentation: async (payload) => {
          if (phase === 'presentation' && denied) return { success: false };
          presented.push(payload.presentation);
          return { success: true };
        },
      }, () => true, now);
      await expect(sync({ settings, presentation })).rejects.toBeInstanceOf(Error);
      expect(committed).toHaveLength(phase === 'schedule' ? 0 : 1);
      expect(presented).toEqual([]);
      denied = false;
      await sync({ settings, presentation });
      expect(committed).toHaveLength(1);
      expect(presented).toEqual([presentation]);
    },
  );
});
