import type {
  HeadingResult,
  NativeGeolocationPlugin,
  Position,
  PositionOptions,
  PermissionStatus,
  WatchHeadingCallback,
} from './definitions';

function readOrientationHeading(
  event: DeviceOrientationEvent & { webkitCompassHeading?: number },
): { heading: number; source: 'webkit' | 'alpha-absolute' | 'alpha-relative' } | null {
  if (typeof event.webkitCompassHeading === 'number' && Number.isFinite(event.webkitCompassHeading)) {
    return { heading: ((event.webkitCompassHeading % 360) + 360) % 360, source: 'webkit' };
  }
  if (typeof event.alpha === 'number' && Number.isFinite(event.alpha)) {
    return {
      heading: ((360 - event.alpha) % 360 + 360) % 360,
      source: event.absolute ? 'alpha-absolute' : 'alpha-relative',
    };
  }
  return null;
}

export class NativeGeolocationWeb implements NativeGeolocationPlugin {
  private requests = new Map<string, { watch: number; cancel: () => void }>();
  private headingWatches = new Map<string, () => void>();
  async getCurrentPosition(options?: PositionOptions): Promise<Position> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Geolocation is not supported by your browser'));
        return;
      }

      const geoOptions: PositionOptions = {
        enableHighAccuracy: options?.enableHighAccuracy ?? true,
        timeout: options?.timeout ?? 15000,
        maximumAge: options?.maximumAge ?? 0,
      };

      const id = options?.requestId ?? crypto.randomUUID();
      if (this.requests.has(id)) { reject(new Error('Duplicate location request')); return; }
      let finished = false;
      const cleanup = () => {
        if (finished) return false;
        finished = true;
        const request = this.requests.get(id);
        if (request) navigator.geolocation.clearWatch(request.watch);
        this.requests.delete(id);
        return true;
      };
      const watch = navigator.geolocation.watchPosition(
        (position) => {
          if (!cleanup()) return;
          resolve({
            coords: {
              latitude: position.coords.latitude,
              longitude: position.coords.longitude,
              accuracy: position.coords.accuracy,
              altitude: position.coords.altitude ?? undefined,
              heading: position.coords.heading ?? undefined,
              speed: position.coords.speed ?? undefined,
            },
            timestamp: position.timestamp,
          });
        },
        (error) => {
          if (!cleanup()) return;
          reject(new Error(`Geolocation error: ${error.message}`));
        },
        geoOptions,
      );
      this.requests.set(id, { watch, cancel: () => { cleanup(); reject(new Error('errors.locationCancelled')); } });
    });
  }

  async cancelPosition(options: { requestId: string }): Promise<void> {
    this.requests.get(options.requestId)?.cancel();
  }

  async requestPermissions(): Promise<PermissionStatus> {
    // Web doesn't have explicit permission requests - it's handled by the browser
    return {
      location: 'prompt',
      coarseLocation: 'prompt',
    };
  }

  async getCurrentHeading(): Promise<HeadingResult> {
    return new Promise((resolve, reject) => {
      if (typeof window === 'undefined' || !('DeviceOrientationEvent' in window)) {
        reject(new Error('Compass heading is not supported on this device'));
        return;
      }

      const handleOrientation = (event: Event) => {
        const parsed = readOrientationHeading(
          event as DeviceOrientationEvent & { webkitCompassHeading?: number },
        );
        if (!parsed) return;
        cleanup();
        resolve({ heading: parsed.heading });
      };

      const timeoutId = window.setTimeout(() => {
        cleanup();
        reject(new Error('Compass heading is unavailable'));
      }, 3000);

      const cleanup = () => {
        window.clearTimeout(timeoutId);
        window.removeEventListener('deviceorientationabsolute', handleOrientation, true);
        window.removeEventListener('deviceorientation', handleOrientation, true);
      };

      window.addEventListener('deviceorientationabsolute', handleOrientation, true);
      window.addEventListener('deviceorientation', handleOrientation, true);
    });
  }

  async watchHeading(callback: WatchHeadingCallback): Promise<string> {
    if (typeof window === 'undefined' || !('DeviceOrientationEvent' in window)) {
      throw new Error('Compass heading is not supported on this device');
    }
    const id = crypto.randomUUID();
    let selectedSource: 'webkit' | 'alpha-absolute' | 'alpha-relative' | null = null;
    const handleOrientation = (event: Event) => {
      const parsed = readOrientationHeading(
        event as DeviceOrientationEvent & { webkitCompassHeading?: number },
      );
      if (!parsed) return;
      if (selectedSource === null || parsed.source === 'webkit') {
        selectedSource = parsed.source;
      }
      if (parsed.source !== selectedSource) return;
      callback({ heading: parsed.heading });
    };
    window.addEventListener('deviceorientationabsolute', handleOrientation, true);
    window.addEventListener('deviceorientation', handleOrientation, true);
    this.headingWatches.set(id, () => {
      window.removeEventListener('deviceorientationabsolute', handleOrientation, true);
      window.removeEventListener('deviceorientation', handleOrientation, true);
    });
    return id;
  }

  async clearHeadingWatch(options: { id: string }): Promise<void> {
    const cleanup = this.headingWatches.get(options.id);
    if (!cleanup) return;
    this.headingWatches.delete(options.id);
    cleanup();
  }
}
