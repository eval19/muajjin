import { z } from 'zod';
import { DEFAULT_CONTAINER_ORDER, DEFAULT_SETTINGS } from '../constants/default-settings';
import type { UserSettings } from '../types';
import { COUNTER_KEYS, INITIAL_COUNTS } from '../features/qaza-tracker/constants';
import type { QazaCounts } from '../features/qaza-tracker/types';

export interface HomeSection { id: string; name: string; visible: boolean; order: number }
export interface AppSettings extends UserSettings { onboardingComplete: boolean; homeSections?: HomeSection[] }
export const defaultAppSettings: AppSettings = {
  ...DEFAULT_SETTINGS,
  onboardingComplete: false,
  homeSections: [
    { id: 'dateTime', name: 'Date & Time Header', visible: true, order: 0 },
    { id: 'currentSalat', name: 'Current Salat', visible: true, order: 1 },
    { id: 'nextSalat', name: 'Next Salat', visible: true, order: 2 },
    { id: 'salatTimes', name: 'Salat Times', visible: true, order: 3 },
    { id: 'prohibitedTimes', name: 'Prohibited Times', visible: true, order: 4 },
    { id: 'saumTimes', name: 'Saum Times', visible: true, order: 5 },
  ],
};

const record = (value: unknown): Record<string, unknown> =>
  value !== null && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {};
const finiteInteger = z.number().finite().int().safe();
const methodSchema = z.union([1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 15].map((id) => z.literal(id)) as [z.ZodLiteral<number>, z.ZodLiteral<number>, ...z.ZodLiteral<number>[]]);
const clockSchema = z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/);
const textSchema = z.string().max(256);
function validOr<T>(schema: z.ZodType<T>, value: unknown, fallback: T): T {
  const parsed = schema.safeParse(value);
  return parsed.success ? parsed.data : fallback;
}

export function normalizeAppSettings(value: unknown, legacyOnboardingComplete = false): AppSettings {
  const source = record(value);
  const onboardingComplete = validOr(z.boolean(), source.onboardingComplete, false) || legacyOnboardingComplete;
  const jamaah: UserSettings['jamaahTimes'] = {};
  const savedJamaah = record(source.jamaahTimes);
  for (const prayer of ['Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha'] as const) {
    const parsed = clockSchema.safeParse(savedJamaah[prayer]);
    if (parsed.success) jamaah[prayer] = parsed.data;
  }
  const sections = Array.isArray(source.homeSections) ? source.homeSections.map(record) : [];
  const homeSections = defaultAppSettings.homeSections!.map((fallback) => {
    const saved = sections.find((section) => section.id === fallback.id) ?? {};
    return {
      id: fallback.id,
      name: validOr(textSchema, saved.name, fallback.name),
      visible: validOr(z.boolean(), saved.visible, fallback.visible),
      order: validOr(finiteInteger.min(0).max(100), saved.order, fallback.order),
    };
  });
  return {
    method: validOr(methodSchema, source.method, DEFAULT_SETTINGS.method),
    madhab: source.madhab === null && !onboardingComplete ? null
      : validOr(z.union([z.literal(0), z.literal(1)]), source.madhab === 2 ? 0 : source.madhab, DEFAULT_SETTINGS.madhab),
    jamaahTimes: jamaah,
    suhoorAdjustment: validOr(finiteInteger, source.suhoorAdjustment ?? source.sehriAdjustment, DEFAULT_SETTINGS.suhoorAdjustment),
    iftarAdjustment: validOr(finiteInteger, source.iftarAdjustment, DEFAULT_SETTINGS.iftarAdjustment),
    hijriAdjustment: validOr(finiteInteger, source.hijriAdjustment, DEFAULT_SETTINGS.hijriAdjustment),
    hijriDateChangeAtMaghrib: validOr(z.boolean(), source.hijriDateChangeAtMaghrib, DEFAULT_SETTINGS.hijriDateChangeAtMaghrib),
    // Only a genuinely absent value gets initial coordinates. In a saved
    // object, missing/damaged coordinates require repair, not another city.
    latitude: validOr(z.number().finite().min(-90).max(90), source.latitude, value === undefined ? DEFAULT_SETTINGS.latitude! : null),
    longitude: validOr(z.number().finite().min(-180).max(180), source.longitude, value === undefined ? DEFAULT_SETTINGS.longitude! : null),
    city: validOr(textSchema, source.city, DEFAULT_SETTINGS.city!),
    country: validOr(textSchema, source.country, DEFAULT_SETTINGS.country!),
    manualLocation: validOr(z.boolean(), source.manualLocation, DEFAULT_SETTINGS.manualLocation),
    timeFormat: validOr(z.enum(['system', '12h', '24h']), source.timeFormat, DEFAULT_SETTINGS.timeFormat),
    onboardingComplete,
    homeSections,
  };
}

export function normalizeQazaCounts(value: unknown): QazaCounts {
  const source = record(value);
  const counts = { ...INITIAL_COUNTS };
  for (const key of COUNTER_KEYS) counts[key] = validOr(finiteInteger.min(0), source[key], 0);
  return counts;
}
export function normalizeContainerOrder(value: unknown): string[] {
  const order = Array.isArray(value) ? value.filter((id): id is string => typeof id === 'string' && DEFAULT_CONTAINER_ORDER.includes(id)) : [];
  return [...new Set([...order, ...DEFAULT_CONTAINER_ORDER])];
}
export function normalizeVisibility(value: unknown): Record<string, boolean> {
  const source = record(value);
  return Object.fromEntries(DEFAULT_CONTAINER_ORDER.map((id) => [id, validOr(z.boolean(), source[id], true)]));
}

export function normalizeStorageValue<T>(key: string, value: unknown, fallback: T, legacyOnboardingComplete = false): T {
  switch (key) {
    case 'muajjin-settings': return normalizeAppSettings(value, legacyOnboardingComplete) as T;
    case 'muajjin-qaza-tracker': return normalizeQazaCounts(value) as T;
    case 'muajjin-container-order': return normalizeContainerOrder(value) as T;
    case 'muajjin-visible-containers': return normalizeVisibility(value) as T;
    default:
      // The remaining hook consumer persists a nullable release-version string.
      return (value === null || typeof value === 'string') && (fallback === null || typeof fallback === 'string') ? value as T : fallback;
  }
}
