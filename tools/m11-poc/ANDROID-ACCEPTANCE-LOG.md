# M11 Android device acceptance — 2026-11-14

## Latest device evidence

The user installed and launched the CodeAssist-built M11 proof APK. The native engine loaded successfully and offline decompilation was ready.

The bundled Newtonsoft.Json 13.0.3 test was run with mobile data showing 0 kB/s. The device returned source with dependency diagnostics:

- Input: 705,296 bytes
- Decompilation time: 20,258 ms
- Source: 1,182,379 characters
- Pages: 50 (24,000 source characters per page)
- Java heap change: 3,584,000 bytes
- Process PSS before: 21,039,104 bytes
- Sampled process PSS peak: 130,262,016 bytes
- Process PSS after: 130,262,016 bytes
- SHA-256 before and after matched:
  8c1dd5c184b4e2e7ead06971ff3ebcb46783be972292d1deb1061744369b4d80
- Working-copy unchanged: PASS

This confirms on-device offline source reconstruction for this assembly and input integrity. It does not yet prove the Find control, navigation, Serilog scenarios, external assembly selection, invalid-input handling, repeatability, or general UI responsiveness.

## UI issue reported

The user reported that the Find symbol or source text field was positioned at the bottom of the screen and only one line was barely visible.

### Fix committed to main

- Moved the search row and source page navigation above the potentially long diagnostic status report.
- Set a 48dp search-field height and added padding.
- Limited the diagnostic status view to five lines with end ellipsis so it cannot push the search field off-screen.
- Enabled adjustResize for the activity so the layout can resize when the keyboard opens.
- Kept the existing Java search logic, 24,000-character source paging, native engine, and decompilation pipeline unchanged.

### Source commits

- 9438a80ac03dd5df95c745956b93c953bc8132f9 — M11 search field visibility and layout correction.
- 830a4b8b922689037e26d83cf86ec3e60a1974c8 — resize layout when the keyboard opens.

## Current acceptance status

| Gate | Status |
|---|---|
| Native engine startup | Passed on device |
| Offline Newtonsoft.Json decompilation | Passed on device |
| Input SHA-256 unchanged | Passed on device |
| Search field visibility | Fix committed; new APK/device confirmation pending |
| Find and Previous/Next behavior | Pending |
| Serilog dependencies available | Pending |
| Serilog missing dependency diagnostic | Pending |
| External managed DLL/EXE | Pending |
| Invalid input handling | Pending |
| Three-run repeatability | Pending |
| Full M11 Android acceptance | Pending |

## Exact next action

Wait for CI to build and verify the updated M11 proof APK. Then the user should download the latest main ZIP, open DroidInspect/tools/m11-poc/android-host in CodeAssist, build and install once, and verify that the search field is visible above the diagnostic report and remains usable when the keyboard opens. Continue the remaining consolidated acceptance tests only after this layout is confirmed.

M11 remains isolated under tools/m11-poc/; do not integrate it into the production app until device acceptance is complete.
