import { createContext, useContext, useCallback, useEffect, useState } from 'react';
import type { FC, ReactNode } from 'react';
import { App as NativeApp } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { readNativeSystemTimeFormat } from '@/services/widget-sync-service';
import { getSystemTimeFormat, setSystemTimeFormat } from '@/utils/time-utils';
import { useLocalStorage } from '@/hooks/use-local-storage';
import { defaultAppSettings } from '@/utils/settings-storage';
import type { AppSettings } from '@/utils/settings-storage';

export type { AppSettings, HomeSection } from '@/utils/settings-storage';

interface AppContextType {
  settings: AppSettings;
  updateSettings: (newSettings: Partial<AppSettings>) => boolean;
  resetSettings: () => boolean;
  storageError: string | null;
  systemTimeFormat: '12h' | '24h';
  bridgeFailed: boolean;
  setBridgeFailed: (failed: boolean) => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: FC<{ children: ReactNode }> = ({ children }) => {
  const [settings, setSettings, status] = useLocalStorage<AppSettings>('muajjin-settings', defaultAppSettings);
  const [systemTimeFormat, publishSystemTimeFormat] = useState(getSystemTimeFormat);
  const [bridgeFailed, setBridgeFailed] = useState(false);
  useEffect(() => {
    let disposed = false;
    let revision = 0;
    const native = Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'android';
    const reconcile = async () => {
      const intent = ++revision;
      try {
        const format = native ? await readNativeSystemTimeFormat()
          : new Intl.DateTimeFormat(undefined, { hour: 'numeric' }).resolvedOptions().hour12 ? '12h' : '24h';
        if (disposed || intent !== revision) return;
        setSystemTimeFormat(format);
        publishSystemTimeFormat(format);
      } catch { console.warn('System clock preference unavailable; explicit 12h/24h choices remain available'); }
    };
    void reconcile();
    window.addEventListener('focus', reconcile);
    const listener = native ? NativeApp.addListener('appStateChange', ({ isActive }) => { if (isActive) void reconcile(); }) : undefined;
    if (listener) void listener.catch(() => console.warn('System clock lifecycle listener unavailable'));
    return () => {
      disposed = true;
      window.removeEventListener('focus', reconcile);
      if (listener) void listener.then((handle) => handle.remove()).catch(() => console.warn('System clock lifecycle cleanup failed'));
    };
  }, []);
  const reset = status.reset;
  const updateSettings = useCallback((newSettings: Partial<AppSettings>) =>
    setSettings((previous) => {
      if (newSettings.latitude !== undefined && newSettings.latitude !== previous.latitude &&
          (typeof newSettings.latitude !== 'number' || !Number.isFinite(newSettings.latitude) || Math.abs(newSettings.latitude) > 90)) {
        throw new RangeError('Enter a valid latitude (-90 to 90)');
      }
      if (newSettings.longitude !== undefined && newSettings.longitude !== previous.longitude &&
          (typeof newSettings.longitude !== 'number' || !Number.isFinite(newSettings.longitude) || Math.abs(newSettings.longitude) > 180)) {
        throw new RangeError('Enter a valid longitude (-180 to 180)');
      }
      const next = {
        ...previous, ...newSettings,
        jamaahTimes: newSettings.jamaahTimes ? { ...previous.jamaahTimes, ...newSettings.jamaahTimes } : previous.jamaahTimes,
      };
      if (next.onboardingComplete && next.madhab !== 0 && next.madhab !== 1) {
        throw new RangeError('Choose a madhab before completing setup');
      }
      if (newSettings.onboardingComplete === true &&
          (typeof next.latitude !== 'number' || !Number.isFinite(next.latitude) || Math.abs(next.latitude) > 90 ||
           typeof next.longitude !== 'number' || !Number.isFinite(next.longitude) || Math.abs(next.longitude) > 180)) {
        throw new RangeError('Choose a valid location before completing setup');
      }
      return next;
    }), [setSettings]);
  const resetSettings = useCallback(() => reset(['muajjin-onboarding-completed']), [reset]);
  return (
    <AppContext.Provider value={{ settings, updateSettings, resetSettings, storageError: status.error, systemTimeFormat, bridgeFailed, setBridgeFailed }}>
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (context === undefined) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
