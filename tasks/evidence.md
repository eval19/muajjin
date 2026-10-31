# Muajjin implementation evidence

## 2.9.83 correction — pinned strip line (unreleased)

- The user reported 2.9.83 as worse: its `dropLabel` heuristic hid the prayer label
  on their host, which is not acceptable. That heuristic, the locale-widened
  countdown measurement, `countdownGlyphHeight`/`countdownMinHeight` and the
  compact override were all removed.
- Corrected approach: `widget_prayer_strip.xml` is a FrameLayout whose hero
  reserves the bar row by an 8dp bottom margin, with the progress include pinned
  with `layout_gravity="bottom"`. The bar therefore cannot be clipped regardless of
  text size. Strip gaps tightened: label→timer 2dp, progress gap 4dp.
- Verified on the disposable API 36 host at 190×100 in English and Arabic, Normal
  and Larger: label visible in all four cases (labelH 19/23 dp), bar present
  (4 dp, bottom 8 dp). Full native suite: 17 tests, 278-case matrix, 0 failures.
- 2.9.83 remains published with the label-hiding behaviour; this correction needs a
  follow-up version.

## 2.9.83 progress-line prerelease — 2026-10-31 (published prerelease)

- Requested fix: with Larger widget text the 2×1 lost its progress line. Root
  cause: label + enlarged timer + bar exceeded the host height, so the last child
  (the bar) was clipped. Source
  `e7dbd380332605e39d2ad266fc60d83487c5b5ff` (9 allowlisted files), annotated
  `v2.9.83`, version 2.9.83 / versionCode 301.
- Fix: the strip hero takes the vertical weight so the bar keeps its row; when the
  host is too short for label + timer + line, `WidgetLayout.dropLabel` hides the
  label and the helper honours it. Larger stays exactly 1.2× of the resolved
  profile (no compact override). New regression
  `stripKeepsItsProgressLineWhenTheHostIsShort` asserts the bar stays inside the
  widget and that a near-full-width line is drawn at 190/160×96 and ×72 dp in both
  languages and both sizes; the edge-alignment case now uses a 190×110 host so it
  exercises alignment with the label present.
- Reproduction: with the pre-fix layout the same probe failed at 160×72
  (`band=69` of 160), and the two-size matrix clipped `tv_strip_countdown`
  (required 47, available 41); both pass after the fix. Verified native suite:
  **17 instrumentation tests**, 278-case matrix, 0 failures; 302 web tests.
- Hosted [signed run 41152268947](https://github.com/eval19/muajjin/actions/runs/41152268947)
  passed both web modes, native unit/debug/test-APK assembly and controlled
  signed release assembly, epoch 1793407776. Artifact `13023298744`, archive
  SHA-256 `026fcc9535138f9b4b8618608d6e7d35b68188741a9a52797db45ad74a483649`.
- Signed APK `muajjin-2.9.83-301-github-release.apk`, 5,095,754 bytes, SHA-256
  `7485b14921d24f944bc6baff4409a53d07de34c4eeec59d8860bbf177e193a89`; verified
  v2/v3 for API 24–36, original certificate, 16 KB alignment, package
  `org.muslimtechnician.muajjin`, 2.9.83/301, GitHub mode, matching epoch.
- Published [v2.9.83 prerelease](https://github.com/eval19/muajjin/releases/tag/v2.9.83),
  release ID `429626906`; anonymous canonical downloads match. Latest stays
  v2.9.1; earlier releases unchanged. Final APK/checksum retained in
  `.tools/release-2.9.83-output/`.

## 2.9.82 reading-edge prerelease — 2026-10-31 (published prerelease)

- Requested follow-up to 2.9.81: the user rejected the centred 2×1 hero and asked
  for reading-edge alignment. Source `2058557d85246ef80dcd7682cf8303ca9a702a53`
  (8 allowlisted files), annotated `v2.9.82`, version 2.9.82 / versionCode 300.
  The strip uses `gravity="start"` (left in LTR, right in RTL) and the regression
  `stripHeroFollowsTheReadingEdgeInBothDirections` asserts both edges at Normal
  and Larger; it passed on the disposable emulator together with the capture probe.
- Local versioned smoke: identity resolves to 2.9.82/300; Bun 1.2.2 reports
  **302 tests / 798 expectations / zero failures**.
- Hosted [signed run 41148840434](https://github.com/eval19/muajjin/actions/runs/41148840434)
  passed both web modes, native unit/debug/test-APK assembly and controlled
  signed release assembly, epoch 1793405508.
- Downloaded Actions artifact `13022556081`; archive SHA-256
  `08e93db902249f18a8a85b0747f00dcb645c5cc9be35b2cec4cad1f1114dc6b8` matches GitHub.
- Signed APK `muajjin-2.9.82-300-github-release.apk`, 5,095,754 bytes, SHA-256
  `1d3128da58d0ee799f67dbeff5a31b1e5fb42555cdeda0599b458898f8562db5`; verified
  v2/v3 for API 24–36, original certificate, 16 KB alignment, ZIP integrity,
  package `org.muslimtechnician.muajjin`, 2.9.82/300, GitHub mode, matching epoch,
  debug host absent.
- Published [v2.9.82 prerelease](https://github.com/eval19/muajjin/releases/tag/v2.9.82),
  release ID `429609717`, with only the signed APK and its 105-byte `.sha256`;
  anonymous canonical downloads match. Stable/latest remains v2.9.1 and the
  v2.9.81 release is unchanged.
- Final APK/checksum retained in `.tools/release-2.9.82-output/`; watcher,
  verifier, build metadata and Actions result JSON removed.

## 2.9.81 widget balance prerelease — 2026-10-31 (published prerelease)

- User requested a prerelease named 2.9.81 containing the widget balance and the
  Normal/Larger text setting. Version 2.9.81, Android versionCode 299 (next after
  298), same package and signing identity; stable/latest must stay v2.9.1.
- The 2×1 keeps the centred composition that the earlier imbalance prompted; the
  user declined further composition changes for the smallest widget in this pass.
- Source: 23 explicitly allowlisted files committed as
  `b20c5f56502ac59cb4e1caa282186eee0cf762f7`; annotated `v2.9.81` targets that
  source. Local versioned smoke: identity resolves to 2.9.81/299 and Bun 1.2.2
  reports 302 tests / 798 expectations / zero failures.
- Hosted [signed run 41147661624](https://github.com/eval19/muajjin/actions/runs/41147661624)
  passed both web modes, native unit/debug/test-APK assembly and controlled
  signed release assembly from that source, epoch 1793404770.
- Downloaded Actions artifact `13021373300`; archive SHA-256
  `99f3d5c09f3f2e722ba6c5b85b99c000919f5c5b19a504b1c406f9763dce23e9` matches
  GitHub's digest.
- Signed APK: `muajjin-2.9.81-299-github-release.apk`, 5,095,754 bytes, SHA-256
  `99b982da1e7c1f39e85d4b3560ac27a8d907894414cd88db7ae92e33584d1a08`.
  Verified: v2/v3 signatures for API 24–36, original certificate
  `78f326588a8b17fb03910e1062341113bf5cdb224ac2a36da0116b6c7faf5f8e`, 16 KB
  alignment, ZIP integrity, package `org.muslimtechnician.muajjin`, version
  2.9.81/299, GitHub mode, matching epoch, debug widget host absent.
- Published [v2.9.81 prerelease](https://github.com/eval19/muajjin/releases/tag/v2.9.81),
  release ID `429603877`, with only the signed APK and its 105-byte `.sha256`.
  Authenticated and anonymous canonical downloads match the verified bytes.
  Stable/latest remains v2.9.1; v2.9.8 and earlier releases keep their IDs/assets.
- Final APK/checksum retained in `.tools/release-2.9.81-output/`; watcher,
  verifier, build metadata and Actions result JSON removed.
- Existing evidence for the implementation is recorded in the widget section
  below: 302 web tests, 52 JVM unit tests, 16 instrumentation tests including the
  278-case matrix, and inspected Normal/Larger renders.

## Widget balance and two text sizes — 2026-10-31 (local source, shipped in 2.9.81 for type scale)

- Post-2.9.81 correction (unreleased): the user rejected the centred 2×1 hero and
  asked for reading-edge alignment — left in English, right in Arabic. The strip
  label and countdown are back to `gravity="start"` (start resolves to the right
  edge under RTL), and the regression now asserts that both sit on the reading
  edge with a left/right progress bar per direction. Verified on the disposable
  emulator: 2 instrumentation tests passed and the native renders show "Asr in"
  flush left and "العصر بعد" flush right. 2.9.81 remains published with the
  centred composition; this change needs a follow-up version.

- Approval: rebalance all seven existing widgets and add exactly one Normal/Larger
  text toggle, with builds and disposable-emulator verification. No new providers,
  dependencies, versions, commits or publication; numerals unchanged.
- Composition: Strip's label and countdown now share the card's centre axis
  instead of hugging one edge; square/banner/lock-medium rows use explicit
  non-baseline composite alignment; the Wide/Table header and grid keep their
  existing matched tracks. No value, cell, column or provider was removed.
- Two sizes: one durable `widgetLargeText` setting (default Normal), carried in
  the existing presentation payload, so it changes only the presentation revision
  — no prayer recalculation, alarms or timers. Native renders Normal from the
  shipped resources and Larger as exactly 1.2× of the *resolved* profile, so the
  compact fallback can never shrink Larger below Normal; only spacing tightens.
  A six-cell single row uses 1dp cell padding to keep complete names. The two
  keyguard-only variants stay Normal: their fixed one-row stack cannot hold a
  larger three-line composition without dropping a value.
- Failing-before proof: with Larger enabled, the English Wide at 360×220 first
  downgraded to the Banner and dropped the daily grid; after the dense six-column
  escape it renders all six prayers at a measured 190dp of 220dp. An attempted
  tighter vertical tier fitted the two-row grid but clipped a clock line box
  (required 22dp, available 19dp), so it was reverted rather than shipping a
  squeezed grid.
- Native regressions: `WidgetPresentationInstrumentedTest` passed 278 matrix
  cases plus new same-bounds Normal/Larger and Strip-centre cases, all with no
  clipped glyphs; `WidgetNumeralInstrumentedTest` and `NativeStorageInstrumentedTest`
  (including the new default/round-trip/non-boolean-recovery case) passed.
  Combined run: **16 instrumentation tests, 0 failures**; **52 JVM unit tests,
  0 failures**.
- Arabic renders at 190×100 and 380×220/320 were captured from the real native
  host for Normal and Larger and inspected: centred Strip, balanced banner,
  square and table, and a complete six-cell Wide row with wrapped clocks.
- Web: zero-warning lint, both TypeScript projects, the domain suite (301 tests
  before the added payload regression) and both builds. The new synchronizer case
  proves the flag changes presentation only and delivers one physical schedule.
- Cross-locale check (2026-10-31): rendered the real widget with Arabic words on
  an English device and English words on an `ar-EG` device. Words and direction
  follow the app language; digits and the numeric day-period ordering follow the
  device. The web app, by contrast, always composes Latin digits itself
  (`formatTime` builds `${hours12}:${minutes}`), so on an Arabic device an English
  app still shows `1:51 AM`. App and widget therefore disagree on numerals; no
  change was made because the device-numeral policy is still an open user
  decision.
- Limits: host captures are the native debug host, not an OEM launcher; the
  settings switch itself was not tapped on a device in this pass. All four new
  display-settings keys resolve in the English, Bengali and Arabic templates, and
  the settings page references no missing key. Very small hosts, large system font
  scales and long imported labels remain bounded by the fixed footprints.

## 2.9.8 Arabic visual prerelease — 2026-10-31 (published prerelease)

- User explicitly approved version 2.9.8/298, the session's tracked visual fixes,
  release documentation, new branch/tag, hosted signing with existing secrets,
  and verified GitHub prerelease publication. Device-dependent numerals remain.
- Read-only GitHub checks: repository `eval19/muajjin`, push access available,
  release workflow active, prior v2.9.7 remains a prerelease, stable/latest is
  v2.9.1. Remote `release/2.9.8` and `v2.9.8` were absent.
- Exclude unrelated `bakery_raw.json`, prayer-comparison CSV/Markdown and
  `widget-colour-screenshots/`; never upload credentials or local signing data.
- Existing verified visual/runtime evidence below applies to the included
  implementation. The keyguard-provider enumeration limitation is retained,
  not hidden by a claim of complete launcher/lock-screen acceptance.
- Source: 47 explicitly allowlisted files committed as
  `b26565493f830a871aa143206740bed944febc5c`; annotated `v2.9.8` targets that
  exact source. The release branch's later evidence-only commit does not move
  the tag or change the published artifact's source.
- Local versioned regression smoke: artifact identity resolves to 2.9.8/298;
  Bun 1.2.2 reports 301 tests / 793 expectations / zero failures.
- Hosted [signed run 41140634852](https://github.com/eval19/muajjin/actions/runs/41140634852)
  passed both web modes' quality gates, native unit/debug/test-APK assembly and
  controlled signed release assembly from that exact source, epoch 1793400517.
  Hosted assembly is not native instrumentation execution; the earlier rendered
  implementation evidence and its limits remain separately recorded below.
- Downloaded Actions artifact `13017869101`; archive SHA-256
  `7f27f46120041cb554797fda5664fc1081a17fc1fb2fc745d529d200b9bd49b5`
  matches GitHub's digest. Inspected artifact members; no signing material was
  downloaded, exported or uploaded.
- Signed APK: `muajjin-2.9.8-298-github-release.apk`, 5,091,658 bytes,
  SHA-256 `0a0f583b85e6cd68ec11220c0817f8d81ed50bec356e9b3359ae246e1f947fe7`.
  Local verification confirms v2/v3 signatures for API 24–36, original release
  certificate `78f326588a8b17fb03910e1062341113bf5cdb224ac2a36da0116b6c7faf5f8e`,
  16 KB alignment, ZIP integrity, package `org.muslimtechnician.muajjin`,
  version 2.9.8/298, minSDK24/targetSDK36, GitHub mode and matching source epoch.
  Debug widget activity is absent. New visual CSS and changed web/native
  payloads are present relative to 2.9.7; equal whole-file size is not identity.
  This is signed artifact/provenance verification, not an independent rebuild.
- Published [v2.9.8 prerelease](https://github.com/eval19/muajjin/releases/tag/v2.9.8),
  release ID `429564951`. Only the signed APK and its 103-byte `.sha256` are
  public assets. Server digests, authenticated draft-download bytes and anonymous
  canonical public-download bytes match the independently verified local files.
  Stable/latest remains v2.9.1; its assets and the prior v2.9.7 release/assets
  retain their original IDs/digests.
- Final APK/checksum remain in `.tools/release-2.9.8-output/`. Cleanup verified:
  only those two files remain; temporary watcher/verifier, build metadata and
  Actions result JSON removed. No new local packages, device mutations, signing
  keys, repository settings or unrelated files were changed.


## Arabic visual finish — 2026-10-31 (implementation verified; shipped in 2.9.8)

- Approval: visual-only app and existing seven-widget refinements, temporary
  screenshots/builds and an isolated read-only instance of the existing API 36
  AVD. No dependencies, commits, signing, publication, personal-device changes,
  host settings, calculation/storage/scheduling changes or new widget identities.
- App: Arabic-specific leading, logical control alignment, wrapping text buttons,
  correctly directed Select roots/items and switch thumbs; isolated numeric/clock
  runs; responsive dashboard tables, calendar, square compass and Qaza controls;
  contained settings/import/font metadata and overlays; shared safe-area bottom
  space. Existing translations, font selection and custom-font ownership remain.
  At enlarged type, dense tables scroll locally rather than clipping values.
- Widgets: names and clocks now share the cell's logical start edge independently
  of clock paragraph direction. Both Square footer resources use LTR clock runs;
  stacked Square clocks retain the Arabic right edge. Widget-only clock strings
  insert an invisible LTR boundary before the translated day period so Arabic-Indic
  digits cannot move it before the numeric clock. The underlying formatter,
  digits, values and notification formatting remain unchanged. Stacked Wide
  measurement now selects the existing matching six-cell/two-row resource.
- Failing-before native proof: the new long-header case measured vertical content
  but inflated a horizontal header; Square footer paragraph direction was -1
  instead of +1. Actual RTL schedule clock edges were 44px versus the required
  101px. The next run exposed Arabic-Indic day-period reversal; its numeric
  selection occupied x18–40 while the period occupied x0–15. These assertions
  pass after the corresponding presentation fixes without relaxed tolerances.
- Web gates: pinned Bun 1.2.2 `bun run validate` passed zero-warning ESLint,
  both TypeScript projects, **301 tests / 793 expectations / zero failures**, and
  standard plus GitHub builds. Existing source-epoch, Capacitor script,
  Browserslist-age and large-chunk warnings remain unsuppressed.
- Browser proof: Microsoft Edge 156 identity and visible, non-minimized,
  non-cloaked Default-desktop window verified. A new task-owned tab/origin on
  localhost:18083 used only synthetic settings. Inspected before/after images:
  key routes at 320/360px; all 13 main/settings/404 routes at 393px dark; three
  onboarding routes at 320px. Checked 20px root typography on seven high-risk
  routes, desktop framed layout at 768/1024/1440px, 12h/24h/system clocks,
  Jama'ah absent/present, a long Arabic city, Qaza 99999 editing/focus restoration,
  live RTL dropdowns/switches, navigation-sheet keyboard access, and real
  Arabic/English/Bengali selection. Final sampled states had no horizontal page
  overflow, measured text-width overflow or unhandled window errors.
- Native proof: existing JDK21/SDK36 and checked-in Gradle wrapper, offline;
  **52 unit tests, zero failures/errors/skips**, debug/test-APK assembly.
  On disposable emulator-5584, `WidgetPresentationInstrumentedTest` and
  `WidgetNumeralInstrumentedTest` pass **9 tests in light and 9 in dark**;
  the presentation matrix covers **278 cases per theme**. Inspected actual
  inflated native images for all seven Arabic variants in both themes. Existing
  pixel checks retain RTL progress direction, palette, reset and theme behavior.
- Limits: native images are from the native debug host, not an OEM launcher or
  real keyguard. Matrix enlarged-font cases also enlarge host bounds; browser
  text stress is root typography, not physical Android accessibility testing.
  Uploaded custom-font decoding, TalkBack, actual location permission states,
  GitHub update-dialog runtime and on-device WebView language-to-widget switching
  were not exercised in this pass. Existing handoff/domain regressions passed.
- Additional check, not a pass: `WidgetProviderInfoInstrumentedTest` failed to
  enumerate `PrayerLockSmallProvider` via its installed-provider query on this
  emulator. Provider declarations and that test were not changed or suppressed.
  All seven native layouts rendered successfully; no launcher/keyguard metadata
  acceptance is inferred from those images. Existing progress-palette minimum
  sampled contrast remains 1.33:1 light / 3.29:1 dark.


- Cleanup verified: synthetic localhost storage returned to empty; owned tab
  closed and original Edge attachment restored. The task preview and disposable
  emulator stopped; original preview/Edge listeners and emulator-5582 remain.
  `.tools/qa-arabic-finish/`, generated `dist/` and the newly created 2.9.7 debug
  APK were removed. Existing native build trees, working tool/dependency caches,
  original AVD and prior release artifacts were not purged.

## 2.9.7 Arabic localization — 2026-10-30 (published prerelease)

- Approved scope: commit/push `release/2.9.7` and annotated tag `v2.9.7`,
  version 2.9.7/297; use the existing signed GitHub workflow and hosted secrets,
  verify the artifact, and publish a prerelease with only the signed APK/checksum.
  Preserve prior releases and stable/latest `v2.9.1`; exclude unrelated untracked
  files and screenshot folders from commits and uploads.
- Implementation: complete 346-key built-in Arabic translation (`src/i18n/template-ar.json`),
  bundled IBM Plex Sans Arabic (Regular, SemiBold, Bold) in `public/fonts/` (web) and
  `android/app/src/main/res/font/` (native, API 26+), logical RTL spacing, mirrored
  dividers and progress bars, direction-aware icons, bidi-isolated clock values,
  automatic Arabic repair for imported translations, and in-place language switching
  across the existing seven widget identities without duplicate providers.
- Local verification: 301 web tests passed; TypeScript and ESLint zero warnings;
  5 native instrumentation tests (278 cases) on API 36 passed with zero defects;
  desktop Edge checks verified Arabic home, settings, translations, display,
  onboarding, Qibla, and prayer calendar.
- Published: [v2.9.7 prerelease](https://github.com/eval19/muajjin/releases/tag/v2.9.7),
  release ID `429506960`, exact source `1fefe705cbee9a60645100fc4bbb82940317bf22`.
  Annotated tag `v2.9.7` targets that source; the documentation evidence commit
  does not move it.
- Hosted verification: [signed run 41130467925](https://github.com/eval19/muajjin/actions/runs/41130467925)
  passed both distribution modes' lint, types, 300 Bun tests per mode and web
  builds; native regressions/debug/instrumentation-APK assembly and controlled
  signed release assembly passed.
- Artifact: `muajjin-2.9.7-297-github-release.apk`, 5,091,658 bytes, SHA-256
  `698fbb225f28d8b0bebdc9a4c4b6e0ba7ac7b9b7afb64dbbce8e14852b8ee528`.
  Downloaded Actions archive digest matches GitHub. Local verification confirms
  v2/v3 signatures for API 24–36 and the original release certificate
  `78f326588a8b17fb03910e1062341113bf5cdb224ac2a36da0116b6c7faf5f8e`,
  16 KB alignment, package `org.muslimtechnician.muajjin`, version 2.9.7/297,
  minSDK24/targetSDK36, GitHub mode and source epoch `1793395274`.
  Release resource dump confirms `font/ibm_plex_sans_arabic` retention through R8.
- Only the signed APK and its 103-byte `.sha256` are public release assets.
  Uploaded asset digests, authenticated draft-download bytes and anonymous
  canonical public-download bytes all match the independently verified local files.
  Stable `/releases/latest` remains `v2.9.1`.
- Final local APK/checksum are retained in `.tools/release-2.9.7-output/`.

## 2.9.6 widget colours — 2026-10-30 (published prerelease)

- Approved scope: commit/push `release/2.9.6` and annotated tag `v2.9.6`,
  version 2.9.6/296; use the existing signed GitHub workflow and hosted secrets,
  verify the artifact, and publish a prerelease with only the signed APK/checksum.
  Preserve prior releases and stable/latest `v2.9.1`; exclude unrelated untracked
  files and local screenshot folders from commits and uploads.
- Final palette and shared timing are frozen exactly as documented in
  `tasks/widget-progress-fill-colors.md`: accent green through 4/11, yellow
  at 7/11, orange at 9/11, orange-red at 10/11, and red at the target.
  Balanced green/lime bridges replace the concentrated green-to-yellow change.
- Local verification of this palette: 52 native unit tests and three targeted
  API 36 native-rendering tests passed. Captured anchor colours match resources;
  halfway pixels match independent references within one channel, without
  increasing tolerance. Matching theme fill extents and transition captures
  were inspected; previous screenshots remain local.
- Contrast limitation is retained and disclosed: minimum sampled fill/track
  contrast is 1.33:1 light / 3.29:1 dark. No physical-device, launcher, Doze,
  alarm-delivery or live-refresh measurement is claimed. No calculation,
  layout, countdown, notification or scheduling logic changed.
- Published: [v2.9.6 prerelease](https://github.com/eval19/muajjin/releases/tag/v2.9.6),
  release ID `429419825`, exact source
  `9139b7b5a088202c110d4633441769fdb1f51968`. Annotated tag `v2.9.6` targets
  that source; the later documentation-only evidence commit does not move it.
- Hosted verification: [signed run 41115858394](https://github.com/eval19/muajjin/actions/runs/41115858394)
  passed both distribution modes' lint, types, 300 Bun tests per mode and web
  builds; native regressions/debug/instrumentation-APK assembly and controlled
  signed release assembly passed. Instrumentation-APK assembly is not execution.
  [Tag verification run 41115761213](https://github.com/eval19/muajjin/actions/runs/41115761213)
  also passed; its unsigned APK was not published.
- Artifact: `muajjin-2.9.6-296-github-release.apk`, 4,558,580 bytes, SHA-256
  `5e79327c54bfc7aeebfaa0be0c3be58835f0a5904bd370159977e07b1746443b`.
  Downloaded Actions archive digest matches GitHub. Local verification confirms
  v2/v3 signatures for API 24–36 and the original release certificate
  `78f326588a8b17fb03910e1062341113bf5cdb224ac2a36da0116b6c7faf5f8e`,
  16 KB alignment, package `org.muslimtechnician.muajjin`, version 2.9.6/296,
  minSDK24/targetSDK36, GitHub mode and source epoch `1793388606`.
- Every one of the 24 compiled theme anchors resolves to the exact approved
  resource colour, including night-qualified accent aliases. The release
  manifest excludes `WidgetTestActivity`; APK ZIP integrity passed.
- Only the signed APK and its 103-byte `.sha256` are public release assets.
  Uploaded asset digests, authenticated draft-download bytes and anonymous
  canonical public-download bytes all match the independently verified local
  files. Stable `/releases/latest` remains `v2.9.1`.
- Final local APK/checksum are retained in `.tools/release-2.9.6-output/`.
  Archive/metadata verification used memory, not leftover extraction files.
  No signing material was downloaded, exported, exposed or changed; existing
  credential-manager authentication and hosted signing secrets were reused.
- Publication triggered the independent
  [Pages run 41116840318](https://github.com/eval19/muajjin/actions/runs/41116840318).
  Website deployment is not APK verification; the prior Pages hosting limitation
  remains separately documented below. No Pages/account settings were changed.

## 2.9.5 widget progress palette — 2026-10-27 (published prerelease)

- Release approval: prepare/push `release/2.9.5` and tag `v2.9.5`, version 2.9.5/295; build/sign with the existing GitHub workflow/secrets; verify and publish a prerelease containing only the signed APK and checksum. Prior releases and stable-latest selection stay unchanged; unrelated untracked files are excluded.
- Scope: only modern `w_progress_fill_0..11` resources/comments in both themes and affected numerical/rendered assertions; runtime sampler, theme resolver, widget helper, geometry, scheduling and API 24–30 legacy stages are unchanged.
- Selected controls: light `#0B6E5B → #EAB308 → #FB923C → #DC2626`; dark `#5FD3B5 → #FACC15 → #FB923C → #EF4444`. Green holds through index 1, yellow is index 6, orange index 9, red index 11; both initial resources reference `w_accent`. Intermediate literals were generated with the existing production OKLab conversion in a disposable Java helper; midpoint expectations used an independent double-precision implementation.
- Verification: checked-in `gradlew.bat` with existing JDK21/SDK36, offline; focused `WidgetProgressColorTest` passed all 13 cases; integrated `:app:testDebugUnitTest :app:assembleDebugAndroidTest` passed all 50 native unit tests (zero failures/errors/skips) and instrumentation-APK assembly. Existing flatDir, SDK XML and Gradle deprecation warnings remain.
- Runtime smoke: actual compiled production sampler, start `1800000000000`, duration `6600000` ms. Elapsed 0/300000/600000 gives exact theme green; 3600000 exact theme yellow; 5400000 exact shared orange; 6599999/6600000 exact selected red. New-interval reset returns green. Inspected 221 production samples per theme as dense swatches; no visible progression reversal.
- Contrast limitation: sampled minimum fill/unchanged-track ratios are light **1.39:1** at yellow and dark **2.88:1** at the red endpoint, below 3:1. Exact selected colours were retained; no track substitution or tolerance increase.
- Android pixel limit: ADB listed no connected device; no emulator was started and instrumentation was not run. Rendered hold/accent, selected yellow/orange/red, independent midpoints, host-theme switch, reset, RTL/variants and hidden-invalid-bar assertions compile but their pixels remain unverified for this palette. Historical 2.9.4 captures below do not verify this revision. No device installation or permission change was performed.
- Published: [v2.9.5 test prerelease](https://github.com/eval19/muajjin/releases/tag/v2.9.5), release ID `425028432`, exact source `1c876021e32f592835cccfb1f4746ee400a1bdc8`; annotated tag targets that source. The source is on `release/2.9.5`; the later documentation-only evidence commit does not move the tag. Stable `/releases/latest` remains `v2.9.1`.
- Hosted verification: [signed run 40448057059](https://github.com/eval19/muajjin/actions/runs/40448057059) passed resolve, both web lint/type/domain-test/build gates, native unit/debug/instrumentation-APK assembly, and controlled signed release assembly. Tag-triggered unsigned verification run `40448009490` also passed; its unsigned APK was not published.
- Artifact: `muajjin-2.9.5-295-github-release.apk`, **4,558,580 bytes**, SHA-256 `d58a19b54164f989ecd4d61fe37185374c686d8eec6ee61946aad35d05b1fd9e`. Downloaded Actions archive digest matches GitHub. Local verification confirms v2/v3 signatures for API 24–36 and original certificate `78f326588a8b17fb03910e1062341113bf5cdb224ac2a36da0116b6c7faf5f8e`, 16 KB-page alignment, package `org.muslimtechnician.muajjin`, version 2.9.5/295, minSDK24/targetSDK36, GitHub mode and source epoch `1793074055`. Compiled resources contain selected green/yellow/orange/red controls; release manifest excludes `WidgetTestActivity`.
- Published assets: only the signed APK and its 103-byte `.sha256`. Both uploaded/draft-download hashes and anonymous canonical public-download bytes match the verified local files. Final APK/checksum are retained in `.tools/release-2.9.5-output/`; no signing material was downloaded, exposed or changed.
- Separate hosting limit: publication-triggered [Pages run 40448831761](https://github.com/eval19/muajjin/actions/runs/40448831761) passed quality and web build, then deployment failed HTTP 404 (`Failed to create deployment` / `HttpError: Not Found`; action indicates Pages must be enabled). No Pages/account setting was changed. This does not affect the published APK or its download verification.

## 2.9.4 widget progress fill — 2026-10-25

- Scope: only the filled portion of the five bar-bearing home-screen widgets and its palette resources. Track, labels, keyguard widgets, layouts, countdowns, notifications, calculations, alarms and scheduling are unchanged.
- Implementation: `WidgetProgressColor` (pure OKLab; 12 anchors at `index/11`, opaque, endpoint-safe), `WidgetProgressPalette` (both theme palettes prepared once per process; one shared light/dark `ColorStateList` pair per dispatch), API 31+ dual-theme `setProgressTintList`. API 24–30 keeps the existing four stages and colours.
- Verification: `:app:testDebugUnitTest` passed 50 tests, including 13 new colour cases. `:app:lintDebug` reported only the pre-existing `MainActivity` `GestureBackNavigation` error and no findings in changed files. Instrumentation on the owned API 36 emulator in light and dark passed the three new tests (anchors/interiors/boundaries, host configuration selecting the prepared palette without redispatch, reset/duplicate/hidden). The existing presentation matrix reproduced four pre-existing failures identically on an untouched `8fa8296` baseline build. Sampled fill/track contrast minima: light 2.62:1 at anchor 0, dark 2.74:1 at anchor 11. Native captures matched every sampled anchor exactly.
- Source: commit `6245203` (`chore(release): prepare 2.9.4 (294)`) on `release/2.9.4`; tag `v2.9.4`; `package.json` 2.9.4, Android versionCode 294.
- Signed workflow run `40190447791` passed; tag-push quality run `40190435553` passed. APK `muajjin-2.9.4-294-github-release.apk`, 4,558,580 bytes, SHA-256 `c644315c1a955981c9bafa0350a2d73140fe7eca13f7bcec80cbd7e23a9363c5`, original certificate `78f326588a8b17fb03910e1062341113bf5cdb224ac2a36da0116b6c7faf5f8e`; package `org.muslimtechnician.muajjin`, version 2.9.4/294, target SDK 36, GitHub mode. `zipalign`, v2/v3 signature and merged-manifest checks passed; no `WidgetTestActivity` is packaged. The anonymous public download matched the uploaded hash.
- Release: `https://github.com/eval19/muajjin/releases/tag/v2.9.4` published as a **prerelease** with the signed APK and `.sha256`; `/releases/latest` remains v2.9.1, so the in-app updater does not advertise this build.
- Not verified: real launcher/keyguard hosts, live theme toggles performed by a host, API 24–30 devices, Doze/alarm delivery, battery comparison and physical-device behaviour. No physical device was used.

## 2.9.3 transition reliability — 2026-10-24

- Scope: exact-alarm/battery status and system-settings links, forced transition/fallback re-arm, one non-wakeup post-target fallback refresh, and next/last transition diagnostics. No countdown, layout, calculation, notification, numeral, or storage-schema change.
- Implementation: `PrayerWidgetAlarmReceiver` treats transition/fallback/reconcile as forced re-arms; `PrayerWidgetHelper` schedules the fallback at target + 15s and replaces it on the next target; `PrayerUpdateCoordinator` records throttled dispatch diagnostics; `PrayerWidgetPlugin` exposes reliability state and settings intents; Display Settings renders the reliability section.
- Local verification: app/tooling TypeScript, zero-warning ESLint, both Vite modes, **37 native unit tests** (5 parity, 23 timeline, 1 receiver policy, 8 progress), debug and instrumentation APK assembly. No Bun binary was available locally, so the Bun domain suite was left to the hosted workflow.
- Signed workflow run `40124978266` passed all jobs. APK `muajjin-2.9.3-293-github-release.apk`, 4,554,484 bytes, SHA-256 `0f0e7b5a6a6990d99b6f84154e289cf452378dac06f133a93338e50c89f955e3`, original certificate `78f326588a8b17fb03910e1062341113bf5cdb224ac2a36da0116b6c7faf5f8e`; package/version 2.9.3/293, GitHub mode. The release asset digest matches. The fallback action and reliability UI strings were present in the release APK.
- Release: `v2.9.3` is published as a **prerelease** from source `2e9f5d7a8f6bec6ab10fc709bb7989c927036577`. `/releases/latest` remains v2.9.1, so the in-app updater does not advertise the test build.
- Not yet verified: physical MIUI transition behavior, exact-alarm grant/deny/revoke, battery-optimization state, fallback timing, and force-stop limits. These are the purpose of the prerelease test. The release-triggered Pages deployment shares the previously documented hosting limitation and is independent of the APK.

## Fixed widget footprints — 2026-10-24

- Scope: all seven `android/app/src/main/res/xml/widget_*_info.xml` provider metadata files, `README.md`, and one installed-provider regression in `androidTest`. No schedule, layout, countdown, alarm, notification, or storage behavior changed.
- Change: `android:resizeMode="none"` and removal of `android:minResizeWidth`/`android:minResizeHeight` for all seven providers. Legacy `minWidth`/`minHeight` hints now use the platform cell convention: 110×40 (2×1), 110×110 (2×2), 250×40 (4×1), 250×220 (4×2/4×3). Target cells, compact host-size fallbacks, provider identities, and layouts are unchanged.
- Reason: the seven widgets are deliberately composed fixed variants. Resizing exposed no designed intermediate composition; a resized Strip could occupy a 2×2 host cell with centered dead space, and MIUI retained the enlarged footprint instead of returning to 2×1. Separately, the old content-sized hints (for example 88dp for a 1-row Strip) made launchers that add a 30dp margin round the initial placement up to two rows.
- Verification: `:app:testDebugUnitTest` passed 35 tests (5 parity, 22 timeline, 8 progress); `:app:assembleDebug` and `:app:assembleDebugAndroidTest` passed. `aapt2 dump xmltree` of `muajjin-2.9.0-291-nongithub-debug.apk` reports `resizeMode=0x0` (none), no `minResize*` attributes, and the canonical min pairs (110×40, 110×110, 250×40, 250×220, 250×220, 110×40, 250×40) for all seven providers. The instrumentation APK containing `WidgetProviderInfoInstrumentedTest`, which pins `resizeMode`, `minWidth`, and `minHeight` per provider, was assembled but not executed.
- Limits: a launcher that ignores `resizeMode` can still stretch a host cell. Widgets placed by an older build keep their host-stored footprint; remove and re-add them once. Actual MIUI add-time cell counts, resize-handle behavior, and existing-instance migration were not runtime-verified in this step; MIUI's effective cell metrics were inferred from the supplied screenshot and the platform 70dp/30dp convention.
- Release: 2.9.1 / Android versionCode 292 published at `https://github.com/eval19/muajjin/releases/tag/v2.9.1` from source `43fe901af72b02af5c456f371a5bffefda35b09f`; tag `v2.9.1` points to that commit. Signed workflow run `40115266456` passed resolve, both web modes, native quality, and signed release. APK SHA-256 `ab7f4c2496a233ffdebbc2c24d7ae28c1faa775e0d5c19ebade690a808bc63ca`; original certificate `78f326588a8b17fb03910e1062341113bf5cdb224ac2a36da0116b6c7faf5f8e`. The anonymous public download matched the uploaded bytes, package `org.muslimtechnician.muajjin`, version 2.9.1/292, and signature.

## 2.9.0 widget overhaul and 291 rebuild — 2026-10-24

- 2.9.0 (Android versionCode 290) shipped the seven-widget visual overhaul from source `3fe8ab4` and tag `v2.9.0`: shared spacing, typography, corners, equal schedule cells, and a bounded Full Table highlight.
- The replacement build 291 (`de1440a`) added staged progress-bar colors, device-numeral formatting, `ACTION_LOCALE_CHANGED` reconciliation, and the widget presentation regressions. Signed workflow run `40111207055` passed; asset `muajjin-2.9.0-291-github-release.apk`, 4,554,484 bytes, SHA-256 `7bd798d80b56ea4a8cfdb28eda79e45f9abccc57038138c2b8b4123c7847131d`, original certificate `78f326588a8b17fb03910e1062341113bf5cdb224ac2a36da0116b6c7faf5f8e`.
- Provenance caveat: the `v2.9.0` tag targets `3fe8ab4` (the build-290 source), while the published 291 asset metadata identifies `de1440a`. The release note was corrected on 2026-10-24 to state that split; the tag and assets were not moved. The fixed-footprint 2.9.1 tag is correct at `43fe901`.
- The 2.9.1 section above records the fixed-size correction and its signed release.

## App-health completion — 2.8.4 / Android versionCode 284

Completed app-health release, 2026-10-18. The historical sections below retain their original scope; their earlier signing/publication prerequisites are not current release-status claims.

- Approval: integrate the pending app/test fixes, repair the identified settings/lifecycle/native/APK-verifier defects, use existing tools and a read-only API36 emulator with synthetic data, curate a clean source branch, compile in GitHub, sign locally with the verified original key, and publish 2.8.4. No new packages/system images, signing-secret provisioning, Pages settings or personal-device changes.
- Samsung diagnosis remains bounded: the screenshot's exact APK/download bytes were not supplied. The public original 2.8.2 APK is signed and passes API35/36 signature verification; the separately inspected unsigned build is non-installable. Neither fact proves the S22 screenshot's exact cause.
- Previously published signed 2.8.3: `https://github.com/eval19/muajjin/releases/tag/v2.8.3`, source `b33d03e9a41315af88786d028e070eea320f5ccb`, successful GitHub compilation run `39095077020`. Original signer SHA-256: `78f326588a8b17fb03910e1062341113bf5cdb224ac2a36da0116b6c7faf5f8e`. Pending local font/Hijri/adjustment/bridge corrections were not included in that five-file release change.
- Failing-before UI proof: native keyboard input produced `121 → 10` on the fasting `+` control; resizing 390→1024 discarded draft `5 → 0`; direct final onboarding with saved `QA Cairo`/null coordinates finished with `0,0`; a stale settings form overwrote a newer unrelated durable city; an unrelated clock-format storage event reset draft `7 → 5`.
- Failing-before lifecycle proof against the immutable shipped source: manual and late compass permission grants left both orientation subscriptions attached after actual route unmount; caller abort left the old update service pending. Compass permission/foreground/focus are explicit controlled platform fixtures, not physical Edge visibility proof.
- Failing-before Android proof: three new real-SharedPreferences/JSON instrumented regressions failed with the expected astronomical `IllegalStateException`, presentation integer rejection and preference `ClassCastException`. All three pass after the native repairs; invalid coordinates do not become another city's schedule.
- Web gate: pinned Bun 1.2.2 `bun run validate` passed zero-warning lint, both TypeScript projects, **300 tests**, and standard/GitHub builds. Existing Capacitor script, stale Browserslist and large-chunk build warnings were not suppressed.
- Native gate: isolated existing JDK21/SDK36/Gradle8.14.3 builds passed **27 unit tests** (22 timeline + 5 exact-engine parity), debug assembly and runtime-test assembly. Existing SDK XML/deprecation warnings remain.
- Android runtime: **six unique instrumented tests pass**, including expired offline renewal, all seven widget providers plus a duplicate, compact/expanded resize, RTL labels, actual countdown deadlines and notification progress. The complete widget scenario also passes under Android dark mode; both theme surfaces were inspected and the original emulator mode restored. The final four-test storage class passes together, including a forced first commit failure after real SharedPreferences publication: rollback restores the previously acknowledged schedule/calculation/revision and unrelated data. No physical disk-full or power-cut claim is made. Debug host fixtures never had the WebView launched over them.
- Font runtime: all **14** actual production-provider/IndexedDB/FontFace cases pass on an initially empty isolated origin, covering pointer/readback/activation/cleanup/stale-selection/migration failures. The licensed bundled Ubuntu font was converted temporarily with already-present fontTools and Node Brotli; no package was installed. Fixture cleanup reports no remaining keys/databases.
- Repaired UI runtime: large adjustments step inward by one and disable outward steps; drafts survive breakpoint and unrelated commits; two actual tabs preserve independent fasting/Hijri saves; a quota-denied Save preserves stored data and navigation, then retries successfully. Damaged or concurrently damaged onboarding coordinates redirect to location setup; explicit zero coordinates finish correctly. Accessibility-tree inspection confirms contextual numeric controls and named Back navigation.
- Update runtime: the real service deadline settles a stalled boundary request as `null` after **10,006 ms**. Cancellation before response headers and during incomplete JSON bodies, followed by a successful later request, passes a real localhost HTTP regression.
- Current Android app: verified web build was synced before packaging. The API36 debug APK installs and opens; actual manual-location/madhab onboarding reaches the calculated dashboard. Both native schedule and presentation transactions are durably present; the initial onboarding-era warning clears after successful acknowledgements. The emulator's first-run stylus tutorial was an OS input overlay, not an application defect.
- APK verifier: **21 Python regressions pass**. Normal signature/trusted-signer verification remains mandatory; present JAR metadata is excluded only after an additional verified API23 v1 pass when minSDK24+ scheme selection omitted it. The final signed APK passes the real canonical CLI against the GitHub unsigned artifact: **484 uncompressed payload entries match**, with mode/epoch/checksum validation and authenticated v1 exclusion. Explicit API35–36 signature and 16KB-alignment checks pass.
- Completed runtime matrix: all 13 non-onboarding/catch-all routes rendered without uncaught errors, alongside the three exercised onboarding routes. Qaza commits Enter `234`, blur `235`, cancels Escape without changing `235`, and retains it on reload. A real RTL translation file imports/selects/reloads/deletes, and a real custom font uploads/loads from IndexedDB after reload/removes through the app UI. Responsive mobile/desktop surfaces were inspected after animations settled.
- Native restart/resume: actual native Qaza input persists `234`; native Back dismisses dialogs without leaving the route. Removing only the schedule revision from the stopped synthetic debug store is repaired after a real cold process launch, with saved location/count and both native transactions retained. The same live WebView survives Cairo→UTC background/resume: Hijri stays `07 Jumada al-Ula 1448`, Fajr displays `02:03` instead of `05:03`, and no bridge warning remains. The actual compass bridge returns heading `0` on the emulator. Original timezone restored.
- Native alias/incoming retention: actual Android `Singapore` versus WebView `Asia/Singapore` is observed. The native-renewed `native:Singapore:` revision and entire schedule/calculation remain unchanged across JS startup and a real cold process restart; no unnecessary month serialization occurs. An actual bridge call with a first previous-Isha shifted by 1 second rejects before saving and leaves the acknowledged native pair/revision intact. Original timezone restored.
- Hosted clean-source gate: PR `https://github.com/eval19/muajjin/pull/2` merged as `b8c1ed51a0562cd80657b292067b6593c495717d`. Final PR gate `39115572436` and release build `39115897428` passed both web modes and native GitHub checks. Only approved source/tests/docs are committed; unrelated files and the original user's checkout/index remain intact. The additional failed-commit test is post-release verification only; it does not change the APK's production source.
- Published release: `https://github.com/eval19/muajjin/releases/tag/v2.8.4`; direct APK `https://github.com/eval19/muajjin/releases/download/v2.8.4/muajjin-2.8.4-284-github-release.apk`. GitHub compiled the exact merged source in GitHub mode with `SOURCE_DATE_EPOCH=1792316095`; signing used the verified original key locally, never an uploaded key/password or replacement identity.
- Final identity: original application ID, version **2.8.4 / 284**, minSdk24/targetSdk36; **4,253,963 bytes**, SHA-256 `6954c87dcde6444b821ece3b421abcf125922fb76c0244a1f7ae3570b536b3a7`. Original signer remains `78f326588a8b17fb03910e1062341113bf5cdb224ac2a36da0116b6c7faf5f8e`. The exact signed APK successfully installs over original-signer **2.8.2 / 282** with `adb install -r`, launches and remains running without a fatal app stack; its public navigation surface was inspected without exposing existing private app fields. No original-package uninstall or data clear was performed.
- Delivery verification: uploaded draft asset size/digest and authenticated download round-trip match, then anonymous public-download bytes/signature match after publication. `v2.8.4` points to the exact compiled source; actual `fetchLatestRelease()` discovers 2.8.4 and its signed APK URL and recognizes an upgrade from 2.8.3.
- Cleanup: both owned tabs and synthetic origins are cleared; previous valid Edge attachment restored; owned servers, debug forwarding and read-only emulator stopped. All **1,316** recorded pre-existing generated files verify byte-for-byte after restoring **1,296** changed/missing files. The task dist, baseline worktree/104 Vite-cache files, four proven task daemon logs and the **1,323-file** verification workspace are removed. The idle task-created ADB server was stopped only after observing no other connected devices, releasing its locked QA log. Final signed APK/checksum and prior release remain; original index, unrelated-file hashes and AVD configuration match the recorded baseline. Existing working dependency/toolchain caches are retained.
- Separate hosting limit: release-triggered Pages deployment `39116750125` fails and the repository Pages endpoint returns 404/not configured. APK compilation/publication succeeds independently. No Pages setting was changed.
- Advisory-audit limit: pinned Bun1.2.2 reports `Script not found "audit"`; the OMP-embedded newer Bun is not a standalone CLI and no standalone Bun is on PATH. No alternate manager/lockfile, installation, forced remediation or dependency-security pass is claimed.
- Physical/platform limits: no physical Samsung S22 Ultra, Android15 runtime image, extended OEM/Doze/battery/overnight or full TalkBack matrix. Ordinary browser offline cold-load caching is still not guaranteed. These are verification limits, not unfinished calculator/import/widget implementations.

## APK installation diagnosis and signed GitHub builds — 2.8.3

- Approved scope: `.github/workflows/release-apk.yml`, `package.json`, `android/app/build.gradle`, `README.md`, this evidence record and local build/signing verification. No dependency/tool installation, production-key use/upload, commit, push, publication or device changes.
- REPRODUCED: the existing `muajjin-2.8.2-282-github-release-unsigned.apk` fails SDK 36 apksigner verification with `DOES NOT VERIFY` / `Missing META-INF/MANIFEST.MF`. AAPT successfully parses its package/manifest; minSdk is 24 and targetSdk is 36. ZIP alignment verification with `-P 16` passes. The unsigned release pipeline is a concrete installation blocker, not evidence of an application startup crash. The screenshot's exact APK bytes were not supplied, so correspondence to this artifact remains unconfirmed.
- Implementation: manual `sign_release` requests use GitHub Secrets, verify API 24–36 signatures and the independently pinned original certificate, then upload only the signed APK and metadata. Missing secrets, signing failure and a wrong certificate cannot fall back to an unsigned upload. Tag/default builds stay explicitly labelled unsigned/non-installable; the controlled unsigned reproducibility script is unchanged. No automatic GitHub Release publication was added.
- Version: package/native 2.8.3 / versionCode 283; original application ID, minSdk and targetSdk remain unchanged. The build-identity resolver confirms the matching versioned APK path.
- REPRODUCED: `bun run validate` passes zero-warning lint, both TypeScript projects, 299 Bun tests (0 failures, 783 expectations), and both web build modes. Existing capacitor.js module, stale Browserslist and large-chunk build warnings remain; no warning was suppressed.
- REPRODUCED: `python -B tests/reproducibility_test.py` passes all 12 tests. All three workflow YAML documents and 22 embedded shell scripts parse; job dependencies resolve. No hosted Actions run is claimed.
- Runtime signing smoke: executed the workflow signing body against the existing unsigned APK with the already-present Android debug keystore, solely as a verification copy. Windows executable spelling was adapted for Java/apksigner, zipalign.exe and Python; no replacement production key was generated. Real signing, pinned-certificate validation, alignment and API 35/36 signature checks pass. Both absent/empty signing secrets reject before signing; an incorrect certificate rejects without an artifact output. The EXIT trap removes temporary decoded signing keys after both success and failure.
- REPRODUCED: GitHub web assets were synced, then the existing JDK 21/SDK 36 toolchain ran `:app:testDebugUnitTest :app:assembleRelease -PbuildMode=github -PsourceDateEpoch=1792017859 -PsignRelease=false --offline --no-daemon --stacktrace`. Gradle succeeded (360 actionable tasks: 58 executed, 19 cached, 283 up-to-date); fresh XML reports show 22 timeline + 5 engine-parity tests, zero failures/errors/skips. Existing flatDir, SDK XML-version and Gradle-deprecation warnings remain.
- Fresh 2.8.3 APK smoke — REPRODUCED: AAPT confirms the original package, version 2.8.3/283, minSdk 24 and targetSdk 36. ZIP integrity passes; embedded build-info is GitHub mode/sourceDateEpoch `1792017859`. A verification-only copy passed the workflow's real signing/pinned-debug-certificate/alignment path and independent API 35 and 36 signature checks. Unsigned build SHA-256 was `271b1d1fa3766c0985a155eaa989a703b16b38536831d1e91f4e38221713d2bc`; this is not an original-release identity or reproducibility claim.
- Release prerequisite: `android/local.properties` is absent. A local release keystore exists, but its password and provenance as the original installed-app signer are not established. It was not opened or used. Production signing, GitHub secret provisioning and actual S22 Ultra installation remain unverified and separately authorized; verification-only debug-signed copies are not release APKs.
- Cleanup — REPRODUCED: restored 1,247 changed pre-existing generated files from the in-memory pre-build snapshot and verified their bytes; removed six newly created Gradle output files, verification-only APKs/metadata, generated dist and one new stopped-daemon log. The signing scratch directory and dist are absent. Ordinary build/cache updates remain. Gradle packaging replaced the previous generated 2.8.2 unsigned release APK; those original APK bytes were not retained. No production-signed APK is delivered or left behind.

## Current behavioral QA correction — Step 6 / R09 native unit effectiveness

The approved native-unit portion is complete. This step changes the safety checks, not production application behavior. Full R09 and F01/F06/F07 runtime acceptance remain open for their named browser/device/persistence/restart gates.

- Scope: only `android/app/src/test/java/org/muslimtechnician/muajjin/PrayerTimelineTest.java` and these three task records are permanent edits. Approved temporary init/JVM helpers, isolated project/build outputs, classpath export and fault-control classes were confined to `.tools/qa-native-regressions/`. No production Java edits, dependency installs/download commands, Capacitor sync, APK/AAB packaging, signing/publication, emulator/device operations, native preferences or system clock/timezone changes.
- Existing toolchain — REPRODUCED: Temurin OpenJDK **21.0.12.1+1-LTS**, cached Gradle wrapper **8.14.3**, Android SDK/build-tools **36/36.0.0**, JUnit **4.13.2**. Wrapper distribution/checksum configuration was inspected; no replacement toolchain was installed.
- Isolation: process-local JAVA_HOME targeted `.tools/jdk-21.0.12.1+1`; ANDROID_HOME/ANDROID_SDK_ROOT targeted `.tools/android-sdk`; ANDROID_USER_HOME used `.tools/android-user`; GRADLE_USER_HOME reused `.tools/gradle-cache`. A temporary init set each project's build directory under the QA root before evaluation, exported the actual unit-test runtime classpath and guarded the task graph against escaped output directories, app packaging/signing/install and connected-device tasks. The project cache was redirected too; existing Android/module build directories were not cleaned or replaced.

Both native runs used the same arguments below, from `android/`, through Python subprocess. `<repo>` denotes the repository's absolute path and `<qa>` is `<repo>/.tools/qa-native-regressions`. These are recorded inputs, not retained helper assets: the temporary init/classpath/helpers were removed after verification.

```text
<repo>/android/gradlew.bat :app:testDebugUnitTest
  --offline --rerun-tasks --no-build-cache --no-configuration-cache --no-daemon
  --project-cache-dir <qa>/project-cache
  --init-script <qa>/qa-build.init.gradle
  -PmuajjinQaRoot=<qa> -PsignRelease=false -PbuildMode=nongithub
```

| Fresh native suite — REPRODUCED | Timeline tests | Parity tests | Failures/errors/skips | Actionable Gradle tasks | Observed subprocess duration |
| --- | ---: | ---: | --- | --- | --- |
| Before test correction | 22 | 5 | 0 / 0 / 0 | 106 executed | 64.75 s |
| After test correction | 22 | 5 | 0 / 0 / 0 | 106 executed | 22.50 s |

- Freshness: final `:app:testDebugUnitTest` was executed, not UP-TO-DATE/FROM-CACHE. The final XML timestamps were 2026-10-16T12:00:16.151Z for NativeEngineParityTest and 2026-10-16T12:00:16.866Z for PrayerTimelineTest; both XML files were inspected with empty system-error output. No tests were filtered/disabled in either full Gradle run. No-action lifecycle tasks may still be labelled UP-TO-DATE; that is not claimed as test execution. Duration differences are not app performance or battery measurements.
- Independent parity: the unchanged committed corpus contains **5712 prayer rows**, including **792 unresolved astronomical references**, and **32226 Hijri rows**, plus metadata. The five parity tests exercised the complete corpus, method/madhab isolation and explicit calendar/timezone behavior. These fixture rows are not counted as extra JUnit tests.
- Real-engine baseline — REPRODUCED: a separate JVM program called production `PrayerSchedule.renew` and `NativePrayerCalculator`, not a mock. Makkah 21.3891/39.8579, method 4, madhab 0, UTC, October 15 seed produced an accepted October 13-November 11 horizon. For latitude 69, longitude 18.96, method 1, madhab 0, UTC, May 17-20 returned positive chronological events; May 21-23 raised IllegalStateException. The May 20 renewal seed also raised IllegalStateException instead of returning a fabricated horizon.
- Numeric evidence boundary: the May polar probe establishes supported-prefix/failure behavior of the pinned production engine, not a separate golden-value comparison for those particular prayer dates. Absolute numeric parity is supplied by the established independent committed matrix; the four matching May-date corpus rows found during inspection were Hijri records, not polar prayer references.
- RED / ineffective tests — REPRODUCED: temporary JVM controls used the JDK compiler AST to replace method bodies in memory and compile only QA-directory classes. Isolated classloaders loaded those controls before application classes. With the original compiled tests, an immediate polar IllegalStateException incorrectly passed the intermediate-collapse test, and an immediate schedule-constructor IllegalArgumentException incorrectly passed both outer-validation tests. Three false passes were observed. Removing outer astronomical validation correctly failed the two old coarse-shift tests; this existing protection was preserved.
- Correction: polar failures are asserted around the intended calls with IllegalStateException, not method-wide `expected=Exception`. The intermediate case first calculates actual May 17-20 events, checks positivity/chronology and the previous-Isha/next-Fajr bounds for May 18/19, then checks May 21's explicit failure and renewal rejection. No synthetic substitute times or exception-message assertion was introduced.
- Correction: each outer-edge test validates its accepted engine-derived baseline, changes only first previousIsha or final nextFajr by signed 60000/3600000 milliseconds, constructs the candidate outside assertThrows, then asserts rejection only by `validateOuterBoundaries`. The positive baseline also proves actual Fajr/current and Sunrise/next timeline behavior rather than merely not throwing.

| Isolated fault control — REPRODUCED | Original tests | Corrected tests |
| --- | --- | --- |
| Early polar calculation failure | Incorrect pass | Fails with injected IllegalStateException before the required valid prefix |
| Early schedule-constructor failure, each outer-edge case | Two incorrect passes | Both fail with injected IllegalArgumentException before the intended validator |
| Removed outer-boundary validator, each outer-edge case | Both fail | Both fail with assertion errors because rejection is missing |

- Final control runner checked exactly one selected JUnit method per control, required the intended exception category and rejected unrelated failure types; all five injected faults were detected. Intentional fault-control failures are not failures of the normal production suite.
- Separate final JVM smoke — REPRODUCED: eight real candidates, both outer edges and signed one-minute/one-hour shifts, successfully constructed and were then rejected by astronomical validation. The valid real-engine horizon, supported polar prefix, first unresolved date and no-horizon renewal failure were also exercised outside JUnit.
- JVM commands: `<jdk>/bin/java.exe --class-path <exported unit runtime classpath> <qa>/NativeRegressionControl.java <repo> <qa> false` reproduced the old false passes; the final corrected/typed control used `true`. `<jdk>/bin/java.exe --class-path <exported unit runtime classpath> <qa>/NativeRenewalSmoke.java` exercised production behavior. All completed with exit 0 and no stderr. Control variants never modified repository production sources or shipped artifacts.
- Warnings retained: two existing flatDir metadata warnings, Gradle deprecated-feature compatibility notice for Gradle 9, and Java unchecked/deprecated-API compilation notes, including the canonical geolocation plugin. No warning was suppressed or dependency/build configuration changed. Web lint/type/build gates were not rerun in this Java-test-only step; their earlier results remain scoped to their recorded runs.
- Cleanup — REPRODUCED: the QA directory measured 10978320 bytes during verification, excluding ordinary shared tool-cache updates. The two new daemon logs were proved task-owned by their QA report paths; both daemon PIDs were absent before deletion. The complete task-owned QA directory and those two logs were removed. Exact path queries confirmed all three targets absent. Existing native build outputs, toolchains, normal Gradle caches, profiles, user data and unrelated logs were preserved.
- UNVERIFIED: actual plugin incoming/persisted schedule/configuration acceptance, store/coordinator retention after partial renewal failure, revision repair/failed commit acknowledgment, genuine WebView/JS process recreation, native alias identity, permission/alarm/widget delivery and device lifecycle. JVM boundary validation and an in-memory exception are not durable-preference or Android runtime proof. R09 remains unchecked for these gates.
- Rollback: reverse only this step's test/task-record edits if required. No production schema or data migration is involved; reverting tests restores the reproduced false-pass risk. No temporary helper, mutant, compiled output or QA configuration remains as production scaffolding.

## Historical behavioral QA correction — Step 5 / F08 adjustments

F08 is complete within the approved utility/browser scope: source correction, failure/recovery regressions, local gates, actual rendered reload cases and user-assisted edit/Save/reload passed. Cleanup is verified below. F01/F06/F07 and native/release gates retain their separately named limits; this is not whole-application validation.

- Approved scope: adjustment utilities; existing `tests/storage-imports.test.ts`, `tests/prayer-timeline.test.ts`, `tests/hijri-timezone.test.ts`; these task records; local gates/builds/in-memory smoke; and one isolated localhost Edge campaign with synthetic settings, reload/repair controls and exact task-owned cleanup. Temporary helpers/screenshots were confined to `.tools/qa-adjustments/`. No installation, personal-data/device/native mutation, system clock/timezone change, signing, publication, commit or push.
- Environment — REPRODUCED: pinned Bun **1.2.2+c1708ea6a**, Node **v26.8.1**, Microsoft Edge **155.0.4283.45**, existing isolated browser-harness/CDP on port 9222. The campaign server is localhost-only on port 18082. Clock/timezone changes in probes are child-process-local.
- Failing-before normalization — REPRODUCED: `bun test tests/storage-imports.test.ts --test-name-pattern "accepted baseline|headroom-boundary|beyond safe millisecond"` yielded 1 pass, 6 failures, 20 filtered skips and 13 expectations. The accepted baseline passed; four signed 143000000000-minute cases and two exact 160000000000-minute cancellation cases were incorrectly reset to 0.
- Failing-before actual Dates — REPRODUCED: `bun test tests/prayer-timeline.test.ts --test-name-pattern "fixed reference|isolates unusable.*144000000000|two invalid fasting"` yielded 1 pass, 3 failures, 14 filtered skips and 4 expectations. The independent Makkah baseline passed; overflow remained a finite raw number and invalid timestamps entered fasting events.
- Failing-before durable browser state — REPRODUCED: a synthetic saved Suhoor adjustment of 143000000000 normalized to 0. Loading the actual Saum Settings route showed 0 and rewrote the durable field to 0; Iftar 10, QA location and the unrelated sentinel survived. This established data loss, not only a schema disagreement.
- Implementation — SOURCE-ESTABLISHED: `settings-storage.ts:adjustmentSchema` removes guessed Date headroom. Inputs must be safe integers; conversion must preserve exact milliseconds. Ordinary safe products use the existing fast numeric path; rare larger products are checked with BigInt equality. Fractional/non-finite/unsafe or precision-losing values use existing field repair while valid siblings remain intact. An exact large duration is not rejected merely because it can overflow at one reference instant: a distant opposite-signed reference can produce a valid Date.
- Implementation — SOURCE-ESTABLISHED: `time-utils.ts:derivePrayerDay` validates actual constructed Dates, reuses the primary Date objects for formatting and timestamps, and removes non-finite events before sorting. An unavailable fasting field renders `--:--`; valid prayer information and the other fasting field are preserved. No copied prayer day, guessed offset cap or alternate-location fallback was added.
- Hijri — SOURCE-ESTABLISHED/REPRODUCED: `hijri-utils.ts` needed no further source change. Its existing adjusted-Date validation and exact table range remain authoritative. Legitimate out-of-table preferences remain stored and return an explicit unavailable/error result, not an approximation or silent reset.
- Regression design: storage cases mutate one field from a fully accepted baseline and compare all siblings. Timeline cases use independent committed `makkah/2026-03-20/1/0` epochs, exact supported minutes, actual Date endpoints and one-minute overflow neighbors. Precision-adjacent integer cases distinguish exact multiplication from rounded multiplication. Existing independent Hijri fixtures remain unchanged.
- Intermediate focused run — REPRODUCED: before UTC-worker isolation, the three affected files passed 183 tests with 542 expectations. This count is historical to that test structure, not the final full-suite count.
- Full-suite failure and root cause — REPRODUCED: the first `bun test tests/` produced 336 passes, 5 failures and 982 expectations. All five failures were later timezone transitions. A separate pinned-Bun probe observed Cairo initially, UTC after assignment, and still UTC after deleting TZ, reassigning UTC and assigning Asia/Dhaka. The new tests' attempted restoration of an absent TZ froze subsequent changes in this runtime.
- Test correction: adjustment groups now run in bounded UTC child processes and never mutate/delete TZ in the shared test process. Their explicit UTC precondition is checked. Existing parent coverage still runs; worker filtering only excludes unrelated groups already executed by the parent. No application timezone workaround, test suppression or new dependency was introduced.

All commands below used the pinned Bun executable at `.tools/node_modules/@oven/bun-windows-x64/bin/bun.exe`.

| Fresh final verification — REPRODUCED | Observed result |
| --- | --- |
| `bun test tests/` | 299 parent tests passed, 0 failed, 783 expectations, 8 files, reported 12.03 s; both isolated adjustment workers are invoked by this suite. |
| Direct UTC worker audit; exact Bash command below | 34 fasting + 10 Hijri cases passed, 205 expectations, 0 failures, 16 unrelated filtered skips, reported 547 ms. The independent positive baseline actually executed. These are separate worker counts, not extra parent tests. |
| `bun run lint` | Exit 0; zero reported warnings/errors after worker correction. |
| `bun run typecheck` | Exit 0 for application and tooling projects after worker correction. |
| `bun run build` | Exit 0; 1791 modules, reported 36.54 s; metadata nongithub/sourceDateEpoch 0. |
| `bun run build:github` | Exit 0; 1796 modules, reported 16.40 s; metadata github/sourceDateEpoch 0. |
| `TZ=UTC bun --eval <in-memory production assertions>` | Exit 0; exact signed 143000000000-minute arithmetic, actual overflow isolation, valid Iftar/Asr preservation, corrupt-field-only repair, retained out-of-table preference and fixed Hijri +121 result asserted. No script file was created. |

```bash
TZ=UTC MUAJJIN_FASTING_UTC=1 MUAJJIN_HIJRI_ARITHMETIC_UTC=1 ./.tools/node_modules/@oven/bun-windows-x64/bin/bun.exe test tests/prayer-timeline.test.ts tests/hijri-timezone.test.ts --test-name-pattern "fasting adjustment arithmetic|Hijri arithmetic versus exact-table availability"
```

- Final full-suite counts do not include the 44 nested Bun arithmetic cases or the 32,226 independent calendar fixture assertions as parent tests. The existing deliberate font cleanup-denial case logged `QA cleanup denied` while passing; this was not an unhandled test failure.
- Build identity: the standard bundle excluded the GitHub releases endpoint; GitHub retained `https://api.github.com/repos/eval19/muajjin/releases/latest`. Neither contained the temporary fixture/sentinel markers. Production source did not change after these builds; subsequent corrections were test isolation only. Existing non-module capacitor.js, stale Browserslist and large-chunk warnings remain. These are not controlled reproducibility, APK, signer or publication checks.
- Actual browser method — REPRODUCED: `bun run dev -- --host 127.0.0.1 --port 18082 --strictPort`; task-owned initially empty origin, synthetic QA Makkah coordinates 21.3891/39.8579, method 1, madhab 0, 24-hour format, Jama'ah Isha 23:45 and an unrelated sentinel. Each case navigated/reloaded the actual app, waited for rendered Saum Times, inspected durable settings, compared unaffected settings/sentinel, checked prayer information and verified only valid event Dates remained. The dashboard has five Salat rows; sunrise-derived information is labelled Shuruq, not a sixth Salat row named Sunrise.

| Actual rendered-app reload cases — all REPRODUCED/PASS | Observed invariant |
| --- | --- |
| Suhoor and Iftar, each signed 143000000000 minutes | Exact saved values survive reload; actual rendered fasting clock values agree with the derived presentation. Independent numeric correctness is established by the committed CLI fixture, not a self-comparison of UI text. |
| Suhoor -121; Iftar +121; Hijri +121 | Supported non-default preferences persist without an invented small cap. |
| Null Suhoor; text Iftar; MAX_SAFE_INTEGER Hijri | Only the invalid field repairs to 0; valid siblings and sentinel remain unchanged. |
| Suhoor overflow; Iftar overflow; both overflow at 144000000000 minutes | Only unavailable fasting values show `--:--`; valid sibling and prayer information remain. |
| Hijri 100000 | Preference remains durable; the actual header reports the supported-calendar-range error while Gregorian/prayer information remains available. |

- Browser input limitation — REPRODUCED: native click/key activation initially returned without delivering DOM activation, and Windows rejected foreground activation. Trusted insertion changed the visible DOM value to 0 without establishing React form-state commitment. The user's first manual Save navigated back but left the full durable object unchanged. This automation artifact was not accepted as a passed repair or classified as an application Save defect.
- Actual user-assisted repair — REPRODUCED: after a clean navigation restored the true 144000000000 input baseline, the user manually selected the Suhoor field, typed 0 and clicked Save. Durable comparison showed exactly one changed field, Suhoor 144000000000 to 0; Iftar 10, all other settings, sentinel and key set remained unchanged. No focus emulation or programmatic Save activation was used.
- Actual page recreation — REPRODUCED: `Page.reload` with ignoreCache true retained the complete repaired object and sentinel. The actual dashboard showed Suhoor 05:02 matching Fajr 05:02 and Iftar 18:05 matching Maghrib 17:55 plus 10 minutes, without an unavailable fasting marker. DOM inspection and an inspected normally scrolled viewport showed all five Salat rows, Shuruq/prohibited information and Saum Times. Temporary full-page/viewport screenshots were removed.
- Remaining browser limits — UNVERIFIED: parts of the campaign ran while the tab was hidden. These checks do not establish foreground performance, battery savings, full accessibility, other UI workflows or Android/WebView behavior.
- A later asynchronous module-based inspection timed out; a plain synchronous DOM/storage inspection succeeded and the isolated server's fixture HTTP response was verified. This tool timeout is not classified as an application defect or successful save.
- Cleanup — REPRODUCED: the app was unmounted onto the task-owned blank page before removal. Guards compared the complete expected repaired object, sentinel, exact two-key set, ownership marker and empty database list. Only the two synthetic localStorage keys and ownership marker were removed; readback returned empty keys/databases and marker removal true. The owned tab was closed, and a targeted lookup reported no target with that ID. The previous attachment was restored and verified without reading private page content. The named local service stopped; Get-NetTCPConnection reported zero listeners on 18082. Exact path queries found neither `.tools/qa-adjustments/` nor dist; screenshots/helpers/generated outputs were removed. Existing runtimes, profiles, ordinary caches/history and unrelated data/files were preserved.
- Acceptance: F08 is complete for the approved utility/browser scope. Actual normalization, overflow isolation, sibling preservation, rendered failure/recovery, manual edit/Save, durable page recreation and local gates passed. Native/WebView/lifecycle/release gates and other handoff items are not established by this campaign.
- Rollback: reverse only this step's utility/test/task-record edits if required; no storage schema/data migration or dependency change is involved. Reversal reintroduces valid-adjustment loss and invalid fasting-event handling, so clearing stored data is not an appropriate rollback.

## Historical behavioral QA correction — Step 4 / F06/F07 JS boundary

The approved JS correction and local verification were complete for Step 4. Full F06/F07 acceptance remained open for the named Android/WebView gates below. Step 5 above subsequently completed F08's approved utility/browser scope. This entry is not whole-application validation or release approval.

- Approved mutation scope: `src/services/widget-sync-service.ts`, `tests/widget-sync.test.ts` and these three task records. Local failure/retry tests, process-local clock/timezone simulation, in-memory production smoke, lint/types, both web builds and exact generated-output cleanup were approved. No native source/preferences, browser/device campaign, dependency installation, signing, publication, commits or pushes.
- Contract inspection — SOURCE-ESTABLISHED: `NativeScheduleStore.epoch` accepts positive integral milliseconds no greater than 8.64e15. `PrayerSchedule.Calculation` preserves methods 1/2/3/4/5/7/8/9/10/11/12/15 and madhabs 0/1, finite latitude [-90,90] and longitude [-180,180]. Plugin status includes schema 2, engine/policy, timezone, calculation inputs, bounds and durable revision only after native checks. Native semantic horizon validation remains native-owned and unchanged; JS metadata checks are not astronomical validation of a stored 30-row pair.
- Interface correction: `getScheduleStatus(): Promise<unknown>` replaces its trust-shaped optional-field return type. Zod validates strict exists=true, supported schema/engine/policy, finite ranged coordinates, supported integral method/madhab, nonempty bounded revision/timezone strings, positive representable integral epochs and ordered bounds. Matching inputs and current proactive coverage are still required. The unused ScheduleStatus interface was removed, with no compatibility alias or wire-schema change. Reference search found the service and its existing tests; no language server is configured.
- Alias correction: native timezone strings are resolved through `Intl.DateTimeFormat('en-US', { timeZone }).resolvedOptions().timeZone`. Unknown/unsupported names fail into the existing legitimate repair path. This uses IANA identity, not equal current offsets, a network resolver or coordinates to infer another timezone.
- Async/resource correction: status success, rejection and parse failure all refresh current time/timezone in finally. The existing five-second deadline is cancelled when its query finishes. Successful schedule/presentation acknowledgments queue a re-entry through the existing serial latest-intent drain before completion; an already pending newer request is preserved rather than replaced.
- Failing-before — REPRODUCED: `bun test tests/widget-sync.test.ts --test-name-pattern "otherwise reusable native status|engine-derived native horizon"` produced 2 passes, 3 failures and 8 filtered skips. The engine-derived baseline passed. An end epoch of 8,650,000,000,000,000, start=-1 and exists='true' each incorrectly prevented repair. Missing revision alone already required repair; its baseline now includes valid schema and other required fields.
- Failing-before alias work — REPRODUCED: after clearing completed deadline timers, `--test-name-pattern "real engine work"` produced 6 passes and 3 failures. Across three fresh synchronizers, each of `UTC` / `Etc/UTC`, `America/New_York` / `US/Eastern` and `Asia/Calcutta` / `Asia/Kolkata` incorrectly caused 96 real factory calls and three schedule transfers. Supported retained UTC, different-zone controls, ordinary web, stable ticks and the same-instance retry controls passed.
- Failing-before asynchronous boundaries — REPRODUCED: `--test-name-pattern "reconsiders"` produced 1 pass and 6 failures. Available status correctly refreshed time, but failed/malformed status used an obsolete October start after a jump to November. Schedule and presentation acknowledgments did not repair after clock/timezone changes. These paths now pass without changing the system clock/timezone.
- Intermediate failures were not accepted as passes: the first isolated work probes hit the Bun runner's five-second limit because completed production status calls left their deadline timers alive; eight probe cases timed out and emitted runner errors. Production timer ownership was corrected, not bypassed with process.exit or a longer production timeout. Isolated child probes now have a three-second termination bound. A later broad test command timed out at 45 seconds because an empty-array test.each row was interpreted as an argument list; wrapping every raw status in a one-argument tuple fixed the harness without changing production behavior.
- Regression effectiveness: accepted status fixtures originate from the real pinned engine's full schedule builder before field mutation. Missing/empty revision tests now change only revision; missing schema is a separate row. The suite covers top-level wrong shapes, missing/wrong/non-finite/fractional/unsafe/unrepresentable/non-positive epochs, reversed/expired/not-yet-covered/threshold coverage, strict discriminators, string bounds, coordinate and method/madhab corruption, zero/negative coordinates, every supported method with both madhabs, negative acknowledgments, same-instance retry and newer intent during all three bridge phases.
- Scalar endpoint cases establish only the native metadata numeric contract, not that native astronomy can generate a horizon at year 275760 or from epoch 1. Full stored-horizon semantic validation remains a native runtime gate.
- Test isolation: the parent test restores its original process TZ (including original absence). Instrumented CalculationMethod.Karachi is replaced only in isolated child processes before production imports, delegates to the original real factory and cannot leak into the suite. Dynamic imports are deliberately limited to that instrumentation/import-order seam. No application storage or Android process is involved.
- Fresh final command — REPRODUCED: pinned Bun 1.2.2 (c1708ea6), `bun test tests/widget-sync.test.ts tests/prayer-timeline.test.ts`, passed **175 tests, 0 failures, 557 expect calls**, across two files, in the final run's reported 2.14 seconds. No tests were filtered or disabled in that final run. This is 145 synchronizer cases plus the existing 30 timeline cases, not the full Bun suite.
- Independent work measurement: each child first proves a 32-call positive calculation baseline with the real builder, then resets the counter before the observed operation. The table counts actual method-factory calls, status requests, schedule transfers and presentation transfers separately; the bridge transfer flag is not used as a calculation counter.

| Controlled scenario | Engine factory calls | Status queries | Schedule transfers | Presentation transfers |
| --- | ---: | ---: | ---: | ---: |
| Three fresh synchronizers, each supported alias pair | 0 | 3 | 0 | 3 |
| Three fresh synchronizers, each genuinely different-zone control | 96 | 3 | 3 | 3 |
| Ordinary web, one call | 0 | 0 | 0 | 0 |
| Twenty warm ticks plus one presentation edit, delta after initialization | 0 | 0 | 0 | 1 |
| First failed status query plus failed fallback | 32 | 1 | 1 | 0 |
| Same synchronizer's recovery call, delta after that failure | 0 | 1 | 0 | 1 |
| Unrepresentable end epoch, one fresh synchronizer | 32 | 1 | 1 | 1 |

- Probe preconditions: supported pairs were `UTC` / `Etc/UTC`, `America/New_York` / `US/Eastern` and `Asia/Calcutta` / `Asia/Kolkata`. Different-zone controls were `UTC` / `Africa/Abidjan` and `America/Phoenix` / `America/Los_Angeles`. Each pair's current long-offset labels were independently checked equal at the fixed instant, so the controls genuinely reject equal-offset-but-different identities/rules. The retained engine-derived horizon was anchored differently from the current request. These are bounded code-path work counts, not CPU/startup/battery measurements or Android bridge timings.
- Separate production smoke — REPRODUCED: `subprocess.run([bun, '--eval', step4_smoke_source], ..., env={...,'TZ':'UTC'})` exited 0. The in-memory program imported production functions after wrapping the real factory, exercised supported alias reuse, corrupt-epoch repair and the mandatory failed-status/failed-fallback/recovered-status sequence on the SAME synchronizer. Assertions checked actual counts and generated schedule configuration/30-row output. No throwaway script file was created.
- Religious-time smoke — REPRODUCED in that same program: UTC device policy, latitude 40.7, longitude -74, method 1, madhab 0. At 2026-03-20T00:00:00.000Z and 00:38:59.999Z, current was Maghrib and next was Isha at 00:39Z. Exactly at 00:39:00.000Z and at 00:39:00.001Z, current was Isha and next was Fajr at 09:28Z. Fixed expected epochs were asserted, rather than echoed from a mocked calculation.
- Fresh quality gates — REPRODUCED: final `bun run lint` exited 0 without warnings/errors; final `bun run typecheck` exited 0 for application and tooling projects. The last test edit removed an incidental exact-error-key assertion while retaining rejection/retry/state checks; the affected tests and lint/types were rerun after it.
- Fresh builds — REPRODUCED: `bun run build` and `bun run build:github` exited 0 with 1791/1796 transformed modules respectively. Standard metadata was nongithub and GitHub metadata github, both sourceDateEpoch 0. Standard bundle search found no GitHub releases endpoint; GitHub retained the intended endpoint. Neither contained the MUAJJIN_QA_MODE/MUAJJIN_QA_ZONE/Work-probe markers. Existing non-module capacitor.js, stale Browserslist and large-chunk warnings remain. No reproducibility, pristine-source, signer, APK or publication claim.
- Cleanup — REPRODUCED: dist was absent before this step's builds, inspected in both modes, then removed; a final path query found no dist directory. Tests/smoke were in-memory or isolated processes and created no task helper files or stored-data fixtures. Existing dependencies, browser profiles, native preferences, normal tooling caches and unrelated user data/files were preserved.
- UNVERIFIED: actual Android/WebView alias pairs and JS/process recreation with retained native preferences; native stored schedule/configuration outer-edge corruption; missing-revision durable repair with otherwise identical bytes; failed revision persistence; native XML freshness/unit suite; whole Bun suite and integrated device/runtime gates. No native build, permission/clock change, browser UI campaign, signing or publication occurred in Step 4.
- Acceptance: JS corrections and their approved local failure/recovery/work-count checks are acceptable. F06/F07 full checkboxes remain open for the native gates, R09 still has native effectiveness/evidence work, and R10/full-handoff closure still depends on F08 and remaining runtime acceptance. No whole-app validation verdict is granted.
- Rollback: reverse only this step's JS/test/task-record edits. No native data or schema migration is required; reverting reintroduces the reproduced metadata, alias and asynchronous-reconciliation defects.


## Current behavioral QA correction — F02/F04/F05

The coordinated font-ownership slice is corrected and its approved browser/local checks passed. This entry supersedes the open-font status recorded at the end of the earlier F01 step; it does not close F06/F07/F08 or whole-application acceptance.

- Approved targets: `src/contexts/translation-context.tsx`, `tests/storage-imports.test.ts`, new `tests/browser/font-ownership.html` and `tests/browser/font-ownership-checks.jsx`, and these three task records. Temporary helpers/font/screenshot under `.tools/qa-font-ownership/`, local gates/build outputs, one localhost-only server on port 18081 and one task-owned Edge tab/origin were separately approved. No source/database schema migration, dependency installation, native/device mutation, system clock/timezone change, signing, publication, commit or push.
- Environment: pinned Bun 1.2.2 (c1708ea6), Node v26.8.1, Vite 7.3.1, Microsoft Edge 155.0.4283.45. The existing isolated browser-harness and preserved `ensure_visible_edge()` helper were used. Process/window inspection found Microsoft Edge on the Default input desktop, visible, non-minimized and not cloaked. No unrelated window titles, authentication state or history were printed.
- Fixture: converted the existing licensed Ubuntu font with already-installed fontTools and Node Brotli, without installing packages. The valid WOFF2 was 114,824 bytes and loaded through the real FontFace API; this size is evidence, not a validation constant. The test origin started with empty localStorage and IndexedDB. A distinct unrelated font sentinel was added and checked throughout.
- Failing-before — REPRODUCED: `muajjinFontQA.run('postCommitFailure')` committed a new pointer, then an injected `FontFaceSet.add` failure led the old provider to delete that selected record. The previous and sentinel records remained. `compoundFailure` retained a cleanup-denied candidate but exposed only the injected pointer failure. `retiredUploadFailure` and `retiredRemovalFailure` each deferred old retirement until a newer upload succeeded, then observed the older cleanup error replacing the newer null storageError.
- Implementation: mark commitment immediately after successful pointer persistence, before activation/state/reporting. Upload cleanup excludes committed candidates and failed uploads return false. The existing reporter can combine original and cleanup messages once, while its request token gates only shared operation-status mutation; obsolete cleanup warnings remain observable through the existing bounded toast channel. Both retirement callers pass their initiating token. Their success toast is emitted before retirement starts, so synchronous pointer-read denial cannot be overwritten by a success toast. Denied pointer reads preserve potentially selected records.
- Regression quality: both existing Map-backed binary tests now establish a passing verified-write baseline before introducing a single readback/cleanup fault, inspect retained/deleted records, preserve a previous record, exercise retry, use deterministic IDs/timestamps and restore the original IndexedDB property descriptor (or original absence). These are lower-level tests, not browser transaction proof. Provider failure coverage is retained in the permanent browser harness.
- Fresh Bun command — REPRODUCED: `bun test tests/storage-imports.test.ts` passed **21 tests, 0 failures, 131 expect calls**, across one file. The deliberate cleanup-denial test emitted its existing expected console error. An earlier invocation included a nonexistent second filter and still ran only this one file; no extra sync-suite execution is claimed.
- Fresh quality gates — REPRODUCED: `bun run lint` passed with zero reported warnings/errors; `bun run typecheck` passed application and tooling projects. A separate in-memory `node --input-type=module --eval` ESLint API program selected `tests/browser/font-ownership-checks.jsx`, used the installed TypeScript parser, browser globals, recommended JS/React Hooks rules and TypeScript unused-variable rule, and returned 0 errors/0 warnings. Ordinary repository lint does not select JSX, and this browser page is not a Bun-discovered automated test.
- Intermediate failures were corrected, not hidden: initial typechecking rejected AggregateError because the existing target libraries do not declare it; final code composes both messages without changing project compatibility. An initial combined CDP evaluation timed out, and the migration harness's React-only wait missed a durable pointer change that did not cause another render. The harness now observes the actual successful pointer write as well. Final gates and the entire browser matrix were rerun after these corrections.
- Fresh browser command: `bun run dev -- --host 127.0.0.1 --port 18081 --strictPort`, using the pinned Bun binary, then `http://127.0.0.1:18081/tests/browser/font-ownership.html`. The page mounts the actual provider; fault wrappers still use real browser storage transactions. A valid local File was supplied programmatically to `muajjinFontQA.setFile`, then each named case was awaited through `muajjinFontQA.run(name)`.

| Actual browser cases — all REPRODUCED/PASS | Observed invariant |
| --- | --- |
| baseline | Valid binary, durable pointer, loaded selected font and unrelated sentinel established. |
| readbackDenied; readbackMismatch; pointerDenied | Failure returns false; previous pointer/records remain; definitely uncommitted candidate is removed. |
| postCommitFailure; postCommitReportingFailure | Later activation or success-report lookup failure returns false without deleting the committed binary; provider remount loads the committed selection. |
| compoundFailure; migrationCompoundFailure | Both injected original and cleanup failures remain observable; cleanup-denied candidate is retained rather than silently lost. |
| pointerReadDenied | Potentially selected and previous records remain; cleanup limitation is observable rather than hidden by success reporting. |
| staleCandidateFailure | Obsolete cleanup failure remains observable without changing newer pointer, selection or null operation-error state. |
| retiredUploadFailure; retiredRemovalFailure | Deferred old retirement failure cannot replace newer selection/status; its warning remains observable and protected records survive. |
| migrationPointerFailure | Two failed migrations preserve exact legacy bytes and previous records, with no extra candidates when deletion works; later retry commits a reloadable binary. |
| obsoleteMigration | A real committed-write completion is held while a newer upload commits; after actual IDB work settles, the obsolete migration cannot replace newer state and its candidate is absent. |

- Final browser result: **14 cases passed, 0 failed**, with no observed unhandled rejections. These were provider/API-driven checks, not a certification of file-chooser/keyboard/touch interactions. A rendered harness screenshot was inspected; some campaign execution occurred with the tab hidden, so this is not a foreground-visibility or performance claim.
- Actual page recreation — REPRODUCED: CDP `Page.reload` with `ignoreCache: true` retained pointer/selected ID `97b7e5ed-cf70-4bde-8e0c-f28bf34c75a6`; the binary and sentinel were still present, storageError was null, and the actual root font family was `MuajjinCustomFont-1, system-ui, -apple-system, sans-serif`. This is browser-page recreation, not Android process/WebView recreation.
- Fresh builds — REPRODUCED: `bun run build` and `bun run build:github` exited 0. Standard metadata was `nongithub`, GitHub metadata was `github`, both sourceDateEpoch 0. The standard bundle had no GitHub releases endpoint; the GitHub bundle retained the intended endpoint. Neither bundle contained the browser harness/sentinel markers. Existing non-module capacitor.js, stale Browserslist and large-chunk warnings remain; development startup also warned about missing SOURCE_DATE_EPOCH. No controlled reproducibility/signature/APK claim.
- Cleanup — REPRODUCED: `muajjinFontQA.cleanup()` returned empty localStorage keys and an empty IndexedDB database list for the initially empty task origin. Only that origin's font pointer/database and session ownership marker were removed. The owned tab was closed, prior attachment restored, and the task server stopped; port 18081 had zero listening sockets. The four inspected task-owned temporary files and generated `dist/` were removed; a final path query found neither `.tools/qa-font-ownership` nor `dist`. Normal browser/tooling caches/history, existing profiles, unrelated records/files and runtimes were preserved.
- Remaining limits: actual settings import/removal chooser and accessibility surfaces, Android WebView/storage/lifecycle behavior, full Bun/native integration and F01 timezone-change resume remain UNVERIFIED. F06/F07/F08 and remaining R09/R10 work still require their separately described approval scopes. The permanent browser regressions require a fresh approved localhost origin and a valid licensed font fixture; they do not install tooling or silently run against personal data.
- Rollback: reverse only this provider/regression/task-record slice if required; no schema/data migration is necessary. Reversal reintroduces the reproduced data-loss/reporting defects, so existing durable font records must not be deleted as a rollback shortcut.


## Current behavioral QA correction — F01

F01 implementation and local regressions are corrected; actual browser/device timezone-change resume remains UNVERIFIED. F03's separately verified artifact-only correction is recorded in the following entry. Earlier implementation counts below are historical.

- Approved scope: `src/utils/hijri-utils.ts`, new `tests/hijri-timezone.test.ts`, these three existing task records, local runtime/tests/lint/type checks, both web builds and exact generated-output cleanup. No downloads, browser/device changes, signing, publication, commits or pushes.
- Tooling: the previously absent pinned Bun path became available and `bun.exe --version` reported 1.2.2. This session did not install it. Nested scripts used process-local PATH only. No language server is configured; references to `calculateHijriDate` were traced in the dashboard date component and existing tests. Public signatures are unchanged.
- Failing-before — REPRODUCED: `bun test tests/hijri-timezone.test.ts --test-name-pattern 'Africa/Cairo to UTC'` failed at the intended independent comparison: adjustment -121 returned day 29 instead of day 28 after the retained module changed timezone. The focused selection reported 1 failure and 1 filtered skip; no full-suite result was claimed.
- Boundary failure — REPRODUCED: a separate focused regression, `--test-name-pattern 'years below 100'`, failed because 0001-01-01 was accepted instead of throwing RangeError. `Date.UTC`'s 0–99 year reinterpretation could convert an unsupported ancient date into an apparently supported twentieth-century date.
- Implementation: prepare 184 stable year-start boundaries once from the existing exact numeric year lengths, anchored at Gregorian 1900-04-30 / Hijri 1318-01-01. Binary search no longer reads timezone-sensitive upstream Gregorian Date objects. Civil ordinal conversion uses `setUTCFullYear` on the existing private Date copy; it does not remap years 0–99. Invalid adjusted Dates/ordinals are rejected. No dependency, table approximation, storage migration, location-timezone policy or native engine change.
- Fresh passing regressions — REPRODUCED: `bun test tests/hijri-timezone.test.ts tests/prayer-timeline.test.ts` passed 47 tests, 0 failures, 179 Bun expect calls. Child-process assertions additionally exercised all 32,226 committed independent Hijri fixtures across 13 target timezones, retained upstream-before-module import order, both demonstrated westward changes, supported +/-120/121 adjustments and ancient out-of-table dates. Fixture generation/data were not modified.
- Fresh checks — REPRODUCED: `bun run lint` passed without reported errors/warnings; `bun run typecheck` passed application and tooling projects. Final `bun run build` and `bun run build:github` passed after the ancient-date correction. Earlier successful checks/builds preceded that extra boundary correction and were rerun because source changed, not as reassurance.
- Build inspection: standard metadata was `nongithub`, GitHub metadata was `github`, both epoch 0. The standard bundle had no updater release endpoint; the GitHub bundle retained its intended endpoint. Missing SOURCE_DATE_EPOCH, non-module capacitor.js, stale Browserslist data and large-chunk warnings remain. No controlled reproducibility or current APK claim.
- Actual production-function smoke — REPRODUCED: `bun --eval` in an isolated Cairo-initialized process kept 2026-03-20 at 01-10-1447 after changing that process to UTC. For physical instant 2026-03-20T23:30:00Z, UTC gave 01-10-1447 and Kiritimati gave 02-10-1447, preserving device civil-date policy. Input Date epoch stayed unchanged. Exact bounds returned 01-01-1318 and 30-12-1500; years 1/99 rejected as outside the table; MAX_SAFE_INTEGER adjustment rejected as invalid arithmetic. System/device timezone and clock were untouched.
- Limits: no actual browser/WebView resume, device timezone change, native consumer parity or full Bun/native integration suite was executed in this local-only step. Other F02/F04/F05/F06/F07/F08 defects and their required runtime gates remain open.
- Data/rollback/cleanup: no saved data changed and no migration is needed to reverse this source change, although reversal reintroduces calendar defects. The new regression file is permanent. No throwaway script files were created. `dist/` was absent before verification, regenerated only by approved builds, inspected, then removed; no existing user output or shared cache was purged. Vite's normal tooling-cache effects remain.


## Current behavioral QA correction — F03

This entry supersedes earlier artifact-only publication claims for the current workflow. Other implementation counts and runtime claims below remain historical; this step does not revalidate them.

- Starting source identity: clean commit `89bd72ec649bd367574ad0a5daf9cfd029be4b55`, application 2.8.2. The F03 changes remain uncommitted; no version change.
- Before correction — SOURCE-ESTABLISHED: `release-apk.yml:98-122` configured a tag-triggered `publish` job with `contents: write` and `softprops/action-gh-release@v2`, publishing unsigned build outputs with `draft: false`. No remote release was created to reproduce it.
- Implementation: removed that entire job only. Existing `resolve`, reusable `quality`, unsigned `release` assembly and CI artifact upload remain. No change to the other two workflows, package scripts, application logic, dependencies or signing.
- Fresh local verification — REPRODUCED: executed `subprocess.run(['node', '-e', workflow_check_source], ...)` with an in-memory assertion program and installed `yaml` 2.6.0. `YAML.parseDocument(..., { uniqueKeys: true })` accepted `.github/workflows/build-apk.yml`, `release-apk.yml` and `deploy-pages.yml`: 3 workflows, 10 jobs, exit 0, no stderr.
- Assertions: each workflow retains root `contents: read`; no inspected job grants repository contents write or `write-all`; all local `needs` dependencies exist; no inspected step invokes a GitHub Release publication action/command. Release jobs are exactly `resolve`, `quality`, `release`; tag/manual inputs, release quality dependency, GitHub artifact mode/upload and metadata path remain; web quality still includes both modes; Pages published-release/manual triggers and its existing Pages-specific deployment permissions/action remain.
- Verification limit: this executes the YAML parser and local policy assertions, not GitHub Actions or the shell commands stored inside workflow YAML. No hosted run, APK build/signature check, tag, push, signing or publication occurred.
- Scope/impact: one workflow and these three existing task records; text edits and in-memory verification only. No temporary script files, installs, downloads, device/browser fixtures, application data or generated build assets were created.
- Rollback: reverse only this reviewed correction if required; restoring unsigned automatic public publication would reintroduce the safety defect. Source rollback has no app-data migration consequence for this workflow-only step.
- At the end of the F03 step, F01/F02/F04/F05/F06/F07/F08, R09/R10 and separately approved integration/runtime gates were pending. Later F01 progress is recorded above; F03 completion is not whole-application or release-readiness sign-off.

## Scope, decisions and environment

Source specification: the Desktop `Muajjin-Improvement-Implementation-Plan.md`, T00–T17. Source changes and the local checks below are delivered; the physical-device and signed-release limits at the end remain explicit. This is an uncommitted implementation, not a published release or certification of every section-9 device configuration.

Approved: repository/native/tests/config/CI/assets/docs, local QA scripts/cleanup, isolated Bun 1.2.2 and locked dependencies, portable JDK21/SDK36/licenses, verified IndexedDB migration and one task-owned API36 emulator with QA-only permission/clock/lifecycle manipulation. No personal device, global PATH/rule mutation, commit, publication, production signing, new application dependency, endpoint change or service worker.

- D1: preserve device-timezone civil dates/clocks, not an inferred selected-city timezone.
- D2: exact compatible native adhan/Umm al-Qura ports with existing upstream attribution, not a longer cache or background WebView. See `tests/native/PROVENANCE.txt` and `android/app/src/main/assets/native-engine-licenses.txt`.
- D3: packaged Android works offline; browser offline cold-load caching remains unpromised because no service worker was added.
- Windows x64; Bun1.2.2; Node26.8.1; TypeScript5.6.3; Vite7.3.1; React18.3.1; Capacitor8; adhan4.4.3; @umalqura/core0.0.7. Android: Temurin21.0.12.1, SDK/build-tools36, Gradle8.14.3.
- Native runtime: only owned `emulator-5556`, API36 Google APIs x86_64, application `org.muslimtechnician.muajjin.debug`, airplane mode, manual fixtures. No personal-device manipulation.
- Browser: Edge155, owned localhost preview tab. Window identity was checked, but later occlusion/hidden state prevented trustworthy physical-visible input/paint timing. Real Android UiAutomation touch, Android file chooser, keyboard and Back supplied actual surface proof; browser-only DOM inspection is not represented as physical input.

## Final executed gates

| Check | Observed outcome |
| --- | --- |
| `bun run validate` | Zero-warning lint, app/tooling TypeScript, 64 Bun tests / 0 failures / 225 assertions, both web modes passed |
| Standard web build and Capacitor Android sync | Passed; actual generated assets copied before native packaging |
| `:app:testDebugUnitTest` | 19 passed: 5 native engine parity + 14 timeline cases; XML recorded zero failures/errors/skips |
| `:app:assembleDebug :app:assembleDebugAndroidTest` | Passed; final debug app and permanent runtime-test APK installed successfully on the owned emulator |
| Permanent Android runtime suite | 2 tests passed in each final light/dark run; latest 3.876s / 3.330s |
| Temporary actual UI probes | Menu/Back; Qaza increments/reload/decrements/focus; valid font import/invalid replacement/offline reload; native language/clock presentation all passed; probes removed after proof |
| Final controlled unsigned release builds | Standard twice and GitHub twice, explicit epoch1790769003; all four exited0 |
| Final strict unsigned payload comparisons | Both modes MATCH: 484 entries each, names/sizes/uncompressed SHA-256; no arbitrary payload exclusions |
| Deliberately mismatched packaging inputs | Wrong mode and wrong epoch each exited1 with the intended bundled-web guard message |

Hosted CI was configured, not executed remotely or published. Local gates cover the relevant workflow commands. Existing capacitor.js/non-module, large-chunk, old Browserslist, Gradle flatDir/deprecation and SDK-XML warnings remain disclosed; no test failure was silenced to make the gates pass. An ordinary build without SOURCE_DATE_EPOCH warns; controlled builds supply it explicitly.

The final repeated-build metadata recorded base commit `582918511dc7783e25e50810b1c4cf5d9bafcee8` and identical tracked source delta `1f110c2b0151e5abc9a0133c600334c04be178c757e7cc2168a9dbe369c91467` across all four builds. The source was intentionally dirty/untracked; the commit is not asserted to contain this implementation. Final documentation was updated after those builds. The debug-only widget test-host type was absent from both actual release DEX files.

## Per-task changes and evidence

### T00 — Executable baseline

- Recorded initial app/tooling type checks, lint failure/warnings, standard/GitHub builds, real onboarding/dashboard, resource sizes and original bridge/timer work. Original coordinate regression failed4/5 before its fix.
- Initial lint had one empty-catch error and nine fast-refresh warnings. Original standard/GitHub JS sizes are preserved in the performance ledger below.
- An original Gradle immutable-workspace rename failure persisted with a short/fresh cache. Vite was watching temporary Gradle/cache trees and later crashed; excluding task/build outputs and stopping that watcher allowed compilation without a Gradle upgrade. Watcher-lock attribution is an inference, not proven causality. Matching upstream report: https://github.com/gradle/gradle/issues/38921.
- No comparable physical CPU/battery or visible startup baseline was obtained. Hidden DOM readiness is not a substitute for paint timing.

### T01 — Meaningful gates

- `package.json`, TypeScript tooling config, `eslint.config.js`, `tests/`, Android JUnit/runtime tests and `.github/workflows/` establish lint/type/domain/both-mode/native gates.
- Regression cases cover calculation boundaries, storage failures/races, hidden clocks, bridge acknowledgments and unsafe APK ZIP/signature scope; the APK regression executes12 Python cases inside its Bun case. Template-only native tests were replaced with consumer-visible checks.
- Final 64/19/2 results are above. App-scoped Android test tasks avoid running unrelated Cordova template test projects; no relevant failed test is excluded.
- Behavior: development failures are surfaced before release. No app permission or data-schema change comes from these gates. Rollback would remove protections, not restore a required app dependency.

### T02 — Coordinates and selected methods

- `src/services/prayer-times-local.ts`, settings/location controls: finite bounds, valid zero/negative coordinates and fresh selected-method parameters. Hanafi state cannot leak into later Shafi calculations.
- Tests first reproduced zero/method faults; canonical tests cover all existing method IDs, both madhabs through independent native fixtures, invalid arguments and polar failures. Legacy15→12 remains supported.
- Behavior correction: use the requested location/method, not a zero-coordinate fallback or a previous call's parameters. Offline calculation retained; no new timezone service or network endpoint.

### T03 — Dated prayer, fasting and Hijri boundaries

- `src/utils/time-utils.ts`, `hijri-utils.ts`, `src/services/prayer-times-local.ts`: retain dated epoch instants, calculate real tomorrow Fajr, previous Isha, overnight Jama’ah and adjusted fasting/prohibited windows. DST-safe civil ordinals prevent a repeated/missing Hijri day; Maghrib rollover and adjustment apply once.
- Bun regressions exercise exact/inclusive end boundaries, sunrise gaps, tomorrow fasting targets, midnight/month/year and supported Hijri range. Independent native fixtures include leap dates, DST forward/back, skipped local dates, antimeridians, methods and both madhabs.
- Behavior correction: old copied Fajr, wrong overnight start and DST date defects change to actual values. Device-timezone policy remains D1. Unsupported astronomy/Hijri results are errors, never guesses.

### T04 — Coherent dashboard and load migration

- `src/pages/index.tsx`, `src/contexts/app-context.tsx`, `src/utils/settings-storage.ts`: one coherent derived prayer/prohibited/fasting result; migrate and validate at load, not in a remount loop. Missing persisted coordinates stay unavailable rather than mixing one saved coordinate with a default city's other coordinate.
- Actual dashboard/settings/reload and zero-coordinate Android surfaces were exercised. New storage regressions reproduce damaged coordinates and explicit unselected onboarding madhab; valid siblings, Jama’ah and preferences survive.
- Behavior correction: no unrelated city under the old label, and onboarding cannot silently complete without a madhab choice. Reset/loading defaults remain deliberate; no all-user-data reset.

### T05 — Schedule/presentation bridge separation

- `src/services/widget-sync-service.ts`, native `PrayerWidgetPlugin.java`, `NativeScheduleStore.java`: schema-2 calculation and presentation contracts; acknowledged revisions, complete invalidation, serial latest-intent delivery and retryable failed acknowledgments.
- Web never prepares a native month payload. Twenty stable native requests do not rebuild/resend it; presentation changes leave physical prayer instants unchanged. Bun race cases cover a late old result and a newer request during cache-hit completion.
- Actual Android native persisted schedules/notification/widgets and real renewal were checked. Transient transfer failure retains known-good state rather than claiming success. Downgrade must account for the new native schedule/presentation schema.

### T06 — One native snapshot per dispatch

- `PrayerUpdateCoordinator.java`, `PrayerWidgetHelper.java`, `PrayerNotificationHelper.java`: one serial worker owns schedule parsing/timeline/presentation; all widget and notification consumers reuse it. Formatters/state are shared within the dispatch; notification primitive state avoids redundant reposts.
- Native timeline regressions and real eight-instance rendering exercised the pipeline, including all seven provider identities. Review found no remaining required native lifecycle issue before final surface fixes.
- Behavior unchanged except correctness fixes. This removes redundant work; Java allocation/CPU percentages were not profiled and are not claimed.

### T07 — Consumer-aware scheduling without fake precision

- `PrayerWidgetHelper.java`, `PrayerWidgetAlarmReceiver.java`, native helpers: necessary transition RTC_WAKEUP is separate from plain RTC cosmetic refresh. No consumers cancels both; legitimate notification/widget consumers restore work. Resume/boot/update/timezone/time/permission events reconcile through the native worker.
- Actual notification-denied/no-widget case cancelled both app alarm types. Exact permission denied produced an approximate alarm with a one-hour window; allowed produced window0 / permission / allow-while-idle flags. The permission-gated real Fajr screen-off/Doze transition delivered and scheduled sunrise next.
- Cosmetic refresh was deferred during Doze instead of waking the device. Android's chronometer still owns countdown display; no one-second notification repost, perpetual service, persistent wakelock or new GPS/network polling.
- Required platform limits: denied exact permission is not exact; force-stop, device-off and OEM policy cannot be eliminated by an app refactor.

### T08 — Complete offline native renewal

- `NativePrayerCalculator.java`, `NativeSolarMath.java`, `NativeCalendarMath.java`, `NativeHijriCalculator.java`, `PrayerSchedule.java`, store/coordinator/receivers: full compatible local engines and saved validated inputs; no extended static-cache workaround, guessed times, internet or background WebView.
- Exact installed-engine reference fixtures are committed with generator/provenance/licenses. Native tests compare independent JavaScript results, including invalid-event throws rather than loose tolerance.
- Actual API36 instrumentation seeded a65-day-expired schedule and renewed it before any WebView. Actual OS clock advanced65days with app closed; the worker produced30 valid rows covering the advanced epoch. Timezone changed to America/New_York and renewed headlessly; Africa/Cairo and automatic time/zone were restored and read back.
- Restore validated configuration/schedules together when downgrading; source rollback alone does not migrate schema2 back. No claim of months of physical-device unattended uptime.

### T09 — Foreground precision with no hidden cosmetic polling

- `src/utils/clock-scheduler.ts`, `src/hooks/use-visible-clock.ts`, dashboard/date widgets and Qibla: shared lifecycle/boundary clock, second precision only where shown, no hidden cosmetic timers, immediate resume reconciliation and bounded active heading work.
- Four scheduler tests cover hidden mounting, resume past transitions, forward/back clock correction and cleanup. Actual browser hidden probe observed no scheduled cosmetic intervals; ordinary web bridge payload work is zero. Compass work is inactive when hidden rather than running GPS in the background.
- Display precision retained. Work-count improvements are measured below, not converted into an invented battery percentage.

### T10 — Durable validated persistence

- `persistent-storage.ts`, `settings-storage.ts`, app/Qaza/customization contexts: updates read the latest committed value, preserve siblings, validate external/reset events and publish only after durable save. Multi-key failure rolls prior writes back; visible success is gated on persistence.
- Bun covers batched increments, quota/denied storage, recovery, corrupt JSON, deletion/clear, same/cross-tab state and multi-key rollback. Actual Qaza add3/reload/subtract3 retained and restored the original count using real touch; manual zero-coordinate settings remained usable offline.
- Corrupt entries are repaired without erasing valid older counts/preferences. Export/back up user data before any future downgrade; do not use a blanket reset as migration.

### T11 — Atomic bounded imports

- `src/utils/font-storage.ts`, `translation-validation.ts`, `src/contexts/translation-context.tsx`, font/translation controls: maximum5MiB `customfont.woff2`, header and real FontFace checks, verified IndexedDB transaction/readback and latest-intent protection; translation maximum1MiB before FileReader/parse plus bounded depth/leaves/strings, placeholders, BCP language/direction and prototype-key validation.
- Actual browser legacy46654-character base64 migrated to a34924-byte binary/schema-2 record and small pointer. Injected IndexedDB denial retained the working legacy font/error; retry migrated successfully. Database version is1; record/pointer schema is2.
- Real Android chooser imported the valid font, rejected a34-byte replacement without changing its working pointer, and retained the font after airplane-mode reload. Replacement upload stays available while a font is selected. Bun covers malformed/oversized imports, older partial translation defaults, corrupted collection repair and late selection/save failure.
- Browser hidden-state upload-helper completion did not deliver React change input; it is not claimed as a successful import. Real native chooser proof supplied that path. Full Android frontend RTL-JSON-picker interaction was not separately certified; compatible partial Arabic native payload and RTL surfaces were checked.
- Downgrade limitation: old source may not understand schema-2 font pointers. Preserve the original WOFF2/export and data before a separately approved reverse migration; verified migration does not mean legacy base64 is kept forever.

### T12 — Native language, clocks and fresh Hijri dates

- `NativePresentation.java`, `NativeHijriCalculator.java`, native snapshot/helpers and presentation bridge: localized labels/direction,12h/24h/system format and fresh adjusted Hijri data without altering event instants. Removed the arbitrary±30 Hijri adjustment restriction while retaining actual table/integer bounds.
- Actual native en/bn/compatible partial ar cases in all three clock modes retained event identity/epoch and parsed13:05 correctly; English1:05PM/13:05 and Bengali digits were observed. SDK±120 checks match29 Dhu al-Hijjah1447 and3 Ramadan1448 for the fixed date.
- Real native Arabic RTL rendering, duplicate instances and resize were exercised in light/dark configurations. Partial imports legitimately use English fallback for missing labels/months; no full Arabic translation completeness claim.

### T13 — Proven dead-code/dependency cleanup

- Compiler graph observed145 source modules,99 reachable from main/GitHub updater roots. The old settings page/dialog had no incoming references; the old wrapper's live selector caller was migrated before deleting those three obsolete files.
- Removed the sole unused React Query provider/direct dependency through Bun's normal manifest/lock update. Preserved static route imports, the intentionally disabled non-GitHub updater and all seven stable Android provider identities.
- Both builds/gates and real offline navigation retained supported features. No unrelated dependency upgrade, fabricated dependency-injection layer or user data deletion.

### T14 — Measured bounded improvements

- Correct PNG dimensions/MIME and smaller icon/favicon; remove ordinary-web/month-resync work and hidden polling rather than speculative lazy routes/abstractions.
- Three fresh-process original/current work runs used the same locked runtime, UTC, Makkah coordinates, method1/Shafi,13:00 start, one priming sync then20 logical30-second ticks. Actual original modules were exported from the recorded base commit; production modules were not rewritten to manufacture the baseline. External bridge acknowledgments were controlled successful adapters, not Android CPU/battery measurements.
- Exact counts and asset/bundle sizes below. Keep independent correctness/clarity improvements; do not claim that the larger JS bundle became smaller or visible startup became faster.

### T15 — Deliberate release and honest verification

- `scripts/reproducible-build.sh`, `verify-reproducible.sh`, `apk-payload.py`, Vite/build-info, Android Gradle and workflows: explicit mode/epoch/new output, no implicit install/sign/publish, recorded dirty inputs and checksums, guard mismatched embedded mode/time. Read-only verification separates trusted signature identity from ZIP payload equality.
- Final standard/GitHub unsigned payloads each matched their second build after the final resource fixes. Every484 entry was compared; safe original filenames reject traversal/NUL/backslash/duplicates/symlinks/encryption. Actual wrong-mode/wrong-epoch packaging failed intentionally.
- No trusted signed reference/certificate was provided. Neither signature authenticity nor whole-file bit identity nor pristine-source reproducibility is asserted. Production signing/publication requires separate authorization. Workflow policy rollback is separate from app-data rollback.

### T16 — Cancelled bounded location and honest recovery

- `src/services/location-service.ts`, `use-location-detection.ts`, native/web geolocation plugin and setup/settings: one user-requested acquisition,25s overall/15s GPS/5s fetch bounds, underlying cancellation, latest-intent/unmount guards, fine OR coarse permission, distinct errors and preserved valid GPS on failed address lookup.
- Existing HTTP IP provider and HTTPS fallback remain unchanged; no endpoint-change approval was given. HTTPS pages may block legacy cleartext and fall back. No background auto-detection/network retry loop.
- Actual airplane-mode GPS denial/timeout retained saved manual0/0 settings and user control. Three permanent service regressions prove offline geocoding retains0/0, abort releases a pending watch and prevents late publication/IP fallback, and denied GPS plus failed providers reports permission failure instead of a fabricated location.
- Native late-permission cancellation logic was inspected; all real OEM permission-dialog/out-of-order combinations were not certified. The OS permission sheet is not forcibly dismissed by application cancellation.

### T17 — Actual usability, native layout and documentation

- Semantic settings buttons, localized44-pixel Qaza controls, existing Radix sheets, native Back and restored focus; reduced-motion CSS; actual dated upcoming picker; correct relative512PNG manifest declaration. README/privacy/root `AGENTS.md` distinguish Android offline from browser caching and record supported tooling/architecture.
- Real Android touch/Back opened/closed the menu. Qaza touch, TAB focus, hardware Back, row-focus restoration and reload persistence passed. The menu was rendered and usable at130% WebView text zoom in dark configuration with no horizontal overflow.
- Real native rendering exposed Arabic glyph layout24px inside13px in the large widget, then still24/18 after header-only compaction. The full daily table now uses three columns/two rows, retaining all six names/times and stable IDs at declared250×220dp; real full-glyph/no-label-ellipsis regression passes. Wide chips previously ellipsized3 characters of `5:02 AM`; two-line clocks now keep the meridiem, with zero actual clock ellipsis in regression and visible capture.
- Permanent SDK suite covers all seven variants plus a duplicate, add/remove,80/220dp compact resize, Arabic RTL and light/dark; independent future-event selection and actual chronometers/notification are checked. A non-exported debug-only `WidgetTestActivity` isolates capture: launching MainActivity's WebView had overwritten native fixtures before screenshots. No production WebView was disabled to hide that QA race.
- Pixel captures inspected the changed menu, full large daily table and complete wide clock markers. Long labels, larger system font scales, other launchers/Android versions and full TalkBack remain explicit validation limits, not universal accessibility claims.

## Performance ledger

Same-input sync probe: three independent fresh Bun processes for each version/mode, primed once,20 logical ticks spaced30seconds. All count rows were identical across the three runs. Native adapter priming in the new implementation legitimately performs32 prayer calculations and2 acknowledged transfers for padded adjacent-day correctness; ordinary web priming performs none. These are work/serialization measurements, not a ten-minute physical-device battery run.

| Metric per20 warm ticks | Original | Current ordinary web | Current native adapter |
| --- | ---: | ---: | ---: |
| Full-month prayer calculations | 600 | 0 | 0 |
| Bridge transfers | 20 | 0 | 0 |
| Serialized bridge payload bytes | 199920 | 0 | 0 |
| Isolated helper wall-time range, ms | 25.56–27.10 | 0.26–1.38 | 2.53–3.19 |

Wall-time scope is only this helper under controlled adapters. It is not total app CPU, render/startup time, native allocation accounting or battery attribution. Retain the eliminated-work changes because exact repeated counts improve without disabling behavior; do not generalize tiny timing samples into percentage battery/CPU claims.

| Build/asset metric | Original | Final observed |
| --- | ---: | ---: |
| Standard JS, KB / gzipKB | 582.86 /179.41 | 640.91 /195.80 |
| GitHub JS, KB / gzipKB | 595.66 /183.20 | 648.35 /196.23 |
| CSS, KB / gzipKB | 72.56 /12.73 | 71.47 /12.71 |
| Icon, dimensions / bytes | 1280×1280 /362432 | 512×512 /73817 |
| Favicon, dimensions / bytes | 1280×1280 /362432 | 32×32 /1173 |

JavaScript grew; this is disclosed, not described as a bundle/startup optimization win. Image savings and elimination of native-only web work are deterministic improvements. Original hidden timer counts continued increasing; after-change hidden cosmetic clock has no timers. Original observation durations were uncontrolled, so those counters are not a battery benchmark. Occluded DOM-ready samples were rejected as paint/startup evidence. Native APK/DEX increase versus an original app build was not measured.

## Real offline/lifecycle scenario detail

- Packaged airplane-mode cold start/reopen, home/settings/upcoming/Qaza/menu and actual import/reload paths exercised without loading route chunks from the network; routes remain static. Manual zero coordinates stayed saved; upcoming picker selected the actual next calendar date.
- Actual expired horizon: native instrumentation seeded65days-old inputs, renewed before a WebView and rendered actual future times. Separate real OS clock jump65days produced30 rows covering the advanced epoch; original clock policy/timezone was restored and verified.
- Actual timezone: Africa/Cairo→America/New_York renewed headlessly with new dated horizon; restored Africa/Cairo, automatic time1, automatic timezone1. Exact-alarm app-op restored to default; night setting restored to its original value1.
- Actual Fajr delivery: exact permission explicitly allowed; pending RTC_WAKEUP window0 / flags0x5; Home/background process kill, screen off and forced deep Doze. No post-deadline manual broadcast was used to deliver the transition. The OS alarm advanced the real notification to `Now: Fajr`, next sunrise, chronometer deadline1791956580000 and the next exact sunrise alarm. Plain RTC cosmetic work was deferred roughly59minutes under Doze. This proves one accelerated permitted OS-alarm scenario, not overnight/OEM timing guarantees.
- Notification denied with zero widgets cancelled both app alarms; granting permission restored legitimate consumer work. Denied exact permission was observed as approximate, including retention in forced Doze; it is never described as exact.
- Owned emulator reboot/background/resume/process recreation were exercised during QA; device powered-off/force-stop limitations remain platform limitations. User force-stop suspends delivery until reopening; do not equate ordinary app closure with force-stop.

## Review, failed attempts and decisions

- Parallel native lifecycle and storage/import review slices completed. Storage findings (partially missing coordinates, unselected madhab, corrupted stored-translation handling, pre-read import limit) were fixed with regressions. Native review had no remaining required lifecycle finding; later real UI glyph/meridiem defects were reproduced and fixed, not ignored.
- Failed software-GPU emulator rendering was diagnosed through Android traces: HardwareRenderer/WebViewFunctor wait and system/app ANRs while the prayer worker was idle. Reusing the same owned AVD with the existing SDK's swiftshader backend restored real interaction. Attribution to that emulator renderer is an inference; no production hardware-acceleration disable or daemon/browser repair was added.
- Native chooser recent-files indexing/animation/offscreen-coordinate probes initially failed; real device-root chooser navigation and settled visual frames fixed the QA harness, not the app MIME restrictions. Real font replacement accessibility was an app defect and was corrected.
- A QA text-zoom call used the wrong WebView API, then was corrected to WebSettings; that temporary probe is not application code. An eager Bun mock rejection produced a fixture-only unhandled promise and was changed to lazy rejected fetch behavior; actual cancellation semantics passed.
- No new route-lazy framework, global install, background service, cache-only expiry concealment, fake prayer values, weakened test gate or hidden unsupported API fallback was retained.

## Second Review Corrections (S01–S07) Evidence

| ID | Implementation | Failing-before evidence | Passing regression evidence | Actual runtime evidence | Remaining criterion/blocker |
|---|---|---|---|---|---|
| **S01** (Real outer boundary validation) | Added `validateOuterBoundaries(Calculation)` in `PrayerSchedule.java`; enforced in `PrayerWidgetPlugin.java:updateSchedule()`, `getScheduleStatus()`, and `NativeScheduleStore.loadSchedule()`. | REPRODUCED: constructor and load paths accepted outer edges shifted by hours within day bounds. | `PrayerTimelineTest.validEngineDerivedSchedulePassesOuterBoundaryValidation`, `slightlyShiftedFirstPreviousIshaFailsOuterBoundaryValidation`, and `slightlyShiftedLastNextFajrFailsOuterBoundaryValidation` using engine-derived Makkah baseline. | UNVERIFIED (Requires physical Android device or emulator bridge run with injected corrupt stored schedule). | Automated emulator/device instrumentation of persisted recovery path. |
| **S02** (Arithmetic usability over arbitrary caps) | Removed arbitrary ±120 cap in `settings-storage.ts`; validated representable `Date` limits with 1e14 ms headroom (`MAX_DATE_HEADROOM`). Guarded timestamps in `time-utils.ts`. | REPRODUCED: valid 121-min and 121-day adjustments were falsely rejected as corrupt; 2e12 headroom risked future Date overflow. | `storage-imports.test.ts`: 121 and -121 preserved; `MAX_SAFE_INTEGER` and NaN normalize safely to 0 without corrupting siblings. | REPRODUCED: Web test suite dashboard derivation and settings reload with valid non-default offsets. | None |
| **S03** (Ownership-safe & observable font cleanup) | Logged/reported cleanup failures with compound error propagation in `font-storage.ts` and `translation-context.tsx`. `uploadFont` cleans uncommitted candidates directly; `cleanupRecord` preserves font on `localStorage` read error; request ownership guards stale migration errors. | SOURCE-ESTABLISHED: empty catch swallowed deletion failure; `cleanupRecord` inferred orphanhood on localStorage read errors; upload candidates leaked when localStorage threw. | `translation-context.tsx` and `font-storage.ts` compound error on cleanup denial; `storage-imports.test.ts` test for compound cleanup error. | UNVERIFIED (Requires physical browser session with provoked storage quota/permission denial). | Real browser quota denial test. |
| **S04** (Real post-write verification regression) | Replaced ineffective 50-byte zero buffer test with a valid WOFF2 header fixture testing post-write readback verification failure and candidate cleanup; restored `globalThis.indexedDB` in `finally`. | REPRODUCED: old test threw synchronously at `validateWoff2` before IndexedDB or cleanup was reached. | `storage-imports.test.ts`: `failed binary verification cleans up candidate record from store` and `failed binary verification with deletion failure raises compound error`. | REPRODUCED (Unit mock IndexedDB in Bun); UNVERIFIED (Real browser/WebView IndexedDB transaction execution). | Real browser IndexedDB execution. |
| **S05** (Durable native revision & status retry) | Fixed `NativeScheduleStore.saveSchedule()` to persist `prayer_schedule_revision` when schedule JSON matches. In `widget-sync-service.ts`, re-sample time/timezone post-await, enforce `schema === 2` and safe integer bounds, and reset `checkedNativeStatus` on failure. | REPRODUCED: matching schedule JSON never wrote revision; status failure permanently disabled future status checks; test never called sync a second time. | `widget-sync.test.ts`: two-pass `failed status query does not permanently lock out subsequent retry`, `missing revision in native status rejects reuse`, and `near-expiry native schedule triggers renewal`. | UNVERIFIED (Requires real Android process restart while retaining native preferences). | Cold-start WebView recreation on actual Android runtime. |
| **S06** (Intermediate polar collapse regression) | Added test starting on May 20, 2026 at latitude 69 where initial days compute but subsequent day collapses. Retained single-sort timeline in `time-utils.ts` and artifact-only CI workflow in `release-apk.yml`. | SOURCE-ESTABLISHED: prior test failed on day 0, never exercising in-progress renewal collapse. | `PrayerTimelineTest.intermediateAstronomicalCollapseFailsRenewalWithoutFabrication`, `prayer-timeline.test.ts` NYC/UTC chronological ordering, `release-apk.yml` artifact-only workflow. | UNVERIFIED (Requires physical device renewal at high latitude boundary). | Physical high-latitude GPS device run. |
| **S07** (Status & claim reconciliation) | Reconciled evidence table with explicit implementation, regression, runtime, and blocker fields with honest `UNVERIFIED` designations for physical-only criteria. | SOURCE-ESTABLISHED: blanket "None" blocker claims replaced with separated verification fields. | Documented in `tasks/evidence.md`, `tasks/plan.md`, `tasks/todo.md`. | Inspected against actual source diff and executed test results. | None |

## Rollback and remaining release limits

1. Source is uncommitted and release version is unchanged. Revert only reviewed task-owned changes; do not use blanket reset/clean against unrelated user edits. Original image rollback copies are retained in `.tools/asset-backups/`.
2. A source downgrade does not reverse IndexedDB/schema2 settings/schedule changes. Back up/export settings, counts, original font bytes and relevant native state with appropriate approval before downgrading. Preserve working data until replacement/reverse migration succeeds; reconcile/cancel obsolete alarms rather than leaving both schedules active.
3. No physical-device battery percentage, controlled app CPU profile, native allocation count or visible startup latency was established. Reduced work is proven; numerical battery savings are not.
4. API36 emulator/local modes do not establish OEM overnight reliability, every supported Android version, launcher/lock-screen implementation, full TalkBack or largest-system-font behavior. Longer names/large accessibility fonts can need larger widget space.
5. No independently trusted production signer/reference was available. Controlled unsigned payload equality is not signer identity, authenticated release content, ZIP whole-file equality or pristine-source reproducibility. No production signing/publication was performed.
6. Android bundled assets/engine remain offline. GPS address/IP fallback and GitHub-mode update checks are existing network exceptions; no new domain or service worker was added. Browser offline cold opening remains unpromised.

## Cleanup status

Verified: owned browser font pointer/database removed, cache-disabled probe reset, only owned Edge tab closed and original harness attachment restored; owned preview stopped. Owned emulator test APK uninstalled, task-created app fixtures/external chooser/screenshot data cleared, QA widget-bind grant revoked, working final debug app left installed and owned emulator stopped. No personal app data or user browser profile was cleared.

Filesystem cleanup verified: removed 66 enumerated task-owned artifacts/directories plus 48 stopped isolated Gradle daemon logs; exact-scope existence checks report no remaining listed probes, screenshots, installer archives, alternate failed cache or generated build/reproducibility outputs. Gradle's isolated daemon was stopped. Permanent source/reference fixtures, project dependencies, isolated working runtimes/dependency caches, AVD runtime and original-icon rollback copies remain. Pre-existing/unrelated data and browser profiles were not purged. Build commands regenerate the cleared web/native outputs; build and sync before standalone Android packaging.
