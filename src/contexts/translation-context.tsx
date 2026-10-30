import { toast } from '@/components/ui/use-toast';
import arTemplate from '@/i18n/template-ar.json';
import bnTemplate from '@/i18n/template-bn.json';
import enTemplate from '@/i18n/template-en.json';
import type { StoredTranslation, TranslationFile, TranslationState } from '@/types/translation';
import { setTranslationFunction } from '@/utils/time-utils';
import { commitStorageChanges, storageFailure } from '@/utils/persistent-storage';
import { normalizeStoredTranslations, validateTranslation } from '@/utils/translation-validation';
import { CUSTOM_FONT_KEY, MAX_FONT_BYTES, LatestFontRequest, decodeLegacyFont, deleteFontRecord, parseFontSelection, readFontRecord, saveFontRecord, validateWoff2 } from '@/utils/font-storage';
import type { FontRecord, StoredFont } from '@/utils/font-storage';
import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';

const TRANSLATION_STORAGE_KEY = 'muajjin-translations';
const ACTIVE_TRANSLATION_KEY = 'muajjin-active-translation';
const BUILTIN_FONTS: Record<string, string> = { en: '/fonts/Ubuntu-Regular.ttf', bn: '/fonts/NotoSerifBengali.ttf' };
const ARABIC_FONT_FAMILY = "'MuajjinArabic', system-ui, -apple-system, sans-serif";
const ARABIC_FONT_WEIGHTS = ['400', '600', '700'] as const;
const ARABIC_APP_NAME_KEYS: Record<string, true> = {
  'common.appName': true,
  'onboarding.welcome': true,
  'aboutPage.appName': true,
};
const ARABIC_APP_NAME_FIXES: Record<string, string> = { 'معجّن': 'مؤذّن' };
const BUILTIN_TRANSLATIONS: Record<string, StoredTranslation> = {
  en: { ...(enTemplate as unknown as TranslationFile), id: 'en', importedAt: new Date(0).toISOString() },
  bn: { ...(bnTemplate as unknown as TranslationFile), id: 'bn', importedAt: new Date(0).toISOString() },
  ar: { ...(arTemplate as unknown as TranslationFile), id: 'ar', importedAt: new Date(0).toISOString() },
};
interface TranslationContextType {
  t: (key: string, params?: Record<string, string | number>) => string;
  getSalatName: (salat: string) => string;
  activeTranslation: StoredTranslation | null;
  setActiveTranslation: (id: string | null) => boolean;
  importedTranslations: Record<string, StoredTranslation>;
  importTranslation: (translation: unknown) => { success: boolean; error?: string; id?: string };
  deleteTranslation: (id: string) => boolean;
  direction: 'ltr' | 'rtl';
  mounted: boolean;
  uploadFont: (file: File) => Promise<boolean>;
  removeFont: () => Promise<boolean>;
  customFont: StoredFont | null;
  storageError: string | null;
}
const TranslationContext = createContext<TranslationContextType | undefined>(undefined);
function getNestedValue(object: unknown, key: string): string | undefined {
  let value = object;
  for (const part of key.split('.')) {
    if (!value || typeof value !== 'object' || !Object.prototype.hasOwnProperty.call(value, part)) return undefined;
    value = (value as Record<string, unknown>)[part];
  }
  return typeof value === 'string' ? value : undefined;
}
function interpolate(template: string, params?: Record<string, string | number>): string {
  if (!params) return template;
  return template.replace(/\{\{(\w+)\}\}/g, (match, key: string) => params[key] === undefined ? match : String(params[key]));
}

export function TranslationProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<TranslationState>({ activeTranslationId: null, translations: {} });
  const stateRef = useRef(state);
  const [mounted, setMounted] = useState(false);
  const [customFont, setCustomFont] = useState<StoredFont | null>(null);
  const selectionRef = useRef<StoredFont | null>(null);
  const cachedFont = useRef<FontRecord | null>(null);
  const [storageError, setStorageError] = useState<string | null>(null);
  const requests = useRef(new LatestFontRequest());
  const working = useRef<{ face: FontFace | null; family: string; selection: string } | null>(null);
  const live = useRef(false);

  const report = useCallback((error: unknown, token?: number, cleanupMessage?: string) => {
    const originalMessage = error instanceof Error ? error.message : storageFailure(error);
    const message = cleanupMessage === undefined ? originalMessage : `${originalMessage}; ${cleanupMessage}`;
    if (live.current && (token === undefined || requests.current.isCurrent(token))) setStorageError(message);
    toast({ title: 'Device data or font operation failed', description: message, variant: 'destructive' });
  }, []);
  const publish = useCallback((next: TranslationState) => { stateRef.current = next; setState(next); setStorageError(null); }, []);
  const selectedLanguage = useCallback(() => {
    const id = stateRef.current.activeTranslationId;
    if (!id) return 'en';
    const active = stateRef.current.translations[id] ?? BUILTIN_TRANSLATIONS[id];
    const code = active?.meta.languageCode.toLowerCase().replace(/_/g, '-').split('-')[0];
    return code && (code === 'en' || code === 'bn' || code === 'ar') ? code : null;
  }, []);
  const prepareFont = useCallback(async (selection: StoredFont | null, token: number) => {
    let face: FontFace | null = null;
    let family = '';
    const identity = selection?.id ?? selectedLanguage() ?? 'system';
    if (selection) {
      const record = cachedFont.current?.id === selection.id ? cachedFont.current : await readFontRecord(selection);
      face = new FontFace(`MuajjinCustomFont-${token}`, record.bytes);
      await face.load();
      family = `'${face.family}', system-ui, -apple-system, sans-serif`;
    } else {
      const language = selectedLanguage();
      if (language === 'ar') {
        await Promise.all(ARABIC_FONT_WEIGHTS.map((weight) => document.fonts.load(`${weight} 16px "MuajjinArabic"`)));
        family = ARABIC_FONT_FAMILY;
      } else if (language) {
        face = new FontFace(`MuajjinBuiltinFont-${language}-${token}`, `url(${BUILTIN_FONTS[language]})`);
        await face.load();
        family = `'${face.family}', system-ui, -apple-system, sans-serif`;
      }
    }
    return { face, family, selection: identity };
  }, [selectedLanguage]);
  const activateFont = useCallback((prepared: { face: FontFace | null; family: string; selection: string }) => {
    const previous = working.current;
    if (prepared.face) document.fonts.add(prepared.face);
    document.documentElement.style.fontFamily = prepared.family;
    working.current = prepared;
    if (previous?.face && previous.face !== prepared.face) document.fonts.delete(previous.face);
  }, []);
  const cleanupRecord = useCallback(async (id: string, token: number) => {
    try {
      try {
        const raw = localStorage.getItem(CUSTOM_FONT_KEY);
        if (raw && parseFontSelection(JSON.parse(raw))?.id === id) return;
      } catch (storageError) {
        // If localStorage is unavailable, do not infer orphanhood; preserve the record
        report(storageError, token);
        return;
      }
      await deleteFontRecord(id);
    } catch (error) { report(error, token); }
  }, [report]);

  const loadSavedFont = useCallback(async (raw: string | null) => {
    const token = requests.current.begin();
    try {
      if (!raw) {
        const prepared = await prepareFont(null, token);
        requests.current.commit(token, () => { activateFont(prepared); selectionRef.current = null; cachedFont.current = null; setCustomFont(null); });
        return;
      }
      const value: unknown = JSON.parse(raw);
      const pointer = parseFontSelection(value);
      if (pointer) {
        const record = await readFontRecord(pointer);
        if (!requests.current.isCurrent(token)) return;
        cachedFont.current = record;
        const prepared = await prepareFont(pointer, token);
        requests.current.commit(token, () => { activateFont(prepared); selectionRef.current = pointer; setCustomFont(pointer); });
        return;
      }
      // Keep legacy data until the font loads and a separately committed binary record reads back byte-for-byte.
      const legacy = decodeLegacyFont(value);
      const record: FontRecord = { schema: 2, id: crypto.randomUUID(), ...legacy };
      const face = new FontFace(`MuajjinCustomFont-${token}`, record.bytes);
      await face.load();
      if (!requests.current.isCurrent(token)) return;
      requests.current.commit(token, () => {
        cachedFont.current = record; selectionRef.current = record; setCustomFont(record);
        activateFont({ face, family: `'${face.family}', system-ui, -apple-system, sans-serif`, selection: record.id });
      });
      let committed = false;
      try {
        await saveFontRecord(record);
        if (!requests.current.isCurrent(token) || localStorage.getItem(CUSTOM_FONT_KEY) !== raw) {
          try {
            await deleteFontRecord(record.id);
          } catch (cleanupError) {
            report(cleanupError, token);
          }
          return;
        }
        const metadata: StoredFont = { schema: 2, id: record.id, name: record.name, timestamp: record.timestamp };
        localStorage.setItem(CUSTOM_FONT_KEY, JSON.stringify(metadata));
        committed = true;
        if (live.current && requests.current.isCurrent(token)) setStorageError(null);
      } catch (error) {
        if (!committed) {
          try {
            await deleteFontRecord(record.id);
          } catch (cleanupError) {
            report(error, token, cleanupError instanceof Error ? cleanupError.message : storageFailure(cleanupError));
            return;
          }
        }
        // IDB/permission/quota failure leaves the loaded working legacy font and its original base64 untouched.
        if (requests.current.isCurrent(token)) report(error, token);
      }
    } catch (error) { if (requests.current.isCurrent(token)) report(error); }
  }, [activateFont, prepareFont, report]);

  useEffect(() => {
    live.current = true;
    const requestTracker = requests.current;
    const loadTranslations = () => {
      try {
        const raw = localStorage.getItem(TRANSLATION_STORAGE_KEY);
        const translations = normalizeStoredTranslations(raw);
        const requested = localStorage.getItem(ACTIVE_TRANSLATION_KEY);
        const activeTranslationId = requested && (translations[requested] || Object.prototype.hasOwnProperty.call(BUILTIN_TRANSLATIONS, requested)) ? requested : null;
        publish({ translations, activeTranslationId });
      } catch (error) { report(error); }
    };
    loadTranslations();
    try { void loadSavedFont(localStorage.getItem(CUSTOM_FONT_KEY)); } catch (error) { report(error); }
    setMounted(true);
    const receive = (event: StorageEvent) => {
      const previousId = stateRef.current.activeTranslationId;
      if (event.key === TRANSLATION_STORAGE_KEY || event.key === ACTIVE_TRANSLATION_KEY || event.key === null) loadTranslations();
      try {
        if (event.key === CUSTOM_FONT_KEY || event.key === null || previousId !== stateRef.current.activeTranslationId) {
          void loadSavedFont(localStorage.getItem(CUSTOM_FONT_KEY));
        }
      } catch (error) { report(error); }
    };
    window.addEventListener('storage', receive);
    return () => {
      live.current = false;
      requestTracker.begin();
      window.removeEventListener('storage', receive);
      if (working.current?.face) document.fonts.delete(working.current.face);
      working.current = null;
      document.documentElement.style.fontFamily = '';
    };
  }, [loadSavedFont, publish, report]);

  const activeTranslation = state.activeTranslationId ? state.translations[state.activeTranslationId] || BUILTIN_TRANSLATIONS[state.activeTranslationId] || null : null;
  const direction = activeTranslation?.meta.direction ?? 'ltr';
  const languageCode = (activeTranslation?.meta.languageCode ?? 'en').replace(/_/g, '-');
  const baseLanguage = languageCode.toLowerCase().split('-')[0];
  useEffect(() => {
    const root = document.documentElement;
    const previous = { lang: root.lang, dir: root.dir };
    root.lang = languageCode;
    root.dir = direction;
    return () => {
      root.lang = previous.lang;
      root.dir = previous.dir;
    };
  }, [direction, languageCode]);
  const t = useCallback((key: string, params?: Record<string, string | number>) => {
    const imported = getNestedValue(activeTranslation?.translations, key);
    const english = getNestedValue(enTemplate.translations, key);
    const arabic = baseLanguage === 'ar' ? getNestedValue(arTemplate.translations, key) : undefined;
    let translated = imported?.trim() ? imported : undefined;
    // Older Arabic imports were deep-filled with English defaults. Replace only
    // values that still equal the English default; keep user-supplied wording.
    if (baseLanguage === 'ar' && (!translated || translated === english)) {
      translated = arabic?.trim() ? arabic : translated;
    }
    if (!translated) translated = english;
    if (baseLanguage === 'ar' && translated && ARABIC_APP_NAME_KEYS[key]) {
      translated = ARABIC_APP_NAME_FIXES[translated] ?? translated;
    }
    return translated ? interpolate(translated, params) : key;
  }, [activeTranslation, baseLanguage]);
  useEffect(() => { setTranslationFunction(t); }, [t]);

  const setActiveTranslation = useCallback((id: string | null): boolean => {
    const previous = stateRef.current;
    if (id && !previous.translations[id] && !Object.prototype.hasOwnProperty.call(BUILTIN_TRANSLATIONS, id)) { report(new Error('Unknown translation selection')); return false; }
    try {
      commitStorageChanges(localStorage, { [ACTIVE_TRANSLATION_KEY]: id });
      requests.current.begin();
      publish({ ...previous, activeTranslationId: id });
      try { void loadSavedFont(localStorage.getItem(CUSTOM_FONT_KEY)); } catch (error) { report(error); }
      return true;
    } catch (error) { report(error); return false; }
  }, [loadSavedFont, publish, report]);
  const importTranslation = useCallback((value: unknown): { success: boolean; error?: string; id?: string } => {
    try {
      const translation = validateTranslation(value);
      const id = `${translation.meta.languageCode}-${crypto.randomUUID()}`;
      // Refresh durable entries before merging so imports in another tab are not accidentally discarded.
      const raw = localStorage.getItem(TRANSLATION_STORAGE_KEY);
      const translations = normalizeStoredTranslations(raw);
      const next = { ...translations, [id]: { ...translation, id, importedAt: new Date().toISOString() } };
      const serialized = JSON.stringify(next);
      if (new TextEncoder().encode(serialized).byteLength > 4 * 1024 * 1024) throw new Error('Saved translations are too large. Remove an unused import before adding another.');
      localStorage.setItem(TRANSLATION_STORAGE_KEY, serialized);
      publish({ ...stateRef.current, translations: next });
      return { success: true, id };
    } catch (error) {
      const message = error instanceof Error ? error.message : storageFailure(error);
      report(error);
      return { success: false, error: message };
    }
  }, [publish, report]);
  const deleteTranslation = useCallback((id: string): boolean => {
    try {
      const raw = localStorage.getItem(TRANSLATION_STORAGE_KEY);
      const translations = normalizeStoredTranslations(raw);
      delete translations[id];
      const requested = localStorage.getItem(ACTIVE_TRANSLATION_KEY);
      const activeTranslationId = requested === id ? null : requested;
      commitStorageChanges(localStorage, { [TRANSLATION_STORAGE_KEY]: JSON.stringify(translations), [ACTIVE_TRANSLATION_KEY]: activeTranslationId });
      const languageChanged = stateRef.current.activeTranslationId !== activeTranslationId;
      publish({ translations, activeTranslationId });
      if (languageChanged) {
        requests.current.begin();
        try { void loadSavedFont(localStorage.getItem(CUSTOM_FONT_KEY)); } catch (error) { report(error); }
      }
      return true;
    } catch (error) { report(error); return false; }
  }, [loadSavedFont, publish, report]);

  const uploadFont = useCallback(async (file: File): Promise<boolean> => {
    const token = requests.current.begin();
    let candidate: FontRecord | null = null;
    let committed = false;
    try {
      if (file.name !== 'customfont.woff2') throw new Error('Font file must be named exactly "customfont.woff2"');
      if (file.size > MAX_FONT_BYTES) throw new Error('Font file too large. Maximum size is 5MB.');
      const bytes = await file.arrayBuffer();
      validateWoff2(file.name, bytes);
      candidate = { schema: 2, id: crypto.randomUUID(), name: 'customfont.woff2', timestamp: Date.now(), bytes };
      const face = new FontFace(`MuajjinCustomFont-${token}`, bytes);
      await face.load();
      if (!requests.current.isCurrent(token)) return false;
      await saveFontRecord(candidate);
      if (!requests.current.isCurrent(token)) {
        try {
          await deleteFontRecord(candidate.id);
        } catch (cleanupError) {
          report(cleanupError, token);
        }
        return false;
      }
      const metadata: StoredFont = { schema: 2, id: candidate.id, name: candidate.name, timestamp: candidate.timestamp };
      const previous = selectionRef.current;
      requests.current.commit(token, () => {
        // Replacing this one small pointer is atomic; until it succeeds the old bytes, face and selection remain intact.
        localStorage.setItem(CUSTOM_FONT_KEY, JSON.stringify(metadata));
        committed = true;
        activateFont({ face, family: `'${face.family}', system-ui, -apple-system, sans-serif`, selection: metadata.id });
        cachedFont.current = candidate; selectionRef.current = metadata; setCustomFont(metadata); setStorageError(null);
      });
      toast({ title: t('settings.fontUploaded'), description: t('settings.fontUploadedDesc') });
      if (previous && previous.id !== metadata.id) void cleanupRecord(previous.id, token);
      return true;
    } catch (error) {
      if (candidate && !committed) {
        try {
          await deleteFontRecord(candidate.id);
        } catch (cleanupError) {
          report(error, token, cleanupError instanceof Error ? cleanupError.message : storageFailure(cleanupError));
          return false;
        }
      }
      if (requests.current.isCurrent(token)) report(error, token);
      return false;
    }
  }, [activateFont, cleanupRecord, report, t]);
  const removeFont = useCallback(async (): Promise<boolean> => {
    const token = requests.current.begin();
    try {
      const prepared = await prepareFont(null, token);
      if (!requests.current.isCurrent(token)) return false;
      const previous = selectionRef.current;
      requests.current.commit(token, () => {
        localStorage.removeItem(CUSTOM_FONT_KEY);
        activateFont(prepared); selectionRef.current = null; cachedFont.current = null; setCustomFont(null); setStorageError(null);
      });
      toast({ title: t('settings.fontRemoved'), description: t('settings.fontRemovedDesc') });
      if (previous) void cleanupRecord(previous.id, token);
      return true;
    } catch (error) { if (requests.current.isCurrent(token)) report(error); return false; }
  }, [activateFont, cleanupRecord, prepareFont, report, t]);

  return (
    <TranslationContext.Provider value={{ t, getSalatName: (salat) => t(`salats.${salat.toLowerCase()}`), activeTranslation, setActiveTranslation, importedTranslations: state.translations, importTranslation, deleteTranslation, direction, mounted, uploadFont, removeFont, customFont, storageError }}>
      {!mounted ? <div className="flex min-h-screen items-center justify-center"><div className="h-10 w-10 animate-pulse rounded-full bg-secondary" /></div> : <div dir={direction} lang={languageCode}>{children}</div>}
    </TranslationContext.Provider>
  );
}
export function useTranslation() {
  const context = useContext(TranslationContext);
  if (context === undefined) throw new Error('useTranslation must be used within a TranslationProvider');
  return context;
}
