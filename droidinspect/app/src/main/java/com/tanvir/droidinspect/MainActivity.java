package com.tanvir.droidinspect;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQUEST_OPEN_FILE = 4101;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView reportView;
    private TextView statusView;
    private ProgressBar progressBar;
    private Button openButton;
    private Button diagnosticsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        reportView = findViewById(R.id.reportView);
        statusView = findViewById(R.id.statusView);
        progressBar = findViewById(R.id.progressBar);
        openButton = findViewById(R.id.openButton);
        diagnosticsButton = findViewById(R.id.diagnosticsButton);

        openButton.setOnClickListener(v -> openDocument());
        diagnosticsButton.setOnClickListener(v -> runDiagnostics());
        reportView.setText("Choose an APK, DLL, or EXE file to begin.\n\n"
                + "APK: archive listing, DEX detection, package metadata and declared permissions.\n\n"
                + ".NET DLL/EXE: PE structure, CLI header, runtime version, metadata streams and table counts.");
    }

    private void openDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/vnd.android.package-archive",
                "application/octet-stream",
                "application/x-msdownload",
                "application/x-msdos-program"
        });
        startActivityForResult(intent, REQUEST_OPEN_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            } catch (SecurityException ignored) {
                // Some document providers grant a temporary read permission only.
            }
            inspectSelectedFile(uri);
        }
    }

    private void inspectSelectedFile(Uri uri) {
        setBusy(true, "Reading selected file…");
        worker.execute(() -> {
            try {
                String report = FileInspectionEngine.inspect(getApplicationContext(), uri);
                runOnUiThread(() -> {
                    reportView.setText(report);
                    setBusy(false, "Inspection finished.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    reportView.setText("INSPECTION FAILED\n\n" + e.getClass().getSimpleName()
                            + ": " + (e.getMessage() == null ? "No additional details." : e.getMessage())
                            + "\n\nThe original selected file was not modified.");
                    setBusy(false, "Inspection failed.");
                });
            }
        });
    }

    private void runDiagnostics() {
        setBusy(true, "Running Phase 1 diagnostics…");
        worker.execute(() -> {
            StringBuilder report = new StringBuilder();
            report.append("DROIDINSPECT — PHASE 1 DIAGNOSTICS\n\n");
            report.append("Android API: ").append(android.os.Build.VERSION.SDK_INT).append('\n');
            report.append("Android release: ").append(android.os.Build.VERSION.RELEASE).append('\n');
            report.append("App package: ").append(getPackageName()).append('\n');
            report.append("Cache writable: ");
            File probe = null;
            try {
                probe = File.createTempFile("di-test-", ".tmp", getCacheDir());
                report.append("PASS\n");
            } catch (Exception e) {
                report.append("FAIL — ").append(e.getMessage()).append('\n');
            } finally {
                if (probe != null) {
                    //noinspection ResultOfMethodCallIgnored
                    probe.delete();
                }
            }
            report.append("Document picker: available through Android ACTION_OPEN_DOCUMENT\n");
            report.append("External storage permission: not requested (SAF is used)\n");
            report.append("APK ZIP reader: Java ZipFile API available\n");
            report.append("PE/CLI parser: built-in Java binary reader available\n");
            report.append("Network required for inspection: NO\n");
            report.append("\nLIMITS\n");
            report.append("Current per-file inspection copy limit: 512 MB\n");
            report.append("APK archive display limit: first 5000 entries\n");
            report.append("This diagnostic checks app/runtime prerequisites; it does not prove every APK or .NET assembly parses correctly.");
            String result = report.toString();
            runOnUiThread(() -> {
                reportView.setText(result);
                setBusy(false, "Phase 1 diagnostics finished.");
            });
        });
    }

    private void setBusy(boolean busy, String status) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        openButton.setEnabled(!busy);
        diagnosticsButton.setEnabled(!busy);
        statusView.setText(status);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
    }
}
