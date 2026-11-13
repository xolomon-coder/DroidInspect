using System.Reflection;
using System.Reflection.Metadata;
using System.Reflection.PortableExecutable;
using System.Text;
using ICSharpCode.Decompiler;
using ICSharpCode.Decompiler.CSharp;
using ICSharpCode.Decompiler.Metadata;

namespace DroidInspect.M11;

public static class DecompilationService
{
    public static string Decompile(string path, string referenceDirectory)
    {
        if (string.IsNullOrWhiteSpace(path))
            throw new ArgumentException("Select a .NET DLL or EXE file.", nameof(path));

        var fullPath = Path.GetFullPath(path);
        if (!File.Exists(fullPath))
            throw new FileNotFoundException("The selected assembly does not exist or is no longer accessible.", fullPath);

        var info = new FileInfo(fullPath);
        if (info.Length == 0)
            throw new InvalidDataException("The selected file is empty; no PE or .NET metadata was found.");

        try
        {
            using var stream = new FileStream(fullPath, FileMode.Open, FileAccess.Read, FileShare.Read);
            using var pe = new PEReader(stream, PEStreamOptions.PrefetchMetadata);

            if (!pe.HasMetadata)
                throw new InvalidDataException("The file is a PE image without a .NET metadata directory; native executables are not supported.");
            if (pe.PEHeaders.PEHeader is null)
                throw new InvalidDataException("The file has no valid PE optional header.");
            if (pe.PEHeaders.CorHeader is null)
                throw new InvalidDataException("The PE file is not a managed .NET DLL or EXE (missing CLR header).");

            MetadataReader metadata;
            try { metadata = pe.GetMetadataReader(); }
            catch (Exception ex) when (ex is BadImageFormatException or InvalidOperationException)
            {
                throw new InvalidDataException("The .NET metadata is malformed or unsupported.", ex);
            }

            if (!metadata.IsAssembly)
                throw new InvalidDataException("The selected managed PE is a module, not a .NET assembly.");

            var searchDirectories = new List<string>();
            var inputDirectory = Path.GetDirectoryName(fullPath);
            if (!string.IsNullOrEmpty(inputDirectory))
                searchDirectories.Add(inputDirectory);
            if (!string.IsNullOrWhiteSpace(referenceDirectory))
                searchDirectories.AddRange(referenceDirectory.Split(
                    Path.PathSeparator,
                    StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries));

            searchDirectories = searchDirectories.Where(Directory.Exists)
                .Distinct(StringComparer.OrdinalIgnoreCase).ToList();

            var unresolved = FindUnresolvedReferences(metadata, searchDirectories);
            var settings = new DecompilerSettings
            {
                LoadInMemory = true,
                ThrowOnAssemblyResolveErrors = false
            };
            var resolver = new UniversalAssemblyResolver(
                fullPath, false, null, null, PEStreamOptions.PrefetchMetadata);
            foreach (var directory in searchDirectories)
                resolver.AddSearchDirectory(directory);

            var source = new CSharpDecompiler(fullPath, resolver, settings)
                .DecompileWholeModuleAsString();
            if (string.IsNullOrWhiteSpace(source))
                throw new InvalidDataException("ILSpy completed without producing C# source. The assembly may contain no decompilable types.");

            if (unresolved.Count == 0)
                return source;

            var diagnostics = new StringBuilder();
            diagnostics.AppendLine("// M11 DEPENDENCY DIAGNOSTICS: references not found in configured search directories.");
            foreach (var item in unresolved)
                diagnostics.Append("// Unresolved or mismatched reference: ").AppendLine(item);
            diagnostics.AppendLine("// Source reconstruction was attempted; this does not mean these dependencies were resolved.");
            diagnostics.AppendLine();
            diagnostics.Append(source);
            return diagnostics.ToString();
        }
        catch (InvalidDataException) { throw; }
        catch (BadImageFormatException ex)
        {
            throw new InvalidDataException("The selected DLL or EXE has invalid or corrupted PE/.NET metadata.", ex);
        }
        catch (EndOfStreamException ex)
        {
            throw new InvalidDataException("The selected DLL or EXE is truncated or incomplete.", ex);
        }
    }

    private static List<string> FindUnresolvedReferences(
        MetadataReader metadata, IReadOnlyList<string> searchDirectories)
    {
        var unresolved = new List<string>();
        foreach (var handle in metadata.AssemblyReferences)
        {
            var reference = metadata.GetAssemblyReference(handle);
            var name = metadata.GetString(reference.Name);
            var expectedVersion = reference.Version;
            var found = false;
            var mismatched = new List<string>();

            foreach (var directory in searchDirectories)
            {
                foreach (var extension in new[] { ".dll", ".exe" })
                {
                    var candidate = Path.Combine(directory, name + extension);
                    if (!File.Exists(candidate))
                        continue;
                    try
                    {
                        var actual = AssemblyName.GetAssemblyName(candidate);
                        if (string.Equals(actual.Name, name, StringComparison.OrdinalIgnoreCase)
                            && (actual.Version is null || actual.Version == expectedVersion))
                        {
                            found = true;
                            break;
                        }
                        mismatched.Add(actual.FullName ?? candidate);
                    }
                    catch (BadImageFormatException)
                    {
                        mismatched.Add(candidate + " (not a managed assembly)");
                    }
                }
                if (found) break;
            }

            if (!found)
            {
                var identity = name + ", Version=" + expectedVersion;
                if (mismatched.Count > 0)
                    identity += "; candidate identity mismatch: " + string.Join(" | ", mismatched.Distinct());
                unresolved.Add(identity);
            }
        }
        return unresolved;
    }
}
