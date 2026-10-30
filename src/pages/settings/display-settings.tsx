import { SettingsPageLayout } from '@/components/settings/settings-page-layout';
import { SettingsSaveButton } from '@/components/settings/settings-save-button';
import { SettingsSection } from '@/components/settings/settings-section';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import { useLocalStorage } from '@/hooks/use-local-storage';
import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import {
  closestCenter,
  DndContext,
  DragEndEvent,
  KeyboardSensor,
  MouseSensor,
  TouchSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { restrictToVerticalAxis } from '@dnd-kit/modifiers';
import {
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Check, GripVertical, Trash2, Upload } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import type { ChangeEvent, FC } from 'react';
import { useNavigate } from 'react-router-dom';

import { DEFAULT_CONTAINER_ORDER } from '@/constants/default-settings';
import { useApp } from '@/contexts/app-context';
import { useTranslation } from '@/contexts/translation-context';
import {
  openBatteryOptimizationSettings,
  openExactAlarmSettings,
  readWidgetReliability,
} from '@/services/widget-sync-service';
import type { WidgetReliability } from '@/services/widget-sync-service';
import { EContainerType } from '@/types/enums';

const CONTAINER_LABELS: Record<string, string> = {
  [EContainerType.DateTime]: 'Date & Time',
  [EContainerType.CurrentSalat]: 'Current Salat',
  [EContainerType.NextSalat]: 'Next Salat',
  [EContainerType.SalatTimes]: 'Salat Times',
  [EContainerType.ProhibitedTimes]: 'Forbidden Times',
  [EContainerType.SaumTimes]: 'Saum Times',
};

// Translation keys mapping
const CONTAINER_LABEL_KEYS: Record<string, string> = {
  [EContainerType.DateTime]: 'settings.dateTime',
  [EContainerType.CurrentSalat]: 'settings.currentSalat',
  [EContainerType.NextSalat]: 'settings.nextSalat',
  [EContainerType.SalatTimes]: 'settings.salatTimes',
  [EContainerType.ProhibitedTimes]: 'settings.forbiddenTimes',
  [EContainerType.SaumTimes]: 'settings.saumTimes',
};

// Sortable item for container reordering
interface SortableContainerProps {
  id: string;
  label: string;
  isVisible: boolean;
  onToggle: (id: string) => void;
}

const SortableContainer: FC<SortableContainerProps> = ({
  id,
  label,
  isVisible,
  onToggle,
}) => {
  const { attributes, listeners, setNodeRef, transform, transition } =
    useSortable({ id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`relative cursor-move rounded-sm border p-3 hover:bg-muted/50 ${
        !isVisible ? 'opacity-50' : 'bg-background'
      }`}>
      <div className="flex items-center justify-between gap-3">
        <div className="flex min-w-0 flex-1 items-center gap-3">
          <div
            aria-label={label}
            {...attributes}
            {...listeners}
            className="shrink-0 cursor-grab active:cursor-grabbing"
            style={{ touchAction: 'none' }}>
            <GripVertical size={18} className="text-muted-foreground" />
          </div>
          <span className="min-w-0 break-words text-sm font-medium">{label}</span>
        </div>
        <Switch
          aria-label={label}
          checked={isVisible}
          onCheckedChange={() => onToggle(id)}
          className="shrink-0 cursor-pointer"
        />
      </div>
    </div>
  );
};

const DisplaySettings: FC = () => {
  const navigate = useNavigate();
  const { t, uploadFont, removeFont, customFont, activeTranslation } =
    useTranslation();
  const { settings, updateSettings } = useApp();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [containerOrder, setContainerOrder] = useLocalStorage<string[]>(
    'muajjin-container-order',
    DEFAULT_CONTAINER_ORDER,
  );
  const hasCustomFont = !!customFont;
  const nativeAndroid =
    Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'android';
  const [reliability, setReliability] = useState<WidgetReliability | null>(null);
  const [reliabilityError, setReliabilityError] = useState(false);
  const refreshReliability = useCallback(async () => {
    if (!nativeAndroid) return;
    try {
      setReliability(await readWidgetReliability());
      setReliabilityError(false);
    } catch {
      setReliabilityError(true);
    }
  }, [nativeAndroid]);

  useEffect(() => {
    if (!nativeAndroid) return;
    let disposed = false;
    void refreshReliability();
    const onFocus = () => {
      if (!disposed) void refreshReliability();
    };
    window.addEventListener('focus', onFocus);
    const listener = App.addListener('appStateChange', ({ isActive }) => {
      if (isActive && !disposed) void refreshReliability();
    });
    return () => {
      disposed = true;
      window.removeEventListener('focus', onFocus);
      void listener
        .then((handle) => handle.remove())
        .catch(() => undefined);
    };
  }, [nativeAndroid, refreshReliability]);

  const exactStatus = !reliability
    ? t('settings.widgetChecking')
    : reliability.exactAlarmSupported === false
      ? t('settings.widgetExactAlwaysAllowed')
      : reliability.exactAlarmAllowed
        ? t('settings.widgetExactAllowed')
        : t('settings.widgetExactDenied');
  const batteryStatus = !reliability
    ? t('settings.widgetChecking')
    : reliability.batteryUnrestricted
      ? t('settings.widgetBatteryUnrestricted')
      : t('settings.widgetBatteryOptimized');

  const getCurrentFontInfo = () => {
    if (hasCustomFont) {
      return { name: 'customfont.woff2', type: 'custom' as const };
    }
    const language = activeTranslation?.meta.languageCode
      .toLowerCase()
      .replace(/_/g, '-')
      .split('-')[0];
    if (!activeTranslation || language === 'en') {
      return { name: 'Ubuntu-Regular.ttf', type: 'builtin' as const };
    }
    if (language === 'bn') {
      return { name: 'NotoSerifBengali.ttf', type: 'builtin' as const };
    }
    if (language === 'ar') {
      return {
        name: 'IBMPlexSansArabic-Regular.ttf',
        type: 'builtin' as const,
      };
    }
    return { name: t('settings.systemFont'), type: 'system' as const };
  };

  const currentFont = getCurrentFontInfo();

  // Default visible containers - all visible by default
  const defaultVisibleContainers: Record<string, boolean> = {
    [EContainerType.DateTime]: true,
    [EContainerType.CurrentSalat]: true,
    [EContainerType.NextSalat]: true,
    [EContainerType.SalatTimes]: true,
    [EContainerType.ProhibitedTimes]: true,
    [EContainerType.SaumTimes]: true,
  };

  const [visibleContainers, setVisibleContainers] = useLocalStorage<
    Record<string, boolean>
  >('muajjin-visible-containers', defaultVisibleContainers);

  const sensors = useSensors(
    useSensor(MouseSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
    useSensor(TouchSensor, {
      activationConstraint: {
        delay: 150,
        tolerance: 8,
      },
    }),
    useSensor(KeyboardSensor),
  );

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (!over || active.id === over.id) {
      return;
    }

    const activeId = String(active.id);
    const overId = String(over.id);
    const oldIndex = containerOrder.indexOf(activeId);
    const newIndex = containerOrder.indexOf(overId);
    if (oldIndex === -1 || newIndex === -1) {
      return;
    }

    const newOrder = [...containerOrder];
    newOrder.splice(oldIndex, 1);
    newOrder.splice(newIndex, 0, activeId);

    setContainerOrder(newOrder);
  };

  const handleToggleContainer = (containerId: string) => {
    setVisibleContainers((prev) => ({
      ...prev,
      [containerId]: !prev[containerId],
    }));
  };

  const handleFontUpload = async (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];
    if (!file) return;

    await uploadFont(file);

    // Reset file input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <SettingsPageLayout
      title={t('settings.displaySettings')}
      contentClassName="max-w-md mx-auto px-5 py-6 space-y-6">
      {/* Custom Font */}
      <SettingsSection
        title={t('settings.customFont')}
        description={t('settings.uploadFontDesc')}>
        <div className="space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-md border bg-muted/50 px-3 py-2">
            <div className="flex min-w-0 items-center gap-2">
              <Check className="h-4 w-4 shrink-0 text-muted-foreground" />
              <span className="min-w-0 break-words text-sm text-muted-foreground [overflow-wrap:anywhere]">
                {t('settings.currentFont')}: <bdi>{currentFont.name}</bdi>
                {currentFont.type === 'builtin' && ` (${t('settings.builtin')})`}
                {currentFont.type === 'system' && ` (${t('settings.systemFont')})`}
              </span>
            </div>
            {hasCustomFont && (
              <Button
                type="button"
                variant="destructive"
                size="sm"
                onClick={removeFont}
                className="gap-1">
                <Trash2 className="h-4 w-4" />
                {t('settings.removeFont')}
              </Button>
            )}
          </div>
          <input
            ref={fileInputRef}
            type="file"
            accept=".woff2"
            onChange={handleFontUpload}
            className="hidden"
          />
          <Button
            type="button"
            variant="outline"
            className="w-full gap-2"
            onClick={() => fileInputRef.current?.click()}>
            <Upload className="h-4 w-4" />
            {t('settings.uploadFont')}
          </Button>
          <p className="text-center text-xs text-muted-foreground">
            {t('settings.fontFileName')}
          </p>
        </div>
      </SettingsSection>

      {/* Home Screen Layout */}
      <SettingsSection
        title={t('settings.homeScreenLayout')}
        description={t('settings.homeScreenLayoutDesc')}>
        <DndContext
          sensors={sensors}
          collisionDetection={closestCenter}
          onDragEnd={handleDragEnd}
          modifiers={[restrictToVerticalAxis]}>
          <SortableContext
            items={containerOrder}
            strategy={verticalListSortingStrategy}>
            <div className="space-y-2">
              {containerOrder.map((containerId) => (
                <SortableContainer
                  key={containerId}
                  id={containerId}
                  label={
                    t(CONTAINER_LABEL_KEYS[containerId]) ||
                    CONTAINER_LABELS[containerId] ||
                    containerId
                  }
                  isVisible={visibleContainers[containerId] ?? true}
                  onToggle={handleToggleContainer}
                />
              ))}
            </div>
          </SortableContext>
        </DndContext>
      </SettingsSection>

      {nativeAndroid && (
        <SettingsSection
          title={t('settings.widgetTextSize')}
          description={t('settings.widgetTextSizeDesc')}>
          <div className="flex items-center justify-between gap-3 rounded-lg border border-border bg-card p-4">
            <div className="min-w-0">
              <p className="font-medium">{t('settings.widgetLargerText')}</p>
              <p className="text-sm text-muted-foreground">
                {settings.widgetLargeText
                  ? t('settings.widgetLargerTextOn')
                  : t('settings.widgetLargerTextOff')}
              </p>
            </div>
            <Switch
              aria-label={t('settings.widgetLargerText')}
              checked={settings.widgetLargeText}
              onCheckedChange={(checked) =>
                updateSettings({ widgetLargeText: checked })
              }
              className="shrink-0 cursor-pointer"
            />
          </div>
        </SettingsSection>
      )}

      {nativeAndroid && (
        <SettingsSection
          title={t('settings.widgetReliability')}
          description={t('settings.widgetReliabilityDesc')}>
          <div className="space-y-3 rounded-lg border border-border bg-card p-4">
            {reliabilityError && (
              <p className="text-sm text-destructive">
                {t('settings.widgetReliabilityUnavailable')}
              </p>
            )}
            <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
              <span className="text-sm font-medium">
                {t('settings.widgetExactAlarms')}
              </span>
              <span
                className={`text-sm [overflow-wrap:anywhere] ${
                  reliability &&
                  reliability.exactAlarmSupported !== false &&
                  !reliability.exactAlarmAllowed
                    ? 'text-destructive'
                    : 'text-muted-foreground'
                }`}>
                {exactStatus}
              </span>
            </div>
            {reliability &&
              reliability.exactAlarmSupported &&
              !reliability.exactAlarmAllowed && (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="w-full"
                  onClick={() => {
                    void openExactAlarmSettings()
                      .then(refreshReliability)
                      .catch(() => setReliabilityError(true));
                  }}>
                  {t('settings.widgetAllowExactAlarms')}
                </Button>
              )}
            <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
              <span className="text-sm font-medium">
                {t('settings.widgetBatteryOptimization')}
              </span>
              <span className="text-sm text-muted-foreground">
                {batteryStatus}
              </span>
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="w-full"
              onClick={() => {
                void openBatteryOptimizationSettings()
                  .then(refreshReliability)
                  .catch(() => setReliabilityError(true));
              }}>
              {t('settings.widgetOpenBatterySettings')}
            </Button>
            <div className="space-y-1 border-t border-border pt-3 text-sm text-muted-foreground">
              <p>
                {t('settings.widgetNextUpdate')}:{' '}
                {reliability && reliability.nextTransition > 0
                  ? <bdi dir="ltr" className="tabular-nums">{new Date(reliability.nextTransition).toLocaleTimeString(
                      [],
                      { hour: '2-digit', minute: '2-digit' },
                    )}</bdi>
                  : t('settings.widgetNotScheduled')}
              </p>
              <p>
                {t('settings.widgetLastRefresh')}:{' '}
                {reliability && reliability.lastDispatch > 0
                  ? <bdi dir="ltr" className="tabular-nums">{new Date(reliability.lastDispatch).toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}</bdi>
                  : t('settings.widgetNotRecorded')}
              </p>
            </div>
            <p className="text-xs text-muted-foreground">
              {t('settings.widgetReliabilityNote')}
            </p>
          </div>
        </SettingsSection>
      )}

      <SettingsSaveButton onClick={() => navigate(-1)} />
    </SettingsPageLayout>
  );
};

export default DisplaySettings;
