# DroidInspect

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

Last project-plan update: 2026-11-12

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

## 4. Complete Roadmap — 10 Milestones

The roadmap has **10 milestones**, grouped into three phases. The sequence may be refined only when a verified technical blocker requires it; document the reason and preserve the original goal.

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

**M5 — .NET code decompilation and IL browsing**
- Prove a practical Android-compatible execution route for a .NET decompiler engine.
- Browse assemblies, namespaces, types, methods, properties, and fields.
- Display decompiled C# and/or IL for supported assemblies.
- Add text search and navigation.
- Exit criteria: representative managed DLL/EXE files decompile on-device with reproducible results. If the runtime bridge is not viable, document the alternative scope before proceeding.

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

**M8 — APK rebuild and signing**
- Rebuild supported APK changes into a new APK.
- Validate archive integrity, manifest/resources, and DEX payload.
- Provide signing using an appropriate user-controlled signing key and explain that a rebuilt APK will not retain the original developer's signature.
- Do not imply that every APK can be rebuilt.
- Exit criteria: a controlled sample APK can be rebuilt, signed, installed for testing, and its changed behavior/content verified.

**M9 — .NET export, file tools, and advanced search**
- Export supported modified assemblies.
- Add a hex/binary viewer for safe inspection, with editing only after bounds and output validation are implemented.
- Add cross-file search/usage navigation where supported.
- Verify exported assemblies can be reopened and inspected.
- Exit criteria: representative exports are valid and all implemented file tools pass their tests.

**M10 — Advanced analysis, debugging feasibility, hardening, and release**
- Investigate whether any useful debugging or runtime-analysis features are possible within Android's security and runtime constraints. Treat full dnSpy-style attach/debug as optional and potentially unsupported.
- Improve crash handling, large-file behavior, cancellation, recovery, and diagnostics.
- Complete regression tests, permission review, dependency/license review, documentation, and release packaging.
- Exit criteria: all committed features have device-test evidence; remaining limitations are explicit; README and handoff documentation match the released state.

## 5. Development and Creation Process

This is the required process for building DroidInspect:

1. **Repository-first:** inspect the latest `main` branch, commit, README, build files, and source before every implementation task.
2. **Feasibility before dependency adoption:** create small, isolated on-device prototypes for risky engines. Do not add a major decompiler/rebuilder dependency until its Android compatibility is demonstrated.
3. **Phase-level delivery:** when the user asks to complete a whole phase, continue through every milestone in that phase in sequence on `main`, using milestone boundaries as internal checkpoints rather than stopping for a routine handoff after each milestone. Do not arbitrarily expand scope beyond the phase. Pause early only when a user decision, missing fixture, safety constraint, or essential device-only validation genuinely blocks further work; explain the blocker and ask whether to defer that check and continue. Keep the ten-milestone roadmap visible.
4. **Plan exact changes:** list files to add/change, dependencies and exact versions, permissions, compatibility effects, and test cases before coding.
5. **Implement phone-friendly code:** provide complete replacement files with full paths; avoid partial snippets and desktop-only instructions.
6. **Cloud verification:** run the configured host tests and GitHub Actions on the latest `main` source; fix failures before handoff. CI artifacts are for verification, not the normal user delivery route.
7. **Source handoff:** provide the repository page, latest `main` commit, and direct `main` ZIP link. Give exact instructions for importing the ZIP or updating/replacing the required files in CodeAssist.
8. **User build and device test:** the user builds in CodeAssist, installs the locally built APK, and runs the agreed test cases. A cloud build alone does not pass functional criteria.
9. **Analyze evidence:** inspect user-provided build output, reports, logs, and results; fix failures directly on `main`, rerun CI, and provide an updated ZIP handoff.
10. **Document and hand off safely:** update this README with status, decisions, changed files, test evidence, limitations, latest `main` commit, exact stopping point, and next action.

No milestone is complete solely because code was written or committed. When delivering a full phase, complete implementation and available automated verification for all milestones in that phase before the normal ZIP handoff. Device-only verification may be collected as a clearly documented user test batch after handoff when it does not block safe implementation of later milestones; never claim device tests passed without user evidence.

## 6. Current Project Status (2026-11-12)

**Current phase: Phase 2 — Code and resource analysis. Active milestone: M4 — APK DEX analysis and code browsing.** The M4 structural browser implementation is committed and GitHub Actions CI passed. The remaining immediate gate is installation and functional verification on the Android 11 test phone.

Phase 1 implementation has passed cloud CI and initial Android 11 device smoke checks. The user chose to proceed without the optional native-PE fixture test. Native/non-managed PE rejection therefore remains an explicit unverified limitation; do not claim full M3 classification coverage. M2 has one successful real APK test, and M3 has one successful CLI-bearing mixed-mode .NET DLL test. Phase 2 may begin while this gap remains documented for later regression coverage.

The project remains the existing Java + native XML Android application. The original ten-milestone roadmap and project goal are unchanged. Development branch: `main`. M4 dashboard Java fix: `6f67cad16e4dab99cc2e13b7a49dbcd8d058eaa9`. Main HEAD immediately before this handoff documentation refresh: `346b1cb63a2e90fb29b09d122ab173b1024a6e02`. The documentation refresh itself creates a newer main commit; use the latest repository HEAD when continuing.

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
| Phase 2 / M4 | IMPLEMENTED; CI PASS — Android 11 device verification pending |

### M4 — APK DEX analysis and code browsing (implementation + CI PASS; device verification pending)

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

**Verification state (2026-11-12):** GitHub Actions run #64 (`43092921972`) PASS. Existing ECMA-335 host test PASS; DEX parser host test PASS; Android 36 resource processing and Java compilation PASS; debug APK output verification PASS; artifact upload PASS. Artifact: `DroidInspect-Phase2-M4-debug`, ID `13888828737`, size 44,218 bytes, SHA-256 `5cfbc700f9242ca014483ab62e537b61010a0c8c6dfc1a2d6692163ddd3bb0f3`, expires 2026-11-25 20:04:22 UTC. This proves host fixture behavior and cloud packaging, not runtime behavior on Android 11. The CI artifact is retained as a verification record; it is not the normal user delivery method. No Android 11 device test has yet been performed for the new browser. M4 remains OPEN until the user downloads the latest `main` ZIP, builds in CodeAssist, installs that locally built APK, and completes the agreed device checks.

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

### Dependencies and compatibility

- No new Android runtime dependencies have been added.
- Existing build configuration: Android Gradle Plugin 8.13.0, Gradle 8.13, compileSdk 36, minSdk 26, targetSdk 36, Java 8 source/target compatibility.
- CI uses Java 17 to run Gradle and host-side tests.
- User development environment for this session: CodeAssist 3.36.1; test device Android 11.
- SAF is used instead of broad storage permissions.

### Important source files

- `droidinspect/app/src/main/java/com/tanvir/droidinspect/MainActivity.java` — dashboard, SAF picker, background execution, diagnostics.
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/FileInspectionEngine.java` — safe temporary copy, APK archive/PackageManager inspection, file-signature detection, PE/CLI inspection.
- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DotNetMetadataReader.java` — bounded ECMA-335 metadata reader for module, assembly, references, types, fields, and methods.\n- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DexFileParser.java` — bounded DEX structural parser for multidex, classes, fields, and methods.\n- `droidinspect/app/src/main/java/com/tanvir/droidinspect/DexAnalysisActivity.java` — DEX browser UI/controller and background analysis.
- `droidinspect/app/src/main/res/layout/activity_main.xml` — dashboard and report layout.\n- `droidinspect/app/src/main/res/layout/activity_dex_analysis.xml` — DEX class/member browser layout.
- `droidinspect/app/src/main/AndroidManifest.xml` — launcher activity and permissions.
- `droidinspect/app/build.gradle.kts` — existing Android build configuration.
- `.github/workflows/phase1-android.yml` — CI build, host parser test, and debug APK artifact.
- `tools/metadata-reader-test/DotNetMetadataReaderSelfTest.java` — synthetic metadata fixture and host-JVM decoder test.\n- `tools/dex-parser-test/DexFileParserSelfTest.java` — synthetic multidex ZIP and malformed DEX host-JVM regression test covering class, field, and method metadata.

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

**Exact stopping point — 2026-11-12**

- Repository: https://github.com/xolomon-coder/DroidInspect
- Branch: `main`.
- Main HEAD immediately before this README handoff refresh: `346b1cb63a2e90fb29b09d122ab173b1024a6e02`. The README refresh creates a newer commit; inspect latest `main` before any changes.
- Latest M4 source fix before the documentation refresh: `6f67cad16e4dab99cc2e13b7a49dbcd8d058eaa9`.
- Android package: `com.tanvir.droidinspect`.
- Android module: `droidinspect/app`.
- User IDE: CodeAssist 3.36.1.
- Test device: Android 11 / API 30.
- Language/UI: Java + native Android XML.
- M4 CI: GitHub Actions run #64, ID `43092921972`, PASS.
- CI artifact (verification record only; not the normal user delivery): `DroidInspect-Phase2-M4-debug`, ID `13888828737`, expires 2026-11-25 20:04:22 UTC.
- CI artifact SHA-256: `5cfbc700f9242ca014483ab62e537b61010a0c8c6dfc1a2d6692163ddd3bb0f3`.
- Workflow page: https://github.com/xolomon-coder/DroidInspect/actions/runs/43092921972
- Artifact ZIP was downloaded and checked; the generated debug APK was also extracted and checked for ZIP integrity. The user has not yet reported installing or functionally testing this M4 build.

**What is complete**
- Phase 1 M1–M3 accepted for progression, with native/non-managed PE rejection and pure-managed .NET fixture still explicitly unverified.
- M4 bounded DEX structural parser and native XML browser implemented.
- Multidex discovery, package/class/member browsing, search, malformed-entry reporting, background processing, and safety caps implemented.
- No new Android runtime dependencies or broad storage permissions.
- GitHub host tests, Android compilation, APK output verification, and artifact upload all passed in run #64.

**What is not complete**
- M4 has NOT passed Android 11 device verification.
- The current browser is a structural DEX/member browser, not a Java decompiler. It does not decode instruction bodies or reconstruct Java source.
- Real-world large/obfuscated APK performance and unusual Modified UTF-8 identifier handling remain to be evaluated.
- Do not start M5 until the M4 device test is complete or the user explicitly chooses to defer it.

**Immediate next action — continue, do not restart**
1. Inspect the latest `main` HEAD and README; inspect `DexFileParser.java`, `DexAnalysisActivity.java`, `MainActivity.java`, their XML layouts, manifest, Gradle config, and CI workflow before making any change.
2. Give the user the latest `main` branch ZIP (https://github.com/xolomon-coder/DroidInspect/archive/refs/heads/main.zip), confirm the commit, and provide exact CodeAssist import/update and build instructions. Do not send the CI APK artifact as the default route. Do not recreate the project or ask the user to repeat Phase 1 tests.
3. The user installs the APK built locally in CodeAssist and uses `Git MANAGER_1.0.apk` as the first fixture (previously inspected in M2; it contains 9 DEX files). Confirm:
   - Existing APK inspection still works.
   - DEX browser opens for the selected APK.
   - All 9 root-level DEX files are discovered.
   - Package selector and class list populate.
   - A class detail view shows fields and methods.
   - Search finds known class/member names.
   - Malformed DEX entries are reported without crashing the whole analysis.
   - A larger APK does not freeze the UI or cause an out-of-memory crash.
4. Ask the user to provide the observed result/report and any crash or build error. Diagnose from the actual evidence; do not infer success from CI.
5. Fix any defects on the existing `main` source, rerun host tests and GitHub Actions, publish a new debug artifact, and update README with device results.
6. Mark M4 complete only after the agreed on-device checks pass. Then proceed to M5 — .NET C# decompilation and IL browsing — by first proving a practical Android-compatible execution/decompiler route.

**Do not**
- Recreate the Android project.
- Reimplement Phase 1.
- Ask the user to repeat Phase 1 tests.
- Change the original ten-milestone roadmap.
- Claim Java decompilation works.
- Add JADX, dexlib2, or another large engine without a demonstrated Android 11 / CodeAssist-compatible prototype, resource measurements, and license review.
- Mark M4 complete solely because GitHub Actions passed.

Preserve the user's phone-only workflow, Java + XML preference, GitHub as source of truth, and complete-file delivery preference when manual code changes are needed.
