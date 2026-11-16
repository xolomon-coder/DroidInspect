package com.tanvir.droidinspect;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;

/** Persistent metadata-token bookmarks isolated by the SHA-256 identity of one assembly. */
final class AssemblyExplorerBookmarkStore {
    private static final int MAX_BOOKMARKS = 5000;
    private final File file;

    AssemblyExplorerBookmarkStore(File filesDirectory, String assemblySha256) throws IOException {
        if (filesDirectory == null || assemblySha256 == null
                || !assemblySha256.matches("[0-9a-fA-F]{64}"))
            throw new IOException("Invalid assembly bookmark identity.");
        File directory = new File(filesDirectory, "assembly-explorer-bookmarks");
        if (!directory.exists() && !directory.mkdirs())
            throw new IOException("Cannot create assembly bookmark storage.");
        File canonicalDirectory = directory.getCanonicalFile();
        file = new File(canonicalDirectory, assemblySha256.toLowerCase(java.util.Locale.ROOT) + ".txt").getCanonicalFile();
        if (!file.getPath().startsWith(canonicalDirectory.getPath() + File.separator))
            throw new IOException("Bookmark path escaped private storage.");
    }

    synchronized Set<String> read() throws IOException {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (!file.isFile()) return result;
        if (file.length() > 128L * 1024L) throw new IOException("Bookmark file exceeds its safety limit.");
        for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
            String token = line.trim().toLowerCase(java.util.Locale.ROOT);
            if (token.matches("0x[0-9a-f]{8}")) result.add(token);
            if (result.size() >= MAX_BOOKMARKS) break;
        }
        return result;
    }

    synchronized void toggle(String token) throws IOException {
        if (token == null || !token.trim().toLowerCase(java.util.Locale.ROOT).matches("0x[0-9a-f]{8}"))
            throw new IOException("Invalid metadata token.");
        LinkedHashSet<String> values = new LinkedHashSet<>(read());
        String normalized = token.trim().toLowerCase(java.util.Locale.ROOT);
        if (values.contains(normalized)) values.remove(normalized);
        else {
            if (values.size() >= MAX_BOOKMARKS) throw new IOException("Assembly reached the 5,000 bookmark limit.");
            values.add(normalized);
        }
        StringBuilder text = new StringBuilder();
        for (String value : values) text.append(value).append('\n');
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            Files.write(temporary.toPath(), text.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (temporary.exists()) temporary.delete();
        }
    }
}
