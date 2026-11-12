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

**Current phase: Phase 3 — implementation and cloud verification completed; consolidated Android 11 device verification pending.** M4–M7 have passed their agreed Android 11 device verification scopes. M8 now includes an on-device APK signing and verification screen; M9 includes raw IL byte/opcode explanations and MethodDef token navigation, while full C# decompilation remains unsupported in the current Java/CodeAssist architecture; M10 regression, signing, tamper, build, and artifact checks pass in GitHub Actions run #131. Phase 3 has not yet received user device-test evidence. M4 large-APK stress and malformed-DEX device edge cases remain untested. Do not confuse CI verification with device verification.

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
