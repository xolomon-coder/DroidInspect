# DroidInspect
The dll editor app

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

GitHub is an important part of my development workflow.

The objective is to avoid repeatedly exporting, downloading ZIP files, and manually rebuilding the entire project.

Before making changes

Always:

1. Inspect the latest GitHub repository.
2. Identify the current branch and latest commit.
3. Read the existing project documentation.
4. Inspect the existing source code.
5. Understand the architecture and previous decisions.
6. Identify completed, pending, and unfinished features.
7. Continue the existing project rather than recreating it.

Never overwrite existing functionality unnecessarily.

During implementation

- Work on the existing source.
- Implement only the requested scope.
- Preserve established architecture.
- Avoid unnecessary dependencies.
- Keep the application compatible with phone-based development.
- Verify changes before considering a milestone complete.
- Keep GitHub updated with relevant changes.

GitHub responsibilities

Use GitHub for:

- README: Project overview, requirements, architecture, current state, and development documentation.
- Issues: Features, bugs, pending work, and development tasks.
- Actions: Automated build and verification results where configured.
- Projects: Milestone and task tracking where useful.
- Commits: Chronological record of development changes.

Do not assume that a successful GitHub commit or build means the feature works correctly on my phone.

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
3. **One milestone at a time:** implement only the current milestone's agreed scope. Keep the ten-milestone roadmap visible.
4. **Plan exact changes:** list files to add/change, dependencies and exact versions, permissions, compatibility effects, and test cases before coding.
5. **Implement phone-friendly code:** provide complete replacement files with full paths; avoid partial snippets and desktop-only instructions.
6. **Build and install:** user builds in CodeAssist and installs on the Android phone.
7. **Test actual behavior:** provide practical test steps and sample fixture requirements. A successful Gradle build alone does not pass functional criteria.
8. **Analyze evidence:** inspect user-provided reports/logs/results; fix failures before marking a milestone complete.
9. **Document continuously:** update this README with status, decisions, changed files, test evidence, limitations, and next action. Keep chronological notes.
10. **Handoff safely:** before a new chat, update documentation and prepare a handoff with branch/commit, milestone state, exact stopping point, and next action.

No milestone is complete solely because code was written or committed.

## 6. Current Project Status (2026-11-12)

**Current milestone: M1 — Project foundation and on-device feasibility.**

Completed and verified:
- Existing GitHub starter project inspected; it was continued rather than recreated.
- User reports the existing project builds successfully in CodeAssist.
- User reports the baseline build is OK.

Not yet verified:
- Installation and startup on the phone for this exact repository build.
- APK file selection, APK archive inspection, and package metadata extraction.
- DLL/EXE selection and .NET/CLI metadata extraction.
- Any decompilation, editing, rebuilding, or debugging engine.

Current starter-project facts:
- Repository: `xolomon-coder/DroidInspect`.
- Branch: `main`.
- Last inspected pre-documentation commit: `7c92f184fa79b3938235f67ce95c410d64e9f54c`.
- Android module: `droidinspect/app`.
- Main activity: `droidinspect/app/src/main/java/com/tanvir/droidinspect/MainActivity.java`.
- Main layout: `droidinspect/app/src/main/res/layout/activity_main.xml`.
- Current starter screen is a basic Hello World screen; no inspection features are implemented yet.
- Build configuration: see `droidinspect/app/build.gradle.kts`.

**Exact next action:** after confirming the starter app installs and opens, implement M1's first small on-device file-intake and file-identification test. Then prototype APK ZIP inspection and .NET PE/CLI detection separately. Do not jump to full decompiler integration yet.

## 7. Chronological Development History

- 2026-11-12: User confirmed Android 11 and CodeAssist 3.36.1 for the current development session.
- 2026-11-12: Existing DroidInspect repository and starter source were inspected. The app module is Java/XML with package `com.tanvir.droidinspect`; the initial screen is Hello World.
- 2026-11-12: User reported the existing CodeAssist build succeeded. This confirms build success only; install/startup and feature tests remain pending.
- 2026-11-12: Added this product specification, ten-milestone roadmap, development process, and current-state handoff information to the README.

## 8. New-Chat Continuation Instructions

When continuing DroidInspect in a new ChatGPT chat:

- Read this README completely before proposing work.
- Inspect the latest repository branch and commit and compare it with this documented state.
- Continue from **M1**, not from project creation.
- Do not recreate the project or replace the existing architecture without a demonstrated reason and discussion.
- Ask for a build/install result only if it is not already recorded here or in the current chat.
- The next task is the M1 file-intake/file-identification prototype, followed by isolated APK ZIP and .NET PE/CLI detection tests.
- Preserve the total roadmap of ten milestones unless a verified technical blocker requires a documented revision.
