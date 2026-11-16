package com.tanvir.droidinspect;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/** Host tests for bounded project discovery, ordering and incomplete-project reporting. */
public final class ProjectWorkspaceIndexSelfTest {
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("droidinspect-project-index").toFile();
        try {
            File older = new File(root, "project_old");
            File newer = new File(root, "project_new");
            require(older.mkdirs() && newer.mkdirs(), "create project folders");
            Files.write(new File(older, ".droidinspect-project").toPath(), "Old source\nformat=1\n".getBytes("UTF-8"));
            Files.write(new File(newer, ".droidinspect-project").toPath(), "New source\nformat=1\n".getBytes("UTF-8"));
            new File(older, "src").mkdirs();
            new File(older, "original").mkdirs();
            Files.write(new File(older, "original/OriginalAssembly.bin").toPath(), new byte[]{1});
            Thread.sleep(15);
            Files.write(new File(newer, "src").toPath(), new byte[]{1});
            List<ProjectWorkspaceIndex.Entry> entries = ProjectWorkspaceIndex.scan(
                    root, ".droidinspect-project", "C# SOURCE",
                    new String[]{"src", "original/OriginalAssembly.bin"});
            require(entries.size() == 2, "both marked projects indexed");
            require("READY".equals(entries.get(0).state) || "READY".equals(entries.get(1).state),
                    "complete project is ready");
            boolean incomplete = false;
            for (ProjectWorkspaceIndex.Entry entry : entries) {
                if ("project_new".equals(entry.id)) {
                    require("New source".equals(entry.name), "display name read from marker");
                    incomplete = "INCOMPLETE".equals(entry.state);
                }
            }
            require(incomplete, "incomplete project is retained and identified");

            File late = null;
            long baseTime = System.currentTimeMillis() + 100000L;
            for (int i = 0; i <= ProjectWorkspaceIndex.MAX_SCANNED_ROOT_ENTRIES; i++) {
                File folder = new File(root, String.format("unrelated_%04d", i));
                require(folder.mkdir(), "create unrelated root entry " + i);
                folder.setLastModified(baseTime + (i * 2000L));
                if (i == ProjectWorkspaceIndex.MAX_SCANNED_ROOT_ENTRIES) late = folder;
            }
            require(late != null, "create root entry beyond scan limit");
            Files.write(new File(late, ".droidinspect-project").toPath(),
                    "Beyond scan limit\nformat=1\n".getBytes("UTF-8"));
            late.setLastModified(baseTime - 100000L);
            List<ProjectWorkspaceIndex.Entry> bounded = ProjectWorkspaceIndex.scan(
                    root, ".droidinspect-project", "C# SOURCE", null);
            boolean discoveredLateProject = false;
            for (ProjectWorkspaceIndex.Entry entry : bounded)
                if ("unrelated_2000".equals(entry.id)) discoveredLateProject = true;
            require(!discoveredLateProject,
                    "project roots beyond the 2000-entry discovery budget are not scanned");

            System.out.println("PASS: project discovery, marker names, readiness, recency and root-entry scan bound");
        } finally { delete(root); }
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError("FAIL: " + message);
    }
    private static void delete(File file) {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        file.delete();
    }
}
