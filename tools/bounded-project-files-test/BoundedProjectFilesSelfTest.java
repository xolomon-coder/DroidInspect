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
