package com.tanvir.droidinspect;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.reandroid.apk.ApkModule;
import com.reandroid.archive.InputSource;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.xml.base.XmlSerializable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.zip.ZipFile;

/**
 * Bounded APK manifest/resource inspection and controlled manifest edits.
 * The source APK is copied into the app cache and is never modified.
 */
public final class ApkResourceActivity extends Activity {
    private static final int REQUEST_CREATE_APK = 5101;
    private static final long MAX_APK_BYTES = 512L * 1024L * 1024L;

    private EditText packageName;
    private EditText versionName;
    private EditText versionCode;
    private TextView summary;
    private TextView manifestText;
    private TextView status;
    private ProgressBar progress;
    private Button saveButton;
    private final java.util.concurrent.ExecutorService worker =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    private File sourceCopy;
    private File pendingOutput;
    private ApkModule apkModule;
    private Uri sourceUri;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_apk_resources);
        packageName = findViewById(R.id.resourcePackageName);
        versionName = findViewById(R.id.resourceVersionName);
        versionCode = findViewById(R.id.resourceVersionCode);
        summary = findViewById(R.id.resourceSummary);
        manifestText = findViewById(R.id.resourceManifestText);
        status = findViewById(R.id.resourceStatus);
        progress = findViewById(R.id.resourceProgress);
        saveButton = findViewById(R.id.resourceSaveButton);
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(v -> prepareModifiedApk());

        String raw = getIntent().getStringExtra("apk_uri");
        if (raw == null) {
            status.setText("No APK document was supplied.");
            return;
        }
        sourceUri = Uri.parse(raw);
        loadApk();
    }

    private void loadApk() {
        setBusy(true, "Copying APK and decoding binary resources…");
        worker.execute(() -> {
            String error = null;
            String xml = "";
            String report = "";
            String pkg = "";
            String verName = "";
            String verCode = "";
            try {
                sourceCopy = File.createTempFile("droidinspect-resource-", ".apk", getCacheDir());
                long copied = copyUri(sourceUri, sourceCopy);
                apkModule = ApkModule.loadApkFile(sourceCopy);
                apkModule.setLoadDefaultFramework(false);
                if (!apkModule.hasAndroidManifest()) {
                    throw new IllegalArgumentException("The APK does not contain AndroidManifest.xml.");
                }
                AndroidManifestBlock manifest = apkModule.getAndroidManifest();
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
                TableBlock table = apkModule.getTableBlock();
                if (table == null) {
                    b.append("Resource table: not present\n");
                } else {
                    b.append("Resource table: parsed\n");
                    b.append("Resource packages: ").append(table.size()).append('\n');
                    for (int i = 0; i < table.size() && i < 100; i++) {
                        PackageBlock p = table.get(i);
                        b.append(" • ").append(p == null ? "(unavailable)" : p.getName()).append('\n');
                    }
                    if (table.size() > 100) b.append(" • Remaining package names omitted by display cap.\n");
                }
                b.append("\nEDITING SAFETY\n");
                b.append("Only package name, version name, and version code are editable in this build.\n");
                b.append("The saved output is unsigned; the original APK signature is removed.\n");
                b.append("Use the later signing milestone/tooling before installation.\n");
                b.append("The selected source APK is never overwritten.");
                report = b.toString();
            } catch (Exception e) {
                error = e.getClass().getSimpleName() + ": " + safe(e.getMessage());
            }
            final String fError = error, fXml = xml, fReport = report;
            final String fPkg = pkg, fName = verName, fCode = verCode;
            runOnUiThread(() -> {
                setBusy(false, fError == null ? "Manifest and resource analysis complete." : "APK resource analysis failed.");
                if (fError != null) {
                    summary.setText("APK RESOURCE ANALYSIS FAILED\n\n" + fError
                            + "\n\nThe original APK was not modified.");
                    manifestText.setText("");
                    return;
                }
                packageName.setText(fPkg);
                versionName.setText(fName);
                versionCode.setText(fCode);
                summary.setText(fReport);
                manifestText.setText(fXml);
                saveButton.setEnabled(true);
            });
        });
    }

    private long copyUri(Uri uri, File destination) throws Exception {
        long total = 0;
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(destination)) {
            if (in == null) throw new IllegalStateException("Android could not open the selected APK.");
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > MAX_APK_BYTES) throw new IllegalArgumentException("APK exceeds the 512 MB analysis limit.");
                out.write(buffer, 0, n);
            }
        }
        return total;
    }

    private void prepareModifiedApk() {
        final String newPackage = packageName.getText().toString().trim();
        final String newVersionName = versionName.getText().toString().trim();
        final String codeText = versionCode.getText().toString().trim();
        if (!newPackage.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")) {
            packageName.setError("Enter a valid dotted package identifier.");
            return;
        }
        if (newVersionName.length() > 256 || newVersionName.indexOf('\u0000') >= 0) {
            versionName.setError("Version name must be at most 256 characters.");
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
        setBusy(true, "Applying manifest edits and validating a new unsigned APK…");
        saveButton.setEnabled(false);
        worker.execute(() -> {
            File output = null;
            String error = null;
            try {
                AndroidManifestBlock manifest = apkModule.getAndroidManifest();
                manifest.setPackageName(newPackage);
                manifest.setVersionName(newVersionName);
                manifest.setVersionCode(newVersionCode);

                // Any content change invalidates APK signing material. Remove both
                // ZIP v1 signature files and the APK Signing Block before writing.
                InputSource[] sources = apkModule.getInputSources();
                for (InputSource source : sources) {
                    String alias = source.getAlias();
                    String upper = alias == null ? "" : alias.toUpperCase(Locale.ROOT);
                    if (upper.startsWith("META-INF/") && (upper.endsWith(".SF")
                            || upper.endsWith(".RSA") || upper.endsWith(".DSA")
                            || upper.endsWith(".EC") || "META-INF/MANIFEST.MF".equals(upper))) {
                        apkModule.removeInputSource(alias);
                    }
                }
                apkModule.setApkSignatureBlock(null);

                output = File.createTempFile("droidinspect-modified-", ".apk", getCacheDir());
                apkModule.writeApk(output);

                // Re-open the serialized file and verify the edited values before export.
                try (ZipFile zip = new ZipFile(output)) {
                    if (zip.getEntry("AndroidManifest.xml") == null) {
                        throw new IllegalStateException("Written APK is missing AndroidManifest.xml.");
                    }
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
                } finally {
                    verify.close();
                }
            } catch (Exception e) {
                error = e.getClass().getSimpleName() + ": " + safe(e.getMessage());
                if (output != null) output.delete();
                output = null;
            }
            final File ready = output;
            final String failure = error;
            runOnUiThread(() -> {
                setBusy(false, failure == null ? "Modified APK validated. Choose where to save it." : "Manifest edit failed.");
                if (failure != null || ready == null) {
                    summary.append("\n\nEDIT FAILED\n" + safe(failure));
                    saveButton.setEnabled(true);
                    return;
                }
                pendingOutput = ready;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/vnd.android.package-archive");
                intent.putExtra(Intent.EXTRA_TITLE, "DroidInspect-modified-unsigned.apk");
                startActivityForResult(intent, REQUEST_CREATE_APK);
            });
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CREATE_APK) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            status.setText("Save cancelled. The original APK remains unchanged.");
            saveButton.setEnabled(true);
            return;
        }
        Uri destination = data.getData();
        setBusy(true, "Exporting validated modified APK…");
        worker.execute(() -> {
            String error = null;
            try (InputStream in = new FileInputStream(pendingOutput);
                 OutputStream out = getContentResolver().openOutputStream(destination, "w")) {
                if (out == null) throw new IllegalStateException("The document provider did not create an output stream.");
                byte[] buffer = new byte[32768];
                int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                out.flush();
            } catch (Exception e) {
                error = e.getClass().getSimpleName() + ": " + safe(e.getMessage());
            }
            final String failure = error;
            runOnUiThread(() -> {
                setBusy(false, failure == null ? "Export complete. Output is unsigned and needs signing." : "Export failed.");
                if (failure == null) summary.append("\n\nEXPORT: PASS — modified unsigned APK saved.");
                else summary.append("\n\nEXPORT FAILED: " + failure);
                if (pendingOutput != null) pendingOutput.delete();
                pendingOutput = null;
                saveButton.setEnabled(true);
            });
        });
    }

    private void setBusy(boolean busy, String message) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!busy && apkModule != null);
        status.setText(message);
    }

    private static String safe(String value) { return value == null ? "" : value; }

    @Override protected void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
        if (apkModule != null) {
            try { apkModule.close(); } catch (Exception ignored) { }
            apkModule = null;
        }
        if (sourceCopy != null) sourceCopy.delete();
        if (pendingOutput != null) pendingOutput.delete();
    }
}
