import umalqura from '@umalqura/core';

const FIRST_HIJRI_YEAR = 1318;
const LAST_HIJRI_YEAR = 1500;
const MILLISECONDS_PER_DAY = 86_400_000;
const FIRST_SUPPORTED_ORDINAL = Date.UTC(1900, 3, 30) / MILLISECONDS_PER_DAY;
const calendar = umalqura.$;

// The exact table starts at 1900-04-30. Numeric month-length flags retain
// their meaning across timezone changes; upstream cached local Dates do not.
const HIJRI_YEAR_STARTS = [FIRST_SUPPORTED_ORDINAL];
for (let year = FIRST_HIJRI_YEAR; year <= LAST_HIJRI_YEAR; year++) {
  HIJRI_YEAR_STARTS.push(
    HIJRI_YEAR_STARTS[year - FIRST_HIJRI_YEAR] + calendar.getDaysInYear(year),
  );
}
const LAST_SUPPORTED_ORDINAL = HIJRI_YEAR_STARTS[HIJRI_YEAR_STARTS.length - 1] - 1;

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
 * @param adjustment - Integer civil-day adjustment
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
  if (!Number.isFinite(dateToConvert.getTime())) {
    throw new RangeError('Invalid Hijri calculation date or adjustment');
  }

  const civilYear = dateToConvert.getFullYear();
  const civilMonth = dateToConvert.getMonth();
  const civilDay = dateToConvert.getDate();
  dateToConvert.setUTCHours(0, 0, 0, 0);
  // Unlike Date.UTC(), setUTCFullYear() does not reinterpret years 0–99.
  const ordinal = dateToConvert.setUTCFullYear(
    civilYear, civilMonth, civilDay,
  ) / MILLISECONDS_PER_DAY;
  if (!Number.isFinite(ordinal) ||
      ordinal < FIRST_SUPPORTED_ORDINAL || ordinal > LAST_SUPPORTED_ORDINAL) {
    throw new RangeError('Date is outside the supported Umm al-Qura calendar');
  }
  let low = FIRST_HIJRI_YEAR;
  let high = LAST_HIJRI_YEAR;
  let yearStart = FIRST_SUPPORTED_ORDINAL;
  while (low <= high) {
    const year = Math.floor((low + high) / 2);
    const startOrdinal = HIJRI_YEAR_STARTS[year - FIRST_HIJRI_YEAR];
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
