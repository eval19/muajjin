import { useMemo } from 'react';
import type { FC } from 'react';
import { useTranslation } from '@/contexts/translation-context';
import { useBoundaryClock } from '@/hooks/use-visible-clock';
import { formatPrayerClock } from '@/services/prayer-times-local';
import {
  formatGregorianDateShort, formatTime, nextLocalMidnight,
} from '@/utils/time-utils';
import { calculateHijriDate } from '@/utils/hijri-utils';
import { MapPin } from 'lucide-react';

interface DateTimeContainerProps {
  hijriAdjustment: number;
  hijriDateChangeAtMaghrib: boolean;
  maghribTimestamp?: number;
  date: Date;
  location?: {
    city: string;
    country: string;
  };
  timeFormat?: 'system' | '12h' | '24h';
}

export const DateTimeContainer: FC<DateTimeContainerProps> = ({
  hijriAdjustment,
  hijriDateChangeAtMaghrib,
  maghribTimestamp,
  date,
  location,
  timeFormat = 'system',
}) => {
  const { t, activeTranslation } = useTranslation();
  const boundaries = useMemo(() => [
    nextLocalMidnight(date), maghribTimestamp,
  ].filter((value): value is number => Number.isFinite(value)), [date, maghribTimestamp]);
  // The visible clock has no seconds; update at minute and date boundaries only.
  const now = useBoundaryClock(boundaries, 60_000);
  const currentTime = formatTime(formatPrayerClock(now), timeFormat, t);
  const year = now.getFullYear();
  const month = now.getMonth();
  const day = now.getDate();
  const afterMaghrib = hijriDateChangeAtMaghrib &&
    maghribTimestamp !== undefined && now.getTime() >= maghribTimestamp;
  const hijri = useMemo(() => {
    try {
      return calculateHijriDate(new Date(year, month, day), hijriAdjustment + (afterMaghrib ? 1 : 0));
    } catch {
      return null;
    }
  }, [year, month, day, hijriAdjustment, afterMaghrib]);
  const monthLabel = hijri ? t(`hijri.months.${hijri.month.number}`) : '';

  return (
    <div className="space-y-3 text-center">
      {/* Large Clock Time */}
      <div className="text-5xl font-bold tracking-tighter text-primary">
        {currentTime}
      </div>

      {/* Date Row */}
      <div className="flex items-center justify-center gap-2 text-sm text-muted-foreground">
        <span>{formatGregorianDateShort(now, activeTranslation?.meta.languageCode)}</span>
        <span className="h-1 w-1 rounded-full bg-muted-foreground" />
        <span>{hijri
          ? `${hijri.day} ${monthLabel} ${hijri.year}`
          : t('errors.hijriDateUnavailable')}</span>
      </div>

      {/* Location Badge */}
      {location && (
        <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-primary">
          <MapPin className="h-3 w-3" />
          {location.city}
        </div>
      )}
    </div>
  );
};
