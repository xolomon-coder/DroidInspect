using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;

namespace DroidInspect.M11;

public static class NativeExports
{
    [UnmanagedCallersOnly(EntryPoint = "DroidInspect_DecompileFile", CallConvs = new[] { typeof(CallConvCdecl) })]
    public static IntPtr DecompileFile(IntPtr path, IntPtr references)
    {
        try
        {
            if (path == IntPtr.Zero)
                return Allocate("M11 DECOMPILATION ERROR: No input path was supplied.");

            var inputPath = Marshal.PtrToStringUTF8(path);
            var referencePath = references == IntPtr.Zero ? string.Empty : Marshal.PtrToStringUTF8(references) ?? string.Empty;
            return Allocate(DecompilationService.Decompile(inputPath ?? string.Empty, referencePath));
        }
        catch (Exception ex)
        {
            var message = "M11 DECOMPILATION ERROR: " + ex.GetType().Name + ": " + ex.Message;
            try { return Allocate(message); }
            catch { return IntPtr.Zero; }
        }
    }

    [UnmanagedCallersOnly(EntryPoint = "DroidInspect_FreeResult", CallConvs = new[] { typeof(CallConvCdecl) })]
    public static void FreeResult(IntPtr result)
    {
        if (result != IntPtr.Zero)
            Marshal.FreeCoTaskMem(result);
    }

    private static IntPtr Allocate(string value) =>
        Marshal.StringToCoTaskMemUTF8(value);
}
