using DroidInspect.M11;
using System.Diagnostics;
if(args.Length!=2){Console.Error.WriteLine("Usage: RealAssemblyTests <assembly.dll> <reference-dir>");return 2;}
var timer=Stopwatch.StartNew();
var source=DecompilationService.Decompile(args[0],args[1]);
timer.Stop();
var required=new[]{"JsonConvert","SerializeObject","JsonSerializer","JsonReader","JsonWriter"};
foreach(var token in required) if(!source.Contains(token,StringComparison.Ordinal)) throw new InvalidOperationException("Missing Newtonsoft.Json C# symbol: "+token);
if(source.Contains("IL_0000",StringComparison.Ordinal)) throw new InvalidOperationException("Output is raw IL, not C#.");
if(source.StartsWith("M11 DECOMPILATION ERROR:",StringComparison.Ordinal)) throw new InvalidOperationException(source);
Console.WriteLine("PASS: ILSpy reconstructed C# from Newtonsoft.Json 13.0.3.");
Console.WriteLine("Verified symbols: "+string.Join(", ",required));
Console.WriteLine("Output characters: "+source.Length);
Console.WriteLine("Host decompilation milliseconds: "+timer.ElapsedMilliseconds);
return 0;
