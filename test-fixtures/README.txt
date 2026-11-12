DroidInspect unsigned .NET rename test fixture

Assembly: UnsignedRenameFixture
Target framework: .NET Standard 2.0
Strong-name signing: disabled

In DroidInspect:
1. Open BROWSE .NET IL AND RENAME METHOD.
2. Select UnsignedRenameFixture.dll.
3. Search for GetValue.
4. Open UnsignedRenameFixture.RenameTarget :: GetValue.
5. Tap RENAME / EXPORT.
6. Rename GetValue to GetTotal (same 8 ASCII bytes).
7. Export as a new file; do not overwrite the input.
8. Reopen the exported DLL and verify GetTotal is indexed.

This is a test-only fixture. It is not part of the Android app.
