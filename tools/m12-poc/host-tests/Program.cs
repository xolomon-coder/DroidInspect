using System.Reflection;
using System.Runtime.Loader;
using System.Reflection.Metadata;
using System.Reflection.PortableExecutable;
using DroidInspect.M12;

if (args.Length != 3)
    throw new ArgumentException("Usage: HostTests <source-directory> <reference-directory> <output-dll>");

var result = CompilerService.Compile(args[0], args[2], args[1]);
Console.WriteLine(result);
if (!result.StartsWith("M12 COMPILATION PASS", StringComparison.Ordinal))
    throw new Exception("Roslyn compiler proof failed.");

using (var stream = File.OpenRead(args[2]))
using (var pe = new PEReader(stream))
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
Console.WriteLine("HOST BEHAVIOR PASS: EditedFixture.GetEditedValue() returned M12_EDITED_VALUE.");
