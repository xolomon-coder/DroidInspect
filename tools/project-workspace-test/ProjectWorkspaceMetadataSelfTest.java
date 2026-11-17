package com.tanvir.droidinspect;

import java.io.File;
import java.io.IOException;
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
            require(!new File(firstRoot, "operation-in-progress.txt").exists(),
                    "recovery marker removed only after completion record");

            File reports = new File(firstRoot, "reports");
            require(reports.mkdir(), "create report listing directory");
            for (int i = 0; i < 120; i++)
                Files.write(new File(reports, String.format("report-%03d.txt", i)).toPath(), new byte[]{1});
            require(first.readOverview().contains("listing truncated by safety limits"),
                    "workspace overview truncates large directory listings");

            File corruptRoot = new File(parent, "corrupt-project");
            require(corruptRoot.mkdir(), "create corrupt metadata project");
            Files.write(new File(corruptRoot, "notes.md").toPath(), new byte[]{(byte) 0xC3, 0x28});
            expectIOException(() -> new ProjectWorkspaceMetadata(corruptRoot).readOverview(),
                    "malformed UTF-8 metadata is rejected");

            File oversizedRoot = new File(parent, "oversized-project");
            require(oversizedRoot.mkdir(), "create oversized metadata project");
            byte[] oversized = new byte[1024 * 1024 + 1];
            Files.write(new File(oversizedRoot, "notes.md").toPath(), oversized);
            expectIOException(() -> new ProjectWorkspaceMetadata(oversizedRoot).readOverview(),
                    "metadata larger than 1 MB is rejected");

            File outside = new File(parent, "outside.txt");
            Files.write(outside.toPath(), "outside".getBytes(StandardCharsets.UTF_8));
            File linkedRoot = new File(parent, "linked-project");
            require(linkedRoot.mkdir(), "create symlink metadata project");
            Files.createSymbolicLink(new File(linkedRoot, "notes.md").toPath(), outside.toPath());
            expectIOException(() -> new ProjectWorkspaceMetadata(linkedRoot).readOverview(),
                    "metadata symlink cannot escape project root");

            File internalLinkRoot = new File(parent, "internal-link-project");
            require(internalLinkRoot.mkdir(), "create in-project metadata symlink fixture");
            File internalTarget = new File(internalLinkRoot, "internal-target.txt");
            Files.write(internalTarget.toPath(), "must not be treated as notes".getBytes(StandardCharsets.UTF_8));
            Files.createSymbolicLink(new File(internalLinkRoot, "notes.md").toPath(),
                    internalTarget.toPath());
            expectIOException(() -> new ProjectWorkspaceMetadata(internalLinkRoot).readOverview(),
                    "metadata rejects symlinks even when their target is inside the project");

            File linkedWorkspaceRoot = new File(parent, "linked-workspace-root");
            Files.createSymbolicLink(linkedWorkspaceRoot.toPath(), firstRoot.toPath());
            expectIOException(() -> new ProjectWorkspaceMetadata(linkedWorkspaceRoot).readOverview(),
                    "project metadata rejects a symbolic-link workspace root");

            System.out.println("PASS: reopen, isolation, notes, bookmarks, searches, recovery, bounded listings, corrupt/oversized metadata and symlink isolation");
        } finally {
            delete(parent);
        }
    }

    private interface IoAction { void run() throws Exception; }

    private static void expectIOException(IoAction action, String message) throws Exception {
        try {
            action.run();
            throw new AssertionError("FAIL: " + message);
        } catch (IOException expected) {
            // Expected bounded-storage or path-safety rejection.
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
