using DroidInspect.M11;
using System.Diagnostics;
if(args.Length!=2){Console.Error.WriteLine("Usage: HostTests <assembly.dll> <reference-dir>");return 2;}
var timer=Stopwatch.StartNew();
var source=DecompilationService.Decompile(args[0],args[1]);
timer.Stop();
var required=new[]{"namespace Newtonsoft.Json","class JsonConvert","SerializeObject","class JsonSerializer","class JsonReader","class JsonWriter"};
foreach(var token in required) if(!source.Contains(token,StringComparison.Ordinal)) throw new InvalidOperationException("Missing real-assembly C# construct: "+token);
if(source.Contains("IL_0000",StringComparison.Ordinal)) throw new InvalidOperationException("Output is raw IL, not C#.");
if(source.StartsWith("M11 DECOMPILATION ERROR:",StringComparison.Ordinal)) throw new InvalidOperationException(source);
Console.WriteLine("PASS: ILSpy reconstructed C# from Newtonsoft.Json 13.0.3.");
Console.WriteLine("Verified: "+string.Join(", ",required));
Console.WriteLine("Output characters: "+source.Length);
Console.WriteLine("Host decompilation milliseconds: "+timer.ElapsedMilliseconds);
return 0;
