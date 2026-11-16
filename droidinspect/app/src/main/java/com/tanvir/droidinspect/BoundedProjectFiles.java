package com.tanvir.droidinspect;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Bounded, symlink-safe inventory helpers for private APK project folders. */
final class BoundedProjectFiles {
    private BoundedProjectFiles() { }

    static List<File> listFiles(File directory, int maxScannedEntries, String suffix)
            throws IOException {
        if (directory == null || !directory.isDirectory() || Files.isSymbolicLink(directory.toPath()))
            throw new IOException("Project directory is unavailable.");
        ArrayList<File> result = new ArrayList<>();
        int scanned = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory.toPath())) {
            for (Path path : stream) {
                if (++scanned > maxScannedEntries)
                    throw new IOException("Project directory exceeds the " + maxScannedEntries
                            + " entry safety limit.");
                if (Files.isSymbolicLink(path) || !Files.isRegularFile(path)) continue;
                File file = path.toFile();
                if (suffix == null || file.getName().toLowerCase(Locale.ROOT)
                        .endsWith(suffix.toLowerCase(Locale.ROOT))) result.add(file);
            }
        } catch (DirectoryIteratorException error) {
            throw new IOException("Cannot read project directory.", error);
        }
        Collections.sort(result, Comparator.comparingLong(File::lastModified).reversed()
                .thenComparing(File::getName));
        return result;
    }

    static List<String> listTree(File root, int maxEntries, int maxDepth) throws IOException {
        if (root == null || !root.isDirectory() || Files.isSymbolicLink(root.toPath()))
            throw new IOException("APK project folder is unavailable.");
        File canonicalRoot = root.getCanonicalFile();
        ArrayList<String> result = new ArrayList<>();
        int[] visited = {0};
        appendTree(canonicalRoot, canonicalRoot, result, 0, maxEntries, maxDepth, visited);
        return result;
    }

    private static void appendTree(File root, File directory, List<String> result, int depth,
                                   int maxEntries, int maxDepth, int[] visited) throws IOException {
        ArrayList<File> children = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory.toPath())) {
            for (Path path : stream) {
                if (++visited[0] > maxEntries)
                    throw new IOException("APK project tree exceeds the " + maxEntries
                            + " entry safety limit.");
                if (Files.isSymbolicLink(path)) continue;
                File child = path.toFile();
                String canonical = child.getCanonicalPath();
                if (!canonical.startsWith(root.getPath() + File.separator))
                    throw new IOException("APK project entry resolves outside its project folder.");
                children.add(child);
            }
        } catch (DirectoryIteratorException error) {
            throw new IOException("Cannot read APK project tree.", error);
        }
        Collections.sort(children, Comparator.comparing(File::getName));
        for (File child : children) {
            if (".droidinspect-apk-project".equals(child.getName())) continue;
            String relative = root.toURI().relativize(child.toURI()).getPath();
            result.add(relative + (child.isDirectory() ? "/" : "  (" + child.length() + " bytes)"));
            if (child.isDirectory() && depth < maxDepth)
                appendTree(root, child, result, depth + 1, maxEntries, maxDepth, visited);
        }
    }
}
