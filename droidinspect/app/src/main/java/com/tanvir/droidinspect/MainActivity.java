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
    private TextView reportView, statusView;
    private ProgressBar progressBar;
    private Button openButton, diagnosticsButton, browseDexButton, browseResourcesButton;
    private Button browseDotNetButton, decompileCsharpButton, apkSigningButton, openSourceWorkspaceButton;
    private Button analyzeDependenciesButton, openDependencyWorkspaceButton, assemblyExplorerButton;
    private Button assemblyComparisonButton;
    private Button dotNetResourcesButton;
    private Button apkComponentExplorerButton;
    private Button apkSplitSetButton;
    private Button apkResourceCatalogButton;
    private Button apkBuildPipelineButton;
    private Button apkProjectWorkspaceButton;
    private Uri selectedUri;
    private String selectedName = "Selected assembly";
    private boolean canBrowseDex, canBrowseApk, canBrowseDotNet;

    @Override protected void onCreate(Bundle savedInstanceState) {
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
        decompileCsharpButton = findViewById(R.id.decompileCsharpButton);
        apkSigningButton = findViewById(R.id.apkSigningButton);
        openSourceWorkspaceButton = findViewById(R.id.openSourceWorkspaceButton);
        analyzeDependenciesButton = findViewById(R.id.analyzeDependenciesButton);
        openDependencyWorkspaceButton = findViewById(R.id.openDependencyWorkspaceButton);
        assemblyExplorerButton = findViewById(R.id.assemblyExplorerButton);
        assemblyComparisonButton = findViewById(R.id.assemblyComparisonButton);
        dotNetResourcesButton = findViewById(R.id.dotNetResourcesButton);
        apkComponentExplorerButton = findViewById(R.id.apkComponentExplorerButton);
        apkSplitSetButton = findViewById(R.id.apkSplitSetButton);
        apkResourceCatalogButton = findViewById(R.id.apkResourceCatalogButton);
        apkBuildPipelineButton = findViewById(R.id.apkBuildPipelineButton);
        apkProjectWorkspaceButton = findViewById(R.id.apkProjectWorkspaceButton);
        browseDexButton.setEnabled(false);
        browseResourcesButton.setEnabled(false);
        browseDotNetButton.setEnabled(false);
        decompileCsharpButton.setEnabled(false);
        analyzeDependenciesButton.setEnabled(false);

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
        apkSigningButton.setOnClickListener(v -> startActivity(new Intent(this, ApkSigningActivity.class)));
        openSourceWorkspaceButton.setOnClickListener(v -> openLastSourceWorkspace());
        analyzeDependenciesButton.setOnClickListener(v -> openDependencyProject());
        openDependencyWorkspaceButton.setOnClickListener(v -> openLastDependencyProject());
        assemblyExplorerButton.setOnClickListener(v -> openAssemblyExplorer());
        assemblyComparisonButton.setOnClickListener(v -> startActivity(new Intent(this, AssemblyComparisonActivity.class)));
        dotNetResourcesButton.setOnClickListener(v -> startActivity(new Intent(this, DotNetResourceActivity.class)));
        apkComponentExplorerButton.setOnClickListener(v -> startActivity(new Intent(this, ApkComponentExplorerActivity.class)));
        apkSplitSetButton.setOnClickListener(v -> startActivity(new Intent(this, ApkSplitSetActivity.class)));
        apkResourceCatalogButton.setOnClickListener(v -> startActivity(new Intent(this, ApkResourceCatalogActivity.class)));
        apkBuildPipelineButton.setOnClickListener(v -> startActivity(new Intent(this, ApkBuildPipelineActivity.class)));
        apkProjectWorkspaceButton.setOnClickListener(v -> startActivity(new Intent(this, ApkProjectWorkspaceActivity.class)));
        browseDotNetButton.setOnClickListener(v -> {
            if (selectedUri == null || !canBrowseDotNet) return;
            Intent intent = new Intent(this, DotNetIlActivity.class);
            intent.putExtra("assembly_uri", selectedUri.toString());
            startActivity(intent);
        });
        decompileCsharpButton.setOnClickListener(v -> {
            if (selectedUri == null || !canBrowseDotNet) return;
            Intent intent = new Intent(this, CSharpDecompileActivity.class);
            intent.setData(selectedUri);
            intent.putExtra("assembly_name", selectedName);
            startActivity(intent);
        });
        reportView.setText("Choose an APK, DLL, or EXE file to begin.\n\n"
                + "APK: archive listing, DEX detection, package metadata, manifest/resource analysis and selected manifest editing.\n\n"
                + ".NET DLL/EXE: PE structure, CLI header, runtime version and metadata inspection.");
    }

    private void openLastSourceWorkspace() {
        String id = CSharpSourceWorkspace.getLastProjectId(this);
        if (id == null) {
            statusView.setText("No saved C# source workspace is available.");
            updateSourceWorkspaceButton();
            return;
        }
        Intent intent = new Intent(this, CSharpSourceEditorActivity.class);
        intent.putExtra("project_id", id);
        startActivity(intent);
    }

    private void openAssemblyExplorer() {
        if (selectedUri == null || !canBrowseDotNet) return;
        Intent intent = new Intent(this, AssemblyExplorerActivity.class);
        intent.putExtra("assembly_uri", selectedUri.toString());
        startActivity(intent);
    }

    private void openDependencyProject() {
        if (selectedUri == null || !canBrowseDotNet) return;
        Intent intent = new Intent(this, DotNetDependencyActivity.class);
        intent.putExtra("assembly_uri", selectedUri.toString());
        startActivity(intent);
    }

    private void openLastDependencyProject() {
        String id = DotNetDependencyWorkspace.getLastProjectId(this);
        if (id == null) {
            statusView.setText("No saved dependency project is available.");
            updateDependencyWorkspaceButtons();
            return;
        }
        Intent intent = new Intent(this, DotNetDependencyActivity.class);
        intent.putExtra("project_id", id);
        startActivity(intent);
    }

    private void updateDependencyWorkspaceButtons() {
        if (openDependencyWorkspaceButton != null)
            openDependencyWorkspaceButton.setEnabled(DotNetDependencyWorkspace.getLastProjectId(this) != null);
        if (analyzeDependenciesButton != null)
            analyzeDependenciesButton.setEnabled(selectedUri != null && canBrowseDotNet);
        if (assemblyExplorerButton != null)
            assemblyExplorerButton.setEnabled(selectedUri != null && canBrowseDotNet);
    }

    private void updateSourceWorkspaceButton() {
        if (openSourceWorkspaceButton != null)
            openSourceWorkspaceButton.setEnabled(CSharpSourceWorkspace.getLastProjectId(this) != null);
    }

    @Override protected void onResume() {
        super.onResume();
        updateSourceWorkspaceButton();
        updateDependencyWorkspaceButtons();
    }

    private void openDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/vnd.android.package-archive", "application/octet-stream",
                "application/x-msdownload", "application/x-msdos-program"
        });
        startActivityForResult(intent, REQUEST_OPEN_FILE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            selectedUri = uri;
            selectedName = queryName(uri);
            canBrowseDex = canBrowseApk = canBrowseDotNet = false;
            browseDexButton.setEnabled(false);
            browseResourcesButton.setEnabled(false);
            browseDotNetButton.setEnabled(false);
            decompileCsharpButton.setEnabled(false);
            analyzeDependenciesButton.setEnabled(false);
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            } catch (SecurityException ignored) {
                // Some document providers grant temporary read permission only.
            }
            inspectSelectedFile(uri);
        }
    }

    private String queryName(Uri uri) {
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (index >= 0 && cursor.getString(index) != null) return cursor.getString(index);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String segment = uri.getLastPathSegment();
        return segment == null ? "Selected assembly" : segment;
    }

    private void inspectSelectedFile(Uri uri) {
        setBusy(true, "Reading selected file…");
        worker.execute(() -> {
            try {
                String report = FileInspectionEngine.inspect(getApplicationContext(), uri);
                boolean isApk = report.startsWith("DROIDINSPECT — APK INSPECTION");
                boolean isManaged = report.startsWith("DROIDINSPECT — PE / .NET INSPECTION")
                        && report.contains(".NET CLI header: PRESENT");
                runOnUiThread(() -> {
                    reportView.setText(report);
                    canBrowseDex = isApk;
                    canBrowseApk = isApk;
                    canBrowseDotNet = isManaged;
                    updateDependencyWorkspaceButtons();
                    setBusy(false, "Inspection finished.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    canBrowseDex = canBrowseApk = canBrowseDotNet = false;
                    updateDependencyWorkspaceButtons();
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
            report.append("PE/CLI metadata and raw IL browser: available\n");
            report.append("C# decompiler ABI: ").append(NativeBridge.isArm64Supported() ? "ARM64 supported" : "unsupported device ABI").append('\n');
            String compilerLoadError = com.tanvir.m12poc.NativeBridge.getLoadError();
            report.append("Offline Roslyn compiler / dependency resolver: ")
                    .append(compilerLoadError == null ? "ARM64 native payload loaded" : "unavailable — " + compilerLoadError)
                    .append('\n');
            report.append("Network required for local inspection/decompilation/dependency analysis: NO\n\n");
            report.append("LIMITS\nInput copy cap: 512 MB; DEX per-file cap: 128 MB\n");
            report.append("C# source reconstruction is not assembly execution or guaranteed recompilation.");
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
        decompileCsharpButton.setEnabled(!busy && canBrowseDotNet);
        analyzeDependenciesButton.setEnabled(!busy && canBrowseDotNet);
        assemblyExplorerButton.setEnabled(!busy && canBrowseDotNet);
        assemblyComparisonButton.setEnabled(!busy);
        dotNetResourcesButton.setEnabled(!busy);
        apkComponentExplorerButton.setEnabled(!busy);
        apkSplitSetButton.setEnabled(!busy);
        apkResourceCatalogButton.setEnabled(!busy);
        apkBuildPipelineButton.setEnabled(!busy);
        apkProjectWorkspaceButton.setEnabled(!busy);
        openDependencyWorkspaceButton.setEnabled(!busy && DotNetDependencyWorkspace.getLastProjectId(this) != null);
        openSourceWorkspaceButton.setEnabled(!busy && CSharpSourceWorkspace.getLastProjectId(this) != null);
        statusView.setText(status);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
    }
}
