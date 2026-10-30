import { Card } from '@/components/ui/card';
import { useTranslation } from '@/contexts/translation-context';
import { PrayerTime } from '@/types';
import {
  formatTime,
  getCurrentSalat,
  prayerBoundaries,
} from '@/utils/time-utils';
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
        <div className="overflow-x-auto">
          <div className="min-w-[17rem]">
            {/* Table Header */}
            <div className="grid grid-cols-[minmax(2.75rem,0.9fr)_repeat(3,minmax(0,1fr))] items-center gap-1 bg-muted/30 px-2.5 py-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              <div className="text-start">{t('salatTimes.salat')}</div>
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
                    className={`grid grid-cols-[minmax(2.75rem,0.9fr)_repeat(3,minmax(0,1fr))] items-center gap-1 px-2.5 py-3 text-sm ${
                      isCurrent &&
                      'border-s-4 border-s-primary bg-primary/5 ps-1.5'
                    }`}>
                    <p
                      className={`min-w-0 break-words text-start font-bold ${isCurrent && 'text-primary'}`}>
                      {salat.name}
                    </p>

                    <p
                      className={`whitespace-nowrap text-center ${isCurrent && 'font-medium'}`}>
                      <bdi dir="ltr" className="tabular-nums">
                        {formatTime(salat.start, timeFormat, t)}
                      </bdi>
                    </p>

                    <p className="whitespace-nowrap text-center font-semibold text-primary">
                      <bdi dir="ltr" className="tabular-nums">
                        {salat.jamaah
                          ? formatTime(jamaahTime, timeFormat, t)
                          : '—'}
                      </bdi>
                    </p>

                    <p className="whitespace-nowrap text-center">
                      <bdi dir="ltr" className="tabular-nums">
                        {salat.end ? formatTime(salat.end, timeFormat, t) : '—'}
                      </bdi>
                    </p>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};
