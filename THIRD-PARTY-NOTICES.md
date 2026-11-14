# Third-party notices

DroidInspect's M11 production C# decompilation integration packages the ILSpy decompiler engine as a .NET Native AOT ARM64 shared library and includes the .NET 11 reference assemblies used for offline resolution.

## ILSpy / ICSharpCode.Decompiler

Project: https://github.com/icsharpcode/ILSpy  
Package: `ICSharpCode.Decompiler` 11.2.0.10050  
License: MIT

MIT license text:

Copyright (c) 2011-2025 AlphaSierraPapa for the ILSpy team

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify,
merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be included in all copies
or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## .NET Native AOT runtime and reference assemblies

Project: https://github.com/dotnet/runtime  
Components: .NET 11 Native AOT runtime components and Microsoft.NETCore.App.Ref 11.0.0 reference assemblies  
License: MIT

MIT license text:

Copyright (c) .NET Foundation and Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Distribution note

The Android application also contains existing third-party components documented in the project README and their respective upstream license files. Before public redistribution of a release, the complete transitive NuGet dependency graph and any generated Native AOT notices must be rechecked against the exact package lock/restore output used for that release. This notice records the direct ILSpy and .NET runtime/reference-pack license texts; it is not a claim that every transitive dependency audit is complete.
