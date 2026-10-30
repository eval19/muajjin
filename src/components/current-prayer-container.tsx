import { Card, CardContent } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { useTranslation } from '@/contexts/translation-context';
import type { PrayerTime } from '@/types';
import {
  formatCountdown,
  formatTime,
  getCurrentSalat,
  getPrayerCountdown,
  prayerBoundaries,
} from '@/utils/time-utils';
import { useBoundaryClock } from '@/hooks/use-visible-clock';
import { Clock } from 'lucide-react';
import { Fragment, useMemo } from 'react';
import type { FC } from 'react';

interface CurrentPrayerContainerProps {
  allPrayers: PrayerTime[];
  timeFormat?: 'system' | '12h' | '24h';
}

export const CurrentPrayerContainer: FC<CurrentPrayerContainerProps> = ({
  allPrayers,
  timeFormat = 'system',
}) => {
  const boundaries = useMemo(() => prayerBoundaries(allPrayers), [allPrayers]);
  const now = useBoundaryClock(boundaries);
  const currentPrayer = getCurrentSalat(allPrayers, now);
  if (!currentPrayer) return null;
  return (
    <CurrentPrayerDisplay
      currentPrayer={currentPrayer}
      timeFormat={timeFormat}
    />
  );
};

/** The seconds clock belongs only to this visible countdown/progress display. */
const CurrentPrayerDisplay = ({
  currentPrayer,
  timeFormat,
}: {
  currentPrayer: PrayerTime;
  timeFormat: 'system' | '12h' | '24h';
}) => {
  const { t } = useTranslation();
  const boundaries = useMemo(
    () =>
      [currentPrayer.endTimestamp + 1, currentPrayer.jamaahTimestamp].filter(
        (value): value is number => Number.isFinite(value),
      ),
    [currentPrayer],
  );
  const now = useBoundaryClock(boundaries, 1000);
  const countdown = getPrayerCountdown(currentPrayer, now);
  const metrics = {
    remainingTime: formatCountdown(countdown.remainingMilliseconds),
    progressPercent: countdown.progressPercent,
    countdownLine: t('salatTimes.remainingToTarget', {
      target: t(
        countdown.target === 'jamaah' ? 'salatTimes.jamaah' : 'salatTimes.end',
      ),
    }),
  };

  return (
    <Card className="relative shadow-lg">
      <CardContent className="space-y-4 p-4 min-[360px]:p-5">
        <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-3">
          <div className="min-w-0 text-start">
            <h2 className="break-words text-3xl font-bold uppercase text-primary">
              {currentPrayer.name}
            </h2>
            <p className="text-sm text-muted-foreground">
              {t('salatTimes.current')}
            </p>
          </div>

          <div className="min-w-0 text-end">
            {!currentPrayer.jamaah ? (
              <div className="gap-2 text-2xl font-bold text-primary">
                <bdi dir="ltr" className="whitespace-nowrap tabular-nums">
                  {currentPrayer.end
                    ? formatTime(currentPrayer.end, timeFormat, t)
                    : '—'}
                </bdi>
                <p className="text-xs text-muted-foreground">
                  {t('salatTimes.end')}
                </p>
              </div>
            ) : (
              <Fragment>
                <div className="flex items-center justify-end gap-2 text-2xl font-bold text-primary">
                  <Clock className="h-5 w-5 shrink-0" />
                  <bdi dir="ltr" className="whitespace-nowrap tabular-nums">
                    {metrics.remainingTime}
                  </bdi>
                </div>
                <p className="text-xs uppercase tracking-wider text-muted-foreground">
                  {metrics.countdownLine}
                </p>
              </Fragment>
            )}
          </div>
        </div>

        {/* Progress Bar */}
        <div className="relative">
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-primary/10">
            <Progress value={metrics.progressPercent} className="h-full" />
          </div>
        </div>

        {/* Times Grid */}
        <div className="grid grid-cols-[repeat(auto-fit,minmax(7rem,1fr))] gap-3 text-center">
          {currentPrayer.jamaah ? (
            <Fragment>
              <div className="min-w-0 rounded-lg border border-primary/20 bg-primary/5 p-2.5">
                <p className="text-xs text-muted-foreground">
                  {t('salatTimes.jamaah')}
                </p>
                <p className="text-xl font-bold">
                  <bdi dir="ltr" className="whitespace-nowrap tabular-nums">
                    {formatTime(currentPrayer.jamaah, timeFormat, t)}
                  </bdi>
                </p>
              </div>

              <div className="min-w-0 rounded-lg border border-primary/20 bg-primary/5 p-2.5">
                <p className="text-xs text-muted-foreground">
                  {t('salatTimes.end')}
                </p>

                <p className="text-xl font-bold">
                  <bdi dir="ltr" className="whitespace-nowrap tabular-nums">
                    {currentPrayer.end
                      ? formatTime(currentPrayer.end, timeFormat, t)
                      : '—'}
                  </bdi>
                </p>
              </div>
            </Fragment>
          ) : (
            <div className="col-span-full">
              <p className="mb-2 flex items-center justify-center gap-2 text-xl font-bold text-primary">
                <Clock className="h-5 w-5 shrink-0" />
                <bdi dir="ltr" className="whitespace-nowrap tabular-nums">
                  {metrics.remainingTime}
                </bdi>
              </p>

              <p className="text-xs uppercase tracking-wider text-muted-foreground">
                {metrics.countdownLine}
              </p>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};
