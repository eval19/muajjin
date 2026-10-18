import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useTranslation } from '@/contexts/translation-context';
import type { FC } from 'react';

interface NumberAdjustmentControlProps {
  id: string;
  label: string;
  value: number;
  min: number;
  max: number;
  onChange: (value: number) => void;
}

export const NumberAdjustmentControl: FC<NumberAdjustmentControlProps> = ({
  id,
  label,
  value,
  min,
  max,
  onChange,
}) => {
  const { t } = useTranslation();
  return (
    <div className="flex items-center gap-3">
      <Button
        variant="outline"
        size="icon"
        aria-label={t('common.decreaseValue', { label })}
        disabled={value <= min || !Number.isSafeInteger(value - 1)}
        onClick={() => onChange(value - 1)}>
        -
      </Button>
      <Input
        id={id}
        type="number"
        aria-label={label}
        value={value}
        onChange={(e) => {
          const next = parseInt(e.target.value, 10) || 0;
          if (Number.isSafeInteger(next)) onChange(next);
        }}
        className="bg-card text-center"
      />
      <Button
        variant="outline"
        size="icon"
        aria-label={t('common.increaseValue', { label })}
        disabled={value >= max || !Number.isSafeInteger(value + 1)}
        onClick={() => onChange(value + 1)}>
        +
      </Button>
    </div>
  );
};
