package com.tanvir.droidinspect;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Host regression tests for bounded C# source-tree enumeration. */
public final class BoundedSourceTreeSelfTest {
    public static void main(String[] args) throws Exception {
        File parent = Files.createTempDirectory("droidinspect-source-tree").toFile();
        try {
            File root = new File(parent, "src");
            File nested = new File(root, "Models");
            require(nested.mkdirs(), "create source folders");
            Files.write(new File(root, "Main.cs").toPath(), "class Main {}".getBytes("UTF-8"));
            Files.write(new File(nested, "Model.cs").toPath(), "class Model {}".getBytes("UTF-8"));
            List<File> files = BoundedSourceTree.scan(root);
            require(files.size() == 2, "discover root and nested source files");

            File external = new File(parent, "outside");
            require(external.mkdir(), "create external source target");
            Files.write(new File(external, "Outside.cs").toPath(), "class Outside {}".getBytes("UTF-8"));
            Path linked = new File(root, "linked-outside").toPath();
            Files.createSymbolicLink(linked, external.toPath());
            List<File> withoutLinks = BoundedSourceTree.scan(root);
            require(withoutLinks.size() == 2, "symbolic-link directories are excluded from source indexing");

            File oversized = new File(root, "Oversized.cs");
            try (RandomAccessFile out = new RandomAccessFile(oversized, "rw")) {
                out.setLength(BoundedSourceTree.MAX_SOURCE_FILE_BYTES + 1);
            }
            expectFailure(root, "per-file size limit");
            oversized.delete();

            File deep = root;
            for (int i = 0; i <= BoundedSourceTree.MAX_DEPTH; i++) {
                deep = new File(deep, "d" + i);
                require(deep.mkdir(), "create nested directory " + i);
            }
            Files.write(new File(deep, "Deep.cs").toPath(), new byte[]{1});
            expectFailure(root, "directory depth limit");

            File countRoot = new File(parent, "many-files");
            require(countRoot.mkdirs(), "create file-count fixture");
            for (int i = 0; i <= BoundedSourceTree.MAX_SOURCE_FILES; i++)
                Files.write(new File(countRoot, "File" + i + ".cs").toPath(), new byte[0]);
            expectFailure(countRoot, "source file-count limit");

            File totalRoot = new File(parent, "large-project");
            require(totalRoot.mkdirs(), "create aggregate-size fixture");
            for (int i = 0; i < 5; i++) {
                try (RandomAccessFile out = new RandomAccessFile(new File(totalRoot, "Part" + i + ".cs"), "rw")) {
                    out.setLength(BoundedSourceTree.MAX_SOURCE_FILE_BYTES);
                }
            }
            expectFailure(totalRoot, "aggregate source-size limit");

            System.out.println("PASS: bounded C# source discovery, nested files, per-file, total-size, file-count and depth limits");
        } finally {
            delete(parent);
        }
    }

    private static void expectFailure(File root, String label) throws Exception {
        boolean rejected = false;
        try { BoundedSourceTree.scan(root); }
        catch (java.io.IOException expected) { rejected = true; }
        require(rejected, label + " must reject unsafe project");
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
