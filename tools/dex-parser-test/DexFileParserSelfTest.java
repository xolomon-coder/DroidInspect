package com.tanvir.droidinspect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Host-JVM regression test for the bounded DEX structure reader. */
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
        String[] strings = {"Lx/A;", "Ljava/lang/Object;"};
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] header = new byte[187];
        header[0] = 'd'; header[1] = 'e'; header[2] = 'x'; header[3] = '\n';
        header[4] = '0'; header[5] = '3'; header[6] = '5'; header[7] = 0;
        put32(header, 32, header.length);
        put32(header, 36, 112);
        put32(header, 40, 0x12345678);
        put32(header, 56, 2); put32(header, 60, 112);
        put32(header, 64, 2); put32(header, 68, 120);
        put32(header, 96, 1); put32(header, 100, 128);
        put32(header, 104, header.length - 160); put32(header, 108, 160);
        put32(header, 112, 160); put32(header, 116, 167);
        put32(header, 120, 0); put32(header, 124, 1);
        put32(header, 128, 0); // class_idx
        put32(header, 132, 1); // public
        put32(header, 136, 1); // superclass_idx
        put32(header, 140, 0); // interfaces_off
        put32(header, 144, 0xffffffff); // source_file_idx
        put32(header, 148, 0); put32(header, 152, 0); put32(header, 156, 0);
        out.write(header);
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
