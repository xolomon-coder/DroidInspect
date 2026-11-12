DroidInspect unsigned .NET test fixture

Assembly: UnsignedRenameFixture
Target framework: .NET Standard 2.0
Strong-name signing: disabled

M5 — constrained method rename test
1. Open BROWSE .NET IL AND RENAME METHOD.
2. Select UnsignedRenameFixture.dll.
3. Search for GetValue.
4. Open UnsignedRenameFixture.RenameTarget :: GetValue.
5. Tap RENAME / EXPORT.
6. Rename GetValue to GetTotal (same 8 ASCII bytes).
7. Export as a new file; do not overwrite the input.
8. Reopen the exported DLL and verify GetTotal is indexed.

M7 — constrained IL edit test
1. Start with the ORIGINAL UnsignedRenameFixture.dll, not the M5 renamed export.
2. Open BROWSE .NET IL AND RENAME METHOD and select the DLL.
3. Search for AddValues.
4. Open UnsignedRenameFixture.RenameTarget :: AddValues.
5. Tap Edit IL / Export.
6. The Release fixture's method IL should be: 03 04 58 2A
   - 03 = ldarg.1
   - 04 = ldarg.2
   - 58 = add
   - 2A = ret
7. Change only 58 to 59, producing: 03 04 59 2A
   - 59 = sub
8. Tap Validate and export copy. Save to a NEW file, not over the input.
9. Reopen the exported DLL in DroidInspect.
10. Search AddValues and open it. Confirm the IL now shows ldarg.1, ldarg.2, sub, ret (bytes 03 04 59 2A).
11. Confirm the original DLL still shows add / bytes 03 04 58 2A.

Expected safety behavior:
- The edit must preserve the original method-body byte length.
- Only supported single-byte operand-free opcodes are accepted.
- Signed assemblies, branch/operand/token instructions, methods with extra data sections, and unsupported instructions must be refused.
- Reopening verifies the bytes structurally; it does NOT prove the changed method is semantically valid or that it executes correctly.

This is a test-only fixture. It is not part of the Android app.
