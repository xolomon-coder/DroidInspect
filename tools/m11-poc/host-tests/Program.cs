using DroidInspect.M11;
if(args.Length!=2){Console.Error.WriteLine("Usage: HostTests <fixture.dll> <reference-dir>");return 2;}
var source=DecompilationService.Decompile(args[0],args[1]);
var required=new[]{"class Calculator","int Add(int left, int right)","for (","try","catch","Echo<T>","class Nested<U>","Multiply"};
foreach(var token in required) if(!source.Contains(token,StringComparison.Ordinal)) throw new InvalidOperationException("Missing reconstructed C# construct: "+token);
if(source.Contains("IL_0000",StringComparison.Ordinal)) throw new InvalidOperationException("Output is raw IL, not C#.");
Console.WriteLine("PASS: ILSpy reconstructed C# from compiled fixture.");
Console.WriteLine("Verified: "+string.Join(", ",required));
Console.WriteLine("Output characters: "+source.Length);
return 0;
