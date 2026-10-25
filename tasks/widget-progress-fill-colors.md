# Home-screen widget progress fill: 12-anchor OKLab progression

Status: implemented on `feature/widget-progress-color` (not released, not pushed).
Prepared: 2026-10-25. Baseline: `8fa8296` (2.9.3 prerelease evidence).

## Contract

Only the filled portion of the five bar-bearing home-screen widgets changes.
Labels, backgrounds, track, typography, countdowns, warm labels, keyguard
widgets, prayer calculations, notifications and scheduling are untouched.

The fill colour is a function of the authoritative `PrayerTimeline` interval:

```
f        = clamp((now - start) / (double)(target - start), 0, 1)
position = f * 11
colour   = mixOKLab(palette[floor(position)], palette[floor(position)+1],
                    position - floor(position))
```

- Twelve anchors at `index/11`, i.e. eleven equal segments. No 45/50% hold, no
  90/40-minute cap, no old stage thresholds on the new path.
- Anchors 0 and 1 are both `#369679`, so the first 1/11 of every interval stays
  the app green. The fill is absent at the exact start (progress 0).
- `now <= start` returns the first anchor and `now >= target` the last, so the
  helper never indexes a thirteenth colour. `target <= start` returns the first
  anchor without dividing; the renderer still hides invalid intervals.
- Exactly one opaque colour per sample; no gradient along the bar, animation,
  glow or pulsing.

Approved palettes (opaque; `res/values` and `res/values-night` hold the same
`w_progress_fill_0..11` names):

| # | dark | light |
|---:|---|---|
| 0 | `#369679` | `#369679` |
| 1 | `#369679` | `#369679` |
| 2 | `#369777` | `#359375` |
| 3 | `#359972` | `#338F6B` |
| 4 | `#339D6A` | `#308B5E` |
| 5 | `#31A25B` | `#2C8750` |
| 6 | `#2EAB42` | `#2B8538` |
| 7 | `#39B629` | `#3D831C` |
| 8 | `#6CC423` | `#63800F` |
| 9 | `#C0D61A` | `#7D790A` |
| 10 | `#EE960F` | `#B36A08` |
| 11 | `#FF0C0C` | `#D62828` |

The first green intentionally matches the app primary, not the widget label
(`w_accent` stays `#5FD3B5` dark / `#0B6E5B` light). Sampled contrast against
the unchanged track (`#34403A` dark, `#D5DDD9` light) is 2.99:1 at dark anchor 0,
2.62:1 at light anchor 0, 2.74:1 at the dark red endpoint and 2.68:1 for the
light segment-1 midpoint (WCAG relative-luminance ratio). Those early values
below 3:1 are accepted to preserve the exact app-colour match; contrast is
reported, never auto-corrected.

## Rendering

- `WidgetProgressColor` is the pure OKLab core: prepared immutable palette,
  scalar sampling, sRGB thresholds 0.04045/0.0031308, Ottosson matrices,
  gamut clamp, 8-bit round, alpha `0xFF`.
- `WidgetProgressPalette` resolves both theme lists once per process from
  configuration contexts that differ only in `UI_MODE_NIGHT_MASK`, so an
  explicit theme switch reuses the prepared palettes without a receiver or
  repeated resource work.
- `PrayerWidgetHelper.updateAllWidgets` samples each needed theme colour at most
  once per dispatch and shares the two `ColorStateList`s across all
  bar-bearing variants and duplicate instances. Keyguard-only dispatches skip
  the work entirely.
- API 31+: one green `ProgressBar` is visible; the other three stage bars are
  `GONE`. `RemoteViews.setColorStateList(id, "setProgressTintList", light, dark)`
  carries both palettes (API 31+, `NightModeReflectionAction`). The track is the
  separate `@android:id/background` layer and is never tinted.
- API 24–30: unchanged four-stage behaviour (`WidgetProgress` thresholds,
  `w_accent`/`w_progress_gold`/`w_progress_amber`/`w_progress_soft_red`). These
  devices do **not** receive the new progression and do not get the new
  starting green.
- The helper endpoint is exactly red, but a live timeline advances at the
  boundary; a widget need not display a full-red frame before resetting.

## Scheduling / battery

Colour is derived only during existing updates. The feature adds no alarm,
worker, service, receiver, wakelock, permission or network call. The cosmetic
refresh remains non-wakeup `AlarmManager.RTC` at the existing 60–300 s cadence;
Chronometer ticks never invoke colour evaluation. "Smooth" means sampled
continuity at those refreshes; Doze may delay them and the next refresh derives
the current colour immediately. No frame animation or catch-up replay exists.
Some theme-change latency remains possible when the host delays re-applying
RemoteViews, but the dual-theme tint list means no app-side dispatch is needed.

## Verification (performed)

Toolchain: isolated `.tools/jdk-21.0.12.1+1`, `.tools/android-sdk` (platform 36,
build-tools 36.0.0), `GRADLE_USER_HOME=.tools/gradle-cache`, checked-in Gradle
8.14.3 wrapper. `android/local.properties` remains absent.

```
cd android
sh ./gradlew :app:testDebugUnitTest --offline --no-daemon
sh ./gradlew :app:assembleDebugAndroidTest --offline --no-daemon
sh ./gradlew :app:lintDebug --no-daemon
sh ./gradlew :app:assembleRelease -PbuildMode=nongithub -PsignRelease=false --offline --no-daemon
```

- JVM: 50 tests, 0 failures — 5 engine parity, 23 timeline, 1 alarm receiver,
  8 legacy progress-stage, 13 new `WidgetProgressColorTest` cases (anchors,
  plateau, independent OKLab midpoints, ±1-channel continuity, endpoints,
  round trips, determinism, interval-length invariance across 90 min / 3 h /
  10 h and an overnight crossing, reset/backward sampling, gamut/alpha,
  palette ownership).
- Instrumentation on the owned API 36 AVD (`emulator-5582`, mdpi, headless),
  light and dark host configurations, `-e captureWidgetDesign true`:
  all three new tests pass — anchors/interiors/boundary fills for both themes,
  all five bar-bearing variants in LTR and RTL, host configuration selecting
  the prepared palette without redispatch, reapply reset from urgent to green,
  duplicate-instance independence, and hidden bars for unavailable data.
  Captured fill pixels equal the approved anchors exactly; track pixels remain
  `w_track`; fill width grows linearly with progress.
- Pre-existing matrix defects are unrelated and reproduce on the untouched
  baseline `8fa8296` build at the same AVD density: `table-288x220-ar` envelope
  and `lock-medium-360x96-bn` clipped countdown. They are recorded, not hidden.
- Android lint: one pre-existing error (`MainActivity` `GestureBackNavigation`)
  and pre-existing warnings; no findings in the new files. Debug and unsigned
  release APKs assemble; no debug host is packaged in release.

Runtime instructions (no production override):

```
adb -s <serial> install -r -t android/app/build/outputs/apk/debug/muajjin-2.9.3-293-nongithub-debug.apk
adb -s <serial> install -r -t android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <serial> shell cmd uimode night no|yes
adb -s <serial> shell am instrument -w -e class org.muslimtechnician.muajjin.WidgetPresentationInstrumentedTest \
  -e captureWidgetDesign true org.muslimtechnician.muajjin.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Captures are written to the debug app cache as
`widget-design-progress-<theme>-anchor<n>-<theme>.png`; the anchor-11 capture is
one millisecond before the target because the live timeline advances at the
boundary. The harness is the non-exported debug `WidgetTestActivity`; it never
launches the WebView over fixtures.

## Limits

- No real launcher, keyguard host, physical device, Doze run or API <31 device
  was exercised. Legacy behaviour is code-preserved but not runtime-verified.
- No alarm/battery dumpsys comparison was run; the absence of new scheduling
  code is a source-level statement, not a measurement.
- The emulator reports density 1.0, so the pre-existing minimum-size matrix
  failures above are not representative of every device.
