package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CSharpSourceEditorActivity extends Activity {
    private static final int REQUEST_IMPORT = 7101;
    private static final int REQUEST_EXPORT_SOURCE = 7102;
    private static final int REQUEST_EXPORT_DLL = 7103;
    private static final int REQUEST_EXPORT_REPORT = 7104;

    private CSharpSourceWorkspace project;
    private Spinner filesSpinner;
    private EditText editor;
    private TextView title, status, lineNumbers, pageInfo;
    private Button saveButton, compileButton, exportDllButton, exportReportButton, findButton, replaceButton;
    private Button previousPageButton, nextPageButton;
    private PagedSourceDocument currentDocument;
    private int currentPage;
    private List<File> sourceFiles = new ArrayList<>();
    private File currentFile;
    private File lastBuildFile;
    private File lastReportFile;
    private boolean loadingText;
    private boolean dirty;
    // Large reconstructed files can overwhelm EditText syntax spans and a full line-number gutter on phones.
    private static final int LARGE_SOURCE_THRESHOLD_CHARS = 250_000;
    private boolean largeFileMode;
    private boolean compiling;
    private boolean ioBusy;
    private volatile boolean activityDestroyed;
    private final ExecutorService compilerWorker = Executors.newSingleThreadExecutor();
    private final ExecutorService ioWorker = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_csharp_source_editor);
        title = findViewById(R.id.sourceEditorTitle);
        status = findViewById(R.id.sourceEditorStatus);
        filesSpinner = findViewById(R.id.sourceEditorFiles);
        editor = findViewById(R.id.sourceEditorText);
        lineNumbers = findViewById(R.id.sourceEditorLineNumbers);
        pageInfo = findViewById(R.id.sourceEditorPageInfo);
        previousPageButton = findViewById(R.id.sourceEditorPreviousPage);
        nextPageButton = findViewById(R.id.sourceEditorNextPage);
        previousPageButton.setOnClickListener(v -> showSourcePage(currentPage - 1));
        nextPageButton.setOnClickListener(v -> showSourcePage(currentPage + 1));
        findButton = findViewById(R.id.sourceEditorFind);
        replaceButton = findViewById(R.id.sourceEditorReplace);
        saveButton = findViewById(R.id.sourceEditorSave);
        compileButton = findViewById(R.id.sourceEditorCompile);
        exportDllButton = findViewById(R.id.sourceEditorExportDll);
        exportReportButton = findViewById(R.id.sourceEditorExportReport);
        exportReportButton.setOnClickListener(v -> exportCompatibilityReport());
        findButton.setOnClickListener(v -> showFindDialog());
        replaceButton.setOnClickListener(v -> showReplaceDialog());
        editor.setOnScrollChangeListener((view, x, y, oldX, oldY) -> lineNumbers.scrollTo(0, y));

        findViewById(R.id.sourceEditorAdd).setOnClickListener(v -> addFile());
        findViewById(R.id.sourceEditorImport).setOnClickListener(v -> importFiles());
        findViewById(R.id.sourceEditorExport).setOnClickListener(v -> exportProject());
        saveButton.setOnClickListener(v -> saveCurrent());
        compileButton.setOnClickListener(v -> compileProject());
        exportDllButton.setOnClickListener(v -> exportRebuiltDll());

        editor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!loadingText) {
                    dirty = true;
                    updateDirtyState();
                }
            }
            @Override public void afterTextChanged(Editable s) {
                if (!largeFileMode) CSharpSyntaxHighlighter.apply(s);
                updateLineNumbers();
            }
        });

        String id = getIntent().getStringExtra("project_id");
        try {
            project = CSharpSourceWorkspace.open(this, id);
            title.setText(project.getDisplayName());
            refreshFiles(null);
            updateLineNumbers();
            lastBuildFile = project.getBuildOutputFile();
            lastReportFile = new File(lastBuildFile.getParentFile(), "M12-Build-Compatibility-Report.txt");
            exportDllButton.setEnabled(isManagedPe(lastBuildFile));
            exportReportButton.setEnabled(lastReportFile.isFile());
            status.setText("Private source workspace. Original assembly and untouched decompilation are preserved.");
        } catch (Exception error) {
            status.setText("SOURCE PROJECT ERROR: " + error.getMessage());
            setEditorEnabled(false);
        }

        filesSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long rowId) {
                if (position >= 0 && position < sourceFiles.size()) switchFile(sourceFiles.get(position));
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        if (currentFile == null && !sourceFiles.isEmpty()) switchFile(sourceFiles.get(0));
    }

    private void refreshFiles(File select) throws IOException {
        sourceFiles = project.listSourceFiles();
        ArrayList<String> labels = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < sourceFiles.size(); i++) {
            File file = sourceFiles.get(i);
            labels.add(file.getName());
            if (select != null && file.equals(select)) selected = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filesSpinner.setAdapter(adapter);
        if (!sourceFiles.isEmpty()) filesSpinner.setSelection(selected);
        setEditorEnabled(!sourceFiles.isEmpty());
    }

    private void switchFile(File file) {
        if (file == null) return;
        if (currentFile != null && dirty && !saveCurrent()) return;
        loadSourceFileAsync(file);
    }

    private void loadSourceFileAsync(File file) {
        final CSharpSourceWorkspace activeProject = project;
        final WeakReference<CSharpSourceEditorActivity> reference = new WeakReference<>(this);
        final Handler ui = new Handler(Looper.getMainLooper());
        setIoBusy(true, "Loading source into bounded editor pages…");
        ioWorker.execute(() -> {
            PagedSourceDocument loaded = null;
            String failure = null;
            try { loaded = activeProject.readPaged(file); }
            catch (Exception error) { failure = error.getClass().getSimpleName() + ": " + error.getMessage(); }
            final PagedSourceDocument document = loaded;
            final String errorText = failure;
            ui.post(() -> {
                CSharpSourceEditorActivity activity = reference.get();
                if (activity == null || activity.activityDestroyed || activity.isFinishing() || activity.isDestroyed()) return;
                activity.setIoBusy(false, errorText == null ? "Source loaded." : "SOURCE LOAD FAILED: " + errorText);
                if (document == null) return;
                activity.currentFile = file;
                activity.currentDocument = document;
                activity.currentPage = 0;
                activity.largeFileMode = document.length() > LARGE_SOURCE_THRESHOLD_CHARS;
                activity.lineNumbers.setVisibility(activity.largeFileMode ? View.GONE : View.VISIBLE);
                activity.pageInfo.setVisibility(activity.largeFileMode ? View.VISIBLE : View.GONE);
                activity.previousPageButton.setVisibility(activity.largeFileMode ? View.VISIBLE : View.GONE);
                activity.nextPageButton.setVisibility(activity.largeFileMode ? View.VISIBLE : View.GONE);
                activity.renderEditorPage(0, 0);
                activity.dirty = false;
                activity.updateDirtyState();
                activity.status.setText(file.getName() + " — " + activity.sourceFiles.size() + " source file(s)."
                        + (activity.largeFileMode ? " Large file: editing one bounded page at a time." : ""));
            });
        });
    }

    private void renderEditorPage(int page, int selectionOffset) {
        if (currentDocument == null) return;
        currentPage = Math.max(0, Math.min(page, currentDocument.pageCount() - 1));
        String text = largeFileMode ? currentDocument.getPage(currentPage) : currentDocument.toString();
        loadingText = true;
        editor.setText(text);
        int safeSelection = Math.max(0, Math.min(selectionOffset, text.length()));
        editor.setSelection(safeSelection);
        loadingText = false;
        updateLineNumbers();
        if (pageInfo != null) pageInfo.setText("Edit page " + (currentPage + 1) + " / " + currentDocument.pageCount());
        previousPageButton.setEnabled(largeFileMode && currentPage > 0);
        nextPageButton.setEnabled(largeFileMode && currentPage + 1 < currentDocument.pageCount());
    }

    private void syncEditorPage() {
        if (currentDocument == null || editor == null) return;
        if (largeFileMode) currentDocument.setPage(currentPage, editor.getText().toString());
        else currentDocument.setAll(editor.getText().toString());
    }

    private void showSourcePage(int page) {
        if (!largeFileMode || currentDocument == null || page < 0 || page >= currentDocument.pageCount()) return;
        syncEditorPage();
        renderEditorPage(page, 0);
        status.setText("Editing page " + (currentPage + 1) + " of " + currentDocument.pageCount()
                + ". Changes remain in the source project when moving between pages.");
    }

    private boolean saveCurrent() {
        if (project == null || currentFile == null) return false;
        try {
            syncEditorPage();
            project.save(currentFile, currentDocument);
            new ProjectWorkspaceMetadata(project.getSourceRoot().getParentFile())
                    .recordOperation("SOURCE_SAVE", currentFile.getName());
            dirty = false;
            updateDirtyState();
            status.setText("Saved " + currentFile.getName() + ". Original assembly and untouched decompilation remain unchanged.");
            return true;
        } catch (Exception error) {
            status.setText("SAVE FAILED: " + error.getMessage());
            return false;
        }
    }

    private void addFile() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Example: Helpers.cs");
        new AlertDialog.Builder(this)
                .setTitle("Add C# source file")
                .setView(input)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("CREATE", (dialog, which) -> {
                    try {
                        if (currentFile != null && dirty && !saveCurrent()) return;
                        File created = project.createSourceFile(input.getText().toString());
                        refreshFiles(created);
                        switchFile(created);
                    } catch (Exception error) {
                        status.setText("CREATE FAILED: " + error.getMessage());
                    }
                }).show();
    }

    private void importFiles() {
        if (currentFile != null && dirty && !saveCurrent()) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    private void exportProject() {
        if (currentFile != null && dirty && !saveCurrent()) return;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, "DroidInspect-CSharp-Source.zip");
        startActivityForResult(intent, REQUEST_EXPORT_SOURCE);
    }

    private void compileProject() {
        if (compiling || ioBusy) return;
        if (currentFile != null && dirty && !saveCurrent()) return;
        if (project == null || !project.getOriginalAssemblyFile().isFile()) {
            status.setText("The original assembly copy is missing. Recreate this project from the C# decompiler.");
            return;
        }
        if (sourceFiles.isEmpty()) {
            status.setText("There are no C# source files to compile.");
            return;
        }
        compiling = true;
        compileButton.setEnabled(false);
        exportDllButton.setEnabled(false);
        status.setText("Compiling all project .cs files offline with Roslyn. This may take time on the first run…");
        try {
            new ProjectWorkspaceMetadata(project.getSourceRoot().getParentFile())
                    .beginOperation("SOURCE_COMPILE", "Compile all C# project files");
        } catch (IOException metadataError) {
            status.setText("PROJECT HISTORY WARNING: " + metadataError.getMessage());
        }
        final CSharpSourceWorkspace buildProject = project;
        final Context appContext = getApplicationContext();
        final WeakReference<CSharpSourceEditorActivity> reference = new WeakReference<>(this);
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        compilerWorker.execute(() -> {
            String result;
            File output = null;
            boolean verifiedOutput = false;
            try {
                File references = DecompilationWorkspace.ensureReferences(appContext);
                output = buildProject.getBuildOutputFile();
                String loadError = com.tanvir.m12poc.NativeBridge.getLoadError();
                if (loadError != null) {
                    result = "M12 COMPILER UNAVAILABLE\n" + loadError;
                } else {
                    result = com.tanvir.m12poc.NativeBridge.compile(
                            buildProject.getSourceRoot().getAbsolutePath(),
                            output.getAbsolutePath(),
                            references.getAbsolutePath(),
                            buildProject.getOriginalAssemblyFile().getAbsolutePath());
                    if (result == null) result = "M12 COMPILER ERROR: No result was returned.";
                    if (result.startsWith("M12 COMPILATION PASS")) {
                        verifiedOutput = isManagedPe(output);
                        if (!verifiedOutput) {
                            result += "\nJAVA OUTPUT VERIFICATION FAIL: output is not a valid managed .NET assembly.";
                        } else {
                            result += "\nJAVA OUTPUT VERIFICATION PASS: managed PE/CLI assembly reopened; "
                                    + output.length() + " bytes.";
                        }
                    }
                }
            } catch (Throwable error) {
                result = "M12 COMPILER ERROR: " + error.getClass().getSimpleName() + ": "
                        + (error.getMessage() == null ? "No additional details." : error.getMessage());
            }
            try {
                new ProjectWorkspaceMetadata(buildProject.getSourceRoot().getParentFile())
                        .finishOperation("SOURCE_COMPILE",
                                result.startsWith("M12 COMPILATION PASS") && verifiedOutput ? "PASS" : "NOT PASS");
            } catch (IOException ignored) { }
            final String report = result;
            final File built = output;
            final boolean managedOutput = verifiedOutput;
            try {
                if (output != null) {
                    File reportFile = new File(output.getParentFile(), "M12-Build-Compatibility-Report.txt");
                    try (FileOutputStream reportOutput = new FileOutputStream(reportFile)) {
                        reportOutput.write(report.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        reportOutput.flush();
                        reportOutput.getFD().sync();
                    }
                }
            } catch (IOException reportError) {
                // The visible report remains available even if report-file persistence fails.
            }
            mainHandler.post(() -> {
                CSharpSourceEditorActivity activity = reference.get();
                if (activity == null || activity.activityDestroyed
                        || activity.isFinishing() || activity.isDestroyed()) return;
                activity.compiling = false;
                activity.compileButton.setEnabled(true);
                activity.lastBuildFile = built;
                activity.lastReportFile = built == null ? null
                        : new File(built.getParentFile(), "M12-Build-Compatibility-Report.txt");
                activity.exportDllButton.setEnabled(managedOutput);
                activity.exportReportButton.setEnabled(activity.lastReportFile != null
                        && activity.lastReportFile.isFile());
                activity.status.setText(report);
            });
        });
    }

    private static boolean isManagedPe(File file) {
        if (file == null || !file.isFile() || file.length() < 512) return false;
        try {
            DotNetIlReader.Result inspected = DotNetIlReader.read(file);
            return inspected != null && inspected.methods != null && !inspected.methods.isEmpty()
                    && inspected.assembly != null && !"(unknown assembly)".equals(inspected.assembly);
        } catch (Exception error) {
            return false;
        }
    }

    private void exportCompatibilityReport() {
        if (lastReportFile == null || !lastReportFile.isFile()) {
            status.setText("No saved compatibility report is available.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "DroidInspect-DLL-Compatibility-Report.txt");
        startActivityForResult(intent, REQUEST_EXPORT_REPORT);
    }

    private void exportRebuiltDll() {
        if (!isManagedPe(lastBuildFile)) {
            status.setText("No verified rebuilt DLL is available to export.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, "DroidInspect.Rebuilt.dll");
        startActivityForResult(intent, REQUEST_EXPORT_DLL);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQUEST_IMPORT) {
            ArrayList<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++)
                    uris.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) uris.add(data.getData());
            importSourcesAsync(uris);
        } else if (requestCode == REQUEST_EXPORT_SOURCE) {
            Uri destination = data.getData();
            if (destination == null) return;
            exportSourceZipAsync(destination);
        } else if (requestCode == REQUEST_EXPORT_REPORT) {
            Uri destination = data.getData();
            if (destination == null || lastReportFile == null || !lastReportFile.isFile()) return;
            exportFileAsync(destination, lastReportFile, "Compatibility report exported.", "REPORT EXPORT FAILED: ");
        } else if (requestCode == REQUEST_EXPORT_DLL) {
            Uri destination = data.getData();
            if (destination == null || !isManagedPe(lastBuildFile)) return;
            exportFileAsync(destination, lastBuildFile, "Rebuilt DLL exported separately. Original assembly remains unchanged.", "DLL EXPORT FAILED: ");
        }
    }

    private void importSourcesAsync(List<Uri> uris) {
        final CSharpSourceWorkspace activeProject = project;
        final Context appContext = getApplicationContext();
        final WeakReference<CSharpSourceEditorActivity> reference = new WeakReference<>(this);
        final Handler ui = new Handler(Looper.getMainLooper());
        setIoBusy(true, "Importing C# source files in the background…");
        ioWorker.execute(() -> {
            List<File> imported = null;
            String failure = null;
            try { imported = activeProject.importFiles(appContext, uris); }
            catch (Exception error) { failure = error.getClass().getSimpleName() + ": " + error.getMessage(); }
            final List<File> completed = imported;
            final String errorText = failure;
            ui.post(() -> {
                CSharpSourceEditorActivity activity = reference.get();
                if (activity == null || activity.activityDestroyed
                        || activity.isFinishing() || activity.isDestroyed()) return;
                activity.setIoBusy(false, errorText == null
                        ? "Imported " + completed.size() + " C# source file(s)."
                        : "IMPORT FAILED: " + errorText);
                if (completed != null && !completed.isEmpty()) {
                    try {
                        activity.refreshFiles(completed.get(0));
                        activity.switchFile(completed.get(0));
                    } catch (Exception refreshError) {
                        activity.status.setText("IMPORT COMPLETED; REFRESH FAILED: " + refreshError.getMessage());
                    }
                }
            });
        });
    }

    private void exportSourceZipAsync(Uri destination) {
        final CSharpSourceWorkspace activeProject = project;
        final Context appContext = getApplicationContext();
        final WeakReference<CSharpSourceEditorActivity> reference = new WeakReference<>(this);
        final Handler ui = new Handler(Looper.getMainLooper());
        setIoBusy(true, "Exporting C# source project ZIP in the background…");
        ioWorker.execute(() -> {
            String failure = null;
            try (OutputStream output = appContext.getContentResolver().openOutputStream(destination)) {
                if (output == null) throw new IOException("Cannot open export destination.");
                activeProject.exportZip(output);
            } catch (Exception error) {
                failure = error.getClass().getSimpleName() + ": " + error.getMessage();
            }
            final String errorText = failure;
            ui.post(() -> {
                CSharpSourceEditorActivity activity = reference.get();
                if (activity == null || activity.activityDestroyed
                        || activity.isFinishing() || activity.isDestroyed()) return;
                activity.setIoBusy(false, errorText == null
                        ? "Source project ZIP exported."
                        : "EXPORT FAILED: " + errorText);
                if (errorText == null) Toast.makeText(activity, "Source project ZIP exported.", Toast.LENGTH_LONG).show();
            });
        });
    }

    private void exportFileAsync(Uri destination, File source, String success, String failurePrefix) {
        if (source == null || !source.isFile()) {
            status.setText(failurePrefix + "Source file is unavailable.");
            return;
        }
        final Context appContext = getApplicationContext();
        final WeakReference<CSharpSourceEditorActivity> reference = new WeakReference<>(this);
        final Handler ui = new Handler(Looper.getMainLooper());
        setIoBusy(true, "Exporting file in the background…");
        ioWorker.execute(() -> {
            String failure = null;
            try (FileInputStream input = new FileInputStream(source);
                 OutputStream output = appContext.getContentResolver().openOutputStream(destination)) {
                if (output == null) throw new IOException("Cannot open export destination.");
                byte[] buffer = new byte[32768];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    checkIoCancelled();
                    output.write(buffer, 0, count);
                }
                output.flush();
            } catch (Exception error) {
                failure = error.getClass().getSimpleName() + ": " + error.getMessage();
            }
            final String errorText = failure;
            ui.post(() -> {
                CSharpSourceEditorActivity activity = reference.get();
                if (activity == null || activity.activityDestroyed
                        || activity.isFinishing() || activity.isDestroyed()) return;
                activity.setIoBusy(false, errorText == null ? success : failurePrefix + errorText);
                if (errorText == null) Toast.makeText(activity, success, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void setIoBusy(boolean busy, String message) {
        ioBusy = busy;
        status.setText(message);
        editor.setEnabled(!busy);
        if (previousPageButton != null) previousPageButton.setEnabled(!busy && largeFileMode && currentPage > 0);
        if (nextPageButton != null) nextPageButton.setEnabled(!busy && largeFileMode
                && currentDocument != null && currentPage + 1 < currentDocument.pageCount());
        saveButton.setEnabled(!busy && !compiling);
        compileButton.setEnabled(!busy && !compiling);
        findViewById(R.id.sourceEditorAdd).setEnabled(!busy && !compiling);
        findViewById(R.id.sourceEditorImport).setEnabled(!busy && !compiling);
        findViewById(R.id.sourceEditorExport).setEnabled(!busy && !compiling);
        exportDllButton.setEnabled(!busy && isManagedPe(lastBuildFile));
        exportReportButton.setEnabled(!busy && lastReportFile != null && lastReportFile.isFile());
    }

    private static void checkIoCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("Source file operation cancelled because the editor was closed.");
    }

    private void updateLineNumbers() {
        if (lineNumbers == null || editor == null) return;
        if (largeFileMode) {
            lineNumbers.setVisibility(View.GONE);
            return;
        }
        lineNumbers.setVisibility(View.VISIBLE);
        int count = Math.max(1, editor.getLineCount());
        StringBuilder numbers = new StringBuilder(count * 4);
        for (int i = 1; i <= count; i++) {
            if (i > 1) numbers.append('\n');
            numbers.append(i);
        }
        lineNumbers.setText(numbers.toString());
        lineNumbers.scrollTo(0, editor.getScrollY());
    }

    private void showFindDialog() {
        EditText query = new EditText(this);
        query.setSingleLine(true);
        query.setHint("Find text in complete source file");
        new AlertDialog.Builder(this)
                .setTitle("Find in C# source")
                .setView(query)
                .setNegativeButton("CLOSE", null)
                .setPositiveButton("FIND NEXT", (dialog, which) -> {
                    String needle = query.getText().toString();
                    if (needle.isEmpty() || currentDocument == null) return;
                    syncEditorPage();
                    int start = largeFileMode
                            ? currentDocument.getPageStart(currentPage) + Math.max(0, editor.getSelectionEnd())
                            : Math.max(0, editor.getSelectionEnd());
                    int found = currentDocument.find(needle, start, true, true);
                    if (found >= 0) {
                        int page = largeFileMode ? currentDocument.pageForOffset(found) : 0;
                        int local = found - (largeFileMode ? currentDocument.getPageStart(page) : 0);
                        renderEditorPage(page, local);
                        editor.requestFocus();
                        editor.setSelection(local, Math.min(editor.length(), local + needle.length()));
                        status.setText("Found in complete source — character " + (found + 1)
                                + (largeFileMode ? ", page " + (page + 1) + " / " + currentDocument.pageCount() : ""));
                    } else {
                        status.setText("No match found in " + currentFile.getName() + ".");
                    }
                }).show();
    }

    private void showReplaceDialog() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        fields.setPadding(pad, 0, pad, 0);
        EditText find = new EditText(this);
        find.setSingleLine(true);
        find.setHint("Find in complete source");
        EditText replacement = new EditText(this);
        replacement.setSingleLine(true);
        replacement.setHint("Replace with");
        fields.addView(find);
        fields.addView(replacement);
        new AlertDialog.Builder(this)
                .setTitle("Replace in C# source")
                .setView(fields)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("REPLACE NEXT", (dialog, which) -> {
                    String needle = find.getText().toString();
                    if (needle.isEmpty() || currentDocument == null) return;
                    syncEditorPage();
                    int start = largeFileMode
                            ? currentDocument.getPageStart(currentPage) + Math.max(0, editor.getSelectionStart())
                            : Math.max(0, editor.getSelectionStart());
                    int found = currentDocument.find(needle, start, true, true);
                    if (found >= 0) {
                        String value = replacement.getText().toString();
                        currentDocument.replaceRange(found, found + needle.length(), value);
                        int page = largeFileMode ? currentDocument.pageForOffset(found) : 0;
                        int local = found - (largeFileMode ? currentDocument.getPageStart(page) : 0);
                        renderEditorPage(page, Math.min(editor.length(), local + value.length()));
                        editor.requestFocus();
                        editor.setSelection(Math.min(editor.length(), local + value.length()));
                        dirty = true;
                        updateDirtyState();
                    } else {
                        status.setText("No match found in " + currentFile.getName() + ".");
                    }
                })
                .setNeutralButton("REPLACE ALL", (dialog, which) -> {
                    String needle = find.getText().toString();
                    if (needle.isEmpty() || currentDocument == null) return;
                    syncEditorPage();
                    int count = currentDocument.replaceAll(needle, replacement.getText().toString(), true);
                    if (count == 0) {
                        status.setText("No matches found in " + currentFile.getName() + ".");
                        return;
                    }
                    renderEditorPage(largeFileMode ? Math.min(currentPage, currentDocument.pageCount() - 1) : 0, 0);
                    dirty = true;
                    updateDirtyState();
                    status.setText("Replaced " + count + " occurrence(s) across the complete source file.");
                }).show();
    }

    private void updateDirtyState() {
        saveButton.setEnabled(dirty);
        if (currentFile != null)
            title.setText(project.getDisplayName() + (dirty ? "  • unsaved changes" : ""));
    }

    private void setEditorEnabled(boolean enabled) {
        editor.setEnabled(enabled);
        saveButton.setEnabled(false);
        compileButton.setEnabled(enabled && !compiling);
    }

    @Override protected void onDestroy() {
        activityDestroyed = true;
        // Native AOT compilation is synchronous and cannot be safely interrupted; let the worker finish its private output/report writes.
        compilerWorker.shutdown();
        ioWorker.shutdownNow();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (compiling) {
            new AlertDialog.Builder(this)
                    .setMessage("Compilation is running. Leave this screen?")
                    .setNegativeButton("STAY", null)
                    .setPositiveButton("LEAVE", (d, w) -> finish())
                    .show();
            return;
        }
        if (dirty && !saveCurrent()) {
            new AlertDialog.Builder(this)
                    .setMessage("Changes could not be saved. Leave this editor?")
                    .setNegativeButton("STAY", null)
                    .setPositiveButton("LEAVE", (d, w) -> finish())
                    .show();
            return;
        }
        super.onBackPressed();
    }
}
