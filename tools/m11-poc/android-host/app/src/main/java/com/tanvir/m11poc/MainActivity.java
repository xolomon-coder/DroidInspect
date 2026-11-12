package com.tanvir.m11poc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private static final int PICK = 41;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Button select, bundled;
    private TextView status, source;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        select = findViewById(R.id.selectAssembly);
        bundled = findViewById(R.id.runBundledTest);
        status = findViewById(R.id.status);
        source = findViewById(R.id.source);
        select.setOnClickListener(v -> pick());
        bundled.setOnClickListener(v -> runBundledTest());
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
        File references = new File(getFilesDir(), "reference-assemblies/net11.0");
        copyAssets("reference-assemblies/net11.0", references);
        String inputHashBefore = sha256(input);
        long inputBytes = input.length();
        long heapBefore = usedJavaHeap();
        long started = System.nanoTime();
        String result = NativeBridge.decompileFile(input.getAbsolutePath(), references.getAbsolutePath());
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        long heapAfter = usedJavaHeap();
        String inputHashAfter = sha256(input);
        String originalHashAfter = originalUri == null ? inputHashAfter : sha256Uri(originalUri);
        boolean unchanged = inputHashBefore.equals(inputHashAfter)
                && inputHashBefore.equals(originalHashAfter);
        String report = String.format(Locale.US,
                "%s\nInput size: %,d bytes\nDecompilation time: %,d ms\nJava heap change: %,d bytes (native memory excluded)\nSHA-256 before: %s\nSHA-256 after:  %s\nOriginal unchanged: %s",
                label, inputBytes, elapsedMs, heapAfter - heapBefore,
                inputHashBefore, originalHashAfter, unchanged ? "PASS" : "FAIL");
        runOnUiThread(() -> {
            source.setText(result);
            status.setText((result.startsWith("M11 DECOMPILATION ERROR:") ? "DECOMPILATION ERROR\n" : "DECOMPILATION RETURNED SOURCE\n") + report);
        });
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
        if (names == null || names.length == 0) throw new IOException("Reference pack missing from APK");
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

    private void setActionsEnabled(boolean enabled) {
        select.setEnabled(enabled);
        bundled.setEnabled(enabled);
    }

    private void showError(Throwable error) {
        runOnUiThread(() -> status.setText("Test failed: " + error));
    }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
