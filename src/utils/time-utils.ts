import { calculatePrayerInstants, formatPrayerClock } from '@/services/prayer-times-local';
import type { PrayerInstants } from '@/services/prayer-times-local';
import type { PrayerTime, ProhibitedTime, UserSettings } from '@/types';

// Cache for translation function to avoid circular imports
let translationFunction: ((key: string) => string) | null = null;

export function setTranslationFunction(fn: (key: string) => string) {
  translationFunction = fn;
}

function t(key: string): string {
  return translationFunction ? translationFunction(key) : key;
}

let systemTimeFormat: '12h' | '24h' | undefined;

/** AppProvider reconciles the OS preference at startup/resume, never per tick. */
export function setSystemTimeFormat(format: '12h' | '24h'): void {
  systemTimeFormat = format;
}
/**
 * Detect if the user's system uses 12-hour or 24-hour time format
 * Uses Intl.DateTimeFormat to check the system preference
 */
export function getSystemTimeFormat(): '12h' | '24h' {
  return systemTimeFormat ?? (systemTimeFormat = new Intl.DateTimeFormat(undefined, { hour: 'numeric' })
    .resolvedOptions().hour12 ? '12h' : '24h');
}

/**
 * Resolve the actual time format ('12h' or '24h') from user preference
 * If user selected 'system', detect system preference
 */
export function resolveTimeFormat(
  format: 'system' | '12h' | '24h',
): '12h' | '24h' {
  if (format === 'system') {
    return getSystemTimeFormat();
  }
  return format;
}

// Format time from 24h format to 12h or 24h format based on preference
export function formatTime(
  time: string,
  format: 'system' | '12h' | '24h' = 'system',
  translate: (key: string) => string = t,
): string {
  if (!time || typeof time !== 'string') {
    return '--:--';
  }

  const parts = time.split(':');
  if (parts.length < 2) {
    return '--:--';
  }

  const [hours, minutes] = parts;
  const hoursNum = parseInt(hours, 10);

  if (isNaN(hoursNum) || !minutes) {
    return '--:--';
  }

  // Resolve the actual format (handle 'system' option)
  const actualFormat = resolveTimeFormat(format);

  if (actualFormat === '24h') {
    return `${String(hoursNum).padStart(2, '0')}:${minutes}`;
  }

  // 12h format with AM/PM
  const ampm = hoursNum >= 12 ? translate('common.pm') : translate('common.am');
  const hours12 = hoursNum % 12 || 12;
  return `${hours12}:${minutes} ${ampm}`;
}


export function localDateKey(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export function addLocalDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

export function nextLocalMidnight(date: Date): number {
  const next = addLocalDays(date, 1);
  next.setHours(0, 0, 0, 0);
  return next.getTime();
}

const MINUTE = 60_000;
type Translate = (key: string) => string;

/** User-entered Jama'ah clocks are the one clock-to-date input boundary. */
function jamaahTimestamp(clock: string | undefined, start: Date, overnight: boolean): number | undefined {
  if (!clock) return undefined;
  const match = /^([01]\d|2[0-3]):([0-5]\d)$/.exec(clock);
  if (!match) throw new RangeError('Invalid Jama’ah time');
  const result = new Date(start);
  result.setHours(Number(match[1]), Number(match[2]), 0, 0);
  if (overnight && result.getTime() < start.getTime()) {
    result.setDate(result.getDate() + 1);
  }
  return result.getTime();
}

export function buildPrayerWindows(
  instants: PrayerInstants,
  nextFajr: Date,
  settings: UserSettings,
  translate: Translate = (key) => key,
): PrayerTime[] {
  const definitions = [
    ['fajr', 'Fajr', instants.fajr, instants.sunrise],
    ['dhuhr', 'Dhuhr', instants.dhuhr, instants.asr],
    ['asr', 'Asr', instants.asr, instants.maghrib],
    ['maghrib', 'Maghrib', instants.maghrib, instants.isha],
    ['isha', 'Isha', instants.isha, nextFajr],
  ] as const;
  return definitions.map(([id, key, start, boundary]) => {
    if (boundary.getTime() <= start.getTime()) {
      throw new RangeError('Prayer windows are not chronological');
    }
    const jamaah = settings.jamaahTimes[key];
    return {
      id, name: translate(`salats.${id}`),
      start: formatPrayerClock(start),
      // Display the preceding minute, including its full 59.999 seconds.
      end: formatPrayerClock(new Date(boundary.getTime() - MINUTE)),
      jamaah,
      startTimestamp: start.getTime(),
      endTimestamp: boundary.getTime() - 1,
      jamaahTimestamp: jamaahTimestamp(jamaah, start, id === 'isha'),
    };
  });
}

export interface FastingEvent {
  id: 'suhoor' | 'iftar';
  timestamp: number;
}

export interface PrayerDay {
  instants: PrayerInstants;
  prayers: PrayerTime[];
  timeline: PrayerTime[];
  prohibitedTimes: ProhibitedTime[];
  prohibitedTimeline: ProhibitedTime[];
  maghribForHijri?: number;
  suhoorTimestamp: number;
  iftarTimestamp: number;
  fastingEvents: FastingEvent[];
  suhoorTime: string;
  iftarTime: string;
}

/** One input revision produces prayer, prohibited and fasting data together. */
export function derivePrayerDay(date: Date, settings: UserSettings, translate: Translate = (key) => key, instantsForDate?: (date: Date) => PrayerInstants): PrayerDay {
  const midnight = new Date(date);
  midnight.setHours(0, 0, 0, 0);
  const from = midnight.getTime();
  const until = nextLocalMidnight(midnight);
  // Coordinates do not imply the device timezone. Adjacent calculation dates
  // can supply today's events; keep their epochs instead of re-dating clocks.
  const days = Array.from({ length: 6 }, (_, index) =>
    instantsForDate ? instantsForDate(addLocalDays(midnight, index - 2)) : calculatePrayerInstants(addLocalDays(midnight, index - 2), settings));
  const windows = days.slice(0, -1).map((instants, index) =>
    buildPrayerWindows(instants, days[index + 1].fajr, settings, translate));
  const timeline = windows.flat();
  const prayers = windows[2].map((prayer) => timeline.find((candidate) =>
    candidate.id === prayer.id && candidate.startTimestamp! >= from &&
    candidate.startTimestamp! < until) ?? prayer);
  const prohibitedDays = days.slice(0, -1).map((instants) => getProhibitedTimes(instants, translate));
  const prohibitedTimeline = prohibitedDays.flat();
  const prohibitedTimes = prohibitedDays[2].map((time) => prohibitedTimeline.find((candidate) =>
    candidate.id === time.id && candidate.endTimestamp! >= from &&
    candidate.startTimestamp! < until) ?? time);
  const fajr = days.find((instants) => instants.fajr.getTime() >= from && instants.fajr.getTime() < until)?.fajr ?? days[2].fajr;
  const maghrib = days.find((instants) => instants.maghrib.getTime() >= from && instants.maghrib.getTime() < until)?.maghrib;
  const suhoorTimestamp = fajr.getTime() + settings.suhoorAdjustment * MINUTE;
  const iftarTimestamp = (maghrib ?? days[2].maghrib).getTime() + settings.iftarAdjustment * MINUTE;
  const fastingEvents: FastingEvent[] = days.slice(0, -1).flatMap((instants) => [
    { id: 'suhoor' as const, timestamp: instants.fajr.getTime() + settings.suhoorAdjustment * MINUTE },
    { id: 'iftar' as const, timestamp: instants.maghrib.getTime() + settings.iftarAdjustment * MINUTE },
  ]).sort((a, b) => a.timestamp - b.timestamp);
  return {
    instants: days[2], prayers, timeline, prohibitedTimes, prohibitedTimeline,
    maghribForHijri: maghrib?.getTime(),
    suhoorTimestamp, iftarTimestamp, fastingEvents,
    suhoorTime: formatPrayerClock(new Date(suhoorTimestamp)),
    iftarTime: formatPrayerClock(new Date(iftarTimestamp)),
  };
}

// Timeline consumers receive already-chronological dated data, not clock strings.
export function getCurrentSalat(salatTimes: readonly PrayerTime[], now: Date): PrayerTime | null {
  const timestamp = now.getTime();
  return salatTimes.find((salat) =>
    timestamp >= salat.startTimestamp && timestamp <= salat.endTimestamp,
  ) ?? null;
}

export function getNextSalat(salatTimes: readonly PrayerTime[], now: Date): PrayerTime | null {
  return salatTimes.find((salat) => salat.startTimestamp > now.getTime()) ?? null;
}

export function isWithinWindow(window: Pick<PrayerTime, 'startTimestamp' | 'endTimestamp'>, now: Date): boolean {
  return now.getTime() >= window.startTimestamp && now.getTime() <= window.endTimestamp;
}

export function prayerBoundaries(prayers: readonly PrayerTime[]): number[] {
  return prayers.flatMap((prayer) => [
    prayer.startTimestamp, prayer.endTimestamp === undefined ? undefined : prayer.endTimestamp + 1,
  ]).filter((value): value is number => Number.isFinite(value));
}

export function getPrayerCountdown(prayer: PrayerTime, now: Date) {
  const start = prayer.startTimestamp;
  const jamaah = prayer.jamaahTimestamp;
  const target = jamaah !== undefined && jamaah > now.getTime() ? 'jamaah' : 'end';
  const timestamp = target === 'jamaah' ? jamaah : prayer.endTimestamp === undefined ? undefined : prayer.endTimestamp + 1;
  if (!Number.isFinite(start) || !Number.isFinite(timestamp)) {
    throw new RangeError('Countdown requires dated prayer windows');
  }
  return {
    target,
    remainingMilliseconds: Math.max(0, timestamp - now.getTime()),
    progressPercent: Math.min(100, Math.max(0, (now.getTime() - start) / Math.max(1, timestamp - start) * 100)),
  };
}

export function getNextFastingEvent(events: readonly FastingEvent[], now: Date): FastingEvent | null {
  return events.find((event) => event.timestamp > now.getTime()) ?? null;
}

export function formatCountdown(milliseconds: number): string {
  const totalSeconds = Math.max(0, Math.floor(milliseconds / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor(totalSeconds / 60) % 60;
  return `${hours < 1 ? '' : `${hours}:`}${String(minutes).padStart(2, '0')}:${String(totalSeconds % 60).padStart(2, '0')}`;
}

export function getProhibitedTimes(instants: PrayerInstants, translate: Translate = t): ProhibitedTime[] {
  const windows: Array<['shuruq' | 'zawal' | 'ghurub', number, number]> = [
    ['shuruq', instants.sunrise.getTime(), instants.sunrise.getTime() + 15 * MINUTE],
    ['zawal', instants.dhuhr.getTime() - 5 * MINUTE, instants.dhuhr.getTime()],
    ['ghurub', instants.maghrib.getTime() - 15 * MINUTE, instants.maghrib.getTime()],
  ];
  return windows.map(([id, start, boundary]) => ({
    id, name: translate(`prohibited.${id}`),
    start: formatPrayerClock(new Date(start)),
    end: formatPrayerClock(new Date(boundary - MINUTE)),
    startTimestamp: start,
    endTimestamp: boundary - 1,
  }));
}

// Format current time
export function getCurrentTimeFormatted(
  format: 'system' | '12h' | '24h' = 'system',
  now: Date = new Date(),
): string {
  // The caller can provide a visible, reconciled clock.

  // Resolve the actual format (handle 'system' option)
  const actualFormat = resolveTimeFormat(format);

  if (actualFormat === '12h') {
    let hours = now.getHours();
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    const ampm = hours >= 12 ? t('common.pm') : t('common.am');

    hours = hours % 12;
    hours = hours ? hours : 12; // the hour '0' should be '12'
    const hoursStr = String(hours).padStart(2, '0');

    return `${hoursStr}:${minutes}:${seconds} ${ampm}`;
  } else {
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');

    return `${hours}:${minutes}:${seconds}`;
  }
}

// Format Gregorian date
export function formatGregorianDate(date: Date): string {
  return new Intl.DateTimeFormat('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(date);
}

// Get day name only (e.g., "Mon")
export function getDayName(date: Date, short: boolean = true): string {
  const weekday = new Intl.DateTimeFormat('en-US', {
    weekday: short ? 'short' : 'long',
  }).format(date);
  return weekday;
}

// Format Gregorian date without day name (e.g., "12 Aug 2026")
export function formatGregorianDateShort(date: Date, language?: string): string {
  return new Intl.DateTimeFormat(language?.replace(/_/g, '-'), {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(date);
}
