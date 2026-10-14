import { describe, expect, test } from 'bun:test';
import { normalizeAppSettings, normalizeQazaCounts, normalizeContainerOrder, normalizeVisibility } from '../src/utils/settings-storage';
import { createPersistentStore, commitStorageChanges } from '../src/utils/persistent-storage';
import { validateTranslation, normalizeStoredTranslations } from '../src/utils/translation-validation';
import { validateWoff2, decodeLegacyFont, LatestFontRequest, saveFontRecord, type FontRecord } from '../src/utils/font-storage';
import enTemplate from '../src/i18n/template-en.json';
import { calculatePrayerInstants } from '../src/services/prayer-times-local';

class MemoryStorage {
  values = new Map<string, string>();
  denied = false;
  getItem(key: string) { if (this.denied) throw new Error('SecurityError'); return this.values.get(key) ?? null; }
  setItem(key: string, value: string) { if (this.denied) throw new Error('QuotaExceededError'); this.values.set(key, value); }
  removeItem(key: string) { if (this.denied) throw new Error('SecurityError'); this.values.delete(key); }
}

describe('settings loading boundary', () => {
  test('migrates legacy names without overwriting valid current values or zero coordinates', () => {
    const settings = normalizeAppSettings({ madhab: 2, sehriAdjustment: -9, latitude: 0, longitude: 0, jamaahTimes: { Fajr: '05:35', Isha: 20 }, homeSections: [{ id: 'saumTimes', visible: false, order: 0 }] });
    expect(settings.madhab).toBe(0);
    expect(settings.suhoorAdjustment).toBe(-9);
    expect(settings.latitude).toBe(0);
    expect(settings.longitude).toBe(0);
    expect(settings.jamaahTimes).toEqual({ Fajr: '05:35' });
    expect(settings.homeSections?.find((section) => section.id === 'saumTimes')?.visible).toBe(false);
    expect(settings.homeSections).toHaveLength(6);
    expect(normalizeAppSettings({ sehriAdjustment: -9, suhoorAdjustment: 3 }).suhoorAdjustment).toBe(3);
  });
  test('damaged saved coordinates cannot calculate another city while valid preferences survive', () => {
    for (const latitude of [91, null, 'wrong', Infinity]) {
      const settings = normalizeAppSettings({ city: 'Dhaka', latitude, longitude: 0, method: 4, madhab: 0, jamaahTimes: { Fajr: '05:35' } });
      expect(() => calculatePrayerInstants(new Date(2026, 9, 13), settings)).toThrow(RangeError);
      expect(settings.city).toBe('Dhaka');
      expect(settings.jamaahTimes.Fajr).toBe('05:35');
    }
  });
  test('a partially missing saved coordinate cannot fabricate a location', () => {
    for (const saved of [{ city: 'Dhaka', longitude: 90.4 }, { city: 'Dhaka', latitude: 23.8 }]) {
      const settings = normalizeAppSettings(saved);
      expect(() => calculatePrayerInstants(new Date(2026, 9, 13), settings)).toThrow(RangeError);
      expect(settings.city).toBe('Dhaka');
    }
  });
  test('onboarding keeps the explicit unselected madhab until the user chooses', () => {
    const settings = normalizeAppSettings({ latitude: 0, longitude: 0, madhab: null, onboardingComplete: false });
    expect(settings.madhab).toBeNull();
    expect(() => calculatePrayerInstants(new Date(2026, 9, 13), settings)).toThrow(RangeError);
    expect(normalizeAppSettings({ ...settings, madhab: 0 }).madhab).toBe(0);
  });
  test('valid Qaza siblings and existing large counts survive invalid entries', () => {
    expect(normalizeQazaCounts({ fajr: 234, dhuhr: -1, asr: '3', other: 100000, saum: 8 })).toEqual({ fajr: 234, dhuhr: 0, asr: 0, maghrib: 0, isha: 0, other: 100000, saum: 8 });
  });
  test('customization restores missing siblings without accepting unknown panels or truthy types', () => {
    const order = normalizeContainerOrder(['saumTimes', 'salatTimes', 'saumTimes', 'unknown', 4]);
    expect(order.slice(0, 2)).toEqual(['saumTimes', 'salatTimes']);
    expect(new Set(order).size).toBe(6);
    expect(normalizeVisibility({ saumTimes: false, currentSalat: 'false' }).saumTimes).toBe(false);
    expect(normalizeVisibility({ currentSalat: 'false' }).currentSalat).toBe(true);
  });
  test('unusable persisted adjustments are normalized to default without corrupting valid siblings', () => {
    for (const badValue of [Number.MAX_SAFE_INTEGER, -Number.MAX_SAFE_INTEGER, 'bad', NaN, Infinity, null, 1e16, -1e16]) {
      const settings = normalizeAppSettings({
        suhoorAdjustment: badValue,
        iftarAdjustment: badValue,
        hijriAdjustment: badValue,
        city: 'Cairo',
        latitude: 30.0444,
        longitude: 31.2357,
        method: 5,
        madhab: 0,
      });
      expect(settings.suhoorAdjustment).toBe(0);
      expect(settings.iftarAdjustment).toBe(0);
      expect(settings.hijriAdjustment).toBe(0);
      expect(settings.city).toBe('Cairo');
      expect(settings.latitude).toBe(30.0444);
      expect(settings.method).toBe(5);
    }
    const valid = normalizeAppSettings({
      suhoorAdjustment: -121,
      iftarAdjustment: 121,
      hijriAdjustment: 121,
    });
    expect(valid.suhoorAdjustment).toBe(-121);
    expect(valid.iftarAdjustment).toBe(121);
    expect(valid.hijriAdjustment).toBe(121);
  });
});

describe('durable shared persistence transitions', () => {
  test('repeated functional updates use the latest committed count before any render', () => {
    const storage = new MemoryStorage();
    const store = createPersistentStore('counter', 0, (value) => typeof value === 'number' ? value : 0, () => storage);
    store.set((value) => value + 1);
    store.set((value) => value + 1);
    store.set((value) => value + 1);
    expect(store.getSnapshot().value).toBe(3);
    expect(storage.getItem('counter')).toBe('3');
    expect(createPersistentStore('counter', 0, (value) => typeof value === 'number' ? value : 0, () => storage).getSnapshot().value).toBe(3);
  });
  test('quota failures do not publish a successful value and recover on retry', () => {
    const storage = new MemoryStorage();
    storage.setItem('counter', '4');
    const store = createPersistentStore('counter', 0, (value) => typeof value === 'number' ? value : 0, () => storage);
    storage.denied = true;
    expect(store.set((value) => value + 1)).toBe(false);
    expect(store.getSnapshot().value).toBe(4);
    expect(store.getSnapshot().error).not.toBeNull();
    storage.denied = false;
    expect(store.set((value) => value + 1)).toBe(true);
    expect(store.getSnapshot().value).toBe(5);
  });
  test('unknown external values, corrupt JSON, deletion and clear are validated and announced', () => {
    const storage = new MemoryStorage();
    const store = createPersistentStore('qaza', normalizeQazaCounts({ fajr: 2 }), normalizeQazaCounts, () => storage);
    let announcements = 0;
    store.subscribe(() => { announcements++; });
    store.receiveExternal('{"fajr":9,"asr":"bad"}');
    expect(store.getSnapshot().value.fajr).toBe(9);
    expect(store.getSnapshot().value.asr).toBe(0);
    store.receiveExternal('{broken');
    expect(store.getSnapshot().value.fajr).toBe(9);
    expect(store.getSnapshot().error).not.toBeNull();
    store.receiveExternal(null);
    expect(store.getSnapshot().value.fajr).toBe(2);
    expect(announcements).toBe(3);
  });
  test('reads another tab latest durable state before functional updates', () => {
    const storage = new MemoryStorage();
    const store = createPersistentStore('counter', 0, (value) => typeof value === 'number' ? value : 0, () => storage);
    storage.setItem('counter', '20');
    store.set((value) => value + 1);
    expect(store.getSnapshot().value).toBe(21);
  });
  test('multi-key failures roll back prior writes without touching unrelated data', () => {
    const storage = new MemoryStorage();
    storage.setItem('a', 'old'); storage.setItem('unrelated', 'keep');
    const failingStorage = {
      getItem: storage.getItem.bind(storage),
      removeItem: storage.removeItem.bind(storage),
      setItem(key: string, value: string) { if (key === 'b') throw new Error('QuotaExceededError'); storage.setItem(key, value); },
    };
    expect(() => commitStorageChanges(failingStorage, { a: 'new', b: 'new' })).toThrow();
    expect(storage.getItem('a')).toBe('old');
    expect(storage.getItem('unrelated')).toBe('keep');
  });
});

describe('translation boundary', () => {
  test('retains older partial imports with English deep defaults', () => {
    const translation = validateTranslation({ meta: enTemplate.meta, translations: { salats: { fajr: 'Dawn' } } });
    expect(translation.translations.salats.fajr).toBe('Dawn');
    expect(translation.translations.salats.isha).toBe(enTemplate.translations.salats.isha);
    const stored = normalizeStoredTranslations({ valid: { ...translation, id: 'valid', importedAt: '2025-01-01T00:00:00.000Z' }, invalid: { meta: 4 } });
    expect(Object.keys(stored)).toEqual(['valid']);
  });
  test('raw stored translations preserve valid imports and allow corrupt collections to be repaired', () => {
    const entry = { meta: enTemplate.meta, translations: { salats: { fajr: 'Dawn' } } };
    expect(normalizeStoredTranslations(JSON.stringify({ saved: entry })).saved.translations.salats.fajr).toBe('Dawn');
    const repaired = { ...normalizeStoredTranslations('{broken'), replacement: entry };
    expect(normalizeStoredTranslations(JSON.stringify(repaired)).replacement.translations.salats.fajr).toBe('Dawn');
  });
  test('rejects wrong shapes, malformed placeholders, direction and excessive strings', () => {
    expect(() => validateTranslation({ meta: { ...enTemplate.meta, direction: 'up' }, translations: {} })).toThrow();
    expect(() => validateTranslation({ meta: enTemplate.meta, translations: { salats: { fajr: 1 } } })).toThrow();
    expect(() => validateTranslation({ meta: enTemplate.meta, translations: { saum: { adjustedBy: '{{hours}} min' } } })).toThrow();
    expect(() => validateTranslation({ meta: enTemplate.meta, translations: { saum: { adjustedBy: '{{minutes' } } })).toThrow();
    expect(() => validateTranslation({ meta: enTemplate.meta, translations: { salats: { fajr: 'x'.repeat(8193) } } })).toThrow();
  });
  test('prototype-bearing properties are rejected', () => {
    expect(() => validateTranslation(JSON.parse('{"meta":{"languageName":"Test","languageCode":"xx","direction":"rtl","version":"1"},"translations":{"__proto__":{"fajr":"bad"}}}'))).toThrow();
  });
});

describe('font binary and latest-selection contracts', () => {
  test('rejects malformed binary headers, wrong filename and excessive size', () => {
    expect(() => validateWoff2('other.woff2', new ArrayBuffer(49))).toThrow();
    expect(() => validateWoff2('customfont.woff2', new ArrayBuffer(48))).toThrow();
    expect(() => validateWoff2('customfont.woff2', new ArrayBuffer(5 * 1024 * 1024 + 1))).toThrow();
  });
  test('legacy migration rejects malformed encodings without modifying saved data', () => {
    expect(() => decodeLegacyFont({ name: 'customfont.woff2', data: 'data:font/woff2;base64,!', timestamp: 7 })).toThrow();
  });
  test('a late prepared font cannot replace a newer working choice, and denied commits preserve the old font', async () => {
    const requests = new LatestFontRequest();
    let working = 'original';
    const old = requests.begin();
    const newer = requests.begin();
    expect(requests.commit(newer, () => { working = 'newer'; })).toBe(true);
    await Promise.resolve();
    expect(requests.commit(old, () => { working = 'late'; })).toBe(false);
    expect(working).toBe('newer');
    const denied = requests.begin();
    expect(() => requests.commit(denied, () => { throw new Error('Storage denied'); })).toThrow();
    expect(working).toBe('newer');
  });

  test('failed binary verification cleans up candidate record from store', async () => {
    const globalWithIdb = globalThis as unknown as { indexedDB: IDBFactory };
    const originalIndexedDB = globalWithIdb.indexedDB;
    try {
      const buffer = new ArrayBuffer(64);
      const view = new DataView(buffer);
      view.setUint32(0, 0x774f4632); // 'wOF2'
      view.setUint32(4, 0x00010000); // TrueType flavor
      view.setUint32(8, 64);         // byteLength
      view.setUint16(12, 1);         // numTables
      view.setUint16(14, 0);         // reserved
      view.setUint32(16, 16);        // totalSfntSize >= 12
      view.setUint32(20, 10);        // totalCompressedSize <= 64 - 48

      const store = new Map<string, FontRecord>();
      let mismatchReadback = true;
      const mockDb = {
        objectStoreNames: { contains: () => true, createObjectStore: () => {} },
        close: () => {},
        transaction: () => {
          let oncomplete: (() => void) | undefined;
          const tx = {
            set oncomplete(fn: () => void) { oncomplete = fn; },
            objectStore: () => ({
              put: (val: FontRecord) => { store.set(val.id, val); return { result: val.id }; },
              get: (id: string) => {
                let rec = store.get(id);
                if (rec && mismatchReadback) {
                  const corrupted = new Uint8Array(rec.bytes).slice();
                  corrupted[30] = (corrupted[30] ?? 0) ^ 0xff;
                  rec = { ...rec, bytes: corrupted.buffer };
                }
                return { result: rec };
              },
              delete: (id: string) => { store.delete(id); return { result: undefined }; },
            }),
          };
          queueMicrotask(() => { if (oncomplete) oncomplete(); });
          return tx;
        },
      };
      globalWithIdb.indexedDB = {
        open: () => {
          const req = {
            result: mockDb,
            onsuccess: null as (() => void) | null,
          };
          queueMicrotask(() => {
            if (req.onsuccess) req.onsuccess();
          });
          return req as unknown as IDBOpenDBRequest;
        },
      } as unknown as IDBFactory;

      const candidateRecord: FontRecord = {
        schema: 2 as const,
        id: 'test-corrupt-readback-' + Date.now(),
        name: 'customfont.woff2' as const,
        timestamp: Date.now(),
        bytes: buffer,
      };

      await expect(saveFontRecord(candidateRecord)).rejects.toThrow('Font storage verification failed');
      // Ensure that because verification failed, deleteFontRecord was called and cleaned up the store
      expect(store.has(candidateRecord.id)).toBe(false);
    } finally {
      globalWithIdb.indexedDB = originalIndexedDB;
    }
  });

  test('failed binary verification with deletion failure raises compound error', async () => {
    const globalWithIdb = globalThis as unknown as { indexedDB: IDBFactory };
    const originalIndexedDB = globalWithIdb.indexedDB;
    try {
      const buffer = new ArrayBuffer(64);
      const view = new DataView(buffer);
      view.setUint32(0, 0x774f4632); // 'wOF2'
      view.setUint32(4, 0x00010000);
      view.setUint32(8, 64);
      view.setUint16(12, 1);
      view.setUint16(14, 0);
      view.setUint32(16, 16);
      view.setUint32(20, 10);

      const store = new Map<string, FontRecord>();
      const mockDb = {
        objectStoreNames: { contains: () => true, createObjectStore: () => {} },
        close: () => {},
        transaction: () => {
          let oncomplete: (() => void) | undefined;
          const tx = {
            set oncomplete(fn: () => void) { oncomplete = fn; },
            objectStore: () => ({
              put: (val: FontRecord) => { store.set(val.id, val); return { result: val.id }; },
              get: (id: string) => {
                let rec = store.get(id);
                if (rec) {
                  const corrupted = new Uint8Array(rec.bytes).slice();
                  corrupted[30] = (corrupted[30] ?? 0) ^ 0xff;
                  rec = { ...rec, bytes: corrupted.buffer };
                }
                return { result: rec };
              },
              delete: () => { throw new Error('Database locked'); },
            }),
          };
          queueMicrotask(() => { if (oncomplete) oncomplete(); });
          return tx;
        },
      };
      globalWithIdb.indexedDB = {
        open: () => {
          const req = {
            result: mockDb,
            onsuccess: null as (() => void) | null,
          };
          queueMicrotask(() => {
            if (req.onsuccess) req.onsuccess();
          });
          return req as unknown as IDBOpenDBRequest;
        },
      } as unknown as IDBFactory;

      const candidateRecord: FontRecord = {
        schema: 2 as const,
        id: 'test-compound-error-' + Date.now(),
        name: 'customfont.woff2' as const,
        timestamp: Date.now(),
        bytes: buffer,
      };

      await expect(saveFontRecord(candidateRecord)).rejects.toThrow('Font verification failed and cleanup failed');
    } finally {
      globalWithIdb.indexedDB = originalIndexedDB;
    }
  });
});
