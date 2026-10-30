import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useTranslation } from '@/contexts/translation-context';
import { StoredTranslation } from '@/types/translation';
import { FileText, Globe } from 'lucide-react';

interface ActiveLanguageSelectProps {
  value: string;
  onValueChange: (value: string) => void;
  importedTranslations: StoredTranslation[];
  sectionClassName?: string;
  labelClassName?: string;
}

export function ActiveLanguageSelect({
  value,
  onValueChange,
  importedTranslations,
  sectionClassName = 'space-y-3',
  labelClassName = 'text-sm font-medium',
}: ActiveLanguageSelectProps) {
  const { t, direction } = useTranslation();

  return (
    <div className={sectionClassName}>
      <Label className={labelClassName}>
        {t('settings.selectActiveLanguage')}
      </Label>
      <Select dir={direction} value={value} onValueChange={onValueChange}>
        <SelectTrigger>
          <SelectValue placeholder={t('settings.selectLanguageOption')} />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="en">
            <div className="flex min-w-0 items-center gap-2">
              <Globe className="h-4 w-4 shrink-0" />
              <span className="min-w-0 [overflow-wrap:anywhere]">
                {t('settings.englishDefault')}
              </span>
            </div>
          </SelectItem>
          <SelectItem value="bn">
            <div className="flex min-w-0 items-center gap-2">
              <Globe className="h-4 w-4 shrink-0" />
              <bdi dir="ltr" className="min-w-0 [overflow-wrap:anywhere]">
                বাংলা (Bangla)
              </bdi>
            </div>
          </SelectItem>
          <SelectItem value="ar">
            <div className="flex min-w-0 items-center gap-2">
              <Globe className="h-4 w-4 shrink-0" />
              <span className="min-w-0 [overflow-wrap:anywhere]">
                <bdi>العربية</bdi> <bdi dir="ltr">(Arabic)</bdi>
              </span>
            </div>
          </SelectItem>
          {importedTranslations.map((translation) => (
            <SelectItem key={translation.id} value={translation.id}>
              <div className="flex min-w-0 items-center gap-2">
                <FileText className="h-4 w-4 shrink-0" />
                <span className="min-w-0 [overflow-wrap:anywhere]">
                  <bdi dir={translation.meta.direction}>{translation.meta.languageName}</bdi>
                  {' '}
                  <bdi dir="ltr" className="text-xs text-muted-foreground">
                    ({translation.meta.direction})
                  </bdi>
                </span>
              </div>
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
