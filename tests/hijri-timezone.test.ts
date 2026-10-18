import { describe, expect, test } from 'bun:test';
import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { z } from 'zod';
import type { infer as Infer } from 'zod';
import { calculateHijriDate } from '../src/utils/hijri-utils';
import { defaultAppSettings, normalizeAppSettings } from '../src/utils/settings-storage';

const root = fileURLToPath(new URL('..', import.meta.url));
const moduleUrl = new URL('../src/utils/hijri-utils.ts', import.meta.url).href;
const caseSchema = z.object({
  id: z.string(),
  now: z.number().finite().int(),
  adjustment: z.number().finite().int(),
  maghrib: z.number().finite().int(),
  changeAtMaghrib: z.boolean(),
  expected: z.tuple([z.number().int(), z.number().int(), z.number().int()]).nullable(),
});
const fixtureSchema = caseSchema.extend({ timezone: z.string() });
type CalendarCase = Infer<typeof caseSchema>;

// Each child retains the actual module across a TZ change without altering the
// test worker's timezone or regenerating the independent committed references.
function checkCalendar(
  initialTimezone: string,
  timezone: string,
  cases: CalendarCase[],
  importAfterChange = false,
) {
  const result = spawnSync(process.execPath, ['--eval', `
    import assert from 'node:assert/strict';
    import { readFileSync } from 'node:fs';
    const input = JSON.parse(readFileSync(0, 'utf8'));
    await import('@umalqura/core');
    // Dynamic imports intentionally test module-loading order around a TZ
    // change; hoisted static imports would erase the boundary under test.
    if (input.importAfterChange) process.env.TZ = input.timezone;
    const { calculateHijriDate } = await import(${JSON.stringify(moduleUrl)});
    process.env.TZ = input.timezone;
    assert.equal(
      Intl.DateTimeFormat().resolvedOptions().timeZone,
      new Intl.DateTimeFormat('en', { timeZone: input.timezone })
        .resolvedOptions().timeZone,
      'Child process did not adopt the requested timezone',
    );
    for (const entry of input.cases) {
      const calculate = () => calculateHijriDate(
        new Date(entry.now), entry.adjustment, entry.maghrib,
        entry.changeAtMaghrib,
      );
      if (entry.expected === null) {
        assert.throws(calculate, RangeError, entry.id);
      } else {
        const actual = calculate();
        assert.deepEqual(
          [Number(actual.day), actual.month.number, Number(actual.year)],
          entry.expected, entry.id,
        );
      }
    }
    console.log(JSON.stringify({ checked: input.cases.length }));
  `], {
    cwd: root,
    env: { ...process.env, TZ: initialTimezone },
    input: JSON.stringify({ timezone, cases, importAfterChange }),
    encoding: 'utf8',
    timeout: 20_000,
  });
  if (result.error) throw result.error;
  expect({ status: result.status, diagnostics: result.stderr }).toEqual({
    status: 0,
    diagnostics: '',
  });
  expect(JSON.parse(result.stdout)).toEqual({ checked: cases.length });
}

const fixedCases: CalendarCase[] = [
  { adjustment: -121, expected: [28, 5, 1447] },
  { adjustment: -120, expected: [29, 5, 1447] },
  { adjustment: 0, expected: [1, 10, 1447] },
  { adjustment: 120, expected: [4, 2, 1448] },
  { adjustment: 121, expected: [5, 2, 1448] },
].map(({ adjustment, expected }) => caseSchema.parse({
  id: `2026-03-20/${adjustment}`,
  now: Date.parse('2026-03-20T12:00:00Z'),
  adjustment,
  maghrib: Date.parse('2026-03-20T18:00:00Z'),
  changeAtMaghrib: false,
  expected,
}));

const fixtureGroups = new Map<string, CalendarCase[]>();
const referenceFile = new URL('./native/engine-parity.jsonl', import.meta.url);
for (const line of readFileSync(referenceFile, 'utf8').trim().split('\n')) {
  const value: unknown = JSON.parse(line);
  if (value === null || typeof value !== 'object' ||
      !('kind' in value) || value.kind !== 'hijri') continue;
  const fixture = fixtureSchema.parse(value);
  const group = fixtureGroups.get(fixture.timezone) ?? [];
  group.push(fixture);
  fixtureGroups.set(fixture.timezone, group);
}

describe('Hijri table remains civil-date based after a timezone change', () => {
  test.each([
    ['Africa/Cairo', 'UTC'],
    ['America/New_York', 'Pacific/Honolulu'],
  ])('%s to %s retains exact dates and supported adjustments', (initial, target) => {
    checkCalendar(initial, target, fixedCases);
  });

  test('upstream initialization before the production module cannot shift its table', () => {
    checkCalendar('Africa/Cairo', 'UTC', fixedCases, true);
  });

  test('years below 100 cannot be reinterpreted as supported twentieth-century dates', () => {
    const cases = ['0001-01-01', '0099-12-31'].map((date) =>
      caseSchema.parse({
        id: `out-of-range/${date}`,
        now: Date.parse(`${date}T12:00:00Z`),
        adjustment: 0,
        maghrib: Date.parse(`${date}T18:00:00Z`),
        changeAtMaghrib: false,
        expected: null,
      }),
    );
    checkCalendar('Africa/Cairo', 'UTC', cases);
  });

  test.each([...fixtureGroups.entries()])(
    'retained table matches independent calendar fixtures in %s',
    (timezone, cases) => checkCalendar('Pacific/Kiritimati', timezone, cases),
    30_000,
  );
});

const hijriArithmeticGroup = 'Hijri arithmetic versus exact-table availability';
describe(hijriArithmeticGroup, () => {
  // Keep the pinned Bun TZ-deletion bug out of the shared test process.
  if (process.env.MUAJJIN_HIJRI_ARITHMETIC_UTC !== '1') {
    test('all arithmetic cases in an isolated UTC process', () => {
      const result = spawnSync(process.execPath, [
        'test', fileURLToPath(import.meta.url),
        '--test-name-pattern', hijriArithmeticGroup,
      ], {
        cwd: root,
        env: { ...process.env, TZ: 'UTC', MUAJJIN_HIJRI_ARITHMETIC_UTC: '1' },
        encoding: 'utf8', timeout: 4_000,
      });
      if (result.error) throw result.error;
      if (result.status !== 0) throw new Error(result.stderr || result.stdout);
    });
    return;
  }
  if (Intl.DateTimeFormat().resolvedOptions().timeZone !== 'UTC')
    throw new Error('Hijri arithmetic worker did not adopt UTC');
  const reference = new Date('2026-03-20T12:00:00.000Z');
  test.each([
    Number.MAX_SAFE_INTEGER, -Number.MAX_SAFE_INTEGER, NaN, Infinity,
    -Infinity, 0.5, -0.5, Number.MAX_SAFE_INTEGER + 1,
  ])('unusable adjustment %p fails without mutating its reference', (value) => {
    expect(() => calculateHijriDate(reference, value)).toThrow(RangeError);
    expect(reference.getTime()).toBe(Date.parse('2026-03-20T12:00:00.000Z'));
  });
  test.each([-100_000, 100_000])(
    'legitimate out-of-table adjustment %i is preserved, not approximated or reset',
    (adjustment) => {
      const input = {
        ...defaultAppSettings, latitude: 0, longitude: 0, madhab: 0,
        suhoorAdjustment: -121, iftarAdjustment: 121,
        hijriAdjustment: adjustment,
      };
      const loaded = normalizeAppSettings(input);
      expect(loaded).toEqual(input);
      expect(() => calculateHijriDate(reference, loaded.hijriAdjustment)).toThrow(RangeError);
    },
  );
});
