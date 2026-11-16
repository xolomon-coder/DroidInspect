package com.tanvir.droidinspect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Bounded source-tree enumeration shared by the C# workspace and host regression tests. */
final class BoundedSourceTree {
    static final int MAX_SOURCE_FILES = 2000;
    static final int MAX_VISITED_NODES = 10000;
    static final int MAX_DEPTH = 16;
    static final long MAX_SOURCE_FILE_BYTES = 32L * 1024L * 1024L;
    static final long MAX_TOTAL_SOURCE_BYTES = 128L * 1024L * 1024L;

    private BoundedSourceTree() { }

    static List<File> scan(File sourceRoot) throws IOException {
        if (sourceRoot == null || !sourceRoot.isDirectory())
            throw new IOException("C# source folder is unavailable.");
        File canonicalRoot = sourceRoot.getCanonicalFile();
        ArrayList<File> result = new ArrayList<>();
        int[] visited = {0};
        long[] totalBytes = {0L};
        visit(canonicalRoot, canonicalRoot, 0, visited, totalBytes, result);
        return result;
    }

    private static void visit(File root, File directory, int depth, int[] visited,
                              long[] totalBytes, List<File> result) throws IOException {
        if (depth > MAX_DEPTH)
            throw new IOException("C# source folder nesting exceeds the " + MAX_DEPTH + " level safety limit.");
        File[] children = directory.listFiles();
        if (children == null) throw new IOException("Cannot read C# source project directory.");
        for (File child : children) {
            if (++visited[0] > MAX_VISITED_NODES)
                throw new IOException("C# source project contains too many filesystem entries.");
            File canonical = child.getCanonicalFile();
            String prefix = root.getPath() + File.separator;
            if (!canonical.getPath().startsWith(prefix))
                throw new IOException("C# source entry resolves outside the project folder.");
            if (child.isDirectory()) {
                visit(root, child, depth + 1, visited, totalBytes, result);
            } else if (child.isFile()
                    && child.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".cs")) {
                long size = child.length();
                if (size > MAX_SOURCE_FILE_BYTES)
                    throw new IOException("C# source file exceeds the 32 MB safety limit: " + child.getName());
                totalBytes[0] += size;
                if (totalBytes[0] > MAX_TOTAL_SOURCE_BYTES)
                    throw new IOException("C# source project exceeds the 128 MB total safety limit.");
                if (result.size() >= MAX_SOURCE_FILES)
                    throw new IOException("C# source project exceeds the " + MAX_SOURCE_FILES + " file safety limit.");
                result.add(child);
            }
        }
    }
}
