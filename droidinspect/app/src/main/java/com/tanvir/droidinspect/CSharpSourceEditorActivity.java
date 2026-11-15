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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSharpSourceEditorActivity extends Activity {
    private static final int REQUEST_IMPORT = 7101;
    private static final int REQUEST_EXPORT = 7102;

    private CSharpSourceWorkspace project;
    private Spinner filesSpinner;
    private EditText editor;
    private TextView title, status;
    private Button saveButton;
    private List<java.io.File> sourceFiles = new ArrayList<>();
    private java.io.File currentFile;
    private boolean loadingText;
    private boolean dirty;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_csharp_source_editor);
        title = findViewById(R.id.sourceEditorTitle);
        status = findViewById(R.id.sourceEditorStatus);
        filesSpinner = findViewById(R.id.sourceEditorFiles);
        editor = findViewById(R.id.sourceEditorText);
        saveButton = findViewById(R.id.sourceEditorSave);
        findViewById(R.id.sourceEditorAdd).setOnClickListener(v -> addFile());
        findViewById(R.id.sourceEditorImport).setOnClickListener(v -> importFiles());
        findViewById(R.id.sourceEditorExport).setOnClickListener(v -> exportProject());
        saveButton.setOnClickListener(v -> saveCurrent());

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
            status.setText("Saved in private app storage. Original decompilation is preserved separately.");
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

    private void refreshFiles(java.io.File select) throws IOException {
        sourceFiles = project.listSourceFiles();
        ArrayList<String> labels = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < sourceFiles.size(); i++) {
            java.io.File file = sourceFiles.get(i);
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

    private void switchFile(java.io.File file) {
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
                        java.io.File created = project.createSourceFile(input.getText().toString());
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
        startActivityForResult(intent, REQUEST_EXPORT);
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
                List<java.io.File> imported = project.importFiles(this, uris);
                refreshFiles(imported.get(0));
                switchFile(imported.get(0));
                status.setText("Imported " + imported.size() + " C# source file(s).");
            } catch (Exception error) {
                status.setText("IMPORT FAILED: " + error.getMessage());
            }
        } else if (requestCode == REQUEST_EXPORT) {
            Uri destination = data.getData();
            if (destination == null) return;
            try (java.io.OutputStream output = getContentResolver().openOutputStream(destination)) {
                if (output == null) throw new IOException("Cannot open export destination.");
                project.exportZip(output);
                Toast.makeText(this, "Source project ZIP exported.", Toast.LENGTH_LONG).show();
            } catch (Exception error) {
                status.setText("EXPORT FAILED: " + error.getMessage());
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
    }

    @Override public void onBackPressed() {
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
