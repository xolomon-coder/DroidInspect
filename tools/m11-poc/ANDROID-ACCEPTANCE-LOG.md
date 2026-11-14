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


---

## Consolidated device verification update — 2026-11-14

This update supersedes the earlier "pending" entries above wherever the same test is now marked complete. All observations below were reported by the user from the installed M11 proof app. No source code was changed as part of these device tests.

### Newly confirmed tests

| Test | Result | Evidence |
|---|---|---|
| External Serilog.dll selection and decompilation | PASS with dependency warnings | Input 161,280 bytes; 3,281 ms; Java heap change 790,528 bytes; 11 source pages. C# source returned. .NET 9.0.0.0 references did not match available .NET 11.0.0.0 candidates. |
| Serilog source paging | PASS | User reached page 11/11; no crash/freeze reported. |
| NLog 6.2.2 external DLL decompilation | PASS with dependency warning | Input 921,088 bytes; 24,908 ms; Java heap change 4,616,192 bytes; 64 source pages. C# source returned. Requested netstandard 2.0.0.0 did not match candidate 2.1.0.0. |
| NLog source paging | PASS | User reached page 64/64; no crash/freeze reported. |
| NLog repeatability | PASS | Initial run plus two repeat runs; user reported the same result on both repeats. Exact repeat-run timings were not provided. |
| Invalid TXT input | PASS | 17,062-byte TXT input rejected in 2 ms with InvalidDataException: selected DLL/EXE has invalid or corrupted PE/.NET metadata. |
| JeffBox.exe native/non-managed input rejection | PASS | 454,286-byte PE file rejected in 47 ms: no .NET metadata directory; native executables are not supported. This is a correct rejection, not a managed-EXE decompilation pass. |

### Existing confirmed results retained

- Bundled Newtonsoft.Json 13.0.3 offline source reconstruction: PASS; see the original metrics above.
- Search highlighting/navigation: PASS; user confirmed working on the phone.
- Serilog dependency-available test: PARTIAL; source returned but framework/reference mismatches remained.
- Serilog missing-dependency test: PARTIAL / expected diagnostic; missing Serilog reference reported, and source reconstruction was attempted.
- The app remained responsive while navigating the tested Serilog and NLog source pages.

### Interpretation and limitations

- "SOURCE RETURNED — DEPENDENCY WARNINGS" means reconstructed source was returned while one or more references remained unresolved or mismatched. It is not a dependency-resolution pass.
- The .NET 9 versus .NET 11 and netstandard 2.0 versus 2.1 identity mismatches must remain visible in the acceptance record.
- JeffBox.exe is a PE executable without a .NET metadata directory in the selected file. Its .NET Desktop Runtime requirement does not prove that the EXE itself is an ordinary managed assembly; it may be a native apphost. The phone test only proves correct rejection of this non-managed PE input.
- A genuine ordinary managed .NET EXE has not yet been selected and successfully decompiled on the phone.

### Verified GitHub Actions

At source commit `42b4b11742a0017bac00b257d817587d6accb38a`:

- M11 ILSpy Native AOT Android Proof, run [43501102992](https://github.com/xolomon-coder/DroidInspect/actions/runs/43501102992): **SUCCESS**. The job's managed fixture, host decompilation, real-world assembly, invalid-input/repeatability, Native AOT ARM64, JNI, Android proof APK build and APK payload verification steps all completed successfully.
- Production DroidInspect Android CI, run [43501103023](https://github.com/xolomon-coder/DroidInspect/actions/runs/43501103023): **SUCCESS**. Host parser tests, Android compilation, release APK signing/verification, tamper test and artifact checks all completed successfully.

These CI results verify automated checks for the stated commit; they are not a substitute for device acceptance.

### Current acceptance matrix

| Gate | Latest status |
|---|---|
| Native engine startup | PASS on device |
| Bundled Newtonsoft.Json offline decompilation | PASS on device |
| Search highlighting/navigation | PASS on device |
| Serilog external DLL decompilation | PASS with dependency warnings |
| Serilog dependency available/missing diagnostics | PARTIAL; diagnostics observed |
| NLog external DLL decompilation | PASS with dependency warning |
| NLog 64-page source navigation | PASS |
| NLog three-run repeatability | PASS |
| Invalid TXT rejection | PASS |
| Native/non-managed PE EXE rejection | PASS |
| Ordinary managed EXE decompilation on device | PENDING |
| M11 production-integration decision | PENDING |
| M12 implementation | NOT STARTED; gated |

### Exact next action

No rebuild is required for this documentation-only update. Continue using the installed M11 app. Test a known ordinary managed .NET EXE (not a native apphost, Native AOT executable, or self-contained native launcher). Record whether C# source is returned, source page count, dependency diagnostics, and responsiveness. Only after this final input-category test should the project record the M11 production-integration decision.

M11 remains isolated in `tools/m11-poc/`. M12 remains gated and has not started.

### FluentCleaner Classic 1.19.269 — managed EXE device test (2026-11-14)

The user selected **`FluentCleaner.exe`** from FluentCleaner Classic 1.19.269 for the M11 **SELECT ANOTHER MANAGED DLL / EXE** test. The installed Android proof app returned reconstructed C# source.

- Input file: `FluentCleaner.exe`
- Input size: 1,164,800 bytes
- Decompilation time: 4,469 ms
- Source pages: 16
- App status: `SOURCE RETURNED — DEPENDENCY WARNINGS`
- Source reconstruction from a real-world managed EXE: **PASS**
- Dependency resolution: **PARTIAL**; do not label dependencies fully resolved.
- Java heap change: 1,146,880 bytes (as displayed by the app).
- Process PSS and input hash were not supplied; no values are inferred.

Reported dependency diagnostics:
- `System.Net.Http, Version=4.2.0.0`: candidate identity mismatch with `System.Net.Http, Version=11.0.0.0`.
- `FluentCleaner.Core, Version=1.0.0.0`: unresolved.
- `System.Text.Json, Version=8.0.0.5`: candidate identity mismatch with `System.Text.Json, Version=11.0.0.0`.
- `System.Windows.Forms, Version=4.0.0.0`: unresolved.
- `System.Management, Version=4.0.0.0`: unresolved.

Interpretation: this closes the previously documented ordinary managed-EXE device-test gap for this selected input. The app reconstructed source and exposed the unresolved/mismatched references. It does **not** establish complete dependency resolution, source recompilation, or runtime compatibility. In particular, the WinForms and Management references indicate Windows-specific dependencies that are not supplied by the Android proof environment.

This test was performed against the already-installed M11 proof app; no app source change or CodeAssist rebuild was needed. M11 production integration remains a separate explicit decision, and M12 remains gated until that decision is recorded.

