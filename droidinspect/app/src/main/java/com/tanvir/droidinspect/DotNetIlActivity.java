package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Raw IL browser plus a narrowly constrained metadata method-name editor. */
public final class DotNetIlActivity extends Activity {
    private static final int REQUEST_SAVE_ASSEMBLY = 6101;
    private static final long MAX_INPUT = 512L * 1024L * 1024L;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayList<DotNetIlReader.MethodInfo> allMethods = new ArrayList<>();
    private final ArrayList<DotNetIlReader.MethodInfo> shownMethods = new ArrayList<>();
    private final ArrayList<String> labels = new ArrayList<>();

    private ArrayAdapter<String> adapter;
    private EditText search;
    private ListView list;
    private TextView report;
    private TextView status;
    private ProgressBar progress;
    private File sourceCopy;
    private File pendingOutput;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_dotnet_il);
        search = findViewById(R.id.dotnetIlSearch);
        list = findViewById(R.id.dotnetIlList);
        report = findViewById(R.id.dotnetIlReport);
        status = findViewById(R.id.dotnetIlStatus);
        progress = findViewById(R.id.dotnetIlProgress);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> showMethod(shownMethods.get(position)));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable e) { }
        });

        String raw = getIntent().getStringExtra("assembly_uri");
        if (raw == null) {
            status.setText("No assembly document was supplied.");
            return;
        }
        load(Uri.parse(raw));
    }

    private void load(Uri uri) {
        setBusy(true, "Copying assembly and indexing method bodies…");
        worker.execute(() -> {
            String failure = null;
            DotNetIlReader.Result parsed = null;
            try {
                sourceCopy = File.createTempFile("droidinspect-il-", ".bin", getCacheDir());
                copyUri(uri, sourceCopy);
                parsed = DotNetIlReader.read(sourceCopy);
            } catch (Exception e) {
                failure = e.getClass().getSimpleName() + ": " + safe(e.getMessage());
            }
            final String error = failure;
            final DotNetIlReader.Result result = parsed;
            runOnUiThread(() -> {
                setBusy(false, error == null ? "IL analysis complete." : "IL analysis failed.");
                if (error != null) {
                    report.setText("IL ANALYSIS FAILED\n\n" + error + "\n\nThe source assembly was not modified.");
                    return;
                }
                allMethods.clear();
                allMethods.addAll(result.methods);
                report.setText(result.report);
                filter(search.getText().toString());
            });
        });
    }

    private void filter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        shownMethods.clear();
        labels.clear();
        for (DotNetIlReader.MethodInfo method : allMethods) {
            String label = method.label();
            if (q.isEmpty() || label.toLowerCase(Locale.ROOT).contains(q)) {
                shownMethods.add(method);
                labels.add(label);
            }
        }
        adapter.notifyDataSetChanged();
        status.setText("Showing " + shownMethods.size() + " of " + allMethods.size() + " methods");
    }

    private void showMethod(DotNetIlReader.MethodInfo method) {
        new AlertDialog.Builder(this)
                .setTitle(method.owner + " :: " + method.name)
                .setMessage(method.body)
                .setNegativeButton("Close", null)
                .setPositiveButton("Rename / Export", (dialog, which) -> promptRename(method))
                .show();
    }

    private void promptRename(DotNetIlReader.MethodInfo method) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setText(method.name);
        new AlertDialog.Builder(this)
                .setTitle("Rename method metadata")
                .setMessage("Only an ASCII identifier with exactly the same UTF-8 byte length is accepted. Signed files, special-name methods, shared metadata strings, and unsafe cases are refused. IL references are not rewritten.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create modified copy", (dialog, which) -> renameMethod(method, input.getText().toString().trim()))
                .show();
    }

    private void renameMethod(DotNetIlReader.MethodInfo method, String newName) {
        setBusy(true, "Checking rename safety and validating a new assembly copy…");
        worker.execute(() -> {
            File output = null;
            String failure = null;
            try {
                output = File.createTempFile("droidinspect-renamed-", ".dll", getCacheDir());
                DotNetIlReader.renameMethod(sourceCopy, method.row, newName, output);
            } catch (Exception e) {
                failure = e.getClass().getSimpleName() + ": " + safe(e.getMessage());
                if (output != null) output.delete();
                output = null;
            }
            final File ready = output;
            final String error = failure;
            runOnUiThread(() -> {
                setBusy(false, error == null ? "Modified assembly reopened and verified. Choose an export location." : "Rename refused or failed.");
                if (error != null || ready == null) {
                    new AlertDialog.Builder(this).setTitle("Rename not applied")
                            .setMessage(safe(error))
                            .setPositiveButton("OK", null).show();
                    return;
                }
                pendingOutput = ready;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_TITLE, "DroidInspect-renamed-assembly.dll");
                startActivityForResult(intent, REQUEST_SAVE_ASSEMBLY);
            });
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SAVE_ASSEMBLY) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            status.setText("Export cancelled. The source assembly remains unchanged.");
            deletePendingOutput();
            return;
        }
        Uri destination = data.getData();
        setBusy(true, "Exporting verified modified assembly…");
        worker.execute(() -> {
            String failure = null;
            try (InputStream in = new FileInputStream(pendingOutput);
                 OutputStream out = getContentResolver().openOutputStream(destination, "w")) {
                if (out == null) throw new IllegalStateException("The document provider did not create an output stream.");
                byte[] buffer = new byte[32768];
                int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                out.flush();
            } catch (Exception e) {
                failure = e.getClass().getSimpleName() + ": " + safe(e.getMessage());
            }
            final String error = failure;
            runOnUiThread(() -> {
                setBusy(false, error == null
                        ? "Export complete. Reopen the output to inspect the new method name."
                        : "Export failed: " + error);
                deletePendingOutput();
                if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void copyUri(Uri uri, File destination) throws Exception {
        long total = 0;
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(destination)) {
            if (in == null) throw new IllegalStateException("Android could not open the selected assembly.");
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > MAX_INPUT) throw new IllegalArgumentException("Assembly exceeds the 512 MB safety limit.");
                out.write(buffer, 0, n);
            }
        }
    }

    private void setBusy(boolean busy, String message) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        search.setEnabled(!busy);
        list.setEnabled(!busy);
        status.setText(message);
    }

    private void deletePendingOutput() {
        if (pendingOutput != null) {
            pendingOutput.delete();
            pendingOutput = null;
        }
    }

    private static String safe(String value) { return value == null ? "unknown" : value; }

    @Override protected void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
        if (sourceCopy != null) sourceCopy.delete();
        deletePendingOutput();
    }
}
