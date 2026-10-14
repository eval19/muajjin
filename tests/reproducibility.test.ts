import { expect, test } from 'bun:test';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

// Python's standard ZIP reader preserves entry identity without extraction;
// this exercises the verifier's real payload and signer semantics, not source.
test('APK payload comparison and signing-scope regression cases', () => {
  const root = fileURLToPath(new URL('..', import.meta.url));
  const python = process.env.PYTHON ?? (process.platform === 'win32' ? 'python' : 'python3');
  const result = spawnSync(python, ['-B', '-m', 'unittest', 'discover', '-s', 'tests', '-p', 'reproducibility_test.py', '-v'], {
    cwd: root,
    encoding: 'utf8',
  });
  if (result.error) throw result.error;
  expect({ status: result.status, diagnostics: result.stderr }).toEqual({
    status: 0,
    diagnostics: expect.stringContaining('OK'),
  });
});
