using ICSharpCode.Decompiler;
using ICSharpCode.Decompiler.CSharp;
using ICSharpCode.Decompiler.Metadata;
namespace DroidInspect.M11;
public static class DecompilationService
{
 public static string Decompile(string path, string referenceDirectory)
 {
  if (string.IsNullOrWhiteSpace(path) || !File.Exists(path)) throw new FileNotFoundException("Managed assembly not found.", path);
  var settings = new DecompilerSettings { LoadInMemory = true, ThrowOnAssemblyResolveErrors = false };
  var resolver = new UniversalAssemblyResolver(path, false, null, null, System.Reflection.PortableExecutable.PEStreamOptions.PrefetchMetadata);
  var directory = Path.GetDirectoryName(path);
  if (!string.IsNullOrEmpty(directory)) resolver.AddSearchDirectory(directory);
  if (!string.IsNullOrWhiteSpace(referenceDirectory))
  {
   foreach (var searchDirectory in referenceDirectory.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries))
    if (Directory.Exists(searchDirectory)) resolver.AddSearchDirectory(searchDirectory);
  }
  return new CSharpDecompiler(path, resolver, settings).DecompileWholeModuleAsString();
 }
}