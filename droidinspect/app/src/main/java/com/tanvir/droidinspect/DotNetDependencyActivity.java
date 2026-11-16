package com.tanvir.droidinspect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
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
    private volatile boolean destroyed;

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
        final Context appContext = getApplicationContext();
        final Handler ui = new Handler(Looper.getMainLooper());
        final WeakReference<DotNetDependencyActivity> reference = new WeakReference<>(this);
        worker.execute(() -> {
            DotNetDependencyWorkspace created = null;
            String errorText = null;
            try {
                created = DotNetDependencyWorkspace.create(appContext, uri);
            } catch (Exception error) {
                errorText = error.getClass().getSimpleName() + ": " + error.getMessage();
            }
            final DotNetDependencyWorkspace result = created;
            final String error = errorText;
            postToUi(ui, reference, activity -> {
                activity.busy = false;
                if (result != null) {
                    activity.project = result;
                    activity.showProjectReady();
                } else {
                    activity.status.setText("Could not create dependency project: " + error);
                    activity.setControls(false);
                }
            });
        });
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
        try {
            new ProjectWorkspaceMetadata(project.getRootAssembly().getParentFile())
                    .beginOperation("DEPENDENCY_RESOLUTION", "Resolve local references offline");
        } catch (IOException metadataError) {
            status.setText("PROJECT HISTORY WARNING: " + metadataError.getMessage());
        }
        final DotNetDependencyWorkspace activeProject = project;
        final Context appContext = getApplicationContext();
        final Handler ui = new Handler(Looper.getMainLooper());
        final WeakReference<DotNetDependencyActivity> reference = new WeakReference<>(this);
        worker.execute(() -> {
            String result;
            try {
                checkCancelled();
                String loadError = com.tanvir.m12poc.NativeBridge.getLoadError();
                if (loadError != null) throw new IOException(loadError);
                java.io.File references = DecompilationWorkspace.ensureReferences(appContext);
                checkCancelled();
                result = com.tanvir.m12poc.NativeBridge.resolveDependencies(
                        activeProject.getRootAssembly().getAbsolutePath(),
                        activeProject.getDependencyDirectory().getAbsolutePath(),
                        references.getAbsolutePath());
                if (result == null) result = "M13 DEPENDENCY RESOLVER ERROR: No result was returned.";
                activeProject.saveReport(result);
            } catch (InterruptedIOException cancelled) {
                // Keep the in-progress journal marker so the next project open can report recovery.
                return;
            } catch (Throwable error) {
                if (Thread.currentThread().isInterrupted()) return;
                result = "DROIDINSPECT — OFFLINE .NET DEPENDENCY RESOLUTION\nRESULT: FAIL\n"
                        + error.getClass().getSimpleName() + ": "
                        + (error.getMessage() == null ? "No additional details." : error.getMessage());
                try { activeProject.saveReport(result); } catch (Exception ignored) { }
            }
            try {
                new ProjectWorkspaceMetadata(activeProject.getRootAssembly().getParentFile())
                        .finishOperation("DEPENDENCY_RESOLUTION",
                                result.contains("RESULT: FAIL") ? "FAIL" : "COMPLETED");
            } catch (IOException ignored) { }
            final String output = result;
            postToUi(ui, reference, activity -> {
                activity.busy = false;
                activity.report.setText(output);
                activity.status.setText("Offline dependency analysis finished. Original assembly was not modified.");
                activity.setControls(true);
                activity.exportButton.setEnabled(activeProject.getReportFile().isFile());
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
            final DotNetDependencyWorkspace activeProject = project;
            final Context appContext = getApplicationContext();
            final Handler ui = new Handler(Looper.getMainLooper());
            final WeakReference<DotNetDependencyActivity> reference = new WeakReference<>(this);
            worker.execute(() -> {
                String result;
                try {
                    List<java.io.File> imported = activeProject.importDependencies(appContext, uris);
                    new ProjectWorkspaceMetadata(activeProject.getRootAssembly().getParentFile())
                            .recordOperation("DEPENDENCY_IMPORT", imported.size() + " file(s)");
                    result = "Imported " + imported.size() + " local dependency file(s).";
                } catch (Exception error) {
                    result = "DEPENDENCY IMPORT FAILED: " + error.getMessage();
                }
                final String message = result;
                postToUi(ui, reference, activity -> {
                    activity.busy = false;
                    activity.showProjectReady();
                    activity.status.setText(message);
                });
            });
        } else if (requestCode == REQUEST_EXPORT_REPORT) {
            Uri destination = data.getData();
            if (destination == null) return;
            busy = true;
            setControls(false);
            final File reportFile = project.getReportFile();
            final Context appContext = getApplicationContext();
            final Handler ui = new Handler(Looper.getMainLooper());
            final WeakReference<DotNetDependencyActivity> reference = new WeakReference<>(this);
            worker.execute(() -> {
                String errorText = null;
                try (FileInputStream input = new FileInputStream(reportFile);
                     OutputStream output = appContext.getContentResolver().openOutputStream(destination)) {
                    if (output == null) throw new IOException("Cannot open report export destination.");
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) { checkCancelled(); output.write(buffer, 0, count); }
                    output.flush();
                } catch (Exception error) {
                    errorText = error.getMessage();
                }
                final String error = errorText;
                postToUi(ui, reference, activity -> {
                    activity.busy = false;
                    activity.setControls(true);
                    if (error == null) Toast.makeText(activity, "Dependency report exported.", Toast.LENGTH_LONG).show();
                    else activity.status.setText("REPORT EXPORT FAILED: " + error);
                });
            });
        }
    }

    private interface UiAction {
        void run(DotNetDependencyActivity activity);
    }

    private static void postToUi(Handler handler,
                                 WeakReference<DotNetDependencyActivity> reference,
                                 UiAction action) {
        handler.post(() -> {
            DotNetDependencyActivity activity = reference.get();
            if (activity == null || activity.destroyed
                    || activity.isFinishing() || activity.isDestroyed()) return;
            action.run(activity);
        });
    }

    private void setControls(boolean ready) {
        importButton.setEnabled(ready && !busy);
        resolveButton.setEnabled(ready && !busy);
        exportButton.setEnabled(ready && !busy && project != null && project.getReportFile().isFile());
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("Dependency workspace operation cancelled because the activity was closed.");
    }

    @Override protected void onDestroy() {
        destroyed = true;
        worker.shutdownNow();
        super.onDestroy();
    }
}
