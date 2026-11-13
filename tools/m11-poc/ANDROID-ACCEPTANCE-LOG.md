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
| Search field layout | Fix committed; new APK/device confirmation pending |
| Keyboard-open search usability | Pending |
| Find and Previous/Next behavior | Pending |
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
