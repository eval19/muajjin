import { z } from 'zod';
import arTemplate from '../i18n/template-ar.json';
import enTemplate from '../i18n/template-en.json';
import type { StoredTranslation, TranslationFile } from '../types/translation';

export const MAX_TRANSLATION_BYTES = 1024 * 1024;
const MAX_STRING_LENGTH = 8192;
const forbidden: Record<'__proto__' | 'prototype' | 'constructor', true> = { ['__proto__']: true, prototype: true, constructor: true };
const metaSchema = z.object({
  languageName: z.string().trim().min(1).max(128),
  languageCode: z.string().regex(/^[a-zA-Z0-9_-]{1,64}$/).default('und'),
  direction: z.enum(['ltr', 'rtl']),
  version: z.string().min(1).max(64).default('1.0'),
});
const objectSchema = z.record(z.unknown());
const translationInputSchema = z.object({ meta: metaSchema, translations: objectSchema });
function placeholders(value: string): string[] {
  const matches = [...value.matchAll(/\{\{(\w+)\}\}/g)].map((match) => match[1]);
  const remaining = value.replace(/\{\{\w+\}\}/g, '');
  if (remaining.includes('{{') || remaining.includes('}}')) throw new Error('Malformed translation placeholder');
  return [...new Set(matches)].sort();
}

export function validateTranslation(value: unknown): TranslationFile {
  const parsed = translationInputSchema.safeParse(value);
  if (!parsed.success) throw new Error('Invalid translation metadata or file structure');
  try { Intl.getCanonicalLocales(parsed.data.meta.languageCode.replace(/_/g, '-')); }
  catch { throw new Error('Invalid translation language code; use a locale such as en, bn or ar'); }
  const input = value as { translations: Record<string, unknown> };
  const baseLanguage = parsed.data.meta.languageCode.toLowerCase().replace(/_/g, '-').split('-')[0];
  const fill = baseLanguage === 'ar' ? arTemplate.translations : enTemplate.translations;
  let leaves = 0;
  let characters = 0;
  const merge = (source: Record<string, unknown>, defaults: Record<string, unknown>, depth: number, fillSource: Record<string, unknown>): Record<string, unknown> => {
    if (depth > 8) throw new Error('Translation nesting is too deep');
    const result: Record<string, unknown> = {};
    for (const key of new Set([...Object.keys(defaults), ...Object.keys(source)])) {
      if (Object.prototype.hasOwnProperty.call(forbidden, key) || key.length > 128) throw new Error('Invalid translation property');
      if (++leaves > 2000) throw new Error('Translation contains too many values');
      const supplied = Object.prototype.hasOwnProperty.call(source, key);
      const fillValue = Object.prototype.hasOwnProperty.call(fillSource, key) ? fillSource[key] : undefined;
      const current = supplied ? source[key] : (fillValue ?? defaults[key]);
      const fallback = defaults[key];
      const childFill = fillValue !== null && typeof fillValue === 'object' && !Array.isArray(fillValue) ? fillValue as Record<string, unknown> : {};
      if (typeof current === 'string') {
        if (fallback !== null && typeof fallback === 'object') throw new Error(`Translation section ${key} must be an object`);
        if (current.length > MAX_STRING_LENGTH) throw new Error(`Translation value ${key} is too long`);
        characters += current.length;
        if (characters > MAX_TRANSLATION_BYTES) throw new Error('Translation file is too large');
        const actual = placeholders(current);
        if (typeof fallback === 'string' && actual.join(',') !== placeholders(fallback).join(',')) throw new Error(`Translation placeholders do not match for ${key}`);
        result[key] = current;
      } else if (current !== null && typeof current === 'object' && !Array.isArray(current) && typeof fallback !== 'string') {
        result[key] = merge(current as Record<string, unknown>, fallback !== null && typeof fallback === 'object' ? fallback as Record<string, unknown> : {}, depth + 1, childFill);
      } else throw new Error(`Translation value ${key} must be a string or a translation section`);
    }
    return result;
  };
  // Inspect original own keys: schema parsing must not silently strip hostile property names.
  const result = { meta: parsed.data.meta, translations: merge(input.translations, enTemplate.translations, 0, fill as Record<string, unknown>) };
  if (new TextEncoder().encode(JSON.stringify(result)).byteLength > MAX_TRANSLATION_BYTES) throw new Error('Translation file is too large (maximum 1 MB)');
  // Every known leaf is now a validated string. Arabic imports fill from the
  // Arabic template; other languages fill from English.
  return result as unknown as TranslationFile;
}

export function normalizeStoredTranslations(value: unknown): Record<string, StoredTranslation> {
  if (typeof value === 'string') {
    try { value = JSON.parse(value) as unknown; }
    catch (error) {
      if (!(error instanceof SyntaxError)) throw error;
      // Keep damaged originals until a validated import explicitly repairs them.
      return {};
    }
  }
  const parsed = objectSchema.safeParse(value);
  if (!parsed.success) return {};
  const result: Record<string, StoredTranslation> = {};
  for (const [id, entry] of Object.entries(parsed.data)) {
    if (Object.prototype.hasOwnProperty.call(forbidden, id) || id.length > 128) continue;
    try {
      const translation = validateTranslation(entry);
      const metadata = z.object({ importedAt: z.string().optional() }).parse(entry);
      const importedAt = metadata.importedAt && Number.isFinite(Date.parse(metadata.importedAt)) ? metadata.importedAt : new Date(0).toISOString();
      result[id] = { ...translation, id, importedAt };
    } catch { /* Invalid entries cannot prevent independent valid imports from loading. Originals stay in storage until an explicit edit. */ }
  }
  return result;
}
