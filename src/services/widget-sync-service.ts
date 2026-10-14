import { Capacitor, registerPlugin } from '@capacitor/core';
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
export interface PrayerWidgetPluginInterface {
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
  let successfulPresentation: string | undefined;
  let presentationRevision = 0;

  const drain = async () => {
    while (desired) {
      const request = desired;
      desired = undefined;
      try {
        const now = clock();
        const settings = request.settings;
        const key = JSON.stringify({
          engine: 'adhan-4.4.3', timezonePolicy: 'device',
          latitude: settings.latitude === undefined ? 21.3891 : settings.latitude,
          longitude: settings.longitude === undefined ? 39.8579 : settings.longitude,
          method: settings.method, madhab: settings.madhab,
          timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        });
        const nearExpiry = addLocalDays(now, 2).getTime();
        if (!successfulSchedule || successfulSchedule.key !== key || now.getTime() < successfulSchedule.validFrom || nearExpiry >= successfulSchedule.validUntil) {
          const payload = buildWidgetSchedule(settings, now);
          const response = await adapter.updateSchedule(payload);
          if (!response.success) throw new Error('Schedule was not durably acknowledged');
          successfulSchedule = { key, validFrom: payload.schedule.validFrom, validUntil: payload.schedule.validUntil };
        }
        // A newer locale/settings intent can arrive while native storage is in flight.
        if (desired) continue;
        const presentationKey = JSON.stringify(request.presentation);
        if (successfulPresentation !== presentationKey) {
          const response = await adapter.updatePresentation({ revision: `presentation:${++presentationRevision}`, presentation: request.presentation });
          if (!response.success) throw new Error('Presentation was not durably acknowledged');
          successfulPresentation = presentationKey;
        }
      } catch {
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
