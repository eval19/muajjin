import { LabeledInputField } from '@/components/form/labeled-input-field';
import { LocationAutoDetectButton } from '@/components/location/location-auto-detect-button';
import { LocationStatusAlert } from '@/components/location/location-status-alert';
import { OnboardingPageLayout } from '@/components/onboarding/onboarding-page-layout';
import { OnboardingStepHeader } from '@/components/onboarding/onboarding-step-header';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { ONBOARDING_DEFAULTS } from '@/constants/default-settings';
import { useApp } from '@/contexts/app-context';
import { useTranslation } from '@/contexts/translation-context';
import { useLocationDetection } from '@/hooks/use-location-detection';
import { MapPin } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function LocationSetupPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { updateSettings } = useApp();
  const [location, setLocation] = useState({ city: '', country: '', latitude: '', longitude: '' });
  const { detect, cancel, isDetecting, status } = useLocationDetection((result) => {
    setLocation({ city: result.city, country: result.country, latitude: String(result.latitude), longitude: String(result.longitude) });
  });
  const latitude = location.latitude.trim() === '' ? NaN : Number(location.latitude);
  const longitude = location.longitude.trim() === '' ? NaN : Number(location.longitude);
  const valid = Number.isFinite(latitude) && latitude >= -90 && latitude <= 90 &&
    Number.isFinite(longitude) && longitude >= -180 && longitude <= 180;
  const changeLocation = (key: 'city' | 'latitude' | 'longitude', value: string) => {
    cancel();
    setLocation((previous) => ({ ...previous, [key]: value }));
  };
  const handleContinue = () => {
    if (!valid || !location.city.trim()) return;
    cancel();
    if (updateSettings({
      latitude, longitude, city: location.city.trim(), country: location.country,
      method: ONBOARDING_DEFAULTS.method, madhab: ONBOARDING_DEFAULTS.madhab,
      timeFormat: 'system', jamaahTimes: {}, suhoorAdjustment: 0, iftarAdjustment: 0,
      hijriAdjustment: 0, hijriDateChangeAtMaghrib: true, manualLocation: true,
    })) navigate('/onboarding/settings', { replace: true });
  };

  return (
    <OnboardingPageLayout currentStep={2}>
      <OnboardingStepHeader
        title={t('onboarding.setLocation')}
        description={t('onboarding.setLocationDesc')}
        icon={<MapPin className="h-10 w-10 text-primary" />}
      />

      <Card className="shadow-lg">
        <CardContent className="space-y-4 p-6">
          {/* Form */}
          <div className="space-y-4">
            {/* Location Name */}
            <LabeledInputField
              id="location-name"
              label={t('onboarding.locationName')}
              placeholder={t('onboarding.locationPlaceholder')}
              value={location.city}
              onChange={(value) => changeLocation('city', value)}
              containerClassName="space-y-2"
            />

            <div className="grid grid-cols-2 gap-3">
              {/* Latitude */}
              <LabeledInputField
                id="latitude"
                type="number"
                step="0.0001"
                label={t('onboarding.latitude')}
                placeholder={t('onboarding.latitudePlaceholder')}
                value={location.latitude}
                onChange={(value) => changeLocation('latitude', value)}
                min={-90}
                max={90}
                required
                containerClassName="space-y-2"
              />

              {/* Longitude */}
              <LabeledInputField
                id="longitude"
                type="number"
                step="0.0001"
                label={t('onboarding.longitude')}
                placeholder={t('onboarding.longitudePlaceholder')}
                value={location.longitude}
                onChange={(value) => changeLocation('longitude', value)}
                min={-180}
                max={180}
                required
                containerClassName="space-y-2"
              />
            </div>

            {/* Auto Detect Button */}
            <LocationAutoDetectButton
              variant="secondary"
              isDetecting={isDetecting}
              onClick={detect}
            />

            {/* Status Message */}
            <LocationStatusAlert status={status} mode="onboarding" />
          </div>
        </CardContent>
      </Card>

      {/* Continue Button - outside card for better reachability */}
      <Button
        onClick={handleContinue}
        size="lg"
        className="w-full"
        disabled={!location.city.trim() || !valid}>
        {t('onboarding.continue')}
      </Button>
    </OnboardingPageLayout>
  );
}
