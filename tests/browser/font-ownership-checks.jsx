import React, { useLayoutEffect } from 'react';
import { createRoot } from 'react-dom/client';
import { TranslationProvider, useTranslation } from '/src/contexts/translation-context.tsx';
import { useToast } from '/src/hooks/use-toast.ts';
import { CUSTOM_FONT_KEY, saveFontRecord } from '/src/utils/font-storage.ts';
import enTemplate from '/src/i18n/template-en.json';

const DATABASE = 'muajjin-fonts';
const OWNER = 'muajjin-font-qa-owned';
const SENTINEL = 'qa-font-unrelated-sentinel';
const observers = new Set();
const unhandled = [];
const restorations = [];
const releases = [];
let root;
let current;
let fixture;
let busy = false;

window.addEventListener('unhandledrejection', (event) => {
  unhandled.push(String(event.reason));
});

function check(condition, message) {
  if (!condition) throw new Error(message);
}

function Probe() {
  const api = useTranslation();
  const { toasts } = useToast();
  useLayoutEffect(() => {
    current = { api, toasts };
    for (const observer of observers) observer();
  });
  return <pre>{JSON.stringify({
    selected: api.customFont?.id ?? null,
    error: api.storageError,
    notices: toasts.map((entry) => entry.description),
  }, null, 2)}</pre>;
}

function waitFor(predicate, description) {
  if (predicate()) return Promise.resolve();
  const pending = Promise.withResolvers();
  const receive = () => {
    if (predicate()) {
      clearTimeout(timeout);
      observers.delete(receive);
      pending.resolve();
    }
  };
  const timeout = setTimeout(() => {
    observers.delete(receive);
    pending.reject(new Error(`Timed out: ${description}`));
  }, 10_000);
  observers.add(receive);
  receive();
  return pending.promise;
}

async function withDeadline(promise, description) {
  const expired = Promise.withResolvers();
  const timeout = setTimeout(() => {
    expired.reject(new Error(`Timed out: ${description}`));
  }, 10_000);
  try { return await Promise.race([promise, expired.promise]); }
  finally { clearTimeout(timeout); }
}

async function ensureOwnedOrigin() {
  check(['localhost', '127.0.0.1'].includes(location.hostname),
    'Use an explicitly approved, dedicated localhost origin.');
  if (sessionStorage.getItem(OWNER)) return;
  check(Object.keys(localStorage).length === 0 &&
    (await indexedDB.databases()).length === 0,
  'Origin is not empty. Choose a fresh port; existing data was not cleared.');
  sessionStorage.setItem(OWNER, crypto.randomUUID());
}

async function mount() {
  if (!root) {
    root = createRoot(document.getElementById('qa-provider'));
    root.render(<TranslationProvider><Probe /></TranslationProvider>);
  }
  await waitFor(() => current?.api.mounted, 'production provider mount');
}

async function remount() {
  root?.unmount();
  root = undefined;
  current = undefined;
  await mount();
}

async function records() {
  if (!(await indexedDB.databases()).some((db) => db.name === DATABASE)) return [];
  const opened = Promise.withResolvers();
  const request = indexedDB.open(DATABASE, 1);
  request.onsuccess = () => opened.resolve(request.result);
  request.onerror = () => opened.reject(request.error);
  const db = await opened.promise;
  try {
    const done = Promise.withResolvers();
    const tx = db.transaction('fonts', 'readonly');
    const read = tx.objectStore('fonts').getAll();
    tx.oncomplete = () => done.resolve(read.result);
    tx.onabort = () => done.reject(tx.error ?? new Error('QA inspection aborted'));
    return await done.promise;
  } finally { db.close(); }
}

function selection(raw = localStorage.getItem(CUSTOM_FONT_KEY)) {
  const parsed = raw ? JSON.parse(raw) : null;
  return parsed?.schema === 2 ? parsed : null;
}

async function inspect() {
  const raw = localStorage.getItem(CUSTOM_FONT_KEY);
  return {
    selected: current?.api.customFont?.id ?? null,
    pointer: selection(raw),
    legacyCharacters: raw && !selection(raw) ? raw.length : 0,
    storageError: current?.api.storageError ?? null,
    notices: current?.toasts.map((entry) => entry.description) ?? [],
    records: (await records()).map((record) => ({
      id: record.id, bytes: record.bytes.byteLength, timestamp: record.timestamp,
    })),
    unhandled: [...unhandled],
  };
}

async function baseline() {
  check(fixture?.name === 'customfont.woff2', 'Choose a valid customfont.woff2 fixture.');
  await mount();
  check(await current.api.uploadFont(fixture) === true, 'Valid baseline upload failed.');
  const pointer = selection();
  check(pointer !== null, 'Successful upload did not persist a font pointer.');
  await waitFor(() => current.api.customFont?.id === pointer?.id &&
    current.api.storageError === null, 'selected baseline commit');
  const existing = await records();
  check(existing.some((record) => record.id === pointer.id &&
    record.bytes.byteLength === fixture.size), 'Baseline record is not durable.');
  if (!existing.some((record) => record.id === SENTINEL)) {
    await saveFontRecord({
      schema: 2, id: SENTINEL, name: 'customfont.woff2', timestamp: 1,
      bytes: await fixture.arrayBuffer(),
    });
  }
  return pointer;
}

function replace(target, key, value) {
  const original = target[key];
  target[key] = value;
  restorations.push(() => { target[key] = original; });
  return original;
}

function inject(mode, deletionId) {
  const state = { candidate: null, pointerAttempts: 0, readbacks: 0, activationFailed: false };
  const nativeGet = Storage.prototype.getItem;
  state.pointer = () => selection(nativeGet.call(localStorage, CUSTOM_FONT_KEY));
  const put = IDBObjectStore.prototype.put;
  replace(IDBObjectStore.prototype, 'put', function (record, ...args) {
    if (this.name === 'fonts') state.candidate ??= record.id;
    return put.call(this, record, ...args);
  });
  const get = IDBObjectStore.prototype.get;
  const result = Object.getOwnPropertyDescriptor(IDBRequest.prototype, 'result');
  replace(IDBObjectStore.prototype, 'get', function (...args) {
    if (this.name === 'fonts') {
      state.readbacks++;
      if (mode === 'readbackDenied') throw new DOMException('QA readback denied', 'UnknownError');
    }
    const request = get.apply(this, args);
    if (this.name === 'fonts' && mode === 'readbackMismatch') {
      Object.defineProperty(request, 'result', {
        get() {
          const record = result.get.call(request);
          if (!record) return record;
          const bytes = record.bytes.slice(0);
          new Uint8Array(bytes)[30] ^= 255;
          return { ...record, bytes };
        },
      });
    }
    return request;
  });
  const set = Storage.prototype.setItem;
  replace(Storage.prototype, 'setItem', function (key, value) {
    if (key === CUSTOM_FONT_KEY && JSON.parse(value).schema === 2) {
      state.pointerAttempts++;
      if (['pointerDenied', 'compoundFailure'].includes(mode)) {
        throw new DOMException('QA pointer write denied', 'QuotaExceededError');
      }
    }
    return set.call(this, key, value);
  });
  if (mode === 'pointerReadDenied') {
    replace(Storage.prototype, 'getItem', function (key) {
      if (key === CUSTOM_FONT_KEY) throw new DOMException('QA pointer read denied', 'SecurityError');
      return nativeGet.call(this, key);
    });
  }
  const remove = IDBObjectStore.prototype.delete;
  replace(IDBObjectStore.prototype, 'delete', function (id) {
    if (this.name === 'fonts' && (mode === 'compoundFailure' || id === deletionId)) {
      throw new DOMException('QA candidate cleanup denied', 'UnknownError');
    }
    return remove.call(this, id);
  });
  const add = FontFaceSet.prototype.add;
  replace(FontFaceSet.prototype, 'add', function (face) {
    if (mode === 'postCommitFailure' && !state.activationFailed &&
        face.family.startsWith('MuajjinCustomFont-')) {
      state.activationFailed = true;
      throw new Error('QA activation failed after pointer commit');
    }
    return add.call(this, face);
  });
  if (mode === 'postCommitReportingFailure') {
    const labels = enTemplate.translations.settings;
    const original = Object.getOwnPropertyDescriptor(labels, 'fontUploaded');
    let failed = false;
    Object.defineProperty(labels, 'fontUploaded', {
      configurable: true,
      get() {
        if (!failed) {
          failed = true;
          throw new Error('QA success reporting failed after pointer commit');
        }
        return original.value;
      },
    });
    restorations.push(() => Object.defineProperty(labels, 'fontUploaded', original));
  }
  return state;
}

function holdWriteCompletion() {
  const ready = Promise.withResolvers();
  const native = IDBDatabase.prototype.transaction;
  const setter = Object.getOwnPropertyDescriptor(IDBTransaction.prototype, 'oncomplete').set;
  let first = true;
  let release;
  let cancelled = false;
  replace(IDBDatabase.prototype, 'transaction', function (...args) {
    const tx = native.apply(this, args);
    if (first && args[0] === 'fonts' && args[1] === 'readwrite') {
      first = false;
      Object.defineProperty(tx, 'oncomplete', {
        configurable: true,
        set(callback) {
          setter.call(tx, (event) => {
            if (cancelled) callback.call(tx, event);
            else { release = () => callback.call(tx, event); ready.resolve(); }
          });
        },
      });
    }
    return tx;
  });
  releases.push(() => { cancelled = true; release?.(); release = undefined; });
  return {
    ready: ready.promise,
    release() { const callback = release; release = undefined; callback?.(); },
  };
}

function holdRetirementOpen(openIndex) {
  const ready = Promise.withResolvers();
  const native = IDBFactory.prototype.open;
  const setter = Object.getOwnPropertyDescriptor(IDBRequest.prototype, 'onsuccess').set;
  let count = 0;
  let release;
  let cancelled = false;
  replace(IDBFactory.prototype, 'open', function (...args) {
    const request = native.apply(this, args);
    if (args[0] === DATABASE && ++count === openIndex) {
      Object.defineProperty(request, 'onsuccess', {
        configurable: true,
        set(callback) {
          setter.call(request, (event) => {
            if (cancelled) callback.call(request, event);
            else { release = () => callback.call(request, event); ready.resolve(); }
          });
        },
      });
    }
    return request;
  });
  releases.push(() => { cancelled = true; release?.(); release = undefined; });
  return {
    ready: ready.promise,
    release() { const callback = release; release = undefined; callback?.(); },
  };
}

// Await real IDB work across promise continuations, without an arbitrary sleep.
function observeFontTransactions() {
  let pending = 0;
  let waiting = false;
  const idle = Promise.withResolvers();
  const channel = new MessageChannel();
  const schedule = () => {
    if (waiting && pending === 0) channel.port2.postMessage(null);
  };
  channel.port1.onmessage = () => {
    if (!waiting || pending !== 0) return;
    waiting = false;
    channel.port1.close();
    channel.port2.close();
    idle.resolve();
  };
  const track = (target, events) => {
    pending++;
    let finished = false;
    const done = () => {
      if (finished) return;
      finished = true;
      pending--;
      schedule();
    };
    for (const event of events) target.addEventListener(event, done, { once: true });
  };
  const open = IDBFactory.prototype.open;
  replace(IDBFactory.prototype, 'open', function (name, ...args) {
    const request = open.call(this, name, ...args);
    if (name === DATABASE) track(request, ['success', 'error']);
    return request;
  });
  const transaction = IDBDatabase.prototype.transaction;
  replace(IDBDatabase.prototype, 'transaction', function (...args) {
    const tx = transaction.apply(this, args);
    if (this.name === DATABASE) track(tx, ['complete', 'abort']);
    return tx;
  });
  restorations.push(() => {
    waiting = false;
    channel.port1.close();
    channel.port2.close();
  });
  return {
    wait() {
      waiting = true;
      schedule();
      return withDeadline(idle.promise, 'font transactions to settle');
    },
  };
}

async function legacyValue() {
  const bytes = new Uint8Array(await fixture.arrayBuffer());
  let binary = '';
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return JSON.stringify({
    name: 'customfont.woff2', timestamp: 2,
    data: `data:font/woff2;base64,${btoa(binary)}`,
  });
}

async function uploadFailure(mode) {
  const previous = await baseline();
  const before = (await records()).map((record) => record.id).sort();
  const fault = inject(mode);
  const outcome = await current.api.uploadFont(fixture);
  await waitFor(() => current.api.storageError !== null, 'reported upload failure');
  const after = await records();
  check(fault.candidate && fault.readbacks > 0, 'Failure preceded a real verified-write boundary.');
  check(fault.pointer()?.id === previous.id, 'Failed upload changed the durable selection.');
  check(JSON.stringify(after.map((record) => record.id).sort()) === JSON.stringify(before),
    'Uncommitted candidate was retained or an unrelated record was removed.');
  check(outcome === false, 'Failed upload did not return its promised false outcome.');
}

async function committedFailure(mode) {
  const previous = await baseline();
  const fault = inject(mode);
  const outcome = await current.api.uploadFont(fixture);
  const pointer = fault.pointer();
  const stored = await records();
  check(pointer?.id === fault.candidate, 'Fault did not follow durable pointer commitment.');
  check(stored.some((record) => record.id === pointer.id), 'Committed selected record was deleted.');
  check(stored.some((record) => record.id === previous.id) &&
    stored.some((record) => record.id === SENTINEL), 'Working/unrelated data was removed.');
  check(outcome === false, 'Post-commit failure was not returned honestly.');
  await remount();
  await waitFor(() => current.api.customFont?.id === pointer.id &&
    current.api.storageError === null, 'committed selection reload');
}

const cases = {
  baseline,
  readbackDenied: () => uploadFailure('readbackDenied'),
  readbackMismatch: () => uploadFailure('readbackMismatch'),
  pointerDenied: () => uploadFailure('pointerDenied'),
  postCommitFailure: () => committedFailure('postCommitFailure'),
  postCommitReportingFailure: () => committedFailure('postCommitReportingFailure'),
  async compoundFailure() {
    const previous = await baseline();
    const fault = inject('compoundFailure');
    await current.api.uploadFont(fixture);
    await waitFor(() => current.api.storageError !== null, 'compound failure report');
    check(fault.pointer()?.id === previous.id, 'Pointer failure lost previous selection.');
    check((await records()).some((record) => record.id === fault.candidate),
      'Deletion-denied fixture did not leave its candidate.');
    check(current.api.storageError.includes('QA pointer write denied') &&
      current.api.storageError.includes('QA candidate cleanup denied'),
    'Original or cleanup failure disappeared from current operation status.');
  },
  async pointerReadDenied() {
    const previous = await baseline();
    const fault = inject('pointerReadDenied');
    check(await current.api.uploadFont(fixture) === true, 'Verified pointer commit failed unexpectedly.');
    await waitFor(() => current.toasts.some((entry) =>
      String(entry.description).includes('QA pointer read denied')), 'visible cleanup limitation');
    const stored = await records();
    check(stored.some((record) => record.id === previous.id) &&
      stored.some((record) => record.id === fault.pointer().id),
    'Unreadable pointer caused unsafe selected-record deletion.');
  },
  async staleCandidateFailure() {
    await baseline();
    const gate = holdWriteCompletion();
    const fault = inject('normal');
    const older = current.api.uploadFont(fixture);
    await withDeadline(gate.ready, 'completed candidate write');
    const oldId = fault.candidate;
    check(await current.api.uploadFont(fixture) === true, 'Newer selection failed.');
    const newest = fault.pointer();
    const remove = IDBObjectStore.prototype.delete;
    replace(IDBObjectStore.prototype, 'delete', function (id) {
      if (id === oldId) throw new DOMException('QA obsolete cleanup denied', 'UnknownError');
      return remove.call(this, id);
    });
    gate.release();
    check(await older === false, 'Obsolete upload reported success.');
    await waitFor(() => current.toasts.some((entry) =>
      String(entry.description).includes('QA obsolete cleanup denied')), 'obsolete cleanup warning');
    check(current.api.storageError === null, 'Obsolete error replaced newer success state.');
    check(selection().id === newest.id, 'Obsolete upload changed the newest pointer.');
    const stored = await records();
    check(stored.some((record) => record.id === oldId) &&
      stored.some((record) => record.id === newest.id), 'Denied-cleanup/newest records were not preserved.');
  },
  async retiredUploadFailure() { await retirementFailure(false); },
  async retiredRemovalFailure() { await retirementFailure(true); },
  async migrationPointerFailure() { await migrationFailure(false); },
  async migrationCompoundFailure() { await migrationFailure(true); },
  async obsoleteMigration() {
    await baseline();
    localStorage.setItem(CUSTOM_FONT_KEY, await legacyValue());
    const work = observeFontTransactions();
    const gate = holdWriteCompletion();
    const fault = inject('normal');
    await remount();
    await withDeadline(gate.ready, 'legacy candidate write');
    const obsoleteId = fault.candidate;
    check(await current.api.uploadFont(fixture) === true, 'Newer upload failed during migration.');
    const newest = selection();
    gate.release();
    await work.wait();
    const stored = await records();
    check(selection()?.id === newest.id && current.api.customFont?.id === newest.id,
      'Obsolete migration replaced newer durable or working selection.');
    check(current.api.storageError === null, 'Obsolete migration replaced newer success.');
    check(!stored.some((record) => record.id === obsoleteId) &&
      stored.some((record) => record.id === newest.id) &&
      stored.some((record) => record.id === SENTINEL),
    'Obsolete migration retained its candidate or removed protected records.');
  },
};

async function retirementFailure(removeSelected) {
  const previous = await baseline();
  const gate = holdRetirementOpen(removeSelected ? 1 : 3);
  inject('normal', previous.id);
  const first = removeSelected ? await current.api.removeFont() : await current.api.uploadFont(fixture);
  check(first === true, 'Initial operation did not commit before retirement.');
  await withDeadline(gate.ready, 'retirement database open');
  check(await current.api.uploadFont(fixture) === true, 'Newer replacement failed.');
  const newest = selection();
  gate.release();
  await waitFor(() => current.toasts.some((entry) =>
    String(entry.description).includes('QA candidate cleanup denied')), 'delayed retirement warning');
  check(current.api.storageError === null, 'Old retirement error replaced newer success state.');
  check(selection().id === newest.id, 'Old retirement changed newest selection.');
  const stored = await records();
  check(stored.some((record) => record.id === newest.id) &&
    stored.some((record) => record.id === previous.id) &&
    stored.some((record) => record.id === SENTINEL), 'Retirement removed protected data.');
}

async function migrationFailure(denyCleanup) {
  await baseline();
  const raw = await legacyValue();
  localStorage.setItem(CUSTOM_FONT_KEY, raw);
  const before = (await records()).map((record) => record.id).sort();
  for (let attempt = 0; attempt < (denyCleanup ? 1 : 2); attempt++) {
    const fault = inject(denyCleanup ? 'compoundFailure' : 'pointerDenied');
    await remount();
    await waitFor(() => fault.pointerAttempts > 0 &&
      current.api.storageError !== null, 'migration pointer failure');
    check(localStorage.getItem(CUSTOM_FONT_KEY) === raw, 'Migration destroyed working legacy bytes.');
    const after = (await records()).map((record) => record.id).sort();
    if (denyCleanup) {
      check(after.includes(fault.candidate), 'Cleanup-denied migration lost its candidate fixture.');
      check(current.api.storageError.includes('QA pointer write denied') &&
        current.api.storageError.includes('QA candidate cleanup denied'),
      'Migration lost original or cleanup failure context.');
    } else {
      check(JSON.stringify(after) === JSON.stringify(before), 'Failed migrations accumulated candidates.');
    }
    restoreFaults();
  }
  const setItem = Storage.prototype.setItem;
  replace(Storage.prototype, 'setItem', function (key, value) {
    const result = setItem.call(this, key, value);
    if (this === localStorage && key === CUSTOM_FONT_KEY) {
      queueMicrotask(() => { for (const observer of observers) observer(); });
    }
    return result;
  });
  await remount();
  await waitFor(() => {
    const pointer = selection();
    return pointer !== null && pointer.id === current.api.customFont?.id &&
      current.api.storageError === null;
  }, 'successful migration retry');
  check((await records()).some((record) => record.id === selection().id),
    'Successful migration selection is not durable.');
}

function restoreFaults() {
  for (const release of releases.splice(0)) release();
  for (const restore of restorations.splice(0).reverse()) restore();
}

async function run(name) {
  check(!busy, 'A check is already running; do not overlap fault injection.');
  check(Object.hasOwn(cases, name), 'Unknown regression case.');
  busy = true;
  let result;
  const priorUnhandled = unhandled.length;
  try {
    await ensureOwnedOrigin();
    await mount();
    await cases[name]();
    check(unhandled.length === priorUnhandled, 'Operation caused an unhandled rejection.');
    result = { name, status: 'PASS' };
  } catch (error) {
    result = { name, status: 'FAIL', error: error instanceof Error ? error.message : String(error) };
  } finally {
    restoreFaults();
    busy = false;
  }
  result.state = await inspect();
  document.getElementById('qa-result').textContent = JSON.stringify(result, null, 2);
  return result;
}

async function cleanup() {
  check(!busy && sessionStorage.getItem(OWNER), 'Only idle, owned-origin fixtures may be removed.');
  root?.unmount();
  root = undefined;
  current = undefined;
  restoreFaults();
  localStorage.removeItem(CUSTOM_FONT_KEY);
  const removed = Promise.withResolvers();
  const request = indexedDB.deleteDatabase(DATABASE);
  request.onsuccess = () => removed.resolve();
  request.onerror = () => removed.reject(request.error);
  request.onblocked = () => removed.reject(new Error('Owned font database cleanup is blocked.'));
  await removed.promise;
  sessionStorage.removeItem(OWNER);
  return { keys: Object.keys(localStorage), databases: await indexedDB.databases() };
}

window.muajjinFontQA = {
  run, inspect, cleanup,
  setFile(file) { fixture = file; return { name: file.name, bytes: file.size }; },
};
document.getElementById('qa-font').addEventListener('change', (event) => {
  fixture = event.target.files?.[0];
});
for (const name of Object.keys(cases)) {
  const button = document.createElement('button');
  button.type = 'button';
  button.textContent = name;
  button.addEventListener('click', () => {
    void run(name).catch((error) => {
      document.getElementById('qa-result').textContent = String(error);
    });
  });
  document.getElementById('qa-actions').append(button);
}
try {
  await ensureOwnedOrigin();
  await mount();
  document.getElementById('qa-result').textContent = 'Ready. Choose a valid customfont.woff2 fixture.';
} catch (error) {
  document.getElementById('qa-result').textContent = String(error);
}
