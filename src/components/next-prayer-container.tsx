import { Card, CardContent } from '@/components/ui/card';
import { useTranslation } from '@/contexts/translation-context';
import { PrayerTime } from '@/types';
import { formatTime, getNextSalat, prayerBoundaries } from '@/utils/time-utils';
import { useBoundaryClock } from '@/hooks/use-visible-clock';
import { ChevronRight } from 'lucide-react';
import { useMemo } from 'react';
import type { FC } from 'react';

interface NextPrayerContainerProps {
  allPrayers: PrayerTime[];
  timeFormat?: 'system' | '12h' | '24h';
}

export const NextPrayerContainer: FC<NextPrayerContainerProps> = ({
  allPrayers,
  timeFormat = 'system',
}) => {
  const { t } = useTranslation();
  const boundaries = useMemo(() => prayerBoundaries(allPrayers), [allPrayers]);
  const now = useBoundaryClock(boundaries);
  const nextPrayer = getNextSalat(allPrayers, now);

  if (!nextPrayer) {
    return null;
  }

  const jamaahTime = nextPrayer.jamaah || nextPrayer.start;

  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-3">
          <div className="flex min-w-[6rem] flex-1 items-center gap-1">
            <div className="min-w-0 text-start">
              <p className="text-xs uppercase tracking-wider text-muted-foreground">
                {t('salatTimes.next')}
              </p>
              <h3 className="break-words text-lg font-bold uppercase">
                {nextPrayer.name}
              </h3>
            </div>
            <ChevronRight className="h-4 w-4 flex-shrink-0 text-muted-foreground rtl:rotate-180" />
          </div>
          <div className="ms-auto flex flex-wrap gap-x-3 gap-y-2">
            <div className="text-end">
              <p className="text-xs text-muted-foreground">
                {t('salatTimes.start')}
              </p>
              <p className="text-lg font-bold">
                <bdi dir="ltr" className="tabular-nums">
                  {formatTime(nextPrayer.start, timeFormat, t)}
                </bdi>
              </p>
            </div>
            {nextPrayer.jamaah && (
              <div className="text-end">
                <p className="text-xs text-muted-foreground">
                  {t('salatTimes.jamaah')}
                </p>
                <p className="text-lg font-bold text-primary">
                  <bdi dir="ltr" className="tabular-nums">
                    {formatTime(jamaahTime, timeFormat, t)}
                  </bdi>
                </p>
              </div>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
};
