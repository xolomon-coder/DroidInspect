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

Last project-plan update: 2026-11-14 (M11 device verification update)

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

**M12 — C# Source Editing & DLL Recompilation (IN PROGRESS — COMPILER FEASIBILITY FIRST)**

**Objective:** Let the user edit reconstructed C# source as a separate working copy and, where technically supported, compile that edited source into a new managed DLL without modifying the original assembly or the original decompilation result.

**Start gate:** SATISFIED. The user accepted the integrated production M11 workflow on 2026-11-14. M12 began on 2026-11-15. The first deliverable is an isolated offline compiler feasibility proof; no compiler is integrated into production until CI and Android-device verification pass.

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

**M13 — Dependency and framework resolution**
- Build on M11's observed unresolved/mismatched-reference diagnostics.
- Identify required framework/reference assemblies and dependency versions using assembly identity, version, culture, public-key token, target framework, and dependency relationships.
- Support user-provided local dependencies and carefully validated reference packs; keep offline operation as the default.
- Add opt-in online-assisted retrieval only with explicit consent, trusted sources, integrity/provenance reporting, licensing checks, and no automatic upload of inspected files.
- Exit criteria: representative dependency graphs resolve accurately or produce precise, actionable unresolved/mismatch reports; no silent incompatible substitutions.

**M14 — Assembly Explorer and advanced IL Viewer**
- Extend the existing metadata and IL browsing foundations into a navigable assembly tree: assemblies, modules, namespaces, types, fields, properties, events, methods, and references.
- Improve method navigation, token/RVA display, raw IL inspection, instruction bytes, operands, and cross-reference navigation.
- Add filters, sorting, bookmarks, and contextual search without removing current inspection/editing behavior.
- Exit criteria: representative managed assemblies can be explored consistently, and displayed metadata/IL is cross-checked against known fixtures.

**M15 — Advanced C# source editor**
- Extend the basic separate-copy source editing/rebuild workflow planned in M12 with a phone-usable multi-file editor.
- Add syntax highlighting, line numbers, find/replace, file tabs or an equivalent lightweight file navigator, unsaved-change indicators, and readable compiler diagnostics.
- Preserve original assemblies and untouched decompilation output; save edits as separate source files.
- Exit criteria: multi-file source projects can be edited and saved reliably on the phone, with editor usability and memory behavior tested on-device.

**M16 — Assembly comparison and resource extraction**
- Compare two assemblies by identity, references, types, members, signatures, metadata, and supported IL changes.
- Present additions, removals, and modifications without implying semantic equivalence where only structural comparison is available.
- Browse and extract supported embedded resources to separate user-selected output files; preserve original inputs.
- Exit criteria: known changed/unchanged assembly pairs produce accurate comparison reports, and resource extraction passes integrity checks.

### Phase 7 — Advanced APK workflows

**M17 — Advanced APK inspection and DEX analysis**
- Extend the existing M2 APK inspection and M4 DEX browsing foundations; do not recreate completed features.
- Improve package/component navigation, manifest and permission analysis, multi-DEX handling, resource mapping, global search, and cross-reference navigation.
- Add robust handling for split APKs, large inputs, malformed structures, and unsupported formats where feasible.
- Exit criteria: representative APK families are inspected and browsed accurately on-device, with limitations clearly reported.

**M18 — APK editing, rebuilding, and signing workflow**
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


## 5. Development and Creation Process

This is the required process for building DroidInspect:

1. **Repository-first:** inspect the latest `main` branch, commit, README, build files, and source before every implementation task.
2. **Feasibility before dependency adoption:** create small, isolated on-device prototypes for risky engines. Do not add a major decompiler/rebuilder dependency until its Android compatibility is demonstrated.
3. **Phase-level delivery:** when the user asks to complete a whole phase, continue through every milestone in that phase in sequence on `main`, using milestone boundaries as internal checkpoints rather than stopping for a routine handoff after each milestone. Do not arbitrarily expand scope beyond the phase. Pause early only when a user decision, missing fixture, safety constraint, or essential device-only validation genuinely blocks further work; explain the blocker and ask whether to defer that check and continue. Keep the twelve-milestone roadmap visible.
4. **Plan exact changes:** list files to add/change, dependencies and exact versions, permissions, compatibility effects, and test cases before coding.
5. **Implement phone-friendly code:** provide complete replacement files with full paths; avoid partial snippets and desktop-only instructions.
6. **Cloud verification:** run the configured host tests and GitHub Actions on the latest `main` source; fix failures before handoff. CI artifacts are for verification, not the normal user delivery route.
7. **Source handoff:** provide the repository page, latest `main` commit, and direct `main` ZIP link. Give exact instructions for importing the ZIP or updating/replacing the required files in CodeAssist.
8. **User build and device test:** the user builds in CodeAssist, installs the locally built APK, and runs the agreed test cases. A cloud build alone does not pass functional criteria.
9. **Analyze evidence:** inspect user-provided build output, reports, logs, and results; fix failures directly on `main`, rerun CI, and provide an updated ZIP handoff.
10. **Document and hand off safely:** update this README with status, decisions, changed files, test evidence, limitations, latest `main` commit, exact stopping point, and next action.

No milestone is complete solely because code was written or committed. When delivering a full phase, complete implementation and available automated verification for all milestones in that phase before the normal ZIP handoff. Device-only verification may be collected as a clearly documented user test batch after handoff when it does not block safe implementation of later milestones; never claim device tests passed without user evidence.

## 6. Current Project Status (2026-11-13)

**Current phase: Phase 5 / M12 — offline compiler feasibility prototype in progress on `main`. M11 production integration and Android-device acceptance are complete.** The isolated proof app remains intact. M4–M7 retain their previously accepted Android 11 device scopes. M8–M10 production foundations and regression CI remain preserved. M11 production integration does not claim complete dependency resolution, recompilation, or target-program execution. Do not confuse cloud APK verification with user device acceptance.

Phase 1 implementation has passed cloud CI and initial Android 11 device smoke checks. The user chose to proceed without the optional native-PE fixture test. Native/non-managed PE rejection therefore remains an explicit unverified limitation; do not claim full M3 classification coverage. M2 has one successful real APK test, and M3 has one successful CLI-bearing mixed-mode .NET DLL test. Phase 2 may begin while this gap remains documented for later regression coverage.

The project remains the existing Java + native XML Android application. The roadmap contains 20 milestones across eight phases. M11 production C# decompilation is integrated and accepted on the user's device. M12 is active for separate-copy C# editing and DLL recompilation; compiler feasibility is the first implementation gate. M13–M20 remain planned only. Development branch: `main`.

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
