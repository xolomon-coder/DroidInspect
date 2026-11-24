# DroidInspect


> **CURRENT ACCEPTANCE STATUS — 2026-11-24 (authoritative update)**
>
> - Production branch: `main`; current documentation HEAD before this update: `7fae21c04c8063fde801c6e0cc53c2098cf90b5f`.
> - The only change after the CI-tested source commit `1e102500ef5f42d8bb0d87176c45563fc10ff183` was a README-only commit. The production Java/XML source and workflow were unchanged. The complete production CI run [#728](https://github.com/xolomon-coder/DroidInspect/actions/runs/45288815735) therefore verifies the exact source tree currently present on `main`, although its Git commit SHA is the parent source commit rather than this documentation HEAD.
> - Run #728: **SUCCESS**. All substantive steps passed: host parser/workspace tests, paged C# editor model tests, NuGet advisory audit, release dependency resolution, Android debug and release compilation, APK signing/tamper verification, payload/ABI/offline checks, and ARM64 ELF dependency inspection. Its only job completed successfully with no failed steps.
> - Run #727 is a historical failed attempt. Its only failure was a paged-model test assertion using a pre-edit offset. Commit `1e102500ef5f42d8bb0d87176c45563fc10ff183` corrected the test expectation; run #728 then passed.
> - Workflow `.github/workflows/phase1-android.yml` supports manual `workflow_dispatch`, but the available GitHub connection exposes no workflow-dispatch write action. The README-only commit is intentionally ignored by the push trigger. No Actions artifacts or Releases were created or used.
> - **Android 11 device acceptance is NOT DONE.** The previous large Unity DLL editable-workspace black-screen/crash report remains an unverified blocker until the updated source is installed and tested on the Infinix X689B / Android 11 device. Keyboard visibility, page navigation/edit persistence, cross-page search/replacement, save and reopen remain pending. Do not test recompilation yet.
> - Current source package: [Download main.zip](https://github.com/xolomon-coder/DroidInspect/archive/refs/heads/main.zip). Import the first CodeAssist project option, `droidinspect`.
>
> This section supersedes older chronological status statements below where they conflict. Historical entries are retained as a development record; they are not current acceptance claims.

> **Current authoritative development policy — 24 November 2026**
>
> DroidInspect uses a **source-first, artifact-free** workflow. GitHub `main` is the source of truth; development changes are committed directly to `main` only. GitHub Actions may run host tests, Android debug/release compilation, signing/tamper checks, and APK payload/security verification, but it must not upload or download APKs, ZIPs, or generated reports. Verification results are retained in normal Actions logs and the job summary; runner-local build outputs are discarded when the job ends. No GitHub Releases are used as an artifact workaround.
>
> The user downloads [the latest main ZIP](https://github.com/xolomon-coder/DroidInspect/archive/refs/heads/main.zip), imports/updates the `droidinspect/` project in Android CodeAssist, builds and installs the APK locally, and reports device-test results. A CI pass is not phone acceptance. Preserve accepted tests, do not create branches or pull requests, and keep this README and the handoff current.
>
> This policy supersedes older workflow and artifact-delivery instructions in the historical development journal below. Historical entries are retained as records of what was true at the time.

## Latest C# workspace device findings — 2026-11-23

The user clarified that the soft keyboard covers the code itself in the editable C# workspace, obstructing the source while typing. This confirms the keyboard/IME layout issue affects the editor content, not merely a small toolbar area.

The user also tested a Unity-game managed file of approximately 1+ MB:
- **C# decompilation: PASS** — the app successfully decompiled the file.
- **Open/edit workspace: FAIL / CRASH** — tapping the Edit control led to a black screen and the app crashed.
- No source edit, save, or recompilation was completed for this fixture.

Track these as two separate C# workspace blockers:
1. Fix the keyboard/IME layout so the editor viewport resizes or scrolls appropriately, keeping the active code and editing controls visible while typing.
2. Investigate and fix the crash when opening the editable workspace for this Unity-game DLL. Reproduce with the user's approximately 1+ MB fixture if it can be supplied, inspect editor/workspace initialization and large-source handling, and preserve the original assembly and decompiled source.

**Acceptance status:** decompilation of the Unity fixture is accepted as working; opening the editor for that fixture is not accepted. Editing, saving, and recompiling remain untested. Do not mark the C# editing workflow accepted until both workspace blockers are fixed and the user verifies editing on-device.

## Paged C# editor implementation update — 2026-11-24

A targeted production `main` update was made after the Unity DLL crash and keyboard obstruction reports. This is an implementation checkpoint, not device acceptance.

**Changes now present in production source:**

- `PagedSourceDocument` keeps the default editable page size at 16,000 UTF-16 characters and preserves the existing UTF-8 load/save path and 32 MiB per-source-file limit.
- Backward search now walks pages from the end and retains only the short overlap needed to match across page boundaries. It no longer calls `toString()` to allocate a second complete-source `String`.
- Replace All and cross-page range replacement now stream output into bounded page chunks instead of building a second complete-document `StringBuilder`. Case-insensitive matching and cross-page matches remain supported.
- The paged-model regression test now covers backward matches across page boundaries, backward wraparound, bounded replacement output, surrogate-pair page boundaries, and deleting all source text to a valid empty document. Existing longer/shorter replacement, save/reopen, small-file, Unicode and range-boundary cases remain in the test.
- Source-project creation now writes UTF-8 through a bounded streaming output stream rather than first allocating a full-source byte array. The 32 MiB byte limit remains enforced, and the raw file descriptor is synced only after the UTF-8 writer has finalized its encoding.
- When the user opens the editable workspace, the decompiler Activity releases its retained complete-source `String`; if the user returns to the decompiler screen, the untouched decompilation is reloaded from the private `original/Decompiled.cs` copy. The original assembly and untouched decompilation remain separate from editable source.
- Editor loading now disables search/replacement while file I/O is in progress. A failed file load reports the requested file and explicitly states when the previously open file remains active. The initial source load re-enables editing only after a valid paged document is installed.

**Source commits on `main`:**

- `b188cc2e96f0eb39d39bff1ac63d07a9c7483e59` — page-based backward search and bounded replacement output.
- `0828aae127a841966984669248e32b04138c9f7b` — backward boundary and wraparound regression tests.
- `bad0f45877af9bb8578591a46538e5fdc4afa4bb` and `47dab6440f7960eb05a40bdafc53138d7686271e` — bounded streaming UTF-8 workspace writes and correct post-encoder sync.
- `96144b670002c8780787a9c98d55363113324f1f` — release the decompiler's full-source field while the editor is active and restore it from the untouched copy on return.
- `20bb1c313abd672b23729c50f747b9d2054b2bb8` and `9d474747ef55d3fc8c9b4bce516a0d6b6baed49b` — editor busy/failure-state hardening and initial-load enable correction.
- `426ead6ef90bbff8471e4a6ee64e8b8e44da81c4` — bounded-page and empty-document replacement regression cases.
- `9cd3470ac2bcd91640f66544eb35b08499c33a83` — keep supplementary Unicode replacements within the page-size bound.
- `98df2c9d46e240850a7f1c76a3658b3641e61972` — regression for supplementary Unicode replacement exactly at a page boundary.
- `7fc3270fd3644ab8dd6a86751aa1a58d0568d8f7` and `cf73f7f81cbfb3496c751e098642a6d5a873170b` — responsive keyboard-compacted editor controls and layout identifiers.
- `8d093ff63e2c6d5b9f7f08fcd9bf1cb0832648d9` — resize-based keyboard detection fallback for older supported Android versions.
- `aeb47a0af8e9af2d6fe23fdc7ddaf7f4384cc2fb` — cancellable paged source loading, expanded page/edit/save regression coverage, and manifest-preserving APK tamper verification.
- `1e102500ef5f42d8bb0d87176c45563fc10ff183` — corrected the case-insensitive search test to compare against the edited document's current offset.

**CI verification update — 2026-11-24:** production run [#728](https://github.com/xolomon-coder/DroidInspect/actions/runs/45288815735) completed **SUCCESS** on current production `main` HEAD `1e102500ef5f42d8bb0d87176c45563fc10ff183`. This run verifies the paged-source implementation at `aeb47a0af8e9af2d6fe23fdc7ddaf7f4384cc2fb` plus the regression-test correction at HEAD. All host parser/workspace tests passed, including the expanded paged C# source test; all three NuGet audit projects restored and reported no vulnerable packages from the configured advisory source; the release dependency graph resolved; Android debug and release compilation passed; CI signing and APK verification passed; payload/ABI/offline checks passed; and all tracked ARM64 native-library ELF dependencies were inspected. There were no failed workflow steps.

**New paged-editor regression and cancellation coverage:** the source loader now checks thread interruption between buffered reads and aborts with an `InterruptedIOException` when the editor's background worker is closed. The paged-model test now covers forward-search wraparound, case-sensitive versus case-insensitive matching, oversized page edits, surrogate-safe page splitting, shorter edits, page-content deletion, CRLF preservation, save/reopen after page growth/shrinkage, save-size rejection without modifying the destination, and cancelled loads. Run #727 initially stopped on a test expectation that compared an updated document offset with the pre-edit input offset; no application check had failed before that test. Commit `1e102500ef5f42d8bb0d87176c45563fc10ff183` corrected the expected offset to use the current document, and run #728 passed.

**Signing-test correction:** the earlier tamper test flipped the first byte of `AndroidManifest.xml`, so verifier failure could be caused by an invalid manifest rather than signature tampering. The current workflow mutates `resources.arsc` while preserving the binary manifest, then requires apksigner's output to identify a signature/digest/integrity failure. Run #728 logged **PASS: modified signed resource rejected while the binary manifest remained intact**. The CI signer verified APK Signature Schemes v2 and v3; v1 was reported false. The application has `minSdk=26`, so v1 signing is not required for its supported Android range.

**APK/native verification from run #728:** debug APK size was 88,428,743 bytes and unsigned release APK size was 87,916,191 bytes. Both contained only `arm64-v8a` native libraries; the four tracked shared libraries totalled 81,306,496 bytes. The production manifest and packaged APK checks confirmed no Internet permission. ELF `DT_NEEDED` inspection showed the engine depends on Android platform `libdl`, `libm`, and `libc`; the M11 JNI library also depends on the engine; the M12 compiler depends on `libdl`, `libz`, `libm`, and `libc`; and the M12 JNI library depends on the compiler plus platform `libm`, `libdl`, and `libc`. NuGet audit reported no vulnerable packages in its configured sources at run time; this is a point-in-time advisory check, not a guarantee against undisclosed vulnerabilities.

**Acceptance remains open:** CI does not reproduce the user's phone-only Android 11 interaction. The Unity DLL's earlier decompilation PASS remains accepted, but the editable-workspace black-screen/crash report remains a device blocker until retested on the updated build. Keyboard visibility, page edits/navigation, cross-page search/replacement, save and reopen all require the targeted Android 11 device batch. Recompilation remains out of scope until editing and saving are stable. No Actions artifacts or Releases were uploaded; runner-local APKs and audit files were discarded.

**Keyboard implementation:** the editor Activity manifest specifies `windowSoftInputMode="adjustResize"`, and the editor `EditText` specifies `imeOptions="flagNoExtractUi"`. The Activity now detects IME visibility using `WindowInsets.Type.ime()` on Android 11+ and a root-height resize baseline on older supported versions. While the keyboard is open it hides the status/file selector, Add/Import controls and project/export action rows, while retaining Save, Find/Replace and (for large files) page navigation. It also scrolls the active cursor line into the resized editor viewport. This is an implementation intended to address the reported obstruction; it is **not device-verified** until the user confirms the keyboard no longer covers code on Android 11.

**Acceptance remains open:**

- Unity DLL decompilation: previously device-tested PASS.
- Large Unity DLL editable workspace: the earlier black-screen/crash report remains a device FAIL until the user repeats only this affected test on the updated build.
- Keyboard visibility and viewport resizing: device verification required.
- Page navigation, edit/return preservation, search, replace, save and reopen on a large source file: device verification required.
- C# recompilation: do not test or mark accepted until editing and saving are stable.
- Small-file editor behavior and all previously accepted M11 decompilation tests must be preserved; do not repeat M11 acceptance.

**Current production source HEAD before this README update:** `8d093ff63e2c6d5b9f7f08fcd9bf1cb0832648d9`. The later README-only documentation commit does not trigger Android CI because the workflow ignores README-only pushes. Project status remains **NOT COMPLETE**. Do not provide a device-acceptance ZIP until the latest source commit has a verified green CI run; then request the targeted Android 11 editor acceptance batch above.

## Device acceptance update — .NET IL editing usability (2026-11-23)

The user tested the production app on Android 11 / CodeAssist after building and installing the current `droidinspect/` project.

**Observed device results:**
- Runtime diagnostics: PASS (Android API 30; ARM64 native decompiler/compiler payloads loaded; offline operation; SAF file selection).
- PE/.NET metadata inspection: PASS on `Serilog.Extensions.Logging.dll` (Serilog.Extensions.Logging 9.0.2.0; PE32/x86; CLI metadata and type/member tables decoded).
- IL instruction browser: PASS. The selected method displayed byte offsets, raw instruction bytes, decoded opcode names, metadata tokens/operands, branch targets and explanatory text (for example `newobj`, `stloc.0`, `ldloc.0`, `ldarg.1`, `stfld`, and `brtrue.s`).
- Method rename attempt on that fixture: safely refused with `IOException: Strong-name signed assemblies cannot be edited by this writer.` This is expected protection for a strong-name-signed input, not a successful rename. The user confirmed the refusal.
- C# decompilation screen has not yet been tested in this acceptance sequence.

**New product requirement — make .NET editing understandable on a phone:**
The current edit path exposes a continuous hexadecimal IL byte string, which is not understandable or safely editable for an ordinary user. This is a usability gap; raw hex must not be the default editing interface.

Required roadmap work:
1. Provide a human-readable IL editor as the default low-level editing view, showing instruction offsets, decoded opcodes, operands/metadata tokens, branch targets and concise explanations.
2. Retain raw hexadecimal as an optional advanced view, not the primary editor.
3. Plan a higher-level C# source editing and recompilation workflow where technically feasible, with compiler diagnostics, validation and preservation of the original assembly and last valid output.
4. Clearly communicate that decompiled C# may not perfectly reconstruct the original assembly or preserve all metadata/compiler-generated details; do not promise lossless round-tripping.
5. Keep strong-name/signature safety explicit: do not silently modify signed assemblies or imply that editing a signed file preserves its signature. Require a deliberate, supported re-signing workflow if that capability is implemented.

**Status:** ROADMAP / NOT IMPLEMENTED. The IL browser's decoding capability is device-confirmed, but the user-facing hex editing experience is not accepted as usable. The signed-assembly rename refusal is a PASS for safe refusal only; unsigned-assembly rename/export remains untested. Do not mark method renaming generally accepted based on this test.

## Latest continuation update — 21 November 2026

**Latest workflow/source commit:** `a507bab47a7b74ca43eca8307e69209eda1b1d21` — `Expose release and native dependency graphs in CI logs`. **Latest documentation-only `main` commit:** `f4da8ebe0fd6fc91e2e55e0fdb994a97e9c953cd`.

**Latest completed production CI:** [run #678](https://github.com/xolomon-coder/DroidInspect/actions/runs/44837694111), successful on source commit `af2e8e4a41f649685220ffb34a70c42c20c9a72d`. All 13 host test steps, NuGet audit, Android debug/release compilation, CI signing, signature verification, tamper rejection, APK payload, ARM64 ABI and offline-permission checks completed successfully. The current HEAD changes only the verification workflow; its newly added checks still require their own run before being marked passed.

**Dependency-review finding and correction:** run #678 captured the Android `releaseRuntimeClasspath` graph to a runner-local file but did not print that file, so the graph was not independently reviewable from retained logs. Commit `a507bab` changes the workflow to stream the full graph into ordinary Actions logs and inspect ELF `DT_NEEDED` entries for every tracked ARM64 shared library using `readelf`. The new native dependency step is included in the job summary. No artifacts are uploaded. The new run's results must be inspected before claiming the Android/native dependency review complete.

**NuGet audit evidence from run #678:** all three configured .NET projects restored successfully (3 + 5 + 5 = 13 package instances). The advisory check reported no vulnerable packages for each project from the sources available at audit time. The package license inventory reported MIT for the M11/M12 compiler packages and MIT/Apache-2.0 for the dependency-test packages; zero packages lacked license metadata requiring manual review. This is a point-in-time advisory/license-metadata check, not a guarantee against undisclosed vulnerabilities or a legal opinion.

**Android dependency declarations inspected on current main:** the production app directly declares `io.github.reandroid:ARSCLib:1.4.0` and `com.android.tools.build:apksig:8.13.0`; the project uses Android Gradle Plugin `8.13.0`, repositories Google Maven and Maven Central, and no additional declared production app dependencies. Their resolved transitive runtime graph is awaiting the new log-visible CI inspection. Native payload remains ARM64-only by design; dynamic library dependencies are awaiting the new `readelf` output review.

**Acceptance remains unchanged:** M11 is accepted and must not be retested. M12–M18 consolidated Android 11 acceptance is pending. M19 host tests pass, but SAF export/reopen, persistent device data and recovery acceptance remain pending. M20 lifecycle hardening compiles and host indexing tests pass, but Activity destruction/reopen, cancellation, stress, memory and responsiveness acceptance remain pending. Project status is **NOT COMPLETE**.

## Current continuation checkpoint — 21 November 2026

## Latest verification update — 21 November 2026

**Current source fix:** `a07bf1dca1fbfe21fbcf9a31c758aaa7ccc152ea` — `ApkSigningActivity.java` now catches checked `IOException` from temporary-file cleanup in the three catch paths identified by production CI run #675. Cleanup failures are attached as suppressed exceptions so the original signing/import failure remains the primary diagnostic.

**Previous failing production run:** [#675](https://github.com/xolomon-coder/DroidInspect/actions/runs/44829668942), commit `549598ba46952c86ef5696d3a5c1dac4f0598d96`. All 13 host-test steps passed, release dependency resolution passed, and the NuGet audit step completed. Android debug compilation failed at `ApkSigningActivity.java` lines 205, 245 and 427 because three calls to `deleteTemporaryFile(...)` did not handle its declared checked `IOException`. Release build, signing/tamper checks and APK payload checks were skipped as downstream steps.

**Follow-up M20 lifecycle hardening:** commits `0a12e3e942a76f389bb04ec61dd02b82f07ab4b6` and `af2e8e4a41f649685220ffb34a70c42c20c9a72d` changed the background project-index callback to use a `WeakReference` to the Activity, while retaining the application context, main-thread Handler, stale-generation rejection and executor shutdown. The newline formatting of multi-line index warnings was preserved. Production CI [#678](https://github.com/xolomon-coder/DroidInspect/actions/runs/44837694111) completed **SUCCESS** on the final source commit, including all host tests, debug/release compilation, NuGet audit, signing/tamper verification and APK payload checks. Run #677 was cancelled by the latest-run concurrency policy before completion; it is not a failure.

**Latest production verification:** [#676](https://github.com/xolomon-coder/DroidInspect/actions/runs/44837050076) completed **SUCCESS** on source commit `a07bf1dca1fbfe21fbcf9a31c758aaa7ccc152ea`. All host tests, NuGet audit, release dependency resolution, Android debug and release compilation, CI signing, tamper rejection, APK payload, ABI and offline-permission checks passed. No failure steps remained. The README-only follow-up commit does not alter the verified application source.

Run #676 produced runner-local debug/release APKs of 88,411,727 and 87,899,175 bytes. Both contained only `arm64-v8a` native libraries, including the M11 decompiler and M12 compiler payloads; no Internet permission was present. The test signer verified under APK Signature Schemes v2 and v3, and a deliberately modified APK was rejected. `apksigner` reported v1=false; the production app has `minSdk=26`, so v1 is not required for its supported Android range. The v1 result is recorded rather than misrepresented as a verified scheme. No APK or report artifacts were uploaded.

**NuGet audit findings from runs #675 and #676:** all three configured projects restored successfully and reported no vulnerable packages from the NuGet advisory source used at audit time. Each run inventoried 13 restored package instances across the three projects: MIT and Apache-2.0 declarations only, with zero packages flagged for manual license review. This is a point-in-time advisory result, not a guarantee that no undisclosed vulnerabilities exist.

**M19/M20 status remains open:** M19 host regression coverage passed in runs #676 and #678, including concurrent metadata writes, persistence/recovery, workspace index and rollback tests. M20 Activity callback lifecycle hardening passed the #678 Android compilation gate. M19 SAF export/reopen and Android device acceptance remain pending. M20 Activity lifecycle, repeated open/close, cancellation, stress, memory and responsiveness acceptance remain pending. The green CI run unlocks the consolidated Android 11 test batch; it does not replace it.


**Repository:** [xolomon-coder/DroidInspect](https://github.com/xolomon-coder/DroidInspect)  
**Branch:** `main` only. No feature branches or pull requests.  
**Latest application source commits:** `0a12e3e942a76f389bb04ec61dd02b82f07ab4b6` (WeakReference lifecycle hardening) and `af2e8e4a41f649685220ffb34a70c42c20c9a72d` (warning display correction). **Latest production CI workflow change:** `549598ba46952c86ef5696d3a5c1dac4f0598d96`. **Latest successful production verification:** run #678 on `af2e8e4a41f649685220ffb34a70c42c20c9a72d`. **Previous successful baseline:** run #676 on `a07bf1dca1fbfe21fbcf9a31c758aaa7ccc152ea`.  
**Production package:** `com.tanvir.droidinspect`. **M11:** accepted; do not repeat its device tests.

### CI failure investigation — run #660

Run [#660](https://github.com/xolomon-coder/DroidInspect/actions/runs/44302103843) targeted commit `66f90efc08fde0052d8fd9e692fdb26a103968f4`. The job-list endpoint returned zero jobs and the commit-status endpoint returned no checks; consequently there is no step log or runner output to quote. The commit diff identifies the workflow defect: `Publish verification summary` was placed at the YAML root rather than under `jobs.build-debug-apk.steps`. The next main commit `bfcaf647135d1070e0fc337113dfb04e96b7fb5b` corrected its indentation. This was a workflow-definition failure before job creation, not evidence of a Java or Gradle compile failure. The exact GitHub YAML parser message was not available.

Subsequent runner-backed history recorded 12 host tests and release dependency resolution passing, then found and fixed a Java help-string compile error (`6cd93e1`) and additional missing imports / lifecycle state / Handler scope errors (`73fb7c3`). The later run #675 exposed three checked-IOException compile errors in APK signing cleanup; commit `a07bf1d` fixed them and run #676 verified the correction with a complete production CI pass. The earlier run #660 parser investigation remains historical context.

### M19/M20 source hardening committed on main

- `ProjectWorkspaceMetadata.java`: replaced object-instance-only synchronization with a canonical-project lock shared by every metadata-store instance for the same project. This protects concurrent notes, bookmarks, saved searches, operation history, recovery markers, and overview reads from lost updates.
- `ProjectWorkspaceMetadataSelfTest.java`: added an eight-thread regression that creates separate metadata instances and performs 160 concurrent history writes, then verifies that all records remain present. This test passed in production CI run #676.
- `ProjectWorkspaceActivity.java`: moved unified APK / C# source / .NET dependency project discovery to a single background executor. Results return on the main thread, stale scans are discarded using a generation counter, and Activity destruction invalidates callbacks and shuts down the worker.
- Existing M19 transaction, project-index, metadata persistence, symlink-boundary, rollback, and bounded-traversal tests passed in production CI run #676. Android SAF and device acceptance remain pending.

### Repository-wide artifact-free Actions policy

The production workflow and the three legacy proof/fixture workflows were inspected directly on `main`. All four contain zero `actions/upload-artifact` and zero `actions/download-artifact` steps. APKs, ZIPs, test DLLs, dependency inventories and temporary reports may be produced only inside the ephemeral runner for verification; they are not published as downloadable workflow artifacts. The M11/M12 workflows may still commit verified native source payloads to `main`, which is source code/input for CodeAssist rather than a workflow artifact.

### NuGet dependency and license audit — CI results reviewed

The artifact-free production workflow now restores and audits these three .NET projects:

- `tools/m11-poc/engine/DroidInspect.M11.Engine.csproj`
- `tools/m12-poc/engine/DroidInspect.M12.Compiler.csproj`
- `tools/m11-poc/dependency-assembly-tests/DependencyAssemblyTests.csproj`

It prints the transitive package graph, NuGet vulnerability-advisory results, and license expression / license-file / license-URL metadata parsed from each restored package's nuspec into Actions logs and the job summary. Missing license metadata is marked for manual review. Run #676 completed this audit successfully: 13 package instances were inventoried, zero vulnerabilities were reported by the configured advisory source, and zero packages required manual license review. The result is point-in-time and must be refreshed before a future release if dependencies change.

### Current acceptance state

| Area | Status |
|---|---|
| M11 C# decompilation | **ACCEPTED** by the user; no retest required |
| M12–M18 | Implementation integrated; consolidated Android 11 device acceptance pending |
| M19 | Host regression tests passed in #678; Android SAF export/reopen, process-restart recovery and device acceptance pending |
| M20 | Background indexing, stale-result rejection, Activity WeakReference callback and executor shutdown integrated; CI compilation passed in #678; Android lifecycle, repeated open/close, cancellation, stress, memory and responsiveness acceptance pending |
| Android debug/release build, CI signing and tamper checks | **PASS** in production CI #676; phone installation and acceptance pending |
| NuGet transitive license/security audit | **PASS** in #676: 13 packages, zero reported vulnerabilities, zero manual-review flags |
| Final release / project completion | **NOT ACCEPTED / NOT READY** |

### Consolidated Android 11 / CodeAssist device acceptance — M12–M20

**The latest source CI run #676 is green.** Proceed with this consolidated batch using the `main.zip` source; do not use Actions artifacts. M11 is already accepted: skip all M11 decompilation retests unless a new regression is observed.

**Preflight and data safety**
- [ ] Confirm the commit in the downloaded `main.zip` matches the latest verified `main` source.
- [ ] Build the existing `droidinspect/` project in CodeAssist 3.36.1 using its local build process; record build output and APK version.
- [ ] Before replacing an installed build, export any project data that must be preserved. If Android reports a signing-certificate mismatch, do not uninstall the existing app until its private workspaces have been exported or otherwise safely backed up.
- [ ] Use copies of all APK/DLL/EXE fixtures. Never select an original file as an output destination.

**M12 — source editing and managed DLL rebuild**
- [ ] Open a supported managed test assembly and create a separate C# source project.
- [ ] Edit a simple method, save the source, compile offline, and confirm a new managed PE/CLI DLL is produced.
- [ ] Reopen the output in DroidInspect and verify the expected edited symbol or method is present.
- [ ] Confirm the original assembly and untouched decompilation source are unchanged.
- [ ] Introduce a deliberate C# syntax error; confirm compilation fails with diagnostics and does not overwrite the last valid output.

**M13 — dependency and target-framework resolution**
- [ ] Open the dependency view for a test assembly and review requested versus resolved assembly identities/framework versions.
- [ ] Test one exact local dependency, one missing dependency, and one version/framework mismatch where suitable fixtures are available.
- [ ] Confirm missing or mismatched references are explicitly reported and never silently replaced with an incompatible reference pack.

**M14 — Assembly Explorer and IL navigation**
- [ ] Navigate assembly/module/namespace/type/member trees, including fields, properties, events and methods where present.
- [ ] Open selected methods in the raw IL viewer; verify tokens, offsets, opcodes and operands are readable.
- [ ] Use search, bookmarks and cross-reference navigation on a fixture containing supported invoke instructions.

**M15 — phone-oriented C# source editor**
- [ ] Verify line numbers, syntax highlighting, current-file search/find-replace and unsaved-change indication.
- [ ] Create/import at least two `.cs` files, save them, close the editor and reopen the project to confirm persistence.
- [ ] Verify the file selector can switch files. A tabbed editor is not currently claimed; record any usability limitation on the phone.

**M16 — assembly comparison and resources**
- [ ] Compare a known original assembly with its rebuilt/edited copy; verify added, removed and changed members are classified accurately.
- [ ] Compare an identical copy and confirm unchanged records are not reported as modified.
- [ ] Export a supported embedded resource through SAF and verify the exported file is non-empty and has the expected content/size.
- [ ] Record linked-resource entries as a known limitation: they may be listed without being extracted. The comparison is structural, not proof of semantic or binary equivalence.

**M17 — advanced APK inspection and DEX analysis**
- [ ] Open a representative APK and inspect the structured manifest view: components, permissions, intent filters, exported/enabled state and SDK declarations.
- [ ] Inspect the resource catalog and navigate at least one supported DEX invoke cross-reference to its indexed target class/method.
- [ ] When a split APK set is available, inspect base/split package identity, version, split names and dependency consistency. Inventory does not mean DroidInspect merges or installs the split set.
- [ ] Confirm malformed/unsupported DEX or APK inputs produce a bounded error rather than a crash.

**M18 — APK edit → rebuild → sign → verify**
- [ ] Create a project from a copy of a representative APK and verify the original remains separate and unchanged.
- [ ] Change a supported manifest field or named string resource, rebuild to a separate unsigned APK, and inspect the changed output.
- [ ] Sign the rebuilt APK with a test key and run signature verification; confirm the signature is reported against that key, not represented as the original developer's identity.
- [ ] Run the update-compatibility assessment for package ID, version code and signer certificate; record WARNING/FAIL where the test signing key differs.
- [ ] Verify a deliberately tampered copy is rejected by signature verification. Do not install a tampered APK.

**M19 — persistent workspaces and recovery**
- [ ] Create or reopen one APK project, one C# source project and one .NET dependency project; verify all appear in recent projects with the right type/name/state.
- [ ] Add notes, bookmarks and saved searches to each project. Close the Activity/app, reopen the projects, and confirm the entries persist and remain isolated between projects.
- [ ] Record an operation as started, force-stop or close the app before marking it finished, reopen the project and confirm the recovery notice/history is visible; mark it finished and confirm the notice clears.
- [ ] Import a rebuilt APK as a new working revision, view history, restore a previous revision, and verify the original APK is still unchanged.
- [ ] Export a project report and working output via SAF; confirm the project remains reopenable after export.

**M20 — lifecycle, responsiveness and final regression**
- [ ] Open/close the project launcher repeatedly and return from child screens; verify background index refresh completes without freezing or stale project entries.
- [ ] During a large but in-limit import/copy, background the app or cancel/close the relevant Activity; reopen the project and verify the last committed working copy is intact.
- [ ] Test representative large APK and .NET inputs within documented per-file limits; record any low-memory warning, slow operation or force-close.
- [ ] Recheck offline behavior and the absence of an Internet permission; verify no network access is required for inspection, editing, compilation or workspace operations.
- [ ] Repeat only the affected test after a fix; do not repeat accepted M11 tests.

**Acceptance record to send back:** CodeAssist build result and APK version; Android version/device; PASS / FAIL / BLOCKED for each M12–M20 section; exact failed screen/action and error text; whether the original input hash/size stayed unchanged; and any observed lag, crash or data loss. Do not mark a milestone PASS on compilation alone.

**Exact source and workflow files changed in this checkpoint:**

- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ProjectWorkspaceMetadata.java`
- `tools/project-workspace-test/ProjectWorkspaceMetadataSelfTest.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ProjectWorkspaceActivity.java`
- `.github/workflows/dotnet-rename-fixture.yml`
- `.github/workflows/m11-nativeaot-poc.yml`
- `.github/workflows/m12-roslyn-compiler-poc.yml`
- `.github/workflows/phase1-android.yml`
- `README.md`

**Next action:** inspect the first runner-backed execution after source checkpoint `35d28c2` and CI workflow checkpoint `549598b`; inspect every host test, NuGet audit output, Android debug/release compile, signing/tamper check and payload check; fix only verified failures on `main`. Once CI verification is complete, use the consolidated M12–M20 device checklist below. Do not repeat M11 tests, do not download CI artifacts, and do not declare project completion before device acceptance.

Offline-first Android APK and .NET assembly inspection and analysis application.

ANDROID APP DEVELOPMENT WORKFLOW

Current workflow — November 2026

1. Development Environment

My development environment is phone-only.

- Device: Android smartphone.
- IDE: CodeAssist.
- Development approach: Build, test, and install directly on my Android phone.
- Preferred language: Java.
- Preferred UI: Native Android XML layouts.
- Source control: GitHub.
- AI assistance: ChatGPT for planning, coding, debugging, reviewing, and project management.

Do not assume my Android version, phone specifications, CodeAssist version, or internet availability. Ask only when the information is necessary.

2. Starting a New App

When I want to build a new application:

Step 1 — Understand the idea

First, understand what I want to build.

You should:

- Evaluate whether the app is technically possible.
- Identify Android API limitations.
- Explain important technical challenges.
- Identify permissions and dependencies.
- Consider phone hardware and performance limitations.
- Explain what can and cannot be implemented.

Do not start generating code before the development approach is clear.

Step 2 — Plan the architecture

Design the application architecture before implementation.

Establish:

- Programming language.
- UI framework.
- Minimum and target Android versions.
- Application architecture.
- Required libraries and dependencies.
- Main features.
- Permissions.
- Storage and data management.
- Compatibility requirements.

Default preference: Java + native Android XML.

If another technology is necessary, explain why before choosing it.

Step 3 — Create a complete roadmap

Divide the project into clearly defined milestones.

Each milestone should have:

- A specific objective.
- Features to implement.
- Files that need modification.
- Expected results.
- Build verification.
- Functional testing instructions.
- Completion criteria.

Tell me the total number of milestones and explain the development sequence.

Do not arbitrarily change the roadmap during development.

3. Initial App Generation

Google AI Studio can be used for initial app generation when appropriate.

My specific requirements:

- When I request a Google AI Studio app-building prompt, provide ONE comprehensive, self-contained prompt.
- The prompt should instruct AI Studio to build the application in one pass.
- Do not divide the actual generation prompt into multiple development steps.
- Account for my preference for Java + XML native Android.
- If AI Studio generates Jetpack Compose/Kotlin instead, investigate whether conversion to Java/XML is practical while preserving the appearance and functionality.

After initial generation, use CodeAssist and GitHub for subsequent development.

If AI Studio is not suitable, use another practical phone-compatible starting approach.

4. GitHub-Based Development

GitHub is the source of truth for DroidInspect. The user's preferred development and delivery workflow is phone-only and uses the repository's `main` branch ZIP.

**Branch policy — main only**
- Use only the `main` branch for all project source, README, workflow, and CI updates.
- Do not create, recommend, or use feature, development, release, or other working branches.
- Make source and documentation commits directly on `main`, after inspecting its latest HEAD.
- Before every change, verify the current `main` HEAD. If the repository state is unexpected or a write cannot safely target `main`, stop and report the issue rather than creating another branch.

**Before making changes**

Always:
1. Inspect the latest `main` branch and identify its HEAD commit.
2. Read the current README and relevant project documentation.
3. Inspect the existing source, build files, manifest, resources, and CI workflow relevant to the task.
4. Understand the architecture, previous decisions, completed work, pending work, and known limitations.
5. Continue the existing project; do not recreate it or overwrite working functionality unnecessarily.

**Assistant's GitHub development cycle**

- Implement the agreed scope directly on `main`.
- Run relevant host-side tests and GitHub Actions.
- Fix source, test, or CI failures and repeat verification as needed.
- Keep the README and development history accurate.
- For work spanning multiple sessions, continue from the latest `main` HEAD and the documented stopping point.
- Do not hand the user an unfinished intermediate build merely because one commit has been made.

**Normal delivery to the user — repository ZIP, not CI APK artifact**

When the agreed implementation batch is ready for the user's local build:
1. Ensure all intended source and README changes are committed to `main` and the relevant GitHub Actions checks have completed.
2. Provide the repository page and a direct ZIP download for the latest `main` branch:
   - Repository: https://github.com/xolomon-coder/DroidInspect
   - Main branch ZIP: https://github.com/xolomon-coder/DroidInspect/archive/refs/heads/main.zip
3. State the exact latest `main` commit.
4. Explain whether the user should import/open the extracted project or update the listed files in the existing CodeAssist project.
5. List the exact files/folders to add, replace, or update, with full repository paths. Mention any required CodeAssist Library Manager dependencies and exact versions before code changes.
6. The user builds the project in CodeAssist, installs the locally built APK, and performs the requested Android device tests.
7. The user reports build output, test results, logs, or errors. Investigate and fix issues on `main`, then repeat CI and handoff as necessary.

**GitHub Actions artifact policy**

- GitHub Actions artifacts are primarily for assistant-side/cloud verification, reproducibility, and build-output checks.
- Do not make downloading an Actions APK artifact the user's normal build or delivery workflow.
- Do not tell the user to install a CI artifact when the agreed workflow is to download the latest `main` ZIP and build in CodeAssist.
- Provide a direct APK artifact download only when the user explicitly requests it or when a specific diagnostic requires a separately identified CI build. Explain why it is being requested.

**GitHub responsibilities**

Use GitHub for:
- README: project overview, requirements, architecture, current state, decisions, verification, and handoff instructions.
- Actions: automated host tests, Android compilation, APK output verification, and artifact publishing where configured.
- Commits: chronological record of changes made directly on `main`.
- Issues and Projects: optional tracking when useful; they do not require additional branches.

A successful commit or cloud build does not prove the feature works on the user's phone. The user's CodeAssist build and Android device test remain separate verification gates.
5. Implementation Rules

My preferred coding delivery format is FAST-TRACK.

Code delivery

- Provide complete replacement files.
- Do not provide incomplete code snippets when a complete replacement is required.
- Do not instruct me to manually edit individual lines.
- Use prominent headings containing the complete file path.
- Make code ready to copy and paste.
- Keep files reasonably manageable in size.
- Split excessively large files when appropriate.

Dependencies

When additional libraries are necessary:

1. List dependencies first.
2. Give exact dependency names and versions.
3. Explain which dependencies need to be added through CodeAssist Library Manager.
4. Then provide the complete replacement files.

Phone compatibility

Always consider:

- Small-screen editing.
- Keyboard visibility.
- RAM and storage limitations.
- Build performance.
- Android API compatibility.
- CodeAssist compatibility.
- Practical installation and testing.

Do not give me desktop Android Studio instructions when a phone-compatible alternative is available.

6. Milestone Development Cycle

Follow this cycle for every milestone:

1. Inspect

Inspect the latest GitHub project and understand its current state.

2. Plan

Identify the exact changes needed for this milestone.

3. Implement

Make the required source-code changes.

4. Verify

Check the implementation, dependencies, compatibility, and potential errors.

5. Update GitHub

Update relevant source files, README, Issues, and other documentation.

6. Build

Provide the necessary instructions for building the app in CodeAssist.

7. Test on phone

Give me practical instructions for testing the actual installed application.

8. Review results

I will provide build results, test reports, screenshots, logs, or diagnostic output.

Analyze the results and determine whether the milestone passes.

9. Complete or fix

If something fails, investigate and correct it before declaring the milestone complete.

A successful build is NOT proof that every feature works.

7. Testing Requirements

Testing must distinguish between:

- Build success.
- Installation success.
- Application startup.
- Individual feature functionality.
- Android permission behavior.
- Real-device compatibility.
- Error handling.
- Performance and stability.

When possible, implement a one-click test button for each major diagnostic or test suite.

A test button should execute the relevant checks automatically and produce a readable report.

When a test is inconclusive, explain what Android could not verify instead of incorrectly declaring success or failure.

8. Documentation and New-Chat Handoff

Before starting a new ChatGPT conversation for an existing project, inspect and update the GitHub documentation.

The documentation must preserve:

1. Original project requirements.
2. Complete development roadmap.
3. Total milestone count.
4. Completed milestones.
5. Pending and unfinished milestones.
6. Architecture and technical decisions.
7. Dependencies and versions.
8. Android compatibility.
9. Important source files and their responsibilities.
10. Exact files changed.
11. Implementation details.
12. Build and testing results.
13. Known bugs and limitations.
14. Unresolved issues.
15. Safety and permission requirements.
16. Latest GitHub branch and commit.
17. Chronological development history.
18. Exact current development stopping point.
19. The next specific action.

Then prepare a complete new-chat handoff containing the essential information and instructions.

The next conversation must continue from the exact stopping point, not restart the project.

9. How ChatGPT Should Act

ChatGPT should act as my:

- Android software architect.
- Senior Android developer.
- GitHub project manager.
- Code reviewer.
- Debugging engineer.
- Testing and verification assistant.
- Technical documentation writer.

Mandatory behavior

1. Inspect before modifying.
2. Never recreate an existing project unnecessarily.
3. Follow the established roadmap.
4. Preserve existing working features.
5. Avoid unnecessary changes.
6. Provide complete replacement files when delivering code.
7. Consider phone-only development constraints.
8. Explain technical problems in understandable language.
9. Never claim a feature works without appropriate verification.
10. Maintain accurate GitHub documentation.
11. Keep track of milestone progress.
12. Prepare reliable new-chat handoffs.
13. Tell me exactly what to do next.
14. Do not make me repeat information already available in the current project documentation or conversation.

Communication style

- Be direct and technically accurate.
- Explain important decisions before implementation.
- Avoid unnecessary theoretical discussions.
- Give clear, actionable instructions.
- Do not overwhelm me with multiple unrelated tasks.
- After each milestone, explain what was completed and what remains.

10. Completion Criteria

A project is not finished simply because the APK builds.

Before declaring the application complete:

- All planned milestones must be addressed.
- Required features must be implemented.
- The application must build and install.
- Important features must be tested on the actual phone.
- Known bugs and limitations must be documented.
- GitHub documentation must reflect the final state.
- The final project must have a clear release/build status.

Final principle:

Build progressively, preserve existing work, verify actual functionality, maintain GitHub as the project source of truth, and make every development step manageable from my Android phone.


---

# DroidInspect — Product Specification and Development Plan

Last project-plan update: 2026-11-16 (M12–M18 guided APK pipeline and CI update)

## 1. App Goal

DroidInspect is an offline-first Android application for inspecting, analyzing, editing, and (where technically supported) rebuilding two kinds of software packages:

1. **Android APK files** — inspect APK structure, package metadata, manifest, resources, DEX contents, and decompiled code; provide selected editing and rebuild capabilities.
2. **.NET assemblies** — inspect Windows/.NET DLL and EXE files, read PE/CLI metadata, browse types and members, decompile supported IL into C# where possible, inspect/edit IL or metadata where safe, and export modified assemblies.

The project is inspired by the capabilities of dnSpyEx and Android reverse-engineering tools, but it is an Android-native implementation. It is not a promise of complete dnSpyEx, JADX, or Apktool feature parity.

**Core principles**

- Run locally on the phone wherever possible; no mandatory cloud service.
- Java + native Android XML is the preferred app UI and starting architecture.
- Work with user-selected files through Android's Storage Access Framework; do not request broad storage access without a demonstrated need.
- Preserve originals. Write modified output to a new file unless the user explicitly chooses otherwise.
- Clearly distinguish read-only inspection, editing, rebuilding, and debugging.
- Handle large files carefully, use background work, and report memory/performance limits.
- Do not claim support for a file or feature until it has been tested on the target Android device.
- Treat advanced debugging and runtime attachment as experimental; Android restrictions may prevent full dnSpy-style debugging.

## 2. Target Development Environment

Current project target recorded for this development session:

- Test OS: Android 11 (API 30).
- IDE: CodeAssist 3.36.1.
- Preferred source language: Java.
- Preferred UI: native Android XML.
- Existing application package: `com.tanvir.droidinspect`.
- Existing project directory in repository: `droidinspect/`.
- Existing Gradle project uses Android Gradle Plugin 8.13.0, Gradle 8.13, compileSdk 36, minSdk 26, targetSdk 36, and Java 8 source/target compatibility.

These are the repository's current settings, not proof that every future library or engine supports Android 11. Re-check device and toolchain details when they materially affect a build.

## 3. Technical Architecture Direction

Use a modular architecture so the app UI is not coupled to a single reverse-engineering engine.

- **Android UI layer:** Java activities/fragments and XML layouts.
- **File intake layer:** Android document picker / Storage Access Framework, file-type detection, safe temporary working storage.
- **Common inspection layer:** file identity, hashes, size, archive listing, errors, export handling.
- **APK processing layer:** APK/ZIP inspection first; then independently validated DEX decompilation and resource/manifest decoding or rebuilding components.
- **.NET processing layer:** PE/CLI metadata inspection first; then validate a compatible .NET runtime/bridge and decompiler/assembly libraries before committing to them.
- **Editing/export layer:** operate on working copies, validate outputs, and preserve the original input.
- **Testing layer:** repeatable test fixtures, diagnostic reports, and a one-tap test entry point for major test suites where practical.

Important feasibility constraints:

- A Java Android app cannot directly use .NET-only libraries such as dnlib or the ILSpy decompiler engine as ordinary Java dependencies. A .NET runtime integration/interop bridge or a separately justified alternative must be proven before selecting that route.
- JADX and Apktool have Android-hosting constraints. Do not assume their desktop/CLI builds can simply be embedded and run on Android. Prototype and measure memory, CPU, API compatibility, temporary storage, and licensing first.
- If a preferred engine cannot run reliably on Android 11 / CodeAssist, investigate a smaller native implementation or clearly document a reduced feature scope. Do not silently change the Java/XML preference; discuss a major architecture change first.
- Check and document each dependency's license and redistribution requirements before packaging it.

## 4. Complete Roadmap — 20 Milestones

The long-term roadmap has **20 milestones**, grouped into eight phases. M1–M10 retain their existing scope and completed history. M11 establishes real on-device C# source decompilation; M12 covers separate-copy C# source editing and managed DLL recompilation. M13–M20 extend the product into deeper .NET analysis, advanced source tools, APK analysis/editing, project management, and final reliability/integration. Existing completed capabilities are foundations to extend, not work to restart. M12 is active following consolidated production M11 device acceptance on 2026-11-14; its first gate is a proven offline Android ARM64 compiler. Later milestones must preserve working features, prove Android feasibility before adopting major engines, and keep the offline-first/phone-only development requirements.

### Phase 1 — Foundation and file understanding

**M1 — Project foundation and on-device feasibility**
- Confirm the existing CodeAssist project builds, installs, and starts on the Android 11 test phone.
- Establish the project structure and basic file-selection flow.
- Detect APK, DLL, and EXE file types safely.
- Prototype APK ZIP/container reading and basic PE/CLI header detection.
- Record engine compatibility findings before adopting large dependencies.
- Exit criteria: baseline app works on-device and both file families can be identified; any metadata-reader prototype is tested and its limits recorded.

**M2 — APK inspection**
- Open a user-selected APK without installing it.
- List archive contents and inspect package/application metadata, version, SDK declarations, permissions, and manifest information where parsing is supported.
- Inspect DEX file presence and basic structure.
- Provide readable errors for malformed, encrypted, unsupported, or oversized inputs.
- Exit criteria: test APKs produce correct, cross-checked inspection results on the phone.

**M3 — .NET assembly inspection**
- Identify PE files and determine whether they contain a valid .NET/CLI header.
- Read assembly/module identity, version, architecture/runtime flags, metadata tables, references, and type/member names as supported.
- Distinguish native PE files from managed .NET assemblies.
- Exit criteria: known managed DLL/EXE fixtures and native/non-managed PE fixtures are classified correctly, with limitations documented.

### Phase 2 — Code and resource analysis

**M4 — APK DEX analysis and code browsing**
- Evaluate and integrate an Android-compatible DEX analysis/decompilation engine only after an on-device prototype passes.
- Browse packages, classes, methods, fields, and decompiled source where supported.
- Add search and navigation.
- Measure memory use and handle large APKs without freezing the UI.
- Exit criteria: representative APKs can be browsed reliably on the test phone; unsupported cases are reported.

**M5 — .NET metadata and IL browsing**
- Browse assemblies, namespaces, types, methods, properties, and fields.
- Display raw IL for supported methods, with text search and navigation.
- Preserve the bounded metadata/IL reader and constrained editing capabilities implemented in later milestones.
- Full C# source reconstruction is deferred to M11; raw IL is not a substitute for C# decompilation.
- Exit criteria: representative managed DLL/EXE files can be inspected and their supported metadata/IL browsed reproducibly on-device.

**M6 — APK resources and manifest analysis/editing**
- Decode supported manifest and resource structures.
- Provide controlled editing of selected manifest/resource values and, if feasible, Smali/DEX-related text representations.
- Validate edits and preserve an untouched source copy.
- Exit criteria: supported edits are saved to a working project and pass structural validation.

**M7 — .NET assembly editing**
- Evaluate safe assembly metadata and IL editing using a validated engine.
- Support selected metadata/member edits and IL editing only where the engine and format permit.
- Validate output structure and report unsupported or risky operations.
- Exit criteria: a test assembly can be modified, reopened, and checked for expected changes without corrupting the original.

### Phase 3 — Output, advanced tools, and release

**M8 — APK rebuilding, signing and verification**
- Existing ARSCLib 1.4.0 rebuild/export remains a separate unsigned-output workflow; the original APK is never overwritten.
- Add a dedicated signing screen using Google's `com.android.tools.build:apksig:8.13.0` library (Apache-2.0).
- Generate RSA-2048 signing keys in Android Keystore using the supplied alias and certificate owner; support importing existing PKCS#12/PFX and JKS keystores with alias/password handling.
- Sign a separate APK copy using v1, v2 and v3 schemes; verify the signed output before enabling export.
- Show signer certificate subject, issuer, serial, validity and SHA-256 fingerprint. Reject invalid/tampered APKs and report verification failures.
- The generated Android Keystore private key is non-exportable and tied to the app installation/device. For portable long-term signing, import and securely back up a PKCS#12/JKS key. A different signing key normally cannot update an app installed with the original developer's key.
- APK Signature Scheme v4 sidecar export is not implemented. Device install/update behavior remains pending the consolidated device test.
- Exit criteria: key generation/import, sign, verify, certificate inspection, tamper rejection, separate export and Android 11 install/update behavior are tested.

**M9 — Advanced .NET inspection and C# decompilation feasibility**
- Preserve the bounded raw ECMA-335 IL browser and existing metadata/member search and constrained editing.
- Show instruction offset, exact raw instruction bytes, mnemonic, operand and human-readable explanation; unknown opcodes receive no invented explanation.
- Show MethodDef metadata tokens in method labels so tokens can be searched alongside method names and RVAs.
- Full C# decompilation is not integrated. ILSpy's decompiler engine is a .NET library, not a Java/Android dependency; embedding it would require a separately shipped .NET runtime or a native/AOT bridge and a different Android build toolchain. That is not a safe drop-in addition to this Java + XML CodeAssist project. Raw IL is explicitly not labelled as C#.
- Exit criteria: instruction/operand/offset fidelity, known-opcode explanations, unsupported-opcode handling, token navigation, and existing M5/M7 regression behavior are verified.

**M10 — Integration, reliability, testing and release preparation**
- Integrate APK signing into the main navigation and IL explanations/token navigation into the existing .NET browser.
- Keep SAF-only file access, bounded APK/keystore copies, private temporary storage, stale temporary-file cleanup, separate outputs, and non-exported signing Activity.
- CI runs the ECMA-335 metadata, IL and DEX host regression suites; builds debug and unsigned release APKs; signs with an ephemeral CI key; verifies v1 JAR, v2 and v3 signatures; rejects a tampered APK; and uploads debug plus signed-release artifacts.
- Review dependency license, Android 11 compatibility, error paths, file limits, permissions and known limitations.
- Device-only checks are delivered as one consolidated M8–M10 test batch; CI success is not device-test evidence.
- Exit criteria: all required CI jobs pass, APK artifacts exist and are integrity-checked, README matches the source, and the remaining device-only limitations are explicit.


### Phase 4 — Full .NET C# source decompilation

**M11 — Full C# decompilation engine and Android integration (IMPLEMENTATION COMPLETE; AUTOMATED CI VERIFIED; DLL AND MANAGED EXE SOURCE RECONSTRUCTION PASSED ON DEVICE; PRODUCTION-INTEGRATION DECISION PENDING)**

**Objective:** Add genuine C# source decompilation for supported managed .NET DLL/EXE assemblies, directly on the Android device, while preserving the existing Java + native XML application and the current metadata/IL/editing features.

**Required work, in order:**

1. **Engine and runtime feasibility**
   - Evaluate the official ILSpy decompiler engine as the first candidate, plus technically credible alternatives only if needed.
   - Determine whether the engine can execute on Android 11/API 30 through a supported .NET runtime, Mono embedding route, Native AOT/native bridge, or another maintainable approach.
   - Verify Android ABI coverage, runtime packaging, native library loading, reference-assembly resolution, dependency closure, licensing, app size, memory/CPU use, and offline operation.
   - Verify whether the required native/.NET toolchain can be built and maintained in GitHub Actions and whether the resulting Android integration remains practical for the user's CodeAssist workflow.
   - Do not assume that a .NET NuGet package, desktop ILSpy executable, or Native AOT support automatically means an Android-compatible library.

2. **Isolated proof of concept**
   - Build a minimal end-to-end prototype before adding a decompiler dependency to the production app.
   - Input: a user-selected managed DLL/EXE fixture.
   - Output: actual reconstructed C# source for known methods and types, not IL text, pseudocode, templates, or hand-written opcode translations.
   - Include at least a simple method, fields/properties, branches/loops, exception handling, and compiler-generated constructs in the fixture set as engine support allows.
   - Run the prototype on the Android 11 test device; a host-only or GitHub Actions pass is not sufficient.
   - Measure cold-start time, decompilation time, peak memory, APK/native payload size, and behavior on larger assemblies.

3. **Production integration**
   - Integrate the proven engine behind a clean Java-facing interface while keeping the Android UI in Java/XML unless a demonstrated technical requirement makes that impossible.
   - Add a View C# action for supported managed assemblies/methods, with loading/progress, cancellation where feasible, readable errors, and a clear unsupported-assembly result.
   - Preserve View IL as a separate option; never label raw IL or explanatory pseudocode as C#.
   - Add source navigation/search and sensible handling of nested types, generics, attributes, properties, events, async/await, lambdas, and references to external assemblies to the extent the selected engine supports them.
   - Keep all processing offline, bounded, and isolated from original files. Decompilation must remain read-only.

4. **Compatibility, regression and acceptance**
   - Test pure-managed DLL and EXE fixtures from more than one compiler/runtime generation where practical.
   - Test malformed, unsupported, mixed-mode/native, obfuscated, and large inputs; report limitations rather than crashing or inventing source.
   - Verify the existing M3 metadata reader, M5/M7 IL browser and editor, and M8–M10 APK workflows still work.
   - Run host tests, Android build/packaging checks, native ABI checks, and GitHub Actions; then complete real-device testing on Android 11 using the CodeAssist-compatible project.
   - Document exact engine/runtime versions, licenses, ABI support, Android/API requirements, build instructions, performance limits, and unsupported assembly categories.

**Hard acceptance criteria:**
- The app produces semantically reconstructed C# source from real managed assembly metadata and IL using a genuine decompiler engine.
- The feature is exercised on the Android 11 phone, not merely compiled or tested on a desktop runner.
- The output is demonstrably C# source, distinct from the raw IL view.
- The implementation works offline, preserves the original assembly, and does not destabilize existing inspection/editing features.
- The complete build and native/runtime packaging path is reproducible in CI and remains maintainable for the phone-only CodeAssist workflow.

**Architecture decision gate:** No production dependency, runtime, ABI change, or UI entry is to be added until the isolated Android proof of concept passes. If no viable route satisfies the device, licensing, performance, and CodeAssist/CI constraints, stop at the feasibility report and present the alternatives; do not ship a fake or partial feature under the name full C# decompilation.

### Phase 5 — C# source editing and managed assembly rebuilding

**M12 — C# Source Editing & DLL Recompilation (IN PROGRESS — PRODUCTION INTEGRATION PRESENT; DEVICE ACCEPTANCE PENDING)**

**Objective:** Let the user edit reconstructed C# source as a separate working copy and, where technically supported, compile that edited source into a new managed DLL without modifying the original assembly or the original decompilation result.

**Current status (2026-11-16):** Production Java/XML source workspace and compile/export UI are integrated with the offline Roslyn Native AOT ARM64 compiler through JNI. Host tests, two-file compilation, managed PE/CLI verification, behavioral invocation, invalid-source output preservation, and explicit original-assembly overwrite rejection pass in GitHub Actions. The ARM64 Bionic compiler, JNI bridge, isolated proof APK, and production native payload checks also pass CI. Android execution/compilation on the user's phone remains pending for the consolidated M20 device-testing session. Native AOT trim-analysis warnings IL2091, IL2050, and IL2072 remain visible and unresolved; they must not be described as harmless or as proof of complete runtime safety.

**Required work, in order:**

1. **Editing model and source preservation**
   - Keep the original input DLL/EXE immutable.
   - Keep the first decompilation result available as a read-only reference.
   - Add an explicit Edit Source action that creates a separate editable working copy; never silently make the original result editable.
   - Support editing and saving reconstructed C# source as separate .cs file(s), with clear filenames and a defined project/source-folder structure.
   - Clearly distinguish decompiled source, edited working source, and compiled output in the UI and status messages.

2. **Compilation feasibility and project generation**
   - Identify the actual target framework/runtime and language/compiler requirements of the input assembly where metadata permits; report uncertainty rather than guessing.
   - Evaluate a compatible offline .NET compiler/toolchain that can run through the app's supported Android architecture. Prove it in an isolated prototype before adopting it in production.
   - Generate the required project/build configuration and references for the selected target. Do not assume that successful decompilation guarantees recompilable source.
   - Resolve or explicitly report missing, mismatched, unavailable, or non-redistributable dependencies. Do not silently substitute incompatible reference assemblies.
   - Document compiler/runtime versions, licenses, ABI and Android API requirements, offline payload size, memory/CPU use, and CodeAssist/CI reproducibility.

3. **Rebuild into a separate assembly**
   - Compile the edited source into a new managed DLL, for example Newtonsoft.Json_edited.dll.
   - Never overwrite the selected original DLL/EXE or the untouched decompilation output.
   - Keep the source files and compiler diagnostics available when compilation fails.
   - Show build progress, errors, warnings, target framework, output identity, and output path.
   - Do not claim binary-equivalent reconstruction: rebuilt assemblies may differ in metadata, signing, layout, compiler-generated details, and behavior.

4. **Validation, replacement compatibility and export**
   - Reopen the rebuilt DLL with DroidInspect and verify its PE/CLI structure, assembly identity, metadata, and expected edited symbols or members.
   - Evaluate whether the rebuilt DLL can replace the original in its intended application. Never assume successful compilation alone makes it a drop-in replacement.
   - Compare required assembly identity (name, version, culture, public key/token), public types and member signatures, target framework/runtime, and dependency requirements. Preserve compatibility where required or clearly report differences.
   - Check strong-name signing and other signature implications; a rebuilt assembly may not retain the original signing identity and may be rejected by consumers that require it.
   - Run available structural and behavioral checks; distinguish structural validity from runtime compatibility and behavioral equivalence.
   - Produce a user-readable **DLL Replacement Compatibility Report** for every rebuilt output. The report must include:
     - **Assembly identity:** original versus rebuilt name, version, culture, and public-key token; explicitly flag identity changes.
     - **Public API:** compare public types and externally visible method/property/field signatures; list added, removed, renamed, or changed members where detectable.
     - **Target framework/runtime:** show the detected original target (or state that it is unknown), the compiler output target, and any framework/API mismatch.
     - **Dependencies:** list required references with requested and resolved versions/identities, and classify each as resolved, missing, mismatched, or unsupported.
     - **Signing:** report whether the original is strong-name signed, whether the output is signed, whether the public-key token matches, and whether the original private key is unavailable. Never imply that re-signing with a different key preserves the original identity.
     - **Overall assessment:** separate statuses for compilation, structural compatibility, and runtime/behavioral compatibility. Use clear outcomes such as PASS, WARNING, FAIL, or NOT VERIFIED, with a short reason for every warning or failure.
     - **Replacement guidance:** state whether the output is suitable for a controlled test, whether compatibility is unverified, or whether known differences make it unsuitable as a drop-in replacement. Never overwrite or automatically replace the original file.
   - Export the rebuilt DLL as a new file through Android's document picker/SAF. Preserve the edited .cs source and build report as separate exports where practical.
   - Report unsupported inputs, framework limitations, and any dependencies the recipient must provide.

**Three explicit success levels (report each independently):**
- **Level 1 — Compilation:** the edited C# source compiles and produces a valid managed DLL. This alone does not establish compatibility.
- **Level 2 — Structural compatibility:** the rebuilt assembly preserves the identity, required types, public members/signatures, framework target, and dependency expectations needed by the intended consumer, or clearly documents any differences.
- **Level 3 — Behavioral compatibility:** the intended application or a suitable runtime test successfully uses the replacement and the relevant behavior—including the user's edits—is verified. A structural match alone does not prove behavior.

**Hard acceptance criteria:**
- The user can edit a separate copy of reconstructed C# source while the original assembly and original decompilation remain unchanged.
- Edited source can be saved as separate .cs file(s).
- A supported offline compilation path produces a new managed DLL on the Android device.
- The output DLL is reopened and its expected edited changes are verified by DroidInspect.
- Missing/incompatible references and compilation failures are reported clearly; no fake success or extension-only rename is allowed.
- The workflow is tested on the user's phone, reproducible in CI, and compatible with the established phone-only CodeAssist workflow.

**Architecture and feasibility gate:** Source editing and DLL compilation are separate capabilities. A text editor plus .cs export does not constitute DLL recompilation. If a reliable offline Android compiler/runtime path cannot be proven, deliver source editing/export as a clearly bounded subfeature and document the blocker and alternatives; do not label M12 complete or claim DLL rebuild support.


### Phase 6 — .NET dependency intelligence and advanced assembly tools

**M13 — Dependency and framework resolution (IN PROGRESS — HOST REGRESSION PASS; DEVICE ACCEPTANCE PENDING)**
- Build on M11's observed unresolved/mismatched-reference diagnostics.
- Identify required framework/reference assemblies and dependency versions using assembly identity, version, culture, public-key token, target framework, and dependency relationships.
- Support user-provided local dependencies and carefully validated reference packs; keep offline operation as the default.
- Add opt-in online-assisted retrieval only with explicit consent, trusted sources, integrity/provenance reporting, licensing checks, and no automatic upload of inspected files.
- Exit criteria: representative dependency graphs resolve accurately or produce precise, actionable unresolved/mismatch reports; no silent incompatible substitutions.

**M14 — Assembly Explorer and advanced IL Viewer (IN PROGRESS — NAMESPACE TREE ADDED; ANDROID CI PASS; DEVICE ACCEPTANCE PENDING)**
- Extend the existing metadata and IL browsing foundations into a navigable assembly tree: assemblies, modules, namespaces, types, fields, properties, events, methods, and references.
- Improve method navigation, token/RVA display, raw IL inspection, instruction bytes, operands, and cross-reference navigation.
- Add filters, sorting, bookmarks, and contextual search without removing current inspection/editing behavior.
- Exit criteria: representative managed assemblies can be explored consistently, and displayed metadata/IL is cross-checked against known fixtures.

**M15 — Advanced C# source editor (IN PROGRESS — INITIAL PHONE-ORIENTED EDITOR FEATURES ADDED)**
- Extend the basic separate-copy source editing/rebuild workflow planned in M12 with a phone-usable multi-file editor.
- Add syntax highlighting, line numbers, find/replace, file tabs or an equivalent lightweight file navigator, unsaved-change indicators, and readable compiler diagnostics.
- Preserve original assemblies and untouched decompilation output; save edits as separate source files.
- Exit criteria: multi-file source projects can be edited and saved reliably on the phone, with editor usability and memory behavior tested on-device.

**M16 — Assembly comparison and resource extraction (IMPLEMENTATION COMPLETE — HOST REGRESSION TESTS AND ANDROID CI PASS; DEVICE ACCEPTANCE PENDING)**
- Compare two assemblies by identity, references, types, members, signatures, metadata, and supported IL changes.
- Present additions, removals, and modifications without implying semantic equivalence where only structural comparison is available.
- Browse and extract supported embedded resources to separate user-selected output files; preserve original inputs.
- Exit criteria: known changed/unchanged assembly pairs produce accurate comparison reports, and resource extraction passes integrity checks.

### Phase 7 — Advanced APK workflows

**M17 — Advanced APK inspection and DEX analysis (IMPLEMENTATION COMPLETE FOR SUPPORTED SCOPE — HOST TESTS AND ANDROID CI PASS; DEVICE ACCEPTANCE PENDING)**
- Extend the existing M2 APK inspection and M4 DEX browsing foundations; do not recreate completed features.
- Improve package/component navigation, manifest and permission analysis, multi-DEX handling, resource mapping, global search, and cross-reference navigation.
- Add robust handling for split APKs, large inputs, malformed structures, and unsupported formats where feasible.
- Exit criteria: representative APK families are inspected and browsed accurately on-device, with limitations clearly reported.

**M18 — APK editing, rebuilding, and signing workflow (IMPLEMENTATION COMPLETE FOR SUPPORTED EDIT TYPES — HOST TESTS AND ANDROID CI PASS; DEVICE ACCEPTANCE PENDING)**
- Extend the existing M6 resource/manifest editing and M8 signing foundations into a coherent project-based workflow.
- Support validated edits to selected resources/manifest/DEX-related representations where the underlying tools permit.
- Rebuild to a separate APK, sign with supported schemes, verify signatures, and report install/update compatibility constraints.
- Preserve the original APK and project inputs; never imply that a rebuilt APK can retain another developer's signing identity.
- Exit criteria: supported edit → rebuild → sign → verify → export flows pass structural checks and agreed real-device install tests.

### Phase 8 — Project workspace, reliability, and final integration

**M19 — Project workspace and professional tools**
- Add persistent workspaces for APK and .NET analysis, recent projects, file trees, bookmarks, notes, saved searches, and export/report management.
- Provide clear project state, operation history, and recovery from interrupted work.
- Keep storage transparent and user-controlled; use SAF and app-private working storage appropriately.
- Exit criteria: users can close and reopen projects without losing saved work, and workspace data remains isolated and recoverable.

**M20 — Performance, compatibility, and final integration**
- Profile large APKs and assemblies, reduce memory duplication, improve background processing/cancellation, and handle low-memory conditions safely.
- Validate supported Android versions, ABIs, CodeAssist build reproducibility, permissions, licensing, and dependency payload sizes.
- Run full regression suites across APK and .NET workflows; resolve integration conflicts and document unsupported cases.
- Complete final on-device acceptance, release checks, and user-facing documentation.
- Exit criteria: the agreed roadmap scope is implemented or explicitly bounded, critical device tests pass, CI is reproducible, originals remain protected, and the release status/limitations are documented accurately.


## 4.1 Development Log — 2026-11-16

Latest inspected repository branch: `main`.
Latest inspected HEAD at this documentation update: `7abe9688121725517ba4ad6fce4d9ccb2dd0fdec`.

### M12/M13 verification evidence
- Commit `7922a821cc5a4a5912b75e61ae64a281bbd9202c`: reject compiler output paths that equal the preserved original assembly.
- Commit `51cf334b170d7417a78ba99c6b7e2ee4daae596d`: add original-assembly overwrite regression; the first test attempt exposed an incorrect hash baseline, corrected in `af2b143636b45d287e8117112cafc3d9ce7599ae`.
- Commit `c315888bb116aad026b5c8fb0ec52ccbb480c3ab`: framework mismatch warnings now force an incomplete dependency-resolution result.
- Commit `e4c2a95e4c009066acf73a5889f5d65396855627`: M12/M13 host regression suite and Android debug/release CI passed.
- M12 compiler workflow run [43777372639](https://github.com/xolomon-coder/DroidInspect/actions/runs/43777372639): all steps passed, including Roslyn host compile, original immutability checks, failed-build preservation, host behavioral invocation, M13 exact/mismatched/missing dependency tests, Bionic ARM64 Native AOT publish, JNI link/export checks, isolated Android proof APK packaging, offline permission check, and native payload publication.
- M12 workflow artifact: `DroidInspect-M12-Roslyn-Compiler-Proof` (cloud verification only; not the user's normal delivery method).
- Remaining M12 limitations: Android device execution is pending; only the bundled .NET 11.0 reference pack is currently supplied; strong-name identity preservation and broad decompiled-project recompilation are not proven; Native AOT trim-analysis warnings IL2091, IL2050 and IL2072 remain unresolved.
- M13 host tests now distinguish exact local resolution from framework compatibility. A .NET 8 target against the bundled .NET 11 reference profile is reported as INCOMPLETE even when its discovered dependency resolves exactly.

### M14/M15 implementation
- Commit `687d994eda328a1ca6b32e7f40d1f46fd9f9df86`: add namespace entries and namespace-to-type navigation in Assembly Explorer.
- Commits `33123c8b6437b80766da0a9df39f1326afd9e2a8`, `b4fc6c5a8bf000950c8d3b327afa4b42f51636c2`, and `be80028b884cf509c1c5b2814de81b79f77f4481`: add lightweight offline C# syntax highlighting, line numbers, current-file find/replace, and retain the existing source-file selector as the lightweight file navigator.
- Android CI run [43777578124](https://github.com/xolomon-coder/DroidInspect/actions/runs/43777578124) passed for namespace navigation; Android CI run [43777648793](https://github.com/xolomon-coder/DroidInspect/actions/runs/43777648793) passed for the M15 editor and documentation integration.
- M14/M15 remain implementation-in-progress: advanced metadata/signature cross-reference coverage, editor tabs and phone usability still require work and/or device verification.

### M16 implementation — 2026-11-16
- Commits `4a8a3b206605e1dbc2198b2cbc641ea571f8f64f`, `c3b73b2b99a0600c6fe6782fefd3a44d15e250a2`, `9bce646a913d8281041246fec5aff904b8f7e245`, `b2323399309cc737d378af0bbc6129a54ad8d17d`, `eaa64fd8b186fe812e97d60a1b56d16c5226ffff`, and `3488e8495a9db0178dfb99fc759da06dc5b64682`: add the production structural assembly comparison screen, compare identity/reference/type/member/property/event metadata and raw IL body records, and expose it in the main menu.
- Commits `4f033b684a68496ac7d6c337738e79e76fd34058`, `2014a5b4ba3be0e1f8839630edaa93301d0aa9d5`, `ea7aa263b961d01cb9b100cfcdf1f3942b7e5291`, `288325ee497811c5bdd24e128415694821e1f619`, `a7d202b7de4d03c533d758a117cc3bdb38f1dd6c`, `6ce33b76ff48978fc96e06ead3f57d35e9f96e00`, and `255befbf8d88c84214d786676c6f7f4f3717decc`: decode ManifestResource metadata, add bounds-checked embedded resource extraction, add a production resource browser/export screen, and add its main-menu entry.
- Commits `e78a2e3737ae61842c65a7ec44352cf7726b4a6f`, `2486f2965a0c35627f495977dea92a405339bdc3`, `05d3db809cb29c1fd40f28b4ed95d7acc9ec9d99`, and `4a0e99310d6ea9184511b2bcd6c09cd31122ecfa`: correct metadata decoder newline handling and add a synthetic ManifestResource metadata fixture with embedded-resource assertions.
- Android CI run [43779727702](https://github.com/xolomon-coder/DroidInspect/actions/runs/43779727702) completed SUCCESS on commit `4a0e99310d6ea9184511b2bcd6c09cd31122ecfa`. Host metadata/IL/DEX tests, Android debug and release builds, APK signing/tamper checks, production native payload checks, and offline permission checks passed. Artifact `DroidInspect-Phase3-builds` was published (69,561,735 bytes; SHA-256 `99dc462b7bec74b561ff2a1b4a592f8edc59652115bd59ed52b0910175ecba7e`).
- M16 is not complete yet: current comparison is structural, not semantic; signature-level comparison is not implemented; parser display caps can omit records; linked resources are listed but not exported; the synthetic fixture verifies resource metadata decoding, but end-to-end embedded payload extraction integrity still needs a dedicated automated fixture and final device verification. Original inputs are copied to cache and never opened for writing.
- Additional resource-extraction commits `5d06704b9a35c795cdd3e5c466baed87c0c173a6`, `3c9c62b6366f478234ae53688c32d8565da96a48`, `e4c8ff5bc2710b68c09bdefd09bea2df7fe5fbd6`, `de40271e0f6d1a5f222c488c447b61ccf68e61e4`, `b08e9b10ca2f477bf675ea7ccd181ad127c5eab1`, and `6623ee13907cd9a45fae14ff55f37414ca3b1d45`: isolate PE/CLI resource extraction, add synthetic payload/negative fixtures, add a host CI test, and correct resource-kind parsing. The fixture validates exact extracted bytes, rejects out-of-range offsets and truncated payloads, and verifies that the source assembly hash remains unchanged.
- Commits `24896c88bd4f6dcfa41b42db86d1978d97c4ff77`, `ac117d531360f84a7fe723c18c9e6ffcd0309965`, and `a783dd5d9315c0af231beeeeca5be9d60f62a1ee`: include ManifestResource rows in assembly comparison and ensure oversized/unavailable IL is reported as unverified rather than falsely unchanged.
- Android CI run [43780357379](https://github.com/xolomon-coder/DroidInspect/actions/runs/43780357379) completed SUCCESS on `a783dd5d9315c0af231beeeeca5be9d60f62a1ee`. The ManifestResource metadata fixture, embedded resource byte-integrity/bounds/source-immutability fixture, IL and DEX host tests, Android debug/release builds, signing/tamper checks, production native payload and offline permission checks all passed. Artifact `DroidInspect-Phase3-builds` was published (69,565,111 bytes; SHA-256 `29670c818390b16c30e56e19ba35be3f6d9133c336ece09e5c10927b104e98ca`).
- M16 implementation status: assembly identity/references, types, fields, methods, properties, events, raw field/method/property signature blobs, available raw IL bodies, and embedded ManifestResource entries are compared or reported. Linked resources are listed but are not extracted. Signatures are compared as raw ECMA-335 blob bytes, not fully decoded into human-readable parameter/return types. The report is structural and does not establish semantic equivalence or binary compatibility. Real-world assembly-pair comparison and resource extraction on device remain part of the final consolidated acceptance session.
- Commits `d4faf525d2541566a74895f7d95cd46e140f7b83` and `520308b04a6984a3c7ce77061a58636e11ca8510`: include raw field, method, and property signature blobs in metadata output and assert representative non-empty signature bytes in the metadata-reader fixture.
- Android CI run [43781106796](https://github.com/xolomon-coder/DroidInspect/actions/runs/43781106796) completed SUCCESS on `cfd01a98ca8629f85978a03c286ab6b8213d84b2`. All host tests (metadata/signatures, resource bytes/bounds/source immutability, IL, assembly diff classification, and DEX), Android debug/release builds, APK signing/tamper checks, production native payload verification, offline permission audit, and artifact upload passed. Artifact `DroidInspect-Phase3-builds` was published (69,567,064 bytes; SHA-256 `ca03dbebf9c788e4cae368c6c02a7aa704b460e929dbc470df0e820f8594be8c`).
- M16 device acceptance remains pending. Original assemblies are read-only and resource export writes to a separately selected destination.- Commits `bcd327d0faea78c5e7354a023ed8d5244b563485`, `416da91f3c6762b2e83e2d574829119d44d7ef12`, `eeb23bac879ee29512287786bd6abcb904ff3b96`, and `631022b56b18bb2fac378deb065efce55b2f65c4`: extract the pure structural/IL diff engine, connect it to the Android screen, and add host regression coverage for added, removed, changed, unchanged, and unavailable IL records.
- Android CI run [43780703602](https://github.com/xolomon-coder/DroidInspect/actions/runs/43780703602) completed SUCCESS on `631022b56b18bb2fac378deb065efce55b2f65c4`. All host parser, resource-integrity, comparison-diff, IL and DEX tests passed; debug/release Android builds, APK signing/tamper checks, native payload checks, offline permission audit, and artifact upload passed. Artifact `DroidInspect-Phase3-builds` was published (69,565,674 bytes; SHA-256 `1e70edb27aaaebaa8a6b45d4ef35f09d8229b7efdbe499a39da667bd8b12fdb2`).
- The production manifest and permission audit remains offline; no Internet permission was added. Device acceptance for M12–M16 is pending the consolidated final test session.

### M17 implementation — 2026-11-16
- Commits `1a5e1fea15ab6cb00e12a54675ba604c42541412`, `1774ad73d12fe1a8709f5f63959361e4e94d1c92`, `257af2d361318046b4e104f617bda3135e05a1e4`, and `ec98a2e8d31e91b8ce07bf645a3434bd71bd7387`: add a structured searchable APK manifest explorer for activities, activity aliases, services, receivers, providers, permissions, features, SDK declarations, exported/enabled state, component permissions, authorities, intent filters, actions, and categories.
- Commits `8bb3b034e72cc55dbdb1f1ebb0af00411c843328`, `54451e966b10f72eba470c194e686d514d18b0f2`, `a2a35ebbd1f9122a3a162e1c4b76ece7e3f98d40`, `e7dc3b896fe5e5e47ee36e199124f6276d52d70f`, `8f5bf4778ad36c7278fdc176b0fb0fa5d6426d6c`, and `9b22ab2488e68f7add77f900327c5a5727c541b1`: add multi-select split APK set inventory, base/split identity and version consistency checks, split-name/feature/config metadata, uses-split dependencies, component/permission/DEX/resource-package counts, safety limits, and source filename reporting.
- Commits `6906cf075cac077ba20f26166f5ca11f4acba512`, `776a8dc89d1ef71a840d852349004469c26af462`, `72513854ab33a4a4eff8b247721e69049d1d44e9`, `f652a23d32f6b00f81bde1073f3515333b86a2e0`, `715f99b0908c23142f6de46c6b38936e57b35a44`, and `b22a7af7be02e360b04b10e912cdcc056f58804c`: scan supported DEX invoke instructions into caller-to-target cross-reference records, expose references in class details and global search, add per-class/global caps, and make rejected DEX parsing roll back cross-reference counts transactionally.
- The DEX synthetic fixture now contains an actual `invoke-static` instruction. Host tests verify target resolution and ensure references from a malformed DEX are rolled back. The parser remains read-only and does not decompile Java source.
- Android CI run [43782215711](https://github.com/xolomon-coder/DroidInspect/actions/runs/43782215711) completed SUCCESS on `9b22ab2488e68f7add77f900327c5a5727c541b1`. Host metadata/resource/IL/comparison/DEX tests passed; Android debug/release builds, signing/tamper checks, native payload checks, offline permission audit, and artifact upload passed. Artifact `DroidInspect-Phase3-builds` was published (69,594,616 bytes; SHA-256 `801055aba6655f34ea3ebd2b2d52308e40b2c4b5bac9532f68cb6be4ff0b8e12`).
- Commits `d5d3245df6c126d48c23389c9e732e2bf76bd12b`, `b047325472568e0ffce572c919b9f27a573db98c`, `5b1e3504e0a542aac56298ae5bea3b2e4915981d`, and `f08258b64a76578f899c4f3ddbe656015eccf858`: add a read-only searchable resource catalog mapping resource type/name/ID to available default values, with per-APK scan and storage caps.
- Android CI run [43782643275](https://github.com/xolomon-coder/DroidInspect/actions/runs/43782643275) completed SUCCESS on `f08258b64a76578f899c4f3ddbe656015eccf858`. All host tests passed, including invoke cross-reference and malformed-DEX transactional rollback. Android debug/release builds, APK signing/tamper checks, native payload verification, offline permission audit, and artifact upload passed. Artifact `DroidInspect-Phase3-builds` was published (69,597,425 bytes; SHA-256 `ef844ab98c7f3c1ca6044e11549a66b4108aea50168c77db61a22b5fccf96ade`).
- Commits `01cfe02f955c5fb0536c07937bdb248aea1df397` and `530f3a942247c043074aa3eda3cc23f859328ff8`: add in-app navigation from DEX invoke references to locally indexed target classes and expose additional component launch/provider attributes including targetActivity, launchMode, screenOrientation, taskAffinity, read/write permissions, URI grants, and direct-boot state.
- Android CI run [43783028036](https://github.com/xolomon-coder/DroidInspect/actions/runs/43783028036) completed SUCCESS on `530f3a942247c043074aa3eda3cc23f859328ff8`. All host tests passed, including DEX invoke resolution and malformed-entry transactional rollback. Android debug/release builds, APK signing/tamper checks, native payload verification, offline permission audit, and artifact upload passed. Artifact `DroidInspect-Phase3-builds` was published (69,598,117 bytes; SHA-256 `68002f506ece9d84c61fa2ed2d253f65d1a3a886cede7c4f25af701487658a13`).
- M17 implementation is complete for the supported scope; device acceptance remains pending. Known limitations: split APK support inventories and consistency-checks selected files but does not merge/install them or parse a combined split DEX/resource graph; resource-to-code mapping is not yet provided; invoke cross-references cover supported invoke opcodes, not every DEX reference kind. Resource catalog indexes at most 50,000 scanned records / 25,000 stored entries. DEX cross-reference indexing is capped at 2,000 records per class and 100,000 total. Root-level multidex files are handled independently; malformed DEX entries are rejected transactionally. Representative APK-family verification remains for the consolidated final device session.

### M18 implementation — 2026-11-16
- Commits `c8900c9ae27c19ac2012c31a716ceda4ae4317fe`, `d5adb7696f891ca41fa37c6961736b81a8185a26`, `ef8fdb89785dabd126ee48effe3d0027748ed4d8`, and `ddefd136190be8b5aaa1c8df64cf26edcc68c6be`: add a guided production flow that selects an original APK, launches the existing manifest/resource editor, receives the separate unsigned output, launches signing/verification, and receives the exported signed APK.
- Commits `6754174e154292b2cbae86ceae989f5d921438e1`, `de20e6ef330e793c2e00796d09b24b99b4788d69`, `243c1c682ef9e3c0a5ae1704317f0203aaf8ceea`, and `d1c0aa7641bf8f2019e0cdfaf50414b6f8fb7c89`: add a pure update-compatibility assessment and host tests requiring the same package ID, a higher version code, and an exact verified signer-certificate fingerprint set.
- Commits `baac2b2d132bb4efbc905ad4187c1ca4c65760b8`, `38d90e71ef3ea02e5a1bcd12160ad6bf9a0c99c9`, `31da24021e3de4adbcd01681141d7dd7867fc303`, `a34843d75635b2857f7fa0da49dac62dea7f9301`, `ad411f83eef5da0094e4d9c9d2babfad0b065ec9`, `9e8f2827307afdb809f46f5669c8eabd64ff5d28`, `0f1afde2a536631a68130fcf8a378d347dc87f87`, `a95d9e19ac6996a72e7f1cf64c132657b220641c`, and `7abe9688121725517ba4ad6fce4d9ccb2dd0fdec`: correct compile issues, reset workflow state between source selections, use long version codes, make the main menu scrollable, and retain SAF output URI access for the editor-to-signer handoff.
- The final pipeline reopens the exported signed APK and verifies its signature again. It compares package identity, version code, and original/new signer fingerprints; it never claims to preserve a developer's signing identity. The original APK, edited unsigned APK, and signed APK are separate files.
- Android CI run [43784022921](https://github.com/xolomon-coder/DroidInspect/actions/runs/43784022921) completed SUCCESS on `7abe9688121725517ba4ad6fce4d9ccb2dd0fdec`. Host tests (including update-compatibility cases), Android debug/release builds, APK signing/tamper checks, native payload verification, offline permission audit, and artifact upload passed. Artifact `DroidInspect-Phase3-builds` was published (69,611,869 bytes; SHA-256 `b2445cf933f3c6c333e922877275c777cdd51329a4d1922fb9e70e8ab7052d1e`).
- M18 is implementation-complete for the currently supported manifest/package/version and named string-resource edits. DEX bytecode editing is not currently supported. The compatibility report is conservative and does not guarantee installation: Android platform/version/ABI/device checks remain necessary, and exact certificate equality may not model every valid signing-certificate rotation lineage. Device acceptance remains pending the consolidated final session.

## 5. Development and Creation Process

This is the required process for building DroidInspect:

## 5. Development and Creation Process

This is the required process for building DroidInspect:

1. **Repository-first:** inspect the latest `main` branch, commit, README, build files, and source before every implementation task.
2. **Feasibility before dependency adoption:** create small, isolated on-device prototypes for risky engines. Do not add a major decompiler/rebuilder dependency until its Android compatibility is demonstrated.
3. **Phase-level delivery:** when the user asks to complete a whole phase, continue through every milestone in that phase in sequence on `main`, using milestone boundaries as internal checkpoints rather than stopping for a routine handoff after each milestone. Do not arbitrarily expand scope beyond the phase. Pause early only when a user decision, missing fixture, safety constraint, or essential device-only validation genuinely blocks further work; explain the blocker and ask whether to defer that check and continue. Keep the twelve-milestone roadmap visible.
4. **Plan exact changes:** list files to add/change, dependencies and exact versions, permissions, compatibility effects, and test cases before coding.
5. **Implement phone-friendly code:** provide complete replacement files with full paths; avoid partial snippets and desktop-only instructions.
6. **Cloud verification:** run the configured host tests and GitHub Actions on the latest `main` source; inspect every job and log, and fix verified failures before handoff. Runner-local build outputs are discarded; never upload or download workflow artifacts. Retain verification evidence in ordinary Actions logs and the job summary.
7. **Source handoff:** provide the repository page, latest `main` commit, and direct `main` ZIP link. Give exact instructions for importing the ZIP or updating/replacing the required files in CodeAssist.
8. **User build and device test:** the user builds in CodeAssist, installs the locally built APK, and runs the agreed test cases. A cloud build alone does not pass functional criteria.
9. **Analyze evidence:** inspect user-provided build output, reports, logs, and results; fix failures directly on `main`, rerun CI, and provide an updated ZIP handoff.
10. **Document and hand off safely:** update this README with status, decisions, changed files, test evidence, limitations, latest `main` commit, exact stopping point, and next action.

No milestone is complete solely because code was written or committed. When delivering a full phase, complete implementation and available automated verification for all milestones in that phase before the normal ZIP handoff. Device-only verification may be collected as a clearly documented user test batch after handoff when it does not block safe implementation of later milestones; never claim device tests passed without user evidence.

## 6. Current Project Status (2026-11-21)

**Current development state: M12–M18 feature implementations are integrated to their documented supported scope, but M14/M15/M16 limitations remain; M19/M20 reliability and final-integration work is still in progress on `main`. M11 production decompilation remains accepted by the user.** Do not repeat M11 acceptance testing. M12–M18 still require the planned consolidated device acceptance; implementation and host/CI evidence must not be confused with device acceptance.

The project remains the existing Java + native Android XML application, package `com.tanvir.droidinspect`, with the 20-milestone roadmap across eight phases. The current source includes separate-copy C# editing/compiler integration, dependency resolution, advanced .NET inspection and comparison, APK analysis and guided editing/signing, persistent APK/.NET project workspaces, bounded source/project indexing, lifecycle protections, offline/backup controls, and an artifact-free dependency audit step. M15 phone-editor usability, M16 human-readable signature comparisons and linked-resource limits, Native AOT trim warnings, and other explicitly documented limitations remain open.

**Current verification gate:** prior runner history recorded 12 baseline host tests and release dependency resolution passing before Android compilation exposed source errors. The MainActivity string literal was corrected in commit `6cd93e1`; missing imports, busy state and Handler scope were corrected in `73fb7c3`. Run #660 (ID `44302103843`) failed before job creation because its verification-summary step was outside the workflow job's `steps` list; the next commit corrected the YAML indentation. The connector returned no jobs or check runs for #660, and no exact parser log was available. Current push-run job IDs/logs are not exposed by the available connector, so no post-fix CI result is verified.

The new M19 cross-instance concurrency regression (160 writes from eight metadata-store instances) and M20 background project-index changes are committed but have not yet been executed by CI. Current host-suite status, Android debug/release compilation, CI signing/tamper checks, native payload verification, and the new NuGet transitive package/license/vulnerability inventory all remain unverified. The dependency audit is added but its actual results and any manual license review are still pending. M19/M20 are not complete and release readiness is not established.

All changes remain on `main`; no alternate development branch is used. The exact current commit and stopping point are recorded in the continuation checkpoint at the top of this README.

### M1 — Foundation and file identification

**Implemented in source:**
- Native Android dashboard and SAF document picker.
- Background inspection using a single worker executor.
- Temporary cache copy; original selected file is not modified.
- 512 MB per-file copy limit and cleanup of temporary copies.
- APK identification by `.apk` extension or ZIP archive containing `AndroidManifest.xml`.
- PE candidate identification by `.dll` / `.exe` extension or MZ signature.
- One-tap runtime/cache diagnostics report.
- No broad external-storage permission.

**Device verification (2026-11-12):** User reports CodeAssist build success and provided the one-tap diagnostics report from Android 11/API 30. Cache writable PASS; SAF document picker available; no broad storage permission requested; ZIP reader and PE reader available; offline inspection confirmed. The user successfully selected an APK and a DLL through the picker, demonstrating the basic SAF intake flow. Error-path and cache-cleanup edge cases remain untested.

### M2 — APK inspection

**Implemented in source:**
- ZIP archive validation and entry listing (maximum 5,000 displayed entries).
- DEX filename detection, including multidex names.
- Android PackageManager extraction when it can parse the APK: package name, version name/code, application label, target SDK, declared permissions, and component counts.
- Clear distinction between archive validity and PackageManager metadata availability.

**Android 11 device test (2026-11-12): PASS for the tested APK.** User inspected `Git MANAGER_1.0.apk` (298,716 bytes): ZIP structure PASS; 51 entries; 9 DEX files (`classes.dex` through `classes9.dex`); binary `AndroidManifest.xml`, resources, and `resources.arsc` present. PackageManager returned package `com.tanvir.gitmanager`, version `1.0` / code `1`, label `Git MANAGER`, target SDK 36, 6 permissions, 13 activities, 1 service, 0 receivers, and 1 provider.

**Known limits:**
- This is one representative APK test, not broad compatibility coverage.
- Minimum SDK is not yet decoded.
- Full binary AndroidManifest.xml decoding is not implemented.
- Resource decoding/editing and DEX decompilation are not implemented.
- PackageManager may not parse malformed, split-only, or unusual APK variants.

### M3 — .NET assembly inspection

**Implemented in source:**
- PE signature, machine architecture, PE32/PE32+, section count, and CLI directory detection.
- CLI header, runtime version, metadata RVA/size, flags, and entry-point token/RVA.
- Metadata root signature/version and stream listing.
- Metadata table row counts for key tables.
- Bounded ECMA-335 metadata reader:
  - Module name and MVID.
  - Assembly name, version, culture, flags, and public-key blob length.
  - Assembly reference names, versions, and cultures.
  - TypeDef namespace/type names.
  - Field and MethodDef names associated with types.
- Original files remain untouched; inspection uses temporary working copies.

**Android 11 device test (2026-11-12): PASS for a CLI-bearing mixed-mode DLL candidate.** User inspected a 71,224-byte `ISymWrapper.dll` candidate. PE signature PASS; x64 / PE32+; 7 sections; CLI header present; runtime string `v4.0.30319`; metadata root and 5 streams parsed; Assembly, Module, TypeDef (52), MethodDef (242), and AssemblyRef (2) rows found. Module/assembly identity, MVID, references, type names, field names, and method names were decoded. Flags and native C++ implementation-detail types suggest a mixed-mode C++/CLI assembly, not a pure-managed C# fixture.

**Known limits and verification gap:**
- Metadata inspection is not C# decompilation, IL editing, or assembly rewriting.
- Display caps: 200 assembly references, 300 types, and 200 members per type.
- Unsupported or malformed metadata tables are reported as incomplete.
- No pure-managed .NET fixture was tested in this device session.
- User declined the optional native PE fixture test. Native/non-managed PE rejection is therefore NOT DEVICE-VERIFIED; do not claim exhaustive M3 classification coverage.

### GitHub Actions build and artifact

Workflow: `.github/workflows/phase1-android.yml`.

Triggers:
- Push to `main`.
- Pull requests targeting `main`.
- Manual `workflow_dispatch`.

Workflow steps:
1. Set up Java 17.
2. Compile and run a host-JVM synthetic ECMA-335 metadata-reader test.
3. Set up Android SDK using only `platform-tools` (the removed legacy `tools` package is not requested).
4. Install Android SDK platform 36 and Build Tools 36.0.0.
5. Set up Gradle 8.13.
6. Run `:app:assembleDebug` from `droidinspect/`.
7. Verify the debug APK exists.
8. Upload `DroidInspect-Phase1-debug` as a 14-day GitHub Actions artifact.

**Latest verified CI run: PASS**
- Workflow: DroidInspect Phase 1 Android CI
- Run number: 26
- Run ID: `43088800159`
- Validation PR head commit: `58947a82af315a98ec84702450b363540b293e73`
- Synthetic ECMA-335 decoder test: PASS.
- Android SDK setup and package installation: PASS.
- Gradle 8.13 setup: PASS.
- `:app:assembleDebug`: PASS.
- APK output verification: PASS.
- Artifact upload: PASS.

Artifact:
- Name: `DroidInspect-Phase1-debug`
- Artifact ID: `13887847693`
- APK size: 34,391 bytes.
- APK SHA-256: `7a85afeb255c1a8407522a73008c3204f1129040bb660d5bede7c82af322479c`
- Artifact ZIP SHA-256: `f13230fb24755ecd0a3bfbe529ed832b3771d3e114be041e9e8628063f5bb208`
- Expires: 2026-11-25 19:37 UTC.

The artifact was downloaded from GitHub Actions and checked locally in the build workspace. The APK was extracted and its ZIP structure passed integrity validation. This proves the cloud build and package integrity, not installation or runtime behavior on the Android 11 phone.

### Earlier CI failures and corrections

The automated tests found and corrected real issues:
- The first synthetic metadata fixture omitted the final AssemblyRef hash-blob index; the fixture was corrected.
- The test then exposed incorrect TypeDef FieldList/MethodList offsets; the parser was corrected.
- Android SDK setup initially requested the removed legacy `tools` package; workflow setup now explicitly requests only `platform-tools`.
- Android Java compilation found a malformed newline character literal in the PE inspection report; it was corrected.
- The latest run (#26) passed all configured checks.

### Verification state

| Check | Status |
|---|---|
| Phase 1 GitHub Actions host parser test | PASS |
| Phase 1 GitHub Actions Android compilation | PASS |
| Debug APK artifact upload and ZIP integrity | PASS |
| User CodeAssist build of latest main source | PASS (user report) |
| Android 11/API 30 diagnostics | PASS for reported runtime checks |
| SAF file selection | PASS — APK and DLL were selected and inspected |
| M1 basic device smoke check | PASS; error-path/cache-cleanup edge cases remain |
| M2 sample APK inspection | PASS — one 51-entry, 9-DEX APK |
| M3 CLI-bearing mixed-mode DLL metadata inspection | PASS — one sample |
| M3 pure-managed .NET fixture | NOT TESTED |
| M3 native/non-managed PE rejection | NOT TESTED — user chose to skip this fixture |
| Phase 1 overall | ACCEPTED TO PROCEED WITH A DOCUMENTED M3 VERIFICATION GAP |
| M4 DEX multidex parsing + class/member detail + class/method search | PASS — Android 11 device (2026-11-12); stress/invalid-input edge cases pending |
| M5 .NET IL browsing and constrained rename | IMPLEMENTED; representative device verification pending |
| M6 APK manifest/resource analysis and selected editing | PASS — Android 11 device (2026-11-12) |
| M7 constrained .NET IL editing | IMPLEMENTED; host/CI PASS (run #101); Android device verification pending |

### M4 — APK DEX analysis and code browsing (IMPLEMENTED; ANDROID 11 DEVICE PASS FOR AGREED CHECKS — 2026-11-12)

**Feasibility decision (2026-11-12):**
- JADX can produce Java-like source, but its maintainers explicitly warn that its library is not suited to Android because of CPU and memory constraints. Its documented Android minimum is API 26, which alone does not prove reliable operation on this API 30 device.
- Google Smali/dexlib2 is a mature DEX library under BSD-3-Clause terms. The inspected AOSP device build metadata for the current dexlib2 module specifies min SDK 33, above this project's Android 11 / API 30 test device. It was therefore not adopted as an unverified runtime dependency.
- Decision: implement a small, read-only, bounded Java DEX structural reader first. No Android runtime dependency was added. Re-evaluate JADX as a separate optional decompilation prototype only after structural browsing is tested on-device.
- Sources reviewed: JADX library-use / Android notes; Google Smali project README and AOSP dexlib2 Android build metadata. This is a feasibility decision, not a claim that every third-party dexlib2 distribution is incompatible.

**Implemented in the current source batch:**
- Detect root-level `classes.dex`, `classes2.dex` through `classesN.dex`, including multidigit names such as `classes10.dex`. Nested asset files are not treated as APK code DEX entries.
- Sort DEX files in numeric order and process them one at a time.
- Validate DEX magic/version (035–040), header size, file size, endian tag, table bounds, indices, and ULEB128 class-data values.
- Decode class descriptors, package names, superclass, class access flags, field names/types/access flags, method names/prototypes/access flags.
- Show per-DEX PASS / REJECTED / SKIPPED results and class counts.
- Add a native XML DEX browser with package selector, class listing, search across package/class/field/method names, and a class detail dialog.
- Keep parsing off the UI thread. APK intake is capped at 512 MB; each extracted DEX is capped at 128 MB; the result is capped at 10,000 classes, 100 displayed fields/methods per class, and 100,000 total indexed members. Each DEX is extracted to a temporary cache file and deleted after parsing.
- A malformed DEX entry is reported separately; successfully parsed earlier DEX entries can still be browsed.
- Original APK is never modified. No broad storage permission or network access is required.
- Structural browsing only: method bodies are not decompiled; Java source reconstruction is NOT implemented.

**New / changed M4 files:**
- Added `droidinspect/app/src/main/java/com/tanvir/droidinspect/DexFileParser.java` — bounded, read-only DEX header/table/class-data parser.
- Added `droidinspect/app/src/main/java/com/tanvir/droidinspect/DexAnalysisActivity.java` — background APK/DEX analysis, search, and class/member navigation.
- Added `droidinspect/app/src/main/res/layout/activity_dex_analysis.xml` — native XML browser UI.
- Updated `droidinspect/app/src/main/java/com/tanvir/droidinspect/MainActivity.java` — launch the DEX browser for the selected document.
- Updated `droidinspect/app/src/main/res/layout/activity_main.xml` — add the DEX browser entry button.
- Updated `droidinspect/app/src/main/AndroidManifest.xml` — register the internal browser activity.
- Added `tools/dex-parser-test/DexFileParserSelfTest.java` — synthetic valid DEX plus malformed and multidex ZIP fixture.
- Updated `.github/workflows/phase1-android.yml` — run both .NET metadata and DEX parser host tests, build the Android app, and upload `DroidInspect-Phase2-M4-debug`.

**Dependencies:** no new Android runtime dependencies. Existing AGP 8.13.0 / Gradle 8.13 / compileSdk 36 / minSdk 26 / targetSdk 36 / Java 8 remain unchanged.

**Initial M4 implementation verification (2026-11-12):** GitHub Actions run #64 (`43092921972`) PASS. Existing ECMA-335 host test PASS; DEX parser host test PASS; Android 36 resource processing and Java compilation PASS; debug APK output verification PASS; artifact upload PASS. Artifact: `DroidInspect-Phase2-M4-debug`, ID `13888828737`, size 44,218 bytes, SHA-256 `5cfbc700f9242ca014483ab62e537b61010a0c8c6dfc1a2d6692163ddd3bb0f3`, expires 2026-11-25 20:04:22 UTC. This initial CI result was followed by a real Android 11 device test and parser correction documented below.

**M4 GitHub Actions result (2026-11-12):**
- Workflow run: #64; run ID `43092921972`; result PASS.
- ECMA-335 metadata-reader host test: PASS.
- DEX parser host test: PASS.
- Android resource processing / Java compilation: PASS.
- Debug APK existence check: PASS.
- Artifact upload: PASS.
- Artifact name: `DroidInspect-Phase2-M4-debug`.
- Artifact ID: `13888828737`; size: 44,218 bytes.
- Artifact SHA-256: `5cfbc700f9242ca014483ab62e537b61010a0c8c6dfc1a2d6692163ddd3bb0f3`.
- Artifact expires: 2026-11-25 20:04:22 UTC.
- Workflow run page: https://github.com/xolomon-coder/DroidInspect/actions/runs/43092921972

**CI corrections made before PASS:**
- First DEX test run caught an incorrectly sized synthetic DEX fixture; header/data sizing and file_size were corrected.
- First Android build caught literal escaped newline sequences in the dashboard XML; corrected.
- Next Android compile caught malformed Java string/character literals in MainActivity; replaced the complete activity with correctly escaped Java literals and made the DEX browser button APK-only.
- The final run #64 passed all configured tests, compilation, output verification, and artifact upload.

**Known technical limits to review during verification:**
- The custom reader is a bounded structural parser, not a full DEX verifier or bytecode disassembler.
- DEX versions 041 and future versions are rejected explicitly.
- Modified UTF-8 strings are decoded using UTF-8; this is sufficient for ordinary ASCII identifiers but may not faithfully represent every unusual encoded string.
- DEX checksum/signature verification and instruction decoding are not implemented.
- Search is in-memory over the capped parsed class/member names.
- The 128 MB per-DEX / 10,000 class / 100 members per class / 100,000 total member caps are deliberate safety limits, not universal DEX format limits.
- Large and obfuscated real-world APKs still require device performance testing.
- Java-like decompilation remains deferred; do not describe this milestone build as a Java decompiler.

### M5 — .NET code browsing, raw IL inspection, and constrained method rename (implemented; core device verification PASS)

**Implemented on `main`:**
- Added a bounded ECMA-335 method-body reader and raw IL disassembler.
- Indexes up to 10,000 MethodDef rows and displays method owner, name, RVA, and decoded method body where available.
- Reads method bodies up to 256 KB and limits display to 250 instructions per method.
- Supports text search across the indexed method/type labels.
- Metadata method rename is deliberately constrained: ASCII identifier, identical UTF-8 byte length, no special-name methods, no shared metadata string, and no strong-name or Authenticode-signed input. The original file is preserved and the exported copy is reopened for verification.
- The current implementation is **not a C# decompiler**. It displays a bounded subset of raw IL; metadata tokens remain numeric and unsupported opcodes are reported/omitted.

**Host verification already present:** `tools/il-reader-test/DotNetIlReaderSelfTest.java` builds a synthetic managed PE/CLI fixture, checks method ownership and IL decoding, verifies a metadata rename round trip, and confirms the source fixture remains unchanged. The newer IL-body editing assertions are described under M7 below. Host verification passed. Android 11 device verification is now complete for the agreed core scope; see the dated M5 device-verification record at the end of this README.

### M6 — APK manifest and resource analysis/editing (COMPLETE; device PASS)

**Dependency:** `io.github.reandroid:ARSCLib:1.4.0` (Apache-2.0), declared in `droidinspect/app/build.gradle.kts`.

**Implemented on `main`:**
- Decodes binary `AndroidManifest.xml` and displays its XML representation plus package/version information.
- Reads the resource table and lists named default string resources within explicit scan/display caps.
- Allows controlled edits to package name, version name, version code, and one selected named default string resource.
- Writes a new APK, removes invalidated source signing material/signing block, reopens the output, and verifies the changed manifest/resource values.
- Exports an **unsigned** APK. It must be signed before installation; the original APK is never overwritten.
- Other resource types and configuration-qualified string variants remain read-only.

**Verification gap:** this feature has been compiled in cloud CI in the preceding development batch, but it has not yet been functionally verified on the Android 11 phone with a real APK. The output-signing/install cycle is also still a user-device test.

### M7 — .NET assembly IL editing (COMPLETE; Android 11 device PASS, 2026-11-12)

**Implemented and cloud-verified on `main`:**
- Bounded same-size method-body byte editor.
- Supports only known single-byte, operand-free IL opcodes; rejects branches, operands, metadata tokens, unsupported opcodes, extra method data sections, oversized edits, and signed assemblies.
- Exports to a separate file, reopens it, and verifies the replacement bytes. The original is preserved.
- This is not a general IL assembler/verifier; structural reopening does not prove runtime semantics or execution correctness.

**Android 11 real-device verification — PASS (2026-11-12):**
- Input: original `test-fixtures/UnsignedRenameFixture.dll`, an unsigned 4,096-byte managed assembly fixture.
- Selected `UnsignedRenameFixture.RenameTarget::AddValues`; original method body was four bytes: `03 04 58 2A`.
- Changed only `58` (`add`) to `59` (`sub`), preserving the four-byte body length, and exported as a new file named `m7 DroidInspect-edited-assembly.dll`.
- Reopened the exported DLL in the .NET IL Browser. It indexed all 4 MethodDef rows and decoded all 4 method bodies. `AddValues` displayed `ldarg.1`, `ldarg.2`, `sub`, `ret`.
- Reopened the original DLL separately. `AddValues` still displayed `ldarg.1`, `ldarg.2`, `add`, `ret`.
- The two independent reopen checks establish that the edited copy contains the requested opcode change and the original remains unchanged.

**Assessment:** M7 is **DEVICE PASS / COMPLETE FOR THE AGREED TEST SCOPE**. This verifies bounded byte editing, separate-file export, exported-file reopening, opcode decoding, and original preservation. It does not verify execution of the edited assembly or general IL validity.

### Dependencies and compatibility

- Android runtime dependency: ARSCLib 1.4.0 (Apache-2.0) for APK binary manifest/resource handling; no .NET runtime bridge or decompiler engine is packaged.
- Existing build configuration: Android Gradle Plugin 8.13.0, Gradle 8.13, compileSdk 36, minSdk 26, targetSdk 36, Java 8 source/target compatibility.
- CI uses Java 17 to run Gradle and host-side tests.
- User development environment for this session: CodeAssist 3.36.1; test device Android 11.
- SAF is used instead of broad storage permissions.

### Important source files

- `droidinspect/app/src/main/java/com/tanvir/droidinspect/MainActivity.java` — dashboard, SAF picker, background execution, diagnostics.
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/FileInspectionEngine.java` — safe temporary copy, APK archive/PackageManager inspection, file-signature detection, PE/CLI inspection.
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetMetadataReader.java` — bounded ECMA-335 metadata reader for module, assembly, references, types, fields, and methods.\n- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DexFileParser.java` — bounded DEX structural parser for multidex, classes, fields, and methods.\n- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DexAnalysisActivity.java` — DEX browser UI/controller and background analysis.
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetIlReader.java` — bounded PE/CLI method-body reader, raw IL disassembler, constrained metadata rename, and same-size IL opcode editing.
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetIlActivity.java` — .NET method/IL browser, search, and constrained edit/export UI.
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkResourceActivity.java` — binary manifest/resource browser and controlled APK edit/export workflow.
- `droidinspect/app/src/main/res/layout/activity_dotnet_il.xml` — .NET IL browser layout.
- `droidinspect/app/src/main/res/layout/activity_apk_resources.xml` — APK manifest/resource editor layout.
- `droidinspect/app/src/main/res/layout/activity_main.xml` — dashboard and report layout.\n- `droidinspect/app/src/main/res/layout/activity_dex_analysis.xml` — DEX class/member browser layout.
- `droidinspect/app/src/main/AndroidManifest.xml` — launcher activity and permissions.
- `droidinspect/app/build.gradle.kts` — existing Android build configuration.
- `.github/workflows/phase1-android.yml` — CI build, host parser test, and debug APK artifact.
- `tools/metadata-reader-test/DotNetMetadataReaderSelfTest.java` — synthetic metadata fixture and host-JVM decoder test.\n- `tools/dex-parser-test/DexFileParserSelfTest.java` — synthetic multidex ZIP and malformed DEX host-JVM regression test covering class, field, and method metadata.
- `tools/il-reader-test/DotNetIlReaderSelfTest.java` — synthetic PE/CLI IL decode, metadata rename, and bounded IL edit round-trip tests.

### Exact Phase 1 implementation batch files

- Added `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetMetadataReader.java`.
- Updated `droidinspect/app/src/main/java/com/tanvir/droidinspect/FileInspectionEngine.java`.
- Added `tools/metadata-reader-test/DotNetMetadataReaderSelfTest.java`.
- Added `.github/workflows/phase1-android.yml`.
- Updated this `README.md`.

No new Android runtime dependency or broad storage permission was introduced.

## 7. Chronological Development History

- 2026-11-12: Inspected the latest `main` branch, README, Java activity, inspection engine, XML layout, manifest, Gradle configuration, and resources.
- 2026-11-12: Confirmed the initial Phase 1 dashboard and APK/PE inspection source had not yet been built or device-tested.
- 2026-11-12: Added the ECMA-335 metadata reader for module identity, MVID, assembly identity, references, TypeDef names, field names, and method names.
- 2026-11-12: Corrected metadata coded-index mappings, TypeDef list offsets, and the synthetic AssemblyRef fixture.
- 2026-11-12: Improved APK and PE identification using file structure as well as filename extensions.
- 2026-11-12: Added and passed the synthetic host-JVM metadata-reader test.
- 2026-11-12: Added GitHub Actions host test, Android debug build, and artifact upload workflow.
- 2026-11-12: Fixed Android SDK setup to avoid the removed legacy `tools` package.
- 2026-11-12: Fixed the malformed PE report newline literal found by the Android Java compiler.
- 2026-11-12: GitHub Actions run #26 passed; debug APK artifact was downloaded and ZIP integrity checked.
- 2026-11-12: User reported successful CodeAssist build and supplied Android 11 diagnostics: cache writable, SAF picker available, no broad storage permission, ZIP/PE readers available, offline inspection supported.
- 2026-11-12: User inspected `Git MANAGER_1.0.apk`: 51 ZIP entries, 9 DEX files, and PackageManager metadata decoded successfully. Recorded as an initial M2 device PASS for this sample.
- 2026-11-12: User inspected a 71,224-byte CLI-bearing `ISymWrapper.dll` candidate: PE/CLI headers and core ECMA-335 metadata parsed, including assembly/module identity, references, types, fields, and methods. Recorded as an initial M3 device PASS for this mixed-mode candidate; not evidence of C# decompilation.
- 2026-11-12: User declined the optional native PE fixture test. Native/non-managed PE rejection and pure-managed fixture coverage remain explicitly unverified.
- 2026-11-12: User chose to proceed to Phase 2 despite the documented M3 verification gap. Do not restart Phase 1 or silently mark the missing native-PE test as passed.
- 2026-11-12: Inspected JADX Android-use guidance and Google Smali/dexlib2 Android build metadata. Deferred JADX because its maintainers warn about Android CPU/RAM limitations; did not adopt dexlib2 because the inspected AOSP device module targets API 33 while the test phone is API 30.
- 2026-11-12: Added a dependency-free bounded DEX structural parser, native XML browser, multidex handling, class/member search, safety caps, and synthetic host-JVM test.
- 2026-11-12: Extended GitHub Actions to run both host parser tests, compile the Android app, verify the APK output, and publish the Phase 2 M4 debug artifact.
- 2026-11-12: CI initially caught a synthetic DEX fixture sizing error, dashboard XML newline escaping, and malformed MainActivity Java literals. All three were corrected; final run #64 passed both host tests, Android resource processing and Java compilation, APK output verification, and artifact upload.
- 2026-11-12: Updated MainActivity so the DEX browser launch button is enabled only for a selected APK. Latest M4 dashboard Java fix commit: `6f67cad16e4dab99cc2e13b7a49dbcd8d058eaa9`.
- 2026-11-12: Final M4 debug artifact produced: `DroidInspect-Phase2-M4-debug`, artifact ID `13888828737`; SHA-256 `5cfbc700f9242ca014483ab62e537b61010a0c8c6dfc1a2d6692163ddd3bb0f3`. User device verification is the remaining gate.
- 2026-11-12: User clarified the mandatory phone-only delivery workflow: use only the `main` branch; assistant performs implementation and CI verification directly on `main`; deliver the latest repository `main` ZIP with exact file update/import instructions; user builds and device-tests in CodeAssist. GitHub Actions APK artifacts are for CI verification and are not the default user download/install route.
- 2026-11-12: User clarified the phase-level work preference: when a whole phase is requested, continue through all milestones in that phase on `main` and complete implementation plus cloud verification before the normal ZIP handoff. Milestone boundaries are internal checkpoints; pause early only for a genuine blocker or essential user decision/device-only check, and explicitly ask whether that check can be deferred.

## 8. New-Chat Continuation Instructions

**Current continuation state — 2026-11-12**

- Repository: https://github.com/xolomon-coder/DroidInspect
- Branch policy: `main` only. All new source, CI, and documentation commits must target `main` directly. Do not create another branch.
- Latest verified M4 status checkpoint before this final CI record: `aecc22da6eab48912a1501cda9fd01682129ca6b`; all project work remains directly on `main`.
- Android package: `com.tanvir.droidinspect`; module: `droidinspect/app`.
- Environment recorded for the current project: Android 11/API 30; CodeAssist 3.36.1; Java + native XML.
- Existing build settings: AGP 8.13.0, Gradle 8.13, compileSdk 36, minSdk 26, targetSdk 36, Java 8 source/target.
- Added Android dependency: ARSCLib 1.4.0 (Apache-2.0).
- Current CI workflow: `.github/workflows/phase1-android.yml`; it runs the .NET metadata test, .NET IL test, DEX parser test, Android debug compilation, APK existence check, and publishes `DroidInspect-Phase2-debug`.
- M7 source includes bounded IL-body editing (4 KB editable-body cap) and its host test. M7 source CI run #101 (`43136882549`) passed. M4 documentation CI run #103 (`43138634224`) also completed successfully for `main` commit `aecc22da6eab48912a1501cda9fd01682129ca6b`; all host tests, Android compilation, APK output verification, artifact upload, and cleanup steps passed. Artifact integrity details are recorded below.
- Normal user delivery remains the latest `main` ZIP: https://github.com/xolomon-coder/DroidInspect/archive/refs/heads/main.zip. CI APK artifacts are verification records, not the default installation route.

**Milestone state**

- **M1:** implementation and initial Android 11 smoke checks accepted; error-path/cache-cleanup edge cases remain.
- **M2:** one real APK inspection passed on-device (`Git MANAGER_1.0.apk`, 51 ZIP entries, 9 DEX files). Broad APK compatibility and full binary manifest decoding were not covered by that test.
- **M3:** one CLI-bearing mixed-mode DLL inspected on-device. Pure-managed fixture and native/non-managed PE rejection remain untested; preserve this documented gap.
- **M4: DEVICE PASS FOR AGREED TESTS (2026-11-12).** Nine-DEX APK parsed 9/9 (310 classes, 2,428 members); `AccountActivity` class details displayed 7 fields and 27 methods; class-name search returned 9 matches; method-name search for `access$0` returned 11 matches including `AccountActivity`. This is structural DEX browsing, not Java decompilation. Malformed-DEX device handling and large-APK stress remain untested.
- **M5: DEVICE PASS FOR AGREED CORE TESTS (2026-11-12).** Assembly inspection, raw IL browsing, method search, safe refusal on a strong-name-signed assembly, successful same-length method rename on an unsigned fixture, export/reopen verification, and original-file preservation all passed on Android 11. C# source decompilation remains unimplemented; additional compatibility/edge-case coverage remains open.
- **M6: COMPLETE / DEVICE PASS (2026-11-12).** The user confirmed on Android 11 that framework attribute names now display readably in manifest XML (for example, `android:name` instead of `android:r0x01010003`). Existing metadata/string editing, unsigned export, and original-APK preservation checks also continue to work. Signing/install and unsupported resource variants remain outside M6.
- **M7:** constrained same-size, operand-free IL byte editing added, with synthetic round-trip and rejection-path host tests. Cloud CI passed in run #101; real assembly/runtime behavior is unverified.
- **M8–M10:** not yet implemented.

**Exact next work**

1. Complete M7 constrained IL editing device verification with a disposable test assembly; preserve the documented restrictions and rejection behavior.
2. M4 and M5 are complete for their agreed Android 11 test scopes. Optional M4 follow-up coverage remains: malformed DEX handling and large-APK responsiveness.
3. After M7 device verification is accepted, proceed to Phase 3 (M8–M10), preserving the ten-milestone roadmap.

**Branch cleanup note**

Five old non-main branches were visible during inspection: `ci/m4-final-build-check`, `ci/phase2-m4-ci-final`, `ci/phase2-m4-validation`, `ci/phase2-m4-validation-final`, and `phase1-ci-validation`. They are stale/diverged from the newer `main` and contain a few branch-only README/test/workflow edits. No branch was deleted or force-merged during this continuation. Future work must remain on `main`; inspect any branch-only changes before the user deletes those old branches.

**Do not**
- Recreate the project or repeat completed Phase 1 implementation.
- Create or switch to another development branch.
- Claim C# or Java source decompilation.
- Describe the narrow IL byte editor as a general-purpose IL assembler/verifier.
- Mark any milestone device-complete from cloud compilation alone.
- Claim the latest CI passed without checking the actual run, every job step, artifact, and ZIP integrity.
- Modify or overwrite original APK/DLL/EXE inputs.

Preserve the phone-only CodeAssist workflow, Java + XML architecture, GitHub as source of truth, complete-file preference, and main-only branch policy.

### M4 — Real-device regression and parser correction (2026-11-12)

**First Android 11 device result: FAIL; correction implemented, device retest pending.**

The user tested the DEX browser against a real APK containing nine root-level DEX files. The browser opened and displayed partial metadata (183 classes and 1,314 members), but only `classes8.dex` passed; the other eight entries were rejected with field-index or method-index out-of-bounds errors. The aggregate class/member counts were therefore partial and must not be treated as a successful parse of the APK.

**Root cause confirmed in DEX class_data decoding:**
- DEX encodes static fields and instance fields as two separate lists. Each list's `field_idx_diff` sequence starts from index zero.
- Direct methods and virtual methods are also separate lists, and each `method_idx_diff` sequence starts from index zero.
- The previous parser incorrectly continued the cumulative index across each list boundary, causing valid member references to exceed the corresponding field/method table bounds.

**Correction committed directly to `main`:**
- Reset the field index when transitioning from static to instance fields.
- Reset the method index when transitioning from direct to virtual methods.
- Make each DEX parse transactional: if a DEX entry fails, remove any classes and member counts added by that entry so partial data cannot leak into the browser.
- Expand the synthetic host regression fixture to include both field lists and both method lists with nonzero first-list indexes, and a malformed second DEX that fails after partial parsing has begun. The test verifies correct index resets, rejection of the malformed DEX, and rollback of its partial results.

**Changed files in the parser-fix batch:**
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DexFileParser.java`
- `tools/dex-parser-test/DexFileParserSelfTest.java`

**Cloud verification after correction: PASS**
- Main commit containing the regression fixture: `4f3d3b42b911c02c8c1ddf0567cdcaad692e7347`.
- GitHub Actions run #92: `43130442795` — completed successfully on `main`.
- ECMA-335 metadata host test: PASS.
- .NET IL reader host test: PASS.
- DEX parser host regression test: PASS (`DexFileParserSelfTest: PASS`).
- Android debug APK compilation: PASS (`BUILD SUCCESSFUL`).
- APK output verification and artifact upload: PASS.
- Artifact: `DroidInspect-Phase2-debug`, ID `13904852545`; artifact ZIP SHA-256 `f0d735c80b27d7adabe9cc716bbe5f4ac246a6e4550ef5eb26f312514a20a032`; expires 2026-11-26 00:53 UTC.
- The downloaded artifact ZIP was independently checked: it contains `app-debug.apk`, and its local SHA-256 matches the GitHub artifact digest.

**Next required action:** user downloads the latest `main` ZIP, builds and installs locally in CodeAssist, then repeats M4 against the same nine-DEX APK. Confirm that all nine DEX entries are reported individually, that valid DEX files pass, and that class/member counts no longer omit valid entries. Capture any remaining per-DEX rejection messages. Do not mark M4 device verification complete until this retest succeeds. M5–M7 device verification also remains pending.

### M4 — Android 11 real-device retest (2026-11-12): DEX parsing PASS

The user rebuilt and tested the corrected main version in CodeAssist against the same APK containing nine root-level DEX files.

**Observed device result:**
- DEX files found: 9.
- DEX files passed: 9/9; rejected: 0.
- Classes parsed: 310.
- Members indexed: 2,428.
- Class and member limits: not reached.
- Per-file counts: classes.dex 76; classes2.dex 56; classes3.dex 34; classes4.dex 31; classes5.dex 24; classes6.dex 58; classes7.dex 15; classes8.dex 9; classes9.dex 7.
- The browser displayed the complete 310-class listing, package names, source DEX file, and per-class field/method counts.

**Assessment:** the original real-world multidex field/method index regression is fixed for this APK on the Android 11 test phone. All nine DEX entries passed and no configured caps were reached. This is strong device evidence for DEX parsing and class indexing. It does not yet verify class-detail navigation, field/method detail rendering, search behavior, malformed-entry handling, or large-APK responsiveness. M4 remains in progress for those remaining browser interaction checks; this is not Java decompilation.

**Next device check:** open a class such as com.tanvir.gitmanager.AccountActivity, verify its field and method lists/details open correctly, then search for a known class or method name and confirm the result filters/navigates correctly. Report any incorrect counts, blank details, crash, or freeze.


### M6 — APK manifest/resource analysis and editing: DEVICE PASS (2026-11-12)

**M6 is now complete for its planned scope.** The user rebuilt and tested the framework-name correction in CodeAssist on Android 11 against `Git MANAGER_1.0.apk`; the readable XML attribute names now work.

**Initial failure and correction**
- Initial device error: `IOException: Can not decode without package`; manifest metadata fields were blank.
- Commit `1613ff4bcd3058e093104209c10c984c32dbb0ed` changed `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkResourceActivity.java` to load the ARSCLib resource table before decoding the Android manifest, providing the required package context.
- GitHub Actions run #95 for that source commit passed all host tests, Android compilation, APK output verification, and artifact upload.

**Successful real-device verification**
- Input copied: 298,716 bytes.
- Manifest decoded from Android binary XML.
- Package: `com.tanvir.gitmanager`.
- Version name: `1.0`.
- Version code: `1`.
- Resource table parsed; one resource package identified: `com.tanvir.gitmanager`.
- Resource entries scanned: 117.
- Named default string resources discovered: `app_name` (`0x7F070000`) and `hello_world` (`0x7F070001`).
- User confirmed the supported editing and export tests work, including original APK preservation.

**M6 scope and safety boundary**
- Supported: binary manifest/resource analysis; package name, version name, version code, and one selected named default string editing; export to a separate unsigned APK.
- Other resource types and configuration variants remain read-only.
- Export is intentionally unsigned; signing and installation are outside M6 and are deferred to a later milestone.
- The original selected APK is preserved and is never overwritten.

**Final assessment: PASS — M6 closed.** Device verification confirms readable Android framework attribute names and preserves the previously tested analysis, supported editing/export workflow, and original APK. This does not claim APK signing, installation, arbitrary resource editing, or complete APK rebuild support.

**Current continuation checkpoint (2026-11-12):**
- Latest source commit at the time of this note: `1613ff4bcd3058e093104209c10c984c32dbb0ed`.
- Latest verified CI run: #95 / `43131718723`, all workflow steps successful; artifact listed above.
- M4 multidex parsing and class indexing are device-verified for the nine-DEX sample (9/9, 310 classes, 2,428 members), but class-detail/member rendering and search interaction checks remain open.
- M6 is complete: the framework attribute-name correction is now confirmed on-device, alongside the earlier editing/export and original-APK preservation checks.
- M5 core real-device checks passed; M7 real-device IL-edit testing remains pending; M8–M10 are not implemented.
- Next: complete M7 device verification. M4 and M5 are complete for their agreed device-test scopes, and M6 is complete. Keep all subsequent work and documentation commits on `main` only.


### M6 — Android framework attribute-name resolution (2026-11-12)

**Issue reported after the earlier M6 editing/export PASS:** the manifest viewer displayed Android framework attributes in the form `android:r0x01010003` rather than readable source names such as `android:name`. The attribute value itself (for example `android.permission.INTERNET`) was decoded, but its framework attribute name was not. This is an M6 manifest XML presentation/decoding defect, not an APK input defect.

**Root cause:** `ApkResourceActivity` loaded the APK resource table but explicitly disabled ARSCLib's default Android framework loading. Without the Android framework resource table, framework attribute IDs could not be resolved to their source names during XML serialization.

**Correction committed directly to main:**
- Load the APK's `resources.arsc` table first, preserving the earlier fix for the package-context decoding error.
- Initialize ARSCLib's Android framework resources before obtaining/serializing the manifest, using automatic framework-version selection: `apkModule.initializeAndroidFramework((Integer) null)`.
- Keep manifest editing, unsigned export, output validation, and original-APK preservation behavior unchanged.
- Source file: `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkResourceActivity.java`.
- Implementation commits: `10309adf9f03eb392626c8044d376c309beb4c72` (framework loading) and `e0dc82647a16042476419008673ed1c4a4a6ccf5` (explicit Integer null cast required to disambiguate the overloaded ARSCLib API).

**CI history and final cloud verification:**
- Run #98 / `43136039196` initially failed at Java compilation because a bare `null` was ambiguous between ARSCLib overloads accepting `Integer` and `XMLDocument`. The compiler output was inspected and the call was corrected to `(Integer) null`.
- Run #99 / `43136201279` completed successfully on `main`.
- Host tests: ECMA-335 metadata decoder PASS; .NET IL reader PASS; DEX parser PASS.
- Android SDK setup and Android 36 package installation PASS; Gradle 8.13 setup PASS; Android debug APK compilation PASS; APK output verification PASS; artifact upload PASS; all post-job cleanup steps PASS.
- Artifact: `DroidInspect-Phase2-debug`, ID `13906953389`, size 3,581,274 bytes, SHA-256 `efec526e2ec496370a158f665e47e1364eb3993356311e972d62d41c54f8405c`, expires 2026-11-26.
- Downloaded artifact ZIP integrity test: PASS; contains `app-debug.apk`; local SHA-256 matches the artifact digest.
- Final source commit at this checkpoint: `e0dc82647a16042476419008673ed1c4a4a6ccf5`.

**Final Android 11 device retest: PASS (2026-11-12).** The user confirmed that the updated build displays Android framework attribute names readably rather than as `android:r0x...`. The user also confirmed that the existing supported editing, unsigned export, and original-APK preservation behavior still works. M6 is closed.


**Final device-verification result (2026-11-12): PASS.** The user tested the build produced from the corrected `main` source in CodeAssist on Android 11. The manifest XML now displays readable Android framework attribute names rather than `android:r0x...`. The user confirmed that the earlier metadata/string editing, unsigned export, and original APK preservation tests continue to work. This satisfies the outstanding M6 acceptance criteria; M6 is now closed.

### M4 — Final Android 11 device verification: PASS (2026-11-12)

The user completed the remaining DEX browser interaction checks on the same `Git MANAGER_1.0.apk` containing nine root-level DEX files.

**Observed results:**
- DEX files found: 9; all 9 passed; none rejected.
- Classes parsed: 310; members indexed: 2,428; class/member limits not reached.
- `com.tanvir.gitmanager.AccountActivity` opened successfully from `classes5.dex`; superclass displayed as `com.tanvir.gitmanager.BaseActivity`.
- Class detail dialog displayed 7 fields and 27 methods, including readable access modifiers, types, and signatures.
- Searching `AccountActivity` filtered the class list to 9 matching classes, including nested classes.
- Searching `access$0` filtered the list to 11 matching classes, including `com.tanvir.gitmanager.AccountActivity`. This confirms search across method names as well as class/package names.

**Final assessment:** M4 is **DEVICE PASS / COMPLETE FOR THE AGREED TEST SCOPE**. The parser and structural browser have been exercised on a real multidex APK on Android 11, including class details and class/method-name search. The feature is a DEX metadata browser; method-body decompilation and reconstructed Java source are not implemented. Large-APK stress testing and malformed-DEX handling on the physical device remain untested and are recorded as optional edge-case coverage, not as passed tests.

**Next milestone verification:** M5 core device verification is PASS; M7 real-device IL-edit testing remains pending. M6 is complete. M8–M10 are not implemented.

### M4 final documentation CI verification — PASS (2026-11-12)

- Main commit tested: `aecc22da6eab48912a1501cda9fd01682129ca6b`.
- GitHub Actions run #103: `43138634224` — completed successfully on `main`.
- Workflow: `DroidInspect Android CI`; job: Host parser tests, Android compile, and debug APK.
- All steps passed: checkout; Java 17 setup; ECMA-335 metadata decoder host test; .NET IL reader host test; DEX parser host test; Android SDK setup; Android 36 SDK package installation; Gradle 8.13 setup; debug APK compilation; APK output existence check; artifact upload; and all post-job cleanup steps.
- Artifact: `DroidInspect-Phase2-debug`, ID `13907774686`, size 3,581,276 bytes, expires 2026-11-26 02:03:39 UTC.
- Artifact SHA-256: `df33bea4dc12d181581c169812a07bb9ba31d4535bfe49bd3a2631564ebf75fd`.
- Downloaded artifact ZIP integrity: PASS (`ZipFile.testzip()` returned no error); contains `app-debug.apk` (3,591,301 bytes), SHA-256 `d78c41c48ac74855ab386e8fc130c9c48ab56dc62205bc3ee17993bb3884f753`.
- Run URL: https://github.com/xolomon-coder/DroidInspect/actions/runs/43138634224
- This artifact is for cloud verification only. The user's normal workflow remains downloading the latest `main` ZIP and building in CodeAssist.

---

## Explicit Feature Backlog — C# Decompilation

**Status: REQUESTED / NOT IMPLEMENTED**

The user explicitly wants a future **View C# / Decompile** capability in DroidInspect.

### Goal
- Add a **View C#** or **Decompile** action for supported .NET assemblies.
- Reconstruct readable C# source from managed .NET method bodies, rather than showing only raw ECMA-335 IL.
- Present decompiled output by namespace, type, and method, with navigation between related members where supported.
- Retain the existing raw IL view as a separate inspection mode; decompiled C# must not replace or be confused with raw IL.

### Feasibility and implementation requirements
- First evaluate a practical Android-compatible decompiler engine/runtime integration for this Java + native XML Android app.
- Investigate ILSpy or another suitable decompiler, including runtime/interop requirements, Android 11 compatibility, licensing, APK size, memory use, and performance on a phone.
- Prototype with representative pure-managed assemblies before selecting an engine.
- Explicitly detect and report unsupported cases, including mixed-mode C++/CLI assemblies and methods without managed IL bodies. Do not promise that every DLL/EXE can be decompiled.
- Never label raw IL or illustrative pseudocode as reconstructed C#.
- Preserve the original assembly; decompilation is read-only and must not modify the input file.

### Relationship to the existing roadmap
This backlog makes the existing M5 C# decompilation goal explicit. The current M5 implementation provides bounded metadata browsing, raw IL viewing, method search, and constrained metadata rename; **it does not currently provide C# decompilation**. The user's successful initial inspection of `ISymWrapper.dll` verifies metadata inspection only, not C# decompilation. Track this capability as outstanding until an Android-compatible engine is selected, implemented, and verified on-device with a supported pure-managed test assembly.

### Verification criteria
1. A supported managed DLL/EXE can be opened without modifying the original.
2. A selected type/method can be displayed as readable reconstructed C#.
3. Decompiled output is distinguishable from raw IL and includes clear unsupported-method diagnostics.
4. Search/navigation works across decompiled types and members where supported.
5. A representative pure-managed assembly is tested on the Android device; mixed-mode and no-IL methods are tested for safe, explicit handling.
6. Engine license, dependency footprint, Android compatibility, memory use, and known limitations are documented.

**Added to the backlog:** 2026-11-12, at the user's request.




## Explicit Feature Backlog — IL Instruction Explanations

**Status: REQUESTED / NOT IMPLEMENTED**

The user requested a human-readable explanation alongside each decoded .NET IL instruction in the IL browser.

### Goal
- Present the raw instruction bytes (hex), decoded IL opcode/mnemonic, and a concise human-readable meaning together.
- Example display:

| Hex byte | IL instruction | Meaning |
|---|---|---|
| `03` | `ldarg.1` | Load the first argument |
| `04` | `ldarg.2` | Load the second argument |
| `58` | `add` | Add the two values |
| `59` | `sub` | Subtract the second value from the first |
| `2A` | `ret` | Return from the method |

### Implementation requirements
- Keep the raw hex bytes and opcode mnemonic visible; the explanation is an additional aid, not a replacement.
- Use a reliable opcode-description table/reference covering supported ECMA-335 IL opcodes.
- Explain operand meaning and stack behavior where it is useful and accurate; do not invent descriptions for unknown or unsupported opcodes.
- Preserve instruction offsets and existing raw IL browsing/editing behavior.
- Keep the presentation readable on a phone screen; consider a compact per-instruction detail or expandable explanation if a three-column table is too wide.
- Clearly distinguish a simple opcode description from full semantic analysis or reconstructed C#.

### Verification criteria
1. Known opcodes such as `ldarg.1`, `ldarg.2`, `add`, `sub`, and `ret` show correct explanations.
2. Hex bytes, offsets, opcode names, and explanations remain aligned to the same instruction.
3. Operand-bearing, prefixed, and unsupported instructions are described safely and accurately, or explicitly marked when no explanation is available.
4. The IL browser remains usable on the Android phone and existing browsing/editing behavior is not regressed.

**Added to the backlog:** 2026-11-12, at the user's request.

### M5 — Android 11 device verification: PASS (2026-11-12)

The user completed the previously pending M5 device checks in DroidInspect on Android 11 / Infinix X689B, using CodeAssist 3.36.1.

**Assembly inspection and raw IL browsing**
- Test input: `ISymWrapper.dll`, 71,224 bytes, a C++/CLI mixed-mode assembly.
- PE signature and .NET CLI header detected; x64 / PE32+; runtime metadata and assembly identity decoded.
- 52 TypeDef rows, 96 fields, 242 MethodDef rows, and 24 properties indexed.
- 242 methods indexed; 231 method bodies decoded.
- Opened `System.Diagnostics.SymbolStore.SymDocument::.ctor`; fat method header and 33 IL bytes displayed with decoded offsets/opcodes/tokens.
- Method search for `get_URL` found and opened `System.Diagnostics.SymbolStore.SymDocument::get_URL`; IL body displayed.
- These results verify metadata/IL inspection of this mixed-mode assembly. They do not establish C# source decompilation.

**Signed-assembly safety check**
- Attempted a same-length rename on the signed `ISymWrapper.dll`.
- The app refused with: `Strong-name signed assemblies cannot be edited by this writer.`
- This is the expected safe refusal and passed the protection check.

**Unsigned rename/export test**
- Added a dedicated unsigned fixture at `test-fixtures/UnsignedRenameFixture.dll`, generated by the repository workflow.
- Fixture workflow run #4 (run ID `43143660891`) completed successfully.
- The workflow built the fixture, verified the strong-name-signed flag was unset and signature directory was empty, published the DLL to `main`, and uploaded the `DroidInspect-Unsigned-DotNet-Test-DLL` artifact.
- Fixture SHA-256: `5cff957a3df17f3011e4a459e22fe8f56a5735b4c1c5da09cc6c5fb5bbb55135`.
- On-device, the user opened the fixture, selected `UnsignedRenameFixture.RenameTarget::GetValue`, renamed `GetValue` to `GetTotal` (same 8 ASCII bytes), and exported to a new file.
- The user reopened the exported DLL and confirmed `GetTotal` appeared in the method browser.
- The user explicitly confirmed that the original input DLL remained unchanged.

**Assessment:** M5's agreed core Android 11 device acceptance tests PASS. The rename is a constrained metadata-string operation, not general .NET refactoring; IL references are not rewritten. C# decompilation remains a separately requested, unimplemented backlog feature. Additional assembly compatibility and edge-case tests are not implied by this result.

**Current stopping point (2026-11-12):**
- M4: device PASS for agreed scope.
- M5: device PASS for agreed core scope.
- M6: device PASS / complete for planned scope.
- M7: implemented and cloud CI-verified; real-device IL-edit test remains pending.
- M8–M10: not implemented.
- Next action: complete M7 device verification. If it passes, begin Phase 3 planning/implementation for M8 while preserving the original ten-milestone roadmap and main-only branch policy.


### M7 — device verification handoff (2026-11-12)

**Current M7 status:** implementation present on `main`; synthetic host round-trip/rejection tests and Android compile passed in Actions run #101 (run ID `43136882549`). The feature is not device-verified yet.

**Exact next action:** download the latest `main` ZIP, build/install in CodeAssist, then follow the M7 instructions in `test-fixtures/README.txt` using the original unsigned `UnsignedRenameFixture.dll`. The expected edit changes only the `AddValues` method body from `03 04 58 2A` (add) to `03 04 59 2A` (sub). Export to a new file, reopen it, verify the new IL, and verify the original still contains the old IL.

**Acceptance criteria:** successful same-size IL edit; verified new export; exported DLL reopens and shows the replacement instruction; original remains unchanged. Report any safety refusal or different original bytes rather than attempting alternative edits.

M4, M5, and M6 remain device-complete for their documented scopes. M7 is the only current Phase 2 device-verification gate. M8–M10 are not implemented. Continue on `main` only.


### M7 — final Android 11 device verification and Phase 2 closure (2026-11-12)

The user supplied two independent .NET IL Browser reports from the same unsigned fixture:
1. Exported file `m7 DroidInspect-edited-assembly.dll`: `AddValues` decoded as `ldarg.1`, `ldarg.2`, `sub`, `ret`.
2. Original `UnsignedRenameFixture.dll`: `AddValues` decoded as `ldarg.1`, `ldarg.2`, `add`, `ret`.

Both files were recognized as `UnsignedRenameFixture`; the browser indexed 4 methods and decoded 4 bodies. The original and exported outputs demonstrate the expected isolated change (`58/add` to `59/sub`) with the original preserved.

**M7 final status: DEVICE PASS / COMPLETE FOR AGREED SCOPE. Phase 2 device verification is now complete for M4–M7.** Remaining M4 stress/malformed-input checks and the separate requested C# decompilation / IL explanation enhancements remain documented as limitations or backlog items, not completed features.

**Next milestone:** begin M8 — APK rebuild and signing. Before implementation, inspect current source/build/CI and evaluate the APK rebuild/signing path against existing ARSCLib integration, original-preservation rules, key handling, and Android 11 device constraints. Keep all changes on `main`; do not start a new branch.

---

## 9. Phase 3 delivery update — authoritative current state (2026-11-12)

This section supersedes earlier continuation notes in this README that said M7 device testing was pending or M8–M10 had not started. Those statements remain in the chronological history as snapshots of the earlier handoff state.

### Current milestone status

- **M1–M3:** complete for documented scope, with the previously recorded native/non-managed PE classification coverage limitation retained.
- **M4:** Android 11 device PASS for the agreed multidex browsing/search scope. Large-APK stress and malformed-DEX device edge cases remain untested.
- **M5:** Android 11 device PASS for the agreed assembly/metadata/raw IL browsing and constrained rename scope. C# source decompilation is not implemented.
- **M6:** Android 11 device PASS for supported manifest/resource inspection, selected edits, separate unsigned export and original preservation.
- **M7:** Android 11 device PASS for the agreed unsigned-fixture same-size IL edit/export/reopen test. Original and exported DLLs were both reopened; original IL remained unchanged.
- **M8:** implementation and CI verification complete; Android 11 key, signing, verification, install and update testing pending.
- **M9:** byte-level IL explanations and MethodDef token display/search implemented; host tests pass. Full C# decompilation is explicitly not implemented. Android 11 presentation/regression testing pending.
- **M10:** integration and CI/release artifact checks pass. Consolidated Android 11 device testing remains the final release gate.

### M8 implementation and security details

- New source: `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkSigningActivity.java`.
- New layout: `droidinspect/app/src/main/res/layout/activity_apk_signing.xml`.
- Main navigation: `MainActivity.java` and `activity_main.xml`; Activity declared non-exported in `AndroidManifest.xml`.
- Dependency: `com.android.tools.build:apksig:8.13.0`, Apache License 2.0. No other runtime signing dependency was added.
- Signing modes: v1, v2 and v3; v4 .idsig sidecar output is not implemented.
- Key options: generate RSA-2048 in Android Keystore or import PKCS#12/PFX and JKS.
- Imported keystore copies are stored temporarily in app-private cache and deleted when the Activity exits; stale signing temporary files are cleaned when the screen starts. Password character arrays are cleared after use and password fields are cleared after signing.
- Generated Android Keystore keys are non-exportable. If the app is uninstalled or its private data is cleared, that generated key may become unavailable. Use a securely backed-up imported keystore for long-term production signing.
- The original APK and original keystore document are never overwritten. Signed output is a distinct file and is only export-enabled after successful signature verification.
- CI test passwords and test keys are ephemeral test-only material; no real private signing key is stored in source control or uploaded to CI artifacts.

### M9 implementation and decompilation decision

- New source: `droidinspect/app/src/main/java/com/tanvir/droidinspect/IlOpcodeExplainer.java`.
- Updated source: `DotNetIlReader.java`; host test: `tools/il-reader-test/DotNetIlReaderSelfTest.java`.
- IL display retains offsets and operands and now shows exact instruction bytes, mnemonic and explanation. Unknown/unsupported opcodes are explicitly marked without guessed semantics.
- Method labels expose the MethodDef token (for example, `0x06000001`) alongside the RVA; the existing method filter can search the token text.
- ILSpy was evaluated conceptually as a .NET decompiler engine, not a Java library. A runtime/AOT/native bridge would add a separate runtime/toolchain and compatibility burden that cannot be validated safely within this Java + XML CodeAssist project. No C# decompiler dependency or misleading “View C#” feature was added. The useful supported alternative is the bounded raw IL browser with explanations and metadata navigation.

### Exact Phase 3 source files changed

- `droidinspect/app/build.gradle.kts`
- `droidinspect/app/src/main/AndroidManifest.xml`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/MainActivity.java`
- `droidinspect/app/src/main/res/layout/activity_main.xml`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkSigningActivity.java`
- `droidinspect/app/src/main/res/layout/activity_apk_signing.xml`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetIlReader.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/IlOpcodeExplainer.java`
- `tools/il-reader-test/DotNetIlReaderSelfTest.java`
- `.github/workflows/phase1-android.yml`
- `README.md`

### Final cloud CI and artifact evidence before this README update

- Workflow: **DroidInspect Android CI**, run **#131**, run ID `43205700204`.
- Tested commit: `754491c612b580a256297122f7aa347f1589ab8d`.
- Result: **success**. All three host test suites passed; Android debug compilation and unsigned release build passed; Android Build Tools signed the release APK; `jarsigner` verified the v1 JAR signature; `apksigner` verified v2 and v3; the modified/tampered APK was rejected; artifact upload passed.
- CI signer: ephemeral test certificate `CN=DroidInspect CI Test`. Certificate SHA-256: `cb72692fbdd0d2b64cdca55f4ddb4efc4eddb8cf9a3ada766214762c3774f187`.
- Artifact: `DroidInspect-Phase3-builds`, ID `13937447401`, 7,312,843 bytes, expires 2026-11-26. Artifact SHA-256: `5f3103eb3b197be731500e835662719bf7a1d87940b4fc76f55e8321b3189bd1`.
- Artifact contains:
  - Debug APK: 3,825,898 bytes; SHA-256 `088722e8de9aa4f58dc6627480b0d6c67b1470014fc67d3e83c872fb722c250a`.
  - CI-signed release APK: 3,508,551 bytes; SHA-256 `ea8ee8fe0458efae55a15fbe5c3e5b5145c10898cfc60f4963c69ce9c0c708db`.
- The downloaded artifact ZIP passed ZIP integrity testing. Both APKs extracted and passed ZIP integrity checks. Artifact metadata points to the same `main` commit used by run #131.
- The CI release APK uses an ephemeral CI key and is for verification only; it is not the user's production signing key and is not the normal CodeAssist installation route.

### Known limitations and device-only verification

- Full C# decompilation is not implemented.
- APK Signature Scheme v4 sidecar output is not implemented.
- AndroidKeyStore-generated private keys are non-exportable; use an imported backed-up keystore for portable long-term signing.
- CI cannot prove Android 11 PackageManager installation/update behavior or real-device keystore provider compatibility. These must be tested on the user's phone.
- Existing M4 large-APK and malformed-DEX edge cases remain untested on the physical device.
- No claim of Phase 3 Android device PASS is made until the user reports the consolidated test results.

### Consolidated Android 11 / CodeAssist 3.36.1 device checklist

1. Download the latest `main` ZIP and import/update the existing project in CodeAssist 3.36.1. Build and install the debug app on Android 11.
2. Open the new **APK Signing and Verification** screen. Select a disposable unsigned APK exported by the existing APK resource editor; confirm the original file remains untouched.
3. Generate an RSA-2048 key with a test alias and owner. Sign the selected APK. Confirm the app reports successful verification and displays certificate subject, issuer, serial, validity and SHA-256 fingerprint.
4. Export the signed APK to a new SAF document. Reopen it in DroidInspect and verify its signature. Keep the original unsigned APK and confirm its size/hash did not change.
5. Import a disposable PKCS#12/PFX keystore and a JKS keystore if available. Test the correct alias/password, a wrong password and an unknown alias. Confirm clear errors and no original keystore overwrite.
6. Make a disposable copy of a signed APK and alter its contents using a file tool. Select the altered copy and verify DroidInspect rejects it.
7. Use a disposable test package to test installation and update behavior: same signing key should preserve update identity; a different signing key should be rejected for an already-installed package. Do not risk overwriting a production app.
8. Open a managed DLL fixture in the .NET browser. Confirm IL lines show offset, exact bytes, mnemonic, operand and meaning; verify `03`, `04`, `58`, `2A` for the known add fixture. Confirm MethodDef tokens appear and can be searched.
9. Exercise an operand-bearing instruction and an unsupported/truncated instruction. Confirm raw bytes/offsets remain accurate and unknown opcodes are not assigned invented meanings.
10. Repeat existing M5/M7 regression checks: assembly/type/method search, raw IL browsing, constrained edit/export/reopen, and original DLL preservation.
11. Reopen the main dashboard and run diagnostics. Confirm no new broad storage permission is requested and the signing Activity remains internal.

### Exact stopping point and next action

- Latest code and workflow commit before the README refresh: `754491c612b580a256297122f7aa347f1589ab8d`.
- All M8–M10 implementation and automated CI tasks are complete for the documented scope.
- This README update must be committed directly to `main`; its resulting commit will trigger one final documentation-only CI run.
- After that run passes, the next exact action is for the user to download the latest `main` ZIP, build/install once in CodeAssist 3.36.1, and complete the consolidated checklist above. Report device evidence for any failure; fix directly on `main`, rerun CI, and issue an updated ZIP only if needed.

---

## 10. M11 approval and continuation update (2026-11-13)

The user approved adding **M11 — Full C# decompilation engine and Android integration** after M10. This is a new major milestone and Phase 4; the roadmap is now **11 milestones across four phases**.

**Current M11 status: FEASIBILITY RESEARCH IN PROGRESS; Android proof of concept not started.** No decompiler dependency, .NET runtime, native bridge, ABI, or production UI has been added. The current app continues to provide metadata inspection, raw IL browsing, opcode explanations, token navigation, and constrained IL editing; it does not yet provide C# source decompilation.

**Key technical direction:**
- Evaluate ILSpy's decompiler engine first, but do not assume its .NET library can be loaded as a normal Java/Android dependency.
- Prove a minimal end-to-end decompilation prototype on the Android 11 device before changing the production app.
- Assess runtime/Native AOT/Mono/native-bridge options, ABI support, reference assemblies, licensing, offline behavior, performance, package size, and CodeAssist/GitHub Actions reproducibility.
- Keep the existing Java + XML UI unless a verified technical requirement makes a change unavoidable.
- Add a real View C# action only after genuine C# reconstruction is demonstrated on-device. Keep View IL separate and preserve all current features.

**Exact next action:** begin M11 feasibility research and compare supported ways to host a real .NET decompiler engine on Android 11. Then design the smallest isolated prototype and its test fixtures. Do not add speculative dependencies to the app or claim M11 complete until the Android device acceptance criteria above pass.

**Repository workflow:** use only main; inspect its latest HEAD before each change. The README update for this approval is the only planned repository change in this step. No feature branch is to be created.

---

## 11. M11 preliminary technical research (2026-11-13)

This is a preliminary feasibility note, not an implementation or Android compatibility claim.

### ILSpy engine findings

- ILSpy identifies ICSharpCode.Decompiler as its standalone decompilation engine; the engine is a class library without UI dependencies and reconstructs C# from assembly metadata and IL.
- The current package documentation marks the engine trim- and Native-AOT-compatible and describes a dedicated AOT test that compares native and managed output. This makes it a strong first engine candidate, but does not prove Android deployment.
- ILSpy is MIT-licensed. Any packaged dependencies and third-party notices still require separate review.
- The decompiler uses an assembly resolver and semantic type system. A phone build will need a deliberate reference-assembly strategy; merely opening the target DLL may not resolve its framework and external dependencies correctly.

### Android hosting findings

- Microsoft's Native AOT platform matrix lists Android architectures, but explicitly marks Android support experimental and notes there is no built-in Java interop.
- Microsoft's Android Bionic Native AOT notes describe a separate, limited native-library route using linux-bionic RIDs and manual JNI. Those notes state that android-* RIDs are not supported for that Bionic publishing mode; this must not be confused with the .NET for Android application runtime route.
- Native AOT shared libraries can expose explicitly exported unmanaged entry points, which could form a narrow JNI boundary. The exact supported Android RID/toolchain combination and runtime dependencies must be proven with a small prototype.
- The current DroidInspect app is a Java/XML Gradle project. Its existing CodeAssist workflow does not automatically provide the .NET SDK, Native AOT compiler, Android NDK integration, or managed-to-Java binding generation. CI may need to build the native component separately and package ABI-specific libraries, but the result must remain maintainable and reproducible.

### Current feasibility assessment

ILSpy plus a narrow native/JNI bridge is the leading candidate to prototype. It is not yet selected as the production architecture. The highest-risk questions are: (1) can the exact decompiler engine be published for the Android 11 phone's ARM64 ABI with all required runtime pieces; (2) can Java call it reliably through JNI; (3) can framework and third-party reference assemblies be resolved offline; and (4) can the build be reproduced in CI without making the user's CodeAssist project unmanageable.

### Primary technical references

- ILSpy decompiler architecture: https://github.com/icsharpcode/ILSpy/blob/master/doc/DecompilerArchitecture.html
- ICSharpCode.Decompiler package and Native AOT notes: https://github.com/icsharpcode/ILSpy/blob/master/ICSharpCode.Decompiler/PackageReadme.md
- ILSpy license: https://github.com/icsharpcode/ILSpy/blob/master/LICENSE
- Microsoft Native AOT platform support and limitations: https://learn.microsoft.com/en-us/dotnet/core/deploying/native-aot/
- Android Bionic Native AOT details: https://github.com/dotnet/runtime/blob/main/src/coreclr/nativeaot/docs/android-bionic.md
- Native AOT shared-library exports: https://learn.microsoft.com/en-us/dotnet/core/deploying/native-aot/libraries
- .NET for Android Java interop: https://learn.microsoft.com/en-us/dotnet/maui/android/internals/java-interop

**Next technical experiment:** create a minimal native decompiler library that accepts a path to a managed fixture and returns decompiled C# text through a stable C ABI/JNI call. Build it for the Android ARM64 target using a supported toolchain, package it into a disposable Java Android test app, and execute it on Android 11. Do not touch production DroidInspect dependencies or UI until this experiment succeeds.

### Refined M11 prototype route (2026-11-13)

Newer official .NET documentation clarifies two distinct Android Native AOT paths:

1. **Bionic shared-library route — preferred first prototype.** The .NET Native AOT Android Bionic guide explicitly supports `linux-bionic-arm64` shared libraries and says Java/Kotlin can call them through manually implemented JNI. It documents Android API 24 as the minimum in .NET 11, so the Android 11/API 30 test phone satisfies the documented OS floor. The route is intentionally a native-library integration, not a full .NET Android application runtime. The guide also notes an OpenSSL packaging requirement if the used code path requires OS crypto.
2. **.NET for Android app/runtime route — fallback candidate.** Current .NET 11 documentation says Android apps use CoreCLR by default and lists `android-arm64` support; Native AOT is experimental. This is a separate project/runtime integration model and may require more architectural changes to embed into the existing Java/Gradle app.

The first experiment should therefore target a small `linux-bionic-arm64` Native AOT shared library containing the ILSpy engine and a narrow C ABI, with a tiny NDK-built JNI shim calling that ABI from a separate Java/XML test APK. Build the .NET library and test APK in GitHub Actions; do not require the user to install the .NET SDK or NDK on the phone. The Android test APK should let the user select a managed DLL through SAF and display the actual decompiled C# text.

**Known technical caveats to test rather than assume:**
- Native AOT has no dynamic managed assembly loading. ILSpy reads the target PE/CLI file through its metadata/decompiler pipeline rather than requiring the target assembly to execute, but this behavior must be confirmed in the AOT prototype.
- The decompiler needs reference assemblies for the target framework and referenced libraries. The package documentation explicitly notes that a Native AOT host without an installed .NET runtime must supply a `UniversalAssemblyResolver` with reference directories. The prototype must bundle and configure at least the reference pack for its test fixture; broader framework reference coverage is a later acceptance task.
- The `ICSharpCode.Decompiler` package is AOT annotated and has a dedicated native-vs-managed output test, but that test is not an Android Bionic/JNI device test.
- The Bionic shared library and JNI bridge must be built for `arm64-v8a`, packaged under the Android ABI library directory, loaded successfully on Android 11, and tested with a real fixture before production integration.

**Primary references for this refined route:**
- .NET Native AOT with Android Bionic (RIDs, NDK, JNI, API floor, shared-library use): https://github.com/dotnet/runtime/blob/main/src/coreclr/nativeaot/docs/android-bionic.md
- .NET for Android runtime scope and supported Android ABIs: https://learn.microsoft.com/en-us/dotnet/android/building-apps/build-properties
- ILSpy package AOT and reference-resolver notes: https://github.com/icsharpcode/ILSpy/blob/master/ICSharpCode.Decompiler/PackageReadme.md
- Native AOT library exports: https://learn.microsoft.com/en-us/dotnet/core/deploying/native-aot/interop
- JNI shared-library example referenced by the .NET Android Bionic guide: https://github.com/josephmoresena/NativeAOT-AndroidHelloJniLib

**Exact next action:** implement the isolated Native AOT + JNI test APK and its CI build. First prove that the native library compiles and exports its C ABI; then prove the JNI host loads and calls it on Android 11; finally add ILSpy and fixture/reference-pack decompilation. These are explicit sub-gates, not assumptions. Production DroidInspect remains unchanged until all device gates pass.



---

## 12. M11 isolated ILSpy Native AOT proof — implementation and CI verification (2026-11-13)

**Status: ISOLATED PROTOTYPE BUILDS AND PASSES CLOUD VERIFICATION; ANDROID 11 DEVICE ACCEPTANCE IS PENDING.** This is not production integration and does not mark M11 complete.

### Isolated implementation

All M11 prototype code is separate from the production Android application under `tools/m11-poc/`.

- `engine/DroidInspect.M11.Engine.csproj`: .NET 11 Native AOT shared-library project, targeting `linux-bionic-arm64`.
- `engine/DecompilationService.cs`: calls the real ILSpy `CSharpDecompiler`, supplies `UniversalAssemblyResolver`, and adds the target assembly directory and explicit reference-assembly directory.
- `engine/NativeExports.cs`: exports `DroidInspect_DecompileFile` and `DroidInspect_FreeResult` through a narrow C ABI. Exceptions are converted to UTF-8 error text and do not cross the native boundary.
- `fixture/`: managed .NET 11 DLL fixture with methods, properties, loops, conditionals, exception handling, generic methods, nested generic types, and multiple members.
- `host-tests/`: managed host-side ILSpy test. It verifies actual reconstructed C# constructs and rejects output that looks like raw IL.
- `android-host/`: separate Java/XML Android application using SAF, a private working copy, JNI, a background worker, and a source-text display.
- `android-host/app/src/main/cpp/`: NDK-built JNI shim. It converts UTF-8 result bytes into a Java UTF-8 string and releases the native result buffer.
- `.github/workflows/m11-nativeaot-poc.yml`: independent CI pipeline for fixture compilation, managed decompilation, Bionic ARM64 Native AOT, native-symbol/ELF verification, JNI compilation, APK build, APK inspection, and artifact upload.

The prototype packages the .NET 11 reference assemblies into the test APK and configures ILSpy to search them. The production DroidInspect application, its dependencies, UI, and package remain unchanged.

### Verified CI results

- Corrected M11 workflow: **M11 ILSpy Native AOT Android Proof**, run **#3**, run ID `43312315948`, conclusion **success**.
- Run commit: `e10335745a7628dc913fd13ff8c0289e91e5738f`.
- Managed fixture compilation: PASS.
- Host-side ILSpy C# reconstruction acceptance test: PASS.
- Native AOT publish for `linux-bionic-arm64`: PASS.
- ELF architecture: ELF64 / AArch64; Android Bionic API 24 target.
- Native exports: `DroidInspect_DecompileFile` and `DroidInspect_FreeResult`: PASS.
- Engine SONAME: `libdroidinspect_engine.so`: PASS.
- JNI shared library: ARM64; dynamic dependency resolves through the engine SONAME, not an absolute CI-machine path: PASS.
- Isolated Java/XML Android APK compilation: PASS.
- APK ZIP integrity and required ARM64 native libraries: PASS.
- Offline .NET reference assembly payload: 292 assets; `System.Runtime.dll` presence verified.
- Artifact upload: PASS.

**Artifact evidence**

- Artifact: `DroidInspect-M11-Android-Proof`, ID `13986682069`.
- Artifact SHA-256: `631b5cbe0c4b533a1e0991cde4c0c4f28c8b90d8ad034a487d713a41fd9a1af4`.
- APK size: 20,606,994 bytes.
- APK SHA-256: `e605d498a25e0f0228245ab1f2047aebc2243f2d9b25597aaa80c7d07db7f121`.
- Native AOT engine size: 45,943,968 bytes; SHA-256 `b26967415938118f719e19619f0cc1e98e403eced6cc3cb05eff88db00b72543`.
- Fixture DLL size: 5,632 bytes; SHA-256 `f4f6784d3dcde476e27234bf43f049e77dbfb7fd28d64957407a1c4bcebd426d`.
- Artifact expiry shown by GitHub: 2026-11-26.

### CI corrections recorded chronologically

1. M11 run #1 failed because the workflow looked for the .NET reference pack under the NuGet cache rather than the installed SDK packs directory. The workflow was corrected to use `$DOTNET_ROOT/packs/Microsoft.NETCore.App.Ref/11.0.0/ref/net11.0`.
2. M11 run #2 passed compilation and packaging, but a post-build ELF audit found that the JNI library recorded an absolute path to the engine in `DT_NEEDED`. That APK was not accepted for device testing.
3. The engine now declares `libdroidinspect_engine.so` as its SONAME; CMake declares the imported SONAME; CI checks the engine SONAME and rejects absolute JNI dependency paths.
4. M11 run #3 passed the complete workflow with those loader checks enabled. The resulting artifact is the current diagnostic test build.

### Production regression status

- The existing production workflow **DroidInspect Android CI**, run **#155**, run ID `43312315970`, completed successfully on commit `e10335745a7628dc913fd13ff8c0289e91e5738f`.
- Production CI runs #136–#155 completed successfully. These verify the existing production host tests and Android build; they do not prove M11 device compatibility.
- The M11 test APK is a separate diagnostic application with application ID `com.tanvir.m11poc`. It is not the production package `com.tanvir.droidinspect`.

### Android device acceptance — not yet performed

The cloud build cannot prove that Android 11 loads both shared libraries, that JNI calls the Native AOT export correctly, or that ILSpy completes decompilation on the phone. The next action is to install the isolated M11 proof APK on the Android 11 / ARM64 device, turn off internet access, select the supplied `DecompilerFixture.dll` through the document picker, and verify that the displayed output contains genuine C# declarations and method bodies (including `Calculator.Add`, loops, exception handling, `Echo<T>`, and `Nested<U>`). Then test at least one other real managed DLL/EXE and confirm the selected original file remains unchanged.

Report the exact status/error text or a screenshot if the app fails to load the native engine, crashes, cannot resolve framework types, or returns an error. Do not integrate the engine into production until these device tests pass.

### Exact current stopping point and next action

- Main branch remains the only branch.
- Prototype and CI implementation are committed directly to `main`; no production source was changed.
- Documentation commit: `7afaa99ae403055528293dad2121dced5c2d62af`. The M11 implementation/CI evidence is from code commit `e10335745a7628dc913fd13ff8c0289e91e5738f`; README is now updated after M11 run #3 and production CI #155 passed.
- Next exact action: user installs and tests the isolated M11 proof APK and fixture on Android 11. If successful, continue with broader managed-assembly/reference coverage and performance testing before planning production integration. If it fails, diagnose the device logs and fix the isolated prototype first.


### Latest M11 repeat run on main

After the loader fix, the workflow was run again against commit `2cea5b5c4c9d47cfa2f5bacff1d4dd7e9722bf0e`.

- M11 workflow run **#4**, run ID `43312342183`: **success**.
- Artifact ID: `13985553840`; size 26,839,252 bytes; artifact SHA-256 `05657b6ad3ff5ab269c121b1a7cc7d910a09b89c5c38f041c6a8a6c0827d9ab1`.
- APK SHA-256: `e9760c51ae5c1bff937e716d344c11a2839ce0b266b6a709a37fbc04c1afd219`.
- Fixture DLL SHA-256: `5411a57b4121a2e95e809be9da179308a8da8979d94c8f7357c005f140b9ad88`.
- Engine SHA-256 remains `b26967415938118f719e19619f0cc1e98e403eced6cc3cb05eff88db00b72543`.
- The APK and ELF dependency audit was repeated: engine SONAME is `libdroidinspect_engine.so`; JNI `DT_NEEDED` refers to that SONAME and Android system libraries only; no absolute build-machine dependency path is present.
- The APK archive integrity test passed again.

The latest production regression run after the README progress update, **DroidInspect Android CI #157** (run ID `43312849428`), also completed successfully. This is cloud build/regression evidence only; Android 11 device acceptance remains pending.


---

## 13. M11 Android 11 device acceptance — offline fixture PASS (2026-11-13)

**Status: ISOLATED M11 PROTOTYPE PASSED ITS FIRST REAL ANDROID DEVICE DECOMPILATION TEST OFFLINE. M11 IS NOT COMPLETE; REAL-WORLD ASSEMBLY COVERAGE AND BROADER COMPATIBILITY TESTING REMAIN.**

The user installed the isolated M11 proof APK on the Android 11 / ARM64 test phone and reported the following:

- App startup: PASS.
- Native engine status displayed exactly: `Native engine loaded. Offline decompilation ready.`
- Native AOT ARM64 library loading: PASS on the test device.
- JNI initialization: PASS on the test device.
- Network state during decompilation: **Wi-Fi and mobile data OFF**.
- Selected input: `DecompilerFixture.dll`.
- Decompilation result: ILSpy returned reconstructed C# source text.
- Original fixture output includes assembly attributes and namespace `DroidInspect.M11.Fixtures`.
- Reconstructed declarations and language constructs observed:
  - `Calculator`, property `Value`, and `Add(int left, int right)`.
  - `SumTo(int limit)` with a `for` loop and conditional.
  - `Describe(int value)` with `try/catch`, an exception, and string interpolation.
  - Generic method `Echo<T>(T value)`.
  - Generic outer class `Container<T>` and nested generic class `Nested<U>`.
  - `FixtureEntry.Multiply(int a, int b)`.
- This is genuine C# reconstruction, not the production app's raw ECMA-335 IL browser output.

### M11 verification gates

| Gate | Result |
|---|---|
| Isolated APK builds in GitHub Actions | PASS (M11 workflow run #4) |
| Android app installs and starts | PASS |
| ARM64 Native AOT engine loads on Android 11 | PASS |
| JNI bridge initializes on Android 11 | PASS |
| Fixture decompiles with network disabled | PASS |
| Fixture C# constructs are reconstructed | PASS |
| Additional real-world managed DLL/EXE coverage | PENDING |
| Reference-resolution coverage across real assemblies | PENDING |
| Large-assembly performance and memory behavior | PENDING |
| Original-input preservation confirmed by a separate integrity comparison | NOT YET VERIFIED |
| Production integration and production regression acceptance | NOT STARTED |

### Exact stopping point

The isolated M11 proof APK has now passed the first offline device decompilation test. No production application integration has been performed. The last known main-branch HEAD before recording this device result was `ef26650405c0dba8e98296d67890911ca64afb3f`.

### Next exact action

Continue M11 on the existing `main` branch. Inspect the isolated prototype and test a second, independently obtained real managed DLL or EXE, preferably one with external framework/library references and substantially more types/methods than the small fixture. Verify successful C# reconstruction, document missing-reference behavior, measure practical runtime/memory behavior, and compare the selected original file's hash before and after processing. Fix any isolated-engine issues and repeat the complete M11 CI workflow plus production regression workflow when source changes. Do not integrate the engine into `com.tanvir.droidinspect` or declare M11 complete until the remaining gates are addressed.


---

## 14. M11 second assembly — Newtonsoft.Json host acceptance and Android test build (2026-11-13)

**Status: SECOND INDEPENDENT MANAGED ASSEMBLY PASSES HOST-SIDE C# RECONSTRUCTION; UPDATED ISOLATED ANDROID TEST APK BUILDS AND PASSES PACKAGE/ELF VERIFICATION. THE SECOND ASSEMBLY HAS NOT YET BEEN TESTED ON THE ANDROID DEVICE. M11 REMAINS IN PROGRESS.**

### Real-world assembly selected

The second assembly is **Newtonsoft.Json 13.0.3**, obtained from the official NuGet flat-container package:

- Package: `Newtonsoft.Json` 13.0.3.
- Assembly target: `lib/netstandard2.0/Newtonsoft.Json.dll`.
- Assembly size: 705,296 bytes.
- SHA-256: `8c1dd5c184b4e2e7ead06971ff3ebcb46783be972292d1deb1061744369b4d80`.
- The assembly is independent of the DroidInspect fixture project and is not compiled by this repository's fixture build.

### Host-side ILSpy result

The M11 workflow now runs two separate acceptance programs:

1. `tools/m11-poc/host-tests/HostTests.csproj` continues to verify the original small fixture.
2. `tools/m11-poc/real-assembly-tests/RealAssemblyTests.csproj` decompiles Newtonsoft.Json and checks for reconstructed C# symbols including `JsonConvert`, `SerializeObject`, `JsonSerializer`, `JsonReader`, and `JsonWriter`.

Results on the GitHub runner:

- Original fixture test: PASS.
- Newtonsoft.Json C# reconstruction: PASS.
- Verified symbols: all five expected symbols present.
- Reconstructed output: 1,182,022 characters.
- Host decompilation time: 21,339 ms.

This confirms real third-party assembly reconstruction on the host using the .NET 11 reference assembly directory. It does not yet prove Android device performance or all external dependency combinations.

### Updated Android proof application

The isolated Java/XML proof application remains under `tools/m11-poc/android-host/` and retains its existing arbitrary DLL/EXE picker.

It now additionally provides:

- **Test bundled Newtonsoft.Json 13.0.3** — runs the exact independently obtained DLL packaged in the APK; no separate file extraction is required.
- **Select another managed DLL / EXE** — retains the custom SAF selection path.
- Input size and decompilation duration in the status report.
- Java heap change measurement (explicitly excludes Native AOT/native memory).
- SHA-256 before and after decompilation.
- Original-file unchanged result for a selected SAF document, based on comparison with the reopened original document.

The bundled test is reproducible with network access disabled after APK installation. The assembly and .NET reference pack are embedded in the APK.

### M11 workflow #12 — complete PASS

- Workflow: **M11 ILSpy Native AOT Android Proof**.
- Run: **#12**.
- Run ID: `43320056080`.
- Commit tested: `d8bd908d9a5afdb0b621842c29ed07ce6d2a4ecc`.
- Result: **SUCCESS**.
- Run URL: https://github.com/xolomon-coder/DroidInspect/actions/runs/43320056080

All required steps passed:

- Original fixture build and reconstruction test.
- Download and integrity check of the official Newtonsoft.Json 13.0.3 package assembly.
- Real-world assembly C# reconstruction test.
- Native AOT `linux-bionic-arm64` publish.
- ELF architecture, exported symbols, and engine SONAME checks.
- JNI dependency and absolute-path checks.
- Android proof APK build.
- APK ZIP integrity and required ARM64 libraries/reference/test assembly checks.
- Artifact upload.

### Diagnostic artifact

- Artifact: `DroidInspect-M11-Android-Proof`.
- Artifact ID: `13989813195`.
- Artifact ZIP size: 27,401,749 bytes.
- Artifact ZIP SHA-256: `3e16071360d0a85d2fa241513be22c3d95f006c0a7586ddff55e35fcc679de48`.
- Expiry: 2026-11-26.
- APK size: approximately 20 MiB.
- APK SHA-256: `366c54f67ba61bcad341231b61200385d97cba462ac2d83efdcb5809755010df`.
- Bundled Newtonsoft.Json DLL SHA-256: `8c1dd5c184b4e2e7ead06971ff3ebcb46783be972292d1deb1061744369b4d80`.
- Native engine SHA-256 remains `b26967415938118f719e19619f0cc1e98e403eced6cc3cb05eff88db00b72543`.

### CI harness correction

The initial test-harness wiring needed correction across three attempts:

- M11 run #8 passed the small fixture DLL to assertions that expected Newtonsoft.Json symbols, so the test failed with a missing Newtonsoft namespace.
- M11 runs #10 and #11 used the restored fixture assertions but still passed the Newtonsoft.Json DLL to that fixture test, so they failed with a missing `Calculator` declaration.
- These were test-command/acceptance-harness mismatches, not ILSpy decompilation defects.

The original fixture assertions were restored unchanged. A separate `real-assembly-tests` project and program were added, and the workflow now runs the two test programs against their correct inputs independently. M11 run #12 passed both.

### Production regression

- Workflow: **DroidInspect Android CI**.
- Run: **#168**.
- Run ID: `43320055735`.
- Commit tested: `d8bd908d9a5afdb0b621842c29ed07ce6d2a4ecc`.
- Result: **SUCCESS**.
- Run URL: https://github.com/xolomon-coder/DroidInspect/actions/runs/43320055735

No production application source or package integration was performed. M11 remains isolated.

### Updated M11 verification gates

| Gate | Result |
|---|---|
| Original fixture host reconstruction | PASS |
| Independent Newtonsoft.Json host reconstruction | PASS |
| Native AOT ARM64 cloud build | PASS |
| JNI/ELF/SONAME verification | PASS |
| Updated proof APK build and package verification | PASS |
| Production regression CI | PASS |
| First fixture offline Android test | PASS |
| Newtonsoft.Json offline Android device test | PENDING |
| Android real-assembly performance and memory | PENDING |
| Additional assembly with third-party library dependencies | PENDING |
| Missing-reference behavior | PENDING |
| Original selected-file SHA-256 device comparison | PENDING |
| Large assembly test | PENDING |
| Malformed/unsupported assembly device tests | PENDING |
| Production integration | NOT STARTED |

### Exact next action

This run-#12 instruction is superseded by the corrected diagnostic build and next action in section 15 below. Do not use the older APK when performing the next Android device test.

Do not declare M11 complete or integrate it into `com.tanvir.droidinspect` yet.

The initial source implementation commit for this update was `d8bd908d9a5afdb0b621842c29ed07ce6d2a4ecc`; the corrected device-test APK and status report are documented in section 15.


---

## 15. M11 updated diagnostic APK — integrity-report correction (2026-11-13)

**Status: UPDATED ISOLATED ANDROID PROOF APK BUILDS AND PASSES ALL M11 CI CHECKS. READY FOR THE SECOND OFFLINE ANDROID DEVICE TEST. NEWTONSOFT.JSON DEVICE ACCEPTANCE IS STILL PENDING.**

A small diagnostic-report correction was made before device testing:

- When a user selects an assembly through SAF, the status line is `Original document unchanged`; the hash copied from the original is compared with the reopened original after decompilation.
- When the bundled Newtonsoft test runs, the status line is `Bundled working-copy unchanged`; this correctly describes the temporary working copy rather than implying that an external original document was selected.
- SHA-256 before/after values, input size, decompilation time, and Java heap change remain visible. Java heap change does not include native AOT memory.

### Final M11 workflow #14 — PASS

- Workflow: **M11 ILSpy Native AOT Android Proof**.
- Run: **#14**.
- Run ID: `43320888768`.
- Commit: `a10f1e207612845dd41f87b575b35acd695abe4a`.
- Result: **SUCCESS**.
- URL: https://github.com/xolomon-coder/DroidInspect/actions/runs/43320888768

All steps passed, including both host-side C# reconstruction tests, Native AOT Bionic ARM64 publish, ELF/export/SONAME verification, JNI linking checks, Android APK compilation, APK payload integrity, and artifact upload.

The real-world host test again reconstructed 1,182,022 characters from Newtonsoft.Json 13.0.3. This run measured 12,960 ms on the GitHub runner; this is not an Android device timing.

### Latest diagnostic artifact

- Artifact: `DroidInspect-M11-Android-Proof`.
- Artifact ID: `13991320684`.
- Artifact ZIP size: 27,401,763 bytes.
- Artifact ZIP SHA-256: `d9cfcf6af29c935bb0b03dd53ccb0e68fd51c76f91718d67964dbdfe3ca01665`.
- Expiry: 2026-11-26.
- APK SHA-256: `c38d98792fd5a8b7caae9e60909778d951002795e215e83c2b461339ac68afce`.
- Bundled Newtonsoft.Json DLL SHA-256: `8c1dd5c184b4e2e7ead06971ff3ebcb46783be972292d1deb1061744369b4d80`.
- Native engine SHA-256: `b26967415938118f719e19619f0cc1e98e403eced6cc3cb05eff88db00b72543`.

Artifact download page:
https://github.com/xolomon-coder/DroidInspect/actions/runs/43320888768/artifacts/13991320684

### Production regression

- Workflow: **DroidInspect Android CI**.
- Run: **#173**.
- Run ID: `43320888399`.
- Commit: `a10f1e207612845dd41f87b575b35acd695abe4a`.
- Result: **SUCCESS**.
- URL: https://github.com/xolomon-coder/DroidInspect/actions/runs/43320888399

No production application code or package integration was changed.

### Exact next device test

1. Download the `DroidInspect-M11-Android-Proof` artifact from the run #14 link above and extract the ZIP.
2. Uninstall the older isolated proof app `com.tanvir.m11poc` before installing the new APK. This avoids an update-signature mismatch between separately generated CI debug builds. The production app `com.tanvir.droidinspect` is not involved.
3. Install `DroidInspect-M11-Android-Proof.apk`.
4. Turn **Wi-Fi and mobile data OFF**.
5. Open the proof app and tap **Test bundled Newtonsoft.Json 13.0.3**.
6. Wait for the operation to finish. The reconstructed C# output is approximately 1.18 million characters on the host baseline, much larger than the original fixture.
7. Confirm the source includes `JsonConvert`, `SerializeObject`, `JsonSerializer`, `JsonReader`, and `JsonWriter`.
8. Report the status text: input size, elapsed milliseconds, Java heap change, SHA-256 before/after, and `Bundled working-copy unchanged: PASS` (or the actual failure).

This will establish whether the second assembly can be decompiled by the Native AOT engine on the Android 11 device while offline, and provide the first real-device timing/Java-heap observation for a larger library.

After this test, continue with another independently obtained assembly that has third-party library references, then missing-reference behavior, larger assemblies, malformed/unsupported inputs, and additional original-file integrity checks. Production integration remains blocked until these broader gates pass.

The latest M11 source implementation commit is `a10f1e207612845dd41f87b575b35acd695abe4a`. The current README update will be committed separately on `main`.


---

## 16. Newtonsoft.Json 13.0.3 — Android device result (2026-11-13)

**Status: ANDROID DEVICE DECOMPILATION RETURNED C# SOURCE; BUNDLED INPUT INTEGRITY CHECK PASSED.**

The user tested the workflow #14 diagnostic APK on the Android 11 device. The app reported:

- Assembly: Newtonsoft.Json 13.0.3 (bundled test).
- Input size: 705,296 bytes.
- Android device decompilation time: 19,042 ms.
- Java heap change: 3,484,336 bytes (native memory excluded).
- SHA-256 before: `8c1dd5c184b4e2e7ead06971ff3ebcb46783be972292d1deb1061744369b4d80`.
- SHA-256 after: identical.
- Bundled working-copy unchanged: **PASS**.
- UI status: **DECOMPILATION RETURNED SOURCE**.
- The supplied output excerpt begins with reconstructed C# using directives.

This is the first recorded Android-device timing and Java-heap observation for the larger independent Newtonsoft.Json assembly. It is a device measurement and must not be conflated with the separate GitHub-host timing. Java heap change excludes Native AOT/native memory and is not a total process-memory measurement.

The supplied screenshot transcription does not include the later declarations or confirm all five expected symbols (`JsonConvert`, `SerializeObject`, `JsonSerializer`, `JsonReader`, `JsonWriter`). Those symbol checks remain to be confirmed from the full output. The report transcription also does not independently state whether Wi-Fi and mobile data were disabled during this run; offline status must not be promoted to PASS until confirmed.

### Updated M11 acceptance gates

| Gate | Result |
|---|---|
| Original fixture host reconstruction | PASS |
| Independent Newtonsoft.Json host reconstruction | PASS |
| Native AOT ARM64 cloud build and APK packaging | PASS |
| Production regression CI | PASS |
| First fixture Android decompilation | PASS |
| Newtonsoft.Json Android source returned | PASS |
| Newtonsoft.Json bundled working-copy SHA-256 unchanged | PASS |
| Newtonsoft.Json Android device timing recorded | PASS — 19,042 ms |
| Newtonsoft.Json Android expected-symbol checks | PENDING full-output confirmation |
| Explicit offline condition for Newtonsoft.Json device run | PENDING user confirmation |
| Java heap measurement | RECORDED — 3,484,336 bytes; native memory excluded |
| Total native/process memory measurement | NOT MEASURED |
| Additional assembly with third-party references | PENDING |
| External and missing-reference behavior | PENDING |
| Large assembly and stability tests | PENDING |
| Malformed/unsupported assembly handling | PENDING |
| Original SAF-selected document preservation | PENDING |
| Production integration | NOT STARTED |

### Exact next action

Confirm that Wi-Fi and mobile data were both disabled during this test, and inspect/search the complete reconstructed output for the five expected symbols listed above. Also report whether the output can be scrolled through and whether the app remained responsive during the approximately 19-second operation.

Then continue with the next independently obtained managed assembly that has third-party library references. Do not integrate M11 into the production package yet.

This device result supersedes the previous statement in section 15 that the Newtonsoft.Json Android test was pending. It does not supersede any uncompleted acceptance gates above.


### Offline-condition confirmation (2026-11-13)

The user explicitly confirmed that both Wi-Fi and mobile data were disabled during the Newtonsoft.Json 13.0.3 Android device test. Therefore the Newtonsoft.Json offline Android decompilation gate is **PASS**, not pending.

Updated acceptance gates:

| Gate | Result |
|---|---|
| Newtonsoft.Json Android C# source returned | PASS |
| Newtonsoft.Json Android offline operation | PASS — user confirmed Wi-Fi and mobile data OFF |
| Bundled working-copy SHA-256 unchanged | PASS |
| Android device timing | 19,042 ms |
| Java heap change | 3,484,336 bytes; native memory excluded |
| All five expected symbols confirmed in full output | PENDING |
| Output scrolling and UI responsiveness | PENDING |

The supplied output excerpt only shows the opening using directives; do not claim the five named symbols were individually checked on-device until the full output is inspected or the user confirms them.


## 17. M11 — third-party dependency-resolution test (prepared 2026-11-13)

**Purpose:** verify ILSpy C# reconstruction for an independent NuGet assembly whose metadata references separate third-party libraries, then observe behavior when one required dependency is intentionally unavailable.

### Test assembly and packages

- Target: `Serilog.Extensions.Logging` **9.0.2**, using its `.NET Standard 2.0` assembly.
- Package source: official NuGet package.
- Target package dependencies include `Microsoft.Extensions.Logging` (>= 9.0.0) and `Serilog` (>= 4.2.0). See the NuGet package metadata.
- The diagnostic host project uses PackageReference so NuGet restores the complete dependency closure. The APK bundles the target DLL separately from its resolved dependency DLLs.
- License: Serilog.Extensions.Logging is Apache-2.0.

### New isolated proof-app actions

1. **Test dependencies available** — decompile the target with the bundled .NET 11 reference pack and third-party dependency directory.
2. **Test with Serilog.dll missing** — repeat with the Serilog dependency deliberately omitted while retaining the other dependencies.
3. The app reports source/error, elapsed time, Java heap change, and SHA-256 integrity for the working copy. Both tests run locally and do not require network access.

### Host verification

The new `tools/m11-poc/dependency-assembly-tests/` project verifies C# symbols including `SerilogLoggerProvider`, `SerilogLogger`, `SerilogLoggerFactoryExtensions`, `AddSerilog`, and `ILoggerProvider` when dependencies are present. It also records the missing-Serilog outcome without assuming in advance whether ILSpy returns partial C# or a clear error.

### Status

- Implementation committed to `main`; cloud workflow verification and Android device acceptance are pending.
- No production application code or package integration has been changed.
- The full dependency test and missing-reference test must both be run offline on the Android device before the corresponding M11 gates can be marked complete.


### M11 dependency APK packaging CI correction (2026-11-13)

The first dependency-test workflow run verified:
- Host C# reconstruction with external dependencies: PASS (27,685 output characters; 3,805 ms cloud-host timing).
- Expected symbols: `SerilogLoggerProvider`, `SerilogLogger`, `SerilogLoggerFactoryExtensions`, `AddSerilog`, and `ILoggerProvider`: PASS.
- Missing `Serilog.dll`: ILSpy returned C# source (31,511 characters), rather than throwing. This is an observed outcome, not a failure by itself.
- ARM64 Native AOT ELF, exported JNI entry points, and SONAME checks: PASS.
- Production Android regression workflow: PASS.

The M11 workflow then failed in APK asset preparation because its shell validation incorrectly expected `Microsoft.Extensions.Logging.Abstractions.dll`. The actual resolved NuGet output contains `Microsoft.Extensions.Logging.dll`, `Microsoft.Extensions.DependencyInjection.dll`, and `Serilog.dll`; the host log confirms these names. The workflow now validates the actual dependency filenames. This is a packaging assertion correction; the decompiler and host dependency test had already passed.

The corrected M11 workflow must complete before a new APK can be distributed for Android device acceptance.


### M11 dependency APK verification correction — follow-up (2026-11-13)

The corrected workflow run #16 (`43329300463`) still failed in **Verify APK payload**, after the APK had compiled successfully. Inspection of the exact job log showed that the APK contained these dependency assets:

- `assets/dependency-references/Serilog.dll`
- `assets/dependency-references/Microsoft.Extensions.Logging.dll`
- `assets/dependency-references/Microsoft.Extensions.DependencyInjection.dll`

The preparation step had already been corrected to require the latter two files, but the later APK-content validation step still searched for the obsolete `Microsoft.Extensions.Logging.Abstractions.dll` path. That stale grep caused the failure. This is a CI packaging-validation mismatch; it does not indicate a Native AOT, JNI, APK compilation, or ILSpy decompilation failure.

The workflow validation now checks the two dependency DLLs that are actually packaged. The production Android regression workflow #178 (run ID `43329300453`) completed successfully on the same commit. A fresh M11 run is required to verify this correction; no new APK is accepted until it passes all payload checks and artifact upload.


### M11 Android proof APK CI verification — PASS (2026-11-13)

M11 Native AOT workflow run #17 (`43331172627`) completed successfully on commit `9c50411cae1d194a6c00f4adfc7710ec21ea89ce`. Every job step passed, including the managed fixture test, independent real-assembly reconstruction test, third-party dependency-resolution tests, Native AOT Bionic ARM64 publish, ELF/export checks, offline asset preparation, Java/XML Android APK build, APK payload verification, and artifact upload. The production Android regression workflow run #179 (`43331172654`) also completed successfully; all parser, host-JVM, Android compile, signing, verification, and tamper-test steps passed.

The M11 artifact is `DroidInspect-M11-Android-Proof.zip` (artifact ID `13995705995`), 27,562,412 bytes, uploaded successfully. GitHub artifact SHA-256: `e4db8754e09eb63e2096eb52589fc8eb254106ddf54adb7c2ab0195cc832ebb9`. Artifact expiry: 2026-11-26.

The extracted Android proof APK is 21,063,673 bytes. Its independently checked SHA-256 is `39a65775b1232e991d5afc9d1a1aa5216d7197ab3219d2cc1345a4ead5dda0b7`; ZIP integrity check passed. The APK contains both ARM64 libraries (`libdroidinspect_engine.so` and `libdroidinspect_jni.so`), .NET 11 reference assemblies, the 705,296-byte Newtonsoft.Json test assembly, Serilog.Extensions.Logging, and all three dependency assemblies: `Serilog.dll`, `Microsoft.Extensions.Logging.dll`, and `Microsoft.Extensions.DependencyInjection.dll`. ELF inspection confirmed the engine is ELF64 AArch64, its SONAME is `libdroidinspect_engine.so`, and JNI links to that SONAME without an absolute DT_NEEDED path. The Native AOT engine extracted from the artifact is 45,943,408 bytes with SHA-256 `fb532cecfd57c22587bbd0fd7492106fdd9ac0187e58fcd7b89ebb85b3bc7ff4`.

**Acceptance boundary:** This is successful cloud CI and APK/package verification, not Android-device acceptance. The APK has not yet been verified by the user on-device. Offline operation, Android-side dependency resolution for Serilog.Extensions.Logging, full output correctness/scrolling, UI responsiveness, and phone memory/performance remain to be tested on the device. M11 remains isolated and is not integrated into the production app.

Exact next action: install the M11 proof APK on the Android phone, disable Wi-Fi and mobile data, run the bundled Newtonsoft.Json and Serilog.Extensions.Logging tests, and report the displayed output, timing, memory change, and any errors. Keep the APK and engine SHA-256 values above as the test artifact identity.


## 18. M11 continued implementation — robustness and coverage batch (historical snapshot, 2026-11-13)

**Historical note:** This section records the state at its original checkpoint. Its pending items and stopping point were superseded by the completed work and verification documented in section 19.

**Latest starting point inspected:** `main` HEAD `0511c36a08f32c2bc9219ab12211d68d799f65c1`. The complete current README, M11 workflow, engine, native export boundary, JNI bridge, Android host, fixture, and existing real/dependency test projects were inspected before this batch. The production application remains untouched.

### Findings from source inspection

- The Native AOT + ILSpy pipeline is a genuine C# decompiler path; it is not an IL-to-C# handwritten translator.
- The previous service passed a path directly to ILSpy and relied on broad exception conversion at the native export. It did not explicitly validate PE/CLI structure before decompilation.
- Existing fixture coverage did not include enough interfaces, inheritance, events, structs, enums, attributes, lambdas, LINQ, async methods, or iterators.
- Existing CI covered fixture, Newtonsoft.Json, Serilog dependency-present/missing cases, ARM64 exports, JNI linking, offline assets, and APK packaging, but did not have a dedicated malformed-input/repeated-call test suite.
- Dependency-present and missing-dependency behavior must remain separate outcomes. Source output with an unresolved dependency is not proof that the dependency was resolved.

### Implemented in this batch

Commits were made directly on `main`:

- `d37fc16f6e2745ece839986de07c43042e0ce60f` — explicit managed PE/CLI validation and clearer invalid/corrupt/truncated input diagnostics in `DecompilationService`.
- `e3b42d56f055b233a6f513e67527c9196286e1f9` — safer Native AOT export pointer handling and error-result allocation.
- `56c2321ec55172ff592f5c0701804710eaf97db2` — expanded C# fixture language coverage.
- `fd26acc4e9e2d403b88cb37671c7e23c7c0b8338` — expanded fixture source assertions.
- `16885ef766fd4092264493d9ebdde0564371e2af` and `bf746a5403738a25635a8a3d0abb9c581caa403d` — added isolated invalid-input and repeated-decompilation test project.
- `c4b0576f9805329266bb71b805d753eac7bf9057` — added iterator coverage to the fixture.
- `803855c6fd47a8bc77adc3628dadbcb74454be9e` — added the robustness suite to M11 GitHub Actions.

The validation layer now checks that the input exists, is non-empty, contains a PE metadata directory and CLR header, has a valid PE header, and represents an assembly rather than a netmodule before invoking ILSpy. It emits explicit diagnostics for common unsupported or invalid inputs. ILSpy's existing resolver still searches the input directory and configured offline reference directories.

### Automated verification status for this batch

The updated workflow is expected to run on the new `main` commits. Until the resulting GitHub Actions run is inspected, the new fixture and robustness assertions are **implemented but not yet verified**. The pre-existing successful M11 run #17 and production regression run #179 apply to the earlier commit only; they do not validate this batch.

The robustness suite currently covers missing path, empty file, text/non-PE input, truncated PE input, malformed executable-like input, and three repeated successful fixture decompilations. It does not yet establish exhaustive malformed-metadata safety, native memory peak usage, cancellation, or a process-wide memory ceiling.

### Remaining M11 work

1. Inspect and fix the new CI run, including any API/compile/assertion failures.
2. Expand real-world assembly coverage beyond Newtonsoft.Json and Serilog where useful and legally redistributable.
3. Add explicit dependency-resolution diagnostics rather than treating returned source as proof that every reference resolved.
4. Review Android result rendering and large-output navigation; current UI uses a single TextView and needs a verified large-source usability assessment.
5. Review JNI exception/pending-exception paths and ensure native result memory is freed under all conversion outcomes.
6. Verify repeated and invalid-input behavior through the Android bridge where practical.
7. Run the production Android regression workflow after the final M11 changes.
8. Build and independently inspect one final isolated APK, then provide one consolidated CodeAssist/device test batch.

### Acceptance status

| Gate | Current status |
|---|---|
| Existing fixture C# reconstruction | Previously passed |
| Expanded C# fixture assertions | Implemented; new CI pending |
| Newtonsoft.Json host reconstruction | Previously passed |
| Newtonsoft.Json offline Android decompilation | Passed as previously documented |
| Serilog dependency-present host reconstruction | Previously passed |
| Serilog missing-dependency host outcome | Previously observed; resolution not proven |
| Explicit PE/CLI input validation | Implemented; new CI pending |
| Invalid-input/repeatability suite | Implemented; new CI pending |
| Native AOT ARM64 for latest source | Pending latest CI |
| JNI/native memory/error-path verification | Partial; review pending |
| Android APK build/payload for latest source | Pending latest CI |
| Android large-output navigation and responsiveness | Not verified |
| Serilog tests on Android offline | Not verified |
| Additional external DLL/EXE Android tests | Not verified |
| Full Android-device acceptance | Pending |
| Production integration | Not started; intentionally isolated |

**Historical stopping point at this checkpoint:** the first expanded robustness workflow had been added; subsequent commits, test corrections, real-world assembly coverage, native/JNI hardening, APK packaging, and CodeAssist payload publication are documented in section 19.

## 19. M11 implementation completion, automated acceptance, and CodeAssist delivery (2026-11-13)

### Current milestone status

**M11 implementation complete. Automated host tests, Native AOT ARM64 verification, Android APK build, and APK payload verification have passed. Android-device acceptance of the latest build remains pending.** M11 remains isolated under tools/m11-poc/; no production application source was modified and no production integration has been attempted.

### Implemented work

- Added explicit PE/CLI validation before ILSpy: input existence, empty files, PE metadata, CLR header, PE header, and assembly-vs-netmodule validation.
- Added understandable errors for non-managed, truncated, corrupted, and malformed inputs.
- Added dependency preflight diagnostics that compare assembly name, version, culture, and public-key token across configured search directories. Missing or mismatched references are listed in C# comments at the beginning of returned source. Returning source with an unresolved reference is not reported as successful dependency resolution.
- Hardened Native AOT exports and JNI conversion/error paths. Native result memory is freed after Java conversion, including conversion-failure paths.
- Expanded the C# fixture to cover interfaces, inheritance, events, fields/properties, overloads, constructors, generics, nested generics, lambdas, LINQ, async/await, iterators, exceptions, attributes, enums, nullable/reference metadata, structs, and control flow.
- Added managed executable-entry-point input coverage. The CI test supplies the managed PE assembly with an .exe extension; this does not claim testing a native Windows executable.
- Added real-world host tests for Newtonsoft.Json 13.0.3, the .NET 11 System.Text.Json runtime assembly, and Serilog.Extensions.Logging 9.0.2 with dependencies present and intentionally missing.
- Added invalid-input and repeatability cases: missing path, empty file, text/non-PE input, truncated managed PE, corrupted CLR metadata, malformed executable-like input, native ELF input, and three repeated successful fixture decompilations.
- Reworked the Android proof UI to show source in 24,000-character pages with Previous/Next controls and source-wide Find. The full output remains searchable in memory.
- Added first-use offline extraction of the bundled .NET reference DLLs and third-party reference DLLs. Bundled and selected assemblies use those offline reference directories.
- Added Java heap delta and process PSS before/after plus a 200 ms sampled PSS peak during decompilation. PSS is a process-wide approximation, not an exact native-allocation counter or guaranteed instantaneous peak.
- Kept decompilation on a background worker so the Android UI thread is not blocked by the synchronous Native AOT call.

### Real-world automated test results

The final M11 workflow verified:

| Test | Verified result |
|---|---|
| Expanded C# fixture | Expected constructs and symbols reconstructed as C# |
| Managed EXE input | Entry-point assembly reconstructed; expected symbols present |
| Newtonsoft.Json 13.0.3 | 705,296-byte official NuGet assembly; 1,182,379 output characters; expected symbols JsonConvert, SerializeObject, JsonSerializer, JsonReader, JsonWriter |
| System.Text.Json runtime assembly | 2,506,064-byte .NET 11 runtime assembly; 1,942,420 output characters; expected JSON API symbols present |
| Serilog.Extensions.Logging 9.0.2 | 29,567 output characters; expected provider/logger/extension symbols present with dependencies supplied |
| Missing Serilog.dll | Source still returned (33,457 characters) and the missing Serilog reference was explicitly diagnosed |
| Invalid/corrupt inputs | All configured robustness assertions passed |
| Repeatability | Three repeated fixture decompilations passed |
| Native input rejection | Built native ELF was rejected as a non-managed input |
| Native AOT | Bionic ARM64 ELF verified; required exports and SONAME checked |
| JNI | JNI library linked against libdroidinspect_engine.so; no absolute DT_NEEDED path |
| Android proof APK | Gradle build, ZIP integrity, ARM64 libraries, offline references, and bundled dependency/test assets verified |

Host decompilation timings are CI-host measurements, not Android performance promises. Earlier Android-device evidence remains limited to the previously documented fixture and offline Newtonsoft.Json test; the latest UI, PSS display, Serilog scenario, and source paging have not yet been accepted on-device.

### Final CI verification

- M11 workflow run #55, ID 43337520812, result SUCCESS, source commit 392137ad907fd5a2a144353ab67fd6404df341e1: full host tests, Native AOT ARM64 publish, native-input rejection, ELF/export/SONAME checks, Android APK build, and APK payload verification.
- Production Android regression workflow #219, ID 43337520745, result SUCCESS, same source commit.
- M11 workflow run #54, ID 43337084718, result SUCCESS, commit 5c68d952873b37b07abddb480d0d3f02cdac18d2: independently rebuilt the Android APK from the committed CodeAssist payload.
- M11 workflow run #53, ID 43336682293, result SUCCESS: generated and committed the CodeAssist payload.

The latest CI APK from run #55 has SHA-256 8b12de6faceec6ee71d5e350f92c5fdd6e258baae98d4de2c2b31b241a6329b6. It is a diagnostic artifact, not the normal delivery format.

### CodeAssist-ready payload committed to main

Payload commit: fcf77f4e7ad88c3badcdbd5d16d1c2c3b0817e2b.

The repository now includes 182 generated files totaling 53,490,176 bytes:

- ARM64 Native AOT engine: 45,966,680 bytes.
- .NET 11 reference assemblies: 176 DLLs.
- Third-party dependency DLLs: Serilog.dll, Microsoft.Extensions.Logging.dll, and Microsoft.Extensions.DependencyInjection.dll.
- Bundled test assemblies: Newtonsoft.Json.dll and Serilog.Extensions.Logging.dll.

This makes the repository ZIP self-contained for the isolated Android proof build. No .NET SDK or Native AOT compiler is required on the phone; the ARM64 engine is prebuilt. CodeAssist still needs to build the Java/JNI Android host using the Android SDK, NDK, and CMake versions below.

### CodeAssist build instructions

1. Download and extract the latest main ZIP.
2. In CodeAssist, open the Gradle project directory DroidInspect/tools/m11-poc/android-host (the directory containing settings.gradle), not the repository root.
3. Use Gradle 8.13 and Android Gradle Plugin 8.13.0.
4. Ensure Android SDK Platform 36, Build Tools 36.0.0, NDK 30.0.16248370, and CMake 3.31.6 are installed/available to the project.
5. Build the app debug variant from CodeAssist. The resulting APK is tools/m11-poc/android-host/app/build/outputs/apk/debug/app-debug.apk.
6. Install the APK on an ARM64 Android device (package com.tanvir.m11poc, minSdk 26). No additional .NET runtime installation is needed.

The initial Gradle/plugin sync may need internet access. Once installed, the bundled decompilation scenarios are designed to run offline.

### Consolidated final Android-device test

Do not test intermediate APKs. Use one APK built from the latest ZIP and complete this batch once:

1. **Install/startup:** Install the CodeAssist-built APK and launch “DroidInspect M11 Proof”. Confirm the native engine loads without an error.
2. **Offline check:** Disable Wi-Fi and mobile data before running the bundled scenarios.
3. **Newtonsoft.Json:** Run the bundled Newtonsoft.Json 13.0.3 test. Confirm C# source appears; use Find for JsonConvert, SerializeObject, JsonSerializer, JsonReader, and JsonWriter.
4. **Large-output navigation:** Use Find, Next, and Previous across the source pages. Confirm the app remains responsive and the displayed page changes correctly.
5. **Serilog with dependencies:** Run “Test dependencies available”. Find SerilogLoggerProvider, SerilogLogger, SerilogLoggerFactoryExtensions, AddSerilog, and ILoggerProvider. Check the dependency-warning status.
6. **Serilog with a missing dependency:** Run “Test with Serilog.dll missing”. Confirm that the output clearly reports the missing Serilog reference. Source may still be returned; that must not be mistaken for complete dependency resolution.
7. **External assembly:** Select another genuine managed .NET DLL or managed EXE through the document picker. Confirm source output, expected symbols, and navigation. Native PE/ELF executables are unsupported.
8. **Error handling:** Select an empty, text, or native/non-.NET file. Confirm a readable error appears and the app remains usable.
9. **Stability:** Repeat the Newtonsoft test three times. Watch for crashes, frozen UI, or stale/overwritten output.
10. **Performance/integrity:** Record input size, decompilation time, Java heap delta, process PSS before/peak/after, source character count, and the unchanged-input/hash result shown by the app.

### Acceptance gates

| Gate | Status |
|---|---|
| M11 implementation | Complete |
| Automated host tests | Passed |
| Real-world DLL reconstruction | Passed for tested assemblies and expected symbols |
| Managed EXE-entrypoint reconstruction | Passed |
| Dependency-present/missing host scenarios | Passed, with explicit missing-reference diagnostic |
| Invalid/corrupt/native input robustness | Passed for configured cases |
| Native AOT ARM64 output | Verified |
| JNI exports/linkage/SONAME | Verified |
| Android APK build | Passed |
| APK payload/ZIP integrity | Passed |
| CodeAssist-ready repository payload | Committed and independently rebuilt in CI |
| Latest Android-device UI/Serilog/paging/PSS acceptance | Pending user test |
| Full M11 milestone acceptance | Pending consolidated Android-device test |
| Production integration | Not started; intentionally deferred until M11 device acceptance |

**Current next action:** download the latest main ZIP, build the isolated tools/m11-poc/android-host project in CodeAssist, and perform the single consolidated device test above. Do not integrate M11 into the production application until those device results have been reviewed.


### User CodeAssist build confirmation (2026-11-13)

The user downloaded the latest `main` ZIP, opened `tools/m11-poc/android-host` in CodeAssist, and reported: **Build ok.** This confirms the extracted Gradle project imports and the debug APK compiles in the user's CodeAssist environment. It does not yet confirm APK installation, app startup, offline operation, decompilation correctness on-device, source paging/Find, Serilog dependency scenarios, error handling, repeatability, or memory/performance. Those remain part of the single consolidated Android-device acceptance test.

**Updated stopping point:** CodeAssist debug build passed. Next action is to install the locally built `com.tanvir.m11poc` APK and perform the consolidated offline device test in section 19. Do not ask the user to rebuild unless a source change is made. Do not integrate M11 into production before reviewing device results.


## 20. M11 Android startup failure — CodeAssist JNI packaging correction (2026-11-13)

The user installed and launched the CodeAssist-built M11 APK. The app reported:

`java.lang.UnsatisfiedLinkError: dlopen failed: library "libdroidinspect_jni.so" not found`

The installed APK successfully found the prebuilt `libdroidinspect_engine.so`, but the separate JNI bridge library was absent. The Java bridge loads both libraries. Cloud Gradle builds had generated and packaged the JNI target through CMake, while the phone-side CodeAssist build did not package that CMake-produced library into the APK. This is an IDE/build integration gap, not evidence of a Native AOT decompilation failure.

### Correction committed to main

- Removed the M11 host app's dependency on Gradle externalNativeBuild/CMake for local packaging.
- Configured the Android app to package prebuilt ARM64 libraries directly from `app/src/main/jniLibs`.
- Updated the M11 workflow to compile `jni_bridge.cpp` with the Android NDK ARM64 clang++ toolchain, link it against `libdroidinspect_engine.so`, verify its SONAME dependency, and publish `libdroidinspect_jni.so` beside the engine in `jniLibs/arm64-v8a`.
- Extended the generated-payload commit step to include the JNI bridge library.
- The production app and its source remain unchanged.

### Verification and next action

The source correction is committed, but the generated JNI binary and new APK must pass the M11 workflow's APK payload checks and production Android regression CI before delivery. Do not ask the user to rebuild from the old ZIP. After the workflow publishes the JNI binary to `main`, provide the new `main` ZIP and ask the user to replace the extracted M11 proof project, build once in CodeAssist, install, and confirm native engine startup. Then resume the existing consolidated offline acceptance batch.

**Current device acceptance:** installation completed; startup failed because the JNI bridge .so was missing. Newtonsoft.Json, Serilog, paging, external assembly, invalid-input, repeatability, and performance tests have not yet run on this installed build. Full M11 acceptance remains pending.


## 21. M11 offline Android result and search layout correction (2026-11-14)

### Device result confirmed

The user confirmed that the latest M11 proof app starts and reports: “Native engine loaded. Offline decompilation ready.” The user then ran the bundled Newtonsoft.Json 13.0.3 test offline. It returned C# source with dependency diagnostics:

- Input: 705,296 bytes.
- Decompilation time: 20,258 ms.
- Output: 1,182,379 characters across 50 pages.
- Java heap change: 3,584,000 bytes.
- Process PSS before: 21,039,104 bytes; sampled peak and after: 130,262,016 bytes.
- SHA-256 before and after matched: 8c1dd5c184b4e2e7ead06971ff3ebcb46783be972292d1deb1061744369b4d80.
- Working-copy unchanged: PASS.

This confirms on-device offline source reconstruction for this assembly and input integrity. Find, page navigation, Serilog dependency-present/missing tests, external managed assembly selection, invalid-input handling, repeatability, and full UI responsiveness remain unverified.

### Search field layout correction

The user reported that the Find symbol or source text field was pushed to the bottom of the screen and barely visible. The layout correction is committed on main:

- Search and Previous/Next controls appear before the diagnostic status report.
- Search field height is 48dp with padding.
- Diagnostic status is limited to five lines with end ellipsis.
- Activity uses adjustResize when the keyboard opens.
- Native engine, decompilation logic, and Java search implementation remain unchanged.

Source commits:
- 9438a80ac03dd5df95c745956b93c953bc8132f9 — search field layout correction.
- 830a4b8b922689037e26d83cf86ec3e60a1974c8 — keyboard resize behavior.
- ff6a6d4fa9682d91a61ac4817a10da18baedb14d — Android acceptance log.

### CI result and exact next action

- M11 workflow #61 passed: https://github.com/xolomon-coder/DroidInspect/actions/runs/43492105166
- Production Android CI #229 passed: https://github.com/xolomon-coder/DroidInspect/actions/runs/43492242520
- Latest checked main commit at this record: 9ea33576ed28fb1b44876be3e5029a443255e588.

**Next:** provide the latest main ZIP. The user should replace the extracted M11 proof project, open `DroidInspect/tools/m11-poc/android-host` in CodeAssist, build and install once, and verify the search field is visible and usable with the keyboard open. After that, continue the remaining consolidated device acceptance tests. Do not repeat the already-passed Newtonsoft.Json offline decompilation test unless needed to investigate a regression. M11 remains isolated; production integration has not started.


## 22. M12 approved scope — C# Source Editing & DLL Recompilation (2026-11-14)

The user approved M12 as the next planned milestone. This approval records the scope only; **no M12 implementation has started and no source-editing or DLL-recompilation feature is claimed to exist**.

The intended user workflow is:

1. Decompile an original managed DLL/EXE using the accepted M11 engine.
2. Keep the original input and original reconstructed source unchanged.
3. Choose Edit Source to create a separate editable copy.
4. Save the edited reconstruction as separate .cs file(s).
5. Compile the edited source using a proven compatible offline .NET compiler/toolchain and the correct references.
6. Export the compiled result as a new DLL, for example Newtonsoft.Json_edited.dll.
7. Reopen the new DLL in DroidInspect and verify its managed metadata and expected changes.

A .cs file cannot be renamed into a DLL; an actual compatible compiler and dependency/reference resolution are required. Decompilation does not guarantee that all source can be recompiled, because reconstructed source may omit or imperfectly reproduce original information and compiler-generated details. Therefore, a successfully rebuilt DLL is not automatically a drop-in replacement and is not guaranteed to be byte-for-byte or behaviorally identical to the original.

M12 must report three independent success levels: **(1) Compilation** — a new managed DLL is produced; **(2) Structural compatibility** — required assembly identity, types, signatures, framework target, and dependencies are compatible with the intended consumer; **(3) Behavioral compatibility** — the intended application/runtime successfully uses the replacement and relevant behavior, including the edits, is verified. Do not describe Level 1 as replacement compatibility. Replacement may work when the reconstructed source, references, target framework, identity and signing requirements are sufficiently compatible, but this must be tested for the intended application. Preserve the original DLL and export the rebuilt assembly as a separate file.

Strong-name signing, framework compatibility, external dependencies, and licensing must be addressed.

**Ordering:** Finish M11 Android-device acceptance first. Then make and document the M11 production-integration decision. Only after that gate should M12 feasibility and implementation begin. Preserve the original assembly and the untouched decompilation result throughout.

**Current M12 status:** Approved scope documented; implementation not started; compiler/runtime feasibility not yet proven; no device tests or CI verification exist for M12.

## 23. Planned enhancement — Online-assisted decompilation and original-source recovery (2026-11-14)

This is a **future roadmap enhancement**, not an implemented M11 feature and not a reason to delay the current M11 Android-device acceptance work. DroidInspect must remain useful in fully offline mode. Online assistance should supplement, never replace, the local decompilation engine.

### Goals

Online assistance may improve results when local decompilation lacks reference assemblies or dependency DLLs, and may recover original source information when publishers have made matching symbols or source available. It cannot recreate original comments, formatting, or source details that were never included in the assembly or published separately; it does not guarantee recovery of original variable names, defeat obfuscation, or make unsupported/native code decompilable.

### Planned operating modes

- **Offline mode:** use the bundled and user-provided local reference assemblies and dependencies. No network connection is required for the supported offline workflow.
- **Online-assisted mode:** when the user explicitly enables it, offer to retrieve missing reference assemblies or dependencies from appropriate trusted sources, and optionally query configured symbol servers or Source Link information for matching original source.
- **Offline cache:** retain successfully retrieved reference packages and symbol/source data in a managed cache so later analysis can work offline where licensing and source terms permit.

### Safety, integrity, and user control

1. Never upload the user's DLL, EXE, APK, decompiled source, or project files automatically. Online assistance must not transmit the inspected assembly or source code.
2. Ask for explicit user consent before downloading each missing dependency/reference package or contacting a symbol/source service. Explain what will be requested and from which source.
3. Prefer authoritative framework, package, publisher, and configured symbol sources. Do not silently substitute arbitrary files.
4. Validate package identity and compatibility, including assembly name, version, culture, public-key token where applicable, target framework, and dependency relationships.
5. Verify cryptographic hashes or trusted package signatures when available; record source, version, hash, and retrieval result. Report when integrity cannot be independently verified.
6. Cache downloads with provenance metadata and provide a way to clear the cache. Do not overwrite bundled references or user-provided files.
7. Respect package licenses, publisher terms, symbol-server access rules, and source-code licensing. Do not redistribute downloaded dependencies or recovered source unless permitted.

### Source provenance and result labels

Keep reconstructed source and recovered original source clearly distinct:

- **Decompiled / reconstructed C#:** generated by the local decompiler from IL and metadata. It may differ from the original source and may omit information not present in the assembly.
- **Recovered original source:** source obtained from a matching PDB, Source Link endpoint, or symbol/source server. Show the source provenance and matching assembly/build identity where available.
- **Partially recovered:** only some source files, symbols, line mappings, or names were available. Identify missing portions rather than presenting the result as complete original source.

A PDB or Source Link record is useful only when it matches the inspected assembly/build and the publisher has made the relevant information accessible. If no matching source is published, fall back to ordinary decompilation and state that original source was unavailable.

### Acceptance criteria for this future enhancement

- Existing offline M11 scenarios continue to work with networking disabled.
- Online assistance is opt-in and no inspected user file is uploaded.
- Missing references are resolved only after consent and are checked for identity/version compatibility.
- Download provenance and integrity-check outcomes are visible; failed or uncertain checks are reported.
- Cached references can be reused offline without silently changing their identity.
- Matching PDB/Source Link source is identified as recovered original source; reconstructed C# remains separately labelled.
- Missing, mismatched, unavailable, or inaccessible symbols produce a clear fallback to decompiled output without falsely claiming original-source recovery.
- Network failures, unavailable servers, invalid packages, and incompatible dependencies do not crash the app or corrupt the offline cache.

**Status:** planned only; no online resolver, downloader, symbol-server client, Source Link client, or cache implementation is claimed to exist. M11 device acceptance remains the immediate priority. M12 remains gated by M11 acceptance and the documented production-integration decision; this enhancement should be separately scoped and scheduled without weakening those gates.


## 24. M11 source search highlighting and navigation — implementation (2026-11-14)

The isolated M11 proof viewer now supports visible search highlighting and match navigation in reconstructed C# source.

- Search the complete decompiled output across all source pages, case-insensitively.
- Highlight every occurrence on the currently displayed page.
- Use a visually distinct highlight for the selected occurrence.
- Provide separate Previous Match and Find/Next Match actions, wrapping at the ends of the match list.
- Automatically switch to the page containing the selected match and scroll it into view.
- Display the query and current match position/count, for example `Found: JsonConvert (match 1 of 12)`.
- Keep page navigation separate from match navigation.
- Apply Android text spans only to a displayed page copy; do not mutate the original complete decompiled source.

Implementation files:
- `tools/m11-poc/android-host/app/src/main/java/com/tanvir/m11poc/MainActivity.java`
- `tools/m11-poc/android-host/app/src/main/res/layout/activity_main.xml`

This changes only the isolated M11 proof viewer, not the native engine or production app. GitHub Actions build verification and Android device acceptance are separate gates. Implementation alone does not establish that highlighting works correctly on the phone.


M11 search implementation follow-up commits on 2026-11-14:
- `4e5b8da368541955335ceebe9661b20c26679fa7` — search matching without allocating a second full-size lowercase copy of the decompiled output.
- `76e75a45fffec14c02b9e3deac23577dc4ed6c37` — correct the match-status line separator.


## 25. M11 consolidated Android-device verification update (2026-11-14)

This section records the user's latest direct CodeAssist/Android test observations. These are device observations, separate from host tests and GitHub Actions. M11 remains isolated under `tools/m11-poc/`; production integration has not started.

### Confirmed device results

| Test | Device result | Notes |
|---|---|---|
| Native engine startup | PASS | App reported: "Native engine loaded. Offline decompilation ready." |
| Bundled Newtonsoft.Json 13.0.3 offline decompilation | PASS | 705,296-byte input; approximately 20.3 seconds; 1,182,379 output characters; 50 pages; input SHA-256 unchanged. Dependency diagnostics remained. |
| Search highlighting and navigation | PASS | User confirmed visible search highlighting/navigation on device. Search is case-insensitive and spans the complete output; page and match navigation are separate. |
| Serilog.dll selected externally | PASS with dependency warnings | 161,280-byte input; 3,281 ms; 790,528-byte Java heap change; 11 source pages. Source returned. Framework references requested .NET 9.0.0.0 while .NET 11.0.0.0 candidates were found. |
| Serilog with configured dependencies available | PARTIAL | Source returned, but framework/dependency identity mismatches remained; this is not a dependency-resolution pass. |
| Serilog with Serilog.dll intentionally missing | PARTIAL / expected diagnostic | Missing Serilog identity was reported and source reconstruction was attempted; unresolved references caused incomplete type information. |
| NLog 6.2.2 external managed DLL | PASS with dependency warning | 921,088-byte input; 24,908 ms; 4,616,192-byte Java heap change; 64 source pages. C# source returned. Requested netstandard 2.0.0.0 did not match the available 2.1.0.0 candidate. |
| NLog repeatability | PASS | User repeated the same NLog test twice after the initial run and reported the same result both times (three consistent runs total). |
| External DLL source paging | PASS | User navigated Serilog's 11 pages and NLog's 64 pages to the final page without a reported crash or freeze. |
| Invalid TXT input | PASS | 17,062-byte TXT file rejected in 2 ms with InvalidDataException explaining invalid/corrupted PE/.NET metadata. |
| Native/non-managed EXE rejection | PASS | JeffBox.exe (454,286 bytes) was rejected in 47 ms: PE image had no .NET metadata directory; native executables are not supported by the current managed-assembly input path. |
| Managed EXE decompilation | NOT YET TESTED | JeffBox's runtime requirement does not establish that its launcher is a directly decompilable managed assembly. No ordinary managed EXE has yet returned C# source on the phone. |

The NLog repeatability checks were user-reported as "Same"; exact repeat-run timings and memory figures were not supplied and are not inferred.

### Dependency-resolution findings

The decompiler can return useful reconstructed C# while also reporting unresolved or mismatched references. These states must remain distinct:

- Newtonsoft.Json: source returned, but framework reference diagnostics were present.
- Serilog: source returned, but .NET 9 versus .NET 11 framework identity mismatches were reported; the missing-Serilog test also reported the absent Serilog reference.
- NLog: source returned, but netstandard 2.0 versus 2.1 identity mismatch was reported.

These results establish source reconstruction for the tested inputs, not complete reference resolution or perfect source reconstruction. The planned online-assisted reference/symbol enhancement in section 23 is not implemented.

### CI verification on the tested source commit

Both workflows were inspected at commit `42b4b11742a0017bac00b257d817587d6accb38a` and their jobs/steps reported completed successfully:

- M11 ILSpy Native AOT Android Proof — run [43501102992](https://github.com/xolomon-coder/DroidInspect/actions/runs/43501102992): SUCCESS. Managed fixture compilation, host ILSpy reconstruction, managed EXE test, invalid-input/repeatability tests, real-world assembly tests, Native AOT ARM64 engine, JNI checks, isolated Android proof APK build and payload verification all passed.
- Production DroidInspect Android CI — run [43501103023](https://github.com/xolomon-coder/DroidInspect/actions/runs/43501103023): SUCCESS. Host metadata/IL/DEX tests, Android debug compilation, unsigned release build, signing verification and tamper test passed.

CI proves the automated host/build/package checks for that commit. It does not replace the user's Android-device acceptance. The documentation-only update in this section does not alter app source and does not require another CodeAssist build.

### Current M11 acceptance decision

**Status: Device validation substantially passed; M11 is not yet fully accepted.**

Remaining acceptance gap:
1. Obtain a known ordinary managed .NET EXE (not a native apphost, Native AOT binary, or self-contained native launcher) and verify that DroidInspect returns C# source on the phone.
2. Record the result and then make an explicit M11 production-integration decision.

Do not hold the completed DLL, paging, invalid-input, repeatability, or CI results open while waiting for the EXE test. Do not claim that managed EXE device decompilation passed until a genuine managed EXE has been tested.

### M12 gate

M12 remains approved as a planned scope only. Do not start M12 implementation until M11 device acceptance and the production-integration decision are recorded. No M12 editing/compiler/rebuild implementation is claimed.

### Exact next action

Continue from the current M11 proof app without rebuilding (no app source changed for this documentation update). Find a known ordinary managed .NET EXE fixture and test it through **SELECT ANOTHER MANAGED DLL / EXE**. If it returns source, record its input identity, output status, page count, and dependency diagnostics. If it is another native apphost, record the rejection and use a purpose-built ordinary managed EXE fixture instead.

### FluentCleaner Classic 1.19.269 — managed EXE device test (2026-11-14)

The user selected `FluentCleaner.exe` through the M11 proof app's **SELECT ANOTHER MANAGED DLL / EXE** action. The app returned reconstructed C# source from this real-world managed EXE.

- Input size: 1,164,800 bytes
- Decompilation time: 4,469 ms
- Source output: 16 pages
- Java heap change: 1,146,880 bytes
- Status: `SOURCE RETURNED — DEPENDENCY WARNINGS`
- Managed EXE source reconstruction: **PASS**
- Dependency resolution: **PARTIAL**, not complete.

Diagnostics:
- `System.Net.Http` requested 4.2.0.0; candidate 11.0.0.0 (identity mismatch).
- `FluentCleaner.Core, Version=1.0.0.0` unresolved.
- `System.Text.Json` requested 8.0.0.5; candidate 11.0.0.0 (identity mismatch).
- `System.Windows.Forms, Version=4.0.0.0` unresolved.
- `System.Management, Version=4.0.0.0` unresolved.

This closes the previously pending ordinary managed-EXE device decompilation test for this input. It does not prove complete dependency resolution, recompilation, or runtime compatibility. The Windows Forms and Management references indicate Windows-specific dependencies. No app source changed and no CodeAssist rebuild was required for this documentation update.

M11 device validation now includes real-world DLL and EXE source reconstruction. The M11 production-integration decision is still pending and must be made explicitly before M12 implementation begins. See `tools/m11-poc/ANDROID-ACCEPTANCE-LOG.md` for the detailed device acceptance record.


## 26. Long-term product direction — Everything gradually (2026-11-14)

The user selected **D — Everything gradually** as the overall product direction. DroidInspect is intended to grow progressively into a broad Android-based APK and .NET inspection, editing, rebuilding, and analysis tool. This is a roadmap decision, not approval to begin all milestones at once.

The agreed long-term sequence is the 20-milestone roadmap in section 4:
- **M11–M16:** real C# decompilation, separate-copy source editing/recompilation, dependency/framework resolution, advanced Assembly Explorer/IL Viewer, advanced C# editing, assembly comparison, and resource extraction.
- **M17–M18:** expand the existing APK/DEX inspection foundations and complete the supported APK editing, rebuilding, signing, and verification workflow.
- **M19–M20:** project workspace/professional tools, performance, compatibility, regression, and final integration.

**Avoid duplicated work:** M2/M4/M6/M8 already establish APK inspection, DEX browsing, resource/manifest editing, and APK signing foundations. M14–M18 must extend and integrate those capabilities rather than restart them. M12 provides the initial separate-source editing and DLL recompilation workflow; M15 is the later advanced multi-file editor experience.

### Current gates and status

- **M11:** Real-world DLL and ordinary managed EXE source reconstruction have both been reported working on the user's Android phone. The latest managed EXE test was FluentCleaner.exe (1,164,800 bytes; 4,469 ms; 16 source pages), with dependency warnings. Dependency resolution is still partial. M11 production integration is authorized and implemented; production-device acceptance remains pending.
- **M12:** Approved scope, not started. It remains gated until the user accepts production M11 behavior on-device.
- **M13–M20:** Planned only; no implementation is claimed.
- The isolated M11 proof app remains under tools/m11-poc/; do not silently merge it into the production app.
- The project remains offline-first and phone-developed through CodeAssist, with Java + native Android XML as the preferred application architecture. Any major runtime/compiler/engine change requires demonstrated feasibility and a documented decision.

### Next decision

M11 production integration is implemented; use section 27 for the current implementation and CI record. The next action is consolidated production-device acceptance. Do not begin M12 or M13 until the user accepts M11 production behavior. Continue preserving all existing APK and .NET functionality.



## 27. M11 production integration — implementation and cloud verification (2026-11-14)

**Status:** Production Java/XML integration and generated native/reference payload are committed on `main`. The production Android regression workflow and expanded M11 Native AOT workflow passed. A production debug APK containing the ARM64 engine, production JNI bridge, and offline .NET 11 references was built and verified. User device acceptance remains pending. M12 is not started.

### Production architecture

- Production package remains `com.tanvir.droidinspect`; the isolated proof package remains `com.tanvir.m11poc`.
- The production app continues to use Java + native Android XML. No new Android runtime library dependency was added.
- The existing ILSpy 11.2.0.10050 / .NET 11 Native AOT Bionic ARM64 engine is reused. It is not rewritten in Java.
- Production JNI uses `droidinspect/app/src/main/cpp/jni_bridge.cpp` and the production Java class `NativeBridge`; the proof JNI class and binary remain unchanged.
- The engine is loaded lazily only when the user opens C# decompilation, and library loading is performed on the background worker. The native load order is engine first, JNI second.
- Decompilation is exposed as a separate dashboard action. The existing raw .NET IL browser, metadata reader, and IL editing/export code remain separate and unchanged.
- A managed assembly is reopened through its SAF URI into a unique app-private cache workspace. The original document is never overwritten or deleted. The working copy remains available for the complete native call and is deleted in the workspace close/finally path.
- The .NET 11 reference pack is extracted from APK assets into app-private persistent storage using a staging directory and completion marker. It is not extracted at app startup.
- The production source viewer uses 12,000-character pages, case-insensitive next/previous search, visible match highlighting, page navigation, and copy-current-page. Search avoids building a second lowercase copy of the complete output.
- The Native AOT call is synchronous and cannot be safely interrupted. Leaving the Activity does not interrupt the native call; the worker completes and cleans its private workspace. Immediate cancellation is not claimed.
- ARM64 is required for C# decompilation. Other DroidInspect features are not coupled to native engine initialization. The app reports unsupported ABI when the decompiler is requested on an unsupported ABI.
- Dependency diagnostics remain visible. Framework identity mismatches and missing third-party references are not silently substituted or represented as resolved. C# source reconstruction is not proof that the target assembly can run on Android or that a rebuilt DLL can replace the original.

### Exact production integration files

Added:
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/NativeBridge.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DecompilationWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/CSharpDecompileActivity.java`
- `droidinspect/app/src/main/cpp/jni_bridge.cpp`
- `droidinspect/app/src/main/res/layout/activity_csharp_decompile.xml`
- `THIRD-PARTY-NOTICES.md`

Updated:
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/MainActivity.java`
- `droidinspect/app/src/main/res/layout/activity_main.xml`
- `droidinspect/app/src/main/AndroidManifest.xml`
- `.github/workflows/m11-nativeaot-poc.yml`

Generated by CI and committed to production source on `main` for CodeAssist:
- `droidinspect/app/src/main/jniLibs/arm64-v8a/libdroidinspect_engine.so`
- `droidinspect/app/src/main/jniLibs/arm64-v8a/libdroidinspect_jni.so`
- `droidinspect/app/src/main/assets/reference-assemblies/net11.0/*.dll`

Intentionally unchanged:
- `FileInspectionEngine.java`, `DotNetMetadataReader.java`, `DotNetIlReader.java`, `DotNetIlActivity.java`, APK/DEX activities and parsers, existing Gradle configuration, and all source under `tools/m11-poc/`.
- No M11 test assemblies or proof-only test buttons are included in the production APK.

### Cloud verification evidence

- Production Android CI run #251, run ID `43543910838`, on source commit `68642367dee691a015140558333cb02b0ac4af8e`: SUCCESS. All existing metadata, IL, and DEX host tests passed; Android debug and release compilation passed; CI APK signing, v1/v2/v3 verification, tamper rejection, and artifact upload passed.
- Production Android CI run #253, run ID `43544227605`, on workflow commit `6af43f88a2b3ecfcb95f24994f5821d062a50b5c`: SUCCESS with the same regression checks.
- Final production Android CI run #255, run ID `43545083044`, on commit `c2d6db4eda9d95650d538cfe179e223653035562` (including the committed engine, JNI, and reference payload): SUCCESS. Metadata, IL, and DEX host tests passed; debug and release APK builds passed; CI signing, v1/v2/v3 verification, tamper rejection, APK output checks, and artifact upload passed. Artifact: `DroidInspect-Phase3-builds`, ID `14094040530`, 41,095,024 bytes. Run: https://github.com/xolomon-coder/DroidInspect/actions/runs/43545083044
- Final documentation/source-tree CI run #256, run ID `43545304528`, on commit `c11b8ee83d68bcd07172217949abaa15b2b9e5c6`: SUCCESS. All host tests, Android debug/release builds, signing/verification, tamper rejection, and artifact upload passed with the committed production payload. Artifact: `DroidInspect-Phase3-builds`, ID `14094240278`. Run: https://github.com/xolomon-coder/DroidInspect/actions/runs/43545304528
- M11 Native AOT workflow run #75, run ID `43544705781`, on source commit `1fa29a6c7c8faa466ea00d913b687741d7edd315`: SUCCESS. Existing M11 managed fixture, real-world assembly, dependency diagnostics, robustness, Native AOT publish, ELF/exports/SONAME, both JNI builds, isolated proof APK, production APK payload checks, generated asset commit, and artifact upload passed.
- Production APK payload checks confirmed both ARM64 shared libraries, the production JNI symbol, and `assets/reference-assemblies/net11.0/System.Runtime.dll`; no proof-only `assets/test-assemblies` were present.
- Generated payload commit: `f5e774d04c466ebdcd020363f5f837786adfa8bd` (`[m11-assets] Add generated ARM64 engine and offline assets for CodeAssist`). It adds 176 .NET 11 reference DLLs (6,483,320 bytes), the 45,966,680-byte engine, and the 11,032-byte production JNI bridge. Engine SHA-1 Git blob: `33761b7fbbdde03ed9fd9d8da74a211b25a9731a`; JNI SHA-1 Git blob: `102b105bdc199a773c909e58f40df73ede43b666`.
- Production debug APK from M11 run #75: approximately 51 MB; SHA-256 `6c3355f6a8020618e0213f9075f3a8520860c7c832cdaea2d4f1342d9eb53cc5`. M11 workflow artifact `DroidInspect-M11-Android-Proof`, artifact ID `14092789745`, contains the isolated proof APK, production debug APK, engine, and verification outputs. Run: https://github.com/xolomon-coder/DroidInspect/actions/runs/43544705781
- The 46 MB Native AOT engine and 6.48 MB reference pack increase the uncompressed APK payload by approximately 52.45 MB before compression/packaging. Actual APK size is measured by CI.

### Known limits and remaining acceptance

- The decompiler is ARM64-only. The Native AOT engine targets Android Bionic API 26+; production minSdk remains 26.
- The current engine returns the complete reconstructed source through JNI. Display paging reduces rendered UI content but does not make decompilation streaming. The M11 proof's reported Newtonsoft.Json process PSS peak was approximately 130 MB; production-device memory behavior must be retested through the production Activity.
- Input copy remains capped at 512 MB. Very large source output may still create memory pressure; no universal large-assembly guarantee is made.
- The reference pack is .NET 11.0. Assemblies targeting other frameworks may show unresolved or identity-mismatch warnings. Dependency resolution remains partial.
- Cancellation of a synchronous native call is not implemented. The worker retains its private input until native execution finishes, then cleans it.
- Direct ILSpy and .NET runtime/reference-pack MIT license texts are recorded in `THIRD-PARTY-NOTICES.md`. Recheck the exact transitive NuGet dependency graph and generated notices before public redistribution.
- No Android device test has yet verified the new production screen, native loading through the production package, SAF workspace cleanup, production source paging/search, or memory behavior. Cloud build success is not device acceptance.

### Current gates

| Item | Status |
|---|---|
| Isolated M11 engine and proof app | Implemented and substantially device-validated |
| Production Java/XML integration | Implemented |
| Production Android regression CI | PASS |
| M11 Native AOT + production APK CI | PASS |
| Generated production engine/JNI/reference payload committed to main | PASS — commit `f5e774d` |
| Final main ZIP and production APK handoff | Ready — final regression run #256 passed |
| Consolidated production Android device acceptance | Pending user test |
| M12 C# source editing/recompilation | Not started; remains gated on production M11 acceptance |
| M13–M20 | Planned only |

**Next exact action:** deliver the latest `main` ZIP and installable debug APK for one consolidated production-device acceptance test. Do not start M12.


---

## 28. M11 production-device acceptance — first test session (2026-11-14)

This section supersedes the earlier production-device acceptance status in section 27 wherever the same checks are now marked complete. These results were reported by the user from the installed production package `com.tanvir.droidinspect`, using `droidinspect-ci-signed.apk`. They are distinct from the earlier isolated proof-app results.

### Production app launch and initial inspection

- Installed `droidinspect-ci-signed.apk` from the GitHub Actions artifact ZIP.
- DroidInspect launched successfully and displayed the production dashboard.
- The dashboard exposed the expected APK/.NET inspection actions, including **DECOMPILE TO C# SOURCE**.
- Selected `Newtonsoft.Json.dll`; PE inspection completed and reported:
  - Size: 705,296 bytes
  - PE signature: PASS
  - Machine: x86 (0x014C)
  - Optional header: PE32
  - Sections: 3
  - .NET CLI header: PRESENT

### Production C# source viewer — Newtonsoft.Json.dll

The production app returned reconstructed C# source and displayed the dependency diagnostics above the source. The viewer reported 99 source pages.

User-confirmed results:

| Production device check | Result |
|---|---|
| Dashboard launch | PASS |
| Newtonsoft.Json.dll selection and PE/.NET recognition | PASS |
| Real C# source returned | PASS |
| Source viewer and 99-page count | PASS |
| Dependency diagnostics visibly included | PASS |
| Search highlighting | PASS |
| PREV/NEXT search navigation | PASS |
| Cross-screen navigation to the next matching occurrence | PASS |
| PREV PAGE/NEXT PAGE navigation | PASS |
| Copy current source page and paste into another text editor | PASS |

Reported dependency diagnostic: requested `netstandard, Version=2.0.0.0` did not match the configured `netstandard, Version=2.1.0.0` candidate. Source reconstruction succeeded, but dependency resolution remains partial.

Search navigation behavior was clarified during testing: PREV/NEXT moves to the next or previous matching occurrence, automatically switching to another visible screen/page when the match is elsewhere. The user confirmed the new match is highlighted and visible. This is accepted as working behavior; it is not a requirement to move only among matches already visible on the current screen.

### Production C# source viewer — FluentCleaner.Classic.exe

The user selected `FluentCleaner.Classic.exe`. The production app returned reconstructed C# source and reported 31 source pages.

Observed diagnostics:

- `System.Net.Http, Version=4.2.0.0`: candidate identity mismatch with version 11.0.0.0.
- `FluentCleaner.Core, Version=1.0.0.0`: unresolved.
- `System.Text.Json, Version=8.0.0.5`: candidate identity mismatch with version 11.0.0.0.
- `System.Windows.Forms, Version=4.0.0.0`: unresolved.
- `System.Management, Version=4.0.0.0`: unresolved.

User confirmed that source paging/search testing on this 31-page result worked. The dependency warnings are correctly exposed; they do not mean decompilation failed or that all references were resolved. The WinForms and Management references are Windows-specific dependencies and do not establish Android runtime compatibility.

### Search match counter — accepted enhancement request

During production testing, the user requested a visible search match counter that shows the exact active match number and total number of matches while navigating with PREV/NEXT.

Add this to the planned DroidInspect enhancement backlog; **do not implement it during the current M11 acceptance session**.

Required behavior:
- Count matches across the complete decompiled source, not only the currently displayed page.
- Display the active position and total, for example `MATCH 1 OF 27`.
- Update immediately on PREV/NEXT, e.g. `MATCH 2 OF 27`.
- Keep the active occurrence highlighted and automatically navigate to its page/screen.
- Reset/recalculate when the search query changes or is cleared.
- Display a clear `NO MATCHES` state when the query has no results.
- Keep page navigation independent from match navigation.
- Define and test consistent wrap-around behavior at the first and last match.
- Avoid unnecessary full-source copies or large temporary allocations; preserve the current memory-conscious search implementation.

This is a planned UX enhancement, not a current M11 acceptance failure. The existing search/highlighting/navigation behavior has passed the production-device checks above.

### Current production acceptance state

**Confirmed PASS:** production install/startup, managed DLL recognition, real C# source reconstruction for both a DLL and a real-world EXE, dependency warning display, source page navigation, search highlighting and cross-page match navigation, and copy-current-page.

**Remaining production acceptance checks after the 2026-11-14 follow-up session:**
1. Offline decompilation with internet disabled.
2. Graceful handling of invalid/unsupported input.
3. Practical responsiveness/memory review during the remaining tests.

Original-file preservation has already been verified in earlier M5/M7 device testing for separate-copy edit/export workflows. Do not repeat that test without a specific reason. The read-only production decompilation flow does not intentionally modify its selected input.

Do not start M12 until the remaining essential production checks are complete and the user explicitly accepts the production integration.

No application source was changed by these documentation updates. No CodeAssist rebuild is required for documentation-only changes.

**Exact stopping point:** the user has completed production testing of the C# source viewer for Newtonsoft.Json.dll and FluentCleaner.Classic.exe, including search/highlighting, page navigation, and copy/paste. In the follow-up session, the production .NET IL browser was confirmed working: 4,185 MethodDef rows indexed, 4,064 method bodies decoded, and representative method bodies opened with decoded instructions and metadata-token operands. Production PE/.NET inspection was also confirmed working for Newtonsoft.Json.dll (PE signature PASS, x86/PE32, 3 sections, .NET CLI header PRESENT). These checks close the previously listed production IL-browser and basic PE-inspection regression items. Search match counter remains a later enhancement.


## 29. M11 production regression follow-up — IL browser and PE inspection (2026-11-14)

The user continued acceptance testing on the installed production package. These results close the two regression items previously listed in section 28.

### Production raw .NET IL browser — PASS

Input: `Newtonsoft.Json.dll`.

- Assembly identified as `Newtonsoft.Json`.
- MethodDef rows: 4,185.
- Methods indexed: 4,185.
- Method bodies decoded: 4,064.
- Method list displayed correctly.
- Opening a selected method displayed its method header, IL byte count, instruction offsets, opcode bytes, decoded instruction names/descriptions, and metadata-token operands.
- Representative methods tested included a short constructor, a short `JsonConvert.SerializeObject`-related wrapper, and a longer `JsonSerializerInternalWriter` method containing `ldarg.0`, `newobj`, `stfld`, `ldarg.1`, `call`, and `ret`.

Assessment: production IL indexing, method selection, and representative raw IL decoding are confirmed working. Exhaustive opcode coverage was not tested and is not claimed.

### Production PE / .NET inspection — PASS

The user selected `Newtonsoft.Json.dll` through the dashboard's **SELECT APK / DLL / EXE** action. The production inspection report showed:

- File size: 705,296 bytes.
- PE signature: PASS.
- Machine: x86 (0x014C).
- Optional header: PE32.
- Sections: 3.
- .NET CLI header: PRESENT.

Assessment: basic production PE structure and .NET CLI-header inspection are confirmed working. This does not represent a fresh exhaustive metadata-table validation; the metadata reader and its host tests, plus earlier M5 device verification, are documented in prior milestone records.

### Avoiding duplicate acceptance tests

- Raw IL browsing and basic PE/.NET inspection are now confirmed in the production package; do not ask the user to repeat them.
- Original-file preservation was already tested on-device in earlier M5/M7 separate-copy edit/export workflows; do not repeat without a specific reason.
- Earlier production C# source viewer tests remain valid and are recorded in section 28.
- The search match counter remains a planned enhancement, not an M11 acceptance blocker.

### Final M11 production acceptance results — 2026-11-14

All listed M11 production acceptance checks have now been confirmed PASS by the user:

- Production APK installation and startup.
- Managed DLL recognition and basic PE/.NET inspection.
- Real C# source reconstruction for `Newtonsoft.Json.dll` and `FluentCleaner.Classic.exe`.
- Dependency diagnostic display, including unresolved and version-mismatched references.
- Source paging, search highlighting, cross-page match navigation, and copy-current-page.
- Offline decompilation: the installed production APK successfully decompiled `Newtonsoft.Json.dll`; the app does not declare/request Android Internet permission. No separate airplane-mode retest is needed.
- Production .NET IL browser: 4,185 MethodDef rows indexed, 4,064 method bodies decoded, and representative method bodies opened with decoded instructions and metadata-token operands.
- Production PE inspection: PE signature PASS, x86/PE32, three sections, and .NET CLI header present.
- Original-file preservation in earlier M5/M7 separate-copy edit/export testing.
- Graceful handling of invalid/unsupported input.
- Practical responsiveness and memory review: user confirmed the app remains responsive while opening and navigating large C# source files.

**M11 production acceptance testing is complete.** Dependency warnings remain expected when required assemblies are not bundled or otherwise available locally; they do not invalidate successful source reconstruction. The reconstructed source is not guaranteed to compile or be binary-compatible, and Windows-specific dependencies do not imply Android runtime compatibility.

The search match counter remains a planned UX enhancement, not an M11 acceptance failure.

**Gate:** M12 has not started. Begin M12 only after the user explicitly authorizes the next phase.

## 30. M12 start — offline compiler feasibility prototype (2026-11-15)

**Status:** M12 is active. The M11 production acceptance gate was satisfied by the user's explicit PASS results on 2026-11-14. No M11 acceptance tests are being repeated.

### Repository and production architecture inspection

- Starting main HEAD: `c476e6778fa592ef724ad1b39f0d67b17548a9cf`.
- Production app remains Java + native Android XML, package `com.tanvir.droidinspect`, minSdk 26, Java 8 source/target.
- Existing production ILSpy Native AOT engine and JNI bridge are preserved; native engine binary is approximately 45.97 MB.
- Existing production .NET 11 reference-assembly bundle is approximately 6.5 MB (the full CI reference pack is approximately 39 MB).
- Production manifest does not declare Android Internet permission.
- Existing `CSharpDecompileActivity`, `DecompilationWorkspace`, `NativeBridge`, `jni_bridge.cpp`, and `DotNetMetadataReader` were inspected before M12 changes.
- The M12 proof reuses the production reference-assembly asset directory instead of duplicating the reference pack.

### Compiler candidate decision

First candidate: **Microsoft.CodeAnalysis.CSharp (Roslyn) 5.12.0**, MIT license, NuGet package size reported as 11.82 MB. Roslyn exposes the C# compiler API and emits PE assemblies; this is a real compiler route, not a decompiler feature or file-extension rename.

Proposed hosting route: .NET 11 Native AOT shared library targeting `linux-bionic-arm64`, called through a separate JNI bridge. The existing M11 architecture proves that this Native AOT/JNI pattern can host native code on the target device, but it does not prove that Roslyn itself is Native AOT-compatible or performant. Native AOT restrictions, including dynamic loading and runtime code generation, remain explicit feasibility risks.

Alternatives assessed:
- Full .NET SDK/compiler bundled into the app: rejected as the first mobile candidate due to large SDK payload and command-line/toolchain complexity.
- A separate Mono/CoreCLR runtime hosting Roslyn: retained as a fallback only; it adds another runtime, Android integration complexity, and a larger packaging/performance burden.
- Roslyn in a Native AOT library: selected for isolated proof only, not production integration.

### Isolated proof files added

- `tools/m12-poc/README.md`
- `tools/m12-poc/engine/DroidInspect.M12.Compiler.csproj`
- `tools/m12-poc/engine/CompilerService.cs`
- `tools/m12-poc/engine/NativeExports.cs`
- `tools/m12-poc/fixture/EditedFixture.cs`
- `tools/m12-poc/host-tests/HostTests.csproj`
- `tools/m12-poc/host-tests/Program.cs`
- `tools/m12-poc/android-host/settings.gradle`
- `tools/m12-poc/android-host/build.gradle`
- `tools/m12-poc/android-host/app/build.gradle`
- `tools/m12-poc/android-host/app/src/main/AndroidManifest.xml`
- `tools/m12-poc/android-host/app/src/main/java/com/tanvir/m12poc/NativeBridge.java`
- `tools/m12-poc/android-host/app/src/main/java/com/tanvir/m12poc/MainActivity.java`
- `tools/m12-poc/android-host/app/src/main/cpp/jni_bridge.cpp`
- `tools/m12-poc/android-host/app/src/main/assets/fixture/EditedFixture.cs`
- `.github/workflows/m12-roslyn-compiler-poc.yml`

The proof compiles a small C# fixture with a deliberately edited `EditedFixture.GetEditedValue()` method. It must emit a genuine managed DLL and verify PE/CLI metadata, assembly identity, and the edited symbol. The Android proof writes only to its private app files directory and requests no Internet or storage permissions.

### CI history and current gate

**Cloud feasibility proof: PASS. Android-device proof: PENDING.**

- Initial M12 workflow run #1 failed during host compilation because the `EmitResult` namespace import was missing. The Actions log identified the error; the import was added and the corrected build passed.
- M12 workflow run #2 completed successfully: Roslyn host compile, PE/CLI output verification, Bionic ARM64 Native AOT publish, JNI bridge build, isolated Android proof APK build, APK integrity, ARM64 payload presence, bundled offline references, and no Internet permission.
- M12 workflow run #3 added an executable behavior test. The host loaded the emitted DLL, invoked `EditedFixture.GetEditedValue()`, and verified the exact result `M12_EDITED_VALUE`. The full Native AOT/JNI/APK workflow passed again.
- The Android proof host now checks the emitted output's actual `MZ` signature rather than inferring it from file size.
- An early workflow run failed because a shell continuation contained two backslashes; corrected.
- A concurrent native-payload publication attempt encountered a rebase conflict. The workflow was changed to serialize M12 proof runs and publish by fetching/resetting to the latest `main`, then copying, comparing, and pushing the verified payload with bounded retries. The corrected publication passed.
- Latest full M12 proof workflow run #8 completed **SUCCESS** on 2026-11-15. It repeated host compilation/behavior verification, ARM64 Native AOT, JNI, APK build and offline payload checks, then successfully published the generated native payload to `main`.
- Normal DroidInspect Android CI passed on the M12 source commits. No production application source or M11 engine was modified by this compiler prototype.

### Compiler output and resource measurements

- Roslyn package: `Microsoft.CodeAnalysis.CSharp 5.12.0`, MIT license; NuGet package download size reported as 11.82 MB.
- Host test output: 2,560-byte managed DLL fixture; PE/CLI metadata verified; assembly identity `DroidInspect.M12.EditedFixture`; edited method invocation returned `M12_EDITED_VALUE`.
- Native compiler: 35,227,344 bytes.
- JNI bridge: 10,704 bytes.
- Isolated Android proof APK: 38,365,488 bytes.
- Existing production .NET 11 reference assets are reused (approximately 6.5 MB); no duplicate reference pack was added.
- Compiler and APK sizes are cloud build measurements. Device compile time, peak RAM, thermal behavior, and installed storage remain unmeasured.

### Native AOT trimming warnings — OPEN

The detailed Native AOT diagnostic run used `TrimmerSingleWarn=false` and exposed Roslyn warnings that were hidden behind the aggregate `IL2104` warning:

- `IL2091`: generic-parameter annotation warnings in `RoslynLazyInitializer` and Roslyn pooled-delegate helpers.
- `IL2050`: COM-marshalling warnings in `Microsoft.DiaSymReader.SymUnmanagedFactory` for Windows symbol-reader/writer paths.
- `IL2072`: reflection/constructor-preservation warning in the same COM symbol factory path.

These are warnings, not build errors. The Bionic ARM64 Native AOT library still builds and the Android APK packages successfully. The Windows COM paths are not expected to be used by the Android compilation path, but that assumption is not treated as proof. The generic warnings may affect code paths that require runtime generic initialization or reflection. Do not suppress or ignore these warnings. The Android-device test must establish whether the selected compiler path works; any runtime failure must be fixed or the Native AOT approach rejected.

### Isolated Android proof — CodeAssist test instructions

1. Download the latest repository `main.zip` and extract it without changing the folder structure.
2. In CodeAssist, open the separate Gradle project at `tools/m12-poc/android-host`. Do not open it as a replacement for the production `droidinspect` project.
3. Build the proof project's Debug APK and install it. Its package is `com.tanvir.m12poc`, so it installs separately from production DroidInspect.
4. Open **DroidInspect M12 Compiler Proof** and tap **COMPILE EDITED C# FIXTURE**.
5. Wait for the result. A successful run should show `M12 COMPILATION PASS`, PE/CLI metadata verification for `EditedFixture.GetEditedValue`, and `JAVA VERIFY PASS: MZ signature`.
6. Send the complete result text and approximate compile duration. Also report any crash, app closure, or device slowdown.

Build configuration: Gradle 8.13, Android Gradle Plugin 8.13.0, Java 17, compile SDK 36, minSdk 26, ARM64 only. The prebuilt ARM64 native libraries are already committed; CodeAssist does not need to compile the native engine or install an NDK for this proof. The app declares no Internet permission and runs the compilation offline. It uses only its private app files directory; it does not overwrite a selected DLL or any production data.

### Current stopping point and next action

**Latest main HEAD before this README refresh:** `9be344aa983fb9bc1e326cf50db4b1380b0df173` (verified native compiler payload publication). Normal DroidInspect Android CI run #289 also passed. The generated Roslyn ARM64 library and JNI bridge are committed under `tools/m12-poc/android-host/app/src/main/jniLibs/arm64-v8a/`.

The cloud compiler feasibility gates are complete. The next required gate is a real run of the isolated proof on the user's Android 11 / ARM64 phone. Do not integrate Roslyn into the production app or claim production DLL recompilation support until the device test passes and the Native AOT trimming warnings have been evaluated. If the device test fails, inspect the exact diagnostics and determine whether to preserve Native AOT with corrected roots/annotations or evaluate the separate CoreCLR/Mono fallback.

Production M11 source, native engine, APK/DEX tools, IL browser, and existing workflows remain preserved. M12 source editing/export and DLL replacement compatibility reporting remain future production implementation work after the compiler feasibility decision.

## M12 continued engineering — compiler safety and multi-file host coverage (2026-11-16)

**Status remains IN PROGRESS.** This work improves the isolated compiler foundation; it does not claim production Android compilation or device acceptance.

Verified starting commit for this development batch: `43b8cb14e9f3681e444e66d337925f2bb7c72e7b`. All changes were made directly on `main`.

### Compiler service hardening

Updated:
- `tools/m12-poc/engine/CompilerService.cs`
- `tools/m12-poc/host-tests/Program.cs`
- `tools/m12-poc/fixture/EditedFixture.cs`
- Added `tools/m12-poc/fixture/FixtureSupport.cs`

Changes:
- Removed the compiler's test-specific requirement that every output contain a type named `EditedFixture` and a method named `GetEditedValue`.
- Managed output verification now accepts general managed assemblies and reports assembly name, version, culture, public-key token, public type count, method count, and whether a strong-name public key is present.
- Compiler output is emitted to a unique temporary file in the destination directory. The requested output is replaced only after Roslyn succeeds and PE/CLI verification passes.
- Failed compilation or failed output verification leaves any previous destination DLL untouched; temporary output is cleaned up.
- The host fixture now spans two C# source files and the edited method calls a helper defined in the second file.
- Host regression coverage checks multi-file compilation, metadata report fields, successful method invocation, and preservation of a pre-existing output DLL after an intentionally invalid compile.

### Verification state

- The changes above are committed on `main`.
- GitHub Actions verification for the new compiler-hardening batch is required and must be checked before this work is called cloud-verified.
- Android-device execution remains PENDING and is intentionally deferred to the consolidated final device-testing session.
- Native AOT warnings IL2091, IL2050, and IL2072 remain OPEN; they have not been suppressed.
- Production Android source editing, persistent multi-file source workspace, JNI integration into the production APK, dependency-aware reference resolution, and the three-level DLL replacement compatibility report remain unfinished.

### Current engineering boundary

The isolated compiler still uses the supplied reference directory, currently populated with the .NET 11 reference profile. This is not yet target-framework resolution and does not establish compatibility with the original assembly's target framework or dependencies. Compilation success is not proof of structural or runtime compatibility.

### M12 production source-workspace implementation — 2026-11-16

Added the first production-side C# source editing workspace, while retaining the M11 decompiler and viewer:
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/CSharpSourceWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/CSharpSourceEditorActivity.java`
- `droidinspect/app/src/main/res/layout/activity_csharp_source_editor.xml`
- Updated `CSharpDecompileActivity.java` and `activity_csharp_decompile.xml` to launch the editor from returned decompilation.
- Updated `MainActivity.java` and `activity_main.xml` with an **Open Last C# Source Workspace** entry; the last project ID is persisted in app preferences.
- Updated `AndroidManifest.xml` to register the non-exported editor activity.

Current source-workspace behavior implemented in source:
- Creates a persistent app-private project from the C# output returned by M11.
- Keeps an untouched copy of the original decompilation under a separate `original/` directory; edits are stored under `src/`.
- Supports multiple `.cs` files through new-file creation and multi-select SAF import.
- Supports file switching, edit/save state, and ZIP export of the source project.
- Applies source import bounds (32 MB per file, 128 MB per import operation).
- Uses app-private storage and SAF; no broad storage or Internet permission was added.
- Source project files are kept after the editor closes and the last project ID is persisted so the user can reopen the last saved C# source workspace from the DroidInspect main screen. A full recent-project picker remains a later workspace milestone item.

**Verification:** source changes are committed on `main`; production Android compilation/CI and actual device usability have not yet been verified. Do not mark M12 complete. Compiler-to-editor integration, Android Roslyn invocation, rebuilt DLL inspection, and the three-level replacement compatibility report remain pending. The isolated Native AOT warning set remains open.

### M12 current stopping point — 2026-11-16

Latest main HEAD before this stopping-point update: `a73b16855125ed2aff8654119fda4e6b44ee86a2`.

M12 now has committed compiler-service hardening and a production-side persistent multi-file C# source workspace/editor. The last source project can be reopened from the main screen. This is an implementation milestone within M12, not M12 completion.

Remaining M12 work:
1. Integrate the Roslyn Native AOT compiler and JNI bridge into the production app without disturbing the M11 ILSpy engine.
2. Add the compile action, compiler diagnostics, and output inspection.
3. Retain the original assembly inside the private project for accurate replacement comparison.
4. Implement the required DLL Replacement Compatibility Report with independent compilation, structural, and runtime/behavioral levels.
5. Expand host tests for metadata comparison and compatibility warnings.
6. Evaluate Native AOT warnings IL2091, IL2050, and IL2072; do not suppress them.
7. Verify production Android build and package size in CI; defer actual Android execution and usability acceptance to the final consolidated device session.

Verification limitation at this update: the connected GitHub commit-status query returned no status contexts for the latest main commit, and the available workflow-run lookup does not expose push-triggered runs. Therefore no new CI PASS is claimed here. The production Android build and device behavior remain pending.

The production manifest was rechecked and still does not declare `android.permission.INTERNET`. The repository branch listing contains only `main`.

### M12 Android CI regression found and corrected — 2026-11-16

The first production Android CI run after adding the source editor failed at Android resource linking. The exact cause was an invalid XML attribute in `activity_csharp_source_editor.xml`: `android:hintTextColor` is not a framework attribute. It has been corrected to `android:textColorHint` in commit `842febe8d29a5a14966d3292eb62904524a3c274`.

The host parser tests in that failed run all passed; Android resource linking failed before APK compilation, so no APK artifact was produced. A new push-triggered Android CI run is required to verify the correction. No PASS is claimed until that run completes successfully.

### M12 production compiler integration progress — 2026-11-16

The isolated Roslyn compiler proof has now completed successfully again after the host-test fix.

Verified GitHub Actions:
- M12 Roslyn workflow run `43769428511` (run #17): **SUCCESS**. Host multi-file compile, failed-build preservation test, metadata verification, Native AOT Bionic ARM64 publish, JNI link, isolated Android proof APK packaging, offline/permission checks, and artifact upload all completed successfully.
- M12 Roslyn workflow run `43769751887` (run #18): **SUCCESS**. The updated publication workflow also copied the verified compiler and JNI shared libraries into the production app's ARM64 `jniLibs` directory.
- DroidInspect Android CI run `43769428468` (run #314): **SUCCESS**. Existing metadata, IL, DEX host tests and debug/release APK compilation, APK signing, signature verification, and tamper rejection completed.

The latest generated production compiler payload is:
- `libdroidinspect_m12_compiler.so`: 35,239,464 bytes.
- `libdroidinspect_m12_jni.so`: 10,704 bytes.
- ARM64 only. The compiler adds approximately 35.25 MB of native payload before APK compression.

Production-side M12 source now:
- Preserves a separate copy of the original selected assembly inside the source project (512 MB safety cap).
- Retains untouched decompilation separately from editable source.
- Provides a Java JNI bridge in `com.tanvir.m12poc.NativeBridge`.
- Adds an offline Roslyn compile action for all project `.cs` files, using the bundled .NET 11 reference profile.
- Writes the rebuilt managed DLL to the project's separate `build/DroidInspect.Rebuilt.dll` output.
- Checks the output MZ signature and uses the native compiler's PE/CLI metadata verification before enabling separate SAF export.
- Does not overwrite the original assembly.

**Still pending:** compatibility comparison against the preserved original assembly (identity, target framework, public API signatures, dependencies, signing status), the three-level DLL Replacement Compatibility Report, full production APK CI after the latest integration commits, and Android-device compiler execution. The compiler is currently tied to the .NET 11 reference profile; this is not general target-framework resolution.

Native AOT warnings IL2091, IL2050, and IL2072 remain present and unsuppressed. They are documented and remain an open review item. Roslyn 5.12.0 MIT licensing is now recorded in `THIRD-PARTY-NOTICES.md`; transitive dependency notice review remains pending.

Do not mark M12 complete. The production integration is still in progress and device verification remains deferred to the consolidated final session.

### M12 three-level DLL compatibility report and compiler JNI update — 2026-11-16

Implemented the first structural replacement comparator in `tools/m12-poc/engine/AssemblyCompatibilityReporter.cs`.

The report compares:
- Assembly name, version, culture and public-key token.
- TargetFrameworkAttribute when declared.
- Public type names.
- Public method names and raw signature blobs.
- Public field names and signatures.
- Public property names and signatures.
- Public event names.
- Assembly reference identities (name, version, culture and public-key token).
- Strong-name signed state.

Status rules:
- Missing/changed original public API, assembly identity or original dependencies are reported as structural FAIL.
- Additional public API/references and target framework uncertainty/mismatch are reported as WARNING.
- Exact metadata-level match is PASS.
- Runtime/behavioral compatibility remains NOT VERIFIED; compilation and metadata comparison cannot prove behavior in the original host application.

The compiler now derives the rebuilt assembly simple name from the preserved original assembly when available. The original assembly is passed through the JNI call and remains a separate, unchanged project file. The source editor saves the full compiler/compatibility report to `build/M12-Build-Compatibility-Report.txt` and offers separate SAF export for the report and rebuilt DLL.

Host regression evidence from M12 workflow run `43770487345`:
- Genuine two-file managed DLL compilation: PASS.
- Identity and metadata verification: PASS.
- Compatibility report generated with Level 1 PASS, Level 2 WARNING (target framework attribute absent in the synthetic fixture), and Level 3 NOT VERIFIED.
- Invalid C# compile left the previous output DLL byte-for-byte unchanged: PASS.
- Host loaded the rebuilt DLL and invoked the edited method; result was `M12_EDITED_VALUE`: PASS.

The same workflow run initially failed only while packaging the isolated proof APK because the proof Java declaration had not yet been updated to the expanded four-argument JNI method. That declaration was corrected. M12 workflow run `43770685184` (run #27) then completed successfully: host tests, Native AOT Bionic ARM64 publish, four-argument JNI build, isolated proof APK packaging, offline/ABI checks, native payload publication, and artifact upload all passed.

The verified four-argument JNI/compiler payload was published to production `jniLibs` in commit `e6af8c879488d3568db7594ac177287259624c59`. A new production Android CI run against that exact payload is still required. Android-device compiler execution remains pending for the final consolidated session.

M12 remains IN PROGRESS. Target framework resolution, dependency-aware compilation beyond the bundled .NET 11 reference profile, signing-key support for strong-name assemblies, and actual runtime replacement testing remain bounded or pending.

### M13 offline dependency resolver implementation — 2026-11-16

Added the first offline dependency graph resolver in `tools/m12-poc/engine/DependencyResolver.cs`, exposed through the existing ARM64 Native AOT compiler library and JNI bridge.

Resolver behavior:
- Reads managed assembly identity and AssemblyRef identities, including requested version, culture and public-key token.
- Reads TargetFrameworkAttribute where present and reports when the target framework is unknown or does not exactly match the bundled profile.
- Indexes user-provided local DLL/EXE dependencies and the bundled `.NET 11.0` reference pack.
- Resolves exact identity matches only; it does not silently substitute a different version, culture or public-key token.
- Traverses local dependency references recursively, with cycle protection and a 500-assembly safety cap.
- Reports missing, mismatched, ambiguous and malformed/non-managed candidates.
- Keeps resolution offline; no Internet permission or automatic upload was added.

Production UI/workspace added:
- `DotNetDependencyWorkspace.java`: private persistent root-assembly and local-dependency storage, with bounded SAF imports.
- `DotNetDependencyActivity.java` and `activity_dotnet_dependency.xml`: import local dependencies, resolve offline, display and export the report.
- Main screen entries to analyze the selected managed assembly and reopen the last dependency project.
- Manifest registration for the non-exported dependency activity.

Host tests added for:
1. Exact local dependency plus framework reference resolution.
2. Same-name dependency with wrong version, which must be reported as an identity mismatch.
3. Missing dependency, which must remain unresolved.

The M13 host test step completed successfully in workflow run `43771727050` through the Roslyn compiler host-test stage. Full Native AOT/JNI republishing and production Android CI for the new dependency activity are still running and must complete before cloud verification is recorded. Device verification remains pending for the final consolidated session.

M13 remains IN PROGRESS until the full native publish, production APK checks and final device acceptance are complete. The resolver currently uses the bundled .NET 11.0 reference profile; other framework packs are not yet bundled.

### M13 native resolver proof and payload publication — 2026-11-16

The M13 dependency resolver host regression suite passed in M12/M13 workflow run `43771727050`:
- Exact local dependency and bundled framework references resolved.
- Wrong version for the same assembly name was identified as an identity mismatch.
- Missing dependency remained explicitly unresolved.
- The report included identity/version/culture/public-key-token matching and framework warnings.

The same workflow run successfully:
- Published the updated Roslyn Native AOT ARM64 library with `DroidInspect_M12_ResolveDependencies`.
- Built the JNI bridge and verified the `NativeBridge.resolveDependencies` JNI export.
- Built and verified the isolated offline proof APK.
- Published both updated native libraries into the production app's ARM64 `jniLibs`.

Generated payload commit: `5a01fa56c51bb7762d8e6267cd890ef694828457`.

Production M13 Java/XML screen and main-screen navigation passed the Android debug/release build and existing APK signing/tamper CI in runs `43771678762` and `43771727048`. Those runs predate the workflow-generated native payload commit. A fresh production Android CI run against the exact `5a01fa56` native payload is required; this README update triggers that verification.

M13 remains IN PROGRESS until that production CI completes and the consolidated Android device session verifies dependency resolution. No device PASS is claimed.

### M13 final automated implementation gates — 2026-11-16

M13 dependency resolver now also reports target-framework requirements for transitive local dependencies, including missing TargetFrameworkAttribute and framework/profile mismatches. Host fixtures explicitly declare `.NETCoreApp,Version=v8.0` against the bundled .NET 11.0 profile and assert a framework warning rather than a false compatibility PASS.

M12/M13 workflow run `43772390454` (run #37) completed successfully:
- Host compilation and M12 regression tests: PASS.
- M13 exact local dependency, mismatched version, missing reference and target-framework warning tests: PASS.
- Native AOT Bionic ARM64 publish: PASS.
- JNI bridge link and dependency-resolver export symbol verification: PASS.
- Isolated proof APK packaging and offline/ABI checks: PASS.
- Native payload publication and artifact upload: PASS.

Latest workflow-generated native payload commit: `29ea824cc26467f6eaa41dc206198334476956c2`. A fresh production Android CI run against this exact payload is still required; this documentation commit triggers that run.

M13 code and automated host/CI coverage are implemented. Device acceptance remains pending for the consolidated final session. The resolver is offline-only and uses the bundled .NET 11.0 reference profile; other framework packs are not yet bundled.


### M19 unified project workspace integration — 2026-11-16

Added a unified **APK / .NET Project Workspace** entry to the production main menu and a recent-project launcher at `ProjectWorkspaceActivity.java`.

The launcher enumerates:
- APK projects from the persistent APK workspace index.
- C# source projects from the existing private `csharp-projects` store.
- Offline .NET dependency projects from the existing `dotnet-dependency-projects` store.

Project selection routes to the existing APK project detail screen, C# source editor, or offline dependency workspace. The APK workspace now accepts a project ID so a selected APK project opens directly rather than returning to the APK project list. This reuses the existing project stores and avoids duplicating original APKs, original assemblies, source trees, or dependency files.

Updated production entry points:
- `ProjectWorkspaceActivity.java`
- `MainActivity.java` and `activity_main.xml`
- `AndroidManifest.xml`
- `ApkProjectWorkspaceActivity.java`

**Verification status: NOT YET VERIFIED.** The push-triggered Android Actions jobs for commits in this integration batch terminate in approximately two seconds with `runner_id=0`, an empty step list, and no runner logs. This indicates the job did not reach a GitHub-hosted runner; it is not evidence of a source compilation failure or a successful build. The job must be run when GitHub Actions runner capacity is available. Do not mark M19 complete until the production Android build succeeds and project navigation/reopen/recovery behavior is covered by tests.

Known remaining M19 work:
1. Add .NET source/dependency project notes, bookmarks, saved searches, revision/recovery and report management to the persistent project model rather than relying only on the APK workspace's richer project controls.
2. Add host-testable project-index and metadata/recovery tests, and Android-level persistence tests where available.
3. Verify APK pipeline path handoff, signed output revision replacement, and project reopen after app restart.
4. Complete a successful production Android CI run against the final M19 commit.

The unified launcher currently indexes existing project types; it does not claim that all APK and .NET project operations have already been merged into one storage format. Existing source/dependency project formats remain separate and backward-compatible.


### M19 persistent project records — implementation started

- Added a shared, app-private metadata store for APK, C# source and offline .NET dependency project folders. It persists notes, bookmarks, saved searches, operation history, interrupted-operation markers, and inventories of existing report/export/build files without moving or duplicating original inputs.
- Added project-workspace UI actions to add notes, bookmarks and saved searches, record an operation, and inspect stored project records. Long-press a recent project to manage its records; the workspace button opens the latest project's record tools.
- Metadata writes use a temporary file, flush/sync and rename, with canonical project-folder containment and a 1 MB per-file / 1,000-record bound. Interrupted operations remain visible until explicitly marked finished.
- Added host-JVM regression coverage for persistence, bookmarks/searches, operation history and interrupted-operation recovery.
- Verification is pending until the complete Android workflow has run successfully; no milestone completion is claimed from source changes alone.


### M19 CI build-fix verification checkpoint — 2026-11-16

- Implementation commit: `5fcdb23668db5a742929407a082507ebc41b0338`; follow-up corrections: `95bd0d176b423dcccc9de68d1cce497d0377a9f9`, `2aec9109a27c37e8290d5dd54d468990276830d7`, `ecb530dcde84701038000d740c9dcb5b8fe2e8bc`, and `b1ad85a95def00da17d19811dbb3d7803e1082d2`.
- The shared metadata store and host regression step are present on `main`. The project activity import declarations were corrected after source review; metadata replacement was hardened to use atomic move where supported and safe replacement fallback.
- Production workflow run [43850687677](https://github.com/xolomon-coder/DroidInspect/actions/runs/43850687677), attempt 1 and retry attempt 2, both failed before executing any job steps. The latest job ID `133242201075` reports `runner_id=0`, an empty step list, and no runner execution. This is an Actions runner-allocation/infrastructure failure; no host test, Android compile, APK signing, artifact, or source-failure conclusion can be inferred.
- The failed job was explicitly rerun. Continue retrying when runner capacity is available, and inspect every job and artifact on each attempt. Do not mark this commit CI-verified until the complete workflow succeeds.
- Device acceptance remains pending. This checkpoint records implementation and the CI blocker only; it is not an M19 completion claim.


### M19 operation tracking and project index expansion — 2026-11-16

- Added bounded project discovery for C# source and offline dependency projects. The index reads marker names, sorts by latest nested modification time, caps discovery at 500 projects / 2,000 visited files per project, and identifies incomplete projects instead of silently presenting them as ready.
- Added host regression coverage for project discovery, display names and incomplete-project state. The metadata persistence/recovery host regression and project-index host regression were each reproduced locally with Java 21 and passed.
- Connected project operation records to C# source saves, C# compilation, dependency imports/resolution, APK working-copy replacement, APK revision restore, APK export, notes, bookmarks, saved searches and report writes. Long-running operations leave a recovery marker if the process is interrupted; normal completion clears it. Invalid APK recovery selections are rejected before a marker is created.
- Latest implementation commits include `10fc73bd16ada851a905b1479c48f95a68af8b1c`, `a57676f91870630c5fc2b6c54fba9423b21fece0`, `e580d9477dfcf5832d9f6573da7dd43295e4bcec`, `7adec561b037ccb3c22e9f99e0df49da15520af4`, `8e2d46c5cee11f7470e4430ddb56915426cd903b`, and `d9bbe5ff197293e184415bf39b6f99fd19c72db9`.
- Production Android workflow run [43851747914](https://github.com/xolomon-coder/DroidInspect/actions/runs/43851747914) failed on attempts 1 and 2 before executing any steps. Both attempts report `runner_id=0`, empty steps, and no artifacts. The workflow was explicitly rerun; the runner-allocation failure remains external to the source changes. Keep the latest production commit unverified until a runner executes all host tests and Android build/signing/artifact steps successfully.
- M19 remains IN PROGRESS. The unified launcher, project metadata tools, bounded index, and operation journal are implemented, but complete Android CI and final on-device close/reopen/recovery acceptance remain pending.


### M19 storage-isolation correction — 2026-11-16

- The shared .NET/APK workspace metadata store now writes its own `workspace-bookmarks.txt` and `workspace-saved-searches.txt` files. This deliberately avoids corrupting the APK workspace's existing Base64-encoded `bookmarks.txt` and `saved-searches.txt` record format.
- Recovery completion now records the FINISHED history event before clearing the interrupted-operation marker, so a history-write failure cannot silently erase the recovery notice.
- The project list explains tap-to-open and long-press metadata management.
- Storage isolation and metadata/recovery regression were checked locally; the project-index host regression also passed locally. These local checks do not replace the required full GitHub Actions workflow.
- Earlier M19 implementation checkpoint: `2d9cd652d48ba8821d9ec7d493c8831e91fa6bb2`. Production CI run [43852103192](https://github.com/xolomon-coder/DroidInspect/actions/runs/43852103192) failed on attempts 1 and 2 with `runner_id=0`, no steps, and no artifacts. The failure is runner allocation; source compilation has not yet been attempted by GitHub Actions.


### M19 latest-main and CI retry checkpoint — 2026-11-16

- Latest main commit at this checkpoint: `d14ee4946552c6cb78070b35f1c67d2f46f5203a`. It includes the M19 workspace-action wording update and the storage-isolation / recovery-history correction documented above.
- Workflow run [43852294365](https://github.com/xolomon-coder/DroidInspect/actions/runs/43852294365) targeted this exact commit. Attempts 1, 2 and 3 all failed before a runner started. The final attempt's job `133247811273` has `runner_id=0`, an empty step list and no logs; the run produced zero artifacts.
- This is conclusively a runner-allocation failure for this run, not a source-level build failure. No further immediate retries will be issued against the same unchanged commit; doing so has repeatedly produced the same no-runner result. Resume CI verification after a new runner becomes available or a fresh run can be allocated.
- The local Java host checks recorded above passed, but they do not establish Android compilation or full workflow success. M19 remains IN PROGRESS and must not be marked complete until production CI executes all required steps and device acceptance is completed.


### Combined M19–M20 integration work — 2026-11-16

M19 and M20 are being completed as one final integration phase, while retaining separate exit criteria.

**M19 workspace refinement**
- Assembly Explorer bookmarks are now persisted per assembly content identity (SHA-256), rather than in one global SharedPreferences set. Different assemblies therefore cannot mix metadata-token bookmarks; bookmarks survive closing and reopening the Explorer and remain private to the app.
- Added bounded token validation, a 5,000-bookmark cap, atomic file replacement, and a host-JVM regression test for persistence, isolation, removal and invalid-token rejection.
- Updated .github/workflows/phase1-android.yml to execute the new bookmark-store host regression.
- This supplements the unified APK/C#/.NET project index and shared notes/bookmarks/search/history/recovery records. It does not yet establish full close/reopen acceptance on Android, and interrupted-operation notices are recovery records rather than automatic rollback snapshots.

**M20 integration status**
- Existing safety boundaries remain: bounded APK/PE inputs, background workers for assembly indexing and dependency resolution, separate working/output files, offline-only production permissions, and ARM64 native payload checks in CI.
- Remaining M20 work includes a systematic large-input/low-memory review, cancellation/lifecycle review for long-running operations, Android/ABI compatibility and CodeAssist reproducibility evidence, licensing/dependency-size audit, and final cross-workflow regression.
- The production CI workflow for commit 67665bdec726dba5e2998b8c9898459a5459f933 (run https://github.com/xolomon-coder/DroidInspect/actions/runs/43863286380) again failed before runner allocation: job 133285312446 has runner_id=0, no steps and no runner logs. No host tests or Android build ran. This is an Actions infrastructure failure, not a source-level pass or failure.
- M19 and M20 remain IN PROGRESS. Do not begin the consolidated phone acceptance session until the final implementation and a complete production CI run are available. Android device acceptance is necessarily a final user-device gate and cannot be truthfully claimed by cloud source changes.


### Latest main checkpoint and new-chat handoff — 2026-11-16

**Repository state checked before this note**
- Branch: `main` only; no alternate development branch is to be created.
- HEAD: `db04a0c228d8813fe442f99283a110b73f3b40dc` — “Document combined M19 M20 integration checkpoint”.
- The M12 editor XML correction from `android:hintTextColor` to `android:textColorHint` is present in the current source.
- The production manifest remains offline-only; do not add Internet permission without explicit user approval.

**Milestone position**
- M11: accepted by the user; preserve it and do not repeat its device test unnecessarily.
- M12: production C# source workspace, offline Roslyn compiler wiring, separate rebuilt DLL output, original-assembly preservation and structural compatibility report are implemented. Cloud proof runs are recorded above. Remaining limitations include the single bundled .NET 11 reference profile, unresolved Native AOT trim-analysis warnings (IL2091, IL2050, IL2072), and final Android-device compiler execution. Do not claim universal .NET project recompilation or runtime replacement compatibility.
- M13: offline dependency resolver and exact-identity/framework warning behavior are implemented with host coverage. The bundled framework profile remains .NET 11.0; additional framework packs and device acceptance remain pending.
- M14/M15: namespace/type navigation and the lightweight C# editor improvements are implemented. Advanced cross-reference coverage, tabs and device usability still need review/acceptance.
- M16: structural assembly comparison and bounded embedded-resource extraction are implemented with host tests. It is not semantic equivalence; linked-resource export and full human-readable signature decoding remain limitations.
- M17: implementation complete for the supported scope, with host/CI evidence recorded above. Split APK inventory does not merge/install splits or produce a combined split graph; resource-to-code mapping and some DEX reference kinds are unsupported.
- M18: implementation complete for supported manifest/package/version and named string-resource edits, guided rebuild/sign/verify workflow and conservative update compatibility assessment. DEX bytecode editing is not supported; device acceptance remains pending.
- M19: unified APK/.NET project launcher, shared project metadata, bounded project discovery, notes/bookmarks/saved searches, operation history/recovery markers and assembly-scoped bookmarks are implemented. Host checks are recorded as local passes, but the latest complete production Android CI has not been verified. Final Android close/reopen/recovery acceptance remains pending.
- M20: final integration and hardening is still in progress. Complete the systematic large-input/low-memory, cancellation/lifecycle, Android/ABI and CodeAssist reproducibility, licensing/dependency-size, permissions and regression audits. Keep unsupported cases explicit.

**Latest production Android CI state**
- Run [43863445524](https://github.com/xolomon-coder/DroidInspect/actions/runs/43863445524) targets HEAD `db04a0c228d8813fe442f99283a110b73f3b40dc` and is marked `failure`.
- The GitHub Actions job listing returned a completed failed job but no step details (`steps: null`). Therefore the available record does not establish whether the runner started or whether source compilation was attempted. Do not label this as a source build failure, runner-allocation failure, or PASS without retrieving job details/logs.
- Earlier runs in this M19/M20 batch also show repeated failures before useful step evidence was available. Do not infer a successful Android build from the older M17/M18 green runs; those predate the current M19/M20 changes.
- Immediate next action: retrieve the detailed job/run record and logs for run `43863445524`. If no runner executed the job, wait for runner capacity and trigger a fresh main-branch run. If steps did execute, diagnose the first real failing step, fix it on `main`, and rerun the full required workflow. Inspect every job and artifact; never stop after merely triggering Actions.

**Required continuation for the next chat**
1. Re-fetch the current `main` HEAD and read this README before editing; the commit above is a checkpoint, not a guarantee that main has not advanced.
2. Continue M19/M20 from the existing implementation. Do not recreate completed milestones, restart M11, or begin a new branch.
3. Finish the M19 project-state persistence/reopen/recovery tests and M20 reliability, compatibility, licensing, permissions, payload-size and performance audits. Preserve original APKs/assemblies and all existing production features.
4. Resolve and obtain a successful full production Android CI run against the final source, including host regressions, debug/release Android builds, signing/tamper verification, native ABI/payload checks, offline permission audit and artifact verification.
5. Do not start the consolidated phone test until implementation and cloud verification are complete. Then provide one consolidated device test plan covering the remaining M12–M20 acceptance points; the user prefers to test once at the end.
6. On final completion, update README with exact commit, all CI run IDs/conclusions, artifacts and hashes, supported limitations, exact files/changes, and the stopping point. Deliver the GitHub `main.zip` and concise CodeAssist build/install instructions. Do not use a temporary APK artifact as the normal delivery.

**Current stopping point:** M19/M20 integration and CI verification. Next work is to inspect the detailed failure state for run `43863445524`, then continue the M19/M20 verification/fix loop. No consolidated device acceptance or final release PASS is claimed.


---

### M19–M20 continuation — persistence tests, lifecycle hardening and CI audit expansion (2026-11-16)

**Latest source checkpoint**
- Branch: `main`.
- Latest source commit before this README update: `5fbdd872fa4cfcb5f9c2a314f0f36a66c5d16ad4`.
- Production package remains `com.tanvir.droidinspect`.
- No alternate branch was created.

**M19 persistence regression expansion**
- Updated `tools/project-workspace-test/ProjectWorkspaceMetadataSelfTest.java`.
- The host regression now recreates the metadata-store object before reading records, verifies that notes/bookmarks/searches/recovery markers persist across store recreation, and verifies that two project roots do not share notes, bookmarks, saved searches or interrupted-operation state.
- It checks that STARTED and FINISHED events are recorded and that the recovery marker is removed only after completion is recorded.
- This test is added to the existing production host-test workflow. It has not yet executed in CI because the current Actions jobs did not obtain a runner.

**M20 background lifecycle hardening**
Updated production Java files:
- `AssemblyExplorerActivity.java`: background worker is interrupted on Activity destruction and its UI completion callback checks Activity lifecycle state.
- `CSharpSourceEditorActivity.java`: compiler completion is prevented from updating a destroyed editor; the compile operation can continue in the background to finish its output and operation journal. The native compiler itself does not expose a guaranteed immediate cancellation contract.
- `DotNetDependencyActivity.java`: dependency-project creation now uses the Activity's managed executor; UI callbacks are guarded and the executor is interrupted on destruction.
- `ApkBuildPipelineActivity.java`: all background UI completion callbacks check that the Activity is still alive; the worker is interrupted on destruction.
- `ApkProjectWorkspaceActivity.java`: all project-operation callbacks check Activity lifecycle state; the worker is interrupted on destruction.
- These are source-level lifecycle corrections. They are not Android runtime/device verification.

**M20 CI audit expansion**
Updated `.github/workflows/phase1-android.yml`:
- Captures the resolved Gradle `releaseRuntimeClasspath` dependency graph for later licensing review.
- Checks both debug and unsigned release APK outputs exist and pass ZIP integrity checks.
- Checks the required ARM64 native libraries are present in both APK variants.
- Checks the source and packaged manifests remain free of Internet permission.
- Records the native shared-library inventory and enforces a 100 MiB source native-payload ceiling.
- Records SHA-256 hashes for debug, unsigned release and CI-signed APK outputs.
- Uploads the dependency graph, native inventory and checksum manifest with the APK verification artifacts.
- This expanded workflow has not yet executed; its checks and artifacts remain pending CI runner availability.

**Static M20 audit findings**
- Android configuration: `minSdk 26`, `compileSdk 36`, `targetSdk 36`, Java 8 source/target; Gradle Android plugin 8.13.0 and Gradle 8.13 are configured.
- Native packaging currently contains ARM64 only. The four production shared libraries total **81,306,496 bytes (77.54 MiB)** before APK packaging: decompiler engine 45,966,680 bytes, compiler engine 35,316,984 bytes, and the two JNI shims 11,032 and 11,800 bytes.
- `DotNetIlReader` uses random-access PE reads, a 512 MiB file ceiling, 256 KiB method-body ceiling, 4 KiB editable-IL ceiling, and bounded method/instruction display counts rather than loading an entire assembly into one byte array.
- APK and .NET input pipelines retain explicit size bounds and separate working/output copies. The Android manifest contains no declared Internet permission.
- The repository contains `THIRD-PARTY-NOTICES.md`. That notice records MIT license text for ILSpy and .NET Native AOT/reference components, and explicitly leaves the exact transitive Native AOT/NuGet dependency graph review open. ARSCLib 1.4.0 and apksig 8.13.0 are documented in the project dependency notes as Apache-2.0.
- Static source/configuration review does not establish real-device peak memory, low-storage behavior, thermal performance, Android lifecycle behavior, or CodeAssist reproducibility. These remain final acceptance tasks.

**Production CI evidence and runner blocker**
- Runs `43863445524` (old documented checkpoint) and `43865488417` (README checkpoint) both failed with no executable job steps and no artifacts.
- Rerunning `43865488417` produced attempt 2, which again had no steps or artifacts.
- The expanded M19 host-test commit `832039f0ada178563085cbf13d3e4b6f7e7c5860` triggered run `43866160965`; its job also had no steps and no artifacts.
- Lifecycle and workflow-audit commits triggered run `43866734077` against `5fbdd872fa4cfcb5f9c2a314f0f36a66c5d16ad4`. The raw GitHub job record confirms `runner_id=0`, blank runner name, empty `steps`, completion in approximately two seconds, and no artifacts. This is direct evidence that the job did not execute on a runner; it is not evidence of a source compilation or host-test failure.
- No current M19/M20 production Android build, signing/tamper verification, host-test execution or APK hash has been proven by these runs. Older successful M17/M18 runs predate the current source and do not satisfy this gate.

**Exact changed paths in this continuation**
- `tools/project-workspace-test/ProjectWorkspaceMetadataSelfTest.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/AssemblyExplorerActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/CSharpSourceEditorActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetDependencyActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkBuildPipelineActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkProjectWorkspaceActivity.java`
- `.github/workflows/phase1-android.yml`
- `README.md` (this checkpoint)

**Current milestone status**
- M19: implementation and host regression coverage expanded; CI execution and actual Android project close/reopen/recovery acceptance remain pending.
- M20: lifecycle guards and CI audit coverage expanded; CI execution, complete transitive license review, low-memory/performance evidence, and final regression remain pending.
- M12–M18 limitations listed above remain unchanged. Do not repeat M11 acceptance.
- Do not start the consolidated device-testing session until a complete production CI run succeeds. Then test all remaining M12–M20 device acceptance points once, in one consolidated session.

**Next exact action**
1. Re-fetch `main` HEAD and inspect the latest run for that exact SHA.
2. Retry the full workflow when a GitHub-hosted runner can be allocated. For every attempt, inspect raw job details, all steps/logs and artifacts. A failed run with `runner_id=0` is an infrastructure allocation failure; a run with executed steps must be diagnosed from its actual first failing step.
3. If source/test/build failures occur, fix them directly on `main`, update this README and rerun the complete workflow.
4. Once CI succeeds, inspect the dependency graph, APK outputs, ABI inventory, permissions, signature/tamper checks and SHA-256 manifest. Complete the remaining licensing and performance review before final device handoff.
5. Only then provide the latest `main.zip`, exact CodeAssist build/install instructions and one consolidated M12–M20 device test plan.

**Stopping point:** M19/M20 source hardening and CI audit expansion are committed; production CI remains blocked before runner execution. No final release PASS or device acceptance is claimed.


### M20 privacy and CI runner retry checkpoint — 2026-11-16

**Latest main before this note:** `6b0ef5b9f0f64243b758b5db6892298b4f70d4ca`.

Additional M20 changes since the previous checkpoint:
- `droidinspect/app/src/main/AndroidManifest.xml`: changed `android:allowBackup` from `true` to `false`. DroidInspect stores user-selected APKs, assemblies, source and dependency files in app-private storage; disabling Android automatic backup reduces the risk of those potentially proprietary inputs being copied into device/cloud backup. No Internet permission was added.
- `.github/workflows/phase1-android.yml`: the production manifest audit now also requires `android:allowBackup="false"`.

**Runner retry evidence**
- Run `43866954568`, attempt 2, failed with `runner_id=0`, blank runner name, empty steps and no artifacts.
- Run `43867085126`, attempt 2, failed with `runner_id=0`, blank runner name, empty steps and no artifacts.
- Run `43867134813` targets `6b0ef5b9f0f64243b758b5db6892298b4f70d4ca`; its first attempt also failed with `runner_id=0`, blank runner name and empty steps.
- These raw job records confirm the jobs were not assigned to a GitHub-hosted runner. No host test, Android compilation, signing/tamper check or artifact generation took place in these attempts. The workflow itself still requires an actual runner execution.

**Current M19/M20 state**
- M19: project metadata isolation/reopen regression test is expanded and committed. Full host execution, Android build, app-level project reopen/recovery and device acceptance remain unverified.
- M20: background callback guards, managed dependency-workspace executor, backup privacy setting, and CI dependency/native-payload/ABI/permission/checksum audits are committed. Static inspection confirms four production ARM64 native libraries total 81,306,496 bytes (77.54 MiB); the workflow now enforces a 100 MiB ceiling. Real build output and artifact hashes remain unavailable until CI runs.
- Remaining release blockers: obtain a runner-backed green production workflow; inspect the emitted resolved dependency graph and finish the transitive licensing/notice review; confirm debug/release APK signatures, tamper rejection, native ABI inventory and checksum artifacts; review actual low-memory and large-input behavior; and complete the final Android device session.
- CodeAssist reproducibility and device acceptance cannot be inferred from GitHub source or a runner-only build.

**Next exact action:** re-fetch `main`, inspect the workflow run associated with that exact commit, and retry the full production workflow when runner capacity is available. If a runner starts, inspect every step and log and fix the first real source/workflow failure on `main`. Do not mark M19/M20 complete or ask for the consolidated phone test until all cloud verification and remaining release audits are complete.

**Stopping point:** privacy and CI audit hardening committed; GitHub Actions runner allocation remains the blocking infrastructure issue. No production CI PASS, final APK hash, final release readiness or consolidated device acceptance is claimed.


### M20 transitive NuGet dependency inventory — 2026-11-16

- Extended `.github/workflows/m11-nativeaot-poc.yml` to restore the ILSpy Native AOT engine for `linux-bionic-arm64`, capture `dotnet list package --include-transitive`, and publish `artifacts/m11-nuget-transitive-dependencies.txt`.
- Extended `.github/workflows/m12-roslyn-compiler-poc.yml` to restore the Roslyn Native AOT compiler for `linux-bionic-arm64`, capture its transitive package graph, and publish `artifacts/m12-nuget-transitive-dependencies.txt`.
- These inventories provide exact package names and versions as inputs to the remaining license review. They do not themselves establish each package's license expression or complete legal compliance; that review must be performed against the produced graphs and corresponding package metadata.
- Latest source commit before this README update: `4d81991d55cd4ea189cae1b2cbd470688dfb239d`.
- Main Android CI run `43867535926` and M12 Roslyn proof run `43867535971`, including attempt 2 of each, both failed before runner assignment. Raw job records show `runner_id=0`, empty steps and no runner name. The M12 package graph step has therefore not executed yet. No source/test/Native AOT failure is established by these runs.
- The M11 workflow's new package-graph step is committed, but a matching run for this commit was not returned by the Actions query; do not claim that graph was generated.
- M19/M20 remain IN PROGRESS. The production workflow and both native proof workflows still require runner-backed execution; full production CI and final device acceptance remain release gates.


### Latest Actions retry — 2026-11-16

- Production Android CI run `43867674091` targets commit `e975020161e6651755488b3cccb5c21c48b593b4`. Attempt 1 and retry attempt 2 both ended with `runner_id=0`, blank runner name and empty steps; no artifacts were produced.
- M12 Native AOT proof run `43867535971`, including retry attempt 2, has the same no-runner result. Its newly added transitive NuGet graph command has not executed.
- The M11 workflow file now contains the corresponding ILSpy/.NET Native AOT package-graph capture and artifact upload step, but no matching M11 workflow run was observed in the queried run set; do not claim that its dependency inventory was generated.
- These results add no source compilation, host-test, native publish, APK build, signing or licensing pass evidence. The runner allocation issue remains the active blocker.


### Production CI retry after dependency-audit changes — 2026-11-16

- Latest observed production run: `43867779547`, targeting commit `433dfb8af0b631f03acd7d7ba75247cbd776cd5f`.
- Attempt 1 and attempt 2 both completed with failure before execution. Raw job data for attempt 2 reports `runner_id=0`, blank runner name, empty steps and no artifact output.
- The main workflow's M19/M20 host tests, Android debug/release builds, signing/tamper checks, native payload checks and checksum generation therefore remain unexecuted for this source state.
- The M12 proof workflow retry `43867535971` also failed before runner allocation. The M11/M12 transitive package graph artifacts have not yet been generated.
- No CI PASS or final release status is claimed. Continue with runner-backed verification when GitHub Actions allocates a runner; keep all work on `main`.


### M19–M20 source-tree bounds and CI retry checkpoint — 2026-11-16

**Latest source change**
- Added `BoundedSourceTree.java` and routed C# project source discovery through it.
- C# source enumeration now enforces a maximum of 2,000 source files, 10,000 visited filesystem entries, 16 directory levels, 32 MiB per source file, and 128 MiB total source bytes. Canonical-path validation rejects source entries that resolve outside the project root.
- C# source reads and writes now enforce the existing 32 MiB per-file limit before materializing or replacing source content. The original assembly remains separately stored and unchanged by source editing.
- Added `tools/bounded-source-tree-test/BoundedSourceTreeSelfTest.java` covering normal nested discovery, oversized source rejection and excessive nesting rejection.
- Added the new host test to `.github/workflows/phase1-android.yml` before the DEX parser regression.

**Exact changed paths**
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/CSharpSourceWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/BoundedSourceTree.java` (new)
- `tools/bounded-source-tree-test/BoundedSourceTreeSelfTest.java` (new)
- `.github/workflows/phase1-android.yml`

**Local test evidence**
- The bounded source-tree regression was compiled with `javac` and executed in the available host container.
- Result: `PASS: bounded C# source discovery, nested files, file-size and depth limits`.
- This is host-side evidence for the new scanner/test only. It is not a complete repository host-test pass, Android Gradle build, APK integrity/signing pass, or device acceptance.

**Production Actions evidence**
- Source/workflow commit: `bc9a5917f745a7c012e2d3ea94d2f7b9665b2fa1`.
- Production run [43915982432](https://github.com/xolomon-coder/DroidInspect/actions/runs/43915982432), attempt 1, targets that commit. Raw job ID `133470117181` reports `runner_id=0`, blank runner name, an empty step list, and no artifacts. This is a runner-allocation failure; none of the host-test or Android build steps executed.
- A retry of the failed job was requested. Its result must be inspected before making any CI claim.
- The immediately preceding run [43867895539](https://github.com/xolomon-coder/DroidInspect/actions/runs/43867895539), attempt 3, also had `runner_id=0`, no steps and no artifacts.

**Milestone state**
- M19 remains IN PROGRESS: metadata persistence/isolation and project indexing tests are present, but the complete CI host suite and APK/C#/dependency close-reopen/recovery acceptance remain outstanding.
- M20 remains IN PROGRESS: the source-tree bounds and earlier lifecycle, offline, native payload and artifact audits are implemented, but the full runner-backed build/audit, transitive license review, low-memory/large-file device evidence, CodeAssist reproducibility and regression are not yet verified.
- No production CI PASS, final APK hash, final release readiness or M19/M20 completion is claimed.

**Next action**
1. Inspect the retry of run `43915982432` and every job's raw runner assignment, steps, logs and artifacts.
2. If a runner executes, diagnose and fix any real test/build failure on `main`, then rerun the complete production workflow.
3. If runner allocation again fails before steps, retain that as an infrastructure blocker; do not misreport it as a source failure or repeatedly claim a retry is verification.
4. Continue the M20 dependency/license and APK/.NET regression audit. Only after full cloud verification should the single consolidated M12–M20 Android device session be requested.

**Stopping point:** C# source workspace file/depth/total-size bounds and a host regression are committed; production CI runner allocation is still unverified.


### Bounded source-tree regression expansion and latest runner result — 2026-11-16

- Expanded `BoundedSourceTreeSelfTest` to cover the 2,000-file cap and 128 MiB aggregate source cap in addition to normal nested discovery, the 32 MiB per-file cap and the 16-level depth cap.
- Recompiled and ran the expanded host regression in the available container. Result: `PASS: bounded C# source discovery, nested files, per-file, total-size, file-count and depth limits`.
- Latest implementation/test commit: `7e1410a31103049791ab52a15019376acc51ae9c`.
- Production workflow run [43916234501](https://github.com/xolomon-coder/DroidInspect/actions/runs/43916234501), attempts 1 and 2, both failed before any step executed. Raw job IDs `133471022548` and `133471249052` both report `runner_id=0`, blank runner name and empty steps. The run produced no artifacts. A source compilation or test failure has not occurred in this run because no runner executed the job.
- Retry of the earlier source commit run [43915982432](https://github.com/xolomon-coder/DroidInspect/actions/runs/43915982432), attempt 2, likewise returned `runner_id=0`, no steps and no artifacts.
- Licensing review remains incomplete: the checked-in notices identify direct ILSpy, .NET runtime/reference pack and Roslyn MIT notices, and the Android Gradle direct dependencies ARSCLib 1.4.0 and apksig 8.13.0 as Apache-2.0. The exact transitive NuGet graphs and resolved Gradle dependency graph have not been produced by runner-backed workflows, so complete redistribution compliance cannot yet be concluded.
- The production manifest was rechecked: `android:allowBackup="false"` is present and no Internet permission is declared. This is a source-manifest observation, not packaged APK verification.
- M19/M20 remain IN PROGRESS. The new bounded source-tree test has a local host PASS, but the full host suite, Android debug/release build, APK signing/tamper verification, native packaged-payload checks, transitive licensing review and device acceptance remain unverified.


### M19 saved-project reopen integrity checks — 2026-11-16

- Hardened `CSharpSourceWorkspace.open()`: a saved C# project is now rejected if its source directory or preserved `original/OriginalAssembly.bin` is missing, empty, or larger than the 512 MiB original-assembly limit. The last-project shortcut is enabled only when these required files are valid.
- Hardened `DotNetDependencyWorkspace.open()`: a dependency project is rejected if its root assembly is missing, empty, or over 512 MiB, or if its local-dependency directory is missing. The last-project shortcut also checks these conditions.
- These checks prevent incomplete workspace folders from appearing reopenable merely because a marker file exists. They are source-level validation changes; Android close/reopen acceptance remains pending.
- Exact commits: `f433ea26b7f81bbbf6f88531daf4b5ad3afead71` (C# source project) and `76ca872d74b877646a57275cd1897e17062651bd` (.NET dependency project).
- Production run [43916562562](https://github.com/xolomon-coder/DroidInspect/actions/runs/43916562562), attempt 1, targets the dependency-project validation commit. Job `133472163216` reports `runner_id=0`, blank runner name and no steps; there are no artifacts. This remains an Actions runner-allocation failure, not a compilation/test result. A retry was requested and still needs inspection.
- M19 and M20 remain IN PROGRESS. Do not claim project reopen acceptance or production CI success until runner-backed and device evidence exists.


### Final M19/M20 continuation checkpoint — current main and runner retries (2026-11-16)

- Current implementation changes on `main` include bounded C# source-tree discovery/read/write limits, host regression coverage for per-file/aggregate-size/file-count/depth limits, and strict reopen validation for preserved C# original assemblies and .NET dependency root assemblies.
- The expanded bounded-source-tree host test passed locally in the host container. The complete repository host suite has not run in this environment.
- Latest production run [43916687315](https://github.com/xolomon-coder/DroidInspect/actions/runs/43916687315), targeting the prior README checkpoint, failed before execution. Raw job `133472609738` reports `runner_id=0`, no runner name, no steps and no artifacts.
- Retrying production run [43916562562](https://github.com/xolomon-coder/DroidInspect/actions/runs/43916562562) produced attempt 2 with job `133472381618`; it again reports `runner_id=0`, no steps and no artifacts.
- The M12 Roslyn Native AOT proof retry [43867535971](https://github.com/xolomon-coder/DroidInspect/actions/runs/43867535971), attempt 3, also failed before runner assignment (job `133472752703`, `runner_id=0`, empty steps, no artifacts). Its transitive NuGet graph and native compiler proof have not been generated by that run.
- This repeated evidence establishes a persistent Actions runner-allocation blocker for these attempts. It does not establish source compilation or host-test failure. No APK build, signing/tamper test, packaged ABI check, dependency graph capture or CI checksum was executed.
- Latest source changes are on `main`; no alternate branch or pull request was created. The production manifest remains source-verified offline with backup disabled.
- **Release gate remains closed.** M19/M20 are not complete. Continue source-level audits that can be performed safely, but do not claim full cloud verification, transitive license compliance, device acceptance or release readiness until a runner-backed production workflow completes and the remaining audits are evidenced.


### CI retry and documentation-trigger refinement — 2026-11-16

- The production workflow now ignores README-only pushes and pull requests, so documentation corrections do not consume a build attempt. Source, workflow, Gradle, native and test changes still trigger production CI; manual `workflow_dispatch` remains enabled.
- Workflow change commit: `dd6acc429096541d8402cffb18e3e842ec7797d0`.
- Production run [43916937917](https://github.com/xolomon-coder/DroidInspect/actions/runs/43916937917), attempt 1, targets that workflow change. Job `133473488446` again failed before execution with `runner_id=0`, blank runner name and no steps; no artifacts were produced.
- Production run [43916807457](https://github.com/xolomon-coder/DroidInspect/actions/runs/43916807457), attempt 2, likewise failed before runner allocation (job `133473191905`, `runner_id=0`, empty steps).
- The latest observed Actions attempts still provide no host-test, Android build, APK signing/tamper, native payload, permission packaging, dependency graph or checksum evidence. Do not infer any source failure from these no-runner records.
- The new workflow path filter is only a CI-efficiency change. It does not resolve runner allocation. M19/M20 remain IN PROGRESS and release readiness remains blocked on a real runner-backed production workflow.
- Latest source/workflow checkpoint before this documentation-only update: `dd6acc429096541d8402cffb18e3e842ec7797d0`. The following README-only commit is intentionally excluded from production build triggers.

### M19 bounded project-discovery correction and host regression — 2026-11-17

**Starting source HEAD:** `f39407f68e84efc6ee7cfa44a16ca5cdeeec4041`.

- Code review found that `ProjectWorkspaceIndex.scan()` capped the number of valid projects but incremented its scan counter only after finding a valid project marker. A directory containing many unrelated folders could therefore require an unbounded root-directory scan.
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ProjectWorkspaceIndex.java` now enforces `MAX_SCANNED_ROOT_ENTRIES = 2000` across every root entry examined, in addition to the existing 500-project result cap and bounded per-project recency traversal.
- `tools/project-workspace-index-test/ProjectWorkspaceIndexSelfTest.java` now creates more than 2,000 unrelated root entries and verifies that a project placed beyond the scan budget is not discovered. Existing marker-name, READY/INCOMPLETE and recency assertions remain.
- Exact implementation commit: `4f9fbabec4ed7fb910aa8f86c177abd66d2d76b4`.
- Exact regression-test commit: `f39407f68e84efc6ee7cfa44a16ca5cdeeec4041`.

**Host-side test evidence (executed in the available host container using the exact committed Java source and test files):**
- `ProjectWorkspaceIndexSelfTest`: PASS — project discovery, marker names, readiness, recency and root-entry scan bound.
- `ProjectWorkspaceMetadataSelfTest`: PASS — metadata persistence across store recreation, project isolation, notes, bookmarks, saved searches, interrupted-operation notice, operation journal and recovery-marker clearing.
- These are two focused host tests, not a claim that the complete production workflow or all host regressions passed.

**Production Actions retry evidence:**
- Production run [43969379323](https://github.com/xolomon-coder/DroidInspect/actions/runs/43969379323) targeted implementation commit `4f9fbabec4ed7fb910aa8f86c177abd66d2d76b4`; its job ended before execution with `runner_id=0`, blank runner name and no steps.
- Production run [43969408209](https://github.com/xolomon-coder/DroidInspect/actions/runs/43969408209) targets test commit `f39407f68e84efc6ee7cfa44a16ca5cdeeec4041`. Attempt 1 and the requested retry (attempt 2) both failed before runner assignment; the latest raw job again reports `runner_id=0`, blank runner name and an empty step list. No artifact was produced.
- These are infrastructure allocation failures. They do not indicate a failing Java test or Android build, because no workflow step ran. The runner-backed production CI gate remains open.

**M20 static audit snapshot (source inspection only):**
- Production manifest still declares no Internet permission and sets `android:allowBackup="false"`.
- The checked-in production ARM64 native library source inventory contains four libraries totalling 81,306,496 bytes (77.54 MiB). This is source-tree size, not measured APK-compressed or installed payload size.
- The production workflow is configured to build debug and release APKs, verify signing and tamper rejection, check packaged ARM64 libraries and offline permissions, capture the resolved Gradle release dependency graph, and publish SHA-256 records. Those steps have not executed on a runner for the current source.
- `THIRD-PARTY-NOTICES.md` includes direct ILSpy, .NET runtime/reference-pack and Roslyn MIT notices, and documents ARSCLib 1.4.0 and apksig 8.13.0 as Apache-2.0. Complete transitive NuGet/Gradle licensing review remains pending the actual restore/dependency-graph artifacts; direct notices are not a completed transitive audit.

**Milestone state and next action:**
- M19 remains IN PROGRESS. The metadata and index host tests now have focused local PASS evidence. Android APK project reopen/recovery, C# source project reopen, dependency project reopen, full project indexing and real app-level persistence still require Android/device or runner-backed verification.
- M20 remains IN PROGRESS. No production CI PASS, debug/release APK verification, final APK hash, complete transitive licensing conclusion, low-memory/large-file device result, CodeAssist reproducibility result or release readiness is claimed.
- Retry production CI when runner allocation is available. On the first runner-backed execution, inspect all host test steps and logs, debug/release outputs, signing/tamper checks, packaged native payload, dependency graph, notices and SHA-256 artifacts. Fix any actual source or workflow failure on `main`.
- Do not request the consolidated M12–M20 phone acceptance session until runner-backed production verification and the remaining release audits are complete.

**Stopping point:** The M19 root-directory scan is now genuinely bounded and both focused M19 host regressions pass locally. GitHub Actions still cannot allocate a runner; M19/M20 and release readiness remain unaccepted.

### M19/M20 reliability continuation — 2026-11-17

**Latest source checkpoint before this README-only update:** `4b5245533661153a638d8be366ebf57d2013d983`. The latest workflow change remains `96e6221fb538b264d2346a619ebc8e686b1a8ec0`. All source and workflow changes in this continuation were committed directly to `main`; this documentation update also targets `main`; no alternate branch was created.

**M19 bounded project discovery and metadata hardening:**
- Reworked `ProjectWorkspaceIndex.scan()` to use `DirectoryStream` and stop after examining at most 2,000 root entries, regardless of whether those entries contain valid project markers. The 500-project result cap remains. Per-project timestamp traversal remains limited to 2,000 visited entries and depth 5.
- Project-folder and recursive timestamp discovery now skip symbolic links. The required-path canonical containment check remains in place.
- Reworked workspace inventory display to scan at most 1,000 directory entries and display at most 100 files per inventory, rather than materializing an unbounded `File.listFiles()` array.
- Metadata reads now reject malformed UTF-8 instead of silently replacing invalid bytes. The 1 MiB per-file bound and canonical project-root containment are retained.
- Expanded index regression coverage to verify the 2,000-root-entry cutoff and exclusion of a symbolic-link project.
- Expanded metadata regression coverage for process/store recreation, project isolation, notes, bookmarks, saved searches, interrupted-operation recovery, bounded report listings, malformed UTF-8, oversized metadata, and symlink path escape.
- Focused host tests were run in the available container: index regression **PASS**; metadata persistence/recovery and corruption-boundary regression **PASS**. These results do not establish Android UI/device acceptance or a runner-backed CI pass.

**M20 lifecycle and packaged-payload hardening:**
- `ApkResourceActivity` now checks worker interruption during APK copy, resource traversal and export loops; guards UI callbacks after activity destruction; and defers closing `ApkModule` / deleting working files until the worker has terminated, using a daemon cleanup thread rather than blocking the UI thread.
- `ApkBuildPipelineActivity` now checks cancellation during both APK copy paths, discards a temporary original copy if its callback arrives after destruction, and waits for worker termination before deleting the retained original copy.
- `DotNetIlActivity` now guards post-destruction UI callbacks, checks interruption while copying/exporting assemblies, and defers source/pending-output cleanup until its worker terminates.
- `DotNetResourceActivity` now guards post-destruction callbacks, checks interruption during the assembly copy, and defers assembly-copy cleanup / pending-byte release until its worker terminates.
- `ApkSigningActivity` now checks cancellation during APK/keystore copies and signed-output export, guards callbacks, and defers temporary APK/keystore cleanup until its worker terminates.
- `ApkSplitSetActivity` now checks cancellation during split-file copy, manifest parsing and ZIP inventory, guards callbacks, and defers copied-file cleanup until its worker terminates.
- `AssemblyExplorerActivity` now defers source-copy deletion until its indexing worker exits and checks cancellation during assembly copy.
- `ApkResourceCatalogActivity` now checks cancellation during APK copy/resource enumeration, guards callbacks, and defers cache cleanup.
- `ApkComponentExplorerActivity` now checks cancellation during APK copy/manifest parsing, guards callbacks, and defers cache cleanup.
- `AssemblyComparisonActivity` now checks cancellation between inspection stages and during input copy, and suppresses callbacks after destruction; its temporary copies remain cleaned in the worker's `finally` block.
- `DotNetDependencyActivity` now checks cancellation while reading saved reports and exporting reports; its UI callbacks already check activity lifecycle state.
- `.github/workflows/phase1-android.yml` now inspects the actual debug and release APK ZIP entries, verifies that the packaged native libraries are ARM64-only and include all four expected libraries, measures compressed and uncompressed native payload bytes and APK file size, enforces a 100 MiB uncompressed native payload ceiling, and publishes `packaged-native-inventory.txt` as an artifact.
- Existing input bounds reviewed during this pass include 512 MiB for individual APK/managed-assembly imports, 16 MiB for imported signing keystores, 512 MiB per split APK with a 1 GiB total split-set cap, 128 MiB for one extracted embedded .NET resource, and 1 MiB per workspace metadata file. Device-level peak-memory/performance validation remains pending.
- These M20 changes are implementation/workflow hardening only. They have not yet been validated by a successful Android build or runner-backed Actions execution. Other background activities still require a complete lifecycle/cancellation audit.

**Files changed in this continuation:**
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ProjectWorkspaceIndex.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ProjectWorkspaceMetadata.java`
- `tools/project-workspace-index-test/ProjectWorkspaceIndexSelfTest.java`
- `tools/project-workspace-test/ProjectWorkspaceMetadataSelfTest.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkResourceActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkBuildPipelineActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetIlActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetResourceActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkSigningActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkSplitSetActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/AssemblyExplorerActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkResourceCatalogActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkComponentExplorerActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/AssemblyComparisonActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetDependencyActivity.java`
- `.github/workflows/phase1-android.yml`

**Commits in this continuation (oldest to newest):**
- `9d670ba0415774ba4b3c2c67cedd0dcb6bf7f51d` — bounded project index directory iteration and symlink-safe traversal.
- `7e8cdc02d8b3d41f6e966d5f8f2b5d892fcc4718` — deterministic root-budget regression.
- `e3e0dd9c48e2f45f6de631413a74e115f84b7332` — bounded metadata listings and strict UTF-8 decoding.
- `449b1360310b213f425c746b0f5132194be892e1` — corrupt, oversized and symlink metadata tests.
- `2a495bdfb0d7a216ea4190c860d0c669a279c4e3` — clarify bounded index behavior.
- `4c7f797176d1eaebf8c50e6e61da3e8654d1481b` — symlink project-index regression.
- `f020eb05219e523668459c9924d3e7596218c137` — initial APK resource worker cleanup lifecycle change.
- `79af5bb09b7e28a29bc25d2c545a2d3dc1b1d48f` — cancellation checks and lifecycle guards for APK resource operations.
- `ad16b86c933b432553168531c18be3a1a96b6df7` — overflow-safe worker termination wait.
- `01153352ac9abaffad0d3695f4fe23a02c5c0e53` — APK pipeline worker cleanup lifecycle change.
- `7a21234e0a2a08424a2fab90e1e91fef23a98de6` — cancellation checks in both APK pipeline copy paths.
- `96e6221fb538b264d2346a619ebc8e686b1a8ec0` — packaged native payload and ABI audit in production workflow.
- `0ead54627ffd17c6d858f0e0f28d988735d1dc72` and `ee71585b03328054c260247a59874757db5c2594` — IL editor cancellation and deferred cleanup.
- `0a97c550fc32b499c4f7397bde8f5ab827c29548` — managed resource activity lifecycle and cancellation hardening.
- `d479886888b6f5d3dffb343d2e8bba472cf9bd8d` — APK signing worker cancellation and deferred cleanup.
- `126da94834fac691bf1b5cc4210a59c167aacec5` — split APK worker cancellation and deferred cleanup.
- `77caec12c0852865a510d9df9eedc51e91219cde` — assembly comparison cancellation and lifecycle guards.
- `12155e51aab3457037e9c2fbaadc12a17e163334` and `4b5245533661153a638d8be366ebf57d2013d983` — dependency report export cancellation and UI-thread read correction.
- `79013720899ccd06f671cae27a9f72c0b81d5e23` — Assembly Explorer deferred working-copy cleanup.
- `8681cc45aba997818d1e13430820fb3b4f060469` — APK resource catalog cancellation and cleanup.
- `da83776b12c0316c3bc3a5711294a20e3610f891` — APK component explorer cancellation and cleanup.

**CI evidence and limitation:**
- The previously inspectable production attempts `43969379323` and `43969408209` ended in failure with no workflow steps and no artifacts. Their jobs did not produce a source compilation or test result; the handoff's runner-allocation diagnosis remains the best-supported explanation.
- Newer push-triggered runs for this source checkpoint are [run 43971206687](https://github.com/xolomon-coder/DroidInspect/actions/runs/43971206687) (APK signing lifecycle commit `d479886888b6f5d3dffb343d2e8bba472cf9bd8d`) and [run 43971236259](https://github.com/xolomon-coder/DroidInspect/actions/runs/43971236259) (split-analysis lifecycle commit `126da94834fac691bf1b5cc4210a59c167aacec5`). Both jobs completed as failures with `runner_id=0`, blank runner names, empty step lists, no downloadable logs, and no artifacts. These runs did not produce a compilation/test result; repeated retries are not justified until runner allocation recovers.
- The commit status endpoint returned no status records for the latest workflow change commit. The available commit-workflow listing only exposes pull-request-triggered runs, so it cannot confirm the push-triggered production run for this checkpoint.
- Subsequent lifecycle-hardening push runs [43971517596](https://github.com/xolomon-coder/DroidInspect/actions/runs/43971517596), [43971540403](https://github.com/xolomon-coder/DroidInspect/actions/runs/43971540403), [43971620363](https://github.com/xolomon-coder/DroidInspect/actions/runs/43971620363), [43971637437](https://github.com/xolomon-coder/DroidInspect/actions/runs/43971637437), and [43971649984](https://github.com/xolomon-coder/DroidInspect/actions/runs/43971649984), plus the latest [run 43971793335](https://github.com/xolomon-coder/DroidInspect/actions/runs/43971793335) (run #581, source commit `4b5245533661153a638d8be366ebf57d2013d983`), likewise report `runner_id=0`, blank runner names, zero steps, and zero artifacts. This confirms the runner-allocation blocker persisted across the additional commits; none of these runs tested the source.
- No successful runner-backed debug/release build, signing/tamper verification, packaged payload measurement, dependency graph artifact, or checksum artifact is claimed.
- The separate M12 compiler-proof workflow is also blocked: runs [43867509159](https://github.com/xolomon-coder/DroidInspect/actions/runs/43867509159) and [43867535971](https://github.com/xolomon-coder/DroidInspect/actions/runs/43867535971), plus M11 engine-proof run [43867503469](https://github.com/xolomon-coder/DroidInspect/actions/runs/43867503469), all report `runner_id=0`, blank runner names, zero steps and no artifacts. The M12 transitive NuGet package graph therefore has not been emitted by these attempts; the transitive license review remains open.

**Milestone state:**
- **M19 — IN PROGRESS.** Focused host regressions pass, but APK/C#/.NET project reopen, process-restart behavior, Android UI integration, and real device project isolation/recovery remain unaccepted.
- **M20 — IN PROGRESS.** Eleven APK/.NET activities have improved cancellation/cleanup handling and the production workflow has stronger packaged-payload assertions. Full activity lifecycle audit (including any remaining project, decompilation and diagnostic workers), large-file/device performance, actual native payload measurement, full transitive licensing, debug/release builds, signing/tamper checks, and CodeAssist reproducibility remain pending.
- **M12–M18** remain implementation-integrated with consolidated device acceptance pending. M11 remains user-accepted; do not repeat its acceptance tests.

**Exact next actions:**
1. Obtain and inspect a runner-backed production Actions run for the latest `main` source. Confirm every host test, Android debug/release build, signing/tamper step, packaged-native inventory, dependency graph and SHA-256 artifact; fix actual failures and rerun.
2. Finish the remaining M20 lifecycle sweep across project, diagnostic and decompilation workers; add cancellation and safe deferred cleanup only where source inspection finds an actual race, then review all touched Java activities for compilation consistency.
3. Complete the transitive Gradle/NuGet license review and record actual APK sizes, native ABI payload, and final artifact hashes.
4. Only after cloud/host release verification, prepare the single consolidated M12–M20 Android 11 / CodeAssist testing session.

**Stopping point:** M19 bounded discovery and metadata hardening have focused host PASS evidence. M20 has source-level lifecycle and packaged-payload improvements. The production CI/release gate remains unverified; neither M19 nor M20 is marked complete.


### M20 continuation — DEX worker cancellation and activity teardown audit — 2026-11-17

**Source review finding and correction:**
- A remaining lifecycle race was found in `DexAnalysisActivity`: it used an unmanaged thread, and `onDestroy()` could delete the temporary APK while the worker was still copying or parsing it. The worker could also post UI updates after the Activity had been destroyed.
- Replaced the unmanaged thread with an owned single-thread executor. Activity teardown now requests interruption, suppresses callbacks after destruction, waits for the worker to terminate on a separate daemon cleanup thread, and only then deletes the temporary APK copy.
- Added interruption checks to DEX ZIP enumeration, DEX extraction, string/type/class table traversal, class member traversal, and invoke-reference instruction scanning. Cancellation is propagated rather than converted into a normal per-DEX rejection.
- Expanded `DexFileParserSelfTest` with a pre-interrupted invocation assertion. This is a regression test addition; it has **not yet executed** because the recent GitHub Actions attempts again failed before running steps.
- `MainActivity` now suppresses all three asynchronous UI callback paths after finishing/destruction and marks itself destroyed before shutting down its worker.
- `CSharpDecompileActivity` now also checks `isDestroyed()` before applying callbacks obtained through its weak Activity reference. Its native decompilation remains non-interruptible; worker shutdown is graceful so its workspace cleanup can finish.

**Exact files changed in this continuation:**
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DexFileParser.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DexAnalysisActivity.java`
- `tools/dex-parser-test/DexFileParserSelfTest.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/MainActivity.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/CSharpDecompileActivity.java`

**Verification and CI:**
- Existing M19 focused host tests remain recorded as PASS from the previous host-container execution; this continuation did not rerun them.
- The DEX parser cancellation regression has been added but is NOT YET VERIFIED by execution.
- Production Actions runs [43974257644](https://github.com/xolomon-coder/DroidInspect/actions/runs/43974257644) (#582), [43974263083](https://github.com/xolomon-coder/DroidInspect/actions/runs/43974263083) (#583), and [43974268202](https://github.com/xolomon-coder/DroidInspect/actions/runs/43974268202) (#584) ended with zero job steps and no runner execution. The latest job-step queries returned empty arrays. These are infrastructure failures, not source test or Android build results.
- No debug/release APK, signing/tamper evidence, packaged native payload measurement, dependency graph, transitive license conclusion, or CI checksum is available from these runs.
- Avoid repeated retry attempts while the same no-runner condition persists.

**Milestone state:** M19 remains IN PROGRESS with the two focused host tests previously passing; app-level project persistence/reopen/isolation acceptance remains pending. M20 remains IN PROGRESS: this continuation closes a specific DEX worker/temporary-file race and adds cancellation coverage, but the full lifecycle audit, regression execution, Android build, device performance and release verification remain pending.

**Next action:** Continue the M20 audit of project, diagnostic and decompilation workers; inspect and correct actual lifecycle/cancellation defects. Then obtain one runner-backed production CI execution and inspect every step and artifact before preparing the consolidated M12–M20 device test session.


### M19 continuation — bounded APK project discovery — 2026-11-17

- Source review found that the unified project launcher used the bounded `ProjectWorkspaceIndex` for C# source and .NET dependency projects, but `ApkProjectWorkspace.listRecentProjects()` still called `File.listFiles()` over the entire APK project root.
- Added a shallow bounded index path in `ProjectWorkspaceIndex.scanShallow()`: at most 2,000 root entries are examined, no more than 500 marked projects are returned, symbolic-link roots/markers are excluded, and sorting uses directory recency. APK project metadata continues to supply its own last-updated timestamp and revision.
- Routed APK recent-project discovery through this bounded helper. The existing deep per-project recency traversal is intentionally avoided for APK projects because their project metadata already records update time.
- Extended `ProjectWorkspaceIndexSelfTest` to verify the same 2,000-entry cutoff for shallow APK-style discovery. The new assertion is committed but has not yet been executed because GitHub Actions is still failing before job steps.
- This closes a source-level unbounded root scan in M19; it does not establish Android project launcher/device acceptance.

**Exact changed files:** `ProjectWorkspaceIndex.java`, `ApkProjectWorkspace.java`, and `ProjectWorkspaceIndexSelfTest.java`.

**Status:** M19 remains IN PROGRESS pending execution of the updated host test and Android project creation/reopen, persistence, isolation and recovery tests.


### M20 continuation — project import cancellation and background source-workspace creation — 2026-11-17

- Added interruption checks to APK project SHA-256 streaming so a closing APK project activity does not continue hashing a large working/original file after its worker is cancelled.
- Added interruption checks to .NET dependency-project document imports. The existing temporary-file/rename pattern now receives cancellation promptly; the incomplete temporary import is deleted by the existing error path, and the batch rollback removes already imported files.
- Moved C# source-project creation out of the decompiler Activity's UI thread and onto its existing worker executor. Preserving a selected original assembly (up to the documented 512 MiB bound) and writing the source workspace can no longer block the UI thread. The callback uses a weak Activity reference and checks finishing/destruction before opening the editor.
- These are source-level lifecycle changes. They have not yet passed a runner-backed Android build or device execution.

**Exact additional changed files:** `DotNetDependencyWorkspace.java`, `ApkProjectWorkspace.java`, and `CSharpDecompileActivity.java`.

**Latest CI evidence:** Production runs [43974403048](https://github.com/xolomon-coder/DroidInspect/actions/runs/43974403048) (#585), [43974549193](https://github.com/xolomon-coder/DroidInspect/actions/runs/43974549193) (#586), and [43974622943](https://github.com/xolomon-coder/DroidInspect/actions/runs/43974622943) (#587) all ended before executing a single step; each job-step query returned an empty list. The latest source change in this continuation will trigger another production run. These attempts do not establish compilation or test results.

**M19/M20 state:** M19 remains IN PROGRESS; bounded APK project discovery now uses the same root-entry budget as the other project types, with an added host regression assertion awaiting execution. M20 remains IN PROGRESS; cancellation and UI-thread work have been improved in additional project/decompilation paths, but a complete lifecycle audit, full host suite, Android debug/release build, packaged APK audit, and device verification are still outstanding.

### M19/M20 continuation — bounded APK indexing, project cancellation, and remaining runner blocker — 2026-11-17

**M19 project discovery correction:**
- APK recent-project discovery no longer calls unbounded File.listFiles() on the project root. It now uses ProjectWorkspaceIndex.scanShallow(), with the shared 2,000-root-entry scan budget and 500-project result cap, while using the APK project's own metadata timestamp/revision.
- The M19 host regression now also checks that a project beyond the root-entry budget is not returned by shallow APK-style indexing. This new assertion is committed but has not yet executed.
- Existing focused host test evidence remains limited to the earlier index and metadata tests. Do not claim the expanded index test passed until it actually runs.

**M20 lifecycle/cancellation continuation:**
- DEX analysis now owns an executor, checks interruption during ZIP extraction and parser table/member/instruction traversal, suppresses callbacks after Activity destruction, and defers temporary APK deletion until the worker exits.
- The DEX host test includes a pre-interrupted parser assertion; execution remains pending.
- Main dashboard asynchronous callbacks now guard against finishing/destroyed Activity state. The file inspection copy loop checks interruption so dashboard teardown can stop a large file copy and remove its temporary file.
- APK project imports, local working-copy transfers, exports, and SHA-256 streaming check cancellation. Interrupted partial local copies are deleted.
- .NET dependency-project SAF imports check interruption and remove their temporary import file; failed batch imports retain the existing rollback behavior.
- C# source-project creation is now performed on the decompiler worker rather than the UI thread. Copying the preserved original assembly and writing the separate source project no longer blocks the UI thread; the UI callback uses a weak Activity reference and lifecycle checks.
- These are source changes only. None of the newly added cancellation tests or Android runtime paths have been accepted on-device.

**Current production CI evidence:**
- Latest source commit before this README update: 465c6499b8bfbfcf3507821588268bb4ad3b4fd5 (M12 compile-operation verification correction). The current README-only checkpoint follows that source commit.
- Production run [43974822673](https://github.com/xolomon-coder/DroidInspect/actions/runs/43974822673) (#589) targeted that source commit. Its job 133675971061 completed with failure, runner_id=0, blank runner name, and an empty step list. It produced no artifacts. This is another runner-allocation failure, not a source compilation or test result.
- The preceding runs #585–#588 also had zero steps. No further retries are useful until runner assignment recovers.
- The production workflow is configured to run the expanded M19 index and metadata tests plus DEX, IL, metadata, resource, APK compatibility, comparison, and source-tree host regressions; build debug and release APKs; sign and verify; reject a tampered APK; inspect actual packaged native libraries/ABI/size; capture the Gradle release dependency graph; and publish SHA-256 artifacts. None of those checks ran in #589.
- The M12 compiler and M11 decompiler proof workflows remain separately subject to their previously documented runner-allocation blocker. Transitive NuGet dependency/license review remains incomplete.

**Source native payload inventory (rechecked from the current repository tree):**
- The only source ABI directory is arm64-v8a.
- libdroidinspect_engine.so: 45,966,680 bytes.
- libdroidinspect_jni.so: 11,032 bytes.
- libdroidinspect_m12_compiler.so: 35,316,984 bytes.
- libdroidinspect_m12_jni.so: 11,800 bytes.
- Total source-tree native library size: **81,306,496 bytes (77.54 MiB)**.
- This is the checked-in source-library total, not a measured compressed/uncompressed APK payload or installed size. The actual packaged inventory and compressed sizes still require a successful production runner.
- The production source manifest was rechecked: no uses-permission declarations are present and android:allowBackup=false is set. Packaged APK permission verification remains unexecuted.
- Direct dependencies in app/build.gradle.kts remain ARSCLib 1.4.0 and apksig 8.13.0 (Apache-2.0). THIRD-PARTY-NOTICES.md records direct ILSpy 11.2.0.10050, .NET 11 runtime/reference pack, and Roslyn 5.12.0 MIT notices. This is not a completed transitive NuGet/Gradle license audit.

**Milestone state:**
- **M19 — IN PROGRESS.** The C# source/dependency index and metadata host tests previously passed. The APK index is now bounded, but its added host assertion, full host regression suite, APK/C#/.NET reopen, restart persistence, project isolation, and recovery remain to be verified.
- **M20 — IN PROGRESS.** More project/decompilation/diagnostic worker races have been addressed. Remaining work includes the complete lifecycle/cancellation sweep, full host regression execution, Android debug/release builds, signing/tamper verification, actual packaged native inventory, transitive license review, device memory/performance tests, and CodeAssist reproducibility.
- **M12–M18** remain implementation-integrated with consolidated device acceptance pending. **M11 remains user-accepted; do not repeat its acceptance tests.**

**Exact next action:** Continue static review of remaining project and decompilation worker ownership/cancellation paths without speculative rewrites. When Actions runner allocation recovers, run the production workflow once, inspect every host test and build step plus every artifact, fix actual failures on main, and only then prepare the consolidated M12–M20 phone test session.
### M12 output verification correction and latest CI result — 2026-11-17

- Source review found that the C# editor's post-compilation output check only tested the DOS MZ signature while describing the result as a valid managed PE. An arbitrary native PE could therefore pass that UI-level check.
- The production editor now reopens the compiler output through the existing bounded DotNetIlReader parser and requires a decoded assembly identity plus at least one indexed MethodDef before reporting managed-assembly verification success. Parse failures now return false.
- This is a stronger structural check than MZ-only detection; it still does not prove runtime/behavioral compatibility or that the rebuilt DLL is a drop-in replacement.
- Production run [43975093614](https://github.com/xolomon-coder/DroidInspect/actions/runs/43975093614) (#590), targeting commit 234e6d3ef838fe7ee6a2637449e0fd77d3526b8f, again failed before runner assignment (job 133676915084, runner_id=0, blank runner name, zero steps, no artifacts). The change has not yet been compiled or tested by CI.
- M12 remains implementation-integrated but device acceptance is pending. M19 and M20 remain IN PROGRESS; no milestone is declared complete by this source correction.
### M12/M20 compiler worker follow-up — 2026-11-17

- The source editor compiler now runs under an owned single-thread executor and captures only application context, project state, and a weak Activity reference. On Activity destruction it shuts down gracefully rather than interrupting the synchronous Native AOT compiler, allowing private build/report writes to finish without retaining the destroyed Activity.
- The strengthened managed-assembly verification is performed on the compiler worker. Its boolean result is passed to the UI callback; the UI thread no longer reparses the generated DLL when enabling export.
- Production run [43975208945](https://github.com/xolomon-coder/DroidInspect/actions/runs/43975208945) (#591), targeting commit 07c9a73e40546cca7ef659bc088e24d39eef9d26, again failed before runner assignment (runner_id=0, empty runner name, zero steps). No source compilation or test result is available.
### M12 build-result state correction — 2026-11-17

- The M12 project-operation journal now records SOURCE_COMPILE as PASS only when the native compiler reports compilation success **and** the independent managed PE/CLI re-open check succeeds. A native compiler PASS followed by Java-side structural verification failure is recorded as NOT PASS.
- Production run [43975311959](https://github.com/xolomon-coder/DroidInspect/actions/runs/43975311959) (#593), targeting 465c6499b8bfbfcf3507821588268bb4ad3b4fd5, ended with runner_id=0, blank runner name, and zero executed steps. No artifacts were produced; source compilation remains unverified.
### M19/M20 continuation — bounded source traversal and cancellable editor I/O — 2026-11-17

**M19 source-tree and workspace hardening:**
- BoundedSourceTree.scan() now enumerates each source directory with DirectoryStream<Path> rather than File.listFiles(). This avoids materializing an unbounded child array before enforcing the existing 10,000-node, 2,000-source-file, 16-level, 32 MiB-per-file and 128 MiB aggregate limits.
- Symbolic-link entries are skipped during source-tree discovery. The existing canonical containment check remains as defense in depth.
- Expanded BoundedSourceTreeSelfTest to create an external C# source directory and verify that a symbolic-link directory is excluded. This regression has now passed in the local host JVM.
- C# source imports now account for existing project bytes and file count, check thread interruption while copying, and roll back every file imported earlier in the same batch if a later import fails or cancellation occurs.
- Source ZIP export checks interruption between files and while streaming file contents.

**M20 lifecycle and UI-thread I/O hardening:**
- DecompilationWorkspace checks interruption during selected-assembly copying and bundled .NET reference extraction. A cancelled staging extraction is removed without activating a partial reference directory.
- CSharpDecompileActivity now interrupts cancellable preparation/source-workspace work when the screen is destroyed. A native decompilation already executing inside JNI may still finish; its try-with-resources cleanup remains responsible for removing the temporary workspace.
- CSharpSourceWorkspace checks interruption while preserving the original assembly and writing source-project files. A cancelled project creation follows its existing failure cleanup path.
- CSharpSourceEditorActivity now performs C# source import, source ZIP export, compatibility-report export and rebuilt-DLL export on a dedicated I/O executor rather than blocking the UI thread. Activity callbacks are weak-reference/lifecycle guarded, and the I/O executor is interrupted on teardown.
- The source editor's import batch now rolls back already imported files on failure/cancellation, and the source-tree walker no longer allocates an unbounded directory listing.

**Exact files changed in this continuation:**
- droidinspect/app/src/main/java/com/tanvir/droidinspect/DecompilationWorkspace.java
- droidinspect/app/src/main/java/com/tanvir/droidinspect/CSharpDecompileActivity.java
- droidinspect/app/src/main/java/com/tanvir/droidinspect/CSharpSourceWorkspace.java
- droidinspect/app/src/main/java/com/tanvir/droidinspect/BoundedSourceTree.java
- tools/bounded-source-tree-test/BoundedSourceTreeSelfTest.java
- droidinspect/app/src/main/java/com/tanvir/droidinspect/CSharpSourceEditorActivity.java

**Commits in this continuation (oldest to newest):**
- 291ca266eb88c2e313e2189f8fdf2fa7fb7cc48f — cancel interrupted decompilation workspace preparation.
- 818d252af45635ca82da45641aba0c02596497b9 — interrupt cancellable decompiler preparation on teardown.
- 56314a3c3bc80305f1e6d5a48c7af007679749fc — cancel interrupted C# source project copies.
- 3bf7d61545f78dc59adb99265ddba0a0a8dc1d65 — bound source-tree traversal without unbounded directory arrays.
- 26e6eb18edcff610662e4f92b391730f8ab4ae19 — test source-tree symlink exclusion.
- 357ef554a2ba8701fef6d4ae9bc733f2aafa5324 — cancel and roll back interrupted C# source imports.
- 32041fc0f10641e84f16b4fd37dd75227c57224b — move C# source import and export off the UI thread.

**CI and verification state:**
- The latest previously inspectable production run remains run 43975311959 (#593), targeting source commit 465c6499b8bfbfcf3507821588268bb4ad3b4fd5. Its job completed as failure with runner_id=0, no runner name, zero steps and no artifacts. It did not compile or test the source.
- The new source commits trigger the main-branch production workflow because it runs on non-README pushes. In this session, the GitHub commit-status endpoint returned no status records for the new commits, and the available commit-workflow query exposes only pull-request-triggered runs; therefore no push-run ID/job record could be confirmed for these commits. Do not infer a build result from the commits.
- M11/M12 proof workflows were inspected. Their latest previously recorded attempts 43867503469, 43867509159 and 43867535971 each have a failed job with no steps and no artifacts. No transitive NuGet graph or new proof artifact is claimed.
- BoundedSourceTreeSelfTest, ProjectWorkspaceIndexSelfTest and ProjectWorkspaceMetadataSelfTest were compiled with javac --release 8 and all passed locally. Source import rollback paths, full host regressions, Android Java/XML compilation, debug/release APK builds, APK signing/tamper checks, packaged native inventory and SHA-256 generation remain unverified by a runner-backed execution.
- Re-ran the focused M19 project-index and project-metadata host tests locally with Java 8 source compatibility: both PASS. The bounded C# source-tree test, including symlink exclusion, also PASS. The broader host regression suite still needs execution.
- No transitive Gradle/NuGet license review, actual packaged native size measurement or final APK checksum is claimed.

**Milestone state:**
- **M19 — IN PROGRESS.** Project discovery/metadata bounds and C# source-tree traversal have been hardened. The three focused host regressions now pass locally; the remaining host suite, project creation/reopen, process-restart persistence, cross-project isolation and interrupted-operation recovery still require runner-backed and Android integration/device acceptance.
- **M20 — IN PROGRESS.** Additional decompiler/editor cancellation and background I/O paths are hardened. Full activity lifecycle review, runner-backed compilation, debug/release/package audits, signing/tamper verification, dependency/license review and device memory/performance testing remain open.
- **M12–M18** remain implementation-integrated with consolidated device acceptance pending. **M11 remains user-accepted; do not repeat its acceptance tests.**

**Exact next actions:**
1. Obtain the main-branch production Actions run for the latest source commit d136de43b227fbc101298201091077bfbdcb9075 and inspect its job, every host-test step and all artifacts. Do not retry while the job still reports runner_id=0 and zero steps.
2. Continue the source audit of remaining production project/dependency workers and bounded file listings; fix only demonstrated defects.
3. On the first runner-backed execution, resolve real source/test/build failures, then inspect the debug/release APKs, signatures, tamper rejection, ARM64 payload, dependency graphs, license notices and SHA-256 output.
4. Only after cloud/host release verification, prepare the single consolidated M12–M20 Android 11 / CodeAssist device-testing session.

**Stopping point:** Seven additional source/test commits are on main. M19/M20 remain unaccepted. The production CI and release gate remains unverified, and no new phone testing is requested yet.

### M20 continuation — dependency worker ownership and bounded local dependencies — 2026-11-17

- DotNetDependencyActivity worker tasks now capture the application Context and active workspace rather than retaining the Activity. UI delivery uses a Handler plus WeakReference and suppresses callbacks after destruction.
- Dependency resolution checks cancellation before entering the native resolver. If the Activity closes during cancellable preparation, the operation journal marker is intentionally left in place so the next project open can show the interrupted-operation recovery notice. A native resolver call already executing may finish; the worker can save its result without retaining the Activity.
- DotNetDependencyWorkspace now lists local dependency assemblies with DirectoryStream, skips symbolic links, and enforces a 2,000-file project limit. Imports enforce the same count limit in addition to the existing 128 MiB per-file and 512 MiB batch bounds.
- The activity/workspace changes are source-level only and have not yet passed Android compilation, host execution, or device testing.

**Additional exact files changed:**
- droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetDependencyActivity.java
- droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetDependencyWorkspace.java

**Additional commits (oldest to newest):**
- b0b315eabd3ed07f576276cac9a4ee3c9629eb9b — detach dependency workers from destroyed Activity.
- f38fe852f8ef5e8e868d8275c5840d085743b3b4 — fix dependency Activity file import.
- d136de43b227fbc101298201091077bfbdcb9075 — bound local dependency project file enumeration.

**Latest verification check:**
- Current source HEAD before this README update: d136de43b227fbc101298201091077bfbdcb9075.
- The combined commit-status endpoint returned no status records for that source commit. The production workflow is configured for non-README pushes, but the available GitHub Actions connector cannot enumerate push-triggered runs; no new run ID or runner-backed result is therefore claimed.
- The last directly inspectable production job remains run 43975311959 (#593): completed failure, empty job-step list, no artifacts. The M11/M12 proof jobs 43867503469, 43867509159 and 43867535971 also have empty step lists and no artifacts.
- Do not retry those infrastructure failures until a fresh Actions job shows runner assignment has recovered. No compilation/test PASS is inferred from an empty status response.

**Milestone state:** M19 and M20 remain IN PROGRESS. The source-level bounds and lifecycle corrections above require execution by the full production CI and the consolidated Android device-testing session before acceptance.

### M19 continuation — bounded APK project history, reports and tree — 2026-11-17

**Source audit finding and correction:**
- The APK project workspace still used unbounded `File.listFiles()` in revision-history pruning, revision-history display, report listing and recursive project-tree display.
- Added `BoundedProjectFiles`, which uses `DirectoryStream`, excludes symbolic links, enforces a global directory-entry budget, checks canonical containment during tree traversal, and sorts bounded results.
- APK revision history now scans at most 2,000 directory entries and exposes the newest two snapshots. History pruning retains the two newest APK snapshots.
- APK project report display scans at most 2,000 entries and displays at most 100 reports.
- APK project-tree display is limited to 2,000 total entries and four directory levels. Directory-read and safety-limit failures are surfaced to the user instead of silently producing an incomplete tree.
- Added `BoundedProjectFilesSelfTest` for ordering, symlink exclusion where supported, scan-budget enforcement and tree enumeration. The production workflow now compiles and runs this test.
- The new host test was compiled with `javac --release 8` and executed in the local host JVM: **PASS**. This is a host regression result, not GitHub Actions or Android device verification. The three earlier focused M19 host tests also remain PASS.

**Exact files changed:**
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/BoundedProjectFiles.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkProjectWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkProjectWorkspaceActivity.java`
- `tools/bounded-project-files-test/BoundedProjectFilesSelfTest.java`
- `.github/workflows/phase1-android.yml`

**Commits on main (oldest to newest):**
- `8fa1be55c0db3aac816c3cceae7bd987fb82904b` — add bounded APK project inventory helper.
- `1c41df35533ddd46c4ffe2462078d9c96bd06abb` — add host regression test.
- `30cb1c324cf6928bc2e246df169ffc0b511b81db` — bound APK project history, reports and tree listings.
- `4d9f40e7685088610c5ecdf2b393895094f4b0e9` — handle inventory safety-limit/read errors in the project UI.
- `e216e249e6101f73a8d3f08717c4183546d85d17` — add the regression suite to production CI.
- `10894133b8bcaf542f9b1e58b6c2c9ff5fdc03d6` — scan existing APK history before creating the next snapshot.

**Verification state:**
- Current source/workflow checkpoint: `10894133b8bcaf542f9b1e58b6c2c9ff5fdc03d6` on `main`.
- No host execution or Android build is claimed for these latest changes.
- The GitHub connector can inspect known workflow run IDs and jobs but does not expose repository-wide push-run enumeration. The commit-status query returned no status records, and the commit-workflow query returned no runs because it only exposes pull-request-triggered runs. This is not evidence of a successful build.
- The previously inspectable production run `43975311959` remains a runner-allocation failure with zero steps and no artifacts. No retry was issued without evidence that runner assignment had recovered.
- The new production workflow step will run `BoundedProjectFilesSelfTest` alongside the existing host suites when a runner is assigned.

**Milestone state:** M19 remains IN PROGRESS. APK project directory inventory is now bounded at the source level, but the new host regression, full host suite, project persistence/reopen/isolation/recovery and Android acceptance remain pending. M20 remains IN PROGRESS; lifecycle audit and runner-backed build/release verification remain open. M12–M18 device acceptance remains pending; M11 remains accepted.

**Exact next actions:**
1. Continue the remaining M20 worker/lifecycle audit and inspect all remaining project/diagnostic/decompilation file operations for demonstrated defects.
2. Obtain and inspect a fresh production Actions run when available; verify runner assignment, all test steps, debug/release APKs and every audit artifact.
3. Run the new bounded-project-files test and complete host regressions before preparing the consolidated M12–M20 phone test session.

**Stopping point:** APK project history, report and tree enumeration have been converted to bounded, symlink-safe scans and the new test is wired into production CI. No runner-backed or device acceptance is yet recorded.

### M19/M20 continuation — bounded cleanup and APK project worker lifecycle — 2026-11-17

**M19 temporary workspace cleanup correction:**
- The final production File.listFiles() call in ApkProjectWorkspace.deleteTree() was unbounded and ignored both enumeration and deletion failures.
- Moved cleanup into BoundedProjectFiles.deleteTree(). It enumerates with DirectoryStream, enforces the shared 2,000-entry budget and a 64-level depth ceiling, rejects a symbolic-link workspace root, deletes symbolic-link children as links without traversing their targets, checks canonical containment, and checks thread interruption during traversal/deletion.
- Failed directory enumeration or deletion now raises an IOException instead of silently reporting cleanup as complete. APK project creation preserves its original failure and attaches cleanup failure as a suppressed exception.
- Extended BoundedProjectFilesSelfTest to cover bounded cleanup, external symlink-target preservation and interruption behavior.

**M20 APK project Activity lifecycle correction:**
- The APK project Activity previously passed the Activity itself into background project open/create work and called runOnUiThread from worker closures. This could retain a destroyed Activity while a long file operation was still running.
- Project open/create workers now receive the application Context. Worker callbacks hold only a WeakReference to the Activity and are delivered through the main-thread Handler after finishing/destroyed checks.
- Project actions now receive a captured ApkProjectWorkspace instead of accessing the Activity's project field from worker lambdas. Text is read from UI widgets before dispatch, avoiding worker retention of editor views.
- The executor is still shut down on Activity destruction; cancellable workspace operations observe interruption where implemented. Operations already in non-interruptible platform/native work remain subject to their underlying API behavior.

**Exact files changed in this continuation:**
- droidinspect/app/src/main/java/com/tanvir/droidinspect/BoundedProjectFiles.java
- tools/bounded-project-files-test/BoundedProjectFilesSelfTest.java
- droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkProjectWorkspace.java
- droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkProjectWorkspaceActivity.java

**Commits on main (oldest to newest):**
- 55368c5d7dd4b48c37ec481b9df6451b37d4348b — bound temporary project cleanup traversal.
- 995bd1f138c71cac42af633fa5a6f97fc5eb1c0d — test bounded temporary workspace cleanup.
- 11c08659399fa504aafa81236c373801cefc18d8 — use bounded safe cleanup for failed APK projects.
- 0bec782c2180c62a4e4060a0b5ad842107eb23fe — detach APK project workers from Activity lifecycle.
- 0aae5c14e7eb5c024064caabdf54213548eab609 — remove Activity capture from APK project I/O tasks.

**Verification and CI:**
- Compiled the updated bounded cleanup helper and regression scenarios using javac --release 8; execution reported BoundedProjectFilesSelfTest: PASS. This is a local host JVM check, not GitHub Actions, Android compilation, or device acceptance.
- The production workflow .github/workflows/phase1-android.yml is configured for non-README pushes and includes this host test. The source commits therefore target the production workflow.
- The combined commit-status query for the prior checkpoint returned no status records. The available GitHub Actions connector can inspect known run IDs/jobs but does not expose repository-wide push-run enumeration; the commit-workflow query only returns pull-request-triggered runs. No new push-run ID, runner assignment, job steps, or artifacts could be confirmed in this session.
- The last directly inspectable production run remains 43975311959 (#593), which failed before executing any steps (runner_id 0, blank runner name, no artifacts). This is not evidence of a source or test failure, and it is not evidence that the latest commits passed.
- No full host-suite result, Android debug/release build, signed APK, tamper-test result, packaged native inventory, dependency graph, license audit or CI checksum is claimed.

**Milestone state:**
- **M19 — IN PROGRESS.** APK history/report/tree and temporary cleanup now have bounded source paths. The focused bounded cleanup host test passed locally. The full host suite, project create/reopen, process-restart persistence, cross-project isolation, metadata recovery and Android integration/device acceptance remain open.
- **M20 — IN PROGRESS.** The APK project worker-retention path and temporary cleanup were hardened. The broader project/diagnostic/decompilation lifecycle sweep, full host regressions, runner-backed Android build/release checks, device memory/performance and CodeAssist reproducibility remain open.
- **M12–M18** remain implementation-integrated with consolidated device acceptance pending. **M11 remains user-accepted; do not repeat its acceptance tests.**

**Exact next actions:**
1. Continue auditing diagnostic, dependency and decompilation Activity workers for strong Activity captures, uncancellable large I/O and callbacks after destruction.
2. Obtain a fresh production Actions run using a supported repository-wide run listing method when available; inspect runner assignment, all job steps and every artifact. Do not infer success from empty status responses.
3. Execute the full host regression suite and the production debug/release, signing/tamper, ARM64/native payload, dependency and checksum gates once a runner is available.
4. Only after implementation and available automated verification are complete, prepare the single consolidated M12–M20 Android 11 / CodeAssist testing checklist.

**Current branch and stopping point:** main, source HEAD 0aae5c14e7eb5c024064caabdf54213548eab609 before this README checkpoint. M19/M20 remain unaccepted; production CI and release verification remain unconfirmed. No individual phone test is requested.

### M20 continuation — dashboard inspection and diagnostics worker ownership — 2026-11-17

- MainActivity file inspection and runtime diagnostics previously called Activity methods and used Activity-owned UI callbacks from background worker closures. Although the callbacks checked destroyed/finishing state, the closures could keep the Activity strongly reachable until the operation ended.
- Both operations now capture only the application Context and a WeakReference to MainActivity. Results are posted through a main-thread Handler and delivered only if the Activity still exists and is not finishing/destroyed.
- Runtime diagnostics now obtains package/cache information through the application Context. FileInspectionEngine already checks thread interruption while copying the selected input; the dashboard executor is interrupted during Activity destruction.
- Exact changed file: droidinspect/app/src/main/java/com/tanvir/droidinspect/MainActivity.java
- Commit: a9e2da78517cfbb39ff5da9ceffa1b0b77260489 — detach diagnostics and file inspection workers from Activity.
- This is a source-level lifecycle correction. No Android compilation or device result is claimed.

**Updated stopping point:** source HEAD a9e2da78517cfbb39ff5da9ceffa1b0b77260489 on main. M19 and M20 remain IN PROGRESS. The full host suite and runner-backed production CI/release checks remain outstanding; no individual phone testing is requested.


### M19/M20 continuation — source-root boundaries and bounded workspace rollback — 2026-11-17

**Confirmed source defects and corrections:**
- BoundedSourceTree previously canonicalized the supplied root before checking whether the supplied root itself was a symbolic link. It now rejects a symbolic-link root explicitly, and the host regression suite includes a root-link rejection case in addition to child-link exclusion.
- DotNetDependencyWorkspace and CSharpSourceWorkspace previously used unbounded recursive File.listFiles() cleanup. Failed project creation now routes through BoundedProjectFiles.deleteTree(), which applies the global entry budget, 64-level depth cap, interruption checks, symlink-safe deletion, and explicit failure reporting.
- The shared cleanup helper now supports an expected trusted parent. Dependency and C# project rollback verifies that the new workspace root is a direct child of its intended private project directory before deleting it.
- C# project folder creation now occurs inside the rollback-protected block, so failure after creating only part of the folder structure attempts bounded cleanup. The original creation exception is preserved; cleanup failures are attached as suppressed exceptions rather than hiding the original failure.
- Dependency and C# project reopening/last-project lookup now reject symlinked workspace roots and essential marker/source/original/dependency paths that are not direct children of their expected private parents. C# source and build-output access also verify the expected directory boundary.
- Dependency listing rejects an unavailable or symlinked local-dependency directory.

**Files changed in this checkpoint:**
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/BoundedProjectFiles.java`
- `tools/bounded-project-files-test/BoundedProjectFilesSelfTest.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/BoundedSourceTree.java`
- `tools/bounded-source-tree-test/BoundedSourceTreeSelfTest.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetDependencyWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/CSharpSourceWorkspace.java`

**Verification status:**
- These changes are committed as source-level corrections only until the updated host suites and Android production workflow execute.
- Production Actions run #615 (run ID `44034415267`, commit `a9e2da78517cfbb39ff5da9ceffa1b0b77260489`) completed as failure in approximately two seconds. Its only job had `runner_id=0`, an empty runner name, and zero steps. No compilation or test step ran and no artifact was produced.
- The six most recent production workflow runs (#610–#615) were previously observed as failures. Do not infer source compilation failure or success from those runner-start failures.
- M19 and M20 remain IN PROGRESS pending reproducible host execution, Android compilation/lifecycle verification, and the consolidated M12–M20 device acceptance. M11 remains accepted and must not be retested.

**Next actions:**
1. Execute all four M19 host suites and resolve any real regressions: ProjectWorkspaceIndexSelfTest, ProjectWorkspaceMetadataSelfTest, BoundedSourceTreeSelfTest, and BoundedProjectFilesSelfTest.
2. Continue auditing Activity-owned workers, cancellation, partial dependency import rollback, project reopen and process-restart recovery.
3. Recheck production Actions runner allocation before retrying; then complete host tests, Android debug/release builds, signature/tamper checks, ARM64/native inventory, dependency/license reports and checksums.
4. Prepare the single consolidated M12–M20 phone test checklist only after implementation and available automated verification are complete.

**Current branch:** `main`. The commit for this checkpoint is recorded in the GitHub commit history; inspect the live branch head before the next edit.


### M19/M20 continuation — unchecked rollback and APK build-pipeline lifecycle — 2026-11-17

**M19 filesystem and rollback corrections now on `main`:**
- `BoundedProjectFiles.rollbackDirectChildren()` now accepts the original failure as a `Throwable`, so batch rollback can run when an import fails with an `IOException`, unchecked exception, or `Error`. Directory and child-path validation failures are attached to the primary failure rather than masking it. Symlinks are removed as links; rollback is confined to direct children of the trusted import directory.
- Added a host regression for rollback after an `AssertionError`, in addition to the existing runtime-exception, symlink, trusted-parent and interruption cases.
- `ApkProjectWorkspace` now also cleans partial file copies after unchecked/fatal failures. Project-creation rollback and temp-file deletion preserve cleanup failures as suppressed diagnostics.
- `DotNetDependencyWorkspace` import rollback now covers checked, unchecked and fatal failures; its temporary input copy is removed on all those failure paths.
- `CSharpSourceWorkspace.copyOriginalAssembly()` now stages the original assembly through a sibling temporary file and uses one checked cleanup path for failed reads, cancellation, empty input and activation failure. Partial source-import cleanup and creation rollback also retain cleanup diagnostics.
- `DecompilationWorkspace` continues to keep normal cleanup separate from interruption-resistant rollback. Reference-staging rollback and partial-input cleanup now also preserve runtime filesystem failures.
- `FileInspectionEngine` temporary-copy cleanup reports both I/O and runtime filesystem failures. No cleanup helper follows symbolic links or relaxes the bounded trusted-root rules.

**M20 Activity lifecycle and cancellation audit:**
- `ApkBuildPipelineActivity` now passes the application Context into the persistent project-open operation and its background copy/inspection work. Its four worker operations (project open, edited-export save, original APK inspection, final signed-output verification) use a main-thread Handler plus a WeakReference callback instead of direct `runOnUiThread()` closures.
- The pipeline's file-copy helpers are static and application-context based. Partial cache copies are removed for checked, unchecked and fatal failures. If the Activity is gone before the inspection callback is delivered, the unclaimed copy is deleted on a separate cleanup thread. The Activity teardown worker waits off the UI thread and then removes the retained original copy.
- The project/dashboard/dependency/decompiler/editor workers inspected in this checkpoint already use application context and weak lifecycle-checked callbacks in their updated paths. `DotNetDependencyActivity` checks interruption before and after the synchronous native dependency resolver. `CSharpDecompileActivity` closes its bounded workspace after JNI returns, including interruption-resistant rollback when needed. `CSharpSourceEditorActivity` explicitly allows synchronous Native AOT compilation to finish its private output/report writes because the native call cannot be safely interrupted; its UI callback is lifecycle guarded.
- The broader legacy Activity sweep is **not complete**. `ApkResourceActivity` and `ApkSigningActivity` still contain worker closures that call `runOnUiThread()` directly (with destroyed/finishing checks and off-UI-thread teardown cleanup). Their Activity-retention paths, as well as remaining analysis screens with their own workers, still need a dedicated pass. Do not interpret this checkpoint as proof that every Activity is lifecycle-detached.

**Exact files changed in this continuation:**
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/BoundedProjectFiles.java`
- `tools/bounded-project-files-test/BoundedProjectFilesSelfTest.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkProjectWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetDependencyWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/CSharpSourceWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DecompilationWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/FileInspectionEngine.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkBuildPipelineActivity.java`

**Commits on `main` (oldest to newest in this continuation):**
- `24e84759f2a48912c0e0fab91ab16f3411cf1e2d` — make workspace import rollback cover unchecked failures.
- `3755ebc7c9a820f54083e55ab96979307ab29284` — detach APK build-pipeline workers from Activity.
- `4081a9b87a76abd4ad7cc92c21af44c4426d8e78` — correct APK pipeline cleanup callback compatibility.
- `e5af4edd4fd3d8ce5e9b394799a825412e918f7e` — preserve APK pipeline cleanup diagnostics.
- `9e63ceda32ad0edc85294dda9aa9f936a207af80` — clean abandoned APK inspection copies on lifecycle exit.
- `5afff9f4915d70e84250226aa0d9f3db7790e032` — preserve primary failures across bounded cleanup paths.
- `e03c3ddcd312751daeb1a64715ded8f34a35d43a` — run discarded inspection cleanup off the UI thread.

**Host tests, CI and release verification:**
- The production workflow remains `.github/workflows/phase1-android.yml` and is wired to compile/run `ProjectWorkspaceIndexSelfTest`, `ProjectWorkspaceMetadataSelfTest`, `BoundedSourceTreeSelfTest` and `BoundedProjectFilesSelfTest` before the Android build. The separate proof workflows remain `.github/workflows/m11-nativeaot-poc.yml` and `.github/workflows/m12-roslyn-compiler-poc.yml`.
- The metadata host-test source covers notes, bookmarks, saved searches, project isolation, operation-journal/recovery-marker behavior after metadata-store recreation, bounded listing, malformed/oversized metadata and symlink rejection. This does not by itself verify Android process-kill/restart or a full `ApkProjectWorkspace` reopen on-device.
- **No M19 host suite was rerun at this source HEAD.** Prior local Java 8 host PASS entries elsewhere in this README refer to earlier source snapshots; they are not new verification of these changes.
- Production Actions runs #626–#632 have all ended before executing a workflow step. In the inspectable job records, runner assignment is `runner_id=0`, runner name is blank, the step list is empty and no artifacts are present. Latest run #632 is [run ID 44082406359](https://github.com/xolomon-coder/DroidInspect/actions/runs/44082406359), targeting source commit `e03c3ddcd312751daeb1a64715ded8f34a35d43a`; its job did not reach checkout, test, Android compilation or APK build. These are runner-allocation failures, not evidence of a source test failure and not evidence of success.
- No debug/release APK, signing/tamper verification, ARM64 ABI/native payload inventory, compressed/uncompressed payload size, dependency/license report or CI checksum has been inspected for this continuation. Do not claim the production release gate passed.

**Milestone and persistence status:**
- **M19 — IN PROGRESS.** Source-level bounds, safe rollback and metadata-store host coverage are present. Latest source changes still need all four host suites executed. Actual APK project create/reopen, Android process-restart persistence, cross-project integration isolation, interrupted-operation recovery and device acceptance remain open.
- **M20 — IN PROGRESS.** Workspace rollback and several important worker paths are improved. The remaining Activity lifecycle sweep, full host execution, runner-backed Android builds/release checks and device lifecycle verification remain open.
- **M12–M18** remain implementation-integrated with consolidated device acceptance pending. **M11 remains user-accepted; do not repeat M11 acceptance testing.**

**Exact next actions:**
1. Continue the lifecycle pass on `ApkResourceActivity`, `ApkSigningActivity` and the remaining worker-owning analysis Activities; detach remaining strong Activity captures where warranted, keep UI delivery lifecycle guarded, and document any non-interruptible JNI/native task behavior.
2. Complete the M19 persistence/recovery integration audit, distinguishing metadata-store host coverage from real Android project create/reopen/process-restart verification.
3. Re-run the four M19 host suites and other applicable host tests when a runner can execute steps; inspect a fresh Actions job before any retry. Do not repeatedly dispatch while `runner_id=0`.
4. Once runners execute the workflow, resolve any demonstrated test/build failures, then verify production debug/release APKs, signing/tamper rejection, ARM64/native inventory, dependencies, licenses and checksums.
5. Prepare the single consolidated M12–M20 Android 11 / CodeAssist checklist only after implementation and available automated verification are complete.

**Current source stopping point:** `main`, source HEAD `e03c3ddcd312751daeb1a64715ded8f34a35d43a` before this README-only update. M19 and M20 remain IN PROGRESS. No new Android testing is requested yet.

---

## Continuation checkpoint — README synchronization — 2026-11-17

This section supersedes earlier stopping-point statements where they conflict with the live repository checkpoint below. It records only work inspected in the current continuation; it does not imply that the pending source refactors or verification gates have completed.

### Live repository checkpoint

- Branch: `main` only.
- Live source checkpoint inspected at the start of this continuation: `2c8b7b3868701bc31a15632c4dfcec9ee56920c4`.
- Latest inspected APK resource editor changes are present on this checkpoint. Their successful Android build and production CI execution have not been established here.
- The README itself was read from the live `main` branch before this synchronization.

### APK signing Activity — outstanding, not committed

File reviewed: `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkSigningActivity.java`.

The signing lifecycle refactor has **not** been committed. The current implementation still requires a focused correction for worker ownership and Activity lifecycle safety. The review identified worker closures that retain/use Activity state, worker-side Activity context/content-resolver access, unchecked temporary-file deletion, and teardown/worker coordination that needs to guarantee no file is deleted while a worker may still use it.

Required next implementation:
- Capture immutable operation inputs before dispatch and use application Context for file/content access.
- Detach worker closures from the Activity; deliver results with a main-thread Handler and WeakReference plus finishing/destroyed validation.
- Check cancellation at safe processing boundaries.
- Roll back partial APK/keystore imports and failed signing outputs with bounded, checked cleanup.
- Preserve primary failures and attach cleanup failures as suppressed exceptions.
- Transfer output ownership only after successful completion.
- Perform worker termination waiting and cleanup on a separate thread, never on the UI thread.
- Verify Java 8 and minSdk 26 compatibility before committing.

### M19/M20 source-review checkpoint

The following current source paths were inspected for the previously reported checked-cleanup defects:
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/ApkProjectWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DecompilationWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetDependencyWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/CSharpSourceWorkspace.java`
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/FileInspectionEngine.java`

Source review found that the specifically flagged metadata/temp-file cleanup paths in ApkProjectWorkspace, DotNetDependencyWorkspace and CSharpSourceWorkspace already use checked cleanup and preserve primary exceptions. DecompilationWorkspace and FileInspectionEngine were also reviewed for bounded/symlink-safe cleanup and primary-exception preservation. These are source-inspection observations only; they are not proof of runtime behavior or test execution.

The four existing host regression source files were located:
- `tools/project-workspace-index-test/ProjectWorkspaceIndexSelfTest.java`
- `tools/project-workspace-test/ProjectWorkspaceMetadataSelfTest.java`
- `tools/bounded-source-tree-test/BoundedSourceTreeSelfTest.java`
- `tools/bounded-project-files-test/BoundedProjectFilesSelfTest.java`

Their source was inspected, but **none of these four suites was executed in this continuation**. Earlier local PASS statements in this README refer to earlier checkpoints and must not be treated as verification of the current source snapshot.

### CI and release verification

- Production workflow: `.github/workflows/phase1-android.yml`.
- No current successful production workflow run, executed job steps, Android build, APK artifact, signing/tamper result, ABI/native inventory, dependency/license report, or checksum was established during this continuation.
- Historical runner-allocation failures remain historical evidence only. Do not infer current runner availability from them.
- M19 and M20 remain **IN PROGRESS**. Android process-restart persistence, complete Activity lifecycle coverage, host-suite execution, Android debug/release builds, device verification, and production release gates remain open.
- M11 remains user-accepted; do not repeat M11 acceptance tests. Do not request separate M12–M20 device tests; prepare one consolidated checklist after implementation and available automated verification are complete.

### Exact next actions

1. Recheck live `main` before editing, then implement and commit the APK signing lifecycle refactor.
2. Continue the remaining Activity worker/cancellation audit, including APK resource/signing and any other worker-owning screens.
3. Run all four named M19 host suites and applicable additional host tests; record exact commands and outputs.
4. Complete M19 persistence/recovery and M20 rollback/cancellation audits without conflating source review with runtime verification.
5. Inspect current Actions runner/job execution before retrying production CI; once jobs execute, fix demonstrated failures and inspect all required APK artifacts.
6. Update this README with exact commits, changed paths, test commands/results, CI run IDs/results and release evidence.
7. Only then prepare the consolidated M12–M20 phone-based CodeAssist test checklist.

**Current documentation status:** this README synchronization records the current inspected checkpoint and outstanding work. It does not mark the APK signing refactor, M19/M20, production CI, or release verification complete.


---

## 6.1 Master completion session update — 2026-11-17

This section is the authoritative status for the completion work recorded below. It supersedes earlier “in progress” notes where the current source differs. It does **not** convert unexecuted tests or pending device checks into acceptance.

### Repository and completion status

- Repository: `xolomon-coder/DroidInspect`
- Branch: `main` only.
- Starting checkpoint for this session: `ef7e7b2efa88360c9686e0b115dc7f84dd57aafd`.
- Latest source commit before this documentation-only update: `da7451318045666032200874411b217ab34a6dca`.
- M11 remains accepted. Its device acceptance was not repeated.
- M12–M18 remain implemented for their documented supported scope; their consolidated Android 11 device acceptance remains pending. The new lifecycle changes in this session have not yet been built or regression-tested, so earlier CI passes do not verify this newer source.
- M19 workspace persistence/recovery code and the four requested host self-tests are present. **M19 is not accepted** because the current source/test suite has not completed on CI and process-restart/device recovery has not been tested.
- M20 lifecycle/cleanup hardening has been implemented across the files listed below. **M20 is not accepted** because current Android compilation, regression execution, cancellation/lifecycle instrumentation, and device acceptance have not been verified.
- No milestone is accepted solely from the commits below.

### Source implementation recorded in this session

The following source changes were committed directly to `main`:

| Commit | Main source change |
|---|---|
| `b1e336f6ee0edd05e86900953069d34eb54b7368` | `ApkSigningActivity.java`: use app context and weak UI delivery for workers; snapshot inputs; clear password fields into temporary character arrays and wipe them; verify signed output before publishing it; re-read and verify exported output; check owned temporary-file deletion and preserve cleanup failures. |
| `1fa75fbf68be823e23d605b3be406e72890c1d60` | `DotNetIlActivity.java`: detach IL load/edit/rename/export work from the Activity; use context-based file access, cancellation checks, weak callbacks, and checked cleanup. |
| `f8a67198519039a5f42346c4afe15be1602d483f` | `DotNetResourceActivity.java`: detach managed-resource parsing, extraction, and SAF export; perform output writing off the UI thread; add checked cleanup. |
| `e82e117cb81f55cbf562a3e2744b79f78613c87f` | `ApkComponentExplorerActivity.java`: detach APK manifest analysis and owned-copy cleanup from the Activity lifecycle. |
| `fd1ffd3e38a337b3474bc117bccd2586811479d7` | `ApkResourceCatalogActivity.java`: detach resource-table indexing and use bounded, checked temporary-copy cleanup. |
| `34523a2d7b3bdbe176168d5238cd235aa7f6819f` | `ApkSplitSetActivity.java`: snapshot SAF inputs, detach split-set inspection, and clean all owned copies after worker termination. |
| `81301981e06f384c53baed43b20c752390bbf8e6` | `DexAnalysisActivity.java`: correct the malformed executor launch (`execute(...).start()`) and detach DEX inspection from the Activity lifecycle. |
| `81fd3bf88bf2e387f294df4a95b7d6db8fb6a2d6` | `AssemblyComparisonActivity.java`: detach comparison inputs and parsing, protect source copies with checked cleanup, and deliver results through a weak UI reference. |
| `4e7bb6fff9e7387766f6a3c9bc5626e6f71daa44` | `AssemblyExplorerActivity.java`: move source copying/indexing/bookmark-store initialization off Activity-owned state and add checked temporary-file cleanup. |
| `96cc1894f6437c7aa4afd3405d0b2ac09d959483` | `ApkResourceActivity.java`: surface discarded-output and module-close cleanup failures instead of silently ignoring them. |
| `c10de816dc8dacbc422cfb35b93d5eadf1fdef65` | `ProjectWorkspaceMetadata.java`: make atomic metadata-writer temporary-file cleanup observable and preserve the primary failure. |
| `da7451318045666032200874411b217ab34a6dca` | `AssemblyComparisonActivity.java`: correct the report-method declaration found during post-edit source review. |

Lifecycle pattern used where applicable: background I/O receives application context and local input snapshots; UI delivery uses a main-thread handler and weak Activity reference; operations check interruption; owned temp files are not deleted until the worker has stopped using them; cleanup errors are logged or attached as suppressed errors instead of replacing the primary operation error. Operations that persist data in a project workspace keep the existing journal/rollback behavior rather than deleting user-owned project files.

These are **implemented source changes, not build-verified changes**. A text-level delimiter and method-declaration review was performed after the source edits, but it is not a substitute for Java/Android compilation.

### M19 and M20 verification inventory

All four requested M19 host tests exist and are wired into `.github/workflows/phase1-android.yml`:

1. `ProjectWorkspaceMetadataSelfTest`
2. `ProjectWorkspaceIndexSelfTest`
3. `BoundedSourceTreeSelfTest`
4. `BoundedProjectFilesSelfTest`

The same workflow also contains the other host checks for .NET metadata decoding, embedded resource extraction, IL reading, APK update-compatibility assessment, assembly-comparison diffing, assembly bookmark persistence, and DEX parsing (11 host test steps in total).

**Execution result for the current HEAD: NOT RUN / NOT VERIFIED.** The four M19 tests and the remaining host tests were inspected in source/workflow configuration, but none was executed successfully against this final source state. There is no dedicated Android Activity lifecycle instrumentation test in the current host suite; M20 lifecycle cancellation and destruction behavior still needs an actual Android runtime test.

### Production CI and artifacts

The production workflow is `.github/workflows/phase1-android.yml`. A source change triggers its `build-debug-apk` job, which is named “Host parser tests, Android compile, and debug APK”.

The latest production run associated with the latest source commit is:

- Run: [#646](https://github.com/xolomon-coder/DroidInspect/actions/runs/44116121191)
- Commit: `da7451318045666032200874411b217ab34a6dca`
- Job conclusion: `failure`
- Runner: GitHub reported `runner_id = 0` and an empty runner name.
- Executed steps: `0`.
- Artifacts: none.

The earlier source-triggered runs #635–#645 also concluded without producing a verified build for this code series. The inspected job records show the same runner-allocation/startup pattern: no runner was assigned and no workflow step ran. This does not establish a Java/Gradle source failure, but it also provides **no evidence that the source compiles or the tests pass**. The workflow job did not reach checkout, Java setup, host tests, Gradle, Android SDK setup, signing, ABI verification, or artifact upload.

Consequently, for the current source state:

- Host regression suite: **not verified**.
- Android Java compilation/resource processing: **not verified**.
- Debug APK: **not built/verified**.
- Unsigned release APK: **not built/verified**.
- CI signing/tamper verification: **not executed**.
- ARM64 native-library packaging inside the APK: **not verified**.
- Release dependency graph and final transitive license/security review: **not captured**.
- Build artifacts and checksums: **none available from run #646**.

Do not use older successful run artifacts as evidence for the newer source changes above. Re-run the production workflow on this latest `main` commit when a GitHub-hosted runner can be allocated; continue fixing any actual source or build failures shown by the resulting steps.

### Security, dependencies, and compatibility review

- Android configuration: namespace/application ID `com.tanvir.droidinspect`; `compileSdk 36`, `targetSdk 36`, `minSdk 26`; Java source/target compatibility 1.8; Android Gradle Plugin 8.13.0; the production workflow configures Gradle 8.13.
- The production manifest contains no `INTERNET`, broad storage, or legacy external-storage permission declaration. File access is based on Android's Storage Access Framework. `allowBackup` is disabled. Only the launcher Activity is exported.
- Direct Gradle dependencies: `io.github.reandroid:ARSCLib:1.4.0` and `com.android.tools.build:apksig:8.13.0`. The Gradle dependency graph was not generated for this current HEAD because the workflow never reached Gradle.
- `THIRD-PARTY-NOTICES.md` records the direct ILSpy/.NET runtime/reference and Roslyn notices. It explicitly states that a complete transitive NuGet dependency/license audit still requires the exact restore/publish graph. That audit remains open; no complete transitive vulnerability or license scan is claimed here.
- The source tree contains ARM64 native payloads for the offline compiler/decompiler path. The workflow's packaged-APK ABI and native inventory checks did not run for this HEAD. ARM64 is the supported device ABI recorded for this project; other ABIs are not claimed.
- The APK signer continues to use v1, v2 and v3 signing schemes. APK Signature Scheme v4 sidecar generation remains unsupported. The Gradle release build is configured without a production signing key; the workflow's intended CI key is ephemeral and is not a user production key.
- Size limits and parser caps remain part of the safety boundary. They do not prove that every malformed archive/PE/DEX input is harmless; unsupported or oversized inputs must continue to fail closed.
- No online decompilation service or Internet permission was introduced.

### Consolidated phone test batch — M12 through M20

Run this only after a fresh CodeAssist build succeeds. Use copies of known test APKs/DLLs/EXEs and preserve the original fixtures.

1. **Install and baseline:** install the new debug APK on Android 11 / ARM64; launch the app; confirm no crash and that offline operation does not request network access.
2. **M12 — source edit/compiler:** open a supported managed assembly; inspect C# source; create an editable working copy; edit and save a .cs file; compile; inspect diagnostics; intentionally compile invalid source and confirm the prior valid output is preserved; reopen a successful output in DroidInspect; confirm original input hashes/files remain unchanged.
3. **M13 — dependency intelligence:** test a matching local reference, a missing reference, and a framework/version mismatch; confirm the warning classification is accurate and no incompatible reference is silently substituted; repeat offline.
4. **M14 — Assembly Explorer/IL:** navigate assembly/module/namespace/type/member nodes; search names and tokens; inspect IL bytes/opcodes; test bookmarks and return navigation; reopen the same assembly and confirm bookmarks persist.
5. **M15 — C# editor:** verify line numbers, syntax highlighting, find/replace, file selection, save/reopen, and unsaved-change prompts; try a multi-file source project within the documented editor scope.
6. **M16 — comparison/resources:** compare a known identical pair and a pair with known added/removed/changed members; verify the report labels structural differences only; extract an embedded resource to a new SAF destination; compare exported bytes with the fixture and confirm neither input assembly changed.
7. **M17 — APK analysis:** inspect a known APK manifest; search components/permissions; browse all DEX files/classes/members; navigate supported invoke references; inspect the resource catalog; select a base/split APK set and review identity/dependency warnings; test oversized and malformed inputs.
8. **M18 — APK edit/build/sign:** make a supported manifest or named string edit; rebuild to a separate unsigned APK; generate an Android Keystore key and separately test imported PKCS#12/JKS; sign and verify; confirm signer details and v1/v2/v3 status; tamper with a copy and confirm verification rejects it; review update-compatibility results without overwriting the original APK.
9. **M19 — workspace persistence/recovery:** create separate APK and .NET projects; import files; add metadata/bookmarks/notes/search state where available; close and reopen each project; force-stop/relaunch the app and reopen; verify project isolation; exercise interruption/rollback and malformed metadata handling using disposable test projects; confirm valid project files remain intact.
10. **M20 — lifecycle/reliability:** start a large APK/assembly operation and leave the Activity while it runs; repeat with cancellation/back navigation; rapidly switch screens; trigger invalid input and failed export; reopen the app and projects; confirm no crash, stale UI update, partial signed result, corruption of originals, or unexplained temp-file accumulation.
11. **Final compatibility:** repeat the key workflows after a process restart; note memory pressure, operation duration, and any Android 11/CodeAssist-specific failure; send the actual build output and test observations before any M12–M20 acceptance is recorded.

**Acceptance rule:** user phone results are still pending. Passing this checklist must be recorded from actual device evidence; do not mark M12–M20 accepted based solely on CI, code review, or an installed APK.

### Immediate next action

The next cloud-side action is to run the production workflow against the latest `main` commit once GitHub provisions an actual runner, then inspect all host-test, Android build, signing, ABI, security, and artifact steps. The next user-side action is to download the current `main` ZIP and use the `droidinspect/` directory as the Gradle project in CodeAssist only after the build can be verified; the consolidated phone test batch above remains pending.


---

# Current verification checkpoint — 2026-11-18

This update supersedes earlier stopping-point and test-status notes. It records the source checkpoint inspected in this session and does not convert source review into Android build or device acceptance.

## Repository and changes

- Branch: **main only**.
- Source checkpoint before this README update: **f7371dc4e7e18c4ca7b216bf263df2b54f4d5e01**.
- Production package: **com.tanvir.droidinspect**.
- Latest production run for that source checkpoint: [run #654](https://github.com/xolomon-coder/DroidInspect/actions/runs/44260929630).
- No branches or pull requests were created.

Source commits in this continuation:
- **e2c37cdb** — restored missing APK project operation-journal methods. Three methods called by ApkProjectWorkspace had no implementation and were a source-level Java compile blocker.
- **70765204** — staged and validated APK working-file replacement, snapshot rollback, synced atomic metadata replacement, and a host transaction regression test wired into CI.
- **07c3c662** — fixed dangling-symbolic-link cleanup in the new test.
- **d4e4e896** — made APK build-pipeline cleanup failures visible in logs instead of silently swallowing them.
- **0dbc9dc2** — added byte-for-byte SHA-256 comparison of the signed source and SAF-exported APK before reporting export success.
- **9ba9f5f7** — added safe installation of a new project export without overwriting an existing file, including validation-failure cleanup.
- **bd02c714** — made existing APK project exports transactional, retaining a rollback copy and restoring the old export if later metadata persistence fails.
- **f7371dc4** — added host regression assertions for install, validation rollback, and refusing to overwrite an existing export.

The new WorkspaceFileTransaction helper rejects linked/non-regular paths, requires staging and destination to share the project directory, uses atomic moves when supported, and restores from an app-owned rollback copy without abandoning recovery merely because the worker thread was interrupted.

## Host-JVM execution

Five pure-Java self-test suites were compiled with **JDK 21**, using **javac -Xlint:all -Werror**, and each ran with exit code 0 in the local host environment:

| Test | Result |
|---|---|
| ProjectWorkspaceMetadataSelfTest | PASS |
| ProjectWorkspaceIndexSelfTest | PASS |
| BoundedSourceTreeSelfTest | PASS |
| BoundedProjectFilesSelfTest | PASS |
| WorkspaceFileTransactionSelfTest | PASS |

The first four cover bounded metadata/index/tree/file-inventory and recovery helpers. The transaction test covers staged replacement, new-file installation, validation failure rollback, interrupted rollback, path-boundary checks, and symbolic-link rejection where supported by the host filesystem.

**Scope limitation:** these are local host-JVM tests of pure-Java helpers, not GitHub Actions execution, Android Activity instrumentation, Android filesystem integration, or phone acceptance. They do not prove APK project create/import/export/reopen/process-restart end-to-end behavior.

## Production CI and APK status

[Run #654](https://github.com/xolomon-coder/DroidInspect/actions/runs/44260929630), on commit **f7371dc4e7e18c4ca7b216bf263df2b54f4d5e01**, completed with failure before any workflow step:

- Job: Host parser tests, Android compile, and debug APK.
- Job ID: 134653809718.
- Runner ID: 0; runner name empty.
- Executed steps: 0.
- Logs and artifacts: none.

Runs #647–#653 show the same runner-allocation pattern. These are **not Java/Gradle build failures**: GitHub did not start the job, so checkout, host tests, Android resource processing, Gradle compilation, APK signing, tamper verification, ABI inspection, dependency capture, and artifact upload never ran.

No current-source debug APK, release APK, signed CI APK, dependency-graph artifact, or checksum artifact is verified. The available host environment has JDK 21 but no Gradle executable or Android SDK manager, so a local Android build was not possible. Do not use an older artifact as proof for this source checkpoint.

The production workflow still includes 12 host-test steps, Android SDK 36 / Build Tools 36, Gradle 8.13 debug and release builds, v1/v2/v3 CI signing and verification, tamper rejection, native ABI/size checks, checksums, and artifact upload. README-only changes are ignored by the workflow path filter, so this documentation commit does not constitute a new build attempt. Retry only when GitHub can allocate a real runner.

## Lifecycle and signing review

The source-level review covered the main file-owning/background-work Activities: APK signing/build/resources/projects, DEX, assembly comparison/explorer, C# decompilation/source editing, .NET IL/resources/dependencies, APK components/splits/catalog, and the main inspection screen.

- Activities generally use single-thread workers, application context for file work, weak Activity references for UI delivery, and destroyed/finishing checks before UI updates.
- Signing, APK pipeline/resources, comparison/explorer, DEX, .NET IL/resources, APK components/splits/catalog use post-destruction cleanup workers that wait for owned worker termination before deleting app-owned cache files. No Activity waits for worker termination on the UI thread.
- Native AOT decompilation/compilation cannot be reliably interrupted inside synchronous JNI. Those workers may finish their private workspace operation; weak UI callbacks are discarded after Activity destruction.
- Source/dependency imports use bounded copies, cancellation checks, stream closure, and rollback of partial batches. Dependency-resolution cancellation deliberately leaves an in-progress operation marker for recovery reporting on the next open.
- APK signing source review confirms generated non-exportable Android Keystore RSA keys, PKCS#12/JKS import paths, password clearing and char-array wiping, separate signed output, v1/v2/v3 enablement, and ApkVerifier checks. Export also compares SHA-256 of the signed source with the SAF-exported copy before success.
- Cleanup failures are logged or attached as suppressed errors in the inspected paths. User-selected original files and SAF destination documents are not intentionally deleted during Activity teardown.

This is source review, not lifecycle instrumentation. Activity destruction during long native or I/O operations, storage-provider failure behavior, and Android 11 process death still require phone testing.

## Security and compatibility snapshot

- Production manifest review: no INTERNET permission declaration; allowBackup is false; feature Activities are not exported; only the launcher Activity is exported. File selection uses Android document-picker flows.
- Build configuration: min SDK 26, compile/target SDK 36, Java 8 source/target, Android Gradle Plugin 8.13.0, Gradle 8.13.
- The tracked production native payload is ARM64-only (arm64-v8a), approximately 81.3 MB across four engine/JNI shared objects. This is a source-tree inventory only; actual APK packaging and compressed APK size remain unverified until CI builds.
- Direct notices identify ILSpy 11.2.0.10050 and Roslyn 5.12.0 as MIT-licensed; the Android Gradle file identifies ARSCLib 1.4.0 and apksig 8.13.0 as Apache-2.0. The complete transitive NuGet license/security review remains open because the exact restore/lock graph was unavailable. Existing notices do not claim the transitive audit is complete.
- Offline-first operation is preserved; no Internet permission or online decompilation dependency was added.

## Milestone state

| Milestone | Status | Evidence / remaining gate |
|---|---|---|
| M11 | **ACCEPTED** | Previously accepted by the user. No M11 retest was performed. |
| M12–M18 | **Implementation-integrated; device acceptance pending** | Existing supported scope remains recorded above. Current source is not build-verified; consolidated device tests remain pending. |
| M19 | **IN PROGRESS — host tests pass locally** | Four requested host suites plus the transaction test pass. Android project create/import/export/reopen, process-restart persistence, project isolation, and recovery remain unverified end to end. |
| M20 | **IN PROGRESS — source review and helper regression pass** | Activity lifecycle/file-owner paths reviewed and targeted fixes committed. Android lifecycle instrumentation/build/device execution remains open. |
| Production release gate | **BLOCKED / NOT VERIFIED** | GitHub runner allocation fails before step 1; no current debug/release APK or artifact. |
| Transitive NuGet audit | **OPEN** | Exact restored dependency graph and generated native notices still need review. |

No milestone M12–M20 is accepted by this update. Acceptance requires consolidated Android 11 / CodeAssist results from the user.

## Next verification and device acceptance

When a production runner is available, run the workflow against the latest source on main and inspect every host test, Android resource/Java compile, debug and release APK, native packaging/ABI, signature/tamper check, dependency graph, SHA-256 manifest, and uploaded artifact. Fix genuine failures and repeat against the resulting final source commit.

Only after an artifact exists for the final source checkpoint:

1. Download the latest main ZIP and open the droidinspect/ directory as the project in CodeAssist; build/install the current debug APK on Android 11 / Infinix X689B (ARM64).
2. Test M12 with valid and invalid multi-file C# projects; confirm a failed compile does not replace a previously valid DLL. Test M13 offline dependency matches, missing references, and version-mismatch warnings.
3. Test M14–M18 within the supported scope: assembly navigation/comparison, source editing, APK manifest/resource edits, split/resource/component inspection, APK build pipeline, PKCS#12/JKS import, Android Keystore generation, signing/verification, tamper rejection, and preservation of original inputs. Do not repeat M11 acceptance.
4. Test M19 with disposable APK and .NET projects: create, import, notes/bookmarks/search, export, close/reopen, force-stop/relaunch, project isolation, history restore, malformed metadata warning, and interrupted-operation rollback.
5. Test M20 by closing Activities during long file operations/native compilation, cancelling imports/exports, rapidly navigating, and reopening projects. Confirm no crash, stale UI, original-file changes, partial project revision, unsafe user-document deletion, or persistent owned temporary files.
6. Record build ID, device/API, actual outputs, warnings, and PASS/FAIL per case. Do not record M12–M20 acceptance until those results exist.

**Current next action:** production build verification remains blocked by GitHub runner allocation. Once a verifiable APK is available, perform the single consolidated phone acceptance batch; previously accepted M11 device tests are not to be repeated.


## Final journal consistency follow-up — 2026-11-18

Source commit **dc554bdfca6bedf0b9a198d2f402ac6c284ac7c1** closes one additional M19 edge case in ApkProjectWorkspace: if APK import/revision restoration fails before a rollback snapshot is created, the operation journal is now explicitly finished with the unchanged-working-file outcome. APK export failures also close their journal record when the prior export state remains intact; if rollback or cleanup errors are suppressed onto the primary failure, the STARTED marker is deliberately retained for recovery review.

Production run [#655](https://github.com/xolomon-coder/DroidInspect/actions/runs/44262500000) targets this source commit and has the same external failure: its sole job (134658976217) completed with runner ID 0, empty runner name, and zero steps. Therefore no Java/Gradle/Android result is available for this additional source commit.

**Latest source checkpoint:** dc554bdfca6bedf0b9a198d2f402ac6c284ac7c1. The M19 host self-tests listed above remain valid for the unchanged pure-Java helpers; the new journal wiring is Android-project code and still awaits an Android build and device recovery test. All previous milestone acceptance restrictions remain in force.


## Regression-test deduplication follow-up — 2026-11-18

Source commit **82eb4a474c8d39ec88ad2456b32d2f63afa413f7** removes a duplicated block of APK workspace transaction assertions from `WorkspaceFileTransactionSelfTest.java`. The duplicate repeated the same new-export success, validation-failure cleanup, and overwrite-refusal cases; retaining it added no coverage and obscured the actual test sequence. The original assertions remain once, followed by restore, replacement rollback, interruption, path-boundary, and symbolic-link checks.

This is a test-maintenance correction only; it does not claim new M19 runtime coverage. The production workflow `.github/workflows/phase1-android.yml` was inspected on main and contains 12 host-JVM self-test steps followed by Android SDK 36 setup, release dependency capture, debug/release Gradle builds, CI v1/v2/v3 signing and verification, tamper rejection, ARM64/native payload checks, checksums, and artifact upload. The workflow uses `ubuntu-latest` and is triggered by pushes to main except README-only changes. The test-source commit therefore requests a fresh production CI run.

Run #655 has no artifacts. Its job summary reports runner ID 0, no runner name, and no steps; this remains a GitHub runner allocation/startup failure, not a source compilation result. The just-committed test cleanup has not yet been compiled or executed by CI. Android build, release signing verification, dependency graph capture, and M12–M20 device acceptance remain unverified. The exact transitive NuGet graph is still unavailable and its license/security audit remains open.

**Current source/documentation checkpoint:** `82eb4a474c8d39ec88ad2456b32d2f63afa413f7` (test-source cleanup). No branch other than `main` was created. Next action: inspect the CI run triggered by this commit; if the runner starts, fix any actual host-test or Android build failures. If runner allocation again fails before step 1, preserve the evidence and continue independent source/test audits without claiming build success.


## Current continuation checkpoint — 2026-11-18

- Inspected source HEAD before this update: `f73e6b697729f87d69620f13f47edb6d3684314f`.
- Corrected malformed Java character literals in `ProjectWorkspaceMetadata.java` and `ProjectWorkspaceIndex.java`.
- Removed the pull-request trigger and the Actions artifact-upload step from `.github/workflows/phase1-android.yml`. The workflow remains available for pushes to `main` and manual dispatch.
- Added stable step IDs and an always-run GitHub Actions job summary reporting each host test, dependency resolution, debug/release compilation, signing/tamper verification, and payload/security check. Skipped checks are reported as skipped.
- CI does not publish APKs, ZIPs, or reports. Runner-local APKs and temporary audit outputs are discarded when the runner ends.
- **Verification state at this checkpoint:** repository edits are prepared for commit; no new Actions execution or Android build result is claimed here. The next step is to inspect the resulting main commit and its workflow run, if a runner is assigned.
- **Milestone state unchanged:** M11 remains accepted. M12–M18 device acceptance remains pending; M19 and M20 remain in progress pending complete verification and the consolidated phone test. The transitive dependency/license audit remains open.
- **Next action:** inspect every step of the artifact-free CI run, fix any demonstrated failures on `main`, and only then prepare the consolidated M12–M20 CodeAssist device-test session. Do not repeat M11 acceptance tests.


## Latest CI compile finding — 2026-11-18

The first runner-backed execution of the artifact-free workflow completed all 12 host-JVM test steps successfully, resolved the release dependency graph, and reached Android debug compilation. Javac then found four `unclosed string literal` errors in `MainActivity.java` lines 112–117: the initial help text contained raw line breaks inside quoted Java strings. The correction replaces those raw line breaks with explicit Java `\n\n` escapes without changing the displayed text or menu behavior.

The Gradle setup action is also configured with `cache-disabled: true` to follow the no-unnecessary-cache policy. The next push will rerun the full host and Android verification. No release build, signing, tamper, payload, or device acceptance is claimed until the new run reaches and passes those checks.


## Second Android compiler finding — 2026-11-18

After fixing the MainActivity help-text literal, the next debug compile exposed additional integration defects that had been hidden by the earlier compiler failures:

- `FileInspectionEngine.java` and `ApkSigningActivity.java` use `IOException` but lacked its import.
- `DotNetIlActivity.java` checks a `busy` state but did not declare/store that state.
- `CSharpDecompileActivity.java` referenced a local `ui` Handler outside the method where it was declared.

These are being corrected narrowly: add the missing imports, make the IL activity's busy flag explicit and synchronize it in `setBusy`, and capture the activity's existing main-thread Handler in the source-workspace method. The prior run's 12 host tests passed; Android release compilation, signing, tamper and payload checks were skipped after debug compilation failed. No successful Android build is claimed yet.

---

## Latest M19–M20 and CI continuation checkpoint — 2026-11-21

**Branch:** `main` only.  
**HEAD immediately before this documentation-only commit:** `61d8615c0b1639524326ba184572ae6c87fab257`.  
**Latest source-hardening commit:** `35d28c260c17156362a53d99a6e6a1ebcafdcca4`.  
**Latest production CI workflow commit:** `549598ba46952c86ef5696d3a5c1dac4f0598d96`.

### Verified source changes in this continuation

1. **M19 metadata concurrency:** `ProjectWorkspaceMetadata` now obtains a shared lock keyed by the canonical project path. Separate Activity/store instances for the same project serialize notes, bookmarks, searches, history, recovery-marker changes and overview reads. The host regression now launches eight threads using separate store instances and checks that all 160 concurrent journal records survive.
2. **M20 project index responsiveness:** `ProjectWorkspaceActivity` scans APK, C# source and dependency workspaces using a single background executor. Results are posted to the main thread; a generation counter rejects stale results; destruction invalidates outstanding results and shuts the executor down. The Java newline literals in the resulting status and warning messages were corrected and the file was cleaned up.
3. **Repository-wide workflow artifact removal:** direct reads of all four current workflows (`phase1-android.yml`, `dotnet-rename-fixture.yml`, `m11-nativeaot-poc.yml`, and `m12-roslyn-compiler-poc.yml`) confirm zero `actions/upload-artifact` and zero `actions/download-artifact` steps. Runner-local build products are used only for verification. Verified native engine/JNI source payloads may still be committed to `main`; this is source needed by the app, not a workflow artifact.
4. **Production CI:** the main workflow restores three .NET 11 projects, prints their full transitive NuGet package graphs, lists known vulnerability advisories, and extracts license expressions/files/URLs from restored nuspec files. Packages with missing license metadata are marked for manual review. The audit outcome is included in the job summary. A fixed concurrency group cancels superseded production CI runs.
5. **Documentation:** this README contains the consolidated M12–M20 Android 11/CodeAssist acceptance checklist. M11 is intentionally excluded because the user already accepted it.

### Verification status — do not infer success

- Run #660 (`44302103843`) failed before job creation. Its job and status endpoints returned no jobs/checks. The commit diff showed `Publish verification summary` incorrectly placed outside the job's `steps` list; commit `bfcaf647135d1070e0fc337113dfb04e96b7fb5b` fixed the indentation. No exact parser log was obtainable.
- The subsequent MainActivity Java literal and Android compile integration findings were fixed in `6cd93e1` and `73fb7c3`.
- No status checks were returned for the source/workflow commits in this continuation. The available GitHub connector does not expose push-triggered run listing, and the latest push job IDs/logs could not be obtained. Therefore the new concurrency test, all current host tests, the NuGet audit, Android debug/release compilation, CI signing/tamper verification and native packaging remain **NOT VERIFIED**.
- The NuGet audit machinery is present, but no package graph, vulnerability result or final license conclusion has been inspected. Do not claim the dependency audit is complete.
- M12–M18 Android 11 device acceptance remains pending. Documented limitations still include Native AOT trim-analysis warnings, the M15 lightweight file-selector model rather than a tabbed editor, M16 structural/raw-signature comparison and linked-resource limitations, M17 split-set inventory without merge/install, and no M18 DEX bytecode editing.
- M19 and M20 are **IN PROGRESS**, not accepted. Cross-instance metadata serialization and asynchronous project indexing are source changes only until their new tests and actual phone behavior are verified.

### Exact current next action

Inspect the first production `main` push run after workflow commit `549598ba46952c86ef5696d3a5c1dac4f0598d96`. Inspect every host test, NuGet restore/audit output, Android debug and release build, signing/tamper check, native ABI/payload check and job summary. Fix only verified failures directly on `main`; do not create branches or PRs and do not upload artifacts.

Only after that CI run is demonstrably green should the user download the [latest main ZIP](https://github.com/xolomon-coder/DroidInspect/archive/refs/heads/main.zip), import `droidinspect/` into CodeAssist, build and install locally, and perform the one consolidated M12–M20 acceptance batch recorded above. Preserve/export important app-private workspaces before any reinstall that could be required by a signing-key mismatch. M11 must not be retested without a concrete regression.

**Final state at this checkpoint:** source hardening and artifact-free workflow configuration are committed; CI, dependency audit, M19/M20 runtime verification and final device acceptance remain open. The project is **NOT COMPLETE**.

## Android 11 device acceptance — M11 C# decompilation on Serilog.Extensions.Logging.dll (2026-11-23)

**Result: C# source reconstruction PASS WITH DEPENDENCY WARNINGS.**

The user tested the production app's **Decompile to C# source** flow with `Serilog.Extensions.Logging.dll` (previously inspected as a .NET assembly targeting .NET 9.0). The app produced substantial readable C# including assembly attributes, namespaces, classes, methods, fields, properties, generic types, nullable annotations, a record struct, lambdas and switch expressions. This confirms that the decompiler can reconstruct useful source for this fixture.

The dependency diagnostic correctly reported that several required .NET 9.0 references were not resolved from the configured search directories. In particular, available `System.*` and `Microsoft.Extensions.*` candidates were version 11.0.0.0 and therefore identity-mismatched; `Serilog, Version=4.2.0.0` and `Microsoft.Extensions.Logging, Version=9.0.0.0` were not found. The output explicitly stated that source reconstruction does not mean dependencies were resolved. Some reconstructed methods contain `Unknown result type` comments, consistent with incomplete reference resolution.

**Interpretation and limits:**
- Source generation itself passed; dependency resolution for this fixture did not.
- The warnings are meaningful compatibility diagnostics, not proof that the DLL is corrupt or that decompilation failed.
- The reconstructed source is useful for inspection but may contain incomplete or approximate type information where references are missing.
- 
- **C# source search:** on the same fixture, the user confirmed that Find in C# Source can find the method name `AddSerilog`. Pressing **Next** moves to the next match. Search does not automatically jump to a match immediately when the query is entered; explicit Next navigation is required. This confirms basic search and manual next-match navigation. The user further confirmed that pressing Next scrolls the source view to the matching `AddSerilog` location and visibly highlights the matched text. - - **Editable C# workspace entry point:** the decompiler screen displays `Separate C# source project is ready` and a `CREATE / OPEN EDITABLE C# WORKSPACE` control. This confirms that the workspace entry point is present, but the user subsequently confirmed that tapping the control opens the editable C# workspace and that reconstructed source is visible in the editor. This confirms workspace launch and source loading. The user then reported an editor usability blocker: when the keyboard opens, it covers the code/editor content. As a result, the save/edit acceptance test could not proceed reliably. Saving edits and compiling edited source remain untested. Track this as a device-confirmed C# workspace keyboard/IME layout bug: the editor must resize or scroll/adjust its visible area when the soft keyboard is shown, so the active code and editing controls remain accessible.
**C# source page navigation:** the user confirmed that the source viewer provides Previous and Next page controls. Page navigation is accepted as working; this does not establish any particular page size or boundary behavior.
- **C# source search Previous navigation:** the user confirmed that Previous moves backward to the prior occurrence and highlights the matching text. Both Previous and Next match navigation are therefore device-accepted.
The search UI does **not** display a total-match count or current-match position such as `2 of 5`; the user reports that the position is shown/referenced as a character number instead. Treat match-count/current-result numbering as NOT IMPLEMENTED or NOT ACCEPTED pending a dedicated UI improvement. Previous-match navigation remains unverified.

This test does not establish that the emitted source compiles, that it can be recompiled into a loadable assembly, or that a rebuilt assembly is equivalent to the original.
- Do not claim pagination, source-search highlighting, compilation, or round-trip fidelity from this result; those behaviors were not established by the supplied test output.

**Next engineering direction:** improve framework/reference-pack discovery so the resolver can locate matching .NET 9 reference assemblies and the required Serilog/Microsoft.Extensions dependencies when supplied or available offline. Preserve explicit unresolved/mismatched-reference diagnostics when they are unavailable. Keep the existing no-network/offline behavior; do not silently substitute .NET 11 assemblies for .NET 9 references.

This is a device-test finding and follow-up engineering requirement, not a claim that dependency resolution or source recompilation has been implemented. The project remains **NOT COMPLETE**.


## Android 11 device acceptance — large Unity assembly editable-workspace crash (2026-11-23)

The user clarified the scope of the C# editor crash:

- Small decompiled source files open in the editable C# workspace successfully.
- A DLL from a Unity game, approximately 1+ MB, decompiles successfully in the source viewer.
- Tapping the Edit / create-open-workspace control for that large source causes a black screen followed by an app crash.
- The keyboard/IME also covers the code in the editor, independently blocking comfortable editing.

**Working hypothesis (not yet confirmed):** the editable workspace may be loading/rendering the entire large reconstructed source in one editor view. The source viewer already uses page navigation, so applying bounded, page/chunk-based loading to the editor may reduce peak memory and UI rendering pressure. File size alone is not proof of the cause; investigate source character count, allocation/heap pressure, editor widget/layout limits, syntax-highlighting work, and workspace initialization/crash logs before deciding the fix.

**Required investigation and engineering direction:**
1. Reproduce with both a small working fixture and the large Unity DLL/source; capture the exception or crash trace.
2. Inspect whether the editor creates one large text/layout object or performs expensive whole-document processing during workspace initialization.
3. Prefer a scalable paged/chunked editing model if the evidence supports it: load a bounded visible region, preserve edits across page transitions, and retain whole-document search/navigation semantics.
4. Fix IME behavior so the editor resizes or scrolls to keep the active code and editing controls visible while the keyboard is open.
5. Preserve the original assembly and source; do not silently truncate large source files.

**Acceptance status:** small-file workspace launch/source display PASS; large-file decompilation PASS; large-file editable-workspace launch FAIL (black screen/crash); editor keyboard visibility FAIL; edit/save/recompile remain untested. The suspected memory/rendering cause and paged-editor solution are hypotheses, not implemented fixes. Do not mark C# editing as accepted until large-file opening and keyboard behavior are retested on the Android 11 device.


## C# editable workspace — first large-file and keyboard mitigations (2026-11-23)

The production editor received an initial mitigation on `main` after the Android 11 device reported that small source files open but a roughly 1+ MB Unity-game DLL decompiles successfully and then crashes to a black screen when opening the editable workspace.

**Changes committed:**
- `CSharpSourceEditorActivity` now enters large-file mode when the loaded C# source exceeds 250,000 characters. In that mode, it skips whole-document syntax highlighting and hides the full-document line-number gutter. The source is still loaded in full; no text is truncated or discarded.
- The production manifest now sets `android:windowSoftInputMode="adjustResize"` for `CSharpSourceEditorActivity`, asking Android to resize the available editor area above the soft keyboard.

**Why these changes:** the previous text watcher applied syntax spans over the entire document after text changes, and the line-number gutter built a second string containing every line number. Those operations can amplify memory and UI work for large generated source files. The keyboard behavior previously had no explicit resize policy on the editor activity.

**Commits:** editor mitigation `14e55ae231575d99d7c6687a21aa83e83434e4fd`; keyboard resize `07d3fe313a54ab1074fae0bac9527fcf8c7b0da2`.

**Verification status:** source edits are committed, but Android compilation/CI and device retesting have not yet been verified. This is a mitigation, not proof that the large-file crash is fixed. The editable workspace still uses a single full-file `EditText`; true paged/chunked editing has not been implemented. If the crash persists, capture the Android crash trace and implement bounded editor paging with safe edit persistence across page changes. Retest large Unity source opening, typing, save/reopen, search/replace, compilation, and keyboard visibility before accepting C# editing.


## C# editor — bounded page editing implementation (2026-11-23)

The first large-file mitigation was not sufficient: it disabled whole-document syntax highlighting and line-number generation, but still loaded the complete reconstructed source into one Android `EditText`. The editor also performed file reading synchronously while switching files.

**Production implementation now on `main`:**

- Added `PagedSourceDocument`, which reads UTF-8 source files off the UI thread and divides the document into bounded 16,000-character pages. Unicode surrogate pairs are kept together at page and reader-buffer boundaries.
- The large-file editor now places only the active page in the Android `EditText`; Previous Page / Next Page controls navigate the complete source without truncation.
- Before leaving a page, the active page is synchronized into the document model. Edits are retained across page navigation and reconstructed in order when the full source file is saved.
- Whole-file Find, Replace Next and Replace All operate against the complete document model, including matches that cross page boundaries. Search navigation opens the page containing the match.
- Source loading uses the editor's background I/O executor. Save writes all pages to a unique temporary file and replaces the source file only after the complete write succeeds. The original assembly and the separately preserved untouched decompilation remain unchanged.
- Small files retain the single-view editing experience. Large files use the bounded page view; syntax highlighting and the full-document line-number gutter remain disabled in that mode.
- The editor toolbar was compacted to leave more vertical space for the code viewport. The activity uses `adjustResize`, and the code input opts out of the keyboard's full-screen extract editor so the visible code area can resize and scroll normally.

**Host regression coverage added to production CI:** large multi-page load, exact page-boundary search, replacement across a page boundary, page edit persistence, page-size bounds after edits, complete-source save/reopen, small-file save/reopen, empty files, and supplementary Unicode handling at a buffered-read boundary.

**Verification status:** the earlier CI run #681 passed all 27 steps for the initial mitigation commit `07d3fe313a54ab1074fae0bac9527fcf8c7b0da2`. The new paged-editor implementation and its additional host test require their own complete CI run; that result is not yet recorded here. Android 11 device acceptance is also pending. Do not treat the large Unity DLL crash or keyboard overlap as fixed until the new CI run passes and the user verifies the editor on-device.



**Follow-up implementation hardening:** forward Find now scans page chunks with a bounded overlap buffer instead of flattening the complete document for every search. Replace All carries only the necessary cross-page search overlap while rebuilding the result, and Replace Next updates the affected page range without first creating a second full-source string. Page controls also recalculate after edits that expand the active page beyond the configured page size. Latest source hardening commit at this update: `9e6233c5cf358e5f0878b12f39332821d46cef5e` (test coverage for cross-page range replacement).


## Android 11 device acceptance — paged C# editor and search follow-up (2026-11-24)

The user installed the current production build on the Android 11 Infinix test phone and confirmed the following large Unity DLL editor behaviors:

- The approximately 1+ MB Unity DLL decompiles and its editable C# workspace opens without the previous black-screen crash.
- Previous Page / Next Page navigation works.
- An edit remains after navigating away from a page and returning.
- Typing and scrolling work while the keyboard is visible; the active code is not obscured in the tested workflow.
- Saving completes successfully.
- Exiting and reopening the editable workspace preserves saved changes.

**Device acceptance:** large-file editor opening, page navigation, edit persistence across pages, keyboard visibility for the tested workflow, save, and reopen are PASS. The user noted that keyboard/editor ergonomics should be improved later; track automatic cursor visibility, viewport resizing, and compact toolbar behavior as UX follow-up rather than a current blocker.

**Search acceptance: FAIL.** The user reported that Find returns only one result and offers no Next control. Source inspection confirmed that the editor's original Find dialog exposed a one-shot FIND NEXT action and closed after one search. The underlying paged model already supported whole-document forward/backward search, including page-boundary matches, but the UI did not provide persistent result navigation or match counts.

**Search repair committed to main:**
- e8d9286b6524e4f098aa5e7de9bfa326b4961d74 — persistent Find dialog with Search, Previous, and Next controls, result highlighting, current match number, total match count, character position, and page position.
- 7c256da8df8ee11b51c9bd7f1ce491de68727a11 — bounded page-aware match counting without flattening the complete source document.
- 40ae9c36e9b7e4362bc4b3f18c7bfc1d00dcab9c — host regression assertions for match totals and selected-result ordinals.

**Verification state:** the search changes are present on main. The latest previously confirmed green CI run predates these search commits; the new model/UI changes must receive a fresh CI verification. Android 11 retest of the repaired search UI is also pending. Do not mark search accepted until both the host test/Android build pass and the user verifies multiple results, Previous/Next navigation, page-crossing matches, match count/ordinal, and visible highlighting on-device.

**Next actions:** verify the latest main CI run for the search commits; fix any demonstrated failure on main; then provide the updated main ZIP for a focused Android 11 search retest. Do not repeat already-passed large-file open, page-edit, save, or reopen tests unless a regression appears. The project remains **NOT COMPLETE**.
