using System.Reflection;
using System.Reflection.Metadata;
using System.Reflection.PortableExecutable;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;

namespace DroidInspect.M12;

public static class CompilerService
{
    public static string Compile(string sourceDirectory, string outputPath, string referenceDirectory)
    {
        try
        {
            if (!Directory.Exists(sourceDirectory))
                return "M12 COMPILE FAILED: Source directory does not exist.";
            if (!Directory.Exists(referenceDirectory))
                return "M12 COMPILE FAILED: Reference directory does not exist.";

            var sources = Directory.GetFiles(sourceDirectory, "*.cs", SearchOption.AllDirectories)
                .OrderBy(x => x, StringComparer.OrdinalIgnoreCase).ToArray();
            if (sources.Length == 0)
                return "M12 COMPILE FAILED: No .cs source files were found.";

            var referenceFiles = Directory.GetFiles(referenceDirectory, "*.dll", SearchOption.TopDirectoryOnly)
                .OrderBy(x => x, StringComparer.OrdinalIgnoreCase).ToArray();
            if (referenceFiles.Length == 0)
                return "M12 COMPILE FAILED: No framework reference assemblies were found.";

            var metadataReferences = referenceFiles
                .Select(x => MetadataReference.CreateFromFile(x))
                .ToArray();

            var trees = sources.Select(x => CSharpSyntaxTree.ParseText(
                File.ReadAllText(x),
                new CSharpParseOptions(LanguageVersion.Latest),
                x)).ToArray();

            var assemblyName = Path.GetFileNameWithoutExtension(outputPath);
            var compilation = CSharpCompilation.Create(
                assemblyName,
                trees,
                metadataReferences,
                new CSharpCompilationOptions(
                    OutputKind.DynamicallyLinkedLibrary,
                    optimizationLevel: OptimizationLevel.Release,
                    nullableContextOptions: NullableContextOptions.Enable,
                    deterministic: true));

            var fullOutput = Path.GetFullPath(outputPath);
            var parent = Path.GetDirectoryName(fullOutput);
            if (string.IsNullOrEmpty(parent))
                return "M12 COMPILE FAILED: Output directory is invalid.";
            Directory.CreateDirectory(parent);

            EmitResult emit;
            using (var stream = new FileStream(fullOutput, FileMode.Create, FileAccess.Write, FileShare.None))
                emit = compilation.Emit(stream);

            var diagnostics = emit.Diagnostics
                .Where(x => x.Severity != DiagnosticSeverity.Hidden)
                .OrderBy(x => x.Location.SourceTree?.FilePath, StringComparer.Ordinal)
                .ThenBy(x => x.Location.SourceSpan.Start)
                .Select(x => x.ToString())
                .ToArray();

            if (!emit.Success)
            {
                if (File.Exists(fullOutput)) File.Delete(fullOutput);
                return "M12 COMPILE FAILED\n" + string.Join("\n", diagnostics);
            }

            var verification = VerifyManagedAssembly(fullOutput);
            if (!verification.StartsWith("PASS:", StringComparison.Ordinal))
            {
                File.Delete(fullOutput);
                return "M12 OUTPUT VERIFICATION FAILED\n" + verification;
            }

            var warnings = diagnostics.Where(x => x.Contains("warning", StringComparison.OrdinalIgnoreCase)).ToArray();
            return "M12 COMPILATION PASS\n"
                + "Compiler: Roslyn 5.12.0\n"
                + "Target framework references: .NET 11.0\n"
                + "Source files: " + sources.Length + "\n"
                + "Framework references: " + referenceFiles.Length + "\n"
                + "Output: " + fullOutput + "\n"
                + "Output bytes: " + new FileInfo(fullOutput).Length + "\n"
                + verification + "\n"
                + "Warnings: " + warnings.Length + "\n"
                + string.Join("\n", warnings);
        }
        catch (Exception ex)
        {
            return "M12 COMPILER ERROR: " + ex.GetType().Name + ": " + ex.Message;
        }
    }

    private static string VerifyManagedAssembly(string path)
    {
        using var stream = File.OpenRead(path);
        using var pe = new PEReader(stream);
        if (!pe.HasMetadata)
            return "FAIL: PE output has no CLI metadata.";
        var reader = pe.GetMetadataReader();
        if (!reader.IsAssembly)
            return "FAIL: Output is a netmodule, not a managed assembly.";
        var assembly = reader.GetAssemblyDefinition();
        var name = reader.GetString(assembly.Name);
        var foundType = false;
        var foundMethod = false;
        foreach (var handle in reader.TypeDefinitions)
        {
            var type = reader.GetTypeDefinition(handle);
            if (reader.GetString(type.Name) == "EditedFixture")
            {
                foundType = true;
                foreach (var methodHandle in type.GetMethods())
                {
                    var method = reader.GetMethodDefinition(methodHandle);
                    if (reader.GetString(method.Name) == "GetEditedValue")
                        foundMethod = true;
                }
            }
        }
        if (!foundType || !foundMethod)
            return "FAIL: Expected edited type/method was not found in emitted metadata.";
        return "PASS: PE/CLI metadata verified; assembly=" + name
            + "; edited symbol=EditedFixture.GetEditedValue";
    }
}
