using System.Reflection;
using System.Reflection.Metadata;
using System.Reflection.PortableExecutable;
using System.Security.Cryptography;

namespace DroidInspect.M12;

public static class DependencyResolver
{
    private sealed class Identity
    {
        public string Name = "";
        public string Version = "";
        public string Culture = "neutral";
        public string PublicKeyToken = "null";

        public bool Matches(Identity other) =>
            StringComparer.OrdinalIgnoreCase.Equals(Name, other.Name)
            && StringComparer.Ordinal.Equals(Version, other.Version)
            && StringComparer.OrdinalIgnoreCase.Equals(Culture, other.Culture)
            && StringComparer.OrdinalIgnoreCase.Equals(PublicKeyToken, other.PublicKeyToken);

        public override string ToString() =>
            Name + ", Version=" + Version + ", Culture=" + Culture + ", PublicKeyToken=" + PublicKeyToken;
    }

    private sealed class AssemblyInfo
    {
        public string Path = "";
        public Identity Identity = new();
        public string TargetFramework = "not declared";
        public List<Identity> References = new();
    }

    public static string Resolve(string rootAssemblyPath, string localDependencyDirectory, string referenceDirectory)
    {
        try
        {
            if (!File.Exists(rootAssemblyPath))
                return "OFFLINE DEPENDENCY RESOLUTION\nRESULT: FAIL\nRoot assembly file does not exist.";
            if (!Directory.Exists(localDependencyDirectory))
                return "OFFLINE DEPENDENCY RESOLUTION\nRESULT: FAIL\nLocal dependency directory does not exist.";
            if (!Directory.Exists(referenceDirectory))
                return "OFFLINE DEPENDENCY RESOLUTION\nRESULT: FAIL\nFramework reference directory does not exist.";

            var root = ReadAssembly(rootAssemblyPath);
            var local = IndexDirectory(localDependencyDirectory);
            var framework = IndexDirectory(referenceDirectory);
            var queue = new Queue<AssemblyInfo>();
            var visited = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            var report = new System.Text.StringBuilder();
            var resolvedLocal = 0;
            var resolvedFramework = 0;
            var missing = 0;
            var mismatched = 0;
            var ambiguous = 0;
            var processed = 0;
            var frameworkWarnings = new List<string>();

            report.AppendLine("DROIDINSPECT — OFFLINE .NET DEPENDENCY RESOLUTION");
            report.AppendLine("Network access: NOT USED");
            report.AppendLine("Root assembly: " + root.Identity);
            report.AppendLine("Target framework: " + root.TargetFramework);
            report.AppendLine("Available reference profile: " + new DirectoryInfo(referenceDirectory).Name);
            if (root.TargetFramework == "not declared")
                frameworkWarnings.Add("Root assembly has no readable TargetFrameworkAttribute; framework compatibility is NOT VERIFIED.");
            else if (!StringComparer.OrdinalIgnoreCase.Equals(root.TargetFramework, ".NETCoreApp,Version=v11.0"))
                frameworkWarnings.Add("The bundled reference profile is .NET 11.0 and does not exactly match the root target framework '" + root.TargetFramework + "'. It is a candidate reference set, not a proven compatible target pack.");

            report.AppendLine();
            report.AppendLine("DEPENDENCY GRAPH");
            queue.Enqueue(root);
            visited.Add(Path.GetFullPath(root.Path));

            while (queue.Count > 0 && processed < 500)
            {
                var current = queue.Dequeue();
                processed++;
                report.AppendLine();
                report.AppendLine((current.Path == root.Path ? "[ROOT] " : "[LOCAL DEPENDENCY] ") + current.Identity);
                if (current.References.Count == 0)
                {
                    report.AppendLine("  No AssemblyRef rows.");
                    continue;
                }

                foreach (var requested in current.References)
                {
                    var localCandidates = Find(local, requested.Name);
                    var frameworkCandidates = Find(framework, requested.Name);
                    var exactLocal = localCandidates.Where(x => x.Identity.Matches(requested)).ToArray();
                    var exactFramework = frameworkCandidates.Where(x => x.Identity.Matches(requested)).ToArray();

                    if (exactLocal.Length > 1)
                    {
                        ambiguous++;
                        report.AppendLine("  AMBIGUOUS LOCAL MATCH: " + requested + " (" + exactLocal.Length + " exact files).");
                        continue;
                    }
                    if (exactLocal.Length == 1)
                    {
                        var found = exactLocal[0];
                        resolvedLocal++;
                        report.AppendLine("  RESOLVED LOCAL: " + requested.Name + " -> " + Path.GetFileName(found.Path));
                        var full = Path.GetFullPath(found.Path);
                        if (visited.Add(full)) queue.Enqueue(found);
                        continue;
                    }
                    if (exactFramework.Length > 1)
                    {
                        ambiguous++;
                        report.AppendLine("  AMBIGUOUS REFERENCE-PACK MATCH: " + requested + " (" + exactFramework.Length + " exact files).");
                        continue;
                    }
                    if (exactFramework.Length == 1)
                    {
                        resolvedFramework++;
                        report.AppendLine("  RESOLVED REFERENCE PACK: " + requested.Name + " -> " + Path.GetFileName(exactFramework[0].Path));
                        continue;
                    }

                    var sameName = localCandidates.Concat(frameworkCandidates).ToArray();
                    if (sameName.Length > 0)
                    {
                        mismatched++;
                        report.AppendLine("  IDENTITY MISMATCH: requested " + requested);
                        foreach (var candidate in sameName.Take(4))
                            report.AppendLine("    Available: " + candidate.Identity + " [" + (localCandidates.Contains(candidate) ? "local" : "reference pack") + "]");
                    }
                    else
                    {
                        missing++;
                        report.AppendLine("  UNRESOLVED: " + requested);
                    }
                }
            }

            if (queue.Count > 0)
                report.AppendLine("\nWARNING: dependency graph traversal reached the 500-assembly safety cap.");

            report.AppendLine();
            report.AppendLine("SUMMARY");
            report.AppendLine("Assemblies inspected: " + processed);
            report.AppendLine("Resolved from user-provided local dependencies: " + resolvedLocal);
            report.AppendLine("Resolved from bundled reference pack: " + resolvedFramework);
            report.AppendLine("Missing references: " + missing);
            report.AppendLine("Identity/version mismatches: " + mismatched);
            report.AppendLine("Ambiguous exact matches: " + ambiguous);
            foreach (var warning in frameworkWarnings) report.AppendLine("FRAMEWORK WARNING: " + warning);

            var incomplete = missing > 0 || mismatched > 0 || ambiguous > 0 || queue.Count > 0;
            report.AppendLine();
            report.AppendLine("RESOLUTION RESULT: " + (incomplete ? "INCOMPLETE — DO NOT ASSUME COMPATIBILITY" : "ALL DISCOVERED REFERENCES RESOLVED EXACTLY"));
            report.AppendLine("Resolution does not prove runtime loadability or behavioral compatibility. No incompatible version was silently substituted.");
            return report.ToString().TrimEnd();
        }
        catch (Exception error)
        {
            return "DROIDINSPECT — OFFLINE .NET DEPENDENCY RESOLUTION\n"
                + "RESULT: FAIL\n"
                + error.GetType().Name + ": " + error.Message;
        }
    }

    private static Dictionary<string, List<AssemblyInfo>> IndexDirectory(string directory)
    {
        var index = new Dictionary<string, List<AssemblyInfo>>(StringComparer.OrdinalIgnoreCase);
        foreach (var path in Directory.EnumerateFiles(directory, "*", SearchOption.TopDirectoryOnly)
                     .Where(x => x.EndsWith(".dll", StringComparison.OrdinalIgnoreCase)
                         || x.EndsWith(".exe", StringComparison.OrdinalIgnoreCase)))
        {
            try
            {
                var info = ReadAssembly(path);
                if (!index.TryGetValue(info.Identity.Name, out var list))
                    index[info.Identity.Name] = list = new List<AssemblyInfo>();
                list.Add(info);
            }
            catch
            {
                // Non-managed and malformed candidates are ignored, never treated as a resolution.
            }
        }
        return index;
    }

    private static List<AssemblyInfo> Find(Dictionary<string, List<AssemblyInfo>> index, string name) =>
        index.TryGetValue(name, out var list) ? list : new List<AssemblyInfo>();

    private static AssemblyInfo ReadAssembly(string path)
    {
        using var stream = File.OpenRead(path);
        using var pe = new PEReader(stream);
        if (!pe.HasMetadata) throw new BadImageFormatException("PE file has no CLI metadata: " + Path.GetFileName(path));
        var reader = pe.GetMetadataReader();
        if (!reader.IsAssembly) throw new BadImageFormatException("Managed file is a netmodule, not an assembly: " + Path.GetFileName(path));
        var assembly = reader.GetAssemblyDefinition();
        var info = new AssemblyInfo
        {
            Path = path,
            Identity = new Identity
            {
                Name = reader.GetString(assembly.Name),
                Version = assembly.Version.ToString(),
                Culture = assembly.Culture.IsNil ? "neutral" : reader.GetString(assembly.Culture),
                PublicKeyToken = GetPublicKeyToken(assembly.PublicKey.IsNil ? Array.Empty<byte>() : reader.GetBlobBytes(assembly.PublicKey))
            },
            TargetFramework = ReadTargetFramework(reader, assembly.GetCustomAttributes())
        };

        foreach (var handle in reader.AssemblyReferences)
        {
            var reference = reader.GetAssemblyReference(handle);
            var key = reference.PublicKeyOrToken.IsNil ? Array.Empty<byte>() : reader.GetBlobBytes(reference.PublicKeyOrToken);
            var token = (reference.Flags & AssemblyFlags.PublicKey) != 0
                ? GetPublicKeyToken(key)
                : key.Length == 0 ? "null" : Convert.ToHexString(key).ToLowerInvariant();
            info.References.Add(new Identity
            {
                Name = reader.GetString(reference.Name),
                Version = reference.Version.ToString(),
                Culture = reference.Culture.IsNil ? "neutral" : reader.GetString(reference.Culture),
                PublicKeyToken = token
            });
        }
        return info;
    }

    private static string ReadTargetFramework(MetadataReader reader, CustomAttributeHandleCollection attributes)
    {
        foreach (var handle in attributes)
        {
            var attribute = reader.GetCustomAttribute(handle);
            EntityHandle typeHandle;
            if (attribute.Constructor.Kind == HandleKind.MethodDefinition)
                typeHandle = reader.GetMethodDefinition((MethodDefinitionHandle)attribute.Constructor).GetDeclaringType();
            else if (attribute.Constructor.Kind == HandleKind.MemberReference)
                typeHandle = reader.GetMemberReference((MemberReferenceHandle)attribute.Constructor).Parent;
            else continue;

            string fullName = "";
            if (typeHandle.Kind == HandleKind.TypeReference)
            {
                var type = reader.GetTypeReference((TypeReferenceHandle)typeHandle);
                var ns = type.Namespace.IsNil ? "" : reader.GetString(type.Namespace);
                fullName = ns.Length == 0 ? reader.GetString(type.Name) : ns + "." + reader.GetString(type.Name);
            }
            else if (typeHandle.Kind == HandleKind.TypeDefinition)
            {
                var type = reader.GetTypeDefinition((TypeDefinitionHandle)typeHandle);
                var ns = type.Namespace.IsNil ? "" : reader.GetString(type.Namespace);
                fullName = ns.Length == 0 ? reader.GetString(type.Name) : ns + "." + reader.GetString(type.Name);
            }
            if (!StringComparer.Ordinal.Equals(fullName, "System.Runtime.Versioning.TargetFrameworkAttribute")) continue;
            var blob = reader.GetBlobReader(attribute.Value);
            if (blob.ReadUInt16() != 1) return "invalid attribute blob";
            return blob.ReadSerializedString() ?? "empty TargetFrameworkAttribute";
        }
        return "not declared";
    }

    private static string GetPublicKeyToken(byte[] publicKey)
    {
        if (publicKey.Length == 0) return "null";
        using var sha = SHA1.Create();
        var hash = sha.ComputeHash(publicKey);
        var token = new byte[8];
        for (var i = 0; i < token.Length; i++) token[i] = hash[hash.Length - 1 - i];
        return Convert.ToHexString(token).ToLowerInvariant();
    }
}
