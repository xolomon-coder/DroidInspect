package com.tanvir.droidinspect;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;

/** Owns a private input copy and the persistent offline framework references. */
final class DecompilationWorkspace implements AutoCloseable {
    private static final long MAX_INPUT = 512L * 1024L * 1024L;
    private static final int MAX_CLEANUP_ENTRIES = 10000;
    private static final int MAX_REFERENCE_FILES = 10000;
    private static final long MAX_REFERENCE_BYTES = 512L * 1024L * 1024L;

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
        File cache = context.getCacheDir();
        File parent = new File(cache, "csharp-decompile");
        ensureDirectory(cache, parent, "private decompilation workspace");
        File root = new File(parent, UUID.randomUUID().toString());
        if (!root.mkdir() || Files.isSymbolicLink(root.toPath()) || !isDirectChild(parent, root))
            throw new IOException("Cannot create a safe private decompilation workspace.");

        try {
            checkCancelled();
            File input = new File(root, safeName(queryName(context, source)));
            copyDocument(context, source, input);
            return new DecompilationWorkspace(root, input, ensureReferences(context));
        } catch (Exception | Error error) {
            cleanupForRollback(root, parent, error);
            if (error instanceof IOException) throw (IOException) error;
            if (error instanceof Error) throw (Error) error;
            throw new IOException("Could not prepare decompilation workspace: " + error.getMessage(), error);
        }
    }

    File getAssembly() { return assembly; }
    String getReferenceDirectory() { return references.getAbsolutePath(); }

    private static void copyDocument(Context context, Uri uri, File target) throws IOException {
        long total = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null)
                throw new IOException("Cannot reopen the selected document. Please select it again.");
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) != -1) {
                checkCancelled();
                total += n;
                if (total > MAX_INPUT)
                    throw new IOException("Input exceeds the 512 MB safety limit.");
                out.write(buffer, 0, n);
            }
            out.flush();
        } catch (IOException | RuntimeException | Error error) {
            deletePartialFile(target, error);
            throw error;
        }
        if (target.length() == 0) {
            IOException empty = new IOException("The selected assembly is empty.");
            deletePartialFile(target, empty);
            throw empty;
        }
    }

    /**
     * Serializes first-use extraction so decompile, build and dependency workers cannot
     * concurrently delete or replace the shared framework reference directory.
     */
    static synchronized File ensureReferences(Context context) throws IOException {
        File files = context.getFilesDir();
        File base = new File(files, "reference-assemblies");
        ensureDirectory(files, base, "offline framework reference directory");

        File ready = new File(base, "net11.0");
        if (Files.isSymbolicLink(ready.toPath()))
            throw new IOException("Offline framework reference directory is a symbolic link.");
        File marker = new File(ready, ".droidinspect-ready");
        if (Files.isSymbolicLink(marker.toPath()))
            throw new IOException("Offline framework reference marker is a symbolic link.");
        if (ready.isDirectory() && marker.isFile()) return ready;

        File staging = new File(base, "net11.0.tmp-" + UUID.randomUUID());
        if (!staging.mkdir())
            throw new IOException("Cannot create framework reference extraction workspace.");
        if (Files.isSymbolicLink(staging.toPath()) || !isDirectChild(base, staging)) {
            IOException unsafe = new IOException("Framework reference staging path is unsafe.");
            cleanupForRollback(staging, base, unsafe);
            throw unsafe;
        }

        boolean activated = false;
        Throwable primaryFailure = null;
        try {
            String assetRoot = "reference-assemblies/net11.0";
            String[] names = context.getAssets().list(assetRoot);
            if (names == null || names.length == 0)
                throw new IOException("Bundled .NET 11 references are missing.");
            if (names.length > MAX_REFERENCE_FILES)
                throw new IOException("Bundled framework references exceed the file safety limit.");

            int copied = 0;
            long totalBytes = 0;
            byte[] buffer = new byte[32768];
            for (String name : names) {
                checkCancelled();
                if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".dll")) continue;
                if (++copied > MAX_REFERENCE_FILES)
                    throw new IOException("Bundled framework references exceed the file safety limit.");
                File outputFile = new File(staging, name);
                if (!isDirectChild(staging, outputFile))
                    throw new IOException("Invalid bundled framework reference file name.");
                try (InputStream in = context.getAssets().open(assetRoot + "/" + name);
                     OutputStream out = new FileOutputStream(outputFile)) {
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        checkCancelled();
                        totalBytes += n;
                        if (totalBytes > MAX_REFERENCE_BYTES)
                            throw new IOException("Bundled framework references exceed the 512 MB safety limit.");
                        out.write(buffer, 0, n);
                    }
                    out.flush();
                }
            }
            if (copied == 0) throw new IOException("No framework reference DLLs were found.");
            try (FileOutputStream readyMarker = new FileOutputStream(
                    new File(staging, ".droidinspect-ready"))) {
                readyMarker.write(("net11.0:" + copied).getBytes("UTF-8"));
                readyMarker.flush();
                readyMarker.getFD().sync();
            }
            checkCancelled();
            BoundedProjectFiles.deleteTree(ready, base, MAX_CLEANUP_ENTRIES);
            if (!staging.renameTo(ready))
                throw new IOException("Cannot activate extracted framework references.");
            activated = true;
            return ready;
        } catch (IOException | RuntimeException | Error error) {
            primaryFailure = error;
            throw error;
        } finally {
            if (!activated) {
                try {
                    BoundedProjectFiles.deleteTreeForRollback(staging, base, MAX_CLEANUP_ENTRIES);
                } catch (IOException | RuntimeException cleanupError) {
                    IOException reported = cleanupError instanceof IOException ? (IOException) cleanupError
                            : new IOException("Could not clean incomplete framework-reference staging files.", cleanupError);
                    if (primaryFailure != null) primaryFailure.addSuppressed(reported);
                    else throw reported;
                }
            }
        }
    }

    private static void cleanupForRollback(File root, File parent, Throwable failure) {
        try {
            BoundedProjectFiles.deleteTreeForRollback(root, parent, MAX_CLEANUP_ENTRIES);
        } catch (IOException | RuntimeException cleanupError) {
            failure.addSuppressed(cleanupError instanceof IOException ? cleanupError
                    : new IOException("Could not roll back incomplete decompilation workspace.", cleanupError));
        }
    }

    private static void deletePartialFile(File file, Throwable failure) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException | RuntimeException cleanupError) {
            failure.addSuppressed(new IOException(
                    "Could not remove partial decompilation input: " + file.getName(), cleanupError));
        }
    }

    private static void ensureDirectory(File parent, File child, String description) throws IOException {
        if (parent == null || Files.isSymbolicLink(parent.toPath()) || !parent.isDirectory())
            throw new IOException("Trusted parent for " + description + " is unavailable.");
        if (Files.isSymbolicLink(child.toPath()))
            throw new IOException("Refusing a symbolic-link " + description + ".");
        if (!child.exists() && !child.mkdir())
            throw new IOException("Cannot create " + description + ".");
        if (!child.isDirectory() || Files.isSymbolicLink(child.toPath()) || !isDirectChild(parent, child))
            throw new IOException("Unsafe or unavailable " + description + ".");
    }

    private static boolean isDirectChild(File parent, File child) throws IOException {
        if (parent == null || child == null || Files.isSymbolicLink(parent.toPath())
                || Files.isSymbolicLink(child.toPath())) return false;
        return parent.getCanonicalFile().equals(child.getCanonicalFile().getParentFile());
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("Decompilation preparation cancelled because its screen was closed.");
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
        if (value.isEmpty() || ".".equals(value) || "..".equals(value))
            value = "selected-assembly.dll";
        return value.length() > 120 ? value.substring(value.length() - 120) : value;
    }

    @Override public void close() throws IOException {
        // A cancelled worker still completes bounded rollback without clearing its interrupt flag.
        if (Thread.currentThread().isInterrupted())
            BoundedProjectFiles.deleteTreeForRollback(root, root.getParentFile(), MAX_CLEANUP_ENTRIES);
        else
            BoundedProjectFiles.deleteTree(root, root.getParentFile(), MAX_CLEANUP_ENTRIES);
    }
}
