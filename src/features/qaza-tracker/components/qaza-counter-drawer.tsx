import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Sheet, SheetContent, SheetTitle } from '@/components/ui/sheet';
import { useTranslation } from '@/contexts/translation-context';
import { Minus, Plus } from 'lucide-react';
import type { ChangeEvent, FC, KeyboardEvent } from 'react';

import { MAX_QAZA_COUNT } from '../constants';
import type { QazaCounterKey } from '../types';

interface QazaCounterDrawerProps {
  isVisible: boolean;
  isOpen: boolean;
  activeKey: QazaCounterKey | null;
  activeCount: number;
  editing: boolean;
  editValue: string;
  onClose: () => void;
  onDecrement: () => void;
  onIncrement: () => void;
  onStartEdit: () => void;
  onEditChange: (e: ChangeEvent<HTMLInputElement>) => void;
  onEditBlur: () => void;
  onEditKeyDown: (e: KeyboardEvent<HTMLInputElement>) => void;
  getLabel: (key: QazaCounterKey) => string;
  manualSetHint: string;
}

export const QazaCounterDrawer: FC<QazaCounterDrawerProps> = ({
  isVisible,
  isOpen,
  activeKey,
  activeCount,
  editing,
  editValue,
  onClose,
  onDecrement,
  onIncrement,
  onStartEdit,
  onEditChange,
  onEditBlur,
  onEditKeyDown,
  getLabel,
  manualSetHint,
}) => {
  const { t, direction } = useTranslation();
  if (!isVisible) return null;
  const label = activeKey ? getLabel(activeKey) : '';

  return (
    <Sheet open={isOpen} onOpenChange={(open) => { if (!open) onClose(); }}>
      <SheetContent
        side="bottom"
        dir={direction}
        closeLabel={t('common.close')}
        aria-describedby="qaza-manual-hint"
        onCloseAutoFocus={(event) => {
          event.preventDefault();
          document.querySelector<HTMLButtonElement>(
            `button[data-qaza-counter="${activeKey}"]`,
          )?.focus();
        }}
        className="bottom-14 rounded-t-3xl bg-card p-0">
        <div className="flex justify-center pb-1 pt-3">
          <div className="h-1 w-9 rounded-full bg-border" />
        </div>
        <SheetTitle className="px-16 pb-2 pt-3 text-center text-[22px] font-bold">
          {label}
        </SheetTitle>
        <div className="mx-6 my-2 border-t border-border" />
        <div className="flex items-center justify-center gap-7 px-8 pb-4 pt-3">
          <Button
            variant="outline"
            size="icon"
            aria-label={`${t('qaza.decreaseCount')}: ${label}`}
            className="h-[52px] w-[52px] shrink-0 rounded-2xl bg-primary text-white shadow-md shadow-primary/30 hover:bg-primary/90"
            onClick={onDecrement}
            disabled={activeCount === 0}>
            <Minus className="h-5 w-5" />
          </Button>
          {editing ? (
            <Input
              type="number"
              aria-label={`${t('qaza.editCount')}: ${label}`}
              min={0}
              max={MAX_QAZA_COUNT}
              value={editValue}
              onChange={onEditChange}
              onBlur={onEditBlur}
              onKeyDown={onEditKeyDown}
              className="min-h-[56px] min-w-[64px] border-2 border-primary bg-primary/10 px-3 text-center text-[36px] font-bold tabular-nums tracking-tight text-primary"
              autoFocus
            />
          ) : (
            <button
              aria-label={`${t('qaza.editCount')}: ${label}`}
              onClick={onStartEdit}
              className={`flex min-h-[56px] min-w-[64px] items-center justify-center rounded-2xl px-3 text-[36px] font-bold tabular-nums tracking-tight transition-colors ${
                activeCount > 0 ? 'bg-primary/10 text-primary' : 'bg-muted text-muted-foreground'
              }`}>
              {activeCount}
            </button>
          )}
          <Button
            variant="outline"
            size="icon"
            aria-label={`${t('qaza.increaseCount')}: ${label}`}
            className="h-[52px] w-[52px] shrink-0 rounded-2xl bg-muted"
            onClick={onIncrement}>
            <Plus className="h-5 w-5" />
          </Button>
        </div>
        <p id="qaza-manual-hint" className="pb-6 text-center text-xs text-muted-foreground">
          {manualSetHint}
        </p>
      </SheetContent>
    </Sheet>
  );
};
