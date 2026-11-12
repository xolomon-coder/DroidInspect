using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
namespace DroidInspect.M11;
public static class NativeExports
{
 [UnmanagedCallersOnly(EntryPoint="DroidInspect_DecompileFile", CallConvs=new[] { typeof(CallConvCdecl) })]
 public static IntPtr DecompileFile(IntPtr path, IntPtr references)
 {
  try { return Marshal.StringToCoTaskMemUTF8(DecompilationService.Decompile(Marshal.PtrToStringUTF8(path) ?? "", Marshal.PtrToStringUTF8(references) ?? "")); }
  catch (Exception ex) { return Marshal.StringToCoTaskMemUTF8("M11 DECOMPILATION ERROR: " + ex.GetType().Name + ": " + ex.Message); }
 }
 [UnmanagedCallersOnly(EntryPoint="DroidInspect_FreeResult", CallConvs=new[] { typeof(CallConvCdecl) })]
 public static void FreeResult(IntPtr result) { if (result != IntPtr.Zero) Marshal.FreeCoTaskMem(result); }
}
