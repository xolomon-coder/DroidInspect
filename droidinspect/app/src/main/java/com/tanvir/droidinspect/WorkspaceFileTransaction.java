package com.tanvir.droidinspect;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Safe replacement of an app-private project file with a previously captured rollback copy.
 * All rollback writes ignore the worker interrupt flag so cancellation cannot destroy the
 * last valid copy while an operation is unwinding.
 */
final class WorkspaceFileTransaction {
    interface Validator {
        void validate(File file) throws IOException;
    }

    private WorkspaceFileTransaction() { }

    static void replace(File destination, File staged, File backup, Validator validator)
            throws IOException {
        File destinationParent = requireDestination(destination, true, "destination");
        requireRegularFile(staged, "staged file");
        requireRegularFile(backup, "rollback copy");
        if (!destinationParent.equals(staged.getCanonicalFile().getParentFile()))
            throw new IOException("Staged file must be in the destination directory.");
        if (samePath(destination, staged) || samePath(destination, backup)
                || samePath(staged, backup))
            throw new IOException("Destination, staged file and rollback copy must be distinct.");

        try {
            moveReplacing(staged.toPath(), destination.toPath());
            if (validator != null) validator.validate(destination);
        } catch (IOException | RuntimeException | Error failure) {
            try {
                restore(destination, backup);
            } catch (IOException | RuntimeException | Error rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    /** Install a staged new file only when no previous destination exists. */
    static void installNew(File destination, File staged, Validator validator) throws IOException {
        File destinationParent = requireDestination(destination, false, "destination");
        requireRegularFile(staged, "staged file");
        if (!destinationParent.equals(staged.getCanonicalFile().getParentFile()))
            throw new IOException("Staged file must be in the destination directory.");
        if (Files.exists(destination.toPath(), LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Refusing to overwrite an existing project file without a rollback copy.");

        try {
            moveNew(staged.toPath(), destination.toPath());
            if (validator != null) validator.validate(destination);
        } catch (IOException | RuntimeException | Error failure) {
            try {
                if (Files.exists(destination.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                    if (Files.isSymbolicLink(destination.toPath()))
                        throw new IOException("Refusing to remove an unexpected symbolic-link destination.");
                    Files.delete(destination.toPath());
                }
            } catch (IOException | RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    static void restore(File destination, File backup) throws IOException {
        File destinationParent = requireDestination(destination, false, "destination");
        requireRegularFile(backup, "rollback copy");
        if (samePath(destination, backup))
            throw new IOException("Rollback copy must not be the destination file.");

        File temporary = new File(destinationParent,
                destination.getName() + ".rollback-" + UUID.randomUUID() + ".tmp");
        requireAbsentChild(temporary, destinationParent);
        Throwable primaryFailure = null;
        try {
            // Files.copy/move do not poll the thread interruption flag; rollback must finish.
            Files.copy(backup.toPath(), temporary.toPath());
            moveReplacing(temporary.toPath(), destination.toPath());
        } catch (IOException | RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            try {
                Files.deleteIfExists(temporary.toPath());
            } catch (IOException | RuntimeException cleanupFailure) {
                IOException reported = new IOException(
                        "Could not remove project rollback temporary file: " + temporary.getName(),
                        cleanupFailure);
                if (primaryFailure != null) primaryFailure.addSuppressed(reported);
                else throw reported;
            }
        }
    }

    private static File requireDestination(File file, boolean mustExist, String role)
            throws IOException {
        if (file == null) throw new IOException("Missing project " + role + ".");
        Path path = file.toPath();
        if (Files.isSymbolicLink(path))
            throw new IOException("Project " + role + " must not be a symbolic link.");
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent == null || Files.isSymbolicLink(parent.toPath()) || !parent.isDirectory())
            throw new IOException("Project " + role + " parent is unavailable or linked.");
        File absoluteParent = parent.getAbsoluteFile();
        File canonicalParent = parent.getCanonicalFile();
        if (!absoluteParent.equals(canonicalParent))
            throw new IOException("Project " + role + " parent resolves through a symbolic link.");
        boolean exists = Files.exists(path, LinkOption.NOFOLLOW_LINKS);
        if (mustExist && !exists)
            throw new IOException("Project " + role + " does not exist.");
        if (exists && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Project " + role + " is not a regular file.");
        if (exists && !canonicalParent.equals(file.getCanonicalFile().getParentFile()))
            throw new IOException("Project " + role + " resolves outside its direct parent.");
        return canonicalParent;
    }

    private static void requireRegularFile(File file, String role) throws IOException {
        requireDestination(file, true, role);
        if (!Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Project " + role + " is not a regular file.");
    }

    private static void requireAbsentChild(File file, File expectedParent) throws IOException {
        if (Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(file.toPath()))
            throw new IOException("Project rollback temporary path already exists.");
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent == null || !expectedParent.equals(parent.getCanonicalFile()))
            throw new IOException("Project rollback temporary path is outside its directory.");
    }

    private static boolean samePath(File first, File second) throws IOException {
        return first.getCanonicalFile().equals(second.getCanonicalFile());
    }

    private static void moveNew(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, destination);
        }
    }

    private static void moveReplacing(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
