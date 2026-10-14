export interface ClockEnvironment<Handle> {
  now: () => number;
  active: () => boolean;
  setTimeout: (callback: () => void, delay: number) => Handle;
  clearTimeout: (handle: Handle) => void;
  subscribe: (reconcile: () => void) => () => void;
}

/** One timer per consumer, no polling for identities and no work while hidden. */
export function startClockScheduler<Handle>(
  environment: ClockEnvironment<Handle>,
  boundaries: readonly number[],
  cadence: number | undefined,
  onTick: (timestamp: number) => void,
): () => void {
  let handle: Handle | undefined;
  let stopped = false;
  const reconcile = () => {
    if (handle !== undefined) environment.clearTimeout(handle);
    handle = undefined;
    if (stopped || !environment.active()) return;
    const now = environment.now();
    onTick(now);
    let next = cadence === undefined ? Infinity : (Math.floor(now / cadence) + 1) * cadence;
    for (const boundary of boundaries) {
      if (boundary > now && boundary < next) next = boundary;
    }
    if (Number.isFinite(next)) {
      handle = environment.setTimeout(reconcile, Math.min(2_147_483_647, Math.max(1, next - environment.now())));
    }
  };
  const unsubscribe = environment.subscribe(reconcile);
  reconcile();
  return () => {
    stopped = true;
    if (handle !== undefined) environment.clearTimeout(handle);
    unsubscribe();
  };
}
