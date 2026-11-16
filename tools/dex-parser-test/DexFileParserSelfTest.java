package com.tanvir.droidinspect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InterruptedIOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Host-JVM regression test for multidex and independent DEX encoded member lists. */
public final class DexFileParserSelfTest {
    public static void main(String[] args) throws Exception {
        File apk = File.createTempFile("droidinspect-dex-test-", ".apk");
        try {
            byte[] dex = minimalDexWithSplitMemberLists();
            byte[] malformedAfterPartialParse = dex.clone();
            // class_data starts at 236; the virtual-method index_diff is byte 12.
            // The direct method is parsed and its invoke reference indexed before this invalid index.
            malformedAfterPartialParse[248] = 5;

            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(apk))) {
                zip.putNextEntry(new ZipEntry("classes.dex"));
                zip.write(dex);
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("classes2.dex"));
                zip.write(malformedAfterPartialParse);
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("classes10.dex"));
                zip.write(new byte[]{1, 2, 3, 4});
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("assets/classes3.dex"));
                zip.write(dex);
                zip.closeEntry();
            }

            DexFileParser.Result result = DexFileParser.analyzeApk(apk);
            check(result.dexCount == 3, "multidex discovery must include classes2.dex/classes10.dex and exclude nested assets");
            check(result.classes.size() == 1, "only the complete valid DEX class should be retained");
            DexFileParser.ClassInfo info = result.classes.get(0);
            check("x.A".equals(info.className), "class descriptor conversion");
            check("x".equals(info.packageName), "package extraction");
            check("java.lang.Object".equals(info.superClass), "superclass decoding");
            check(info.fields.size() == 2, "both encoded field lists should be decoded");
            check(info.fields.get(0).contains("int other"), "static field index starts from its own list base");
            check(info.fields.get(1).contains("int value"), "instance field index resets to its own list base");
            check(info.methods.size() == 2, "both encoded method lists should be decoded");
            check(info.methods.get(0).contains("void ping"), "direct method index starts from its own list base");
            check(info.methods.get(1).contains("void run"), "virtual method index resets to its own list base");
            check(info.references.size() == 1, "invoke instruction should produce one cross-reference");
            check(info.references.get(0).contains("x.A->ping"), "cross-reference should identify caller method");
            check(info.references.get(0).contains("x.A->run"), "cross-reference should resolve target method");
            check(result.totalMembers == 4, "member index count excludes rejected DEX partial data");
            check(result.totalReferences == 1, "cross-reference counts from rejected DEX entries must roll back transactionally");
            check(result.dexReports.size() == 3, "each DEX entry should have a report");
            check(result.dexReports.get(0).contains("PASS"), "valid DEX should pass");
            check(result.dexReports.get(1).contains("REJECTED"), "invalid member index should be rejected");
            check(result.dexReports.get(2).contains("REJECTED"), "malformed DEX should be rejected");
            Thread.currentThread().interrupt();
            try {
                DexFileParser.analyzeApk(apk);
                throw new AssertionError("pre-interrupted DEX analysis must stop before scanning");
            } catch (InterruptedIOException expected) {
                check(expected.getMessage().contains("cancelled"), "cancellation has a clear diagnostic");
            } finally {
                Thread.interrupted();
            }
            System.out.println("DexFileParserSelfTest: PASS");
        } finally {
            //noinspection ResultOfMethodCallIgnored
            apk.delete();
        }
    }

    private static byte[] minimalDexWithSplitMemberLists() throws Exception {
        String[] strings = {
                "Lx/A;", "Ljava/lang/Object;", "value", "run",
                "I", "V", "other", "ping"
        };
        byte[] classData = new byte[] {
                1, 1, 1, 1, // static fields, instance fields, direct methods, virtual methods
                1, 1,       // static field: field_id[1]
                0, 1,       // instance field: field_id[0], index delta resets
                1, 1, (byte) 0xB5, 0x02, // direct method: method_id[1] ping, code_off 309
                0, 1, 0     // virtual method: method_id[0], index delta resets, access, no code
        };
        int headerSize = 236;
        int stringsStart = headerSize + classData.length;
        int[] stringOffsets = new int[strings.length];
        int cursor = stringsStart;
        for (int i = 0; i < strings.length; i++) {
            stringOffsets[i] = cursor;
            cursor += 1 + strings[i].getBytes("UTF-8").length + 1;
        }
        int codeItemOffset = cursor;
        check(codeItemOffset == 309, "synthetic code item offset must match its ULEB128 reference");
        cursor += 24;

        byte[] header = new byte[headerSize];
        header[0] = 'd'; header[1] = 'e'; header[2] = 'x'; header[3] = '\n';
        header[4] = '0'; header[5] = '3'; header[6] = '5'; header[7] = 0;
        put32(header, 32, cursor);
        put32(header, 36, 112);
        put32(header, 40, 0x12345678);
        put32(header, 56, strings.length); put32(header, 60, 112);
        put32(header, 64, 4); put32(header, 68, 144);
        put32(header, 72, 1); put32(header, 76, 160);
        put32(header, 80, 2); put32(header, 84, 172);
        put32(header, 88, 2); put32(header, 92, 188);
        put32(header, 96, 1); put32(header, 100, 204);
        put32(header, 104, headerSize); put32(header, 108, headerSize);

        for (int i = 0; i < stringOffsets.length; i++) put32(header, 112 + i * 4, stringOffsets[i]);
        put32(header, 144, 0); put32(header, 148, 1); put32(header, 152, 4); put32(header, 156, 5);
        put32(header, 160, 5); put32(header, 164, 3); put32(header, 168, 0);
        // Two field_ids: value is index 0, other is index 1.
        put16(header, 172, 0); put16(header, 174, 2); put32(header, 176, 2);
        put16(header, 180, 0); put16(header, 182, 2); put32(header, 184, 6);
        // Two method_ids: run is index 0, ping is index 1.
        put16(header, 188, 0); put16(header, 190, 0); put32(header, 192, 3);
        put16(header, 196, 0); put16(header, 198, 0); put32(header, 200, 7);
        // class_def_item
        put32(header, 204, 0); put32(header, 208, 1); put32(header, 212, 1);
        put32(header, 216, 0xffffffff); put32(header, 220, 0); put32(header, 224, 0);
        put32(header, 228, headerSize); put32(header, 232, 0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(header);
        out.write(classData);
        for (String value : strings) {
            byte[] bytes = value.getBytes("UTF-8");
            out.write(bytes.length);
            out.write(bytes);
            out.write(0);
        }
        // code_item for ping: invoke-static method_id[0] (run), then return-void.
        put16(out, 1); put16(out, 0); put16(out, 0); put16(out, 0);
        put32(out, 0); put32(out, 4);
        put16(out, 0x0071); put16(out, 0); put16(out, 0); put16(out, 0x000e);
        byte[] result = out.toByteArray();
        check(result.length == cursor, "synthetic DEX fixture size");
        return result;
    }

    private static void put16(ByteArrayOutputStream out, int value) throws Exception {
        out.write(value & 255);
        out.write((value >>> 8) & 255);
    }

    private static void put32(ByteArrayOutputStream out, int value) throws Exception {
        put16(out, value);
        put16(out, value >>> 16);
    }

    private static void put16(byte[] data, int offset, int value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
    }

    private static void put32(byte[] data, int offset, int value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
