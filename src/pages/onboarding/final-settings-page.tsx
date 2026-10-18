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
import { UserSettings } from '@/types';
import { CalendarDays, Scale } from 'lucide-react';
import { useEffect, useState } from 'react';
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

  const isReadyToFinish = settings.madhab !== null && hasValidLocation;
  const handleFinish = () => {
    if (!hasValidLocation) {
      navigate('/onboarding/location', { replace: true });
      return;
    }
    if (settings.madhab === null) return;
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
        </CardContent>
      </Card>

      {/* Finish Button - outside card for consistent onboarding layout */}
      <Button
        onClick={handleFinish}
        size="lg"
        className="w-full"
        disabled={!isReadyToFinish}>
        {!isReadyToFinish
          ? t('onboarding.madhabRequired')
          : t('onboarding.getStarted')}
      </Button>
    </OnboardingPageLayout>
  );
}
