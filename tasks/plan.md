# Muajjin implementation plan

Source specification: `C:/Users/Mohamed Tawfig/Desktop/Muajjin-Improvement-Implementation-Plan.md` (T00–T17). All tasks remain in scope; this file records execution decisions, not a reduced replacement specification.

## Approved boundaries

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

## Delivery decisions

- Keep static route imports and existing helpers/primitives; do not introduce a second framework or service worker.
- Preserve consumer-visible precision. Exact permission denied means a documented approximate OS fallback, not an exact-delivery promise.
- Preserve all seven providers and existing data. Correct layout changes retain six daily prayers and complete clock markers.
- Keep the widget instrumentation host in non-exported `src/debug` only. Launching the real WebView during native fixture capture can overwrite that fixture through the bridge.
- Scope performance claims to measured work counts and asset bytes. JavaScript grew; visible startup and physical battery savings were not measured.
- Leave source uncommitted and release version unchanged. Delete task-created probes/build outputs; retain permanent fixtures, isolated working runtimes and approved original-icon rollback copies.
