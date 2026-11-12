package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
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
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private TextView selectedApk, selectedKey, status, report;
    private EditText alias, owner, storePassword, keyPassword;
    private Button signButton, verifyButton, exportButton;
    private ProgressBar progress;
    private File apkFile, keyStoreFile, signedFile;
    private boolean generatedKey;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_apk_signing);
        cleanupStaleSigningFiles();
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

        findViewById(R.id.pickApkButton).setOnClickListener(v -> pick(PICK_APK,
                "application/vnd.android.package-archive"));
        findViewById(R.id.generateKeyButton).setOnClickListener(v -> generateKey());
        findViewById(R.id.importKeyButton).setOnClickListener(v -> pick(PICK_KEYSTORE, "*/*"));
        signButton.setOnClickListener(v -> signApk());
        verifyButton.setOnClickListener(v -> verifyApk());
        exportButton.setOnClickListener(v -> exportSigned());
        updateButtons();
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
                status.setText("Export cancelled; original APK remains unchanged."); return;
            }
            Uri destination = data.getData();
            setBusy(true, "Exporting verified signed APK…");
            worker.execute(() -> {
                String error = null;
                try (InputStream in = new FileInputStream(signedFile);
                     OutputStream out = getContentResolver().openOutputStream(destination, "w")) {
                    if (out == null) throw new IllegalStateException("Document provider did not create an output stream.");
                    byte[] buffer = new byte[32768]; int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                    out.flush();
                } catch (Exception e) { error = message(e); }
                final String failure = error;
                runOnUiThread(() -> {
                    setBusy(false, failure == null ? "Signed APK exported." : "Export failed: " + failure);
                    if (failure != null) showError(failure);
                });
            });
            return;
        }
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (request == PICK_APK) {
            setBusy(true, "Copying selected APK…");
            worker.execute(() -> {
                File copy = null;
                String error = null;
                try {
                    copy = copyDocument(uri, ".apk", MAX_APK);
                    if (!isZip(copy)) throw new IllegalArgumentException("Selected file is not a readable APK/ZIP archive.");
                } catch (Exception e) { error = message(e); if (copy != null) copy.delete(); copy = null; }
                final File ready = copy; final String failure = error;
                runOnUiThread(() -> {
                    setBusy(false, failure == null ? "APK ready." : "APK selection failed.");
                    if (failure == null) {
                        if (apkFile != null) apkFile.delete();
                        apkFile = ready;
                        selectedApk.setText("APK: " + ready.getName() + " (" + ready.length() + " bytes)");
                        if (signedFile != null) { signedFile.delete(); signedFile = null; }
                    } else showError(failure);
                    updateButtons();
                });
            });
        } else if (request == PICK_KEYSTORE) {
            setBusy(true, "Copying keystore into private app cache…");
            worker.execute(() -> {
                File copy = null; String error = null;
                try { copy = copyDocument(uri, ".keystore", MAX_KEYSTORE); }
                catch (Exception e) { error = message(e); if (copy != null) copy.delete(); copy = null; }
                final File ready = copy; final String failure = error;
                runOnUiThread(() -> {
                    setBusy(false, failure == null ? "Keystore copied. Enter its alias and passwords." : "Keystore import failed.");
                    if (failure == null) {
                        if (keyStoreFile != null) keyStoreFile.delete();
                        keyStoreFile = ready; generatedKey = false;
                        selectedKey.setText("Imported keystore: " + ready.getName());
                    } else showError(failure);
                    updateButtons();
                });
            });
        }
    }

    private File copyDocument(Uri uri, String suffix, long limit) throws Exception {
        File outFile = File.createTempFile("droidinspect-sign-", suffix, getCacheDir());
        long total = 0;
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(outFile)) {
            if (in == null) throw new IllegalStateException("Android could not open the selected document.");
            byte[] buffer = new byte[32768]; int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > limit) throw new IllegalArgumentException("Selected document exceeds the safety size limit.");
                out.write(buffer, 0, n);
            }
            out.flush();
        } catch (Exception e) { outFile.delete(); throw e; }
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
        worker.execute(() -> {
            String error = null;
            try {
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
            } catch (Exception e) { error = message(e); }
            final String failure = error;
            runOnUiThread(() -> {
                setBusy(false, failure == null ? "Signing key created in Android Keystore." : "Key generation failed.");
                if (failure == null) {
                    generatedKey = true;
                    if (keyStoreFile != null) { keyStoreFile.delete(); keyStoreFile = null; }
                    selectedKey.setText("Android Keystore alias: " + a + " (non-exportable)");
                    report.setText("KEY CREATED\nAlias: " + a + "\nOwner: " + o
                            + "\nAlgorithm: RSA 2048 / SHA-256\nStorage: Android Keystore\n"
                            + "Private key export: not available. Preserve this app installation and device data.");
                } else showError(failure);
                updateButtons();
            });
        });
    }

    private KeyMaterial loadKeyMaterial(String a, String storePasswordText, String keyPasswordText) throws Exception {
        if (a.isEmpty()) throw new IllegalArgumentException("Enter the signing-key alias.");
        if (generatedKey) {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
            PrivateKey key = (PrivateKey) ks.getKey(a, null);
            java.security.cert.Certificate cert = ks.getCertificate(a);
            if (key == null || !(cert instanceof X509Certificate)) throw new IllegalArgumentException("Android Keystore alias or certificate was not found.");
            return new KeyMaterial(key, (X509Certificate) cert);
        }
        if (keyStoreFile == null) throw new IllegalArgumentException("Generate a key or import a keystore first.");
        char[] storePass = storePasswordText.toCharArray();
        char[] privatePass = keyPasswordText.toCharArray();
        try {
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
            Arrays.fill(storePass, '\0'); Arrays.fill(privatePass, '\0');
        }
    }

    private void signApk() {
        if (apkFile == null) return;
        final String aliasValue = alias.getText().toString().trim();
        final String storePassValue = storePassword.getText().toString();
        final String keyPassValue = keyPassword.getText().toString();
        setBusy(true, "Signing APK with v1, v2 and v3 schemes, then verifying…");
        worker.execute(() -> {
            File output = null; String resultText = null; String error = null;
            try {
                KeyMaterial material = loadKeyMaterial(aliasValue, storePassValue, keyPassValue);
                output = File.createTempFile("droidinspect-signed-", ".apk", getCacheDir());
                ApkSigner.SignerConfig config = new ApkSigner.SignerConfig.Builder(
                        "DroidInspect", material.key, material.chain).build();
                new ApkSigner.Builder(java.util.Collections.singletonList(config))
                        .setInputApk(apkFile).setOutputApk(output)
                        .setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true)
                        .build().sign();
                ApkVerifier.Result verified = new ApkVerifier.Builder(output).build().verify();
                if (!verified.isVerified()) {
                    StringBuilder problems = new StringBuilder("Signature verification failed.");
                    for (ApkVerifier.IssueWithParams issue : verified.getErrors()) problems.append("\n").append(issue);
                    throw new IllegalStateException(problems.toString());
                }
                resultText = formatVerification(verified);
            } catch (Exception e) { error = message(e); if (output != null) output.delete(); output = null; }
            final File ready = output; final String details = resultText; final String failure = error;
            runOnUiThread(() -> {
                storePassword.setText(""); keyPassword.setText("");
                setBusy(false, failure == null ? "Signed APK verified successfully." : "Signing or verification failed.");
                if (failure == null) {
                    if (signedFile != null) signedFile.delete();
                    signedFile = ready; report.setText(details + "\n\nSigned output is separate from the original input.");
                    exportButton.setEnabled(true);
                } else showError(failure);
                updateButtons();
            });
        });
    }

    private void verifyApk() {
        if (apkFile == null) return;
        setBusy(true, "Verifying APK signature…");
        worker.execute(() -> {
            String result = null, error = null;
            try {
                ApkVerifier.Result checked = new ApkVerifier.Builder(apkFile).build().verify();
                result = checked.isVerified() ? formatVerification(checked) : verificationFailure(checked);
            } catch (Exception e) { error = message(e); }
            final String text = result, failure = error;
            runOnUiThread(() -> {
                setBusy(false, "Verification finished.");
                report.setText(failure == null ? text : "VERIFICATION ERROR\n" + failure);
                updateButtons();
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

    private String formatVerification(ApkVerifier.Result r) throws Exception {
        StringBuilder b = new StringBuilder("APK SIGNATURE VERIFICATION: PASS\n");
        b.append("v1/JAR: ").append(r.isVerifiedUsingV1Scheme()).append('\n');
        b.append("v2: ").append(r.isVerifiedUsingV2Scheme()).append('\n');
        b.append("v3: ").append(r.isVerifiedUsingV3Scheme()).append('\n');
        b.append("v4: ").append(r.isVerifiedUsingV4Scheme()).append('\n');
        b.append("Signer certificates: ").append(r.getSignerCertificates().size()).append('\n');
        for (int i = 0; i < r.getSignerCertificates().size(); i++) {
            X509Certificate c = r.getSignerCertificates().get(i);
            b.append("\nSIGNER ").append(i + 1).append('\nSubject: ").append(c.getSubjectX500Principal())
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

    private String verificationFailure(ApkVerifier.Result r) {
        StringBuilder b = new StringBuilder("APK SIGNATURE VERIFICATION: FAIL\n");
        for (ApkVerifier.IssueWithParams issue : r.getErrors()) b.append("• ").append(issue).append('\n');
        if (r.getErrors().isEmpty()) b.append("No verified signer was found. The APK may be unsigned or use an unsupported scheme.\n");
        return b.toString();
    }

    private static boolean isZip(File f) throws Exception {
        try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(f)) { return z.getEntry("AndroidManifest.xml") != null; }
    }
    private static String hex(byte[] data) {
        StringBuilder b = new StringBuilder(); for (byte v : data) b.append(String.format(Locale.ROOT, "%02X", v & 255)); return b.toString();
    }
    private static String message(Exception e) { return e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "No additional details." : e.getMessage()); }
    private void showError(String value) { new AlertDialog.Builder(this).setTitle("APK signing / verification").setMessage(value).setPositiveButton("OK", null).show(); }
    private void setBusy(boolean busy, String value) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        findViewById(R.id.pickApkButton).setEnabled(!busy);
        findViewById(R.id.generateKeyButton).setEnabled(!busy);
        findViewById(R.id.importKeyButton).setEnabled(!busy);
        alias.setEnabled(!busy); owner.setEnabled(!busy);
        storePassword.setEnabled(!busy); keyPassword.setEnabled(!busy);
        status.setText(value); signButton.setEnabled(!busy && apkFile != null);
        verifyButton.setEnabled(!busy && apkFile != null); exportButton.setEnabled(!busy && signedFile != null);
    }
    private void updateButtons() {
        signButton.setEnabled(apkFile != null && (generatedKey || keyStoreFile != null));
        verifyButton.setEnabled(apkFile != null); exportButton.setEnabled(signedFile != null);
    }

        private static final class KeyMaterial {
        final PrivateKey key; final List<X509Certificate> chain;
        KeyMaterial(PrivateKey k, X509Certificate c) { key = k; chain = java.util.Collections.singletonList(c); }
        KeyMaterial(PrivateKey k, List<X509Certificate> c) { key = k; chain = c; }
    }

    @Override protected void onDestroy() {
        super.onDestroy(); worker.shutdownNow();
        if (apkFile != null) apkFile.delete();
        if (keyStoreFile != null) keyStoreFile.delete();
        if (signedFile != null) signedFile.delete();
    }
}
