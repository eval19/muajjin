import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { StatusBar, Style } from '@capacitor/status-bar';
import { EdgeToEdge } from '@capawesome/capacitor-android-edge-to-edge-support';
import { useTheme } from 'next-themes';
import { useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { useApp } from '@/contexts/app-context';
import { useTranslation } from '@/contexts/translation-context';
import { useForegroundActive } from '@/hooks/use-visible-clock';
import {
  buildWidgetPresentation,
  syncPrayerTimesToWidget,
} from '@/services/widget-sync-service';

function useWidgetSync() {
  const { settings, setBridgeFailed } = useApp();
  const { t, activeTranslation, direction } = useTranslation();
  const foreground = useForegroundActive();
  const [retryTrigger, setRetryTrigger] = useState(0);
  const lastFailedRef = useRef(false);
  const wasForegroundRef = useRef(foreground);

  useEffect(() => {
    if (foreground && !wasForegroundRef.current && lastFailedRef.current) {
      setRetryTrigger((prev) => prev + 1);
    }
    wasForegroundRef.current = foreground;
  }, [foreground]);

  const presentation = useMemo(
    () =>
      buildWidgetPresentation(
        settings,
        t,
        activeTranslation?.meta.languageCode ?? 'en',
        direction,
      ),
    [settings, t, activeTranslation, direction],
  );

  useEffect(() => {
    let live = true;
    void syncPrayerTimesToWidget({ settings, presentation })
      .then(() => {
        if (!live) return;
        lastFailedRef.current = false;
        setBridgeFailed(false);
      })
      .catch(() => {
        if (!live) return;
        lastFailedRef.current = true;
        setBridgeFailed(true);
      });
    return () => {
      live = false;
    };
  }, [settings, presentation, retryTrigger, setBridgeFailed]);
}

export function CapacitorApp({ children }: { children: ReactNode }) {
  useWidgetSync();
  const { theme, resolvedTheme } = useTheme();

  // Handle status bar color based on theme (Android only)
  useEffect(() => {
    const updateStatusBarColor = async () => {
      // Only run on native Android
      if (
        !Capacitor.isNativePlatform() ||
        Capacitor.getPlatform() !== 'android'
      ) {
        return;
      }

      // Determine current theme (resolves "system" to "light" or "dark")
      const currentTheme = theme === 'system' ? resolvedTheme : theme;

      // Set status bar color based on theme
      const color = currentTheme === 'dark' ? '#1d283a' : '#ffffff';
      const style = currentTheme === 'dark' ? Style.Dark : Style.Light;

      try {
        await EdgeToEdge.setBackgroundColor({ color });
        await StatusBar.setStyle({ style });
      } catch (error) {
        console.error('Failed to set status bar color/style:', error);
      }
    };

    updateStatusBarColor();
  }, [theme, resolvedTheme]);

  useEffect(() => {
    if (!Capacitor.isNativePlatform()) return;
    let disposed = false;
    const handler = App.addListener('backButton', ({ canGoBack }) => {
      if (disposed) return;
      // Let the open dialog/select consume Back before navigating or exiting.
      const escape = new KeyboardEvent('keydown', {
        key: 'Escape',
        bubbles: true,
        cancelable: true,
      });
      if (!(document.activeElement ?? document).dispatchEvent(escape)) return;
      if (canGoBack && window.location.pathname !== '/') window.history.back();
      else void App.exitApp();
    });
    void handler.catch((error) => console.warn('Android back-button listener failed:', error));
    return () => {
      disposed = true;
      void handler.then((listener) => listener.remove())
        .catch((error) => console.warn('Android back-button cleanup failed:', error));
    };
  }, []);

  return children;
}
