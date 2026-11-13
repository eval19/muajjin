import { CalculationMethodSelect } from '@/components/calculation-method-select';
import { HijriAdjustmentSelector } from '@/components/hijri-adjustment-selector';
import { HijriCalendarInfoCard } from '@/components/hijri-calendar-info-card';
import { MadhabSelector } from '@/components/madhab-selector';
import { OnboardingPageLayout } from '@/components/onboarding/onboarding-page-layout';
import { OnboardingStepHeader } from '@/components/onboarding/onboarding-step-header';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { useApp } from '@/contexts/app-context';
import { useTranslation } from '@/contexts/translation-context';
import {
  openBatteryOptimizationSettings,
  openExactAlarmSettings,
  readWidgetReliability,
} from '@/services/widget-sync-service';
import type { WidgetReliability } from '@/services/widget-sync-service';
import { UserSettings } from '@/types';
import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { CalendarDays, Clock, Scale } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
export default function FinalSettingsPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { settings: appSettings, updateSettings } = useApp();

  const { method, madhab, hijriAdjustment, latitude, longitude } = appSettings;
  const [settings, setSettings] = useState<Pick<UserSettings, 'method' | 'madhab' | 'hijriAdjustment'>>({
    method, madhab, hijriAdjustment,
  });

  useEffect(() => {
    setSettings({ method, madhab, hijriAdjustment });
  }, [method, madhab, hijriAdjustment]);

  const hasValidLocation = typeof latitude === 'number' && Number.isFinite(latitude) && Math.abs(latitude) <= 90 &&
    typeof longitude === 'number' && Number.isFinite(longitude) && Math.abs(longitude) <= 180;
  useEffect(() => {
    if (!hasValidLocation) navigate('/onboarding/location', { replace: true });
  }, [hasValidLocation, navigate]);

  const nativeAndroid =
    Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'android';
  const [reliability, setReliability] = useState<WidgetReliability | null>(null);
  const [reliabilityUnavailable, setReliabilityUnavailable] = useState(false);

  const refreshReliability = useCallback(async () => {
    if (!nativeAndroid) return;
    try {
      setReliability(await readWidgetReliability());
      setReliabilityUnavailable(false);
    } catch {
      setReliabilityUnavailable(true);
    }
  }, [nativeAndroid]);

  useEffect(() => {
    if (!nativeAndroid) return;
    void refreshReliability();
    const onFocus = () => void refreshReliability();
    window.addEventListener('focus', onFocus);
    const listener = App.addListener('appStateChange', ({ isActive }) => {
      if (isActive) void refreshReliability();
    });
    void listener.catch(() => {});
    return () => {
      window.removeEventListener('focus', onFocus);
      void listener.then((handle) => handle.remove()).catch(() => {});
    };
  }, [nativeAndroid, refreshReliability]);

  const checkingExactAlarm =
    nativeAndroid && !reliabilityUnavailable && reliability === null;
  const needsExactAlarm =
    nativeAndroid &&
    !reliabilityUnavailable &&
    reliability !== null &&
    reliability.exactAlarmSupported !== false &&
    !reliability.exactAlarmAllowed;
  const isReadyToFinish =
    settings.madhab !== null &&
    hasValidLocation &&
    !checkingExactAlarm &&
    !needsExactAlarm;
  const handleFinish = () => {
    if (!hasValidLocation) {
      navigate('/onboarding/location', { replace: true });
      return;
    }
    if (settings.madhab === null || checkingExactAlarm || needsExactAlarm) {
      return;
    }
    if (updateSettings({ ...settings, onboardingComplete: true })) navigate('/', { replace: true });
  };

  return (
    <OnboardingPageLayout currentStep={3}>
      <OnboardingStepHeader
        title={t('onboarding.customizing')}
        description={t('onboarding.selectCalculationMethod')}
        icon={<CalendarDays className="h-10 w-10 text-primary" />}
      />

      <Card className="shadow-lg">
        <CardContent className="space-y-6 p-6">
          {/* Calculation Method */}
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-sm font-semibold">
              <CalendarDays className="h-4 w-4 text-primary" />
              <Label>{t('settings.calculationMethod')}</Label>
            </div>
            <CalculationMethodSelect
              value={settings.method}
              onChange={(value) => setSettings({ ...settings, method: value })}
              triggerClassName="w-full bg-card"
            />
          </div>

          {/* Madhab Selection */}
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-sm font-semibold">
              <Scale className="h-4 w-4 text-primary" />
              <Label>{t('settings.madhab')}</Label>
            </div>
            <p className="text-sm text-muted-foreground">
              {t('settings.madhabDesc')}
            </p>
            <MadhabSelector
              value={settings.madhab}
              onChange={(value) => setSettings({ ...settings, madhab: value })}
            />
          </div>

          {/* Hijri Date Adjustment */}
          <HijriAdjustmentSelector
            value={settings.hijriAdjustment}
            onChange={(value) =>
              setSettings((prev) => ({
                ...prev,
                hijriAdjustment: value,
              }))
            }
            showIcon={true}
          />

          {/* Hijri Calendar Info */}
          <HijriCalendarInfoCard />

          {/* Exact Alarm Permission (Android 12+) */}
          {needsExactAlarm && (
            <div className="space-y-3 rounded-lg border border-amber-500/40 bg-amber-500/10 p-4">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <Clock className="h-4 w-4 text-primary" />
                <span>{t('onboarding.exactAlarmTitle')}</span>
              </div>
              <p className="text-sm text-muted-foreground">
                {t('onboarding.exactAlarmDesc')}
              </p>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="w-full"
                onClick={() => {
                  void openExactAlarmSettings()
                    .then(refreshReliability)
                    .catch(() => setReliabilityUnavailable(true));
                }}>
                {t('onboarding.exactAlarmButton')}
              </Button>
            </div>
          )}

          {/* Optional Battery Unrestricted Recommendation (One UI / Android 14+) */}
          {nativeAndroid && reliability !== null && !reliability.batteryUnrestricted && (
            <div className="space-y-3 rounded-lg border border-border bg-muted/40 p-4">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <Clock className="h-4 w-4 text-primary" />
                <span>{t('onboarding.batteryTitle')}</span>
              </div>
              <p className="text-sm text-muted-foreground">
                {t('onboarding.batteryDesc')}
              </p>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="w-full"
                onClick={() => {
                  void openBatteryOptimizationSettings()
                    .then(refreshReliability)
                    .catch(() => {});
                }}>
                {t('onboarding.batteryButton')}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Finish Button - outside card for consistent onboarding layout */}
      <Button
        onClick={handleFinish}
        size="lg"
        className="w-full"
        disabled={!isReadyToFinish}>
        {settings.madhab === null || !hasValidLocation
          ? t('onboarding.madhabRequired')
          : needsExactAlarm
            ? t('onboarding.exactAlarmRequiredAction')
            : t('onboarding.getStarted')}
      </Button>
    </OnboardingPageLayout>
  );
}
