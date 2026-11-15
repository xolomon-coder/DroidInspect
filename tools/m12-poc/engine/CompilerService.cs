using System.Reflection.Metadata;
using System.Reflection.PortableExecutable;
using System.Security.Cryptography;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.Emit;

namespace DroidInspect.M12;

public static class CompilerService
{
    public static string Compile(string sourceDirectory, string outputPath, string referenceDirectory, string? originalAssemblyPath = null)
    {
        string? temporaryOutput = null;
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

            var assemblyName = !string.IsNullOrWhiteSpace(originalAssemblyPath) && File.Exists(originalAssemblyPath)
                ? AssemblyCompatibilityReporter.GetAssemblyName(originalAssemblyPath)
                : Path.GetFileNameWithoutExtension(outputPath);
            if (string.IsNullOrWhiteSpace(assemblyName))
                return "M12 COMPILE FAILED: Output assembly name is invalid.";

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

            temporaryOutput = Path.Combine(parent,
                "." + Path.GetFileName(fullOutput) + "." + Guid.NewGuid().ToString("N") + ".tmp");

            EmitResult emit;
            using (var stream = new FileStream(temporaryOutput, FileMode.CreateNew, FileAccess.Write, FileShare.None))
                emit = compilation.Emit(stream);

            var diagnostics = emit.Diagnostics
                .Where(x => x.Severity != DiagnosticSeverity.Hidden)
                .OrderBy(x => x.Location.SourceTree?.FilePath, StringComparer.Ordinal)
                .ThenBy(x => x.Location.SourceSpan.Start)
                .Select(x => x.ToString())
                .ToArray();

            if (!emit.Success)
                return "M12 COMPILE FAILED\n" + string.Join("\n", diagnostics);

            var verification = VerifyManagedAssembly(temporaryOutput);
            if (!verification.StartsWith("PASS:", StringComparison.Ordinal))
                return "M12 OUTPUT VERIFICATION FAILED\n" + verification;

            // Replace the destination only after a complete, verified DLL exists.
            File.Move(temporaryOutput, fullOutput, true);
            temporaryOutput = null;

            var warnings = diagnostics.Where(x =>
                x.Contains("warning", StringComparison.OrdinalIgnoreCase)).ToArray();
            return "M12 COMPILATION PASS\n"
                + "Compiler: Roslyn 5.12.0\n"
                + "Reference profile: caller-supplied directory (currently .NET 11.0 in the Android proof)\n"
                + "Source files: " + sources.Length + "\n"
                + "Framework references: " + referenceFiles.Length + "\n"
                + "Output: " + fullOutput + "\n"
                + "Output bytes: " + new FileInfo(fullOutput).Length + "\n"
                + verification + "\n"
                + "Warnings: " + warnings.Length + "\n"
                + string.Join("\n", warnings) + "\n\n"
                + (string.IsNullOrWhiteSpace(originalAssemblyPath)
                    ? "M12 DLL REPLACEMENT COMPATIBILITY REPORT\nLEVEL 1 — COMPILATION: PASS\nLEVEL 2 — STRUCTURAL COMPATIBILITY: NOT VERIFIED (no original assembly path was supplied)\nLEVEL 3 — RUNTIME / BEHAVIORAL COMPATIBILITY: NOT VERIFIED."
                    : AssemblyCompatibilityReporter.Compare(originalAssemblyPath, fullOutput));
        }
        catch (Exception ex)
        {
            return "M12 COMPILER ERROR: " + ex.GetType().Name + ": " + ex.Message;
        }
        finally
        {
            if (temporaryOutput != null && File.Exists(temporaryOutput))
            {
                try { File.Delete(temporaryOutput); } catch { }
            }
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
        var culture = assembly.Culture.IsNil ? "neutral" : reader.GetString(assembly.Culture);
        var publicKey = assembly.PublicKey.IsNil ? Array.Empty<byte>() : reader.GetBlobBytes(assembly.PublicKey);
        var token = GetPublicKeyToken(publicKey);
        var typeCount = reader.TypeDefinitions.Count;
        var methodCount = reader.MethodDefinitions.Count;

        return "PASS: PE/CLI metadata verified\n"
            + "Assembly name: " + name + "\n"
            + "Version: " + assembly.Version + "\n"
            + "Culture: " + culture + "\n"
            + "Public-key token: " + token + "\n"
            + "Public types: " + typeCount + "\n"
            + "Methods: " + methodCount + "\n"
            + "Strong-name public key present: " + (publicKey.Length > 0 ? "yes" : "no");
    }

    private static string GetPublicKeyToken(byte[] publicKey)
    {
        if (publicKey.Length == 0) return "null";
        using var sha1 = SHA1.Create();
        var hash = sha1.ComputeHash(publicKey);
        var token = new byte[8];
        for (var i = 0; i < token.Length; i++)
            token[i] = hash[hash.Length - 1 - i];
        return Convert.ToHexString(token).ToLowerInvariant();
    }
}
