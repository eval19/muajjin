import { Capacitor, registerPlugin } from '@capacitor/core';
import { z } from 'zod';
import type { UserSettings } from '@/types';
import { calculatePrayerInstants } from '@/services/prayer-times-local';
import { addLocalDays, localDateKey } from '@/utils/time-utils';
import english from '@/i18n/template-en.json';

export interface WidgetScheduleDay {
  date: string;
  previousIsha: number;
  fajr: number;
  sunrise: number;
  dhuhr: number;
  asr: number;
  maghrib: number;
  isha: number;
  nextFajr: number;
}
export interface WidgetPresentation {
  schema: 2;
  language: string;
  direction: 'ltr' | 'rtl';
  timeFormat: UserSettings['timeFormat'];
  locationName: string;
  hijriAdjustment: number;
  hijriDateChangeAtMaghrib: boolean;
  widgetLargeText: boolean;
  labels: Record<string, string>;
  months: string[];
}
export interface PrayerSchedulePayload {
  revision: string;
  calculation: {
    engine: 'adhan-4.4.3';
    timezonePolicy: 'device';
    latitude: number;
    longitude: number;
    method: number;
    madhab: number;
  };
  schedule: {
    schema: 2;
    timezone: string;
    validFrom: number;
    validUntil: number;
    days: WidgetScheduleDay[];
  };
}
// Match NativeScheduleStore.epoch: positive integral milliseconds within Date range.
const nativeEpoch = z.number().finite().int().positive().max(8.64e15);
const scheduleStatusSchema = z.object({
  exists: z.literal(true),
  schema: z.literal(2),
  engine: z.literal('adhan-4.4.3'),
  timezonePolicy: z.literal('device'),
  // Intl resolves IANA links using rules/identity, never the current UTC offset.
  timezone: z.string().min(1).max(128).transform((timeZone) =>
    new Intl.DateTimeFormat('en-US', { timeZone }).resolvedOptions().timeZone),
  validFrom: nativeEpoch,
  validUntil: nativeEpoch,
  latitude: z.number().finite().min(-90).max(90),
  longitude: z.number().finite().min(-180).max(180),
  method: z.union([
    z.literal(1), z.literal(2), z.literal(3), z.literal(4),
    z.literal(5), z.literal(7), z.literal(8), z.literal(9),
    z.literal(10), z.literal(11), z.literal(12), z.literal(15),
  ]),
  madhab: z.union([z.literal(0), z.literal(1)]),
  revision: z.string().min(1).max(1024),
}).refine((status) => status.validFrom < status.validUntil);
const widgetReliabilitySchema = z.object({
  exactAlarmSupported: z.boolean(),
  exactAlarmAllowed: z.boolean(),
  batteryUnrestricted: z.boolean(),
  widgetsPresent: z.boolean(),
  nextTransition: z.number().finite().int().nonnegative(),
  lastDispatch: z.number().finite().int().nonnegative(),
});
export type WidgetReliability = z.infer<typeof widgetReliabilitySchema>;
export interface PrayerWidgetPluginInterface {
  getScheduleStatus?(): Promise<unknown>;
  getWidgetReliability(): Promise<unknown>;
  openExactAlarmSettings(): Promise<void>;
  openBatteryOptimizationSettings(): Promise<void>;
  updateSchedule(payload: PrayerSchedulePayload): Promise<{ success: boolean }>;
  updatePresentation(payload: { revision: string; presentation: WidgetPresentation }): Promise<{ success: boolean }>;
}
interface SyncRequest {
  settings: UserSettings;
  presentation: WidgetPresentation;
}

export function buildWidgetPresentation(
  settings: UserSettings,
  translate: (key: string) => string,
  language: string,
  direction: 'ltr' | 'rtl',
): WidgetPresentation {
  const keys: Record<string, string> = {
    fajr: 'salats.fajr', sunrise: 'salats.shuruq', dhuhr: 'salats.dhuhr',
    asr: 'salats.asr', maghrib: 'salats.maghrib', isha: 'salats.isha',
    current: 'salatTimes.current', next: 'salatTimes.next',
    now: 'nativeWidget.now', in: 'nativeWidget.in', at: 'nativeWidget.at',
    startsIn: 'nativeWidget.startsIn', am: 'common.am', pm: 'common.pm',
    remainingHours: 'nativeWidget.remainingHours', remainingMinutes: 'nativeWidget.remainingMinutes',
    recovery: 'nativeWidget.recovery',
  };
  const labels: Record<string, string> = {};
  for (const [name, key] of Object.entries(keys)) {
    const value = translate(key);
    let fallback: unknown = english.translations;
    for (const part of key.split('.')) fallback = (fallback as Record<string, unknown>)[part];
    labels[name] = value && value !== key ? value : String(fallback);
  }
  const months = Array.from({ length: 12 }, (_, index) => {
    const key = `hijri.months.${index + 1}`;
    const value = translate(key);
    return value && value !== key ? value : english.translations.hijri.months[String(index + 1) as keyof typeof english.translations.hijri.months];
  });
  return {
    schema: 2, language, direction, timeFormat: settings.timeFormat,
    locationName: settings.city ?? '', hijriAdjustment: settings.hijriAdjustment,
    hijriDateChangeAtMaghrib: settings.hijriDateChangeAtMaghrib, labels, months,
    widgetLargeText: settings.widgetLargeText,
  };
}

export function buildWidgetSchedule(settings: UserSettings, now: Date): PrayerSchedulePayload {
  const first = new Date(now);
  first.setHours(0, 0, 0, 0);
  first.setDate(first.getDate() - 2);
  const calculation: PrayerSchedulePayload['calculation'] = {
    engine: 'adhan-4.4.3', timezonePolicy: 'device',
    latitude: settings.latitude === undefined ? 21.3891 : settings.latitude!,
    longitude: settings.longitude === undefined ? 39.8579 : settings.longitude!,
    method: settings.method, madhab: settings.madhab,
  };
  let previousIsha = calculatePrayerInstants(addLocalDays(first, -1), settings).isha.getTime();
  let current = calculatePrayerInstants(first, settings);
  const days: WidgetScheduleDay[] = [];
  for (let index = 0; index < 30; index++) {
    const date = addLocalDays(first, index);
    const following = calculatePrayerInstants(addLocalDays(date, 1), settings);
    days.push({
      date: localDateKey(date), previousIsha,
      fajr: current.fajr.getTime(), sunrise: current.sunrise.getTime(),
      dhuhr: current.dhuhr.getTime(), asr: current.asr.getTime(),
      maghrib: current.maghrib.getTime(), isha: current.isha.getTime(), nextFajr: following.fajr.getTime(),
    });
    previousIsha = current.isha.getTime();
    current = following;
  }
  const schedule: PrayerSchedulePayload['schedule'] = {
    schema: 2, timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    validFrom: first.getTime(), validUntil: addLocalDays(first, 30).getTime(), days,
  };
  return { revision: JSON.stringify({ calculation, timezone: schedule.timezone, validFrom: schedule.validFrom }), calculation, schedule };
}

/** Serial delivery means the latest intent wins; only durable acknowledgments are cached. */
export function createWidgetSync(
  adapter: PrayerWidgetPluginInterface,
  supported: () => boolean,
  clock: () => Date = () => new Date(),
): (request: SyncRequest) => Promise<void> {
  let desired: SyncRequest | undefined;
  let running: Promise<void> | undefined;
  let successfulSchedule: { key: string; validFrom: number; validUntil: number } | undefined;
  let checkedNativeStatus = false;
  let successfulPresentation: string | undefined;
  let presentationRevision = 0;
  const drain = async () => {
    while (desired) {
      const request = desired;
      desired = undefined;
      try {
        const settings = request.settings;
        const targetLat = settings.latitude === undefined ? 21.3891 : settings.latitude;
        const targetLon = settings.longitude === undefined ? 39.8579 : settings.longitude;
        let targetTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
        let key = JSON.stringify({
          engine: 'adhan-4.4.3', timezonePolicy: 'device',
          latitude: targetLat,
          longitude: targetLon,
          method: settings.method, madhab: settings.madhab,
          timezone: targetTimezone,
        });
        let now = clock();
        let nearExpiry = addLocalDays(now, 2).getTime();

        if (!successfulSchedule && !checkedNativeStatus && adapter.getScheduleStatus) {
          let cancelStatusTimeout: (() => void) | undefined;
          let validatedStatus: z.infer<typeof scheduleStatusSchema> | undefined;
          try {
            const statusPromise = adapter.getScheduleStatus();
            const timeoutPromise = new Promise<never>((_, reject) => {
              const timeout = setTimeout(() => reject(new Error('Status query timeout')), 5000);
              cancelStatusTimeout = () => clearTimeout(timeout);
            });
            const status = await Promise.race([statusPromise, timeoutPromise]);
            checkedNativeStatus = true;
            validatedStatus = scheduleStatusSchema.parse(status);
          } catch {
            // Invalid/unavailable status falls through to repair; failed repair remains retryable.
          } finally {
            cancelStatusTimeout?.();
            // Failed and malformed responses can also arrive after suspension.
            now = clock();
            targetTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
            key = JSON.stringify({
              engine: 'adhan-4.4.3', timezonePolicy: 'device',
              latitude: targetLat,
              longitude: targetLon,
              method: settings.method, madhab: settings.madhab,
              timezone: targetTimezone,
            });
            nearExpiry = addLocalDays(now, 2).getTime();
          }
          if (
            validatedStatus &&
            validatedStatus.timezone === targetTimezone &&
            validatedStatus.latitude === targetLat &&
            validatedStatus.longitude === targetLon &&
            validatedStatus.method === settings.method &&
            validatedStatus.madhab === settings.madhab &&
            now.getTime() >= validatedStatus.validFrom &&
            nearExpiry < validatedStatus.validUntil
          ) {
            successfulSchedule = { key, validFrom: validatedStatus.validFrom, validUntil: validatedStatus.validUntil };
          }
        }
        if (desired) continue;

        if (!successfulSchedule || successfulSchedule.key !== key || now.getTime() < successfulSchedule.validFrom || nearExpiry >= successfulSchedule.validUntil) {
          const payload = buildWidgetSchedule(settings, now);
          const response = await adapter.updateSchedule(payload);
          if (!response.success) throw new Error('Schedule was not durably acknowledged');
          successfulSchedule = { key, validFrom: payload.schedule.validFrom, validUntil: payload.schedule.validUntil };
          // Re-enter after acknowledgments to reconsider time, zone and newest intent.
          desired ??= request;
          continue;
        }
        const presentationKey = JSON.stringify(request.presentation);
        if (successfulPresentation !== presentationKey) {
          const response = await adapter.updatePresentation({ revision: `presentation:${++presentationRevision}`, presentation: request.presentation });
          if (!response.success) throw new Error('Presentation was not durably acknowledged');
          successfulPresentation = presentationKey;
          desired ??= request;
          continue;
        }
      } catch {
        if (!successfulSchedule) checkedNativeStatus = false;
        if (desired) continue;
        throw new Error('errors.widgetSyncFailed');
      }
    }
  };
  const sync = (request: SyncRequest): Promise<void> => {
    if (!supported()) return Promise.resolve();
    desired = request;
    if (!running) running = drain().finally(() => {
      running = undefined;
      // A synchronous cache hit can finish before a same-turn newer intent arrives.
      if (desired) return sync(desired);
    });
    return running;
  };
  return sync;
}

const PrayerWidget = registerPlugin<PrayerWidgetPluginInterface & {
  getSystemClockFormat(): Promise<{ timeFormat: '12h' | '24h' }>;
}>('PrayerWidget');
export const syncPrayerTimesToWidget = createWidgetSync(PrayerWidget, () =>
  Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'android' && Capacitor.isPluginAvailable('PrayerWidget'),
);

export async function readNativeSystemTimeFormat(): Promise<'12h' | '24h'> {
  const result = await PrayerWidget.getSystemClockFormat();
  if (result.timeFormat !== '12h' && result.timeFormat !== '24h') throw new Error('Invalid native system clock format');
  return result.timeFormat;
}

export async function readWidgetReliability(): Promise<WidgetReliability> {
  return widgetReliabilitySchema.parse(await PrayerWidget.getWidgetReliability());
}

export async function openExactAlarmSettings(): Promise<void> {
  await PrayerWidget.openExactAlarmSettings();
}

export async function openBatteryOptimizationSettings(): Promise<void> {
  await PrayerWidget.openBatteryOptimizationSettings();
}
