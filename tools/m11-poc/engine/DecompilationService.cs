using System.Reflection.Metadata;
using System.Reflection.PortableExecutable;
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
            try
            {
                metadata = pe.GetMetadataReader();
            }
            catch (Exception ex) when (ex is BadImageFormatException or InvalidOperationException)
            {
                throw new InvalidDataException("The .NET metadata is malformed or unsupported.", ex);
            }

            if (!metadata.IsAssembly)
                throw new InvalidDataException("The selected managed PE is a module, not a .NET assembly.");

            var settings = new DecompilerSettings
            {
                LoadInMemory = true,
                ThrowOnAssemblyResolveErrors = false
            };

            var resolver = new UniversalAssemblyResolver(
                fullPath,
                false,
                null,
                null,
                PEStreamOptions.PrefetchMetadata);

            var directory = Path.GetDirectoryName(fullPath);
            if (!string.IsNullOrEmpty(directory))
                resolver.AddSearchDirectory(directory);

            if (!string.IsNullOrWhiteSpace(referenceDirectory))
            {
                foreach (var searchDirectory in referenceDirectory.Split(
                    Path.PathSeparator,
                    StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
                {
                    if (Directory.Exists(searchDirectory))
                        resolver.AddSearchDirectory(searchDirectory);
                }
            }

            var source = new CSharpDecompiler(fullPath, resolver, settings)
                .DecompileWholeModuleAsString();

            if (string.IsNullOrWhiteSpace(source))
                throw new InvalidDataException("ILSpy completed without producing C# source. The assembly may contain no decompilable types.");

            return source;
        }
        catch (InvalidDataException)
        {
            throw;
        }
        catch (BadImageFormatException ex)
        {
            throw new InvalidDataException("The selected DLL or EXE has invalid or corrupted PE/.NET metadata.", ex);
        }
        catch (EndOfStreamException ex)
        {
            throw new InvalidDataException("The selected DLL or EXE is truncated or incomplete.", ex);
        }
    }
}
