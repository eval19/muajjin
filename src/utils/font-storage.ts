import { z } from 'zod';

export const CUSTOM_FONT_KEY = 'muajjin-custom-font';
export const MAX_FONT_BYTES = 5 * 1024 * 1024;
const FONT_DATABASE = 'muajjin-fonts';
const FONT_STORE = 'fonts';
const fontMetadataSchema = z.object({
  schema: z.literal(2), id: z.string().min(1).max(128), name: z.literal('customfont.woff2'), timestamp: z.number().finite().nonnegative(),
});
export type StoredFont = z.infer<typeof fontMetadataSchema>;
export interface FontRecord extends StoredFont { bytes: ArrayBuffer }
const legacyFontSchema = z.object({ name: z.literal('customfont.woff2'), data: z.string().max(Math.ceil(MAX_FONT_BYTES / 3) * 4 + 256), timestamp: z.number().finite().nonnegative() });

export function parseFontSelection(value: unknown): StoredFont | null {
  const parsed = fontMetadataSchema.safeParse(value);
  return parsed.success ? parsed.data : null;
}
export function validateWoff2(name: string, bytes: ArrayBuffer): void {
  if (name !== 'customfont.woff2') throw new Error('Font file must be named exactly "customfont.woff2"');
  if (bytes.byteLength > MAX_FONT_BYTES) throw new Error('Font file too large. Maximum size is 5MB.');
  if (bytes.byteLength < 48) throw new Error('Invalid WOFF2 font content');
  const header = new DataView(bytes);
  const flavor = header.getUint32(4);
  if (header.getUint32(0) !== 0x774f4632 || ![0x00010000, 0x4f54544f, 0x74746366].includes(flavor) || header.getUint32(8) !== bytes.byteLength || header.getUint16(12) === 0 || header.getUint16(12) > 4096 || header.getUint16(14) !== 0 || header.getUint32(16) < 12 || header.getUint32(20) === 0 || header.getUint32(20) > bytes.byteLength - 48) {
    throw new Error('Invalid WOFF2 font content');
  }
}
export function decodeLegacyFont(value: unknown): { name: 'customfont.woff2'; timestamp: number; bytes: ArrayBuffer } {
  const legacy = legacyFontSchema.parse(value);
  const encoded = /^data:[^,]*;base64,([A-Za-z0-9+/]*={0,2})$/.exec(legacy.data)?.[1];
  if (!encoded || encoded.length % 4 !== 0) throw new Error('Invalid stored font encoding');
  const decoded = atob(encoded);
  const bytes = new Uint8Array(decoded.length);
  for (let index = 0; index < decoded.length; index++) bytes[index] = decoded.charCodeAt(index);
  validateWoff2(legacy.name, bytes.buffer);
  return { name: legacy.name, timestamp: legacy.timestamp, bytes: bytes.buffer };
}

export class LatestFontRequest {
  private generation = 0;
  begin(): number { return ++this.generation; }
  isCurrent(generation: number): boolean { return this.generation === generation; }
  commit(generation: number, operation: () => void): boolean {
    if (!this.isCurrent(generation)) return false;
    operation();
    return true;
  }
}

function openFonts(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(FONT_DATABASE, 1);
    let blocked = false;
    request.onupgradeneeded = () => { if (!request.result.objectStoreNames.contains(FONT_STORE)) request.result.createObjectStore(FONT_STORE, { keyPath: 'id' }); };
    request.onerror = () => reject(request.error ?? new Error('Font storage unavailable'));
    request.onblocked = () => { blocked = true; reject(new Error('Font storage is blocked by another open app window')); };
    request.onsuccess = () => {
      if (blocked) { request.result.close(); return; }
      request.result.onversionchange = () => request.result.close();
      resolve(request.result);
    };
  });
}
async function fontTransaction<T>(mode: IDBTransactionMode, operation: (store: IDBObjectStore) => IDBRequest<T>): Promise<T> {
  const database = await openFonts();
  try {
    return await new Promise<T>((resolve, reject) => {
      const transaction = database.transaction(FONT_STORE, mode, mode === 'readwrite' ? { durability: 'strict' } : undefined);
      const request = operation(transaction.objectStore(FONT_STORE));
      transaction.oncomplete = () => resolve(request.result);
      transaction.onabort = () => reject(transaction.error ?? request.error ?? new Error('Font storage transaction aborted'));
      transaction.onerror = () => reject(transaction.error ?? new Error('Font storage unavailable'));
    });
  } finally { database.close(); }
}
export async function readFontRecord(selection: StoredFont): Promise<FontRecord> {
  const value: unknown = await fontTransaction('readonly', (store) => store.get(selection.id));
  const parsed = fontMetadataSchema.extend({ bytes: z.instanceof(ArrayBuffer) }).parse(value) as FontRecord;
  if (parsed.id !== selection.id || parsed.timestamp !== selection.timestamp || parsed.name !== selection.name) throw new Error('Saved custom font does not match its selection');
  validateWoff2(parsed.name, parsed.bytes);
  return parsed;
}
export async function deleteFontRecord(id: string): Promise<void> {
  await fontTransaction('readwrite', (store) => store.delete(id));
}
export async function saveFontRecord(record: FontRecord): Promise<void> {
  validateWoff2(record.name, record.bytes);
  await fontTransaction('readwrite', (store) => store.put(record));
  try {
    // Read in a separate completed transaction before replacing the selected pointer or deleting legacy base64.
    const saved = await readFontRecord(record);
    const expected = new Uint8Array(record.bytes);
    const actual = new Uint8Array(saved.bytes);
    if (actual.length !== expected.length) throw new Error('Font storage verification failed');
    for (let index = 0; index < actual.length; index++) {
      if (actual[index] !== expected[index]) throw new Error('Font storage verification failed');
    }
  } catch (error) {
    try {
      await deleteFontRecord(record.id);
    } catch (cleanupError) {
      console.error('Failed to cleanup uncommitted font record:', cleanupError);
      throw new Error(`Font verification failed and cleanup failed: ${error instanceof Error ? error.message : String(error)}; cleanup error: ${cleanupError instanceof Error ? cleanupError.message : String(cleanupError)}`);
    }
    throw error;
  }
}
