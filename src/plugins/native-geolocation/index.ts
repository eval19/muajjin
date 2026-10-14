import { registerPlugin } from '@capacitor/core';
import { NativeGeolocationWeb } from './web';
import type { NativeGeolocationPlugin } from './definitions';

const NativeGeolocation = registerPlugin<NativeGeolocationPlugin>('NativeGeolocation', {
  web: async () => new NativeGeolocationWeb(),
});

export type { PositionOptions, Position, Coordinates, PermissionStatus, PermissionState, HeadingResult } from './definitions';
export { NativeGeolocation };
