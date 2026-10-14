# Repository Guidelines

## Project Overview

Muajjin is a local prayer/fasting companion with upcoming times, Qibla, Qaza tracking, translation/font imports, and seven Android widget variants. React 18, TypeScript, Vite, Tailwind/Radix and Capacitor 8 provide the UI. Bundled Android calculations and native schedule renewal work offline; the browser has **no service worker or guaranteed offline cold-load cache**.

## Architecture & Data Flow

- `src/main.tsx` mounts theme/translation providers; `src/app.tsx` composes `AppProvider`, router/guards and native back-button integration. Routes use static imports; there is no React Query provider.
- `AppProvider` owns normalized settings through the committed external store in `persistent-storage.ts`. `updateSettings(partial)` returns durable-save success and merges nested Jama’ah siblings. Qaza/customization use the same hook/store convention; same-tab subscriptions and cross-tab storage events reconcile committed values.
- `prayer-times-local.ts` invokes fresh `adhan` method factories and returns actual dated instants. `time-utils.ts` derives chronological prayer/prohibited/fasting windows; the dashboard memoizes one coherent result. **D1:** calculations retain their physical instants but civil grids/clocks use the device timezone, not an inferred location timezone. Tomorrow’s Fajr is calculated, never copied.
- `widget-sync-service.ts` serializes a validated 30-row padded schedule only when calculation inputs/timezone/horizon change. Presentation has a separate acknowledged revision. Latest intent wins; failed acknowledgments remain retryable; ordinary web never prepares a native month payload.
- Android `PrayerUpdateCoordinator` owns one serial worker and one parsed snapshot per dispatch. `PrayerSchedule` renews locally through the exact native adhan/Umm al-Qura implementations; widgets and notifications reuse that snapshot. Required transition alarms and non-wakeup cosmetic refresh are separate; no consumers means no alarms. Android permission/Doze/OEM limits remain real.
- Visible clocks use `clock-scheduler.ts`/`use-visible-clock.ts`: second ticks only for visible seconds, identity changes at actual boundaries, immediate resume reconciliation, no hidden cosmetic polling. Qibla heading work also stops while inactive.
- `TranslationProvider` validates bounded imports and prepares fonts before committing selection. Font binaries use IndexedDB `muajjin-fonts` database version 1 with schema-2 records/pointers; legacy base64 remains until durable verified migration. `LatestFontRequest` prevents stale asynchronous selection.
- Bare `@/features/update-checker` resolves to `index.github.ts` only in `github` build mode; other modes retain the intentionally disabled implementation.

## Key Directories

- `src/pages/`, `src/features/`: routes/settings and Qibla, Qaza, upcoming times, distribution-specific updater.
- `src/services/`, `src/utils/`, `src/hooks/`, `src/contexts/`: validated boundaries, dated domain logic, lifecycle clocks and persistence owners. `src/i18n/` contains English/Bengali templates.
- `android/app/src/main/java/org/muslimtechnician/muajjin/`: native engine, schedule store, worker, alarms, widgets and notifications. `native-geolocation-plugin/` is the canonical local plugin source; `src/plugins/` is its typed bridge.
- `tests/`, `tests/native/`: Bun regressions and native reference fixtures/provenance. Native JUnit and Android runtime tests live under `android/app/src/test/` and `src/androidTest/`.
- `scripts/`: explicit-mode unsigned builds and read-only APK verification. `tasks/plan.md`, `tasks/todo.md`, `tasks/evidence.md`: implementation decisions, acceptance status and exercised evidence. `docs/PRIVACY_POLICY.md` documents network/storage exceptions.

## Development Commands

Run from the root; Android Gradle commands run from `android/`.

| Purpose | Command |
| --- | --- |
| Locked install | `bun install --frozen-lockfile` |
| Development / GitHub updater | `bun run dev` / `bun run dev:github` |
| App + tooling type checks | `bun run typecheck` |
| Zero-warning lint / domain regressions | `bun run lint` / `bun run test` |
| Full web gate, both modes | `bun run validate` |
| Standard / GitHub build | `bun run build` / `bun run build:github` |
| Preview / native sync | `bun run preview` / `bun run sync:android` |
| Debug APK, standard / GitHub | `bun run android:build` / `bun run android:build:github` |
| Unsigned release APK / AAB | `bun run android:release` / `bun run android:bundle` |
| Native unit suite | `./gradlew :app:testDebugUnitTest` |
| Android runtime-test APK | `./gradlew :app:assembleDebugAndroidTest` |
| Controlled unsigned build | `bun run reproducible:build --mode nongithub --time <epoch> --output <new-directory>` |
| Signature + payload verification | `bun run reproducible:verify --help` for all mandatory inputs |

Build before standalone Capacitor sync. The build script replaces generated assets, cleans Android outputs and records inputs; dependency installation requires explicit `--install`. The verification script is **read-only**: it requires an existing signed reference, unsigned local APK, metadata, explicit mode/epoch, SDK apksigner and independently trusted signer fingerprint. Payload equality is not whole-file bit identity or proof of pristine source.

## Code Conventions & Common Patterns

- Functional components/hooks; PascalCase components/types, `use...` hooks and kebab-case files. Use `@/`, existing UI primitives, `cn()` and CSS-variable theme tokens. Prettier: two spaces, single quotes, 80 columns.
- Known modules use static imports. Top-level `import type`; no inline type specifiers or `import("pkg").Type`. Small static string-key lookups use `Record`; reserve Map/Set for genuinely dynamic membership/iteration. Avoid speculative helpers and dependency-injection frameworks; injectable scheduler/bridge adapters are focused test seams.
- Treat persisted/imported/network values as unknown. Reuse Zod and existing normalization, preserve valid siblings, and report save failure before success/navigation. Missing initial coordinates may use defaults; zero is valid; damaged saved coordinates remain unavailable and require repair, never another city’s times under the old label.
- Preserve method IDs including legacy `15 → 12`; madhab `0` Shafi / `1` Hanafi. Epoch milliseconds are canonical; `HH:MM` is display only. Inclusive end minutes, overnight Jama’ah, DST and Maghrib Hijri rollover are domain contracts. Exact Hijri table range is 1900-04-30 through 2077-11-16; unsupported results are errors, not approximations.
- Async operations use owned cancellation/cleanup and latest-intent guards. Location requests are user-initiated and bounded; address lookup failure retains valid GPS coordinates. Native bridge failure must not claim delivery. Do not log private location/history/import data.
- Use translation helpers; add keys to **both** `template-en.json` and `template-bn.json`, preserve placeholders and RTL. Keep reduced motion, keyboard labels, visible errors and native back behavior.

## Important Files

- `src/contexts/app-context.tsx`, `src/utils/settings-storage.ts`, `persistent-storage.ts`: settings ownership, migrations and durable updates.
- `src/pages/index.tsx`, `src/services/prayer-times-local.ts`, `src/utils/time-utils.ts`, `hijri-utils.ts`: coherent religious-time calculations.
- `src/services/widget-sync-service.ts`, native `PrayerWidgetPlugin.java`, `PrayerSchedule.java`, `PrayerUpdateCoordinator.java`, `PrayerWidgetHelper.java`: acknowledged bridge and offline consumer pipeline.
- `src/services/location-service.ts`, `src/utils/font-storage.ts`, `translation-validation.ts`, `src/contexts/translation-context.tsx`: cancellation/import boundaries.
- `package.json`, `.bun-version`, `bun.lock`, `vite.config.ts`, `capacitor.config.ts`, `android/app/build.gradle`, `android/variables.gradle`, `.github/workflows/`: runtime, build-mode, artifact and gated CI contracts.

## Runtime/Tooling Preferences

Use **Bun 1.2.2** and `bun.lock`; no additional package-manager lockfile. Capacitor requires Node **22.12+**. Android uses JDK **21**, compile/target SDK **36**, build-tools **36.0.0**, min SDK **24**, Gradle wrapper **8.14.3**. Windows native commands use `gradlew.bat`; package/reproducibility shell commands require compatible Bash. Never edit copied bundles or generated plugin files; settings.gradle preserves the canonical local plugin path after sync. Release signing requires deliberate `-PsignRelease=true` and local keystore settings; never commit credentials. Controlled builds require explicit source epoch and mode, not publication time. Local `.tools` is verification tooling, not an app dependency.

## Testing & QA

Bun covers domain boundaries, visible/hidden clocks, persistence/imports, bounded location cancellation, native synchronization races and APK ZIP/signature scope. JUnit checks native timeline/cadence and exact engine reference parity; no arbitrary coverage percentage is configured. Android runtime regressions require an explicitly authorized debug emulator/device with widget-bind and notification permission. `android/app/src/debug/java/org/muslimtechnician/muajjin/WidgetTestActivity.java` is a non-exported, debug-only host declared in `android/app/src/debug/AndroidManifest.xml`: do not launch the WebView over native fixtures. Check all seven providers, duplicate instances, compact resize, RTL and both themes. Tests do not replace runtime import/reload/navigation and real alarm delivery checks. CI gates do not authorize signing or publication.
