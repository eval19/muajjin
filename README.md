<p align="center">
  <img src="public/icon.png" width="120" alt="Muajjin App Icon">
</p>

<h1 align="center">Muajjin</h1>

<p align="center">
Privacy-focused, offline prayer and fasting times companion with Android home screen widgets.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Release-v2.8.0-emerald" alt="Version 2.8.0">
  <img src="https://img.shields.io/badge/React-18-blue" alt="React">
  <img src="https://img.shields.io/badge/TypeScript-5-blue" alt="TypeScript">
  <img src="https://img.shields.io/badge/Vite-7-purple" alt="Vite">
  <img src="https://img.shields.io/badge/Capacitor-8-red" alt="Capacitor">
  <img src="https://img.shields.io/badge/Android-green" alt="Android">
</p>

---

## Overview

Muajjin is a lightweight, offline-first prayer times and fasting companion app built with React and packaged for Android via Capacitor.

This fork adds Android home screen widgets and improves offline reliability, battery consumption, and calculation edge cases.

## Features

- Local prayer time calculations (no internet required)
- Android home screen widgets in multiple sizes (small strip, 2x2 square, banner, wide card with chips, and full daily table)
- Suhoor and Iftar times for fasting
- Hijri calendar display with Maghrib date transition
- Next prayer countdown
- Qibla compass and Qaza tracker
- Light and dark theme support
- Low battery footprint: background alarms stop completely when no widgets or notifications are active
- Offline schedule renewal: widgets keep calculating upcoming prayer times without needing the app to be reopened

---

## Getting Started

### Install Dependencies

```bash
bun install --frozen-lockfile
```

### Development

```bash
bun run dev
```

### Production Build

```bash
bun run build
```

Use Bun **1.2.2** and Node **22.12+**. Quality checks: `bun run validate` runs zero-warning lint, app/tooling TypeScript, regressions and both web modes. Vite alone does not type-check. Native builds require JDK **21**, SDK **36**, build-tools **36.0.0** and the Gradle **8.14.3** wrapper.

### Correctness and power behavior

Prayer instants remain dated; clocks and civil grids follow the **device timezone**. Tomorrow’s Fajr is calculated rather than copied. Zero coordinates are valid; damaged saved coordinates require repair without replacing valid preferences. Unsupported astronomical/Hijri results are reported, not guessed.

Visible countdowns retain seconds; hidden cosmetic work and inactive compass sampling stop. Native widgets/notifications share one update snapshot. Required transition alarms are separate from non-wakeup cosmetic refresh, and no active consumers means no alarms. The native engine renews an expired schedule offline. Exact-alarm/notification permission, Android Doze and OEM policy can affect delivery; **this is not a zero-battery guarantee**.

When exact-alarm permission is denied, Android uses an **inexact** fallback; precise delivery cannot be guaranteed. Force-stopping the package suspends background delivery until the user reopens it. A powered-off device cannot deliver notifications. Ordinary app closure is different: native renewal and transition delivery do not require the WebView.

Settings commit before success/navigation; failed saves preserve the working value. Font migration moves verified binaries into IndexedDB and retains legacy data until durable readback. Implementation decisions and actual verification: [plan](tasks/plan.md), [checklist](tasks/todo.md), [evidence and limits](tasks/evidence.md).

### Key Improvements in v2.8.0

- Correct zero-coordinate handling, isolated calculation-method parameters, dated overnight/Jama’ah/fasting boundaries and DST-safe Hijri dates.
- Renew native schedules locally after expiry; separate acknowledged calculation/presentation revisions and reuse one snapshot across consumers.
- Remove hidden cosmetic timers and redundant month payloads without removing visible second-level countdowns.
- Validate and durably commit settings/Qaza changes; preserve valid older data and the working selection after failed imports.
- Migrate verified font binaries to IndexedDB; bound translation/font imports and cancel obsolete location requests.
- Use semantic settings buttons and translated, 44-pixel Qaza controls with keyboard focus and native Back handling.
- Preserve all seven widgets. The large widget now fits all six prayers in a three-column, two-row table; wide clock chips wrap instead of hiding AM/PM.
- Correct manifest images, remove proven unused code/React Query, and gate unsigned releases without automatic signing/publication.

Measured work reductions, bundle-size increases, device checks, rollback caveats and unverified physical-device limits are recorded in [the implementation report](tasks/evidence.md). No battery-percentage or startup-latency improvement is claimed.

---

## Android Build

<details>
<summary><strong>Signing, APK Build, and Reproducible Verification</strong></summary>

### Keystore Setup

Only required for deliberate signed release builds. The release package scripts and CI produce **unsigned** artifacts by default; signing requires `-PsignRelease=true`. Never commit a keystore, password or `android/local.properties`.

1. Place your keystore file in:

```
android/app/
```

2. Create or edit `android/local.properties`:

```properties
keystore.file=app/your-keystore.keystore
keystore.password=your_password
keystore.key.alias=your_alias
keystore.key.password=your_key_password
```

### Build APK

```bash
# Debug APK (larger, faster build)
bun run android:build

# Unsigned release APK (optimized, smaller)
bun run android:release

# Unsigned GitHub-mode release APK (includes update checker)
bun run android:release:github
```

**APK Output Directory**

```
android/app/build/outputs/apk/
```

### Controlled builds and signed-reference verification

```bash
# Explicit mode and actual source/build epoch; new output directory required.
bun run reproducible:build --mode github --time <epoch-seconds> --output <new-directory>

# All required inputs; verification is read-only and never rebuilds/signs.
bun run reproducible:verify --apk <signed-reference.apk> --local-apk <unsigned-local.apk> \
  --metadata <build-metadata.txt> --mode github --time <epoch-seconds> \
  --apksigner <SDK-apksigner-path> --certificate <independently-trusted-SHA256>
```

The build replaces `dist`/Capacitor-generated assets, cleans Android outputs and records source/toolchain inputs; `--install` explicitly permits a frozen dependency install. `--tag <local-tag>` requires matching HEAD and uses its commit epoch, not the release publication date. Dirty/untracked source must be disclosed.

Verification first validates the APK signature and independently trusted signer, then compares every uncompressed ZIP payload entry. Only verified standard v1 signature metadata is excluded; v2/v3 blocks are outside ZIP entries. **Payload equality is not whole-file bit identity**, pristine-source proof or an independently established release identity. A checksum supplied alongside an APK is not independent trust.

Native unit tests: `cd android && ./gradlew :app:testDebugUnitTest`. Assemble the application’s runtime tests with `:app:assembleDebugAndroidTest`; run them only on an explicitly selected authorized debug emulator/device with widget-bind and notification permission. Windows uses `gradlew.bat`.

</details>

---

## Contributing

Contributions are welcome! However, please note that this project follows a **minimal feature philosophy**.

Before proposing new features, **open a discussion**. Preserve the existing app’s features and offline/privacy behavior; do not expand network access or background activity as an incidental refactor.

Bug fixes, performance improvements, and localization/translation contributions are always appreciated.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
