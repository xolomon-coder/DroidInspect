package com.tanvir.droidinspect;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Private offline dependency-resolution project. User-supplied files never leave the device. */
final class DotNetDependencyWorkspace {
    private static final long MAX_ROOT_ASSEMBLY = 512L * 1024L * 1024L;
    private static final long MAX_DEPENDENCY_FILE = 128L * 1024L * 1024L;
    private static final long MAX_IMPORT_BATCH = 512L * 1024L * 1024L;
    private static final int MAX_DEPENDENCY_FILES = 2000;

    private final File root;
    private final File dependencies;
    private final File rootAssembly;
    private final String id;
    private final String displayName;

    private DotNetDependencyWorkspace(File root, String id, String displayName) {
        this.root = root;
        this.id = id;
        this.displayName = displayName;
        this.rootAssembly = new File(root, "OriginalAssembly.bin");
        this.dependencies = new File(root, "local-dependencies");
    }

    static DotNetDependencyWorkspace create(Context context, Uri selectedAssembly) throws IOException {
        if (selectedAssembly == null) throw new IOException("No root assembly was selected.");
        File base = new File(context.getFilesDir(), "dotnet-dependency-projects");
        if (Files.isSymbolicLink(base.toPath())
                || (!base.exists() && !base.mkdirs()) || !base.isDirectory())
            throw new IOException("Cannot create dependency project storage.");
        String id = "dependency_" + UUID.randomUUID().toString().replace("-", "");
        File root = new File(base, id);
        try {
            if (!root.mkdir()) throw new IOException("Cannot create dependency project.");
            String name = queryName(context, selectedAssembly);
            DotNetDependencyWorkspace project = new DotNetDependencyWorkspace(root, id, name);
            if (!project.dependencies.mkdir())
                throw new IOException("Cannot create local dependency folder.");
            copyDocument(context, selectedAssembly, project.rootAssembly, MAX_ROOT_ASSEMBLY);
            write(new File(root, ".droidinspect-dependency-project"), name + "\nformat=1\n");
            remember(context, id);
            return project;
        } catch (IOException error) {
            cleanupFailedCreation(root, base, error);
            throw error;
        } catch (RuntimeException error) {
            cleanupFailedCreation(root, base, error);
            throw error;
        }
    }

    static DotNetDependencyWorkspace open(Context context, String id) throws IOException {
        if (id == null || !id.matches("dependency_[A-Fa-f0-9]{32}"))
            throw new IOException("Invalid dependency project identifier.");
        File base = new File(context.getFilesDir(), "dotnet-dependency-projects");
        File root = new File(base, id);
        File marker = new File(root, ".droidinspect-dependency-project");
        File assembly = new File(root, "OriginalAssembly.bin");
        File deps = new File(root, "local-dependencies");
        if (!isSafeProjectLayout(base, root))
            throw new IOException("Saved dependency project is incomplete, linked, or outside its safety boundary.");
        String name = "Dependency project";
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new FileInputStream(marker), "UTF-8"))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) name = line.trim();
        }
        remember(context, id);
        return new DotNetDependencyWorkspace(root, id, name);
    }

    static String getLastProjectId(Context context) {
        String id = context.getSharedPreferences("dotnet_dependency_workspace", Context.MODE_PRIVATE)
                .getString("last_project_id", null);
        if (id == null || !id.matches("dependency_[A-Fa-f0-9]{32}")) return null;
        File base = new File(context.getFilesDir(), "dotnet-dependency-projects");
        File root = new File(base, id);
        try {
            return isSafeProjectLayout(base, root) ? id : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static void remember(Context context, String id) {
        context.getSharedPreferences("dotnet_dependency_workspace", Context.MODE_PRIVATE)
                .edit().putString("last_project_id", id).apply();
    }

    String getId() { return id; }
    String getDisplayName() { return displayName; }
    File getRootAssembly() { return rootAssembly; }
    File getDependencyDirectory() { return dependencies; }
    File getReportFile() { return new File(root, "Dependency-Resolution-Report.txt"); }

    List<File> listDependencies() throws IOException {
        if (!isDirectChild(root, dependencies) || !dependencies.isDirectory())
            throw new IOException("Local dependency folder is unavailable or linked.");
        ArrayList<File> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dependencies.toPath())) {
            for (Path path : stream) {
                if (Files.isSymbolicLink(path)) continue;
                File file = path.toFile();
                if (!file.isFile()) continue;
                if (result.size() >= MAX_DEPENDENCY_FILES)
                    throw new IOException("Local dependency project exceeds the " + MAX_DEPENDENCY_FILES + " file safety limit.");
                result.add(file);
            }
        } catch (DirectoryIteratorException error) {
            throw new IOException("Cannot list local dependency files.", error);
        }
        Collections.sort(result, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return result;
    }

    List<File> importDependencies(Context context, List<Uri> uris) throws IOException {
        ArrayList<File> imported = new ArrayList<>();
        int existingCount = listDependencies().size();
        long batchBytes = 0;
        try {
            for (Uri uri : uris) {
                if (uri == null) continue;
                String name = safeName(queryName(context, uri));
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (!lower.endsWith(".dll") && !lower.endsWith(".exe")) continue;
                if (existingCount + imported.size() >= MAX_DEPENDENCY_FILES)
                    throw new IOException("Local dependency project exceeds the " + MAX_DEPENDENCY_FILES + " file safety limit.");
                File target = uniqueTarget(name);
                long copied = copyDocument(context, uri, target, MAX_DEPENDENCY_FILE);
                imported.add(target);
                batchBytes += copied;
                if (batchBytes > MAX_IMPORT_BATCH)
                    throw new IOException("Dependency import batch exceeds the 512 MB safety limit.");
            }
            if (imported.isEmpty()) throw new IOException("No .dll or .exe dependency files were selected.");
            if (getReportFile().exists() && !getReportFile().delete())
                throw new IOException("Cannot invalidate the previous dependency report.");
            return imported;
        } catch (IOException error) {
            BoundedProjectFiles.rollbackDirectChildren(dependencies, imported, error);
            throw error;
        }
    }

    void saveReport(String report) throws IOException {
        write(getReportFile(), report == null ? "" : report);
    }

    private File uniqueTarget(String name) {
        File target = new File(dependencies, name);
        int suffix = 2;
        while (target.exists()) {
            int dot = name.lastIndexOf('.');
            if (dot < 1) dot = name.length();
            target = new File(dependencies, name.substring(0, dot) + "_" + suffix++ + name.substring(dot));
        }
        return target;
    }

    private static long copyDocument(Context context, Uri uri, File target, long limit) throws IOException {
        File temporary = new File(target.getParentFile(), target.getName() + ".tmp-" + UUID.randomUUID());
        long total = 0;
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(temporary)) {
            if (input == null) throw new IOException("Cannot open selected document: " + target.getName());
            byte[] buffer = new byte[32768];
            int count;
            while ((count = input.read(buffer)) != -1) {
                checkCancelled();
                total += count;
                if (total > limit) throw new IOException("File exceeds the " + (limit / (1024 * 1024)) + " MB safety limit.");
                output.write(buffer, 0, count);
            }
            output.flush();
            output.getFD().sync();
        } catch (IOException error) {
            try {
                Files.deleteIfExists(temporary.toPath());
            } catch (IOException cleanupError) {
                error.addSuppressed(cleanupError);
            }
            throw error;
        }
        if (total == 0 || !temporary.renameTo(target)) {
            IOException failure = new IOException(
                    total == 0 ? "Selected file is empty." : "Cannot activate copied file.");
            try {
                Files.deleteIfExists(temporary.toPath());
            } catch (IOException cleanupError) {
                failure.addSuppressed(cleanupError);
            }
            throw failure;
        }
        return total;
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("Dependency project import cancelled because its activity was closed.");
    }

    private static String queryName(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.trim().isEmpty()) return value;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String segment = uri.getLastPathSegment();
        return segment == null ? "dependency.dll" : segment;
    }

    private static String safeName(String value) {
        String name = value == null ? "dependency.dll" : value.replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) name = "dependency.dll";
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }

    private static void write(File target, String value) throws IOException {
        File temporary = new File(target.getParentFile(), target.getName() + ".tmp-" + UUID.randomUUID());
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.flush();
            output.getFD().sync();
        }
        if (!temporary.renameTo(target)) {
            temporary.delete();
            throw new IOException("Cannot save dependency project metadata.");
        }
    }

    private static boolean isSafeProjectLayout(File base, File root) throws IOException {
        File marker = new File(root, ".droidinspect-dependency-project");
        File assembly = new File(root, "OriginalAssembly.bin");
        File deps = new File(root, "local-dependencies");
        return !Files.isSymbolicLink(base.toPath()) && base.isDirectory()
                && isDirectChild(base, root) && root.isDirectory()
                && isDirectChild(root, marker) && marker.isFile()
                && isDirectChild(root, assembly) && assembly.isFile()
                && assembly.length() > 0 && assembly.length() <= MAX_ROOT_ASSEMBLY
                && isDirectChild(root, deps) && deps.isDirectory();
    }

    private static boolean isDirectChild(File parent, File child) throws IOException {
        if (parent == null || child == null || Files.isSymbolicLink(parent.toPath())
                || Files.isSymbolicLink(child.toPath())) return false;
        return parent.getCanonicalFile().equals(child.getCanonicalFile().getParentFile());
    }

    private static void cleanupFailedCreation(File root, File expectedParent, Throwable failure) {
        try {
            BoundedProjectFiles.deleteTreeForRollback(root, expectedParent, 10000);
        } catch (IOException cleanupError) {
            failure.addSuppressed(cleanupError);
        }
    }
}
