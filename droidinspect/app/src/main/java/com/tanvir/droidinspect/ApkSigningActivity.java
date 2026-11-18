package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.security.auth.x500.X500Principal;

/** On-device APK signing and signature verification. Original inputs are never overwritten. */
public final class ApkSigningActivity extends Activity {
    private static final int PICK_APK = 8101;
    private static final int PICK_KEYSTORE = 8102;
    private static final int SAVE_APK = 8103;
    private static final long MAX_APK = 512L * 1024L * 1024L;
    private static final long MAX_KEYSTORE = 16L * 1024L * 1024L;

    private final java.util.concurrent.ExecutorService worker =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView selectedApk, selectedKey, status, report;
    private EditText alias, owner, storePassword, keyPassword;
    private Button signButton, verifyButton, exportButton;
    private ProgressBar progress;
    private volatile boolean destroyed;
    private volatile boolean busy;
    private File apkFile, keyStoreFile, signedFile;
    private boolean generatedKey;
    private boolean returnSignedUri;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_apk_signing);
        selectedApk = findViewById(R.id.signSelectedApk);
        selectedKey = findViewById(R.id.signSelectedKey);
        status = findViewById(R.id.signStatus);
        report = findViewById(R.id.signReport);
        alias = findViewById(R.id.signAlias);
        owner = findViewById(R.id.signOwner);
        storePassword = findViewById(R.id.signStorePassword);
        keyPassword = findViewById(R.id.signKeyPassword);
        progress = findViewById(R.id.signProgress);
        signButton = findViewById(R.id.signButton);
        verifyButton = findViewById(R.id.verifyButton);
        exportButton = findViewById(R.id.exportSignedButton);
        String staleCleanupWarning = cleanupStaleSigningFiles(getApplicationContext());
        if (staleCleanupWarning != null) status.setText(staleCleanupWarning);

        findViewById(R.id.pickApkButton).setOnClickListener(v -> pick(PICK_APK,
                "application/vnd.android.package-archive"));
        findViewById(R.id.generateKeyButton).setOnClickListener(v -> generateKey());
        findViewById(R.id.importKeyButton).setOnClickListener(v -> pick(PICK_KEYSTORE, "*/*"));
        signButton.setOnClickListener(v -> signApk());
        verifyButton.setOnClickListener(v -> verifyApk());
        exportButton.setOnClickListener(v -> exportSigned());
        updateButtons();
        returnSignedUri = getIntent().getBooleanExtra("return_signed_apk", false);
        String initialApk = getIntent().getStringExtra("apk_uri");
        if (initialApk != null) loadApkUri(Uri.parse(initialApk));
    }

    private void pick(int request, String mime) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime);
        if (request == PICK_KEYSTORE) i.setType("*/*");
        startActivityForResult(i, request);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == SAVE_APK) {
            if (result != RESULT_OK || data == null || data.getData() == null) {
                status.setText("Export cancelled; original APK remains unchanged.");
                if (returnSignedUri) {
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
            setBusy(true, "Exporting and re-verifying signed APK…");
            final File source = signedFile;
            final Context app = getApplicationContext();
            final File cacheDir = app.getCacheDir();
            final WeakReference<ApkSigningActivity> screenRef = new WeakReference<>(this);
            final Handler handler = mainHandler;
            worker.execute(() -> {
                Throwable problem = null;
                File exportedCopy = null;
                try (InputStream in = new FileInputStream(source);
                     OutputStream out = app.getContentResolver().openOutputStream(destination, "w")) {
                    if (out == null) throw new IllegalStateException("Document provider did not create an output stream.");
                    byte[] buffer = new byte[32768]; int n;
                    while ((n = in.read(buffer)) != -1) {
                        checkCancelled();
                        out.write(buffer, 0, n);
                    }
                    out.flush();
                } catch (Exception | Error e) {
                    problem = e;
                }
                if (problem == null) {
                    try {
                        checkCancelled();
                        exportedCopy = copyDocument(app, destination, ".apk", MAX_APK);
                        if (!MessageDigest.isEqual(sha256(source), sha256(exportedCopy)))
                            throw new IllegalStateException(
                                    "The exported APK bytes do not match the verified signed output.");
                        ApkVerifier.Result checked = new ApkVerifier.Builder(exportedCopy).build().verify();
                        if (!checked.isVerified())
                            throw new IllegalStateException("The exported document failed signature verification: "
                                    + verificationFailure(checked));
                    } catch (Exception | Error e) {
                        problem = e;
                    }
                }
                if (exportedCopy != null) {
                    try { deleteTemporaryFile(exportedCopy, cacheDir, problem); }
                    catch (IOException cleanupFailure) {
                        if (problem == null) problem = cleanupFailure;
                        else problem.addSuppressed(cleanupFailure);
                    }
                }
                final String failure = problem == null ? null : message(problem);
                handler.post(() -> {
                    ApkSigningActivity screen = screenRef.get();
                    if (screen == null || screen.destroyed || screen.isFinishing() || screen.isDestroyed()) return;
                    screen.setBusy(false, failure == null
                            ? "Signed APK exported and signature re-verified."
                            : "Export failed: " + failure);
                    if (failure != null) screen.showError(failure);
                    if (screen.returnSignedUri) {
                        Intent resultIntent = new Intent();
                        if (failure == null) {
                            resultIntent.setData(destination);
                            resultIntent.putExtra("signed_apk_uri", destination.toString());
                            screen.setResult(RESULT_OK, resultIntent);
                        } else {
                            screen.setResult(RESULT_CANCELED);
                        }
                        screen.finish();
                    }
                });
            });
            return;
        }
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (request == PICK_APK) {
            loadApkUri(uri);
        } else if (request == PICK_KEYSTORE) {
            setBusy(true, "Copying keystore into private app cache…");
            final Context app = getApplicationContext();
            final File cacheDir = app.getCacheDir();
            final WeakReference<ApkSigningActivity> screenRef = new WeakReference<>(this);
            final Handler handler = mainHandler;
            worker.execute(() -> {
                File copy = null;
                Throwable problem = null;
                try { copy = copyDocument(app, uri, ".keystore", MAX_KEYSTORE); }
                catch (Exception | Error e) {
                    problem = e;
                    if (copy != null) deleteTemporaryFile(copy, cacheDir, problem);
                    copy = null;
                }
                final File ready = copy;
                final String failure = problem == null ? null : message(problem);
                handler.post(() -> {
                    ApkSigningActivity screen = screenRef.get();
                    if (screen == null || screen.destroyed || screen.isFinishing() || screen.isDestroyed()) {
                        deleteTemporaryFileAsync(ready, cacheDir);
                        return;
                    }
                    screen.setBusy(false, failure == null
                            ? "Keystore copied. Enter its alias and passwords." : "Keystore import failed.");
                    if (failure == null) {
                        File previous = screen.keyStoreFile;
                        screen.keyStoreFile = ready;
                        screen.generatedKey = false;
                        screen.selectedKey.setText("Imported keystore: " + ready.getName());
                        deleteTemporaryFileAsync(previous, cacheDir);
                    } else screen.showError(failure);
                    screen.updateButtons();
                });
            });
        }
    }

    private void loadApkUri(Uri uri) {
        setBusy(true, "Copying selected APK…");
        final Context app = getApplicationContext();
        final File cacheDir = app.getCacheDir();
        final WeakReference<ApkSigningActivity> screenRef = new WeakReference<>(this);
        final Handler handler = mainHandler;
        worker.execute(() -> {
            File copy = null;
            Throwable problem = null;
            try {
                copy = copyDocument(app, uri, ".apk", MAX_APK);
                if (!isZip(copy)) throw new IllegalArgumentException("Selected file is not a readable APK/ZIP archive.");
            } catch (Exception | Error e) {
                problem = e;
                if (copy != null) deleteTemporaryFile(copy, cacheDir, problem);
                copy = null;
            }
            final File ready = copy;
            final String failure = problem == null ? null : message(problem);
            handler.post(() -> {
                ApkSigningActivity screen = screenRef.get();
                if (screen == null || screen.destroyed || screen.isFinishing() || screen.isDestroyed()) {
                    deleteTemporaryFileAsync(ready, cacheDir);
                    return;
                }
                screen.setBusy(false, failure == null ? "APK ready." : "APK selection failed.");
                if (failure == null) {
                    File previousApk = screen.apkFile;
                    File previousSigned = screen.signedFile;
                    screen.apkFile = ready;
                    screen.signedFile = null;
                    screen.selectedApk.setText("APK: " + ready.getName() + " (" + ready.length() + " bytes)");
                    deleteTemporaryFileAsync(previousApk, cacheDir);
                    deleteTemporaryFileAsync(previousSigned, cacheDir);
                } else screen.showError(failure);
                screen.updateButtons();
            });
        });
    }

    private static File copyDocument(Context context, Uri uri, String suffix, long limit) throws IOException {
        File outFile = File.createTempFile("droidinspect-sign-", suffix, context.getCacheDir());
        long total = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(outFile)) {
            if (in == null) throw new IOException("Android could not open the selected document.");
            byte[] buffer = new byte[32768]; int n;
            while ((n = in.read(buffer)) != -1) {
                checkCancelled();
                total += n;
                if (total > limit) throw new IOException("Selected document exceeds the safety size limit.");
                out.write(buffer, 0, n);
            }
            out.flush();
            out.getFD().sync();
        } catch (IOException | RuntimeException | Error failure) {
            deleteTemporaryFile(outFile, context.getCacheDir(), failure);
            throw failure;
        }
        return outFile;
    }

    private void generateKey() {
        String a = alias.getText().toString().trim();
        String o = owner.getText().toString().trim();
        if (!a.matches("[A-Za-z0-9._-]{1,48}")) {
            alias.setError("Use 1–48 letters, digits, dot, underscore or hyphen."); return;
        }
        if (o.isEmpty() || o.length() > 80) { owner.setError("Enter a certificate owner name (1–80 characters)."); return; }
        new AlertDialog.Builder(this).setTitle("Create on-device signing key")
                .setMessage("A 2048-bit RSA key will be generated in Android Keystore. The private key is non-exportable and remains tied to this app installation. Back up your app-signing strategy separately; uninstalling/clearing app data can make this key unavailable.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create key", (d, w) -> createAndroidKey(a, o)).show();
    }

    private void createAndroidKey(String a, String o) {
        setBusy(true, "Generating RSA signing key in Android Keystore…");
        final WeakReference<ApkSigningActivity> screenRef = new WeakReference<>(this);
        final Handler handler = mainHandler;
        final File cacheDir = getCacheDir();
        worker.execute(() -> {
            String error = null;
            try {
                checkCancelled();
                KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
                java.util.Calendar start = java.util.Calendar.getInstance();
                java.util.Calendar end = (java.util.Calendar) start.clone(); end.add(java.util.Calendar.YEAR, 25);
                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(a,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                        .setKeySize(2048)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS)
                        .setCertificateSubject(new X500Principal("CN=" + o.replace(",", "\\,").replace("+", "\\+")))
                        .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
                        .setCertificateNotBefore(start.getTime())
                        .setCertificateNotAfter(end.getTime())
                        .build();
                generator.initialize(spec); generator.generateKeyPair();
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
                if (ks.getCertificate(a) == null) throw new IllegalStateException("Android Keystore did not return the generated certificate.");
            } catch (Exception | Error e) { error = message(e); }
            final String failure = error;
            handler.post(() -> {
                ApkSigningActivity screen = screenRef.get();
                if (screen == null || screen.destroyed || screen.isFinishing() || screen.isDestroyed()) return;
                screen.setBusy(false, failure == null ? "Signing key created in Android Keystore." : "Key generation failed.");
                if (failure == null) {
                    screen.generatedKey = true;
                    File previous = screen.keyStoreFile; screen.keyStoreFile = null;
                    deleteTemporaryFileAsync(previous, cacheDir);
                    screen.selectedKey.setText("Android Keystore alias: " + a + " (non-exportable)");
                    screen.report.setText("KEY CREATED\nAlias: " + a + "\nOwner: " + o
                            + "\nAlgorithm: RSA 2048 / SHA-256\nStorage: Android Keystore\n"
                            + "Private key export: not available. Preserve this app installation and device data.");
                } else screen.showError(failure);
                screen.updateButtons();
            });
        });
    }

    private static KeyMaterial loadKeyMaterial(String a, File keyStoreFile, boolean generatedKey,
            char[] storePass, char[] privatePass) throws Exception {
        try {
            if (a.isEmpty()) throw new IllegalArgumentException("Enter the signing-key alias.");
            if (generatedKey) {
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
                PrivateKey key = (PrivateKey) ks.getKey(a, null);
                java.security.cert.Certificate cert = ks.getCertificate(a);
                if (key == null || !(cert instanceof X509Certificate))
                    throw new IllegalArgumentException("Android Keystore alias or certificate was not found.");
                return new KeyMaterial(key, (X509Certificate) cert);
            }
            if (keyStoreFile == null) throw new IllegalArgumentException("Generate a key or import a keystore first.");
            KeyStore ks;
            try {
                ks = KeyStore.getInstance("PKCS12");
                try (FileInputStream in = new FileInputStream(keyStoreFile)) { ks.load(in, storePass); }
            } catch (Exception pkcs12Error) {
                ks = KeyStore.getInstance("JKS");
                try (FileInputStream in = new FileInputStream(keyStoreFile)) { ks.load(in, storePass); }
            }
            if (!ks.containsAlias(a)) throw new IllegalArgumentException("Alias not found in the imported PKCS#12 keystore.");
            java.security.Key key = ks.getKey(a, privatePass.length == 0 ? storePass : privatePass);
            if (!(key instanceof PrivateKey)) throw new IllegalArgumentException("The selected alias does not contain a private signing key.");
            java.security.cert.Certificate[] chain = ks.getCertificateChain(a);
            if (chain == null || chain.length == 0 || !(chain[0] instanceof X509Certificate))
                throw new IllegalArgumentException("The selected alias has no X.509 certificate.");
            ArrayList<X509Certificate> certs = new ArrayList<>();
            for (java.security.cert.Certificate c : chain) {
                if (!(c instanceof X509Certificate)) throw new IllegalArgumentException("Unsupported certificate type in chain.");
                certs.add((X509Certificate)c);
            }
            return new KeyMaterial((PrivateKey)key, certs);
        } finally {
            if (storePass != null) Arrays.fill(storePass, '\0');
            if (privatePass != null) Arrays.fill(privatePass, '\0');
        }
    }

    private void signApk() {
        if (apkFile == null || busy) return;
        final File sourceApk = apkFile;
        final File keyStoreSnapshot = keyStoreFile;
        final boolean generatedSnapshot = generatedKey;
        final String aliasValue = alias.getText().toString().trim();
        final char[] storePassValue = readAndClearPassword(storePassword);
        final char[] keyPassValue = readAndClearPassword(keyPassword);
        final File cacheDir = getCacheDir();
        final WeakReference<ApkSigningActivity> screenRef = new WeakReference<>(this);
        final Handler handler = mainHandler;
        setBusy(true, "Signing APK with v1, v2 and v3 schemes, then verifying…");
        worker.execute(() -> {
            File output = null;
            String resultText = null;
            Throwable problem = null;
            try {
                KeyMaterial material = loadKeyMaterial(aliasValue, keyStoreSnapshot, generatedSnapshot,
                        storePassValue, keyPassValue);
                output = File.createTempFile("droidinspect-sign-signed-", ".apk", cacheDir);
                checkCancelled();
                ApkSigner.SignerConfig config = new ApkSigner.SignerConfig.Builder(
                        "DroidInspect", material.key, material.chain).build();
                new ApkSigner.Builder(java.util.Collections.singletonList(config))
                        .setInputApk(sourceApk).setOutputApk(output)
                        .setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true)
                        .build().sign();
                checkCancelled();
                ApkVerifier.Result verified = new ApkVerifier.Builder(output).build().verify();
                if (!verified.isVerified()) {
                    StringBuilder problems = new StringBuilder("Signature verification failed.");
                    for (ApkVerifier.IssueWithParams issue : verified.getErrors()) problems.append("\n").append(issue);
                    throw new IllegalStateException(problems.toString());
                }
                resultText = formatVerification(verified);
            } catch (Exception | Error e) {
                problem = e;
                if (output != null) deleteTemporaryFile(output, cacheDir, problem);
                output = null;
            } finally {
                Arrays.fill(storePassValue, '\0');
                Arrays.fill(keyPassValue, '\0');
            }
            final File ready = output;
            final String details = resultText;
            final String failure = problem == null ? null : message(problem);
            handler.post(() -> {
                ApkSigningActivity screen = screenRef.get();
                if (screen == null || screen.destroyed || screen.isFinishing() || screen.isDestroyed()) {
                    deleteTemporaryFileAsync(ready, cacheDir);
                    return;
                }
                screen.setBusy(false, failure == null ? "Signed APK verified successfully." : "Signing or verification failed.");
                if (failure == null) {
                    File previous = screen.signedFile;
                    screen.signedFile = ready;
                    screen.report.setText(details + "\n\nSigned output is separate from the original input.");
                    deleteTemporaryFileAsync(previous, cacheDir);
                } else screen.showError(failure);
                screen.updateButtons();
            });
        });
    }

    private void verifyApk() {
        if (apkFile == null || busy) return;
        final File sourceApk = apkFile;
        final WeakReference<ApkSigningActivity> screenRef = new WeakReference<>(this);
        final Handler handler = mainHandler;
        setBusy(true, "Verifying APK signature…");
        worker.execute(() -> {
            String result = null, error = null;
            try {
                checkCancelled();
                ApkVerifier.Result checked = new ApkVerifier.Builder(sourceApk).build().verify();
                result = checked.isVerified() ? formatVerification(checked) : verificationFailure(checked);
            } catch (Exception | Error e) { error = message(e); }
            final String text = result, failure = error;
            handler.post(() -> {
                ApkSigningActivity screen = screenRef.get();
                if (screen == null || screen.destroyed || screen.isFinishing() || screen.isDestroyed()) return;
                screen.setBusy(false, "Verification finished.");
                screen.report.setText(failure == null ? text : "VERIFICATION ERROR\n" + failure);
                screen.updateButtons();
            });
        });
    }

    private void exportSigned() {
        if (signedFile == null || !signedFile.isFile()) return;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/vnd.android.package-archive");
        i.putExtra(Intent.EXTRA_TITLE, "DroidInspect-signed.apk");
        startActivityForResult(i, SAVE_APK);
    }

    private static String formatVerification(ApkVerifier.Result r) throws Exception {
        StringBuilder b = new StringBuilder("APK SIGNATURE VERIFICATION: PASS\n");
        b.append("v1/JAR: ").append(r.isVerifiedUsingV1Scheme()).append('\n');
        b.append("v2: ").append(r.isVerifiedUsingV2Scheme()).append('\n');
        b.append("v3: ").append(r.isVerifiedUsingV3Scheme()).append('\n');
        b.append("v4: ").append(r.isVerifiedUsingV4Scheme()).append('\n');
        b.append("Signer certificates: ").append(r.getSignerCertificates().size()).append('\n');
        for (int i = 0; i < r.getSignerCertificates().size(); i++) {
            X509Certificate c = r.getSignerCertificates().get(i);
            b.append("\nSIGNER ").append(i + 1).append("\nSubject: ").append(c.getSubjectX500Principal())
                    .append("\nIssuer: ").append(c.getIssuerX500Principal())
                    .append("\nSerial: ").append(c.getSerialNumber().toString(16))
                    .append("\nValid until: ").append(c.getNotAfter())
                    .append("\nSHA-256: ").append(hex(java.security.MessageDigest.getInstance("SHA-256").digest(c.getEncoded()))).append('\n');
        }
        if (!r.getWarnings().isEmpty()) {
            b.append("\nWarnings:\n"); for (ApkVerifier.IssueWithParams w : r.getWarnings()) b.append("• ").append(w).append('\n');
        }
        return b.toString();
    }

    private static String verificationFailure(ApkVerifier.Result r) {
        StringBuilder b = new StringBuilder("APK SIGNATURE VERIFICATION: FAIL\n");
        for (ApkVerifier.IssueWithParams issue : r.getErrors()) b.append("• ").append(issue).append('\n');
        if (r.getErrors().isEmpty()) b.append("No verified signer was found. The APK may be unsigned or use an unsupported scheme.\n");
        return b.toString();
    }

    private static boolean isZip(File f) throws Exception {
        try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(f)) { return z.getEntry("AndroidManifest.xml") != null; }
    }
    private static byte[] sha256(File file) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException unavailable) {
            throw new IOException("SHA-256 is unavailable on this Android device.", unavailable);
        }
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[32768];
            int count;
            while ((count = input.read(buffer)) != -1) {
                checkCancelled();
                digest.update(buffer, 0, count);
            }
        }
        return digest.digest();
    }

    private static String hex(byte[] data) {
        StringBuilder b = new StringBuilder(); for (byte v : data) b.append(String.format(Locale.ROOT, "%02X", v & 255)); return b.toString();
    }
    private static String message(Throwable e) {
        StringBuilder result = new StringBuilder(e.getClass().getSimpleName()).append(": ")
                .append(e.getMessage() == null ? "No additional details." : e.getMessage());
        for (Throwable suppressed : e.getSuppressed())
            result.append("\nCleanup warning: ").append(suppressed.getClass().getSimpleName()).append(": ")
                    .append(suppressed.getMessage() == null ? "No additional details." : suppressed.getMessage());
        return result.toString();
    }
    private void showError(String value) { new AlertDialog.Builder(this).setTitle("APK signing / verification").setMessage(value).setPositiveButton("OK", null).show(); }
    private void setBusy(boolean busyValue, String value) {
        busy = busyValue;
        progress.setVisibility(busyValue ? View.VISIBLE : View.GONE);
        findViewById(R.id.pickApkButton).setEnabled(!busyValue);
        findViewById(R.id.generateKeyButton).setEnabled(!busyValue);
        findViewById(R.id.importKeyButton).setEnabled(!busyValue);
        alias.setEnabled(!busyValue); owner.setEnabled(!busyValue);
        storePassword.setEnabled(!busyValue); keyPassword.setEnabled(!busyValue);
        status.setText(value);
        updateButtons();
    }
    private void updateButtons() {
        signButton.setEnabled(!busy && apkFile != null && (generatedKey || keyStoreFile != null));
        verifyButton.setEnabled(!busy && apkFile != null);
        exportButton.setEnabled(!busy && signedFile != null);
    }

    private static char[] readAndClearPassword(EditText field) {
        android.text.Editable editable = field.getText();
        char[] value = new char[editable.length()];
        editable.getChars(0, editable.length(), value, 0);
        editable.clear();
        return value;
    }

    private static String cleanupStaleSigningFiles(Context context) {
        File cacheDir = context.getCacheDir();
        File[] files = cacheDir.listFiles();
        if (files == null) return null;
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        ArrayList<String> failures = new ArrayList<>();
        for (File file : files) {
            if (!file.getName().startsWith("droidinspect-sign-") || file.lastModified() > cutoff) continue;
            try { deleteTemporaryFile(file, cacheDir, null); }
            catch (IOException cleanupFailure) {
                failures.add(file.getName() + ": " + cleanupFailure.getMessage());
                Log.w("DroidInspectSigning", "Could not clean stale signing file " + file, cleanupFailure);
            }
        }
        return failures.isEmpty() ? null : "Old signing temporary-file cleanup needs attention: "
                + android.text.TextUtils.join("; ", failures);
    }

    private static void deleteTemporaryFile(File file, File cacheDir, Throwable primary) throws IOException {
        if (file == null) return;
        IOException cleanupFailure = null;
        try {
            File absolute = file.getAbsoluteFile();
            File parent = absolute.getParentFile();
            if (parent == null || !parent.equals(cacheDir.getAbsoluteFile())
                    || !absolute.getName().startsWith("droidinspect-sign-")
                    || Files.isSymbolicLink(absolute.toPath())) {
                throw new IOException("Refusing to delete a file outside the owned signing cache.");
            }
            Files.deleteIfExists(absolute.toPath());
            if (absolute.exists()) throw new IOException("Temporary file still exists after deletion.");
        } catch (IOException e) {
            cleanupFailure = e;
        } catch (RuntimeException e) {
            cleanupFailure = new IOException("Signing temporary-file cleanup failed.", e);
        }
        if (cleanupFailure != null) {
            if (primary != null) primary.addSuppressed(cleanupFailure);
            else throw cleanupFailure;
        }
    }

    private static void deleteTemporaryFileAsync(File file, File cacheDir) {
        if (file == null) return;
        Thread cleanup = new Thread(() -> {
            try { deleteTemporaryFile(file, cacheDir, null); }
            catch (IOException e) { Log.e("DroidInspectSigning", "Signing temporary file remains: " + file, e); }
        }, "droidinspect-signing-file-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }

    private static final class KeyMaterial {
        final PrivateKey key; final List<X509Certificate> chain;
        KeyMaterial(PrivateKey k, X509Certificate c) { key = k; chain = java.util.Collections.singletonList(c); }
        KeyMaterial(PrivateKey k, List<X509Certificate> c) { key = k; chain = c; }
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("APK signing operation cancelled because the activity was closed.");
    }

    @Override protected void onDestroy() {
        destroyed = true;
        final java.util.concurrent.ExecutorService executor = worker;
        final File cacheDir = getCacheDir();
        final File[] ownedFiles = new File[]{apkFile, keyStoreFile, signedFile};
        apkFile = null; keyStoreFile = null; signedFile = null;
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
            for (File owned : ownedFiles) {
                try { deleteTemporaryFile(owned, cacheDir, null); }
                catch (IOException e) { Log.e("DroidInspectSigning", "Could not remove owned signing file: " + owned, e); }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }, "droidinspect-signing-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }
}
