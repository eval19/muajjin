import { CurrentPrayerContainer } from '@/components/current-prayer-container';
import { DateTimeContainer } from '@/components/date-time-container';
import { FastingTimesContainer } from '@/components/fasting-times-container';
import { NextPrayerContainer } from '@/components/next-prayer-container';
import { PrayerTimesContainer } from '@/components/prayer-times-container';
import { ProhibitedTimesContainer } from '@/components/prohibited-times-container';
import { DEFAULT_CONTAINER_ORDER } from '@/constants/default-settings';
import { useApp } from '@/contexts/app-context';
import { useTranslation } from '@/contexts/translation-context';
import { useLocalStorage } from '@/hooks/use-local-storage';
import { EContainerType } from '@/types/enums';
import { derivePrayerDay } from '@/utils/time-utils';
import { useLocalDate } from '@/hooks/use-visible-clock';
import { Fragment, useMemo } from 'react';
import type { FC } from 'react';

// Default visible containers - all visible by default
const defaultVisibleContainers: Record<string, boolean> = {
  [EContainerType.DateTime]: true,
  [EContainerType.CurrentSalat]: true,
  [EContainerType.NextSalat]: true,
  [EContainerType.SalatTimes]: true,
  [EContainerType.ProhibitedTimes]: true,
  [EContainerType.SaumTimes]: true,
};

const HomeDashboardPage: FC = () => {
  const { t, mounted } = useTranslation();
  const { settings, bridgeFailed } = useApp();
  const date = useLocalDate();
  const [containerOrder] = useLocalStorage<string[]>(
    'muajjin-container-order',
    DEFAULT_CONTAINER_ORDER,
  );
  const [visibleContainers] = useLocalStorage<Record<string, boolean>>(
    'muajjin-visible-containers',
    defaultVisibleContainers,
  );
  const result = useMemo(() => {
    try {
      return { day: derivePrayerDay(date, settings, t), error: null };
    } catch (error) {
      return { day: null, error };
    }
  }, [date, settings, t]);

  const salatTimes = result.day?.prayers ?? [];
  const prohibitedTimes = result.day?.prohibitedTimes ?? [];

  // Render containers based on user order
  const renderContainers = () => {
    if (!result.day) return null;
    return (
      <Fragment>
        {containerOrder.map((containerId, index) => {
          // Skip rendering if container is not visible
          if (!visibleContainers[containerId]) {
            return null;
          }

          switch (containerId) {
            case EContainerType.DateTime:
              return (
                <div
                  key={containerId}
                  className="duration-500 animate-in fade-in slide-in-from-bottom-4"
                  style={{ animationDelay: `${index * 100}ms` }}>
                  <DateTimeContainer
                    date={date}
                    hijriAdjustment={settings.hijriAdjustment}
                    hijriDateChangeAtMaghrib={settings.hijriDateChangeAtMaghrib}
                    maghribTimestamp={result.day.maghribForHijri}
                    location={{
                      city: settings.city ?? '',
                      country: '',
                    }}
                    timeFormat={settings.timeFormat}
                  />
                </div>
              );
            case EContainerType.CurrentSalat:
              return (
                <div
                  key={containerId}
                  className="duration-500 animate-in fade-in slide-in-from-bottom-4"
                  style={{ animationDelay: `${index * 100}ms` }}>
                  <CurrentPrayerContainer
                    allPrayers={result.day.timeline}
                    timeFormat={settings.timeFormat}
                  />
                </div>
              );
            case EContainerType.NextSalat:
              return (
                <div
                  key={containerId}
                  className="duration-500 animate-in fade-in slide-in-from-bottom-4"
                  style={{ animationDelay: `${index * 100}ms` }}>
                  <NextPrayerContainer
                    allPrayers={result.day.timeline}
                    timeFormat={settings.timeFormat}
                  />
                </div>
              );
            case EContainerType.SalatTimes:
              return (
                <div
                  key={containerId}
                  className="duration-500 animate-in fade-in slide-in-from-bottom-4"
                  style={{ animationDelay: `${index * 100}ms` }}>
                  <PrayerTimesContainer
                    salats={salatTimes}
                    allPrayers={result.day.timeline}
                    timeFormat={settings.timeFormat}
                  />
                </div>
              );
            case EContainerType.ProhibitedTimes:
              return (
                <div
                  key={containerId}
                  className="duration-500 animate-in fade-in slide-in-from-bottom-4"
                  style={{ animationDelay: `${index * 100}ms` }}>
                  <ProhibitedTimesContainer
                    prohibitedTimes={prohibitedTimes}
                    timeline={result.day.prohibitedTimeline}
                    timeFormat={settings.timeFormat}
                  />
                </div>
              );
            case EContainerType.SaumTimes:
              return (
                <div
                  key={containerId}
                  className="duration-500 animate-in fade-in slide-in-from-bottom-4"
                  style={{ animationDelay: `${index * 100}ms` }}>
                  <FastingTimesContainer
                    suhoorTime={result.day.suhoorTime}
                    iftarTime={result.day.iftarTime}
                    events={result.day.fastingEvents}
                    timeFormat={settings.timeFormat}
                  />
                </div>
              );
            default:
              return null;
          }
        })}
      </Fragment>
    );
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Main Content */}
      <div className="mx-auto max-w-md space-y-6 px-5 py-6">
        {bridgeFailed && <div role="alert" className="rounded-lg border border-destructive/50 p-4 text-destructive">{t('errors.widgetSyncFailed')}</div>}
        {!mounted ? (
          <div className="flex h-[60vh] animate-pulse flex-col items-center justify-center">
            <div className="flex flex-col items-center gap-4">
              <div
                className="h-12 w-12 animate-bounce rounded-full bg-secondary"
                style={{ animationDelay: '0ms' }}></div>
              <div
                className="h-4 w-32 rounded bg-secondary"
                style={{ animationDelay: '150ms' }}></div>
              <div
                className="h-3 w-24 rounded bg-secondary"
                style={{ animationDelay: '300ms' }}></div>
            </div>
          </div>
        ) : result.day ? (
          renderContainers()
        ) : (
          <div role="alert" className="rounded-lg border border-destructive/50 p-4 text-destructive">
            {t('errors.prayerCalculationUnavailable')}
          </div>
        )}
      </div>
    </div>
  );
};

export default HomeDashboardPage;
