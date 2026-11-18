package com.tanvir.droidinspect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Small, bounded, crash-tolerant metadata store shared by APK and .NET workspaces. */
final class ProjectWorkspaceMetadata {
    private static final int MAX_TEXT_BYTES = 1024 * 1024;
    private static final int MAX_RECORDS = 1000;
    private static final int MAX_DIRECTORY_ENTRIES_SCANNED = 1000;
    private static final int MAX_DIRECTORY_FILES_SHOWN = 100;
    private final File root;

    ProjectWorkspaceMetadata(File projectRoot) throws IOException {
        if (projectRoot == null || Files.isSymbolicLink(projectRoot.toPath())
                || !projectRoot.isDirectory())
            throw new IOException("Project folder is unavailable or is a symbolic link.");
        this.root = projectRoot.getCanonicalFile();
        if (!root.isDirectory()) throw new IOException("Project folder does not exist.");
    }

    synchronized void appendNote(String note) throws IOException {
        append("notes.md", note, true);
    }

    synchronized void addBookmark(String label, String target) throws IOException {
        append("workspace-bookmarks.txt", clean(label) + "\t" + clean(target), false);
    }

    synchronized void saveSearch(String query, String scope) throws IOException {
        append("workspace-saved-searches.txt", clean(scope) + "\t" + clean(query), false);
    }

    synchronized void recordOperation(String operation, String detail) throws IOException {
        append("operation-history.log", System.currentTimeMillis() + "\t"
                + clean(operation) + "\t" + clean(detail), false);
    }

    synchronized void beginOperation(String operation, String detail) throws IOException {
        writeAtomic("operation-in-progress.txt", System.currentTimeMillis() + "\n"
                + clean(operation) + "\n" + clean(detail) + "\n");
        recordOperation("STARTED", operation + ": " + detail);
    }

    synchronized void finishOperation(String operation, String detail) throws IOException {
        recordOperation("FINISHED", operation + ": " + detail);
        File marker = child("operation-in-progress.txt");
        if (marker.exists() && !marker.delete()) throw new IOException("Cannot clear operation recovery marker.");
    }

    synchronized String readOverview() throws IOException {
        StringBuilder out = new StringBuilder();
        out.append("PROJECT METADATA\n");
        out.append("Folder: ").append(root.getName()).append('\n');
        appendFile(out, "NOTES", "notes.md");
        appendFile(out, "WORKSPACE BOOKMARKS", "workspace-bookmarks.txt");
        appendFile(out, "WORKSPACE SAVED SEARCHES", "workspace-saved-searches.txt");
        appendFile(out, "OPERATION HISTORY", "operation-history.log");
        File pending = child("operation-in-progress.txt");
        if (pending.isFile()) {
            out.append("\nRECOVERY NOTICE\nAn operation was interrupted. Review the last operation before continuing.\n");
            appendFile(out, "INTERRUPTED OPERATION", "operation-in-progress.txt");
        }
        appendDirectory(out, "REPORTS", "reports");
        appendDirectory(out, "EXPORTS", "exports");
        appendDirectory(out, "BUILD OUTPUTS", "build");
        appendRootFiles(out);
        return out.toString();
    }

    private void append(String filename, String value, boolean markdown) throws IOException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) throw new IOException("Entry cannot be empty.");
        String line = markdown ? (text + "\n\n") : (text.replace('\n', ' ') + "\n");
        File file = child(filename);
        String existing = readBounded(file);
        int records = existing.isEmpty() ? 0 : existing.split("\\n").length;
        if (records >= MAX_RECORDS) throw new IOException("Project metadata reached its " + MAX_RECORDS + " entry limit.");
        writeAtomic(filename, existing + line);
    }

    private void appendFile(StringBuilder out, String heading, String filename) throws IOException {
        String value = readBounded(child(filename));
        out.append("\n").append(heading).append("\n");
        out.append(value.isEmpty() ? "(none)\n" : value);
    }

    private void appendDirectory(StringBuilder out, String heading, String directoryName)
            throws IOException {
        File directory = child(directoryName);
        out.append("\n").append(heading).append("\n");
        if (!directory.isDirectory()) { out.append("(none)\n"); return; }
        appendDirectoryEntries(out, directory);
    }

    private void appendRootFiles(StringBuilder out) throws IOException {
        out.append("\nPROJECT-ROOT REPORTS AND FILES\n");
        appendDirectoryEntries(out, root);
    }

    private void appendDirectoryEntries(StringBuilder out, File directory) throws IOException {
        int scanned = 0;
        int shown = 0;
        boolean truncated = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory.toPath())) {
            for (Path path : stream) {
                if (scanned++ >= MAX_DIRECTORY_ENTRIES_SCANNED) {
                    truncated = true;
                    break;
                }
                if (Files.isSymbolicLink(path)) continue;
                File file = path.toFile();
                if (!file.isFile()) continue;
                if (directory.equals(root) && (file.getName().startsWith(".droidinspect")
                        || file.getName().endsWith(".tmp"))) continue;
                if (shown >= MAX_DIRECTORY_FILES_SHOWN) {
                    truncated = true;
                    break;
                }
                out.append(file.getName()).append("  •  ").append(file.length()).append(" bytes\n");
                shown++;
            }
        } catch (DirectoryIteratorException error) {
            throw new IOException("Cannot list project files in " + directory.getName(), error);
        }
        if (shown == 0) out.append("(none)\n");
        if (truncated) out.append("…listing truncated by safety limits\n");
    }

    private String readBounded(File file) throws IOException {
        if (!file.isFile()) return "";
        if (file.length() > MAX_TEXT_BYTES) throw new IOException(file.getName() + " exceeds the 1 MB metadata limit.");
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > MAX_TEXT_BYTES)
                    throw new IOException(file.getName() + " exceeds the 1 MB metadata limit.");
                output.write(buffer, 0, count);
            }
            try {
                return StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(output.toByteArray())).toString();
            } catch (CharacterCodingException corrupt) {
                throw new IOException(file.getName() + " contains invalid UTF-8 metadata.", corrupt);
            }
        }
    }

    private void writeAtomic(String filename, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_TEXT_BYTES) throw new IOException("Project metadata exceeds the 1 MB safety limit.");
        File target = child(filename);
        File temporary = child(filename + ".tmp-" + UUID.randomUUID());
        Throwable primaryFailure = null;
        try {
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(bytes);
                output.flush();
                output.getFD().sync();
            }
            try {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            try {
                Files.deleteIfExists(temporary.toPath());
            } catch (IOException | RuntimeException cleanupError) {
                IOException reported = new IOException("Could not remove temporary project metadata file: "
                        + temporary.getName(), cleanupError);
                if (primaryFailure != null) primaryFailure.addSuppressed(reported);
                else throw reported;
            }
        }
    }

    private File child(String name) throws IOException {
        if (name == null || name.isEmpty() || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0
                || Files.isSymbolicLink(root.toPath()) || !root.isDirectory())
            throw new IOException("Metadata path or project folder is unsafe.");
        File candidate = new File(root, name);
        if (Files.isSymbolicLink(candidate.toPath()))
            throw new IOException("Metadata file is a symbolic link: " + name);
        File file = candidate.getCanonicalFile();
        if (!root.equals(file.getParentFile()))
            throw new IOException("Metadata path escaped the project folder.");
        return file;
    }

    private static String clean(String value) {
        if (value == null) return "";
        return value.replace('\\', '/').replace('\r', ' ').replace('\n', ' ')
                .replace('\t', ' ').trim();
    }
}
