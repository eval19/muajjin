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
});
