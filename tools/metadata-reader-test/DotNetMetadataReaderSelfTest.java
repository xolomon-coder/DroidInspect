package com.tanvir.droidinspect;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Host-JVM smoke test using a tiny synthetic ECMA-335 metadata root. */
public final class DotNetMetadataReaderSelfTest {
    private static final class Strings {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        Strings() { out.write(0); }
        int add(String s) {
            int index = out.size();
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            out.write(b, 0, b.length);
            out.write(0);
            return index;
        }
        byte[] bytes() { return out.toByteArray(); }
    }

    public static void main(String[] args) throws Exception {
        byte[] metadata = fixture();
        File temp = File.createTempFile("droidinspect-cli-", ".bin");
        try {
            try (RandomAccessFile raf = new RandomAccessFile(temp, "rw")) {
                raf.write(metadata);
                String report = DotNetMetadataReader.inspect(raf, 0, metadata.length, metadata.length);
                require(report, "Name: TestAssembly");
                require(report, "Version: 1.2.3.4");
                require(report, "Module name: module.dll");
                require(report, "MVID:");
                require(report, "mscorlib");
                require(report, "TestNamespace.TestType");
                require(report, "Field: fieldOne");
                require(report, "Method: Run");
                if (report.contains("Metadata decoding: INCOMPLETE")) {
                    throw new AssertionError(report);
                }
                System.out.println("PASS: ECMA-335 module, assembly, reference, type, field and method metadata decoded.");
            }
        } finally {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    private static byte[] fixture() throws Exception {
        Strings strings = new Strings();
        int module = strings.add("module.dll");
        int assembly = strings.add("TestAssembly");
        int namespace = strings.add("TestNamespace");
        int type = strings.add("TestType");
        int field = strings.add("fieldOne");
        int method = strings.add("Run");
        int reference = strings.add("mscorlib");
        byte[] stringHeap = strings.bytes();
        byte[] blobHeap = new byte[]{0};
        byte[] guidHeap = new byte[16];
        for (int i = 0; i < guidHeap.length; i++) guidHeap[i] = (byte)(i + 1);

        ByteArrayOutputStream tableBytes = new ByteArrayOutputStream();
        DataOutputStream t = new DataOutputStream(tableBytes);
        le32(t, 0); t.writeByte(2); t.writeByte(0); t.writeByte(0); t.writeByte(1);
        long valid = (1L << 0) | (1L << 2) | (1L << 4) | (1L << 6) | (1L << 32) | (1L << 35);
        le64(t, valid); le64(t, 0);
        for (int i = 0; i < 6; i++) le32(t, 1);

        // Module: generation, name, MVID, EncId, EncBaseId.
        le16(t, 0); le16(t, module); le16(t, 1); le16(t, 0); le16(t, 0);
        // TypeDef: flags, name, namespace, extends, FieldList, MethodList.
        le32(t, 1); le16(t, type); le16(t, namespace); le16(t, 0); le16(t, 1); le16(t, 1);
        // Field: flags, name, signature.
        le16(t, 6); le16(t, field); le16(t, 0);
        // MethodDef: RVA, ImplFlags, Flags, Name, Signature, ParamList.
        le32(t, 0); le16(t, 0); le16(t, 6); le16(t, method); le16(t, 0); le16(t, 1);
        // Assembly: HashAlgId, version, flags, public key, name, culture.
        le32(t, 0x8004); le16(t, 1); le16(t, 2); le16(t, 3); le16(t, 4);
        le32(t, 0); le16(t, 0); le16(t, assembly); le16(t, 0);
        // AssemblyRef: version, flags, key/token, name, culture, hash.
        le16(t, 4); le16(t, 0); le16(t, 0); le16(t, 0); le32(t, 0);
        le16(t, reference); le16(t, 0); le16(t, 0);

        byte[] tables = tableBytes.toByteArray();
        int headerSize = 24 + streamDescriptorSize("#~") + streamDescriptorSize("#Strings")
                + streamDescriptorSize("#Blob") + streamDescriptorSize("#GUID");
        int offTables = headerSize;
        int offStrings = offTables + tables.length;
        int offBlob = offStrings + stringHeap.length;
        int offGuid = offBlob + blobHeap.length;

        ByteArrayOutputStream rootBytes = new ByteArrayOutputStream();
        DataOutputStream root = new DataOutputStream(rootBytes);
        le32(root, 0x424A5342); le16(root, 1); le16(root, 1); le32(root, 0);
        le32(root, 4); root.write("v4.0".getBytes(StandardCharsets.US_ASCII));
        le16(root, 0); le16(root, 4);
        stream(root, offTables, tables.length, "#~");
        stream(root, offStrings, stringHeap.length, "#Strings");
        stream(root, offBlob, blobHeap.length, "#Blob");
        stream(root, offGuid, guidHeap.length, "#GUID");
        root.write(tables); root.write(stringHeap); root.write(blobHeap); root.write(guidHeap);
        return rootBytes.toByteArray();
    }

    private static int streamDescriptorSize(String name) {
        return 8 + ((name.length() + 1 + 3) & ~3);
    }

    private static void stream(DataOutputStream out, int offset, int size, String name) throws Exception {
        le32(out, offset); le32(out, size);
        byte[] b = name.getBytes(StandardCharsets.US_ASCII);
        out.write(b); out.writeByte(0);
        int padded = (b.length + 1 + 3) & ~3;
        for (int i = b.length + 1; i < padded; i++) out.writeByte(0);
    }

    private static void require(String report, String expected) {
        if (!report.contains(expected)) throw new AssertionError("Missing [" + expected + "] in report:\n" + report);
    }
    private static void le16(DataOutputStream o, int v) throws Exception { o.writeByte(v); o.writeByte(v >>> 8); }
    private static void le32(DataOutputStream o, long v) throws Exception { le16(o, (int)v); le16(o, (int)(v >>> 16)); }
    private static void le64(DataOutputStream o, long v) throws Exception { le32(o, v); le32(o, v >>> 32); }
}