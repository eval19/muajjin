import { BottomNav } from '@/components/bottom-nav';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import { useApp } from '@/contexts/app-context';
import { useTranslation } from '@/contexts/translation-context';
import { calculatePrayerInstants } from '@/services/prayer-times-local';
import type { PrayerInstants } from '@/services/prayer-times-local';
import type { PrayerTime } from '@/types';
import { derivePrayerDay, formatTime, localDateKey } from '@/utils/time-utils';
import { useMemo, useState } from 'react';
import type { FC } from 'react';


type UpcomingDay = {
  dateKey: string;
  dateLabel: string;
  prayers: PrayerTime[];
  unavailable: boolean;
};

const DATE_PICKER_MIN = '1900-01-01';
const DATE_PICKER_MAX = '2100-12-31';


const parseInputDate = (value: string): Date => {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
};


const UpcomingPrayerTimesPage: FC = () => {
  const { settings } = useApp();
  const { t, activeTranslation } = useTranslation();
  const language = (activeTranslation?.meta.languageCode ?? 'en').replace(/_/g, '-');

  const [startDateValue, setStartDateValue] = useState(() =>
    localDateKey(new Date()),
  );
  const [singleDayOnly, setSingleDayOnly] = useState(false);
  const [monthOffset, setMonthOffset] = useState(0);

  const viewedMonthLabel = useMemo(() => {
    const selectedDate = parseInputDate(startDateValue);
    const viewedMonthDate = new Date(
      selectedDate.getFullYear(),
      selectedDate.getMonth() + monthOffset,
      1,
    );

    return viewedMonthDate.toLocaleDateString(language, {
      month: 'long',
      year: 'numeric',
    });
  }, [monthOffset, startDateValue, language]);

  const upcomingDays = useMemo<UpcomingDay[]>(() => {
    const selectedDate = parseInputDate(startDateValue);
    selectedDate.setHours(0, 0, 0, 0);

    const datesToRender: Date[] = [];

    if (singleDayOnly) {
      datesToRender.push(new Date(selectedDate));
    } else {
      const viewedMonthDate = new Date(
        selectedDate.getFullYear(),
        selectedDate.getMonth() + monthOffset,
        1,
      );
      const year = viewedMonthDate.getFullYear();
      const month = viewedMonthDate.getMonth();
      const daysInMonth = new Date(year, month + 1, 0).getDate();

      for (let day = 1; day <= daysInMonth; day++) {
        datesToRender.push(new Date(year, month, day));
      }
    }

    const schedules = new Map<string, PrayerInstants>();
    const instantsForDate = (date: Date) => {
      const key = localDateKey(date);
      const cached = schedules.get(key);
      if (cached) return cached;
      const instants = calculatePrayerInstants(date, settings);
      schedules.set(key, instants);
      return instants;
    };
    return datesToRender.map((date) => {
      let prayers: PrayerTime[] = [];
      let unavailable = false;
      try {
        prayers = derivePrayerDay(date, settings, t, instantsForDate).prayers;
      } catch {
        unavailable = true;
      }

      return {
        dateKey: localDateKey(date),
        dateLabel: date.toLocaleDateString(language, {
          weekday: 'short',
          day: 'numeric',
          month: 'short',
          year: 'numeric',
        }),
        prayers,
        unavailable,
      };
    });
  }, [t, language, monthOffset, settings, singleDayOnly, startDateValue]);

  return (
    <div className="min-h-screen bg-background pb-[var(--app-bottom-space)]">
      <div className="mx-auto max-w-md space-y-4 px-5 py-6">
        <h1 className="text-2xl font-bold tracking-tight">
          {t('salatTimes.upcomingTitle')}
        </h1>

        <Card className="p-4">
          <div className="flex flex-wrap items-end gap-3">
            <div className="min-w-0 flex-1 basis-44">
              <label htmlFor="upcoming-date" className="mb-1 block text-sm font-medium">
                {t('salatTimes.datePickerLabel')}
              </label>
              <div className="relative">
                <Input
                  id="upcoming-date"
                  type="date"
                  value={startDateValue}
                  min={DATE_PICKER_MIN}
                  max={DATE_PICKER_MAX}
                  className="h-11 min-w-0 max-w-full"
                  onChange={(e) => {
                    if (!e.target.value || !e.target.validity.valid) return;
                    setStartDateValue(e.target.value);
                    setMonthOffset(0);
                  }}
                />
              </div>
            </div>

            <div className="flex min-w-0 flex-1 basis-20 flex-col items-center gap-1">
              <p className="text-center text-xs font-medium text-muted-foreground">
                {t('salatTimes.singleDayCompact')}
              </p>
              <Switch
                aria-label={t('salatTimes.singleDayCompact')}
                checked={singleDayOnly}
                onCheckedChange={setSingleDayOnly}
              />
            </div>
          </div>
        </Card>

        <p className="text-sm text-muted-foreground">
          {singleDayOnly
            ? t('salatTimes.singleDaySubtitle')
            : t('salatTimes.monthlySubtitle', { month: viewedMonthLabel })}
        </p>

        {!singleDayOnly && (
          <div className="grid grid-cols-2 gap-2">
            <Button
              variant="outline"
              onClick={() => setMonthOffset((prev) => prev - 1)}>
              {t('salatTimes.previousMonth')}
            </Button>
            <Button
              variant="outline"
              onClick={() => setMonthOffset((prev) => prev + 1)}>
              {t('salatTimes.nextMonth')}
            </Button>
          </div>
        )}

        <div className="space-y-3">
          {upcomingDays.map((day) => (
            <Card key={day.dateKey} className="overflow-x-auto">
              <div className="border-b bg-muted/30 px-4 py-2 text-sm font-semibold text-muted-foreground">
                <bdi>{day.dateLabel}</bdi>
              </div>

              {day.unavailable && (
                <p role="alert" className="p-4 text-sm text-destructive">
                  {t('errors.prayerCalculationUnavailable')}
                </p>
              )}
              <div className="grid min-w-[18rem] grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)_minmax(0,1fr)] gap-2 border-b bg-muted/20 px-3 py-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                <div className="text-start">{t('salatTimes.salat')}</div>
                <div className="text-end">{t('salatTimes.start')}</div>
                <div className="text-end">{t('salatTimes.end')}</div>
              </div>

              <div className="divide-y divide-border">
                {day.prayers.map((prayer) => (
                  <div
                    key={prayer.id}
                    className="grid min-w-[18rem] grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)_minmax(0,1fr)] items-center gap-2 px-3 py-2">
                    <span className="min-w-0 break-words text-start font-medium">{prayer.name}</span>
                    <span className="text-end tabular-nums">
                      <bdi dir="ltr">{formatTime(prayer.start, settings.timeFormat, t)}</bdi>
                    </span>
                    <span className="text-end tabular-nums">
                      <bdi dir="ltr">{formatTime(prayer.end, settings.timeFormat, t)}</bdi>
                    </span>
                  </div>
                ))}
              </div>
            </Card>
          ))}
        </div>
      </div>
      <BottomNav />
    </div>
  );
};

export default UpcomingPrayerTimesPage;
