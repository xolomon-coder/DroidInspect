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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Persistent, app-private C# source project. Original decompilation is kept separately. */
final class CSharpSourceWorkspace {
    private static final long MAX_SOURCE_FILE = 32L * 1024L * 1024L;
    private static final long MAX_PROJECT_IMPORT = 128L * 1024L * 1024L;
    private static final long MAX_ORIGINAL_ASSEMBLY = 512L * 1024L * 1024L;
    private final File root;
    private final File sourceRoot;
    private final File originalRoot;
    private final String id;
    private final String displayName;

    private CSharpSourceWorkspace(File root, String id, String displayName) {
        this.root = root;
        this.sourceRoot = new File(root, "src");
        this.originalRoot = new File(root, "original");
        this.id = id;
        this.displayName = displayName;
    }

    static CSharpSourceWorkspace create(Context context, String name, String decompiledSource, Uri originalAssemblyUri) throws IOException {
        File base = new File(context.getFilesDir(), "csharp-projects");
        if (Files.isSymbolicLink(base.toPath())
                || (!base.exists() && !base.mkdirs()) || !base.isDirectory())
            throw new IOException("Cannot create source project storage.");
        String id = "project_" + UUID.randomUUID().toString().replace("-", "");
        File root = new File(base, id);
        File src = new File(root, "src");
        File original = new File(root, "original");
        String safeDisplay = name == null || name.trim().isEmpty() ? "Decompiled assembly" : name.trim();
        CSharpSourceWorkspace project = new CSharpSourceWorkspace(root, id, safeDisplay);
        try {
            if (!root.mkdir()) throw new IOException("Cannot create source project folder.");
            if (!src.mkdir() || !original.mkdir())
                throw new IOException("Cannot create source project folders.");
            project.write(new File(original, "Decompiled.cs"), decompiledSource == null ? "" : decompiledSource);
            project.write(new File(src, "Decompiled.cs"), decompiledSource == null ? "" : decompiledSource);
            if (originalAssemblyUri == null) throw new IOException("Original assembly URI is unavailable; a safe rebuild project cannot be created.");
            copyOriginalAssembly(context, originalAssemblyUri, new File(original, "OriginalAssembly.bin"));
            project.write(new File(root, ".droidinspect-project"), safeDisplay + "\nformat=1\n");
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

    static CSharpSourceWorkspace open(Context context, String id) throws IOException {
        if (id == null || !id.matches("project_[A-Fa-f0-9]{32}"))
            throw new IOException("Invalid source project identifier.");
        File base = new File(context.getFilesDir(), "csharp-projects");
        File root = new File(base, id);
        File marker = new File(root, ".droidinspect-project");
        File source = new File(root, "src");
        File original = new File(root, "original");
        File originalAssembly = new File(original, "OriginalAssembly.bin");
        if (!isSafeProjectLayout(base, root))
            throw new IOException("Saved C# source project is incomplete, linked, or outside its safety boundary.");
        String name = "C# source project";
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new FileInputStream(marker), "UTF-8"))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) name = line.trim();
        }
        remember(context, id);
        return new CSharpSourceWorkspace(root, id, name);
    }

    static String getLastProjectId(Context context) {
        String id = context.getSharedPreferences("csharp_source_workspace", Context.MODE_PRIVATE)
                .getString("last_project_id", null);
        if (id == null || !id.matches("project_[A-Fa-f0-9]{32}")) return null;
        File base = new File(context.getFilesDir(), "csharp-projects");
        File root = new File(base, id);
        try {
            return isSafeProjectLayout(base, root) ? id : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static void remember(Context context, String id) {
        context.getSharedPreferences("csharp_source_workspace", Context.MODE_PRIVATE)
                .edit().putString("last_project_id", id).apply();
    }

    String getId() { return id; }
    String getDisplayName() { return displayName; }
    File getSourceRoot() { return sourceRoot; }
    File getOriginalAssemblyFile() { return new File(originalRoot, "OriginalAssembly.bin"); }
    File getBuildOutputFile() throws IOException {
        File build = new File(root, "build");
        if (Files.isSymbolicLink(build.toPath()) || !isDirectChild(root, build))
            throw new IOException("Build output directory is outside the source project.");
        if (!build.exists() && !build.mkdirs()) throw new IOException("Cannot create build output directory.");
        if (!build.isDirectory() || !isDirectChild(root, build))
            throw new IOException("Build output directory is unavailable or linked.");
        return new File(build, "DroidInspect.Rebuilt.dll");
    }

    List<File> listSourceFiles() throws IOException {
        ArrayList<File> files = new ArrayList<>(BoundedSourceTree.scan(sourceRoot));
        Collections.sort(files, Comparator.comparing(f -> relativePath(sourceRoot, f).toLowerCase(java.util.Locale.ROOT)));
        return files;
    }

    String read(File file) throws IOException {
        ensureInsideSource(file);
        if (!file.isFile() || file.length() > MAX_SOURCE_FILE)
            throw new IOException("C# source file is missing or exceeds the 32 MB safety limit.");
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (bytes.length > MAX_SOURCE_FILE)
            throw new IOException("C# source file exceeds the 32 MB safety limit.");
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    void save(File file, String text) throws IOException {
        ensureInsideSource(file);
        write(file, text == null ? "" : text);
    }

    File createSourceFile(String requestedName) throws IOException {
        String name = safeFileName(requestedName);
        if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".cs")) name += ".cs";
        File target = new File(sourceRoot, name);
        int suffix = 2;
        while (target.exists()) {
            int dot = name.toLowerCase(java.util.Locale.ROOT).lastIndexOf(".cs");
            target = new File(sourceRoot, name.substring(0, dot) + "_" + suffix++ + ".cs");
        }
        save(target, "");
        return target;
    }

    List<File> importFiles(Context context, List<Uri> uris) throws IOException {
        ArrayList<File> imported = new ArrayList<>();
        List<File> existing = listSourceFiles();
        long total = 0;
        for (File file : existing) total += file.length();
        byte[] buffer = new byte[32768];
        try {
            for (Uri uri : uris) {
                checkCancelled();
                if (uri == null) continue;
                String name = safeFileName(queryName(context, uri));
                if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".cs")) continue;
                if (existing.size() + imported.size() >= BoundedSourceTree.MAX_SOURCE_FILES)
                    throw new IOException("C# source project exceeds the " + BoundedSourceTree.MAX_SOURCE_FILES + " file safety limit.");
                File target = uniqueTarget(name);
                long copied = 0;
                try (InputStream input = context.getContentResolver().openInputStream(uri);
                     OutputStream output = new FileOutputStream(target)) {
                    if (input == null) throw new IOException("Cannot open selected source file: " + name);
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        checkCancelled();
                        copied += count;
                        total += count;
                        if (copied > MAX_SOURCE_FILE || total > MAX_PROJECT_IMPORT)
                            throw new IOException("Source import exceeds the 32 MB per-file or 128 MB total project safety limit.");
                        output.write(buffer, 0, count);
                    }
                    output.flush();
                } catch (IOException error) {
                    target.delete();
                    throw error;
                }
                imported.add(target);
            }
            if (imported.isEmpty()) throw new IOException("No .cs files were selected.");
            return imported;
        } catch (IOException error) {
            for (File file : imported) file.delete();
            throw error;
        }
    }

    void exportZip(OutputStream destination) throws IOException {
        List<File> files = listSourceFiles();
        if (files.isEmpty()) throw new IOException("There are no C# source files to export.");
        ZipOutputStream zip = new ZipOutputStream(destination);
        byte[] buffer = new byte[32768];
        for (File file : files) {
            checkCancelled();
            zip.putNextEntry(new ZipEntry(relativePath(sourceRoot, file)));
            try (InputStream input = new FileInputStream(file)) {
                int count;
                while ((count = input.read(buffer)) != -1) {
                    checkCancelled();
                    zip.write(buffer, 0, count);
                }
            }
            zip.closeEntry();
        }
        zip.finish();
        zip.flush();
    }

    private File uniqueTarget(String name) {
        File target = new File(sourceRoot, name);
        int suffix = 2;
        while (target.exists()) {
            int dot = name.toLowerCase(java.util.Locale.ROOT).lastIndexOf(".cs");
            target = new File(sourceRoot, name.substring(0, dot) + "_" + suffix++ + ".cs");
        }
        return target;
    }

    private void ensureInsideSource(File file) throws IOException {
        if (!sourceRoot.isDirectory() || !isDirectChild(root, sourceRoot)
                || Files.isSymbolicLink(file.toPath()))
            throw new IOException("C# source directory or file is unavailable or linked.");
        String rootPath = sourceRoot.getCanonicalPath() + File.separator;
        String filePath = file.getCanonicalPath();
        if (!filePath.startsWith(rootPath) || !file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".cs"))
            throw new IOException("Source file path is outside the project or is not a .cs file.");
    }

    private static String relativePath(File root, File file) {
        return file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1).replace(File.separatorChar, '/');
    }

    private static String safeFileName(String value) {
        String name = value == null ? "Source.cs" : value.replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) name = "Source.cs";
        if (name.length() > 100) name = name.substring(name.length() - 100);
        return name;
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
        return segment == null ? "Source.cs" : segment;
    }


    private static void copyOriginalAssembly(Context context, Uri uri, File target) throws IOException {
        File temporary = new File(target.getParentFile(), target.getName() + ".tmp-" + UUID.randomUUID());
        long total = 0;
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(temporary)) {
            if (input == null) throw new IOException("Cannot reopen the selected original assembly.");
            byte[] buffer = new byte[32768];
            int count;
            while ((count = input.read(buffer)) != -1) {
                checkCancelled();
                total += count;
                if (total > MAX_ORIGINAL_ASSEMBLY)
                    throw new IOException("Original assembly exceeds the 512 MB project safety limit.");
                output.write(buffer, 0, count);
            }
            output.flush();
            output.getFD().sync();
        } catch (IOException error) {
            temporary.delete();
            throw error;
        }
        if (total == 0 || !temporary.renameTo(target)) {
            temporary.delete();
            throw new IOException(total == 0 ? "The selected original assembly is empty." : "Cannot preserve the original assembly in the project.");
        }
    }

    private static void write(File target, String text) throws IOException {
        checkCancelled();
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs())
            throw new IOException("Cannot create source directory.");
        File temporary = new File(parent, target.getName() + ".tmp-" + UUID.randomUUID());
        try {
            byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (bytes.length > MAX_SOURCE_FILE)
                throw new IOException("C# source file exceeds the 32 MB safety limit.");
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                checkCancelled();
                output.write(bytes);
                output.flush();
                output.getFD().sync();
            }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (temporary.exists()) temporary.delete();
        }
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("C# source project creation cancelled because its screen was closed.");
    }

    private static boolean isSafeProjectLayout(File base, File root) throws IOException {
        File marker = new File(root, ".droidinspect-project");
        File source = new File(root, "src");
        File original = new File(root, "original");
        File originalAssembly = new File(original, "OriginalAssembly.bin");
        return !Files.isSymbolicLink(base.toPath()) && base.isDirectory()
                && isDirectChild(base, root) && root.isDirectory()
                && isDirectChild(root, marker) && marker.isFile()
                && isDirectChild(root, source) && source.isDirectory()
                && isDirectChild(root, original) && original.isDirectory()
                && isDirectChild(original, originalAssembly) && originalAssembly.isFile()
                && originalAssembly.length() > 0
                && originalAssembly.length() <= MAX_ORIGINAL_ASSEMBLY;
    }

    private static boolean isDirectChild(File parent, File child) throws IOException {
        if (parent == null || child == null || Files.isSymbolicLink(parent.toPath())
                || Files.isSymbolicLink(child.toPath())) return false;
        return parent.getCanonicalFile().equals(child.getCanonicalFile().getParentFile());
    }

    private static void cleanupFailedCreation(File root, File expectedParent, Throwable failure) {
        try {
            BoundedProjectFiles.deleteTree(root, expectedParent, 10000);
        } catch (IOException cleanupError) {
            failure.addSuppressed(cleanupError);
        }
    }
}
