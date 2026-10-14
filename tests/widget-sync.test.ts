import { describe, expect, test } from 'bun:test';
import { createWidgetSync, buildWidgetPresentation } from '../src/services/widget-sync-service';
import { DEFAULT_SETTINGS } from '../src/constants/default-settings';
import type { PrayerSchedulePayload, WidgetPresentation, PrayerWidgetPluginInterface } from '../src/services/widget-sync-service';
import { derivePrayerDay } from '../src/utils/time-utils';

const now = () => new Date(2026, 9, 13, 12);
const translate = (key: string) => key;
const presentation: WidgetPresentation = buildWidgetPresentation(DEFAULT_SETTINGS, translate, 'en', 'ltr');
const settings = { ...DEFAULT_SETTINGS, latitude: 21.3891, longitude: 39.8579, madhab: 0 };

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
    expect(schedules).toEqual([first]);
    expect(first.schedule.days[29].nextFajr).toBeGreaterThan(first.schedule.days[29].isha);
    expect(first.schedule.days[0].previousIsha).toBeLessThan(first.schedule.days[0].fajr);
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
    await expect(sync({ settings, presentation })).rejects.toThrow('errors.widgetSyncFailed');
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
  test('fresh JS synchronizer reuses matching valid native schedule without full-month rebuild', async () => {
    let scheduleGenerated = false;
    let presentationDelivered = false;
    const currentTime = now();
    const sync = createWidgetSync({
      getScheduleStatus: async () => ({
        exists: true,
        schema: 2,
        engine: 'adhan-4.4.3',
        timezonePolicy: 'device',
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        latitude: 21.3891,
        longitude: 39.8579,
        method: DEFAULT_SETTINGS.method,
        madhab: 0,
        validFrom: new Date(2026, 9, 10).getTime(),
        validUntil: new Date(2026, 10, 10).getTime(),
        revision: 'existing-rev',
      }),
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
      getScheduleStatus: async () => ({
        exists: true,
        schema: 2,
        engine: 'adhan-4.4.3',
        timezonePolicy: 'device',
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        latitude: 21.3891,
        longitude: 39.8579,
        method: DEFAULT_SETTINGS.method,
        madhab: 0,
        validFrom: new Date(2026, 8, 10).getTime(),
        validUntil: new Date(2026, 9, 14).getTime(), // near expiry (< 2 days from Oct 13)
        revision: 'old-rev',
      }),
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
        return {
          exists: true,
          schema: 2,
          engine: 'adhan-4.4.3',
          timezonePolicy: 'device',
          timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
          latitude: 21.3891,
          longitude: 39.8579,
          method: DEFAULT_SETTINGS.method,
          madhab: 0,
          validFrom: new Date(2026, 9, 10).getTime(),
          validUntil: new Date(2026, 10, 10).getTime(),
          revision: 'valid-rev',
        };
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

  test('missing revision in native status rejects reuse and requires schedule transfer', async () => {
    let scheduleGenerated = false;
    const currentTime = now();
    const sync = createWidgetSync({
      getScheduleStatus: async () => ({
        exists: true,
        engine: 'adhan-4.4.3',
        timezonePolicy: 'device',
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        latitude: 21.3891,
        longitude: 39.8579,
        method: DEFAULT_SETTINGS.method,
        madhab: 0,
        validFrom: new Date(2026, 9, 10).getTime(),
        validUntil: new Date(2026, 10, 10).getTime(),
        revision: '', // empty revision
      }),
      updateSchedule: async () => {
        scheduleGenerated = true;
        return { success: true };
      },
      updatePresentation: async () => ({ success: true }),
    }, () => true, () => currentTime);

    await sync({ settings, presentation });
    expect(scheduleGenerated).toBe(true);
  });
});
