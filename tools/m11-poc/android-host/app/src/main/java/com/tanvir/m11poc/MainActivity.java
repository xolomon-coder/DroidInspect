package com.tanvir.m11poc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class MainActivity extends Activity {
    private static final int PICK = 41;
    private static final int SOURCE_PAGE_CHARS = 24000;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService memorySampler = Executors.newSingleThreadScheduledExecutor();
    private Button select, bundled, dependencyFull, dependencyMissing;
    private Button findNext, previousPage, nextPage;
    private EditText searchText;
    private TextView status, source, pageInfo;
    private ScrollView sourceScroll;
    private String completeSource = "";
    private int currentPage;
    private int lastSearchIndex = -1;
    private String lastSearchQuery = "";
    private String lastReport = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        select = findViewById(R.id.selectAssembly);
        bundled = findViewById(R.id.runBundledTest);
        dependencyFull = findViewById(R.id.runDependencyTest);
        dependencyMissing = findViewById(R.id.runMissingDependencyTest);
        status = findViewById(R.id.status);
        source = findViewById(R.id.source);
        pageInfo = findViewById(R.id.pageInfo);
        sourceScroll = findViewById(R.id.sourceScroll);
        searchText = findViewById(R.id.searchText);
        findNext = findViewById(R.id.findNext);
        previousPage = findViewById(R.id.previousPage);
        nextPage = findViewById(R.id.nextPage);

        select.setOnClickListener(v -> pick());
        bundled.setOnClickListener(v -> runBundledTest());
        dependencyFull.setOnClickListener(v -> runDependencyTest(false));
        dependencyMissing.setOnClickListener(v -> runDependencyTest(true));
        findNext.setOnClickListener(v -> findNextMatch());
        previousPage.setOnClickListener(v -> showPage(currentPage - 1));
        nextPage.setOnClickListener(v -> showPage(currentPage + 1));
        updatePageControls();

        try {
            Class.forName("com.tanvir.m11poc.NativeBridge");
            status.setText("Native engine loaded. Offline decompilation ready.");
        } catch (Throwable error) {
            status.setText("Native load failed: " + error);
            setActionsEnabled(false);
        }
    }

    private void pick() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK);
    }

    private void runBundledTest() {
        setActionsEnabled(false);
        status.setText("Preparing bundled Newtonsoft.Json 13.0.3 assembly...");
        worker.execute(() -> {
            File input = null;
            try {
                input = File.createTempFile("m11-newtonsoft-", ".dll", getCacheDir());
                try (InputStream in = getAssets().open("test-assemblies/Newtonsoft.Json.dll");
                     OutputStream out = new FileOutputStream(input)) {
                    copy(in, out);
                }
                runDecompilation(input, "Newtonsoft.Json 13.0.3 (bundled test)", null);
            } catch (Throwable error) {
                showError(error);
            } finally {
                if (input != null) input.delete();
                runOnUiThread(() -> setActionsEnabled(true));
            }
        });
    }

    private void runDependencyTest(boolean omitSerilog) {
        setActionsEnabled(false);
        status.setText(omitSerilog
                ? "Preparing missing-reference test (Serilog.dll intentionally omitted)..."
                : "Preparing dependency-resolution test...");
        worker.execute(() -> {
            File work = new File(getCacheDir(), omitSerilog ? "m11-dependency-missing" : "m11-dependency-full");
            try {
                deleteTree(work);
                if (!work.mkdirs()) throw new IOException("Cannot create dependency test directory");
                File input = new File(work, "Serilog.Extensions.Logging.dll");
                try (InputStream in = getAssets().open("test-assemblies/Serilog.Extensions.Logging.dll");
                     OutputStream out = new FileOutputStream(input)) {
                    copy(in, out);
                }
                File refs = new File(work, "references");
                if (!refs.mkdirs()) throw new IOException("Cannot create dependency reference directory");
                copyAssets("dependency-references", refs);
                if (omitSerilog) {
                    File omitted = new File(refs, "Serilog.dll");
                    if (!omitted.delete()) throw new IOException("Could not remove Serilog.dll for missing-reference test");
                }
                String frameworkRefs = new File(getFilesDir(), "reference-assemblies/net11.0").getAbsolutePath();
                String searchPath = frameworkRefs + File.pathSeparator + refs.getAbsolutePath();
                String label = omitSerilog
                        ? "Serilog.Extensions.Logging 9.0.2 — Serilog.dll intentionally missing"
                        : "Serilog.Extensions.Logging 9.0.2 — third-party dependencies available";
                runDecompilation(input, label, null, searchPath);
            } catch (Throwable error) {
                showError(error);
            } finally {
                deleteTree(work);
                runOnUiThread(() -> setActionsEnabled(true));
            }
        });
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        setActionsEnabled(false);
        status.setText("Copying selected assembly...");
        worker.execute(() -> {
            File input = null;
            try {
                input = File.createTempFile("m11-selected-", ".dll", getCacheDir());
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(input)) {
                    if (in == null) throw new IOException("Cannot open selected document");
                    copy(in, out);
                }
                runDecompilation(input, "Selected assembly", uri);
            } catch (Throwable error) {
                showError(error);
            } finally {
                if (input != null) input.delete();
                runOnUiThread(() -> setActionsEnabled(true));
            }
        });
    }

    private void runDecompilation(File input, String label, Uri originalUri) throws Exception {
        String frameworkReferences = ensureFrameworkReferences();
        String bundledDependencies = ensureThirdPartyReferences();
        String references = frameworkReferences + File.pathSeparator + bundledDependencies;
        runDecompilation(input, label, originalUri, references);
    }

    private void runDecompilation(File input, String label, Uri originalUri, String referenceSearchPath) throws Exception {
        runOnUiThread(() -> status.setText("Preparing offline .NET reference assemblies..."));
        ensureFrameworkReferences();
        String inputHashBefore = sha256(input);
        long inputBytes = input.length();
        long heapBefore = usedJavaHeap();
        long pssBefore = Debug.getPss() * 1024L;
        AtomicLong sampledPeakPss = new AtomicLong(pssBefore);
        ScheduledFuture<?> sampler = memorySampler.scheduleAtFixedRate(
                () -> sampledPeakPss.accumulateAndGet(Debug.getPss() * 1024L, Math::max),
                0, 200, TimeUnit.MILLISECONDS);
        long started = System.nanoTime();
        String result;
        try {
            result = NativeBridge.decompileFile(input.getAbsolutePath(), referenceSearchPath);
        } finally {
            sampler.cancel(false);
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        long heapAfter = usedJavaHeap();
        long pssAfter = Debug.getPss() * 1024L;
        sampledPeakPss.accumulateAndGet(pssAfter, Math::max);
        String inputHashAfter = sha256(input);
        String originalHashAfter = originalUri == null ? inputHashAfter : sha256Uri(originalUri);
        boolean unchanged = inputHashBefore.equals(inputHashAfter)
                && inputHashBefore.equals(originalHashAfter);
        String integrityLabel = originalUri == null
                ? "Working-copy unchanged" : "Original document unchanged";
        String report = String.format(Locale.US,
                "%s\nInput size: %,d bytes\nDecompilation time: %,d ms\nJava heap change: %,d bytes\nProcess PSS before: %,d bytes\nProcess PSS peak (200 ms sampling): %,d bytes\nProcess PSS after: %,d bytes\nSHA-256 before: %s\nSHA-256 after:  %s\n%s: %s",
                label, inputBytes, elapsedMs, heapAfter - heapBefore,
                pssBefore, sampledPeakPss.get(), pssAfter,
                inputHashBefore, originalHashAfter, integrityLabel, unchanged ? "PASS" : "FAIL");
        runOnUiThread(() -> {
            completeSource = result == null ? "" : result;
            currentPage = 0;
            lastSearchIndex = -1;
            lastSearchQuery = "";
            renderCurrentPage();
            String state;
            if (completeSource.startsWith("M11 DECOMPILATION ERROR:"))
                state = "DECOMPILATION ERROR";
            else if (completeSource.startsWith("// M11 DEPENDENCY DIAGNOSTICS:"))
                state = "SOURCE RETURNED — DEPENDENCY WARNINGS";
            else
                state = "DECOMPILATION RETURNED SOURCE";
            lastReport = state + "\n" + report + "\nSource characters: " + completeSource.length();
            status.setText(lastReport);
        });
    }

    private void findNextMatch() {
        String query = searchText.getText().toString();
        if (query.trim().isEmpty() || completeSource.isEmpty()) {
            status.setText("Enter text to search in the reconstructed source.");
            return;
        }
        if (!query.equals(lastSearchQuery)) {
            lastSearchQuery = query;
            lastSearchIndex = -1;
        }
        int found = completeSource.indexOf(query, lastSearchIndex + 1);
        if (found < 0 && lastSearchIndex >= 0) found = completeSource.indexOf(query);
        if (found < 0) {
            status.setText("No match found: " + query);
            return;
        }
        lastSearchIndex = found;
        currentPage = found / SOURCE_PAGE_CHARS;
        renderCurrentPage();
        status.setText(lastReport + "\nFound \"" + query + "\" at character " + found
                + " (page " + (currentPage + 1) + ").");
    }

    private void showPage(int page) {
        int pageCount = getPageCount();
        if (page < 0 || page >= pageCount) return;
        currentPage = page;
        renderCurrentPage();
    }

    private int getPageCount() {
        return completeSource.isEmpty() ? 0
                : (completeSource.length() + SOURCE_PAGE_CHARS - 1) / SOURCE_PAGE_CHARS;
    }

    private void renderCurrentPage() {
        int start = currentPage * SOURCE_PAGE_CHARS;
        int end = Math.min(completeSource.length(), start + SOURCE_PAGE_CHARS);
        source.setText(completeSource.isEmpty()
                ? "No source output is available."
                : completeSource.substring(start, end));
        pageInfo.setText("Source page " + (getPageCount() == 0 ? 0 : currentPage + 1)
                + " / " + getPageCount());
        updatePageControls();
        sourceScroll.post(() -> sourceScroll.scrollTo(0, 0));
    }

    private void updatePageControls() {
        int count = getPageCount();
        previousPage.setEnabled(count > 0 && currentPage > 0);
        nextPage.setEnabled(count > 0 && currentPage + 1 < count);
        findNext.setEnabled(count > 0);
    }

    private String ensureFrameworkReferences() throws IOException {
        File references = new File(getFilesDir(), "reference-assemblies/net11.0");
        File marker = new File(references, ".m11-ready");
        if (marker.isFile()) return references.getAbsolutePath();

        deleteTree(references);
        if (!references.mkdirs()) throw new IOException("Cannot create offline .NET reference directory");
        String assetRoot = "reference-assemblies/net11.0";
        String[] names = getAssets().list(assetRoot);
        if (names == null || names.length == 0)
            throw new IOException("Offline .NET reference assemblies are missing from the APK.");

        int copied = 0;
        for (String name : names) {
            if (!name.toLowerCase(Locale.ROOT).endsWith(".dll")) continue;
            try (InputStream input = getAssets().open(assetRoot + "/" + name);
                 OutputStream output = new FileOutputStream(new File(references, name))) {
                copy(input, output);
            }
            copied++;
        }
        if (copied == 0) throw new IOException("No .NET reference DLLs were extracted.");
        try (FileOutputStream output = new FileOutputStream(marker)) {
            output.write(("net11.0:" + copied).getBytes("UTF-8"));
            output.flush();
        }
        return references.getAbsolutePath();
    }

    private String ensureThirdPartyReferences() throws IOException {
        File references = new File(getFilesDir(), "dependency-references");
        File marker = new File(references, ".m11-ready");
        if (marker.isFile()) return references.getAbsolutePath();

        deleteTree(references);
        if (!references.mkdirs()) throw new IOException("Cannot create offline dependency reference directory");
        copyAssets("dependency-references", references);
        try (FileOutputStream output = new FileOutputStream(marker)) {
            output.write("m11-dependencies".getBytes("UTF-8"));
            output.flush();
        }
        return references.getAbsolutePath();
    }

    private long usedJavaHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private String sha256Uri(Uri uri) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IOException("Cannot reopen original document for SHA-256 check");
            byte[] buffer = new byte[32768];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return hex(digest.digest());
    }

    private String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[32768];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return hex(digest.digest());
    }

    private String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) value.append(String.format(Locale.US, "%02x", item & 0xff));
        return value.toString();
    }

    private void copyAssets(String path, File destination) throws IOException {
        String[] existing = destination.list();
        if (destination.isDirectory() && existing != null && existing.length > 0) return;
        if (!destination.exists() && !destination.mkdirs()) throw new IOException("Cannot create reference folder");
        String[] names = getAssets().list(path);
        if (names == null || names.length == 0) throw new IOException("Reference pack missing from APK: " + path);
        for (String name : names) {
            String child = path + "/" + name;
            String[] nested = getAssets().list(child);
            File target = new File(destination, name);
            if (nested != null && nested.length > 0) copyAssets(child, target);
            else try (InputStream in = getAssets().open(child); OutputStream out = new FileOutputStream(target)) {
                copy(in, out);
            }
        }
    }

    private void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[32768];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
    }

    private void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        file.delete();
    }

    private void setActionsEnabled(boolean enabled) {
        select.setEnabled(enabled);
        bundled.setEnabled(enabled);
        dependencyFull.setEnabled(enabled);
        dependencyMissing.setEnabled(enabled);
    }

    private void showError(Throwable error) {
        runOnUiThread(() -> status.setText("Test failed: " + error));
    }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        memorySampler.shutdownNow();
        super.onDestroy();
    }
}
