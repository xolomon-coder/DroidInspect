package com.tanvir.droidinspect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.reandroid.apk.ApkModule;
import com.reandroid.archive.InputSource;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.ValueType;
import com.reandroid.arsc.value.Entry;
import com.reandroid.xml.base.XmlSerializable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

/**
 * Binary Android manifest/resource browser with controlled default-string edits.
 * All work is performed against a cache copy; the selected original APK is preserved.
 */
public final class ApkResourceActivity extends Activity {
    private static final int REQUEST_CREATE_APK = 5101;
    private static final long MAX_APK_BYTES = 512L * 1024L * 1024L;
    private static final int MAX_RESOURCE_SCAN = 50000;
    private static final int MAX_STRING_RESOURCES = 1500;
    private static final int MAX_STRING_VALUE_LENGTH = 8192;

    private final java.util.concurrent.ExecutorService worker =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private final ArrayList<ResourceEntry> stringResources = new ArrayList<>();
    private final ArrayList<String> stringResourceLabels = new ArrayList<>();

    private EditText packageName;
    private EditText versionName;
    private EditText versionCode;
    private EditText resourceStringValue;
    private ListView resourceList;
    private ArrayAdapter<String> resourceAdapter;
    private TextView summary;
    private TextView manifestText;
    private TextView status;
    private ProgressBar progress;
    private Button saveButton;

    private volatile boolean destroyed;
    private File sourceCopy;
    private File pendingOutput;
    private ApkModule apkModule;
    private ResourceEntry selectedResource;
    private Uri sourceUri;
    private File sourceFile;
    private boolean returnOutputUri;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_apk_resources);
        packageName = findViewById(R.id.resourcePackageName);
        versionName = findViewById(R.id.resourceVersionName);
        versionCode = findViewById(R.id.resourceVersionCode);
        resourceStringValue = findViewById(R.id.resourceStringValue);
        resourceList = findViewById(R.id.resourceStringList);
        summary = findViewById(R.id.resourceSummary);
        manifestText = findViewById(R.id.resourceManifestText);
        status = findViewById(R.id.resourceStatus);
        progress = findViewById(R.id.resourceProgress);
        saveButton = findViewById(R.id.resourceSaveButton);

        resourceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, stringResourceLabels);
        resourceList.setAdapter(resourceAdapter);
        resourceList.setOnItemClickListener((parent, view, position, id) -> selectStringResource(position));
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(v -> prepareModifiedApk());

        returnOutputUri = getIntent().getBooleanExtra("return_output_uri", false);
        String filePath = getIntent().getStringExtra("apk_file_path");
        if (filePath != null) {
            sourceFile = new File(filePath);
            if (!sourceFile.isFile()) {
                status.setText("Project working APK file is unavailable.");
                return;
            }
            loadApk();
            return;
        }
        String raw = getIntent().getStringExtra("apk_uri");
        if (raw == null) {
            status.setText("No APK document was supplied.");
            return;
        }
        sourceUri = Uri.parse(raw);
        loadApk();
    }

    private void loadApk() {
        setBusy(true, "Copying APK and decoding binary manifest/resources…");
        final Context appContext = getApplicationContext();
        final Uri activeSourceUri = sourceUri;
        final File activeSourceFile = sourceFile;
        final Handler ui = new Handler(Looper.getMainLooper());
        final WeakReference<ApkResourceActivity> owner = new WeakReference<>(this);
        worker.execute(() -> {
            File copy = null;
            ApkModule loadedModule = null;
            String error = null;
            String xml = "";
            String report = "";
            String pkg = "";
            String verName = "";
            String verCode = "";
            ArrayList<ResourceEntry> foundStrings = new ArrayList<>();
            ArrayList<String> foundLabels = new ArrayList<>();
            try {
                checkCancelled();
                copy = File.createTempFile("droidinspect-resource-", ".apk", appContext.getCacheDir());
                long copied = activeSourceFile == null
                        ? copyUri(appContext, activeSourceUri, copy)
                        : copyFile(activeSourceFile, copy);
                checkCancelled();
                loadedModule = ApkModule.loadApkFile(copy);
                TableBlock table = loadedModule.getTableBlock();
                loadedModule.initializeAndroidFramework((Integer) null);
                if (!loadedModule.hasAndroidManifest()) {
                    throw new IllegalArgumentException("The APK does not contain AndroidManifest.xml.");
                }
                AndroidManifestBlock manifest = loadedModule.getAndroidManifest();
                if (manifest == null) throw new IllegalArgumentException("The binary Android manifest could not be decoded.");
                pkg = safe(manifest.getPackageName());
                verName = safe(manifest.getVersionName());
                Integer code = manifest.getVersionCode();
                verCode = code == null ? "" : String.valueOf(code);
                xml = XmlSerializable.toXmlString(manifest, true);

                StringBuilder b = new StringBuilder();
                b.append("APK RESOURCE ANALYSIS\n\n");
                b.append("Copied input: ").append(copied).append(" bytes\n");
                b.append("Manifest: decoded from Android binary XML\n");
                b.append("Package: ").append(pkg).append('\n');
                b.append("Version name: ").append(verName).append('\n');
                b.append("Version code: ").append(verCode).append('\n');
                if (table == null) {
                    b.append("Resource table: not present\n");
                } else {
                    b.append("Resource table: parsed\n");
                    b.append("Resource packages: ").append(table.size()).append('\n');
                    for (int i = 0; i < table.size() && i < 100; i++) {
                        checkCancelled();
                        PackageBlock p = table.get(i);
                        b.append(" • ").append(p == null ? "(unavailable)" : p.getName()).append('\n');
                    }
                    if (table.size() > 100) b.append(" • Remaining package names omitted by display cap.\n");
                    Iterator<ResourceEntry> entries = table.getResources();
                    int scanned = 0;
                    while (entries.hasNext() && scanned < MAX_RESOURCE_SCAN
                            && foundStrings.size() < MAX_STRING_RESOURCES) {
                        checkCancelled();
                        ResourceEntry resource = entries.next();
                        scanned++;
                        if (resource == null || !"string".equals(resource.getType()) || resource.getName() == null) continue;
                        Entry valueEntry = resource.get();
                        if (valueEntry == null || !valueEntry.isDefault() || valueEntry.getResValue() == null
                                || valueEntry.getResValue().getValueType() != ValueType.STRING) continue;
                        foundStrings.add(resource);
                        foundLabels.add(resource.getName() + "  •  0x"
                                + String.format(Locale.ROOT, "%08X", resource.getResourceId()));
                    }
                    b.append("Resource entries scanned: ").append(scanned);
                    if (scanned >= MAX_RESOURCE_SCAN) b.append(" (scan cap reached)");
                    b.append('\n');
                    b.append("Editable named default string values found: ").append(foundStrings.size());
                    if (foundStrings.size() >= MAX_STRING_RESOURCES) b.append(" (display cap reached)");
                    b.append('\n');
                }
                b.append("\nEDITING SAFETY\n");
                b.append("Editable: package name, version name, version code, and one selected named default string resource.\n");
                b.append("Other resource types/configuration variants are read-only in this build.\n");
                b.append("The saved output is unsigned; source signing material is removed.\n");
                b.append("The original selected APK is never overwritten.");
                report = b.toString();
                checkCancelled();
            } catch (Exception failure) {
                error = message(failure);
                closeModule(loadedModule, failure);
                loadedModule = null;
                deleteTemporaryFile(copy, failure);
                copy = null;
                for (Throwable cleanup : failure.getSuppressed())
                    error += "\nCleanup: " + cleanup.getMessage();
            } catch (Error fatal) {
                closeModule(loadedModule, fatal);
                deleteTemporaryFile(copy, fatal);
                throw fatal;
            }
            final File readyCopy = copy;
            final ApkModule readyModule = loadedModule;
            final String fError = error, fXml = xml, fReport = report;
            final String fPkg = pkg, fName = verName, fCode = verCode;
            final ArrayList<ResourceEntry> fStrings = foundStrings;
            final ArrayList<String> fLabels = foundLabels;
            postToActivity(ui, owner, activity -> {
                if (fError != null) {
                    activity.setBusy(false, "APK resource analysis failed.");
                    activity.summary.setText("APK RESOURCE ANALYSIS FAILED\n\n" + fError
                            + "\n\nThe original APK was not modified.");
                    activity.manifestText.setText("");
                    return;
                }
                activity.sourceCopy = readyCopy;
                activity.apkModule = readyModule;
                activity.setBusy(false, "Manifest and resource analysis complete.");
                activity.packageName.setText(fPkg);
                activity.versionName.setText(fName);
                activity.versionCode.setText(fCode);
                activity.summary.setText(fReport);
                activity.manifestText.setText(fXml);
                activity.stringResources.clear();
                activity.stringResources.addAll(fStrings);
                activity.stringResourceLabels.clear();
                activity.stringResourceLabels.addAll(fLabels);
                activity.resourceAdapter.notifyDataSetChanged();
                activity.saveButton.setEnabled(true);
            }, () -> {
                closeModuleQuietly(readyModule);
                deleteQuietly(readyCopy);
            });
        });
    }

    private void selectStringResource(int position) {
        if (position < 0 || position >= stringResources.size()) return;
        selectedResource = stringResources.get(position);
        Entry entry = selectedResource.get();
        if (entry == null || entry.getResValue() == null
                || entry.getResValue().getValueType() != ValueType.STRING) {
            status.setText("Selected resource is not an editable default string.");
            resourceStringValue.setText("");
            return;
        }
        resourceStringValue.setText(entry.getValueAsString());
        status.setText("Selected string resource: " + selectedResource.getName()
                + " (only its default configuration will be changed)");
    }

    private static long copyFile(File source, File destination) throws IOException {
        long total = 0;
        try (InputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) != -1) {
                checkCancelled();
                total += n;
                if (total > MAX_APK_BYTES)
                    throw new IllegalArgumentException("Project APK exceeds the 512 MB editor limit.");
                out.write(buffer, 0, n);
            }
            out.flush();
            return total;
        } catch (IOException | RuntimeException | Error failure) {
            deleteTemporaryFile(destination, failure);
            throw failure;
        }
    }

    private static long copyUri(Context context, Uri uri, File destination) throws IOException {
        long total = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(destination)) {
            if (in == null) throw new IllegalStateException("Android could not open the selected APK.");
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) != -1) {
                checkCancelled();
                total += n;
                if (total > MAX_APK_BYTES)
                    throw new IllegalArgumentException("APK exceeds the 512 MB analysis limit.");
                out.write(buffer, 0, n);
            }
            out.flush();
            return total;
        } catch (IOException | RuntimeException | Error failure) {
            deleteTemporaryFile(destination, failure);
            throw failure;
        }
    }

    private void prepareModifiedApk() {
        final String newPackage = packageName.getText().toString().trim();
        final String newVersionName = versionName.getText().toString().trim();
        final String codeText = versionCode.getText().toString().trim();
        final ResourceEntry resourceToEdit = selectedResource;
        final String newResourceValue = resourceStringValue.getText().toString();
        final ApkModule activeModule = apkModule;
        if (activeModule == null) {
            status.setText("APK resource workspace is not ready.");
            return;
        }
        if (!newPackage.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")) {
            packageName.setError("Enter a valid dotted package identifier.");
            return;
        }
        if (newVersionName.length() > 256 || newVersionName.indexOf('\u0000') >= 0) {
            versionName.setError("Version name must be at most 256 characters.");
            return;
        }
        if (resourceToEdit != null && (newResourceValue.length() > MAX_STRING_VALUE_LENGTH
                || newResourceValue.indexOf('\u0000') >= 0)) {
            resourceStringValue.setError("String resource value must be at most 8192 characters and cannot contain NUL.");
            return;
        }
        final int newVersionCode;
        try {
            newVersionCode = Integer.parseInt(codeText);
            if (newVersionCode < 0) throw new NumberFormatException();
        } catch (Exception e) {
            versionCode.setError("Enter a non-negative 32-bit version code.");
            return;
        }

        setBusy(true, "Applying selected edits and validating a new unsigned APK…");
        final Context appContext = getApplicationContext();
        final Handler ui = new Handler(Looper.getMainLooper());
        final WeakReference<ApkResourceActivity> owner = new WeakReference<>(this);
        worker.execute(() -> {
            File output = null;
            String error = null;
            try {
                checkCancelled();
                AndroidManifestBlock manifest = activeModule.getAndroidManifest();
                manifest.setPackageName(newPackage);
                manifest.setVersionName(newVersionName);
                manifest.setVersionCode(newVersionCode);

                int editedResourceId = 0;
                if (resourceToEdit != null) {
                    editedResourceId = resourceToEdit.getResourceId();
                    Entry valueEntry = resourceToEdit.get();
                    if (valueEntry == null || valueEntry.getResValue() == null
                            || valueEntry.getResValue().getValueType() != ValueType.STRING) {
                        throw new IllegalStateException("Selected string resource changed state; edit refused.");
                    }
                    valueEntry.setValueAsString(newResourceValue);
                }

                InputSource[] sources = activeModule.getInputSources();
                for (InputSource source : sources) {
                    checkCancelled();
                    String alias = source.getAlias();
                    String upper = alias == null ? "" : alias.toUpperCase(Locale.ROOT);
                    if (upper.startsWith("META-INF/") && (upper.endsWith(".SF")
                            || upper.endsWith(".RSA") || upper.endsWith(".DSA")
                            || upper.endsWith(".EC") || "META-INF/MANIFEST.MF".equals(upper))) {
                        activeModule.removeInputSource(alias);
                    }
                }
                activeModule.setApkSignatureBlock(null);

                output = File.createTempFile("droidinspect-modified-", ".apk", appContext.getCacheDir());
                activeModule.writeApk(output);
                checkCancelled();
                try (ZipFile zip = new ZipFile(output)) {
                    if (zip.getEntry("AndroidManifest.xml") == null)
                        throw new IllegalStateException("Written APK is missing AndroidManifest.xml.");
                    if (zip.getEntry("resources.arsc") == null && resourceToEdit != null)
                        throw new IllegalStateException("Written APK is missing resources.arsc after a resource edit.");
                }
                ApkModule verify = ApkModule.loadApkFile(output);
                try {
                    verify.setLoadDefaultFramework(false);
                    AndroidManifestBlock checked = verify.getAndroidManifest();
                    if (checked == null || !newPackage.equals(checked.getPackageName())
                            || !newVersionName.equals(safe(checked.getVersionName()))
                            || checked.getVersionCode() == null || checked.getVersionCode() != newVersionCode) {
                        throw new IllegalStateException("Re-open validation did not match the requested manifest values.");
                    }
                    if (resourceToEdit != null) {
                        TableBlock checkedTable = verify.getTableBlock();
                        ResourceEntry checkedResource = checkedTable == null ? null : checkedTable.getResource(editedResourceId);
                        Entry checkedEntry = checkedResource == null ? null : checkedResource.get();
                        if (checkedEntry == null || checkedEntry.getResValue() == null
                                || checkedEntry.getResValue().getValueType() != ValueType.STRING
                                || !newResourceValue.equals(checkedEntry.getValueAsString())) {
                            throw new IllegalStateException("Re-open validation did not match the edited string resource value.");
                        }
                    }
                } finally {
                    verify.close();
                }
                checkCancelled();
            } catch (Exception failure) {
                error = message(failure);
                if (output != null) {
                    deleteTemporaryFile(output, failure);
                    for (Throwable cleanup : failure.getSuppressed())
                        error += "\nCleanup: " + cleanup.getMessage();
                }
                output = null;
            } catch (Error fatal) {
                deleteTemporaryFile(output, fatal);
                throw fatal;
            }
            final File ready = output;
            final String failure = error;
            postToActivity(ui, owner, activity -> {
                activity.setBusy(false, failure == null
                        ? "Modified APK reopened and validated. Choose where to save it." : "APK edit failed.");
                if (failure != null || ready == null) {
                    activity.summary.append("\n\nEDIT FAILED\n" + safe(failure));
                    activity.saveButton.setEnabled(true);
                    return;
                }
                activity.pendingOutput = ready;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/vnd.android.package-archive");
                intent.putExtra(Intent.EXTRA_TITLE, "DroidInspect-modified-unsigned.apk");
                activity.startActivityForResult(intent, REQUEST_CREATE_APK);
            }, () -> deleteQuietly(ready));
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CREATE_APK) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            status.setText("Save cancelled. The original APK remains unchanged.");
            deletePendingOutput();
            saveButton.setEnabled(true);
            if (returnOutputUri) {
                setResult(RESULT_CANCELED);
                finish();
            }
            return;
        }
        Uri destination = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(destination,
                    data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        } catch (SecurityException ignored) { }
        final File activeOutput = pendingOutput;
        if (activeOutput == null || !activeOutput.isFile()) {
            status.setText("Modified APK temporary output is unavailable.");
            setResult(returnOutputUri ? RESULT_CANCELED : RESULT_OK);
            if (returnOutputUri) finish();
            return;
        }
        setBusy(true, "Exporting validated modified APK…");
        final Context appContext = getApplicationContext();
        final Handler ui = new Handler(Looper.getMainLooper());
        final WeakReference<ApkResourceActivity> owner = new WeakReference<>(this);
        worker.execute(() -> {
            String error = null;
            try (InputStream in = new FileInputStream(activeOutput);
                 OutputStream out = appContext.getContentResolver().openOutputStream(destination, "w")) {
                if (out == null) throw new IllegalStateException("The document provider did not create an output stream.");
                byte[] buffer = new byte[32768];
                int n;
                while ((n = in.read(buffer)) != -1) { checkCancelled(); out.write(buffer, 0, n); }
                out.flush();
            } catch (Exception failure) {
                error = message(failure);
            }
            final String failure = error;
            postToActivity(ui, owner, activity -> {
                activity.setBusy(false, failure == null
                        ? "Export complete. Output is unsigned and needs signing." : "Export failed: " + failure);
                if (failure == null) activity.summary.append("\n\nEXPORT: PASS — modified unsigned APK saved.");
                else activity.summary.append("\n\nEXPORT FAILED: " + failure);
                activity.deletePendingOutput();
                activity.saveButton.setEnabled(true);
                if (activity.returnOutputUri) {
                    Intent result = new Intent();
                    if (failure == null) {
                        result.setData(destination);
                        result.putExtra("exported_apk_uri", destination.toString());
                        activity.setResult(RESULT_OK, result);
                    } else {
                        activity.setResult(RESULT_CANCELED);
                    }
                    activity.finish();
                }
            }, () -> deleteQuietly(activeOutput));
        });
    }

    private void deletePendingOutput() {
        if (pendingOutput != null) {
            pendingOutput.delete();
            pendingOutput = null;
        }
    }

    private void setBusy(boolean busy, String message) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!busy && apkModule != null);
        status.setText(message);
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private static String message(Throwable error) {
        return error.getClass().getSimpleName() + ": "
                + (error.getMessage() == null ? "No details." : error.getMessage());
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("APK operation cancelled because the activity was closed.");
    }

    private interface ActivityCallback { void deliver(ApkResourceActivity activity); }

    private static void postToActivity(Handler handler,
                                       WeakReference<ApkResourceActivity> owner,
                                       ActivityCallback callback) {
        postToActivity(handler, owner, callback, null);
    }

    private static void postToActivity(Handler handler,
                                       WeakReference<ApkResourceActivity> owner,
                                       ActivityCallback callback,
                                       Runnable onDiscard) {
        handler.post(() -> {
            ApkResourceActivity activity = owner.get();
            if (activity == null || activity.destroyed
                    || activity.isFinishing() || activity.isDestroyed()) {
                if (onDiscard != null) {
                    Thread cleanup = new Thread(onDiscard, "droidinspect-resource-discard-cleanup");
                    cleanup.setDaemon(true);
                    cleanup.start();
                }
                return;
            }
            callback.deliver(activity);
        });
    }

    private static void closeModule(ApkModule module, Throwable primaryFailure) {
        if (module == null) return;
        try { module.close(); }
        catch (Exception | Error cleanupError) {
            if (primaryFailure != null) primaryFailure.addSuppressed(cleanupError);
        }
    }

    private static void closeModuleQuietly(ApkModule module) {
        if (module == null) return;
        try { module.close(); } catch (Exception | Error ignored) { }
    }

    private static void deleteTemporaryFile(File file, Throwable primaryFailure) {
        if (file == null) return;
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException | RuntimeException | Error cleanupError) {
            IOException reported = new IOException(
                    "Could not remove temporary APK resource file: " + file.getName(), cleanupError);
            if (primaryFailure != null) primaryFailure.addSuppressed(reported);
            else throw new IllegalStateException(reported);
        }
    }

    private static void deleteQuietly(File file) {
        if (file == null) return;
        try { Files.deleteIfExists(file.toPath()); }
        catch (IOException | RuntimeException ignored) { }
    }

    @Override protected void onDestroy() {
        destroyed = true;
        worker.shutdownNow();
        final java.util.concurrent.ExecutorService activeWorker = worker;
        final ApkModule module = apkModule;
        apkModule = null;
        final File source = sourceCopy;
        sourceCopy = null;
        final File pending = pendingOutput;
        pendingOutput = null;
        super.onDestroy();
        Thread cleanup = new Thread(() -> {
            boolean interrupted = false;
            while (!activeWorker.isTerminated()) {
                try { activeWorker.awaitTermination(1, TimeUnit.DAYS); }
                catch (InterruptedException ignored) { interrupted = true; }
            }
            closeModuleQuietly(module);
            deleteQuietly(source);
            deleteQuietly(pending);
            if (interrupted) Thread.currentThread().interrupt();
        }, "droidinspect-apk-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }
}
