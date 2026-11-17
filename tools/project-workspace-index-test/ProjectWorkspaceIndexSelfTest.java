package com.tanvir.droidinspect;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Host tests for bounded project discovery, ordering and incomplete-project reporting. */
public final class ProjectWorkspaceIndexSelfTest {
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("droidinspect-project-index").toFile();
        try {
            File older = new File(root, "project_old");
            File newer = new File(root, "project_new");
            require(older.mkdirs() && newer.mkdirs(), "create project folders");
            Files.write(new File(older, ".droidinspect-project").toPath(),
                    "Old source\nformat=1\n".getBytes("UTF-8"));
            Files.write(new File(newer, ".droidinspect-project").toPath(),
                    "New source\nformat=1\n".getBytes("UTF-8"));
            new File(older, "src").mkdirs();
            new File(older, "original").mkdirs();
            Files.write(new File(older, "original/OriginalAssembly.bin").toPath(), new byte[]{1});
            Thread.sleep(15);
            Files.write(new File(newer, "src").toPath(), new byte[]{1});

            File external = new File(root.getParentFile(), "droidinspect-external-project");
            require(external.mkdir(), "create external project folder");
            Files.write(new File(external, ".droidinspect-project").toPath(),
                    "External project\\nformat=1\\n".getBytes("UTF-8"));
            Files.createSymbolicLink(new File(root, "linked-external-project").toPath(),
                    external.toPath());

            List<ProjectWorkspaceIndex.Entry> entries = ProjectWorkspaceIndex.scan(
                    root, ".droidinspect-project", "C# SOURCE",
                    new String[]{"src", "original/OriginalAssembly.bin"});
            require(entries.size() == 2, "both marked projects indexed and symbolic-link project excluded");
            for (ProjectWorkspaceIndex.Entry entry : entries)
                require(!"linked-external-project".equals(entry.id), "symbolic-link project is not indexed");
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

            File linkedRequiredProject = new File(root, "project_linked_required");
            require(linkedRequiredProject.mkdir(), "create project with linked required path");
            Files.write(new File(linkedRequiredProject, ".droidinspect-project").toPath(),
                    "Linked required path\\nformat=1\\n".getBytes("UTF-8"));
            File internalTarget = new File(linkedRequiredProject, "target.bin");
            Files.write(internalTarget.toPath(), new byte[]{1});
            Files.createSymbolicLink(new File(linkedRequiredProject, "src").toPath(),
                    internalTarget.toPath());
            List<ProjectWorkspaceIndex.Entry> linkedRequired = ProjectWorkspaceIndex.scan(
                    root, ".droidinspect-project", "C# SOURCE", new String[]{"src"});
            boolean linkedPathIncomplete = false;
            for (ProjectWorkspaceIndex.Entry entry : linkedRequired) {
                if ("project_linked_required".equals(entry.id))
                    linkedPathIncomplete = "INCOMPLETE".equals(entry.state);
            }
            require(linkedPathIncomplete,
                    "required project paths reject symbolic links even when targets remain inside the project");
            
            File oversizedMarkerProject = new File(root, "project_oversized_marker");
            require(oversizedMarkerProject.mkdir(), "create oversized marker project");
            File oversizedMarker = new File(oversizedMarkerProject, ".droidinspect-project");
            byte[] markerBytes = new byte[4097];
            java.util.Arrays.fill(markerBytes, (byte) 'X');
            Files.write(oversizedMarker.toPath(), markerBytes);
            List<ProjectWorkspaceIndex.Entry> markerEntries = ProjectWorkspaceIndex.scan(
                    root, ".droidinspect-project", "C# SOURCE", null);
            boolean oversizedUsesFallback = false;
            for (ProjectWorkspaceIndex.Entry entry : markerEntries) {
                if ("project_oversized_marker".equals(entry.id))
                    oversizedUsesFallback = "project_oversized_marker".equals(entry.name);
            }
            require(oversizedUsesFallback, "oversized project marker uses a safe fallback name");
            
            File[] unrelated = new File[ProjectWorkspaceIndex.MAX_SCANNED_ROOT_ENTRIES + 1];
            for (int i = 0; i < unrelated.length; i++) {
                unrelated[i] = new File(root, String.format("unrelated_%04d", i));
                require(unrelated[i].mkdir(), "create unrelated root entry " + i);
            }
            Path firstUnvisited = null;
            int position = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root.toPath())) {
                for (Path path : stream) {
                    if (position++ == ProjectWorkspaceIndex.MAX_SCANNED_ROOT_ENTRIES) {
                        firstUnvisited = path;
                        break;
                    }
                }
            }
            require(firstUnvisited != null, "identify a root entry beyond the scan budget");
            Files.write(firstUnvisited.resolve(".droidinspect-project"),
                    "Beyond scan limit\nformat=1\n".getBytes("UTF-8"));

            List<ProjectWorkspaceIndex.Entry> bounded = ProjectWorkspaceIndex.scan(
                    root, ".droidinspect-project", "C# SOURCE", null);
            boolean discoveredLateProject = false;
            for (ProjectWorkspaceIndex.Entry entry : bounded)
                if (firstUnvisited.getFileName().toString().equals(entry.id))
                    discoveredLateProject = true;
            require(!discoveredLateProject,
                    "project root beyond the 2000-entry discovery budget is not scanned");
            List<ProjectWorkspaceIndex.Entry> shallow = ProjectWorkspaceIndex.scanShallow(
                    root, ".droidinspect-project", "APK");
            boolean shallowLateProject = false;
            for (ProjectWorkspaceIndex.Entry entry : shallow)
                if (firstUnvisited.getFileName().toString().equals(entry.id)) shallowLateProject = true;
            require(!shallowLateProject,
                    "shallow APK-style project indexing also respects the root-entry budget");

            System.out.println("PASS: bounded root iteration, shallow APK-style indexing, project readiness, marker names and recency");
        } finally {
            delete(root);
            delete(new File(root.getParentFile(), "droidinspect-external-project"));
        }
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
