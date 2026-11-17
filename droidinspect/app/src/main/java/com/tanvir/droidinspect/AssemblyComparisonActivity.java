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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.lang.ref.WeakReference;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Structural .NET assembly comparison. It deliberately does not claim semantic equivalence. */
public final class AssemblyComparisonActivity extends Activity {
    private static final int PICK_LEFT = 6101, PICK_RIGHT = 6102;
    private static final long MAX_INPUT = 512L * 1024L * 1024L;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean destroyed;
    private boolean busy;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Uri leftUri, rightUri;
    private String leftName = "Baseline assembly", rightName = "Candidate assembly";
    private Button leftButton, rightButton, compareButton;
    private TextView status, report;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(0xFF101318);

        TextView title = new TextView(this);
        title.setText("Assembly Comparison");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title);

        leftButton = button("SELECT BASELINE DLL / EXE");
        rightButton = button("SELECT CANDIDATE DLL / EXE");
        compareButton = button("COMPARE ASSEMBLIES");
        compareButton.setEnabled(false);
        root.addView(leftButton);
        root.addView(rightButton);
        root.addView(compareButton);

        status = new TextView(this);
        status.setText("Select two managed assemblies. Original files are read-only.");
        status.setTextColor(0xFF80CBC4);
        status.setPadding(0, dp(8), 0, dp(8));
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        report = new TextView(this);
        report.setTextColor(0xFFE5EAF0);
        report.setTextSize(12);
        report.setTypeface(android.graphics.Typeface.MONOSPACE);
        report.setTextIsSelectable(true);
        report.setPadding(dp(8), dp(8), dp(8), dp(8));
        scroll.addView(report);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        leftButton.setOnClickListener(v -> pick(PICK_LEFT));
        rightButton.setOnClickListener(v -> pick(PICK_RIGHT));
        compareButton.setOnClickListener(v -> compare());
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        return b;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void pick(int request) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, request);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String name = queryName(uri);
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        } catch (SecurityException ignored) { }
        if (request == PICK_LEFT) {
            leftUri = uri;
            leftName = name;
            leftButton.setText("BASELINE: " + name);
        } else if (request == PICK_RIGHT) {
            rightUri = uri;
            rightName = name;
            rightButton.setText("CANDIDATE: " + name);
        }
        compareButton.setEnabled(leftUri != null && rightUri != null);
    }

    private String queryName(Uri uri) {
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int i = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (i >= 0 && cursor.getString(i) != null) return cursor.getString(i);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String name = uri.getLastPathSegment();
        return name == null ? "Selected assembly" : name;
    }

    private void compare() {
        if (busy || leftUri == null || rightUri == null) return;
        final Uri baselineUri = leftUri, candidateUri = rightUri;
        final String baselineName = leftName, candidateName = rightName;
        final Context app = getApplicationContext();
        final File cacheDir = app.getCacheDir();
        final Handler ui = mainHandler;
        final WeakReference<AssemblyComparisonActivity> owner = new WeakReference<>(this);
        busy = true;
        setButtons(false);
        status.setText("Copying and analyzing both assemblies…");
        worker.execute(() -> {
            File baseline = null, candidate = null;
            Throwable problem = null;
            String reportText = null;
            try {
                baseline = copy(app, baselineUri, "droidinspect-compare-left-");
                candidate = copy(app, candidateUri, "droidinspect-compare-right-");
                checkCancelled();
                String baselineMetadata = FileInspectionEngine.inspectPe(baseline, baselineName);
                checkCancelled();
                String candidateMetadata = FileInspectionEngine.inspectPe(candidate, candidateName);
                checkCancelled();
                if (!baselineMetadata.contains(".NET CLI header: PRESENT")
                        || !candidateMetadata.contains(".NET CLI header: PRESENT"))
                    throw new IllegalArgumentException("Both selected files must contain a valid managed .NET CLI header.");
                DotNetIlReader.Result baselineIl = DotNetIlReader.read(baseline);
                checkCancelled();
                DotNetIlReader.Result candidateIl = DotNetIlReader.read(candidate);
                checkCancelled();
                reportText = buildReport(baselineName, candidateName,
                        baselineMetadata, candidateMetadata, baselineIl, candidateIl);
            } catch (Exception | Error error) {
                problem = error;
            } finally {
                deleteOwnedFile(baseline, cacheDir, problem);
                deleteOwnedFile(candidate, cacheDir, problem);
            }
            final String readyReport = reportText;
            final String failure = problem == null ? null : describe(problem);
            ui.post(() -> {
                AssemblyComparisonActivity activity = owner.get();
                if (activity == null || activity.destroyed
                        || activity.isFinishing() || activity.isDestroyed()) return;
                activity.busy = false;
                activity.setButtons(true);
                if (failure != null || readyReport == null) {
                    activity.report.setText("COMPARISON FAILED\n\n" + safe(failure)
                            + "\n\nNo source assembly was modified.");
                    activity.status.setText("Comparison failed.");
                } else {
                    activity.report.setText(readyReport);
                    activity.status.setText("Structural comparison finished. Original assemblies were not modified.");
                }
            });
        });
    }

    private static File copy(Context context, Uri uri, String prefix) throws IOException {
        File output = File.createTempFile(prefix, ".dll", context.getCacheDir());
        long total = 0;
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream stream = new FileOutputStream(output)) {
            if (input == null) throw new IOException("Unable to open selected document.");
            byte[] buffer = new byte[32768];
            int count;
            while ((count = input.read(buffer)) != -1) {
                checkCancelled();
                total += count;
                if (total > MAX_INPUT) throw new IOException("Assembly exceeds the 512 MB comparison limit.");
                stream.write(buffer, 0, count);
            }
            stream.flush();
            stream.getFD().sync();
            return output;
        } catch (IOException | RuntimeException | Error failure) {
            deleteOwnedFile(output, context.getCacheDir(), failure);
            throw failure;
        }
    }

    private void setButtons(boolean enabled) {
        leftButton.setEnabled(enabled);
        rightButton.setEnabled(enabled);
        compareButton.setEnabled(enabled && leftUri != null && rightUri != null);
    }

    private static String buildReport(String leftName, String rightName, String leftMetadata, String rightMetadata,
                               DotNetIlReader.Result leftIl, DotNetIlReader.Result rightIl) 
        StringBuilder out = new StringBuilder();
        out.append("DROIDINSPECT — STRUCTURAL ASSEMBLY COMPARISON\n\n");
        out.append("BASELINE: ").append(leftName).append("\nCANDIDATE: ").append(rightName).append("\n\n");
        appendIdentity(out, leftMetadata, rightMetadata);
        compareSection(out, "ASSEMBLY REFERENCES", leftMetadata, rightMetadata);
        compareSection(out, "TYPES AND MEMBERS", leftMetadata, rightMetadata);
        compareSection(out, "PROPERTIES", leftMetadata, rightMetadata);
        compareSection(out, "EVENTS", leftMetadata, rightMetadata);
        compareSection(out, "MANIFEST RESOURCES", leftMetadata, rightMetadata);
        compareMethods(out, leftIl, rightIl);
        out.append("\nINTERPRETATION\n");
        out.append("This report compares decoded metadata records and available raw IL bodies. ");
        out.append("It does not prove semantic equivalence, behavioral equivalence, or binary compatibility.\n");
        out.append("Items omitted by parser display limits or unsupported metadata encodings may not appear.\n");
        return out.toString();
    }

    private static void appendIdentity(StringBuilder out, String left, String right) {
        out.append("ASSEMBLY IDENTITY\n");
        String a = section(left, "ASSEMBLY IDENTITY", "ASSEMBLY REFERENCES");
        String b = section(right, "ASSEMBLY IDENTITY", "ASSEMBLY REFERENCES");
        if (a.equals(b)) out.append("Identity metadata: unchanged\n");
        else {
            out.append("Identity metadata: DIFFERENT\n");
            out.append("Baseline:\n").append(a).append("\nCandidate:\n").append(b).append('\n');
        }
        out.append('\n');
    }

    private static void compareSection(StringBuilder out, String heading, String left, String right) {
        Set<String> a = sectionEntries(section(left, heading, nextHeading(heading)));
        Set<String> b = sectionEntries(section(right, heading, nextHeading(heading)));
        out.append(AssemblyComparisonDiff.compareSets(heading, a, b));
    }

    private static String nextHeading(String heading) {
        if ("ASSEMBLY REFERENCES".equals(heading)) return "MODULE REFERENCES";
        if ("TYPES AND MEMBERS".equals(heading)) return "PROPERTIES";
        if ("PROPERTIES".equals(heading)) return "EVENTS";
        if ("EVENTS".equals(heading)) return "MANIFEST RESOURCES";
        return "Decode status:";
    }

    private static Set<String> sectionEntries(String block) {
        Set<String> values = new TreeSet<>();
        if (block == null || block.isEmpty()) return values;
        for (String raw : block.split("\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("TypeDef rows:") || line.startsWith("PropertyMap rows:")
                    || line.startsWith("EventMap rows:") || line.startsWith("…")) continue;
            if (line.matches("^\\d+\\. .*")) line = line.replaceFirst("^\\d+\\. ", "");
            values.add(line);
        }
        return values;
    }

    private static void compareMethods(StringBuilder out, DotNetIlReader.Result left, DotNetIlReader.Result right) {
        out.append(AssemblyComparisonDiff.compareMethods(left, right));
    }

    private static String section(String source, String start, String end) {
        if (source == null) return "";
        int from = source.indexOf(start);
        if (from < 0) return "";
        from = source.indexOf('\n', from);
        if (from < 0) return "";
        int to = source.indexOf(end, from + 1);
        return source.substring(from + 1, to < 0 ? source.length() : to).trim();
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("Assembly comparison cancelled because the activity was closed.");
    }

    private static String safe(String value) { return value == null ? "unknown" : value; }

    private static String describe(Throwable error) {
        StringBuilder message = new StringBuilder(error.getClass().getSimpleName())
                .append(": ").append(safe(error.getMessage()));
        for (Throwable suppressed : error.getSuppressed()) {
            message.append("\nCleanup warning: ").append(suppressed.getClass().getSimpleName())
                    .append(": ").append(safe(suppressed.getMessage()));
        }
        return message.toString();
    }

    private static void deleteOwnedFile(File file, File cacheDir, Throwable primary) {
        if (file == null) return;
        try {
            File absolute = file.getAbsoluteFile();
            if (!cacheDir.getAbsoluteFile().equals(absolute.getParentFile())
                    || !absolute.getName().startsWith("droidinspect-compare-")
                    || Files.isSymbolicLink(absolute.toPath()))
                throw new IOException("Refusing to delete a comparison temporary file outside the private cache.");
            Files.deleteIfExists(absolute.toPath());
            if (absolute.exists()) throw new IOException("Comparison temporary file remains after deletion.");
        } catch (IOException | RuntimeException cleanupFailure) {
            IOException reported = cleanupFailure instanceof IOException ? (IOException) cleanupFailure
                    : new IOException("Assembly comparison cleanup failed.", cleanupFailure);
            if (primary != null) primary.addSuppressed(reported);
            else Log.e("DroidInspectComparison", "Temporary file could not be removed: " + file, reported);
        }
    }

    @Override protected void onDestroy() {
        destroyed = true;
        final ExecutorService executor = worker;
        executor.shutdownNow();
        super.onDestroy();
        Thread termination = new Thread(() -> {
            boolean interrupted = false;
            while (true) {
                try {
                    if (executor.awaitTermination(1, TimeUnit.MINUTES)) break;
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }, "droidinspect-comparison-worker-cleanup");
        termination.setDaemon(true);
        termination.start();
    }
}
