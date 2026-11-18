package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Raw IL browser plus a narrowly constrained metadata method-name editor. */
public final class DotNetIlActivity extends Activity {
    private static final int REQUEST_SAVE_ASSEMBLY = 6101;
    private static final long MAX_INPUT = 512L * 1024L * 1024L;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ArrayList<DotNetIlReader.MethodInfo> allMethods = new ArrayList<>();
    private final ArrayList<DotNetIlReader.MethodInfo> shownMethods = new ArrayList<>();
    private final ArrayList<String> labels = new ArrayList<>();

    private ArrayAdapter<String> adapter;
    private EditText search;
    private ListView list;
    private TextView report;
    private TextView status;
    private ProgressBar progress;
    private volatile boolean destroyed;
    private boolean busy;
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
        final Context app = getApplicationContext();
        final File cacheDir = app.getCacheDir();
        final WeakReference<DotNetIlActivity> owner = new WeakReference<>(this);
        final Handler ui = mainHandler;
        worker.execute(() -> {
            File copy = null;
            DotNetIlReader.Result parsed = null;
            Throwable problem = null;
            try {
                copy = File.createTempFile("droidinspect-il-", ".bin", cacheDir);
                copyUri(app, uri, copy);
                checkCancelled();
                parsed = DotNetIlReader.read(copy);
                checkCancelled();
            } catch (Exception | Error error) {
                problem = error;
                if (copy != null) deleteOwnedFile(copy, cacheDir, problem);
                copy = null;
            }
            final File ready = copy;
            final DotNetIlReader.Result result = parsed;
            final String errorText = problem == null ? null : describe(problem);
            ui.post(() -> {
                DotNetIlActivity activity = owner.get();
                if (activity == null || activity.destroyed || activity.isFinishing() || activity.isDestroyed()) {
                    deleteOwnedFileAsync(ready, cacheDir);
                    return;
                }
                activity.setBusy(false, errorText == null ? "IL analysis complete." : "IL analysis failed.");
                if (errorText != null || result == null) {
                    activity.report.setText("IL ANALYSIS FAILED\n\n" + safe(errorText)
                            + "\n\nThe source assembly was not modified.");
                    return;
                }
                File previous = activity.sourceCopy;
                activity.sourceCopy = ready;
                deleteOwnedFileAsync(previous, cacheDir);
                activity.allMethods.clear();
                activity.allMethods.addAll(result.methods);
                activity.report.setText(result.report);
                activity.filter(activity.search.getText().toString());
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
                .setNeutralButton("Edit IL / Export", (dialog, which) -> promptIlEdit(method))
                .setPositiveButton("Rename / Export", (dialog, which) -> promptRename(method))
                .show();
    }

    private void promptIlEdit(DotNetIlReader.MethodInfo method) {
        if (method.codeHex == null || method.codeHex.isEmpty()) {
            Toast.makeText(this, "This method has no available IL body.", Toast.LENGTH_LONG).show();
            return;
        }
        EditText input = new EditText(this);
        input.setMinLines(3);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setText(method.codeHex);
        new AlertDialog.Builder(this)
                .setTitle("Edit method IL bytes")
                .setMessage("Enter hexadecimal bytes. This limited editor accepts only same-length sequences of single-byte, operand-free opcodes. Branches, operands, metadata tokens, exception-handler sections, signed assemblies, and unsupported instructions are refused. The original file is never changed.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Validate and export copy",
                        (dialog, which) -> editMethodBody(method, input.getText().toString()))
                .show();
    }

    private void editMethodBody(DotNetIlReader.MethodInfo method, String replacementHex) {
        if (busy) return;
        final File source = sourceCopy;
        if (source == null || !source.isFile()) return;
        setBusy(true, "Validating a same-size IL edit and a new assembly copy…");
        final Context app = getApplicationContext();
        final File cacheDir = app.getCacheDir();
        final WeakReference<DotNetIlActivity> owner = new WeakReference<>(this);
        final Handler ui = mainHandler;
        worker.execute(() -> {
            File output = null;
            Throwable problem = null;
            try {
                output = File.createTempFile("droidinspect-il-edited-", ".dll", cacheDir);
                checkCancelled();
                DotNetIlReader.replaceMethodBody(source, method.row, replacementHex, output);
                checkCancelled();
            } catch (Exception | Error error) {
                problem = error;
                if (output != null) deleteOwnedFile(output, cacheDir, problem);
                output = null;
            }
            final File ready = output;
            final String errorText = problem == null ? null : describe(problem);
            ui.post(() -> {
                DotNetIlActivity activity = owner.get();
                if (activity == null || activity.destroyed || activity.isFinishing() || activity.isDestroyed()) {
                    deleteOwnedFileAsync(ready, cacheDir);
                    return;
                }
                activity.setBusy(false, errorText == null
                        ? "Edited assembly reopened and verified. Choose an export location."
                        : "IL edit refused or failed.");
                if (errorText != null || ready == null) {
                    new AlertDialog.Builder(activity).setTitle("IL edit not applied")
                            .setMessage(safe(errorText))
                            .setPositiveButton("OK", null).show();
                    return;
                }
                activity.replacePendingOutput(ready, cacheDir);
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_TITLE, "DroidInspect-edited-assembly.dll");
                activity.startActivityForResult(intent, REQUEST_SAVE_ASSEMBLY);
            });
        });
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
        if (busy) return;
        final File source = sourceCopy;
        if (source == null || !source.isFile()) return;
        setBusy(true, "Checking rename safety and validating a new assembly copy…");
        final Context app = getApplicationContext();
        final File cacheDir = app.getCacheDir();
        final WeakReference<DotNetIlActivity> owner = new WeakReference<>(this);
        final Handler ui = mainHandler;
        worker.execute(() -> {
            File output = null;
            Throwable problem = null;
            try {
                output = File.createTempFile("droidinspect-renamed-", ".dll", cacheDir);
                checkCancelled();
                DotNetIlReader.renameMethod(source, method.row, newName, output);
                checkCancelled();
            } catch (Exception | Error error) {
                problem = error;
                if (output != null) deleteOwnedFile(output, cacheDir, problem);
                output = null;
            }
            final File ready = output;
            final String errorText = problem == null ? null : describe(problem);
            ui.post(() -> {
                DotNetIlActivity activity = owner.get();
                if (activity == null || activity.destroyed || activity.isFinishing() || activity.isDestroyed()) {
                    deleteOwnedFileAsync(ready, cacheDir);
                    return;
                }
                activity.setBusy(false, errorText == null
                        ? "Modified assembly reopened and verified. Choose an export location." : "Rename refused or failed.");
                if (errorText != null || ready == null) {
                    new AlertDialog.Builder(activity).setTitle("Rename not applied")
                            .setMessage(safe(errorText))
                            .setPositiveButton("OK", null).show();
                    return;
                }
                activity.replacePendingOutput(ready, cacheDir);
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_TITLE, "DroidInspect-modified-assembly.dll");
                activity.startActivityForResult(intent, REQUEST_SAVE_ASSEMBLY);
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
        final File source = pendingOutput;
        if (source == null || !source.isFile()) {
            status.setText("The modified assembly output is no longer available.");
            return;
        }
        setBusy(true, "Exporting verified modified assembly…");
        final Context app = getApplicationContext();
        final File cacheDir = app.getCacheDir();
        final WeakReference<DotNetIlActivity> owner = new WeakReference<>(this);
        final Handler ui = mainHandler;
        worker.execute(() -> {
            Throwable problem = null;
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = app.getContentResolver().openOutputStream(destination, "w")) {
                if (out == null) throw new IOException("The document provider did not create an output stream.");
                byte[] buffer = new byte[32768];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    checkCancelled();
                    out.write(buffer, 0, n);
                }
                out.flush();
            } catch (Exception | Error error) {
                problem = error;
            }
            final String errorText = problem == null ? null : describe(problem);
            ui.post(() -> {
                DotNetIlActivity activity = owner.get();
                if (activity == null || activity.destroyed || activity.isFinishing() || activity.isDestroyed()) return;
                activity.setBusy(false, errorText == null
                        ? "Export complete. Reopen the output to inspect the modified method."
                        : "Export failed: " + errorText);
                activity.deletePendingOutput();
                if (errorText != null) Toast.makeText(activity, errorText, Toast.LENGTH_LONG).show();
            });
        });
    }

    private static void copyUri(Context context, Uri uri, File destination) throws IOException {
        long total = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(destination)) {
            if (in == null) throw new IOException("Android could not open the selected assembly.");
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) != -1) {
                checkCancelled();
                total += n;
                if (total > MAX_INPUT) throw new IOException("Assembly exceeds the 512 MB safety limit.");
                out.write(buffer, 0, n);
            }
            out.flush();
            out.getFD().sync();
        } catch (IOException | RuntimeException | Error failure) {
            deleteOwnedFile(destination, context.getCacheDir(), failure);
            throw failure;
        }
    }

    private void setBusy(boolean busy, String message) {
        this.busy = busy;
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        search.setEnabled(!busy);
        list.setEnabled(!busy);
        status.setText(message);
    }

    private void replacePendingOutput(File ready, File cacheDir) {
        File previous = pendingOutput;
        pendingOutput = ready;
        deleteOwnedFileAsync(previous, cacheDir);
    }

    private void deletePendingOutput() {
        File previous = pendingOutput;
        pendingOutput = null;
        deleteOwnedFileAsync(previous, getCacheDir());
    }

    private static String safe(String value) { return value == null ? "unknown" : value; }

    private static String describe(Throwable error) {
        StringBuilder text = new StringBuilder(error.getClass().getSimpleName()).append(": ").append(safe(error.getMessage()));
        for (Throwable suppressed : error.getSuppressed())
            text.append("\nCleanup warning: ").append(suppressed.getClass().getSimpleName())
                    .append(": ").append(safe(suppressed.getMessage()));
        return text.toString();
    }

    private static void deleteOwnedFile(File file, File cacheDir, Throwable primary) {
        if (file == null) return;
        try {
            File absolute = file.getAbsoluteFile();
            if (!cacheDir.getAbsoluteFile().equals(absolute.getParentFile())
                    || !(absolute.getName().startsWith("droidinspect-il-")
                    || absolute.getName().startsWith("droidinspect-renamed-"))
                    || Files.isSymbolicLink(absolute.toPath()))
                throw new IOException("Refusing to delete an assembly temporary file outside its private cache.");
            Files.deleteIfExists(absolute.toPath());
            if (absolute.exists()) throw new IOException("Assembly temporary file still exists after deletion.");
        } catch (IOException | RuntimeException cleanupError) {
            IOException reported = cleanupError instanceof IOException ? (IOException) cleanupError
                    : new IOException("Assembly temporary-file cleanup failed.", cleanupError);
            if (primary != null) primary.addSuppressed(reported);
            else Log.e("DroidInspectIL", "Temporary file could not be removed: " + file, reported);
        }
    }

    private static void deleteOwnedFileAsync(File file, File cacheDir) {
        if (file == null) return;
        Thread cleanup = new Thread(() -> deleteOwnedFile(file, cacheDir, null), "droidinspect-il-file-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("Assembly operation cancelled because the activity was closed.");
    }

    @Override protected void onDestroy() {
        destroyed = true;
        final ExecutorService executor = worker;
        final File cacheDir = getCacheDir();
        final File source = sourceCopy;
        final File pending = pendingOutput;
        sourceCopy = null;
        pendingOutput = null;
        executor.shutdownNow();
        super.onDestroy();
        Thread cleanup = new Thread(() -> {
            boolean interrupted = false;
            while (true) {
                try {
                    if (executor.awaitTermination(1, TimeUnit.MINUTES)) break;
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            deleteOwnedFile(source, cacheDir, null);
            deleteOwnedFile(pending, cacheDir, null);
            if (interrupted) Thread.currentThread().interrupt();
        }, "droidinspect-il-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }
}
