# Implementation task checklist

Checked items mean the scoped implementation and documented local checks are delivered. They do not certify unmeasured physical-device battery/CPU/startup performance, every OEM/launcher/accessibility configuration, or a trusted signed release. Exact outcomes and limits are in `tasks/evidence.md`.

- [x] T00: executable baseline, runtime/device facts and repeated comparable work observations.
- [x] T01: meaningful tests/type-check/lint/CI gates; no automatic publishing.
- [x] T02: valid zero coordinates, input bounds, isolated selected-method parameters.
- [x] T03: actual next-day boundaries, dated instants, DST/device-timezone semantics.
- [x] T04: coherent prayer/prohibited/fasting dashboard state and loading-boundary migrations.
- [x] T05: native schedule/presentation separation, invalidation and acknowledged transfers.
- [x] T06: one parsed native state per update dispatch and shared consumer rendering.
- [x] T07: consumer-aware alarms, precise permitted transitions, honest denied-permission fallback.
- [x] T08: complete offline native renewal beyond the old horizon and headless runtime proof.
- [x] T09: no hidden cosmetic polling; retained foreground countdown precision and resume reconciliation.
- [x] T10: committed updates, migration/validation, reset and durable failure behavior.
- [x] T11: atomic bounded imports, verified font migration and stale-load protection.
- [x] T12: native language/clock/Hijri parity, RTL rendering and fresh background dates.
- [x] T13: reference-proven dead code/dependency removal and caller cutover.
- [x] T14: asset/code size audited and sync-work reduced; startup latency optimization evaluated and bounded without speculative claims.
- [x] T15: deliberate unsigned releases, explicit inputs and strict actual payload comparisons.
- [x] T16: bounded/cancelled location requests and safe offline/manual recovery.
- [x] T17: semantic controls, actual keyboard/touch/Back and widget checks, valid manifest assets and consistent docs.


## Review corrections (C01–C08)

- [x] C01: Remove native fabricated-time fallback in PrayerSchedule.java; fail uncomputable astronomical renewals explicitly and present honest recovery state.
- [x] C02: Produce strictly chronological web prayer timeline in time-utils.ts; fix cross-timezone earliest-future prayer selection.
- [x] C03: Remove automatic unsigned publication job, write permissions, and release action in release-apk.yml.
- [x] C04: Validate native horizon outer edges in PrayerSchedule.java and tests; reject ancient previousIsha or far-future nextFajr.
- [x] C05: Repair malformed adjustment handling in settings-storage.ts and time-utils.ts; bound fasting and Hijri adjustments without crashing dashboard.
- [x] C06: Clean up uncommitted font-migration candidates in font-storage.ts and translation-context.tsx on verification/pointer failure.
- [x] C07: Reconcile persisted native acknowledgment on JS restart via getScheduleStatus() in PrayerWidgetPlugin and widget-sync-service.
- [x] C08: Correct status, claims, and file references (PrayerWidgetHelper, no unmeasured performance claims) in tasks documents.

## Second review corrections (S01–S07)

- [x] S01: Real outer native boundaries validated against astronomical calculations in PrayerSchedule.validateOuterBoundaries and PrayerWidgetPlugin.updateSchedule.
- [x] S02: Removed arbitrary ±120 cap; enforced representable timestamp safety while preserving valid offsets like 121 minutes and 121 days.
- [x] S03: Observable font cleanup handling; cleanupRecord preserves font on localStorage read errors.
- [x] S04: Replaced ineffective 50-byte zero buffer test with real post-write verification failure and candidate deletion test in storage-imports.test.ts.
- [x] S05: Fixed NativeScheduleStore.saveSchedule revision persistence check and made JS status query retryable on failure.
- [x] S06: Multi-day polar renewal collapse regression (intermediateAstronomicalCollapseFailsRenewalWithoutFabrication).
- [x] S07: Task evidence and documentation reconciled with explicit separated status fields.
## Release validation not yet established

- [ ] Controlled physical-device battery attribution, CPU profiles and visible startup benchmarks.
- [ ] Extended overnight/OEM/device-off delivery and the supported Android-version/launcher matrix.
- [ ] Full TalkBack/assistive-technology and larger-system-font matrix; unusually long labels may need more widget space.
- [ ] Independently trusted production signer/reference verification and deliberate publication authorization.

These are explicit verification limits, not calculator stubs, disabled features or hidden implementation work.
