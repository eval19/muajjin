import { registerPlugin } from '@capacitor/core';
import { PrayerTime } from '@/types';

export interface PrayerWidgetPluginInterface {
  updateWidgetData(options: {
    currentName: string;
    currentTime: string;
    nextName: string;
    timeRemaining: string;
    progressPercent: number;
    locationName: string;
    hijriDate: string;
    activePrayerId: string;
    prayers: Record<string, string>;
  }): Promise<{ success: boolean }>;
}

const PrayerWidget = registerPlugin<PrayerWidgetPluginInterface>('PrayerWidget');

let lastSyncedMinute = -1;
let lastProgress = -1;
let lastPrayerId = '';

export const syncPrayerWidget = async (params: {
  currentPrayer: PrayerTime | null;
  nextPrayer: PrayerTime | null;
  timeRemaining?: string;
  progressPercent?: number;
  locationName?: string;
  hijriDate?: string;
  allPrayers: PrayerTime[];
  force?: boolean;
}) => {
  const now = new Date();
  const currentMinute = now.getMinutes();
  const currentProgress = params.progressPercent !== undefined ? Math.round(params.progressPercent) : 0;
  const currentId = params.currentPrayer ? params.currentPrayer.id : '';

  // Throttle updates: once per minute or when prayer switches, unless forced
  if (
    !params.force &&
    lastSyncedMinute === currentMinute &&
    lastPrayerId === currentId &&
    Math.abs(lastProgress - currentProgress) < 2
  ) {
    return;
  }

  lastSyncedMinute = currentMinute;
  lastProgress = currentProgress;
  lastPrayerId = currentId;

  try {
    const prayersMap: Record<string, string> = {};
    for (const p of params.allPrayers) {
      prayersMap[p.id] = p.start;
    }

    await PrayerWidget.updateWidgetData({
      currentName: params.currentPrayer ? params.currentPrayer.name : 'Salat',
      currentTime: params.currentPrayer ? params.currentPrayer.start : '--:--',
      nextName: params.nextPrayer ? params.nextPrayer.name : '',
      timeRemaining: params.timeRemaining || '',
      progressPercent: currentProgress,
      locationName: params.locationName || 'Muajjin',
      hijriDate: params.hijriDate || '',
      activePrayerId: currentId || 'dhuhr',
      prayers: prayersMap,
    });
  } catch (error) {
    // Non-native / Web environment
    console.debug('PrayerWidget sync skipped (non-native):', error);
  }
};
