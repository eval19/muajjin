# Muajjin implementation evidence

## Scope, decisions and environment

Source specification: the Desktop `Muajjin-Improvement-Implementation-Plan.md`, T00–T17. Source changes and the local checks below are delivered; the physical-device and signed-release limits at the end remain explicit. This is an uncommitted implementation, not a published release or certification of every section-9 device configuration.

Approved: repository/native/tests/config/CI/assets/docs, local QA scripts/cleanup, isolated Bun 1.2.2 and locked dependencies, portable JDK21/SDK36/licenses, verified IndexedDB migration and one task-owned API36 emulator with QA-only permission/clock/lifecycle manipulation. No personal device, global PATH/rule mutation, commit, publication, production signing, new application dependency, endpoint change or service worker.

- D1: preserve device-timezone civil dates/clocks, not an inferred selected-city timezone.
- D2: exact compatible native adhan/Umm al-Qura ports with existing upstream attribution, not a longer cache or background WebView. See `tests/native/PROVENANCE.txt` and `android/app/src/main/assets/native-engine-licenses.txt`.
- D3: packaged Android works offline; browser offline cold-load caching remains unpromised because no service worker was added.
- Windows x64; Bun1.2.2; Node26.8.1; TypeScript5.6.3; Vite7.3.1; React18.3.1; Capacitor8; adhan4.4.3; @umalqura/core0.0.7. Android: Temurin21.0.12.1, SDK/build-tools36, Gradle8.14.3.
- Native runtime: only owned `emulator-5556`, API36 Google APIs x86_64, application `org.muslimtechnician.muajjin.debug`, airplane mode, manual fixtures. No personal-device manipulation.
- Browser: Edge155, owned localhost preview tab. Window identity was checked, but later occlusion/hidden state prevented trustworthy physical-visible input/paint timing. Real Android UiAutomation touch, Android file chooser, keyboard and Back supplied actual surface proof; browser-only DOM inspection is not represented as physical input.

## Final executed gates

| Check | Observed outcome |
| --- | --- |
| `bun run validate` | Zero-warning lint, app/tooling TypeScript, 64 Bun tests / 0 failures / 225 assertions, both web modes passed |
| Standard web build and Capacitor Android sync | Passed; actual generated assets copied before native packaging |
| `:app:testDebugUnitTest` | 19 passed: 5 native engine parity + 14 timeline cases; XML recorded zero failures/errors/skips |
| `:app:assembleDebug :app:assembleDebugAndroidTest` | Passed; final debug app and permanent runtime-test APK installed successfully on the owned emulator |
| Permanent Android runtime suite | 2 tests passed in each final light/dark run; latest 3.876s / 3.330s |
| Temporary actual UI probes | Menu/Back; Qaza increments/reload/decrements/focus; valid font import/invalid replacement/offline reload; native language/clock presentation all passed; probes removed after proof |
| Final controlled unsigned release builds | Standard twice and GitHub twice, explicit epoch1790769003; all four exited0 |
| Final strict unsigned payload comparisons | Both modes MATCH: 484 entries each, names/sizes/uncompressed SHA-256; no arbitrary payload exclusions |
| Deliberately mismatched packaging inputs | Wrong mode and wrong epoch each exited1 with the intended bundled-web guard message |

Hosted CI was configured, not executed remotely or published. Local gates cover the relevant workflow commands. Existing capacitor.js/non-module, large-chunk, old Browserslist, Gradle flatDir/deprecation and SDK-XML warnings remain disclosed; no test failure was silenced to make the gates pass. An ordinary build without SOURCE_DATE_EPOCH warns; controlled builds supply it explicitly.

The final repeated-build metadata recorded base commit `582918511dc7783e25e50810b1c4cf5d9bafcee8` and identical tracked source delta `1f110c2b0151e5abc9a0133c600334c04be178c757e7cc2168a9dbe369c91467` across all four builds. The source was intentionally dirty/untracked; the commit is not asserted to contain this implementation. Final documentation was updated after those builds. The debug-only widget test-host type was absent from both actual release DEX files.

## Per-task changes and evidence

### T00 — Executable baseline

- Recorded initial app/tooling type checks, lint failure/warnings, standard/GitHub builds, real onboarding/dashboard, resource sizes and original bridge/timer work. Original coordinate regression failed4/5 before its fix.
- Initial lint had one empty-catch error and nine fast-refresh warnings. Original standard/GitHub JS sizes are preserved in the performance ledger below.
- An original Gradle immutable-workspace rename failure persisted with a short/fresh cache. Vite was watching temporary Gradle/cache trees and later crashed; excluding task/build outputs and stopping that watcher allowed compilation without a Gradle upgrade. Watcher-lock attribution is an inference, not proven causality. Matching upstream report: https://github.com/gradle/gradle/issues/38921.
- No comparable physical CPU/battery or visible startup baseline was obtained. Hidden DOM readiness is not a substitute for paint timing.

### T01 — Meaningful gates

- `package.json`, TypeScript tooling config, `eslint.config.js`, `tests/`, Android JUnit/runtime tests and `.github/workflows/` establish lint/type/domain/both-mode/native gates.
- Regression cases cover calculation boundaries, storage failures/races, hidden clocks, bridge acknowledgments and unsafe APK ZIP/signature scope; the APK regression executes12 Python cases inside its Bun case. Template-only native tests were replaced with consumer-visible checks.
- Final 64/19/2 results are above. App-scoped Android test tasks avoid running unrelated Cordova template test projects; no relevant failed test is excluded.
- Behavior: development failures are surfaced before release. No app permission or data-schema change comes from these gates. Rollback would remove protections, not restore a required app dependency.

### T02 — Coordinates and selected methods

- `src/services/prayer-times-local.ts`, settings/location controls: finite bounds, valid zero/negative coordinates and fresh selected-method parameters. Hanafi state cannot leak into later Shafi calculations.
- Tests first reproduced zero/method faults; canonical tests cover all existing method IDs, both madhabs through independent native fixtures, invalid arguments and polar failures. Legacy15→12 remains supported.
- Behavior correction: use the requested location/method, not a zero-coordinate fallback or a previous call's parameters. Offline calculation retained; no new timezone service or network endpoint.

### T03 — Dated prayer, fasting and Hijri boundaries

- `src/utils/time-utils.ts`, `hijri-utils.ts`, `src/services/prayer-times-local.ts`: retain dated epoch instants, calculate real tomorrow Fajr, previous Isha, overnight Jama’ah and adjusted fasting/prohibited windows. DST-safe civil ordinals prevent a repeated/missing Hijri day; Maghrib rollover and adjustment apply once.
- Bun regressions exercise exact/inclusive end boundaries, sunrise gaps, tomorrow fasting targets, midnight/month/year and supported Hijri range. Independent native fixtures include leap dates, DST forward/back, skipped local dates, antimeridians, methods and both madhabs.
- Behavior correction: old copied Fajr, wrong overnight start and DST date defects change to actual values. Device-timezone policy remains D1. Unsupported astronomy/Hijri results are errors, never guesses.

### T04 — Coherent dashboard and load migration

- `src/pages/index.tsx`, `src/contexts/app-context.tsx`, `src/utils/settings-storage.ts`: one coherent derived prayer/prohibited/fasting result; migrate and validate at load, not in a remount loop. Missing persisted coordinates stay unavailable rather than mixing one saved coordinate with a default city's other coordinate.
- Actual dashboard/settings/reload and zero-coordinate Android surfaces were exercised. New storage regressions reproduce damaged coordinates and explicit unselected onboarding madhab; valid siblings, Jama’ah and preferences survive.
- Behavior correction: no unrelated city under the old label, and onboarding cannot silently complete without a madhab choice. Reset/loading defaults remain deliberate; no all-user-data reset.

### T05 — Schedule/presentation bridge separation

- `src/services/widget-sync-service.ts`, native `PrayerWidgetPlugin.java`, `NativeScheduleStore.java`: schema-2 calculation and presentation contracts; acknowledged revisions, complete invalidation, serial latest-intent delivery and retryable failed acknowledgments.
- Web never prepares a native month payload. Twenty stable native requests do not rebuild/resend it; presentation changes leave physical prayer instants unchanged. Bun race cases cover a late old result and a newer request during cache-hit completion.
- Actual Android native persisted schedules/notification/widgets and real renewal were checked. Transient transfer failure retains known-good state rather than claiming success. Downgrade must account for the new native schedule/presentation schema.

### T06 — One native snapshot per dispatch

- `PrayerUpdateCoordinator.java`, `PrayerWidgetHelper.java`, `PrayerNotificationHelper.java`: one serial worker owns schedule parsing/timeline/presentation; all widget and notification consumers reuse it. Formatters/state are shared within the dispatch; notification primitive state avoids redundant reposts.
- Native timeline regressions and real eight-instance rendering exercised the pipeline, including all seven provider identities. Review found no remaining required native lifecycle issue before final surface fixes.
- Behavior unchanged except correctness fixes. This removes redundant work; Java allocation/CPU percentages were not profiled and are not claimed.

### T07 — Consumer-aware scheduling without fake precision

- `PrayerWidgetHelper.java`, `PrayerWidgetAlarmReceiver.java`, native helpers: necessary transition RTC_WAKEUP is separate from plain RTC cosmetic refresh. No consumers cancels both; legitimate notification/widget consumers restore work. Resume/boot/update/timezone/time/permission events reconcile through the native worker.
- Actual notification-denied/no-widget case cancelled both app alarm types. Exact permission denied produced an approximate alarm with a one-hour window; allowed produced window0 / permission / allow-while-idle flags. The permission-gated real Fajr screen-off/Doze transition delivered and scheduled sunrise next.
- Cosmetic refresh was deferred during Doze instead of waking the device. Android's chronometer still owns countdown display; no one-second notification repost, perpetual service, persistent wakelock or new GPS/network polling.
- Required platform limits: denied exact permission is not exact; force-stop, device-off and OEM policy cannot be eliminated by an app refactor.

### T08 — Complete offline native renewal

- `NativePrayerCalculator.java`, `NativeSolarMath.java`, `NativeCalendarMath.java`, `NativeHijriCalculator.java`, `PrayerSchedule.java`, store/coordinator/receivers: full compatible local engines and saved validated inputs; no extended static-cache workaround, guessed times, internet or background WebView.
- Exact installed-engine reference fixtures are committed with generator/provenance/licenses. Native tests compare independent JavaScript results, including invalid-event throws rather than loose tolerance.
- Actual API36 instrumentation seeded a65-day-expired schedule and renewed it before any WebView. Actual OS clock advanced65days with app closed; the worker produced30 valid rows covering the advanced epoch. Timezone changed to America/New_York and renewed headlessly; Africa/Cairo and automatic time/zone were restored and read back.
- Restore validated configuration/schedules together when downgrading; source rollback alone does not migrate schema2 back. No claim of months of physical-device unattended uptime.

### T09 — Foreground precision with no hidden cosmetic polling

- `src/utils/clock-scheduler.ts`, `src/hooks/use-visible-clock.ts`, dashboard/date widgets and Qibla: shared lifecycle/boundary clock, second precision only where shown, no hidden cosmetic timers, immediate resume reconciliation and bounded active heading work.
- Four scheduler tests cover hidden mounting, resume past transitions, forward/back clock correction and cleanup. Actual browser hidden probe observed no scheduled cosmetic intervals; ordinary web bridge payload work is zero. Compass work is inactive when hidden rather than running GPS in the background.
- Display precision retained. Work-count improvements are measured below, not converted into an invented battery percentage.

### T10 — Durable validated persistence

- `persistent-storage.ts`, `settings-storage.ts`, app/Qaza/customization contexts: updates read the latest committed value, preserve siblings, validate external/reset events and publish only after durable save. Multi-key failure rolls prior writes back; visible success is gated on persistence.
- Bun covers batched increments, quota/denied storage, recovery, corrupt JSON, deletion/clear, same/cross-tab state and multi-key rollback. Actual Qaza add3/reload/subtract3 retained and restored the original count using real touch; manual zero-coordinate settings remained usable offline.
- Corrupt entries are repaired without erasing valid older counts/preferences. Export/back up user data before any future downgrade; do not use a blanket reset as migration.

### T11 — Atomic bounded imports

- `src/utils/font-storage.ts`, `translation-validation.ts`, `src/contexts/translation-context.tsx`, font/translation controls: maximum5MiB `customfont.woff2`, header and real FontFace checks, verified IndexedDB transaction/readback and latest-intent protection; translation maximum1MiB before FileReader/parse plus bounded depth/leaves/strings, placeholders, BCP language/direction and prototype-key validation.
- Actual browser legacy46654-character base64 migrated to a34924-byte binary/schema-2 record and small pointer. Injected IndexedDB denial retained the working legacy font/error; retry migrated successfully. Database version is1; record/pointer schema is2.
- Real Android chooser imported the valid font, rejected a34-byte replacement without changing its working pointer, and retained the font after airplane-mode reload. Replacement upload stays available while a font is selected. Bun covers malformed/oversized imports, older partial translation defaults, corrupted collection repair and late selection/save failure.
- Browser hidden-state upload-helper completion did not deliver React change input; it is not claimed as a successful import. Real native chooser proof supplied that path. Full Android frontend RTL-JSON-picker interaction was not separately certified; compatible partial Arabic native payload and RTL surfaces were checked.
- Downgrade limitation: old source may not understand schema-2 font pointers. Preserve the original WOFF2/export and data before a separately approved reverse migration; verified migration does not mean legacy base64 is kept forever.

### T12 — Native language, clocks and fresh Hijri dates

- `NativePresentation.java`, `NativeHijriCalculator.java`, native snapshot/helpers and presentation bridge: localized labels/direction,12h/24h/system format and fresh adjusted Hijri data without altering event instants. Removed the arbitrary±30 Hijri adjustment restriction while retaining actual table/integer bounds.
- Actual native en/bn/compatible partial ar cases in all three clock modes retained event identity/epoch and parsed13:05 correctly; English1:05PM/13:05 and Bengali digits were observed. SDK±120 checks match29 Dhu al-Hijjah1447 and3 Ramadan1448 for the fixed date.
- Real native Arabic RTL rendering, duplicate instances and resize were exercised in light/dark configurations. Partial imports legitimately use English fallback for missing labels/months; no full Arabic translation completeness claim.

### T13 — Proven dead-code/dependency cleanup

- Compiler graph observed145 source modules,99 reachable from main/GitHub updater roots. The old settings page/dialog had no incoming references; the old wrapper's live selector caller was migrated before deleting those three obsolete files.
- Removed the sole unused React Query provider/direct dependency through Bun's normal manifest/lock update. Preserved static route imports, the intentionally disabled non-GitHub updater and all seven stable Android provider identities.
- Both builds/gates and real offline navigation retained supported features. No unrelated dependency upgrade, fabricated dependency-injection layer or user data deletion.

### T14 — Measured bounded improvements

- Correct PNG dimensions/MIME and smaller icon/favicon; remove ordinary-web/month-resync work and hidden polling rather than speculative lazy routes/abstractions.
- Three fresh-process original/current work runs used the same locked runtime, UTC, Makkah coordinates, method1/Shafi,13:00 start, one priming sync then20 logical30-second ticks. Actual original modules were exported from the recorded base commit; production modules were not rewritten to manufacture the baseline. External bridge acknowledgments were controlled successful adapters, not Android CPU/battery measurements.
- Exact counts and asset/bundle sizes below. Keep independent correctness/clarity improvements; do not claim that the larger JS bundle became smaller or visible startup became faster.

### T15 — Deliberate release and honest verification

- `scripts/reproducible-build.sh`, `verify-reproducible.sh`, `apk-payload.py`, Vite/build-info, Android Gradle and workflows: explicit mode/epoch/new output, no implicit install/sign/publish, recorded dirty inputs and checksums, guard mismatched embedded mode/time. Read-only verification separates trusted signature identity from ZIP payload equality.
- Final standard/GitHub unsigned payloads each matched their second build after the final resource fixes. Every484 entry was compared; safe original filenames reject traversal/NUL/backslash/duplicates/symlinks/encryption. Actual wrong-mode/wrong-epoch packaging failed intentionally.
- No trusted signed reference/certificate was provided. Neither signature authenticity nor whole-file bit identity nor pristine-source reproducibility is asserted. Production signing/publication requires separate authorization. Workflow policy rollback is separate from app-data rollback.

### T16 — Cancelled bounded location and honest recovery

- `src/services/location-service.ts`, `use-location-detection.ts`, native/web geolocation plugin and setup/settings: one user-requested acquisition,25s overall/15s GPS/5s fetch bounds, underlying cancellation, latest-intent/unmount guards, fine OR coarse permission, distinct errors and preserved valid GPS on failed address lookup.
- Existing HTTP IP provider and HTTPS fallback remain unchanged; no endpoint-change approval was given. HTTPS pages may block legacy cleartext and fall back. No background auto-detection/network retry loop.
- Actual airplane-mode GPS denial/timeout retained saved manual0/0 settings and user control. Three permanent service regressions prove offline geocoding retains0/0, abort releases a pending watch and prevents late publication/IP fallback, and denied GPS plus failed providers reports permission failure instead of a fabricated location.
- Native late-permission cancellation logic was inspected; all real OEM permission-dialog/out-of-order combinations were not certified. The OS permission sheet is not forcibly dismissed by application cancellation.

### T17 — Actual usability, native layout and documentation

- Semantic settings buttons, localized44-pixel Qaza controls, existing Radix sheets, native Back and restored focus; reduced-motion CSS; actual dated upcoming picker; correct relative512PNG manifest declaration. README/privacy/root `AGENTS.md` distinguish Android offline from browser caching and record supported tooling/architecture.
- Real Android touch/Back opened/closed the menu. Qaza touch, TAB focus, hardware Back, row-focus restoration and reload persistence passed. The menu was rendered and usable at130% WebView text zoom in dark configuration with no horizontal overflow.
- Real native rendering exposed Arabic glyph layout24px inside13px in the large widget, then still24/18 after header-only compaction. The full daily table now uses three columns/two rows, retaining all six names/times and stable IDs at declared250×220dp; real full-glyph/no-label-ellipsis regression passes. Wide chips previously ellipsized3 characters of `5:02 AM`; two-line clocks now keep the meridiem, with zero actual clock ellipsis in regression and visible capture.
- Permanent SDK suite covers all seven variants plus a duplicate, add/remove,80/220dp compact resize, Arabic RTL and light/dark; independent future-event selection and actual chronometers/notification are checked. A non-exported debug-only `WidgetTestActivity` isolates capture: launching MainActivity's WebView had overwritten native fixtures before screenshots. No production WebView was disabled to hide that QA race.
- Pixel captures inspected the changed menu, full large daily table and complete wide clock markers. Long labels, larger system font scales, other launchers/Android versions and full TalkBack remain explicit validation limits, not universal accessibility claims.

## Performance ledger

Same-input sync probe: three independent fresh Bun processes for each version/mode, primed once,20 logical ticks spaced30seconds. All count rows were identical across the three runs. Native adapter priming in the new implementation legitimately performs32 prayer calculations and2 acknowledged transfers for padded adjacent-day correctness; ordinary web priming performs none. These are work/serialization measurements, not a ten-minute physical-device battery run.

| Metric per20 warm ticks | Original | Current ordinary web | Current native adapter |
| --- | ---: | ---: | ---: |
| Full-month prayer calculations | 600 | 0 | 0 |
| Bridge transfers | 20 | 0 | 0 |
| Serialized bridge payload bytes | 199920 | 0 | 0 |
| Isolated helper wall-time range, ms | 25.56–27.10 | 0.26–1.38 | 2.53–3.19 |

Wall-time scope is only this helper under controlled adapters. It is not total app CPU, render/startup time, native allocation accounting or battery attribution. Retain the eliminated-work changes because exact repeated counts improve without disabling behavior; do not generalize tiny timing samples into percentage battery/CPU claims.

| Build/asset metric | Original | Final observed |
| --- | ---: | ---: |
| Standard JS, KB / gzipKB | 582.86 /179.41 | 640.91 /195.80 |
| GitHub JS, KB / gzipKB | 595.66 /183.20 | 648.35 /196.23 |
| CSS, KB / gzipKB | 72.56 /12.73 | 71.47 /12.71 |
| Icon, dimensions / bytes | 1280×1280 /362432 | 512×512 /73817 |
| Favicon, dimensions / bytes | 1280×1280 /362432 | 32×32 /1173 |

JavaScript grew; this is disclosed, not described as a bundle/startup optimization win. Image savings and elimination of native-only web work are deterministic improvements. Original hidden timer counts continued increasing; after-change hidden cosmetic clock has no timers. Original observation durations were uncontrolled, so those counters are not a battery benchmark. Occluded DOM-ready samples were rejected as paint/startup evidence. Native APK/DEX increase versus an original app build was not measured.

## Real offline/lifecycle scenario detail

- Packaged airplane-mode cold start/reopen, home/settings/upcoming/Qaza/menu and actual import/reload paths exercised without loading route chunks from the network; routes remain static. Manual zero coordinates stayed saved; upcoming picker selected the actual next calendar date.
- Actual expired horizon: native instrumentation seeded65days-old inputs, renewed before a WebView and rendered actual future times. Separate real OS clock jump65days produced30 rows covering the advanced epoch; original clock policy/timezone was restored and verified.
- Actual timezone: Africa/Cairo→America/New_York renewed headlessly with new dated horizon; restored Africa/Cairo, automatic time1, automatic timezone1. Exact-alarm app-op restored to default; night setting restored to its original value1.
- Actual Fajr delivery: exact permission explicitly allowed; pending RTC_WAKEUP window0 / flags0x5; Home/background process kill, screen off and forced deep Doze. No post-deadline manual broadcast was used to deliver the transition. The OS alarm advanced the real notification to `Now: Fajr`, next sunrise, chronometer deadline1791956580000 and the next exact sunrise alarm. Plain RTC cosmetic work was deferred roughly59minutes under Doze. This proves one accelerated permitted OS-alarm scenario, not overnight/OEM timing guarantees.
- Notification denied with zero widgets cancelled both app alarms; granting permission restored legitimate consumer work. Denied exact permission was observed as approximate, including retention in forced Doze; it is never described as exact.
- Owned emulator reboot/background/resume/process recreation were exercised during QA; device powered-off/force-stop limitations remain platform limitations. User force-stop suspends delivery until reopening; do not equate ordinary app closure with force-stop.

## Review, failed attempts and decisions

- Parallel native lifecycle and storage/import review slices completed. Storage findings (partially missing coordinates, unselected madhab, corrupted stored-translation handling, pre-read import limit) were fixed with regressions. Native review had no remaining required lifecycle finding; later real UI glyph/meridiem defects were reproduced and fixed, not ignored.
- Failed software-GPU emulator rendering was diagnosed through Android traces: HardwareRenderer/WebViewFunctor wait and system/app ANRs while the prayer worker was idle. Reusing the same owned AVD with the existing SDK's swiftshader backend restored real interaction. Attribution to that emulator renderer is an inference; no production hardware-acceleration disable or daemon/browser repair was added.
- Native chooser recent-files indexing/animation/offscreen-coordinate probes initially failed; real device-root chooser navigation and settled visual frames fixed the QA harness, not the app MIME restrictions. Real font replacement accessibility was an app defect and was corrected.
- A QA text-zoom call used the wrong WebView API, then was corrected to WebSettings; that temporary probe is not application code. An eager Bun mock rejection produced a fixture-only unhandled promise and was changed to lazy rejected fetch behavior; actual cancellation semantics passed.
- No new route-lazy framework, global install, background service, cache-only expiry concealment, fake prayer values, weakened test gate or hidden unsupported API fallback was retained.

## Second Review Corrections (S01–S07) Evidence

| ID | Implementation | Failing-before evidence | Passing regression evidence | Actual runtime evidence | Remaining criterion/blocker |
|---|---|---|---|---|---|
| **S01** (Real outer boundary validation) | Added `validateOuterBoundaries(Calculation)` in `PrayerSchedule.java`; enforced in `PrayerWidgetPlugin.java:updateSchedule()`, `getScheduleStatus()`, and `NativeScheduleStore.loadSchedule()`. | REPRODUCED: constructor and load paths accepted outer edges shifted by hours within day bounds. | `PrayerTimelineTest.validEngineDerivedSchedulePassesOuterBoundaryValidation`, `slightlyShiftedFirstPreviousIshaFailsOuterBoundaryValidation`, and `slightlyShiftedLastNextFajrFailsOuterBoundaryValidation` using engine-derived Makkah baseline. | UNVERIFIED (Requires physical Android device or emulator bridge run with injected corrupt stored schedule). | Automated emulator/device instrumentation of persisted recovery path. |
| **S02** (Arithmetic usability over arbitrary caps) | Removed arbitrary ±120 cap in `settings-storage.ts`; validated representable `Date` limits with 1e14 ms headroom (`MAX_DATE_HEADROOM`). Guarded timestamps in `time-utils.ts`. | REPRODUCED: valid 121-min and 121-day adjustments were falsely rejected as corrupt; 2e12 headroom risked future Date overflow. | `storage-imports.test.ts`: 121 and -121 preserved; `MAX_SAFE_INTEGER` and NaN normalize safely to 0 without corrupting siblings. | REPRODUCED: Web test suite dashboard derivation and settings reload with valid non-default offsets. | None |
| **S03** (Ownership-safe & observable font cleanup) | Logged/reported cleanup failures with compound error propagation in `font-storage.ts` and `translation-context.tsx`. `uploadFont` cleans uncommitted candidates directly; `cleanupRecord` preserves font on `localStorage` read error; request ownership guards stale migration errors. | SOURCE-ESTABLISHED: empty catch swallowed deletion failure; `cleanupRecord` inferred orphanhood on localStorage read errors; upload candidates leaked when localStorage threw. | `translation-context.tsx` and `font-storage.ts` compound error on cleanup denial; `storage-imports.test.ts` test for compound cleanup error. | UNVERIFIED (Requires physical browser session with provoked storage quota/permission denial). | Real browser quota denial test. |
| **S04** (Real post-write verification regression) | Replaced ineffective 50-byte zero buffer test with a valid WOFF2 header fixture testing post-write readback verification failure and candidate cleanup; restored `globalThis.indexedDB` in `finally`. | REPRODUCED: old test threw synchronously at `validateWoff2` before IndexedDB or cleanup was reached. | `storage-imports.test.ts`: `failed binary verification cleans up candidate record from store` and `failed binary verification with deletion failure raises compound error`. | REPRODUCED (Unit mock IndexedDB in Bun); UNVERIFIED (Real browser/WebView IndexedDB transaction execution). | Real browser IndexedDB execution. |
| **S05** (Durable native revision & status retry) | Fixed `NativeScheduleStore.saveSchedule()` to persist `prayer_schedule_revision` when schedule JSON matches. In `widget-sync-service.ts`, re-sample time/timezone post-await, enforce `schema === 2` and safe integer bounds, and reset `checkedNativeStatus` on failure. | REPRODUCED: matching schedule JSON never wrote revision; status failure permanently disabled future status checks; test never called sync a second time. | `widget-sync.test.ts`: two-pass `failed status query does not permanently lock out subsequent retry`, `missing revision in native status rejects reuse`, and `near-expiry native schedule triggers renewal`. | UNVERIFIED (Requires real Android process restart while retaining native preferences). | Cold-start WebView recreation on actual Android runtime. |
| **S06** (Intermediate polar collapse regression) | Added test starting on May 20, 2026 at latitude 69 where initial days compute but subsequent day collapses. Retained single-sort timeline in `time-utils.ts` and artifact-only CI workflow in `release-apk.yml`. | SOURCE-ESTABLISHED: prior test failed on day 0, never exercising in-progress renewal collapse. | `PrayerTimelineTest.intermediateAstronomicalCollapseFailsRenewalWithoutFabrication`, `prayer-timeline.test.ts` NYC/UTC chronological ordering, `release-apk.yml` artifact-only workflow. | UNVERIFIED (Requires physical device renewal at high latitude boundary). | Physical high-latitude GPS device run. |
| **S07** (Status & claim reconciliation) | Reconciled evidence table with explicit implementation, regression, runtime, and blocker fields with honest `UNVERIFIED` designations for physical-only criteria. | SOURCE-ESTABLISHED: blanket "None" blocker claims replaced with separated verification fields. | Documented in `tasks/evidence.md`, `tasks/plan.md`, `tasks/todo.md`. | Inspected against actual source diff and executed test results. | None |

## Rollback and remaining release limits

1. Source is uncommitted and release version is unchanged. Revert only reviewed task-owned changes; do not use blanket reset/clean against unrelated user edits. Original image rollback copies are retained in `.tools/asset-backups/`.
2. A source downgrade does not reverse IndexedDB/schema2 settings/schedule changes. Back up/export settings, counts, original font bytes and relevant native state with appropriate approval before downgrading. Preserve working data until replacement/reverse migration succeeds; reconcile/cancel obsolete alarms rather than leaving both schedules active.
3. No physical-device battery percentage, controlled app CPU profile, native allocation count or visible startup latency was established. Reduced work is proven; numerical battery savings are not.
4. API36 emulator/local modes do not establish OEM overnight reliability, every supported Android version, launcher/lock-screen implementation, full TalkBack or largest-system-font behavior. Longer names/large accessibility fonts can need larger widget space.
5. No independently trusted production signer/reference was available. Controlled unsigned payload equality is not signer identity, authenticated release content, ZIP whole-file equality or pristine-source reproducibility. No production signing/publication was performed.
6. Android bundled assets/engine remain offline. GPS address/IP fallback and GitHub-mode update checks are existing network exceptions; no new domain or service worker was added. Browser offline cold opening remains unpromised.

## Cleanup status

Verified: owned browser font pointer/database removed, cache-disabled probe reset, only owned Edge tab closed and original harness attachment restored; owned preview stopped. Owned emulator test APK uninstalled, task-created app fixtures/external chooser/screenshot data cleared, QA widget-bind grant revoked, working final debug app left installed and owned emulator stopped. No personal app data or user browser profile was cleared.

Filesystem cleanup verified: removed 66 enumerated task-owned artifacts/directories plus 48 stopped isolated Gradle daemon logs; exact-scope existence checks report no remaining listed probes, screenshots, installer archives, alternate failed cache or generated build/reproducibility outputs. Gradle's isolated daemon was stopped. Permanent source/reference fixtures, project dependencies, isolated working runtimes/dependency caches, AVD runtime and original-icon rollback copies remain. Pre-existing/unrelated data and browser profiles were not purged. Build commands regenerate the cleared web/native outputs; build and sync before standalone Android packaging.
