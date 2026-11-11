package com.tanvir.droidinspect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Host-JVM regression test for multidex, class, field, method, and malformed-entry handling. */
public final class DexFileParserSelfTest {
    public static void main(String[] args) throws Exception {
        File apk = File.createTempFile("droidinspect-dex-test-", ".apk");
        try {
            byte[] dex = minimalDex();
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(apk))) {
                zip.putNextEntry(new ZipEntry("classes.dex"));
                zip.write(dex);
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("classes10.dex"));
                zip.write(new byte[]{1, 2, 3, 4});
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("assets/classes3.dex"));
                zip.write(dex);
                zip.closeEntry();
            }
            DexFileParser.Result result = DexFileParser.analyzeApk(apk);
            check(result.dexCount == 2, "multidex discovery must include classes10.dex and exclude nested assets");
            check(result.classes.size() == 1, "one valid DEX class should be parsed");
            DexFileParser.ClassInfo info = result.classes.get(0);
            check("x.A".equals(info.className), "class descriptor conversion");
            check("x".equals(info.packageName), "package extraction");
            check("java.lang.Object".equals(info.superClass), "superclass decoding");
            check(info.fields.size() == 1 && info.fields.get(0).contains("int value"), "field name and type decoding");
            check(info.methods.size() == 1 && info.methods.get(0).contains("void run"), "method name and prototype decoding");
            check(result.totalMembers == 2, "member index count");
            check(result.dexReports.size() == 2, "each DEX entry should have a report");
            check(result.dexReports.get(0).contains("PASS"), "valid DEX should pass");
            check(result.dexReports.get(1).contains("REJECTED"), "malformed DEX should be rejected");
            System.out.println("DexFileParserSelfTest: PASS");
        } finally {
            //noinspection ResultOfMethodCallIgnored
            apk.delete();
        }
    }

    private static byte[] minimalDex() throws Exception {
        String[] strings = {"Lx/A;", "Ljava/lang/Object;", "value", "run", "I", "V"};
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] header = new byte[266];
        header[0] = 'd'; header[1] = 'e'; header[2] = 'x'; header[3] = '\n';
        header[4] = '0'; header[5] = '3'; header[6] = '5'; header[7] = 0;
        put32(header, 32, header.length);
        put32(header, 36, 112);
        put32(header, 40, 0x12345678);
        put32(header, 56, 6); put32(header, 60, 112);
        put32(header, 64, 4); put32(header, 68, 136);
        put32(header, 72, 1); put32(header, 76, 152);
        put32(header, 80, 1); put32(header, 84, 164);
        put32(header, 88, 1); put32(header, 92, 172);
        put32(header, 96, 1); put32(header, 100, 180);
        put32(header, 104, header.length - 212); put32(header, 108, 212);
        int[] stringOffsets = {221, 228, 248, 255, 260, 263};
        for (int i = 0; i < stringOffsets.length; i++) put32(header, 112 + i * 4, stringOffsets[i]);
        put32(header, 136, 0); put32(header, 140, 1);
        put32(header, 144, 4); put32(header, 148, 5);
        put32(header, 152, 5); put32(header, 156, 3); put32(header, 160, 0);
        header[164] = 0; header[165] = 0; header[166] = 2; header[167] = 0;
        put32(header, 168, 2);
        header[172] = 0; header[173] = 0; header[174] = 0; header[175] = 0;
        put32(header, 176, 3);
        put32(header, 180, 0); put32(header, 184, 1); put32(header, 188, 1);
        put32(header, 192, 0); put32(header, 196, 0xffffffff);
        put32(header, 200, 0); put32(header, 204, 212); put32(header, 208, 0);
        out.write(header);
        // class_data_item: one static field, one direct method, no other members.
        out.write(new byte[]{1, 0, 1, 0, 0, 1, 0, 1, 0});
        for (String value : strings) {
            byte[] bytes = value.getBytes("UTF-8");
            out.write(bytes.length);
            out.write(bytes);
            out.write(0);
        }
        byte[] result = out.toByteArray();
        check(result.length == header.length, "synthetic DEX fixture size");
        return result;
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
