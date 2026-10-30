export interface PrayerTime {
  id: string; // Salat ID (e.g., 'fajr', 'dhuhr') - untranslated key for logic
  name: string; // Translated display name (e.g., 'Fajr', 'الفجر')
  start: string; // "HH:MM" in 24h format (use formatTime() for display)
  end?: string; // "HH:MM" in 24h format
  jamaah?: string; // "HH:MM" in 24h format (user optional)
  startTimestamp?: number; // Actual epoch milliseconds, never parsed from display.
  endTimestamp?: number; // Inclusive end: the millisecond before the next boundary.
  jamaahTimestamp?: number;
}

export interface UserSettings {
  method: number;
  madhab: number | null; // null: onboarding has not selected a calculation rule.
  jamaahTimes: {
    Fajr?: string; // Stored as string "HH:MM" for user input
    Dhuhr?: string;
    Asr?: string;
    Maghrib?: string;
    Isha?: string;
  };
  suhoorAdjustment: number;
  iftarAdjustment: number;
  hijriAdjustment: number;
  hijriDateChangeAtMaghrib: boolean;
  latitude?: number | null; // null: damaged persisted coordinate, requires repair.
  longitude?: number | null;
  city?: string;
  country?: string;
  manualLocation: boolean;
  timeFormat: 'system' | '12h' | '24h';
  // Exactly two widget type scales: false = Normal, true = Larger.
  widgetLargeText: boolean;
}

export interface ProhibitedTime {
  id: 'shuruq' | 'zawal' | 'ghurub';
  name: string;
  start: string; // "HH:MM" in 24h format
  end: string; // "HH:MM" in 24h format
  startTimestamp?: number;
  endTimestamp?: number; // Inclusive complete end minute.
}

export type CalculationMethod = {
  id: number;
  name: string;
};

export type Madhab = {
  id: number;
  name: string;
};

export type ContainerOrder = {
  dateTimeContainer: number;
  salatTimesContainer: number;
  prohibitedTimesContainer: number;
  saumTimesContainer: number;
};
