package com.tanvir.droidinspect;

import java.io.File;
import java.nio.file.Files;
import java.util.Set;

/** Host regression tests for persistent assembly-scoped bookmarks and token validation. */
public final class AssemblyExplorerBookmarkStoreSelfTest {
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("droidinspect-assembly-bookmarks").toFile();
        try {
            String firstHash = "a".repeat(64);
            String secondHash = "b".repeat(64);
            AssemblyExplorerBookmarkStore first = new AssemblyExplorerBookmarkStore(root, firstHash);
            AssemblyExplorerBookmarkStore second = new AssemblyExplorerBookmarkStore(root, secondHash);
            first.toggle("0x06000001");
            first.toggle("0x02000002");
            require(first.read().contains("0x06000001"), "bookmark persisted");
            require(second.read().isEmpty(), "different assembly has isolated bookmarks");
            first.toggle("0x06000001");
            require(!first.read().contains("0x06000001"), "bookmark removal persisted");
            boolean rejected = false;
            try { first.toggle("../escape"); } catch (java.io.IOException expected) { rejected = true; }
            require(rejected, "invalid metadata token rejected");
            AssemblyExplorerBookmarkStore reopened = new AssemblyExplorerBookmarkStore(root, firstHash);
            require(reopened.read().contains("0x02000002"), "bookmarks survive store recreation");
            System.out.println("PASS: assembly-scoped bookmark persistence, isolation, removal and token validation");
        } finally {
            delete(root);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) delete(child);
        }
        file.delete();
    }
}
