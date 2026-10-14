import { describe, expect, test } from 'bun:test';
import { startClockScheduler, type ClockEnvironment } from '../src/utils/clock-scheduler';

function fakeClock() {
  let now = 1000;
  let active = true;
  let id = 0;
  const pending = new Map<number, { at: number; callback: () => void }>();
  const listeners = new Set<() => void>();
  const environment: ClockEnvironment<number> = {
    now: () => now,
    active: () => active,
    setTimeout: (callback, delay) => { pending.set(++id, { at: now + delay, callback }); return id; },
    clearTimeout: (handle) => { pending.delete(handle); },
    subscribe: (listener) => { listeners.add(listener); return () => { listeners.delete(listener); }; },
  };
  return {
    environment, pending, listeners,
    advance(timestamp: number) {
      now = timestamp;
      const due = [...pending].filter(([, timer]) => timer.at <= now);
      for (const [handle, timer] of due) { pending.delete(handle); timer.callback(); }
    },
    visibility(visible: boolean, timestamp: number) {
      active = visible; now = timestamp; for (const listener of listeners) listener();
    },
    timeChange(timestamp: number) { now = timestamp; for (const listener of listeners) listener(); },
  };
}

describe('visible boundary clock scheduler', () => {
  test('identity-only clock updates at real boundaries without safety polling', () => {
    const clock = fakeClock();
    const updates: number[] = [];
    const stop = startClockScheduler(clock.environment, [5000, 9000], undefined, (now) => updates.push(now));
    expect(updates).toEqual([1000]);
    expect([...clock.pending.values()].map((timer) => timer.at)).toEqual([5000]);
    clock.advance(4999); expect(updates).toEqual([1000]);
    clock.advance(5000); expect(updates).toEqual([1000, 5000]);
    clock.advance(9000); expect(updates).toEqual([1000, 5000, 9000]);
    expect(clock.pending.size).toBe(0);
    stop(); expect(clock.listeners.size).toBe(0);
  });
  test('hidden countdown has no timers and immediately reconciles skipped transitions at resume', () => {
    const clock = fakeClock();
    const updates: number[] = [];
    const stop = startClockScheduler(clock.environment, [5000, 9000], 1000, (now) => updates.push(now));
    clock.visibility(false, 1500);
    expect(clock.pending.size).toBe(0);
    clock.advance(12000); expect(updates).toEqual([1000]);
    clock.visibility(true, 12500);
    expect(updates).toEqual([1000, 12500]);
    expect([...clock.pending.values()].map((timer) => timer.at)).toEqual([13000]);
    stop(); expect(clock.pending.size).toBe(0);
  });
  test('clock correction reselects future boundaries in either direction without drift', () => {
    const clock = fakeClock();
    const updates: number[] = [];
    const stop = startClockScheduler(clock.environment, [5000, 9000], undefined, (now) => updates.push(now));
    clock.timeChange(7000);
    expect(updates).toEqual([1000, 7000]);
    expect([...clock.pending.values()][0].at).toBe(9000);
    clock.timeChange(2000);
    expect([...clock.pending.values()][0].at).toBe(5000);
    stop();
  });
  test('mounting while inactive does not start cosmetic work; teardown removes subscription', () => {
    const clock = fakeClock();
    clock.visibility(false, 1000);
    const updates: number[] = [];
    const stop = startClockScheduler(clock.environment, [], 1000, (now) => updates.push(now));
    expect(clock.pending.size).toBe(0);
    expect(updates).toEqual([]);
    clock.visibility(true, 4000);
    expect(updates).toEqual([4000]);
    stop(); expect(clock.listeners.size).toBe(0); expect(clock.pending.size).toBe(0);
  });
});
