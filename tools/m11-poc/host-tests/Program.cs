using DroidInspect.M11;

if (args.Length != 2)
{
    Console.Error.WriteLine("Usage: HostTests <fixture.dll> <reference-dir>");
    return 2;
}

var source = DecompilationService.Decompile(args[0], args[1]);
var required = new[]
{
    "class Calculator", "interface ICalculator", "CalculatorBase",
    "int Add(int left, int right)", "event", "get; private set;",
    "for (", "try", "catch", "Echo<T>", "Map<T, TResult>",
    "Where(", "AddLaterAsync", "yield", "class Nested<U>",
    "FixtureMarker", "enum FixtureMode", "readonly struct Coordinate",
    "Capture", "Multiply"
};
foreach (var token in required)
    if (!source.Contains(token, StringComparison.Ordinal))
        throw new InvalidOperationException("Missing reconstructed C# construct: " + token);

if (source.Contains("IL_0000", StringComparison.Ordinal))
    throw new InvalidOperationException("Output is raw IL, not C#.");

Console.WriteLine("PASS: ILSpy reconstructed expanded C# fixture.");
Console.WriteLine("Verified constructs/symbols: " + string.Join(", ", required));
Console.WriteLine("Output characters: " + source.Length);
return 0;
