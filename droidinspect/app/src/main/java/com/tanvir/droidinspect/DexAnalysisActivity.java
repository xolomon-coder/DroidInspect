package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.zip.ZipFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DexAnalysisActivity extends Activity {
    private static final long MAX_APK_COPY = 512L * 1024L * 1024L;
    private final ArrayList<DexFileParser.ClassInfo> allClasses = new ArrayList<>();
    private final ArrayList<DexFileParser.ClassInfo> shownClasses = new ArrayList<>();
    private final ArrayList<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView summary;
    private TextView status;
    private ProgressBar progress;
    private EditText search;
    private ListView list;
    private Spinner packageSpinner;
    private String selectedPackage = "All packages";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean busy;
    private volatile boolean destroyed;
    private volatile File apkCopy;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_dex_analysis);
        summary = findViewById(R.id.dexSummary);
        status = findViewById(R.id.dexStatus);
        progress = findViewById(R.id.dexProgress);
        search = findViewById(R.id.dexSearch);
        list = findViewById(R.id.dexClassList);
        packageSpinner = findViewById(R.id.dexPackageSpinner);
        packageSpinner.setEnabled(false);
        packageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPackage = parent.getItemAtPosition(position).toString();
                filter(search.getText().toString());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> showClass(shownClasses.get(position)));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable e) { }
        });
        String raw = getIntent().getStringExtra("apk_uri");
        if (raw == null) {
            status.setText("No APK document was supplied.");
            return;
        }
        analyze(Uri.parse(raw));
    }

    private void analyze(Uri uri) {
        if (busy) return;
        setBusy(true, "Copying APK and parsing DEX files…");
        final Context app = getApplicationContext();
        final File cacheDir = app.getCacheDir();
        final WeakReference<DexAnalysisActivity> owner = new WeakReference<>(this);
        final Handler ui = mainHandler;
        worker.execute(() -> {
            File copy = null;
            DexFileParser.Result parsed = null;
            Throwable problem = null;
            try {
                copy = File.createTempFile("droidinspect-apk-", ".apk", cacheDir);
                long total = 0;
                try (InputStream in = app.getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(copy)) {
                    if (in == null) throw new IOException("Android could not open the selected APK.");
                    byte[] buffer = new byte[32768];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        checkCancelled();
                        total += n;
                        if (total > MAX_APK_COPY) throw new IOException("APK exceeds the 512 MB inspection limit.");
                        out.write(buffer, 0, n);
                    }
                    out.flush();
                    out.getFD().sync();
                }
                checkCancelled();
                parsed = DexFileParser.analyzeApk(copy);
                checkCancelled();
            } catch (Exception | Error error) {
                problem = error;
                if (copy != null) deleteOwnedFile(copy, cacheDir, problem);
                copy = null;
            }
            final File ready = copy;
            final DexFileParser.Result result = parsed;
            final String errorText = problem == null ? null : describe(problem);
            ui.post(() -> {
                DexAnalysisActivity activity = owner.get();
                if (activity == null || activity.destroyed || activity.isFinishing() || activity.isDestroyed()) {
                    deleteOwnedFileAsync(ready, cacheDir);
                    return;
                }
                activity.setBusy(false, errorText == null ? "DEX analysis finished." : "DEX analysis failed.");
                if (errorText != null || result == null) {
                    activity.summary.setText("DEX ANALYSIS FAILED\n\n" + safe(errorText)
                            + "\n\nThe original APK was not modified.");
                    return;
                }
                File previous = activity.apkCopy;
                activity.apkCopy = ready;
                deleteOwnedFileAsync(previous, cacheDir);
                activity.allClasses.clear();
                activity.allClasses.addAll(result.classes);
                LinkedHashSet<String> packages = new LinkedHashSet<>();
                packages.add("All packages");
                for (DexFileParser.ClassInfo info : activity.allClasses) packages.add(info.packageName);
                ArrayAdapter<String> packageAdapter = new ArrayAdapter<>(activity,
                        android.R.layout.simple_spinner_item, new ArrayList<>(packages));
                packageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                activity.packageSpinner.setAdapter(packageAdapter);
                activity.packageSpinner.setEnabled(true);
                StringBuilder report = new StringBuilder();
                report.append("DEX files found: ").append(result.dexCount).append('\n');
                report.append("Classes parsed: ").append(result.totalClasses).append('\n');
                report.append("Members indexed: ").append(result.totalMembers).append('\n');
                report.append("Invoke cross-references indexed: ").append(result.totalReferences);
                if (result.referenceLimitReached) report.append(" (global safety cap reached)");
                report.append('\n');
                report.append("Class limit: ").append(result.classLimitReached ? "REACHED" : "not reached").append('\n');
                report.append("Member limit: ").append(result.memberLimitReached ? "REACHED" : "not reached").append("\n\n");
                report.append("DEX FILE RESULTS\n");
                for (String line : result.dexReports) report.append("• ").append(line).append('\n');
                if (!result.warning.isEmpty()) report.append("\nNOTE: ").append(result.warning);
                report.append("\n\nSelect a class to inspect its fields and methods.\n");
                report.append("This view shows DEX metadata, not reconstructed Java source.");
                activity.summary.setText(report.toString());
                activity.filter(activity.search.getText().toString());
            });
        });
    }

    private void filter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        shownClasses.clear();
        rows.clear();
        for (DexFileParser.ClassInfo info : allClasses) {
            boolean packageMatch = "All packages".equals(selectedPackage) || selectedPackage.equals(info.packageName);
            boolean textMatch = q.isEmpty() || info.className.toLowerCase(Locale.ROOT).contains(q)
                    || info.packageName.toLowerCase(Locale.ROOT).contains(q)
                    || contains(info.fields, q) || contains(info.methods, q)
                    || contains(info.references, q);
            boolean match = packageMatch && textMatch;
            if (match) {
                shownClasses.add(info);
                rows.add(info.className + "\n" + info.packageName + "  •  " + info.dexName
                        + "\nFields: " + info.fields.size() + "   Methods: " + info.methods.size()
                        + "   Calls: " + info.references.size());
            }
        }
        adapter.notifyDataSetChanged();
        status.setText("Showing " + shownClasses.size() + " of " + allClasses.size() + " classes");
    }

    private boolean contains(List<String> values, String q) {
        for (String value : values) if (value.toLowerCase(Locale.ROOT).contains(q)) return true;
        return false;
    }

    private void showClass(DexFileParser.ClassInfo info) {
        StringBuilder text = new StringBuilder();
        text.append(info.access).append(info.className).append('\n');
        text.append("DEX: ").append(info.dexName).append('\n');
        text.append("Superclass: ").append(info.superClass).append("\n");
        if (info.membersTruncated) text.append("NOTE: Member display limit reached for this class or APK.\n");
        text.append("\n");
        text.append("FIELDS (").append(info.fields.size()).append(")\n");
        if (info.fields.isEmpty()) text.append("(none)\n");
        for (String field : info.fields) text.append("• ").append(field).append('\n');
        text.append("\nMETHODS (").append(info.methods.size()).append(")\n");
        if (info.methods.isEmpty()) text.append("(none)\n");
        for (String method : info.methods) text.append("• ").append(method).append('\n');
        text.append("\nINVOKE CROSS-REFERENCES (").append(info.references.size()).append(")\n");
        if (info.referencesTruncated) text.append("NOTE: Cross-reference display cap reached for this class or APK.\n");
        if (info.references.isEmpty()) text.append("(none indexed or no supported invoke instructions found)\n");
        for (String reference : info.references) text.append("• ").append(reference).append('\n');
        if (!info.referenceWarnings.isEmpty()) {
            text.append("\nCROSS-REFERENCE WARNINGS\n");
            for (String warning : info.referenceWarnings) text.append("• ").append(warning).append('\n');
        }
        text.append("\nMethod bodies / Java decompilation: not implemented in this milestone build.");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(info.className).setMessage(text.toString())
                .setPositiveButton("Close", null);
        if (!info.references.isEmpty())
            dialog.setNeutralButton("REFERENCES", (d, which) -> showReferences(info));
        dialog.show();
    }

    private void showReferences(DexFileParser.ClassInfo info) {
        ArrayList<String> choices = new ArrayList<>();
        for (String reference : info.references) choices.add(reference);
        new AlertDialog.Builder(this).setTitle("Method references — " + info.className)
                .setItems(choices.toArray(new String[0]), (dialog, which) ->
                        openReferenceTarget(info.references.get(which)))
                .setNegativeButton("CLOSE", null).show();
    }

    private void openReferenceTarget(String reference) {
        int arrow = reference.indexOf("  ->  ");
        if (arrow < 0) {
            new AlertDialog.Builder(this).setMessage("The reference target could not be decoded.")
                    .setPositiveButton("CLOSE", null).show();
            return;
        }
        String target = reference.substring(arrow + 6);
        int separator = target.indexOf("->");
        if (separator <= 0) {
            new AlertDialog.Builder(this).setMessage("The reference target could not be decoded.")
                    .setPositiveButton("CLOSE", null).show();
            return;
        }
        String owner = target.substring(0, separator);
        for (DexFileParser.ClassInfo candidate : allClasses) {
            if (candidate.className.equals(owner)) {
                showClass(candidate);
                return;
            }
        }
        new AlertDialog.Builder(this).setTitle("External / unindexed target")
                .setMessage("Target class: " + owner
                        + "\nThis class is not present in the parsed DEX class index. It may belong to another split, library, or omitted DEX entry.")
                .setPositiveButton("CLOSE", null).show();
    }

    private void setBusy(boolean busyValue, String message) {
        busy = busyValue;
        progress.setVisibility(busyValue ? View.VISIBLE : View.GONE);
        search.setEnabled(!busyValue);
        packageSpinner.setEnabled(!busyValue && !allClasses.isEmpty());
        status.setText(message);
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("DEX analysis cancelled because the activity was closed.");
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
                    || !absolute.getName().startsWith("droidinspect-apk-")
                    || Files.isSymbolicLink(absolute.toPath()))
                throw new IOException("Refusing to delete a DEX input copy outside the private cache.");
            Files.deleteIfExists(absolute.toPath());
            if (absolute.exists()) throw new IOException("DEX input copy remains after deletion.");
        } catch (IOException | RuntimeException cleanupError) {
            IOException reported = cleanupError instanceof IOException ? (IOException) cleanupError
                    : new IOException("DEX temporary-file cleanup failed.", cleanupError);
            if (primary != null) primary.addSuppressed(reported);
            else Log.e("DroidInspectDex", "Temporary APK could not be removed: " + file, reported);
        }
    }

    private static void deleteOwnedFileAsync(File file, File cacheDir) {
        if (file == null) return;
        Thread cleanup = new Thread(() -> deleteOwnedFile(file, cacheDir, null), "droidinspect-dex-file-cleanup");
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
        }, "droidinspect-dex-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }
}
