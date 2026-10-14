import umalqura from '@umalqura/core';

export interface HijriDateResult {
  day: string;
  month: {
    number: number;
    en: string;
    ar: string;
  };
  year: string;
  date: string; // DD-MM-YYYY format
}

const HIJRI_MONTHS = [
  { en: 'Muharram', ar: 'محرم' },
  { en: 'Safar', ar: 'صفر' },
  { en: 'Rabi al-Awwal', ar: 'ربيع الأول' },
  { en: 'Rabi al-Thani', ar: 'ربيع الثاني' },
  { en: 'Jumada al-Ula', ar: 'جمادى الأولى' },
  { en: 'Jumada al-Akhirah', ar: 'جمادى الآخرة' },
  { en: 'Rajab', ar: 'رجب' },
  { en: "Sha'ban", ar: 'شعبان' },
  { en: 'Ramadan', ar: 'رمضان' },
  { en: 'Shawwal', ar: 'شوال' },
  { en: "Dhu al-Qi'dah", ar: 'ذو القعدة' },
  { en: 'Dhu al-Hijjah', ar: 'ذو الحجة' },
];

/**
 * Calculate Hijri date from Gregorian date using Umm al-Qura calendar
 * @param gregorianDate - The Gregorian date to convert
 * @param adjustment - Day adjustment (-3 to +3 days for lunar calendar variations)
 * @param maghribTimestamp - Optional actual Maghrib instant (epoch milliseconds)
 * @param changeAtMaghrib - If true, Hijri date changes at Maghrib instead of midnight
 * @returns HijriDateResult with day, month, year, and formatted date
 */
export function calculateHijriDate(
  gregorianDate: Date,
  adjustment: number = 0,
  maghribTimestamp?: number,
  changeAtMaghrib: boolean = false,
): HijriDateResult {
  if (!Number.isFinite(gregorianDate.getTime()) || !Number.isInteger(adjustment)) {
    throw new RangeError('Invalid Hijri calculation date or adjustment');
  }
  const dateToConvert = new Date(gregorianDate);
  const rollover = changeAtMaghrib && Number.isFinite(maghribTimestamp) &&
    gregorianDate.getTime() >= maghribTimestamp ? 1 : 0;
  dateToConvert.setDate(dateToConvert.getDate() + adjustment + rollover);

  // @umalqura/core's Gregorian conversion divides local-midnight epoch
  // differences by 24h. Across DST that can produce fractional Hijri days.
  // Reuse its exact public year/month dataset with civil-date ordinals instead.
  const ordinal = Date.UTC(dateToConvert.getFullYear(), dateToConvert.getMonth(), dateToConvert.getDate()) / 86_400_000;
  const calendar = umalqura.$;
  const first = calendar.hijriToGregorian(1318, 1, 1);
  const last = calendar.hijriToGregorian(1500, 12, 30);
  const firstOrdinal = Date.UTC(first.gy, first.gm, first.gd) / 86_400_000;
  const lastOrdinal = Date.UTC(last.gy, last.gm, last.gd) / 86_400_000;
  if (ordinal < firstOrdinal || ordinal > lastOrdinal) {
    throw new RangeError('Date is outside the supported Umm al-Qura calendar');
  }
  let low = 1318;
  let high = 1500;
  let yearStart = firstOrdinal;
  while (low <= high) {
    const year = Math.floor((low + high) / 2);
    const start = calendar.hijriToGregorian(year, 1, 1);
    const startOrdinal = Date.UTC(start.gy, start.gm, start.gd) / 86_400_000;
    if (startOrdinal <= ordinal) {
      low = year + 1;
      yearStart = startOrdinal;
    } else {
      high = year - 1;
    }
  }
  let day = ordinal - yearStart + 1;
  let month = 1;
  while (day > calendar.getDaysInMonth(high, month)) {
    day -= calendar.getDaysInMonth(high, month);
    month++;
  }
  const hijri = { hd: day, hm: month, hy: high };
  const monthIndex = month - 1;

  return {
    day: String(hijri.hd).padStart(2, '0'),
    month: {
      number: hijri.hm,
      en: HIJRI_MONTHS[monthIndex].en,
      ar: HIJRI_MONTHS[monthIndex].ar,
    },
    year: String(hijri.hy),
    date: `${String(hijri.hd).padStart(2, '0')}-${String(hijri.hm).padStart(2, '0')}-${hijri.hy}`,
  };
}

/**
 * Format Hijri date for display
 * @param hijriData - The Hijri date result
 * @returns Formatted string like "27 Rajab 1447"
 */
export function formatHijriDateLocal(
  hijriData: HijriDateResult | null,
): string {
  if (!hijriData) {
    return 'Loading Hijri date...';
  }

  return `${hijriData.day} ${hijriData.month.en} ${hijriData.year}`;
}

/**
 * Format Hijri date in Arabic
 * @param hijriData - The Hijri date result
 * @returns Formatted Arabic string
 */
export function formatHijriDateArabic(hijriData: HijriDateResult): string {
  return `${hijriData.day} ${hijriData.month.ar} ${hijriData.year} هـ`;
}
