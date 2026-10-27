# Home-screen widget progress fill: 12-anchor OKLab progression

Status: palette revision prepared for 2.9.5 (Android versionCode 295).
The runtime progression first shipped in 2.9.4 (Android versionCode 294).
Original implementation prepared: 2026-10-25; baseline `8fa8296`.

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
- Anchors 0 and 1 reference `w_accent`: `#5FD3B5` dark / `#0B6E5B` light,
  exactly matching current-prayer text through the first 1/11 of each interval.
  The fill is absent at the exact start (progress 0).
- `now <= start` returns the first anchor and `now >= target` the last, so the
  helper never indexes a thirteenth colour. `target <= start` returns the first
  anchor without dividing; the renderer still hides invalid intervals.
- Exactly one opaque colour per sample; no gradient along the bar, animation,
  glow or pulsing.

Approved palettes (opaque; `res/values` and `res/values-night` hold the same
`w_progress_fill_0..11` names):

| # | dark | light |
|---:|---|---|
| 0 | `#5FD3B5` | `#0B6E5B` |
| 1 | `#5FD3B5` | `#0B6E5B` |
| 2 | `#89D3A1` | `#4D7D58` |
| 3 | `#AAD38C` | `#778B52` |
| 4 | `#C7D173` | `#9E9948` |
| 5 | `#E1CF53` | `#C4A636` |
| 6 | `#FACC15` | `#EAB308` |
| 7 | `#FBB929` | `#F0A824` |
| 8 | `#FCA634` | `#F69D32` |
| 9 | `#FB923C` | `#FB923C` |
| 10 | `#F66F41` | `#ED6431` |
| 11 | `#EF4444` | `#DC2626` |

The selected controls hold prayer-text green through 9.09%, reach yellow at
54.55%, shared orange at 81.82%, and red at 100%. Other literals were derived
with the existing OKLab conversion and rounding: indices 2–5 mix green→yellow
at `(index-1)/5`; 7–8 mix yellow→orange at `(index-6)/3`; 10 is orange→red at
`1/2`. No generator is retained in production; numerical midpoint references
were computed independently in double precision from the published matrices.

Among 221 equally spaced production samples per theme, minimum WCAG
relative-luminance contrast against the unchanged track (`#34403A` dark,
`#D5DDD9` light) is 1.39:1 at light yellow and 2.88:1 at the dark red endpoint.
These are weak fill/track contrasts, not a 3:1 pass. The exact selected colours
are retained without changing the track or silently darkening the palette.

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
  devices do **not** receive the new yellow→orange→red progression.
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

## Palette revision verification (2026-10-27)

Using the existing isolated JDK 21, SDK 36 and Gradle cache, from `android/`:

```
gradlew.bat :app:testDebugUnitTest --tests org.muslimtechnician.muajjin.WidgetProgressColorTest --offline --no-daemon
gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest --offline --no-daemon --console=plain
```

- Focused colour suite: 13 tests passed. Integrated native suite: 50 tests,
  zero failures/errors/skips; instrumentation APK assembled successfully.
- Disposable package-local Java main compiled the unchanged production sampler
  and checked resolved resource literals. Interval start `1800000000000`,
  duration `6600000` ms: elapsed 0/300000/600000 returned exact theme green;
  3600000 returned exact theme yellow; 5400000 returned `#FB923C` for both;
  6599999 and 6600000 returned the exact selected endpoint reds. A new interval
  beginning at the old target returned green.
- Dense output: 221 samples per theme inspected as swatches; green→yellow→
  orange→red without a visible reversal. Transitional shades remain inherent
  to OKLab interpolation; this is not an Android pixel capture.
- Rendered assertions retain independent midpoints, variants/RTL, theme
  selection, reset and hidden-invalid-bar coverage. Initial hold/reset now
  compare to `w_accent`; host-theme checks use the distinct selected yellows.
  Removed the resource-array-copy checks; no tolerance was increased.
- ADB reported no connected devices. No emulator was started, app installed,
  permission changed, or instrumentation executed. Actual Android pixels for
  this revision remain unverified; the historical captures below used the
  superseded palette. Existing flatDir, SDK XML and Gradle deprecation warnings
  remain. No signing, publication or dependency installation was performed.

## Original progression verification (2026-10-25; superseded palette)

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
