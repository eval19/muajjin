import { LabeledInputField } from '@/components/form/labeled-input-field';
import { LocationAutoDetectButton } from '@/components/location/location-auto-detect-button';
import { LocationStatusAlert } from '@/components/location/location-status-alert';
import { SettingsPageLayout } from '@/components/settings/settings-page-layout';
import { SettingsSaveButton } from '@/components/settings/settings-save-button';
import { SettingsSection } from '@/components/settings/settings-section';
import { Label } from '@/components/ui/label';
import { SelectionButtonGroup } from '@/components/ui/selection-button-group';
import { useApp } from '@/contexts/app-context';
import { useTranslation } from '@/contexts/translation-context';
import { useLocationDetection } from '@/hooks/use-location-detection';
import type { UserSettings } from '@/types';
import { MapPin } from 'lucide-react';
import { useEffect, useState } from 'react';
import type { FC } from 'react';
import { useNavigate } from 'react-router-dom';

const TimeLocationSettings: FC = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { settings, updateSettings } = useApp();
  const [localSettings, setLocalSettings] = useState<UserSettings>({
    ...settings,
  });
  const [latitudeText, setLatitudeText] = useState(Number.isFinite(settings.latitude) ? String(settings.latitude) : '');
  const [longitudeText, setLongitudeText] = useState(Number.isFinite(settings.longitude) ? String(settings.longitude) : '');
  const { detect, cancel, isDetecting, status } = useLocationDetection((location) => {
    setLocalSettings((previous) => ({ ...previous, latitude: location.latitude, longitude: location.longitude, city: location.city, country: location.country }));
    setLatitudeText(String(location.latitude));
    setLongitudeText(String(location.longitude));
  });
  useEffect(() => {
    cancel();
    setLocalSettings({ ...settings });
    setLatitudeText(Number.isFinite(settings.latitude) ? String(settings.latitude) : '');
    setLongitudeText(Number.isFinite(settings.longitude) ? String(settings.longitude) : '');
  }, [settings, cancel]);
  const latitude = latitudeText.trim() === '' ? NaN : Number(latitudeText);
  const longitude = longitudeText.trim() === '' ? NaN : Number(longitudeText);
  const valid = Number.isFinite(latitude) && latitude >= -90 && latitude <= 90 &&
    Number.isFinite(longitude) && longitude >= -180 && longitude <= 180;
  const handleSave = () => {
    if (!valid) return;
    cancel();
    const country = latitude === localSettings.latitude && longitude === localSettings.longitude ? localSettings.country : '';
    if (updateSettings({ ...localSettings, latitude, longitude, country })) navigate(-1);
  };
  const updateSetting = <K extends keyof UserSettings>(key: K, value: UserSettings[K]) => {
    cancel();
    setLocalSettings((previous) => ({ ...previous, [key]: value }));
  };

  return (
    <SettingsPageLayout
      title={t('settings.timeLocationSettings')}
      contentClassName="max-w-md mx-auto px-5 py-6 space-y-6">
      {/* Time Format */}
      <SettingsSection
        title={t('settings.timeFormat')}
        description={t('settings.timeFormatDesc')}>
        <SelectionButtonGroup
          value={localSettings.timeFormat}
          onChange={(value) => updateSetting('timeFormat', value)}
          options={[
            { value: '24h', label: t('settings.24hour') },
            { value: '12h', label: t('settings.12hour') },
            { value: 'system', label: t('settings.system') },
          ]}
        />
      </SettingsSection>

      {/* Location Section */}
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <MapPin className="h-5 w-5 text-muted-foreground" />
          <Label className="text-base">{t('settings.locationSection')}</Label>
        </div>

        {/* Location Name */}
        <LabeledInputField
          id="location-name"
          label={t('settings.locationNameLabel')}
          placeholder={t('settings.locationNamePlaceholder')}
          value={localSettings.city || ''}
          onChange={(value) => updateSetting('city', value)}
          containerClassName="space-y-1"
        />

        {/* Latitude */}
        <LabeledInputField
          id="latitude"
          type="number"
          step="0.0001"
          label={t('settings.latitudeLabel')}
          placeholder={t('settings.latitudePlaceholder')}
          value={latitudeText}
          onChange={(value) => { cancel(); setLatitudeText(value); }}
          min={-90}
          max={90}
          required
          containerClassName="space-y-1"
        />

        {/* Longitude */}
        <LabeledInputField
          id="longitude"
          type="number"
          step="0.0001"
          label={t('settings.longitudeLabel')}
          placeholder={t('settings.longitudePlaceholder')}
          value={longitudeText}
          onChange={(value) => { cancel(); setLongitudeText(value); }}
          min={-180}
          max={180}
          required
          containerClassName="space-y-1"
        />

        {/* Auto Detect Button */}
        <LocationAutoDetectButton
          isDetecting={isDetecting}
          onClick={detect}
        />

        {/* Status Message */}
        <LocationStatusAlert status={status} mode="settings" />
      </div>

      <SettingsSaveButton onClick={handleSave} disabled={!valid} />
    </SettingsPageLayout>
  );
};

export default TimeLocationSettings;
