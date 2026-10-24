# Repository Guidelines

## Project Overview

Muajjin is a privacy-focused prayer/fasting companion with Qibla, Qaza tracking, translation/font imports, and seven Android widget variants. React 18 + TypeScript + Vite + Tailwind/Radix, packaged with Capacitor 8. Calculations run locally; Android renews schedules offline. The browser has **no service worker or guaranteed offline cold-load cache**. Location lookup and GitHub-mode updates are network exceptions; see `docs/PRIVACY_POLICY.md`.

## Architecture & Data Flow

- **Composition:** `src/main.tsx` mounts theme/translation providers; `src/app.tsx` composes `AppProvider`, static routes, guards, and `CapacitorApp` integration. No React Query provider. Guards use committed `settings.onboardingComplete`; `/upcoming-prayer-times` is intentionally unguarded.
- **State:** `use-local-storage.ts` shares one external store per key through `useSyncExternalStore`. `persistent-storage.ts` normalizes and durably saves before publishing; same-tab subscriptions and cross-tab storage events reconcile values. `AppProvider.updateSettings(partial)` returns success and merges nested Jama’ah siblings. Translation/fonts, theme, Qaza, and dashboard layout have separate owners; dashboard order/visibility do not come from `AppSettings.homeSections`.
- **Time:** `prayer-times-local.ts` creates fresh Adhan method parameters and actual dated instants; `time-utils.ts` derives adjacent-day prayer/prohibited/fasting windows for the dashboard. **Epoch milliseconds drive calculations; civil dates/clocks use the device timezone, not an inferred location timezone.** Calculate tomorrow’s Fajr; never copy today’s value or advance calendar dates by a fixed 24 hours.
- **Native handoff:** `widget-sync-service.ts` separates calculation/schedule and presentation revisions. It serializes a 30-date padded horizon only when required, reuses valid native status, and caches only durable acknowledgments. Latest intent wins; failed acknowledgments remain retryable. Ordinary web does not build native month payloads.
- **Android:** `PrayerUpdateCoordinator` serializes renewal/rendering around one parsed snapshot. `PrayerSchedule` renews through native Adhan/Umm al-Qura implementations without WebView/network. Widgets/notifications share the snapshot; transition alarms and non-wakeup cosmetic refresh are separate. No consumers means no alarms; exact-alarm denial falls back to approximate delivery.
- **Lifecycle:** `clock-scheduler.ts` / `use-visible-clock.ts` schedule boundaries or visible cadence, stop while inactive, and reconcile on resume. Identity consumers do not tick every second. Qibla heading work stops while inactive.
- **Imports:** `TranslationProvider` validates bounded imports and prepares fonts before committing selection. Font binaries live in IndexedDB `muajjin-fonts`; localStorage holds schema-2 selection metadata. Latest-request guards prevent stale activation; retain legacy base64 until migration is verified.
- **Distribution boundary:** only Vite mode `github` maps bare `@/features/update-checker` to `index.github.ts`; all other modes use disabled `index.ts`. Keep callers on the bare import: subpath imports bypass the boundary.

## Key Directories

- `src/services/`, `src/utils/`: validated I/O boundaries, persistence, dated domain calculations; `src/hooks/`, `src/contexts/`: lifecycle and state ownership.
- `src/pages/`, `src/features/`, `src/components/`: route/settings screens, feature modules, reusable UI; `src/i18n/`: English/Bengali templates.
- `android/app/src/main/java/org/muslimtechnician/muajjin/`: native schedule engine, storage, worker, alarms, widgets, notifications. `native-geolocation-plugin/` is the canonical local plugin; `src/plugins/` contains its typed bridge.
- `tests/`: Bun regressions, `native/` parity fixtures/provenance, `browser/` interactive font checks. Android JUnit/instrumentation live under `android/app/src/test/` and `android/app/src/androidTest/`.
- `scripts/`: controlled builds and APK verification. `tasks/plan.md`, `tasks/todo.md`, `tasks/evidence.md`: decisions/status/evidence; later sections may be historical, not current acceptance criteria.

## Development Commands

Run from the repository root unless noted. These commands may install dependencies or write build outputs; inspection alone does not require running them.

| Purpose | Command |
| --- | --- |
| Locked install | `bun install --frozen-lockfile` |
| Dev server, port 8080 / GitHub mode | `bun run dev` / `bun run dev:github` |
| Zero-warning lint / app + tooling types | `bun run lint` / `bun run typecheck` |
| Domain/service suite | `bun run test` (same as `bun run test:domain`) |
| Focused regression | `bun test tests/<file>.test.ts` or `bun test tests/ -t "<name>"` |
| Full web gate | `bun run validate` |
| Standard / GitHub web build | `bun run build` / `bun run build:github` |
| Preview / Android sync | `bun run preview` / `bun run sync:android` |
| Debug APK, standard / GitHub | `bun run android:build` / `bun run android:build:github` |
| Unsigned release APK / AAB | `bun run android:release` / `bun run android:bundle` |
| Native unit tests, from `android/` | `./gradlew :app:testDebugUnitTest` (Windows: `gradlew.bat`) |
| Assemble instrumentation APK, not execute it | `./gradlew :app:assembleDebugAndroidTest` from `android/` |
| Controlled unsigned build | `bun run reproducible:build --mode nongithub --time <epoch> --output <new-directory>` |
| APK verifier options | `bun run reproducible:verify --help` |

- `validate` runs **lint → typecheck → tests → standard build → GitHub build**, leaving GitHub assets in `dist/`. Vite builds do **not** type-check. `typecheck` covers `src/` and three root tooling configs, **not tests/scripts**.
- Build the intended web mode **before** Capacitor sync. Native packaging rejects missing or mismatched `build-info.json` mode/epoch; native unit tests do not require web assets.
- Controlled builds require explicit mode and time (or a local tag matching HEAD), reject existing output directories, replace generated web/native assets, and clean Android outputs. Dependency installation requires `--install`; Gradle may still download/cache tooling.
- APK verification is read-only: verify the reference signature against an independently trusted certificate fingerprint, then compare uncompressed ZIP payloads. Equality is **not whole-file bit identity or pristine-source proof**.
- `bun run format` is repository-wide `prettier --write .`, not a check-only command; avoid using it for unrelated cleanup.

## Code Conventions & Common Patterns

- Functional components/hooks; PascalCase components/types, `use...` hooks, kebab-case files. Reuse `@/` aliases, existing UI primitives, `cn()`, and CSS-variable theme tokens. Prettier: 2 spaces, single quotes, 80 columns, Tailwind class sorting.
- Static imports for known modules; separate top-level `import type`, not inline type specifiers or `import("pkg").Type`. Prefer `Record` for small static key lookups; use `Map`/`Set` for dynamic membership. No speculative DI framework: reuse functional seams such as `ClockEnvironment`, storage access, widget adapters, and calculator/translator arguments.
- Treat persisted/imported/network values as unknown; reuse Zod and normalization, preserving valid siblings. Report failed durable saves before success/navigation. Initial missing coordinates may use defaults; `0` is valid; damaged saved coordinates remain unavailable, never another city’s times under the old label.
- Preserve method IDs, including legacy `15 → 12`, and madhab `0` = Shafi / `1` = Hanafi. Stable prayer IDs drive logic; translated names and `HH:MM` are presentation. Preserve inclusive end minutes, overnight Jama’ah, DST, and actual-Maghrib Hijri rollover. Exact Hijri support is 1900-04-30 through 2077-11-16; unsupported calculations must fail, not approximate.
- Async work owns cancellation/cleanup and latest-intent guards. Location requests are user-initiated and bounded; reverse-geocoding failure keeps valid GPS coordinates. Respect boundary-specific errors: calculations throw, persistence retains committed state, and native bridges reject failed delivery. Never log private location/history/import data.
- Add keys to both `src/i18n/template-en.json` and `src/i18n/template-bn.json`; preserve placeholders, RTL, reduced motion, keyboard labels, visible errors, and native back behavior.

## Important Files

- `src/main.tsx`, `src/app.tsx`, `src/components/capacitor-app.tsx`: composition, routing, native reconciliation.
- `src/contexts/app-context.tsx`, `src/hooks/use-local-storage.ts`, `src/utils/settings-storage.ts`, `src/utils/persistent-storage.ts`: normalized settings and durable ownership.
- `src/services/prayer-times-local.ts`, `src/utils/time-utils.ts`, `src/utils/hijri-utils.ts`: religious-time contracts; `src/services/widget-sync-service.ts`: acknowledged native handoff.
- Native `PrayerWidgetPlugin.java`, `NativeScheduleStore.java`, `PrayerSchedule.java`, `PrayerUpdateCoordinator.java`, `PrayerWidgetHelper.java`: persistence/renewal/consumer pipeline under the Java directory above.
- `src/services/location-service.ts`, `src/utils/font-storage.ts`, `src/utils/translation-validation.ts`, `src/contexts/translation-context.tsx`: cancellation/import boundaries.
- `package.json`, `.bun-version`, `bun.lock`, `vite.config.ts`, `capacitor.config.ts`, `eslint.config.js`, `tsconfig.app.json`, `tsconfig.node.json`, `android/app/build.gradle`, `android/variables.gradle`, `.github/workflows/`: command, validation, distribution, and artifact contracts.

## Runtime/Tooling Preferences

- Use **Bun 1.2.2** and `bun.lock`; do not introduce another lockfile. Node **22.12+** is required by the controlled build; CI selects **22.14.0**. Python 3 is required by APK tooling **and the Bun regression suite**; `PYTHON` overrides interpreter selection.
- Android: **JDK 21**, compile/target SDK **36**, build-tools **36.0.0**, min SDK **24**, Gradle wrapper **8.14.3**. Use the checked-in wrapper. Reproducibility scripts require Bash/POSIX utilities; package Android scripts also use `./gradlew`, so do not assume PowerShell-native execution.
- Never edit copied bundles/generated plugin files. `android/settings.gradle` preserves the canonical local geolocation plugin path after sync. `.tools/` is gitignored isolated verification tooling, not an app dependency.
- Release scripts default to **unsigned**. Signing requires deliberate `-PsignRelease=true` and local keystore configuration; never commit keystores, passwords, or `android/local.properties`. Build metadata can expose local paths/filenames: review before sharing.

## Testing & QA

- Use **`bun:test`, not Jest/Vitest**. Regressions cover domain boundaries, lifecycle clocks, persistence/imports, cancellation, native-sync races, and Python-backed APK checks. Use injected clocks/storage/bridges rather than sleeps; preserve timezone subprocess isolation on Windows. No percentage coverage threshold is configured.
- Native **JUnit 4** covers timelines/cadence and exact engine parity. Engine upgrades require port/license/provenance review and regenerated independent fixtures (`tests/native/PROVENANCE.txt`, `tests/native/generate-fixtures.mjs`), not increased tolerances. Fixture generation writes files; it is not a test run.
- Lint excludes Android and the native plugin; Gradle covers them. CI validates both web modes and conditionally runs native unit tests/debug assembly plus instrumentation-APK assembly. It does **not** execute instrumentation tests. Release artifacts are unsigned by default; the release workflow does not publish a GitHub Release. Pages uses the standard, updater-disabled build.
- Runtime instrumentation needs an explicitly authorized debug device/emulator and widget-bind/notification permissions. Keep `WidgetTestActivity.java` under `android/app/src/debug/` non-exported; do not launch the WebView over native fixtures. Verify all seven providers, duplicate instances, compact resize, RTL, and both themes.
- `tests/browser/font-ownership.html` is an interactive real-IndexedDB/FontFace check, not an automated package suite. Use an approved fresh localhost origin and valid licensed `customfont.woff2`; never inject failures into personal-data storage.
- Unit tests and APK assembly do not prove real font decoding, import/reload/navigation, alarm delivery, or long-running offline behavior. Android permission, Doze, force-stop, and OEM limits remain real; CI success does not authorize signing or publication.
