package com.tanvir.droidinspect;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;

/** Bounded PE/CLI embedded-resource extractor; no source assembly is ever opened for writing. */
final class DotNetResourceExtractor {
    private static final long MAX_RESOURCE_BYTES = 128L * 1024L * 1024L;

    private DotNetResourceExtractor() { }

    static byte[] extractEmbeddedResource(File file, long resourceOffset) throws Exception {
        if (file == null || !file.isFile()) throw new IllegalArgumentException("Assembly file is unavailable.");
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            if (length < 64 || u16(raf, 0) != 0x5A4D) throw new IOException("Missing PE DOS signature.");
            long pe = u32(raf, 0x3C);
            if (!bounds(pe, 24, length) || u32(raf, pe) != 0x00004550L)
                throw new IOException("Invalid PE signature.");
            int sections = u16(raf, pe + 6);
            int optionalSize = u16(raf, pe + 20);
            long optional = pe + 24;
            if (!bounds(optional, optionalSize, length)) throw new IOException("Truncated PE optional header.");
            int magic = u16(raf, optional);
            int directoryOffset = magic == 0x10B ? 96 : magic == 0x20B ? 112 : -1;
            if (directoryOffset < 0 || optionalSize < directoryOffset + 15 * 8)
                throw new IOException("Unsupported or truncated PE optional header.");
            long sectionTable = optional + optionalSize;
            if (!bounds(sectionTable, (long) sections * 40L, length))
                throw new IOException("Truncated PE section table.");

            long cliRva = u32(raf, optional + directoryOffset + 14L * 8);
            long cliSize = u32(raf, optional + directoryOffset + 14L * 8 + 4);
            if (cliRva == 0 || cliSize < 32) throw new IOException("No complete .NET CLI directory.");
            long cliOffset = mapRange(raf, cliRva, 32, sectionTable, sections, length);
            if (cliOffset < 0) throw new IOException("Invalid .NET CLI header mapping.");

            long resourcesRva = u32(raf, cliOffset + 24);
            long resourcesSize = u32(raf, cliOffset + 28);
            if (resourcesRva == 0 || resourcesSize < 4)
                throw new IOException("The assembly has no embedded managed resource directory.");
            if (resourceOffset < 0 || resourceOffset > resourcesSize - 4)
                throw new IOException("Manifest resource offset is outside the managed resource directory.");

            long resourceBase = mapRange(raf, resourcesRva, resourcesSize, sectionTable, sections, length);
            if (resourceBase < 0) throw new IOException("Managed resource directory is outside a file-backed PE section.");
            long dataLength = u32(raf, resourceBase + resourceOffset);
            if (dataLength > MAX_RESOURCE_BYTES)
                throw new IOException("Resource exceeds the 128 MB extraction limit.");
            if (dataLength > resourcesSize - resourceOffset - 4
                    || !bounds(resourceBase + resourceOffset + 4, dataLength, length))
                throw new IOException("Embedded resource data is truncated or outside its directory.");
            byte[] data = new byte[(int) dataLength];
            raf.seek(resourceBase + resourceOffset + 4);
            raf.readFully(data);
            return data;
        }
    }

    private static long mapRange(RandomAccessFile raf, long rva, long size,
                                 long sectionTable, int sections, long fileLength) throws Exception {
        if (rva < 0 || size < 0) return -1;
        for (int i = 0; i < sections && i < 96; i++) {
            long p = sectionTable + i * 40L;
            long virtualSize = u32(raf, p + 8);
            long virtualAddress = u32(raf, p + 12);
            long rawSize = u32(raf, p + 16);
            long rawPointer = u32(raf, p + 20);
            if (rva < virtualAddress) continue;
            long delta = rva - virtualAddress;
            if (delta > rawSize || size > rawSize - delta) continue;
            long offset = rawPointer + delta;
            if (bounds(offset, size, fileLength)) return offset;
        }
        return -1;
    }

    private static boolean bounds(long offset, long size, long length) {
        return offset >= 0 && size >= 0 && offset <= length && size <= length - offset;
    }

    private static int u16(RandomAccessFile raf, long offset) throws Exception {
        raf.seek(offset);
        return raf.readUnsignedByte() | (raf.readUnsignedByte() << 8);
    }

    private static long u32(RandomAccessFile raf, long offset) throws Exception {
        return u16(raf, offset) | ((long) u16(raf, offset + 2) << 16);
    }
}
