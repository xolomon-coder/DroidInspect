package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.reandroid.apk.ApkModule;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.Entry;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Searchable read-only map of Android resource IDs to named resource entries and default values. */
public final class ApkResourceCatalogActivity extends Activity {
    private static final int PICK_APK = 6501;
    private static final long MAX_APK_BYTES = 512L * 1024L * 1024L;
    private static final int MAX_SCAN = 50000;
    private static final int MAX_STORED = 25000;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean busy;
    private volatile boolean destroyed;
    private final ArrayList<ResourceInfo> all = new ArrayList<>();
    private final ArrayList<ResourceInfo> shown = new ArrayList<>();
    private final ArrayList<String> labels = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private EditText search;
    private Spinner typeFilter;
    private ListView list;
    private TextView summary, status;
    private ProgressBar progress;
    private Button choose;
    private File apkCopy;
    private String selectedType = "All types";

    private static final class ResourceInfo {
        final String type, name, value, id;
        final boolean isDefault;
        ResourceInfo(String type, String name, String value, String id, boolean isDefault) {
            this.type = type; this.name = name; this.value = value; this.id = id; this.isDefault = isDefault;
        }
        String label() { return type + "/" + name + "  •  0x" + id; }
        String report() {
            return "Resource: " + type + "/" + name + "\nResource ID: 0x" + id
                    + "\nDefault configuration: " + (isDefault ? "yes" : "no")
                    + "\nDefault value: " + (value.isEmpty() ? "(unavailable / non-scalar)" : value);
        }
        boolean matches(String q) {
            return type.toLowerCase(Locale.ROOT).contains(q)
                    || name.toLowerCase(Locale.ROOT).contains(q)
                    || id.toLowerCase(Locale.ROOT).contains(q)
                    || value.toLowerCase(Locale.ROOT).contains(q);
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.setBackgroundColor(0xFF101318);
        TextView title = new TextView(this);
        title.setText("APK Resource ID Catalog");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(19);
        root.addView(title);
        choose = new Button(this);
        choose.setText("SELECT APK");
        root.addView(choose);
        summary = new TextView(this);
        summary.setTextColor(0xFFE5EAF0);
        summary.setTextSize(12);
        root.addView(summary);
        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search resource name, ID, type or default value");
        root.addView(search);
        typeFilter = new Spinner(this);
        root.addView(typeFilter);
        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));
        status = new TextView(this);
        status.setTextColor(0xFF80CBC4);
        status.setPadding(0, dp(5), 0, dp(5));
        root.addView(status);
        list = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        choose.setOnClickListener(v -> pickApk());
        typeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = parent.getItemAtPosition(position).toString();
                filter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
            @Override public void afterTextChanged(android.text.Editable e) { }
        });
        list.setOnItemClickListener((parent, view, position, id) ->
                new AlertDialog.Builder(this).setTitle(shown.get(position).name)
                        .setMessage(shown.get(position).report()).setPositiveButton("CLOSE", null).show());
        status.setText("Select an APK to map resource IDs to names and available default values.");
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private void pickApk() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.android.package-archive");
        startActivityForResult(intent, PICK_APK);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_APK || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        } catch (SecurityException ignored) { }
        load(uri);
    }

    private void load(Uri uri) {
        if (busy) return;
        setBusy(true, "Copying APK and indexing resource table…");
        final Context app = getApplicationContext();
        final File cacheDir = app.getCacheDir();
        final WeakReference<ApkResourceCatalogActivity> owner = new WeakReference<>(this);
        final Handler ui = mainHandler;
        worker.execute(() -> {
            File copy = null;
            Throwable problem = null;
            ArrayList<ResourceInfo> found = new ArrayList<>();
            Set<String> types = new LinkedHashSet<>();
            int scanned = 0;
            long bytes = 0;
            try {
                copy = File.createTempFile("droidinspect-resource-catalog-", ".apk", cacheDir);
                try (InputStream in = app.getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(copy)) {
                    if (in == null) throw new IOException("Unable to open selected APK.");
                    byte[] buffer = new byte[32768];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        checkCancelled();
                        bytes += n;
                        if (bytes > MAX_APK_BYTES) throw new IOException("APK exceeds the 512 MB resource scan limit.");
                        out.write(buffer, 0, n);
                    }
                    out.flush();
                    out.getFD().sync();
                }
                checkCancelled();
                ApkModule module = ApkModule.loadApkFile(copy);
                module.initializeAndroidFramework((Integer) null);
                if (!module.hasAndroidManifest()) throw new IllegalArgumentException("APK manifest is missing.");
                TableBlock table = module.getTableBlock();
                if (table == null) throw new IllegalArgumentException("APK has no resources.arsc table.");
                java.util.Iterator<ResourceEntry> iterator = table.getResources();
                while (iterator.hasNext() && scanned < MAX_SCAN && found.size() < MAX_STORED) {
                    checkCancelled();
                    ResourceEntry resource = iterator.next();
                    scanned++;
                    if (resource == null) continue;
                    String type = value(resource.getType());
                    String name = value(resource.getName());
                    if (type.isEmpty() || name.isEmpty()) continue;
                    Entry entry = resource.get();
                    boolean isDefault = entry != null && entry.isDefault();
                    String resourceValue = "";
                    if (entry != null) {
                        try { resourceValue = value(entry.getValueAsString()); }
                        catch (Exception ignored) { }
                    }
                    String id = String.format(Locale.ROOT, "%08X", resource.getResourceId());
                    found.add(new ResourceInfo(type, name, resourceValue, id, isDefault));
                    types.add(type);
                }
                checkCancelled();
            } catch (Exception | Error error) {
                problem = error;
                if (copy != null) deleteOwnedFile(copy, cacheDir, problem);
                copy = null;
            }
            final File ready = copy;
            final int scannedFinal = scanned;
            final long bytesFinal = bytes;
            final String errorText = problem == null ? null : describe(problem);
            ui.post(() -> {
                ApkResourceCatalogActivity activity = owner.get();
                if (activity == null || activity.destroyed || activity.isFinishing() || activity.isDestroyed()) {
                    deleteOwnedFileAsync(ready, cacheDir);
                    return;
                }
                if (errorText != null || ready == null) {
                    activity.setBusy(false, "Resource catalog failed.");
                    activity.summary.setText("RESOURCE CATALOG FAILED\n\n" + safe(errorText));
                    return;
                }
                File previous = activity.apkCopy;
                activity.apkCopy = ready;
                deleteOwnedFileAsync(previous, cacheDir);
                activity.all.clear();
                activity.all.addAll(found);
                ArrayList<String> choices = new ArrayList<>();
                choices.add("All types");
                choices.addAll(types);
                ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(activity,
                        android.R.layout.simple_spinner_item, choices);
                typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                activity.typeFilter.setAdapter(typeAdapter);
                activity.typeFilter.setSelection(0);
                activity.summary.setText("APK bytes: " + bytesFinal + "\nResource records scanned: " + scannedFinal
                        + "\nNamed records indexed: " + found.size()
                        + (scannedFinal >= MAX_SCAN || found.size() >= MAX_STORED ? "\nSafety/display cap reached." : ""));
                activity.filter();
                activity.setBusy(false, "Resource ID catalog ready. Original APK was not modified.");
            });
        });
    }

    private void filter() {
        if (adapter == null) return;
        String q = search.getText() == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
        shown.clear();
        labels.clear();
        for (ResourceInfo info : all) {
            if (("All types".equals(selectedType) || selectedType.equals(info.type))
                    && (q.isEmpty() || info.matches(q))) {
                shown.add(info);
                labels.add(info.label() + (info.isDefault ? "  •  default" : ""));
            }
        }
        adapter.notifyDataSetChanged();
        status.setText("Showing " + shown.size() + " of " + all.size() + " indexed resources");
    }

    private static String value(String value) { return value == null ? "" : value; }

    private void setBusy(boolean busyValue, String message) {
        busy = busyValue;
        choose.setEnabled(!busyValue);
        search.setEnabled(!busyValue);
        typeFilter.setEnabled(!busyValue && !all.isEmpty());
        progress.setVisibility(busyValue ? View.VISIBLE : View.GONE);
        status.setText(message);
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("APK resource catalog cancelled because the activity was closed.");
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
                    || !absolute.getName().startsWith("droidinspect-resource-catalog-")
                    || Files.isSymbolicLink(absolute.toPath()))
                throw new IOException("Refusing to delete a resource catalog temporary file outside private cache.");
            Files.deleteIfExists(absolute.toPath());
            if (absolute.exists()) throw new IOException("Resource catalog temporary file remains after deletion.");
        } catch (IOException | RuntimeException cleanupError) {
            IOException reported = cleanupError instanceof IOException ? (IOException) cleanupError
                    : new IOException("Resource catalog cleanup failed.", cleanupError);
            if (primary != null) primary.addSuppressed(reported);
            else Log.e("DroidInspectResourceCatalog", "Temporary file could not be removed: " + file, reported);
        }
    }

    private static void deleteOwnedFileAsync(File file, File cacheDir) {
        if (file == null) return;
        Thread cleanup = new Thread(() -> deleteOwnedFile(file, cacheDir, null), "droidinspect-resource-catalog-file-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        final ExecutorService executor = worker;
        final File cacheDir = getCacheDir();
        final File copy = apkCopy;
        apkCopy = null;
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
        }, "droidinspect-resource-catalog-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }
}
