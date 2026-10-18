# Implementation task checklist

Checked items mean the scoped implementation and documented local checks are delivered. They do not certify unmeasured physical-device battery/CPU/startup performance, every OEM/launcher/accessibility configuration, or a trusted signed release. Exact outcomes and limits are in `tasks/evidence.md`.

## Current app-health completion — 2.8.4

- [x] H00: Read-only audit of shipped versus local runtime fixes and app/native/tooling paths; distinguish confirmed defects from unverified scenarios.
- [x] H01: Record failing-before reproductions for the scoped UI, async, native persistence/presentation and APK verification defects.
- [x] H02: Integrate pending runtime fixes and complete the approved minimal repairs without unrelated edits or new dependencies.
- [x] H03: Pass full web gates, Python APK regressions and native unit/runtime checks with consumer-visible assertions.
- [x] H04: Exercise actual browser routes/imports/settings/lifecycle and isolated API36 native/widget/persistence/restart scenarios; retain platform limits accurately.
- [ ] H05: Publish the verified original-signed 2.8.4 APK from a clean GitHub-compiled source and verify its public download/updater path.

The older completed and unchecked rows below retain their historical scope. They are not a whole-app health certificate or proof that 2.8.3 included the pending local fixes.

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
- [x] C03/F03: Removed unsigned publication job, repository-write permission and release action; local three-workflow verification passed. No hosted Actions or public release was executed.
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
## Behavioral QA handoff — current correction status

This section records the new correction sequence. Older checked implementation rows are historical delivery claims, not proof that the current behavioral handoff is complete.

- [x] F03: Remove automatic unsigned public-release publication; parse all three workflows locally and verify dependencies, permissions, artifact uploads and retained Pages policy.
- [ ] F01: Code and isolated-process regressions corrected; 47 affected tests passed, including 32,226 independent Hijri fixtures across 13 timezones; lint/types/both web builds passed. Actual browser/device timezone-change resume remains UNVERIFIED.
- [x] F02: Preserve committed font records after activation/reporting failure and return false honestly; actual provider/IndexedDB failure and reload checks passed.
- [x] F04: Preserve original plus cleanup errors, make stale cleanup warnings observable without changing newer status, and retain potentially selected records when pointer reads fail; actual browser checks passed.
- [x] F05: Carry initiating request ownership through both upload/remove retirement paths; delayed failures preserve newer selection/status and remain observable in actual browser checks.
- [ ] F06: JS validation/retry/reconciliation corrected; 175 affected tests, direct production smoke, lint/types/both web modes passed. Actual Android persisted-pair validation, durable revision repair/failed commit and retained WebView/process restart remain UNVERIFIED.
- [ ] F07: JS canonical-alias reuse and genuine-zone invalidation corrected; independent factory counts and post-wait timezone checks passed. Actual Android/WebView alias identity and retained native preferences across process recreation remain UNVERIFIED.
- [x] F08: Exact adjustment conversion replaces guessed headroom; actual resulting Dates govern availability and valid siblings survive. Full suite: 299 parent tests plus 44 isolated arithmetic cases; lint/types/both builds, production smoke and 14 real browser reload cases passed. User-assisted actual typing/Save and page reload preserved the repaired value, all other settings and sentinel. Task-owned cleanup verified; native/WebView gates remain separate.
- [ ] R09: Calendar/font, JS status/work-count and native core regression portions are corrected. Step 6 passed 27 fresh native tests and real-engine JVM smoke; fault controls proved three old false passes are now rejected, and removing outer validation is detected. Actual incoming/persisted-pair validation, coordinator/store retention/recovery, durable/failed revision commits and retained WebView/process restart remain UNVERIFIED.
- [ ] R10: Reconcile all affected historical implementation/regression/runtime claims with the final corrected source.
- [ ] Run separately approved remaining native/runtime integration gates; the full Bun suite passed in Step 5. Distinguish unverified scenarios and platform/release limits.

Approved scopes so far cover F03, F01 and coordinated F02/F04/F05 source/regressions/task records, local gates/build cleanup, and the isolated font browser/IndexedDB campaign. The browser campaign removed only its initially empty origin's synthetic font fixtures, closed its owned tab, restored the previous attachment and stopped its local server. Temporary helpers/font/screenshot and generated dist were removed. No dependencies were installed, devices or system clocks/timezones changed, or signing/publication/commits/pushes performed. Unchecked runtime gates are not implied complete by these results.

Step 4 additionally approved only the JS synchronizer, existing widget-sync tests, these task records and local verification/build cleanup. Its production smoke and test clocks/timezones were process-local; no native preferences, personal data, devices or system clocks/timezones changed. Full F06/F07 checkboxes remain open for their named Android runtime gates despite the completed local code correction.

Step 5 additionally approved adjustment utilities, three existing test files, these task records, local gates/builds/smoke and the isolated synthetic-data Edge campaign. Actual manual typing/Save/reload completed the repair check without focus emulation. The initially empty task origin returned to empty localStorage/IndexedDB with its ownership marker removed; the owned tab was closed and prior attachment restored. Port 18082 has zero listeners; `.tools/qa-adjustments/`, screenshots and generated dist were removed. No personal data, native preferences, devices, system clocks/timezones, dependencies or release/signing state were changed.

Step 6 additionally approved only `PrayerTimelineTest.java`, these records and isolated offline native/JVM verification helpers. No production Java or Android data/device state changed. Fresh XML reports were inspected, then task-owned helpers, control classes, build/project-cache outputs and two stopped-daemon logs were removed. Existing native build directories and working JDK/SDK/Gradle caches were preserved. Unit results do not close the Android persistence/lifecycle gates.

## Release validation not yet established

- [ ] Controlled physical-device battery attribution, CPU profiles and visible startup benchmarks.
- [ ] Extended overnight/OEM/device-off delivery and the supported Android-version/launcher matrix.
- [ ] Full TalkBack/assistive-technology and larger-system-font matrix; unusually long labels may need more widget space.
- [ ] Independently trusted production signer/reference verification and deliberate publication authorization.

These are explicit verification limits, not calculator stubs, disabled features or hidden implementation work.
