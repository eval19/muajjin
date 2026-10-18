import { CalculationMethodSelect } from '@/components/calculation-method-select';
import { MadhabSelector } from '@/components/madhab-selector';
import { JamaahTimesFields } from '@/components/settings/jamaah-times-fields';
import { SettingsPageLayout } from '@/components/settings/settings-page-layout';
import { SettingsSaveButton } from '@/components/settings/settings-save-button';
import { SettingsSection } from '@/components/settings/settings-section';
import { useApp } from '@/contexts/app-context';
import { useTranslation } from '@/contexts/translation-context';
import type { UserSettings } from '@/types';
import { useEffect, useState } from 'react';
import type { FC } from 'react';
import { useNavigate } from 'react-router-dom';

const PrayerTimesSettings: FC = () => {
  const navigate = useNavigate();
  const { t, getSalatName } = useTranslation();
  const { settings, updateSettings } = useApp();
  const { method, madhab, jamaahTimes: { Fajr, Dhuhr, Asr, Maghrib, Isha } } = settings;
  const [localSettings, setLocalSettings] = useState<Pick<UserSettings, 'method' | 'madhab' | 'jamaahTimes'>>({
    method, madhab, jamaahTimes: { Fajr, Dhuhr, Asr, Maghrib, Isha },
  });

  useEffect(() => {
    setLocalSettings({ method, madhab, jamaahTimes: { Fajr, Dhuhr, Asr, Maghrib, Isha } });
  }, [method, madhab, Fajr, Dhuhr, Asr, Maghrib, Isha]);

  const handleSave = () => {
    if (updateSettings(localSettings)) navigate(-1);
  };

  const updateSetting = <K extends keyof typeof localSettings>(
    key: K,
    value: (typeof localSettings)[K],
  ) => {
    setLocalSettings((prev) => ({ ...prev, [key]: value }));
  };

  const handleJamaahTimeChange = (
    prayer: keyof UserSettings['jamaahTimes'],
    value: string,
  ) => {
    setLocalSettings({
      ...localSettings,
      jamaahTimes: {
        ...localSettings.jamaahTimes,
        [prayer]: value,
      },
    });
  };

  return (
    <SettingsPageLayout
      title={t('settings.salatTimesSettings')}
      contentClassName="max-w-md mx-auto px-5 py-6 space-y-6">
      {/* Calculation Method */}
      <SettingsSection
        title={t('settings.calculationMethod')}
        description={t('settings.calculationMethodDesc')}>
        <CalculationMethodSelect
          value={localSettings.method}
          onChange={(value) => updateSetting('method', value)}
          triggerClassName="bg-card"
        />
      </SettingsSection>

      {/* Madhab */}
      <SettingsSection
        title={t('settings.madhab')}
        description={t('settings.madhabDesc')}>
        <MadhabSelector
          value={localSettings.madhab}
          onChange={(value) => updateSetting('madhab', value)}
        />
      </SettingsSection>

      {/* Jama'ah Times */}
      <SettingsSection
        title={t('settings.jamaahTimes')}
        description={t('settings.jamaahTimesDesc')}>
        <JamaahTimesFields
          values={localSettings.jamaahTimes}
          onChange={handleJamaahTimeChange}
          getPrayerLabel={getSalatName}
          containerClassName="space-y-3"
          itemClassName="space-y-1"
        />
      </SettingsSection>

      <SettingsSaveButton onClick={handleSave} />
    </SettingsPageLayout>
  );
};

export default PrayerTimesSettings;
