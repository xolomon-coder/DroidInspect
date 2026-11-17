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

            File scopedRoot = new File(root, "scoped-cleanup");
            if (!scopedRoot.mkdir()) throw new AssertionError("scoped cleanup directory");
            write(new File(scopedRoot, "temporary.bin"));
            File wrongParent = new File(root, "not-the-parent");
            if (!wrongParent.mkdir()) throw new AssertionError("wrong parent fixture");
            boolean parentRejected = false;
            try { BoundedProjectFiles.deleteTree(scopedRoot, wrongParent, 10); }
            catch (IOException expected) { parentRejected = true; }
            check(parentRejected && scopedRoot.exists(),
                    "cleanup rejects a root outside its trusted direct parent");
            BoundedProjectFiles.deleteTree(scopedRoot, root, 10);
            check(!scopedRoot.exists(), "cleanup accepts the correct trusted parent");

            File rootLink = new File(root, "cleanup-root-link");
            try {
                Files.createSymbolicLink(rootLink.toPath(), reports.toPath());
                boolean rootLinkRejected = false;
                try { BoundedProjectFiles.deleteTree(rootLink, root, 10); }
                catch (IOException expected) { rootLinkRejected = true; }
                check(rootLinkRejected && reports.exists(),
                        "cleanup refuses a symbolic-link workspace root");
            } catch (UnsupportedOperationException | SecurityException ignored) {
                // Symlink creation is not available on every host.
            } catch (IOException ignored) {
                // Filesystem may not support symlinks.
            }

            File rollbackDirectory = new File(root, "rollback");
            if (!rollbackDirectory.mkdir()) throw new AssertionError("rollback directory");
            File rollbackOne = new File(rollbackDirectory, "first.dll");
            File rollbackLink = new File(rollbackDirectory, "linked.dll");
            write(rollbackOne);
            File rollbackOutside = File.createTempFile("droidinspect-rollback-outside", ".dll");
            try {
                try {
                    Files.createSymbolicLink(rollbackLink.toPath(), rollbackOutside.toPath());
                    java.io.IOException importFailure = new java.io.IOException("simulated import failure");
                    BoundedProjectFiles.rollbackDirectChildren(
                            rollbackDirectory, java.util.Arrays.asList(rollbackOne, rollbackLink), importFailure);
                    check(!rollbackOne.exists() && !Files.isSymbolicLink(rollbackLink.toPath()),
                            "rollback deletes imported files and removes a link itself");
                    check(rollbackOutside.exists(), "rollback never follows imported-file symlinks");
                } catch (UnsupportedOperationException | SecurityException ignored) {
                    // Symlink creation is not available on every host.
                } catch (IOException ignored) {
                    // Filesystem may not support symlinks.
                }
                File rollbackTwo = new File(rollbackDirectory, "second.dll");
                write(rollbackTwo);
                RuntimeException pathFailure = new RuntimeException("simulated unchecked import failure");
                BoundedProjectFiles.rollbackDirectChildren(
                        rollbackDirectory, java.util.Collections.singletonList(new File(root, "outside.dll")), pathFailure);
                check(rollbackTwo.exists() && pathFailure.getSuppressed().length == 1,
                        "rollback refuses out-of-directory files and records cleanup failure for unchecked failures");
                File rollbackThree = new File(rollbackDirectory, "third.dll");
                write(rollbackThree);
                RuntimeException runtimeFailure = new RuntimeException("simulated import runtime failure");
                BoundedProjectFiles.rollbackDirectChildren(
                        rollbackDirectory, java.util.Collections.singletonList(rollbackThree), runtimeFailure);
                check(!rollbackThree.exists() && runtimeFailure.getSuppressed().length == 0,
                        "rollback removes completed imports when the primary failure is unchecked");
            } finally {
                rollbackOutside.delete();
                BoundedProjectFiles.deleteTree(rollbackDirectory, root, 20);
            }

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


            File interruptedRollback = new File(root, "interrupted-rollback");
            File interruptedRollbackNested = new File(interruptedRollback, "nested");
            if (!interruptedRollbackNested.mkdirs())
                throw new AssertionError("interrupted rollback workspace");
            write(new File(interruptedRollbackNested, "temporary.bin"));
            File rollbackTarget = File.createTempFile("droidinspect-rollback-target", ".bin");
            try {
                try {
                    Files.createSymbolicLink(new File(interruptedRollback, "outside-link").toPath(),
                            rollbackTarget.toPath());
                } catch (UnsupportedOperationException | SecurityException ignored) {
                    // Symlink creation is not available on every host.
                } catch (IOException ignored) {
                    // Filesystem may not support symlinks.
                }
                Thread.currentThread().interrupt();
                try {
                    BoundedProjectFiles.deleteTreeForRollback(interruptedRollback, root, 10);
                } finally {
                    Thread.interrupted();
                }
                check(!interruptedRollback.exists(),
                        "rollback cleanup completes even when the worker is interrupted");
                check(rollbackTarget.exists(),
                        "interrupted rollback removes links without following their targets");
            } finally {
                rollbackTarget.delete();
            }

            File interruptedLimited = new File(root, "interrupted-limited-rollback");
            if (!interruptedLimited.mkdir()) throw new AssertionError("interrupted limited rollback");
            write(new File(interruptedLimited, "one.bin"));
            write(new File(interruptedLimited, "two.bin"));
            boolean rollbackBudgetEnforced = false;
            Thread.currentThread().interrupt();
            try {
                BoundedProjectFiles.deleteTreeForRollback(interruptedLimited, root, 1);
            } catch (IOException expected) {
                rollbackBudgetEnforced = !(expected instanceof java.io.InterruptedIOException);
            } finally {
                Thread.interrupted();
            }
            check(rollbackBudgetEnforced && interruptedLimited.exists(),
                    "rollback ignores interruption but still enforces its global entry budget");
            BoundedProjectFiles.deleteTreeForRollback(interruptedLimited, root, 10);

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
