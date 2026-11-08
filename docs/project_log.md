# Project log

A dated record of what was done and decided, newest first. Add an entry at the end of every working session. Keep entries short and link to the backlog stories, ADRs and open questions involved.

## Entry template

```markdown
## YYYY-MM-DD: <short title>

**Done**
- …

**Decisions**
- …

**Open / next**
- …
```

---

## 2026-11-08: LB-003, golden fixtures and recorder

**Done**
- **PR #53 (LB-303) merged** on 2026-11-07, after the two CI fixes its entry records, and `main` was synced to `1066aa3e`.
- **LB-003 (`feat/LB-003-golden-fixtures`):** spec §10.2, `api_spec.md` §9, NFR-PRIV-02.
  - **`scripts/record-fixtures.sh`:** the only place that may call the live source. One table of 19 endpoints (spec §10.2's 16, both API-07 routes included, plus API-12's `sections.json` and the two CartoCiudad calls), each recorded once, at least a second apart, with `LogronoBus/<VERSION_NAME> (fixture recorder <kernel>)` and `Accept-Encoding: gzip`. A whole run is staged in a temporary directory beside `fixtures/` and moved into place only when every endpoint answered, so a failure leaves the committed captures untouched. `--list` prints the table with no network; `--check` records into a temporary directory, writes nothing, prints the shape diff and exits 1 on drift.
  - **`scripts/ci/fixture-shape.sh`:** the one shape implementation, `jq`. One line per key path and type, arrays collapsed to `[]` so the item count is not part of the contract, an integer and a float both `number` (the source mixes `42.0` and `42`), and an array that reports its element path `a[]` as `unknown` when it holds nothing, so a collection that became empty, or was empty and now holds items, is not drift in either direction. `null` and `unknown` are the open types: neither they nor anything beneath them is drift, while a path that appears or disappears elsewhere is a change whatever it carries, `null` included and unless it sits beneath an open path. Diff lines are `+ path type`, `- path type` and `~ path old->new`.
  - **`fixtures/`:** 19 captures, 3.5 MB, canonicalised with `jq --sort-keys --indent 2`. A second `--check` right after recorded all 19 as `unchanged` although values and sizes had moved.
  - **`.github/workflows/weekly-fixtures.yml`:** every Monday at 04:17 UTC and on demand, `Fixtures` runs `--check` and `Contract drift` opens one issue per ISO week, titled `Contract drift: weekly fixtures <year>-W<week>`, with the run URL, the drift lines read back from the failed job's log (never passed through `$GITHUB_OUTPUT`, since they quote the source's own key names) and the playbook link. It is in no gate, so a drift never blocks a merge. Documented in `ci_cd.md` §5.1.
  - **Test-scope network guard:** a JDK `InetAddressResolverProvider` in `:shared:core:testing`, put by `logrobus.kmp.library` (`androidHostTest`) and `configureAndroidModule` (`testImplementation`) on the test classpath of every module that uses one of those two conventions, except the two test-support modules, which make no request; `:build-logic:convention:test` and `:tools:architecture-tests:test` get neither. Loopback only; any other host name throws `NETWORK GUARD: real network access is forbidden in tests`. `lookupByAddress` still delegates, so reverse lookups work.
  - **Robolectric had to follow:** it fetched its Android framework jar from the test JVM through `MavenArtifactFetcher`, into `~/.m2`, which CI never caches and which `project_log.md` already records flaking. With the guard in place that fetch would fail, so `RobolectricOffline.kt` resolves four jars through Gradle (the target 37, `SCREENSHOT_SDK` 36, `LINEAR_FONT_SCALING_SDK` 33 and the shared KMP modules' API 23 fallback) and hands them over with `robolectric.offline=true` and `robolectric.dependency.dir`. The four jars are about 663 MB, downloaded once into Gradle's cache and copied into each module's `build/`. One configuration per SDK, because one would conflict-resolve three versions of the same module to the highest.
  - **Tests:** `FixtureRecorderScriptTest` (JUnit 5 in `:tools:architecture-tests`: the real script against a Python stub bound to 127.0.0.1 for the recording cases, `scripts/ci/fixture-shape.sh --diff` on two documents with no server for the shape cases, and three cases that need neither a server nor a network: `--list`, the two-mode error and the staging-directory check), `TestNetworkGuardTest` (3) and `NetworkGuardProbeTest` (2).
  - **Docs:** `acceptance_criteria.md` with the evidence, `api_spec.md` §9 and §10, `ci_cd.md` §1 and §5.1, `testing_strategy.md` §4.2–4.3, the recorder playbook, `backlog.md`, `AGENTS.md` §2 and §4, both READMEs, and `.gitignore` for a staging directory a killed run leaves behind.

**Review**
- Four reviewers ran: scripts-and-CI, docs-and-fixtures, security, and the agent's own gate runs.
- **Fixed:**
  - **A body that is not a JSON object or array was accepted.** An empty, `null` or scalar body canonicalised to a 0-byte fixture that a later `--check` compared against itself and reported `unchanged`; the recorder now requires `jq -e 'type == "object" or type == "array"'` (NFR-REL-02).
  - **A second mode word was silently ignored:** `--list --check` listed and exited 0, so a caller believed it had checked the fixtures. Two modes are an error now.
  - **An empty collection read as every child path removed,** so a night-time `activities: []` would have opened a false drift issue every week. An array now reports its element path `a[]` with type `unknown`, and the diff reads `unknown` as compatible with any element type, so a collection that became empty is not drift.
  - **The recorder had no cap on a response body.** Every request carries `--max-filesize`, and the decompressed size is checked after the download: a broken or hostile source must not fill the disk beside `fixtures/`.
  - **The workflow passed untrusted JSON key names through `$GITHUB_OUTPUT`,** where they could forge job outputs or close the issue body's code fence. The drift job now reads the failed run's log and sanitises it.
  - **A staging directory a killed run leaves behind** is now ignored by git.
  - **The docs claimed** the app's `User-Agent`, 13 stub-driven cases, "every JVM test classpath", and a null rule that a new key breaks.
- **Not acted on, with reasons:**
  - **A numeric IP literal bypasses every JDK resolver,** so the guard stops host names only, and the docs now say exactly that. A socket-level control would need `--add-security-manager` or a byte-level rewrite of the JDK.
  - **The four framework jars are copied into each module's `build/`,** about 660 MB per module, which is the price of the guard staying absolute (measured; the download itself happens once, in Gradle's cache).
  - **`localhost.` with a trailing dot is a false negative.**
- **Flagged, then decided by the owner:** `fixtures/recharge_points.json` publishes about 100 named private shopkeepers with their street addresses. The privacy review called it personal data; the owner chose to keep it (see Decisions), so the dataset is named in `security_privacy.md` instead.

**CI**
- **Run 1** (42473163535): every Kotlin job died at about 20 s, and the iOS run too, on the Gradle wrapper's own download: `Downloading from https://services.gradle.org/distributions/gradle-9.7.1-bin.zip failed: timeout (10000ms)`. A fresh runner has an empty `GRADLE_USER_HOME` and thirty jobs asked at once; the wrapper allowed a 10 s read and no retry, and `scripts/ci/gradle-retry.sh` cannot help because `./gradlew` is what failed.
- **Run 2** (42473359481): the same failure, 17 jobs at about 20 s. Recurring, so the pipeline was made resilient instead of rerun a second time: `gradle-wrapper.properties` now waits up to 120 s and retries four times, 2 s apart, with the distribution checksum still pinned, and `lb003_theWrapperRetriesItsOwnDownload` keeps both floors (red first: the test failed on `networkTimeout=10000` and `retries=0`). `ci_cd.md` §5 records it.
- **Run 3** (42473618515): the first run past the wrapper. Every job but two passed; `Shared unit tests (:shared:core:presentation)` and `Android unit tests (:android:feature:planner)` burned the wrapper's five retries on `502` and `504` from `services.gradle.org`, an upstream outage rather than the branch.
- **Run 4** (42474654968, iOS 42474655004): the Kotlin workflow passed **every** job with the same hardened wrapper, during the same outage fading out. That is the evidence that the two flake fixes work: the retries survive a slow or briefly failing distribution host.
- **The final tree** adds only log entries after run 4, and every push re-runs the whole pull request, so its own run repeats that check. The watch started at the end of the session reports it.

**Decisions**
- **Owner (2026-11-07, carried into this session):** LB-003 is the next task.
- **Owner (2026-11-08):** the captures are committed with OQ-11 still open; the merge, and with it the legal reading of committing public operator data, is the owner's.
- **Owner (2026-11-08), the guard's scope:** host names only is a decided limitation, not a defect. A numeric IP literal never reaches a JDK resolver, and a socket-level control would need `--add-security-manager` or a byte-level rewrite of the JDK.
- **Owner (2026-11-08), `recharge_points.json`:** keep the capture rather than replace it with a shape-only `synthetic_` copy. It is API-11's public answer, the fixture is API-11's contract, and `testing_strategy.md` §4.2 forbids hand-editing a recorded capture. The dataset is recorded in `security_privacy.md` §2 and the decision here, so the privacy review's finding is answered rather than dropped.
- **Agent:**
  - **The guard stays absolute.** No carve-out for Maven Central: the framework jars come from Gradle instead, which also turns a `~/.m2` download that CI never cached into a cached one.
  - **The wrapper outlasts the distribution host,** not only the dependency repositories: a 10 s read timeout with no retry could not survive thirty fresh runners asking `services.gradle.org` at once, and it failed that way twice in one session.
  - **`sections.json` is recorded** although spec §10.2 omits it: it is API-12's fifth path and the same capture costs one request.
  - **The route fixtures use fixed instants and public places** from `api_spec.md` §4.7, so the rejection is reproducible; `route_rejected.json` keeps the `walk-too-far` rejection the source now sends, with no routes, and `route_ok.json` the five routes.
  - **`synthetic_` was not needed:** the live `vm_all_with_object_lineRef.json` carries `"[object Object]"` in 40 of 57 activities, at `result.activities[].lineRef` and `publishedLineName`.

**Open / next**
- **This entry's pull request** is open, and its watch runs.
- **Next, once it is merged:** LB-005 part 2 (the DTOs and services, whose contract tests now have the fixtures), then LB-006 part 2, LB-008 part 2 and LB-101 part 2.
- **Waiting for the owner:**
  - OQ-11, now that the captures are in the repository and `recharge_points.json` is committed under the decision above;
  - the TalkBack and VoiceOver passes already listed in the LB-303 entry;
  - the iOS job limit, Android's backups, DD-10's provisional stops, alert 6 and the Gradle cache provider;
  - OQ-10.

---

## 2026-11-06: LB-303, Leave now or Depart at

**Done**
- **PR #52 (LB-304) merged** on 2026-11-06, and `main` was synced.
- **LB-303 (`feat/LB-303-departure-time`):** FR-PLAN-03, "Leave now" or "Depart at" a date and time.
  - **Shared (`:shared:feature:planner`):**
    - `PlanDeparture` is `Now` or `At(instant)`. `departureAt(now)` is what API-07 will send: the app clock's now for "Leave now", the picked moment for "Depart at". `atMadrid(time, now)` reads a picked date and time on the Madrid clock, whatever the device's zone, and `PlannerEvent.DepartAt.departure(now)` calls it with the pickers' fields; kotlinx-datetime moves a time in the hour skipped on the last Sunday of March on by that hour, and the hour repeated on the last Sunday of October is its first pass unless that has gone by.
    - `PlannerUiState` gains `DepartureUi` (`Now`, or `At` with its `DepartureDay` on the Madrid calendar: today, tomorrow or another day) and `DeparturePickerUi` (where the pickers open, and now, before whose day no date can be picked), while the pickers are open.
    - `PlannerEvent` gains `LeaveNow`, `ChooseDepartureTime`, `CancelDepartureTime` and `DepartAt` (Madrid wall-clock fields).
    - `PlannerUiMapper.state(places, departure, picking, now)`.
  - **Umbrella:** `PlannerUseCases.clock()` hands Swift the app clock. `PreviewUseCases` delegates How to Get There's group to `PreviewPlanner`: the initial origin over `PreviewLocation`'s script, and a clock that stands at the sample sync, 10:30 on 28 Oct 2026 in Madrid, until `timePasses(minutes:)` moves it on.
    - `scripts/checks/run-all.sh` first failed on detekt's `TooManyFunctions`: the two overrides took `PreviewUseCases` to 13 functions, over 11. `PreviewPlanner` holds them now, as `PreviewSettings` and `PreviewLocation` hold theirs (bd3bde65).
  - **Android:** `PlannerViewModel` takes `AppClock` from Koin and keeps the departure and whether the pickers are open. Under the card, an assist chip (`DepartureChip`) says the departure and opens a menu, "Salir ahora" or "Salir a las…", whose second item opens Material's date picker (`DaysFrom`: no day before today), then its time input, two typed fields in a dialog at least `Tokens.dialogMinWidth` wide. `clockText` in the design system formats HH:mm for the ETAs and the chip.
  - **iOS:** `PlannerViewModel` does the same on `useCases.clock()`, and stores the state, mapped again on every event. A section of its own holds a `Menu` row with the same items; the picker is a sheet, presented by item, with the system's graphical `DatePicker`, which shows and reads the Madrid clock through `Calendar.madrid` (`MadridClock.swift`). The design system's `clockText` formats HH:mm here too.
  - **`:android:core:testing`:** `assertAccessible(knownIssues)` leaves out problems named word for word, and fails on any other and on a named one that no longer occurs.
  - **Strings:** `plan_leave_now`, `plan_depart_at_choose`, the plurals `plan_depart_today`, `plan_depart_tomorrow` and `plan_depart_on` (the hour picks "a la" or "a las"), and the pickers' buttons, in Spanish and English on both platforms.
  - **Tests:** `PlanDepartureTest` and `PlannerDepartureUiTest` (shared); both ViewModels' `lb303_` tests; Android's `PlannerDepartureTest`, `PlannerRouteTest`, screenshots, and `DepartureDialogsScreenshotTest` with `assertAccessible` on both dialogs; iOS's end-to-end `ShellUITests.test_lb303_…`, `MadridClockTests`, snapshots of the row and of every text, and the "Departure time" sheet in `AccessibilityAuditUITests`' walk. The other states' screenshots and snapshots show the new row.
  - **Docs:** the acceptance criteria record the evidence and two decided behaviours, and LB-306's criteria now send `departureAt`. `design_guidelines.md` §3.3 and §9, `architecture.md`, `AGENTS.md`'s status and both READMEs are updated.

- **Review:** the four reviewer agents asked for changes, with no blockers; every finding was acted on, or answered below.
  - **Where the pickers open for a time gone by** was decided in each view, differently (Android today at the picked HH:mm, iOS today at 00:00), and untested. The shared mapper opens them at now (4ce3194a, then 23575f9d).
  - **October's repeated hour:** a time picked during its second pass resolved to the first, an hour ago. `atMadrid()` takes now and takes the second once the first has gone by (635d74a1, then 742f63f3). With now it took six parameters, over detekt's five, which `run-all.sh` caught: it takes a `LocalDateTime`, internal, and the event builds it (330414ef).
  - **Time-zone tests** in winter time and on both clock-change Sundays, seen failing with the days counted in UTC; a day gone by shows its date. `DepartureUi.At.day` became `relativeDay`, apart from its time's day of the month.
  - **The dialogs had no screenshots and no accessibility checks.** Checked, the time picker's clock face failed at once: hours with 32 dp touches, numbers overlapping at large text, and at 200 % its OK broken mid-word. The time is typed now, and the dialog takes Material's narrowest dialog width, so its buttons stay on one line, at 200 % with SDK 33's linear scaling too. Two of Material's own issues remain, named in `assertAccessible`'s new `knownIssues`. Robolectric's default 320 dp screen cut the dialogs, so they run on a phone-sized one.
  - **iOS:** the Madrid conversions are tested from New York's and Tokyo's zones (a GMT calendar fails them; `NSTimeZone.default` does not move `TimeZone.current`, so a calendar on the device's zone would fail on CI's UTC simulators only); every text has a snapshot in both languages; the sheet is in the audit's walk, where the system picker's own issues are left out by name (`isTheSystemPickersOwn`) and its title in dark mode joins the Dark theme's known contrast false positives; the state is stored, so a "tomorrow" that midnight changed reaches the view when the root shows again (a test with the preview clock moved on); the sheet keeps its content while it slides away; the day and month are formatted on the Gregorian calendar; a preview of the open sheet; and the UI test waits for the old row to go.
  - **Nits:** one HH:mm formatter per platform in the design system; `PlannerScreen.kt` passed detekt's function count once the chip moved to `DepartureChip.kt`; the docs name `PreviewPlanner`, and the READMEs say whose pickers each platform shows.
  - **Not acted on, with reasons:** the year in another day's date (no day gone by can be picked, so a date without a year is ambiguous only beyond a year ahead); VoiceOver reading "15 ene" rather than the month's full name (for the device pass); Material's calendar marking today by the device's own clock (a Material limitation, in §3.3); and the red commit ab598e6, which brought `clock()`'s plumbing whole, as its body says.

**CI**
- **Run 1 (42356647912):** `verifyRoborazziDebug` failed on `Planner_timeDialog_linearFontScale2.png` alone, and the Kotlin gate with it. The diff was the antialiasing of the dialog's rounded corners: Robolectric's SDK 33 draws them differently on CI's Linux than on macOS, where the reference was recorded; SDK 36's captures match on both. A fault in the branch, not a flake: that test now checks accessibility at linear scaling without a capture, and its reference went. No tolerance was added.
- **Run 2 (42357253375):** "Android screenshots (:android:app)" failed in `TabNavigationTest.lb101_reselectingLinesAtItsRootScrollsTheListToItsTop`, at its first check: after scrolling the Lines list to "Line B3", "Line 1" still showed. A runner flake: the branch does not touch the Lines list, the same job passed on run 1 with the same code, and the failure has no earlier record here. The failed jobs were rerun once. If it recurs, the test should wait for the scroll to settle.

**Decisions**
- **Agent:**
  - **LB-303 is done without its request part, for the owner to confirm.** There is no plan request until LB-306, whose criteria now send `departureAt` from `PlanDeparture.departureAt(now)`, in UTC with milliseconds as API-07's example. The value itself is tested here.
  - **The pickers:** Android's chip follows the sketch in §3.3, with Material's dialogs; iOS keeps the inset grouped list, so the departure is a menu row in a section of its own, and the picker the system's graphical one in a sheet. The pickers open at the picked time, or at now for "Leave now" or a time gone by.
  - **Android's time is typed,** never on the clock face, whose touch targets and large-text layout fail the checks (Review above).
- **Owner (2026-11-07):** the pickers' own accessibility issues are exempted by name ("Eximir por nombre"), over leaving the pickers unchecked or building pickers of our own.
  - Android: Material's date picker's previous-month arrow at 200 % (41 dp of 48) and the time input's ":" glyph, in `assertAccessible`'s `knownIssues`; an exemption fails once its issue no longer occurs.
  - iOS: the system date picker's own Dynamic Type, contrast and unattributed-text issues on the "Departure time" sheet, inside its frame only; the sheet's title and buttons are still audited.
- **Agent (continued):**
  - **The day it says is counted again on every event,** the root showing again included, not at midnight: no timer may run (AGENTS.md §5.7).
  - **No day before today; any time.** Material's time picker cannot be limited, so neither platform limits the time: a time earlier today is allowed, and the planner (LB-306) will plan it. No limit ahead either: a date beyond the timetables gets LB-307's no-route message.
  - **A picked time stays** until the user changes it: the root showing again keeps it, and so does the time passing. A time whose day has gone by shows its date, and the pickers open at now.
  - **The text:** HH:mm on the Madrid clock, as the ETAs' clock times, with "mañana" or the date; the hour picks Spanish's article, as `eta_clock_spoken` does.
  - **No compile gap this time:** the mapper's new state lived beside the old one until both ViewModels moved, and the new events came with both ViewModels' red commit.

**Open / next**
- **This entry's pull request** is open, and its watch runs.
- **Next, once it is merged:** the owner's choice. How to Get There's next P0 stories need the source's DTOs (LB-005 part 2, after LB-003's fixtures, which only the owner may have recorded) or place search (LB-302, which needs CartoCiudad's fixtures), and most others need OQ-10 or OQ-11.
- **With the owner:** confirming that LB-303 closes with its request part in LB-306, as LB-304's results part; the TalkBack and VoiceOver passes on a device, for LB-301 part 1, LB-304 and LB-303 (the chip and the row, Material's and the system's pickers).
- **Waiting for the owner:**
  - the iOS job limit;
  - Android's end-to-end encrypted backups;
  - DD-10 provisional stops;
  - alert 6 (Kotlin Gradle plugin);
  - the Gradle cache provider;
  - OQ-10 and OQ-11.

---

## 2026-11-06: LB-304, swap origin and destination

**Done**
- **PR #51 (LB-301 part 1) merged** on 2026-11-06, after its third CI run passed, and `main` was synced.
- **LB-304 (`feat/LB-304-swap-places`):** FR-PLAN-08, one tap swaps the trip's ends.
  - **Shared (`:shared:feature:planner`):**
    - `PlanPlaces` holds the trip's two ends. `swapped()` exchanges them.
    - `following(initialOrigin)` applies FR-PLAN-11 each time the root shows. "My location" fills an empty origin unless the destination holds it, and leaves either field without the permission.
    - `PlannerEvent.Swap`, and `PlannerUiMapper.state(places)`, which shows each end in its own field, "My location" in either.
  - **Android:** `PlannerViewModel` keeps `PlanPlaces`. `PlannerScreen` takes `onEvent`, as the other screens do, and puts an icon button (Material Symbols `swap_vert`) beside the card, centred between the fields.
  - **iOS:** `PlannerViewModel` keeps `PlanPlaces`. The section's one row holds both fields, the line between them, and a borderless button (`arrow.up.arrow.down`) at its trailing end. A screen reader reads the fields, then the button.
  - **Strings:** `plan_swap` / `plan.swap`, "Intercambiar origen y destino" ("Swap origin and destination"). "Mi ubicación" moved to `plan_my_location` / `plan.my_location`, since either field can show it now.
  - **Tests:**
    - **Shared:** `PlanPlacesTest`, and `PlannerUiMapperTest`'s swapped state.
    - **Android:** `PlannerViewModelTest`, `PlannerScreenTest` (a button, spoken, sends `Swap`), `PlannerRouteTest` (the tap reaches the ViewModel), and screenshots of the swapped state.
    - **iOS:**
      - `PlannerViewModelTests`, and snapshots of the swapped state.
      - `ShellUITests.test_lb304_…`, end to end: location turned on from Near Me's row, the system's prompt answered in English or Spanish, then one tap on the button by its spoken label. The accessibility audits walk the button with the root.
      - A first try, a package test that walked a hosted view's accessibility tree to find the button, found nothing, with the button in place or not: SwiftUI builds no such tree there. The UI test replaced it (a second red commit).
      - A run stopped while the system's location prompt showed left the simulator's location denied, and the next run's `resetAuthorizationStatus` did not clear it; `simctl privacy … reset location` did. CI's fresh simulators start never asked.
  - **Docs:** the acceptance criteria record the evidence, and LB-306's criterion now clears the results on any change of origin, destination or time, a swap included. `design_guidelines.md` §3.3 and §9 and `architecture.md` are updated, as are `AGENTS.md`'s status and both READMEs.
- **Review:** the four reviewer agents approved with findings, all acted on:
  - **Android, TalkBack's order:** nothing made the card a traversal group, so TalkBack could read the button, centred beside the card, between the fields. Material's Surface marks itself with the deprecated `isContainer`, which a test showed is not `IsTraversalGroup`. The card now sets it (red 8471b73 then green 6c650c0, with `lb304_talkBackReadsBothFields_beforeTheSwapButton`).
  - **Shared:** `following()` now moves "My location" only, in both branches, so part 2's other places can never be filled in or kept by it. Two tests pin "My location" leaving the destination, and a revoke then a re-grant after a swap; both were seen failing with the rule broken on purpose.
  - **Evidence:** the iOS reading order rests on the fields' sort priority, which no automated check reads, so it joins the device pass. The two behaviours decided here are now acceptance criteria of LB-304. Only "My location" and an empty field can be swapped until part 2, which also brings a two-place swap test.
  - **LB-301 part 2's follow-ups,** in the backlog: an origin the user empties must not get "My location" back each time the root shows, since FR-PLAN-11 is a default.
  - **Nits:** a preview of the swapped state on both platforms; the screen test uses the shared swapped state; the iOS ViewModel's state derives from its places; the UI test resets location when it ends and lists the prompt's labels when it fails; comments and the design text say the button is centred on the card; `architecture.md` counts `PlanPlaces` among the domain models; LB-306's criterion names the time too (LB-303).
  - **History:** between the Android red commit 245e2e6, which added `PlannerEvent.Swap`, and the iOS red commit f1b6ffe, which added its Swift case, the iOS package would not have compiled. `main` only sees the merge.

**Decisions**
- **Agent:**
  - **A swap survives the root showing again.** FR-PLAN-11 makes "My location" the default origin, and LB-301 read it again each time the root shows. After a swap, that read would have put "My location" back in the origin, in both fields at once. It now fills an empty origin only while the destination does not hold it.
  - **Without the permission, "My location" leaves either field**, the destination included, as it left the origin in LB-301.
  - **Placement:** Android puts the button beside the card, as the sketch in `design_guidelines.md` §3.3 does. iOS keeps the system's inset section, so the button sits inside it, at the trailing end of one row that holds both fields. Each is at least 48 dp or 44 pt to touch.
  - **The button is always enabled.** With both fields empty, a swap changes nothing visible, which a disabled state would only complicate.
  - **LB-304 is done without its results part, for the owner to confirm.** The results arrive with LB-306, whose criterion now names the swap among the changes that clear them (FR-PLAN-14).
  - **No announcement after a swap, for now.** Focus stays on the button, and the fields say what changed when the user moves back to them. Whether TalkBack and VoiceOver should announce the new trip is a question for the device pass.

**Open / next**
- **This entry's pull request** is open, and its watch runs.
- **Next, once it is merged:** LB-303 (Leave now / Depart at), unless the owner chooses otherwise.
- **With the owner:** the TalkBack and VoiceOver passes on a device, for LB-301 part 1 and LB-304 (the iOS reading order, and whether a swap should be announced); and confirming that LB-304 closes with its results part in LB-306.
- **Waiting for the owner:**
  - the iOS job limit;
  - Android's end-to-end encrypted backups;
  - DD-10 provisional stops;
  - alert 6 (Kotlin Gradle plugin);
  - the Gradle cache provider;
  - OQ-10 and OQ-11.

---

## 2026-11-06: LB-301 part 1, How to Get There's origin and destination fields

**Done**
- **PR #50 (LB-201 part 1) merged** on 2026-11-06, after the third attempt at its dark audits held on CI, and `main` was synced.
- **The next story:** no P0 story left can be done whole without OQ-10 or OQ-11. The owner chose LB-301 part 1 (2026-11-06) over LB-404's attributions and an engine-only LB-213 part 1.
- **LB-301 part 1 (`feat/LB-301-planner-fields`):** FR-PLAN-11 on How to Get There's root.
  - **Shared (`:shared:feature:planner`):**
    - `ReadInitialOrigin` returns `PlanPlace.MyLocation` with the location permission, approximate or precise, and nothing without it. It reads the permission only: it never asks and starts no updates.
    - `PlannerUiMapper` turns it into the two fields, `PlaceFieldUi.MyLocation` or `Empty`; the destination is `Empty`.
    - `PlannerEvent.Shown` reads the origin again.
    - The graph binds `ReadInitialOrigin`, and `PlannerUseCases` hands it to Swift. `PreviewLocation` serves it from the same scripted permission as Near Me's, so `locationOn()` scripts both tabs.
  - **Android (`:android:feature:planner`):**
    - `PlannerViewModel` starts from the origin read at once, so the first frame shows it.
    - `PlannerRoute` sends `Shown` on resume.
    - `PlannerScreen` draws the fields in an outlined card, each one merged for TalkBack as "Origin, My location".
    - The shell opens `PlannerRoute` on the tab, and `TabRootPlaceholder` and the app's `plan_title` are gone.
  - **iOS (`FeaturePlanner`):**
    - `PlannerViewModel` reads the origin through `SharedDependencies`; `PlannerView` sends `Shown` on appear and each time the scene becomes active.
    - Each field is one accessibility element, with the field's name as its label and what it shows as its value.
    - The accessibility audit first reported both fields' text as clipped, at every size, although it showed whole. Each field is now an `HStack` with its text across the row's whole width, as Near Me's row since LB-201. Before, it was a `Label`, whose frame starts at the icon. The icons share one scaled width, so both texts start in line.
    - The shell opens `PlannerView`. `TabRootPlaceholder`, `TopLevelDestination.title` and the app's `plan.title` are gone: `plan.title` lives in `FeaturePlanner`'s catalogue now.
  - **Tests:**
    - **Shared:** `ReadInitialOriginTest`, `PlannerUiMapperTest`; the graph and preview tests in `:shared:umbrella`.
    - **Android:**
      - `PlannerViewModelTest`, `PlannerScreenTest`, `PlannerRouteTest`;
      - `PlannerScreenshotTest`: both states in light, dark and 200 %, with `assertAccessible`;
      - `TabNavigationTest`, with and without approximate location, and `AppGraphTest`.
    - **iOS:**
      - the new `FeaturePlannerTests` target: ViewModel tests, and snapshots of both states in light, dark and large text;
      - `ShellUITests.test_lb301_…`, without location;
      - `PlannerViewTests`: the view reads the origin by itself once in a window. It fails without the view's `onAppear`, which the ViewModel tests and snapshots call for themselves;
      - the accessibility audit now waits for the fields, and walks the root in the dark walk too.
  - **CI:** `:android:feature:planner` joins the Android screenshot matrix, and `FeaturePlannerTests` the iOS package matrix.
  - **Docs:**
    - the backlog splits LB-301 in two, and the acceptance criteria record part 1's evidence;
    - `design_guidelines.md` §3.3 describes the fields, and §9 lists the new keys `plan.origin.hint`, `plan.origin` and `plan.destination`;
    - `architecture.md`, `ci_cd.md` and `testing_strategy.md` §4.7 are updated, as are `AGENTS.md`'s status and both READMEs.

- **Review:** the four reviewer agents approved with nits, all fixed:
  - a KDoc's section citation;
  - a test comment over 120 characters;
  - the deprecated `UITraitCollection(traitsFrom:)` in the new snapshot test;
  - the missing test of the view's own read (`PlannerViewTests` above);
  - `security_privacy.md`, which now says How to Get There reads the permission only.
  - The general review also found a blocker: `SharedDependencies` had 12 functions, over detekt's `TooManyFunctions` limit of 11, which fails `./gradlew check`. Its use cases now come from one object per feature, by delegation, as `PreviewUseCases` already does; the limit stays.

**CI**
- **The first run of PR #51 (42141341518) failed** on the runner flake of PR #50: XCTest gave up on the largest text's audit of Lines, the launch's first screen ("Audit failed to complete in time", error -56). Every other job passed, the default size's and the dark audits included.
- **The retry added in PR #50 ran, and gave up too.** The audit gave up three times in a row, each attempt slower than the one before (6, 15 and 34 s), as if each new audit waited behind the one abandoned.
- **The fix:** after XCTest gives up, the audit waits before it runs again, 5 s and then 10 s. It still runs at most three times, and a third give-up still fails the test. The error cannot be provoked locally, so the fix is checked by the next CI runs; locally the audits still pass.
- **The second run (42148708782) got past Lines,** and then failed on a different false positive, on the "Edit favourite lines" sheet.
  - **What it reported:** at the largest text, the audit flagged the Dynamic Type of the sheet's system "Done" bar button, at (295.3, 67, 74.7 × 36). The test already excludes that issue, because the system caps a bar button's text.
  - **Why it failed anyway:** the exclusion matches a button whose frame equals a navigation bar button's frame, and this time it did not match. The screenshot shows the sheet settled.
  - **The same issue, locally:** a diagnostic run (not committed) reports it for the same frame, as a button. There the frame equals the first navigation bar button's frame, and the exclusion holds.
  - **Why it is a flake:** the sheet is outside this change, and it passed in every earlier run.
  - **What was done:** the jobs ran once more, in the run of the commit that records this (`AGENTS.md` §10). If it recurs, the cause of the mismatch is the next thing to find.

**Decisions**
- **The fields open nothing in part 1 (owner decision, 2026-11-06).** So they are plain elements, not buttons: a field that announced a button role but did nothing would mislead. They become buttons with place search, in part 2.
- **Copy:**
  - The empty origin's hint is "¿Desde dónde?" ("Where from?"), as a proposal beside the destination's "¿A dónde vas?".
  - The spoken names are "Origen" and "Destino".
  - iOS says them as a label with a value, Android as one description, "Origen, Mi ubicación".
- **The origin is read, not observed.** The permission can only change while the root is away: in Near Me, in the system's settings, or in the background. Reading it each time the root shows is enough, and needs no flow.

**Open / next**
- **This entry's pull request** is open, and its watch runs.
- **LB-301 part 2:** choosing a stop, a saved place, an address or a map point; it waits for LB-008 part 2, LB-311, LB-302 and LB-013.
- **Next, once it is merged:** LB-304 (swap origin and destination) and LB-303 (Leave now / Depart at), each in its own pull request, unless the owner chooses otherwise. Neither waits for OQ-10 or OQ-11.
- **Waiting for the owner:**
  - the iOS job limit;
  - Android's end-to-end encrypted backups;
  - DD-10 provisional stops;
  - alert 6 (Kotlin Gradle plugin);
  - the Gradle cache provider;
  - OQ-10 and OQ-11.

## 2026-11-05: LB-201 part 1, the location permission flow

**Done**
- **PR #48 (LB-408 part 1) merged** on 2026-11-05, and `main` was synced. PR #49 (the Guava floors) had conflicts with it, so `main` was merged into its branch.
  - Its first run had already passed after one rerun of a Maven Central 429. Its run after this second merge lost a job to a runner flake: "iOS ViewModel and snapshot tests (DesignSystemTests)" never got a macOS runner and was cancelled after 35 min in the queue. The failed jobs were rerun once, as its description records.
- **LB-201 part 1 (`feat/LB-201-location-permission`):** FR-LOC-01 to 03 and FR-NEAR-06, on Near Me's root.
  - **Shared (`:shared:feature:nearme`):**
    - `ObserveLocationAccess` emits `Off` without permission, and starts no updates. With permission it emits `On` and keeps the provider's updates running while collected, then `Off` when location goes.
    - `NearMeUiMapper` makes "Near you" the `TurnOnLocation` row or `LocationOn`, and `Unknown` until the first read, so the row never flashes.
    - The graph binds the use case, `SharedDependencies` hands it to Swift, and `preview()` scripts location with `locationOn()` and `locationOff()`.
  - **Android (`:android:feature:nearme`, `:android:app`):**
    - `NearMeViewModel` (`WhileSubscribed(5 s)`) and `NearMeRoute`, now the Near Me tab's root.
    - The row asks for coarse and fine location together. Once the system no longer asks, it opens the app's settings page.
    - The manifest declares the two location permissions, and a test reads the merged manifest. `BackupTest` now reads it through the same helper.
  - **iOS (`FeatureNearMe`, the app):**
    - `NearMeViewModel` and `NearMeView`, now the tab's root.
    - `CoreLocationAuthorization` shows the When In Use prompt, or opens Settings after a denial, behind a protocol that the tests fake.
    - The view observes only while it shows and the scene is active.
    - The purpose string ships in Spanish in the build settings, and in Spanish and English in `InfoPlist.xcstrings`; a test keeps the two Spanish texts equal.
  - **Tests:**
    - Shared: provider fakes for the updates, and the mapper.
    - Android: the ViewModel, including the 5 s stop; the route, through a recording `ActivityResultRegistry` and Robolectric's rationale shadow; the screen; screenshots with `assertAccessible`.
    - iOS: Swift ViewModel tests with a fake authorization, snapshots, and `AppBundleTests` for the purpose string. The accessibility audit already walked the Near Me tab.
  - **CI:** `:android:feature:nearme` joins the screenshot matrix, and `FeatureNearMeTests` the iOS package matrix and the scheme.
  - **Docs:** the LB-201 criteria are split into parts, with part 1's evidence. Also updated: `architecture.md` §5, `design_guidelines.md` §3.2 and §9, `security_privacy.md` §3, `ci_cd.md` §3, and the backlog.

**Reviews** (code-reviewer, data-rules-reviewer, android-ui-reviewer, ios-ui-reviewer). Changes were required, and all are fixed:
- **detekt failed the Kotlin gate:** `PreviewUseCases` had 14 functions, three over the limit. Near Me's preview moved to `PreviewLocation`, which `PreviewUseCases` delegates as it does `PreviewSettings`. `locationOn()` and `locationOff()` became extensions, and the scripted updates end on a count of revocations.
- **Android:**
  - An answer with no grants (an interrupted request, or a second request while one was pending) counted as a denial, and opened the settings with the prompt still on screen. It is now a cancellation.
  - A tap while the prompt shows does nothing.
  - A device without the app's settings screen no longer crashes.
  - A test now pins the saved rationale across a rotation.
  - The previews are light and dark, and at 200 % font scale.
- **Shared:**
  - A platform service that fails with the permission granted showed "Turn on location", a row that could never work. It is now `Unavailable`, which shows nothing until LB-203 explains it.
  - The fake counts its calls before its guard, so the no-permission test can fail.
  - The fake now emits a fix, which nothing passes on.
  - `IosGraphTest` lists the new accessor.
- **iOS:**
  - Xcode's accessibility audit reported the row's text as clipped, although it showed whole. The text now takes the row's whole width, in an `HStack` instead of a `Label`.
  - The list keeps the grouped background with the section and without it.
  - The observing task also needs the view on screen (`onAppear`/`onDisappear`), so a pushed screen or another tab cannot restart it.
  - The previews use a preview authorization, in light and dark.
  - The audit waits for the row and now visits Near Me in dark mode. A shell UI test finds the row and no system alert, with location reset to never asked.
  - New tests: the granted branch of the row, and the two Spanish purpose texts kept equal.
  - The comment on `.restricted` is corrected: it opens Settings, where only the restriction's owner can lift it.
- **Process:**
  - The earlier green commits say "No refactor needed" rather than the `TDD: refactor not needed` marker; the later ones use the marker.
  - One green commit (ef2c22b) also changed its red test. That test's fake answered from a queue, which the resume effect's extra read emptied, so the fake now answers with the current permission.
- **Left for later:**
  - one `SectionHeading` in the design system for the three copies of the section heading (LB-203);
  - the tab root's collapsing bar and scroll to top on reselection, which come with the map (LB-202).

**CI**
- **The first run of PR #50 failed** on a runner flake. XCTest's accessibility audit at the largest text gave up on the first screen it audits, Lines ("Audit failed to complete in time", `com.apple.xcode.xctest.accessibilityAudit` error -56, after 16 s). The failed jobs were rerun once, and passed.
- **The second run (42100639495), after `main` was merged in, failed in the same test class.** The default-size audit reported the Dynamic Type of Settings' picker values, "System" and "Minutes", on the screen audited as "Settings, dark".
  - **That screenshot was light, and so were all six "dark" screenshots of both runs.** On CI the running app stayed light when the test turned the device to dark mode, so the dark audits audited light mode a second time without saying so. Locally the app turns dark.
  - The light Settings audit earlier in the same walk passed, so the Dynamic Type report is the audit's own variance on one screen. If it recurs on a dark screen, it is checked in its screenshot like the other exclusions.
  - **The check:** each dark audit checks that its screenshot is mostly dark (306a3dc, red). A local run with the device left light failed on all six screens.
  - **First fix, which did not hold (cbcf303):** relaunching the app once the device is dark, and waiting for a dark screen. It passed locally, but in the third run (42114144578) all six dark audits reported a light screen while `XCUIDevice` read dark. Under `xcodebuild` the simulator ignores `XCUIDevice.appearance`, as others report of iOS 26.2 and later.
  - **Second fix (225ceff):** the app relaunches in its Dark theme, which a launch argument sets over the saved one, and which does not depend on the simulator. In it the audit reports the contrast of the system's search field and of the Clear cache alert's title as failing, although the screenshots show both white on dark. Both are named false positives, on those two screens only. CI had never audited them in dark mode, since its dark audits ran in light.
- **The audit timeout had happened before.** `main`'s run 42011966908, earlier on 2026-11-05, hit it on the default-size launch's first screen, Lines; this entry first said that no earlier run had. As a repeated flake, it now makes the audit run again, up to three times (5732346, `AGENTS.md` §10).

**Decisions**
- **Owner (2026-11-05):** until the map and the sheet land, Near Me's root is the "Near you" section only. The row shows without location; with location the screen shows nothing, and reads location only while it shows.
- **Agent:**
  - **"The system no longer asks" on Android:** a denial with no rationale before or after the prompt. This needs no stored flag, and it also holds after process death. A first prompt dismissed without an answer looks the same and opens the settings too, where location can be turned on.
  - **Reading location access again:** only while the row shows, after each answer and on resume. Until access is known, the read under way already gets the latest permission. While location is on, its flow ends by itself if location goes, so reading again would only restart the updates.
  - **The purpose string** names both uses of location in the MVP: nearby stops, and trips from "My location" (LB-301).

**Open / next**
- **This entry's pull request (#50)** is open, and its watch runs. PR #49 merged on 2026-11-05, and `main` was merged into this branch.
- **LB-201 part 2** comes with LB-202, LB-203 and LB-410, plus the device passes (`acceptance_criteria.md`).
- **Next, once both are merged:** the next P0 story that needs neither OQ-10 nor OQ-11, unless the owner chooses otherwise. Most of M1 and M2 waits for LB-003's fixtures (OQ-11) or the map (OQ-10, LB-013).
- **Waiting for the owner:**
  - the iOS job limit;
  - Android's end-to-end encrypted backups;
  - DD-10 provisional stops;
  - alert 6 (Kotlin Gradle plugin);
  - the Gradle cache provider;
  - OQ-10 and OQ-11.

## 2026-11-05: The Guava alert again: security floors for a test-support module

**Done**
- **Dependabot alert 7 (GHSA-xxph-c9ww-hj94, high, Guava up to 33.7.1) reopened on `main` on 2026-11-04,** after PR #43 (LB-406 part 1) merged. GitHub reported it on a push.
  - The cause: `:android:core:testing` declares Robolectric as a dependency of its own (`implementation`), so its nine classpaths resolved Robolectric's Guava 33.6.0-jre. The floors covered test configurations only, and this module's are not named as such.
  - The fix (`fix/LB-002-guava-floor-test-support`, two red and green cycles): a test-support module, as `ModuleRules` defines it, gets the floors and BOMs on its own dependencies, which its compile and runtime classpaths extend. Only tests may use such a module, so its classpaths are test classpaths.
    - An Android module declares them in `implementation`. A KMP one, such as `:shared:core:testing`, has no `implementation` configuration: a JVM library goes in `androidMainImplementation`, and its common and iOS configurations get none.
    - `securityFloorsFor(path, name)` decides what each configuration gets, and `raiseSecurityFloors` applies it, so the wiring is the tested decision.
    - `ModuleKind.isTesting` moved to `ModuleKind`, so `ModuleRules` and the floors use one definition.
- **Verified:**
  - An init script resolved every configuration of every project. Before the fix, nine configurations of `:android:core:testing` resolved Guava 33.6.0-jre; after it, none resolves a Guava older than 33.7.2.
  - The app's release runtime classpath is identical before and after.
  - `:shared:core:testing` gets the BOMs on its Android classpaths only, and still compiles for iOS.
- **The other open alert** is still alert 6, the Kotlin Gradle plugin's, which waits on the owner (`ci_cd.md` §6).

**Reviews** (the code-reviewer subagent; no app code changed). It approved with nits, all fixed:
- **`:shared:core:testing` was claimed but not covered:** it is a KMP module with no `implementation` configuration. It now gets the floors on `androidMainImplementation` (the second cycle).
- **The wiring of `raiseSecurityFloors` had no test:** a call without the module's kind would have dropped the BOMs, and with them Guava's floor, with every test green. It now applies `securityFloorsFor`, which the tests cover.
- **Wording:** "which all its classpaths extend" was too broad, since its own tests' classpaths get the floors otherwise. `ci_cd.md` read as if the alert were closed. The LB-002 backlog row records the extension.

**CI**
- **The first run after `main` was merged failed** in the `:shared:core:database` unit tests: Maven Central answered `429 Too Many Requests` for Robolectric's `nativeruntime-dist-compat-1.0.19.jar` in all three attempts of `gradle-retry.sh`. A change to the Gradle files starts with no cache, so every job downloads at once, as on PRs #36, #38 and #41. The failed jobs were rerun once, and passed.
- **The lasting fix,** a Gradle cache with a fallback key, still waits for the owner's choice of cache provider.

**Decisions**
- **The same floors as on 2026-11-02,** which the owner approved then, reach one more place. No new floor and no new version.

**Open / next**
- **This entry's pull request** is open, and its watch runs. PRs #47 and #48 merged on 2026-11-05, and `main` was merged into this branch after each. The alert should close once this branch reaches `main` and submits its dependency graph.
- **Next, once it is merged:** LB-201 part 1 (the location permission flow), unless the owner chooses otherwise, as the LB-408 entry says.

## 2026-11-05: LB-408 part 1, ETAs from the server's clock

**Done**
- **PR #44 (LB-407 part 1) merged** on 2026-11-05, and `main` was synced. Dependabot's PRs #45 and #46 (`gradle/actions` 6.4.1) were replayed as PR #47, which is open, with its own entry.
- **LB-408 part 1 (`feat/LB-408-server-clock-offset`):** FR-ERR-06, ETAs count from the server's clock.
  - The shared pieces were built earlier, in LB-004, LB-005, LB-010, LB-011 and LB-403:
    - LB-004: the server-corrected clock;
    - LB-005: the `Date` of every response feeding it;
    - LB-010: one clock bound as both `ServerCorrectedClock` and `AppClock` in the graph;
    - LB-011: `EtaText`, which shows `Minutes(n)` as "n min";
    - LB-403: `EtaDisplay.of`, which takes the now to count from.
  - `ServerClockOffsetTest` in `:shared:umbrella` wires the real source client, over a mock engine, to the clock by hand, on the JVM and the iOS simulator.
    - With the device clock 3 min ahead, an arrival the server expects in 5 min shows 2 min before any response, and 5 min after the first.
    - With the device clock 50 s ahead, an arrival 4 min 30 s away shows 3 min before, and 4 min after.
    - These are acceptance tests of existing behaviour, so they passed when written, with no red or green phase. Taking out the client's `Date` sampling, or the offset in `ServerCorrectedClock.now`, fails the first two; a 60 s threshold in `now` fails the third alone.
  - `:shared:umbrella`'s tests now use Ktor's mock engine (a test dependency, already in the catalogue).
  - **Part 2 comes with LB-205,** whose row and criteria now carry it (FR-ERR-06):
    - its rows count each ETA from the graph's `AppClock` on both platforms;
    - Swift reaches that clock, or the computed ETAs, through `SharedDependencies`;
    - a test runs through the graph, with a mock engine and a device clock off by 3 min.

**Reviews** (the code-reviewer and data-rules-reviewer subagents; no UI code changed, so no other review). Both asked for changes, all fixed:
- **Marked done with no ETA rendered anywhere,** and nothing in LB-205 carried the work. LB-408 is now part 1, in progress, as LB-406 and LB-407 are, and LB-205's row and criteria name FR-ERR-06.
- **The graph's wiring was claimed but not tested:** `GraphAssertions` shows one clock bound as both types, not that the graph's source client gets it. A `SourceClient` given its own clock in `Koin.kt` would pass every test. The docs now say so. The test through the graph needs a mock engine and a fake device clock in the graph, which LB-205's test needs too, so both come in part 2.
- **The "any size" decision had no test:** `lb408_anOffsetUnder60SecondsCorrectsTheEtaToo` pins it.
- **The arrivals sat on a minute's boundary** (exactly 5 min away), so a correction of a second would have flipped them: they are now 5 min 30 s away.
- **Wording:** detekt forbids direct calls to `Clock.System.now()` and `System.currentTimeMillis()`, which is narrower than "outside `AppClock`"; Swift has no such check, which part 2 adds if Swift gets the clock. `EtaText`'s tests use 4 min, not 5. The docs list the same earlier stories everywhere, and use the future tense for what the arrival rows will do.
- **Not changed:** the offsets are measured against the device's wall clock, so if it jumps mid-session (a time sync, or a manual change), ETAs are off by the jump until 3 fresh samples move the median, for some 10 to 60 s. Anchoring samples to a monotonic clock would fix it; neither FR-ERR-06 nor spec §8.6 asks for it, so it is noted for LB-205.

**Decisions**
- **The offset applies at any size,** as spec §8.6 has it, not only beyond FR-ERR-06's 60 s, which the requirement allows. Below 60 s it still matters: with the device clock 50 s ahead, an arrival 4 min 30 s away would show 3 min. On a device whose clock is synchronised, it adds about a second of noise (the `Date` header's whole seconds and the network's delay), well under a minute's rounding.
- **Split into two parts:** the arrival rows that count from the clock do not exist yet.

**Open / next**
- **The pull request of this entry** is open, and its watch runs. PR #47 merged on 2026-11-05, and `main` was merged into this branch.
- **Next, once it is merged:** LB-201 part 1, unless the owner chooses otherwise. That is the location permission flow, the manifest permissions, the iOS purpose string, and updates that stop when no visible screen needs them. Its map framing waits for LB-202 and OQ-10, and most other P0 stories wait for OQ-11 (LB-003) or OQ-10 (LB-013).
- **Waiting on the owner:** as in the LB-407 entry below.

---

## 2026-11-05: gradle/actions 6.4.1, Dependabot PRs #45 and #46

**Done**
- **PR #44 (LB-407 part 1) merged** on 2026-11-05, and `main` was synced.
- **Dependabot PRs #45 and #46** bump `gradle/actions/dependency-submission` (`kotlin-checks.yml`) and `gradle/actions/setup-gradle` (`.github/actions/gradle-setup`) from 6.4.0 to 6.4.1. Both are replayed as our own commits on `chore/deps-gradle-actions-6.4.1` (`engineering_workflow.md` §9), and Dependabot's PRs are closed with a link to its PR.
  - The new pin, `8da46b5`, is the commit that the annotated tag `v6.4.1` points to.
  - **v6.4.1 fixes GHSA-chpw-2mqm-95cc** (high): in `download-and-submit` mode, a fork's pull request could supply a crafted dependency graph and obtain the `GITHUB_TOKEN` of the `workflow_run` job that submits it. This repository generates and submits the graph in one job (`dependency-graph: generate-and-submit`) and has no `workflow_run` workflow, so it was not affected.

**Decisions**
- **One pull request for both bumps:** they are the same upstream release, in the two places that pin it.

**Open / next**
- **The pull request of this entry** is open, and its watch runs.
- **Next:** LB-408 (the server clock offset), on its own branch.
- **Waiting on the owner:** as in the LB-407 entry below.

---

## 2026-11-05: LB-407 part 1, favourites and settings in the OS backups

**Done**
- **PR #43 (LB-406 part 1) merged** on 2026-11-04, and `main` was synced.
  - **CI:** its first run failed in "Android unit tests (:android:core:map)", and so the `Kotlin gate`: the Gradle Plugin Portal did not serve `kotlin-dsl`'s plugin marker in any of `gradle-retry.sh`'s three runs, as on PRs #36, #38 and #41. The failed jobs were rerun once and passed. The lasting fix, a Gradle cache with a fallback key (`ci_cd.md` §5), still waits for the owner's choice of cache provider.
  - **The `iOS build and tests` job took 31 min 9 s** with the accessibility audits, against 17 to 23 min before them, so 3 min 51 s under its 35-minute limit. The audits add more on CI than the 6 to 7 minutes estimated. For the owner: whether to raise the limit again, or move the audits to a job of their own.
  - **`main`'s own iOS runs:** three of the last four failed outside the code, while the pull request runs of the same commits passed. After LB-110's and LB-405's merges, macOS jobs were never started ("The job was not acquired by Runner of type hosted even after multiple attempts", GitHub's macOS arm64 capacity); after LB-403's, the Gradle wrapper's download timed out. Nothing in the repository can make GitHub start a job, so this is recorded rather than fixed.
- **LB-407 part 1 (`feat/LB-407-favourites-persistence`):** the backups (FR-FAV-03, `architecture.md` §4.3).
  - **Android:** the manifest gives data extraction rules (Android 12+, a cloud backup and a transfer to a new device) and full backup content rules (Android 8 to 11). Both include only the database, to whose include Android adds its journal and write-ahead log (`FullBackup.BackupScheme`), the settings' DataStore file, and AppCompat's language file below Android 13. Before, with no rules, Android backed up every file.
    - `BackupTest` saves a favourite line and the theme through the app's graph, copies the files each rule set backs up while the app runs, restores them to an app with no data, and opens the graph again. The language goes through androidx.core's `AppLocalesStorageHelper`, which AppCompat uses. Robolectric's manifest parser predates Android 12 and leaves the data extraction rules out of `ApplicationInfo`, so the test reads the merged manifest the build gives Robolectric.
    - On a Pixel 9 Pro emulator (API 37), with `bmgr` and the local transport: line 3 saved, a stand-in tile cache in `files/`, a backup, the app's data cleared, a restore. Only the database and its journal came back, and the app opened with line 3 in "Your favourite lines". `force-stop` and `pm clear` leave an app stopped, and Android does not back up a stopped app ("Backup is not allowed"), so the app went to the background with Home instead. The emulator's transport and Backup Manager were set back as they were.
  - **iOS:** iCloud and device backups already kept the database, in Application Support and not excluded, and the settings, in the standard user defaults. `IosDatabaseTest.lb407_iosBacksTheDatabaseUp` pins the database's place, an acceptance test of what iOS already did, with no red or green phase. Marking the file or its folder excluded fails it; each mark was removed from the simulator afterwards.
  - **Part 2:** "No longer served" in "My stops" waits for saved stops (LB-204, LB-208) and the daily sync (LB-008 part 2); `updateMissingSyncs` already counts the missing syncs (LB-008 part 1).

**Reviews** (the code-reviewer and android-ui-reviewer subagents; no UI, shared production or iOS app code changed, so no other review; both approved with nits). Fixed:
- **The iOS test passed without a database:** the native driver creates the file only once something is written, and Foundation reads no exclusion for a missing file. It now saves a favourite, asserts the file exists, and checks it and its folder. Application Support itself is left out: these tests run outside an app's container, in the simulator's system folders, which iOS excludes.
- **The language test checked a copy of AppCompat's file name:** it now saves and reads the language through androidx.core's own storage, across a real restore.
- **The restore copied the files after the app had closed them,** so a journal could never matter: the copy is now taken while the app runs. Android adds a database's journal and write-ahead log to its include itself (`FullBackup.BackupScheme`), so the explicit lines went, and the test adds them as Android does. Each restore also checks a tile cache does not come back.
- **Docs:** `security_privacy.md` names the language file and records the encryption decision; the first criterion cites the restart tests and marks the update check manual, and the migrations are verified "once a release exists" (ADR-0013).
- **Not changed:** AppCompat never deletes its language file, so a phone updated to Android 13 may back up an older choice, which a new phone would take: rare, and noted in the rules.

**Decisions**
- **The database is backed up whole,** the public lines and stops it caches included. One file holds them and the favourites (ADR-0013), and splitting it would amend that ADR for a catalogue of 13 lines and about 254 stops. After a restore the sync refreshes them as usual.
- **Android includes files by name, not by domain:** a file a library adds later, such as MapLibre's tile cache in `files/`, stays out of the backup, and cannot use up Auto Backup's 25 MB, after which Android backs up nothing. A new file with the user's data must join both rule files, and `BackupTest` shows whether it does.
- **The language below Android 13 is restored too:** AppCompat keeps it in a file of its own (ADR-0020). From Android 13 the system keeps it and backs it up.
- **No encryption requirement on the cloud backup:** a user without a screen lock would get no backup at all, and lose their favourites on a new phone. Their backup is encrypted by Google on its servers, not end to end, and will hold saved places' coordinates once LB-311 saves them. For the owner to confirm (`security_privacy.md` §4).
- **Split into two parts:** the saved stops that "No longer served" labels do not exist yet.

**Open / next**
- **The pull request of this entry** is open, and its watch runs.
- **Next, once it is merged:** LB-408 (the server clock offset), unless the owner chooses otherwise.
- **Waiting on the owner:** the `iOS build and tests` limit (above); whether Android's cloud backup must be end-to-end encrypted (above); the fifth LB-407 criterion (provisional stops never count as missing); and as in the LB-405 entry below.

---

## 2026-11-04: LB-406 part 1, accessibility checks on every screen

**Done**
- **PR #42 (LB-405) merged** on 2026-11-04, and `main` was synced.
- **LB-406 part 1 (`feat/LB-406-accessibility-pass`):** automated checks that every screen built so far runs, and later screens join (`testing_strategy.md` §4.7).
  - **Android, `assertAccessible`:**
    - It lives in a new test-only module, `:android:core:testing`. `ModuleRules` keeps it out of production code, and it has a matrix job of its own in CI.
    - It reads the semantics tree that TalkBack reads: labelled controls, 48 dp touch areas (`touchBoundsInRoot`), and no cut text.
    - Every screenshot test runs it on each case in light and at 200 %: every state of the shell and its rail, More, Lines, line detail, the favourites editor, Settings and its language dialog, and About data. Dark repeats light, so it is skipped. ETAs and line badges run it at 200 % in both languages.
    - Every screen passed at once. Making Lines' names end in an ellipsis failed 9 cases, and was undone.
  - **iOS, `AccessibilityAuditUITests`:** two launches, at the default and the largest text, walk every screen and the Clear cache alert, and run Xcode's `performAccessibilityAudit()`, with a screenshot of each screen. The default launch then walks them again in dark mode.
    - It found the system's secondary grey (about 3.5:1) on Settings' and About data's headings, footers and values, and the system blue (about 4:1) of Clear cache.
    - Now the app's root tint is `Tokens.Colors.accent`, and those texts, with Lines' headings, use `Tokens.Colors.secondaryText` (`design_guidelines.md` §5).
    - `TokensTests` checks both on the system's backgrounds, raised ones and Increase Contrast included. `DesignSystem`'s `appTint()` sets the tint at the root and in every snapshot helper, so the re-recorded Lines, line detail and Settings references show the accent that ships. SwiftLint's `system_secondary_text` rule forbids the system's greys.
  - **Ahead of part 2:** the arrival rows' spoken label and polite announcements (LB-205, LB-213), the maps' list equivalents (LB-203, LB-104, LB-308), glass legibility (LB-202), and the device passes (`acceptance_criteria.md`).

**Reviews** (the code-reviewer, android-ui-reviewer and ios-ui-reviewer subagents; no shared code changed, so no data-rules review; all three asked for changes). Fixed in three TDD cycles:
- **Majors:**
  - **Android touch targets** were read from `touchBoundsInRoot`, the widened rectangle, while TalkBack gets that rectangle cut at the window's edge and where a later control takes the space. A probe showed a 24 dp control in a corner given 36 × 12 dp. `assertAccessible` now reads the view's `AccessibilityNodeInfo`.
  - **The iOS snapshots lacked the app's tint,** so the references showed the system blue the story removed. The tint is now `appTint()`, applied at the root and by every snapshot helper.
  - **A "Towards " label exclusion** would have hidden clipped text on any later screen, such as the direction toggle. Each exclusion now holds on its screen only, and the spoken-in-words texts are matched by accessibility identifiers.
- **Minors:**
  - Android: labels are checked off screen too, and for custom actions, expand, collapse and dismiss; a typed text is no label. Hidden nodes are skipped only as TalkBack skips them, all text is checked, and a container that does not scroll may not cut text.
  - iOS: the Dynamic Type exclusion holds for a navigation bar's buttons only, the search field's only at the largest text. Settings' opens-outside icon was 1.7:1, and now takes the tokens' grey. The Clear cache alert and dark mode are audited.
- **Not changed, or noted:**
  - The system's alert, its search field's 20 pt Clear text button, and the Dynamic Type of its capped alert text are excluded, each a system component, found when the alert and dark mode joined the walk.
  - The glass button style keeps a primary label, which the app's tint would otherwise colour. Map controls need the strongest contrast, and their legibility is part 2's.
  - No `secondaryTextStyle()` helper: it would only rename the 19 uses; the SwiftLint rule and the audit guard the colour.
  - Previews do not take the app's tint, since they are a development aid; snapshots do.
  - The commit bodies before the review wrote "Refactor not needed." rather than `TDD: refactor not needed`; they are pushed, so they stay.

**Decisions**
- **Our own Android checks, not the Accessibility Test Framework:** Compose UI Test's `enableAccessibilityChecks()` does nothing under Robolectric (its own documentation says so). A 12 dp unlabelled button passed it, even with Robolectric's accessibility turned on, and CI has no emulator. So `assertAccessible` reads the semantics tree directly.
  - It caught two false positives of its own, each fixed with a regression test. First, every text seemed to overflow, because the layout that semantics give lays a paragraph out at its widest. Second, a control scrolled off screen has an empty touch area.
  - It refuses to run under legacy graphics, which do not measure text.
- **What the iOS audit gets wrong is named in the test, each case checked in its screenshot.**
  - "Clipped" text whose spoken label says a symbol in words: removing the hint's label removed the issue.
  - The system search field's placeholder.
  - A navigation bar button's Dynamic Type.
  - The Dynamic Type of Settings' Data heading and footer: they scale at the largest text.
- **Android touch targets as TalkBack gets them:** from the `AccessibilityNodeInfo` that Compose builds for the view, which works under Robolectric without an accessibility service. A control that a scrolling ancestor shows only in part is measured once scrolled to.
- **Contrast is audited where nothing scrolls under the floating bars.**
  - iOS 26's scroll edge effect fades what is under the tab bar and the navigation bar, and the audit sometimes reports those issues without an element. So they cannot be excluded by position: excluding unnamed issues once hid real ones in Settings.
  - The Lines list is audited for contrast while a search keeps it short. The largest size is audited without contrast: all its text is large, with a 3:1 threshold.
  - An audit with an unnamed issue runs again, up to three times, while the screen settles.
- **The iOS tint at the app's root, and secondary text set where it is used:** a root `.foregroundStyle(.primary, secondary)` fixed the grey in one place, but turned Clear cache and Edit black, so they no longer looked like buttons.
- **The `iOS build and tests` job's limit is 35 minutes** (owner decision, 2026-11-04), not 25. The audits add about 3.5 minutes locally and 6 to 7 on CI, and the job already took 17 to 23 (`ci_cd.md` §5). A separate job and a nightly run were the alternatives.
- **Split into two parts:** the arrival, live and map screens that the other criteria need do not exist yet, so they come with them.

**Open / next**
- **The pull request of this entry, PR #43** (branch `feat/LB-406-accessibility-pass`), is open, and its watch runs. Its run is the first to show the `iOS build and tests` job with the audits, against the new 35-minute limit.
- **Next, once it is merged:** LB-407 (favourites in OS backups; "no longer served" after 7 missed syncs), whose backup part waits for nothing, unless the owner chooses otherwise.
- **Waiting on the owner:** as in the LB-405 entry below.

---

## 2026-11-04: LB-405, localisation

**Done**
- **PR #41 (LB-403) merged** on 2026-11-04, and `main` was synced. **CI:** its first run failed in "Android unit tests (:android:core:designsystem)", and so the `Kotlin gate`: the Gradle Plugin Portal did not serve `kotlin-dsl`'s plugin marker in any of `gradle-retry.sh`'s three runs, as on PR #36 and #38. The failed jobs were rerun once and passed. The lasting fix, a Gradle cache with a fallback key (`ci_cd.md` §5), still waits for the owner's choice of cache provider.
- **LB-405 (`feat/LB-405-localisation`):**
  - **The check of the "Done when":** `LocalisationTest` in `:tools:architecture-tests` fails on user-facing text written into UI code, on both platforms (`code_style.md` §1). Its rules, unit-tested in `UiTextRulesTest`, read the code through small lexers of their own for Kotlin and Swift (`SourceLexers.kt`), which blank comments and strings, and a model of the calls whose arguments hold text (`TextSinks.kt`): every top-level literal with a letter of such an argument fails on Android, and on iOS every one that is not a catalogue key, verbatim text included. Previews are exempt.
  - **The iOS catalogues:** every entry translated into Spanish and English, plural forms included; every key the code uses in its target's catalogue, arguments included; every dotted key in use; and a package target's keys looked up in its own bundle. Android lint's `MissingTranslation`, an error, already checked `values-en/` (shown failing on a dropped string).
  - **CI:** `changed-areas.sh` runs the Kotlin jobs when a Swift source or a string catalogue under `iosApp/` changes, since the localisation tests read them (`ChangedAreasScriptTest`). The test task declares them as inputs, so Gradle reruns it.
  - **Android:** the app's strings already fell back to Spanish from `values/`, but on a French device Material's and Android's words, the plural rules and the date formats were French. `MainActivity` now attaches in the device's languages that the app speaks, in their order, or else in Spanish with the device's region and regional preferences, and makes that the default locale (`AppLocales.kt`, `AppLocalesTest`).
  - **iOS:** with French alone, iOS gives the app its development language, Spanish, as Android does, the system's own words included. But the lookups that take a locale, the tab labels' `LocalizedStringResource` and DesignSystem's strings, matched English for the device's `fr_FR`: English tabs and line badges over Spanish screens, found when the UI tests began checking the package targets' words. The root view now gives SwiftUI `Locale.app`, the app's language in the device's region (`AppLocaleTests`), and two UI tests pin the result: French gives Spanish everywhere, "French, then English" gives English. The favourite card joins its headsigns through FeatureLines' bundle, so the not-translated entry Xcode extracted there is the one used.
  - **Met early:** the plurals (LB-011: the spoken ETAs and delays, tested at 1 and 4 minutes in both languages) and the Madrid times (LB-010 part 2, `lb405_…`).
  - **iOS test runs:** on the iPhone 17e, iOS 26.5.
  - GitHub answered pushes with "Internal Server Error" several times today; each went through on a retry.

**Reviews** (the code-reviewer, android-ui-reviewer and ios-ui-reviewer subagents; no shared code changed, so no data-rules review; all three asked for changes). Fixed in four TDD cycles:
- **Majors:**
  - Words returned by a member typed `LocalizedStringKey` or `LocalizedStringResource`, the codebase's own pattern, passed both Swift rules. They are now text, as are initial values and default parameters of those types.
  - A preview with an expression body exempted the declaration after it. An expression body now exempts nothing, and a function or type is a preview by its name only when private.
  - A package target's key given to `Button`, `.navigationTitle`, a search prompt or `Text` without `bundle:` is looked up in the app's bundle and shows the raw key: `unbundledSwiftKeys` and `lb405_packageTargetsLookTheirKeysUpInTheirOwnBundles` now catch it.
  - Below Android 13, choosing "System" in Settings lets AppCompat apply the device's locale to the activity until the app restarts. Giving the application Spanish resources (an override of its `getResources()`) made every other context's configuration context come out in the device's locale under Robolectric, so it was dropped; ADR-0020 records the case and the alternative, the owner's call.
- **The iOS UI tests only checked the app target's words** (a minor): checking a row and the Lines title too found the mixed-language screens above, now fixed.
- **Minors:** only the literal right after a call's parenthesis was checked: now every top-level literal of the argument, in ternaries, elvis operators, concatenations and interpolations. Positional descriptions (`Icon(x, "…")`), semantics actions' labels, `append`, toasts after a nested first argument, notifications, and on iOS announcements, map pins, date pickers, input labels, custom content, share links, UIKit's text properties and the widget and App Intents titles are sinks. Keys keep their arguments, so a key that drops one no longer matches. Format specifiers are not letters, and `text =` counts only in text calls. The default locale and the device's regional preferences follow the fallback. A plain Kotlin string ends with its line. The iOS UI tests take a list of languages.
- **Not changed, or noted:**
  - A literal given to a component of ours is not seen by the check: components take a `Text`, as `FavouriteToggle` does (`code_style.md` §1).
  - A literal passed as an argument of `stringResource(…)` inside `Text` belongs to the inner call, and is not checked.
  - TalkBack and VoiceOver read the fallback language with the device's voice: for the device check.
  - The green commit b4e9c88 also moved the Kotlin lexer into `SourceLexers.kt`, a refactor, while its body says none was needed. It is pushed, so it stays, and the PR says so.

**Decisions**
- **Each platform's own fallback** (owner decision, 2026-11-04), with no hack such as writing `AppleLanguages` into the app's defaults. The question rested on a misreading: the English that a French device saw on iOS came from the class method `Bundle.preferredLocalizations(from:forPreferences:)`, which knows no development language, and from the app's own lookups with the device's locale. iOS's own rule is the development language, Spanish, so both platforms fall back to Spanish and the acceptance criterion stands as written (ADR-0020).
- **The check is a test, not a detekt plugin or a SwiftLint regex:** one place for both platforms, unit-tested, and run by the Kotlin jobs on Linux, which a SwiftLint custom rule could not be.

**Open / next**
- **The pull request of this entry, PR #42** (branch `feat/LB-405-localisation`), was merged on 2026-11-04. **CI:** its first "PR title" check failed. GitHub answered the first `gh pr create` with a 502 but still opened the PR, with a title one character over the 100-character limit; the title was shortened at once, and this log's commit runs the check again, since editing a title triggers no workflow and a rerun replays the old one.
  - **GitHub lost that push:** an incident from 10:38 UTC (pull requests, Git operations and Actions) left the PR on b0252d1 for over three hours while the branch was on 46d574c, and no run started for it. The next push, this note's commit, brings the PR up to date.
  - **A runner flake outside the change:** in that first run, "Android unit tests (:android:feature:settings)", and so the `Kotlin gate`, failed in its 22 screenshot tests. Robolectric's own fetcher (`MavenArtifactFetcher`, at test time, not through Gradle) could not download the SDK 36 jar from Maven Central (an `IOException`). "Android screenshots (:android:feature:settings)" downloaded the same jar in the same run and passed, and this module's code is unchanged. It is the first failure of that path, so the new run is its one retry. `gradle-retry.sh` rightly does not retry it, since it sees failing tests. If it recurs, Gradle could resolve Robolectric's SDK jars, with its retries and cache, and Robolectric could run offline (`ci_cd.md` §5).
- **To check on a device:** a French device on Android 12 and on 13 or later (Material's words, dates), and on iOS (Spanish, the system's words included); TalkBack and VoiceOver; plus LB-403's checks.
- **Next, once it is merged:** LB-406 (accessibility pass), which waits for nothing, unless the owner chooses otherwise. LB-404's remaining parts (data source, disclaimer, attributions) touch the tile licensing of OQ-10.
- **Waiting on the owner:**
  - Below Android 13, whether to keep the language choice in the app's own settings, so that "System" never brings the device's locale back during a session (ADR-0020).
  - Android's "Undo" after removing a favourite line (LB-110).
  - The Gradle cache provider, and the Kotlin Gradle plugin's Dependabot alert.
  - OQ-10, OQ-11 and OQ-16, DD-10, and WorkManager, as in the entries below.

---

## 2026-11-03: LB-403, Settings

**Done**
- **PR #40 (LB-110) merged** on 2026-11-03, and `main` was synced.
- **LB-403 (`feat/LB-403-settings`):**
  - **Shared:**
    - A new core module, `:shared:core:preferences`, with Multiplatform Settings 1.3.0 (ADR-0018, its first use) and AndroidX DataStore 1.3.0 on Android. `PreferencesStore` keeps `UserPreferences` (`ThemePreference`, `EtaFormat`) under `settings.theme` and `settings.eta_format`.
    - Reads that fail give the defaults and are retried, and writes that fail are `Failure(Data)`. Android's DataStore file restarts from the defaults when damaged, and its scope ends with the graph's. iOS keeps the settings in the app's user defaults, and `savedPreferences(defaults:)` reads them at once.
    - `EtaDisplay.of(expected, now, format)` applies spec §5.7's display rule, Now, minutes or the Madrid clock time, and in the clock-time format every ETA as the clock time. It was pulled forward from LB-205, which will build its arrivals with it.
    - `:shared:feature:settings`: `ObservePreferences`, `SetTheme`, `SetEtaFormat`, `ClearCache` over the `AppCaches` port (Android's cache directory; iOS's `Library/Caches`, after the URL cache empties itself), and `SettingsUiState` with `AppLanguage`, `DataStatusUi` (the oldest last sync, never synced, or unknown) and `CacheMessage`.
    - `:shared:umbrella` binds them. Swift gets them through `SettingsUseCases`, which `UseCases` extends, and `PreviewUseCases` delegates them to `PreviewSettings`.
  - **Android:**
    - Settings has three sections. Appearance: the language, a row that opens a dialog of radio buttons, and the theme as radio rows under a heading. Arrivals: the ETA format with examples. Data: the data status, and Clear cache with what it keeps. A short snackbar says Clear cache's outcome.
    - The app is an `AppCompatActivity`. The language goes through AppCompat (`AppCompatLanguages`, bound by the shell), which the manifest declares with `localeConfig` and `AppLocalesMetadataHolderService`, and the bundle keeps every language.
    - The theme is the system's per-app night mode from Android 12 (`UiModeManager`). Below Android 12 it is AppCompat's night mode, with the saved theme read once per process before the first frame (ADR-0020).
  - **iOS:**
    - A `Form` of system controls. The language row, with an "open outside" icon, opens the app's page in the iPhone's Settings (owner decision). Theme and ETA format are menu pickers. Clear cache's outcome is an alert, and the first load is redacted and read as "Cargando ajustes".
    - `LogroBusApp` starts `AppAppearance` from the saved theme and applies its style to every window.
  - **Tests:**
    - Shared: `PreferencesStoreTest` and the platform stores' tests, `EtaDisplayTest` (CET, CEST and the end of summer time), the use cases, `SettingsUiMapperTest`, the caches on both platforms, `PreviewUseCasesTest`, and both graphs.
    - Android: `SettingsViewModelTest`, `SettingsScreenTest`, `AppThemeTest`, `AppLanguagesTest` and `AppSettingsTest`.
    - iOS: `SettingsViewModelTests` and `AppAppearanceTests`, `ShellSnapshotTests.lb403_settings` (loading, content, unknown status) and `ShellUITests.test_lb403_…`.
    - Screenshots on both platforms in light, dark and 200 %, and the language dialog in landscape at 200 %.
  - **iOS test runs:** the iPhone 17e simulator, `scripts/ios/test.sh`'s default and CI's, was missing on this machine. It was created again on iOS 26.5 (`xcrun simctl create`), and More's references pass on it, so LB-110's differences came from the 17 Pro Max.

**Reviews** (the code-reviewer, data-rules-reviewer, android-ui-reviewer and ios-ui-reviewer subagents; the data reviewer approved with nits, the other three asked for changes). Fixed in three TDD cycles, test first where a test could show it:
- **Majors (Android):**
  - The blocking theme read ran at every recreation. It now runs once per process and only below Android 12, as the one `runBlocking` outside tests (owner decision, 2026-11-03; `code_style.md` §2.2).
  - Settings kept showing an old language after Android's own settings changed it; `MainActivity` now refreshes it.
  - Play's language splits could leave English out of an install; they are off.
  - The language dialog did not scroll, which could cut it at 200 % in landscape.
- **Shared:**
  - A read that failed once ended the settings' flow.
  - The settings' keys were bare in iOS's shared user defaults.
  - The cache message is mapped by `SettingsUiMapper`, not by each ViewModel.
  - The store and the caches are internal behind their factories. The iOS store takes the injected dispatcher, and the Android factory takes its scope.
  - KDocs: the clock-time format, the arrivals the list drops, and that the source client keeps no HTTP cache.
- **Android minors:**
  - From Android 12 the starting window follows the theme (`UiModeManager`).
  - The theme tests prove the per-version paths and the single read.
  - The radio groups are named by headings.
  - Previews and screenshots: the unknown status and the language dialog.
  - Nits: resource names that mirror §9's keys, an exhaustive state, a pinned top app bar.
- **iOS minors:**
  - The first frame is in the saved theme.
  - VoiceOver hears "Cargando ajustes" on the first load.
  - The language row shows that it leaves the app.
  - The UI test no longer depends on what the device saved, and sets the theme back even when it fails.
  - The window style is tested.
  - Two quick themes land in order (a regression guard).
  - The large-text snapshot shows the whole screen.
  - Previews for never synced and unknown.
  - Nits: the language names in the catalogue, regional localizations, and snapshots named after LB-403.
- **Not changed:**
  - The ten green commits that say "Refactor not needed." in prose were not reworded, which would take a force-push; the PR's TDD section says so.
  - `EtaFormat` stays in `:shared:core:preferences`, which `:shared:core:presentation` now uses; `architecture.md` §3.2 records the edge.
  - VoiceOver reads "English" in the device's voice: SwiftUI has no speech-language attribute for a `Text`, while TalkBack reads it in English.

**Decisions**
- **iOS's language stays in the system's Settings** (owner decision, 2026-11-03, ADR-0020). The app shows the language it runs in and opens its page there. "Without a restart" holds on Android only, and the criterion says so.
- **Settings in three sections, with no new navigation** (owner decision, 2026-11-03): the data status is text, and About data stays in More.
- **The one `runBlocking` outside tests** (owner decision, 2026-11-03): below Android 12, the saved theme before the first frame, once per process. From Android 12 the system applies the theme.
- **Radio rows, not segmented buttons, on Android, and menu pickers on iOS:** segmented controls cut their labels at 200 %.
- **Clear cache empties the platforms' cache directories.** The MVP keeps no live data on disk (ADR-0008), and the database (favourites, places, recents, static data, the learned direction codes) and the settings live elsewhere.
- **No new Robolectric SDK:** code that differs by Android version takes the version as a parameter, rather than make every CI job download another SDK from Maven Central.

**Open / next**
- **The pull request of this entry** (branch `feat/LB-403-settings`), PR #41, was merged on 2026-11-04.
- **To check on a device:**
  - Switching the language on both platforms: Android at once; iOS through the iPhone's Settings, which relaunches the app.
  - The theme from the first frame and in sheets and alerts.
  - TalkBack and VoiceOver in Settings and in the language dialog.
- **Next, once it is merged:** LB-405 (localisation: plurals, locale formatting, and a check against hard-coded strings), which waits for nothing, unless the owner chooses otherwise. LB-104 waits for OQ-10, LB-102 part 2 and LB-205 for LB-008 part 2, and LB-008 part 2 for LB-003's fixtures and OQ-11.
- **Waiting on the owner:**
  - Android's "Undo" after removing a favourite line (LB-110).
  - The Gradle cache provider, and the Kotlin Gradle plugin's Dependabot alert.
  - OQ-10, OQ-11 and OQ-16, DD-10, and WorkManager, as in the entries below.

---

## 2026-11-03: LB-110, editing the favourite lines

**Done**
- **PR #39 (LB-102 part 1) merged** on 2026-11-03, and `main` was synced.
- **LB-110 (`feat/LB-110-edit-favourite-lines`):**
  - **Shared:** `FavouriteLinesRepository.add` answers `FavouriteAddition` (added, already saved, or `LIMIT_REACHED` at `FAVOURITES_PER_TYPE`, 20), counting in the insert's transaction; `reorder` writes every position in one transaction (`updateFavouritePosition`), so the carousel gets one emission, with the lines it leaves out after the named ones and ids no longer saved skipped. `SetFavouriteLine` answers `FavouriteToggle` (`Done`, or `LimitReached(limit)`) and counts no `favourite_add` beyond the limit; `ReorderFavouriteLines` is new, bound in Koin and in `SharedDependencies`, and `PreviewUseCases` keeps the limit and the order as the database does. `LinesUiState.Content.editable` lists every saved favourite, `Served` or `NoLongerServed`; `LinesEvent.ReorderFavourites` and `FavouriteLimitShown`, and `LineDetailEvent.FavouriteLimitShown`.
  - **ViewModels, both platforms:** the reorder event goes to the use case; the limit a toggle reached is held beside the state (`favouriteLimit`) until the screen says it has shown the message.
  - **Android:** "Editar" on the favourites heading (spoken "Editar líneas favoritas") while there is a favourite to edit, a line no longer served included; it opens `FavouriteLinesSheet`, the design system's new `ModalSheet` (which keeps Material's experimental sheet API in the design system) over `FavouriteLinesEditor`: a `LazyColumn` of rows with a remove button, the badge and name (stacked at large text) or "Línea que ya no circula", and a Reorderable drag handle. The sheet keeps its own order during a drag and sends it when the drag ends; "Subir" and "Bajar" are custom actions of each row. The limit message is a long snackbar with a close button, on the list and on the line detail (`FavouriteLimitMessage`). Reorderable 3.1.0 is in the version catalog (ADR-0019).
  - **iOS:** the same "Editar" in the section's header; a `.sheet` with `FavouriteLinesEditor` (title and "Listo") over `FavouriteLinesList`, a `List` in edit mode with `.onMove`, `.onDelete` and the "Subir"/"Bajar" accessibility actions; the limit is an alert (`favouriteLimitAlert`), on the list and on the line detail.
  - **Tests:** `FavouriteLinesRepositoryTest`, `SetFavouriteLineTest`, `ReorderFavouriteLinesTest`, `LinesUiMapperTest`, `PreviewUseCasesTest`, the graph test (Swift gets the new use case), both platforms' ViewModel tests, `FavouriteLinesEditTest` on Android (Edit, the sheet's order, remove, a line no longer served, TalkBack's moves, a drag of the third line to the top, the sheet following the database, both snackbars), screenshots of the sheet on both platforms (light, dark, 200 %) and the favourites screenshots re-recorded with "Editar", and `ShellUITests.test_lb110_…` on iOS end to end.
  - **Kover:** `:shared:favourites:domain` had no executable lines until `FavouriteAddition`, so its 80 % floor now applies; `FavouriteAdditionTest` covers the outcomes and the limit.
  - **iOS test runs:** the default simulator of `scripts/ios/test.sh`, the iPhone 17e on iOS 26.5, is no longer on this machine, so the tests ran on the iPhone 17 Pro Max on iOS 26.5 (`IOS_SIMULATOR`). There, every test passed except six `ShellSnapshotTests` of More and Settings, which this branch does not touch: the 17 Pro Max draws their title about 16 pt lower than the 17e their references come from. They were not re-recorded; CI runs the 17e.

**Reviews** (the code-reviewer, data-rules-reviewer, android-ui-reviewer and ios-ui-reviewer subagents; the data reviewer approved with nits, the other three asked for changes). Fixed, test first where a test could show it:
- **A blocker:** detekt with type resolution (`ciDetekt`, CI's) found `PreviewUseCases` one function over its limit; the collection counter is now a top-level function. The detekt run without types had passed.
- **Shared:** a damaged row (a ref that is not a number) no longer counts towards the limit, since the user could never remove it; ids are compared as numbers, so a ref saved as "07" is line 7, and `add` reports from the insert's row count; the preview fake fails when `failing()` is scripted and saves atomically; the test fake drops repeated ids, as the repository does. Regression guard: a new order that names a line twice keeps it once.
- **Android, a major:** removing a line took TalkBack's focus with it. The focus now moves to the next line's remove button, else the one before, else Done. Minors: Done gives the focus back to "Editar"; with the last favourite removed the sheet shows the carousel's hint; each row says its place ("2 de 3"), so a move is heard; the favourites heading keeps "Editar"'s height without it, in the skeleton too, so the list no longer moves 8 dp (the Lines and shell screenshots were re-recorded); the limit message counts as shown even when the screen leaves before it ends, so it never comes back; "Line no longer in service" became "Line that no longer runs" in English, apart from the "Not in service" chip. Regression guards: a row is read as its line and name without the handle, and a change from the database during a drag waits for the drag to end.
- **iOS, a major:** "Editar" had a touch target of about 20 pt; it has 44 pt now (the favourites snapshots were re-recorded). Minors: new orders queue with the toggles' writes, as SKIE may start two calls in either order; "Move up" and "Move down" are a pure function with its test; the sheet follows whole rows, not only ids; the empty sheet shows the hint; each row says its place; the limit alert keeps its number while it closes; a stray `Package.resolved` in the Swift package was removed and is ignored; the string catalogue is back in the repository's format, and Xcode's extracted `"%@, %@"` is marked as not translated; a UI test's doc comment is back above its function. Regression guards: two quick new orders leave the last one, and a move past either end changes nothing.
- **Not changed, for the owner or the device check:**
  - On Android a line is removed with one tap, and adding it again puts it last; on iOS the system asks for a second tap. An "Undo" snackbar would keep its place: the owner's call.
  - The sheet's pane title is Material's generic "bottom sheet", the dragged row's elevation is a feature constant without animation, and TalkBack reads a row before its remove button: to look at in the TalkBack pass.
  - Android's screenshots draw the sheet's content without the modal window; the M3 window adds only its container and drag handle.
  - A reorder that fails, a damaged database the repository has logged, leaves the sheet showing the new order until it reopens; the ViewModels say so, as they do for the toggles.

**Decisions**
- **A modal sheet from "Edit"** (owner decision, 2026-11-03), rather than editing the carousel in place: a vertical list is easier to drag, to read with a screen reader and to fit at 200 %, and it has room for lines no longer served, which have no card. It adds no navigation route. `design_guidelines.md` §2.2 (SC-15) and §3.1 say so.
- **Reorderable for Android's drag** (owner decision, 2026-11-03, ADR-0019), rather than a drag of our own on `LazyListState`.
- **A line no longer served is "Línea que ya no circula"**: the device keeps only its id, and the catalogue no longer has its code or name. It counts towards the limit, since the repository counts every saved line.
- **The limit message:** a snackbar on Android, an alert on iOS, each platform's way to say why an action did nothing; one sentence, the same in both: "Ya tienes 20 líneas favoritas, el máximo. Quita alguna para añadir otra."
- **The order is saved once per drag**, when it ends, so the database sees one write and the carousel one change; a screen reader's move saves at once.
- **iOS's snapshot of the sheet takes the list without its navigation bar:** rendered off screen, iOS 26's bar drew the inline title and Done in fixed colours whatever the appearance (a white title in light mode, a black Done in dark). The UI test checks the title and Done.

**Open / next**
- **The pull request of this entry** (branch `feat/LB-110-edit-favourite-lines`) is open, and its watch runs.
- **To check on a device:** TalkBack and VoiceOver in the sheet, on iOS 26 and iOS 18. On iOS the system merges the delete control's symbol into the row, which reads "Remove, Line 3, …", labels the reorder control "Reorder Remove", and the "Subir" and "Bajar" actions sit on the row's content, not the cell: check that VoiceOver offers them on the element it focuses, that Delete names the line, and where the focus goes after a move. Accessibility Inspector on the simulator can show the first two.
- **For the owner:** an "Undo" after removing a line on Android (see Reviews).
- **Next, once it is merged:** LB-403 (Settings: language, theme, ETA format, data refresh status, clear cache), which waits for nothing, unless the owner chooses otherwise. It is the first story to persist a setting, so Multiplatform Settings enters the version catalog then (ADR-0018). LB-104 waits for OQ-10's map style terms, LB-102 part 2 for LB-008 part 2, and LB-008 part 2 for LB-003's fixtures and OQ-11.
- **Waiting on the owner:** the Gradle cache provider; the Kotlin Gradle plugin's Dependabot alert; OQ-10, OQ-11, OQ-16; DD-10; WorkManager; as in the entries below.

## 2026-11-03: LB-102, part 1: the Lines search by code, name and headsigns

**Done**
- **PR #37 (LB-002, Gradle's longer backoff for rate-limited downloads) merged** on 2026-11-03, then `main` was merged into LB-103's branch: the conflicts in both READMEs, `ci_cd.md` §5 and the log kept both changes.
- **PR #38 (LB-103) merged** on 2026-11-03, and `main` was synced.
  - **Its seventh run**, after the merge of `main`, failed in "Shared unit tests (:shared:core:network)", outside the change: Maven Central answered 429 for Robolectric's `nativeruntime-dist-compat-1.0.19.jar` in all three of `gradle-retry.sh`'s runs, about 8 minutes, despite PR #37's longer backoff. It is the jar of every earlier 429. The failed jobs were rerun once, and passed; the merge came before this note could reach LB-103's entry.
  - **For the owner:** downloads keep failing because a pull request that changes the Gradle files starts with no cache. A cache with a fallback key would fix it: the "enhanced" provider of `setup-gradle` (proprietary terms, free for public repositories), or a first-party `actions/cache` step beside the basic provider, which adds to the full 10 GB quota (`ci_cd.md` §5).
- **LB-102 part 1 (`feat/LB-102-lines-search`):**
  - **Shared (`:shared:feature:lines`):** `LineQuery`: a line matches when each word of the query begins a word of its code, display name or headsigns, folded by `searchWords` as the stop search folds them; a query with no letters or digits searches nothing. `LinesUiMapper.state(result, query)` gives `SearchResults` (the found rows in their groups, without the carousel) or `NoMatches`; with no lines or a failure the query changes nothing. `LinesEvent.Search` carries the field's text.
  - **ViewModels:** both keep the query and map the lines already read again, so a key never starts a load; a Retry keeps the query. Swift keeps the use case's last result for it.
  - **Android:** `SearchField` in the design system, a `TextField` in the search bar's shape and colours, with a 48 dp clear button and the keyboard's search key. The Lines list has it as its first item; Content, SearchResults and NoMatches share one branch, so the field keeps its text, focus and keyboard. NoMatches is a sentence in a polite live region and "Borrar búsqueda". The skeleton has the field's shape. The favourites carousel moved to `FavouriteLines.kt` for detekt.
  - **iOS:** `.searchable` under the large title, always shown, bound to the view model's query; NoMatches is a `ContentUnavailableView` with "Borrar búsqueda", announced to VoiceOver. The carousel moved to `FavouriteLines.swift` for SwiftLint.
  - **Tests:** `LineQueryTest`, `LinesUiMapperTest`, both ViewModels' tests, `SearchFieldTest`, `LinesScreenTest`, the screenshots of both platforms (search and no matches, in light, dark and large text; every Lines capture now has the field), and `ShellUITests` end to end on iOS.

**Reviews** (the code-reviewer, data-rules-reviewer, android-ui-reviewer and ios-ui-reviewer subagents; the code and Android reviewers asked for changes, the other two approved with nits). Fixed, test first where a test could show it:
- **Shared:** a code typed with a space or a dash ("B 1", "B-1") finds its line; regression guards for the words of a query matching different fields and for the `ñ`.
- **Android, two majors:**
  - The list ran under the keyboard, the app's first text field: `MainActivity` has `adjustResize`, and the list pads by the keyboard's insets (`LinesScreenTest` dispatches a keyboard's insets; `MainActivityTest` checks the mode).
  - The no-matches sentence was a live region on a new node, which TalkBack never hears: it is now the title of a pane that appears, which Compose announces (`AndroidComposeViewAccessibilityDelegateCompat` sends `PANE_APPEARED` for a new pane title).
- **Android, minors:** the field keeps its prompt as its spoken name once it has text, as M3's does; clearing it, or Clear search, leaves the focus in the field; the skeleton's field is the design system's `SearchFieldPlaceholder`, as tall as the field at 200 %; the carousel's move had left eleven unused imports. Regression guards: a restored field sends its text again, the field keeps its focus as the results change, and the ViewModel shows NoMatches and filters lines that arrive after the query. The screenshot test remembers its field state.
- **iOS:** the no-matches state keeps the grouped list's background; the search snapshot is tall enough for both groups at large text; the comment over `.searchable` now says how it differs from Android's. Regression guards: a Retry keeps the search, a key during a Retry's load keeps Loading, and the UI test checks both groups and the emptied field.
- **Not changed:**
  - es "Buscar línea" and en "Search lines": each is the natural prompt in its language.
  - `dropWhile` on the field's text stays: the field's saved state and the ViewModel share the navigation entry's lifetime (`AppShell`), which the comment now relies on.
  - On iOS, a query typed during the first load hides the favourites when the lines arrive, so the skeleton's favourites section disappears then; it is rare, and the rule of an active query is shared code's, not the view's.
  - A design-system message-with-action component for Empty, Error and NoMatches: NoMatches sits inside the list, unlike `RetryMessage`, which fills the screen.
- **Process:** two green commits adapted tests, each said so in its body: `TabNavigationTest` (the field scrolls too, and pushes line 3 below the window), and the iOS search snapshot's favourite. Twice a commit went in after a failing check (detekt), fixed by the next commit; checks now gate the commit with `&&`.

**Decisions**
- **Part 1 now, stop names in part 2** (owner decision, 2026-11-03): no stops are cached until LB-008 part 2, so the "Iglesia de Lardero" criterion waits. The prompt says "Buscar línea" until then.
- **Android filters in place, as iOS does** (owner decision, 2026-11-03), instead of the full-screen search `design_guidelines.md` §4 named: the criteria want the carousel back when the query is cleared, not when a search view closes. M3's collapsed `SearchBar` disables the soft keyboard until it expands, so the design system's `SearchField` is built on `TextField`.
- **Prefix words, as the stop search:** "1" finds 1, 10 and 11, "dero" finds nothing; the rule is the stop search's (`architecture.md` §4.3), so part 2 can search stop names the same way.
- **The field's text lives in the screen on Android** (`TextFieldState`, saved across configuration changes and process death), which sends it as an event; an empty field is news only once it has had text. On iOS the view model holds it, as `.searchable` binds to a property.
- **The iOS field shows in every state:** the system's search belongs to the navigation stack; a query typed during the first load filters the lines once they arrive. Android shows its field only with lines to filter.
- **`@Suppress("LongParameterList")` on `LinesScreen`,** with its reason: its sixth parameter is the field's hoisted state, which previews and screenshots set. Near Me and the planner will meet the same limit; detekt's `ignoreDefaultParameters` would then be the project-wide answer.
- **A pane title announces the no-matches state on Android**, rather than `announceForAccessibility`, deprecated in API 36, or a live region, which a new node cannot be.

**Open / next**
- **The pull request of this entry** (branch `feat/LB-102-lines-search`) is open, and its watch runs.
- **Next, once it is merged:** LB-110 (edit favourite lines: reorder, remove, the limit of 20), which needs nothing else, unless the owner chooses otherwise. LB-102 part 2 waits for LB-008 part 2, and LB-104 for OQ-10's map style terms.
- **For LB-102 part 2 (data-rules-reviewer):** find stop names through the FTS index (`searchStops`, then the patterns' line ids), not by folding every pattern stop's name on each key; and match a word that is all digits, or shaped like a code, against codes only, or rank exact codes first, as the stop search puts an id first: stop names carry numbers ("Avda. Pío XII, 38").
- **Waiting on the owner:** the Gradle cache provider (above); the Kotlin Gradle plugin's Dependabot alert; OQ-10, OQ-11, OQ-16; DD-10; WorkManager; as in the entries below.

## 2026-11-02: LB-103, favourite lines, with a minimal line detail

**Done**
- **PR #36 (LB-101 part 1) merged,** and `main` was synced. Seven of the eight Dependabot alerts closed; the Kotlin Gradle plugin's stays open, waiting for the owner.
- **PR #37** (Gradle's longer retries for rate-limited downloads, LB-002) is open. Its first run's iOS gate failed outside the change: "iOS ViewModel and snapshot tests (FeatureSettingsTests)" never got a macOS runner, queued for 16 minutes while `main`'s full run after PR #36 held them (GitHub Free allows 5 at once), and ended cancelled. The failed jobs were rerun once; it is the first time, so the pipeline is unchanged. Recorded here rather than on PR #37's branch, since a push there would have cancelled the rerun.
- **LB-103 (`feat/LB-103-favourite-lines`):**
  - **Shared:**
    - `FavouriteLinesRepository` (`:shared:favourites:domain`) and its implementation over the `favourite` table (`:shared:favourites:data`): a favourite line goes last, one already saved keeps its place, and the favourites survive a restart. `FavouriteTypes.LINE`; `logroBusDatabase` passes favourite's Instant adapter.
    - `Line.headsigns`: `selectLines` joins the patterns, outbound first.
    - `:shared:feature:lines`: `ObserveLinesWithFavourites`, which wraps `ObserveLines`, and `SetFavouriteLine`, which counts `favourite_add` of kind LINE once a line is saved. `LinesUiState.Content` has the carousel's `FavouriteLineCardUi`s in the user's order, and each `LineRowUi` says whether it is a favourite. `LineDetailUiState` and its mapper are the minimal detail.
    - `:shared:umbrella`: the graph binds the repository and both use cases, keeping favourites in the real database in debug builds too. The sample lines head for each end of their names. `PreviewUseCases.favouriteLine(id:)` scripts favourites. `UseCases.observeLines()` gave way to `observeLinesWithFavourites()` and `setFavouriteLine()`.
  - **Android:** `FavouriteToggle` in the design system (Material Symbols' star). `LinesScreen` has the favourites heading, the hint card or a `HorizontalUncontainedCarousel` whose cards share the tallest one's height, and a toggle on each row; a row or a card opens `LineDetailKey`. `LineDetailRoute`, `LineDetailScreen` and `LineDetailViewModel`, which takes its line through `koinViewModel`'s parameters. Strings in es and en; screenshots recorded and reviewed.
  - **iOS:** `FavouriteToggle` in `DesignSystem`; `LinesView`'s favourites `Section` with an `HStack` of cards that share the tallest one's height, rows that are buttons beside their toggles, `LineDetailView` and `LineDetailViewModel`, and `Route.line` in the shell. Strings in es and en; snapshots recorded and reviewed.
  - **End to end through both shells:** a line added from its row goes at the end of the carousel and its toggle says so; a card opens its line; a line added from its detail is in the carousel back on Lines.

**Reviews** (the code-reviewer, data-rules-reviewer, android-ui-reviewer and ios-ui-reviewer subagents; no blockers or majors; the UI reviewers asked for changes). Fixed, test first where a test could show it:
- **Shared:**
  - `favourite_add` was counted when a double tap saved nothing: `add` now says whether it saved the line.
  - Two quick opposite taps could land in the wrong order on the I/O threads: the repository writes behind a `Mutex`, and both Swift ViewModels chain their writes, since SKIE may start two Kotlin calls in either order. The Swift test failed now and then before the chain.
  - A malformed saved id is dropped and counted in the log; `favouriteLine(id:)` keeps one entry per line; `dd08_` covers the cards, and a test covers a pattern of an unknown direction (both regression guards).
- **Android:**
  - The shell's preview had lost its binding for `SetFavouriteLine`.
  - A quick second tap could push a second detail: `TabNavigator.open` now takes the screen it opens from.
  - The toggle was a checkbox, so TalkBack said "checked" beside "Remove…": it is a button now, with a 48 dp test.
  - The detail flashed Loading on return after a Retry, and drew nothing while loading: it keeps its load and draws a skeleton, titled "Línea", with a Loading preview.
  - At 200 %, the star left "Valdegastea" too narrow: the badge goes above the name.
  - Cards widen with the text; rows and cards say "abrir línea"; the hint is read without its ☆; no-break spaces keep arrows with their headsigns; the skeleton has the favourites section; the measured copies take no input.
- **iOS:**
  - The same large-text, card width, skeleton, title, no-break space and spoken hint fixes.
  - Content margins replace padding, which view-aligned snapping ignores.
  - The headsigns' spoken label uses `Text` interpolation, since `Text + Text` is deprecated in the iOS 26 SDK.
  - The card colour is a token, and the toggle has previews and snapshots.
- **UI tests:** both shells' favourites tests no longer depend on favourites left by a manual run: rows are found without cards, a test checks only line 3's card, and Back is the button labelled Lines.
- **Docs:**
  - LB-110 lists the favourites whose line left the catalogue, which have no card now, so that the user can remove them.
  - LB-008 part 2 clears the favourites saved against the sample lines' ids in debug builds.
- **Not changed:**
  - The detail's bar names the line in words, with the badge in the body: a deliberate exception until LB-104 designs the bar (`design_guidelines.md` §3.1).
  - A second tap that arrives before the first one's state shows sends the same value again, so it does nothing; the writes keep their order.
  - On iOS the third card is cut by the inset row's rounded corner.
  - The green commits before the review say "No refactor needed." instead of the exact line `TDD: refactor not needed` (`testing_strategy.md` §7.1); pushed history is not rewritten for it, and the review's commits use the line.

**CI fixes**
- **PR #38's first run** failed in "Shared unit tests (:shared:transit:data)", outside the change: configuring build-logic, the Gradle Plugin Portal answered "not found" for `kotlin-stdlib:2.4.0`, as in PR #36's nearme job. It recurred, so the pipeline was made resilient in this pull request, test first: build-logic's `pluginManagement` lists Maven Central before the Portal, as the root settings do (`ci_cd.md` §5).
- **PR #38's second run:** "iOS ViewModel and snapshot tests (FeatureSettingsTests)" reached its 20-minute limit and was cancelled. With cold caches and the five macOS runners busy, it spent 625 s generating the framework and 469 s building, and the other matrix jobs took 17 to 18 minutes. It is the first time this job timed out (in PR #37 it never got a runner), so the failed jobs were rerun once.
  - The rerun's job never got a runner: GitHub's annotation says "not acquired … capacity constraints" for macOS arm64, as in PR #37. Asked, the owner chose to rerun and to raise the matrix jobs' limit from 20 to 30 minutes (owner decision, 2026-11-02), done in this pull request (`ios-checks.yml`, `ci_cd.md` §3 and §5). A job that gets no runner cannot be prevented from the pipeline.
- **PR #38's fourth run** failed in "Shared unit tests (:shared:feature:stops)", outside the change: the Plugin Portal answered "not found" for `kotlin-dsl`'s own plugin marker, which only it serves, so Maven Central first could not help. Pull request jobs restore no Gradle cache ("no match found"), and some thirty jobs download everything at once. It is the third such failure in two days, so the pipeline was made resilient, test first: every Gradle step of the workflows, and `generate-project.sh`, runs through `scripts/ci/gradle-retry.sh`, which runs Gradle again, up to three runs, when it failed only because a repository did not serve a dependency (`ci_cd.md` §5).
  - **Why pull request jobs find no cache:** the basic provider keys the Gradle User Home on a hash of the Gradle files, with no fallback key (`ci_cd.md` §5), and this pull request changes them, so every run starts cold. The repository's cache is also at its 10 GB quota with `main`'s entries.
- **PR #38's fifth run** failed in "Android screenshots (:android:feature:lines)", outside the change. Maven Central did not serve compose ui 1.10.2's module metadata, so Gradle read its POM, whose iOS artifact matches no Android variant. Gradle remembered the missing file, and the script's reruns failed the same way. The reruns now pass `--refresh-dependencies` and treat that variant error as a dependency that was not served, test first.
  - **For the owner:** a cache that survives a change to the Gradle files needs a fallback key, which the basic provider lacks; the "enhanced" provider has one, but it is proprietary (`ci_cd.md` §5).
- **PR #38's sixth run** passed every Kotlin and Android check, with the refreshed reruns. Only the iOS gate failed: "iOS ViewModel and snapshot tests (DesignSystemTests)" never got a macOS runner ("not acquired … capacity constraints"). As agreed with the owner, such a job is only rerun; the rerun passed, and all 57 checks were green.
- **PR #38's third run** failed in "Android screenshots (:android:app)", a fault in the branch: `TabNavigationTest.lb103_aFavouriteCardOpensItsLine_andBackReturnsToLines` found no "Line 10" heading right after tapping the card. The detail reads its line on the I/O threads, which the test's idle sync does not wait for, and is titled "Line" until then. Locally and in the earlier runs it had always been quick enough. The three favourites tests now wait, up to 5 s, for the state they check (e9f32ad).

**Decisions**
- **A minimal line detail now** (owner decision, 2026-11-02): two criteria need line detail, which LB-104 builds. LB-103 adds its route, ViewModel and toolbar toggle, with the line's badge and name; LB-104 adds the map, directions and stops.
- **Rows open their line** too, so a line can be added from its detail before it is a favourite (FR-LINE-09).
- **`ObserveLinesWithFavourites` wraps `ObserveLines`,** whose sync logic and tests stay as they were. Favourites that cannot be read leave the lines with none: the list still shows (spec §7.3).
- **Equal-height cards on both platforms,** so the carousel is never ragged at large text sizes, and headsigns wrap rather than clip. Android measures copies of the cards hidden from TalkBack, since its carousel centres its items; iOS uses an `HStack` of fixed height rather than the `LazyHStack` that `design_guidelines.md` named, which it now names instead. iOS cards are 150 pt, so the next card's edge shows inside the inset row; Android's are 168 dp.
- **A Gradle rerun step after all,** which PR #37's entry below had decided against: the Portal's "not found" is a 404, which no backoff retries. The script reruns only a failure whose output names a dependency that was not served; a failing test or a compile error fails at once, so it hides no failing check. PR #37's longer backoff stays, and spares a rerun when a 429 passes within a minute.
- **Swift calls a `suspend` use case** through `@preconcurrency import Shared`: Swift 6 refuses to send a Kotlin object, which declares no `Sendable` conformance, to SKIE's `async` function (`architecture.md` §5.2).
- **iOS UI tests that change favourites start and end with them removed:** the simulator keeps the app's database between launches, unlike Robolectric.
- **Test fixtures adapted while going green** (each in its commit body): `LinesScreenTest` runs on a taller window, since the favourites section comes first and a lazy list composes only what it shows; `TabNavigationTest` scrolls the list from its top before a tap, so a row is never under the top app bar, which the list runs behind, and reads the cards from left to right, as TalkBack does; `AndroidGraphTest`'s restart test is a regression guard written after the wiring.

**Open / next**
- **The pull request of this entry** (branch `feat/LB-103-favourite-lines`) is open, and its watch runs. PR #37 was merged on 2026-11-03, and `main` was merged into this branch.
- **Next, once it is merged:** LB-102 part 1, the search bar over the lines' codes and termini; finding lines by a stop's name waits for the stops (LB-008 part 2) or sample stops. LB-110 (edit favourite lines: reorder, remove, the limit of 20) needs nothing else either. Unless the owner chooses otherwise, LB-102 (`/start-story LB-102`).
- **LB-104** builds on this detail; its map needs a style for development, where OQ-10's documented fallback names a free OSM-based style whose terms are still to be verified: that check is the owner's.
- **For LB-104:** a shared `ObserveLine(lineId)` use case, so the detail stops collecting every line and syncing on each opening, and the badge in the detail's bar.
- **Follow-ups from LB-101's reviews** stay open, as listed in its entry below.
- **Waiting on the owner:** as in the entries below.

## 2026-11-02: Gradle waits out rate-limited downloads

**Done**
- **PR #36 (LB-101 part 1) merged,** and `main` was synced. Seven of the eight Dependabot alerts closed once `main` submitted its dependency graph; the Kotlin Gradle plugin's (GHSA-r937-wjx7-w2jp) stays open.
- **A runner flake that recurred, made harmless (`ci_cd.md` §5):** Maven Central answered `429 Too Many Requests` for Robolectric's `nativeruntime-dist-compat` jar twice in a few minutes: in "Android unit tests (:android:feature:settings)" on PR #36's first run, and in "Coverage" on `main` after PR #35 (7266c7f). Both runs had started with an empty Gradle cache, since PR #35 changed the dependencies, so every job downloaded at once.
  - Gradle 9.7.1 already retries 408, 429 and 5xx answers, but 3 times from 1 s. `gradle.properties` now sets `org.gradle.internal.repository.max.tentatives=6` and `initial.backoff=2000`: about a minute of backoff.
  - `ArchitectureTest.lb002_gradleWaitsOutRateLimitedDownloads` keeps those floors (red, then green; no refactor needed).
  - **Checked against a local server that answers 429 to everything:** Gradle made 3 requests over 3 s by default, and 6 over 62 s with the settings. The wrapper also copies the `systemProp.` entries of its own project's `gradle.properties`, so a probe run through this repository's `gradlew` gets them even with `-p`; the comparison used Gradle's own launcher.
- **Not covered:** the other failure of PR #36's first run, in "Android unit tests (:android:feature:nearme)": the Gradle Plugin Portal answered that `kotlin-stdlib:2.4.0` did not exist, a 404 that Gradle does not retry.

**Decisions**
- **Retries rather than a rerun step:** a longer backoff only waits when a repository refuses a download, while rerunning a whole Gradle step after a failure could also hide a failing check.

**Open / next**
- **The pull request of this entry** (branch `fix/LB-002-download-retries`) is open, and its watch runs.
- **Next:** LB-103 (favourite lines and their carousel), as the LB-101 entry below says, unless the owner chooses otherwise.
- **If the Plugin Portal's 404 recurs,** rerun the failed jobs, then look for a fix that does not need the Portal for Kotlin's artifacts.
- **Waiting on the owner:** as in the LB-101 entry below.

## 2026-11-02: LB-101, part 1: the Lines list over sample lines

**Done**
- **Merges:** the owner merged PR #34 (LB-006 part 1), then PR #35 (the Dependabot alerts), and `main` was synced each time. `main` was merged into PR #35 after PR #34, and into this branch after PR #35, with the 2026-11-02 entries combined.
- **LB-101 split in two,** like LB-005, LB-006 and LB-008. Part 1 is the list over a repository contract and sample lines; part 2 checks it against `lines_2026-10-22.json` once OQ-11 unblocks LB-003 and the part 2s of LB-005, LB-006 and LB-008.
- **Shared (`architecture.md` §4.2, §4.6):**
  - `:shared:transit:domain`: `Line` (spec §5.1, without patterns) and `LinesRepository`;
  - `:shared:transit:data`: `LinesRepositoryImpl` over the `line` table, with the new `selectLines` query;
  - `:shared:feature:lines`: `ObserveLines`, with a sync on every collection: an empty cache waits for it and shows its failure (FR-ERR-05), and a failed refresh of a full cache stays silent (FR-ERR-04). `LinesUiState`, `LineGroupUi`, `LineRowUi` and `LinesUiMapper`: day then night groups, sorted by the number in the code, with no fixed set of lines.
  - `:shared:umbrella`: `observeLines()` for Swift, the sample lines, and `PreviewUseCases`' `offline()` and `synced()` scripts.
- **Android:** `LinesViewModel`, `LinesRoute` and `LinesScreen` in `:android:feature:lines`, at the root of the Lines tab; strings in es and en; screenshots of the four states in light, dark and 200 %. The shell's screenshots were recorded again, since the Lines root now shows the list. The shell's preview gets its ViewModel through `KoinApplicationPreview`.
- **iOS:** `LinesViewModel` and `LinesView` in `FeatureLines`, at the root of the Lines tab; `Localizable.xcstrings` in es and en; `FeatureLinesTests` with the ViewModel tests and snapshots of the four states in light, dark and large text.
- **Scroll to top,** end to end through the shell: `TabNavigationTest` on Android, and `ShellUITests` on iOS at the largest text size, so the list is far longer than the screen.
- **CI:** the "iOS ViewModel and snapshot tests" job, one per test target of `LogroBusUI` (`DesignSystemTests`, `FeatureLinesTests`, `FeatureSettingsTests`). `scripts/ios/test.sh` takes test targets, and the app's job runs only `iosAppTests` and `iosAppUITests`. An architecture test fails when the matrix and `Package.swift` differ. `:android:feature:lines` joined the Android screenshot matrix.
- **A coverage gap found by the gates:** `:shared:transit:domain` fell to 42.9 % (floor 80 %), because Kover counts each module's own tests and `Line` had none there. `LineTest` covers it with a DD-08 case: two lines that share a colour are still two lines.
- **The iOS green commit needed no refactor:** it follows About data's pattern. The snapshot appearances repeat `DesignSystemTests`' small enum rather than add a shared test target for one type.
- **Reviews** (the code-reviewer, data-rules-reviewer, android-ui-reviewer and ios-ui-reviewer subagents; two asked for changes). Fixed:
  - **Android, Loading on every return (major, two reviewers):** after the upstream stopped, coming back to the list showed the skeleton again and lost the scroll position. Only a new load after Retry starts at Loading now, as on iOS; a test with a read that takes a moment pins it.
  - **iOS, Retry (major):** a text button about 20 pt tall. `RetryButton` in `DesignSystem` is a large bordered button, at least 44 pt (`SizeTests`), and About data uses it too.
  - **Screenshots at 200 % (major):** they showed only the first rows. The list's screenshots are now as tall as the list on both platforms, so every code, the chip and the night group show.
  - **Shared:** a test with a repository that reads once pins that the first sync's lines are read after it ends (a mutant that combines the two fails it); a refresh with new lines; a real `dd08_` test in the mapper, with the data-only `dd08_` tests renamed `lb101_`; an exhaustive `when` instead of a cast; a cached line whose id is no `Int` is dropped and logged, not truncated.
  - **Android:** the `error_*` strings, which both feature modules defined (resource merging keeps one silently), `ErrorReason.messageRes` and `RetryMessage` moved to `:android:core:designsystem`, and the message scrolls at large font sizes. The first load is announced as "Loading lines" and its skeleton has the list's heading and badge sizes. Every error reason is tested. The top app bar tints on scroll.
  - **iOS:** the scroll to the top reaches the real top, the Day lines heading included, through `ScrollPosition`, since a `ScrollViewReader` could not reach a section header. The loading list reads as "Loading lines". The chip uses error container tokens, the values of Android's Material scheme. The UI tests wait for the scroll to settle. Dark previews for Loading and Empty.
  - **CI and docs:** `ios-checks.yml` now runs the Kotlin jobs too, since the architecture tests read its matrix. Each matrix artifact is named after its target. The docs that still placed `DesignSystemTests` in the app's job are updated.
  - **TDD deviations, for the record:** the first data cycle has no refactor commit or note; the iOS green commit (6261018) changed the snapshot layout of its own tests; `9a26339` and the review's race and DD-08 tests were written after the behaviour, as regression guards.

**Decisions**
- **Debug builds list sample lines** until LB-008 part 2 can download the real ones (owner decision, 2026-11-02). They have the network's codes, no line 8, and the shared colours of DD-08; their names and colours are placeholders, and line 11 is not operational so that the chip shows. Release builds read the empty cache behind a sync that fails with `Data` (`NoSourceSync`), so they show the error with Retry.
- **One CI job per Swift test target** (owner decision, 2026-11-02), each building the framework again. "iOS build and tests" took 19 minutes on PR #35, against its 10-minute budget. A pull request now uses 5 macOS runners at once, GitHub Free's limit.
- **`ObserveLines` reads the cache again after the sync,** with `flatMapLatest`, so the lines a sync stores are never read before its transaction.
- **The status chip says "Sin servicio" / "Not in service"** (`lines.not_operational`), and the Empty state "No hay líneas que mostrar." (`lines.empty`); both are in `design_guidelines.md` §9.

**Open / next**
- **PR #36** (this entry, branch `feat/LB-101-lines-list`) is open, and its watch runs (`scripts/agent/watch-pr.sh 36 180`). Its first CI run failed once in one job, outside the change: Gradle could not fetch `kotlin-stdlib:2.4.0` from the Gradle Plugin Portal while configuring build-logic, which every other job resolved. The merge of `main` started a new run.
- **Next, once PR #36 is merged:** the next M1 story that needs neither OQ-11 nor OQ-10. LB-103 (favourite lines and their carousel) needs only the lines and the `favourite` table; LB-102's search also finds stops by name, so its part 1 would need sample stops too. Unless the owner chooses otherwise, LB-103 (`/start-story LB-103`).
- **LB-101 part 2** waits for OQ-11 (LB-003), and with it the part 2s of LB-005, LB-006 and LB-008; LB-008 part 2 removes the sample lines.
- **Follow-ups from the reviews:**
  - **About data on Android** shows Loading again whenever it comes back, as the Lines list did; give it the same rule.
  - **Android's top app bar** does not collapse yet (design_guidelines.md §4): a scroll to the top on re-selection is programmatic and would leave a collapsed bar hidden. Reset the bar with the scroll first.
  - **iOS's tab bar** does not minimise on scroll yet (`.tabBarMinimizeBehavior(.onScrollDown)`, design_guidelines.md §5): check first that re-selecting Lines on a minimised bar still scrolls to the top.
- **Waiting on the owner:** the Kotlin Gradle plugin alert (PR #35), and the items of the hand-over entry below: PR #34's three points (now merged), OQ-11, OQ-10, OQ-16, DD-10's provisional saved stop, the limits of the rule on failing checks, and WorkManager.

## 2026-11-02: The Dependabot alerts: security floors for the build's tools and tests

**Done**
- **The owner's go-ahead** for the 8 open Dependabot alerts on `main` (3 high, 5 moderate). PR #34 (LB-006 part 1) stays open meanwhile, and its watch runs.
- **Where they come from:** an init script resolved every configuration, as the dependency graph does, and printed who brings each affected version. None of them reaches the app.
  - AGP's tools (`androidLintTool`, `unified-test-platform-*`): Guava 32.0.1 and 33.4.0, Commons Lang 3.16.0, HttpClient 4.5.6.
  - AGP's plugin classpath: Guava 33.4.0, Commons Lang 3.16.0, jose4j 0.9.5 (bundletool), JDOM 2.0.6 (Jetifier).
  - KGP's Swift export (`swiftExportClasspath`): OpenTelemetry API 1.41.0, two advisories.
  - The unit and host tests: Guava 33.6.0, which Robolectric 4.17 brings.
  - build-logic's plugin classpath: Kotlin Gradle plugin 2.4.0, the Kotlin that Gradle 9.7.1 embeds for `kotlin-dsl`.
- **The floors (`ci_cd.md` §6):**
  - `SecurityFloors.kt` replaces `AgpToolSecurityFloors.kt`. The conventions add the floors to AGP's tools, the Swift export classpath and the test dependencies, never to the app's classpaths.
  - The root build script raises Guava, Commons Lang, jose4j and JDOM on the plugin classpath, next to Bouncy Castle and FreeMarker.
  - New catalog entries: `guava` (BOM), `opentelemetry` (API), `commons-lang3`, `httpclient`, `jose4j` and `jdom2`.
- **A trap found on the way:** a BOM in `swiftExportClasspath` removed KGP's `swift-export-embeddable` instead of raising its OpenTelemetry. KGP adds it as a default dependency, which applies only while the configuration declares none. That classpath now gets plain constraints, and a test (`lb002_swiftExportClasspathGetsNoBoms`) keeps the BOMs out of it; the resolution itself was checked by hand, as below.
- **Verified:**
  - Every affected library now resolves to its fixed version or later, except the Kotlin Gradle plugin of build-logic.
  - A diff of the modules that every configuration resolves, before and after: only tool and test configurations change. The BOMs appear, Guava 33.7.2 trades `jsr305` and `checker-qual` for `jspecify`, and OpenTelemetry 1.67 adds `opentelemetry-common`. No app classpath and no iOS framework changes.
  - GitHub's advisory database has nothing against the new versions.
- **A failing check fixed** (`AGENTS.md` §10): PR #35's SwiftLint job failed on `ubuntu-26.04`, which no longer ships `libxml2.so.2` (exit 127). PR #34 met and fixed the same change of runner image, but its fix has not reached `main`, so PR #35 cherry-picks that commit (pin `ubuntu-24.04`, and fail an install that cannot run). A rerun would have failed the same way. Whichever of the two merges second brings the same change twice, which merges cleanly.
- **PR #34 merged first:** `main` was merged into this branch. The README and project log entries of both are combined under 2026-11-02, and the SwiftLint fix that both carry merged cleanly.

**Decisions**
- **The Kotlin Gradle plugin alert has no floor.** A constraint raises it, but `kotlin-dsl` then runs outside the Kotlin it is built for, and every build prints "Unsupported Kotlin plugin version". Gradle 9.8 embeds Kotlin 2.4.10, still affected, so the alert waits for a Gradle release that embeds 2.4.20 or later.

**Open / next**
- **PR #35** (this entry, branch `fix/LB-002-dependabot-alerts`) is open, and its watch runs (`scripts/agent/watch-pr.sh 35 180`). A new session starts it again, as the hand-over entry below says.
- **Next:** LB-101 part 1 (`/start-story LB-101`): the Lines list over a repository contract and scripted lines. It does not wait for PR #35.
- **Waiting on the owner:**
  - keep the Kotlin Gradle plugin alert open until Gradle catches up, or dismiss it as a tolerable risk;
  - the other items of the hand-over entry below (PR #34's three points, OQ-11, OQ-10, OQ-16, DD-10's provisional saved stop, the limits of the rule on failing checks, WorkManager). Its eighth item, the Dependabot alerts, is this entry.

---

## 2026-11-02: Hand-over: where work stands, and how to resume

**Done**
- **A restart of the owner's machine:** the owner asked for the docs to record where work stands, so that a new agent session can resume after the restart. This entry is that record.
- **State at the hand-over:**
  - **PR #34** (LB-006 part 1, branch `feat/LB-006-normalisation-rules`) is open. All 53 checks passed on `6fc65b5`, and there is no review, comment or change request yet. This entry's commits are pushed to the same branch, so the checks run again.
  - **`main`** is at `94fa572`, the merge of PR #33.
  - **Local state:** nothing is uncommitted, and no simulator or emulator started by the agent is running. The background watch of PR #34 (`scripts/agent/watch-pr.sh 34 180`) ends with the machine, so the next session starts it again.
- **Docs:** `AGENTS.md` §10 and `finish-story.md` step 9 now say where a session resumes, and both READMEs record it.
- **The watch starts again by itself** (the owner asked that a new chat resume it): `scripts/agent/session-start.sh`, which Claude Code runs at the start of every session, now lists the open pull requests with the command that watches each. `AGENTS.md` §10 says to start those watches before the owner's first task, whatever it is. `gh` gets 6 s, inside the hook's 10 s; when it fails or hangs, the context says to list the pull requests by hand. Checked with a fake `gh` that fails, hangs and finds nothing.
  - **The limit:** an agent acts only once the owner writes, so the watch starts with the session's first message, whatever it says.
- **A failing check fixed** (`AGENTS.md` §10): the run on `aba277b` failed in SwiftLint. `ubuntu-latest` has moved to Ubuntu 26.04, which no longer ships `libxml2.so.2`, and SwiftLint 0.65.1's Linux binary needs it. The same job had passed on `6fc65b5` on Ubuntu 24.04, and a rerun would fail the same way.
  - The SwiftLint job pins `ubuntu-24.04`, until a SwiftLint release runs on 26.04 (`ci_cd.md` §5).
  - `install-swiftlint.sh` had reported the broken binary as installed. It now fails with the binary's error; a fake `curl`, `sha256sum` and `unzip` show both paths.

**Decisions**
- **Open pull requests first:** every session starts the watch of each open pull request before anything else. The session-start hook lists them, so no session depends on remembering it.
- **Where a session resumes:** the newest entry of this log is the hand-over. Its "Open / next" names any open pull request, the next task and what waits for the owner. A session that ends with a pull request open records them there before it ends.

**Open / next**
- **Resume, in this order:**
  1. Run `scripts/agent/session-start.sh` (Claude Code's hook runs it), and start the watch of each open pull request it lists. Then run `git fetch` and `git status`. Expect `feat/LB-006-normalisation-rules`, clean and level with `origin`.
  2. Check PR #34: `gh pr view 34 --json state,reviewDecision,reviews,comments` and `gh pr checks 34`.
     - **Open:** the watch from step 1 (`scripts/agent/watch-pr.sh 34 180`, in the background) runs; act on its result as `AGENTS.md` §10 says. Failed checks are fixed without waiting for the owner.
     - **Merged:** run `git checkout main && git pull --ff-only`, tell the owner in one line, and start the next task.
     - **Closed without a merge, changes requested or new comments:** stop and report to the owner.
  3. **The next task, once PR #34 is merged,** is LB-101 part 1, through `start-story.md` (`/start-story LB-101`): the Lines list over a repository contract and scripted lines.
     - Part 1 covers every LB-101 criterion that scripted lines can show.
     - Part 2 checks the list against `lines_2026-10-22.json`, once OQ-11 unblocks LB-003 and the part 2s of LB-005, LB-006 and LB-008.
- **Waiting on the owner.** None of these blocks LB-101 part 1.
  - **PR #34's three points,** in its description and in `api_spec.md` §4 and §5:
    - DD-05 takes the reading nearest the report around the changes of time;
    - the readings of spec §5.6: `order` counts from 1, calls at one stop are placed by their order first, and only step 3's decisions across several stops are cached;
    - line codes are `B` and up to three digits.
  - **OQ-11,** the terms of reuse: it blocks LB-003, and through it the part 2s of LB-005, LB-006 and LB-008.
  - **OQ-10,** the tile provider: it blocks LB-013.
  - **OQ-16,** the analytics, crash-reporting and remote-config vendors: it blocks LB-014's last ADR and LB-015.
  - **DD-10's provisional saved stop counts as served** (FR-FAV-04), in the 2026-10-30 LB-008 entry.
  - **The limits of the rule on failing checks,** in the same entry.
  - **WorkManager** for LB-008 part 2's daily job: a new dependency, with its ADR.
  - **8 Dependabot alerts on `main`:** 3 high and 5 moderate.
- **Still open from the 2026-10-31 entry:** LB-006 part 2's notes, and the notes for the planner and Near Me stories.

---

## 2026-10-31: LB-006, part 1: the DD rules that need no DTO shapes

**Done**
- **Merge:** the owner merged PR #33 (LB-008 part 1, the CI retry and the rule on failing checks), with all 53 checks green, and `main` was synced. The iOS job passed at its first attempt; the simulator did not vanish, so the new retry did not run.
- **LB-006 split in two,** like LB-005 and LB-008. Part 1 is the rules that need no DTO shapes. Part 2 is the mappers over LB-005's DTOs and the rules that need them, checked against LB-003's fixtures (OQ-11).
- **`:shared:core:network`:**
  - `String?.trimmedOrNull()` (DD-07);
  - `IdParser.stopId()` and `lineId()` (DD-01, DD-02, DD-06). They live here so the planner's mappers can read the same formats.
- **`:shared:core:common`:**
  - `madridTimeZone`, moved from `MadridDateTime` as ADR-0015 planned, with kotlinx-datetime now exposed as API;
  - `parseCssColor()` (DD-08), beside `onColorFor()`.
- **`:shared:transit:data`:**
  - `DirectionMapper` (DD-04 words) and `EtDirectionInferrer` (spec §5.6 steps 2 to 5);
  - `correctNextStopTime()` (DD-05);
  - `NameNormaliser` (DD-11) and `LineNameParser` (spec §5.5);
  - `ServiceArea` (spec §5.4, step 7).
- **Tests:** 67 new tests on the Android host and the iOS simulator, named after their DD rule or `lb006_`. A mutation to `api_spec.md`'s DD-05 sketch fails exactly the October second-pass case.
- **Reviews** (the code-reviewer and data-rules-reviewer subagents; no blockers, changes asked). Fixed, each with its own TDD cycle:
  - **ET directions (major, both reviewers):** calls at one stop skipped the positional steps, although `order` still places them, so every stop on both patterns would have lost its direction in API-06's answers. Positions now come first, with membership as step 4's fallback.
  - **The ±1 window (major):** it was symmetric only for an `order` counted from 0. `order` now counts from 1, as spec §4.6's example suggests, and fits one place either side.
  - **The cache:** `infer()` now says whether its answer may be cached, so part 2 keeps only step 3's decisions across several stops.
  - **DD-05:** March's skipped hour is read nearest the report too. New tests: that hour, a time across October's change, and a late bus 40 min after its report, which pins the threshold from above.
  - **DD-11:**
    - the overrides apply in one pass, so no fix is rewritten by another;
    - a combining accent (NFD) is part of its word;
    - the longest-first order is pinned.
  - **Line names:**
    - the outbound pattern's termini show as delivered ("Puente Madre-Hospital", "IES"), not title-cased;
    - codes are checked against spec §5.5's form and upper-cased; a name with no code gives `null`;
    - the parameter is `name`, as it comes after the overrides.
  - **Smaller:** a test of 39 of 49 calls pins the rounding, and `DirectionMapper`'s comment names the real sources of its words.
  - **Left, recorded below:**
    - the planner's direction words;
    - sharing the service box;
    - the part 2 notes.

**Decisions**
- **DD-05 around the changes of time:** of the instants a wall-clock time can stand for, the one nearest the report wins: two in October's repeated hour, and the CET and CEST readings in March's skipped hour. The zone's default reading, which `api_spec.md` §4 sketched, would put a bus in the second 02:xx an hour early. Night lines run then.
- **Spec §5.6 for calls at one stop:** steps 2 and 3 run first, and step 4, the stop's membership, is the fallback for any set of calls at a single stop, not only a single call. The cache keeps only step 3's decisions across several stops.
- **`order` counts from 1** (spec §4.6's example), with a call fitting one place either side; the fixtures will confirm it.
- **Overrides (DD-11)** replace whole words in their exact case, longest first, in one pass. A typo in another case needs its own key.
- **The display name** takes the outbound pattern's first and last stops, as delivered, when a terminus holds a hyphen; with no patterns, it shows every part of the name. A line code is `B` and up to three digits, since no set of lines is fixed.
- **Only the agency prefix `1:` is stripped from ids;** another prefix would be another feed's id space, so the id is `null`.

**Open / next**
- **LB-006 part 2,** with LB-005 part 2 and LB-003's fixtures (OQ-11):
  - the DTO mappers;
  - the `StaticNormaliser` for LB-008, with a fingerprint of its version and the overrides;
  - DD-03, DD-09, DD-10's synthetic stops, DD-16 to DD-18;
  - the persisted `direction_code_cache` with codes 401 and 402;
  - the `DataQualityReport` with the dropped items.
- **Notes for part 2, from the reviews:**
  - **ET calls:** the ET mapper drops calls with no stop or `order` before grouping them.
  - **Pattern lists:** the lists passed to `EtDirectionInferrer` include DD-10's provisional stops in sequence, or every later position shifts.
  - **Colours:** `parseCssColor` keeps alpha, so a translucent line colour must be made opaque before `onColorFor`.
  - **OTP line ids:** check in `route_ok.json` whether OTP names night lines by id or by code (`IdParser.lineId("1:B1-asc")` is `null`).
  - **Line names:** a line whose name has no code falls back on its id, and is counted.
- **For the planner and Near Me stories:**
  - API-07's legs send `asc` and `desc`, but `DirectionMapper` is internal to `:shared:transit:data`, which features may not use. Move the word table to a core module then, or record a planner copy.
  - The planner (5 km, FR-PLAN-10) and Near Me's camera need the stops' extent too. Move the unexpanded box where they can reach it.
- **Still waiting on the owner:** OQ-11 (LB-003), OQ-10 (LB-013), the provisional saved stop of DD-10, the limits of the rule on failing checks, and the 8 Dependabot alerts.
- **Next story:** everything left in M0 waits for an owner answer: OQ-11 (LB-003, and through it LB-005, LB-006 and LB-008 part 2), OQ-10 (LB-013) or OQ-16 (LB-014). The next task is LB-101 part 1: the Lines list over a repository contract and scripted lines, with `lines_2026-10-22.json` checked in part 2.

## 2026-10-30: LB-008, part 1: the static sync engine

**Done**
- **Merges:** the owner merged PR #31 (commit identity) and PR #32 (LB-017), and `main` was synced. The web merge of #31 carried the owner's gmail as its author.
- **LB-008 split in two.** Part 1 is the engine. Part 2 wires it to the real source and normaliser, which wait for LB-005's services and LB-006's mappers, and so for LB-003's fixtures (OQ-11).
- **`:shared:transit:domain`:** `StaticSyncEngine.syncIfStale()`, and `Direction` and `LineKind` from spec §5.1.
- **`:shared:transit:data`:**
  - `DefaultStaticSyncEngine`, with a `StaticSource` and a `StaticNormaliser` injected, and the `StaticCatalogue` it stores;
  - the database keys of directions and line kinds;
  - SHA-256 from each platform, `MessageDigest` on Android and CommonCrypto on iOS, so no dependency is added.
- **The engine** (`architecture.md` §4.4, as built):
  - It downloads lines and stops together, on first use and once they are 24 h old by `AppClock`.
  - It skips the write of an unchanged payload, rejects a suspicious one, and replaces everything in one transaction, `sync_state` and the saved stops' missing syncs included.
  - It turns any failure into `Failure(Data)` with the cache intact, and runs one sync at a time.
- **`:shared:core:database`:** the replace queries in `Line.sq` and `Stop.sq`, `updateMissingSyncs` in `Favourite.sq`, and `FavouriteTypes.STOP`.
- **Architecture test:** no module bundles seed data (ADR-0008).
- **Tests:** 32 engine tests, plus the hash, the stored keys and the domain enums, on the Android host and the iOS simulator; the seed rule and its predicate in `:tools:architecture-tests`.
- **Reviews** (the code-reviewer and data-rules-reviewer subagents; no blockers, changes asked). Fixed:
  - **The content hash** covered only the bodies, so new DD-11 name overrides or a fixed normaliser would never reach the cache while the source stayed the same. The normaliser now has a `fingerprint`, folded into the hash.
  - **A last success in the future** kept the cache fresh: `ServerCorrectedClock` has no offset before the first response, so a slow device clock could skip every sync. It now counts as stale.
  - **DD-10:**
    - a saved stop that is only provisional counted as missing, and would have ended up "No longer served" although buses still call there; it now counts as served;
    - a pattern stop with no stop now rejects the catalogue.
  - **Types:** a stop's lines are a `Set` and a line's patterns a `Map` by direction, so a repeated item from the source can no longer break a primary key on every sync.
  - **Smaller fixes:**
    - lines and stops download at once;
    - only the caller's cancellation propagates;
    - the query is named `updateMissingSyncs`, and `ref_id`'s canonical form is documented (DD-01);
    - the stored keys are pinned;
    - a failed sync is shown to count no missing stop;
    - the seed check also catches lines or stops JSON in assets or resources;
    - the DD-10 tests are named `dd10_`.
  - **Docs:** the hash's deviation from spec §8.5, the amended unchanged-payload criterion, and the static sync as a `code_style.md` §2.2 catch boundary.
  - **Left:**
    - the normaliser contract has no place yet for the `DataQualityReport`'s dropped counts; LB-006 adds them with the report;
    - "concurrent readers never see a partial dataset" rests on the one transaction (one notification, full rollback), not on a concurrent reader.
- **CI of PR #33:** the run on `d2fffea`, a docs-only commit, failed in "iOS build and tests". After a clean build, `xcodebuild test-without-building` found no simulator ("Unable to find a device matching the provided destination specifier", exit code 70), as on PRs #21 and #28; the same code had passed on `1c91632`. Following the owner's new rule (below):
  - the failed jobs were rerun once;
  - since the flake had happened twice before, `scripts/ios/test.sh` now waits up to 2 minutes for a vanished simulator, boots it and retries the tests once (LB-002). Any other failure fails at once. A fake `xcodebuild` checked both paths locally;
  - `scripts/agent/watch-pr.sh` watches a pull request and also reports failed checks (LB-016). It was checked against a fake `gh` and against the merged PR #32.
- **Deviations** (`testing_strategy.md` §7.1):
  - `c41eec9`, the first red commit, was made before ktlint had run. The lint command named a task that does not exist, and piping it into `tail` hid the failure. ktlint then passed on that commit. Gradle output is no longer piped before its exit code is checked.
  - `c5f30b9`, a green commit, does not say "refactor not needed". The next commit, `850e704`, adds a domain test only because Kover measures each module by its own tests; its scope is `data` for a domain change.
  - `850e704` and `c206c9a` have no `TDD:` line. Both are guards that passed at once, as their bodies say.
  - Other guards that passed at once are named in their commits: the failure paths in `673e9d0`, four TTL cases in `1855d7d`, the one-notification test in `b752043`, two missing-sync cases in `d896d31`, and `e7e9a0d`.

**Decisions**
- **Owner (2026-10-30): agents fix the failing checks of their pull requests** without waiting for the owner, so that a pull request reaches review green. Recorded in `AGENTS.md` §10, `playbooks/finish-story.md` step 9, `engineering_workflow.md` §5 and `agent_harness.md`.
  - **Agent, for the owner to confirm, the limits of the rule:** read the log before acting; fix a fault on the branch with the usual protocol; rerun a runner flake once, and make a recurring one harmless; never weaken, skip or disable a check; stop and report when the fix needs an owner decision, touches `main` or its protection, or the same check still fails after three attempts.
- **Owner (2026-10-30):** the `AGENTS.md` §11 amendment made with LB-017 is confirmed. Until LB-409 lets the user consent, tests verify the analytics events instead of the debug view. The alternative, a debug-only way to grant consent, is not needed.
- **LB-008 in two parts,** like LB-005: the engine and its rules now, against injected seams; the source, the normaliser, Koin and the triggers in part 2.
- **The content hash is over the normaliser's fingerprint and the re-encoded JSON,** not the raw bytes of spec §8.5. `SourceClient` returns decoded JSON, and encoding it again keeps key order.
- **An unchanged payload still counts missing stops.** FR-FAV-04 counts 7 consecutive daily syncs, and a source that stops changing must still reach "No longer served". The LB-008 criterion is amended.
- **A provisional saved stop counts as served.** FR-FAV-04 says "disappears from API-02". A stop made up from a pattern (DD-10), such as 384, was never in API-02, but buses call there. **For the owner to confirm.**
- **The sync stores a `StaticCatalogue` shaped like the tables,** not domain entities. The mappers of LB-006 produce it.
- **Stored values:**
  - directions: `outbound`, `inbound`;
  - line kinds: `day`, `night`;
  - a saved stop's `favourite.type` is `stop`, and its `ref_id` is the canonical decimal id.

**Open / next**
- **LB-008 part 2:**
  - a `StaticSource` on `SourceClient` for API-01 and API-02;
  - LB-006's normaliser, with a fingerprint of its version and the name overrides;
  - the Koin `single`;
  - the app-start trigger;
  - the daily background job: WorkManager, a new dependency for an ADR, and `BGAppRefreshTask`;
  - the screen-level Retry.
- **LB-006:** drop repeated lines and stops, make provisional stops for pattern stops, count drops in the `DataQualityReport`.
- **Favourites:** store a saved stop under `FavouriteTypes.STOP` with its canonical id.
- **Next story:**
  - LB-006 part 1: the DD rules that need no DTO shapes, such as stop ids, directions, the DD-05 time fix, colours and empty strings;
  - LB-013 still waits for OQ-10, and LB-003 for OQ-11.

---

## 2026-10-30: LB-017: telemetry contracts

**Done**
- **Merges:** the owner merged PR #30 (LB-012), and `main` was synced. PR #31 (commit identity) was merged while this branch was open; `main` was merged into it, keeping both log and README entries.
- **`:shared:core:telemetry`:**
  - `AnalyticsEvent`: the six MVP events of NFR-OBS-03, with ids, counts, flags and nested enums only.
  - `CrashReporter`, with `CrashArea` and `Breadcrumb` (`ScreenView`).
  - `TelemetryConsent`, one `ConsentDecision` per kind of telemetry, and `MemoryConsentStore`, which starts undecided.
  - `ConsentGatedAnalyticsTracker` and `ConsentGatedCrashReporter`, which read their kind of consent on every call and keep nothing.
  - `NoOp*` objects for release builds, and `Logging*` for debug builds, printing through the `Logger`.
- **`:shared:umbrella`:** `telemetryModule(debug, platform)` binds one consent store and the gated telemetry. `initKoin` takes a `PlatformTelemetry` on Android (default empty) and iOS (`LogroBusApp` passes one with `nil`s).
- **Rules:**
  - a Konsist rule allows only `Int`, `Long`, `Boolean` and telemetry enums in events and breadcrumbs, and pins the catalogue to the MVP events;
  - `ModuleRules` rejects vendor telemetry SDKs in every module but `:android:app`.
- **Tests:**
  - telemetry: gates, logging and graph, on the Android host and the iOS simulator;
  - `AndroidGraphTest` and `IosGraphTest` with a recording double passed to `initKoin`;
  - two Swift tests with Swift doubles behind the shared gates;
  - `ModuleRulesTest`, `ArchitectureTest` and `ArchitectureRulesTest`.
  - All 47 iOS tests pass on the iPhone 17e (iOS 26.5).
- **Reviews** (the code-reviewer and data-rules-reviewer subagents; no blockers). Fixed:
  - **The Konsist rule:** it read direct parents only and keyed properties by simple name, so a free-text event under an intermediate sealed interface, or a breadcrumb sharing an event's name, passed. It now follows every type under either root and keys by full name (mutation-checked before and after).
  - **`ModuleRules`:** it missed the Kotlin Multiplatform wrappers, such as GitLive's Firebase, CrashKiOS and Kermit's Crashlytics and Bugsnag writers, and exempted the tools. Fixed, and detekt forbids those imports.
  - **Error messages:** the crash gate passed the raw `Throwable` on, so a vendor would have sent its message, which can quote a URL with search text or a payload with coordinates. It now forwards a `RedactedThrowable`, and the logging reporter prints the type chain itself.
  - **Failing vendors:** a vendor's exception escaped the gates into the caller; the gates swallow it now.
  - **iOS:**
    - Swift could adopt `AnalyticsEvent` or `Breadcrumb`, which export as protocols with no requirements; a SwiftLint rule rejects it;
    - an architecture test keeps vendor packages out of LogroBusUI's manifest;
    - a Swift test builds `PlatformTelemetry`.
  - **Docs:**
    - the READMEs said debug builds print telemetry, which they cannot before LB-409;
    - the threading contract and the limits of the gates are now in the KDoc;
    - LB-409 gains criteria for the SDKs' automatic collection and an atomic, Swift-reachable consent store.
  - **Left:** a Kotlin CocoaPods `pod(…)` in a shared module is not a Gradle dependency, so `ModuleRules` cannot see it; no module uses CocoaPods.
- **Deviations** (`testing_strategy.md` §7.1):
  - Four green commits (`b599b10`, `b826583`, `91aedb3`, `1fdcd56`) said "refactor not needed" without running `run-all.sh`. `b5b3915` then refactored `initKoin`'s parameters into `PlatformTelemetry` and fixed lint findings from `b599b10`, `4055ca4` and `b092d76`.
  - `1b635d8` and `f541dbc` were committed although detekt had failed: the command chain used `;` and did not stop. The next commits, `cd79552` and `a554821`, fixed the findings. Commits are now chained with `&&` after the lint.
  - `0161a6f` fixes test code, the Konsist rule, so its evidence is a mutation run before and after rather than a red commit.
  - Guards that passed at once are named in their commits: `a4a16dd` and `cfdc1dd` (Swift), and parts of `77da0e8`, `6253ce5` and `cf1c4a6`.
- **Unrelated, seen while linting:** detekt reports "compiler errors found during analysis" for `:android:app`, `:shared:core:network`, `:shared:core:database` and `:shared:transit:data`. These modules, apart from a comment in `:android:app`, are untouched here, and detekt still passes.

**Decisions**
- **Consent in memory until LB-409.** No screen can record consent before the consent dialog and toggles, so LB-017 adds no persistence and no Multiplatform Settings dependency (ADR-0018 update). The consequence: until LB-409, even debug builds print no telemetry in the running app. Events are checked in tests.
- **The logging implementations are gated too.** Debug builds use the same consent path as release builds, so the gate is exercised whatever the build.
- **AGENTS.md §11** said analytics events are "verified in the debug view", which no story can do before LB-409. It now says tests verify them until then. **The owner may prefer** a debug-only way to grant consent instead.
- **Redaction at the gate** rather than in each vendor implementation: the gate is the one place every non-fatal passes through, so no implementation can forget it.
- **Nested enums** (`PlanResult.Rejection`, `FavouriteAdd.Kind`, `Breadcrumb.Screen`) instead of ADR-0009's `PlanRejection` and `FavouriteType`: a core module cannot use the planner and favourites domains, and nested names cannot clash in Swift (ADR-0009 update).
- **`PlatformTelemetry`** is the one injection point, instead of two `initKoin` parameters. Android's `initKoin` would have had six, over detekt's limit, and Swift reads one named object.
- **The vendor-SDK rule** matches by group (Firebase, Crashlytics, Sentry, Bugsnag, Datadog, Amplitude, Mixpanel, Segment, New Relic, Embrace, Instabug, AppsFlyer, Adjust). It matches by artifact for Google Play services, whose group also holds the allowed Location library.

**Open / next**
- LB-409 (blocked by OQ-16) adds the consent dialog, the Settings toggles, a persisted `ConsentStore` and the vendor implementations.
- Feature stories call `AnalyticsTracker.track` from their ViewModels; a recording double can join `:shared:core:testing` when the first one needs it.
- **Next story:** LB-013 waits for the owner on the tile provider's terms (OQ-10). The remaining M0 work is LB-003 (fixtures, OQ-11) and LB-005 part 2.

---

## 2026-10-30: One commit identity, and the history rewritten to match

**Done**
- **The rule (owner, 2026-10-30):** every commit is authored and committed as `David Ruiz <davidru85@gmail.com>`, with no `Co-Authored-By` trailer, an agent's included (`AGENTS.md` §12, `engineering_workflow.md` §2).
  - Enforced by `scripts/checks/check-commit-identity.sh`, in the `pre-commit`, `commit-msg` and `pre-push` hooks and in `run-all.sh`. Its negative cases fail as they should: another author email, a co-author trailer, the old history, a non-merge commit by GitHub and a bot author. A merge commit with GitHub as committer passes.
  - `scripts/bootstrap.sh` sets the identity for the clone. `.claude/settings.json` turns off Claude Code's commit attribution.
  - Dependabot's commits are replayed as our own on a branch of ours (`engineering_workflow.md` §9).
- **Why:** the clone's local `user.email` was the GitHub noreply address, so 407 of the 462 commits on `main` carried it. Claude's co-author trailer was on 431.
- **The history rewrite (owner decisions, 2026-10-30):**
  - Every commit on `main` and on the open branches was rebuilt with the same tree, parents, dates and message, but with this author and committer and without the co-author trailer, and re-signed with the owner's SSH key. That covers the 28 web merges committed by GitHub and the one Dependabot commit too.
  - It was checked on a scratch clone first: the same 462 commits and 33 merges, identical trees and dates, and every commit signed.
  - The owner lifted `main`'s protection, force-pushed with `--force-with-lease` (`main` went from `3c9026a` to `83f3858`) and reapplied the protection with `scripts/github/protect-main.sh`, which left it exactly as before. This is a one-off exception to "no force pushes" (`engineering_workflow.md` §1.1).
  - Locally, the open branches were moved to their rewritten commits, and the 22 stale branches, all merged before the rewrite, were deleted.

**Decisions**
- **Merges on the web:** GitHub stays the committer of the merge commits it creates, the only exception to the rule. Their author is the owner's GitHub commit email: with "Keep my email addresses private" turned off and `davidru85@gmail.com` as the primary email, or that address chosen in the merge box, it matches.
- **Pull request descriptions** keep the "Generated with Claude Code" line; only commit trailers go.

**Open / next**
- The 30 closed pull requests keep their original commits, under read-only `refs/pull/*` refs, so the old identity is still visible there. Only GitHub Support can purge them.
- Commit hashes quoted in older log entries and pull request descriptions name the commits before the rewrite. They still open from their pull requests.
- Any other clone must be cloned again.

---

## 2026-10-30: LB-012: the app shell, with three tabs

**Done**
- **Merges:** the owner merged PR #28 and PR #29 (LB-011 parts 1 and 2), and `main` was synced.
- **Android:**
  - `AppShell`: `NavigationSuiteScaffold` with the three tabs, a short bar on compact widths and a collapsed wide rail from the medium width up, whatever the height (FR-NAV-05).
  - `TabNavigator`: one Navigation 3 back stack of `@Serializable` keys per tab (`:android:core:navigation`), saved across recreation and process death.
    - Re-selecting a tab pops it to its root or, at the root, asks the root to scroll to its top (`LocalScrollToTopRequests`, `ScrollToTopOnReselect`).
    - Back at the root of Near Me or How to Get There returns to Lines.
  - `NavDisplay`:
    - each tab has its own saveable-state and ViewModel decorators, and content keys that carry the tab;
    - screens cross-fade on the motion scheme's effects spring.
  - More, Settings (a titled placeholder until LB-403) and About data, which gains its top app bar.
  - The design system gains `MoreActionButton`, `UpButton`, `ScreenTitle` (a heading) and `ScreenTransitions`.
  - Tab labels: in the bar, one line that shrinks to fit at large font sizes; in the rail, full size and wrapping.
  - New dependencies, all AndroidX: navigation3-runtime and -ui 1.2.0, lifecycle-viewmodel-navigation3 2.11.0, and material3-adaptive-navigation-suite at Material 3's pin.
- **iOS:**
  - `AppShell`: a `TabView` with one `NavigationStack` per tab.
  - `TabRouter` keeps the paths. The selection setter, which also runs on a re-tap, pops a tab or counts a scroll-to-top request (`scrollToTopRequests` in `DesignSystem`).
  - The More toolbar action, `MoreView`, `SettingsView`, and About data's title.
  - The scheme runs the tests in Spanish, because the app's strings follow the process language.
- **Shared:** `plan.title` ("How to get there") titles the planner root on both platforms.
- **Tests:**
  - Android: `AppShellTest`, `TabNavigationTest`, `TabNavigatorTest`, `TabLabelTest`, `ScrollToTopOnReselectTest`, `TopAppBarActionsTest`, and screenshots of the shell (bar and rail), More and Settings.
  - iOS: `TabRouterTests`, `ShellUITests`, `LaunchTests`, and snapshots of More and Settings.
- **The first recordings exposed a defect, fixed:** at 200 % on a 320 dp phone, "Cerca de mí" drew over "Cómo llegar".
- **Reviews** (code-reviewer, android-ui-reviewer, ios-ui-reviewer subagents):
  - **Fixed:**
    - a comment that failed detekt (blocker);
    - a bar instead of the rail on short wide windows;
    - screen titles that were not headings;
    - navigation3-ui's 700 ms fade;
    - rail labels that shrank;
    - the planner root's title;
    - iOS screen snapshots at 0.99, which could lose a whole title;
    - scroll padding outside the scroll;
    - previews.
  - **Guards added:** per-tab content keys (mutation-checked), the up button, the Spanish tab order on iOS.
  - **Left:**
    - the whole scroll-to-top path, which the first scrolling root tests end to end (now an LB-101 criterion);
    - bar labels that shrink each to its own size at 200 %;
    - the TalkBack and VoiceOver passes on a device.

**Decisions** (taken while the owner was away; listed in the PR)
- More, and what it opens, are pushed onto the selected tab's stack, rather than shown as a sheet.
- Settings is a titled placeholder until LB-403. There is no onboarding placeholder, because FR-NAV-06 is LB-410's.
- Android back at the root of another tab returns to Lines, as Material recommends for a start destination.
- The rail follows FR-NAV-05's width rule rather than Material's recommended type.
- One cross-fade for every screen change, until a screen needs another motion.
- The iOS hosted tests run in Spanish (scheme `language: es`, `region: ES`).

**Process deviations**
- **Commit contents:**
  - `553b50b` (red) carried the first Roborazzi recordings, staged with `git add -A` after recording; `8e2cdca` replaced the faulty ones.
  - `e3c9a23` said "refactor not needed" without running `run-all.sh`, and a 121-character comment failed detekt; fixed in `fea76e2`.
  - `TopAppBarActionsTest` (`395fea9`) was written after the buttons it tests (`98e25f9`), and passed at once.
- **Commit types:**
  - `a5fee8c` and `14a22a7` are snapshot changes typed `test(ios)`;
  - `8412595`, `325deb1` and `cda07f4` span both platforms and have no scope. AGENTS.md §12 now lists a `shell` scope for such changes, which the PR's title uses.
- **Build:** a stale incremental compile reran an old `MainActivityTest` and showed a false green. Every run since uses `--no-configuration-cache --no-watch-fs`.

**Open / next**
- **Owner:**
  - the TalkBack and VoiceOver passes;
  - accepting ADR-0005, still Proposed, which LB-011 and LB-012 build on;
  - the earlier decisions still pending (iOS 18 runtime, iOS CI budget, Material 3 beta pin, OQ-11, OQ-16).
- **Next story:** LB-017, telemetry contracts with no-op and logging implementations, which OQ-16 does not block. LB-013 waits for the owner on the tile provider's terms (OQ-10).

---

## 2026-10-29: LB-011, part 2: the iOS design system

**Done**
- **CI of PR #28 (part 1):** the first run failed in "iOS build and tests". The simulator disappeared before the tests ran ("Unable to find a device matching the provided destination"), after a clean build. A rerun of the failed jobs passed: 52 checks, with the iOS job at 12.8 min. The Android screenshots recorded on macOS passed on Linux at once.
- **Dependency:** swift-snapshot-testing 1.19.7 (MIT, test-only), named in AGENTS.md §9. `iosApp/Package.resolved` pins it and its transitive packages.
- **`DesignSystem`** (it now depends on `Shared`, and only on it, which an architecture test checks):
  - `Tokens`: Android's palette as ARGB values, dynamic colours, `Typography`, spacing, radii and the 44 pt target.
  - `LineBadge`: VoiceOver says "Línea 3", and the badge always holds its code.
  - `EtaText`: Android's 13 cases in both languages, "a la 01:05" through an hour-keyed substitution, and the "Now" pulse through `phaseAnimator`.
  - `adaptiveGlass(in:)` and `AdaptiveGlassContainer`.
  - The two button styles, at least 44 pt, with a material fallback.
  - `GlassRendering`, which forces the fallback on iOS 26.
  - Words follow SwiftUI's environment locale.
- **Snapshots:**
  - the components in `DesignSystemTests`;
  - the glass in `iosAppTests/GlassSnapshotTests`, which needs the app's key window: a package test crashed there, and layer rendering drew Liquid Glass blank.
- **CI:** the iOS build-and-tests job runs them through the scheme, and uploads failed snapshots with the test results.
- **The first recordings exposed defects, fixed:**
  - the prominent button's white label on the light dark-mode accent;
  - an ETA truncated to "…";
  - words in the simulator's language rather than the view's.
- **Reviews** (ios-ui-reviewer and code-reviewer subagents):
  - **Fixed:**
    - the plain glass button's `.bordered` fallback was 1.3–1.6:1 (blocker);
    - buttons were 35 pt tall;
    - badges were smaller than their code at 200 %;
    - "Now" never pulsed when a row turned into it;
    - EtaText drew `Color.primary` rather than the measured `onSurface`.
  - **Tightened:**
    - snapshot precision and 200 % (`accessibility3`), with a regression snapshot for truncation;
    - the glass snapshots gained Increase Contrast, 200 % and disabled cases, at a tenth of the size;
    - type tokens, a bundle cache, verbatim preview text and dark glass previews;
    - Swift packages pinned, and their two pins checked equal.
- **Tests:** 53 architecture tests (the body of `14ec80c` says 55), and every iOS test on the iPhone 17e (iOS 26.5). `run-all.sh` passes.

**Decisions**
- The snapshot tests run inside the existing iOS job, not one of their own as the criterion says. A separate job would rebuild the framework on a second macOS runner, while the job is already over its 10-minute budget. Owner decision, with the iOS budget.
- iOS 18 is covered by forcing the fallback on iOS 26. Real iOS 18 runs wait for the nightly (LB-412), because no iOS 18 runtime is installed. Owner decision.
- The glass buttons are taller than Android's 48 dp controls, because the 44 pt minimum sits inside the system's own padding.

**Process slips:**
- `c349aab` brought the snapshot tests, `Bundle.designSystem(for:)` and three fixes as one green commit, without a red one.
- `566eb8e` (green) brought the two button styles, which no red test covered.
- `b7847e9` (red) added the snapshot dependency before anything used it.
- `2525019` is typed refactor but adds previews.
- `14ec80c` counts 55 architecture tests; there are 53.

**CI of PR #29:**
- **First run:** Xcode 26.6 rejected the `Package.resolved` written by Xcode 27.1 as out of date, because the `originHash` differs. `-onlyUsePackageVersionsFromResolvedFile` made that an error. `5c69080` drops the flag; the file is still copied into place, and its pins hold.
- **Second run:** the component snapshots passed. 6 of 10 glass snapshots failed on pixel match (0.164 against 0.99), although the uploaded images matched the references to the eye, with 0.0007 % of pixels 1 apart in the high byte. They were 16-bit Display P3. `7316ec4` renders the glass in 8-bit sRGB, like the components.
- **Third run:** 52 checks passed. The iOS job took about 21 min, up from 12.8–17.5, because it now resolves and builds swift-snapshot-testing.

**Open / next**
- PR #28's merge. This branch then merges `main`, and PR #29 shows part 2 only.
- The iOS job's time, for the owner's budget decision.
- **LB-012:** the app shell, the next story.

---

## 2026-10-28: LB-011, part 1: the shared contrast rule and the Android design system

**Done**
- **Merge:** PR #27 (LB-010 part 2) was merged with a merge commit (`401ae0f`). Its CI was green, with the iOS job at about 14 min.
- **Split:** LB-011 is in two parts. Part 1 covers shared code and Android; part 2 covers iOS.
- **Dependencies:**
  - Roborazzi 1.77.0 (Apache-2.0), for tests only.
  - Compose Material 3 is pinned at 1.5.0-beta02 over the BOM's 1.4.0, in which the expressive components and emphasized type are internal. ADR-0005 has a dated note.
- **Shared:**
  - `contrastRatio` and `onColorFor` (WCAG 2.2) in `:shared:core:common`. All 13 current line colours reach at least 4.5:1; line 3's purple is the closest, at 4.66:1 in black.
  - `EtaUi`, `EtaDisplay` and `EtaDelay` in `:shared:core:presentation`.
- **`:android:core:designsystem`:**
  - `Tokens`: the neutral placeholder schemes, a warning colour (`StatusColors`), type, shapes, spacing and touch target.
  - `LogroBusTheme`: `MaterialExpressiveTheme` with the expressive motion scheme.
  - `LineBadge`.
  - `EtaText`: lines and spoken labels from resources, the "Now" pulse, the low-confidence icon.
  - Previews, and Roborazzi references in light, dark and at 200 % font.
- **App and About data:**
  - The app draws on `LogroBusTheme`, its bars follow dark mode, and its window theme has a night variant.
  - About data uses the theme and the spacing tokens, with screenshots of its four states.
- **Checks:**
  - An architecture test keeps `ExperimentalMaterial3ExpressiveApi` inside the design system; a mutation check confirmed it fails elsewhere.
  - Another requires the `android-screenshots` CI job to list every module with reference images. That job runs `verifyRoborazziDebug` per module behind the Kotlin gate.
- **Screenshot tests run on SDK 36.** On SDK 37, the target, Robolectric 4.17 drew only the first capture of a test run, and later ones came out as the bare window background with the right size. A probe found SDK 34, 35 and 36 correct.
- **Reviews** (code-reviewer and android-ui-reviewer subagents; no blockers):
  - **Fixed, with TDD:**
    - The "Now" pulse dropped to 3.6:1.
    - Nothing tested the pulse's reduced-motion hold.
    - TalkBack and the screen disagreed for some `EtaUi` combinations.
    - Spanish said "a las 01:05" and was barely tested.
    - There was no warning colour token.
    - The window theme stayed light in dark mode.
  - **Fixed in the green and refactor commits:**
    - `LocalContext.resources` raised a lint warning.
    - The pulse recomposed the row every frame.
    - The icon did not grow at 200 %.
    - The 200 % EtaText image was cut off.
    - The dark references were half light.
    - The previews were missing.
    - The motion scheme was implicit.
    - `BLACK` and `WHITE` were public.
    - A comment on the API 26 scrim was wrong.

**Decisions**
- The expressive component wrappers (`LbLoadingIndicator`, `LbButtonGroup`) are built with the first screen that needs each; no screen does yet.
- The window themes set no colour, so the brand colours live only in `Tokens`.
- `SCREENSHOT_SDK` is defined once per module until an Android test-fixtures module exists.

**Process slips** (recorded here, since history is not rewritten):
- `82b53e0` was committed with detekt and Kover failing: a chained command committed regardless. `760b2a7` fixed it, and every commit now follows its checks with `&&`.
- `343b00e`, a green commit, is typed `ci:` without a scope.
- Three commits do more than their phase:
  - `c1d6ef1` (red) also pins Material 3;
  - `15f17af` (green) also edits `TokensTest`;
  - `05ca286` (green) fixes its red test's locale.
- A command with `rm -rf` was denied by the settings; it was split and rerun without it.

**Open / next**
- **Part 2 (iOS):**
  - `DesignSystem` tokens, `LineBadge`, `EtaText`, the `adaptiveGlass` helpers and glass button styles;
  - swift-snapshot-testing and its CI job.
  - The iOS 18 fallback needs an iOS 18 runtime, which this machine lacks. The proposal is to force the fallback path in iOS 26 snapshots and leave real iOS 18 runs to the nightly (LB-412). Owner decision.
- **The CI job's first run:** the references were recorded on macOS and are verified on Linux. If anti-aliasing differs, record them on Linux or set a small threshold, and document the choice in `testing_strategy.md` §4.6.
- **Still pending owner decisions:**
  - the deny-list patterns (`--force-with-lease`, `+branch`, `--no-verify origin main`);
  - OQ-11 and OQ-16;
  - the iOS CI budget, the iOS connect timeout, and the foreground-fetch floor.

---

## 2026-10-28: LB-010, part 2: the ViewModel conventions, on About data

**Done**
- **Owner decisions, 2026-10-28:**
  - The departures of LB-007's schema from spec §8.8 are confirmed (`architecture.md` §4.3).
  - "Continue with LB-010": part 2 now, instead of LB-011.
  - `git push` to any branch but `main` without asking. `.claude/settings.json` moves it from `ask` to `allow`, because an ask rule beats any allow rule (`agent_harness.md` §4.1, commit `d43b34d`).
- **Merge:** PR #26 (part 1) was merged with a merge commit (`d9fa9c9`), and `main` was merged into this branch.
- **The example screen:** LB-101 waits for the fixtures (OQ-11), so part 2 builds on About data's last sync per dataset (LB-404, FR-SET-03). It reads `sync_state` and needs no source data.
- **Dependencies** (Apache-2.0):
  - kotlinx-datetime 0.8.0 (ADR-0015, which gets a dated note);
  - `koin-androidx-compose` 4.2.2 (ADR-0014);
  - androidx `lifecycle-runtime-compose` and `lifecycle-viewmodel` 2.11.0;
  - Compose `ui-tooling`.
- **Shared:**
  - `MadridDateTime` (display only).
  - `ErrorReason`.
  - `StaticDataset` and `SyncStatusRepository`, with `SyncStatusRepositoryImpl`: stable keys, catches at the database boundary.
  - `ObserveLastSyncs`.
  - `AboutDataUiState` (`Loading`, `Content`, `Error`), `AboutDataEvent.Retry` and `AboutDataUiMapper.state`.
  - In the umbrella: `UseCases`, which `SharedDependencies` implements; `preview()`, which returns scripted `PreviewUseCases`; one Koin module each for transit data and settings.
- **Android:**
  - `AboutDataViewModel` (`stateIn` with WhileSubscribed 5 s, Retry through `flatMapLatest`).
  - `AboutDataRoute` (`koinViewModel()`) and a stateless `AboutDataScreen`: placeholders while loading, Error with Retry, scrolling.
  - Times in ICU's pattern for the locale and the 12 or 24-hour setting.
  - `AboutDataPreviewData` with light and dark previews.
  - `settingsUiModule`, which the app passes to `initKoin(uiModules)`.
- **iOS:**
  - `AboutDataViewModel`: a cheap init, `observe()` with `for await`, and `attempt` for Retry.
  - `AboutDataView`: `.task(id:)`, `onEnum(of:)`, a redacted list while loading, `ContentUnavailableView` for Error, and the environment locale.
  - A string catalogue for the target.
  - `FeatureSettingsTests`, run by the iosApp scheme.
- **TDD:** 8 red-green cycles, the last 3 from the reviews, then a refactor.
- **Tests:** all pass; `run-all.sh` passes.
  - shared: on the Android host and the iOS simulator;
  - `:android:feature:settings`: 10, and `:android:app`: 8;
  - iOS: 13 through `scripts/ios/test.sh`.
- **Reviews** (code-reviewer, data-rules-reviewer, android-ui-reviewer and ios-ui-reviewer subagents; no blockers):
  - **Fixed, with TDD:**
    - The repository threw SQLite's exceptions, which would crash both apps, and the screens had no Error state (AGENTS.md §5 rule 11, FR-ERR-05).
    - The first load was a spinner (spec §7.3).
    - Android had no cancellation test.
    - Times ignored the 24-hour setting on Android and SwiftUI's locale on iOS.
    - The Swift ViewModel resolved Koin in its init.
    - Before the graph started, a lookup failed with no hint.
  - **Docs:**
    - The static sync is LB-008, not LB-006.
    - §4.4 now says that an unchanged hash still records `last_success`, and that a failed sync must not erase it.
    - The criteria of LB-404 and LB-405 met early are listed under those stories.
    - `agent_harness.md` says exactly what the deny list catches.

**Decisions**
- LB-010 is done. LB-404 is in progress: the rest of its page (data source, disclaimer, attributions) comes with M4.
- Retry starts a new read on each platform: Android through `flatMapLatest` over a counter, iOS through `.task(id:)` on `attempt`.
- The error strings (`error.*`) live in the settings feature for now. LB-011 moves the Error component and the time formatters into the design systems.

**Open / next**
- For the owner: the deny list does not catch every spelling of a force push or of a push to `main`, such as `--force-with-lease`, a `+branch` refspec or `--no-verify origin main`. `main`'s protection on GitHub still rejects direct pushes. Should the deny list add those patterns?
- **LB-011:**
  - design tokens instead of Material's baseline schemes;
  - Roborazzi and swift-snapshot-testing for About data's states;
  - a shared Error component;
  - the `MadridDateTime` formatters.
- **LB-012:**
  - the More entry, with a title and back action;
  - inset padding;
  - per-entry ViewModel stores (`rememberViewModelStoreNavEntryDecorator`).
- **LB-006:** move the Madrid zone where `:shared:transit:data` can use it too.
- **LB-101:** the iOS ViewModel test job (`Feature<X>` matrix).
- **Definition of done:** TalkBack and VoiceOver passes over About data.
- **Still pending owner decisions:** OQ-11, OQ-16, the iOS CI 10-minute budget, the iOS connect timeout, and the foreground-fetch floor.

---

## 2026-10-28: LB-010, part 1: the dependency graph and SKIE

**Done**
- **Merge:** PR #25 (LB-007) was merged. Its CI was green, with the iOS job at about 16.4 min.
- **Split:** LB-010 is in two parts. Part 2, the ViewModel conventions, needs a use case to show and comes with LB-101 (`backlog.md`).
- **Dependencies** (both Apache-2.0):
  - Koin 4.2.2 (`koin-core`, ADR-0014);
  - SKIE 0.10.15 (ADR-0004), kept in the catalogue beside Kotlin 2.4.20 as a documented pair.
- **TDD:** 6 red-green cycles, then 3 more from the reviews, with 2 refactors.
- **`:shared:umbrella`:**
  - `coreModules` and `startGraph`, which refuses overrides;
  - one `ServerCorrectedClock` bound as `AppClock`;
  - `SourceClient`, which reads `api_base_url` on every call;
  - `DefaultPollingEngine` on an application scope that `stopKoin` cancels;
  - the database, whose driver `stopKoin` closes;
  - per platform: the logger, the app lifecycle (created at start, on the main thread), the location provider, the client identity and the SQLite driver.
- **Startup:**
  - `initKoin` takes the app's version and build type, and returns nothing, so no Koin type reaches the shells.
  - Android: `LogroBusApplication` passes `BuildConfig` (now on).
  - iOS: `LogroBusApp.init` calls SKIE's `doInitKoin(appVersion:debug:)`.
  - `SharedDependencies.isReady` says whether the graph has started.
  - Robolectric tests run on `TestLogroBusApplication`, which stops the graph after each test.
- **SKIE:**
  - The tab labels switch over `TopLevelDestination` without a default, which settles the `TODO(LB-010)`.
  - One Swift file now imports Foundation itself.
- **iOS linking:** referencing the iOS driver made the umbrella's test binary fail to link with undefined `_sqlite3_*`, as LB-007's review predicted. Every iOS binary of the umbrella now links `-lsqlite3`; the app links SQLite through `Package.swift`.
- **Tests:** all pass, and `run-all.sh` passes.
  - the umbrella: 8 on the Android host and 6 on the iOS simulator;
  - `:android:app`: 7;
  - the iOS app: 8, through `scripts/ios/test.sh`.
- **Reviews** (code-reviewer and data-rules-reviewer subagents; both asked for changes, with no blockers):
  - **Fixed, with TDD:**
    - Koin allowed overrides, so a later module could quietly replace the clock or the engine.
    - The log level followed the shared binary, so a release archive on a debug framework, or Android's debuggable benchmark variant, logged verbosely. The version could also come out empty, giving `LogronoBus/ (…)`.
    - Stopping the graph left the scope and the database open.
  - **Refactors:**
    - one test Application instead of a teardown in every Robolectric class;
    - the Fused provider is lazy.
  - **For part 2:** the list of roots in `GraphAssertions` is written by hand, so new bindings must be added to it.
  - **Process notes:**
    - `e9b1f97`, the red commit for the log level, turned production code back into a stub. The choice had shipped untested in `7f4e15f`, so its test came after the code.
    - `7f4e15f` and `1da9f6c` lack `TDD: refactor not needed`.
    - `1da9f6c` also changes `initKoin`'s return type and the iOS tests.
    - `d92cf61` is a red commit that changes only production Swift; the failing build is its test.
    - `42143c6` was committed without `run-all.sh`, and its `::println` default broke detekt's `ForbiddenMethodCall` until `c09d554`.

**Decisions**
- **Agent:**
  - The Fused location provider is lazy, while `ProcessAppLifecycle`, `NotificationAppLifecycle` and Core Location are created when the graph starts, on the main thread.
  - `SharedDependencies.isReady` is public. It is the Swift-visible check that the graph has started, and part 2's preview fakes can use it.

**Open / next**
- **LB-010 part 2, with LB-101:**
  - `koinViewModel` and the Android ViewModel convention;
  - Swift ViewModels over `SharedDependencies` use cases;
  - `SharedDependencies.preview` fakes;
  - the cancellation test.
- **Still waiting on the owner:**
  - LB-005's DTOs wait for LB-003's fixtures (OQ-11);
  - the iOS CI budget;
  - the spec §8.8 departures of LB-007.

## 2026-10-27: LB-007: local database and stop search

**Done**
- **Merge:** PR #24 (LB-005 part 2, first half) was merged. Its CI was green, with the iOS job at about 12.6 min.
- **Criteria aligned with the ADRs** at the start:
  - `arrival_snapshot` waits for v1.0 (ADR-0008);
  - migration tests start with the first schema change after release (ADR-0013), since version 1 has no previous schema.
- **Dependencies:** SQLDelight 2.4.1 (Apache-2.0, ADR-0013):
  - the Gradle plugin and the coroutines extension;
  - the Android, native and JDBC SQLite drivers, JDBC for tests only.
  A spike confirmed that its plugin works with AGP 9's KMP Android plugin.
- **TDD:** 6 red-green cycles, then 4 more from the reviews, with 4 refactors and 2 guard tests.
- **`:shared:core:common`:** `foldForSearch` and `searchWords`. They fold case, accents, `ñ` and combining marks, and split on anything but letters and digits.
- **`:shared:core:database`** (`architecture.md` §4.3):
  - the schema of spec §8.8 plus the MVP additions, in five `.sq` files;
  - FTS4 stop search through `searchStops` and `searchableName`;
  - triggers that keep the index in step with every write;
  - `Instant` columns in UTC epoch milliseconds;
  - `logroBusDatabase(driver)`, and `logroBusDriver` on each platform.
- **Tests:**
  - 12 database tests on the Android host and 12 on the iOS simulator. The Android file test runs on Robolectric, so the JVM, Robolectric's SQLite and iOS's system SQLite all support FTS4.
  - Search over 254 stops: the median is well under 100 ms on both.
  - `scripts/ios/test.sh` passes.
- **Build:**
  - detekt skips sources under `build/`, in its baseline task too. SQLDelight's generated classes, such as `Fare_product`, failed the naming rules.
  - The Swift package links the system SQLite, because `Shared` is a static framework.
- **Found while testing:** spec §8.8's `content = 'stop'` fails in SQLite with "unrecognized parameter", because FTS4 rejects spaces around `=`. The schema writes `content='stop'`.
- **Reviews** (code-reviewer and data-rules-reviewer subagents; both asked for changes, with no blockers):
  - **Fixed, with TDD:**
    - `INSERT OR REPLACE` left a renamed stop findable by its old name: REPLACE fires no delete trigger. A guarded `BEFORE INSERT` trigger fixes it.
    - The FTS4 tokenizer and the query split words differently, so a typographic apostrophe, a non-breaking space, a dash or a decomposed accent hid matches. There is now one split for both.
    - Constraints: `NOT NULL` key columns, `UNIQUE` favourites, and lookup indexes. The schema test now also checks keys, defaults and indexes.
    - Instants are typed, in UTC epoch milliseconds.
  - **Refactors:**
    - one public, parameterless iOS driver function;
    - the iOS file test cleans up before and after;
    - the timing test uses the median of 20 runs;
    - detekt baselines skip generated code too.
  - **Build:** the SQLite link for iOS. It is verified only by the app still building; it is exercised once LB-010 calls the driver.
  - **Process notes:**
    - `d7cfd5a` fixes a red test's expectation in its own commit, with no `TDD:` line.
    - `eb64a3f` (red) also reformats `SchemaTest`.
    - `6133785` (green) changed one expected order, because the folded name lost its punctuation; its body explains this.
    - `829698a` (timing) is a guard test.
    - `1782de7` (SQLite link) was committed after SwiftLint and the iOS tests but without `run-all.sh`. Its multi-line Swift targets broke `lb001_logroBusUiPackageHasTheArchitectureTargets`, which reads one target per line. The next commit restores single-line targets. Lesson: run `run-all.sh` before every commit that touches a file a check reads, not only Kotlin.

**Decisions**
- **Agent, for the owner to confirm:** these departures from spec §8.8, recorded in `architecture.md` §4.3. They are free to change before the first release.
  - `NOT NULL` keys in `direction_code_cache` and `sync_state`;
  - `UNIQUE(type, ref_id)` on `favourite`;
  - `NOT NULL` `plan_recent.created_at`;
  - two indexes;
  - `content='stop'`.
- **Agent:**
  - The fare, recharge-point and news tables of spec §8.8 exist now, empty, for v1.0. This avoids a migration later.
  - A stop's searchable name is computed in `:shared:core:database` (`searchableName`), so the sync (LB-008) cannot fold it differently from the query.

**Open / next**
- **LB-008:**
  - write stops through plain inserts, or `INSERT OR REPLACE`, in one transaction, with `searchableName` for `normalized_name`;
  - add the queries it needs. The first query on `favourite`, `news` or `plan_recent` requires its `InstantColumnAdapter` in `logroBusDatabase`.
- **LB-010:** create `logroBusDatabase(logroBusDriver(context))` on Android, and `logroBusDatabase(logroBusDriver())` on iOS.
- **LB-209 and LB-411:** the search benchmark on the reference devices.
- **LB-005, second half:** still waits for LB-003's fixtures (OQ-11).

## 2026-10-27: LB-005, part 2, first half: GET queries and no cleartext

**Done**
- **Merge:** PR #23 (LB-005 part 1) was merged. Its CI was green, with the iOS job at about 15 min.
- **TDD:**
  - three red-green cycles: the GET query, Android cleartext, and a path that carries a query;
  - two refactors;
  - three guard tests, which pass over behaviour already in place: the query log, iOS ATS, and stronger encoding cases.
- **`SourceClient.get(path, kind, query)`** (API-05, API-06):
  - names and values are percent-encoded, except the commas in values, so `lines=1,33` goes out as the source was captured (spec §4);
  - the failure log names the path without the query;
  - a path that carries `?` is rejected as a programming error, before the catch.
- **Cleartext HTTP refused** (NFR-SEC-01):
  - `[Android]` `res/xml/network_security_config.xml` has one `base-config` with `cleartextTrafficPermitted="false"`, named by the manifest. `NetworkSecurityTest` (Robolectric) reads the hidden `ApplicationInfo.networkSecurityConfigRes` and the config's elements. Mutation-checked.
  - `[iOS]` the generated Info.plist has no `NSAppTransportSecurity` key, so the default ATS applies. `lb005_appUsesDefaultTransportSecurity` passes in `scripts/ios/test.sh`.
- **Tests:** 31 network tests on the Android host and 30 on the iOS simulator, plus the Android app test and the 7 iOS app tests, all passing.
- **Review** (code-reviewer subagent): approved with nits. Applied:
  - the rejection of a path with a query;
  - wider encoding cases;
  - the ATS test checks its own bundle;
  - the KDoc on commas.
- **Process notes:**
  - The Android green commit `2a9103a` also fixed its red test, which read the attribute in the `android` namespace; the network security config's attributes have none. Next time, a test fix goes in its own `test(...)` commit.
  - `a9a115f` (iOS ATS) and `9512407` (encoding cases, ATS bundle) are guard tests without a `TDD:` line. The ATS test was not mutation-checked, which would need a regenerated project.

**Decisions**
- **Agent:**
  - Commas in query values are sent as is, matching the captured requests. A comma inside free text is harmless for parsers that follow RFC 3986, and no BFF endpoint takes free text.
  - The Android network security config has no `debug-overrides`: debug builds refuse cleartext too.

**Open / next**
- **LB-005 part 2, second half:** the DTOs and services for API-01 to API-08. They wait for LB-003's fixtures, which wait for the owner's go-ahead on OQ-11.
- **Next without the fixtures:** LB-007, the SQLDelight database.

## 2026-10-27: LB-005, part 1: `SourceClient` and lenient JSON

**Done**
- **Merge:** PR #22 (LB-009) was merged; LB-005 started from the hand-over.
- **Dependencies:** Ktor 3.6.0 (core, encoding, OkHttp, Darwin, and mock for tests) and kotlinx.serialization 1.12.0, with the Kotlin serialization plugin (ADR-0012). Both are Apache-2.0. kotlinx-datetime still waits for LB-006 (ADR-0015).
- **TDD:** 13 red-green cycles, then 4 more from the reviews, plus 2 guard tests and 8 refactors: 44 commits. All 28 tests pass on the Android host and 27 on the iOS simulator. `GzipTest` runs on the JVM only.
- **`:shared:core:network`** (`architecture.md` §4.1):
  - `SourceClient`, which returns `AppResult<JsonElement>`:
    - headers, timeouts and the 2xx rule (DD-15);
    - `Server(status)`, `Data` and `Network` mapping;
    - every response's `Date` corrects `ServerCorrectedClock`;
    - failures are logged without bodies or messages.
  - `ClientIdentity` and `clientIdentity(appVersion)`, and the OkHttp and Darwin engines.
  - `parseHttpDate` and `serverNow`.
  - The lenient JSON helpers: `SourceJson`, `arrayAt`, `decodeEach` and `decodeAs`.
- **Found while testing:**
  - NSURLSession caches responses and stores cookies by default. The iOS engine now turns both off, because a cached response would carry stale arrivals and a stale `Date`.
  - The JSON reader takes an unquoted word as a literal, so a short HTML page or `OK` passed as success. A body must now be an object or an array.
- **Reviews** (code-reviewer and data-rules-reviewer subagents; both asked for changes, with no blockers):
  - **Fixed, with TDD:**
    - **Engine timeouts.** Ktor's OkHttp engine kept OkHttp's 10 s read timeout, so a 30 s static download failed at 10 s on Android. The engines' socket timeouts now follow the request timeout.
    - **Any failure to send is `Network` for that call.** A malformed `api_base_url` or a redirect loop used to throw past the `IOException` catch. Only the caller's own cancellation propagates, which a mutation test checks.
    - **`Age`.** Behind a caching proxy (spec §8.10), stale `Date`s outvoted the live one and ran the clock 40 min slow. A response now counts from `Date` plus `Age`.
    - **Early dates.** A time before 2026, such as a proxy at 1970, is ignored.
  - **Refactors:**
    - the engine constructor is public, so the part-2 repository tests can inject a `MockEngine`;
    - the iOS version is read from `NSProcessInfo`, which any thread may use, instead of `UIDevice`;
    - the dropped-item test asserts the exact message;
    - the platform tests close their clients;
    - `GzipTest` documents what it pins.
  - **Not applied:**
    - **Catching `Exception` in `decodeEach` and `decodeAs`.** kotlinx.serialization 1.12 already wraps what a type throws while decoding, an `init` check or `error()` in a custom serializer, in a `JsonDecodingException`, which the catch holds. A guard test pins this.
    - **A separate 5 s connect timeout on iOS.** NSURLSession has only an idle timeout, so a 5 s value would also cap the time to first byte of the static payloads.
  - **Process notes:**
    - `8ec86c1` is a red commit typed `fix(network)`; it should be `test(network)`. History is not rewritten.
    - `9b09518` (gzip) and `f59064d` (an item its type rejects) are guard tests that passed when added. They have no `TDD:` line; their bodies explain this and the mutation or probe that backs them.
    - `39b5635`, a green commit, changed its red test's input. Its body explains why: the space-free HTML body exposed the scalar gap, which the next cycle fixed test-first.

**Decisions**
- **Agent, for the owner to confirm:** on iOS, connecting is bounded by the 10 s or 30 s request timeout, not by 5 s. The Darwin engine has no connect timeout of its own (`api_spec.md` §1).
- **Agent:**
  - Ktor stays behind `SourceClient`. Only `HttpClientEngine` is public, for tests; data modules get `JsonElement` and the JSON helpers.
  - `parseHttpDate` reads IMF-fixdate only, with no exception path. Ktor's `fromHttpToGmtDate` can throw past its own catch, and kotlinx-datetime enters with LB-006.
  - The earliest accepted server time is 2026-01-01, a compiled bound. Capping the offset instead would also block correcting a device clock that is wrong.

**Open / next**
- **LB-005 part 2:**
  - the DTOs and services for API-01 to API-08, whose contract tests wait for LB-003's fixtures (OQ-11);
  - GET query parameters (API-05, API-06), logging the path without its query;
  - the cleartext settings in both apps.
- **For LB-010:**
  - create one `SourceClient` in the composition root, with `clientIdentity(appVersion)`, the `ServerCorrectedClock`, `{ remoteConfig.get(ApiBaseUrl) }` and the `Logger`;
  - pass the same clock to `DefaultPollingEngine`.
- **iOS CI budget:** still raised with the owner.

## 2026-10-27: LB-009: `PollingEngine`

**Done**
- **Merges (2026-10-26):** PR #20 (ADR acceptance) and PR #21 (LB-004 part 2) were merged. #21 conflicted with #20 in the docs only (`AGENTS.md`, both READMEs, this log); `main` was merged into it, keeping both sides. Its first CI run then failed in the iOS job: the simulator the build had just used vanished before the test step (`xcodebuild` exit 70). Rerunning the failed jobs passed, so this was a runner flake.
- **TDD:** seven red-green cycles for the criteria, plus one for the newest caller's interval, and six more cycles from the reviews (below), with three refactors.
- **`:shared:core:polling`:**
  - `PollingEngine`, `PollKey` and `Timed`.
  - `DefaultPollingEngine`: per key, a `StateFlow` of the latest value and one loop, which runs while it has collectors and the app is in the foreground. Details in `architecture.md` §4.5.
  - 20 tests in `commonTest`, on virtual time, run on the Android host and the iOS simulator.
- **Reviews** (code-reviewer and data-rules-reviewer subagents; both asked for changes):
  - **Fixed, with TDD:**
    - A `fetch` that threw killed its key and could crash the app or cancel every key. A timeout inside it stopped polling silently. Both now fail the key alone (`Data` or `Network`), are logged by type and key, and are retried. The keys run under a child supervisor.
    - Every activation fetched at once and reset the backoff, so re-entering a screen every 2 s polled every 2 s. A key now keeps its schedule, and the backoff, across activations. A return to the foreground fetches at once unless the last attempt is under 10 s old.
    - A source failing every other request was retried after 2 s every cycle. Two successes in a row are now needed to restart the ladder.
    - Clamping at the floor put half of the 10 s waits at exactly 10 s, so clients at the floor polled in step. The jitter is now reflected above the floor.
    - A screen coming back after 30 min first got a 30-min-old value. After 120 s without collectors, the key forgets its value.
    - An infinite interval parked the key forever. `poll()` now rejects it.
  - **Tests strengthened:**
    - two collectors overlapping when the interval changes;
    - jitter on both sides of the interval;
    - polling carrying on after the foreground fetch;
    - the public constructor's jitter (mutation-checked).
  - **Not applied:**
    - **Jitter on the backoff.** The criterion asks for exact 2, 4, 8, 16 and 30 s. Clients fail at their own jittered polling instants, so their retries keep that spread. Only requests in flight at the very moment of a source restart retry in step.
    - **A typed `PollKey<T>`.** A phantom type does not stop two keys with the same string and different types. `PollKey` documents a `<resource>:<id>` scheme instead.
    - **`refresh(key)` and an in-flight flag on `Timed`,** for the Refreshing state, Tap to refresh and Retry (spec §7.3). These are additive changes, so they are recorded on LB-213, the story that needs them.
  - **Process notes:**
    - The first green stubbed constructor parameters five cycles early, so detekt stayed red on unused parameters until they were used.
    - 96656e7, a refactor, added the public constructor without a test. f0cb64d added that test later.
    - f0cb64d itself was committed with a ktlint finding, because the check's exit status was lost in a command chain. df6450f fixes it.
    - Every green commit without a refactor after it says `TDD: refactor not needed`.

**Decisions**
- **Agent, for the owner to confirm:**
  - **Return to the foreground:** spec §8.6 asks for an immediate fetch, and the engine now waits up to the 10 s floor if the last attempt is younger than that. This reconciles the immediate fetch with the floor: a value under 10 s old is still fresh (45 s, spec §5.7). The LB-009 criterion says so.
  - **Retries** after 2, 4 and 8 s stay below the 10 s floor, as FR-ERR-03 requires (`architecture.md` §4.5).
- **Agent:**
  - `PollingEngine` is the one place outside the network and database boundaries that catches exceptions (`code_style.md`).
  - Freshness is computed by the presentation layer from `fetchedAt` on a UI ticker, because repeated identical failures are merged into one emission.

**Open / next**
- **LB-009 is done** when this merges.
- **For LB-010:** create one `DefaultPollingEngine` in the composition root, with an application-wide scope, `AppLifecycle`, the `ServerCorrectedClock` and the `Logger`.
- **Next: LB-005, part 1:**
  - the `HttpClient` factory: headers, timeouts, the 2xx rule, and `Date` sampling into `ServerCorrectedClock`;
  - lenient `Json` and per-item decoding.
  These are all tested with `MockEngine`. The DTO contract tests wait for LB-003's fixtures (OQ-11).
- **iOS CI budget:** the iOS job is still over 10 min (about 13 min on #21's rerun). It's raised with the owner.

## 2026-10-26: LB-004, part 2: logger, dispatchers, app lifecycle and location

**Done**
- **TDD:**
  - Five red-green cycles (`Logger`, `AppDispatchers`, `AppLifecycle`, the shared half of `LocationProvider`, and its Fused and Core Location adapters) and one refactor.
  - Then four red-green cycles and three refactors from the reviews (below).
  - The part 2 acceptance criteria were added to `acceptance_criteria.md` first.
  - **Process slip:** six green commits that had no refactor after them lack the `TDD: refactor not needed` line (`testing_strategy.md` §7.1): `25a7a29`, `5b303b4`, `c07c747`, `5ba76c3`, `3acf856`, `dcc595e`. They were pushed, and history is never rewritten, so this entry records it instead.
- **`:shared:core:common`:**
  - `Logger`, with `NoOpLogger` and `ConsoleLogger`: minimum level, injectable output.
  - `AppDispatchers`, injected, with a one-dispatcher constructor for tests.
  - `AppLifecycle.isInForeground`: `ProcessAppLifecycle` on Android, `NotificationAppLifecycle` on iOS.
- **`:shared:core:location`:**
  - `LocationProvider`, `LocationPermission` and `LocationFix`.
  - A shared `callbackFlow` starts the platform updates on collection and stops them on cancellation (FR-LOC-03). Without permission it completes at once; it keeps only the newest fix and runs on the injected main dispatcher.
  - `fusedLocationProvider` (Android) and `coreLocationProvider` (iOS), behind an internal `LocationSource`, so the platform code stays thin.
- **Tests** run on the Android host and on the iOS simulator. The Fused adapter's host tests use Robolectric, with a fake client built as a `Proxy`. The Core Location test uses a `CLLocationManager` subclass that records start and stop.
- **Reviews** (code-reviewer and data-rules-reviewer subagents):
  - **Fixed, with TDD:**
    - `ConsoleLogger` printed throwable messages, which can quote a URL with search text or a payload with coordinates. It now prints the types of the cause chain only (NFR-PRIV-02).
    - The location flow never ended once started. It now completes when the permission is revoked (iOS: an authorization change or `kCLErrorDenied`) or the Fused request fails (FR-LOC-02).
    - iOS `AppLifecycle` could stay in the background after a cold launch. It now also follows did-become-active.
    - The iOS permission manager could be created off the main thread. It is now created with the source.
  - **Tests added for existing behaviour:** `flowOn(main)` and the conflated buffer. Each test fails when its behaviour is removed.
  - **Refactors:**
    - The provider factories take `AppDispatchers`.
    - The test constructors are `internal`.
    - Robolectric's JDK flag lives in the conventions.
  - **Not applied:**
    - Pausing location in the provider while the app is in the background. Stopping updates with hidden screens is LB-201's criterion, and the KDoc no longer claims the provider does it.
    - Dropping old cached fixes. A cached first fix gives a quick nearby list, and the next fix replaces it. LB-202 can revisit this.
  - **For LB-010:** create `AppLifecycle` and the location providers on the main thread, in the composition root (Koin `createdAtStart`).
- **New dependencies** (`libs.versions.toml`):
  - kotlinx-coroutines 1.11.0;
  - lifecycle-process 2.11.0;
  - play-services-location 21.4.0;
  - for tests: Turbine 1.2.1, kotlinx-coroutines-test and Robolectric.
- **App changes:**
  - Play services adds a non-exported `GoogleApiActivity` and the `com.google.android.gms.version` metadata to the merged Android manifest, and no permission.
  - The iOS binary does not reference Core Location yet, because nothing exported uses it.

**Decisions**
- **Owner (2026-10-26):**
  - Google Play services Location is approved for Android. Its licence, the Android Software Development Kit License, is recorded as an approved exception in `engineering_workflow.md` §9.
  - The LB-004 defaults are confirmed (`api_spec.md` §8).
- **Agent:**
  - The permission request, the manifest permissions and the iOS purpose string stay with LB-201, which shows the permission flow. The backlog notes that App Store Connect requires the purpose string once the binary uses Core Location.
  - Update parameters, which the spec does not give: high accuracy, every 5 s and 10 m on Android; nearest ten metres and 10 m on iOS. Updates run only while a screen collects them.
  - Logging in release is errors only, as `security_privacy.md` §4 says. `architecture.md` §7 said no-op and now matches.

**Open / next**
- **LB-004 is done** when this merges.
- **Next: LB-009 (`PollingEngine`).** It needs only `AppLifecycle`, `AppDispatchers` and `RemoteConfig`, which now all exist.
- **LB-003 (fixtures)** records from the live source and waits for the owner's go-ahead (OQ-11). LB-005's contract tests need those fixtures.

## 2026-10-26: LB-014: ADR-0012 to ADR-0018 accepted

**Done**
- PR #18 (LB-004, part 1) and PR #19 (LB-014) were merged.
  - #19 was stacked on #18. After #18 merged, GitHub retargeted #19 to `main` and marked it `BEHIND`, because the branch protection requires an up-to-date branch. `main` was merged into it (a merge commit, no rebase), the tree did not change, and its CI then ran green.
- **ADR-0012 to ADR-0018 are accepted** and marked so in each ADR and in the index.

**Decisions**
- **The owner accepted all seven ADRs** (2026-10-26), including ADR-0016, which refines spec §8.3: Coil 3 on Android only, and `AsyncImage` on iOS.
- **LB-014 is `blocked` on OQ-16.** The only ADR left is the analytics and crash backend.
- **Owner decisions for LB-004 part 2**, recorded with that story:
  - Google Play Services Location is approved for the Android `LocationProvider`.
  - The three provisional defaults are confirmed.

**Open / next**
- LB-004, part 2: `Logger`, `AppDispatchers`, `AppLifecycle` and `LocationProvider`.
- The libraries of ADR-0012 to ADR-0018 can now enter the version catalog with the first story that uses each one.

## 2026-10-26: LB-014: ADRs for the spec §8.3 choices

**Done**
- This was started while the owner was away, because writing the ADRs needed no owner decision. The branch is stacked on LB-004's, to avoid conflicts in this log and the READMEs.
- **Seven ADRs, all Proposed** (NFR-MNT-03). Each names its rejected alternatives with reasons:
  - **ADR-0012, Ktor and kotlinx.serialization.** Rejected: platform clients, which would duplicate the lenient decoding; Ktorfit; JVM-only JSON libraries.
  - **ADR-0013, SQLDelight.** Rejected: Room KMP (an annotated-entity model, and KSP on every target); Realm (deprecated); files.
  - **ADR-0014, Koin, with a graph-verification test.** Rejected: kotlin-inject and Metro (compile-time plugins); manual DI; Hilt.
  - **ADR-0015, `kotlin.time` for instants and kotlinx-datetime for `Europe/Madrid`.** Rejected: `expect`/`actual` over platform APIs; fixed offsets.
  - **ADR-0016, Coil 3 on Android and `AsyncImage` on iOS.** This refines spec §8.3, which names Coil 3 for both: its multiplatform support renders through Compose, which ADR-0001 rules out on iOS. Owner to confirm.
  - **ADR-0017, MapLibre Native on both platforms.** Rejected: Google Maps with MapKit; Mapbox; a web map. The tile provider stays with OQ-10.
  - **ADR-0018, Multiplatform Settings.** Rejected: DataStore KMP used directly; a settings table; `expect`/`actual`.
- **No dependency is added.** Each library enters the catalog with the first story that uses it.
- **Linked from** the ADR index and from `architecture.md` (the dependency rules and the cross-cutting table).

**Open / next**
- **Owner:** accept or amend ADR-0012 to ADR-0018. ADR-0016 deviates from spec §8.3.
- **LB-014 stays in progress** until OQ-16 picks the analytics and crash backend and its ADR is written.

## 2026-10-26: LB-004, part 1: core common and config

**Context**
- The owner was away for about 10 hours and asked the agent to carry on without questions.
- **Decisions the docs leave open:** the agent took the documented fallback or the most conservative option, and marked each one below for the owner to confirm.
- **Merges:** none happened while the owner was away, because only the owner merges.

**Agent decisions, to be confirmed by the owner**
- **Split LB-004 in two, as was done for LB-001.**
  - **Part 1 (this PR):** exactly the acceptance criteria, using only the Kotlin standard library.
  - **Part 2:** `Logger`, `AppDispatchers`, `AppLifecycle` and `LocationProvider`.
    - They need kotlinx-coroutines.
    - `LocationProvider`'s Android implementation needs Google Play Services Location (`architecture.md` §5.1). That is a third-party SDK, which needs the owner's approval (`AGENTS.md` §10).
  - LB-004 stays in progress.
- **Defaults for keys whose values the docs leave open** (`api_spec.md` §8):
  - `night_service_text`: the FR-STOP-06 sentence, in es and en (OQ-08).
  - `fare_tier_texts`: neutral "20 % / 50 % discount" texts, with no eligibility claims (OQ-09).
  - `map_style_url`: empty. Choosing a tile provider is a licensing matter for the owner (OQ-10), so none is set here.

**Done**
- **TDD:** seven red-green cycles, plus two refactors.
- **Every LB-004 acceptance criterion has a test:**
  - `AppError` (`Network`, `Server(status)`, `Data`, `NotFound`) and `AppResult`, matched exhaustively.
  - `AppClock` (a `fun interface`) and `ServerCorrectedClock`: the device clock plus the median of the last 5 `Date` offsets. A device clock 90 s behind the server is corrected, and before the first sample the device clock is used as is.
  - `RemoteConfigKey`, a sealed catalogue of the 14 keys with their compiled defaults, and `CompiledDefaultsRemoteConfig` (NFR-REL-05).
  - `pollInterval`, which clamps intervals under 10 000 ms to 10 s.
  - Geo, in `:shared:core:common`, package `geo`:
    - `GeoPoint.distanceMetersTo` (Haversine) is within 0.15 % of WGS84 geodesic references computed with Vincenty's formula.
    - `Polyline` round-trips Google's example at precision 5 and a Logroño path at precision 6.
    - `BoundingBox.expandedBy` and `around` are tested at 2 km and 5 km.
- **Found by the tests:** a JVM class-initialisation cycle.
  - When `PollIntervalTest` ran first, `RemoteConfigKey.all` held `null`.
  - The list is now built lazily.
- **Review:** the code-reviewer and data-rules-reviewer subagents both approved with minor findings. These were fixed with TDD:
  - **Median for the clock offset.** Under the mean, one stale `Date` (a response cached by a proxy) moved the offset from +90 s to −10 min 30 s for four samples.
  - **Malformed polylines** now raise a documented `IllegalArgumentException` instead of `StringIndexOutOfBoundsException`, and precision is limited to 0..10.
  - **`GeoPoint` and `BoundingBox`** print `(redacted)` in `toString` (NFR-PRIV-02), and negative box distances are rejected.
  - **Refactor:** `AppResult` is a sealed class, so Swift keeps its generic; `serverOffset` is internal; the box test is checked to 0.5 %; an exhaustive `when` guards the key catalogue.
  - **Not applied:**
    - The `asin` NaN guard: in 2 million random antipodal pairs, `sqrt(h)` never exceeded 1, so no test could prove it.
    - Re-clamping after jitter: it belongs to the `PollingEngine` and is noted on LB-009.
  - **Recorded in `api_spec.md` §8:** line ETAs reuse `poll_arrivals_ms`.
- **Verified:**
  - 21 common tests and 6 config tests pass on the Android host, and both modules pass on the iOS simulator;
  - ktlint and detekt are clean;
  - `run-all.sh` passes.

**Open / next**
- **Owner:** confirm the three defaults above, and decide on Play Services Location for part 2.
- **`TestAppClock`** (`:shared:core:testing`) arrives with the first test that needs a settable clock, likely in part 2 or LB-005.
- **LB-005** feeds `ServerCorrectedClock.onServerDate` from every BFF response.

## 2026-10-25: LB-002 done: CI failure probes

**Decisions (owner)**
- **LB-002 is closed.** Its planned work without code yet moves to the stories that will have that code:
  - the Android screenshot and iOS snapshot jobs, to LB-011;
  - the iOS ViewModel test job, to LB-101;
  - the nightly workflow, to LB-412, unless an instrumented test needs it first.
  - Their acceptance criteria now say so.

**Done**
- PR #13 merged, and the merge was detected by the watcher. The probes were the task named in its hand-over.
- **The method:** three throwaway draft PRs from `main`, with one deliberate fault each.
  - Each fault was first seen failing locally.
  - Each PR's result was posted as a PR comment, the PR was closed unmerged and its branch deleted.
  - The `pre-push` hook runs no tests, so no hook was bypassed.
- **#14, a regression** (`TopLevelDestination.start = NEAR_ME`) failed every test layer:
  - the shared tests on the Android host (`lb012_theAppOpensOnLines`, `lb001_swiftEntryPointExposesTheStartDestination`);
  - `Shared iOS tests`, on the simulator;
  - Robolectric (`lb001_appLaunchesOnTheLinesTab`, plus the Spanish variant);
  - XCUITest (`test_lb001_launchesOnLines`, plus the Spanish variant).
  - `Coverage` failed too, because `koverVerify` runs the tests first.
  - Both gates failed.
- **#15, a ktlint violation** (2-space indentation): `ktlint` failed with "Unexpected indentation", and so did the Kotlin gate.
- **#16, a coverage drop** (an untested object in `:shared:core:presentation`): `Coverage` failed with 45.45 % against the 80 % rule, and so did the Kotlin gate.
- `README parity` failed in all three, as expected, because a probe does not update the docs.
- **Every LB-002 acceptance criterion is now met.** `acceptance_criteria.md` records the evidence.

**Open / next**
- E0 continues with the data layer:
  - LB-003, the golden fixtures, which needs OQ-11 (data reuse) settled before fixtures are committed to the public repository (`ci_cd.md` §1.1);
  - LB-004, the core common, config and location modules.

## 2026-10-25: LB-002: faster iOS app job

**Decisions (owner)**
- **`iOS build and tests` gets a 10-minute budget,** as a measured exception to the 5-minute rule (`ci_cd.md` §2). The launch UI tests stay on every pull request.
- **OWASP Dependency-Check is dropped.** The dependency review on pull requests and Dependabot alerts on `main` are the NFR-SEC-05 equivalent. The nightly workflow will be created when its first job exists: Android instrumented tests, iOS 18 fallback snapshots or Maestro.
- **Throwaway draft PRs approved** for the last LB-002 criterion: a failing test, a lint violation and a coverage drop each fail CI. They are the next task after this PR.

**Done**
- First task started under the new rule: the agent detected PR #12's merge on its own and went on with the task named in the hand-over.
- **Measured before changing anything.** The last 8 successful runs of `iOS build and tests` took 5 to 12 minutes, against an 8-minute budget.
  - Gradle and the XCFramework took about 3 minutes.
  - XcodeGen took 16 s.
  - `xcodebuild test` took 169–496 s, and almost all of it went before "Testing started" completed: the simulator's first boot on a fresh runner.
- **Gradle cache.**
  - **Restores:** none of those runs restored it. The basic provider keys it on a hash of the Gradle files, with no fallback key, and every recent LB-002 PR changed them.
  - **Saves:** on `main`, the parallel jobs race for one key. One saves it, which `gh cache list` confirms, and the others log "save failed".
  - **So** pull requests that leave the Gradle files alone get the cache.
- **`scripts/ios/test.sh`** now:
  - looks up the simulator's UDID (`IOS_SIMULATOR` and `IOS_SIMULATOR_OS` replace `IOS_DESTINATION`);
  - generates the project, then starts `simctl bootstatus -b` in the background;
  - runs `build-for-testing`, waits for the boot, then runs `test-without-building`, printing each phase's time.
  - Locally, from a shut-down simulator, it passed in 38 s, and an unknown simulator fails with a hint.
- **Two variants, measured on CI** (table in `ci_cd.md` §5):
  - **Boot in parallel with Gradle:** 516 s and 458 s. The boot was fully hidden, but it slowed Gradle to 225–257 s.
  - **Boot in parallel with the app build (kept):** 319, 526 and 648 s. Gradle took 42–56 s, the build absorbed the boot, and the tests never waited.
  - **Before,** the median was about 550 s.
  - **Result:** neither variant is reliably faster, and the gain over the old median is small and uncertain. On the 3-core runner, the simulator's first boot costs minutes wherever it runs.
  - **Correction:** after two runs, the agent wrote that the kept variant had the better mean. A third run (648 s) disproved it, and the docs were corrected.
  - **What the PR does deliver:** the per-phase `TIME:` lines, a clear failure for a missing simulator, and a short Gradle phase.
- **Not cached, on purpose:**
  - `~/.konan`: 1.2 GB per Kotlin version, about as slow to restore as to download, and it would crowd the 10 GB cache quota;
  - DerivedData: the app builds in seconds.
  - `ci_cd.md` §5 records both decisions.

**Open / next**
- **Budget:** decided above (10 minutes). Revisit it if the `TIME:` lines show the median drifting past it.
- Rest of LB-002:
  - next: the throwaway draft PRs (approved);
  - then: the nightly workflow, once it has a job, and the iOS snapshot and ViewModel tests, which have no code to test before E1.

## 2026-10-24: LB-002: dependency graph and dependency review

**Decisions (owner)**
- **ADR-0011 accepted** after PR #11 merged: the ktlint CLI and the Kover scope, on top of the detekt version the owner had already chosen.
- **Merges are no longer announced** (2026-10-25). The agent watches the pull request it opened. Once it is merged, the agent syncs `main` and goes on with the next task. On a close without merge, requested changes or new comments, it stops and reports. Recorded in `AGENTS.md` §10, `engineering_workflow.md` §5 and `playbooks/finish-story.md` step 9.
- **Dependabot alerts enabled**, which also enables the dependency graph that the dependency review needs. Before this, the repository's dependency graph was off: its SBOM endpoint returned 404. The agent enabled it through the API (`PUT /repos/…/vulnerability-alerts`) with the owner's approval. Dependabot security update PRs stay off.

**Done**
- TDD:
  - **red:** `lb002_dependencyReviewWaitsForTheGradleDependencyGraph`;
  - **green:** `ArchitectureRules.jobNeeds`, then the two jobs.
- GitHub cannot read Gradle build files. A new `Dependency graph` job therefore runs `gradle/actions/dependency-submission`:
  - it is pinned by SHA (v6.4.0), uses the basic cache and validates the wrapper;
  - it resolves every configuration and submits the graph;
  - it runs on Kotlin-area pull requests and on every push to `main`;
  - it is the only job with `contents: write`.
- `Dependency review` (`actions/dependency-review-action@v5`):
  - it needs the graph job, and waits for GitHub to process the snapshot;
  - it fails on a known critical vulnerability in any scope.
  - The Kotlin gate needs both jobs.
- **The first review found a real issue.**
  - **The finding:** GHSA-c4c3-7fpv-j4q5 (critical) in Netty 4.1.93. A probe of every configuration traced it, and 4.1.110, to gRPC in AGP's Unified Test Platform (`unified-test-platform-*`), the runner for instrumented tests. Netty never reaches the app.
  - **Exposure:** the flaw is an SNI-routing bypass in TLS servers, and here Netty only talks to a local emulator, so practical exposure is low.
  - **The fix (owner decision):** patch it rather than allow-list it or hide those configurations. The owner chose 4.1.139.Final, the latest 4.1.x, which has no known advisories.
  - **TDD:** red `UnifiedTestPlatformTest`, then green.
  - **How:** the Android convention adds the Netty BOM to those configurations, through the DependencyHandler. Added straight to the configuration, the BOM lost its platform attribute and changed nothing.
  - **Probe:** in all 18 configurations, every Netty module now resolves to 4.1.139.Final.
- **The second review found two more libraries.** Once Netty was patched, it failed on Bouncy Castle 1.79 (two critical advisories) in Android lint, the test runner and AGP's own plugin classpath, and the first run had not shown them.
  - **A full query to stop finding issues one at a time:** an init script listed the 511 coordinates resolved across every configuration, settings and buildscript classpaths included. GitHub's advisory API was then queried for them in batches (`GET /advisories?ecosystem=maven&severity=critical&affects=…`). It found exactly three critical issues:
    - Netty, already patched;
    - Bouncy Castle;
    - FreeMarker 2.3.32 (GHSA-27j2-h3m2-8237), on AGP's plugin classpath.
  - **The fix (owner decision):** patch them as Netty was patched.
    - TDD: red `AgpToolConfigurationTest`, then green.
    - `AgpToolSecurityFloors.kt` adds the Netty and Bouncy Castle BOMs to every AGP tool configuration (`unified-test-platform-*`, `androidLintTool`), from both the Android and the KMP conventions.
    - The root build script raises Bouncy Castle (its BOM, 1.85, the version Robolectric already uses) and FreeMarker (2.3.35) on the plugin classpath.
  - **Verified:**
    - the query again found no critical advisory;
    - AGP generated a new debug keystore with Bouncy Castle 1.85 (empty `ANDROID_USER_HOME`);
    - `apksigner` verified the debug APK;
    - Android lint and `run-all.sh` pass.
- **Without the ordering, the review gives a false green.** If it ran alongside the graph job, it would compare a pull request without its Gradle dependencies and pass. The new guard test prevents that.
- Docs updated: `ci_cd.md` (catalogue, §6 permissions and scanning), `security_privacy.md`, `architecture.md`, `AGENTS.md`, ADR-0011 and its index, the backlog and both READMEs.

**Open / next**
- **Scope of the review:** it covers the Kotlin area, which holds every Gradle dependency. Actions changed only in `ios-checks.yml` or `policy-checks.yml` are covered by Dependabot alerts after the merge.
- **First run:** `main` had no submitted Gradle graph, so this PR's review compared against an empty base. It waited the full 3 minutes for the base snapshot, then reviewed every Gradle dependency as added; that is how it found the Netty issue. After the merge, `main` has a graph and reviews compare against it.
- **Drop each security floor** once AGP brings a fixed version itself (`ci_cd.md` §6). The nightly workflow will be the first to run instrumented tests with the patched Netty and Bouncy Castle.
- **Two high advisories remain**, which do not block: jdom2 2.0.6 (GHSA-2363-cqg2-863c, XXE) and jose4j 0.9.5 (GHSA-3677-xxcr-wjqv, DoS), both on AGP's plugin classpath. Revisit them with the next AGP upgrade.
- **Rest of LB-002:**
  - the nightly workflow, with OWASP Dependency-Check;
  - the iOS snapshot and ViewModel tests, which have no code to test before E1;
  - the throwaway-branch check that a failing test, a lint violation and a coverage drop each fail CI;
  - speeding up the iOS app job with caches.

## 2026-10-24: LB-002: ktlint, detekt and Kover (ADR-0011)

**Decisions (owner)**
- **detekt 2.0.0-alpha.6, pinned.** The stable 1.23.8 is built against Kotlin 2.0, and its type-resolved rules do not support Kotlin 2.4. ADR-0011 records the decision.

**Done**
- The owner was away for some hours, so the three tools went into one PR rather than three. This avoids stacking PRs that would conflict in the READMEs and this log. This was the agent's choice, not an owner decision.
- **ktlint 1.8.0 (CLI):**
  - the root tasks `ktlintCheck` and `ktlintFormat` use the shadowed artefact on the JDK 21 toolchain, over every Kotlin source and build script;
  - `.editorconfig` exempts `@Composable` functions from function naming;
  - **negative case:** the first run found 198 violations;
  - `ktlintFormat` fixed all of them, with no manual edits, in a separate `style:` commit; `check` passed afterwards.
- **detekt 2.0.0-alpha.6:**
  - `config/detekt/detekt.yml` holds only the differences from the defaults: complexity 15, Composable naming, `ForbiddenMethodCall` (print, println, `Clock.System.now`, `System.currentTimeMillis`) and `ForbiddenImport` (GlobalScope, Firebase);
  - `ciDetekt` runs the type-resolved tasks: `detektMainAndroid` and `detektHostTestAndroid` for KMP modules, `detektMain` and `detektTest` for Android modules, plus the iOS source sets;
  - **probe:** `println(kotlin.time.Clock.System.now())` in umbrella common code was reported twice, and the file was restored.
  - The clean-architecture import rule stays with `ModuleRules` and Konsist, which already enforce it; `code_style.md` is updated.
- **Kover 0.9.11:**
  - shared domain and data modules are gated at 80 % line coverage, and feature modules on their `domain`, `data` and `presentation` packages;
  - **probe:** an untested class in `:shared:transit:domain` failed with "lines covered percentage is 0.000000, but expected minimum is 80", and was removed.
- **CI:** the `ktlint`, `detekt` and `Coverage` jobs run behind the `Kotlin gate`, and the root `check` runs all three.
- Docs: ADR-0011, `code_style.md`, `ci_cd.md`, `testing_strategy.md`, `AGENTS.md` (status and commands), the backlog and both READMEs.
- The owner asked for an AFK run without permission prompts. Auto mode was already on. The classifier blocked my attempt to add local allow rules, as self-modification, so permissions are unchanged.

**Review (code-reviewer subagent): changes required, 1 major and 13 others.** Addressed:
- **CI false green**, a TDD cycle. A PR touching only `config/detekt/detekt.yml` or `.editorconfig` skipped ktlint and detekt. Both paths now count as Kotlin paths.
- **Coverage scope**, a TDD cycle. `coverageGate(path)` is now a tested rule, and it gates `:shared:core:presentation` too (shared presentation mappers, `testing_strategy.md` §3).
- **detekt:**
  - `ciDetekt` also runs every iOS, Apple and native source set (untyped);
  - `androidHostTest` is excluded from `FunctionNaming` and `MagicNumber`;
  - detekt's default `ForbiddenMethodCall` entries are kept;
  - `GlobalCoroutineUsage` is on;
  - the Firebase, token and fixture exemptions are suppressed at the call site, as documented.
- **ADR-0011 is now Proposed.** The owner decided only the detekt version, so the ktlint CLI and the Kover scope await confirmation. Its claims are narrowed: type-resolved rules cover common and Android code, and `build-logic` and `tools` get ktlint only.
- **Smaller fixes:**
  - the post-edit hook formats only with the pinned ktlint 1.8.0;
  - `run-all.sh` names every tool that `check` runs;
  - `config/` is listed in the READMEs and `AGENTS.md` §3;
  - stale lines are fixed in `ci_cd.md`, `architecture.md` and `testing_strategy.md`.
- **Verified by the reviewer:**
  - the `style:` commit changes no behaviour;
  - the new tasks are safe under the configuration cache and Isolated Projects;
  - each CI job runs one command.

**Open / next**
- The rest of LB-002: dependency review, the iOS snapshot and ViewModel tests, the nightly workflow, and the `iOS build and tests` budget (caches).
- Move detekt to 2.0 stable when it ships.
- The owner confirms ADR-0011 (the ktlint CLI and the Kover scope), or asks for changes.

## 2026-10-24: LB-002: the iOS workflow (SwiftLint, shared iOS tests, app build and tests)

**Done**
- **TDD cycle 1 (change detection):**
  - A preparatory refactor first: the script tests read each area by name.
  - Red then green: `changed-areas.sh` now also prints `ios=`. The area covers:
    - `iosApp/`, `.swiftlint.yml` and `shared/`;
    - the Gradle build (`gradle/`, `build-logic/`, `*.gradle.kts`, `gradlew*`, `gradle.properties`);
    - `version.properties`, `scripts/ios/`, `scripts/ci/`, the iOS workflow and the Gradle setup action.
  - Docs-only and Android-only changes skip the iOS jobs.
- **TDD cycle 2 (workflow):**
  - `lb002_iosGateWaitsForEveryIosJob` requires `ios-checks.yml`, with a gate that waits for every job.
  - The job-name guard now covers every workflow.
- **`ios-checks.yml`:**
  - `iOS changes`;
  - `SwiftLint` (Linux): `scripts/ci/install-swiftlint.sh` installs the pinned 0.65.1 binary after a SHA-256 check;
  - `Shared iOS tests` (`macos-26`): `./gradlew iosSimulatorArm64Test`;
  - `iOS build and tests` (`macos-26`, Xcode 26.6 pinned through `DEVELOPER_DIR`, XcodeGen from Homebrew): `scripts/ios/test.sh`, which generates the project and runs `xcodebuild test` on the iPhone 17e simulator with iOS 26.5;
  - `iOS gate`.
- **The first run on PR #10 passed:**
  - SwiftLint: 15 s, 0 violations in 13 files.
  - Shared iOS tests: 2 min 45 s. The `core:presentation` and `umbrella` simulator tests ran.
  - iOS build and tests: 8 min 44 s. The cold framework build and project generation took about 3 min, and `xcodebuild test` about 5 min 45 s.
  - The macOS jobs waited about 9 min for a runner.
- `scripts/ios/test.sh` passes locally in 40 s.
- Updated `ci_cd.md` (the catalogue and §4), `engineering_workflow.md`, `AGENTS.md`, both READMEs and the backlog. `protect-main.sh` now also requires `iOS gate`.

**Decisions**
- **One `Shared iOS tests` job, not a per-module matrix.** GitHub Free runs at most 5 macOS jobs at once, and a 19-job matrix would queue for a long time. Split it when its time grows.
- **SwiftLint on Linux with a pinned, checksummed binary**, instead of a container image, whose tags cannot be pinned by digest from here.

**Open / next**
- **After this PR is merged:** apply `protect-main.sh`, so that `iOS gate` becomes required.
- **The budget is exceeded:** `iOS build and tests` took 8 min 44 s, against 8 min. Next, cache `~/.konan` and the Xcode DerivedData, or split the build from the UI tests (`ci_cd.md` §5).
- **The rest of LB-002:** ktlint, detekt, Kover, dependency review, the iOS snapshot and ViewModel tests, and the nightly workflow.

## 2026-10-24: Skipped CI checks reviewed; readable matrix job names (LB-002)

**Done**
- **The owner asked to check and fix every skipped check on GitHub.**
  - Nothing on `main` was skipped.
  - On PR #8, a docs-only PR, the six Kotlin jobs were skipped by the path filter, as designed. The required `Kotlin gate` passed.
  - The two matrix jobs showed as `${{ matrix.module }}`, because GitHub does not expand a skipped job's name.
- TDD cycle:
  - `lb002_kotlinJobNamesReadWellWhenSkipped`, backed by the `jobNamesWithExpressions` rule and its unit test, forbids `${{ }}` in job names.
  - The matrix jobs are now "Shared unit tests" and "Android unit tests"; GitHub appends the module when they run.
- **First strict SwiftLint run** (0.65.1, installed by the owner) on the Swift code from LB-001:
  - the only findings were the built-in `todo` rule rejecting the two `TODO(LB-010)` and `TODO(LB-012)` comments, which `code_style.md` §1 allows;
  - that rule is replaced by `todo_without_story`. Probe: it rejected `// TODO: …` and `// FIXME …`, and accepted `// TODO(LB-012): …`;
  - every other rule passes, including the trailing commas and test names prepared in PR #7.

**Decisions (owner)**
- **Keep the path filter.** Docs-only PRs skip the Kotlin jobs on purpose (`ci_cd.md` §2, principle 5), and the skipped checks now read well. The alternatives were rejected:
  - running every job on every PR;
  - jobs that run but do nothing, a misleading green.

## 2026-10-23: ADR-0010 accepted; the Android module-rule probe

**Decisions (owner)**
- **ADR-0010 accepted.** The iOS project is generated with XcodeGen, `Shared` is linked once through the `LogroBusUI` package, and a script refreshes it.
  - ADR-0004's status records the amendment.
  - ADR-0007's "validate in LB-001" consequence is annotated as validated; its decision is unchanged.
- **The negative Android probe from PR #6 is approved**, and was run.

**Done**
- Probe, on `main` after PR #7, reverted afterwards:
  - added `implementation(projects.android.feature.stops)` to `android/feature/lines`, then ran `./gradlew :android:feature:lines:help`;
  - the build failed at configuration with: `Module dependency rules violated in 'implementation': :android:feature:lines -> :android:feature:stops is not allowed: a Android feature (lines) module may not depend on a Android feature (stops) module (ADR-0007, docs/architecture.md §3.2)`;
  - `ModuleRules` therefore blocks the dependency in a real Android build, which closes LB-001's "on either platform" criterion on Android too.
- This PR is docs-only, so it is also the first docs-only PR since the Kotlin gate became required. Its CI run closes the check left open by LB-002 part 1:
  - `Kotlin changes` passed, and the six other Kotlin jobs were skipped;
  - `Kotlin gate` passed, logging `Kotlin job results: success skipped skipped skipped skipped skipped skipped`;
  - the PR was `MERGEABLE` / `CLEAN` under branch protection.

**Open / next**
- The `ModuleRules` message says "a Android feature". Fix the article in build-logic, with a test (small, LB-002 or the next build-logic change).
- LB-002: `ios-checks.yml` (macOS runner), ktlint, detekt, Kover.

## 2026-10-23: LB-001 part 3 of 3: the iOS app skeleton (LB-001 done)

**Done**
- TDD cycle 1 (shared): `SharedDependencies`, the Swift entry point of `architecture.md` §5.2. It exposes the start destination.
  - With a source of its own, the umbrella finally links the `Shared` framework; it had been `NO-SOURCE`.
  - Swift sees it as `SharedDependencies.shared.startDestination`.
- TDD cycle 2 (architecture tests): `iosApp/project.yml` (XcodeGen), the `LogroBusUI` package and an ignored `.xcodeproj`.
  - The spec covers the bundle id, iPhone only, iOS 18.0, Swift 6, Spanish as the development language, and `MARKETING_VERSION` from the generated `Version.xcconfig`.
  - The package has the DesignSystem, MapUI and Feature<X> targets, still empty, plus `Shared` as an XCFramework binary target.
  - `scripts/ios/generate-project.sh` builds and copies the XCFramework, writes `Version.xcconfig` and runs XcodeGen. `bootstrap.sh` calls it on macOS.
- TDD cycle 3 (iOS): an XCUITest launches the app in English and Spanish and expects "Lines" and "Líneas".
  - The placeholder reads the start destination from Kotlin.
  - Swift maps it to the `tab.*` keys of a `Localizable.xcstrings` catalog: Spanish as the source language, plus English.
- TDD cycle 4 (iOS): a hosted unit test target (`iosAppTests`, Swift Testing) checks the **built** bundle: iPhone only, iOS 18.0, and the store bundle id.
  - It caught a real defect: the built `Info.plist` had `UIDeviceFamily` [1,2], because XcodeGen's application preset sets the device family on the target, overriding the project-level value. The setting moved to the target.
- All iOS tests pass on the iPhone 17e simulator (iOS 26.5) with Xcode 27.1: 3 bundle tests and 2 launch UI tests.
- Docs:
  - `architecture.md` §5.2 now describes the integration as built;
  - `release_process.md` §1 says the version file is generated by the script;
  - `AGENTS.md` §4 lists the iOS commands, and §8 the Swift test naming;
  - both READMEs say how to run the iOS app.

**Reviews (code-reviewer and ios-ui-reviewer subagents): both "changes required".** Addressed:
- **A false green in CI**, a TDD cycle. A PR touching only `iosApp/project.yml`, a `Package.swift` or `.gitignore` skipped the architecture tests that read them. `changed-areas.sh` now counts those paths as Kotlin paths.
- **No iOS feature-to-feature check**, a TDD cycle. LB-001 requires one on both platforms. `crossFeatureSwiftDependencies` parses the package manifest. Probe: a `FeatureLines -> FeatureStops` dependency was reported.
- **Portrait lock**, a TDD cycle. No doc decided it, Android does not do it, and it goes against WCAG 1.3.4 (NFR-A11Y-01). The app now supports portrait and both landscape orientations, checked on the built bundle.
- **ADR conflict.** ADR-0004 says `embedAndSignAppleFrameworkForXcode`; the build links through the package. **ADR-0010 is proposed for the owner to accept.** It records XcodeGen, the single link through the package, the refresh script and the release argument.
- **Weak spec test.** The iOS spec test now reads the app target's own settings. Probe: a project-level device family now fails.
- **SwiftLint, prepared but not run:**
  - trailing commas removed;
  - `allowed_symbols` dropped, since SwiftLint 0.55 or later skips function names anyway;
  - test names kept under 40 characters.
- **`generate-project.sh`:**
  - takes `debug|release`, so Release builds no longer link the debug Kotlin framework;
  - runs Gradle without the configuration cache and file-system watching;
  - requires XcodeGen 2.46.0 or later.
- **`bootstrap.sh`:** the header now says it runs a Kotlin/Native build, and `LOGROBUS_SKIP_IOS=1` skips it.
- **Docs:**
  - the `architecture.md` tree and testing table;
  - `testing_strategy.md`;
  - the `ci_cd.md` paths;
  - `AGENTS.md`;
  - the umbrella build comment;
  - `release_process.md`.
- **Noted, not changed:**
  - **TDD scope.** Some green commits carried code no red test required: the app stub and the script in `d436ce3`, and `bootstrap.sh` in `8fc6e7a`. Harness and lint changes belong in their own commits. History is not rewritten.
  - **Commit scope.** `ci(policy)` is not in the `AGENTS.md` §12 scope list.

**Decisions**
- **The app links `Shared` once, through the `LogroBusUI` package's binary target.** It does not use `embedAndSignAppleFrameworkForXcode`, which would link a second copy next to the package's.
  - Swift Package Manager resolves binary targets before any build phase, so after a Kotlin change you rerun `scripts/ios/generate-project.sh`.
  - The documented fallback, feature folders in the app target, was not needed.
- **`Version.xcconfig` comes from the generation script, not from a build phase:** Xcode resolves build settings before any phase runs.
- **SKIE stays in LB-010.** Until then, Kotlin enums are classes in Swift, so the label `switch` needs a `default`.

**Learned**
- **The false `NO-SOURCE` results had a second cause.** In a new source directory, a test stayed `NO-SOURCE` even with `--no-configuration-cache`; it ran only with `--no-watch-fs`.
  - The Gradle daemon's file-system watching on macOS misses newly created directories.
  - `run-all.sh` now turns off both the configuration cache and file-system watching, and `AGENTS.md` §4 explains why.
- **XcodeGen computes `TEST_HOST` from the target name** (`iosApp.app`). The product is `LogroBus.app`, so the unit test target spells it out.
- **My mistake, fixed before any commit:** I overwrote the existing `.swiftlint.yml` from LB-016 without reading it first. I restored it from git, then added only the generated-path exclusions and `_` in identifiers, for the test naming.

**Open / next**
- SwiftLint is not installed on this machine, so `run-all.sh` skips the Swift gate, and `.swiftlint.yml` has not been checked against the new Swift code yet.
- The owner accepts or changes ADR-0010. On acceptance, ADR-0004's index status records the amendment, and ADR-0007's "validate in LB-001" consequence is resolved.
- Check the empty `Shared.framework` stub that Xcode embeds at the first archive. Change the `.gitignore` pattern when the first remote Swift package needs a committed `Package.resolved` (LB-013).
- LB-002: `ios-checks.yml` (macOS runner: framework, generation, `xcodebuild test`, SwiftLint), plus ktlint, detekt and Kover.
- From PR #6: the negative Android build for a feature-to-feature dependency, pending the owner's go-ahead.

## 2026-10-23: LB-001 part 2 of 3: the Android app skeleton

**Done**
- After PR #5 was merged, `protect-main.sh` was applied. `Kotlin gate` is now a required check, alongside the six policy jobs; the GitHub API confirms it.
- TDD cycle 1 (build-logic): Android namespaces keep their platform segment. Without it, `:android:feature:lines` and the Android target of `:shared:feature:lines` would share a namespace, and AGP rejects duplicates.
- TDD cycle 2 (architecture): the 9 Android modules, each with its convention plugin.
  - Conventions:
    - `logrobus.android.application` and `logrobus.android.library` take the SDK levels and the JVM target from the catalog;
    - they enable Compose with its BOM and Robolectric-ready unit tests;
    - they add the `ciUnitTest` alias (`testDebugUnitTest`) and enforce `ModuleRules`.
  - `:android:app`: `applicationId` `com.ruizurraca.logrobus`, minSdk 26, targetSdk 37, versionName 0.1.0 from the new `version.properties` (`release_process.md` §1), versionCode 1 locally.
  - Refactor: the catalog helpers and `verifyModuleRules` moved to their own files.
- TDD cycle 3 (shared): `TopLevelDestination` in `:shared:core:presentation`.
  - It lists LINES, NEAR_ME and PLANNER, and starts on LINES (FR-NAV-01, FR-NAV-02, OQ-15). It is the first value shared by both apps.
  - The tests are named `lb012_…`, after the app-shell story that owns the requirements. They pass on the Android host and on the iOS simulator.
- TDD cycle 4 (Android): a Robolectric Compose test launches `MainActivity` and expects "Lines" in English and "Líneas" in Spanish.
  - The app draws edge-to-edge and shows a placeholder with the start destination title. Android maps the shared destination to its own string resources.
  - Manual check: on the API 36 emulator, a cold start of 881 ms, with no crash.
- LB-002 work in this PR, as planned in the PR #5 log entry. Without it, the Kotlin gate would have passed Android changes without running a single Android test.
  - A TDD cycle requires the workflow matrix to list every Android module.
  - New jobs: `Android unit tests: <module>`, `Android build` and `Android lint`.
- Clean-copy check (`git archive`): `./gradlew build allTests` succeeds. That covers the release APK, the iOS simulator tests and lint. `AGENTS.md` §4 now lists the verified commands.

**Reviews (code-reviewer and android-ui-reviewer subagents): both "changes required", no blockers.** Addressed:
- **Dark-mode system bars**, a TDD cycle. `enableEdgeToEdge()` picked light icons in dark mode on the always-light placeholder, so they were invisible. Both bars now use `SystemBarStyle.light` until the brand theme (LB-011).
- **Tab copy**, a TDD cycle. The labels now follow `design_guidelines.md` §2.1 and §9:
  - keys `tab_lines`, `tab_near_me` and `tab_directions`;
  - en "Lines", "Near me", "Directions"; "How to get there" stays the screen title;
  - the mapping lives in its own file, `TopLevelDestinationLabels.kt`.
  The LB-012 criterion said "Near Me, How to Get There". It now points to the guidelines, and **the English copy is still a proposal for the owner**.
- **Namespaces**, a TDD cycle. The architecture tests' `expectedNamespace` now keeps the `android` segment, as build-logic does, and the namespace rule covers `android/` sources.
- **Gate needs**, a TDD cycle. `lb002_kotlinGateWaitsForEveryKotlinJob` fails if a Kotlin job is missing from the gate's `needs`. Probe: dropping `android-lint` was reported.
- **Lint:**
  - CI runs `lintDebug` in every Android module;
  - `MissingApplicationIcon` is suppressed, with a pointer to LB-413;
  - all 9 reports say "No issues found".
- **Smaller fixes:**
  - `run-all.sh` runs Gradle without the configuration cache;
  - the `versionCode` comment no longer claims CI support that does not exist;
  - a missing `VERSION_NAME` fails with a clear message.
- **Not done, for the owner to decide:** a negative build of an Android feature-to-feature dependency, to show `ModuleRules` failing in a real Android build. The reviewer's attempt to run it was denied by its permissions. The rule itself is unit-tested, and the conventions call `verifyModuleRules`.
- **Follow-ups:**
  - `ModuleRules` lets `:android:app` depend on anything except tools, which is wider than `architecture.md` §3.2.
  - Check how `TopLevelDestination.start`, a companion member, looks from Swift under SKIE (part 3).

**Decisions**
- **First shared value:** `TopLevelDestination` rather than a placeholder. It carries real behaviour (FR-NAV-01, FR-NAV-02) that LB-012 builds on, and shared code carries only the meaning.
- **Placeholder screen without a theme:** there is no `MaterialTheme` in the app, and the platform theme is `Theme.Material.Light.NoActionBar`. Theming belongs to the design system (LB-011).
- **Mixed story in one PR:** the Android CI jobs (LB-002) ship with the Android code (LB-001), so the code is never merged untested. The commits keep their own story ids.

**Learned**
- **API 37 under Robolectric 4.17 needs three adjustments** (`testing_strategy.md` §4.5):
  - `--add-opens=java.base/jdk.internal.access=ALL-UNNAMED`;
  - Espresso 3.7.0, because Compose UI Test brings 3.5.0, which calls the removed `InputManager.getInstance()`;
  - NATIVE graphics, or text measures 5 px wide and `assertIsDisplayed()` fails.
- **Configuration cache:** a reused entry missed newly created source directories, and reported the new tests as `NO-SOURCE`, a false green. Run once with `--no-configuration-cache` after creating a source directory (`AGENTS.md` §4). CI always configures from scratch.
- **The iOS framework still links as `NO-SOURCE`:** the umbrella module has no sources of its own, even though it exports `TopLevelDestination`. Part 3 must give it one.

**Open / next**
- LB-001 part 3: the iOS app (XcodeGen, the `LogroBusUI` package, the `Shared` framework, a launch test).
- On the first docs-only PR: confirm that the Kotlin jobs are skipped and the gate passes.

## 2026-10-23: LB-002 part 1: Kotlin checks in CI

**Done**
- `kotlin-checks.yml`, with one atomic job per concern:
  - `Build-logic tests` (`:build-logic:convention:test`);
  - `Architecture rules` (`:tools:architecture-tests:test`);
  - `Shared unit tests: <module>`, a matrix over the 19 shared modules (`ciUnitTest`).
- Path filtering with a gate:
  - `Kotlin changes` runs `scripts/ci/changed-areas.sh`. It reports `kotlin=false` for a docs-only change, so the Kotlin jobs are skipped.
  - `Kotlin gate` always runs. It passes when every job passed or was skipped, and it is the one Kotlin check made required, so docs-only PRs are not blocked.
  - Verified locally against a docs-only commit (false), a code merge (true) and a push with no base (true).
- The workflow also runs on pushes to `main`: it verifies each merge and writes the Gradle cache that pull requests read.
- The JDK and Gradle setup lives in the `.github/actions/gradle-setup` composite action. `gradle/actions` v6.4.0 is pinned by commit SHA, because it is third-party. Dependabot now also watches `.github/actions/*`.
- TDD cycle 1 (matrix guard):
  - Red: `lb002_kotlinWorkflowRunsUnitTestsForEverySharedModule` requires the matrix to list exactly the shared modules in `settings.gradle.kts`.
  - Green: the workflow, plus the `workflowMatrixModules` pure rule with its own test. The architecture tests now take `.github/workflows/*.yml` as input; without it, a matrix-only edit would leave them up to date.
  - A probe that dropped `:shared:feature:planner` from the matrix failed the test and was reverted.
  - **Deviation from `testing_strategy.md` §7.1:** the green commit (`2e0a586`) is typed `ci(build)` instead of `feat`, and it has no `TDD: refactor not needed` line. History is not rewritten. The refactor of cycle 2 also tidies cycle 1's code.
- First CI run: all 29 checks passed, each Kotlin job in 1.5 to 2.5 minutes.
- **Code review (code-reviewer subagent): changes required, 10 findings.** All are addressed:
  - TDD cycle 2: `ChangedAreasScriptTest` runs the real script in throwaway git repositories.
    - Red, `73e908f`: three tests failed with `kotlin=false`. The first was a large diff with an early Kotlin path, where `grep -q` exits early, the writer gets SIGPIPE, and `pipefail` turns the match into a miss. The other two were a file moved out of `shared/` and a `fixtures/` change.
    - The red commit was pushed on purpose. On GitHub, `Architecture rules` failed and `Kotlin gate` failed with it: the gate's failure path, seen in CI.
    - Green, `6cec702`: a here-string instead of a pipe, `git diff --no-renames`, and `fixtures/` as a Kotlin path. The architecture tests now take `scripts/ci` as input.
    - Refactor, `8f65a36`: a shared `repositoryRoot`, the matrix guard limited to `:shared:` modules, and a lost space restored.
    - Test isolation, `d05ccf9`: on ubuntu-latest (git 2.55), the large-diff test passed its assertion, but JUnit could not delete its temporary repository. The likely cause is background git maintenance started by thousands of loose objects. The test repositories now set `gc.auto=0` and `maintenance.auto=false`, and the test passes on Linux.
  - The push concurrency group is per commit, so back-to-back merges are each verified rather than cancelled.
  - `ci_cd.md` §1, §2 and §4, and the structure rows of both READMEs, are updated.
- The Gradle cache now uses `cache-provider: basic`, which is MIT-licensed. The default "enhanced" provider is proprietary, under the Gradle Technologies Terms of Use (free for public repositories). Opting in is the owner's decision.
- `protect-main.sh` now also requires `Kotlin gate`.
- **Runner hiccup:** one `Build-logic tests` run hung until its 10-minute timeout, and GitHub kept no log for it. A re-run of the failed jobs passed in 1m38s. If this repeats, add test-report artefacts and look again.

**Open / next**
- **After this PR is merged:**
  - Apply `protect-main.sh`. Applying it before the merge would make PRs based on the old `main` wait for a `Kotlin gate` that never reports.
  - Confirm, on the first docs-only PR, that `Kotlin changes` reports `false`, the Kotlin jobs are skipped and `Kotlin gate` passes.
- The rest of LB-002: ktlint, detekt, Kover, the Android jobs (with LB-001 part 2), `ios-checks.yml` (with part 3), dependency review and the nightly workflow.
- The next story is LB-001 part 2 (Android).

## 2026-10-23: LB-415: repository public, `main` protected

**Done**
- Merged `main` into the LB-001 branch after PR #2, keeping both entries in the READMEs and in this log. PR #3 (LB-001 part 1) was then merged.
- Ran the pre-public checklist (`ci_cd.md` §1.1):
  - History scan of every blob reachable from any ref: 34 commits, 174 blobs, no secrets and no local paths.
  - Spec review: §3.4 (non-production hosts) and §3.3 (test payment pages) come from the public web bundle and hold no credentials.
- Made the repository public, and enabled GitHub secret scanning and push protection.
- PR #4 was the test PR: the six policy jobs ran in CI for the first time, and all passed.
- Applied `scripts/github/protect-main.sh`. The GitHub API confirms the settings:
  - `main` is protected, with a pull request required (0 approvals) and the six policy jobs as strict required checks;
  - the rules are enforced for administrators, conversations must be resolved, and force pushes and deletions are blocked;
  - linear history is not required.
  - No real direct push to `main` was attempted: if the protection had failed, that commit would have landed on `main`. The API check stands in for it.
- Updated `ci_cd.md`, `engineering_workflow.md`, `AGENTS.md`, `architecture.md`, `requirements.md`, the playbooks, the PR templates and both READMEs.

**Decisions (owner)**
- **Go public now**, ahead of the MVP, so CI runs.
- **Licence (OQ-21):** none is granted, all rights reserved. There is no `LICENSE` file; both READMEs state it.
- **Spec:** published unchanged.
- **Commit identity:** new commits use the GitHub noreply address. Older commits keep the address they were made with, because history is not rewritten.
- **LB-002 comes next, in part:** a `kotlin-checks.yml` workflow with the jobs that already have code to check. The Android and iOS jobs follow once those apps exist.

**Open / next**
- OQ-11 (data reuse) is still open. Revisit it before LB-003 commits golden fixtures to the public repository.
- The `ci-gate` job (`ci_cd.md` §4) must become a required check when LB-002 adds the path-filtered Kotlin workflow.

## 2026-10-22: Dependabot PR #2: actions/checkout v5 to v7

**Done**
- Reviewed Dependabot's bump of `actions/checkout` from v5 to v7 in `policy-checks.yml`. v7.0.0 only blocks fork checkouts on `pull_request_target` and `workflow_run`, which this repository does not use.
- Added the README, project-log and workflow commits to the Dependabot branch.
- The first docs commit claimed a project-log entry that a failed script step had not written. This commit adds it; history is not rewritten.

**Decisions**
- Dependabot PRs get no exception from the README rule. The README update is added to their branch before merging (`engineering_workflow.md` §9).

## 2026-10-22: LB-001 part 1 of 3: Gradle build, shared modules, architecture rules

**Done**
- Gradle 9.7.1 wrapper, with the distribution checksum pinned; version catalog; `build-logic` included build.
- `ModuleRules` (16 tests) enforces the ADR-0007 module dependencies at configuration time.
- `logrobus.kmp.library` convention plugin: Android KMP library plugin with host tests, `iosArm64` and `iosSimulatorArm64`, namespaces derived from the module path, the `ciUnitTest` alias.
- The 19 shared modules, with the umbrella `Shared` framework and XCFramework configured.
- `:tools:architecture-tests` (Konsist, 6 tests): exact module list, the convention plugin applied, no version literals, no cross-feature imports, clean domain imports, module namespaces.
- TDD with two cycles, each with red, green and refactor commits. The guards were proven against deliberate violations, which were then reverted.

**Decisions**
- Versions:
  - **Kotlin 2.4.20**, explicitly supported by SKIE 0.10.15 (2.4.21 is not yet).
  - **AGP 9.3.3**: KGP 2.4.20 is fully supported up to AGP 9.3.1, and 9.4 is out of range.
  - **Gradle 9.7.1**: KGP is fully supported up to 9.7.0.
  - compileSdk 37, minSdk 26, JVM toolchain 21, Android bytecode JVM 17.
- **No placeholder code** in shared modules without a test. The `Shared` framework links once the Android and iOS red tests introduce the first shared value (parts 2 and 3).
- **ktlint, detekt and Kover move to LB-002** (CI pipeline). `code_style.md` is updated.
- **Android feature modules may also depend on domain modules and shared core modules**, which is inward and clean. `architecture.md` §3.2 is updated.

**Review (code-reviewer subagent): verdict "changes required", 16 findings, all addressed**
- **TDD cycle 3 (build-logic):**
  - Dependencies are validated when declared, so RuntimeOnly buckets and late additions are covered.
  - Platform libraries are banned in domain modules.
  - Ktor and SQLDelight are limited to their core module, data modules and features.
  - New Testing kind.
  - The namespace and bucket helpers are unit-tested (23 tests).
- **TDD cycle 4 (architecture):**
  - The `ArchitectureRules` pure rules are unit-tested with good and bad samples.
  - Fixed: the umbrella false positive, multi-argument includes, every form of version literal (now also in build-logic sources), and `java.`/`javax.`/`dalvik.` in domain code.
  - New presentation-imports rule, portable paths, and an isolated-projects-safe input tree (15 tests).
- **Hygiene:**
  - The 9.7.1 wrapper jar now matches the official checksum.
  - The Android JVM target and toolchain come from the catalog.
  - Docs fixed: `AGENTS.md` status, rule 1 and scopes; the `architecture.md` dependency table; the `testing_strategy.md` heading.

**Open / next**
- LB-001 part 2: Android app and modules. Part 3: iOS app with XcodeGen and the `LogroBusUI` Swift package.

## 2026-10-22: Merge strategy: merge commits, no squash

**Decisions**
- PRs are not squashed when merged. The method is a merge commit only: no squash, no rebase.
- The merge commit takes the PR title (a Conventional Commit) and the PR body. The TDD commits are preserved in `main`'s history, and the first-parent history stays green.
- Branch protection no longer requires linear history.

**Done**
- Updated `engineering_workflow.md` §1.1, §3 and §5, `testing_strategy.md` §7.1, `AGENTS.md` §12, and `playbooks/finish-story.md`.
- Updated `scripts/github/protect-main.sh` and added `scripts/github/configure-merge.sh`, which the owner runs and which works on a private repository.
- Claude Code now denies `gh pr merge --squash` and `gh pr merge --rebase`.

**Open / next**
- `shellcheck` 0.11.0 installed. All harness scripts lint clean, with `.shellcheckrc` and explicit `source=` directives.
- Git hooks installed (`scripts/bootstrap.sh`), and GitHub merge settings applied (`scripts/github/configure-merge.sh`): merge commits only; the PR title and body are used for the merge commit; branches are deleted on merge.
- Next: open the first PR with this work (`docs-chore` template); after it, start LB-001.

## 2026-10-22: Owner decisions on scope, TDD, architecture and repository

**Done**
- Wrote ADR-0007 (clean architecture, feature modules, platform-specific presentation, shared presentation models), ADR-0008 (offline mode deferred) and ADR-0009 (telemetry contracts). Amended ADR-0001 and ADR-0004.
- Rewrote `architecture.md` for the feature-module layout.
- Updated `AGENTS.md`, `requirements.md`, `acceptance_criteria.md`, `backlog.md`, `open_questions.md`, `testing_strategy.md`, `engineering_workflow.md`, `ci_cd.md`, `code_style.md`, `design_guidelines.md`, `security_privacy.md`, `api_spec.md`, the playbooks, the PR template, the reviewer agents and both READMEs.
- Added `scripts/github/protect-main.sh`. The pre-push hook now only warns about README parity, because TDD pushes after every cycle.

**Decisions**
- **Offline functionality** is not in the MVP. Static data is still cached; the offline mode moves to v1.0 (LB-607, ADR-0008).
- **TDD protocol:** each phase gets its own commit (red, green, refactor), then a push. With nothing to refactor, the refactor commit is skipped and noted in the green commit body.
- **Privacy labels and disclosures** are the final MVP step (LB-414, which replaces LB-701).
- **`main` will be protected on GitHub** once the repository is public. The repository stays private now and goes public before the MVP is completed (LB-415, which replaces LB-708). OQ-20 is resolved.
- **No iPad in the MVP** (OQ-19 resolved, NFR-PLAT-03).
- **Telemetry:** contracts for analytics and crash reporting are built now (LB-017). The vendor is likely Firebase Analytics and Crashlytics, but not final (OQ-16).
- **Figma designs** for the MVP arrive near the end of the MVP (LB-413). Branding comes with them (OQ-18 resolved).
- **Architecture:** clean architecture with feature modules; a platform-specific presentation layer; presentation models shared through KMP (ADR-0007).

**Open / next**
- Confirm the interpretations noted in the chat: shared mappers count as presentation models; no empty refactor commits; squash merge keeps the TDD commits only in the PR.
- Install the git hooks, move this work to a branch, and open the first PR.

## 2026-10-22: Pre-scaffolding definition: requirements, guidelines and harnesses

**Done**
- **Requirements:**
  - `requirements.md`: the MVP scope baseline and the requirements added after the spec (FR-NAV, FR-NEAR, FR-LINE-12 to 16, FR-PLAN-11 to 14, NFR-PLAT, NFR-REL-05, NFR-MNT-04 and 05), plus the FR-HOME adaptation.
  - `acceptance_criteria.md`: criteria for all 61 MVP stories.
- **Engineering guidelines:**
  - `testing_strategy.md`, including TDD.
  - `engineering_workflow.md`, including the PR templates.
  - `ci_cd.md`, `release_process.md`, `security_privacy.md` and `code_style.md`.
  - `.editorconfig` and `.swiftlint.yml`.
- **Agent harness (ADR-0006):**
  - `agent_harness.md` and the playbooks.
  - `scripts/checks/*` and `scripts/agent/*`, `.githooks/`, `scripts/bootstrap.sh`.
  - Claude Code settings, hooks, four reviewer subagents and four skills.
  - `GEMINI.md` and `.github/copilot-instructions.md`.
- **CI and test harness:**
  - The `policy-checks.yml` workflow: six atomic jobs, inactive while the repository is private.
  - Two PR templates, `CODEOWNERS` and Dependabot for GitHub Actions.
  - The design of the test harness and the code workflows.
- Fixed the line 10 headsigns in the `design_guidelines.md` mock-ups. Brought the backlog references in line with the new requirement ids.

**Decisions**
- Several AI agents will work on the repository: `AGENTS.md` is canonical, and each agent gets a thin adapter (ADR-0006).
- OQ-16 (remote config, analytics and crash backend) is deferred until before the beta. OQ-18 (branding) is deferred, with a neutral placeholder theme in the meantime.
- **The owner's CI rule:** checks do not run while the repository is private. Local gates (git hooks plus `run-all.sh`) are authoritative, and their summary is pasted into every PR.
- CI checks are atomic: one concern per job, under 5 minutes, split when they grow.
- TDD is the default (`testing_strategy.md` §7).
- GitHub Free offers no branch protection for private repositories, so local enforcement is used (`engineering_workflow.md` §1.1).
- New open questions: OQ-19 (iPad), OQ-20 (visibility and plan), OQ-21 (licence), OQ-22 (domain).

**Open / next**
- The owner reviews the non-technical summary and the proposed defaults (OQ-19).
- Run `scripts/bootstrap.sh` to install the git hooks.
- From now on, changes go through a branch and a PR. Then start LB-001.

## 2026-10-22: Language policy

**Done**
- Added a strict language policy to the top of `AGENTS.md` and mentioned it in both READMEs.

**Decisions**
- Repository artefacts (code, comments, commits, pull requests, docs) are always in technical English.
- Agents talk to the owner in Peninsular Spanish unless told otherwise. The owner may write in English or Spanish.
- `README.es.md` and the localised UI strings are the only Spanish content in the repository.

**Open / next**
- Define the pre-scaffolding requirements, guidelines and harnesses (scope pending the owner's answers).

## 2026-10-22: Bilingual README and the README rule

**Done**
- Created the project README in English (`README.md`) and Spanish (`README.es.md`), with links to switch language.
- Added the README rule to `AGENTS.md` (§2, §10, §11, §12) and linked both READMEs from `docs/README.md`.

**Decisions**
- Every merged PR must update both READMEs. The update goes inside the PR, at least in "Recent changes" and "Project status".

**Open / next**
- Start M0 with LB-001.

## 2026-10-22: Owner decisions on OQ-13, OQ-14, OQ-15 and OQ-17

**Done**
- Recorded the owner's answers in `open_questions.md`, ADR-0002, ADR-0003, `AGENTS.md`, `architecture.md` and `design_guidelines.md`.

**Decisions**
- **OQ-13:** the iOS deployment target is 18.0, with Liquid Glass on iOS 26+ and a material fallback below. ADR-0003 is now accepted.
- **OQ-14:** the `applicationId`, bundle id and Kotlin package root are `com.ruizurraca.logrobus`. The name "LogroBus" and the scheme `logronobus://` are unchanged.
- **OQ-15:** the app always opens on Lines. Deep links open their target tab. G1 is met with one tap to Near Me.
- **OQ-17:** saved stops with live arrivals are shown in the Near Me sheet ("My stops").

**Open / next**
- Choose the https domain for deep links (spec §6.15) before LB-603.
- Start M0 with LB-001.

## 2026-10-22: Project documentation bootstrapped

**Done**
- Read the functional specification v0.1 (`logrono-bus-app-functional-spec.md`).
- Created `AGENTS.md` as the canonical agent instructions, and `CLAUDE.md` as a pointer that imports it.
- Created `docs/`:
  - `README.md`, `architecture.md`, `api_spec.md`, `design_guidelines.md`
  - `backlog.md`, `open_questions.md`, `project_log.md`
  - `adr/`, with ADR-0001 to ADR-0005.
- Initialised the git repository on branch `main` and made the first commit.

**Decisions**
- **Three-section navigation** (Lines, Near Me, How to Get There) replaces the five-tab structure of spec §7.1. Home was dissolved: favourite lines moved to the Lines carousel, and saved and nearby stops moved to the Near Me sheet. News, fares, recharge points and info moved to a More entry (ADR-0002, accepted).
- **Native UI on both platforms** over a shared KMP core: Compose with Material 3 Expressive, and SwiftUI with Liquid Glass. This resolves OQ-01 (ADR-0001, accepted).
- **iOS deployment target 18.0**, with the Liquid Glass fallback (ADR-0003, proposed; OQ-13).
- **Swift interop** through a single umbrella framework, `Shared`, plus SKIE (ADR-0004, proposed).
- **Android UI stack:** M3 Expressive, Navigation 3 with per-tab back stacks, and `NavigationSuiteScaffold` (ADR-0005, proposed).
- **MVP scope** is milestones M0–M4, built around the three sections. News, fares, recharge points, the widget and notifications move to M5 (v1.0); their spec priorities still apply to the public release.

**Open / next**
- The owner should confirm OQ-13 (iOS target), OQ-14 (identifiers), OQ-15 (launch tab) and OQ-17 (favourite stops placement).
- Start M0 with LB-001 (scaffold), then LB-003 (fixtures) and LB-004 to LB-006 (core, network, normaliser).
- Decide where to keep the functional spec: it currently sits at the repository root.
