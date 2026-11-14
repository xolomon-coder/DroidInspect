package com.tanvir.droidinspect;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

/** Owns a private input copy and the persistent offline framework references. */
final class DecompilationWorkspace implements AutoCloseable {
    private static final long MAX_INPUT = 512L * 1024L * 1024L;
    private final File root;
    private final File assembly;
    private final File references;

    private DecompilationWorkspace(File root, File assembly, File references) {
        this.root = root;
        this.assembly = assembly;
        this.references = references;
    }

    static DecompilationWorkspace prepare(Context context, Uri source) throws IOException {
        if (source == null) throw new IOException("Selected document is unavailable.");
        File root = new File(new File(context.getCacheDir(), "csharp-decompile"), UUID.randomUUID().toString());
        if (!root.mkdirs()) throw new IOException("Cannot create private decompilation workspace.");
        try {
            File input = new File(root, safeName(queryName(context, source)));
            copyDocument(context, source, input);
            return new DecompilationWorkspace(root, input, ensureReferences(context));
        } catch (Exception e) {
            deleteTree(root);
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException("Could not prepare decompilation workspace: " + e.getMessage(), e);
        }
    }

    File getAssembly() { return assembly; }
    String getReferenceDirectory() { return references.getAbsolutePath(); }

    private static void copyDocument(Context context, Uri uri, File target) throws IOException {
        long total = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) throw new IOException("Cannot reopen the selected document. Please select it again.");
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > MAX_INPUT) throw new IOException("Input exceeds the 512 MB safety limit.");
                out.write(buffer, 0, n);
            }
            out.flush();
        } catch (IOException e) {
            target.delete();
            throw e;
        }
        if (target.length() == 0) {
            target.delete();
            throw new IOException("The selected assembly is empty.");
        }
    }

    private static File ensureReferences(Context context) throws IOException {
        File base = new File(context.getFilesDir(), "reference-assemblies");
        File ready = new File(base, "net11.0");
        if (new File(ready, ".droidinspect-ready").isFile()) return ready;
        if (!base.exists() && !base.mkdirs()) throw new IOException("Cannot create reference directory.");
        File staging = new File(base, "net11.0.tmp-" + UUID.randomUUID());
        if (!staging.mkdirs()) throw new IOException("Cannot create reference extraction workspace.");
        boolean activated = false;
        try {
            String assetRoot = "reference-assemblies/net11.0";
            String[] names = context.getAssets().list(assetRoot);
            if (names == null || names.length == 0) throw new IOException("Bundled .NET 11 references are missing.");
            int copied = 0;
            byte[] buffer = new byte[32768];
            for (String name : names) {
                if (!name.toLowerCase(Locale.ROOT).endsWith(".dll")) continue;
                try (InputStream in = context.getAssets().open(assetRoot + "/" + name);
                     OutputStream out = new FileOutputStream(new File(staging, name))) {
                    int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                    out.flush();
                }
                copied++;
            }
            if (copied == 0) throw new IOException("No framework reference DLLs were found.");
            try (FileOutputStream marker = new FileOutputStream(new File(staging, ".droidinspect-ready"))) {
                marker.write(("net11.0:" + copied).getBytes("UTF-8"));
                marker.flush();
                marker.getFD().sync();
            }
            deleteTree(ready);
            if (!staging.renameTo(ready)) throw new IOException("Cannot activate extracted framework references.");
            activated = true;
            return ready;
        } finally {
            if (!activated) deleteTree(staging);
        }
    }

    private static String queryName(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String name = cursor.getString(column);
                    if (name != null && !name.trim().isEmpty()) return name;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String segment = uri.getLastPathSegment();
        return segment == null ? "selected-assembly.dll" : segment;
    }

    private static String safeName(String name) {
        String value = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (value.isEmpty() || ".".equals(value) || "..".equals(value)) value = "selected-assembly.dll";
        return value.length() > 120 ? value.substring(value.length() - 120) : value;
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    @Override public void close() { deleteTree(root); }
}
