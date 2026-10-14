#!/usr/bin/env node
// Reference data comes from installed upstream JavaScript, never the Java port.
// Run from the repository root: node tests/native/generate-fixtures.mjs
// Bun is also supported: bun tests/native/generate-fixtures.mjs
import { createRequire } from 'node:module';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';

const directory = dirname(fileURLToPath(import.meta.url));
const root = resolve(directory, '../..');
const require = createRequire(import.meta.url);
const adhan = require('adhan');
const hijriModule = require('@umalqura/core');
const umalqura = hijriModule.default ?? hijriModule;
const versions = {
  adhan: JSON.parse(readFileSync(resolve(root, 'node_modules/adhan/package.json'), 'utf8')).version,
  umalqura: JSON.parse(readFileSync(resolve(root, 'node_modules/@umalqura/core/package.json'), 'utf8')).version,
};
if (versions.adhan !== '4.4.3' || versions.umalqura !== '0.0.7') {
  throw new Error('Update the native port and provenance before regenerating fixtures for different engine versions.');
}
const factories = {
  1: 'Karachi', 2: 'NorthAmerica', 3: 'MuslimWorldLeague', 4: 'UmmAlQura',
  5: 'Egyptian', 7: 'Tehran', 8: 'Dubai', 9: 'Kuwait', 10: 'Qatar',
  11: 'Singapore', 12: 'MoonsightingCommittee', 15: 'MoonsightingCommittee',
};
const locations = [
  ['makkah', 'Asia/Riyadh', 21.3891, 39.8579],
  ['dhaka', 'Asia/Dhaka', 23.8103, 90.4125],
  ['singapore', 'Asia/Singapore', 1.3521, 103.8198],
  ['tehran', 'Asia/Tehran', 35.6892, 51.389],
  ['new-york', 'America/New_York', 40.7128, -74.006],
  ['london', 'Europe/London', 51.5074, -0.1278],
  ['oslo', 'Europe/Oslo', 59.9139, 10.7522],
  ['sydney', 'Australia/Sydney', -33.8688, 151.2093],
  ['cape-town', 'Africa/Johannesburg', -33.9249, 18.4241],
  ['equator-prime-meridian', 'UTC', 0, 0],
  ['western-antimeridian', 'Pacific/Honolulu', 0, -180],
  ['eastern-antimeridian', 'Pacific/Auckland', 0, 180],
  ['tromso', 'Europe/Oslo', 69.6492, 18.9553],
  ['north-pole', 'UTC', 90, 0],
  ['south-pole', 'UTC', -90, 0],
  // Preserve device-zone policy, including a zone unrelated to the coordinates.
  ['makkah-on-new-york-device', 'America/New_York', 21.3891, 39.8579],
  ['apia-date-line', 'Pacific/Apia', -13.8333, -171.75],
];
const dates = [
  '2024-02-29', '2024-03-10', '2024-03-31', '2024-04-07', '2024-06-21',
  '2024-10-06', '2024-10-27', '2024-11-03', '2024-12-21', '2024-12-31',
  '2025-01-01', '2026-03-20', '2026-09-23', '2011-12-29',
];
function localDate(date, hour = 12) {
  const [year, month, day] = date.split('-').map(Number);
  return new Date(year, month - 1, day, hour);
}
function prayerRows(timezone) {
  const rows = [];
  for (const [location, zone, latitude, longitude] of locations) {
    if (zone !== timezone) continue;
    for (const date of dates) {
      for (const [method, factory] of Object.entries(factories)) {
        for (const madhab of [0, 1]) {
          const parameters = adhan.CalculationMethod[factory]();
          parameters.madhab = madhab === 1 ? adhan.Madhab.Hanafi : adhan.Madhab.Shafi;
          const times = new adhan.PrayerTimes(new adhan.Coordinates(latitude, longitude), localDate(date), parameters);
          const expected = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'].map(event => times[event].getTime());
          rows.push({ kind: 'prayer', id: `${location}/${date}/${method}/${madhab}`, date, timezone, latitude, longitude, method: Number(method), madhab, expected: expected.every(Number.isFinite) ? expected : null });
        }
      }
    }
  }
  return rows;
}
function hijriRows(timezone) {
  const rows = [];
  const datesToTest = ['1900-04-30', '2077-11-16', '2024-02-29', '2024-12-31'];
  // Exercise every Hijri month start and end in the exact upstream table.
  if (timezone === 'UTC') {
    for (let year = 1318; year <= 1500; year++) {
      for (let month = 1; month <= 12; month++) {
        const first = umalqura(year, month, 1);
        for (const instance of [first, umalqura(year, month, first.daysInMonth)]) {
          const d = instance.date;
          datesToTest.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`);
        }
      }
    }
  }
  // Real local-date increments across leap day and both DST transitions.
  for (const start of ['2024-02-25', '2024-03-07', '2024-03-28', '2024-04-04', '2024-10-03', '2024-10-24', '2024-10-31']) {
    const d = localDate(start);
    for (let offset = 0; offset < 8; offset++) {
      datesToTest.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`);
      d.setDate(d.getDate() + 1);
    }
  }
  for (const date of [...new Set(datesToTest)]) {
    // All month boundaries need one exact conversion; selected dates cover preferences.
    const adjustments = date >= '2024-01-01' && date <= '2025-01-01' ? [-3, -1, 0, 1, 3] : [0];
    for (const adjustment of adjustments) {
      for (const phase of ['disabled', 'before', 'at', 'after']) {
        const changeAtMaghrib = phase !== 'disabled';
        const maghrib = localDate(date, 18).getTime();
        const now = maghrib + (phase === 'before' ? -1 : phase === 'after' || phase === 'disabled' ? 1 : 0);
        const d = localDate(date);
        d.setDate(d.getDate() + adjustment + (changeAtMaghrib && now >= maghrib ? 1 : 0));
        // @umalqura/core subtracts local-midnight epochs and truncates fractional
        // days. Normalize ONLY the reference conversion to UTC date components,
        // retaining its exact dataset while correcting the known DST defect.
        const canonical = Date.UTC(d.getFullYear(), d.getMonth(), d.getDate());
        let expected = null;
        if (canonical >= Date.UTC(1900, 3, 30) && canonical < Date.UTC(2077, 10, 17)) {
          // This worker's zone can be non-UTC; defer canonical table calls to a UTC worker.
          expected = [d.getFullYear(), d.getMonth() + 1, d.getDate()];
        }
        rows.push({ kind: 'hijri', id: `${date}/${timezone}/${adjustment}/${phase}`, date, timezone, adjustment, changeAtMaghrib, now, maghrib, canonical: expected });
      }
    }
  }
  for (const date of ['1900-04-29', '2077-11-17']) {
    rows.push({ kind: 'hijri', id: `out-of-range/${date}/${timezone}`, date, timezone, adjustment: 0, changeAtMaghrib: false, now: localDate(date).getTime(), maghrib: localDate(date, 18).getTime(), canonical: null });
  }
  return rows;
}
if (process.argv[2] === '--worker') {
  process.stdout.write([...prayerRows(process.env.TZ), ...hijriRows(process.env.TZ)].map(row => JSON.stringify(row)).join('\n'));
} else if (process.argv[2] === '--convert-hijri') {
  const canonicalDates = JSON.parse(readFileSync(0, 'utf8'));
  process.stdout.write(JSON.stringify(canonicalDates.map(date => {
    if (!date) return null;
    const value = umalqura(new Date(Date.UTC(date[0], date[1] - 1, date[2])));
    return [value.hd, value.hm, value.hy];
  })));
} else {
  const rows = [];
  const zones = [...new Set([...locations.map(location => location[1]), 'America/New_York', 'Europe/London', 'Australia/Sydney'])];
  for (const timezone of zones) {
    const worker = spawnSync(process.execPath, [fileURLToPath(import.meta.url), '--worker'], {
      env: { ...process.env, TZ: timezone }, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024,
    });
    if (worker.status !== 0) throw new Error(worker.stderr || `Fixture worker failed: ${timezone}`);
    rows.push(...worker.stdout.split('\n').filter(Boolean).map(line => JSON.parse(line)));
  }
  const hijri = rows.filter(row => row.kind === 'hijri');
  const conversion = spawnSync(process.execPath, [fileURLToPath(import.meta.url), '--convert-hijri'], {
    env: { ...process.env, TZ: 'UTC' }, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024,
    input: JSON.stringify(hijri.map(row => row.canonical)),
  });
  if (conversion.status !== 0) throw new Error(conversion.stderr || 'UTC Hijri conversion failed');
  JSON.parse(conversion.stdout).forEach((expected, index) => {
    hijri[index].expected = expected;
    delete hijri[index].canonical;
  });
  const sourcePaths = [
    'node_modules/adhan/lib/esm/PrayerTimes.js', 'node_modules/adhan/lib/esm/Astronomical.js',
    'node_modules/adhan/lib/esm/SolarTime.js', 'node_modules/adhan/lib/esm/SolarCoordinates.js',
    'node_modules/adhan/lib/esm/CalculationMethod.js', 'node_modules/adhan/lib/esm/CalculationParameters.js',
    'node_modules/adhan/lib/esm/DateUtils.js', 'node_modules/adhan/lib/esm/TimeComponents.js',
    'node_modules/adhan/lib/esm/MathUtils.js', 'node_modules/@umalqura/core/dist/index.esm.js',
  ];
  const metadata = { kind: 'metadata', versions, prayerToleranceMillis: 0, hijriPolicy: 'upstream-table/UTC-local-date-components', sources: sourcePaths.map(path => ({ path, sha256: createHash('sha256').update(readFileSync(resolve(root, path))).digest('hex') })) };
  mkdirSync(directory, { recursive: true });
  writeFileSync(resolve(directory, 'engine-parity.jsonl'), [metadata, ...rows].map(row => JSON.stringify(row)).join('\n') + '\n');
  console.log(`Generated ${rows.filter(row => row.kind === 'prayer').length} prayer and ${hijri.length} Hijri reference cases in tests/native/engine-parity.jsonl`);
}
