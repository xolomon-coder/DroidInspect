package com.tanvir.droidinspect;

import android.content.Context;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipFile;
import java.nio.file.Files;

/** Persistent private APK editing project with immutable original, working copy, exports and recovery. */
final class ApkProjectWorkspace {
    private static final long MAX_APK_BYTES = 512L * 1024L * 1024L;
    private static final int MAX_HISTORY_REVISIONS = 2;
    private static final int MAX_PROJECT_DIRECTORY_ENTRIES = 2000;
    private static final int MAX_PROJECT_REPORTS = 100;
    private static final int MAX_PROJECT_TREE_DEPTH = 4;
    private final Context context;
    private final File root;
    private final File originalApk;
    private final File workingApk;
    private final File historyDirectory;
    private final File exportsDirectory;
    private final File reportsDirectory;
    private final File notesFile;
    private final File bookmarksFile;
    private final File searchesFile;
    private final String id;
    private final String name;
    private long revision;

    static final class ProjectSummary {
        final String id, name;
        final long updated, revision, workingBytes;
        ProjectSummary(String id, String name, long updated, long revision, long workingBytes) {
            this.id=id; this.name=name; this.updated=updated; this.revision=revision; this.workingBytes=workingBytes;
        }
        @Override public String toString() {
            return name + "  •  revision " + revision + "  •  " + workingBytes + " bytes";
        }
    }

    private ApkProjectWorkspace(Context context, File root, String id, String name, long revision) {
        this.context = context.getApplicationContext();
        this.root = root;
        this.id = id;
        this.name = name;
        this.revision = revision;
        this.originalApk = new File(new File(root, "original"), "original.apk");
        this.workingApk = new File(new File(root, "working"), "working.apk");
        this.historyDirectory = new File(root, "history");
        this.exportsDirectory = new File(root, "exports");
        this.reportsDirectory = new File(root, "reports");
        this.notesFile = new File(root, "notes.md");
        this.bookmarksFile = new File(root, "bookmarks.txt");
        this.searchesFile = new File(root, "saved-searches.txt");
    }

    static ApkProjectWorkspace create(Context context, Uri source) throws IOException {
        if (source == null) throw new IOException("No APK was selected.");
        File base = new File(context.getFilesDir(), "apk-projects");
        if (!base.exists() && !base.mkdirs()) throw new IOException("Cannot create APK project storage.");
        String id = "apk_" + UUID.randomUUID().toString().replace("-", "");
        File root = new File(base, id);
        if (!root.mkdirs()) throw new IOException("Cannot create APK project folder.");
        String name = queryName(context, source);
        ApkProjectWorkspace project = new ApkProjectWorkspace(context, root, id, name, 0);
        try {
            if (!new File(root, "original").mkdirs()
                    || !new File(root, "working").mkdirs()
                    || !project.historyDirectory.mkdirs()
                    || !project.exportsDirectory.mkdirs()
                    || !project.reportsDirectory.mkdirs())
                throw new IOException("Cannot create APK project subfolders.");
            copyUri(context, source, project.originalApk, MAX_APK_BYTES);
            validateApk(project.originalApk);
            copyFile(project.originalApk, project.workingApk);
            writeText(new File(root, ".droidinspect-apk-project"), "format=1\nid=" + id + "\n");
            writeText(project.notesFile, "# " + name + "\n\n");
            writeText(project.bookmarksFile, "");
            writeText(project.searchesFile, "");
            project.saveMetadata(0, sha256(project.originalApk), sha256(project.workingApk));
            remember(context, id);
            return project;
        } catch (Exception error) {
            deleteTree(root);
            if (error instanceof IOException) throw (IOException) error;
            throw new IOException("APK project creation failed: " + safe(error.getMessage()), error);
        }
    }

    static ApkProjectWorkspace open(Context context, String id) throws IOException {
        if (id == null || !id.matches("apk_[A-Fa-f0-9]{32}"))
            throw new IOException("Invalid APK project identifier.");
        File root = new File(new File(context.getFilesDir(), "apk-projects"), id);
        File marker = new File(root, ".droidinspect-apk-project");
        File metadata = new File(root, "project.properties");
        if (!marker.isFile() || !metadata.isFile()) throw new IOException("Saved APK project marker or metadata is missing.");
        Properties properties = loadProperties(metadata);
        String name = properties.getProperty("name", "APK project");
        long revision = parseLong(properties.getProperty("revision"), 0);
        ApkProjectWorkspace project = new ApkProjectWorkspace(context, root, id, name, revision);
        if (!project.originalApk.isFile() || !project.workingApk.isFile()
                || !project.historyDirectory.isDirectory() || !project.exportsDirectory.isDirectory())
            throw new IOException("Saved APK project is incomplete; original or working APK is missing.");
        validateApk(project.originalApk);
        validateApk(project.workingApk);
        remember(context, id);
        project.touch();
        return project;
    }

    static String getLastProjectId(Context context) {
        String id = context.getSharedPreferences("apk_project_workspace", Context.MODE_PRIVATE)
                .getString("last_project_id", null);
        if (id == null || !id.matches("apk_[A-Fa-f0-9]{32}")) return null;
        File root = new File(new File(context.getFilesDir(), "apk-projects"), id);
        return new File(root, ".droidinspect-apk-project").isFile()
                && new File(new File(root, "working"), "working.apk").isFile() ? id : null;
    }

    static List<ProjectSummary> listRecentProjects(Context context) {
        ArrayList<ProjectSummary> result = new ArrayList<>();
        File base = new File(context.getFilesDir(), "apk-projects");
        try {
            for (ProjectWorkspaceIndex.Entry entry : ProjectWorkspaceIndex.scanShallow(
                    base, ".droidinspect-apk-project", "APK")) {
                File root = new File(base, entry.id);
                if (Files.isSymbolicLink(root.toPath())) continue;
            File marker = new File(root, ".droidinspect-apk-project");
            File metadata = new File(root, "project.properties");
            File working = new File(new File(root, "working"), "working.apk");
            if (!root.isDirectory() || !marker.isFile() || !metadata.isFile() || !working.isFile()) continue;
            try {
                Properties p = loadProperties(metadata);
                result.add(new ProjectSummary(p.getProperty("id", root.getName()),
                        p.getProperty("name", "APK project"),
                        parseLong(p.getProperty("updated"), entry.updated),
                        parseLong(p.getProperty("revision"), 0), working.length()));
                } catch (Exception ignored) { }
            }
        } catch (IOException ignored) {
            // A bounded index failure must not crash the project launcher.
        }
        Collections.sort(result, (a,b) -> Long.compare(b.updated, a.updated));
        return result;
    }

    private static void remember(Context context, String id) {
        context.getSharedPreferences("apk_project_workspace", Context.MODE_PRIVATE)
                .edit().putString("last_project_id", id).apply();
    }

    String getId() { return id; }
    String getName() { return name; }
    long getRevision() { return revision; }
    File getRoot() { return root; }
    File getOriginalApk() { return originalApk; }
    File getWorkingApk() { return workingApk; }
    File getExportsDirectory() { return exportsDirectory; }
    File getReportsDirectory() { return reportsDirectory; }
    File getNotesFile() { return notesFile; }

    synchronized void replaceWorkingFromUri(Uri source) throws IOException {
        beginOperationQuietly("APK_WORKING_REPLACE", "Replace working APK from selected document");
        File incoming = new File(new File(root, "working"), "working-incoming.tmp");
        try {
            copyUri(context, source, incoming, MAX_APK_BYTES);
            validateApk(incoming);
            snapshotCurrent();
            if (workingApk.exists() && !workingApk.delete())
                throw new IOException("Cannot replace the current working APK.");
            if (!incoming.renameTo(workingApk)) {
                copyFile(incoming, workingApk);
                incoming.delete();
            }
            revision++;
            saveMetadata(revision, sha256(originalApk), sha256(workingApk));
            finishOperationQuietly("APK_WORKING_REPLACE", "revision " + revision);
        } finally {
            if (incoming.exists()) incoming.delete();
        }
    }

    synchronized void restoreRevision(File snapshot) throws IOException {
        if (snapshot == null || !snapshot.isFile() || !snapshot.getCanonicalPath().startsWith(historyDirectory.getCanonicalPath() + File.separator))
            throw new IOException("Selected recovery revision is not inside this project's history folder.");
        beginOperationQuietly("APK_REVISION_RESTORE", "Restore a saved working revision");
        File incoming = new File(new File(root, "working"), "working-restore.tmp");
        try {
            copyFile(snapshot, incoming);
            validateApk(incoming);
            snapshotCurrent();
            if (workingApk.exists() && !workingApk.delete())
                throw new IOException("Cannot replace the current working APK.");
            if (!incoming.renameTo(workingApk)) {
                copyFile(incoming, workingApk);
                incoming.delete();
            }
            revision++;
            saveMetadata(revision, sha256(originalApk), sha256(workingApk));
            finishOperationQuietly("APK_REVISION_RESTORE", "revision " + revision);
        } finally {
            if (incoming.exists()) incoming.delete();
        }
    }

    private void snapshotCurrent() throws IOException {
        // Validate and bound the existing inventory before creating the next snapshot.
        List<File> files = BoundedProjectFiles.listFiles(
                historyDirectory, MAX_PROJECT_DIRECTORY_ENTRIES, ".apk");
        File snapshot = new File(historyDirectory,
                "before-revision-" + (revision + 1) + "-" + System.currentTimeMillis() + ".apk");
        copyFile(workingApk, snapshot);
        files.add(snapshot);
        Collections.sort(files, Comparator.comparingLong(File::lastModified).reversed()
                .thenComparing(File::getName));
        for (int i = MAX_HISTORY_REVISIONS; i < files.size(); i++) files.get(i).delete();
    }

    List<File> listHistory() throws IOException {
        List<File> files = BoundedProjectFiles.listFiles(historyDirectory, MAX_PROJECT_DIRECTORY_ENTRIES, ".apk");
        return new ArrayList<>(files.subList(0, Math.min(MAX_HISTORY_REVISIONS, files.size())));
    }

    synchronized File saveExportFromUri(Uri source, String requestedName) throws IOException {
        beginOperationQuietly("APK_EXPORT", requestedName);
        String filename = safeFilename(requestedName);
        if (!filename.toLowerCase(java.util.Locale.ROOT).endsWith(".apk")) filename += ".apk";
        File output = new File(exportsDirectory, filename);
        File temp = new File(exportsDirectory, filename + ".tmp");
        try {
            copyUri(context, source, temp, MAX_APK_BYTES);
            validateApk(temp);
            if (output.exists() && !output.delete()) throw new IOException("Cannot replace existing project export.");
            if (!temp.renameTo(output)) {
                copyFile(temp, output);
                temp.delete();
            }
            touch();
            finishOperationQuietly("APK_EXPORT", output.getName());
            return output;
        } finally {
            if (temp.exists()) temp.delete();
        }
    }

    void exportWorking(Uri destination) throws IOException {
        copyToUri(workingApk, destination);
    }

    void exportFile(File source, Uri destination) throws IOException {
        if (source == null || !source.isFile()) throw new IOException("Selected project export file is missing.");
        copyToUri(source, destination);
    }

    void saveNotes(String notes) throws IOException {
        writeText(notesFile, notes == null ? "" : notes);
        touch();
        recordOperationQuietly("NOTES_UPDATED", "Project notes saved");
    }

    String readNotes() throws IOException {
        return readText(notesFile);
    }

    void addBookmark(String kind, String target, String note) throws IOException {
        appendRecord(bookmarksFile, kind, target, note);
        touch();
        recordOperationQuietly("BOOKMARK_ADDED", kind + ": " + target);
    }

    void addSavedSearch(String scope, String query) throws IOException {
        appendRecord(searchesFile, scope, query, "");
        touch();
        recordOperationQuietly("SEARCH_SAVED", scope + ": " + query);
    }

    List<String> readBookmarks() throws IOException { return readRecords(bookmarksFile); }
    List<String> readSavedSearches() throws IOException { return readRecords(searchesFile); }

    File writeReport(String name, String content) throws IOException {
        String safe = safeFilename(name);
        if (!safe.toLowerCase(java.util.Locale.ROOT).endsWith(".txt")) safe += ".txt";
        File output = new File(reportsDirectory, safe);
        writeText(output, content == null ? "" : content);
        touch();
        recordOperationQuietly("REPORT_WRITTEN", output.getName());
        return output;
    }

    List<File> listReports() throws IOException {
        List<File> files = BoundedProjectFiles.listFiles(
                reportsDirectory, MAX_PROJECT_DIRECTORY_ENTRIES, null);
        return new ArrayList<>(files.subList(0, Math.min(MAX_PROJECT_REPORTS, files.size())));
    }

    List<String> listProjectTree() throws IOException {
        return BoundedProjectFiles.listTree(root, MAX_PROJECT_DIRECTORY_ENTRIES, MAX_PROJECT_TREE_DEPTH);
    }

    private void touch() throws IOException {
        File metadata = new File(root, "project.properties");
        Properties previous = metadata.isFile() ? loadProperties(metadata) : new Properties();
        String originalHash = previous.getProperty("originalSha256");
        String workingHash = previous.getProperty("workingSha256");
        if (originalHash == null) originalHash = sha256(originalApk);
        if (workingHash == null) workingHash = sha256(workingApk);
        saveMetadata(revision, originalHash, workingHash);
    }

    private void saveMetadata(long rev, String originalHash, String workingHash) throws IOException {
        File metadata = new File(root, "project.properties");
        Properties previous = metadata.isFile() ? loadProperties(metadata) : new Properties();
        Properties p = new Properties();
        p.setProperty("id", id);
        p.setProperty("name", name);
        p.setProperty("created", previous.getProperty("created", String.valueOf(System.currentTimeMillis())));
        p.setProperty("updated", String.valueOf(System.currentTimeMillis()));
        p.setProperty("revision", String.valueOf(rev));
        p.setProperty("originalSha256", originalHash);
        p.setProperty("workingSha256", workingHash);
        File temp = new File(root, "project.properties.tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) { p.store(out, "DroidInspect APK project"); }
        if (!temp.renameTo(metadata)) {
            if (metadata.exists() && !metadata.delete())
                throw new IOException("Cannot replace existing project metadata.");
            if (!temp.renameTo(metadata)) {
                copyFile(temp, metadata);
                temp.delete();
            }
        }
        revision = rev;
    }

    private static void appendRecord(File file, String... values) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file, true)) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) out.write('\t');
                String value = values[i] == null ? "" : values[i];
                out.write(Base64.encode(value.getBytes("UTF-8"), Base64.NO_WRAP));
            }
            out.write('\n');
        }
    }

    private static List<String> readRecords(File file) throws IOException {
        ArrayList<String> result = new ArrayList<>();
        if (!file.isFile()) return result;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\t", -1);
                StringBuilder decoded = new StringBuilder();
                for (String field : fields) {
                    if (decoded.length() > 0) decoded.append("  •  ");
                    try { decoded.append(new String(Base64.decode(field, Base64.DEFAULT), "UTF-8")); }
                    catch (Exception ignored) { decoded.append("(invalid record)"); }
                }
                result.add(decoded.toString());
            }
        }
        return result;
    }

    private static void copyUri(Context context, Uri uri, File output, long limit) throws IOException {
        long total = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(output)) {
            if (in == null) throw new IOException("Android could not open the selected APK.");
            byte[] buffer = new byte[32768];
            int read;
            while ((read = in.read(buffer)) != -1) {
                checkCancelled();
                total += read;
                if (total > limit) throw new IOException("APK exceeds the 512 MB project import limit.");
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (Exception error) {
            output.delete();
            if (error instanceof IOException) throw (IOException) error;
            throw new IOException(error);
        }
    }

    private static void copyFile(File source, File output) throws IOException {
        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(output)) {
            byte[] buffer = new byte[32768];
            int read;
            while ((read = in.read(buffer)) != -1) {
                checkCancelled();
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (IOException error) {
            output.delete();
            throw error;
        }
    }

    private void copyToUri(File source, Uri destination) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = context.getContentResolver().openOutputStream(destination, "w")) {
            if (out == null) throw new IOException("Document provider did not create an output stream.");
            byte[] buffer = new byte[32768];
            int read;
            while ((read = in.read(buffer)) != -1) {
                checkCancelled();
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    private static void validateApk(File file) throws IOException {
        checkCancelled();
        if (file == null || !file.isFile() || file.length() <= 0 || file.length() > MAX_APK_BYTES)
            throw new IOException("APK is empty or exceeds the 512 MB limit.");
        try (ZipFile zip = new ZipFile(file)) {
            if (zip.getEntry("AndroidManifest.xml") == null)
                throw new IOException("Selected document is not an APK with AndroidManifest.xml.");
        } catch (IOException error) {
            throw new IOException("APK archive validation failed: " + safe(error.getMessage()), error);
        }
    }

    private static String queryName(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0 && cursor.getString(column) != null) return cursor.getString(column);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String name = uri.getLastPathSegment();
        return name == null ? "APK project" : name;
    }

    private static void writeText(File file, String text) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
        try (FileOutputStream out = new FileOutputStream(file)) { out.write((text == null ? "" : text).getBytes("UTF-8")); }
    }

    private static String readText(File file) throws IOException {
        StringBuilder text = new StringBuilder();
        if (!file.isFile()) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line; while ((line = reader.readLine()) != null) text.append(line).append('\n');
        }
        return text.toString();
    }

    private static String sha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[32768]; int n;
                while ((n = in.read(buffer)) != -1) {
                    checkCancelled();
                    digest.update(buffer, 0, n);
                }
            }
            StringBuilder value = new StringBuilder();
            for (byte b : digest.digest()) value.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return value.toString();
        } catch (Exception error) { throw new IOException("Cannot calculate APK SHA-256.", error); }
    }

    private static Properties loadProperties(File file) throws IOException {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(file)) { p.load(in); }
        return p;
    }

    private static long parseLong(String value, long fallback) {
        try { return Long.parseLong(value); } catch (Exception ignored) { return fallback; }
    }

    private static String safeFilename(String name) {
        String value = name == null ? "report" : name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (value.isEmpty()) value = "report";
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("APK project operation cancelled because the workspace activity was closed.");
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        file.delete();
    }
}
