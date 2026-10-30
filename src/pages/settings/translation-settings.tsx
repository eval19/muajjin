import { SettingsPageLayout } from '@/components/settings/settings-page-layout';
import { SettingsSection } from '@/components/settings/settings-section';
import { ActiveLanguageSelect } from '@/components/translation/active-language-select';
import { TranslationImportControl } from '@/components/translation/translation-import-control';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { useTranslation } from '@/contexts/translation-context';
import { ArrowUpRight, FileText, Trash2 } from 'lucide-react';
import type { FC } from 'react';
import { toast } from 'sonner';

const TranslationSettings: FC = () => {
  const {
    t,
    activeTranslation,
    setActiveTranslation,
    importedTranslations,
    deleteTranslation,
  } = useTranslation();

  const handleDelete = (id: string, languageName: string) => {
    if (
      confirm(
        t('settings.deleteConfirm').replace(
          '"{{languageName}}"',
          `"${languageName}"`,
        ),
      )
    ) {
      if (deleteTranslation(id)) toast.success(t('settings.translationDeleted'));
    }
  };

  const importedTranslationsList = Object.values(importedTranslations);

  return (
    <SettingsPageLayout
      title={t('settings.translations')}
      contentClassName="max-w-md mx-auto px-5 py-6 space-y-6">
      {/* Import Section */}
      <SettingsSection
        title={
          <span className="text-base font-semibold">
            {t('settings.importTranslation')}
          </span>
        }>
        <TranslationImportControl
          buttonLabel={t('settings.importFile')}
          helper={
            <a
              href="https://github.com/MuslimTechnician/muajjin/discussions/1"
              target="_blank"
              rel="noopener noreferrer"
              className="block text-sm text-primary hover:underline">
              {t('settings.importDescriptionWithLearnMore')}{' '}
              <ArrowUpRight className="inline h-4 w-4 rtl:-scale-x-100" />
            </a>
          }
          helperPosition="before"
        />
      </SettingsSection>

      {/* Active Language Selection */}
      <ActiveLanguageSelect
        value={activeTranslation?.id || 'en'}
        importedTranslations={importedTranslationsList}
        onValueChange={(value) =>
          setActiveTranslation(value === 'en' ? null : value)
        }
        sectionClassName="space-y-3"
        labelClassName="text-base font-semibold"
      />

      {/* Imported Translations List */}
      {importedTranslationsList.length > 0 && (
        <div className="space-y-3">
          <Label className="text-base font-semibold">
            {t('settings.importedTranslations')}
          </Label>
          <div className="space-y-2">
            {importedTranslationsList.map((translation) => (
              <Card key={translation.id}>
                <CardContent className="p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1 [overflow-wrap:anywhere]">
                      <div className="flex items-start gap-2">
                        <FileText className="mt-1 h-4 w-4 shrink-0 text-muted-foreground" />
                        <p className="min-w-0 font-medium">
                          <bdi>{translation.meta.languageName}</bdi>
                        </p>
                      </div>
                      <div className="mt-1 flex flex-wrap gap-x-2 text-xs text-muted-foreground">
                        <span>
                          {t('settings.code')}:{' '}
                          <bdi>{translation.meta.languageCode}</bdi>
                        </span>
                        <span aria-hidden>•</span>
                        <span>
                          {t('settings.direction')}:{' '}
                          <bdi>{translation.meta.direction.toUpperCase()}</bdi>
                        </span>
                        <span aria-hidden>•</span>
                        <span>
                          {t('settings.version')}:{' '}
                          <bdi>{translation.meta.version}</bdi>
                        </span>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        {t('settings.imported')}:{' '}
                        <bdi>{new Date(translation.importedAt).toLocaleDateString()}</bdi>
                      </p>
                    </div>
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label={t('settings.deleteTranslation', { languageName: translation.meta.languageName })}
                      onClick={() =>
                        handleDelete(
                          translation.id,
                          translation.meta.languageName,
                        )
                      }
                      className="h-8 w-8 shrink-0 text-destructive">
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}
    </SettingsPageLayout>
  );
};

export default TranslationSettings;
