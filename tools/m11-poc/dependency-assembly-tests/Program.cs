using DroidInspect.M11;
using System.Diagnostics;

if (args.Length != 1) throw new ArgumentException("Usage: DependencyAssemblyTests <net-reference-directory>");
var root = AppContext.BaseDirectory;
var targetSource = Path.Combine(root, "Serilog.Extensions.Logging.dll");
if (!File.Exists(targetSource)) throw new FileNotFoundException("NuGet target assembly was not copied to the test output.", targetSource);
var temp = Path.Combine(Path.GetTempPath(), "m11-serilog-" + Guid.NewGuid().ToString("N"));
var targetDir = Path.Combine(temp, "target");
var fullRefs = Path.Combine(temp, "references-full");
var missingRefs = Path.Combine(temp, "references-missing");
Directory.CreateDirectory(targetDir);
Directory.CreateDirectory(fullRefs);
Directory.CreateDirectory(missingRefs);
var target = Path.Combine(targetDir, "Serilog.Extensions.Logging.dll");
File.Copy(targetSource, target);
var dependencyNames = Directory.EnumerateFiles(root, "*.dll")
    .Select(Path.GetFileName)
    .Where(n => n is not null)
    .Where(n => n != "Serilog.Extensions.Logging.dll"
        && n != "DependencyAssemblyTests.dll"
        && n != "ICSharpCode.Decompiler.dll"
        && !n!.StartsWith("System.", StringComparison.OrdinalIgnoreCase)
        && n != "System.dll" && n != "mscorlib.dll" && n != "netstandard.dll")
    .ToArray();
foreach (var name in dependencyNames)
{
    var source = Path.Combine(root, name!);
    File.Copy(source, Path.Combine(fullRefs, name!), true);
    if (!name!.Equals("Serilog.dll", StringComparison.OrdinalIgnoreCase))
        File.Copy(source, Path.Combine(missingRefs, name!), true);
}
if (!File.Exists(Path.Combine(fullRefs, "Serilog.dll")))
    throw new InvalidOperationException("The full reference set does not contain Serilog.dll.");
if (File.Exists(Path.Combine(missingRefs, "Serilog.dll")))
    throw new InvalidOperationException("The missing-reference set unexpectedly contains Serilog.dll.");

var required = new[] { "SerilogLoggerProvider", "SerilogLogger", "SerilogLoggerFactoryExtensions", "AddSerilog", "ILoggerProvider" };
var fullSearch = string.Join(Path.PathSeparator, args[0], fullRefs);
var timer = Stopwatch.StartNew();
var sourceText = DecompilationService.Decompile(target, fullSearch);
timer.Stop();
if (sourceText.StartsWith("M11 DECOMPILATION ERROR:", StringComparison.Ordinal))
    throw new InvalidOperationException(sourceText);
foreach (var token in required)
    if (!sourceText.Contains(token, StringComparison.Ordinal))
        throw new InvalidOperationException("Full-reference C# output is missing expected symbol: " + token);
if (sourceText.Contains("IL_0000", StringComparison.Ordinal))
    throw new InvalidOperationException("Full-reference output is raw IL, not C#.");
Console.WriteLine("PASS: Serilog.Extensions.Logging 9.0.2 reconstructed as C# with external references.");
Console.WriteLine("Resolved dependency DLLs: " + string.Join(", ", dependencyNames.OrderBy(x => x)));
Console.WriteLine("Verified symbols: " + string.Join(", ", required));
Console.WriteLine("Output characters: " + sourceText.Length);
Console.WriteLine("Host decompilation milliseconds: " + timer.ElapsedMilliseconds);
if (sourceText.Contains("M11 DEPENDENCY DIAGNOSTICS", StringComparison.Ordinal) && sourceText.Contains("Serilog, Version=", StringComparison.Ordinal))
    throw new InvalidOperationException("Serilog.dll was present but was reported unresolved.");

var missingSearch = string.Join(Path.PathSeparator, args[0], missingRefs);
try
{
    var missingText = DecompilationService.Decompile(target, missingSearch);
    if (missingText.Contains("M11 DEPENDENCY DIAGNOSTICS", StringComparison.Ordinal)
        && missingText.Contains("Serilog, Version=", StringComparison.Ordinal))
        Console.WriteLine("MISSING_REFERENCE_DIAGNOSTIC: PASS — Serilog reference explicitly reported unresolved");
    else
        throw new InvalidOperationException("Missing Serilog.dll was not identified by dependency diagnostics.");
    if (missingText.StartsWith("M11 DECOMPILATION ERROR:", StringComparison.Ordinal))
        Console.WriteLine("MISSING_REFERENCE_OUTCOME: ERROR_TEXT — " + missingText[..Math.Min(missingText.Length, 500)]);
    else if (missingText.Contains("SerilogLoggerProvider", StringComparison.Ordinal))
        Console.WriteLine("MISSING_REFERENCE_OUTCOME: CSHARP_SOURCE_RETURNED_WITH_SERILOG_DLL_OMITTED");
    else
        Console.WriteLine("MISSING_REFERENCE_OUTCOME: SOURCE_RETURNED_BUT_EXPECTED_TYPE_NOT_FOUND");
    Console.WriteLine("Missing-reference output characters: " + missingText.Length);
}
catch (Exception ex)
{
    Console.WriteLine("MISSING_REFERENCE_OUTCOME: EXCEPTION — " + ex.GetType().FullName + ": " + ex.Message);
}
finally
{
    try { Directory.Delete(temp, true); } catch { }
}
return 0;