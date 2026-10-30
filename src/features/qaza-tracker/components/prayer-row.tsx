import { Button } from '@/components/ui/button';
import { useTranslation } from '@/contexts/translation-context';
import { Pencil } from 'lucide-react';
import { useCallback } from 'react';
import type { FC } from 'react';

import type { QazaCounterKey } from '../types';

interface PrayerRowProps {
  label: string;
  count: number;
  counterKey: QazaCounterKey;
  onEdit: (key: QazaCounterKey) => void;
}

export const PrayerRow: FC<PrayerRowProps> = ({
  label,
  count,
  counterKey,
  onEdit,
}) => {
  const { t } = useTranslation();
  const handleClick = useCallback(() => {
    onEdit(counterKey);
  }, [counterKey, onEdit]);

  return (
    <div className="flex items-center gap-3 rounded-xl border border-border bg-card px-4 py-3 shadow-sm">
      <span className="min-w-0 flex-1 break-words text-start text-[15px] font-medium tracking-tight">
        {label}
      </span>
      <span
        className={`min-w-[28px] shrink-0 text-end text-lg font-bold tabular-nums tracking-tight ${
          count > 0 ? 'text-primary' : 'text-muted-foreground'
        }`}>
        <bdi dir="ltr">{count}</bdi>
      </span>
      <Button
        size="icon"
        className="size-11 shrink-0 rounded-lg shadow-sm"
        aria-label={`${t('qaza.editCount')}: ${label}`}
        data-qaza-counter={counterKey}
        onClick={handleClick}>
        <Pencil className="h-4 w-4" />
      </Button>
    </div>
  );
};
