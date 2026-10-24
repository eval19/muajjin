# Muajjin widgets: progressive bar colors and device-localized numerals

Status: implemented in the 2.9.0 build 291 release; kept as the historical implementation contract. The 2.9.1 release fixed the widget footprints described below.
Prepared: 2026-10-24. Baseline: the widget overhaul released as 2.9.0 (Android versionCode 290; replacement build 291).

## 0. Read this first: scope and authority

You are the implementation AI. Your task is to add a restrained time-proximity color progression to existing native widget progress bars and make widget numeric presentation consistently follow the DEVICE numbering locale, independently of the app/import language.

This document specifies the intended result. It does not authorize application edits, builds, installations, device settings, signing, commits, pushes, or publication. The planning session authorized only this new Markdown file. Read the current repository instructions, inspect the actual checkout, explain your exact mutation/verification scope, and obtain implementation approval before changing code. Obtain separate authorization for device/emulator operations. Do not reuse the prior 2.9.0 publication approval for this feature.

Do not overwrite `tasks/plan.md`, `tasks/todo.md`, `tasks/evidence.md`, or `tasks/widget-visual-overhaul-prompt.md`. They contain other work and historical decisions. This is a standalone handoff. Do not reinstall tools or introduce dependencies to implement it.

The user's request is about the native widgets discussed in this conversation. All seven widget variants participate in numeral consistency; only the five home widgets that ALREADY have a progress bar receive progressive bar colors. Do not add bars to the two transparent keyguard variants. The React dashboard, app screens, notification colors, and prayer-time algorithms are not being redesigned.

Priority: correct dated prayer state and live countdown > complete readable numerals > consistent stage behavior > visual polish. A conflict is something to measure and resolve, not permission to silently remove required information.

## 1. Product decision: quiet proximity, not an alarm

The widgets must remain minimal, calm, and recognizably Muajjin. Keep the current card surfaces, green identity, spacing, rounded corners, typography, countdown emphasis, and current/next prayer cell treatments.

The progress FILL has one solid color at any instant:

1. Green: comfortably before the next event.
2. Muted gold: approaching.
3. Restrained amber: close.
4. Soft terracotta/rose-red: final moments.

This is a progression over TIME, not a multicolor horizontal gradient. Never put all four colors along one bar. No flashing, pulsing, animation, rainbow effect, glow, added icon, warning badge, vibration, sound, or notification. No Material You/wallpaper color adaptation; semantic hues must not depend on wallpaper or language.

Do not recolor the countdown digits, entire card, prayer cells, or location/date. The countdown remains primary neutral text; transparent variants retain white text. The existing warm-label treatment is a separate existing behavior, addressed explicitly in section 5.4, not a reason to recolor all text.

Red means only “the displayed next-event countdown is near its target.” It does NOT mean a missed prayer, a religious ruling, an overdue task, or a prohibited period. Do not use phrases such as “late” or “missed” based on color. Sunrise/Shuruq and the no-current interval before Dhuhr retain their existing target/state meanings.

### What consistency means

At the same dated interval, instant, theme, and usable bounds, every applicable widget selects the same semantic color stage, regardless of device language, app language, numeral script, RTL, or 12/24-hour presentation.

Device density, OEM fonts, host insets, color management, accessibility settings, and launcher clipping prevent a universal identical-pixel promise. Consistency is shared rules, hierarchy, dimensions, and complete content—not forced English digits or per-device offsets.

## 2. Numeral decision: follow the device, keep labels in the app language

Recommended and specified policy: numeric content follows the device's effective numbering locale. Do not force Western digits everywhere.

“English numbers” is imprecise. Use these terms in code reviews and documentation:

- Western/Latin digits: `0123456789`.
- Arabic-Indic digits: `٠١٢٣٤٥٦٧٨٩`.
- Extended Arabic-Indic digits, commonly Persian: `۰۱۲۳۴۵۶۷۸۹`.
- Bengali digits: `০১২৩৪৫৬৭৮৯`.

Arabic language alone is NOT sufficient to choose a digit set. Region, locale data, and an explicit Unicode numbering extension such as `u-nu-latn` can change the expected result. An Arabic-language device using Latin digits is not automatically broken. Respect the actual device locale/numbering preference; do not implement `if language == ar then replace digits`.

| Presentation element | Owner after implementation |
|---|---|
| Live countdown digit shapes | Device/launcher system numbering locale, using the existing native Chronometer |
| Next clock, current-start clock, all six daily clocks | Device numbering locale |
| Hijri day/year digits in Full Table | Device numbering locale |
| Prayer names, Now/in/at/recovery text, Hijri month names | Existing app/import translation data |
| AM/PM/day-period words | Existing app/import labels; preserve the current product contract |
| 12-hour versus 24-hour choice | Existing `timeFormat`: explicit app choice wins; `system` follows Android |
| Calendar instants and timezone | Existing dated calculations and device timezone; unchanged |
| Widget spatial direction | Existing `NativePresentation.direction` from app/import language |
| Progress stage | Numeric timestamps only, independent of every locale above |

Example: English app labels on an Arabic-Indic-numbering phone can show English prayer names with `٢:٥٣ PM`. Arabic app labels on an English-numbering phone can show Arabic names with `2:53 م`. These intentionally mixed presentations must have correct bidi isolation and complete clocks. Do not silently move AM/PM ownership to the device language; that is a different product change.

Do not transform digits embedded in user-provided location names, arbitrary imported labels, or month names. Only format owned numeric values. A place name containing a street number is not yours to rewrite.

No new settings switch is needed. Changing the app language must not change widget digit shapes; changing device numbering locale must not translate the user's selected app labels.

## 3. Evidence from the inspected implementation

Paths below are relative to the repository root. Line numbers describe the planning snapshot and may move; inspect symbols rather than applying these as edit offsets.

| Source | Observed behavior and consequence |
|---|---|
| `android/app/src/main/java/org/muslimtechnician/muajjin/PrayerTimeline.java:12-16` | `progress` is elapsed fraction, integer 0–1000. `warm` means at most 900,000 ms left and explicitly excludes Dhuhr. It is NOT a four-stage color policy. |
| `PrayerTimeline.java:17-26` | Intervals include Fajr→sunrise, sunrise→Dhuhr with no current prayer, and overnight Isha→next Fajr. Use these actual dated intervals. |
| `PrayerTimeline.java:28-32` | Cosmetic refresh is 60–300 seconds, bounded by interval duration, with the existing warm boundary hint. It is not a per-second paint loop. |
| `PrayerWidgetHelper.java:55-93` | One `WidgetTimelineState` carries numeric timestamps, Chronometer base, progress, and preformatted strings. It is shared with notifications. |
| `PrayerWidgetHelper.java:125-130` | `setupChronometer` uses a native countdown and elapsed-realtime base, not a string updated by the app every second. Preserve it. |
| `PrayerWidgetHelper.java:201-237` | Per-size rendering includes recovery visibility and normal/warm progress-view switching. Four variants have dual bars; Full Table has one. |
| `PrayerWidgetHelper.java:375-396` | Prayer transitions use wakeup alarms/exact permission fallback; cosmetic updates use `AlarmManager.RTC`, not wakeup alarms. Do not turn color changes into wakeups. |
| `NativePresentation.java:60-74` | `locale()` comes from presentation language. `clockFormatter`, `remaining`, and `hijri` currently use that locale. Clock AM/PM strings come from imported labels. Changing this globally would affect more than widgets. |
| `PrayerNotificationHelper.java:51-70,91-94` | Notification title uses `state.nextTime`; notification chip uses `presentation.remaining()`. It consumes shared state and has its own warm colors/cache. A widget formatting change can leak into notifications unless addressed deliberately. |
| `WidgetLayout.java:36-37,127-130` | Fit paint uses context locale; countdown width uses `DateUtils.formatElapsedTime`. A configured test Context is not proof that host-generated countdown text uses that locale. |
| `PrayerWidgetAlarmReceiver.java:16-20` and `AndroidManifest.xml:123-139` | Existing reconciliation actions do not include `ACTION_LOCALE_CHANGED`. A device-language change needs an explicit reconciliation path. |
| `WidgetPresentationInstrumentedTest.java:214-245,327-340,413-440` | Current fixture uses one `language` for configured Context and presentation labels. It checks numeric countdown value and context locale, not independently selected device/app numbering systems. Existing warm-bar assertions encode the old two-state implementation. |
| `res/drawable/widget_progress_bar.xml` and `_warm.xml` | Native layer-list progress drawables use the shared track and scale-based fill; geometry is already 4dp high with 2dp ends. |

The inspected production source has no `Locale.setDefault`, app-locale override setter, or per-widget process-locale mutation. Do not introduce one.

This planning work inspected source and public Android documentation. It did not build, run a device, reproduce an Arabic-host defect, or prove the proposed new design on a launcher. The observed ownership mismatch is a source fact; its precise appearance on any particular user's phone still requires native verification.

## 4. Exact color-stage policy

### 4.1 Preserve fill direction and numeric progress

The existing bar fills as the interval elapses. Keep `max = 1000` and the existing numeric `state.progress` semantics. Do not replace it with `1000 - progress`, swap to a shrinking bar, or change event selection.

Call it an interval-progress bar whose color reflects proximity to the next event. The countdown gives exact remaining time. Never label a nearly full bar “90% time remaining.”

At a new interval the numeric fill returns to the existing low/zero value and the stage returns to green. At zero fill, a green stage may not yet be visibly painted; do not draw a fake minimum-width fill that misrepresents progress.

### 4.2 Relative thresholds with absolute caps

A pure percentage policy would make the last 10% of a ten-hour night red for one hour. A pure five-minute policy could make a short interval red for too much of its life. Use capped proportions, with actual epoch-millisecond duration.

For a valid live snapshot:

```text
duration = targetTimestamp - startTimestamp
remaining = targetTimestamp - now

goldAt  = min(duration / 2,  30 minutes)
amberAt = min(duration / 4,  15 minutes)
roseAt  = min(duration / 10,  5 minutes)

if remaining <= roseAt:       SOFT_RED
else if remaining <= amberAt: AMBER
else if remaining <= goldAt:  GOLD
else:                        GREEN
```

All quantities are `long` milliseconds. Division is integer division; equality belongs to the later/warmer stage. Evaluate hottest first so overlapping thresholds on exceptionally short intervals are deterministic. Use a small package-private pure function or similarly small shared presentation policy; no framework, persistence, or configurable rules engine.

Inputs must represent a valid live interval: `state.valid`, `target > start`, and `now < target`. Invalid/recovery or already-expired snapshots must not fabricate a new interval or advertise an active urgent bar. Return an explicit unavailable result and hide the active bar using the existing recovery/presentation path. At a real event boundary, the normal timeline selection must supply the new interval; the color helper must never advance prayer IDs itself. A before-start sample can remain green while existing progress clamps to zero; do not invent negative time.

Do not derive stage from the displayed countdown, `nextTime`, `HH:mm`, translated labels, rounded minutes, localized digits, or serialized JSON strings. Do not use integer `progress` to reconstruct millisecond thresholds: its rounding loses precision. The numeric timestamps already exist.

The fraction/division approach also avoids multiplying epoch milliseconds by threshold percentages. Do not perform avoidable overflow-prone multiplication.

| Example interval duration | Gold begins when remaining ≤ | Amber begins when remaining ≤ | Soft red begins when remaining ≤ |
|---|---:|---:|---:|
| 20 minutes | 10 minutes | 5 minutes | 2 minutes |
| 60 minutes | 30 minutes | 15 minutes | 5 minutes |
| 120 minutes | 30 minutes | 15 minutes | 5 minutes |
| 600 minutes | 30 minutes | 15 minutes | 5 minutes |

These are policy examples, not assertions that prayer intervals always have these lengths. DST, high latitude, adjustments, and overnight scheduling must use actual timestamps.

Apply this policy uniformly to the current next-event countdown, including sunrise and Dhuhr. The existing no-warm-Dhuhr predicate remains unchanged for its existing consumers; it does not disable the new neutral proximity progression. This difference is intentional: the new stage is not a religious warning.

### 4.3 Refresh behavior and battery contract

Compute stage once for the snapshot/update, not independently per prayer cell, per responsive-size alternative, or per second. Every widget rendered from the same snapshot gets the same result.

Use existing dispatch, resize, transition, resume/unlock, and cosmetic refresh opportunities. Retain the current 60–300-second non-wakeup cadence and existing exact/approximate prayer-transition behavior. Do not add another service, Worker, timer, notification channel, wake lock, foreground service, or new alarm-per-stage schedule. Do not poll while the screen is off.

Consequently, this is a sampled color progression, not a guaranteed second-exact color transition. On an actively updating host, colors normally change at the next delivered cosmetic update; the nominal existing interval can be up to five minutes. Android batching, Doze, force-stop, and OEM restrictions can delay it further. A short stage may not be visible before a subsequent delivered update. A live Chronometer can continue ticking while a RemoteViews bar remains on its last snapshot.

Do not falsely claim each stage always appears or that red begins at exactly `05:00` on every phone. Do not add per-second redraws to hide this platform constraint. If the owner requires tighter color timing after seeing the native result, request a separately approved narrow change to the EXISTING non-wakeup boundary scheduling, explain battery/timing limits, and retain one scheduling owner. That enhancement is not part of this baseline implementation.

## 5. Visual tokens and RemoteViews implementation

### 5.1 Palette

Use the existing `res/values/colors.xml` and `res/values-night/colors.xml`. Green remains the current `w_accent`; add only three dedicated progress tokens, such as `w_progress_gold`, `w_progress_amber`, `w_progress_soft_red`. Do not replace generic error/warning colors or repurpose the existing warm-label token.

| Stage | Light theme | Dark theme | Intended appearance |
|---|---|---|---|
| Green | Existing `#0B6E5B` | Existing `#5FD3B5` | Muajjin green/teal identity |
| Gold | `#8A6A20` | `#CBB568` | Muted ochre/gold, not bright lemon yellow |
| Amber | `#B45C28` | `#D99A64` | Warm copper/amber, not saturated traffic orange |
| Soft red | `#B55350` | `#D98C89` | Desaturated terracotta/rose, not emergency red |

Track remains `w_track`: light `#D5DDD9`, dark `#34403A`. Surface remains light `#FBFCFB`, dark `#1B211E`.

The light-theme yellow is intentionally darker ochre: a pastel yellow on a pale track would disappear. Do not use the dark-theme gold directly on the light card merely to make the hex colors identical.

Computed WCAG relative-luminance contrast for the proposed opaque fill versus existing track (not a native screenshot measurement): light green 4.47:1, gold 3.64:1, amber 3.37:1, soft red 3.51:1; dark green 5.91:1, gold 5.33:1, amber 4.51:1, soft red 4.16:1. Each exceeds the 3:1 non-text graphical contrast target against the track. Actual edge antialiasing, host tinting, display rendering, and accessibility modes still require native inspection.

Color must not be the sole information source: bar length and visible numeric countdown remain. No user must distinguish red from green to determine the remaining time. Do not add spoken announcements on each cosmetic tick; preserve understandable accessibility text and exact countdown access.

### 5.2 Geometry and mirroring

Keep the existing full content-width bar, 4dp height, 2dp end radius, track, insets, and position. Color transitions must not change width, height, padding, visible text, countdown base, cell geometry, or selected responsive composition.

Preserve existing layout direction semantics. Check actual fill origin in RTL before changing any scale/clip gravity. A digit-script change alone must not mirror the widget. App-language direction drives layout; a localized time is kept in logical order with correct bidi handling. Do not blindly use left/right padding or force all mixed-script text LTR.

### 5.3 RemoteViews-compatible rendering choice

Preferred boring baseline: extend the existing native drawable/view technique to four mutually exclusive fixed-color progress views sharing exactly the same geometry inside the existing progress container. Use normal `ProgressBar` and supported `RemoteViews` operations only.

- Keep the existing green bar IDs where practical.
- Add dedicated gold, amber, and soft-red resources/IDs using a consistent naming convention.
- Use theme-aware color resources in identical layer-list drawables. Share resource dimensions; do not independently restyle each stage.
- Bind the same progress value to whichever views could become visible; explicitly reset visibility of ALL stages on every update/reapply. Exactly one active stage is visible for valid live content, zero for unavailable/recovery.
- All five bar-bearing variants and their compact/stacked/metadata-only alternatives must use this contract. Full Table summary-only mode still has a bar and participates.
- Prefer a small shared included bar group if it genuinely reduces duplication and preserves current view IDs/fixture access. Do not create a generic widget renderer or broad hierarchy rewrite.
- Remove obsolete widget-only two-state warm bar switches/IDs/drawables after tracing all references; do not leave old and new systems active together. Retain resources still used by live unrelated surfaces.

Reason: arbitrary methods are not automatically callable via `RemoteViews.setInt`, and setting a ProgressBar background does not recolor its fill. Do not assume `setProgressTintList`, `setProgressDrawable`, a custom View subclass, or a ColorStateList method is a compatible remote action on API 24. A single-bar implementation is acceptable only if its exact public remote action is verified across the supported API range and tested in an actual launcher; do not trade a proven baseline for a speculative tint shortcut.

Four small native bars increase view count slightly but avoid bitmap rendering, dependencies, and a divergent old-Android implementation. Do not allocate or upload a bitmap on every tick. No Compose/Glance migration, WebView widget, reflection, hidden APIs, or OEM special cases.

### 5.4 Existing warm labels and notification behavior

`state.isWarm` currently controls warm labels on Strip/Square/Banner/Wide and notification behavior. It means something different from the new stage and excludes Dhuhr.

Keep that existing domain predicate and warm LABEL behavior unchanged in this task. The bar alone adopts the new proximity stages. Do not recolor a label by reusing the bar's gold/amber/red token. A warm label may coexist with a soft-red bar near the deadline; this is deliberate preservation of the existing label contract, not a bug in stage selection.

Full Table does not gain a warm label. Keyguard variants do not gain colored bars or red countdown text. Notification color/progress/warm timing remains unchanged.

## 6. Device numerals: implementation contract and Android limitations

### 6.1 Resolve the device numbering locale explicitly

Capture one device numeric locale per dispatch/presentation snapshot. Keep it separate from `NativePresentation.language`, `direction`, imported labels, and selected clock-format policy.

- API 33+: `LocaleManager.getSystemLocales()` is the public API that explicitly ignores app-specific locale overrides. Use the first effective system locale, preserve its full language tag/numbering extension, and handle an unavailable service or empty list safely.
- API 24–32: use the system resource configuration's LocaleList as the compatibility source, not a translated app Context created for labels. Resolve a real available device default only as a documented fallback; do not hard-code English or infer locale from location.
- Do not write system/app locale settings in production, call `Locale.setDefault`, or globally mutate resources. Those approaches can alter calculations/notifications, race other work, and still do not control the launcher process.
- Do not persist device locale in the widget schedule/schema or make an app-launch synchronization necessary. It is derived native presentation state.
- Do not strip `u-nu-*` extensions by reconstructing a Locale from language alone.

Prefer one tiny package-private resolver/helper rather than scattered device-locale lookups. If method signatures are exported, inspect every reference before changing them and migrate callers cleanly.

### 6.2 Format numeric values at their owners

Preserve existing date/clock patterns and selected 12/24-hour semantics. For widget clock strings, construct the existing formatter using the DEVICE numeric locale, then preserve the imported day-period labels. Explicitly ensure its NumberFormat is for that locale and grouping is disabled. Verify actual emitted digits; `SimpleDateFormat` construction alone is not a visual proof on every Android API.

For Full Table Hijri day/year, use device-locale integer formatting with grouping disabled, keeping the imported month name and existing exact Hijri computation. Do not localize arbitrary text by search-and-replace. Do not convert an already formatted clock back to an integer.

Use one formatter per dispatch where practical. `SimpleDateFormat` and NumberFormat are mutable: do not add globally shared unsynchronized instances or caches that survive locale changes incorrectly.

Missing clocks remain the established placeholder `--:--`; stopped/recovery countdown semantics remain `—`. Never substitute a fabricated zero time, translate separator characters opportunistically, or remove day periods to make a layout fit.

### 6.3 Native Chronometer is the critical feasibility checkpoint

Retain `Chronometer`, `setChronometerCountDown(true)`, current elapsed-realtime base/deadline calculation, seconds, and native host ticking. Do not replace it with an app-updated TextView, one-second alarms, custom unsupported RemoteViews views, or bitmap digits.

AOSP `Chronometer.updateText` delegates to `DateUtils.formatElapsedTime`. In the inspected AOSP source, that formatter uses `Locale.getDefault()` in the process executing it. In a real widget, that is host-side execution. The optional Chronometer format wraps an already produced elapsed-time string; it is not a digit-shaping hook.

Therefore:

- `TextView.setTextLocale`, a configuration Context, imported Arabic labels, or app-side `Locale.setDefault` are NOT sufficient proof that live countdown digits follow the intended locale.
- Keep the Chronometer native and let the normal launcher/system locale determine its digits.
- Match app-generated widget clocks and Hijri numeric fragments to the device numbering locale so they agree on normal hosts.
- Verify digit code points on an actual Arabic-numbering device/emulator host, not just visual direction or numeric parse equivalence. Check a countdown after a real tick, not only its initial RemoteViews binding.
- Measure the host's actual number output when evaluating layouts. ASCII `23:14` is not a universal width proxy.

Known limit: an OEM launcher could use an application-specific locale/numbering override that differs from device locale. Public RemoteViews APIs do not give this app universal control over the host's `Locale.getDefault()` used by Chronometer. Do not claim a per-view text locale fixes that limitation. Test the supported hosts. If a host genuinely disagrees, record its OS/launcher version, device locale, actual digits, and available API evidence; request a decision before replacing the native countdown architecture or silently making everything English. Preserving native timing is the default. Exact digit identity on arbitrary custom hosts cannot honestly be guaranteed by this plan.

### 6.4 Protect the shared notification consumer

Do NOT globally change `NativePresentation.locale()` to device locale. Its labels, notification remaining-time strings, and existing behavior have other owners.

Introduce an explicit numeric-locale formatting path for widget-generated clock/Hijri strings, preserving the app-locale formatting path where it has a real existing consumer. Avoid obsolete aliases or a second formatter implementation.

Because `PrayerNotificationHelper` currently reads the shared `state.nextTime`, changing that field's numeric owner also changes the notification title unless addressed. The narrow required migration is: format the notification target clock from the same numeric target timestamp using the existing app-locale clock policy in the notification path, while keeping the widget snapshot device-numeric. It may call the same parameterized formatting helper; it must not parse widget text or recalculate prayer times. Preserve notification `remaining()` numeral ownership, day-period labels, warm color, progress, deadline, cache behavior, permissions, and channel configuration.

Check all other consumers before implementation. Do not duplicate the entire snapshot or schedule for one differently formatted clock. If another consumer needs a field, document and migrate it rather than silently broadening the widget-only locale policy.

This small notification call-site adjustment is necessary to avoid an accidental behavior change, not authorization to redesign notifications. Add a regression for the contrasting app/device locale pair so the boundary remains explicit.

### 6.5 Locale changes, offline operation, and fit

Add `Intent.ACTION_LOCALE_CHANGED` to the existing manifest receiver and its reconciliation predicate. Route it through the existing serialized coordinator with current consumer detection; do not introduce another receiver/service/scheduler unless required by a demonstrated platform constraint. Read fresh system locale at dispatch and update all widget instances even when Muajjin's WebView is closed and the device is offline.

Keep timezone/date-change behavior intact. Changing locale must not change schedule epochs, calculation settings, stored translations, permissions, or pending intent identity. Existing no-consumers behavior must still leave no alarms.

`WidgetLayout` must measure the strings that will actually be rendered, with an appropriate numeric/script-aware paint locale. Keep role sizes, complete clocks, shared cell height, current 288×220dp floor, and approved compact fallbacks. A numeral change must not cause a lone wrapped clock tile or a clipped Arabic/Persian digit. Honor font scale, including enlarged text. No per-prayer/per-device text shrinking.

For countdown fit, using `DateUtils.formatElapsedTime` in the app process may not match the host numeric locale. Use a small measurement-only representation formatted for the resolved device locale with the same elapsed-time shape (minute-only and hour-bearing, including two-digit hours), and verify it against native host output. This helper does not replace the live Chronometer. Preserve a conservative width budget around hour transitions; do not rely only on the current narrowest seconds.

Refresh locale-sensitive fit choices on every fresh snapshot/configuration reconcile. Do not cache one composition across different widgets, locales, densities, or font scales.

## 7. Ordered implementation work and checkpoints

These are slices, not permission to code before approval. Inspect current source and references first; prefer existing conventions and public supported APIs.

### T01 — prove numeral ownership before changing production behavior

Dependencies: implementation and test-device approval.
Likely scope: existing `WidgetPresentationInstrumentedTest.java` and native debug host fixtures only where needed.

- Split fixture inputs into independent app-label language, simulated device numeric locale, font scale, and clock-format choice. Preserve the existing matrix, do not replace it with one Arabic screenshot.
- Add a real launcher/isolated-emulator checkpoint with Arabic numbering and English app labels, and the inverse pair. Inspect live Chronometer digit code points after ticking, complete prayer clocks, and imported words.
- Confirm API 33+ system-locale lookup versus app override and the older compatibility path. Context-only native rendering is useful but not proof of host-process locale behavior.

Acceptance: documented actual numeral ownership and a feasible standard-host device policy; OEM disagreement is identified, not disguised. No global locale mutation in production. Test-only process-locale changes, if necessary for isolated unit fixtures, must be serialized, restored in `finally`, and never presented as real-host evidence.

Checkpoint: if device/host numerals cannot agree using public APIs and the existing native Chronometer, stop that incompatible-host claim and request an explicit decision. Continue reachable standard-host work; do not silently change to English.

### T02 — implement and test the pure stage selector

Dependency: T01 for shared snapshot integration; stage arithmetic can be developed independently.
Likely scope: one small package-private native stage helper plus focused JUnit tests.

- Implement section 4's exact caps, priorities, numeric types, and invalid-state contract.
- Keep no stored stage/hysteresis. Compute directly from current timestamps so time changes or rescheduling naturally recompute the correct color.
- Do not modify `PrayerTimeline.at`, `warm`, or `progress` to implement visual colors.

Acceptance: threshold equality and ±1ms cases pass; long-night, short-interval, boundary, recovery, clock reversal, and reschedule cases behave deterministically. Locale does not enter the function's signature or computation.

### T03 — apply one visual system to all existing bars

Dependency: T02.
Likely scope: color resources and native drawable family; then a second small slice for helper binding and compact Strip/Banner/Square layouts; then schedule-bearing includes/Full Table.

- Add the specified three colors in both themes and four-stage bar presentation.
- Explicitly set/reset every stage visibility/progress property on fresh apply and compatible reapply.
- Preserve progress geometry and existing warm-label behavior. Remove obsolete widget-only dual-bar plumbing after verifying all consumers.

Acceptance: all five home variants, responsive alternatives, and compact fallbacks show the same stage for the same snapshot; no inherited warm-only bar overrides it. Invalid/recovery has no active colored bar. Two keyguard variants remain bar-free.

Checkpoint: native review of the four stages in both themes at ordinary viewing size. Numerical contrast is not enough; confirm the gold is legible and the soft red is restrained. If a palette adjustment is needed, adjust the shared token for the role/theme, recompute contrast, and record the measured reason.

### T04 — implement device numeric formatting with explicit shared-consumer boundaries

Dependency: T01.
Likely scope: small device-locale helper, `NativePresentation.java`, snapshot construction in `PrayerWidgetHelper.java`; separate small slice for the notification formatting call site and fit metrics.

- Resolve full system numeric locale once; format widget-owned clocks and Hijri numeric fragments with it.
- Preserve imported text, selected 12/24-hour behavior, native Chronometer, and independent notification formatting semantics.
- Adjust native width/line-box measurement to actual emitted glyphs; preserve all six daily entries within the existing approved size envelope.

Acceptance: mixed app/device language fixtures emit the expected digit set without changing labels; all clocks/countdown/date digits agree on verified standard hosts. Notification formatting remains on its original policy. No clipped day periods, bidi corruption, global locale changes, or stored-schema migrations.

### T05 — reconcile device-language changes and verify end to end

Dependency: T03 and T04.
Likely scope: existing receiver/manifest plus focused lifecycle/instrumentation tests; then README widget explanation and truthful native preview updates only if needed/approved.

- Handle locale changes through the serialized native update flow while the app is closed/offline.
- Exercise a real countdown tick, event transition, host resize, theme change, recovery, and multiple differently sized instances.
- Update user-facing explanation: bar is elapsed progress, capped stage timing is approximate under Android delivery, numerals follow device numbering and labels follow app language.

Acceptance: actual native captures and installed-host behavior substantiate the policy; unchanged scheduling/notification/calculation boundaries are verified. No version bump, signing, publishing, or permanent debug clock override as part of this implementation unless separately requested.

## 8. Required regression and native verification matrix

### 8.1 Stage arithmetic regressions

Keep focused consumer-visible tests, not tests asserting XML token names or hard-coded source snippets.

- Normal intervals of 20, 60, 120, and 600 minutes, with all three thresholds at minus one millisecond, exact equality, and plus one millisecond.
- Stage order, minimum/cap precedence, exact current interval start, and real target rollover into the next interval.
- Invalid timeline, target <= start, expired sample, and a before-start sample without negative progress inventions.
- Updated timestamps after a clock correction or changed prayer adjustment: recompute from current values, never retain stale red.
- Overnight Isha→Fajr, Fajr→sunrise, no-current sunrise→Dhuhr. Preserve existing domain `warm` tests including no-warm-Dhuhr.
- Verify unchanged max/progress direction and unchanged Chronometer deadline across stage transitions.

### 8.2 Locale pairs that existing tests do not prove

Exercise independent inputs, not one `language` variable reused everywhere:

| Device numbering locale | App/import labels | Required observation |
|---|---|---|
| `en-US` | English | Western digits, existing English words |
| `ar-EG` or a confirmed Arabic-Indic system locale | English | Arabic-Indic owned numeric content with English words |
| `en-US` | Imported Arabic RTL | Western digits with Arabic words and RTL spatial layout |
| Arabic-Indic device locale | Bengali | Device digits with Bengali names/day periods; all clocks fit |
| `bn-BD` | English | Bengali digits where the actual device locale specifies them |
| `fa-IR` | English | Extended Arabic-Indic digits, not hard-coded Arabic substitutions |
| Explicit `ar-EG-u-nu-latn` | Arabic | Latin numbering extension retained; Arabic script does not force Arabic-Indic digits |
| Arabic regional locale that uses Latin digits by default | Arabic | Respect observed platform locale behavior, not a universal Arabic assumption |

Locale tags above are test inputs, not a guarantee that every OEM settings screen offers every extension. For code-level explicit-extension tests, compare with native NumberFormat for that exact locale and inspect emitted code points. For real-host tests, record the actual available device locale and observed host output. Do not claim an injected Context changed the whole device.

Test all seven variants for numeral policy, and all five bar variants for all four stages. Both light and dark, 12h and 24h/system, minute-only and hour-bearing countdowns, two-digit hour duration, two-digit noon hours, long translated day periods, and Hijri day/year are required.

### 8.3 Native visual and lifecycle coverage

- Real Android `RemoteViews.apply` and `reapply`, including repeated green→gold→amber→soft-red→new-green and valid→invalid→valid transitions.
- Native stage color/track checks plus equal bar bounds; actual visible stage, not just the resource ID selected by helper code.
- Full Table full-grid and summary-only; Wide six columns and 3×2; Square/Strip and Wide/Banner fallbacks.
- Two instances with different bounds: no state or locale/measurement leakage.
- Device locale changes while the app is closed and offline. Correct digits and unchanged labels after reconcile; include a real tick after locale change.
- App-label language change while device locale stays fixed: numerals stay device-owned; layout direction follows imported language.
- Font scales 1.0, 1.3, 1.5, 2.0 using appropriate supported bounds; both 2× and 3× density for narrow clock-fit cases.
- API 24–30 compatibility host and API 33+ system-locale API path; API 36 target. Do not label a build-only compatibility check as runtime proof.
- AOSP/Pixel-style launcher plus the reported Xiaomi/MIUI/HyperOS host if authorized and available. Preserve all pre-existing user widgets/data.
- Keyguard transparent variants: native host rendering only is not proof of actual lockscreen support.
- All four colors remain distinguishable enough against the track in both themes; numerical countdown/bar length remain usable without hue discrimination. Check RTL fill origin, no clipping, reduced-motion expectations (there is no new animation), and TalkBack behavior without repeated cosmetic announcements.
- Notification title/remaining chip retain their original formatting owner, warm behavior and deadline; no new alerts/permissions.
- No consumers → no alarms; no new wakeup events from color stages; exact-alarm denial behavior unchanged.

A parser that converts any Unicode decimal digit into a number is useful for countdown correctness but cannot prove digit-script correctness. Add explicit expected-code-point assertions for fixed locale fixtures. For the native Chronometer, assert that its displayed digit repertoire agrees with the actual host locale; do not merely compare `clock.getResources().getConfiguration()` or the numeric seconds value.

Keep tests deterministic with injected timestamp/state fixtures. Do not wait through an entire prayer interval. A brief native tick/event-latch smoke is appropriate for Chronometer behavior, but permanent tests must not depend on arbitrary long sleeps or real prayer times. Never launch the production WebView over synthetic native preference fixtures.

## 9. Commands and toolchain: only after implementation approval

Use Bun 1.2.2, the existing locked dependencies, JDK 21, compile/target SDK 36, build-tools 36.0.0, and the checked-in wrapper. Inspect existing isolated tool locations before use. Do not install or upgrade packages just for this feature.

Native JUnit gate, from `android/` on Windows:

```text
gradlew.bat :app:testDebugUnitTest
```

For a standard fresh debug app, from the repository root:

```text
bun run build
bun run sync:android
```

Then from `android/`:

```text
gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest -PbuildMode=nongithub
```

Use matching source epoch settings for web and native packaging. For GitHub mode, use `bun run build:github` and `-PbuildMode=github`. Do not edit generated build metadata or disable the packaging checks to bypass a mismatch.

After explicit authorization to install matching app/test APKs on a selected synthetic-data device, run the existing presentation suite with the selected tool path and device serial:

```text
.tools\android-sdk\platform-tools\adb.exe -s <authorized-serial> shell am instrument -w -e class org.muslimtechnician.muajjin.WidgetPresentationInstrumentedTest org.muslimtechnician.muajjin.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Run the full existing instrumentation suite for locale/lifecycle/notification interactions after the focused regression. Assembly does not execute it. Exact install, widget binding, language changes, screenshots and permission commands need device-specific approval; do not use the first connected personal phone automatically.

Native/Web handoff regression when shared formatting or bridge boundaries are touched:

```text
bun test tests/widget-sync.test.ts
```

If implementation genuinely reaches TypeScript/shared web contracts, run `bun run validate`; do not widen the task into React changes merely to justify that command. It leaves GitHub assets in `dist/`, so rebuild the intended mode before subsequent sync.

Do not run repository-wide formatting. In particular, do not feed Android XML into an HTML/CSS formatter that can reinterpret `@style/...` attributes. Use the existing native resource conventions.

## 10. Completion and final report

Complete means all applicable widgets implement the same specified stage policy, every owned numeric field follows the device policy on verified hosts, app-language labels remain independent, and the protected native behavior is intact.

The implementation AI's final report must include:

1. Changed source/resource/test groups and why each was necessary.
2. Exact stage caps, retained elapsed-fill direction, and approximate refresh limitation.
3. Final theme tokens and measured contrast/native screenshots; no unverified “perfect on every Android” claim.
4. Actual app/device locale pairs tested, observed countdown digit sets, and whether each was a real host or configured debug Context.
5. Proof that 12/24-hour choice, imported AM/PM labels, Hijri calculation, countdown base, progress, provider identities, click behavior, and notification formatting ownership were retained.
6. Commands/tests actually executed and any unavailable API/OEM/lockscreen coverage.
7. Device-language change behavior with app closed/offline, plus evidence that no new wakeup/per-second scheduler was added.
8. Approved final screenshot locations and cleanup status. Delete only task-created helpers/captures/debug dumps; preserve requested deliverables, existing user data, working runtimes, keys, and prior release artifacts.

Do not say done because the colors appear in XML or the APK compiles. Do not solve an unsupported host locale mismatch by forcing English, hiding seconds, rasterizing text, weakening assertions, or claiming a configured Context is a device-language test.

## 11. Sources, evidence, and remaining limits of this plan

Repository evidence is listed in section 3. The planning session inspected those sources and computed the proposed opaque sRGB color contrasts in memory. It did not modify application code, tests, package versions, settings, or GitHub.

Public Android sources consulted through the existing Obscura research tool:

- [AOSP Chronometer source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/widget/Chronometer.java): `updateText`, delegation to `DateUtils.formatElapsedTime`, host-native ticking, optional format behavior.
- [AOSP DateUtils source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/text/format/DateUtils.java): `formatElapsedTime` uses `Formatter(..., Locale.getDefault())` in the inspected source.
- [Android LocaleManager reference](https://developer.android.com/reference/android/app/LocaleManager#getSystemLocales()): `getSystemLocales`, added API 33, explicitly ignores app-specific locale overrides.

AOSP main is implementation evidence, not a promise about every OEM fork or every supported historical API. The implementer must verify the supported runtime paths and any proposed RemoteViews action before claiming compatibility.

Numerical palette contrast is calculated, not a calibrated-display or native screenshot measurement. The stage thresholds, tokens, locale resolver, and tests were implemented in 2.9.0 build 291; 2.9.1 fixed the widget footprints. This document remains the historical implementation contract.
