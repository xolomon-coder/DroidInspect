# M12 Offline C# Compiler Feasibility Proof

This is an isolated proof of concept. It is not integrated into the production DroidInspect application.

## Candidate

- Roslyn compiler API: Microsoft.CodeAnalysis.CSharp 5.12.0 (MIT).
- Compiler host: .NET 11 Native AOT shared library.
- Android target: linux-bionic-arm64, called through a small JNI bridge.
- Framework references: .NET 11.0 reference assemblies.
- Android proof host: Java/XML, minSdk 26, ARM64 only.

## What the proof must establish

1. Compile one edited C# source file offline on an Android ARM64 device.
2. Emit a genuine managed DLL, not renamed text or IL.
3. Reopen the output with PEReader and verify CLI metadata, assembly identity, and the edited type/method symbol.
4. Reproduce compilation and output verification in GitHub Actions.
5. Measure native library, APK, and reference-pack sizes; record device time and memory when tested.

## Current status

Prototype source is being prepared. No compiler feasibility PASS is claimed until CI and Android-device checks both pass.

## Safety

The proof writes only under its private app files directory. It does not request Internet or storage permissions and does not modify the production app or any user-selected assembly.
