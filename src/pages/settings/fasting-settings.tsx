import { NumberAdjustmentControl } from '@/components/settings/number-adjustment-control';
import { SettingsPageLayout } from '@/components/settings/settings-page-layout';
import { SettingsSaveButton } from '@/components/settings/settings-save-button';
import { SettingsSection } from '@/components/settings/settings-section';
import { useTranslation } from '@/contexts/translation-context';
import type { UserSettings } from '@/types';
import { useEffect, useState } from 'react';
import type { FC } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '@/contexts/app-context';

const FastingSettings: FC = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { settings, updateSettings } = useApp();
  const { suhoorAdjustment, iftarAdjustment } = settings;
  const [localSettings, setLocalSettings] = useState<Pick<UserSettings, 'suhoorAdjustment' | 'iftarAdjustment'>>({
    suhoorAdjustment, iftarAdjustment,
  });

  useEffect(() => {
    setLocalSettings({ suhoorAdjustment, iftarAdjustment });
  }, [suhoorAdjustment, iftarAdjustment]);

  const handleSave = () => {
    if (updateSettings(localSettings)) navigate(-1);
  };

  const updateSetting = <K extends keyof typeof localSettings>(
    key: K,
    value: (typeof localSettings)[K],
  ) => {
    setLocalSettings((prev) => ({ ...prev, [key]: value }));
  };

  return (
    <SettingsPageLayout
      title={t('settings.saumSettings')}
      contentClassName="max-w-md mx-auto px-5 py-6 space-y-6">
      {/* Suhoor Adjustment */}
      <SettingsSection
        title={t('settings.suhoorAdjustment')}
        description={t('settings.suhoorAdjustmentDesc')}>
        <NumberAdjustmentControl
          id="suhoor-adjustment"
          label={t('settings.suhoorAdjustment')}
          value={localSettings.suhoorAdjustment || 0}
          min={-10}
          max={10}
          onChange={(value) => updateSetting('suhoorAdjustment', value)}
        />
      </SettingsSection>

      {/* Iftar Adjustment */}
      <SettingsSection
        title={t('settings.iftarAdjustment')}
        description={t('settings.iftarAdjustmentDesc')}>
        <NumberAdjustmentControl
          id="iftar-adjustment"
          label={t('settings.iftarAdjustment')}
          value={localSettings.iftarAdjustment || 0}
          min={-10}
          max={10}
          onChange={(value) => updateSetting('iftarAdjustment', value)}
        />
      </SettingsSection>

      <SettingsSaveButton onClick={handleSave} />
    </SettingsPageLayout>
  );
};

export default FastingSettings;
