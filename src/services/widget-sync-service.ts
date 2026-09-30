import { registerPlugin } from '@capacitor/core';
import { PrayerTime, UserSettings } from '@/types';
import { LocalPrayerTimes } from '@/services/prayer-times-local';
import { formatTime, getCurrentSalat, getNextSalat } from '@/utils/time-utils';

export interface PrayerWidgetPluginInterface {
  updateWidgetData(options: {
    statusLabel: string;
    currentName: string;
    currentTime: string;
    nextTitle: string;
    nextName: string;
    timeRemaining: string;
    progressPercent: number;
    targetTimestamp: number;
    locationName: string;
    hijriDate: string;
    activePrayerId: string;
    prayers: Record<string, string>;
  }): Promise<{ success: boolean }>;
}

const PrayerWidget = registerPlugin<PrayerWidgetPluginInterface>('PrayerWidget');

let lastSyncedMinute = -1;
let lastSyncedDataHash = '';

function parseTimeToDate(timeStr: string, baseDate = new Date()): Date {
  const [h, m] = (timeStr || '00:00').split(':').map(Number);
  const d = new Date(baseDate);
  d.setHours(h || 0, m || 0, 0, 0);
  return d;
}

export const syncPrayerTimesToWidget = async (params: {
  salats: PrayerTime[];
  timings: LocalPrayerTimes;
  settings: UserSettings;
  hijriDateStr?: string;
  force?: boolean;
}) => {
  const { salats, timings, settings, hijriDateStr, force } = params;
  if (!salats || salats.length === 0 || !timings) return;

  const now = new Date();
  const currentMinute = now.getMinutes();

  const currentSalat = getCurrentSalat(salats);
  const nextSalat = getNextSalat(salats);

  let statusLabel: string;
  let currentName: string;
  let currentTime: string;
  let nextTitle: string;
  let nextName: string;
  let timeRemaining: string;
  let progressPercent: number;
  let targetTimestamp: number;
  let activePrayerId: string;

  if (currentSalat) {
    // Mode 1: Inside an active prayer window
    statusLabel = 'CURRENT SALAT';
    currentName = currentSalat.name;
    currentTime = formatTime(currentSalat.start, settings.timeFormat);
    nextName = nextSalat?.name || '';
    nextTitle = `Next: ${nextName}`;
    activePrayerId = currentSalat.id;

    // Calculate end of current salat
    let endTime: Date;
    if (currentSalat.id === 'isha') {
      const ishaStart = parseTimeToDate(currentSalat.start, now);
      if (now.getHours() >= ishaStart.getHours()) {
        const tomorrow = new Date(now);
        tomorrow.setDate(tomorrow.getDate() + 1);
        endTime = parseTimeToDate(currentSalat.end || timings.Fajr, tomorrow);
      } else {
        endTime = parseTimeToDate(currentSalat.end || timings.Fajr, now);
      }
    } else {
      endTime = parseTimeToDate(currentSalat.end || nextSalat?.start || '12:00', now);
    }

    targetTimestamp = endTime.getTime();
    const diffMs = Math.max(0, targetTimestamp - now.getTime());
    const totalMins = Math.floor(diffMs / 60000);
    const hours = Math.floor(totalMins / 60);
    const mins = totalMins % 60;
    timeRemaining = hours > 0 ? `in ${hours}h ${String(mins).padStart(2, '0')}m` : `in ${mins}m`;

    const startTime = parseTimeToDate(currentSalat.start, now);
    if (currentSalat.id === 'isha' && now.getHours() < parseTimeToDate(currentSalat.start, now).getHours()) {
      startTime.setDate(startTime.getDate() - 1);
    }
    const totalDuration = Math.max(1, targetTimestamp - startTime.getTime());
    const elapsed = Math.max(0, now.getTime() - startTime.getTime());
    progressPercent = Math.min(100, Math.max(0, Math.round((elapsed / totalDuration) * 100)));
  } else {
    // Mode 2: Outside any prayer (e.g. Duha between Sunrise & Dhuhr, or gap between prayers)
    statusLabel = 'NEXT SALAT';
    const targetSalat = nextSalat || salats[0];
    currentName = targetSalat.name;
    currentTime = formatTime(targetSalat.start, settings.timeFormat);
    nextName = targetSalat.name;
    nextTitle = 'Starts in';
    activePrayerId = targetSalat.id;

    let targetStartTime = parseTimeToDate(targetSalat.start, now);
    if (targetStartTime.getTime() <= now.getTime()) {
      targetStartTime.setDate(targetStartTime.getDate() + 1);
    }

    targetTimestamp = targetStartTime.getTime();
    const diffMs = Math.max(0, targetTimestamp - now.getTime());
    const totalMins = Math.floor(diffMs / 60000);
    const hours = Math.floor(totalMins / 60);
    const mins = totalMins % 60;
    timeRemaining = hours > 0 ? `in ${hours}h ${String(mins).padStart(2, '0')}m` : `in ${mins}m`;

    // Calculate progress from previous event (e.g. Sunrise) to next salat
    let prevEventTime: Date;
    if (targetSalat.id === 'dhuhr' && timings.Shuruq) {
      prevEventTime = parseTimeToDate(timings.Shuruq, now);
    } else {
      const prevIdx = salats.findIndex((s) => s.id === targetSalat.id);
      const prevSalat = prevIdx > 0 ? salats[prevIdx - 1] : salats[salats.length - 1];
      prevEventTime = parseTimeToDate(prevSalat.start, now);
      if (prevEventTime.getTime() > now.getTime()) {
        prevEventTime.setDate(prevEventTime.getDate() - 1);
      }
    }

    const totalDuration = Math.max(1, targetTimestamp - prevEventTime.getTime());
    const elapsed = Math.max(0, now.getTime() - prevEventTime.getTime());
    progressPercent = Math.min(100, Math.max(0, Math.round((elapsed / totalDuration) * 100)));
  }

  const prayersMap: Record<string, string> = {
    fajr: formatTime(timings.Fajr, settings.timeFormat),
    sunrise: formatTime(timings.Shuruq, settings.timeFormat),
    dhuhr: formatTime(timings.Dhuhr, settings.timeFormat),
    asr: formatTime(timings.Asr, settings.timeFormat),
    maghrib: formatTime(timings.Maghrib, settings.timeFormat),
    isha: formatTime(timings.Isha, settings.timeFormat),
  };

  const locationName = (settings.city || 'Muajjin').toUpperCase();
  const hijriDate = hijriDateStr || '';

  const dataHash = `${statusLabel}_${currentName}_${currentTime}_${timeRemaining}_${progressPercent}_${locationName}`;
  if (!force && lastSyncedMinute === currentMinute && lastSyncedDataHash === dataHash) {
    return;
  }

  lastSyncedMinute = currentMinute;
  lastSyncedDataHash = dataHash;

  try {
    await PrayerWidget.updateWidgetData({
      statusLabel,
      currentName,
      currentTime,
      nextTitle,
      nextName,
      timeRemaining,
      progressPercent,
      targetTimestamp,
      locationName,
      hijriDate,
      activePrayerId,
      prayers: prayersMap,
    });
  } catch (error) {
    console.debug('PrayerWidget sync skipped (non-native or web):', error);
  }
};
