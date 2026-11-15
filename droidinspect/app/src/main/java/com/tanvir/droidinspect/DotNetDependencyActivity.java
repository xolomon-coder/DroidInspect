package com.tanvir.droidinspect;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DotNetDependencyActivity extends Activity {
    private static final int REQUEST_IMPORT = 8101;
    private static final int REQUEST_EXPORT_REPORT = 8102;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView title, status, report;
    private Button importButton, resolveButton, exportButton;
    private DotNetDependencyWorkspace project;
    private boolean busy;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_dotnet_dependency);
        title = findViewById(R.id.dependencyTitle);
        status = findViewById(R.id.dependencyStatus);
        report = findViewById(R.id.dependencyReport);
        importButton = findViewById(R.id.dependencyImport);
        resolveButton = findViewById(R.id.dependencyResolve);
        exportButton = findViewById(R.id.dependencyExport);

        importButton.setOnClickListener(v -> importLocalDependencies());
        resolveButton.setOnClickListener(v -> resolveOffline());
        exportButton.setOnClickListener(v -> exportReport());
        setControls(false);

        String projectId = getIntent().getStringExtra("project_id");
        String assemblyUri = getIntent().getStringExtra("assembly_uri");
        if (projectId != null) {
            try {
                project = DotNetDependencyWorkspace.open(this, projectId);
                showProjectReady();
            } catch (Exception error) {
                status.setText("DEPENDENCY PROJECT ERROR: " + error.getMessage());
            }
        } else if (assemblyUri != null) {
            createProject(Uri.parse(assemblyUri));
        } else {
            status.setText("No managed assembly or saved dependency project was supplied.");
        }
    }

    private void createProject(Uri uri) {
        busy = true;
        status.setText("Copying selected assembly into a private offline dependency project…");
        new Thread(() -> {
            DotNetDependencyWorkspace created = null;
            String errorText = null;
            try {
                created = DotNetDependencyWorkspace.create(getApplicationContext(), uri);
            } catch (Exception error) {
                errorText = error.getClass().getSimpleName() + ": " + error.getMessage();
            }
            final DotNetDependencyWorkspace result = created;
            final String error = errorText;
            runOnUiThread(() -> {
                busy = false;
                if (result != null) {
                    project = result;
                    showProjectReady();
                } else {
                    status.setText("Could not create dependency project: " + error);
                    setControls(false);
                }
            });
        }, "DroidInspect-M13-Workspace").start();
    }

    private void showProjectReady() {
        title.setText("Dependency analysis — " + project.getDisplayName());
        try {
            List<java.io.File> files = project.listDependencies();
            StringBuilder text = new StringBuilder();
            text.append("Root assembly preserved in private project storage.\n");
            text.append("Local dependencies: ").append(files.size()).append('\n');
            for (java.io.File file : files) text.append(" • ").append(file.getName()).append('\n');
            if (project.getReportFile().isFile()) {
                try (FileInputStream input = new FileInputStream(project.getReportFile())) {
                    byte[] saved = new byte[(int) Math.min(project.getReportFile().length(), 2L * 1024L * 1024L)];
                    int offset = 0;
                    while (offset < saved.length) {
                        int count = input.read(saved, offset, saved.length - offset);
                        if (count < 0) break;
                        offset += count;
                    }
                    report.setText(offset == 0 ? text.toString()
                            : new String(saved, 0, offset, java.nio.charset.StandardCharsets.UTF_8));
                }
                status.setText("Saved dependency report reopened. Local dependencies: " + files.size() + ".");
            } else {
                report.setText(text.toString());
                status.setText("Offline dependency project ready. Import local DLL/EXE dependencies, then resolve.");
            }
            exportButton.setEnabled(project.getReportFile().isFile());
            setControls(true);
        } catch (Exception error) {
            status.setText("Cannot read local dependencies: " + error.getMessage());
            setControls(false);
        }
    }

    private void importLocalDependencies() {
        if (busy || project == null) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/octet-stream", "application/x-msdownload", "application/x-msdos-program"
        });
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    private void resolveOffline() {
        if (busy || project == null) return;
        busy = true;
        setControls(false);
        status.setText("Resolving assembly references offline. Indexing the bundled .NET 11 reference pack and local dependencies…");
        worker.execute(() -> {
            String result;
            try {
                String loadError = com.tanvir.m12poc.NativeBridge.getLoadError();
                if (loadError != null) throw new IOException(loadError);
                java.io.File references = DecompilationWorkspace.ensureReferences(getApplicationContext());
                result = com.tanvir.m12poc.NativeBridge.resolveDependencies(
                        project.getRootAssembly().getAbsolutePath(),
                        project.getDependencyDirectory().getAbsolutePath(),
                        references.getAbsolutePath());
                if (result == null) result = "M13 DEPENDENCY RESOLVER ERROR: No result was returned.";
                project.saveReport(result);
            } catch (Throwable error) {
                result = "DROIDINSPECT — OFFLINE .NET DEPENDENCY RESOLUTION\nRESULT: FAIL\n"
                        + error.getClass().getSimpleName() + ": "
                        + (error.getMessage() == null ? "No additional details." : error.getMessage());
                try { project.saveReport(result); } catch (Exception ignored) { }
            }
            final String output = result;
            runOnUiThread(() -> {
                busy = false;
                report.setText(output);
                status.setText("Offline dependency analysis finished. Original assembly was not modified.");
                setControls(true);
                exportButton.setEnabled(project.getReportFile().isFile());
            });
        });
    }

    private void exportReport() {
        if (project == null || !project.getReportFile().isFile()) {
            status.setText("No saved dependency report is available.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "DroidInspect-Dependency-Resolution-Report.txt");
        startActivityForResult(intent, REQUEST_EXPORT_REPORT);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || project == null) return;
        if (requestCode == REQUEST_IMPORT) {
            ArrayList<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++)
                    uris.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) uris.add(data.getData());
            busy = true;
            setControls(false);
            status.setText("Importing selected local dependency files into private storage…");
            worker.execute(() -> {
                String result;
                try {
                    List<java.io.File> imported = project.importDependencies(getApplicationContext(), uris);
                    result = "Imported " + imported.size() + " local dependency file(s).";
                } catch (Exception error) {
                    result = "DEPENDENCY IMPORT FAILED: " + error.getMessage();
                }
                final String message = result;
                runOnUiThread(() -> {
                    busy = false;
                    showProjectReady();
                    status.setText(message);
                });
            });
        } else if (requestCode == REQUEST_EXPORT_REPORT) {
            Uri destination = data.getData();
            if (destination == null) return;
            busy = true;
            setControls(false);
            worker.execute(() -> {
                String errorText = null;
                try (FileInputStream input = new FileInputStream(project.getReportFile());
                     OutputStream output = getContentResolver().openOutputStream(destination)) {
                    if (output == null) throw new IOException("Cannot open report export destination.");
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    output.flush();
                } catch (Exception error) {
                    errorText = error.getMessage();
                }
                final String error = errorText;
                runOnUiThread(() -> {
                    busy = false;
                    setControls(true);
                    if (error == null) Toast.makeText(this, "Dependency report exported.", Toast.LENGTH_LONG).show();
                    else status.setText("REPORT EXPORT FAILED: " + error);
                });
            });
        }
    }

    private void setControls(boolean ready) {
        importButton.setEnabled(ready && !busy);
        resolveButton.setEnabled(ready && !busy);
        exportButton.setEnabled(ready && !busy && project != null && project.getReportFile().isFile());
    }

    @Override protected void onDestroy() {
        worker.shutdown();
        super.onDestroy();
    }
}
