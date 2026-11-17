import { Toaster as Sonner } from '@/components/ui/sonner';
import { UpdateChecker } from '@/features/update-checker';
import { lazy, Suspense, useEffect } from 'react';
import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom';
import { CapacitorApp } from './components/capacitor-app';
import { RouteGuard } from './components/layout/route-guard';
import { AppProvider } from './contexts/app-context';
import HomePage from './pages/home-page';
import HomeDashboardPage from './pages/index';

const NotFound = lazy(() => import('./pages/not-found'));
const FinalSettingsPage = lazy(() => import('./pages/onboarding/final-settings-page'));
const LocationSetupPage = lazy(() => import('./pages/onboarding/location-setup-page'));
const WelcomePage = lazy(() => import('./pages/onboarding/welcome-page'));
const QazaTrackerPage = lazy(() => import('./pages/qaza-tracker-page'));
const QiblaCompassPage = lazy(() => import('./features/qibla-compass/qibla-compass-page'));
const AboutSettings = lazy(() => import('./pages/settings/about-settings'));
const DisplaySettings = lazy(() => import('./pages/settings/display-settings'));
const FastingSettings = lazy(() => import('./pages/settings/fasting-settings'));
const HijriSettings = lazy(() => import('./pages/settings/hijri-settings'));
const PrayerTimesSettings = lazy(() => import('./pages/settings/prayer-times-settings'));
const SettingsHome = lazy(() => import('./pages/settings/settings-home'));
const TimeLocationSettings = lazy(() => import('./pages/settings/time-location-settings'));
const TranslationSettings = lazy(() => import('./pages/settings/translation-settings'));
const UpcomingPrayerTimesPage = lazy(() => import('./features/upcoming-prayer-times/upcoming-prayer-times-page'));

function ScrollToTopOnNavigate() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  }, [pathname]);

  return null;
}

const App = () => (
    <AppProvider>
        <Sonner />
        <BrowserRouter>
          <CapacitorApp>
            <ScrollToTopOnNavigate />
            <UpdateChecker />
            <Suspense fallback={null}>
              <Routes>
                {/* Onboarding routes - blocked after completion */}
                <Route
                  path="/onboarding/welcome"
                  element={
                    <RouteGuard requireOnboardingComplete={false} redirectTo="/">
                      <WelcomePage />
                    </RouteGuard>
                  }
                />
                <Route
                  path="/onboarding/location"
                  element={
                    <RouteGuard requireOnboardingComplete={false} redirectTo="/">
                      <LocationSetupPage />
                    </RouteGuard>
                  }
                />
                <Route
                  path="/onboarding/settings"
                  element={
                    <RouteGuard requireOnboardingComplete={false} redirectTo="/">
                      <FinalSettingsPage />
                    </RouteGuard>
                  }
                />

                {/* Home page routes - require onboarding completion */}
                <Route
                  path="/"
                  element={
                    <RouteGuard
                      requireOnboardingComplete={true}
                      redirectTo="/onboarding/welcome">
                      <HomePage />
                    </RouteGuard>
                  }>
                  <Route index element={<HomeDashboardPage />} />
                </Route>

                <Route
                  path="/upcoming-prayer-times"
                  element={<UpcomingPrayerTimesPage />}
                />
                <Route
                  path="/qibla"
                  element={
                    <RouteGuard
                      requireOnboardingComplete={true}
                      redirectTo="/onboarding/welcome">
                      <QiblaCompassPage />
                    </RouteGuard>
                  }
                />

                {/* Settings routes - require onboarding completion */}
                <Route
                  path="/settings"
                  element={
                    <RouteGuard
                      requireOnboardingComplete={true}
                      redirectTo="/onboarding/welcome">
                      <SettingsHome />
                    </RouteGuard>
                  }
                />
                <Route
                  path="/settings/prayer-times"
                  element={
                    <RouteGuard
                      requireOnboardingComplete={true}
                      redirectTo="/onboarding/welcome">
                      <PrayerTimesSettings />
                    </RouteGuard>
                  }
                />
                <Route
                  path="/settings/fasting"
                  element={
                    <RouteGuard
                      requireOnboardingComplete={true}
                      redirectTo="/onboarding/welcome">
                      <FastingSettings />
                    </RouteGuard>
                  }
                />
                <Route
                  path="/settings/hijri"
                  element={
                    <RouteGuard
                      requireOnboardingComplete={true}
                      redirectTo="/onboarding/welcome">
                      <HijriSettings />
                    </RouteGuard>
                  }
                />
                <Route
                  path="/settings/time-location"
                  element={
                    <RouteGuard
                      requireOnboardingComplete={true}
                      redirectTo="/onboarding/welcome">
                      <TimeLocationSettings />
                    </RouteGuard>
                  }
                />
                <Route
                  path="/settings/display"
                  element={
                    <RouteGuard
                      requireOnboardingComplete={true}
                      redirectTo="/onboarding/welcome">
                      <DisplaySettings />
                    </RouteGuard>
                  }
                />
                <Route
                  path="/settings/translations"
                  element={
                    <RouteGuard
                      requireOnboardingComplete={true}
                      redirectTo="/onboarding/welcome">
                      <TranslationSettings />
                    </RouteGuard>
                  }
                />
                <Route
                  path="/qaza"
                  element={
                    <RouteGuard
                      requireOnboardingComplete={true}
                      redirectTo="/onboarding/welcome">
                      <QazaTrackerPage />
                    </RouteGuard>
                  }
                />

                <Route
                  path="/settings/about"
                  element={
                    <RouteGuard
                      requireOnboardingComplete={true}
                      redirectTo="/onboarding/welcome">
                      <AboutSettings />
                    </RouteGuard>
                  }
                />

                {/* ADD ALL CUSTOM ROUTES ABOVE THE CATCH-ALL "*" ROUTE */}
                <Route path="*" element={<NotFound />} />
              </Routes>
            </Suspense>
          </CapacitorApp>
        </BrowserRouter>
    </AppProvider>
);

export default App;
