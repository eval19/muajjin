import {
  PrayerTimes,
  Coordinates,
  CalculationMethod,
  Madhab as AdhanMadhab,
} from 'adhan';
import type { UserSettings } from '@/types';

export interface LocalPrayerTimes {
  Fajr: string;
  Shuruq: string;
  Dhuhr: string;
  Asr: string;
  Ghurub: string;
  Maghrib: string;
  Isha: string;
}

export interface PrayerInstants {
  fajr: Date;
  sunrise: Date;
  dhuhr: Date;
  asr: Date;
  maghrib: Date;
  isha: Date;
}

const METHOD_FACTORIES = {
  1: CalculationMethod.Karachi,
  2: CalculationMethod.NorthAmerica,
  3: CalculationMethod.MuslimWorldLeague,
  4: CalculationMethod.UmmAlQura,
  5: CalculationMethod.Egyptian,
  7: CalculationMethod.Tehran,
  8: CalculationMethod.Dubai,
  9: CalculationMethod.Kuwait,
  10: CalculationMethod.Qatar,
  11: CalculationMethod.Singapore,
  12: CalculationMethod.MoonsightingCommittee,
  15: CalculationMethod.MoonsightingCommittee,
};

/**
 * The supplied date selects a local Gregorian calendar day. Adhan returns
 * actual instants; display uses the device timezone, not a location timezone.
 * Only absent coordinates use the initial Makkah default.
 */
export function calculatePrayerInstants(
  date: Date,
  settings: UserSettings,
): PrayerInstants {
  if (!Number.isFinite(date.getTime())) {
    throw new RangeError('Invalid prayer calculation date');
  }
  const latitude = settings.latitude === undefined ? 21.3891 : settings.latitude;
  const longitude = settings.longitude === undefined ? 39.8579 : settings.longitude;
  if (
    typeof latitude !== 'number' || !Number.isFinite(latitude) || Math.abs(latitude) > 90 ||
    typeof longitude !== 'number' || !Number.isFinite(longitude) || Math.abs(longitude) > 180
  ) {
    throw new RangeError('Invalid prayer calculation coordinates');
  }
  const factory = METHOD_FACTORIES[settings.method as keyof typeof METHOD_FACTORIES];
  if (!factory) throw new RangeError('Unsupported prayer calculation method');
  if (settings.madhab !== 0 && settings.madhab !== 1) {
    throw new RangeError('Unsupported prayer calculation madhab');
  }
  const parameters = factory();
  parameters.madhab = settings.madhab === 1 ? AdhanMadhab.Hanafi : AdhanMadhab.Shafi;
  const times = new PrayerTimes(new Coordinates(latitude, longitude), date, parameters);
  const instants: PrayerInstants = {
    fajr: times.fajr,
    sunrise: times.sunrise,
    dhuhr: times.dhuhr,
    asr: times.asr,
    maghrib: times.maghrib,
    isha: times.isha,
  };
  let previous = -Infinity;
  for (const instant of Object.values(instants)) {
    const timestamp = instant.getTime();
    if (!Number.isFinite(timestamp) || timestamp <= previous) {
      throw new RangeError('Prayer times are unavailable for this date and location');
    }
    previous = timestamp;
  }
  return instants;
}

/**
 * Calculate salat times locally using adhan library (no API call needed)
 * @param date - The Gregorian date to calculate salat times for
 * @param settings - User settings including location and calculation method
 * @returns Salat times in 24-hour format ("HH:MM")
 */
export function calculatePrayerTimesLocally(
  date: Date,
  settings: UserSettings,
): LocalPrayerTimes {
  const instants = calculatePrayerInstants(date, settings);
  return {
    Fajr: formatPrayerClock(instants.fajr),
    Shuruq: formatPrayerClock(instants.sunrise),
    Dhuhr: formatPrayerClock(instants.dhuhr),
    Asr: formatPrayerClock(instants.asr),
    Ghurub: formatPrayerClock(instants.maghrib),
    Maghrib: formatPrayerClock(instants.maghrib),
    Isha: formatPrayerClock(instants.isha),
  };
}


/**
 * Format Date object to "HH:MM" string (24-hour format)
 */
export function formatPrayerClock(date: Date): string {
  if (!Number.isFinite(date.getTime())) throw new RangeError('Invalid prayer display instant');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}`;
}

/**
 * Calculation methods supported by adhan library
 * Sorted alphabetically A-Z
 * Maps to the IDs used in the app
 */
export const CALCULATION_METHODS = [
  { id: 1, name: 'University of Islamic Sciences, Karachi' },
  { id: 2, name: 'Islamic Society of North America (ISNA)' },
  { id: 3, name: 'Muslim World League' },
  { id: 4, name: 'Umm al-Qura, Makkah' },
  { id: 5, name: 'Egyptian General Authority of Survey' },
  { id: 7, name: 'Institute of Geophysics, University of Tehran' },
  { id: 8, name: 'Dubai' },
  { id: 9, name: 'Kuwait' },
  { id: 10, name: 'Qatar' },
  { id: 11, name: 'Majlis Ugama Islam Singapore, Singapore' },
  { id: 12, name: 'Moonsighting Committee Worldwide' },
];

// Map calculation method IDs to translation keys
export const CALCULATION_METHOD_KEYS: Record<number, string> = {
  1: 'calculationMethods.karachi',
  2: 'calculationMethods.northAmerica',
  3: 'calculationMethods.muslimWorldLeague',
  4: 'calculationMethods.ummAlQura',
  5: 'calculationMethods.egyptian',
  7: 'calculationMethods.tehran',
  8: 'calculationMethods.dubai',
  9: 'calculationMethods.kuwait',
  10: 'calculationMethods.qatar',
  11: 'calculationMethods.singapore',
  12: 'calculationMethods.moonsightingCommittee',
};

/**
 * Madhabs (schools of jurisprudence)
 */
export const MADHABS = [
  { id: 0, name: "Shafi'i, Maliki, Hanbali" },
  { id: 1, name: 'Hanafi' },
];
