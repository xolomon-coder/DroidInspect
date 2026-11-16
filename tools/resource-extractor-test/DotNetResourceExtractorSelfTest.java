package com.tanvir.droidinspect;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/** Synthetic PE/CLI fixture validates bounded embedded resource extraction and source immutability. */
public final class DotNetResourceExtractorSelfTest {
    public static void main(String[] args) throws Exception {
        File fixture = File.createTempFile("droidinspect-resource-", ".dll");
        try {
            createFixture(fixture);
            byte[] before = digest(fixture);
            byte[] actual = DotNetResourceExtractor.extractEmbeddedResource(fixture, 0);
            byte[] expected = "hello".getBytes(StandardCharsets.UTF_8);
            if (!Arrays.equals(expected, actual))
                throw new AssertionError("Embedded resource bytes differ from fixture payload.");

            expectFailure(fixture, 0x1d, "offset outside");
            if (!Arrays.equals(before, digest(fixture)))
                throw new AssertionError("Extractor modified the source assembly.");

            File truncated = File.createTempFile("droidinspect-resource-truncated-", ".dll");
            try {
                Files.copy(fixture.toPath(), truncated.toPath(), StandardCopyOption.REPLACE_EXISTING);
                try (RandomAccessFile raf = new RandomAccessFile(truncated, "rw")) {
                    le32(raf, 0x300, 0x40);
                }
                expectFailure(truncated, 0, "truncated");
            } finally {
                truncated.delete();
            }
            System.out.println("PASS: embedded resource bytes, bounds rejection, truncated payload rejection, and source immutability.");
        } finally {
            //noinspection ResultOfMethodCallIgnored
            fixture.delete();
        }
    }

    private static void createFixture(File file) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(0x400);
            raf.seek(0);
            raf.write('M'); raf.write('Z');
            le32(raf, 0x3c, 0x80);
            le32(raf, 0x80, 0x00004550L);
            le16(raf, 0x84, 0x8664);
            le16(raf, 0x86, 1);
            le16(raf, 0x94, 224);
            long optional = 0x98;
            le16(raf, optional, 0x10b);
            le32(raf, optional + 208, 0x2000);
            le32(raf, optional + 212, 72);
            long section = optional + 224;
            le32(raf, section + 8, 0x200);
            le32(raf, section + 12, 0x2000);
            le32(raf, section + 16, 0x200);
            le32(raf, section + 20, 0x200);
            le32(raf, 0x200, 72);
            le32(raf, 0x200 + 24, 0x2100);
            le32(raf, 0x200 + 28, 0x20);
            le32(raf, 0x300, 5);
            raf.seek(0x304);
            raf.write("hello".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void expectFailure(File file, long offset, String label) throws Exception {
        try {
            DotNetResourceExtractor.extractEmbeddedResource(file, offset);
            throw new AssertionError("Expected " + label + " resource rejection.");
        } catch (IllegalArgumentException | java.io.IOException expected) {
            // Expected: invalid range or invalid embedded payload.
        }
    }

    private static byte[] digest(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) md.update(buffer, 0, n);
        }
        return md.digest();
    }

    private static void le16(RandomAccessFile raf, long offset, int value) throws Exception {
        raf.seek(offset); raf.write(value & 255); raf.write((value >>> 8) & 255);
    }

    private static void le32(RandomAccessFile raf, long offset, long value) throws Exception {
        le16(raf, offset, (int) value);
        le16(raf, offset + 2, (int) (value >>> 16));
    }
}
