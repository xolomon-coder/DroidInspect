package com.tanvir.droidinspect;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Host-JVM regression tests for project isolation, persistence and interrupted-operation recovery. */
public final class ProjectWorkspaceMetadataSelfTest {
    public static void main(String[] args) throws Exception {
        File parent = Files.createTempDirectory("droidinspect-project-meta").toFile();
        File firstRoot = new File(parent, "project-one");
        File secondRoot = new File(parent, "project-two");
        require(firstRoot.mkdirs() && secondRoot.mkdirs(), "create isolated project roots");
        try {
            ProjectWorkspaceMetadata first = new ProjectWorkspaceMetadata(firstRoot);
            ProjectWorkspaceMetadata second = new ProjectWorkspaceMetadata(secondRoot);

            first.appendNote("Investigate assembly identity");
            first.addBookmark("Serialize method", "0x06000001");
            first.saveSearch("Newtonsoft", "types and methods");
            first.beginOperation("REBUILD", "compile source project");

            String interrupted = new ProjectWorkspaceMetadata(firstRoot).readOverview();
            require(interrupted.contains("Investigate assembly identity"), "notes survive store recreation");
            require(interrupted.contains("Serialize method"), "bookmarks survive store recreation");
            require(interrupted.contains("Newtonsoft"), "saved searches survive store recreation");
            require(interrupted.contains("RECOVERY NOTICE"), "interrupted operation survives store recreation");
            require(interrupted.contains("STARTED"), "operation start is journaled");

            String isolated = second.readOverview();
            require(!isolated.contains("Investigate assembly identity"), "notes are project-isolated");
            require(!isolated.contains("Serialize method"), "bookmarks are project-isolated");
            require(!isolated.contains("Newtonsoft"), "saved searches are project-isolated");
            require(!isolated.contains("RECOVERY NOTICE"), "recovery markers are project-isolated");

            new ProjectWorkspaceMetadata(firstRoot).finishOperation("REBUILD", "compile failed safely");
            String recovered = new ProjectWorkspaceMetadata(firstRoot).readOverview();
            require(!recovered.contains("RECOVERY NOTICE"), "completed operation clears recovery marker");
            require(recovered.contains("FINISHED"), "completion is journaled before marker removal");

            File note = new File(firstRoot, "notes.md");
            String noteText = new String(Files.readAllBytes(note.toPath()), StandardCharsets.UTF_8);
            require(noteText.contains("Investigate assembly identity"), "note bytes survive reopen");

            File marker = new File(firstRoot, "operation-in-progress.txt");
            require(!marker.exists(), "recovery marker removed only after completion record");

            System.out.println("PASS: metadata reopen, project isolation, notes, bookmarks, searches and recovery journal");
        } finally {
            delete(parent);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("FAIL: " + message);
    }

    private static void delete(File file) {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        file.delete();
    }
}
