import type {
  HeadingResult,
  NativeGeolocationPlugin,
  Position,
  PositionOptions,
  PermissionStatus,
} from './definitions';

export class NativeGeolocationWeb implements NativeGeolocationPlugin {
  private requests = new Map<string, { watch: number; cancel: () => void }>();
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
      if (
        typeof window === 'undefined' ||
        !('DeviceOrientationEvent' in window)
      ) {
        reject(new Error('Compass heading is not supported on this device'));
        return;
      }

      const handleOrientation = (event: Event) => {
        const orientationEvent = event as DeviceOrientationEvent & {
          webkitCompassHeading?: number;
        };

        if (
          typeof orientationEvent.webkitCompassHeading === 'number' &&
          Number.isFinite(orientationEvent.webkitCompassHeading)
        ) {
          cleanup();
          resolve({ heading: orientationEvent.webkitCompassHeading });
          return;
        }

        if (
          typeof orientationEvent.alpha === 'number' &&
          Number.isFinite(orientationEvent.alpha)
        ) {
          cleanup();
          resolve({ heading: (360 - orientationEvent.alpha + 360) % 360 });
        }
      };

      const timeoutId = window.setTimeout(() => {
        cleanup();
        reject(new Error('Compass heading is unavailable'));
      }, 3000);

      const cleanup = () => {
        window.clearTimeout(timeoutId);
        window.removeEventListener(
          'deviceorientationabsolute',
          handleOrientation,
          true,
        );
        window.removeEventListener(
          'deviceorientation',
          handleOrientation,
          true,
        );
      };

      window.addEventListener(
        'deviceorientationabsolute',
        handleOrientation,
        true,
      );
      window.addEventListener('deviceorientation', handleOrientation, true);
    });
  }
}
