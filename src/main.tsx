import { createRoot } from 'react-dom/client';
import App from './app.tsx';
import './index.css';
import { ThemeProvider } from './components/theme-provider';
import { TranslationProvider } from './contexts/translation-context';
import { Capacitor } from '@capacitor/core';

export const Root = () => {
  const isNative = Capacitor.isNativePlatform();

  if (isNative) {
    return (
      <ThemeProvider
        defaultTheme="system"
        storageKey="muajjin-theme"
        attribute="class">
        <TranslationProvider>
          <App />
        </TranslationProvider>
      </ThemeProvider>
    );
  }

  return (
    <ThemeProvider
      defaultTheme="system"
      storageKey="muajjin-theme"
      attribute="class">
      <TranslationProvider>
        <div className="md:flex md:min-h-dvh md:items-center md:justify-center md:bg-muted md:p-4">
          <div className="md:relative md:h-[852px] md:max-h-[95dvh] md:w-[393px] md:overflow-hidden md:rounded-[2.5rem] md:border-[8px] md:border-foreground/15 md:bg-background md:shadow-2xl md:[transform:translateZ(0)]">
            <div className="md:h-full md:overflow-y-auto">
              <App />
            </div>
          </div>
        </div>
      </TranslationProvider>
    </ThemeProvider>
  );
};

createRoot(document.getElementById('root')!).render(<Root />);
