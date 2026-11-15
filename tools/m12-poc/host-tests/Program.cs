using System.Reflection.Metadata;
using System.Reflection.PortableExecutable;
using DroidInspect.M12;

if (args.Length != 3)
    throw new ArgumentException("Usage: HostTests <source-directory> <reference-directory> <output-dll>");

var result = CompilerService.Compile(args[0], args[2], args[1]);
Console.WriteLine(result);
if (!result.StartsWith("M12 COMPILATION PASS", StringComparison.Ordinal))
    throw new Exception("Roslyn compiler proof failed.");

using var stream = File.OpenRead(args[2]);
using var pe = new PEReader(stream);
if (!pe.HasMetadata) throw new Exception("Output is not a managed PE.");
var metadata = pe.GetMetadataReader();
if (!metadata.IsAssembly) throw new Exception("Output is not an assembly.");
var assembly = metadata.GetAssemblyDefinition();
Console.WriteLine("HOST VERIFY PASS: " + metadata.GetString(assembly.Name));
