package com.tanvir.droidinspect;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

    static CSharpSourceWorkspace create(Context context, String name, String decompiledSource) throws IOException {
        File base = new File(context.getFilesDir(), "csharp-projects");
        if (!base.exists() && !base.mkdirs()) throw new IOException("Cannot create source project storage.");
        String id = "project_" + UUID.randomUUID().toString().replace("-", "");
        File root = new File(base, id);
        File src = new File(root, "src");
        File original = new File(root, "original");
        if (!src.mkdirs() || !original.mkdirs()) throw new IOException("Cannot create source project folders.");
        String safeDisplay = name == null || name.trim().isEmpty() ? "Decompiled assembly" : name.trim();
        CSharpSourceWorkspace project = new CSharpSourceWorkspace(root, id, safeDisplay);
        try {
            project.write(new File(original, "Decompiled.cs"), decompiledSource == null ? "" : decompiledSource);
            project.write(new File(src, "Decompiled.cs"), decompiledSource == null ? "" : decompiledSource);
            project.write(new File(root, ".droidinspect-project"), safeDisplay + "\nformat=1\n");
            return project;
        } catch (IOException error) {
            deleteTree(root);
            throw error;
        }
    }

    static CSharpSourceWorkspace open(Context context, String id) throws IOException {
        if (id == null || !id.matches("project_[A-Fa-f0-9]{32}"))
            throw new IOException("Invalid source project identifier.");
        File root = new File(new File(context.getFilesDir(), "csharp-projects"), id);
        File marker = new File(root, ".droidinspect-project");
        if (!marker.isFile() || !new File(root, "src").isDirectory())
            throw new IOException("The saved C# source project could not be found.");
        String name = "C# source project";
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new FileInputStream(marker), "UTF-8"))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) name = line.trim();
        }
        return new CSharpSourceWorkspace(root, id, name);
    }

    String getId() { return id; }
    String getDisplayName() { return displayName; }
    File getSourceRoot() { return sourceRoot; }

    List<File> listSourceFiles() throws IOException {
        ArrayList<File> files = new ArrayList<>();
        collect(sourceRoot, files);
        Collections.sort(files, Comparator.comparing(f -> relativePath(sourceRoot, f).toLowerCase(java.util.Locale.ROOT)));
        return files;
    }

    String read(File file) throws IOException {
        ensureInsideSource(file);
        byte[] bytes = Files.readAllBytes(file.toPath());
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
        long total = 0;
        byte[] buffer = new byte[32768];
        for (Uri uri : uris) {
            if (uri == null) continue;
            String name = safeFileName(queryName(context, uri));
            if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".cs")) continue;
            File target = uniqueTarget(name);
            long copied = 0;
            try (InputStream input = context.getContentResolver().openInputStream(uri);
                 OutputStream output = new FileOutputStream(target)) {
                if (input == null) throw new IOException("Cannot open selected source file: " + name);
                int count;
                while ((count = input.read(buffer)) != -1) {
                    copied += count;
                    total += count;
                    if (copied > MAX_SOURCE_FILE || total > MAX_PROJECT_IMPORT)
                        throw new IOException("Source import exceeds the 32 MB per-file or 128 MB total safety limit.");
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
    }

    void exportZip(OutputStream destination) throws IOException {
        List<File> files = listSourceFiles();
        if (files.isEmpty()) throw new IOException("There are no C# source files to export.");
        ZipOutputStream zip = new ZipOutputStream(destination);
        byte[] buffer = new byte[32768];
        for (File file : files) {
            zip.putNextEntry(new ZipEntry(relativePath(sourceRoot, file)));
            try (InputStream input = new FileInputStream(file)) {
                int count;
                while ((count = input.read(buffer)) != -1) zip.write(buffer, 0, count);
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
        String rootPath = sourceRoot.getCanonicalPath() + File.separator;
        String filePath = file.getCanonicalPath();
        if (!filePath.startsWith(rootPath) || !file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".cs"))
            throw new IOException("Source file path is outside the project or is not a .cs file.");
    }

    private static void collect(File directory, List<File> output) throws IOException {
        File[] children = directory.listFiles();
        if (children == null) throw new IOException("Cannot read source project directory.");
        for (File child : children) {
            if (child.isDirectory()) collect(child, output);
            else if (child.isFile() && child.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".cs"))
                output.add(child);
        }
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

    private static void write(File target, String text) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs())
            throw new IOException("Cannot create source directory.");
        File temporary = new File(parent, target.getName() + ".tmp-" + UUID.randomUUID());
        try {
            byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(bytes);
                output.flush();
                output.getFD().sync();
            }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (temporary.exists()) temporary.delete();
        }
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        file.delete();
    }
}
