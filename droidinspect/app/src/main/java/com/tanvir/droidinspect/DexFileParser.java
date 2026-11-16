package com.tanvir.droidinspect;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Bounded, read-only DEX structural reader.
 * It deliberately does not decompile instructions into Java source.
 */
public final class DexFileParser {
    public static final long MAX_DEX_BYTES = 128L * 1024L * 1024L;
    public static final int MAX_CLASSES = 10000;
    public static final int MAX_TOTAL_MEMBERS = 100000;
    public static final int MAX_TOTAL_REFERENCES = 100000;
    private static final int MAX_MEMBERS_PER_CLASS = 100;
    private static final int MAX_TABLE_ITEMS = 1000000;

    private DexFileParser() { }

    public static final class ClassInfo {
        public final String dexName;
        public final String packageName;
        public final String className;
        public final String descriptor;
        public final String access;
        public final String superClass;
        public boolean membersTruncated;
        public final List<String> fields = new ArrayList<>();
        public final List<String> methods = new ArrayList<>();
        public final List<String> references = new ArrayList<>();
        public final List<String> referenceWarnings = new ArrayList<>();
        public boolean referencesTruncated;

        ClassInfo(String dex, String descriptor, String access, String superClass) {
            this.dexName = dex;
            this.descriptor = descriptor;
            this.className = javaName(descriptor);
            int dot = className.lastIndexOf('.');
            this.packageName = dot < 0 ? "(default package)" : className.substring(0, dot);
            this.access = access;
            this.superClass = superClass;
        }
    }

    public static final class Result {
        public final List<ClassInfo> classes = new ArrayList<>();
        public final List<String> dexReports = new ArrayList<>();
        public int dexCount;
        public int totalClasses;
        public int totalMembers;
        public int totalReferences;
        public boolean classLimitReached;
        public boolean memberLimitReached;
        public boolean referenceLimitReached;
        public String warning = "";
    }

    public static Result analyzeApk(File apk) throws Exception {
        Result result = new Result();
        List<ZipEntry> dexEntries = new ArrayList<>();
        try (ZipFile zip = new ZipFile(apk)) {
            Enumeration<? extends ZipEntry> it = zip.entries();
            while (it.hasMoreElements()) {
                ZipEntry entry = it.nextElement();
                if (isDexName(entry.getName()) && !entry.isDirectory()) dexEntries.add(entry);
            }
            Collections.sort(dexEntries, Comparator.comparingInt(e -> dexNumber(e.getName())));
            result.dexCount = dexEntries.size();
            if (dexEntries.isEmpty()) {
                result.warning = "No root-level classes.dex / classesN.dex files were found.";
                return result;
            }
            File parent = apk.getParentFile();
            for (ZipEntry entry : dexEntries) {
                if (result.classes.size() >= MAX_CLASSES) {
                    result.classLimitReached = true;
                    result.warning = "Class display limit reached (" + MAX_CLASSES + "). Remaining DEX files were not parsed.";
                    break;
                }
                long declared = entry.getSize();
                if (declared > MAX_DEX_BYTES) {
                    result.dexReports.add(entry.getName() + ": SKIPPED — exceeds 128 MB per-DEX safety limit.");
                    continue;
                }
                File temp = File.createTempFile("di-dex-", ".dex", parent);
                try {
                    long copied = 0;
                    try (java.io.InputStream in = zip.getInputStream(entry);
                         java.io.FileOutputStream out = new java.io.FileOutputStream(temp)) {
                        byte[] buffer = new byte[32768];
                        int n;
                        while ((n = in.read(buffer)) != -1) {
                            copied += n;
                            if (copied > MAX_DEX_BYTES) throw new IOException("DEX entry exceeds 128 MB safety limit.");
                            out.write(buffer, 0, n);
                        }
                    }
                    int before = result.classes.size();
                    int membersBefore = result.totalMembers;
                    int referencesBefore = result.totalReferences;
                    boolean referenceLimitBefore = result.referenceLimitReached;
                    try {
                        parseDex(temp, entry.getName(), result);
                        result.dexReports.add(entry.getName() + ": PASS — " + (result.classes.size() - before) + " classes.");
                    } catch (Exception ex) {
                        // Parsing is transactional per DEX: do not expose partial classes or member counts.
                        while (result.classes.size() > before) result.classes.remove(result.classes.size() - 1);
                        result.totalMembers = membersBefore;
                        result.totalReferences = referencesBefore;
                        result.referenceLimitReached = referenceLimitBefore;
                        result.dexReports.add(entry.getName() + ": REJECTED — " + safeMessage(ex));
                    }
                } finally {
                    //noinspection ResultOfMethodCallIgnored
                    temp.delete();
                }
            }
            result.totalClasses = result.classes.size();
            Collections.sort(result.classes, Comparator
                    .comparing((ClassInfo c) -> c.packageName)
                    .thenComparing(c -> c.className));
            return result;
        }
    }

    private static void parseDex(File file, String dexName, Result result) throws Exception {
        if (file.length() < 112 || file.length() > MAX_DEX_BYTES) throw new IOException("Invalid DEX file length.");
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] magic = new byte[8];
            raf.seek(0);
            raf.readFully(magic);
            String version = new String(magic, 4, 3, StandardCharsets.US_ASCII);
            if (magic[0] != 'd' || magic[1] != 'e' || magic[2] != 'x' || magic[3] != '\n'
                    || magic[7] != 0 || !("035".equals(version) || "037".equals(version)
                    || "038".equals(version) || "039".equals(version) || "040".equals(version))) {
                throw new IOException("Unsupported DEX magic/version (supported: 035–040).");
            }
            long fileSize = u32(raf, 32);
            long headerSize = u32(raf, 36);
            long endian = u32(raf, 40);
            if (fileSize != file.length() || headerSize != 112 || endian != 0x12345678L)
                throw new IOException("Invalid DEX header size, file size, or endian tag.");

            int stringCount = count(raf, 56, file.length(), "string_ids");
            long stringOff = offset(raf, 60, stringCount, 4, file.length(), "string_ids");
            int typeCount = count(raf, 64, file.length(), "type_ids");
            long typeOff = offset(raf, 68, typeCount, 4, file.length(), "type_ids");
            int protoCount = count(raf, 72, file.length(), "proto_ids");
            long protoOff = offset(raf, 76, protoCount, 12, file.length(), "proto_ids");
            int fieldCount = count(raf, 80, file.length(), "field_ids");
            long fieldOff = offset(raf, 84, fieldCount, 8, file.length(), "field_ids");
            int methodCount = count(raf, 88, file.length(), "method_ids");
            long methodOff = offset(raf, 92, methodCount, 8, file.length(), "method_ids");
            int classCount = count(raf, 96, file.length(), "class_defs");
            long classOff = offset(raf, 100, classCount, 32, file.length(), "class_defs");
            if (classCount > MAX_CLASSES * 4) throw new IOException("DEX class count exceeds safety limit.");

            int[] stringOffsets = new int[stringCount];
            for (int i = 0; i < stringCount; i++) {
                long v = u32(raf, stringOff + i * 4L);
                if (v > file.length() - 1) throw new IOException("string_data offset is out of bounds.");
                stringOffsets[i] = (int) v;
            }
            int[] typeStringIndexes = new int[typeCount];
            for (int i = 0; i < typeCount; i++) {
                int idx = (int) u32(raf, typeOff + i * 4L);
                if (idx < 0 || idx >= stringCount) throw new IOException("Invalid type descriptor string index.");
                typeStringIndexes[i] = idx;
            }
            Map<Integer, String> strings = new HashMap<>();
            for (int i = 0; i < classCount && result.classes.size() < MAX_CLASSES; i++) {
                long p = classOff + i * 32L;
                int classIdx = (int) u32(raf, p);
                int access = (int) u32(raf, p + 4);
                int superIdx = (int) u32(raf, p + 8);
                long classDataOff = u32(raf, p + 24);
                if (classIdx < 0 || classIdx >= typeCount) throw new IOException("Invalid class type index.");
                String descriptor = typeDescriptor(raf, typeStringIndexes, stringOffsets, strings, file.length(), classIdx);
                String superName = superIdx == -1 ? "(none)" :
                        (superIdx >= 0 && superIdx < typeCount
                                ? javaName(typeDescriptor(raf, typeStringIndexes, stringOffsets, strings, file.length(), superIdx))
                                : "(invalid)");
                ClassInfo info = new ClassInfo(dexName, descriptor, accessFlags(access), superName);
                result.classes.add(info);
                if (classDataOff != 0) {
                    if (classDataOff >= file.length()) throw new IOException("class_data offset is out of bounds.");
                    parseClassData(raf, classDataOff, file.length(), info, result, typeStringIndexes,
                            stringOffsets, strings, protoOff, protoCount, fieldOff, fieldCount,
                            methodOff, methodCount);
                }
            }
            if (classCount > result.classes.size() && result.classes.size() >= MAX_CLASSES) result.classLimitReached = true;
            if (result.totalMembers >= MAX_TOTAL_MEMBERS) result.memberLimitReached = true;
        }
    }

    private static void parseClassData(RandomAccessFile raf, long p, long length, ClassInfo info, Result result,
            int[] typeStrings, int[] stringOffsets, Map<Integer, String> strings,
            long protoOff, int protoCount, long fieldOff, int fieldCount,
            long methodOff, int methodCount) throws Exception {
        Cursor c = new Cursor(p);
        int staticFields = uleb(raf, c, length);
        int instanceFields = uleb(raf, c, length);
        int directMethods = uleb(raf, c, length);
        int virtualMethods = uleb(raf, c, length);
        if ((long) staticFields + instanceFields > 200000L || (long) directMethods + virtualMethods > 200000L)
            throw new IOException("Unreasonable class_data member count.");
        int fieldIndex = 0;
        for (int i = 0; i < staticFields + instanceFields; i++) {
            // DEX field_idx_diff is relative to the start of each encoded field list.
            if (i == staticFields) fieldIndex = 0;
            fieldIndex += uleb(raf, c, length);
            int flags = uleb(raf, c, length);
            if (fieldIndex < 0 || fieldIndex >= fieldCount) throw new IOException("Field index out of bounds.");
            if (info.fields.size() < MAX_MEMBERS_PER_CLASS && result.totalMembers < MAX_TOTAL_MEMBERS) {
                long f = fieldOff + fieldIndex * 8L;
                int type = u16(raf, f + 2);
                int name = (int) u32(raf, f + 4);
                if (type >= typeStrings.length || name < 0 || name >= stringOffsets.length) throw new IOException("Invalid field reference.");
                String fieldName = string(raf, name, stringOffsets, strings, length);
                String fieldType = javaName(string(raf, typeStrings[type], stringOffsets, strings, length));
                info.fields.add(accessFlags(flags) + fieldType + " " + fieldName);
                result.totalMembers++;
            } else {
                info.membersTruncated = true;
            }
        }
        int methodIndex = 0;
        for (int i = 0; i < directMethods + virtualMethods; i++) {
            // DEX method_idx_diff is relative to the start of each encoded method list.
            if (i == directMethods) methodIndex = 0;
            methodIndex += uleb(raf, c, length);
            int flags = uleb(raf, c, length);
            int codeOff = uleb(raf, c, length);
            if (methodIndex < 0 || methodIndex >= methodCount) throw new IOException("Method index out of bounds.");
            if (info.methods.size() < MAX_MEMBERS_PER_CLASS && result.totalMembers < MAX_TOTAL_MEMBERS) {
                long m = methodOff + methodIndex * 8L;
                int proto = u16(raf, m + 2);
                int name = (int) u32(raf, m + 4);
                if (proto >= protoCount || name < 0 || name >= stringOffsets.length) throw new IOException("Invalid method reference.");
                String methodName = string(raf, name, stringOffsets, strings, length);
                String signature = protoSignature(raf, protoOff, proto, typeStrings, stringOffsets, strings, length);
                info.methods.add(accessFlags(flags) + signature + " " + methodName);
                result.totalMembers++;
                if (codeOff != 0 && info.references.size() < 2000) {
                    String caller = info.className + "->" + methodName + signature;
                    try {
                        scanInvokeReferences(raf, codeOff, length, caller, info, result, typeStrings,
                                stringOffsets, strings, protoOff, protoCount, methodOff, methodCount);
                    } catch (Exception scanError) {
                        if (info.referenceWarnings.size() < 20)
                            info.referenceWarnings.add(caller + ": " + safeMessage(scanError));
                    }
                }
            } else {
                info.membersTruncated = true;
            }
        }
    }

    private static void scanInvokeReferences(RandomAccessFile raf, long codeOff, long length,
            String caller, ClassInfo info, Result result, int[] typeStrings, int[] stringOffsets,
            Map<Integer, String> strings, long protoOff, int protoCount,
            long methodOff, int methodCount) throws Exception {
        if (codeOff < 0 || codeOff > length - 16) throw new IOException("code_item header is out of bounds.");
        long instructionCount = u32(raf, codeOff + 12);
        long instructionStart = codeOff + 16;
        if (instructionCount > 1000000L || instructionCount * 2L > length - instructionStart)
            throw new IOException("code_item instruction array is invalid or exceeds scan limit.");
        long cursor = 0;
        while (cursor < instructionCount && info.references.size() < 2000
                && result.totalReferences < MAX_TOTAL_REFERENCES) {
            int unit = u16(raf, instructionStart + cursor * 2L);
            if (unit == 0x0100) {
                if (cursor + 2 > instructionCount) throw new IOException("Truncated packed-switch payload.");
                long size = u16(raf, instructionStart + (cursor + 1) * 2L);
                long width = 4L + size * 2L;
                if (width > instructionCount - cursor) throw new IOException("Packed-switch payload exceeds code_item.");
                cursor += width;
                continue;
            }
            if (unit == 0x0200) {
                if (cursor + 2 > instructionCount) throw new IOException("Truncated sparse-switch payload.");
                long size = u16(raf, instructionStart + (cursor + 1) * 2L);
                long width = 2L + size * 4L;
                if (width > instructionCount - cursor) throw new IOException("Sparse-switch payload exceeds code_item.");
                cursor += width;
                continue;
            }
            if (unit == 0x0300) {
                if (cursor + 4 > instructionCount) throw new IOException("Truncated array-data payload.");
                int elementWidth = u16(raf, instructionStart + (cursor + 1) * 2L);
                long size = u32(raf, instructionStart + (cursor + 2) * 2L);
                if (elementWidth <= 0 || size > 100000000L) throw new IOException("Invalid array-data payload size.");
                long dataUnits = (size * elementWidth + 1L) / 2L;
                long width = 4L + dataUnits;
                if (width > instructionCount - cursor) throw new IOException("Array-data payload exceeds code_item.");
                cursor += width;
                continue;
            }

            int opcode = unit & 0xff;
            int width = instructionWidth(opcode);
            if (width <= 0 || width > instructionCount - cursor)
                throw new IOException("Instruction stream is truncated at code unit " + cursor + ".");
            if ((opcode >= 0x6e && opcode <= 0x72)
                    || (opcode >= 0x74 && opcode <= 0x78)
                    || opcode == 0xfa || opcode == 0xfb) {
                int targetIndex = u16(raf, instructionStart + (cursor + 1) * 2L);
                if (targetIndex < 0 || targetIndex >= methodCount)
                    throw new IOException("Invoke method index is out of bounds: " + targetIndex);
                long target = methodOff + targetIndex * 8L;
                int targetClass = u16(raf, target);
                int targetProto = u16(raf, target + 2);
                int targetNameIndex = (int) u32(raf, target + 4);
                if (targetClass >= typeStrings.length || targetProto >= protoCount
                        || targetNameIndex < 0 || targetNameIndex >= stringOffsets.length)
                    throw new IOException("Invoke target metadata is invalid.");
                String owner = javaName(string(raf, typeStrings[targetClass], stringOffsets, strings, length));
                String name = string(raf, targetNameIndex, stringOffsets, strings, length);
                String signature = protoSignature(raf, protoOff, targetProto, typeStrings, stringOffsets, strings, length);
                String reference = caller + "  ->  " + owner + "->" + name + signature;
                if (!info.references.contains(reference)) {
                    info.references.add(reference);
                    result.totalReferences++;
                }
            }
            cursor += width;
        }
        if (cursor < instructionCount && (info.references.size() >= 2000
                || result.totalReferences >= MAX_TOTAL_REFERENCES)) {
            info.referencesTruncated = true;
            if (result.totalReferences >= MAX_TOTAL_REFERENCES) result.referenceLimitReached = true;
        }
    }

    static int instructionWidth(int opcode) {
        if (opcode == 0x18) return 5;
        if (opcode == 0xfa || opcode == 0xfb) return 4;
        if ((opcode >= 0x14 && opcode <= 0x14) || opcode == 0x17 || opcode == 0x1b
                || (opcode >= 0x24 && opcode <= 0x26) || (opcode >= 0x2a && opcode <= 0x2c)
                || (opcode >= 0x6e && opcode <= 0x72) || (opcode >= 0x74 && opcode <= 0x78)
                || opcode == 0xfc || opcode == 0xfd) return 3;
        if (opcode == 0x13 || opcode == 0x15 || opcode == 0x16 || opcode == 0x19
                || opcode == 0x1a || opcode == 0x1c || opcode == 0x1f || opcode == 0x20
                || opcode == 0x22 || opcode == 0x23 || opcode == 0x29
                || (opcode >= 0x2d && opcode <= 0x3d)
                || (opcode >= 0x44 && opcode <= 0x6d)
                || (opcode >= 0x90 && opcode <= 0xaf)
                || (opcode >= 0xd0 && opcode <= 0xe2)
                || opcode == 0xfe || opcode == 0xff) return 2;
        return 1;
    }

    private static String protoSignature(RandomAccessFile raf, long protoOff, int proto,
            int[] typeStrings, int[] stringOffsets, Map<Integer, String> strings, long length) throws Exception {
        long p = protoOff + proto * 12L;
        int returnType = (int) u32(raf, p + 4);
        long paramsOff = u32(raf, p + 8);
        if (returnType < 0 || returnType >= typeStrings.length) throw new IOException("Invalid return type index.");
        StringBuilder b = new StringBuilder("(");
        if (paramsOff != 0) {
            if (paramsOff > length - 4) throw new IOException("Parameter type list is out of bounds.");
            long sizeLong = u32(raf, paramsOff);
            if (sizeLong > 4096 || paramsOff + 4L + sizeLong * 2L > length) throw new IOException("Invalid parameter type list.");
            int size = (int) sizeLong;
            for (int i = 0; i < size; i++) {
                int t = u16(raf, paramsOff + 4L + i * 2L);
                if (t >= typeStrings.length) throw new IOException("Invalid parameter type index.");
                if (i > 0) b.append(", ");
                b.append(javaName(string(raf, typeStrings[t], stringOffsets, strings, length)));
            }
        }
        b.append("): ");
        b.append(javaName(string(raf, typeStrings[returnType], stringOffsets, strings, length)));
        return b.toString();
    }

    private static String typeDescriptor(RandomAccessFile raf, int[] typeStrings, int[] stringOffsets,
            Map<Integer, String> cache, long length, int type) throws Exception {
        return string(raf, typeStrings[type], stringOffsets, cache, length);
    }

    private static String string(RandomAccessFile raf, int index, int[] offsets,
            Map<Integer, String> cache, long length) throws Exception {
        String existing = cache.get(index);
        if (existing != null) return existing;
        if (index < 0 || index >= offsets.length) throw new IOException("String index out of bounds.");
        Cursor c = new Cursor(offsets[index]);
        uleb(raf, c, length); // UTF-16 length; byte sequence ends at NUL.
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        int n = 0;
        while (c.p < length && n++ < 65536) {
            int value = readByte(raf, c.p++);
            if (value == 0) break;
            bytes.write(value);
        }
        if (n >= 65536) throw new IOException("DEX string exceeds 64 KB.");
        String value = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        if (cache.size() < 12000) cache.put(index, value);
        return value;
    }

    private static int count(RandomAccessFile raf, long p, long length, String name) throws Exception {
        long value = u32(raf, p);
        if (value > MAX_TABLE_ITEMS) throw new IOException(name + " count exceeds safety limit.");
        return (int) value;
    }
    private static long offset(RandomAccessFile raf, long p, int count, int width, long length, String name) throws Exception {
        long off = u32(raf, p);
        if (count == 0) return off;
        if (off < 112 || off > length || (long) count * width > length - off)
            throw new IOException(name + " table is out of bounds.");
        return off;
    }
    private static int uleb(RandomAccessFile raf, Cursor c, long length) throws Exception {
        int result = 0;
        for (int i = 0; i < 5; i++) {
            if (c.p >= length) throw new IOException("Truncated ULEB128 value.");
            int b = readByte(raf, c.p++);
            if (i == 4 && (b & 0xF0) != 0) throw new IOException("ULEB128 overflow.");
            result |= (b & 0x7F) << (i * 7);
            if ((b & 0x80) == 0) return result;
        }
        throw new IOException("Invalid ULEB128 value.");
    }
    private static int readByte(RandomAccessFile r, long p) throws Exception { r.seek(p); return r.readUnsignedByte(); }
    private static int u16(RandomAccessFile r, long p) throws Exception { r.seek(p); return r.readUnsignedByte() | (r.readUnsignedByte() << 8); }
    private static long u32(RandomAccessFile r, long p) throws Exception {
        r.seek(p);
        return ((long)r.readUnsignedByte()) | ((long)r.readUnsignedByte() << 8)
                | ((long)r.readUnsignedByte() << 16) | ((long)r.readUnsignedByte() << 24);
    }
    private static boolean isDexName(String name) { return name.matches("classes([2-9]|[1-9][0-9]+)?\\.dex") || "classes.dex".equals(name); }
    private static int dexNumber(String name) {
        if ("classes.dex".equals(name)) return 1;
        try { return Integer.parseInt(name.substring(7, name.length() - 4)); }
        catch (Exception e) { return Integer.MAX_VALUE; }
    }
    private static String safeMessage(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }

    private static String javaName(String d) {
        if (d == null || d.length() == 0) return "?";
        int arrays = 0;
        while (arrays < d.length() && d.charAt(arrays) == '[') arrays++;
        String base = d.substring(arrays);
        String name;
        if (base.length() == 1) {
            switch (base.charAt(0)) {
                case 'V': name = "void"; break; case 'Z': name = "boolean"; break;
                case 'B': name = "byte"; break; case 'S': name = "short"; break;
                case 'C': name = "char"; break; case 'I': name = "int"; break;
                case 'J': name = "long"; break; case 'F': name = "float"; break;
                case 'D': name = "double"; break; default: name = base;
            }
        } else if (base.startsWith("L") && base.endsWith(";")) {
            name = base.substring(1, base.length() - 1).replace('/', '.');
        } else name = base;
        StringBuilder out = new StringBuilder(name);
        for (int i = 0; i < arrays; i++) out.append("[]");
        return out.toString();
    }
    private static String accessFlags(int f) {
        StringBuilder b = new StringBuilder();
        if ((f & 0x1) != 0) b.append("public ");
        if ((f & 0x2) != 0) b.append("private ");
        if ((f & 0x4) != 0) b.append("protected ");
        if ((f & 0x8) != 0) b.append("static ");
        if ((f & 0x10) != 0) b.append("final ");
        if ((f & 0x400) != 0) b.append("abstract ");
        if ((f & 0x200) != 0) b.append("interface ");
        if ((f & 0x1000) != 0) b.append("synthetic ");
        if ((f & 0x2000) != 0) b.append("annotation ");
        if ((f & 0x4000) != 0) b.append("enum ");
        return b.toString();
    }
    private static final class Cursor { long p; Cursor(long p) { this.p = p; } }
}
