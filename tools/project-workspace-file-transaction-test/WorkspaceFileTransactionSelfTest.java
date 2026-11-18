package com.tanvir.droidinspect;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;

/** Host regression tests for atomic project-file replacement and cancellation-safe rollback. */
public final class WorkspaceFileTransactionSelfTest {
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("droidinspect-file-transaction").toFile();
        try {
            File history = new File(root, "history");
            File working = new File(root, "working.apk");
            File staged = new File(root, "working-incoming.tmp");
            File backup = new File(history, "before-revision-1.apk");
            require(history.mkdir(), "create history directory");
            write(working, "original-valid-apk");
            write(staged, "replacement-valid-apk");
            write(backup, "original-valid-apk");

            WorkspaceFileTransaction.replace(working, staged, backup, file -> {
                require("replacement-valid-apk".equals(read(file)), "validator receives installed replacement");
            });
            require("replacement-valid-apk".equals(read(working)), "successful transaction installs staged bytes");
            require(!staged.exists(), "successful transaction consumes staged file");
            require("original-valid-apk".equals(read(backup)), "rollback copy is preserved");

            File firstExport = new File(root, "new-export.apk");
            File firstExportStage = new File(root, "new-export.tmp");
            write(firstExportStage, "new-export-bytes");
            WorkspaceFileTransaction.installNew(firstExport, firstExportStage,
                    file -> require("new-export-bytes".equals(read(file)), "validator sees newly installed file"));
            require("new-export-bytes".equals(read(firstExport)), "new export is installed without an old destination");
            require(!firstExportStage.exists(), "successful new export consumes staging file");

            File rejectedExport = new File(root, "rejected-export.apk");
            File rejectedStage = new File(root, "rejected-export.tmp");
            write(rejectedStage, "invalid-export");
            boolean invalidNewExportRejected = false;
            try {
                WorkspaceFileTransaction.installNew(rejectedExport, rejectedStage, file -> {
                    throw new IOException("simulated new-export validation failure");
                });
            } catch (IOException expected) {
                invalidNewExportRejected = true;
            }
            require(invalidNewExportRejected && !rejectedExport.exists(),
                    "invalid new export is removed after validation failure");

            File occupiedExport = new File(root, "occupied-export.apk");
            File occupiedStage = new File(root, "occupied-export.tmp");
            write(occupiedExport, "preserve-this-export");
            write(occupiedStage, "must-not-replace");
            boolean occupiedRejected = false;
            try {
                WorkspaceFileTransaction.installNew(occupiedExport, occupiedStage, null);
            } catch (IOException expected) {
                occupiedRejected = true;
            }
            require(occupiedRejected && "preserve-this-export".equals(read(occupiedExport)),
                    "new-file installation refuses to overwrite an existing output");

            File firstExport = new File(root, "new-export.apk");
            File firstExportStage = new File(root, "new-export.tmp");
            write(firstExportStage, "new-export-bytes");
            WorkspaceFileTransaction.installNew(firstExport, firstExportStage,
                    file -> require("new-export-bytes".equals(read(file)), "validator sees newly installed file"));
            require("new-export-bytes".equals(read(firstExport)), "new export is installed without an old destination");
            require(!firstExportStage.exists(), "successful new export consumes staging file");

            File rejectedExport = new File(root, "rejected-export.apk");
            File rejectedStage = new File(root, "rejected-export.tmp");
            write(rejectedStage, "invalid-export");
            boolean invalidNewExportRejected = false;
            try {
                WorkspaceFileTransaction.installNew(rejectedExport, rejectedStage, file -> {
                    throw new IOException("simulated new-export validation failure");
                });
            } catch (IOException expected) {
                invalidNewExportRejected = true;
            }
            require(invalidNewExportRejected && !rejectedExport.exists(),
                    "invalid new export is removed after validation failure");

            File occupiedExport = new File(root, "occupied-export.apk");
            File occupiedStage = new File(root, "occupied-export.tmp");
            write(occupiedExport, "preserve-this-export");
            write(occupiedStage, "must-not-replace");
            boolean occupiedRejected = false;
            try {
                WorkspaceFileTransaction.installNew(occupiedExport, occupiedStage, null);
            } catch (IOException expected) {
                occupiedRejected = true;
            }
            require(occupiedRejected && "preserve-this-export".equals(read(occupiedExport)),
                    "new-file installation refuses to overwrite an existing output");

            WorkspaceFileTransaction.restore(working, backup);
            require("original-valid-apk".equals(read(working)), "manual recovery restores original working bytes");

            write(staged, "invalid-replacement");
            boolean rejected = false;
            try {
                WorkspaceFileTransaction.replace(working, staged, backup, file -> {
                    throw new IOException("simulated post-replacement validation failure");
                });
            } catch (IOException expected) {
                rejected = true;
            }
            require(rejected, "post-replacement validation failure is propagated");
            require("original-valid-apk".equals(read(working)),
                    "failed replacement automatically restores the last valid working file");

            write(working, "damaged-working-file");
            Thread.currentThread().interrupt();
            try {
                WorkspaceFileTransaction.restore(working, backup);
            } finally {
                Thread.interrupted();
            }
            require("original-valid-apk".equals(read(working)),
                    "rollback completes even when the worker thread is interrupted");

            File externalDirectory = new File(root, "external");
            require(externalDirectory.mkdir(), "create external staging directory");
            File externalStage = new File(externalDirectory, "external.tmp");
            write(externalStage, "external");
            boolean parentRejected = false;
            try {
                WorkspaceFileTransaction.replace(working, externalStage, backup, null);
            } catch (IOException expected) {
                parentRejected = true;
            }
            require(parentRejected, "staged file outside destination directory is rejected");
            require("original-valid-apk".equals(read(working)),
                    "rejecting an external staged file preserves the working file");

            File external = new File(externalDirectory, "outside.bin");
            write(external, "do-not-delete");
            Path linkedStage = new File(root, "linked-stage.tmp").toPath();
            try {
                Files.createSymbolicLink(linkedStage, external.toPath());
                boolean symlinkRejected = false;
                try {
                    WorkspaceFileTransaction.replace(working, linkedStage.toFile(), backup, null);
                } catch (IOException expected) {
                    symlinkRejected = true;
                }
                require(symlinkRejected, "symbolic-link staged file is rejected");
                require("do-not-delete".equals(read(external)),
                        "rejecting a symbolic link never changes its external target");
                require("original-valid-apk".equals(read(working)),
                        "symbolic-link rejection preserves the destination");
            } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
                // Some host filesystems do not permit symbolic-link creation.
            }

            File linkedBackup = new File(root, "linked-backup.apk");
            try {
                Files.createSymbolicLink(linkedBackup.toPath(), backup.toPath());
                boolean backupRejected = false;
                try {
                    WorkspaceFileTransaction.restore(working, linkedBackup);
                } catch (IOException expected) {
                    backupRejected = true;
                }
                require(backupRejected, "symbolic-link rollback copy is rejected");
            } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
                // Some host filesystems do not permit symbolic-link creation.
            }

            System.out.println("WorkspaceFileTransactionSelfTest: PASS");
        } finally {
            deleteTree(root);
        }
    }

    private static void write(File file, String value) throws IOException {
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("FAIL: " + message);
    }

    private static void deleteTree(File file) throws IOException {
        if (!Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) return;
        if (!Files.isSymbolicLink(file.toPath())
                && Files.isDirectory(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        Files.deleteIfExists(file.toPath());
    }
}
