import { Card, CardContent } from '@/components/ui/card';
import { useTranslation } from '@/contexts/translation-context';
import { formatCountdown, formatTime, getNextFastingEvent } from '@/utils/time-utils';
import type { FastingEvent } from '@/utils/time-utils';
import { useBoundaryClock } from '@/hooks/use-visible-clock';
import { MoonStar } from 'lucide-react';
import { useMemo } from 'react';
import type { FC } from 'react';

interface FastingTimesContainerProps {
  suhoorTime: string;
  iftarTime: string;
  events: FastingEvent[];
  timeFormat?: 'system' | '12h' | '24h';
}

export const FastingTimesContainer: FC<FastingTimesContainerProps> = ({
  suhoorTime,
  iftarTime,
  events,
  timeFormat = 'system',
}) => {
  const { t } = useTranslation();
  const boundaries = useMemo(() => events.map((event) => event.timestamp), [events]);
  const now = useBoundaryClock(boundaries, 1000);
  const nextEvent = getNextFastingEvent(events, now);
  const countdown = nextEvent ? formatCountdown(nextEvent.timestamp - now.getTime()) : '--:--:--';
  const nextEventName = nextEvent
    ? t(nextEvent.id === 'suhoor' ? 'saum.suhoorEndsIn' : 'saum.iftarStartsIn')
    : t('saum.nextMeal');

  return (
    <Card>
      <CardContent className="space-y-3 p-4">
        <div className="flex items-center gap-2">
          <MoonStar className="h-5 w-5 text-primary" />
          <h3 className="font-bold">{t('saum.title')}</h3>
        </div>
        <div className="grid grid-cols-3 divide-x divide-border rtl:divide-x-reverse">
          <div className="px-2 text-center">
            <p className="text-xs text-muted-foreground">
              {t('saum.suhoorEnds')}
            </p>
            <p className="text-lg font-bold">
              {formatTime(suhoorTime, timeFormat, t)}
            </p>
          </div>
          <div className="px-2 text-center">
            <p className="text-xs text-muted-foreground">
              {t('saum.iftarStarts')}
            </p>
            <p className="text-lg font-bold">
              {formatTime(iftarTime, timeFormat, t)}
            </p>
          </div>
          <div className="px-2 text-center">
            <p className="text-xs text-muted-foreground">
              {nextEventName || t('saum.nextMeal')}
            </p>
            <p className="font-mono text-lg font-bold text-primary">
              {countdown}
            </p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};
