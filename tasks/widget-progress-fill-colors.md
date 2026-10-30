# Home-screen widget progress fill: 12-anchor OKLab progression

Status: final palette frozen for the 2.9.6/296 prerelease (2026-10-30),
native-render verified; publication evidence belongs in `tasks/evidence.md`.
The previous palette shipped in the
[2.9.5 prerelease](https://github.com/eval19/muajjin/releases/tag/v2.9.5)
(Android versionCode 295); its release evidence is in `tasks/evidence.md`.
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

- Twelve anchors at `index/11`, i.e. eleven equal segments. Colour holds come
  from repeated anchors, not fixed-minute caps or extra stage-timing logic.
- Anchors 0–4 in both themes reference `w_accent`: `#0B6E5B` light /
  `#5FD3B5` dark. The exact prayer-text green holds through 36.36% in both.
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
| 2 | `#5FD3B5` | `#0B6E5B` |
| 3 | `#5FD3B5` | `#0B6E5B` |
| 4 | `#5FD3B5` | `#0B6E5B` |
| 5 | `#98D993` | `#3E9558` |
| 6 | `#CAD46A` | `#8AB648` |
| 7 | `#F0CC4B` | `#E8B923` |
| 8 | `#F8B74B` | `#EEA224` |
| 9 | `#FFA24A` | `#F28A25` |
| 10 | `#FF7845` | `#EB5E2F` |
| 11 | `#F15C52` | `#DC3D3D` |

The five design targets are emerald, clean sunflower yellow, clear orange,
orange-red and clear red. Both themes hold emerald through 36.36%, then use
fresh green at 45.45% and a lime bridge at 54.55%, reaching yellow at 63.64%. Both reach
orange at 81.82%, orange-red at 90.91%, and red at 100%.
Only theme-specific tones differ; stage positions and green-hold duration match.

The longer shared green phase corrects the warm colours' disproportionate
presence: green previously occupied the short early fill, while later warm
colours occupied more of the interval with a wider bar. Both themes use the
same colour timing without changing calculations, refresh or urgency labels.
Anchor 8 remains the rounded OKLab midpoint of anchors 7 and 9 in each theme.
Targets 9–11 retain the vivid orange/orange-red/red rather than earthy shades.

Anchors 5–6 distribute the green→yellow hue change across all three existing
segments from 36.36% to 63.64%, rather than concentrating it between 54.55%
and 63.64%. The light bridge was balanced numerically before selection:
an initially proposed brighter green would merely have moved the large jump
earlier. The approved pair reduces the largest light segment's OKLab distance
by approximately 34%, with nearly equal distances across those three segments.
Dark mode uses the same timing with its own fresh-green/lime tones.

There is one intentional yellow target per theme. Transitional yellowish
shades remain inherent to smooth interpolation; fixed colour stages would be
a separate behaviour change. No generator is retained in production;
independent midpoint references use double precision.

Among 221 equally spaced independent samples per theme, minimum WCAG
relative-luminance contrast against the unchanged track (`#34403A` dark,
`#D5DDD9` light) is 1.33:1 at light yellow and 3.29:1 at the dark red endpoint.
The native anchor/interior contrast audit reports the same rounded minima.
Light mode falls below 3:1; the sampled dark progression exceeds 3:1.
Brighter light-mode yellow deliberately trades track contrast for the requested
cleaner hue; the existing track remains unchanged rather than quietly restoring
the rejected brown tone. No overall accessibility pass is claimed.

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

## Smoother yellow transition verification (2026-10-30)

- Changed only anchors 5–6 in each palette. Emerald hold, yellow/orange/red
  checkpoints, runtime sampler and refresh scheduling remain unchanged.
  Updated independent midpoint and host-theme colour references.
- Extended the existing capture-enabled native test to save its already-tested
  interior samples; there is no production preview override or extra updater.
- Offline native unit/build command below passed: 52 tests, zero
  failures/errors/skips; debug and instrumentation APKs built successfully.
- All three targeted native presentation tests passed, including unchanged
  green-hold/reset/host-theme behaviour. The duration regressions remain green.
- Saved 30 native 160×96 PNGs: all 12 anchors plus the three transition midpoints
  per theme, with four overview/detail images under
  `widget-colour-screenshots/smoother-yellow/`.
  Anchors match resource colours exactly; midpoint pixels match independent
  references within one channel. No tolerance was increased.
- Captured fill extents match between themes at every nonzero sampled position.
  Visually inspected both six-frame transition details, including halfway
  samples rather than only endpoints. Original native pixels are preserved.
- Minimum fill/track contrast remains 1.33:1 light / 3.29:1 dark; the light-mode
  limitation is unchanged. Earlier screenshots are preserved.
- Controlled-time debug native host only; no launcher/physical-device,
  alarm/Doze or live cosmetic-refresh measurement. No dependencies, signing,
  publication, animations or extra runtime updates.

## Earlier matched theme timing verification (2026-10-30; superseded bridge tones)

- Aligned the dark palette with the longer light progression: accent green
  through anchor 4, fresh greens at 5–6, yellow at 7 and orange transition at 8.
  Existing orange/orange-red/red targets remain at 9–11 in both themes.
  Light palette resource bytes are unchanged.
- Added a dark timing regression against the same green/yellow/orange
  checkpoints: it failed on the mismatched palette, then passed after alignment.
  Updated dark midpoint/host-theme references and the sampler's timing comment;
  sampling and scheduling code are unchanged.
- Offline native unit/build command below passed: 52 tests, zero
  failures/errors/skips; debug and instrumentation APKs built successfully.
- All three targeted native presentation tests passed. Saved 24 native
  160×96 PNGs, two full-progression overviews and a paired checkpoint overview
  under `widget-colour-screenshots/matched-timing/`.
- Every nonzero captured anchor fill matches its configured colour; fill
  extents match exactly between themes at all 11 nonzero anchors.
  The paired images show both themes at the same authoritative elapsed times.
- Minimum sampled fill/track contrast remains 1.33:1 light at yellow /
  3.29:1 dark at red. The light-mode limitation remains disclosed.
- Previous screenshots are preserved. Controlled-time native debug host only;
  no launcher/physical-device, alarm or Doze verification. No dependency
  downloads, signing, publication, countdown or layout changes.

## Longer light green verification (2026-10-30; superseded theme mismatch)

- Changed only the light palette's early/middle anchors: exact emerald through
  anchor 4, fresh greens at 5–6, yellow at 7 and its orange transition at 8.
  Orange/orange-red/red anchors 9–11 and all dark resource bytes are unchanged.
- Added a duration-boundary regression: it failed on the preceding palette,
  then passed with the extended emerald hold, later yellow and unchanged orange.
  Updated numerical midpoints and host-theme expectations for the same fraction.
- Offline native unit/build command below passed: 51 tests, zero
  failures/errors/skips; debug and instrumentation APKs built successfully.
- All three targeted native presentation tests passed. Captured 24 native
  160×96 PNGs and two full-progression overviews under
  `widget-colour-screenshots/more-green/`; every nonzero fill matches its exact
  anchor colour. Dark mode was captured again without changing its palette.
- Minimum sampled fill/track contrast remains 1.33:1 light at yellow, now at
  63.64% elapsed, and 3.29:1 dark at red. No accessibility pass is claimed.
- Earlier screenshots are preserved. Controlled-time debug native host only;
  no launcher/physical-device, alarm or Doze proof. No dependency downloads,
  signing, publication, layout, countdown or scheduling changes.

## Vivid ending verification (2026-10-30; superseded light green duration)

- Changed only anchors 8–11 in each theme: one transition midpoint and three
  vivid ending targets. Updated independent segment 7–10 and endpoint
  regression references. Earlier green/yellow anchors, timing, surfaces and
  text tokens remain unchanged.
- Offline native unit/build command below passed: 50 tests, zero
  failures/errors/skips; debug and instrumentation APKs built successfully.
- All three targeted native presentation tests passed: anchor/interior fill
  pixels, host-theme selection, reset and unavailable-state hiding.
- Saved 24 native 160×96 captures, two full 12-position overviews and a focused
  final-three overview under `widget-colour-screenshots/vivid-ending/`.
  Every nonzero captured anchor fill matches its exact configured hex.
  All previous screenshots are preserved.
- Independent 221-sample contrast and native anchor/interior audits agree on
  rounded minima: 1.33:1 light at the unchanged yellow / 3.29:1 dark at red.
  No overall accessibility pass is claimed.
- Controlled-time native debug host only; no launcher/physical-device,
  alarm/Doze or offline-duration claim. No timing/layout changes, dependency
  downloads, signing or publication.

## Clean yellow verification (2026-10-30; superseded ending)

- Changed only anchors 2–8 in each theme; updated independent midpoint and
  rendered host-theme regression references. Starting emerald and ending
  orange/burnt-orange/red targets, interval timing and text tokens are unchanged.
- Offline native unit/build command below passed: 50 tests, zero
  failures/errors/skips; debug and instrumentation APKs built successfully.
- All three targeted native presentation tests passed: anchor/interior fill
  pixels, host-theme selection, reset and unavailable-state hiding.
- Saved 24 native 160×96 captures and two full 12-position overviews under
  `widget-colour-screenshots/clean-yellow/`. Every nonzero captured anchor fill
  matches its configured hex exactly. Visually inspected both full progressions,
  including the intermediate tones rather than only selected target colours.
- Independent 221-sample contrast and native anchor/interior audits agree on
  rounded minima: 1.33:1 light at yellow / 3.28:1 dark at red. The light-mode
  decrease is disclosed; no track change or silent colour darkening was made.
- Existing screenshots are preserved. These are controlled-time debug-host
  captures, not launcher/physical-device or alarm-delivery proof. No dependency
  downloads, signing, publishing, timing or layout changes.

## Earlier final red verification (2026-10-30; superseded middle tones)

- Changed only anchor 11 to dark `#E2685A` / light `#C34D42`; updated the
  independent segment-10 midpoint and endpoint regression references.
  All preceding anchors, surfaces, text tokens and timing remain unchanged.
- Offline native unit/build command below passed: 50 tests, zero
  failures/errors/skips; debug and instrumentation APKs built successfully.
- All three targeted native presentation tests passed: anchor/interior fill,
  host-theme selection, interval reset and unavailable-state hiding.
- Saved 24 native 160×96 captures, two full overviews and a before/after
  comparison under `widget-colour-screenshots/red-endpoint/`. Every nonzero
  captured anchor fill matches its exact configured colour; prior captures
  are preserved. Comparison images use native pixels, not recoloured mockups.
- Minimum sampled fill/track contrast is 2.82:1 light / 3.28:1 dark, confirmed
  to two decimals by the native anchor/interior audit. Light mode remains
  below 3:1 because its earlier orange is unchanged.
- Controlled-time debug-host captures only; no physical launcher, alarm/Doze,
  downloads, signing or publication. Final-frame countdown timing is synthetic.

## Warmer endpoint verification (2026-10-30; superseded final red)

- Changed only design targets 10–11: burnt orange and warm brick red, replacing
  the salmon/pink ending. Updated independent references for segments 9–10.
  Anchors 0–9, interval timing, refresh, surfaces and text tokens are unchanged.
- Repeated the offline native unit/build command below: 50 unit tests,
  zero failures/errors/skips; debug and instrumentation APKs built successfully.
- All three targeted native presentation tests passed again: anchor/interior
  pixels, host-theme selection, reset and unavailable-state hiding.
- Saved 24 native 160×96 captures and two labelled overviews under
  `widget-colour-screenshots/warmer-endpoints/`. Every nonzero captured anchor
  fill matches its palette hex exactly. Earlier captures remain untouched.
- The native contrast audit reports 2.82:1 minimum light / 3.11:1 dark.
  These remain controlled-time native-host captures, not launcher, physical
  device or alarm-delivery verification. No downloads, signing or publication.

## Initial tone revision verification (2026-10-30; superseded endpoints)

With the existing isolated JDK 21, SDK 36 and Gradle cache, offline:

```
gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest -PbuildMode=nongithub -PsourceDateEpoch=0 --offline --no-daemon --console=plain
```

- Native unit suite: 50 tests, zero failures/errors/skips, including 13 colour
  tests with independent midpoint references. Debug and instrumentation APKs built.
- Read-only API 36 emulator: the three targeted presentation tests passed
  (`approvedPaletteDrivesEveryAnchorAndInteriorOnTheNewPath`,
  `hostConfigurationSelectsThePaletteWithoutAnotherDispatch`,
  `reapplyResetsUrgentFillAndHidesUnavailableBars`).
  This exercised native anchor/interior fill pixels, all five bar-bearing
  variants, host-theme selection, interval reset and unavailable-state hiding.
- Captured 12 interval positions per theme using the existing debug native host.
  All 24 original PNGs decode as 160×96; every nonzero anchor's captured fill
  matches its resource hex exactly. Screenshots and five-target overview images:
  `widget-colour-screenshots/new-tones/` from the repository root.
- The overview images enlarge native captures without recolouring. Their final
  frame is one millisecond before the target; its `-00:01` countdown is a
  synthetic capture-timing artefact, not a claim about live boundary delivery.
- Only palette resources, colour regression references and this document changed.
  No source timing/calculation/refresh changes, dependency downloads, signing,
  publication or release-version change. Android 7–11 retains its legacy palette.
- These are native debug-host renders, not real launcher/physical-device
  screenshots. No alarm, Doze, keyguard or long-running delivery claim follows.

## Previous palette revision verification (2026-10-27; superseded palette)

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
