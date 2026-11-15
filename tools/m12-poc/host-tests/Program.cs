using System.Reflection;
using System.Reflection.Metadata;
using System.Runtime.Loader;
using System.Security.Cryptography;
using DroidInspect.M12;

if (args.Length != 3)
    throw new ArgumentException("Usage: HostTests <source-directory> <reference-directory> <output-dll>");

var originalDirectory = Path.Combine(Path.GetTempPath(), "droidinspect-m12-original-" + Guid.NewGuid().ToString("N"));
Directory.CreateDirectory(originalDirectory);
var originalPath = Path.Combine(Path.GetDirectoryName(Path.GetFullPath(args[2]))!, "OriginalFixture.dll");
try
{
    File.WriteAllText(Path.Combine(originalDirectory, "EditedFixture.cs"), """
        namespace DroidInspect.M12Fixture;
        public sealed class EditedFixture
        {
            public string GetEditedValue() => "M12_ORIGINAL_VALUE";
        }
        """);
    var originalResult = CompilerService.Compile(originalDirectory, originalPath, args[1]);
    if (!originalResult.StartsWith("M12 COMPILATION PASS", StringComparison.Ordinal))
        throw new Exception("Could not build original compatibility fixture.");
}
finally
{
    Directory.Delete(originalDirectory, true);
}

var result = CompilerService.Compile(args[0], args[2], args[1], originalPath);
Console.WriteLine(result);
if (!result.StartsWith("M12 COMPILATION PASS", StringComparison.Ordinal))
    throw new Exception("Roslyn compiler proof failed.");
if (!result.Contains("Source files: 2", StringComparison.Ordinal))
    throw new Exception("Compiler did not compile the complete two-file fixture.");
if (!result.Contains("Public-key token:", StringComparison.Ordinal)
    || !result.Contains("Strong-name public key present:", StringComparison.Ordinal))
    throw new Exception("Assembly identity verification report is incomplete.");
if (!result.Contains("LEVEL 1 — COMPILATION: PASS", StringComparison.Ordinal)
    || !result.Contains("LEVEL 2 — STRUCTURAL COMPATIBILITY: WARNING", StringComparison.Ordinal)
    || !result.Contains("LEVEL 3 — RUNTIME / BEHAVIORAL COMPATIBILITY: NOT VERIFIED", StringComparison.Ordinal))
    throw new Exception("The three-level DLL replacement compatibility report is incomplete or overclaims compatibility.");

var originalOutput = File.ReadAllBytes(args[2]);
var originalHash = Convert.ToHexString(SHA256.HashData(originalOutput));
var invalidDirectory = Path.Combine(Path.GetTempPath(), "droidinspect-m12-invalid-" + Guid.NewGuid().ToString("N"));
Directory.CreateDirectory(invalidDirectory);
try
{
    File.WriteAllText(Path.Combine(invalidDirectory, "Broken.cs"), "public class Broken { this is not valid C# }");
    var failed = CompilerService.Compile(invalidDirectory, args[2], args[1], originalPath);
    Console.WriteLine(failed);
    if (!failed.StartsWith("M12 COMPILE FAILED", StringComparison.Ordinal))
        throw new Exception("Invalid source unexpectedly compiled.");
    var afterFailureHash = Convert.ToHexString(SHA256.HashData(File.ReadAllBytes(args[2])));
    if (!StringComparer.Ordinal.Equals(originalHash, afterFailureHash))
        throw new Exception("A failed compile modified the previous output DLL.");
    Console.WriteLine("HOST PRESERVATION PASS: failed compilation left the previous DLL unchanged.");
}
finally
{
    Directory.Delete(invalidDirectory, true);
}

using (var stream = File.OpenRead(args[2]))
using (var pe = new System.Reflection.PortableExecutable.PEReader(stream))
{
    if (!pe.HasMetadata) throw new Exception("Output is not a managed PE.");
    var metadata = pe.GetMetadataReader();
    if (!metadata.IsAssembly) throw new Exception("Output is not an assembly.");
    var assembly = metadata.GetAssemblyDefinition();
    Console.WriteLine("HOST METADATA PASS: " + metadata.GetString(assembly.Name));
}

var loaded = AssemblyLoadContext.Default.LoadFromAssemblyPath(Path.GetFullPath(args[2]));
var type = loaded.GetType("DroidInspect.M12Fixture.EditedFixture", throwOnError: true)!;
var instance = Activator.CreateInstance(type) ?? throw new Exception("Cannot instantiate edited fixture.");
var method = type.GetMethod("GetEditedValue", BindingFlags.Public | BindingFlags.Instance)
    ?? throw new Exception("Edited method is missing.");
var value = method.Invoke(instance, null) as string;
if (value != "M12_EDITED_VALUE")
    throw new Exception("Edited method returned an unexpected value: " + value);
Console.WriteLine("HOST BEHAVIOR PASS: two-file EditedFixture.GetEditedValue() returned M12_EDITED_VALUE.");
