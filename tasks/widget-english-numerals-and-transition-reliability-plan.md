# Widget English numerals toggle and prayer-transition reliability

Status: planning only. No application code has been changed.

Prepared: 2026-10-24. Baseline: 2.9.1 / Android versionCode 292, source
`43fe901af72b02af5c456f371a5bffefda35b09f`.

This document is a handoff for a separate implementation AI. It records the
source findings, the recommended design, the platform limits, and the tests
needed to make the change coherent. It does not authorize builds, commits,
device changes, signing, or publication.

---

## 0. Read this first: one hard decision

The request has two independent goals:

1. A settings toggle that makes widget numbers English.
2. Stop the widget from showing the previous prayer with a negative countdown
   after the next prayer has begun.

Goal 1 is fully implementable for the numbers the app formats itself:

- every widget clock;
- Hijri day/year;
- any app-generated countdown text.

Goal 1 is **not fully implementable for the live seconds countdown** while it
remains an Android native `Chronometer`. The countdown text is produced in the
launcher process by AOSP `Chronometer.updateText()`, which delegates to
`DateUtils.formatElapsedTime()`. That formatter uses the launcher process's
`Locale.getDefault()`. The public `RemoteViews` API can set the Chronometer
base, format wrapper, countdown direction, and started state; it cannot change
the digit set chosen inside the host process.

The repository already records this limitation:

- `WidgetNumeralInstrumentedTest` excludes `Chronometer` from its static digit
  assertions.
- Its live-countdown test explicitly asserts the **device** digits, not the
  app/import digits.
- `WidgetLocale.elapsedTime()` is measurement-only and says so.
- The 2.9.0 handoff cites AOSP `Chronometer` and `DateUtils`.

Therefore choose one product behavior before implementation:

### Option A — keep live seconds; English applies to clocks, dates, and generated text

- Toggle on: widget clocks, Hijri day/year, and any app-generated numeric text
  use Latin digits.
- Toggle off: those values follow the device numbering locale, exactly as now.
- The live seconds countdown keeps the launcher's digit set on non-Latin
  numbering hosts. The setting description must say this.
- Best accuracy and best battery behavior. No new alarm cadence.

### Option B — strict English, minute-level static countdown

- Replace the live `Chronometer` with an app-formatted countdown text on the
  affected widget views.
- The text is refreshed by the existing 60–300 second cosmetic dispatch and by
  prayer transitions.
- Every widget number is English in English mode.
- Negative countdown is impossible by construction.
- Cost: seconds are no longer live. Display minutes, or a static `H:MM` value
  that is explicitly refreshed, rather than misleading stale seconds. Expect up
  to the existing cosmetic interval of staleness.

### Option C — per-second app updates

Rejected. It requires either:

- one alarm per second, or
- a foreground service / persistent process, or
- a custom RemoteViews view / bitmap pipeline.

All three violate the requested CPU/battery optimization and are fragile across
API 24–36 and OEM launchers.

**Recommendation:** implement Option A first, because it preserves the live
countdown and adds no per-second work. If the owner requires every digit to be
English, implement Option B and accept minute-level countdown accuracy.

The transition reliability fix in section 4 is required for both options.
Option B additionally removes the negative-display symptom.

---

## 1. Current implementation map

### 1.1 Numerals

`WidgetLocale.resolve(context)`:

- API 33+: `LocaleManager.getSystemLocales()`; first system locale.
- API 24–32: system resource `LocaleList`.
- Fallback: `Locale.getDefault(Locale.Category.FORMAT)`.
- The app/import language is deliberately not used.

`PrayerWidgetHelper.WidgetTimelineState`:

- Resolves `numericLocale` in the constructor.
- Builds one clock formatter per dispatch with
  `NativePresentation.clockFormatter(context, numericLocale)`.
- Formats the next clock, current-start clock, all six daily clocks, and the
  Hijri day/year with that locale.
- `WidgetLayout` uses `state.numericLocale` for `TextPaint.setTextLocale()`,
  countdown measurement, and clock widths.

`NativePresentation`:

- `clockFormatter(context)` uses the app/import language locale.
- `clockFormatter(context, numericLocale)` is the widget-specific path.
- `remaining(minutes)` uses the app locale and is a notification consumer.
- The notification path calls `clockFormatter(context)` again, preserving its
  original app-numbering policy.

This separation is correct and must stay.

### 1.2 Settings and immediate native refresh

- `UserSettings` lives in `src/types/index.ts`.
- `AppSettings` and normalization live in `src/utils/settings-storage.ts`.
- `DEFAULT_SETTINGS` lives in `src/constants/default-settings.ts`.
- `buildWidgetPresentation(settings, ...)` in
  `src/services/widget-sync-service.ts` serializes the presentation payload.
- `CapacitorApp.useWidgetSync()` builds the presentation with `useMemo` keyed on
  `settings`, then calls `syncPrayerTimesToWidget`.
- `createWidgetSync` sends `updatePresentation` only when the serialized
  presentation changes. It does not rebuild the prayer schedule for a
  presentation-only change.
- `PrayerWidgetPlugin.updatePresentation` validates and durably saves the
  presentation, then calls `PrayerUpdateCoordinator.dispatchNow(application,
  false)`.
- `dispatchNow` rebuilds the widget state from the saved presentation and
  updates every widget and the live notification.

Immediate refresh after a setting change is therefore already structurally
supported. The new setting must be part of the presentation payload and must be
committed through `updateSettings`, not held only in a page-local draft.

### 1.3 Prayer transition and the negative countdown

- `PrayerTimeline.at()` is correct at boundaries:
  - `now < asr` selects Asr/Maghrib.
  - `now >= asr` selects Maghrib/Isha.
  - The same pattern covers every prayer and overnight Isha → Fajr.
  - The timeline therefore cannot itself return a negative interval.
- `PrayerWidgetHelper.scheduleNextAlarm`:
  - Uses `setExactAndAllowWhileIdle` when
    `AlarmManager.canScheduleExactAlarms()` is true.
  - Otherwise uses `setAndAllowWhileIdle`, which is inexact.
  - Caches the last target in static `scheduledTransition` and only re-arms
    when `force` is true, the target changes, or exactness changes.
- `PrayerWidgetAlarmReceiver`:
  - Transition action: `transition = true`, `reconcile = false`.
  - Calls `dispatchNow(application, reconcile)`.
  - Therefore a transition broadcast does **not** force an alarm re-arm.
- `PrayerTimeline.cosmeticTrigger()`:
  - Returns the next cosmetic progress update.
  - Returns `0` when that update would be at or after the target.
  - `scheduleNextAlarm` cancels the cosmetic alarm when it receives `0`.
  - There is no post-target watchdog update.
- `WidgetHelper.setupChronometer`:
  - Sets the native countdown base.
  - Stops it only when a new render sees `diff <= 0` and writes `—`.
  - A running native `Chronometer` has no public stop-at-zero; it continues
    below zero until the host is given a new RemoteViews update.

The Android manifest declares `SCHEDULE_EXACT_ALARM`, and the receiver handles
`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, but the app has **no**
`ACTION_REQUEST_SCHEDULE_EXACT_ALARM` flow and no UI that tells the user exact
alarms are denied. `MainActivity.onResume()` force-dispatches, which repairs a
stale widget when the app is opened, but not while the user stays on the home
screen.

### 1.4 What the screenshot shows

`Asr in −05:48` with a full amber bar means the last rendered state still had
`nextId = asr`. The device is past Asr, but no update reached the widget. The
native countdown kept ticking below zero.

This is an **alarm delivery / re-arm gap**, not a prayer-time calculation error.
The timeline boundary logic is already correct.

The most likely chain is:

1. Exact alarm permission is denied or unavailable; the app uses an inexact
   alarm.
2. MIUI/Doze delays or drops the inexact transition delivery.
3. No cosmetic alarm exists after the target, so nothing repairs the state.
4. The transition receiver does not force a re-arm if the alarm arrived early
   or if the same target is still cached.
5. The host `Chronometer` keeps subtracting seconds and displays negative digits.
6. If MIUI force-stopped the app, no broadcast can run until the app is opened.

Exact alarm permission and OEM battery policy are the primary limits. The code
can remove two avoidable gaps (no re-arm after early delivery, no post-target
fallback) and must make the remaining limit visible to the user.

---

## 2. Feature 1 — English widget numerals setting

### 2.1 Setting model

Add one required boolean to `UserSettings`:

```ts
widgetEnglishNumerals: boolean; // default false
```

- Default: `false` — follow the device numbering locale, preserving current
  behavior.
- Stored through the existing `muajjin-settings` normalization. Missing or
  invalid values normalize to `false`; valid siblings remain intact.
- It is a presentation preference, not a calculation input. It must not
  invalidate the prayer schedule or trigger month recalculation.

Do not add a second storage key. Do not store it only in native preferences:
the web settings page must own the durable user choice, and the native widget
must receive it through the existing presentation payload.

### 2.2 Where the toggle lives

Put it in `src/pages/settings/display-settings.tsx`, in a new section titled
“Widget numbers” (translation keys below). Display settings already owns visual
presentation; this avoids adding a new route.

Render the section only on native Android:

```ts
Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'android'
```

The web app has no Android widgets; showing an inert toggle there is confusing.

Use the existing `Switch` primitive and a controlled value from
`settings.widgetEnglishNumerals`. Do **not** use a page-local draft. On toggle:

```ts
const ok = updateSettings({ widgetEnglishNumerals: checked });
```

`updateSettings` persists durably before publishing. If it returns `false`, the
controlled switch remains on the committed value and the existing storage-error
toast is shown by `useLocalStorage`. This gives immediate refresh and honest
failure behavior.

If Option A is chosen, the description must say that the live seconds countdown
follows the launcher's number system. If Option B is chosen, describe the
minute-level countdown. Do not claim a guarantee the platform cannot provide.

### 2.3 Presentation payload

Extend `WidgetPresentation`:

```ts
widgetEnglishNumerals: boolean;
```

`buildWidgetPresentation` sets it from `settings.widgetEnglishNumerals`.
The serialized presentation key changes when the toggle changes, so
`createWidgetSync` sends exactly one `updatePresentation` and no
`updateSchedule`.

### 2.4 Native presentation

`NativePresentation`:

- Add `public final boolean widgetEnglishNumerals`.
- Constructor and `defaults()` set it.
- `parse()`:
  - Missing field => `false` for backward compatibility.
  - Present but not a boolean => reject.
- `json()` includes it.
- Keep schema `2`: this is an optional additive field. Old native code ignores
  it; new native code defaults absent values to `false`. Do not require a
  schema migration.

`NativeScheduleStore.savePresentation` already compares and commits the JSON;
no schema change is needed.

### 2.5 Choosing the numeric locale

Add one small policy helper, preferably beside `WidgetLocale`:

```java
static Locale resolve(Context context, boolean forceEnglish) {
    return forceEnglish ? Locale.US : resolve(context);
}
```

Use `Locale.US` (or another explicitly Latin-numbering locale) only for
**widget-owned numeric formatting**. Do not change `NativePresentation.locale()`
globally: it owns imported labels and the notification path.

In `WidgetTimelineState`:

```java
Locale numericLocale = presentation.widgetEnglishNumerals
    ? Locale.US
    : WidgetLocale.resolve(context);
```

Pass that locale to:

- `presentation.clockFormatter(context, numericLocale)`;
- `presentation.hijri(..., numericLocale)`;
- `WidgetLayout` (already uses `state.numericLocale`).

No other formatter path changes.

### 2.6 What changes and what must not

Changes when the toggle is on:

- widget next/current/daily clock digits;
- widget Hijri day/year digits;
- any app-generated widget numeric text.

Does not change:

- prayer names, Hijri month names, AM/PM words, or any imported label;
- app/import language or layout direction;
- 12/24-hour policy;
- user-provided location text, including digits inside a place name;
- notification digits and notification remaining-time labels;
- the app dashboard or other screens;
- prayer calculations, timestamps, timezone semantics, or schedules;
- widget provider identities.

Bidi: Latin digits in an RTL layout must retain the label/import direction.
Do not force the whole clock or widget to LTR. Test Arabic labels + Latin digits
and English labels + Arabic-Indic device digits.

### 2.7 Layout fitting

`WidgetLayout` already measures with `state.numericLocale`. Verify:

- countdown width measurement uses the same English/device digit shape;
- English digits do not make a clock clip;
- the six-column Wide and Full Table grids still fit;
- font scale 1.0–2.0 and 2×/3× density.

Do not add per-prayer text shrinking to make English fit.

### 2.8 Immediate refresh and lifecycle

When the toggle changes:

1. `updateSettings` durably writes `muajjin-settings`.
2. `settings` changes, so `useWidgetSync` rebuilds `presentation`.
3. `createWidgetSync` sees a new presentation key.
4. `updatePresentation` saves it natively and calls `dispatchNow`.
5. All widget instances re-render with the new locale.

No app restart, no schedule recalculation, and no network are required.

Edge cases:

- Bridge failure: the web setting remains durable; `bridgeFailed` shows the
  existing dashboard warning; retry on foreground.
- App closed before sync finishes: the setting persists in web storage and is
  sent on the next app launch. The native widget keeps its last acknowledged
  presentation until then.
- Device locale changes while the app is closed: `ACTION_LOCALE_CHANGED`
  triggers native reconciliation. With the toggle on, English remains; with it
  off, the fresh device locale is used.
- Native presentation missing/corrupt: `NativeScheduleStore.loadPresentation`
  falls back to defaults (toggle off) until the web sync repairs it.
- Old native / new web mismatch: unknown JSON fields are ignored by old native;
  missing field in new native defaults to off.

---

## 3. Feature 2 — transition reliability and no negative countdown

### 3.1 Goals

- At the prayer boundary, the widget changes to the next prayer.
- The previous prayer is not shown with a negative countdown.
- Do not add per-second work, a foreground service, or a periodic worker.
- Keep the existing exact-alarm path when permitted.
- Make denied exact alarms and OEM battery restrictions visible and actionable.

### 3.2 Make exact alarms a real, user-visible permission

The manifest already has:

```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

Do not add `USE_EXACT_ALARM`; it is restricted and unnecessary here.

Add native plugin methods to `PrayerWidgetPlugin`:

- `getWidgetReliability()`
- `openExactAlarmSettings()`
- `openBatteryOptimizationSettings()`

`getWidgetReliability()` should return a small validated object:

```ts
{
  exactAlarmSupported: boolean;   // SDK >= 31
  exactAlarmAllowed: boolean;     // SDK < 31 or canScheduleExactAlarms()
  batteryUnrestricted: boolean;   // isIgnoringBatteryOptimizations()
  widgetsPresent: boolean;        // any AppWidget IDs
  nextTransition: number | null;  // optional diagnostics
  lastDispatch: number | null;    // optional diagnostics
}
```

Native implementation:

- `AlarmManager.canScheduleExactAlarms()` on API 31+.
- `PowerManager.isIgnoringBatteryOptimizations(packageName)`.
- `PrayerWidgetHelper.widgetIds(...)` for widget presence.
- Optional: persist `next_transition_at` in `scheduleNextAlarm` and
  `last_dispatch_at` in `dispatchNow` for support/diagnostics.

Exact settings intent:

```java
new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
    Uri.parse("package:" + context.getPackageName()))
```

- API 31+ only.
- Catch `ActivityNotFoundException` and fall back to
  `ACTION_APPLICATION_DETAILS_SETTINGS`.
- Do not launch it automatically. It is a special access screen; show the
  rationale and a button.

Battery settings:

- Open `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` or the app details
  page.
- Do not call `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` automatically.
- Do not request a Play-policy-sensitive battery exemption automatically.
- Display a short MIUI/HyperOS note: allow autostart/no battery restrictions if
  the OEM provides those controls. There is no standard API to query autostart.

UI:

- Add a “Widget reliability” section to Display settings on native Android.
- Show exact-alarm state and battery state.
- Show buttons to open the relevant system settings.
- Refresh on page mount and on app resume/return from settings.
- If exact alarms are denied and widgets exist, show a dismissible dashboard
  banner with a “Fix” action. Do not nag permanently; the settings section is
  the durable place.

The app must remain functional when the user denies exact alarms: it falls back
to the current inexact path and labels the state approximate. It must not claim
that exact alarms are guaranteed on every OEM.

### 3.3 Re-arm the transition alarm

Change `PrayerWidgetAlarmReceiver` so a transition forces re-arming:

```java
boolean force = transition || reconcile;
PrayerUpdateCoordinator.dispatchNow(application, force);
```

Reason: if an inexact alarm is delivered early, `PrayerTimeline` still selects
the old interval and the static `scheduledTransition` cache would otherwise
prevent a new alarm at the real target. `force` re-arms the same target.

Keep:

- `reconcile = true` for boot/update/time/timezone/date/locale/unlock/exact-
  permission-change actions.
- `force = false` for a normal cosmetic progress refresh, unless it arrives at
  or after the target and the timeline advances naturally.

Do not remove the static cache without replacing it with an equally bounded
schedule policy; re-arming on every cosmetic tick would add unnecessary
AlarmManager writes.

### 3.4 Add a post-target fallback watchdog

The current cosmetic alarm stops at the target (`cosmeticTrigger() == 0`).
Add one bounded, non-wakeup fallback per prayer:

- Trigger: `targetTimestamp + 15_000` (tune after device measurement; 15–60s).
- API: `AlarmManager.set(AlarmManager.RTC, ...)` — non-wakeup.
- PendingIntent: separate request code/action, or the existing cosmetic action
  with a separate request code. A separate request code is clearer.
- Receiver handling: treat it as a cosmetic refresh (`dispatchNow(false)`).
  At/after the target, `PrayerTimeline.at` selects the next prayer.
- Cancel/replace it on every `scheduleNextAlarm` and in `cancelAlarms`.
- Schedule it only for a valid live state with a future target.
- If no widgets/notifications, cancel it with the other alarms.

Why non-wakeup: the screen is off while the user cannot see the widget. The
fallback does not need to wake the device; it runs when the device next wakes,
and `ACTION_USER_PRESENT`/`onResume` already reconcile on unlock/open. Exact
permission remains the primary on-time mechanism.

Do not add a minute-by-minute watchdog or a periodic worker.

### 3.5 Negative countdown semantics

Option A (live Chronometer):

- Exact transition + fallback + forced re-arm remove the normal failure.
- The Chronometer can still show a negative value between the target and the
  next delivered update if:
  - exact alarms are denied and the device is idle;
  - the app is force-stopped or MIUI blocks delivery;
  - the OEM delays the fallback as well.
- The widget should not claim otherwise in the UI or docs.
- `setupChronometer` already stops and writes `—` when a new render sees an
  expired target; do not attempt a fake zero-width fill or a custom view.
- Do not add a `TextView` overlay or per-second update just to hide the
  negative; that either loses accuracy or violates the battery goal.

Option B (static countdown):

- The countdown is rendered as app-formatted text, so it cannot go negative.
- At worst it shows the last minute-level value until the next cosmetic,
  fallback, or transition update.
- The transition name still depends on the exact/fallback delivery; the
  watchdog is still required.
- This is the only way to satisfy “always English” for the countdown.

### 3.6 What not to do

- Do not add a foreground service.
- Do not add one alarm per second or per minute.
- Do not add `setAlarmClock` (visible alarm icon, wrong UX).
- Do not add a WorkManager polling loop.
- Do not use `USE_EXACT_ALARM`.
- Do not request battery-optimization exemption automatically.
- Do not change prayer calculations or move the boundary by a fixed offset.
- Do not parse the displayed countdown or localized digits to decide state.
- Do not add per-OEM branches.

---

## 4. Battery and CPU analysis

| Change | Ongoing cost | Assessment |
|---|---|---|
| English numerals toggle | Formatting only; no new timer, alarm, service, or network | Negligible |
| Exact-alarm permission UI | One system-settings interaction; no runtime work | Negligible |
| Exact transition alarm | Same one-per-prayer boundary as the current intended path; exact instead of delayed inexact | Acceptable; preserves accuracy |
| Post-target fallback | One non-wakeup alarm per prayer (≈5/day); no wake lock; no work while asleep | Negligible |
| Transition force re-arm | One AlarmManager write per transition/reconcile | Negligible |
| Option B static countdown | Removes host per-second Chronometer rendering; text refreshes only on existing cosmetic/transition updates | Slightly lower host work; loses seconds |
| Option C per-second updates | 86,400 alarms/day or a persistent service | Rejected |

Do not claim measured battery-percentage savings. The claim is bounded work:
no per-second app work, no new wakeup alarm count beyond the transition, one
non-wakeup fallback per prayer, and no schedule recalculation for the toggle.

Android/OEM limits remain real:

- Force-stopped app: no alarms/broadcasts until reopened.
- Doze/App Standby: exact alarms are allowed when permission is granted; OEM
  battery managers can still restrict.
- MIUI/HyperOS: battery saver/autostart may block; user action needed.
- The fallback is intentionally non-wakeup, so it may wait until the device is
  awake; `USER_PRESENT`/`onResume` also reconcile.

---

## 5. File map

### Web

- `src/types/index.ts` — add `widgetEnglishNumerals`.
- `src/constants/default-settings.ts` — default `false`.
- `src/utils/settings-storage.ts` — normalize the boolean; preserve siblings.
- `src/services/widget-sync-service.ts` — presentation type/field; plugin
  reliability interface and validated calls; optional helper exports.
- `src/components/capacitor-app.tsx` — verify the memo/effect picks up the field;
  likely no code change beyond the presentation payload.
- `src/pages/settings/display-settings.tsx` — numerals toggle; widget
  reliability section on native Android.
- `src/pages/index.tsx` — optional dismissible exact-alarm/battery banner.
- `src/i18n/template-en.json` and `template-bn.json` — new settings/reliability
  strings.
- `src/components/settings/...` — reuse `SettingsSection`, `Switch`,
  `SettingsPageLayout`; no new generic component unless it genuinely reduces
  duplication.

### Native

- `NativePresentation.java` — field, constructor, defaults, parse, json.
- `WidgetLocale.java` — force-English locale policy.
- `PrayerWidgetHelper.java` — numeric locale in `WidgetTimelineState`;
  transition/fallback scheduling; optional diagnostic prefs.
- `PrayerWidgetAlarmReceiver.java` — transition force; fallback action handling.
- `PrayerWidgetPlugin.java` — reliability status and settings intents.
- `PrayerWidgetAlarmReceiver`/`AndroidManifest.xml` — a new fallback action may
  need a filter only if it uses a distinct action; a distinct request code with
  the existing action needs no manifest change.
- `WidgetLayout.java` — verify measurement only; no new logic expected.

### Tests

- `tests/widget-sync.test.ts` — presentation field, one presentation transfer,
  no schedule transfer; latest-intent behavior.
- `tests/storage-imports.test.ts` — default/preserve/reject invalid boolean.
- `android/app/src/test/.../PrayerTimelineTest.java` — explicit target,
  target−1 ms, target+1 ms, overnight, DST/zone boundary; no negative interval.
- `android/app/src/test/.../WidgetProgressTest.java` — unchanged unless the
  countdown policy adds a pure helper.
- `android/app/src/androidTest/.../WidgetNumeralInstrumentedTest.java` —
  forced-English fixture; device-mode fixture remains device digits; labels,
  RTL, location text, Hijri, all seven widgets.
- `android/app/src/androidTest/.../NativeStorageInstrumentedTest.java` —
  presentation JSON missing/true/invalid round-trip.
- `android/app/src/androidTest/.../PrayerLifecycleInstrumentedTest.java` —
  notification remains app-numbered while widget is forced-English; transition
  boundary; optional alarm status.
- Manual/device: exact permission grant/deny/revoke; battery settings; MIUI
  transition; force-stop limit; `dumpsys alarm` count.

### Docs

- `README.md` — widget numerals section; exact alarm/battery guidance; honest
  countdown limitation or Option B behavior.
- `docs/PRIVACY_POLICY.md` — no new data/network; exact alarm/battery are local
  OS controls, so likely no change.
- `tasks/evidence.md` — implementation and verification record after code.

---

## 6. Implementation order

1. **Decision gate:** choose Option A or B for the live countdown. Do not start
   native countdown code before this.
2. **Web setting/presentation:** add the boolean, normalization, payload, UI,
   translations, and immediate-sync tests.
3. **Native presentation/formatting:** add the field, locale policy, tests with
   Arabic/Bengali/English device fixtures.
4. **Exact alarm reliability:** add status/intent plugin methods, settings
   section, optional banner, and native permission-state refresh.
5. **Transition reliability:** force transition re-arm, add the post-target
   fallback, diagnostics; add boundary/policy tests.
6. **Option B only:** replace the Chronometer with static English countdown;
   update layout measurement and tests; verify no negative.
7. **Docs and full gates:** web lint/typecheck/tests/both builds; native unit,
   debug, instrumentation-APK; authorized device checks; evidence record.
8. **Release:** version bump/signing/publication only after separate approval.

---

## 7. Acceptance criteria

### Numerals

- Toggle off: widget clocks/Hijri follow the actual device numbering locale;
  labels follow app/import language.
- Toggle on: every app-formatted widget number uses Latin digits on Arabic,
  Bengali, Persian, and Latin-numbering device fixtures.
- Toggle on/off updates the widget immediately while the app is open, without
  restarting or recalculating the prayer schedule.
- The setting survives app restart and works with the WebView closed.
- Labels, AM/PM words, Hijri month names, layout direction, 12/24-hour policy,
  location text, and notification digits are unchanged.
- Old/new presentation payloads interoperate: missing field = off; invalid
  field is rejected by the new native parser.
- All seven widgets, both themes, RTL, 12/24h, and font scales 1.0–2.0 are
  covered.
- Option A: the setting description and README state the live-countdown limit.
- Option B: all widget numbers, including the countdown, are English; accuracy
  is minute-level and documented.

### Transition reliability

- A transition delivered exactly at the target changes the next prayer.
- A transition delivered early re-arms the same target.
- A missed/late transition is repaired by the post-target non-wakeup fallback
  when the device is awake.
- `USER_PRESENT`, `onResume`, boot, app update, time/timezone change, locale
  change, and exact-permission change reconcile.
- Exact-alarm status is visible; granting/revoking it re-arms the alarm.
- Battery-optimization status is visible; no automatic exemption request.
- No per-second alarms, foreground service, or periodic worker is added.
- Force-stop/OEM limits are documented, not hidden.

---

## 8. Risks and rollback

| Risk | Mitigation |
|---|---|
| `Chronometer` cannot be forced to English | Decision A/B; document; do not fake a fix |
| OEM ignores exact permission or delays alarms | Settings status, fallback, USER_PRESENT/onResume, docs |
| Force-stop shows stale/negative widget | Document as platform limit; static countdown only if B |
| Fallback adds alarm noise | One non-wakeup per prayer; cancel/replace on every dispatch |
| Early transition re-arm loops | Re-arm at the same target only; test early delivery |
| English digits change fit | Measure with `numericLocale`; all seven widgets/densities |
| Mixed digits look wrong in RTL | Keep label direction; target only owned numbers; test bidi |
| Presentation field breaks old native | Optional field, default false, schema 2 unchanged |
| Toggle sync fails | Durable web setting, existing bridge error banner, retry |
| Battery exemption/Play policy | User-initiated settings link only; no auto request |

Rollback is additive:

- Remove the toggle/presentation field; old widgets revert to device digits.
- Remove the exact-alarm UI and fallback; the app returns to the current
  inexact path.
- No schedule/storage migration is introduced; saved presentation JSON with the
  extra field remains readable by older code.

---

## 9. Open decisions for the owner

1. **Countdown:** Option A (live seconds, host digits) or Option B (strict
   English, minute-level static)?
2. Should the widget-reliability warning be only in Display settings, or also a
   dismissible dashboard banner?
3. Fallback delay: 15 seconds (faster repair, still non-wakeup) or 60 seconds
   (fewer updates)?
4. Should the reliability section show next-transition/last-dispatch
   diagnostics to users, or only permission/battery state?
5. Should the toggle be visible on web? Recommendation: Android only.
6. Should notifications also gain an English-digits option? Recommendation: no,
   unless separately requested; the current notification contract is app locale.

Until decision 1 is made, the implementation AI should not change the
countdown architecture. The transition reliability work and the static-number
toggle can proceed independently.
