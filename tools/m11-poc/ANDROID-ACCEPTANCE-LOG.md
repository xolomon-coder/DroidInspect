# M11 Android device acceptance — 2026-11-14

## Confirmed device results

The user installed and launched the CodeAssist-built M11 proof APK. The native engine loaded successfully and reported: “Native engine loaded. Offline decompilation ready.”

The bundled Newtonsoft.Json 13.0.3 test was run offline. The device returned C# source with dependency diagnostics:

- Input: 705,296 bytes
- Decompilation time: 20,258 ms
- Source: 1,182,379 characters
- Source pages: 50
- Java heap change: 3,584,000 bytes
- Process PSS before: 21,039,104 bytes
- Sampled process PSS peak: 130,262,016 bytes
- Process PSS after: 130,262,016 bytes
- SHA-256 before and after matched: 8c1dd5c184b4e2e7ead06971ff3ebcb46783be972292d1deb1061744369b4d80
- Working-copy unchanged: PASS

This confirms offline source reconstruction on the phone for this real-world assembly and verifies that the input working copy remained unchanged. The status “SOURCE RETURNED — DEPENDENCY WARNINGS” means source was returned while unresolved references were also diagnosed; it is not a claim that all dependencies resolved.

## Search field layout correction

The user reported that the Find symbol or source text field was fixed at the bottom of the screen and barely visible.

Committed fixes:
- Search row and Previous/Next page controls are placed before the diagnostic status report.
- Search field has a 48dp height and horizontal padding.
- Diagnostic status is limited to five lines with end ellipsis.
- Activity uses adjustResize for keyboard presentation.
- Existing Java search logic, source paging, native engine, and decompilation pipeline were preserved.

Source commits:
- 9438a80ac03dd5df95c745956b93c953bc8132f9 — search field visibility correction.
- 830a4b8b922689037e26d83cf86ec3e60a1974c8 — keyboard resize behavior.

## CI verification

- M11 workflow #61: SUCCESS — https://github.com/xolomon-coder/DroidInspect/actions/runs/43492105166
- Production Android CI #229: SUCCESS — https://github.com/xolomon-coder/DroidInspect/actions/runs/43492242520
- Source layout commits are included in M11 workflow #61; the README-only follow-up commit does not change app source.

## Acceptance matrix

| Gate | Status |
|---|---|
| Native engine startup | Passed on device |
| Offline Newtonsoft.Json decompilation | Passed on device |
| Input SHA-256 unchanged | Passed on device |
| Search field layout | User confirmed visible and usable after fix |
| Keyboard-open search usability | Pending |
| Find and Previous/Next behavior | Implemented; CI/device confirmation pending |
| Serilog dependencies available | Pending |
| Serilog missing dependency diagnostic | Pending |
| External managed DLL/EXE | Pending |
| Invalid input handling | Pending |
| Three-run repeatability | Pending |
| Full M11 Android acceptance | Pending |

## Exact next action

Download the latest `main` ZIP, replace the extracted M11 proof project, open `DroidInspect/tools/m11-poc/android-host` in CodeAssist, build and install once, then confirm that the search field is visible and usable with the keyboard open.

After the layout is confirmed, continue the consolidated remaining device acceptance batch. Do not repeat the already-passed Newtonsoft.Json offline decompilation test unless needed to diagnose a regression.

M11 remains isolated under `tools/m11-poc/`. Do not integrate it into the production app until full device acceptance is complete.


## Source implementation commits

- `9fc6ee9956ce92636bd7f3bc0fcb03f545fdf13b` — implement match indexing, page highlights, selected-match styling, navigation and scrolling.
- `4cb1a7903207d84f1075b7a8caae1f42a78f1ed1` — add Previous Match control and clarify page navigation labels.
- `590339f8f584c228bd303860726c8193d2d1c261` — avoid repeatedly lowercasing the full source while searching and correct match status line formatting.

The M11 GitHub Actions workflow is triggered by these `tools/m11-poc/**` changes. Do not ask the user to rebuild until CI has completed and the final source batch is ready.


## Search highlighting and match navigation — implementation (2026-11-14)

Implemented in the isolated Android proof viewer:
- Case-insensitive matching over the complete decompiled output, across all pages.
- All occurrences on the current page receive a visible highlight.
- The active match uses a stronger, distinct highlight.
- Previous Match and Find/Next Match navigate through the full result list and wrap at either end.
- Selecting a match opens its source page, scrolls it into view, and reports the match number/count.
- Page navigation remains independent.
- Search spans are applied to the displayed page copy; the original complete source string is not modified.

Changed files:
- `tools/m11-poc/android-host/app/src/main/java/com/tanvir/m11poc/MainActivity.java`
- `tools/m11-poc/android-host/app/src/main/res/layout/activity_main.xml`

Source commits:
- `9fc6ee9956ce92636bd7f3bc0fcb03f545fdf13b`
- `4cb1a7903207d84f1075b7a8caae1f42a78f1ed1`
- `590339f8f584c228bd303860726c8193d2d1c261`
- Latest search-memory optimization is committed separately on main; see the current branch history.

**Next:** wait for M11 CI to complete. If CI passes, download the latest main ZIP, open `DroidInspect/tools/m11-poc/android-host` in CodeAssist, build/install, and verify search highlights, selected-match styling, Previous/Find navigation, cross-page jumps, and match count. Do not repeat the already-passed Newtonsoft.Json decompilation unless diagnosing a regression.


Additional search implementation follow-up:
- `4e5b8da368541955335ceebe9661b20c26679fa7` — avoid duplicating the complete source in memory for case-insensitive matching.
- `76e75a45fffec14c02b9e3deac23577dc4ed6c37` — correct match-status formatting.
