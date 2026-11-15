# M12 Offline C# Compiler Feasibility Proof

**Cloud proof: PASS. Android-device proof: PENDING.** This is an isolated test application, not a production DroidInspect feature.

## Compiler candidate

- Roslyn API: `Microsoft.CodeAnalysis.CSharp 5.12.0` (MIT).
- Compiler host: .NET 11 Native AOT shared library.
- Android native target: `linux-bionic-arm64`.
- Java integration: separate JNI bridge.
- Compilation references: production DroidInspect .NET 11 reference assets (approximately 6.5 MB).
- Android app: Java/XML, package `com.tanvir.m12poc`, minSdk 26, ARM64 only.
- No Internet or storage permissions.

## Cloud proof results

- Roslyn host compile emitted a genuine managed DLL.
- PE/CLI metadata, assembly identity, expected type, and method were verified.
- Host loaded the output DLL and invoked `EditedFixture.GetEditedValue()`; it returned `M12_EDITED_VALUE`.
- Roslyn Native AOT publish for Bionic ARM64 passed.
- JNI bridge linked to the compiler library and passed ELF/ABI checks.
- Isolated Android proof APK built and passed archive/payload checks.
- No Android Internet permission was found.
- Compiler library: 35,227,344 bytes.
- JNI bridge: 10,704 bytes.
- Proof APK: 38,365,488 bytes.

## Native AOT warnings — not resolved

Detailed trimming analysis reports:
- IL2091 generic-parameter annotation warnings in Roslyn lazy initialization and pooled delegates.
- IL2050 COM-marshalling warnings in Windows DiaSymReader/DiaSymWriter paths.
- IL2072 reflection constructor-preservation warning in the COM symbol factory.

These did not prevent Native AOT publishing, but do require real Android execution testing. They are not suppressed. Do not treat a successful link as proof that every Roslyn code path is safe under trimming.

## Build and test in CodeAssist

1. Download the latest repository `main.zip` and extract it while preserving the folder structure.
2. Open `tools/m12-poc/android-host` as a separate Gradle project in CodeAssist. Do not replace the production DroidInspect project.
3. Build the Debug APK and install it. The proof app uses package `com.tanvir.m12poc` and can coexist with production DroidInspect.
4. Open **DroidInspect M12 Compiler Proof**.
5. Tap **COMPILE EDITED C# FIXTURE**.
6. Wait for the result. Expected successful output includes:
   - `M12 COMPILATION PASS`
   - .NET 11.0 target and Roslyn 5.12.0 compiler
   - `PASS: PE/CLI metadata verified`
   - `EditedFixture.GetEditedValue` symbol verification
   - `JAVA VERIFY PASS: MZ signature`

Build configuration: Gradle 8.13, Android Gradle Plugin 8.13.0, Java 17, compile SDK 36, minSdk 26. Prebuilt ARM64 `.so` files are already committed, so the CodeAssist build does not need an NDK. The proof uses the reference assemblies from the existing production app's asset folder through a relative Gradle asset source path.

The app copies the fixture and references into its private app files directory and writes `DroidInspect.M12.EditedFixture.dll` there. It does not modify production DroidInspect, any original DLL, or external user files. It does not need network access.

## Device measurements still required

On the user's Android 11 / ARM64 phone, report:
- Full result text.
- Approximate compile time.
- Whether the app remains responsive.
- Any crash, warning, or abnormal memory/thermal behavior.

Peak RAM and device storage use have not yet been measured.

## Boundary

This prototype proves only a small edited C# source compilation path if the Android-device test passes. It does not yet prove compilation of large decompiled projects, support for every .NET target framework, dependency resolution, strong-name preservation, drop-in DLL replacement, or runtime/behavioral compatibility. No production integration is authorized by this proof alone.

## Compiler hardening update — 2026-11-16

The shared `CompilerService` is no longer tied to the original proof fixture's type or method names.

- It verifies that output is a managed PE/CLI assembly and reports identity/version/culture/public-key token/type count/method count/strong-name public-key presence.
- It emits to a unique temporary file in the destination directory and replaces an existing output only after successful compilation and metadata verification.
- An invalid compile must leave an existing output DLL byte-for-byte unchanged.
- The host fixture now consists of two C# files, and the primary fixture calls a helper in the second file.

The updated host test asserts:
1. Two source files were compiled.
2. Generic assembly identity/metadata fields are reported.
3. The output is loadable and the edited method returns `M12_EDITED_VALUE`.
4. An intentionally invalid source fails compilation.
5. The previously existing output DLL remains byte-for-byte unchanged after that failure.

**This update is not yet a production Android integration.** The Actions run for this change must pass before cloud verification is recorded. Android execution remains pending for the final consolidated device session. Native AOT warnings IL2091, IL2050, and IL2072 remain open and unsuppressed.

## Production-side source workspace progress — 2026-11-16

The production DroidInspect app now has an initial source workspace/editor implementation in Java/XML:
- Persistent app-private project storage with a separate untouched decompilation copy.
- Editable source files, file switching, new-file creation, and multi-select SAF import of `.cs` files.
- ZIP export of the edited source project.
- The M11 C# viewer launches this workspace without modifying the original assembly or M11 source result.

The editor is a source-editing/export subfeature only at this point. The last workspace ID is persisted and can be reopened from the production main screen. The production Roslyn JNI bridge, compile UI, compiler output inspection, and DLL replacement compatibility report are not integrated yet. Production Android CI and Android-device behavior are not verified by the source commit alone.

## Production compiler wiring update — 2026-11-16

M12 workflow run `43769751887` passed. It built the Native AOT compiler for Bionic ARM64, linked the JNI bridge, packaged and verified the isolated proof APK, and copied both verified shared libraries into:
- `droidinspect/app/src/main/jniLibs/arm64-v8a/libdroidinspect_m12_compiler.so`
- `droidinspect/app/src/main/jniLibs/arm64-v8a/libdroidinspect_m12_jni.so`

Production Java source now calls the JNI entry point through `com.tanvir.m12poc.NativeBridge`. The C# source editor has an offline compile action, writes to a separate project build folder, verifies the resulting managed PE file, and exports the rebuilt DLL separately. The original selected assembly is copied to the project `original/` folder (512 MB cap).

Verified payload sizes: compiler 35,239,464 bytes; JNI bridge 10,704 bytes. ARM64 only.

The production integration still requires full Android CI after all integration changes, and actual device execution remains pending. Compatibility comparison and the three-level DLL replacement report are not implemented yet. The only compiler reference profile currently bundled is .NET 11.0. The Native AOT warnings IL2091, IL2050, and IL2072 remain unsuppressed.

## Three-level replacement report and JNI signature update — 2026-11-16

Added `engine/AssemblyCompatibilityReporter.cs`. It compares assembly identity, target framework attribute, public types, public method/field/property signatures, public events, assembly-reference identities and strong-name signed state. It reports structural PASS/FAIL/WARNING/NOT VERIFIED without claiming runtime behavior. The compiler uses the original assembly simple name when an original path is supplied.

The original assembly path is now a fourth argument through NativeExports → JNI → Java. The isolated proof Java bridge was updated to the same signature.

Host regression evidence:
- Two-file compilation and PE/CLI metadata: PASS.
- Three-level compatibility report: generated; synthetic fixture correctly reports Level 2 WARNING because neither fixture contains TargetFrameworkAttribute, and Level 3 NOT VERIFIED.
- Invalid compile preserves previous output byte-for-byte: PASS.
- Host loads rebuilt assembly and edited method returns `M12_EDITED_VALUE`: PASS.

M12 workflow run `43770685184` (run #27) completed successfully, including Native AOT Bionic ARM64 publishing, JNI link, isolated proof APK, offline checks, production payload publication and artifact upload. Verified production native payload is at `droidinspect/app/src/main/jniLibs/arm64-v8a/`.

The production source editor now saves and exports `M12-Build-Compatibility-Report.txt` separately from the rebuilt DLL. Android-device execution and final production CI against the latest four-argument native payload remain pending. Compiler reference support remains limited to the bundled .NET 11 profile.
