import { Button } from '@/components/ui/button';
import { useTranslation } from '@/contexts/translation-context';
import { MAX_TRANSLATION_BYTES } from '@/utils/translation-validation';
import { Upload } from 'lucide-react';
import { Fragment, useRef } from 'react';
import type { ChangeEvent, ReactNode } from 'react';
import { toast } from 'sonner';

interface TranslationImportControlProps {
  buttonLabel: string;
  helper?: ReactNode;
  helperPosition?: 'before' | 'after';
  requireIdForSuccess?: boolean;
  onImported?: (id: string) => void;
}

export function TranslationImportControl({
  buttonLabel,
  helper,
  helperPosition = 'after',
  requireIdForSuccess = false,
  onImported,
}: TranslationImportControlProps) {
  const { t, importTranslation } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileImport = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (file.size > MAX_TRANSLATION_BYTES) {
      toast.error(t('settings.importError'), { description: 'Translation file too large (maximum 1 MB).' });
      event.currentTarget.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const json: unknown = JSON.parse(e.target?.result as string);
        const result = importTranslation(json);
        const importSuccess = requireIdForSuccess
          ? result.success && Boolean(result.id)
          : result.success;

        if (importSuccess) {
          if (result.id && onImported) {
            onImported(result.id);
          }
          toast.success(t('settings.importSuccess'));

          if (fileInputRef.current) {
            fileInputRef.current.value = '';
          }
        } else {
          toast.error(result.error || t('settings.importError'));
        }
      } catch (error) {
        toast.error(t('errors.invalidFile'));
      }
    };
    reader.onerror = () => toast.error(t('settings.importError'));

    reader.readAsText(file);
  };

  return (
    <Fragment>
      {helperPosition === 'before' ? helper : null}

      <Button
        variant="outline"
        onClick={() => fileInputRef.current?.click()}
        className="w-full">
        <Upload className="me-2 h-4 w-4" />
        {buttonLabel}
      </Button>
      <input
        ref={fileInputRef}
        type="file"
        accept=".json"
        onChange={handleFileImport}
        className="hidden"
      />

      {helperPosition === 'after' ? helper : null}
    </Fragment>
  );
}
