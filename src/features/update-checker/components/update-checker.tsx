import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { toast } from '@/components/ui/use-toast';
import { useTranslation } from '@/contexts/translation-context';
import { useLocalStorage } from '@/hooks/use-local-storage';
import { subscribeUpdateChecks } from '@/features/update-checker/services/update-events';
import {
  fetchLatestRelease,
  isNewerVersion,
} from '@/features/update-checker/services/update-service';
import type { ReleaseInfo } from '@/features/update-checker/services/update-service';
import { useCallback, useEffect, useRef, useState } from 'react';
import type { FC } from 'react';
import packageInfo from '../../../../package.json';

const IGNORED_RELEASE_KEY = 'muajjin-update-ignored';

const isOnline = () => {
  if (typeof navigator === 'undefined') return true;
  return navigator.onLine;
};

export const UpdateChecker: FC = () => {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const [releaseInfo, setReleaseInfo] = useState<ReleaseInfo | null>(null);
  const [ignoredVersion, setIgnoredVersion] = useLocalStorage<string | null>(
    IGNORED_RELEASE_KEY,
    null,
  );
  const requestRef = useRef<AbortController | null>(null);
  const mountedRef = useRef(false);

  const currentVersion = packageInfo.version || '0.0.0';

  const cancelUpdateCheck = useCallback(() => {
    requestRef.current?.abort();
    requestRef.current = null;
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      cancelUpdateCheck();
    };
  }, [cancelUpdateCheck]);

  const checkForUpdates = useCallback(
    async (options?: { force?: boolean; notifyUpToDate?: boolean }) => {
      if (!mountedRef.current || requestRef.current) return;
      const controller = new AbortController();
      requestRef.current = controller;

      try {
        const latestRelease = await fetchLatestRelease(controller.signal);
        if (
          !mountedRef.current ||
          controller.signal.aborted ||
          requestRef.current !== controller
        ) {
          return;
        }
        if (!latestRelease) {
          if (options?.notifyUpToDate) {
            toast({
              title: t('update.checkFailedTitle'),
              description: t('update.checkFailedDescription'),
              duration: 3000,
            });
          }
          return;
        }
        const isNewer = isNewerVersion(
          currentVersion,
          latestRelease.latestVersion,
        );
        if (!isNewer) {
          if (options?.notifyUpToDate) {
            toast({
              title: t('update.upToDateTitle'),
              description: t('update.upToDateDescription', {
                currentVersion,
              }),
              duration: 3000,
            });
          }
          return;
        }
        if (
          !options?.force &&
          ignoredVersion &&
          ignoredVersion === latestRelease.latestVersion
        ) {
          return;
        }

        setReleaseInfo(latestRelease);
        setIsOpen(true);
      } finally {
        if (requestRef.current === controller) requestRef.current = null;
      }
    },
    [currentVersion, ignoredVersion, t],
  );

  useEffect(() => {
    if (!isOnline()) return;
    checkForUpdates();
  }, [checkForUpdates]);

  useEffect(() => {
    const unsubscribe = subscribeUpdateChecks((options) => {
      if (!isOnline()) {
        toast({
          title: t('update.offlineTitle'),
          description: t('update.offlineDescription'),
          duration: 3000,
        });
        return;
      }
      checkForUpdates({
        force: Boolean(options?.force),
        notifyUpToDate: options?.notifyUpToDate ?? true,
      });
    });

    const handleOnline = () => {
      if (isOnline()) {
        checkForUpdates();
      }
    };

    window.addEventListener('online', handleOnline);
    return () => {
      window.removeEventListener('online', handleOnline);
      unsubscribe();
    };
  }, [checkForUpdates, t]);

  const handleOpenChange = (open: boolean) => {
    if (!open) cancelUpdateCheck();
    setIsOpen(open);
  };

  const handleIgnore = () => {
    if (releaseInfo?.latestVersion) {
      setIgnoredVersion(releaseInfo.latestVersion);
    }
    handleOpenChange(false);
  };

  const handleDownload = () => {
    const target = releaseInfo?.downloadUrl || releaseInfo?.releaseUrl || null;
    if (target) {
      window.open(target, '_blank', 'noopener,noreferrer');
    }
    handleOpenChange(false);
  };

  if (!releaseInfo) return null;

  return (
    <AlertDialog open={isOpen} onOpenChange={handleOpenChange}>
      <AlertDialogContent
        className="w-[90%] rounded-2xl"
        onOpenAutoFocus={(event) => event.preventDefault()}>
        <AlertDialogHeader>
          <AlertDialogTitle>{t('update.availableTitle')}</AlertDialogTitle>
          <AlertDialogDescription>
            {t('update.availableDescription', {
              latestVersion: releaseInfo.latestVersion,
              currentVersion,
            })}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel
            onClick={handleIgnore}
            className="outline-none ring-0 focus-visible:outline-none focus-visible:ring-0 focus-visible:ring-offset-0">
            {t('update.ignore')}
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleDownload}
            disabled={!releaseInfo.downloadUrl && !releaseInfo.releaseUrl}
            className="focus-visible:outline-none focus-visible:ring-0 focus-visible:ring-offset-0">
            {t('update.download', {
              latestVersion: releaseInfo.latestVersion,
            })}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
};
