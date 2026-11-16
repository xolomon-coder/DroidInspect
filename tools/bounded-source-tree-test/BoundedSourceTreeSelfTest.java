package com.tanvir.droidinspect;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
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

            System.out.println("PASS: bounded C# source discovery, nested files, file-size and depth limits");
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
