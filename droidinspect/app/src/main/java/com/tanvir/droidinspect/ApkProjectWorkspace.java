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
    private static final long MAX_PROJECT_METADATA_BYTES = 64L * 1024L;
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
        File files = context.getFilesDir();
        if (files == null || Files.isSymbolicLink(files.toPath()) || !files.isDirectory())
            throw new IOException("Trusted application files directory is unavailable.");
        File base = new File(files, "apk-projects");
        if (Files.isSymbolicLink(base.toPath()))
            throw new IOException("APK project storage cannot be a symbolic link.");
        if (!base.exists() && !base.mkdir()) throw new IOException("Cannot create APK project storage.");
        if (!isSafeDirectoryChild(files, base))
            throw new IOException("APK project storage is outside its trusted application directory.");
        String id = "apk_" + UUID.randomUUID().toString().replace("-", "");
        File root = new File(base, id);
        if (!root.mkdir() || !isSafeDirectoryChild(base, root))
            throw new IOException("Cannot create a safe APK project folder.");
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
        } catch (Exception | Error error) {
            try {
                BoundedProjectFiles.deleteTreeForRollback(root, base, MAX_PROJECT_DIRECTORY_ENTRIES);
            } catch (IOException | RuntimeException cleanupError) {
                error.addSuppressed(cleanupError);
            }
            if (error instanceof IOException) throw (IOException) error;
            if (error instanceof Error) throw (Error) error;
            throw new IOException("APK project creation failed: " + safe(error.getMessage()), error);
        }
    }

    static ApkProjectWorkspace open(Context context, String id) throws IOException {
        if (id == null || !id.matches("apk_[A-Fa-f0-9]{32}"))
            throw new IOException("Invalid APK project identifier.");
        File files = context.getFilesDir();
        File base = new File(files, "apk-projects");
        if (!isSafeDirectoryChild(files, base))
            throw new IOException("APK project storage is unavailable or linked.");
        File root = new File(base, id);
        if (!isSafeDirectoryChild(base, root))
            throw new IOException("Saved APK project folder is unavailable or linked.");
        File marker = new File(root, ".droidinspect-apk-project");
        File metadata = new File(root, "project.properties");
        if (!isSafeFileChild(root, marker) || !isSafeFileChild(root, metadata))
            throw new IOException("Saved APK project marker or metadata is missing or linked.");
        Properties properties = loadProperties(metadata);
        String name = properties.getProperty("name", "APK project");
        long revision = parseLong(properties.getProperty("revision"), 0);
        ApkProjectWorkspace project = new ApkProjectWorkspace(context, root, id, name, revision);
        File originalDirectory = new File(root, "original");
        File workingDirectory = new File(root, "working");
        if (!isSafeDirectoryChild(root, originalDirectory)
                || !isSafeDirectoryChild(root, workingDirectory)
                || !isSafeDirectoryChild(root, project.historyDirectory)
                || !isSafeDirectoryChild(root, project.exportsDirectory)
                || !isSafeDirectoryChild(root, project.reportsDirectory)
                || !isSafeFileChild(originalDirectory, project.originalApk)
                || !isSafeFileChild(workingDirectory, project.workingApk)
                || !isSafeDirectChild(root, project.notesFile)
                || !isSafeDirectChild(root, project.bookmarksFile)
                || !isSafeDirectChild(root, project.searchesFile))
            throw new IOException("Saved APK project contains missing or unsafe workspace paths.");
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
        try {
            File files = context.getFilesDir();
            File base = new File(files, "apk-projects");
            File root = new File(base, id);
            File workingDirectory = new File(root, "working");
            return isSafeDirectoryChild(files, base) && isSafeDirectoryChild(base, root)
                    && isSafeFileChild(root, new File(root, ".droidinspect-apk-project"))
                    && isSafeDirectoryChild(root, workingDirectory)
                    && isSafeFileChild(workingDirectory, new File(workingDirectory, "working.apk"))
                    ? id : null;
        } catch (IOException unsafe) {
            return null;
        }
    }

    static List<ProjectSummary> listRecentProjects(Context context) {
        ArrayList<ProjectSummary> result = new ArrayList<>();
        File files = context.getFilesDir();
        File base = new File(files, "apk-projects");
        try {
            if (!isSafeDirectoryChild(files, base)) return result;
            for (ProjectWorkspaceIndex.Entry entry : ProjectWorkspaceIndex.scanShallow(
                    base, ".droidinspect-apk-project", "APK")) {
                File root = new File(base, entry.id);
                File marker = new File(root, ".droidinspect-apk-project");
                File metadata = new File(root, "project.properties");
                File workingDirectory = new File(root, "working");
                File working = new File(workingDirectory, "working.apk");
                if (!isSafeDirectoryChild(base, root)
                        || !isSafeFileChild(root, marker)
                        || !isSafeFileChild(root, metadata)
                        || !isSafeDirectoryChild(root, workingDirectory)
                        || !isSafeFileChild(workingDirectory, working)) continue;
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

    private static void deleteTemporaryFile(File file, Throwable primaryFailure)
            throws IOException {
        if (file == null) return;
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException | RuntimeException cleanupError) {
            IOException reported = new IOException(
                    "Could not remove temporary project file: " + file.getName(), cleanupError);
            if (primaryFailure != null) primaryFailure.addSuppressed(reported);
            else throw reported;
        }
    }

    private static void ensureSafeFile(File file) throws IOException {
        if (file == null || Files.isSymbolicLink(file.toPath()))
            throw new IOException("Project file is unavailable or is a symbolic link.");
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent == null || Files.isSymbolicLink(parent.toPath()) || !parent.isDirectory()
                || !parent.getAbsoluteFile().equals(parent.getCanonicalFile()))
            throw new IOException("Project file parent is unavailable or linked.");
        if (!parent.getCanonicalFile().equals(file.getCanonicalFile().getParentFile()))
            throw new IOException("Project file resolves outside its direct parent.");
    }

    private static boolean isSafeDirectChild(File parent, File child) throws IOException {
        if (parent == null || child == null || Files.isSymbolicLink(parent.toPath())
                || Files.isSymbolicLink(child.toPath()) || !parent.isDirectory()) return false;
        File absoluteParent = parent.getAbsoluteFile();
        File canonicalParent = parent.getCanonicalFile();
        if (!absoluteParent.equals(canonicalParent)) return false;
        return canonicalParent.equals(child.getCanonicalFile().getParentFile());
    }

    private static boolean isSafeDirectoryChild(File parent, File child) throws IOException {
        return isSafeDirectChild(parent, child) && child.isDirectory();
    }

    private static boolean isSafeFileChild(File parent, File child) throws IOException {
        return isSafeDirectChild(parent, child) && child.isFile();
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
        long previousRevision = revision;
        Properties previousMetadata = loadProperties(new File(root, "project.properties"));
        String previousOriginalHash = previousMetadata.getProperty("originalSha256", "");
        String previousWorkingHash = previousMetadata.getProperty("workingSha256", "");
        beginOperationQuietly("APK_WORKING_REPLACE", "Replace working APK from selected document");
        File incoming = new File(new File(root, "working"), "working-incoming.tmp");
        File rollbackSnapshot = null;
        Throwable primaryFailure = null;
        try {
            copyUri(context, source, incoming, MAX_APK_BYTES);
            validateApk(incoming);
            rollbackSnapshot = snapshotCurrent();
            WorkspaceFileTransaction.replace(
                    workingApk, incoming, rollbackSnapshot, ApkProjectWorkspace::validateApk);
            revision = previousRevision + 1;
            saveMetadata(revision, sha256(originalApk), sha256(workingApk));
            finishOperationQuietly("APK_WORKING_REPLACE", "revision " + revision);
        } catch (IOException | RuntimeException | Error error) {
            primaryFailure = error;
            if (rollbackSnapshot != null) {
                rollbackWorkingRevision(rollbackSnapshot, previousRevision,
                        previousOriginalHash, previousWorkingHash, "APK_WORKING_REPLACE", error);
            } else {
                finishOperationQuietly("APK_WORKING_REPLACE",
                        "failed before replacement; previous working APK was not changed");
            }
            throw error;
        } finally {
            deleteTemporaryFile(incoming, primaryFailure);
        }
    }

    synchronized void restoreRevision(File snapshot) throws IOException {
        if (snapshot == null || Files.isSymbolicLink(snapshot.toPath())
                || !isSafeDirectChild(historyDirectory, snapshot) || !snapshot.isFile())
            throw new IOException("Selected recovery revision is not a safe file inside this project's history folder.");
        long previousRevision = revision;
        Properties previousMetadata = loadProperties(new File(root, "project.properties"));
        String previousOriginalHash = previousMetadata.getProperty("originalSha256", "");
        String previousWorkingHash = previousMetadata.getProperty("workingSha256", "");
        beginOperationQuietly("APK_REVISION_RESTORE", "Restore a saved working revision");
        File incoming = new File(new File(root, "working"), "working-restore.tmp");
        File rollbackSnapshot = null;
        Throwable primaryFailure = null;
        try {
            copyFile(snapshot, incoming);
            validateApk(incoming);
            rollbackSnapshot = snapshotCurrent();
            WorkspaceFileTransaction.replace(
                    workingApk, incoming, rollbackSnapshot, ApkProjectWorkspace::validateApk);
            revision = previousRevision + 1;
            saveMetadata(revision, sha256(originalApk), sha256(workingApk));
            finishOperationQuietly("APK_REVISION_RESTORE", "revision " + revision);
        } catch (IOException | RuntimeException | Error error) {
            primaryFailure = error;
            if (rollbackSnapshot != null) {
                rollbackWorkingRevision(rollbackSnapshot, previousRevision,
                        previousOriginalHash, previousWorkingHash, "APK_REVISION_RESTORE", error);
            } else {
                finishOperationQuietly("APK_REVISION_RESTORE",
                        "failed before replacement; previous working APK was not changed");
            }
            throw error;
        } finally {
            deleteTemporaryFile(incoming, primaryFailure);
        }
    }

    private File snapshotCurrent() throws IOException {
        // Validate and bound the existing inventory before creating the next snapshot.
        List<File> files = BoundedProjectFiles.listFiles(
                historyDirectory, MAX_PROJECT_DIRECTORY_ENTRIES, ".apk");
        File snapshot = new File(historyDirectory,
                "before-revision-" + (revision + 1) + "-" + System.currentTimeMillis() + ".apk");
        copyFile(workingApk, snapshot);
        files.add(snapshot);
        Collections.sort(files, Comparator.comparingLong(File::lastModified).reversed()
                .thenComparing(File::getName));
        for (int i = MAX_HISTORY_REVISIONS; i < files.size(); i++)
            deleteTemporaryFile(files.get(i), null);
        return snapshot;
    }

    private void rollbackWorkingRevision(File snapshot, long previousRevision,
                                         String originalHash, String workingHash,
                                         String operation, Throwable primaryFailure) {
        if (snapshot == null) return;
        try {
            // Rollback deliberately ignores thread interruption so cancellation cannot
            // strand a deleted or partially copied working APK.
            WorkspaceFileTransaction.restore(workingApk, snapshot);
            revision = previousRevision;
            saveMetadata(previousRevision, originalHash, workingHash);
            recordOperationQuietly("ROLLBACK", operation + " restored revision " + previousRevision);
            finishOperationQuietly(operation, "rolled back to revision " + previousRevision);
        } catch (IOException | RuntimeException | Error rollbackFailure) {
            primaryFailure.addSuppressed(rollbackFailure);
            android.util.Log.e("DroidInspectApkProject",
                    "Could not restore the previous working APK after " + operation,
                    rollbackFailure);
        }
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
        ensureSafeFile(output);
        boolean hadPreviousOutput = output.isFile();
        File rollbackCopy = hadPreviousOutput
                ? new File(exportsDirectory, filename + ".rollback-" + UUID.randomUUID() + ".tmp")
                : null;
        boolean outputCommitted = false;
        Throwable primaryFailure = null;
        try {
            copyUri(context, source, temp, MAX_APK_BYTES);
            validateApk(temp);
            if (hadPreviousOutput) {
                // Preserve an existing project export until its replacement has been installed
                // and re-opened successfully. The backup is app-private and never user-owned.
                copyFile(output, rollbackCopy);
                WorkspaceFileTransaction.replace(
                        output, temp, rollbackCopy, ApkProjectWorkspace::validateApk);
            } else {
                WorkspaceFileTransaction.installNew(
                        output, temp, ApkProjectWorkspace::validateApk);
            }
            outputCommitted = true;
            touch();
            finishOperationQuietly("APK_EXPORT", output.getName());
            return output;
        } catch (IOException | RuntimeException | Error error) {
            primaryFailure = error;
            if (outputCommitted) {
                try {
                    if (rollbackCopy != null) {
                        WorkspaceFileTransaction.restore(output, rollbackCopy);
                    } else {
                        if (Files.isSymbolicLink(output.toPath()))
                            throw new IOException("Refusing to roll back an export through a symbolic link.");
                        Files.deleteIfExists(output.toPath());
                    }
                } catch (IOException | RuntimeException | Error rollbackFailure) {
                    error.addSuppressed(rollbackFailure);
                    android.util.Log.e("DroidInspectApkProject",
                            "Could not roll back project export after a metadata failure.", rollbackFailure);
                }
            }
            // A suppressed rollback/cleanup failure keeps the journal's STARTED marker,
            // so the next project open can request recovery. Otherwise close the failed
            // operation and report that the previous export remains the valid state.
            if (error.getSuppressed().length == 0) {
                finishOperationQuietly("APK_EXPORT",
                        "failed; previous export state retained: " + error.getClass().getSimpleName());
            }
            throw error;
        } finally {
            try { deleteTemporaryFile(temp, primaryFailure); }
            catch (IOException cleanupFailure) {
                android.util.Log.w("DroidInspectApkProject",
                        "Could not remove staged APK export: " + temp.getName(), cleanupFailure);
            }
            if (rollbackCopy != null) {
                try { deleteTemporaryFile(rollbackCopy, primaryFailure); }
                catch (IOException cleanupFailure) {
                    android.util.Log.w("DroidInspectApkProject",
                            "Could not remove temporary APK export rollback copy: " + rollbackCopy.getName(),
                            cleanupFailure);
                }
            }
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
        File temp = new File(root, "project.properties.tmp");
        if (!isSafeDirectChild(root, metadata) || !isSafeDirectChild(root, temp))
            throw new IOException("APK project metadata paths are unsafe.");
        Properties previous = metadata.isFile() ? loadProperties(metadata) : new Properties();
        String originalHash = previous.getProperty("originalSha256");
        String workingHash = previous.getProperty("workingSha256");
        if (originalHash == null) originalHash = sha256(originalApk);
        if (workingHash == null) workingHash = sha256(workingApk);
        saveMetadata(revision, originalHash, workingHash);
    }

    private void saveMetadata(long rev, String originalHash, String workingHash) throws IOException {
        File metadata = new File(root, "project.properties");
        File temp = new File(root, "project.properties.tmp");
        if (!isSafeDirectChild(root, metadata) || !isSafeDirectChild(root, temp))
            throw new IOException("APK project metadata paths are unsafe.");
        Properties previous = metadata.isFile() ? loadProperties(metadata) : new Properties();
        Properties p = new Properties();
        p.setProperty("id", id);
        p.setProperty("name", name);
        p.setProperty("created", previous.getProperty("created", String.valueOf(System.currentTimeMillis())));
        p.setProperty("updated", String.valueOf(System.currentTimeMillis()));
        p.setProperty("revision", String.valueOf(rev));
        p.setProperty("originalSha256", originalHash);
        p.setProperty("workingSha256", workingHash);
        Throwable primaryFailure = null;
        try {
            try (FileOutputStream out = new FileOutputStream(temp)) {
                p.store(out, "DroidInspect APK project");
                out.flush();
                out.getFD().sync();
            }
            try {
                Files.move(temp.toPath(), metadata.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temp.toPath(), metadata.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            revision = rev;
        } catch (IOException | RuntimeException | Error error) {
            primaryFailure = error;
            throw error;
        } finally {
            deleteTemporaryFile(temp, primaryFailure);
        }
    }

    private static void appendRecord(File file, String... values) throws IOException {
        ensureSafeFile(file);

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
        ensureSafeFile(file);

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
        ensureSafeFile(output);

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
        } catch (Exception | Error error) {
            deleteTemporaryFile(output, error);
            if (error instanceof IOException) throw (IOException) error;
            if (error instanceof Error) throw (Error) error;
            throw new IOException(error);
        }
    }

    private static void copyFile(File source, File output) throws IOException {
        ensureSafeFile(source);
        ensureSafeFile(output);

        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(output)) {
            byte[] buffer = new byte[32768];
            int read;
            while ((read = in.read(buffer)) != -1) {
                checkCancelled();
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (IOException | RuntimeException | Error error) {
            deleteTemporaryFile(output, error);
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
        ensureSafeFile(file);

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
        ensureSafeFile(file);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
        try (FileOutputStream out = new FileOutputStream(file)) { out.write((text == null ? "" : text).getBytes("UTF-8")); }
    }

    private static String readText(File file) throws IOException {
        ensureSafeFile(file);

        StringBuilder text = new StringBuilder();
        if (!file.isFile()) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line; while ((line = reader.readLine()) != null) text.append(line).append('\n');
        }
        return text.toString();
    }

    private static String sha256(File file) throws IOException {
        ensureSafeFile(file);

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
        ensureSafeFile(file);
        if (file == null || Files.isSymbolicLink(file.toPath()) || !file.isFile())
            throw new IOException("Project metadata file is unavailable or linked.");
        if (file.length() > MAX_PROJECT_METADATA_BYTES)
            throw new IOException("Project metadata exceeds the 64 KB safety limit.");
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



    /**
     * Record project operations through the shared bounded journal. Journal failures are
     * diagnostic only: they must not turn a successful APK operation into a failed one.
     * A failed STARTED/FINISHED write is logged, and an uncleared STARTED marker remains
     * visible as a recovery warning when the workspace is next opened.
     */
    private void beginOperationQuietly(String operation, String detail) {
        try {
            new ProjectWorkspaceMetadata(root).beginOperation(operation, detail);
        } catch (IOException | RuntimeException journalFailure) {
            android.util.Log.w("DroidInspectApkProject",
                    "Could not write APK project operation-start marker: " + operation,
                    journalFailure);
        }
    }

    private void finishOperationQuietly(String operation, String detail) {
        try {
            new ProjectWorkspaceMetadata(root).finishOperation(operation, detail);
        } catch (IOException | RuntimeException journalFailure) {
            android.util.Log.w("DroidInspectApkProject",
                    "Could not finish APK project operation journal: " + operation,
                    journalFailure);
        }
    }

    private void recordOperationQuietly(String operation, String detail) {
        try {
            new ProjectWorkspaceMetadata(root).recordOperation(operation, detail);
        } catch (IOException | RuntimeException journalFailure) {
            android.util.Log.w("DroidInspectApkProject",
                    "Could not record APK project operation: " + operation,
                    journalFailure);
        }
    }

}
