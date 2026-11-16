package com.tanvir.droidinspect;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class FileInspectionEngine {
    private static final long MAX_COPY_BYTES = 512L * 1024L * 1024L;
    private static final int MAX_ARCHIVE_ENTRIES = 5000;

    private FileInspectionEngine() { }

    static String inspect(Context context, Uri uri) throws Exception {
        File file = copyToCache(context, uri);
        try {
            String name = queryName(context, uri);
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".apk") || hasApkManifest(file)) return inspectApk(context, file, name);
            if (lower.endsWith(".dll") || lower.endsWith(".exe") || hasMzSignature(file)) return inspectPe(file, name);
            return inspectUnknown(file, name);
        } finally {
            // The temporary copy is no longer needed after inspection.
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private static File copyToCache(Context context, Uri uri) throws Exception {
        File out = File.createTempFile("droidinspect-", ".bin", context.getCacheDir());
        long total = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(out)) {
            if (in == null) throw new IllegalStateException("Android could not open the selected document.");
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                checkCancelled();
                total += read;
                if (total > MAX_COPY_BYTES) throw new IllegalArgumentException("File exceeds the current 512 MB inspection limit.");
                fos.write(buffer, 0, read);
            }
            fos.flush();
            return out;
        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            out.delete();
            throw e;
        }
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("File inspection cancelled because the dashboard activity was closed.");
    }

    private static String queryName(Context context, Uri uri) {
        String name = uri.getLastPathSegment();
        android.database.Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (index >= 0) name = cursor.getString(index);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return name == null ? "Selected file" : name;
    }

    private static boolean hasApkManifest(File file) {
        try (ZipFile zip = new ZipFile(file)) {
            return zip.getEntry("AndroidManifest.xml") != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean hasMzSignature(File file) {
        if (file.length() < 2) return false;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            return readU16(raf, 0) == 0x5A4D;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String inspectUnknown(File file, String name) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            StringBuilder out = new StringBuilder();
            out.append("DROIDINSPECT — FILE IDENTIFICATION\n\n");
            out.append("Name: ").append(name).append('\n');
            out.append("Size: ").append(file.length()).append(" bytes\n");
            if (file.length() >= 2 && readU16(raf, 0) == 0x5A4D) {
                out.append("\nDetected: DOS/Windows PE candidate (MZ signature).\n");
                out.append("The extension is not APK, DLL, or EXE. Use a .dll/.exe filename to run PE/CLI inspection.");
            } else {
                out.append("\nDetected: Unknown or unsupported file type.\n");
                out.append("Supported inputs in this Phase 1 build: .apk, .dll, .exe.");
            }
            return out.toString();
        }
    }

    private static String inspectApk(Context context, File file, String name) throws Exception {
        StringBuilder out = new StringBuilder();
        out.append("DROIDINSPECT — APK INSPECTION\n\n");
        out.append("File: ").append(name).append('\n');
        out.append("Size: ").append(file.length()).append(" bytes\n");
        int dexCount = 0;
        int shown = 0;
        List<String> entries = new ArrayList<>();
        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                if (entries.size() < MAX_ARCHIVE_ENTRIES) entries.add(entry.getName());
                if (entry.getName().matches("classes([0-9]+)?\\.dex")) dexCount++;
                shown++;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("The file has an .apk extension but is not a readable ZIP/APK archive: " + e.getMessage());
        }
        out.append("\nARCHIVE\n");
        out.append("ZIP structure: PASS\n");
        out.append("Entries: ").append(shown).append(shown > MAX_ARCHIVE_ENTRIES ? " (display capped at 5000)" : "").append('\n');
        out.append("DEX files: ").append(dexCount).append('\n');
        out.append("\nARCHIVE CONTENTS (first ").append(entries.size()).append(")\n");
        for (String entry : entries) out.append(" • ").append(entry).append('\n');

        out.append("\nANDROID PACKAGE METADATA\n");
        PackageInfo info = null;
        try {
            int flags = PackageManager.GET_PERMISSIONS | PackageManager.GET_ACTIVITIES
                    | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS
                    | PackageManager.GET_PROVIDERS;
            info = context.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), flags);
        } catch (Exception ignored) {
        }
        if (info == null) {
            out.append("Package metadata: UNAVAILABLE — Android PackageManager could not parse this APK manifest.\n");
            out.append("The archive may be malformed, use unsupported manifest data, or be a non-installable APK variant.");
        } else {
            out.append("Package name: ").append(info.packageName).append('\n');
            out.append("Version name: ").append(info.versionName == null ? "(not declared)" : info.versionName).append('\n');
            long versionCode = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
            out.append("Version code: ").append(versionCode).append('\n');
            if (info.applicationInfo != null) {
                info.applicationInfo.sourceDir = file.getAbsolutePath();
                info.applicationInfo.publicSourceDir = file.getAbsolutePath();
                CharSequence label = context.getPackageManager().getApplicationLabel(info.applicationInfo);
                out.append("Application label: ").append(label).append('\n');
                out.append("Target SDK: ").append(info.applicationInfo.targetSdkVersion).append('\n');
            }
            if (info.requestedPermissions != null) {
                out.append("Declared permissions (").append(info.requestedPermissions.length).append("):\n");
                for (String permission : info.requestedPermissions) out.append(" • ").append(permission).append('\n');
            } else out.append("Declared permissions: none reported\n");
            out.append("Activities: ").append(info.activities == null ? 0 : info.activities.length).append('\n');
            out.append("Services: ").append(info.services == null ? 0 : info.services.length).append('\n');
            out.append("Receivers: ").append(info.receivers == null ? 0 : info.receivers.length).append('\n');
            out.append("Providers: ").append(info.providers == null ? 0 : info.providers.length).append('\n');
        }
        out.append("\nMANIFEST NOTE\n");
        out.append("Binary AndroidManifest.xml is present in the APK archive when listed above. Full decoded manifest/resource editing is not part of Phase 1.");
        return out.toString();
    }

    static String inspectPe(File file, String name) throws Exception {
        StringBuilder out = new StringBuilder();
        out.append("DROIDINSPECT — PE / .NET INSPECTION\n\n");
        out.append("File: ").append(name).append('\n');
        out.append("Size: ").append(file.length()).append(" bytes\n");
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (file.length() < 64 || readU16(raf, 0) != 0x5A4D)
                return out.append("\nPE check: FAIL — missing MZ DOS signature.").toString();
            long peOffset = readU32(raf, 0x3C);
            if (!inBounds(peOffset, 24, file.length()) || readU32(raf, peOffset) != 0x00004550L)
                return out.append("\nPE check: FAIL — missing or invalid PE signature.").toString();

            int machine = readU16(raf, peOffset + 4);
            int sectionCount = readU16(raf, peOffset + 6);
            int optionalSize = readU16(raf, peOffset + 20);
            long optional = peOffset + 24;
            int magic = readU16(raf, optional);
            int dataDirectoryOffset = magic == 0x10B ? 96 : (magic == 0x20B ? 112 : -1);
            if (dataDirectoryOffset < 0 || optionalSize < dataDirectoryOffset + 15 * 8)
                return out.append("\nPE check: PASS\nOptional header: unsupported or truncated.\n.NET CLI status: INCONCLUSIVE").toString();

            out.append("PE signature: PASS\n");
            out.append("Machine: ").append(machineName(machine)).append(" (0x").append(hex(machine, 4)).append(")\n");
            out.append("Optional header: ").append(magic == 0x10B ? "PE32" : "PE32+").append('\n');
            out.append("Sections: ").append(sectionCount).append('\n');

            long cliRva = readU32(raf, optional + dataDirectoryOffset + 14L * 8);
            long cliSize = readU32(raf, optional + dataDirectoryOffset + 14L * 8 + 4);
            if (cliRva == 0 || cliSize == 0) {
                out.append("\n.NET CLI header: NOT PRESENT\n");
                out.append("Classification: Native PE or managed metadata not detected.");
                return out.toString();
            }
            long sectionTable = optional + optionalSize;
            long cliOffset = rvaToOffset(raf, cliRva, sectionTable, sectionCount, file.length());
            if (cliOffset < 0 || !inBounds(cliOffset, 24, file.length())) {
                out.append("\n.NET CLI header: INVALID RVA / OUT OF FILE BOUNDS\n");
                out.append("Classification: PE file with a CLI directory entry, but invalid section mapping.");
                return out.toString();
            }
            long metadataRva = readU32(raf, cliOffset + 8);
            long metadataSize = readU32(raf, cliOffset + 12);
            long metadataOffset = rvaToOffset(raf, metadataRva, sectionTable, sectionCount, file.length());
            out.append("\n.NET CLI header: PRESENT\n");
            out.append("CLI header size: ").append(readU32(raf, cliOffset)).append('\n');
            out.append("Runtime: ").append(readU16(raf, cliOffset + 4)).append('.').append(readU16(raf, cliOffset + 6)).append('\n');
            out.append("Metadata RVA: 0x").append(Long.toHexString(metadataRva).toUpperCase(Locale.ROOT)).append('\n');
            out.append("Metadata size: ").append(metadataSize).append(" bytes\n");
            out.append("Flags: 0x").append(hex(readU32(raf, cliOffset + 16), 8)).append('\n');
            out.append("Entry point token/RVA: 0x").append(hex(readU32(raf, cliOffset + 20), 8)).append('\n');
            out.append("Classification: MANAGED .NET / CLI PE candidate\n");

            if (metadataOffset < 0 || !inBounds(metadataOffset, 20, file.length())) {
                out.append("\nMetadata root: INVALID / OUT OF BOUNDS");
                return out.toString();
            }
            if (readU32(raf, metadataOffset) != 0x424A5342L) {
                out.append("\nMetadata root: INVALID — BSJB signature missing.");
                return out.toString();
            }
            int major = readU16(raf, metadataOffset + 4);
            int minor = readU16(raf, metadataOffset + 6);
            long versionLength = readU32(raf, metadataOffset + 12);
            if (versionLength > 1024 || !inBounds(metadataOffset + 16, versionLength + 4, file.length())) {
                out.append("\nMetadata root: invalid version string length.");
                return out.toString();
            }
            byte[] versionBytes = new byte[(int) versionLength];
            raf.seek(metadataOffset + 16);
            raf.readFully(versionBytes);
            String runtimeVersion = new String(versionBytes, StandardCharsets.UTF_8).replace("\u0000", "").trim();
            long flagsPos = align4(metadataOffset + 16 + versionLength);
            int streamCount = readU16(raf, flagsPos + 2);
            out.append("\nMETADATA ROOT\n");
            out.append("Metadata version: ").append(major).append('.').append(minor).append('\n');
            out.append("Runtime version string: ").append(runtimeVersion).append('\n');
            out.append("Metadata streams: ").append(streamCount).append('\n');
            long cursor = flagsPos + 4;
            for (int i = 0; i < streamCount && i < 32; i++) {
                if (!inBounds(cursor, 9, file.length())) break;
                long streamOffset = readU32(raf, cursor);
                long streamSize = readU32(raf, cursor + 4);
                StringBuilder streamName = new StringBuilder();
                long p = cursor + 8;
                int count = 0;
                while (p < file.length() && count < 32) {
                    int b = readByteAt(raf, p++);
                    if (b == 0) break;
                    streamName.append((char) b);
                    count++;
                }
                cursor = align4(p);
                out.append(" • ").append(streamName).append(": ").append(streamSize).append(" bytes");
                if (!inBounds(metadataOffset + streamOffset, streamSize, file.length())) out.append(" (invalid bounds)");
                out.append('\n');
            }
            out.append("\nMETADATA TABLE SUMMARY\n");
            out.append("Assembly table rows: ").append(findTableRows(raf, metadataOffset, metadataSize, file.length(), 0x20)).append('\n');
            out.append("Module table rows: ").append(findTableRows(raf, metadataOffset, metadataSize, file.length(), 0x00)).append('\n');
            out.append("TypeDef table rows: ").append(findTableRows(raf, metadataOffset, metadataSize, file.length(), 0x02)).append('\n');
            out.append("MethodDef table rows: ").append(findTableRows(raf, metadataOffset, metadataSize, file.length(), 0x06)).append('\n');
            out.append("AssemblyRef table rows: ").append(findTableRows(raf, metadataOffset, metadataSize, file.length(), 0x23)).append('\n');
            out.append(DotNetMetadataReader.inspect(raf, metadataOffset, metadataSize, file.length()));
            out.append("\nPHASE 1 SCOPE\n");
            out.append("This screen performs bounded PE/CLI metadata inspection. Raw IL browsing, constrained IL editing, and offline C# decompilation are available through separate DroidInspect tools. Metadata inspection does not prove Android runtime compatibility.");
        }
        return out.toString();
    }

    static byte[] extractEmbeddedManagedResource(File file, long resourceOffset) throws Exception {
        return DotNetResourceExtractor.extractEmbeddedResource(file, resourceOffset);
    }

    private static long findTableRows(RandomAccessFile raf, long metadataOffset, long metadataSize, long fileLength, int tableId) {
        try {
            long p = align4(metadataOffset + 16);
            int versionLength = (int) readU32(raf, metadataOffset + 12);
            p = align4(metadataOffset + 16L + versionLength);
            int streams = readU16(raf, p + 2);
            p += 4;
            long tablesOffset = -1, tablesSize = 0;
            for (int i = 0; i < streams && i < 32; i++) {
                long offset = readU32(raf, p);
                long size = readU32(raf, p + 4);
                StringBuilder n = new StringBuilder();
                long q = p + 8;
                for (int j = 0; j < 32; j++) {
                    int c = readByteAt(raf, q++);
                    if (c == 0) break;
                    n.append((char)c);
                }
                p = align4(q);
                if ("#~".contentEquals(n) || "#-".contentEquals(n)) {
                    tablesOffset = metadataOffset + offset;
                    tablesSize = size;
                }
            }
            if (tablesOffset < 0 || !inBounds(tablesOffset, 24, fileLength) || tablesSize > metadataSize) return -1;
            long valid = readU64(raf, tablesOffset + 8);
            long rowPos = tablesOffset + 24;
            for (int i = 0; i < 64; i++) {
                if ((valid & (1L << i)) != 0) {
                    long rows = readU32(raf, rowPos);
                    if (i == tableId) return rows;
                    rowPos += 4;
                }
            }
        } catch (Exception ignored) { }
        return -1;
    }

    private static long rvaToOffset(RandomAccessFile raf, long rva, long sectionTable, int sections, long length) throws Exception {
        for (int i = 0; i < sections && i < 96; i++) {
            long p = sectionTable + i * 40L;
            long virtualSize = readU32(raf, p + 8);
            long virtualAddress = readU32(raf, p + 12);
            long rawSize = readU32(raf, p + 16);
            long rawPointer = readU32(raf, p + 20);
            long span = Math.max(virtualSize, rawSize);
            if (rva >= virtualAddress && rva - virtualAddress < span) {
                long offset = rawPointer + (rva - virtualAddress);
                return inBounds(offset, 1, length) ? offset : -1;
            }
        }
        return -1;
    }

    private static boolean inBounds(long offset, long size, long length) {
        return offset >= 0 && size >= 0 && offset <= length && size <= length - offset;
    }
    private static long align4(long value) { return (value + 3L) & ~3L; }
    private static int readByteAt(RandomAccessFile r, long p) throws Exception { r.seek(p); return r.readUnsignedByte(); }
    private static int readU16(RandomAccessFile r, long p) throws Exception {
        r.seek(p); return r.readUnsignedByte() | (r.readUnsignedByte() << 8);
    }
    private static long readU32(RandomAccessFile r, long p) throws Exception {
        r.seek(p); return ((long)r.readUnsignedByte()) | ((long)r.readUnsignedByte() << 8)
                | ((long)r.readUnsignedByte() << 16) | ((long)r.readUnsignedByte() << 24);
    }
    private static long readU64(RandomAccessFile r, long p) throws Exception {
        return readU32(r, p) | (readU32(r, p + 4) << 32);
    }
    private static String hex(long value, int digits) {
        String s = Long.toHexString(value).toUpperCase(Locale.ROOT);
        while (s.length() < digits) s = "0" + s;
        return s;
    }
    private static String machineName(int m) {
        switch (m) {
            case 0x014c: return "x86";
            case 0x8664: return "x64";
            case 0x01c0: case 0x01c4: return "ARM";
            case 0xAA64: return "ARM64";
            case 0x0200: return "IA64";
            default: return "Unknown";
        }
    }
}
