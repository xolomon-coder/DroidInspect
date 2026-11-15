using System.Reflection;
using System.Reflection.Metadata;
using System.Reflection.PortableExecutable;
using System.Security.Cryptography;

namespace DroidInspect.M12;

internal static class AssemblyCompatibilityReporter
{
    private sealed class Snapshot
    {
        public string Name = "";
        public string Version = "";
        public string Culture = "neutral";
        public string PublicKeyToken = "null";
        public string TargetFramework = "not declared";
        public bool StrongNameSigned;
        public readonly SortedSet<string> Types = new(StringComparer.Ordinal);
        public readonly SortedSet<string> Methods = new(StringComparer.Ordinal);
        public readonly SortedSet<string> Fields = new(StringComparer.Ordinal);
        public readonly SortedSet<string> Properties = new(StringComparer.Ordinal);
        public readonly SortedSet<string> Events = new(StringComparer.Ordinal);
        public readonly SortedSet<string> References = new(StringComparer.Ordinal);
    }

    internal static string GetAssemblyName(string path)
    {
        try
        {
            var snapshot = Read(path);
            return string.IsNullOrWhiteSpace(snapshot.Name)
                ? Path.GetFileNameWithoutExtension(path)
                : snapshot.Name;
        }
        catch
        {
            return Path.GetFileNameWithoutExtension(path);
        }
    }

    internal static string Compare(string originalPath, string rebuiltPath)
    {
        try
        {
            if (!File.Exists(originalPath))
                return "M12 DLL REPLACEMENT COMPATIBILITY REPORT\n"
                    + "LEVEL 1 — COMPILATION: PASS (compiler output was emitted and PE/CLI verified)\n"
                    + "LEVEL 2 — STRUCTURAL COMPATIBILITY: NOT VERIFIED (original assembly copy is unavailable)\n"
                    + "LEVEL 3 — RUNTIME / BEHAVIORAL COMPATIBILITY: NOT VERIFIED (runtime tests have not been run).";

            var original = Read(originalPath);
            var rebuilt = Read(rebuiltPath);
            var identityIssues = new List<string>();
            CompareValue(identityIssues, "Assembly name", original.Name, rebuilt.Name);
            CompareValue(identityIssues, "Assembly version", original.Version, rebuilt.Version);
            CompareValue(identityIssues, "Culture", original.Culture, rebuilt.Culture);
            CompareValue(identityIssues, "Public-key token", original.PublicKeyToken, rebuilt.PublicKeyToken);

            var removedTypes = original.Types.Except(rebuilt.Types).ToArray();
            var removedMethods = original.Methods.Except(rebuilt.Methods).ToArray();
            var removedFields = original.Fields.Except(rebuilt.Fields).ToArray();
            var removedProperties = original.Properties.Except(rebuilt.Properties).ToArray();
            var removedEvents = original.Events.Except(rebuilt.Events).ToArray();
            var missingReferences = original.References.Except(rebuilt.References).ToArray();
            var addedReferences = rebuilt.References.Except(original.References).ToArray();

            var structuralFailures = new List<string>();
            structuralFailures.AddRange(identityIssues);
            AddMissing(structuralFailures, "Public types removed", removedTypes);
            AddMissing(structuralFailures, "Public methods removed or signature changed", removedMethods);
            AddMissing(structuralFailures, "Public fields removed or signature changed", removedFields);
            AddMissing(structuralFailures, "Public properties removed or signature changed", removedProperties);
            AddMissing(structuralFailures, "Public events removed or signature changed", removedEvents);
            AddMissing(structuralFailures, "Original dependencies missing or mismatched", missingReferences);

            var structuralWarnings = new List<string>();
            if (original.TargetFramework == "not declared" || rebuilt.TargetFramework == "not declared")
                structuralWarnings.Add("Target framework: NOT VERIFIED (TargetFrameworkAttribute was not present on one or both assemblies).");
            else if (!StringComparer.Ordinal.Equals(original.TargetFramework, rebuilt.TargetFramework))
                structuralWarnings.Add("Target framework changed: " + original.TargetFramework + " -> " + rebuilt.TargetFramework + ".");
            if (addedReferences.Length > 0)
                structuralWarnings.Add("Additional assembly references: " + addedReferences.Length + ".");
            if (rebuilt.Types.Except(original.Types).Any())
                structuralWarnings.Add("Additional public types: " + rebuilt.Types.Except(original.Types).Count() + ".");
            if (rebuilt.Methods.Except(original.Methods).Any())
                structuralWarnings.Add("Additional public methods/signatures: " + rebuilt.Methods.Except(original.Methods).Count() + ".");
            if (original.StrongNameSigned && !rebuilt.StrongNameSigned)
                structuralFailures.Add("Original assembly is strong-name signed but rebuilt output is not signed.");
            else if (original.StrongNameSigned != rebuilt.StrongNameSigned)
                structuralWarnings.Add("Strong-name signing state differs between original and rebuilt assemblies.");

            var structuralStatus = structuralFailures.Count > 0 ? "FAIL"
                : structuralWarnings.Count > 0 ? "WARNING" : "PASS";

            var report = new System.Text.StringBuilder();
            report.AppendLine("M12 DLL REPLACEMENT COMPATIBILITY REPORT");
            report.AppendLine();
            report.AppendLine("LEVEL 1 — COMPILATION: PASS");
            report.AppendLine("A genuine managed PE/CLI DLL was emitted and its metadata was verified.");
            report.AppendLine();
            report.AppendLine("LEVEL 2 — STRUCTURAL COMPATIBILITY: " + structuralStatus);
            report.AppendLine("Original identity: " + Identity(original));
            report.AppendLine("Rebuilt identity:  " + Identity(rebuilt));
            report.AppendLine("Original target framework: " + original.TargetFramework);
            report.AppendLine("Rebuilt target framework:  " + rebuilt.TargetFramework);
            report.AppendLine("Public API counts (original -> rebuilt):");
            report.AppendLine("  Types: " + original.Types.Count + " -> " + rebuilt.Types.Count);
            report.AppendLine("  Methods/signatures: " + original.Methods.Count + " -> " + rebuilt.Methods.Count);
            report.AppendLine("  Fields/signatures: " + original.Fields.Count + " -> " + rebuilt.Fields.Count);
            report.AppendLine("  Properties/signatures: " + original.Properties.Count + " -> " + rebuilt.Properties.Count);
            report.AppendLine("  Events: " + original.Events.Count + " -> " + rebuilt.Events.Count);
            report.AppendLine("  Assembly references: " + original.References.Count + " -> " + rebuilt.References.Count);
            report.AppendLine("Strong-name signed (original -> rebuilt): " + original.StrongNameSigned + " -> " + rebuilt.StrongNameSigned);
            foreach (var issue in structuralFailures) report.AppendLine("FAIL: " + issue);
            foreach (var warning in structuralWarnings) report.AppendLine("WARNING: " + warning);
            if (structuralFailures.Count == 0 && structuralWarnings.Count == 0)
                report.AppendLine("PASS: identity, public API signatures, dependencies, target framework, and signing state match.");
            report.AppendLine();
            report.AppendLine("LEVEL 3 — RUNTIME / BEHAVIORAL COMPATIBILITY: NOT VERIFIED");
            report.AppendLine("Compilation and metadata comparison do not prove runtime behavior. The rebuilt DLL has not been loaded into the original host application or exercised against its real dependencies.");
            report.AppendLine();
            report.AppendLine("Safety: original assembly remains unchanged; rebuilt DLL is a separate project output.");
            return report.ToString().TrimEnd();
        }
        catch (Exception error)
        {
            return "M12 DLL REPLACEMENT COMPATIBILITY REPORT\n"
                + "LEVEL 1 — COMPILATION: PASS (managed PE/CLI output was emitted)\n"
                + "LEVEL 2 — STRUCTURAL COMPATIBILITY: NOT VERIFIED — "
                + error.GetType().Name + ": " + error.Message + "\n"
                + "LEVEL 3 — RUNTIME / BEHAVIORAL COMPATIBILITY: NOT VERIFIED.";
        }
    }

    private static Snapshot Read(string path)
    {
        using var stream = File.OpenRead(path);
        using var pe = new PEReader(stream);
        if (!pe.HasMetadata) throw new BadImageFormatException("No CLI metadata was found.");
        var reader = pe.GetMetadataReader();
        if (!reader.IsAssembly) throw new BadImageFormatException("The PE is a netmodule, not an assembly.");
        var assembly = reader.GetAssemblyDefinition();
        var snapshot = new Snapshot
        {
            Name = reader.GetString(assembly.Name),
            Version = assembly.Version.ToString(),
            Culture = assembly.Culture.IsNil ? "neutral" : reader.GetString(assembly.Culture),
            PublicKeyToken = PublicKeyToken(assembly.PublicKey.IsNil ? Array.Empty<byte>() : reader.GetBlobBytes(assembly.PublicKey)),
            StrongNameSigned = pe.PEHeaders.CorHeader != null
                && (pe.PEHeaders.CorHeader.Flags & CorFlags.StrongNameSigned) != 0,
            TargetFramework = ReadTargetFramework(reader, assembly.GetCustomAttributes())
        };

        foreach (var handle in reader.TypeDefinitions)
        {
            var type = reader.GetTypeDefinition(handle);
            var visibility = type.Attributes & TypeAttributes.VisibilityMask;
            if (visibility != TypeAttributes.Public && visibility != TypeAttributes.NestedPublic) continue;
            var typeName = FullTypeName(reader, type.Namespace, type.Name);
            snapshot.Types.Add(typeName);

            foreach (var methodHandle in type.GetMethods())
            {
                var method = reader.GetMethodDefinition(methodHandle);
                if (!IsPublic(method.Attributes)) continue;
                snapshot.Methods.Add(typeName + "::" + reader.GetString(method.Name)
                    + " " + BlobHex(reader, method.Signature));
            }
            foreach (var fieldHandle in type.GetFields())
            {
                var field = reader.GetFieldDefinition(fieldHandle);
                if (!IsPublic(field.Attributes)) continue;
                snapshot.Fields.Add(typeName + "::" + reader.GetString(field.Name)
                    + " " + BlobHex(reader, field.Signature));
            }
            foreach (var propertyHandle in type.GetProperties())
            {
                var property = reader.GetPropertyDefinition(propertyHandle);
                var accessors = property.GetAccessors();
                if (!IsPublic(reader, accessors.Getter) && !IsPublic(reader, accessors.Setter)) continue;
                snapshot.Properties.Add(typeName + "::" + reader.GetString(property.Name)
                    + " " + BlobHex(reader, property.Signature));
            }
            foreach (var eventHandle in type.GetEvents())
            {
                var ev = reader.GetEventDefinition(eventHandle);
                var accessors = ev.GetAccessors();
                if (!IsPublic(reader, accessors.Adder) && !IsPublic(reader, accessors.Remover)
                    && !IsPublic(reader, accessors.Raiser)) continue;
                snapshot.Events.Add(typeName + "::" + reader.GetString(ev.Name));
            }
        }

        foreach (var handle in reader.AssemblyReferences)
        {
            var reference = reader.GetAssemblyReference(handle);
            var name = reader.GetString(reference.Name);
            var culture = reference.Culture.IsNil ? "neutral" : reader.GetString(reference.Culture);
            var key = reference.PublicKeyOrToken.IsNil ? Array.Empty<byte>() : reader.GetBlobBytes(reference.PublicKeyOrToken);
            var token = (reference.Flags & AssemblyFlags.PublicKey) != 0 ? PublicKeyToken(key) : Convert.ToHexString(key).ToLowerInvariant();
            snapshot.References.Add(name + ", Version=" + reference.Version + ", Culture=" + culture + ", PublicKeyToken=" + (token.Length == 0 ? "null" : token));
        }
        return snapshot;
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
                fullName = FullTypeName(reader, type.Namespace, type.Name);
            }
            else if (typeHandle.Kind == HandleKind.TypeDefinition)
            {
                var type = reader.GetTypeDefinition((TypeDefinitionHandle)typeHandle);
                fullName = FullTypeName(reader, type.Namespace, type.Name);
            }
            if (!StringComparer.Ordinal.Equals(fullName, "System.Runtime.Versioning.TargetFrameworkAttribute")) continue;
            var blob = reader.GetBlobReader(attribute.Value);
            if (blob.ReadUInt16() != 1) return "invalid attribute blob";
            return blob.ReadSerializedString() ?? "empty TargetFrameworkAttribute";
        }
        return "not declared";
    }

    private static bool IsPublic(MethodAttributes attributes)
    {
        var access = attributes & MethodAttributes.MemberAccessMask;
        return access == MethodAttributes.Public || access == MethodAttributes.Family
            || access == MethodAttributes.FamORAssem;
    }

    private static bool IsPublic(FieldAttributes attributes)
    {
        var access = attributes & FieldAttributes.FieldAccessMask;
        return access == FieldAttributes.Public || access == FieldAttributes.Family
            || access == FieldAttributes.FamORAssem;
    }

    private static bool IsPublic(MetadataReader reader, MethodDefinitionHandle handle)
    {
        return !handle.IsNil && IsPublic(reader.GetMethodDefinition(handle).Attributes);
    }

    private static string FullTypeName(MetadataReader reader, StringHandle ns, StringHandle name)
    {
        var space = ns.IsNil ? "" : reader.GetString(ns);
        var type = name.IsNil ? "" : reader.GetString(name);
        return space.Length == 0 ? type : space + "." + type;
    }

    private static string BlobHex(MetadataReader reader, BlobHandle handle)
    {
        return handle.IsNil ? "" : Convert.ToHexString(reader.GetBlobBytes(handle));
    }

    private static string PublicKeyToken(byte[] publicKey)
    {
        if (publicKey.Length == 0) return "null";
        using var sha = SHA1.Create();
        var hash = sha.ComputeHash(publicKey);
        var token = new byte[8];
        for (var i = 0; i < token.Length; i++) token[i] = hash[hash.Length - 1 - i];
        return Convert.ToHexString(token).ToLowerInvariant();
    }

    private static string Identity(Snapshot value)
    {
        return value.Name + ", Version=" + value.Version + ", Culture=" + value.Culture
            + ", PublicKeyToken=" + value.PublicKeyToken;
    }

    private static void CompareValue(List<string> issues, string label, string original, string rebuilt)
    {
        if (!StringComparer.Ordinal.Equals(original, rebuilt))
            issues.Add(label + " mismatch: original '" + original + "', rebuilt '" + rebuilt + "'.");
    }

    private static void AddMissing(List<string> issues, string label, string[] missing)
    {
        if (missing.Length > 0)
            issues.Add(label + ": " + missing.Length + " item(s); first: " + missing[0]);
    }
}
