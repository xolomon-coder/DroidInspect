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
