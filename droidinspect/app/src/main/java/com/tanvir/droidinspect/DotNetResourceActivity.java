package com.tanvir.droidinspect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Lists and exports embedded ManifestResource payloads without modifying the source assembly. */
public final class DotNetResourceActivity extends Activity {
    private static final int PICK_ASSEMBLY = 6201, CREATE_OUTPUT = 6202;
    private static final long MAX_INPUT = 512L * 1024L * 1024L;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean busy;
    private volatile boolean destroyed;
    private final ArrayList<ResourceItem> resources = new ArrayList<>();
    private File assemblyCopy;
    private TextView status, details;
    private Button chooseButton, exportButton;
    private ListView list;
    private ResourceItem selected;
    private byte[] pendingBytes;

    private static final class ResourceItem {
        final String name, implementation;
        final long offset, flags;
        ResourceItem(String name, long offset, long flags, String implementation) {
            this.name = name; this.offset = offset; this.flags = flags; this.implementation = implementation;
        }
        @Override public String toString() {
            return name + (implementation.equals("embedded") ? "  [embedded]" : "  [" + implementation + "]");
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(0xFF101318);
        TextView title = new TextView(this);
        title.setText("Embedded .NET Resources");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        root.addView(title);
        chooseButton = new Button(this);
        chooseButton.setText("SELECT MANAGED DLL / EXE");
        root.addView(chooseButton);
        status = new TextView(this);
        status.setText("Select a managed assembly to list its ManifestResource entries.");
        status.setTextColor(0xFF80CBC4);
        status.setPadding(0, dp(8), 0, dp(8));
        root.addView(status);
        list = new ListView(this);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        details = new TextView(this);
        details.setTextColor(0xFFE5EAF0);
        details.setTextSize(12);
        details.setPadding(0, dp(8), 0, dp(8));
        root.addView(details);
        exportButton = new Button(this);
        exportButton.setText("EXPORT SELECTED EMBEDDED RESOURCE");
        exportButton.setEnabled(false);
        root.addView(exportButton);
        setContentView(root);

        chooseButton.setOnClickListener(v -> pickAssembly());
        exportButton.setOnClickListener(v -> exportSelected());
        list.setOnItemClickListener((parent, view, position, id) -> select(resources.get(position)));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void pickAssembly() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_ASSEMBLY);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null) return;
        if (request == PICK_ASSEMBLY && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            } catch (SecurityException ignored) { }
            loadAssembly(uri);
        } else if (request == CREATE_OUTPUT && data.getData() != null && pendingBytes != null) {
            writeOutput(data.getData());
        }
    }

    private void loadAssembly(Uri uri) {
        if (busy) return;
        busy = true;
        chooseButton.setEnabled(false);
        exportButton.setEnabled(false);
        status.setText("Reading managed resource metadata…");
        final Context app = getApplicationContext();
        final File cacheDir = app.getCacheDir();
        final WeakReference<DotNetResourceActivity> owner = new WeakReference<>(this);
        final Handler ui = mainHandler;
        worker.execute(() -> {
            File copy = null;
            Throwable problem = null;
            List<ResourceItem> parsed = null;
            try {
                copy = File.createTempFile("droidinspect-resource-source-", ".dll", cacheDir);
                long total = 0;
                try (InputStream in = app.getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(copy)) {
                    if (in == null) throw new IOException("Unable to open selected assembly.");
                    byte[] buffer = new byte[32768];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        checkCancelled();
                        total += n;
                        if (total > MAX_INPUT) throw new IOException("Assembly exceeds the 512 MB inspection limit.");
                        out.write(buffer, 0, n);
                    }
                    out.flush();
                    out.getFD().sync();
                }
                checkCancelled();
                String metadata = FileInspectionEngine.inspectPe(copy, copy.getName());
                if (!metadata.contains(".NET CLI header: PRESENT"))
                    throw new IllegalArgumentException("Selected file is not a recognized managed .NET assembly.");
                parsed = parseResources(metadata);
                checkCancelled();
            } catch (Exception | Error error) {
                problem = error;
                if (copy != null) deleteOwnedFile(copy, cacheDir, problem);
                copy = null;
            }
            final File ready = copy;
            final List<ResourceItem> items = parsed;
            final String errorText = problem == null ? null : describe(problem);
            ui.post(() -> {
                DotNetResourceActivity activity = owner.get();
                if (activity == null || activity.destroyed || activity.isFinishing() || activity.isDestroyed()) {
                    deleteOwnedFileAsync(ready, cacheDir);
                    return;
                }
                activity.busy = false;
                activity.chooseButton.setEnabled(true);
                if (errorText != null || items == null) {
                    activity.status.setText("RESOURCE INSPECTION FAILED: " + safe(errorText));
                    activity.exportButton.setEnabled(false);
                    deleteOwnedFileAsync(ready, cacheDir);
                    return;
                }
                File old = activity.assemblyCopy;
                activity.assemblyCopy = ready;
                deleteOwnedFileAsync(old, cacheDir);
                activity.resources.clear();
                activity.resources.addAll(items);
                activity.selected = null;
                activity.pendingBytes = null;
                ArrayAdapter<ResourceItem> adapter = new ArrayAdapter<>(activity,
                        android.R.layout.simple_list_item_1, activity.resources);
                activity.list.setAdapter(adapter);
                activity.details.setText("");
                activity.exportButton.setEnabled(false);
                activity.status.setText("Found " + activity.resources.size()
                        + " manifest resource record(s). Linked resources are listed but not exported.");
            });
        });
    }

    private static List<ResourceItem> parseResources(String metadata) {
        ArrayList<ResourceItem> result = new ArrayList<>();
        boolean inSection = false;
        for (String raw : metadata.split("\\n")) {
            String line = raw.trim();
            if (line.startsWith("MANIFEST RESOURCES (")) { inSection = true; continue; }
            if (inSection && (line.startsWith("Decode status:") || line.startsWith("PHASE 1 SCOPE"))) break;
            if (!inSection || !line.matches("^\\d+\\..*ManifestResource token 0x[0-9A-Fa-f]{8}.*$")) continue;
            int marker = line.indexOf(" (ManifestResource token");
            int offsetAt = line.indexOf("; offset=");
            int flagsAt = line.indexOf("; flags=0x");
            int implementationAt = line.indexOf("; implementation=");
            if (marker < 0 || offsetAt < 0 || flagsAt < 0 || implementationAt < 0) continue;
            String name = line.substring(line.indexOf(". ") + 2, marker);
            try {
                long offset = Long.parseLong(line.substring(offsetAt + 9, line.indexOf(';', offsetAt + 2)));
                long flags = Long.parseLong(line.substring(flagsAt + 10, line.indexOf(';', flagsAt + 2)), 16);
                int end = line.indexOf(' ', implementationAt + 17);
                String implementation = line.substring(implementationAt + 17, end < 0 ? line.length() - 1 : end);
                result.add(new ResourceItem(name, offset, flags, implementation));
            } catch (Exception ignored) { }
        }
        return result;
    }

    private void select(ResourceItem item) {
        selected = item;
        pendingBytes = null;
        if (!"embedded".equals(item.implementation)) {
            details.setText("Linked resource (" + item.implementation + ") is not stored in this PE resource directory. Locate its external file or referenced assembly.");
            exportButton.setEnabled(false);
            return;
        }
        details.setText("Name: " + item.name + "\nResource directory offset: " + item.offset
                + "\nManifest flags: 0x" + Long.toHexString(item.flags)
                + "\nEmbedded data will be checked and extracted when you export.");
        exportButton.setEnabled(assemblyCopy != null);
    }

    private void exportSelected() {
        if (busy || selected == null || assemblyCopy == null || !"embedded".equals(selected.implementation)) return;
        final File source = assemblyCopy;
        final long offset = selected.offset;
        final String fileName = safeFilename(selected.name);
        if (!source.isFile()) return;
        busy = true;
        exportButton.setEnabled(false);
        chooseButton.setEnabled(false);
        status.setText("Validating and reading resource payload…");
        final WeakReference<DotNetResourceActivity> owner = new WeakReference<>(this);
        final Handler ui = mainHandler;
        worker.execute(() -> {
            byte[] bytes = null;
            Throwable problem = null;
            try {
                checkCancelled();
                bytes = FileInspectionEngine.extractEmbeddedManagedResource(source, offset);
                checkCancelled();
            } catch (Exception | Error error) {
                problem = error;
            }
            final byte[] ready = bytes;
            final String errorText = problem == null ? null : describe(problem);
            ui.post(() -> {
                DotNetResourceActivity activity = owner.get();
                if (activity == null || activity.destroyed || activity.isFinishing() || activity.isDestroyed()) return;
                activity.busy = false;
                activity.chooseButton.setEnabled(true);
                activity.status.setText(errorText == null
                        ? "Resource ready: " + (ready == null ? 0 : ready.length) + " bytes. Choose where to save it."
                        : "EXTRACTION FAILED: " + errorText);
                if (errorText != null || ready == null) {
                    activity.exportButton.setEnabled(true);
                    return;
                }
                activity.pendingBytes = ready;
                activity.exportButton.setEnabled(true);
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_TITLE, fileName);
                activity.startActivityForResult(intent, CREATE_OUTPUT);
            });
        });
    }

    private static String safeFilename(String name) {
        String value = name == null ? "resource.bin" : name.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (value.isEmpty()) value = "resource.bin";
        return value.length() > 120 ? value.substring(0, 120) : value;
    }

    private void writeOutput(Uri uri) {
        final byte[] bytes = pendingBytes;
        pendingBytes = null;
        if (bytes == null || busy) return;
        busy = true;
        chooseButton.setEnabled(false);
        exportButton.setEnabled(false);
        status.setText("Saving embedded resource…");
        final Context app = getApplicationContext();
        final WeakReference<DotNetResourceActivity> owner = new WeakReference<>(this);
        final Handler ui = mainHandler;
        worker.execute(() -> {
            Throwable problem = null;
            try (OutputStream out = app.getContentResolver().openOutputStream(uri, "w")) {
                if (out == null) throw new IOException("Unable to create destination document.");
                out.write(bytes);
                out.flush();
            } catch (Exception | Error error) {
                problem = error;
            }
            final String errorText = problem == null ? null : describe(problem);
            ui.post(() -> {
                DotNetResourceActivity activity = owner.get();
                if (activity == null || activity.destroyed || activity.isFinishing() || activity.isDestroyed()) return;
                activity.busy = false;
                activity.chooseButton.setEnabled(true);
                activity.exportButton.setEnabled(activity.selected != null
                        && activity.assemblyCopy != null && "embedded".equals(activity.selected.implementation));
                if (errorText == null) {
                    activity.status.setText("Exported " + bytes.length + " bytes. Original assembly was not modified.");
                    Toast.makeText(activity, "Resource exported", Toast.LENGTH_SHORT).show();
                } else {
                    activity.status.setText("SAVE FAILED: " + errorText);
                }
            });
        });
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("Resource operation cancelled because the activity was closed.");
    }

    private static String safe(String value) { return value == null ? "unknown" : value; }

    private static String describe(Throwable error) {
        StringBuilder text = new StringBuilder(error.getClass().getSimpleName()).append(": ").append(safe(error.getMessage()));
        for (Throwable suppressed : error.getSuppressed())
            text.append("\nCleanup warning: ").append(suppressed.getClass().getSimpleName())
                    .append(": ").append(safe(suppressed.getMessage()));
        return text.toString();
    }

    private static void deleteOwnedFile(File file, File cacheDir, Throwable primary) {
        if (file == null) return;
        try {
            File absolute = file.getAbsoluteFile();
            if (!cacheDir.getAbsoluteFile().equals(absolute.getParentFile())
                    || !absolute.getName().startsWith("droidinspect-resource-source-")
                    || Files.isSymbolicLink(absolute.toPath()))
                throw new IOException("Refusing to delete a file outside the private assembly cache.");
            Files.deleteIfExists(absolute.toPath());
            if (absolute.exists()) throw new IOException("Assembly temporary file still exists after deletion.");
        } catch (IOException | RuntimeException cleanupError) {
            IOException reported = cleanupError instanceof IOException ? (IOException) cleanupError
                    : new IOException("Assembly temporary-file cleanup failed.", cleanupError);
            if (primary != null) primary.addSuppressed(reported);
            else Log.e("DroidInspectResource", "Temporary file could not be removed: " + file, reported);
        }
    }

    private static void deleteOwnedFileAsync(File file, File cacheDir) {
        if (file == null) return;
        Thread cleanup = new Thread(() -> deleteOwnedFile(file, cacheDir, null), "droidinspect-resource-file-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        final ExecutorService executor = worker;
        final File cacheDir = getCacheDir();
        final File copy = assemblyCopy;
        assemblyCopy = null;
        pendingBytes = null;
        executor.shutdownNow();
        super.onDestroy();
        Thread cleanup = new Thread(() -> {
            boolean interrupted = false;
            while (true) {
                try {
                    if (executor.awaitTermination(1, TimeUnit.MINUTES)) break;
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            deleteOwnedFile(copy, cacheDir, null);
            if (interrupted) Thread.currentThread().interrupt();
        }, "droidinspect-resource-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }
}
