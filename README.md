<p align="center">
  <img src="public/icon.png" width="120" alt="Muajjin App Icon">
</p>

<h1 align="center">Muajjin</h1>

<p align="center">
  <strong>Prayer times on your Android home screen.</strong>
</p>

<p align="center">
  An independently maintained fork of
  <a href="https://github.com/MuslimTechnician/muajjin">Muajjin by MuslimTechnician</a>,
  adding Android home-screen widgets and improving location setup, offline
  scheduling, and day-to-day reliability. This is not an official upstream release.
</p>

<p align="center">
  <a href="https://github.com/eval19/muajjin/releases"><strong>Download Android APK</strong></a>
  &middot; <a href="#what-this-fork-adds">What's different</a>
  &middot; <a href="https://github.com/MuslimTechnician/muajjin">Original project</a>
</p>

## What this fork adds

### Prayer times at a glance

See prayer times without opening the app. Choose from five Android home-screen
layouts to suit your launcher:

- **Strip (2×1):** a compact horizontal layout.
- **Square (2×2):** a small card for a compact home screen.
- **Banner (4×1):** a wide, single-row layout.
- **Wide card (4×2):** a larger card with the daily schedule.
- **Full table (4×3):** the day's prayer times in one view.

Actual size and resizing depend on your launcher. Two additional keyguard-oriented
variants are included, but lock-screen widget availability depends on Android and
the widget host; they are not universally supported.

### Smoother first-time setup

Revised Android permission handling and cancellable, time-bounded location requests
help avoid unnecessary waits during initial setup. Approximate location permission
is supported, and obsolete detection results cannot overwrite a newer choice.
Manual location entry remains available when GPS or network services are unavailable.

### Offline widget scheduling

Android renews prayer schedules locally so widgets can keep updating without
reopening the app or fetching prayer times from a server. Widgets and optional
notifications share the native schedule. Android permissions and power-management
restrictions still affect background delivery.

### Targeted reliability improvements

This fork also includes repairs to dated prayer-time boundaries, settings saves,
font and translation imports, and background lifecycle handling. Unnecessary hidden
updates stop when inactive; background alarms stop when no widgets or notifications
are active. These are specific work reductions, not a measured battery-life or
startup-speed guarantee.

## Built on the original Muajjin

[MuslimTechnician's Muajjin](https://github.com/MuslimTechnician/muajjin) provides
the foundation. This fork retains its core capabilities:

- Locally calculated prayer times and Suhoor/Iftar times.
- Current-prayer highlighting, next-prayer countdown, and Hijri date display.
- Qibla compass and Qaza tracking.
- Light and dark themes.
- Translation and font customization.

GPS detection and IP-based fallback also originate upstream. This fork improves
their handling rather than introducing automatic location detection as a new feature.
Credit for the original app belongs to its upstream authors and contributors.

## Install and add a widget

Requires **Android 7.0 or newer**.

1. Open [this fork's Releases](https://github.com/eval19/muajjin/releases) and
   download the signed **`.apk`** asset. Do not install a source-code archive,
   an Actions ZIP, or a file ending in `-unsigned.apk`.
2. Open the APK and follow Android's installation prompts. If required, allow
   installation from the browser or file manager you used.
3. Open Muajjin and complete location and prayer settings. Use automatic detection
   or enter your location manually.
4. Long-press an empty area of your Android home screen, open **Widgets**, find
   **Muajjin** or **Prayer Times**, and drag a layout onto the home screen.
   Widget-picker labels and resizing controls vary by launcher.

**Updating an existing installation:** compatible package identity and signing
certificates are required. This fork is not automatically a drop-in update for an
APK from the original project or another distributor. Do not uninstall an existing
app as a workaround: its local settings, Qaza history, and customizations may be lost.

**Background limits:** exact-alarm or notification permission denial, Android Doze,
and manufacturer power management can delay delivery. Exact-alarm denial uses an
inexact fallback. Force-stopping the app suspends background delivery until it is
reopened; a powered-off device cannot deliver notifications. Ordinary app closure
does not require the WebView to remain running for native schedule renewal.

## Privacy and offline use

Prayer and fasting times are calculated locally. Android can renew widget schedules
offline. No account or login is required.

Automatic location setup can contact external services: GPS address lookup sends
coordinates to OpenStreetMap Nominatim, and IP fallback providers receive your IP
address. Manual location entry avoids those lookups. GitHub-distribution builds
also use the network for update checks and downloads.

The browser version has **no service worker or guaranteed offline cold-load cache**.
Local calculations do not mean the website can always open without a connection.
See the [Privacy Policy](docs/PRIVACY_POLICY.md) for storage, permissions, and
network-service details.

## Development

Built with React 18, TypeScript, Vite, Tailwind/Radix, and Capacitor 8.
Use Bun **1.2.2** and Node **22.12+**.

```bash
# Install locked dependencies
bun install --frozen-lockfile

# Start the development server
bun run dev

# Build the standard distribution (GitHub updater disabled)
bun run build

# Build the GitHub distribution (includes update checker)
bun run build:github

# Lint, app/tooling TypeScript, regressions, and both web builds
bun run validate
```

Vite alone does not type-check. Native builds require JDK **21**, SDK **36**,
build-tools **36.0.0**, and the Gradle **8.14.3** wrapper. Build the intended web
mode before Capacitor sync.

<details>
<summary><strong>Calculation, storage, and lifecycle implementation notes</strong></summary>

Prayer instants remain dated; clocks and civil grids follow the **device timezone**.
Tomorrow's Fajr is calculated rather than copied. Zero coordinates are valid;
damaged saved coordinates require repair without replacing valid preferences.
Unsupported astronomical/Hijri results are reported, not guessed.

Visible countdowns retain seconds; hidden cosmetic work and inactive compass
sampling stop. Native widgets and notifications share one update snapshot.
Required transition alarms are separate from non-wakeup cosmetic refresh.

Settings commit before success/navigation; failed saves preserve the working value.
Font migration moves verified binaries into IndexedDB and retains legacy data until
durable readback. Imports are validated and bounded, with protection against
obsolete requests activating stale selections.

The native handoff separates acknowledged calculation and presentation revisions,
reuses valid status, and retries failed delivery. All seven provider identities are
retained. The full daily widget uses a three-column, two-row table; wide clocks wrap
rather than hiding AM/PM.

Implementation decisions and recorded verification:
[plan](tasks/plan.md), [checklist](tasks/todo.md), and
[evidence and limits](tasks/evidence.md). These include measured work reductions,
bundle-size increases, device checks, and rollback caveats; they are not a claim of
universal device support or measured battery-percentage/startup improvements.

</details>

---

## Android Build

<details>
<summary><strong>Signing, APK Build, and Reproducible Verification</strong></summary>

### Keystore Setup

Only required for deliberate signed release builds. The local release package scripts and default CI runs produce **unsigned** verification artifacts, which Android cannot install. Local signing requires `-PsignRelease=true`; GitHub signing uses the explicit manual option below. Never commit a keystore, password or `android/local.properties`.

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

### Installable APK from GitHub Actions

For installation, use the signed `.apk` on
[this fork's Releases page](https://github.com/eval19/muajjin/releases),
not a source archive, Actions ZIP, or `*-unsigned.apk`. Updates to an existing
installation must retain its signing identity; upstream APK compatibility is
not established by this workflow.

Version 2.8.4 completes the pending font/import, timezone, adjustment and
widget-sync corrections. Settings save only their own fields; responsive
resizing preserves drafts; Qibla subscriptions and update requests have
owned cleanup. Invalid native schedules recover without fabricated times,
and an unsupported Hijri adjustment does not discard other widget settings.
Exact exercised checks and device limitations are in `tasks/evidence.md`.

The release workflow supports **explicit manual signing**. Version 2.8.4
(Android versionCode 284) keeps `org.muslimtechnician.muajjin`; it does not
change the minimum Android version (7.0/API 24) or lower the target SDK.
Android 15/16 can reject an unsigned APK before the app ever launches.

Configure these repository Actions secrets yourself using the **existing signing
key for this fork's release line**, only after authorizing its use in GitHub:

- `ANDROID_KEYSTORE_BASE64`: base64 encoding of that release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: its store password.
- `ANDROID_KEY_ALIAS`: the existing signing-key alias.
- `ANDROID_KEY_PASSWORD`: that key's password.
- `ANDROID_SIGNING_CERT_SHA256`: the independently confirmed signing
  certificate's SHA-256 fingerprint, with or without colons.

Do not substitute a newly generated key or the Android debug key. An update
must match the installed app's signing identity; changing keys can prevent
updates. Do not uninstall the existing app as a workaround: its local data
may be lost. Key provisioning, commits/pushes and publication are separate
authorized actions, not performed by preparing this workflow.

After the source changes are committed and pushed:

1. Open **Actions → Build release APK → Run workflow**.
2. Select the trusted source commit/tag in `ref`; enable **sign_release**.
3. Download the successful run's artifact
   `muajjin-2.8.4-284-github-release.apk`, extract the Actions ZIP, and install
   the **`.apk` inside it**, not the ZIP.

The signed path checks alignment, verifies the APK for API 24–36, requires
the pinned original certificate and records its checksum. Missing secrets,
invalid signatures or a different certificate fail the run without uploading
an unsigned substitute. Temporary decoded keys are removed on step exit.
No GitHub Release is created or modified automatically.

Tag builds and manual runs with **sign_release disabled** still produce
`*-release-unsigned.apk` for reproducibility checks only. These are **not
installable**. Debug builds use a separate `.debug` package and do not update
the release application.

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

This fork is maintained independently. Widget development and fork-specific
releases are maintained here; improvements that fit the original project's scope
may be proposed upstream. This does not imply upstream endorsement or acceptance.

For contributions to this fork, discuss substantial new features with the
maintainer before implementation. Bug fixes, performance improvements, and
localization contributions are welcome. Preserve existing features and privacy
behavior; do not expand network access or background activity incidentally.

For contributions to the original app, follow
[upstream's contribution policy](https://github.com/MuslimTechnician/muajjin#contributing):
bug fixes and performance improvements are welcome, while new features require
discussion first. Prefer focused fixes against current upstream rather than a
single pull request containing this fork's widgets, branding, and release changes.

---

## License

Released under the [MIT License](LICENSE). Original Muajjin copyright:
**Copyright (c) 2025 MuslimTechnician**. The original license and copyright notice
are retained.
