using DroidInspect.M11;

if (args.Length != 2)
{
    Console.Error.WriteLine("Usage: ExeTests <managed-exe> <reference-dir>");
    return 2;
}

var source = DecompilationService.Decompile(args[0], args[1]);
var required = new[] { "class Program", "interface IRunner", "static int Main", "Run(IEnumerable<int>", "Where(", "DescribeAsync" };
foreach (var token in required)
    if (!source.Contains(token, StringComparison.Ordinal))
        throw new InvalidOperationException("Managed EXE C# output is missing: " + token);

if (source.Contains("IL_0000", StringComparison.Ordinal))
    throw new InvalidOperationException("Managed EXE output is raw IL, not C#.");

Console.WriteLine("PASS: managed console EXE reconstructed as C# source.");
Console.WriteLine("Verified symbols: " + string.Join(", ", required));
Console.WriteLine("Output characters: " + source.Length);
return 0;
