import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from '@/contexts/translation-context';
import { detectLocation } from '@/services/location-service';
import type { LocationResult } from '@/services/location-service';
import type { LocationStatus } from '@/components/location/location-status-alert';

/** Only the current mounted request can publish a location or status. */
export function useLocationDetection(onLocation: (location: LocationResult) => void) {
  const { t } = useTranslation();
  const request = useRef<AbortController>();
  const [isDetecting, setIsDetecting] = useState(false);
  const [status, setStatus] = useState<LocationStatus | null>(null);
  useEffect(() => () => { request.current?.abort(); request.current = undefined; }, []);

  const cancel = useCallback(() => {
    request.current?.abort();
    request.current = undefined;
    setIsDetecting(false);
    setStatus(null);
  }, []);
  const detect = async () => {
    cancel();
    const current = new AbortController();
    request.current = current;
    setIsDetecting(true);
    setStatus({ type: 'info', message: t('onboarding.detectingGps') });
    try {
      const location = await detectLocation(current.signal);
      if (request.current !== current || current.signal.aborted) return;
      onLocation(location);
      setStatus({ type: 'success', message: location.addressUnavailable
        ? t('errors.locationAddressUnavailable')
        : t('onboarding.locationDetectedWithMethod', {
          city: location.city, country: location.country,
          method: location.method === 'gps' ? 'GPS' : t('onboarding.locationMethodIp'),
        }) });
    } catch (error) {
      if (request.current !== current || current.signal.aborted) return;
      setStatus({ type: 'error', message: t(error instanceof Error && error.message.startsWith('errors.') ? error.message : 'errors.locationFailed') });
    } finally {
      if (request.current === current) {
        request.current = undefined;
        setIsDetecting(false);
      }
    }
  };
  return { detect, cancel, isDetecting, status };
}
