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
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1">
            <div>
              <p className="text-xs uppercase tracking-wider text-muted-foreground">
                {t('salatTimes.next')}
              </p>
              <h3 className="text-lg font-bold uppercase">{nextPrayer.name}</h3>
            </div>
            <ChevronRight className="h-4 w-4 flex-shrink-0 text-muted-foreground rtl:rotate-180" />
          </div>
          <div className="flex gap-4">
            <div className="text-end">
              <p className="text-xs text-muted-foreground">
                {t('salatTimes.start')}
              </p>
              <p className="text-lg font-bold">
                {formatTime(nextPrayer.start, timeFormat, t)}
              </p>
            </div>
            {nextPrayer.jamaah && (
              <div className="text-end">
                <p className="text-xs text-muted-foreground">
                  {t('salatTimes.jamaah')}
                </p>
                <p className="text-lg font-bold text-primary">
                  {formatTime(jamaahTime, timeFormat, t)}
                </p>
              </div>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
};
