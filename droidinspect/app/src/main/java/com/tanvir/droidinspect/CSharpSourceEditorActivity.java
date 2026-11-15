package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CSharpSourceEditorActivity extends Activity {
    private static final int REQUEST_IMPORT = 7101;
    private static final int REQUEST_EXPORT_SOURCE = 7102;
    private static final int REQUEST_EXPORT_DLL = 7103;

    private CSharpSourceWorkspace project;
    private Spinner filesSpinner;
    private EditText editor;
    private TextView title, status;
    private Button saveButton, compileButton, exportDllButton;
    private List<File> sourceFiles = new ArrayList<>();
    private File currentFile;
    private File lastBuildFile;
    private boolean loadingText;
    private boolean dirty;
    private boolean compiling;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_csharp_source_editor);
        title = findViewById(R.id.sourceEditorTitle);
        status = findViewById(R.id.sourceEditorStatus);
        filesSpinner = findViewById(R.id.sourceEditorFiles);
        editor = findViewById(R.id.sourceEditorText);
        saveButton = findViewById(R.id.sourceEditorSave);
        compileButton = findViewById(R.id.sourceEditorCompile);
        exportDllButton = findViewById(R.id.sourceEditorExportDll);

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
            @Override public void afterTextChanged(Editable s) { }
        });

        String id = getIntent().getStringExtra("project_id");
        try {
            project = CSharpSourceWorkspace.open(this, id);
            title.setText(project.getDisplayName());
            refreshFiles(null);
            lastBuildFile = project.getBuildOutputFile();
            exportDllButton.setEnabled(isManagedPe(lastBuildFile));
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
        if (currentFile != null && dirty && !saveCurrent()) return;
        try {
            currentFile = file;
            loadingText = true;
            editor.setText(project.read(file));
            editor.setSelection(0);
            loadingText = false;
            dirty = false;
            updateDirtyState();
            status.setText(file.getName() + " — " + sourceFiles.size() + " source file(s).");
        } catch (Exception error) {
            loadingText = false;
            status.setText("Cannot open source file: " + error.getMessage());
        }
    }

    private boolean saveCurrent() {
        if (project == null || currentFile == null) return false;
        try {
            project.save(currentFile, editor.getText().toString());
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
        if (compiling) return;
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
        new Thread(() -> {
            String result;
            File output = null;
            try {
                File references = DecompilationWorkspace.ensureReferences(getApplicationContext());
                output = project.getBuildOutputFile();
                String loadError = com.tanvir.m12poc.NativeBridge.getLoadError();
                if (loadError != null) {
                    result = "M12 COMPILER UNAVAILABLE\n" + loadError;
                } else {
                    result = com.tanvir.m12poc.NativeBridge.compile(
                            project.getSourceRoot().getAbsolutePath(),
                            output.getAbsolutePath(),
                            references.getAbsolutePath(),
                            project.getOriginalAssemblyFile().getAbsolutePath());
                    if (result == null) result = "M12 COMPILER ERROR: No result was returned.";
                    if (result.startsWith("M12 COMPILATION PASS")) {
                        if (!isManagedPe(output)) {
                            result += "\nJAVA OUTPUT VERIFICATION FAIL: output is not a valid managed PE file.";
                        } else {
                            result += "\nJAVA OUTPUT VERIFICATION PASS: managed PE file exists; "
                                    + output.length() + " bytes.";
                        }
                    }
                }
            } catch (Throwable error) {
                result = "M12 COMPILER ERROR: " + error.getClass().getSimpleName() + ": "
                        + (error.getMessage() == null ? "No additional details." : error.getMessage());
            }
            final String report = result;
            final File built = output;
            runOnUiThread(() -> {
                compiling = false;
                compileButton.setEnabled(true);
                lastBuildFile = built;
                exportDllButton.setEnabled(isManagedPe(lastBuildFile));
                status.setText(report);
            });
        }, "DroidInspect-M12-Compiler").start();
    }

    private static boolean isManagedPe(File file) {
        if (file == null || !file.isFile() || file.length() < 512) return false;
        try (FileInputStream input = new FileInputStream(file)) {
            return input.read() == 'M' && input.read() == 'Z';
        } catch (IOException error) {
            return false;
        }
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
            try {
                List<File> imported = project.importFiles(this, uris);
                refreshFiles(imported.get(0));
                switchFile(imported.get(0));
                status.setText("Imported " + imported.size() + " C# source file(s).");
            } catch (Exception error) {
                status.setText("IMPORT FAILED: " + error.getMessage());
            }
        } else if (requestCode == REQUEST_EXPORT_SOURCE) {
            Uri destination = data.getData();
            if (destination == null) return;
            try (OutputStream output = getContentResolver().openOutputStream(destination)) {
                if (output == null) throw new IOException("Cannot open export destination.");
                project.exportZip(output);
                Toast.makeText(this, "Source project ZIP exported.", Toast.LENGTH_LONG).show();
            } catch (Exception error) {
                status.setText("EXPORT FAILED: " + error.getMessage());
            }
        } else if (requestCode == REQUEST_EXPORT_DLL) {
            Uri destination = data.getData();
            if (destination == null || !isManagedPe(lastBuildFile)) return;
            try (FileInputStream input = new FileInputStream(lastBuildFile);
                 OutputStream output = getContentResolver().openOutputStream(destination)) {
                if (output == null) throw new IOException("Cannot open DLL export destination.");
                byte[] buffer = new byte[32768];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                output.flush();
                Toast.makeText(this, "Rebuilt DLL exported separately. Original assembly remains unchanged.", Toast.LENGTH_LONG).show();
            } catch (Exception error) {
                status.setText("DLL EXPORT FAILED: " + error.getMessage());
            }
        }
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
