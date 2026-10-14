import { useEffect, useMemo, useSyncExternalStore } from 'react';
import { toast } from '@/components/ui/use-toast';
import { createPersistentStore } from '@/utils/persistent-storage';
import type { PersistentStore, StorageSnapshot, StorageUpdate } from '@/utils/persistent-storage';
import { normalizeStorageValue } from '@/utils/settings-storage';

const stores = new Map<string, PersistentStore<unknown>>();
const reported = new WeakSet<object>();

function browserStorage() {
  if (typeof window === 'undefined') throw new Error('Storage unavailable');
  return window.localStorage;
}

function getStore<T>(key: string, initialValue: T): PersistentStore<T> {
  const existing = stores.get(key);
  if (existing) return existing as PersistentStore<T>;
  const normalize = (value: unknown) => {
    let legacyOnboarding = false;
    if (key === 'muajjin-settings') {
      try { legacyOnboarding = browserStorage().getItem('muajjin-onboarding-completed') === 'true'; }
      catch { /* The store reports unavailable storage without discarding saved state. */ }
    }
    return normalizeStorageValue(key, value, initialValue, legacyOnboarding);
  };
  const store = createPersistentStore(key, initialValue, normalize, browserStorage);
  stores.set(key, store as PersistentStore<unknown>);
  return store;
}

export interface LocalStorageStatus {
  error: string | null;
  reset: (extraKeys?: string[]) => boolean;
}

export function useLocalStorage<T>(
  key: string,
  initialValue: T,
): [T, (value: StorageUpdate<T>) => boolean, LocalStorageStatus] {
  // One owner per key makes synchronous functional updates and same-tab consumers consistent.
  const store = useMemo(() => getStore(key, initialValue), [key, initialValue]);
  const snapshot: StorageSnapshot<T> = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
  useEffect(() => {
    const receive = (event: StorageEvent) => {
      if ((event.key === key || event.key === null) && event.storageArea === window.localStorage) {
        store.receiveExternal(event.key === null ? null : event.newValue);
      }
    };
    window.addEventListener('storage', receive);
    // Catch changes between initial render and subscription, including external deletion.
    try { store.receiveExternal(window.localStorage.getItem(key)); }
    catch { /* Initialization already provides a surfaced storage error. */ }
    store.persistMigration();
    return () => window.removeEventListener('storage', receive);
  }, [key, store]);
  useEffect(() => {
    if (snapshot.error && !reported.has(snapshot)) {
      reported.add(snapshot);
      toast({ title: 'Device data could not be saved or loaded', description: snapshot.error, variant: 'destructive' });
    }
  }, [snapshot]);
  return [snapshot.value, store.set, { error: snapshot.error, reset: store.reset }];
}
