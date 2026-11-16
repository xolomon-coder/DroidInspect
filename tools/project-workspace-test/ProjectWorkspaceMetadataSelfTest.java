package com.tanvir.droidinspect;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Host-JVM regression tests for persistent project metadata and interrupted-operation recovery. */
public final class ProjectWorkspaceMetadataSelfTest {
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("droidinspect-project-meta").toFile();
        try {
            ProjectWorkspaceMetadata store = new ProjectWorkspaceMetadata(root);
            store.appendNote("Investigate assembly identity");
            store.addBookmark("Serialize method", "0x06000001");
            store.saveSearch("Newtonsoft", "types and methods");
            store.beginOperation("REBUILD", "compile source project");
            String interrupted = store.readOverview();
            require(interrupted.contains("Investigate assembly identity"), "notes persist");
            require(interrupted.contains("Serialize method"), "bookmarks persist");
            require(interrupted.contains("Newtonsoft"), "saved searches persist");
            require(interrupted.contains("RECOVERY NOTICE"), "interrupted operation is visible");
            store.finishOperation("REBUILD", "compile failed safely");
            String recovered = store.readOverview();
            require(!recovered.contains("RECOVERY NOTICE"), "completed operation clears recovery marker");
            require(recovered.contains("FINISHED"), "operation history persists");
            File note = new File(root, "notes.md");
            String noteText = new String(Files.readAllBytes(note.toPath()), StandardCharsets.UTF_8);
            require(noteText.contains("Investigate assembly identity"), "note bytes survive reopen");
            System.out.println("PASS: notes, bookmarks, saved searches, operation history and recovery marker");
        } finally {
            delete(root);
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
