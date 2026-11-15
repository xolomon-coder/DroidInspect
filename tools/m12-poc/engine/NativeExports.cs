using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;

namespace DroidInspect.M12;

public static class NativeExports
{
    [UnmanagedCallersOnly(EntryPoint = "DroidInspect_M12_Compile", CallConvs = new[] { typeof(CallConvCdecl) })]
    public static IntPtr Compile(IntPtr sourceDirectory, IntPtr outputPath, IntPtr referenceDirectory, IntPtr originalAssemblyPath)
    {
        try
        {
            var source = Marshal.PtrToStringUTF8(sourceDirectory) ?? string.Empty;
            var output = Marshal.PtrToStringUTF8(outputPath) ?? string.Empty;
            var references = Marshal.PtrToStringUTF8(referenceDirectory) ?? string.Empty;
            var original = Marshal.PtrToStringUTF8(originalAssemblyPath) ?? string.Empty;
            return Marshal.StringToCoTaskMemUTF8(CompilerService.Compile(source, output, references, original));
        }
        catch (Exception ex)
        {
            return Marshal.StringToCoTaskMemUTF8("M12 NATIVE ERROR: " + ex.GetType().Name + ": " + ex.Message);
        }
    }

    [UnmanagedCallersOnly(EntryPoint = "DroidInspect_M12_FreeResult", CallConvs = new[] { typeof(CallConvCdecl) })]
    public static void FreeResult(IntPtr result)
    {
        if (result != IntPtr.Zero) Marshal.FreeCoTaskMem(result);
    }
}
