import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { useEffect, useMemo, useState, useSyncExternalStore } from 'react';
import { startClockScheduler } from '@/utils/clock-scheduler';
import type { ClockEnvironment } from '@/utils/clock-scheduler';
import { localDateKey, nextLocalMidnight } from '@/utils/time-utils';

const subscribers = new Set<() => void>();
let nativeActive = true;
let pageActive = true;
let detachLifecycle: (() => void) | undefined;

function attachLifecycle(): () => void {
  let disposed = false;
  let nativeEventVersion = 0;
  const notify = () => { for (const subscriber of subscribers) subscriber(); };
  const show = () => { pageActive = true; notify(); };
  const hide = () => { pageActive = false; notify(); };
  document.addEventListener('visibilitychange', notify);
  window.addEventListener('focus', notify);
  window.addEventListener('pageshow', show);
  window.addEventListener('pagehide', hide);
  // Browsers do not universally expose clock changes; host integrations may.
  window.addEventListener('timechange', notify);
  window.addEventListener('timezonechange', notify);
  const listener = Capacitor.isNativePlatform()
    ? App.addListener('appStateChange', ({ isActive }) => {
        if (disposed) return;
        nativeEventVersion++;
        nativeActive = isActive;
        notify();
      })
    : undefined;
  if (listener) {
    void listener.catch((error) => console.warn('App lifecycle listener unavailable:', error));
    void App.getState().then(({ isActive }) => {
      if (disposed || nativeEventVersion !== 0) return;
      nativeActive = isActive;
      notify();
    }).catch((error) => console.warn('App lifecycle state unavailable:', error));
  }
  return () => {
    disposed = true;
    document.removeEventListener('visibilitychange', notify);
    window.removeEventListener('focus', notify);
    window.removeEventListener('pageshow', show);
    window.removeEventListener('pagehide', hide);
    window.removeEventListener('timechange', notify);
    window.removeEventListener('timezonechange', notify);
    if (listener) void listener.then((handle) => handle.remove()).catch((error) => console.warn('App lifecycle cleanup failed:', error));
    nativeActive = true;
    pageActive = true;
  };
}

const environment: ClockEnvironment<number> = {
  now: Date.now,
  active: () => !document.hidden && nativeActive && pageActive,
  setTimeout: (callback, delay) => window.setTimeout(callback, delay),
  clearTimeout: (handle) => window.clearTimeout(handle),
  subscribe(reconcile) {
    subscribers.add(reconcile);
    if (subscribers.size === 1) detachLifecycle = attachLifecycle();
    return () => {
      subscribers.delete(reconcile);
      if (subscribers.size === 0) {
        detachLifecycle?.();
        detachLifecycle = undefined;
      }
    };
  },
};

/** Share the same native/browser lifecycle gate with sensor consumers. */
export function useForegroundActive(): boolean {
  return useSyncExternalStore(environment.subscribe, environment.active);
}

/** Identity-only consumers omit cadence; seconds stay local to visible displays. */
export function useBoundaryClock(boundaries: readonly number[], cadence?: number): Date {
  const [timestamp, setTimestamp] = useState(Date.now);
  useEffect(() => startClockScheduler(environment, boundaries, cadence, setTimestamp), [boundaries, cadence]);
  return new Date(timestamp);
}

/** Recalculate the whole date input together, only when date/timezone changes. */
export function useLocalDate(): Date {
  const [calendar, setCalendar] = useState(() => {
    const now = new Date();
    return { date: now, key: `${localDateKey(now)}:${Intl.DateTimeFormat().resolvedOptions().timeZone}:${now.getTimezoneOffset()}` };
  });
  const boundaries = useMemo(() => [nextLocalMidnight(calendar.date)], [calendar]);
  useEffect(() => startClockScheduler(environment, boundaries, 60_000, (timestamp) => {
    const date = new Date(timestamp);
    const key = `${localDateKey(date)}:${Intl.DateTimeFormat().resolvedOptions().timeZone}:${date.getTimezoneOffset()}`;
    setCalendar((previous) => previous.key === key ? previous : { date, key });
  }), [boundaries]);
  return calendar.date;
}
