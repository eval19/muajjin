# Muajjin widget visual overhaul — implementation prompt

Status: implemented in the 2.9.0 release; kept as the historical design contract. The 2.9.1 release fixed the widget footprints described below.

Prepared from the current repository, both user-supplied Xiaomi screenshots, the three README widget screenshots, and official Android widget documentation on 2026-10-23.

## 0. Instructions to the implementation AI

You are implementing a presentation-only refinement of Muajjin's seven native Android widgets. Treat this document as the visual contract, not an invitation to invent a new look.

The owner wants the existing product to look finished: coherent corners, balanced whitespace, deliberate typography, aligned information, readable times, and predictable resizing. Keep Muajjin recognizable. Do not redesign the app, change prayer calculations, add features, or replace the widget architecture.

**Approval boundary:** the authoring session authorized only this Markdown file. It did not authorize application edits, builds, installations, device access, screenshots, or new dependencies. Read the repository's current instructions and explain your exact proposed changes, disk impact, risks, rollback, and verification before requesting implementation approval. This prompt is not permission to bypass those rules. Obtain separate authorization for device installation/binding/permissions and any change outside the approved scope.

Read the whole document before implementation. Follow its ordered tasks. If a real measured constraint conflicts with a design target, preserve information and technical behavior, document the conflict, and resolve it explicitly; do not quietly substitute a different design or mark a partially verified result complete.

### Priority order

1. Correct prayer information and unchanged native behavior.
2. Complete, legible clocks and countdowns; accessible localized text.
3. Shared layout geometry and state treatment.
4. The specified visual composition.
5. Decorative details.

This ordering does not authorize silently abandoning a lower-priority requirement. It defines what must not be damaged while resolving a conflict.

## 1. Product objective and non-goals

### The intended visual character

Quiet, precise, restrained, green-neutral, and easy to scan. A countdown-led instrument, not a decorative dashboard. Rounded cards remain, but their contents should look intentionally composed rather than pinned to opposite ends of a stretching rectangle.

Preserve:

- Existing green/teal semantic palette, light and dark resource modes, warm-state amber, and rounded-card identity.
- Large live countdowns and secondary current/next-prayer context.
- All six daily entries, including sunrise/Shuruq, in the schedule-bearing variants.
- Location and Hijri date in the Full Table.
- Filled current and outlined next treatments in the Wide Card, preserving their existing meaning.
- The transparent, white-on-wallpaper identity of the two keyguard-oriented variants.
- All seven provider identities, whole-widget tap-to-open behavior, supported localization, and selected 12/24-hour format.

Do not introduce gradients, illustrations, icons, logos, decorative dividers, extra badges, glass effects, new settings, wallpaper-derived dynamic colors, new fonts, new libraries, animations, or unrelated app-screen changes. Do not add controls to passive prayer cells.

### Cross-device promise

The target is **the same design system and content hierarchy at equivalent usable bounds**, with intentional responsive compositions when bounds differ. It is not identical screenshot pixels on every Android device.

Launcher cell sizes, host insets, outer gaps, system corner enforcement, display density, font scale, font substitution, and lockscreen support are not fully controlled by this app. Never implement Xiaomi/Samsung/Pixel model checks, device-specific offsets, fixed screenshot-pixel coordinates, font-scale suppression, or a flattened bitmap widget to imitate one capture.

## 2. Evidence and diagnosis

These are source observations and screenshot observations, not a runtime reproduction of the current checkout. No app build, test, emulator launch, or device action was performed to author this plan.

### 2.1 Reference material

User-provided files, inspected in place and not copied into the repository:

- `C:/Users/Mohamed Tawfig/Downloads/Screenshot_2026-10-23-14-29-45-627_com.miui.home.jpg`
- `C:/Users/Mohamed Tawfig/Downloads/Screenshot_2026-10-23-14-29-46-953_com.miui.home.jpg`

They show Banner, Full Table, Wide Card, Square, and Strip. The two keyguard variants are not shown. The files decode as 1280 × 2772 images; the chat displays scaled previews. Neither physical dp dimensions nor font scale can be recovered reliably from those image pixels alone.

Repository references:

- `docs/images/widgets-home-screen.png`
- `docs/images/widget-wide.png`
- `docs/images/widget-full-table.png`
- `README.md:24–68`, especially its capture attribution at lines 49–51.

README attributes its images to the 2.8.4 APK on an Android 16/API 36 emulator using Makkah, explicitly noting cropping and launcher-dependent sizing. These images also show large empty interior regions. They are historical examples, not the target design or proof of source/artifact parity. Unlike the user's capture, the table reference has no giant active-current rectangle and the wide reference has no wrapped AM/PM suffix. Different active state and available space are plausible contributors; the exact installed Xiaomi build and host configuration were not established.

### 2.2 Defects tied to implementation

Paths in this table are under `android/app/src/main/` unless otherwise stated.

| Observed problem | Source evidence | Required correction |
|---|---|---|
| Square's countdown and footer feel disconnected | `res/layout/widget_prayer_square.xml:83–87` is one unbounded weighted spacer between them | Compose a compact information group; put surplus height outside the group rather than inside its relationships |
| Similar unused bands in Strip/Banner/Wide | Weighted spacers in `widget_prayer_strip.xml:54–58`, `widget_prayer_banner.xml:104–108`, and `widget_prayer_wide.xml:129–133` | Remove unbounded internal expansion; use explicit gaps and deliberate outer surplus allocation |
| Full Table text crowds its card edge | `widget_prayer_large.xml:10` uses 6dp root padding, versus 16–18dp horizontal padding in the other home widgets | Use the shared regular/compact inset system |
| Full Table's active Dhuhr becomes a giant panel | Table container and its two rows consume remaining height with weights; `row_*` cells use `match_parent` height (`widget_prayer_large.xml:120–340`); renderer paints the cell itself | Keep active decoration on equal, content-sized cells, not a stretched row slot |
| Wide Card AM/PM creates unequal tile heights | All six cells are `wrap_content`; all six time views allow two lines independently; see `widget_prayer_wide.xml:135–386` | Prefer full single-line time strings in a fitting composition; constrain all cells in the selected composition to equal geometry |
| Dhuhr remains heavier even when not current | Dhuhr's name/time are permanently bold in both Wide and Full Table XML; `renderSingleChip` changes colors/backgrounds, not font weight; table renderer changes backgrounds only | Give all prayer IDs identical base typography; any state-specific emphasis must be set/reset from actual existing state |
| Full Table looks like another generation of design | Cyan hard-coded current-status text/badge, emerald countdown badge, and gradient progress at `widget_prayer_large.xml:59–118` | Migrate this live surface to the same semantic palette and progress treatment as the other cards |
| Resize behavior cannot account for width | `PrayerWidgetHelper.java:105–110` reads only `OPTION_APPWIDGET_MIN_HEIGHT`; Square/Wide fall back below 100dp | Add presentation-only width-and-height-aware composition without changing schedule/update ownership |
| Dark screenshots hide light-mode inconsistencies | Full Table uses fixed-color badge/progress resources alongside day/night card colors | Verify actual light-mode pairs, not only dark screenshots |

Do not blame all defects on the launcher. The stretching and inconsistent styling are explicitly present in the app's layout definitions. Conversely, do not claim this source audit proves the exact binary installed on the user's phone.

### 2.3 Important existing limits

- The current native helper comment calls Full Table “4x4,” but its metadata and README target **4×3**. Metadata is authoritative; a stale comment is not a design requirement.
- Current source declares version 2.8.4/284. Read-only inspection found older and mutually inconsistent generated APK/output metadata under `android/app/build`. Never treat an existing APK in that directory as a fresh rendering of this source.
- The current tests check some text visibility and countdown behavior, but do not detect balanced whitespace, equal tile heights, universal light-mode consistency, or a lone wrapped AM/PM suffix.

## 3. Actual widget inventory and protected implementation boundary

### 3.1 Seven stable variants

The size columns below describe the authoring snapshot. In 2.9.1 the resize minimums were removed and the legacy minimums were replaced with canonical fixed cell hints; see the top status line.

Java classes live under `android/app/src/main/java/org/muslimtechnician/muajjin/`. Layout and metadata filenames below are relative to `android/app/src/main/res/layout/` and `res/xml/` respectively. Dimensions are declared dp hints, not guaranteed measured content bounds.

| Product name | Provider class | Active layout | Metadata file | Declared min W×H | Resize min W×H | Target cells / category |
|---|---|---|---|---|---|---|
| Strip | `PrayerWidgetStripProvider` | `widget_prayer_strip.xml` | `widget_prayer_strip_info.xml` | 130×50 | 110×50 | 2×1 / home_screen |
| Square | `PrayerWidgetSmallProvider` | `widget_prayer_square.xml` | `widget_prayer_small_info.xml` | 130×110 | 110×50 | 2×2 / home_screen |
| Banner | `PrayerWidgetBannerProvider` | `widget_prayer_banner.xml` | `widget_prayer_banner_info.xml` | 250×50 | 220×50 | 4×1 / home_screen |
| Wide Card | `PrayerWidgetMediumProvider` | `widget_prayer_wide.xml` | `widget_prayer_medium_info.xml` | 250×110 | 220×50 | 4×2 / home_screen |
| Full Table | `PrayerWidgetLargeProvider` | `widget_prayer_large.xml` | `widget_prayer_large_info.xml` | 250×220 | Not explicit | 4×3 / home_screen |
| Lock Small | `PrayerLockSmallProvider` | `widget_lock_small.xml` | `widget_lock_small_info.xml` | 170×60 | Not explicit | 2×1 / keyguard |
| Lock Medium | `PrayerLockMediumProvider` | `widget_lock_medium.xml` | `widget_lock_medium_info.xml` | 280×60 | Not explicit | 4×1 / keyguard |

All seven use fixed footprints (`resizeMode="none"`) and `updatePeriodMillis="0"` as of 2.9.1. None declares `previewLayout` or `previewImage`. The two Lock providers are **keyguard-only**, not home-screen-and-keyguard. Do not change categories to make testing easier.

The old `widget_prayer_small.xml` and `widget_prayer_medium.xml` layouts were unused by the active Small/Medium renderer paths and have been removed.

### 3.2 What may change after implementation approval

- Active native widget XML hierarchy, padding, alignment, typography, and shape resources.
- Shared native widget dimensions/text styles, preferably in existing resource conventions; create small `values` resource files only where needed.
- Minimal `PrayerWidgetHelper` presentation code for layout selection, styling, sizing, and applying existing formatted data to alternative views.
- Existing debug/instrumentation visual fixtures and focused regressions for consumer-visible layout/state failures.
- Widget picker preview resources and metadata references when included in the approved file scope.
- README's widget descriptions/screenshots after real verification, without implying universal device support.

Changing minimum/default size hints or broadening information-reducing compact fallbacks is a user-visible compatibility decision. It needs the explicit approval described in section 6.5, not an unexplained metadata tweak.

### 3.3 What must not change

Preserve `PrayerUpdateCoordinator`, its serialized update flow, `PrayerSchedule`, `PrayerTimeline`, storage schemas, renewal, Hijri calculations, Adhan configuration, native handoff revisions, notification behavior, alarm cadence/permission fallback, and all provider component identities.

`PrayerWidgetHelper` currently combines rendering and alarms. Editing this file does not authorize touching its `scheduleNextAlarm`, `cancelAlarms`, transition/cosmetic scheduling, or `WidgetTimelineState` time-selection/calculation semantics.

Retain:

- One shared `WidgetTimelineState` per dispatch.
- Existing native `Chronometer`, count-down direction, base/deadline, seconds, and stopped/invalid behavior. Do not replace it with strings updated every second.
- Existing progress meaning and 0–1000 runtime values. A visual bar change is not permission to reverse elapsed/remaining semantics.
- Existing current/next/passed predicates, including date-sensitive event equality and overnight Isha handling. Do not “fix” religious-time logic during a styling task.
- `NativePresentation.clockFormatter()` and its selected system/12h/24h behavior, localized digits and AM/PM labels, and missing-clock representation.
- Imported labels and RTL direction. Do not rename Shuruq/Sunrise or reconstruct labels from English in XML.
- All six schedule entries and the Full Table metadata.
- Whole-root click behavior and immutable existing `PendingIntent` flags.
- Existing below-100dp Square→Strip and Wide→Banner compact behavior unless a separately approved sizing decision explicitly revises it.

Use framework views supported by `RemoteViews`. No Compose/Glance migration, WebView, HTML/CSS rendering, custom unsupported View subclasses, ConstraintLayout assumption, dependency installation, or bitmap-rendered live text. API-31 features require an API-24-compatible path.

## 4. Shared visual system

The values below are the **proposed baseline specification**, not measurements of an implemented result. Implement them consistently first, then validate native fit. If glyph metrics require an adjustment, change the relevant shared role consistently and record the reason; do not invent per-prayer or per-device exceptions.

### 4.1 Color: keep the existing semantic identity

Use `res/values/colors.xml` and `res/values-night/colors.xml` as the source of truth. Do not introduce a second palette.

| Token | Light | Dark | Role |
|---|---|---|---|
| `w_surface` | `#FBFCFB` | `#1B211E` | Home-widget surface |
| `w_border` | `#E3E9E5` | `#2A332F` | Quiet card outline |
| `w_text_primary` | `#14201B` | `#EEF3F0` | Countdown, primary clock content |
| `w_text_secondary` | `#4B5A53` | `#B4C0BA` | Context and ordinary labels |
| `w_text_muted` | `#66746D` | `#8A9791` | Existing passed/low-emphasis state |
| `w_accent` | `#0B6E5B` | `#5FD3B5` | Active accent and normal progress |
| `w_accent_tint` | `#DCEFE9` | `#1F3A33` | Current-prayer fill |
| `w_track` | `#D5DDD9` | `#34403A` | Progress track |
| `w_warm_text` | `#9A4A00` | `#FFB74D` | Existing warm-state label |
| `w_warm_fill` | `#D97706` | `#F59E0B` | Existing warm-state progress |

Remove live Full Table dependence on cyan status styling and its gradient progress. Use neutral secondary text for current context, primary text for the countdown, and the shared normal progress drawable. Do not add a warm-state behavior to variants that do not currently have one without identifying that semantic change separately. Existing warm transitions must retain the same geometry as normal state.

Use flat surfaces. Keep the existing subtle 1dp border, no additional elevation/shadow on the five home cards. Transparent keyguard variants retain a modest existing text shadow; do not put them on an opaque green card.

Target measured contrast of at least 4.5:1 for normal text and 3:1 for large text. These ratios have not been measured in this planning session. Check actual text/fill pairs in both themes; do not swap `w_warm_fill` into small text. Transparent keyguard text cannot be guaranteed to meet contrast on every arbitrary wallpaper; disclose this, test representative backgrounds, and do not silently add a new background feature.

### 4.2 Geometry and spacing

| Role | Baseline rule |
|---|---|
| App-defined card radius | 20dp, preserving the existing card identity; shared across five home variants |
| Card border | 1dp `w_border` |
| Interior cell radius | 8dp, shared for current/next/neutral cell geometry |
| Progress | 4dp high, 2dp end radius, full content width, same start/end edge as content |
| Regular card inset | 16dp on all sides |
| Compact card inset | 12dp horizontal; 8dp vertical when measured height requires it |
| Label→countdown | 4dp |
| Primary block→progress | 12dp regular / 8dp compact |
| Progress→schedule | 16dp regular / 8dp compact |
| Progress→simple footer | 8dp |
| Schedule column gap | 6dp; compact 4dp |
| Schedule row gap | 8dp; compact 6dp |
| Cell text padding | 8dp horizontal and vertical regular; compact 4dp horizontal, 6dp vertical |
| Cell label→clock | 4dp regular / 2dp compact |
| Independent text columns | At least 8dp separation regular / 4dp compact |

Use semantic dimensions/styles rather than retyping values in seven files. Avoid a theme framework or configuration object with dozens of speculative options.

Android 12+ may enforce a system outer radius. Make the background/container compatible with system clipping; prefer an API-qualified system radius where necessary, with the existing 20dp legacy fallback. Do not paint a contradictory nested outline or disable host behavior merely to force the exact 20dp on every launcher. Record host-enforced radius as a legitimate platform difference. Content insets must keep text clear of the effective curved edge.

**Spacing invariant:** surplus height must not all accumulate in one internal spacer. Build content-sized groups with explicit internal gaps. Center the complete group vertically when the host gives it extra height. For the Full Table, a separately centered schedule region is permitted, but its six decorated cells remain bounded, equal, and separated by a bounded row gap.

Do not solve empty space by stretching the active fill, enlarging every gap, or making the countdown grow indefinitely. On a deliberately oversized host rectangle, some outer whitespace is correct; an arbitrary interior gulf is not.

### 4.3 Typography

Keep native system sans-serif. No bundled font, font download, or web font bridge. Use two principal weights: normal and bold. Tabular numerals (`tnum`) remain on clocks/countdowns where supported; this is not a guarantee that all OEM font metrics match.

| Text role | Regular target | Compact target | Treatment |
|---|---|---|---|
| Countdown in Strip/Banner | 26sp | 22sp | Bold, primary, single line |
| Countdown in Square/Wide | 32sp | 26sp | Bold, primary, single line |
| Countdown in Full Table | 28sp | 24sp | Bold, primary, single line; no decorative badge |
| Next-prayer label | 13sp | 12sp | Bold accent; existing warm token when applicable |
| Current/next context | 12sp | 11sp | Secondary; next-clock emphasis only as specified per variant |
| Schedule prayer name | 12sp | 11sp | Same base weight for all six IDs |
| Schedule time | 14sp | 12sp | Bold or medium-equivalent shared treatment, same for all six |
| Location/Hijri | 12sp | 11sp | Secondary; location may be bold |
| Lock Small countdown | 26sp | 22sp | White, bold, existing restrained shadow |
| Lock Medium countdown | 28sp | 24sp | White, bold, existing restrained shadow |

These are sp, not dp or raw pixels. Honor system font scaling. Do not autosize each time or prayer name independently: that produces another inconsistent set of six unrelated text sizes. Choose a role scale for the whole composition. Do not shrink schedule times below the compact 12sp target simply to retain six columns.

Use `includeFontPadding` consistently within a text role, but do not universally force it off without testing Bengali and Arabic ascenders/descenders and diacritics. Set adequate measured line boxes; glyph clipping is a failure even if a bounding rectangle fits. Avoid fixed text-view heights derived from Latin screenshots.

Keep complete critical clocks and countdowns. `11:37 AM`, localized equivalents, and hour-bearing countdowns such as `1:09:26` must not become `11:37`, `11:…`, clipped digits, or only a day-period fragment. Metadata and unusually long labels may use deliberate ellipsis as a last resort after the specified reflow; preserve their complete accessibility text.

## 5. Per-widget composition contract

“Start/end” means logical layout direction, not hard-coded left/right. Mirror spatial alignment for RTL without corrupting the logical order of a localized time string. Use the existing stable prayer ordering; never sort by translated labels.

### 5.1 Strip — one compact countdown instrument

Content order: next-prayer label, countdown, progress.

- Shared surface/radius/border. Regular content inset 12dp horizontal and 8dp vertical for this intentionally compact variant.
- Start-aligned label; countdown directly beneath with the shared 4dp gap.
- Progress below the primary group with 8dp gap. Do not pin it to the bottom with an unbounded spacer.
- Vertically center the complete group inside extra host height. Its content remains coherent when a launcher makes a 2×1 widget unusually tall.
- No new current-prayer footer, metadata, or icon.
- Small-height fit must be verified against the existing 50dp hint; do not claim this baseline fits that hint at every font scale. Apply section 6.5 if it cannot.

### 5.2 Square — balanced, not hollow

Content order: label plus next clock, countdown, progress, current-prayer footer plus current start clock.

- Keep the next prayer clock in the top row's end column when it fits; the label uses the remaining width with a protected gap.
- Countdown sits 4dp under that row, not centered in a separate giant area.
- Progress is 12dp below the countdown block; footer is 8dp below progress.
- Footer contains the same `footerText` and `currentStartTime` data as today.
- Treat the entire structure as one content-sized vertical group and center it in spare height. This is the direct correction for the second user screenshot.
- When top/footer columns do not fit, stack the existing fields in logical reading order with 4dp gaps. Protect the clock rather than truncating it to preserve a side-by-side arrangement.
- Do not remove the current start clock, make a fake circular gauge, or add the daily schedule.
- Preserve the existing short-height Strip fallback unless explicitly revised with approval.

### 5.3 Banner — wider Strip with quiet context

Content: primary label/countdown at start; current and next context at end; shared progress below.

- Primary and context groups are vertically centered relative to each other; their text is respectively start/end aligned.
- Do not assume an unconditional 50/50 split. Reserve measured width for the complete countdown, then allocate remaining width to context with the shared gap.
- Context line 1 is `footerText`; line 2 is `contextLine2`. Both use the shared context role; avoid an arbitrary size/weight mismatch with the Wide Card.
- Place progress 8dp below this row. Center the combined row+progress structure within extra height.
- Reflow context beneath the primary block only if the host has sufficient height and horizontal fit otherwise fails. Do not silently remove context to simulate the Strip.

### 5.4 Wide Card — the same primary block plus an orderly schedule

Content: Banner-style primary/context block, progress, six daily cells.

- Use Square/Wide countdown sizing. Preserve all existing primary/context strings.
- The schedule's six entries have identical base type roles, identical padding, equal widths within a row, and one shared cell height determined by the largest required content height in the chosen composition.
- Current entry: `w_accent_tint` fill and accent text. Next entry: 1dp accent outline, transparent fill. Ordinary/passed entries: transparent background and existing secondary/muted semantics. Geometry must be identical in all states.
- Remove permanent Dhuhr emphasis from XML. A static example must not make one prayer permanently look active.
- Keep background IDs mapped to the actual cell, not an unbounded flex slot.

**Responsive arrangement, in this exact order:**

1. Use six columns × one row only when every complete displayed clock and ordinary supported prayer label fits at the chosen shared role size with cell padding and inter-cell gaps.
2. Otherwise use three columns × two rows, in the order Fajr, Sunrise/Shuruq, Dhuhr / Asr, Maghrib, Isha. This is the preferred narrow-phone solution, not smaller unreadable text. Preserve all six entries, the same state predicates, and the same information.
3. For an unusually shallow host where the two-row composition cannot fit, a six-column compact composition may use **uniform two-line clock slots** only if native measurements prove every complete clock fits. All six slots reserve the same line count/height and align their first clock baselines. Do not allow one current chip to grow alone. Do not parse English `AM`/`PM`, fabricate abbreviations, or discard localized day-period text. If uniform wrapping still cannot fit, apply section 6.5 instead of forcing it.

Single-line clocks are the normal result. The controlled shallow fallback is an explicit exception, not permission to leave the current accidental wrapping unchanged.

Keep the schedule 16dp below progress in regular mode, 8dp in compact mode. No huge expanding band between them. Center the complete composition vertically when extra height remains. Use a three-column composition only where the height budget actually supports it; do not label an overflowing grid “responsive.”

### 5.5 Full Table — composed daily overview

Content order: metadata, current/next summary with countdown, progress, three-column/two-row daily grid.

- Keep location and Hijri date. Use one metadata row when both fit with a protected gap. Otherwise use two start-aligned lines; do not sacrifice the Hijri date merely to keep a horizontal row.
- After 8dp, place a start block with ordinary current context above the next-prayer summary. Put the live countdown in an end block aligned to the visual center of that summary group.
- Remove the full-width cyan outlined current pill and the separate emerald countdown badge. They add competing shapes and make the table look unrelated to the other widgets. Current context is plain secondary text; summary is primary; countdown is primary bold.
- Keep existing summary content (`nextName · nextTime`) and current context. Do not change their meaning or add another duplicate next-prayer label.
- Use the shared 4dp progress bar after the shared primary-to-progress gap.
- Preserve the three-column/two-row schedule. Each cell contains its name above its time, start-aligned, with equal padding and line-box geometry.
- Current cell uses the shared 8dp-radius accent-tint treatment with the existing active-row outline if retained. Do not add a next-cell state to this variant merely because Wide has one.
- No prayer name is permanently bold because of its ID. Dynamic emphasis must follow existing state and reset on subsequent updates.
- Regular cell height target: 64dp minimum at font scale 1.0; compact target: 48dp minimum. These are minimums, not glyph-clipping fixed heights. If localized/scaled content needs more height, grow **all six cells equally** and re-evaluate the whole composition.
- The active background covers only that bounded cell. It must not extend across half of the remaining widget height.
- Regular grid row gap is 8dp; compact is 6dp. Do not spread rows to opposite ends of the card.
- Use compact roles/insets at constrained widths/heights. A 250×220dp native host is an existing important verification case and must be tested, not assumed.

### 5.6 Lock Small — preserve the minimal transparent identity

- Transparent root, centered content group: label, countdown, next clock.
- Keep existing white/near-white colors and restrained shadow behavior; move repeated constants into shared keyguard text styles if useful.
- Use consistent 4dp vertical relationships and measured safe outer padding rather than changing each line independently.
- Complete countdown and next clock; labels may reflow when there is room.
- Do not add a progress bar, opaque card, colored state chips, or a home-screen category.

### 5.7 Lock Medium — transparent Banner relative

- Transparent root. Start primary label/countdown group; end two-line current/next context group.
- Use the same column-fit and baseline principles as Banner, with the existing white/near-white typography and shadows.
- Center the whole content row vertically. Keep safe insets, protected text-column gap, and complete clocks.
- No added progress bar, opaque card, or category changes.
- Verify in the debug host, but claim actual lockscreen support only after testing a real supporting host.

## 6. Responsive implementation and feasibility rules

### 6.1 Fit is based on usable bounds, not cell labels

Let `W` and `H` mean available widget content bounds in dp after the host's own allocation/insets have been understood. Subtract app-owned padding once; do not double-subtract launcher padding. Record actual measured native view bounds during verification to catch option/bounds mismatches.

For a six-column row, require:

`contentWidth >= 6 * requiredCellWidth + 5 * columnGap`

`requiredCellWidth` is the maximum measured label/clock width for the selected shared typography plus both cell insets. For a three-column grid, use the analogous three-cell/two-gap condition. Height must include metadata/hero line boxes, every explicit gap, progress, both grid rows if present, and top/bottom padding.

These fit checks must account for selected locale/time format, system font scale, and long hour-bearing countdowns. Do not choose a layout from width alone and then hope it fits vertically. Do not assume the screenshot's `23:14` is the widest countdown the Chronometer can display before the next scheduled update.

Use a small set of semantic compositions, not a unique layout for every pixel size. Reuse role sizes and predicates. No brand-specific branches or permanent per-instance presentation preferences.

### 6.2 Android 12+ and older hosts

Official Android guidance recommends a small set of responsive `RemoteViews` layouts on API 31+, with exact-size alternatives when a finite set is insufficient. Use that facility for genuine composition changes if it reduces guesswork. Keep each size entry fully bound to the same immutable snapshot, including correct Chronometer base, progress, locale, click intent, and visibility/state resets.

On older Android, or when a launcher supplies incomplete size information, retain a conservative layout path using the available option ranges and existing resize callback flow. `OPTION_APPWIDGET_SIZES` may be absent or empty; do not crash, show nothing, or silently fall back to fake dimensions. Treat missing/zero information as unknown, not proof the host is 0dp tall.

Avoid using `layout-swNNNdp` as a substitute for widget-instance size: device screen width is not an individual widget's usable width. Prefer explicit widget composition selection and a small bounded resource set. Do not build a parallel update service.

### 6.3 State must not change geometry

Normal→warm, one current prayer→another, no-current→current, 12h→24h, and light→dark must not produce inconsistent padding or one-off tile heights. Apply/reset every presentation property that can survive an update: visibility, background, text color, emphasis, text size, and line policy where changed.

Do not leave Dhuhr bold after Asr becomes current. Do not carry a prior active background into the recovery state. Distinguish a background/layout reset defect from the existing domain predicate; this task may fix the former, not redesign the latter.

### 6.4 RTL, recovery, and text scaling

- Use start/end layout attributes and preserve root direction from `NativePresentation.direction`.
- Keep Arabic/Bengali glyphs intact. Test imported RTL labels and translated day periods; avoid ASCII-based splitting of clocks.
- Do not blanket-force all mixed-script strings LTR. Use native bidi behavior and targeted isolation only when a demonstrated clock-order defect requires it.
- Existing recovery content must remain understandable and tappable. Give the recovery message the available primary region rather than clipping it to a tiny normal-state label slot. Preserve the existing stopped countdown/missing-clock semantics, with no fake prayer data.
- Test 1.0, 1.3, 1.5, and 2.0 font scales. Scaling may require a different composition and a larger user-selected widget; it must not be disabled or counteracted with fixed-pixel text.

### 6.5 Explicit size-compatibility decision gate

**Known tension:** current metadata advertises heights as low as 50dp, while the existing full Wide path can be selected at 100–110dp. A legible hero, context, progress, and six complete clocks may not all fit those bounds, particularly with scaled or long localized text. This plan does not falsely declare them feasible.

At the first native fit checkpoint:

1. Measure the old smallest/transition sizes and the proposed compositions using real native text metrics.
2. First reduce only to the specified compact roles/gaps and use the permitted reflow. Do not lower font floors, remove AM/PM, crop clocks, or omit schedule entries.
3. Preserve the existing below-100dp information-reducing fallback while assessing compatibility. Do not silently extend it to a size that previously showed the daily schedule.
4. If a full composition cannot fit, report exact host W×H, font scale, failing content, measured required height/width, and the existing metadata/fallback threshold.
5. **Recommended resolution to request:** raise the affected minimum/default size hints to the measured readable floor, and, only if necessary, approve a clearly documented compact presentation for already-installed undersized instances. Keep provider identities and calculations unchanged. Explain that larger minimums can change launcher cell occupancy and resize freedom.
6. Do not claim that metadata alone fixes existing widgets or every launcher; some hosts may ignore hints. Verify already-installed instances as well as newly placed ones.

The implementation AI must obtain this compatibility decision if needed. It is not authorized to conceal impossible geometry or silently change the information contract. This is the principal unresolved runtime design constraint, not an invitation to choose arbitrary breakpoints.

## 7. File-level implementation approach

### Shared native resources

Reuse the existing `w_*` colors and live drawables:

- `widget_card_bg.xml`
- `widget_chip_current.xml`
- `widget_chip_next.xml`
- `widget_chip_transparent.xml`
- `widget_active_row.xml`
- `widget_progress_bar.xml`
- `widget_progress_bar_warm.xml`

Introduce only the small shared dimension/text-style definitions needed for section 4. An API-qualified resource for system-compatible outer radius is reasonable; it does not authorize dynamic recoloring. Ensure a reference added to a style is actually supported in `RemoteViews` inflation across the supported API range.

Migrate the live Full Table away from its old fixed-color badge/progress resources. Do not globally recolor `widget_background.xml`, `widget_status_badge.xml`, or `widget_badge_background.xml` without tracing their other consumers. The obsolete `widget_progress_drawable.xml` was removed together with the unused legacy layouts after reference verification.

### Renderer boundary

Keep the seven public render entry points and provider mapping unless a strictly internal presentation refactor is necessary. Reuse existing state formatting, `setupChronometer`, `applyDirection`, click binding, and state predicates. Extract only enough local rendering logic to populate a bounded set of compositions without copying business logic.

Do not introduce a generic widget engine, a persisted size cache, background text-measurement service, new polling, or per-second calculations. Select/build presentation at the existing update/resize boundary. All alternative layouts must receive equivalent data bindings and state resets.

Preserve root IDs where practical. Existing instrumentation locates these roots and specific text/cell IDs. If a view hierarchy changes, update tests to inspect actual rendered content and geometry, not merely to accept a different implementation shape. Do not weaken six-clock or countdown assertions.

### Picker previews and documentation

A mature result includes an honest picker preview, not a separately styled marketing mockup. After the live layouts pass, add API-appropriate preview support if approved and supported by the existing toolchain; use the same visual resources and clearly synthetic content. Provide an appropriate older-host fallback rather than pretending all Android versions use the same preview API. Do not add a preview-generation framework.

Update README's widget claims and real screenshots after verification. In particular, its statement that wide clocks wrap rather than hiding AM/PM must describe the new single-line/reflow policy and any narrowly retained uniform-wrap fallback accurately. Keep the device/launcher caveat. Do not replace screenshots with fabricated data presented as an actual capture.

## 8. Ordered implementation tasks and checkpoints

This is a standalone handoff; do not overwrite `tasks/plan.md`, `tasks/todo.md`, or `tasks/evidence.md` merely because this file exists. They contain other work. Integrate status only with explicit authorization and without displacing unfinished items.

### T01 — establish a native baseline and readable size envelope

Dependency: implementation/device approval. Scope: existing instrumentation/debug host, small focused fixture changes only if approved.

- Identify the actual fresh debug artifact, source version/build mode, device/host, density, font scale, bounds, theme, language/time format, and displayed state.
- Capture baseline examples of the reported wide wrapping, square void, and stretched active table cell using synthetic fixtures. User observations are already accepted; do not rerun merely to challenge their validity.
- Measure critical minimum sizes and resolve section 6.5 if necessary before freezing metadata or responsive breakpoints.

Acceptance: reproducible native baseline and explicit feasible size policy, not screenshots with unknown provenance. Verify through actual measured native views and an authorized launcher where available.

### T02 — establish shared tokens through Strip and Banner

Dependency: T01. Keep each edit slice small: shared resources plus one or two layouts and their presentation binding only if needed.

- Apply shared surface, typography, progress, and bounded group spacing.
- Remove their unbounded interior spacers without changing displayed fields or state behavior.
- Exercise both themes, normal/warm states, complete long countdowns, and approved minimum sizes.

Acceptance: the two variants visibly belong to one family and fit their supported envelope. Checkpoint: native visual review before spreading the treatment.

### T03 — correct Square composition

Dependency: T02. Scope: Square layout and its necessary fit/binding logic.

- Recompose the entire content group; implement top/footer reflow without losing clock information.
- Keep the short-height Strip fallback and stable click/deadline behavior.
- Test narrow/tall dimensions, RTL, and scaled text.

Acceptance: no single giant internal gap; current/next clock data remain visible in supported normal sizes.

### T04 — correct Wide schedule geometry and fit

Dependency: T02 and approved T01 size policy. Scope: Wide layout/composition resources and localized renderer changes.

- Implement fit-based six-column or three-column/two-row composition, with the strictly bounded shallow fallback only where necessary.
- Equalize all cell geometry; remove ID-specific Dhuhr emphasis; preserve existing state predicates.
- Verify complete 12h/24h clocks, state transitions, simultaneous differently sized instances, and no reset leaks.

Acceptance: the user's lone tall AM/PM chip cannot recur; no clock content disappears. Checkpoint: inspect all six cells, not just the current one, before proceeding.

### T05 — unify Full Table and bound its active cell

Dependency: T02. Scope: Full Table layout/resources and presentation binding only.

- Replace conflicting badge/gradient treatment with the shared hierarchy.
- Reflow metadata when necessary; preserve the daily 3×2 grid.
- Bound all six cells equally; remove static Dhuhr typography; exercise moving active states and 250×220dp fit.

Acceptance: current highlight no longer becomes a giant stretched panel; all metadata and daily clocks remain available within the approved size policy.

### T06 — refine the two transparent variants

Dependency: T02. Scope: two keyguard layouts and shared keyguard text styles if needed.

- Align typography, column fit, insets, and shadow treatment while retaining transparency and categories.
- Verify complete clocks on light, dark, and busy wallpaper backgrounds in a permitted host.
- Report real lockscreen testing separately from custom-host rendering.

Acceptance: coherent transparent siblings, not converted home cards. Checkpoint: review the complete seven-variant family together.

### T07 — finish previews, documentation, and evidence

Dependency: T03–T06. Scope: approved picker metadata/resources, relevant README sections/images, and focused existing test updates.

- Ensure picker, freshly placed widgets, and already-installed resized widgets use the final design.
- Run the verification matrix below and retain only authorized final evidence/deliverables.
- Document tested environments and untested limits. Remove only task-created disposable scripts/captures/artifacts; preserve original assets, user data, and approved final screenshots.

Acceptance: live native visuals, truthful documentation, protected behavior, and no temporary scaffolding. Never replace this checkpoint with a successful build alone.

## 9. Verification contract

### 9.1 Existing proof and its limits

`android/app/src/androidTest/java/org/muslimtechnician/muajjin/PrayerLifecycleInstrumentedTest.java` already exercises all seven providers plus a duplicate Strip, offline renewal, native Chronometers, RTL, and heights 80/220dp. Full Table is checked at 250dp width; Wide clock assertions reject ellipsis but currently permit wrapping. Its optional `captureSurfaces=true` captures a debug Activity column after the resize loop, not a real launcher or system lockscreen.

`android/app/src/debug/java/org/muslimtechnician/muajjin/WidgetTestActivity.java` is a non-exported native host. Preserve it. Do not launch the production MainActivity/WebView over native fixtures: that can synchronize application settings over synthetic test data.

The existing resize test explicitly dispatches after updating options. Add an actual host-resize interaction check; do not claim it independently proves callback-only update behavior. A duplicate Strip is not sufficient proof that differently sized Wide instances choose independent layouts.

### 9.2 Required native visual matrix

Use deterministic synthetic states where possible; do not depend on waiting for the real day to reach each prayer. Any new fixture seam must stay in debug/tests or be a narrowly justified pure presentation seam, not a production clock override.

| Axis | Required coverage |
|---|---|
| Variant | All seven; all five home variants reviewed together |
| Theme | Light and dark resource modes; transparent variants on light/dark/busy wallpaper |
| Host | Native debug host plus an actual launcher; Xiaomi/MIUI/HyperOS from the reported environment if authorized/available, and AOSP/Pixel-style reference host |
| OS | At least an API-24–30 compatibility environment and API 31+ responsive environment; API 36 target path |
| Size | Actual reported host bounds; existing declared minima; 80dp and 220dp heights; Full Table 250×220dp; narrow/tall and wide/shallow; portrait/landscape if supported |
| Breakpoints | Each actual composition threshold at −1dp, exact threshold, +1dp, including the existing 100dp fallback boundary |
| Density | Equivalent dp bounds at two densities, checking raster rounding without changing proportions |
| Font scale | 1.0, 1.3, 1.5, 2.0; record any approved minimum-size requirement |
| Locale | English, Bengali, imported Arabic RTL; long location/Hijri/prayer labels; translated day periods |
| Clock format | 12h and 24h; narrow/wide numerals; hour-bearing countdown and minute-only countdown |
| State | Current prayer moving across entries; next highlight; passed entries; no current prayer; warm state where supported; recovery/invalid data; overnight state |
| Multiple instances | Same provider at different bounds simultaneously; no shared layout selection leaking between instances |
| Lifecycle | Resize, theme/locale change, update while host remains open, and whole-widget tap to open |

Not every axis needs a full Cartesian product. Every variant needs a baseline in both themes, and the known high-risk combinations must be exercised: narrow+12h, RTL+scaled text, light+active/warm styling, Full Table minimum size, and differently sized Wide instances.

If a device/launcher is unavailable, state that exact limitation. Do not label a debug Activity screenshot “tested on Xiaomi.” Do not claim universal Android or real-keyguard validation.

### 9.3 Permanent regression coverage worth retaining

Use the existing JUnit/instrumentation conventions. Keep assertions about actual user-visible failures, not XML source strings or exact incidental implementation structure:

- Every complete rendered clock is present with no unintended ellipsis/clipping, including localized day periods.
- All six schedule cells in a selected composition have matching heights and intended baseline alignment within pixel-rounding tolerance.
- Current/next appearance follows existing state after updates and resets; Dhuhr has no permanent ID-specific emphasis.
- Active decoration remains bounded to a content-sized cell, rather than growing with extra grid-slot height.
- Glyphs fit line boxes for Bengali/Arabic at supported sizes/scales.
- Resize/composition changes preserve the real Chronometer deadline, count-down mode, root click action, RTL, and complete required data.
- Invalid/recovery state cannot retain a previous prayer's stale highlight.

Keep countdown/alarm/domain tests intact. New static-source snapshots or “the XML contains 16dp” tests are not evidence of good rendering. Use manual native finish review for optical balance; tests cannot certify beauty.

### 9.4 Commands — only after relevant approval

Use the repository toolchain: Bun 1.2.2, JDK 21, SDK/build tools 36/36.0.0, checked-in Gradle wrapper. Do not install or upgrade anything without separate permission.

Native unit gate, from `android/` on Windows:

```text
gradlew.bat :app:testDebugUnitTest
```

For a fresh standard debug artifact, from the repository root, build intended web assets and sync before packaging:

```text
bun run build
bun run sync:android
```

Then from `android/`:

```text
gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest -PbuildMode=nongithub
```

Use matching source epoch settings for web/native builds. If an explicit epoch is selected, pass that same value through the repository's documented web environment/native `-PsourceDateEpoch` mechanism. Never repair a metadata mismatch by editing generated `build-info.json` or disabling the packaging check. For GitHub mode, use `bun run build:github` and matching `-PbuildMode=github`.

The assemble command **does not run instrumentation**. After approval to install the freshly matching app/test APKs on a selected synthetic-data device and grant the required widget/notification permissions, the current runner is:

```text
.tools\android-sdk\platform-tools\adb.exe -s <authorized-serial> shell am instrument -w -e captureSurfaces true org.muslimtechnician.muajjin.debug.test/androidx.test.runner.AndroidJUnitRunner
```

`<authorized-serial>` is an explicit future device selection, not permission to use the first connected personal phone. Verify the tool path exists before use. Installation, binding, permission grants, and evidence export need their own approved exact commands; do not invent a universal grant sequence for every OS.

Existing Bun/native synchronization regression when needed:

```text
bun test tests/widget-sync.test.ts
```

If approved work reaches TypeScript or shared web contracts, use `bun run validate` as the full web gate. It ends with GitHub assets in `dist/`; rebuild the intended mode before any subsequent sync. Native layout changes are not validated by web lint or a Vite build. Do not run repository-wide formatting for this task.

### 9.5 Optical finish gate

Review actual native captures at normal viewing size, not only zoomed emulator previews:

- Card edges, content starts, and progress starts/ends align.
- Corners look coherent and no stroke is clipped by the host.
- The countdown is the dominant information without overwhelming context.
- No single arbitrary interior void splits related information.
- All six cells read as one organized schedule, with matched geometry and clear state.
- No lone day-period wrap creates a taller tile; any approved uniform fallback is visibly deliberate.
- Full Table metadata is not stuck to the corners; current context is no longer a stretched cyan banner.
- Active decoration is local and proportional; it never resembles an accidental empty panel.
- Current/next state remains distinguishable through fill/outline and existing context, not hue alone.
- Long countdowns do not collide with context or alter neighboring alignment.
- RTL is a designed mirrored layout, not simply an English screenshot with Arabic strings inserted.
- Transparent variants remain legible to the extent their wallpaper-dependent contract permits.

## 10. Completion criteria and final report for the implementation AI

Do not say “done” because XML compiles. Complete means:

1. All seven variants follow this shared system and their specified compositions.
2. The four primary reported defects are resolved: hollow Square, isolated Wide schedule, uneven wrapped Wide cells, and stretched Full Table active panel.
3. Full Table no longer uses a conflicting cyan/gradient design; static Dhuhr emphasis is removed.
4. All required prayer/time/location/Hijri information is preserved within the approved size policy; no silent format changes or missing day periods.
5. Native deadline/progress/RTL/click behavior and protected scheduling/storage/calculation boundaries remain intact.
6. The actual approved size envelope and any minimum/fallback compatibility changes are documented, with no universal pixel-identity claim.
7. Both themes, all seven variants, key risky layouts/states, and real-host resizing are visually verified to the available device coverage.
8. Picker/documentation accurately represent the final rendered widgets, where those changes were approved.
9. Existing relevant regressions pass; permanent new tests cover actual plausible failures rather than incidental source text.
10. No task-created temporary scaffolding remains; no unrelated files, version numbers, dependencies, provider categories, or app architecture changed.

Final report must contain: changed file groups; concise before/after defect explanation; exact tests/commands actually executed; real visual capture provenance and tested matrix; size-policy decisions; untouched technical boundaries; and explicit untested host/device/accessibility limits. Link approved final screenshots. Do not claim unexecuted checks or present synthetic mockups as native proof.

## 11. Sources and verification status of this handoff

### Repository sources inspected

- `android/app/src/main/java/org/muslimtechnician/muajjin/PrayerWidgetHelper.java`: provider mapping, active layout selection, fields, state presentation, Chronometer, and protected alarms.
- `android/app/src/main/java/org/muslimtechnician/muajjin/NativePresentation.java:60–68`: locale-aware selected clock formatting.
- `android/app/src/main/java/org/muslimtechnician/muajjin/PrayerUpdateCoordinator.java:13–55`: serialized shared snapshot/update ownership.
- Seven active XML layouts listed in section 3; all seven provider metadata files; provider callbacks and manifest registrations.
- Existing widget drawables, `values/colors.xml`, `values-night/colors.xml`, and theme resources.
- `PrayerLifecycleInstrumentedTest.java`, debug `WidgetTestActivity.java`, native build configuration, `package.json`, README, and its three actual widget screenshots.
- `tasks/plan.md:106–113`: existing delivery boundaries and native-fixture warning. This document does not supersede that work wholesale.

### Official Android references consulted

- [Widget sizing and responsive layouts](https://developer.android.com/develop/ui/views/appwidgets/layouts): API-31 responsive/exact layouts; limitations of older min/max range estimation; possible absent/empty host size lists.
- [Create a basic widget](https://developer.android.com/develop/ui/views/appwidgets): native widget construction and platform constraints.
- [System widget corner guidance](https://developer.android.com/develop/ui/compose/glance/create-app-widget#rounded-corner): Android-12 system background radius and proportional inner geometry. Cited for platform corner behavior only, **not** as a recommendation to migrate this Java/RemoteViews app to Glance.

### What was and was not verified now

Verified by read-only inspection: screenshot defects, active layout/renderer mapping, existing colors and size hints, current state-style inconsistencies, README capture attribution, and the limits of the existing instrumentation.

Not performed at authoring time: application edits, native rendering of the proposed design, builds, tests, emulator/device operation, contrast measurement, actual Xiaomi layout measurement, or runtime validation of the proposed typography/breakpoints.

Historical note: the design was implemented in 2.9.0; 2.9.1 corrected the widget footprints. Executed verification and remaining device limits are recorded in `README.md` and `tasks/evidence.md`.
