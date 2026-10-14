import { Card } from '@/components/ui/card';
import { useTranslation } from '@/contexts/translation-context';
import { PrayerTime } from '@/types';
import { formatTime, getCurrentSalat, prayerBoundaries } from '@/utils/time-utils';
import { useBoundaryClock } from '@/hooks/use-visible-clock';
import { useMemo } from 'react';
import type { FC } from 'react';

interface PrayerTimesContainerProps {
  salats: PrayerTime[];
  allPrayers: PrayerTime[];
  timeFormat?: 'system' | '12h' | '24h';
}

export const PrayerTimesContainer: FC<PrayerTimesContainerProps> = ({
  salats,
  allPrayers,
  timeFormat = 'system',
}) => {
  const { t } = useTranslation();
  const boundaries = useMemo(() => prayerBoundaries(allPrayers), [allPrayers]);
  const now = useBoundaryClock(boundaries);
  const currentPrayer = getCurrentSalat(allPrayers, now);

  return (
    <div>
      <Card className="overflow-hidden">
        {/* Table Header */}
        <div className="grid grid-cols-4 gap-2 bg-muted/30 p-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          <div className="pl-2 text-left">{t('salatTimes.salat')}</div>
          <div className="text-center">{t('salatTimes.start')}</div>
          <div className="text-center">{t('salatTimes.jamaah')}</div>
          <div className="text-center">{t('salatTimes.end')}</div>
        </div>

        {/* Table Rows */}
        <div className="divide-y divide-border">
          {salats.map((salat) => {
            const isCurrent = currentPrayer?.id === salat.id;
            const jamaahTime = salat.jamaah || salat.start;
            return (
              <div
                key={salat.id}
                className={`grid grid-cols-4 gap-2 p-3 ${
                  isCurrent && 'border-l-4 border-l-primary bg-primary/5'
                }`}>
                <p
                  className={`text-left font-bold ${isCurrent && 'text-primary'}`}>
                  {salat.name}
                </p>

                <p className={`text-center ${isCurrent && 'font-medium'}`}>
                  {formatTime(salat.start, timeFormat, t)}
                </p>

                <p className="text-center font-semibold text-primary">
                  {salat.jamaah ? formatTime(jamaahTime, timeFormat, t) : '—'}
                </p>

                <p className="text-center">
                  {salat.end ? formatTime(salat.end, timeFormat, t) : '—'}
                </p>
              </div>
            );
          })}
        </div>
      </Card>
    </div>
  );
};
