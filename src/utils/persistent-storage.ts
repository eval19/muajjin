export interface StorageAccess {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}
export interface StorageSnapshot<T> { value: T; error: string | null }
export type StorageUpdate<T> = T | ((previous: T) => T);
export interface PersistentStore<T> {
  getSnapshot(): StorageSnapshot<T>;
  subscribe(listener: () => void): () => void;
  set(update: StorageUpdate<T>): boolean;
  reset(extraKeys?: string[]): boolean;
  receiveExternal(raw: string | null): void;
  persistMigration(): void;
}

export function storageFailure(error: unknown): string {
  const name = error instanceof Error ? error.name + ' ' + error.message : '';
  if (/quota/i.test(name)) return 'Device storage is full. Your previous saved data has been kept. Free some space and try again.';
  if (/security|denied|unavailable/i.test(name)) return 'Device storage is unavailable. Your previous saved data has been kept. Allow storage and try again.';
  return 'Could not save or read device data. Your previous saved data has been kept. Please try again.';
}

// localStorage has no multi-key transaction. Roll back only the keys this operation changed.
export function commitStorageChanges(storage: StorageAccess, changes: Record<string, string | null>): void {
  const originals = Object.keys(changes).map((key) => [key, storage.getItem(key)] as const);
  const changed: (readonly [string, string | null])[] = [];
  try {
    for (const original of originals) {
      const [key] = original;
      if (changes[key] === null) storage.removeItem(key);
      else storage.setItem(key, changes[key]!);
      changed.push(original);
    }
  } catch (error) {
    let rollbackFailed = false;
    for (const [key, value] of changed.reverse()) {
      try { if (value === null) storage.removeItem(key); else storage.setItem(key, value); }
      catch { rollbackFailed = true; }
    }
    if (rollbackFailed) throw new Error('Storage operation and rollback failed. Reopen the app to check saved data.');
    throw error;
  }
}

export function createPersistentStore<T>(
  key: string,
  initialValue: T,
  normalize: (value: unknown) => T,
  getStorage: () => StorageAccess,
): PersistentStore<T> {
  const listeners = new Set<() => void>();
  let snapshot: StorageSnapshot<T> = { value: initialValue, error: null };
  let rawValue: string | null = null;
  let needsMigration = false;
  const publish = (value: T, error: string | null) => {
    snapshot = { value, error };
    listeners.forEach((listener) => listener());
  };
  const read = (raw: string | null, announce: boolean) => {
    rawValue = raw;
    try {
      const value = normalize(raw === null ? initialValue : JSON.parse(raw) as unknown);
      needsMigration = raw !== null && JSON.stringify(value) !== raw;
      if (announce) publish(value, null);
      else snapshot = { value, error: null };
    } catch {
      needsMigration = false;
      if (announce) publish(snapshot.value, 'Saved data is damaged. The last working value has been kept; save a valid value or reset this setting to repair it.');
      else snapshot = { ...snapshot, error: 'Saved data is damaged. Defaults are shown; the original stored value has not been deleted.' };
    }
  };
  try { read(getStorage().getItem(key), false); }
  catch (error) { snapshot = { ...snapshot, error: storageFailure(error) }; }
  const refresh = () => {
    const current = getStorage().getItem(key);
    if (current !== rawValue) read(current, true);
  };
  return {
    getSnapshot: () => snapshot,
    subscribe(listener) { listeners.add(listener); return () => { listeners.delete(listener); }; },
    receiveExternal(raw) { read(raw, true); },
    set(update) {
      try {
        refresh();
        const candidate = typeof update === 'function' ? (update as (previous: T) => T)(snapshot.value) : update;
        const value = normalize(candidate);
        const serialized = JSON.stringify(value);
        // Persistence is outside React updaters and completes before subscribers see success.
        getStorage().setItem(key, serialized);
        rawValue = serialized;
        needsMigration = false;
        publish(value, null);
        return true;
      } catch (error) {
        publish(snapshot.value, storageFailure(error));
        return false;
      }
    },
    reset(extraKeys = []) {
      try {
        commitStorageChanges(getStorage(), Object.fromEntries([key, ...extraKeys].map((resetKey) => [resetKey, null])));
        rawValue = null;
        needsMigration = false;
        publish(normalize(initialValue), null);
        return true;
      } catch (error) { publish(snapshot.value, storageFailure(error)); return false; }
    },
    persistMigration() {
      if (!needsMigration) return;
      try {
        refresh();
        if (!needsMigration) return;
        const serialized = JSON.stringify(snapshot.value);
        getStorage().setItem(key, serialized);
        rawValue = serialized;
        needsMigration = false;
        if (snapshot.error) publish(snapshot.value, null);
      } catch (error) { publish(snapshot.value, storageFailure(error)); }
    },
  };
}
