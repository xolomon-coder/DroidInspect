using DroidInspect.M11;

if (args.Length != 2)
{
    Console.Error.WriteLine("Usage: RobustnessTests <fixture.dll> <reference-dir>");
    return 2;
}

var fixture = args[0];
var references = args[1];
var temp = Path.Combine(Path.GetTempPath(), "m11-invalid-" + Guid.NewGuid().ToString("N"));
Directory.CreateDirectory(temp);
try
{
    ExpectFailure(Path.Combine(temp, "missing.dll"), "FileNotFoundException", "missing path");

    var empty = Path.Combine(temp, "empty.dll");
    File.WriteAllBytes(empty, Array.Empty<byte>());
    ExpectFailure(empty, "InvalidDataException", "empty input");

    var text = Path.Combine(temp, "not-managed.dll");
    File.WriteAllText(text, "This is not a PE or .NET assembly.");
    ExpectFailure(text, "InvalidDataException", "non-PE input");

    var truncated = Path.Combine(temp, "truncated.dll");
    File.WriteAllBytes(truncated, new byte[] { 0x4D, 0x5A, 0x00, 0x00, 0x01 });
    ExpectFailure(truncated, "InvalidDataException", "truncated PE input");

    var nativeLike = Path.Combine(temp, "native.exe");
    File.WriteAllBytes(nativeLike, new byte[512]);
    ExpectFailure(nativeLike, "InvalidDataException", "invalid executable input");

    for (var i = 0; i < 3; i++)
    {
        var source = DecompilationService.Decompile(fixture, references);
        if (!source.Contains("class Calculator", StringComparison.Ordinal))
            throw new InvalidOperationException("Repeated decompilation " + (i + 1) + " returned incomplete output.");
    }

    Console.WriteLine("PASS: invalid-input diagnostics and three repeated decompilations.");
    return 0;
}
finally
{
    try { Directory.Delete(temp, true); } catch { }
}

static void ExpectFailure(string path, string expectedType, string scenario)
{
    try
    {
        DecompilationService.Decompile(path, string.Empty);
        throw new InvalidOperationException("Expected failure for " + scenario + ".");
    }
    catch (Exception ex) when (ex.GetType().Name == expectedType)
    {
        if (string.IsNullOrWhiteSpace(ex.Message))
            throw new InvalidOperationException("Empty diagnostic for " + scenario + ".");
        Console.WriteLine("PASS: " + scenario + " -> " + ex.GetType().Name + ": " + ex.Message);
    }
}
