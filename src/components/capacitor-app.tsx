import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { StatusBar, Style } from '@capacitor/status-bar';
import { EdgeToEdge } from '@capawesome/capacitor-android-edge-to-edge-support';
import { useTheme } from 'next-themes';
import { useEffect } from 'react';
import type { ReactNode } from 'react';

export function CapacitorApp({ children }: { children: ReactNode }) {
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
