# Muajjin implementation plan

## 2.9.83 progress-line prerelease — 2026-10-31

Requested: ship the 2×1 progress-line fix as a prerelease. Same governed path —
explicit allowlist, version 2.9.83 / versionCode 301, new `release/2.9.83` branch
and annotated `v2.9.83` tag, unchanged hosted signing workflow, artifact
verification against the original certificate, publish only APK + checksum,
stable/latest stays v2.9.1.

## 2.9.82 reading-edge prerelease — 2026-10-31

Requested: ship the reading-edge correction for the 2×1 widget as a prerelease.
Same governed path as 2.9.81 — explicit allowlist, version 2.9.82 / versionCode
300, new `release/2.9.82` branch and annotated `v2.9.82` tag, unchanged hosted
quality/signing workflow, artifact verification against the original certificate,
publish only the signed APK and checksum, stable/latest stays v2.9.1.

Completed: source/tag `2058557d85246ef80dcd7682cf8303ca9a702a53`, signed run
`41148840434`, prerelease `429609717`. Original signer, 2.9.82/300 identity,
alignment, artifact/public checksums verified; stable/latest v2.9.1 unchanged.

## 2.9.81 widget balance prerelease — 2026-10-31

Requested: publish a GitHub prerelease named 2.9.81 with the current uncommitted
widget work. Same governed path as 2.9.8: explicit file allowlist, version
2.9.81 / versionCode 299, new `release/2.9.81` branch and annotated `v2.9.81`
tag, unchanged hosted quality/signing workflow with existing secrets, artifact
verification against the original certificate, then publish only the signed APK
and checksum. Stable/latest stays v2.9.1. Unrelated untracked files stay excluded.
No numeral-policy change; the 2×1 keeps the centred composition the user did not
choose to alter.

Completed: source/tag `b20c5f56502ac59cb4e1caa282186eee0cf762f7`, signed run
`41147661624`, prerelease `429603877`. Original signer, 2.9.81/299 identity,
alignment, artifact/public download checksums and unchanged stable v2.9.1
verified. Final APK/checksum retained; temporary watcher/verifier removed.

## Widget balance and two text sizes — 2026-10-31

Approved: rebalance the seven existing widgets and add one Normal/Larger text
setting (no slider, no third size). Preserve providers, palette, calculations,
numerals and every displayed value; build and verify on a disposable emulator;
no version bump or publication.

Decisions: one durable boolean rides the existing presentation payload, so the
setting costs a presentation revision only. Larger multiplies the *resolved*
Normal profile by 1.2 so compaction cannot shrink it; the two keyguard-only
variants stay Normal because their fixed one-row stack cannot hold a larger
three-line composition. Strip is centred; wider widgets keep matched leading and
trailing tracks. A six-cell single row uses 1dp cell padding with wrapped clocks
rather than hiding a prayer.

Outcome: 16 instrumentation tests (278-case matrix plus two-size and centring
cases) and 52 JVM unit tests pass; Normal/Larger Arabic captures inspected. The
settings switch was not tapped on a device; that remains a disclosed verification
limit alongside launcher/keyguard coverage.

## 2.9.8 Arabic visual prerelease — 2026-10-31

Approved: include this session's tracked Arabic visual fixes/regressions,
version 2.9.8/298 and existing release documentation; commit/push a new
`release/2.9.8` branch and annotated `v2.9.8` tag; run the unchanged hosted
quality/signing workflow with existing secrets; verify the original signer,
artifact identity/checksum/alignment and public downloads; publish only the
signed APK/checksum as a prerelease without changing stable/latest v2.9.1.
This approval supersedes the earlier no-commit/no-publication boundary only for
this release. Keep device-dependent numerals, existing providers, calculations
and scheduling unchanged. Exclude all unrelated untracked files and screenshot
folders. No local packages, keys, device changes or GitHub settings changes.

Sequence: explicit source allowlist → version/release identity checks →
commit/tag/push → hosted gates and signed artifact → draft asset verification →
publish prerelease → public download/latest verification. Retain final APK and
checksum in `.tools/release-2.9.8-output/`; remove temporary helpers/downloads.
Failed artifact verification blocks publication. Prior releases stay intact.
The known emulator keyguard-provider enumeration limitation remains disclosed.

Completed: source/tag `b26565493f830a871aa143206740bed944febc5c`, signed Actions
run `41140634852`, published prerelease `429564951`. Original signer, 2.9.8/298
identity, alignment, artifact/public download checksums and unchanged stable
v2.9.1 verified. Final APK/checksum retained; task-owned temporary files removed.
Evidence-only branch follow-up does not move the release tag.


## Arabic visual finish — 2026-10-31

Approved scope: visual-only refinement of the existing Arabic app and seven
language-following Android widgets. Preserve the palette, translations, all
displayed information, other languages, calculation/storage/scheduling behavior,
provider identities and fixed widget footprints. No dependencies, commits,
signing, publication or personal-device/system-setting changes.

### Design contract and execution

1. Capture Arabic baseline on a fresh synthetic localhost origin and native-only
   debug host. Preserve the existing Edge attachment and emulator. Temporary
   helpers, captures and isolated emulator artifacts belong to
   `.tools/qa-arabic-finish/`; reuse installed toolchains.
2. Refine shared typography/controls first: Arabic leading, logical alignment,
   RTL select/switch behavior, contained wrapping and safe-area bottom spacing.
3. Refine dashboard, onboarding/settings, calendar, Qibla and Qaza presentation.
   Isolate clock/numeric runs without changing formatter output or digit policy;
   never mirror geographic compass geometry or conceal data to obtain fit.
4. Refine existing native widget resources and measured composition. Verify the
   reported progress-line appearance before changing its inherited RTL behavior.
   Measurement and actual inflated composition must agree.
5. Verify Arabic at 320/360/393px, both themes and enlarged text; compare English
   and Bengali. Exercise dropdowns, switches, navigation, numeric controls and
   language switching. Inspect all seven native variants, RTL order, baselines,
   glyph bounds and progress geometry. Run lint/types/domain tests, both web
   builds, native unit tests and targeted instrumentation.

Risks: shared-control regressions and responsive widget cutovers. Mitigation:
before/after rendered checks, other-language comparisons and existing native
fit assertions without relaxed tolerances. Rollback is file-level reversal of
this task's uncommitted edits; preserve all pre-existing work. Record actual
results and platform limits in `tasks/evidence.md`.

Execution completed locally: app controls/screens and widget alignment/composition
refined; original formatter/digit policy preserved with widget-only bidi controls.
Web gates and native visual/numeral gates pass. The additional provider metadata
query did not enumerate keyguard-only widgets on the emulator; do not treat native
host captures as launcher/keyguard acceptance. Exact evidence and runtime limits
are recorded in `tasks/evidence.md`; no release or commit was made.


## 2.9.0 / 2.9.1 widget work — 2026-10-24

- 2.9.0 shipped the seven-widget visual overhaul (build 290, source `3fe8ab4`). The 291 replacement (source `de1440a`) added staged progress-bar colors, device-numeral formatting, locale-change reconciliation, and presentation regressions.
- 2.9.1 (source/tag `43fe901` / `v2.9.1`) fixes widget footprints: `resizeMode="none"`, canonical launcher cell hints, and an installed-provider metadata regression. Fixed size supersedes user resizing; compact host-size fallbacks remain.
- Provenance: the historical `v2.9.0` tag remains on `3fe8ab4` (the 290 source), while its published 291 asset was built from `de1440a`. The 2.9.1 tag correctly identifies its built source.
- The release branch has later docs-only commits after the 2.9.1 tag; the published APK remains the tagged `43fe901` build.

Source specification: `C:/Users/Mohamed Tawfig/Desktop/Muajjin-Improvement-Implementation-Plan.md` (T00–T17). All tasks remain in scope; this file records execution decisions, not a reduced replacement specification.

## Current app-health completion — approved 2.8.4 scope

The user approved integrating the pending app fixes, repairing the source-confirmed gaps below, exercising browser/native runtime paths, and publishing 2.8.4 with the existing verified signing key. This approval supersedes the older no-commit/no-publication restrictions only for this scoped completion. Unrelated `AGENTS.md` changes and `bakery_raw.json` remain excluded.

1. H01 — Reproduce the onboarding coordinate substitution, settings snapshot overwrite, stepper reversal, responsive remount, compass permission cleanup, updater cancellation, native corrupt-state recovery/Hijri type mismatch, and v1 APK comparison failures before their repairs.
2. H02 — Preserve the existing local Hijri/font/bridge/adjustment corrections and tests. Repair only the identified web/native/tooling paths; use partial settings commits, owned async cancellation, exact numeric boundaries and existing UI primitives. Do not introduce arbitrary adjustment caps, replacement calculation engines, packages or service workers.
3. H03 — Run full web gates in both modes, Python APK regressions and native unit/runtime suites with meaningful assertions. APK comparison must verify present v1 metadata before excluding it; v2-only APKs must remain supported.
4. H04 — Exercise the actual app in a new synthetic-data localhost Edge origin and a task-owned read-only instance of the existing API36 emulator. Cover all routes, settings/save failures, cross-tab state, imports/font ownership, timezone resume, Qibla lifecycle, Qaza editing, native saved-pair/revision recovery and seven widget providers. Preserve the existing browser attachment/profile, original AVD, personal devices and Windows clock/settings.
5. H05 — Curate a clean health-completion branch from published main, include the approved app/test/docs changes, run hosted gates, compile the APK in GitHub, sign locally with the original verified key and publish 2.8.4 only after signature/payload/download verification. Keep unrelated local changes untouched; do not configure GitHub signing secrets or Pages hosting.

Temporary scripts, screenshots, builds and emulator overlays are confined to `.tools/qa-app-health/` and a task-owned verification worktree; remove them after proof. Existing SDK/JDK/Bun/dependency/Gradle caches are reused. No new tools, packages or system images are authorized. Actual Samsung hardware, Android15 runtime and extended OEM/Doze/battery/assistive-technology coverage remain explicit limits, not inferred passes.

## Historical approved boundaries — earlier implementation

Approved: repository source/native/tests/build/CI/assets/docs, local verification scripts and task-owned cleanup; isolated Bun 1.2.2 and locked dependencies; portable JDK 21 and SDK 36/licenses; verified IndexedDB font migration; one task-owned API36 emulator and QA-only permissions, clocks and lifecycle actions. No global PATH/rule changes, personal-device changes, commits, publishing, production signing, new app dependencies or endpoint changes.

## Decisions

- Preserve device-timezone semantics; no silent selected-location timezone feature.
- Preserve current web caching behavior; no new service worker.
- D2 completed: exact compatible native adhan/Umm al-Qura ports, with source/license attribution and independent reference fixtures. Renewal uses saved validated inputs, not a longer static cache or background WebView.
- Font migration approved and exercised: IndexedDB database version 1, schema-2 records/pointers; legacy data is retained until the binary transaction and readback succeed. Downgrade needs an explicit data-backup/reverse-migration decision.
- No language server is configured; use compiler/reference searches until one is available.

## Sequence and integration ownership

The main assistant owns baseline, contracts, test execution, integration and verification. Domain/native schema changes are defined together before parallel edits. Independent storage/import and CI/release slices can run in parallel after baseline. Agents do not execute checks mid-flight; the integration owner runs red/green and checkpoint verification.

1. T00 executable baseline and environment evidence.
2. T01 gates; T02 coordinates/methods; T03 date-boundary correctness.
3. T04 dashboard; T05 bridge contract and schedule/presentation separation.
4. T06 native per-dispatch state; T07 scheduling; T08 real offline renewal; T09 frontend timers.
5. T10 storage; T11 imports; T12 native presentation.
6. T13 proven cleanup; T14 measured startup; T15 release/reproducibility; T16 location; T17 surface/docs.

## Checkpoints

- Core: failing-before/passing-after regression coverage, lint/type checks, both web builds, actual dashboard/settings smoke.
- Native: build/tests, offline horizon renewal/parity, lifecycle/device checks where available; no fabricated battery results.
- Storage: migration/failure/order tests and actual import/reload smoke.
- Final source changes and reachable local checks are recorded per T00–T17 in the evidence ledger. Physical-device battery/CPU/startup measurements, OEM overnight reliability, full assistive-technology testing and independently trusted signed-release verification remain unverified release gates, not completed claims.

Task status and acceptance criteria: `tasks/todo.md`. Baseline and measured outcomes: `tasks/evidence.md`.

## Behavioral QA correction sequence

Handoff: `C:/Users/Mohamed Tawfig/Desktop/Muajjin-Behavioral-QA-Fixes-Handoff.md`.
This correction sequence starts from clean commit `89bd72ec649bd367574ad0a5daf9cfd029be4b55`; earlier implementation approvals and results below are historical, not authorization or fresh verification for this sequence.

- Approved scopes so far: F03 workflow/task-record correction; F01 calendar/regression/task records with local checks; coordinated F02/F04/F05 provider/storage regressions, two permanent browser regression assets and task records. Font approval also covered temporary helpers/fixture, one isolated localhost server/origin and owned Edge tab, synthetic storage/activation/race failures, reload, local tests/lint/types/both builds and exact task-owned cleanup. No installation, device changes, system clock/timezone changes, signing, publication, commits or pushes.
- Step 4 approved `src/services/widget-sync-service.ts`, `tests/widget-sync.test.ts` and these three task records, with focused failure/retry regressions, in-memory production smoke, lint/types, both web builds and generated-output cleanup. No native source/preferences, browser/device campaign, dependencies, signing or publication changes were authorized or performed in this step.
- Step 5 approved adjustment utilities, the three existing storage/timeline/Hijri test files and these task records; local tests/lint/types/both builds/in-memory smoke; and one isolated localhost Edge campaign with synthetic settings, reload/repair checks, temporary helpers/screenshots under `.tools/qa-adjustments/` and exact task-owned cleanup. No new dependencies, native/device changes, system clock/timezone changes, signing or publication.
- Step 6 approved only `PrayerTimelineTest.java`, these task records, fresh offline native unit verification and temporary JVM/init helpers under `.tools/qa-native-regressions/`. Production Java, native preferences, devices/emulators, permissions, system clocks/timezones, dependencies, signing and publication were outside scope and unchanged.
- F03 implemented: `release-apk.yml` now ends with its CI artifact upload; the `publish` job, its repository-write permission and GitHub Release action are removed.
- Local verification passed: installed YAML 2.6.0 parsed all three workflows and ten jobs; dependencies resolve, repository contents permissions are read-only, and no inspected step configures GitHub Release publication. Tag/manual artifact builds, both web quality modes and the existing Pages deployment remain configured.
- F03 verification did not execute hosted Actions or APK/signature checks; its workflow result is not whole-application acceptance.
- F01 code corrected: exact year-start ordinals now come from the existing calendar's numeric year lengths and fixed civil anchor, not cached local Gregorian Dates. The API, supported range, adjustments and Maghrib semantics remain; adjusted invalid Dates and years below 100 are rejected explicitly.
- F01 local evidence passed: 47 calendar/timeline tests, including 32,226 independent Hijri fixtures across 13 target timezones; zero-warning lint; both TypeScript projects; both web modes. Direct production smoke retained 01-10-1447 across Cairo-to-UTC and correctly changed the civil date across UTC-to-Kiritimati.
- Actual browser/device retained-process resume remains UNVERIFIED and is not covered by the F01 local-only approval. Step 5 subsequently passed the full Bun suite; native/runtime gates remain pending.
- F02/F04/F05 implemented: upload marks durable pointer commitment before throwable activation/reporting and deletes only uncommitted candidates; failure returns false. Compound failures retain both messages. Retirement carries the existing request token, so obsolete cleanup warnings remain observable without changing newer operation status; success notification precedes retirement to avoid hiding an immediate cleanup warning.
- Font runtime evidence: 14 cases passed against the actual TranslationProvider, valid FontFace loading and real Edge IndexedDB transactions, including both post-commit failures, denied/mismatched readback, denied pointer access, compound errors, stale upload/migration and both delayed-retirement callers. Actual page reload retained the selected binary/pointer. No unhandled rejection was observed.
- Font local evidence: 21 storage/import tests passed with 131 expectations; lint and both TypeScript projects passed. The JSX regression asset received a separate zero-warning ESLint/API check because ordinary repository lint does not select JSX. Both web builds passed with the intended updater mode separation and no test-harness markers bundled.
- Font limits: API-driven provider/transaction checks do not establish the actual settings file chooser, full accessibility/UI matrix, Android WebView behavior or native lifecycle acceptance. Dedicated font fixtures and generated web outputs were removed; normal browser/tool caches and unrelated user data were preserved.
- F06 JS corrected: native status crosses the bridge as unknown and must pass a Zod schema matching native numeric/method/madhab/string contracts, ordered bounds, matching calculation inputs and current coverage before reuse. The unused trust-shaped ScheduleStatus interface was removed; the wire protocol remains schema 2.
- F07 JS corrected: supported native IANA aliases are resolved by the existing Intl implementation before comparison with device identity. Same-offset zones with different identities/rules still require repair; no location-timezone inference or offset heuristic.
- Async reconciliation corrected: successful, failed and malformed status waits all reconsider current time/timezone. Schedule/presentation acknowledgments re-enter the existing latest-intent drain before completion. Finished status queries cancel their existing five-second deadline timer.
- Step 4 local evidence passed: 175 tests across widget-sync and prayer-timeline, 557 expectations, zero-warning lint, both TypeScript projects and both web modes. An independent real-engine factory probe distinguishes zero month work from zero schedule transfers, including the mandatory same-synchronizer recovery sequence. Separate production smoke preserved the UTC/New York Isha boundary.
- R09 JS status portions corrected: engine-derived positive baselines, isolated missing/empty revision mutations, numeric/discriminator matrix, asynchronous boundary checks and independent work counters. Step 6 subsequently established native core regression effectiveness; actual persisted-pair/revision-commit and restart evidence remain pending.
- F06/F07 full acceptance stays open: no actual Android/WebView retained-process recreation, native stored-pair corruption/revision-commit campaign or real Android/JS alias pair was executed. These are explicit runtime gates, not inferred passes from controlled adapters.
- F08 source corrected: normalization accepts safe integer adjustments with exact unit conversion instead of guessed Date headroom. Actual resulting Dates decide fasting availability; invalid events cannot enter ordering, and valid prayer/fasting siblings remain available. Existing Hijri adjusted-Date and exact-table checks remain unchanged.
- Step 5 local evidence: full Bun suite passed 299 parent tests; isolated UTC workers also execute 34 fasting and 10 Hijri cases. Independent fixtures, actual Date endpoints, precision-loss inputs, sibling preservation, lint/types, both web modes and separate production smoke passed.
- Step 5 browser evidence: 14 actual rendered-app reload cases passed with durable settings and an unrelated sentinel checked. After an automation-only DOM edit proved insufficient, the user manually typed 0 and saved through the real control. Only Suhoor changed durably; actual page reload retained the repair, every other setting and the sentinel, with valid fasting/prayer information rendered.
- New adjustment tests isolate UTC in child processes: deleting TZ in pinned Windows Bun freezes later timezone changes. The first full run exposed five timezone-transition failures; after removing shared-process TZ mutation, all full-suite cases passed without a production workaround.
- F08 is complete within the approved utility/browser scope; fixture/storage/tab/server/build cleanup was verified. Remaining R09/R10 and F01/F06/F07 native/runtime gates retain their separate approval requirements; this is not whole-application acceptance.
- Step 6 native regressions corrected: the polar case proves valid May 17-20 engine events and complete May 18/19 boundary vectors before explicit May 21 failure. Both outer-edge cases establish an accepted engine-derived horizon and successful candidate construction before checking astronomical rejection of signed one-minute and one-hour shifts.
- Step 6 verification passed: 27 freshly executed native tests, no failures/errors/skips, with 106 actionable Gradle tasks executed. Two fresh XML reports were inspected. Independent parity covers 5712 prayer and 32226 Hijri fixture rows. Separate real-engine JVM smoke and temporary fault controls passed; three old false passes are now detected, without increasing the permanent test count.
- Step 6 cleanup removed isolated build/project-cache/helper/control artifacts and two proven task-owned stopped-daemon logs. R09 remains open for actual native persisted-pair validation, failed/durable revision commits, coordinator retention/recovery and WebView/process recreation. No device authorization is inferred from the unit-only scope.

## Review Corrections (C01–C08)

Follows `Muajjin-Review-Corrections-Handoff.md`:
- C03/F03: CI unsigned publication job removed from `release-apk.yml`; current local workflow verification is recorded above and in `tasks/evidence.md`.
- C01: Fabricated-time fallback deleted from `PrayerSchedule.java:renew()`.
- C04: Outer edge validation enforced on `PrayerSchedule` constructor.
- C02: Strictly chronological web timeline sort in `derivePrayerDay()`.
- C05: Bounded adjustment normalization in `settings-storage.ts` and date guards in `time-utils.ts`.
- C06: Candidate record deletion in `translation-context.tsx` and `font-storage.ts` on failed migration.
- C07: Native schedule status inspection on cold start in `widget-sync-service.ts` and `PrayerWidgetPlugin.java`.
- C08: Accurate reporting, corrected class references, and explicit verification status.

## Second Review Corrections (S01–S07)

Follows `secend improvments.md`:
- S01: Exact outer boundary calculation validation in `PrayerSchedule.java` and `PrayerWidgetPlugin.java`.
- S02: Arithmetic date-safety validation replacing arbitrary ±120 cap in `settings-storage.ts` and `time-utils.ts`.
- S03: Observable font cleanup reporting and protected font preservation in `translation-context.tsx`.
- S04: Valid WOFF2 fixture testing post-write IndexedDB readback failure and candidate deletion in `storage-imports.test.ts`.
- S05: Revision persistence in `NativeScheduleStore.java` and retryable status query in `widget-sync-service.ts`.
- S06: Multi-day intermediate polar collapse regression in `PrayerTimelineTest.java`.
- S07: Reconciled evidence table with separated implementation, regression, and runtime status fields.

## Delivery decisions

- Keep static route imports and existing helpers/primitives; do not introduce a second framework or service worker.
- Preserve consumer-visible precision. Exact permission denied means a documented approximate OS fallback, not an exact-delivery promise.
- Preserve all seven providers and existing data. Correct layout changes retain six daily prayers and complete clock markers.
- Keep the widget instrumentation host in non-exported `src/debug` only. Launching the real WebView during native fixture capture can overwrite that fixture through the bridge.
- Scope performance claims to measured work counts and asset bytes. JavaScript grew; visible startup and physical battery savings were not measured.
- Earlier scopes left source uncommitted and the release version unchanged; the approved 2.9.0 and 2.9.1 releases superseded that restriction. Delete task-created probes/build outputs; retain permanent fixtures, isolated working runtimes and approved original-icon rollback copies.
