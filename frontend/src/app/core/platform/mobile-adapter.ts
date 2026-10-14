import { InjectionToken } from '@angular/core';

/** Fronteira web para futuras capacidades nativas, sem dependencia de Capacitor no MVP. */
export interface MobileAdapter {
  readonly platform: 'web' | 'android' | 'ios';
  readonly supportsNativeFilePicker: boolean;
  readonly supportsSecureStorage: boolean;
}

export const MOBILE_ADAPTER = new InjectionToken<MobileAdapter>('MOBILE_ADAPTER');

export const WEB_MOBILE_ADAPTER: MobileAdapter = Object.freeze({
  platform: 'web',
  supportsNativeFilePicker: false,
  supportsSecureStorage: false,
});
