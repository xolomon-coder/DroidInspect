package com.tanvir.droidinspect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Bounded, deterministic index for app-private project folders. */
final class ProjectWorkspaceIndex {
    private static final int MAX_PROJECTS = 500;
    static final int MAX_SCANNED_ROOT_ENTRIES = 2000;
    private static final int MAX_VISITED_FILES = 2000;

    static final class Entry {
        final String id, kind, name, state;
        final long updated;
        Entry(String id, String kind, String name, String state, long updated) {
            this.id=id; this.kind=kind; this.name=name; this.state=state; this.updated=updated;
        }
    }

    private ProjectWorkspaceIndex() { }

    static List<Entry> scan(File base, String markerName, String kind, String[] requiredPaths)
            throws IOException {
        ArrayList<Entry> entries = new ArrayList<>();
        if (base == null || !base.isDirectory()) return entries;
        File[] folders = base.listFiles();
        if (folders == null) throw new IOException("Cannot read project index: " + base.getName());
        ArrayList<File> sorted = new ArrayList<>();
        Collections.addAll(sorted, folders);
        Collections.sort(sorted, Comparator.comparingLong(File::lastModified).reversed());
        int scannedRoots = 0;
        for (File folder : sorted) {
            if (scannedRoots >= MAX_SCANNED_ROOT_ENTRIES) break;
            scannedRoots++;
            if (entries.size() >= MAX_PROJECTS) break;
            if (!folder.isDirectory()) continue;
            File marker = new File(folder, markerName);
            if (!marker.isFile()) continue;
            String name = readName(marker, folder.getName());
            boolean complete = true;
            if (requiredPaths != null) for (String relative : requiredPaths) {
                if (relative == null || relative.trim().isEmpty()) continue;
                File required = new File(folder, relative).getCanonicalFile();
                if (!required.getPath().startsWith(folder.getCanonicalPath() + File.separator)
                        || !required.exists()) complete = false;
            }
            long updated = latestModified(folder, 0, new int[]{0});
            entries.add(new Entry(folder.getName(), kind, name,
                    complete ? "READY" : "INCOMPLETE", updated));
        }
        Collections.sort(entries, (a,b) -> Long.compare(b.updated, a.updated));
        return entries;
    }

    private static String readName(File marker, String fallback) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(marker), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) return line.trim();
        } catch (Exception ignored) { }
        return fallback;
    }

    private static long latestModified(File file, int depth, int[] visited) {
        long latest = file.lastModified();
        if (!file.isDirectory() || depth >= 5 || visited[0] >= MAX_VISITED_FILES) return latest;
        File[] children = file.listFiles();
        if (children == null) return latest;
        for (File child : children) {
            if (++visited[0] >= MAX_VISITED_FILES) break;
            latest = Math.max(latest, latestModified(child, depth + 1, visited));
        }
        return latest;
    }
}
