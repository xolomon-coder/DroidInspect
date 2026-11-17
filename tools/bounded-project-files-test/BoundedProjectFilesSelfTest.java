package com.tanvir.droidinspect;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class BoundedProjectFilesSelfTest {
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("droidinspect-bounded-files").toFile();
        try {
            File reports = new File(root, "reports");
            if (!reports.mkdir()) throw new AssertionError("reports directory");
            File old = new File(reports, "old.txt");
            File recent = new File(reports, "recent.txt");
            write(old);
            write(recent);
            old.setLastModified(1000L);
            recent.setLastModified(2000L);

            List<File> listed = BoundedProjectFiles.listFiles(reports, 10, ".txt");
            check(listed.size() == 2, "both report files listed");
            check("recent.txt".equals(listed.get(0).getName()), "newest report sorts first");

            File outside = File.createTempFile("droidinspect-outside", ".txt");
            try {
                Path link = reports.toPath().resolve("outside-link.txt");
                try {
                    Files.createSymbolicLink(link, outside.toPath());
                    check(BoundedProjectFiles.listFiles(reports, 10, null).size() == 2,
                            "symbolic links excluded from file listing");
                } catch (UnsupportedOperationException | SecurityException ignored) {
                    // Symlink creation is not available on every host.
                } catch (IOException ignored) {
                    // Filesystem may not support symlinks.
                }
            } finally {
                outside.delete();
            }

            boolean listBounded = false;
            try { BoundedProjectFiles.listFiles(reports, 1, null); }
            catch (IOException expected) { listBounded = true; }
            check(listBounded, "file listing rejects directories beyond scan budget");

            File nested = new File(root, "nested");
            if (!nested.mkdir()) throw new AssertionError("nested directory");
            write(new File(nested, "source.txt"));
            List<String> tree = BoundedProjectFiles.listTree(root, 10, 4);
            check(tree.size() >= 3, "tree includes directories and files");

            boolean treeBounded = false;
            try { BoundedProjectFiles.listTree(root, 1, 4); }
            catch (IOException expected) { treeBounded = true; }
            check(treeBounded, "tree rejects directories beyond global scan budget");

            File cleanupRoot = new File(root, "cleanup");
            File cleanupNested = new File(cleanupRoot, "nested");
            if (!cleanupNested.mkdirs()) throw new AssertionError("cleanup nested directory");
            write(new File(cleanupNested, "temporary.bin"));
            File preserved = File.createTempFile("droidinspect-preserved", ".bin");
            try {
                try {
                    Files.createSymbolicLink(cleanupRoot.toPath().resolve("outside-link"), preserved.toPath());
                } catch (UnsupportedOperationException | SecurityException ignored) {
                    // Symlink creation is not available on every host.
                } catch (IOException ignored) {
                    // Filesystem may not support symlinks.
                }
                BoundedProjectFiles.deleteTree(cleanupRoot, 10);
                check(!cleanupRoot.exists(), "bounded cleanup removes temporary workspace");
                check(preserved.exists(), "cleanup never follows a symbolic link outside the workspace");
            } finally {
                preserved.delete();
            }

            File limitedRoot = new File(root, "limited-cleanup");
            if (!limitedRoot.mkdir()) throw new AssertionError("limited cleanup directory");
            write(new File(limitedRoot, "one.bin"));
            write(new File(limitedRoot, "two.bin"));
            boolean cleanupBounded = false;
            try { BoundedProjectFiles.deleteTree(limitedRoot, 1); }
            catch (IOException expected) { cleanupBounded = true; }
            check(cleanupBounded && limitedRoot.exists(), "cleanup stops at its global entry budget");
            BoundedProjectFiles.deleteTree(limitedRoot, 10);

            File interruptedRoot = new File(root, "interrupted-cleanup");
            if (!interruptedRoot.mkdir()) throw new AssertionError("interrupted cleanup directory");
            write(new File(interruptedRoot, "temporary.bin"));
            Thread.currentThread().interrupt();
            boolean interrupted = false;
            try { BoundedProjectFiles.deleteTree(interruptedRoot, 10); }
            catch (java.io.InterruptedIOException expected) { interrupted = true; }
            finally { Thread.interrupted(); }
            check(interrupted && interruptedRoot.exists(), "cleanup honors thread interruption");
            BoundedProjectFiles.deleteTree(interruptedRoot, 10);

            System.out.println("BoundedProjectFilesSelfTest: PASS");
        } finally {
            delete(root);
        }
    }

    private static void write(File file) throws IOException {
        Files.write(file.toPath(), new byte[]{1});
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        file.delete();
    }
}
