import { Card, CardContent } from '@/components/ui/card';
import { useTranslation } from '@/contexts/translation-context';
import type { ProhibitedTime } from '@/types';
import { formatTime, isWithinWindow, prayerBoundaries } from '@/utils/time-utils';
import { useBoundaryClock } from '@/hooks/use-visible-clock';
import { AlertTriangle, Sun, Sunrise, Sunset } from 'lucide-react';
import { useMemo } from 'react';
import type { FC } from 'react';

interface ProhibitedTimesContainerProps {
  prohibitedTimes: ProhibitedTime[];
  timeline: ProhibitedTime[];
  timeFormat?: 'system' | '12h' | '24h';
}

export const ProhibitedTimesContainer: FC<ProhibitedTimesContainerProps> = ({
  prohibitedTimes,
  timeline,
  timeFormat = 'system',
}) => {
  const { t } = useTranslation();
  const boundaries = useMemo(() => prayerBoundaries(timeline), [timeline]);
  const now = useBoundaryClock(boundaries);

  const getTimeIcon = (id: ProhibitedTime['id']) => {
    switch (id) {
      case 'shuruq':
        return Sunrise;
      case 'ghurub':
        return Sunset;
      case 'zawal':
      default:
        return Sun;
    }
  };

  return (
    <Card>
      <CardContent className="space-y-3 p-4">
        <div className="flex items-center gap-2">
          <AlertTriangle className="h-5 w-5 text-orange-500" />
          <h3 className="font-bold">{t('prohibited.title')}</h3>
        </div>
        <div className="space-y-2">
          {prohibitedTimes.map((display) => {
            const current = timeline.find((candidate) => candidate.id === display.id && isWithinWindow(candidate, now));
            const time = current ?? display;
            const isCurrent = current !== undefined;
            const TimeIcon = getTimeIcon(time.id);
            return (
              <div
                key={time.id}
                className={[
                  'flex items-center justify-between rounded-lg border p-3 transition-colors',
                  isCurrent
                    ? 'animate-pulse border-red-500/60 bg-red-500/10'
                    : 'border-border bg-muted/20',
                ].join(' ')}>
                <div className="flex items-center gap-2">
                  <TimeIcon
                    className={[
                      'h-4 w-4',
                      isCurrent ? 'text-red-500' : 'text-orange-500',
                    ].join(' ')}
                  />
                  <span
                    className={[
                      'text-sm font-semibold',
                      isCurrent ? 'text-red-600' : '',
                    ].join(' ')}>
                    {time.name}
                  </span>
                </div>
                <span className="text-sm font-bold">
                  {formatTime(time.start, timeFormat, t)} →{' '}
                  {formatTime(time.end, timeFormat, t)}
                </span>
              </div>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
};
