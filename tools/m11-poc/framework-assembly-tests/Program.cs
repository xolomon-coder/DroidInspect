using DroidInspect.M11;
using System.Diagnostics;

if (args.Length != 2)
{
    Console.Error.WriteLine("Usage: FrameworkAssemblyTests <System.Text.Json.dll> <reference-search-path>");
    return 2;
}

var timer = Stopwatch.StartNew();
var source = DecompilationService.Decompile(args[0], args[1]);
timer.Stop();

var required = new[] { "JsonSerializer", "JsonDocument", "JsonElement", "Utf8JsonWriter", "JsonSerializerOptions" };
foreach (var token in required)
    if (!source.Contains(token, StringComparison.Ordinal))
        throw new InvalidOperationException("System.Text.Json C# output is missing expected symbol: " + token);

if (source.Contains("IL_0000", StringComparison.Ordinal))
    throw new InvalidOperationException("System.Text.Json output is raw IL, not C#.");

Console.WriteLine("PASS: Microsoft System.Text.Json runtime assembly reconstructed as C#.");
Console.WriteLine("Input bytes: " + new FileInfo(args[0]).Length);
Console.WriteLine("Verified symbols: " + string.Join(", ", required));
Console.WriteLine("Output characters: " + source.Length);
Console.WriteLine("Host decompilation milliseconds: " + timer.ElapsedMilliseconds);
var diagnosticStart = source.IndexOf("// M11 DEPENDENCY DIAGNOSTICS:", StringComparison.Ordinal);
if (diagnosticStart >= 0)
    Console.WriteLine("Dependency diagnostic header present; unresolved references must be reviewed separately.");
return 0;
