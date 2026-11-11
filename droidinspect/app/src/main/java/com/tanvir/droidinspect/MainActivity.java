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
    private Button browseDexButton;
    private Button browseResourcesButton;
    private Button browseDotNetButton;
    private Uri selectedUri;
    private boolean canBrowseDex;
    private boolean canBrowseApk;
    private boolean canBrowseDotNet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        reportView = findViewById(R.id.reportView);
        statusView = findViewById(R.id.statusView);
        progressBar = findViewById(R.id.progressBar);
        openButton = findViewById(R.id.openButton);
        diagnosticsButton = findViewById(R.id.diagnosticsButton);
        browseDexButton = findViewById(R.id.browseDexButton);
        browseResourcesButton = findViewById(R.id.browseResourcesButton);
        browseDotNetButton = findViewById(R.id.browseDotNetButton);
        browseDexButton.setEnabled(false);
        browseResourcesButton.setEnabled(false);
        browseDotNetButton.setEnabled(false);

        openButton.setOnClickListener(v -> openDocument());
        diagnosticsButton.setOnClickListener(v -> runDiagnostics());
        browseDexButton.setOnClickListener(v -> {
            if (selectedUri == null || !canBrowseDex) return;
            Intent intent = new Intent(this, DexAnalysisActivity.class);
            intent.putExtra("apk_uri", selectedUri.toString());
            startActivity(intent);
        });
        browseResourcesButton.setOnClickListener(v -> {
            if (selectedUri == null || !canBrowseApk) return;
            Intent intent = new Intent(this, ApkResourceActivity.class);
            intent.putExtra("apk_uri", selectedUri.toString());
            startActivity(intent);
        });
        browseDotNetButton.setOnClickListener(v -> {
            if (selectedUri == null || !canBrowseDotNet) return;
            Intent intent = new Intent(this, DotNetIlActivity.class);
            intent.putExtra("assembly_uri", selectedUri.toString());
            startActivity(intent);
        });
        reportView.setText("Choose an APK, DLL, or EXE file to begin.\n\n"
                + "APK: archive listing, DEX detection, package metadata, manifest/resource analysis and selected manifest editing.\n\n"
                + ".NET DLL/EXE: PE structure, CLI header, runtime version and metadata inspection.");
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
            selectedUri = uri;
            canBrowseDex = false;
            canBrowseApk = false;
            canBrowseDotNet = false;
            browseDexButton.setEnabled(false);
            browseResourcesButton.setEnabled(false);
            browseDotNetButton.setEnabled(false);
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
                boolean isApk = report.startsWith("DROIDINSPECT — APK INSPECTION");
                boolean isManaged = report.startsWith("DROIDINSPECT — PE / .NET INSPECTION") && report.contains(".NET CLI header: PRESENT");
                runOnUiThread(() -> {
                    reportView.setText(report);
                    canBrowseDex = isApk;
                    canBrowseApk = isApk;
                    canBrowseDotNet = isManaged;
                    setBusy(false, "Inspection finished.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    canBrowseDex = false;
                    canBrowseApk = false;
                    canBrowseDotNet = false;
                    reportView.setText("INSPECTION FAILED\n\n" + e.getClass().getSimpleName()
                            + ": " + (e.getMessage() == null ? "No additional details." : e.getMessage())
                            + "\n\nThe original selected file was not modified.");
                    setBusy(false, "Inspection failed.");
                });
            }
        });
    }

    private void runDiagnostics() {
        setBusy(true, "Running runtime diagnostics…");
        worker.execute(() -> {
            StringBuilder report = new StringBuilder();
            report.append("DROIDINSPECT — RUNTIME DIAGNOSTICS\n\n");
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
                if (probe != null) probe.delete();
            }
            report.append("Document picker: available through Android ACTION_OPEN_DOCUMENT\n");
            report.append("External storage permission: not requested (SAF is used)\n");
            report.append("APK ZIP and DEX structural parsers: available\n");
            report.append("ARSCLib manifest/resource engine: packaged in this build\n");
            report.append("PE/CLI metadata and IL browser: available in this build\n");
            report.append("Network required for local inspection: NO\n\n");
            report.append("LIMITS\n");
            report.append("Input copy cap: 512 MB; DEX per-file cap: 128 MB\n");
            report.append("Runtime diagnostics do not prove every parser or edit operation works on every file.");
            String result = report.toString();
            runOnUiThread(() -> {
                reportView.setText(result);
                setBusy(false, "Runtime diagnostics finished.");
            });
        });
    }

    private void setBusy(boolean busy, String status) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        openButton.setEnabled(!busy);
        diagnosticsButton.setEnabled(!busy);
        browseDexButton.setEnabled(!busy && canBrowseDex);
        browseResourcesButton.setEnabled(!busy && canBrowseApk);
        browseDotNetButton.setEnabled(!busy && canBrowseDotNet);
        statusView.setText(status);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
    }
}
