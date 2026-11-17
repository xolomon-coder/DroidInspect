package com.tanvir.droidinspect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.apksig.ApkVerifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Guided edit -> rebuild -> sign -> verify -> compatibility workflow. */
public final class ApkBuildPipelineActivity extends Activity {
    private static final int PICK_SOURCE = 6601, EDIT_APK = 6602, SIGN_APK = 6603;
    private static final long MAX_APK_BYTES = 512L * 1024L * 1024L;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean destroyed;
    private Button selectButton, editButton, signButton;
    private ProgressBar progress;
    private TextView status, sourceSummary, report;
    private Uri sourceUri, editedUri, signedUri;
    private File sourceFile;
    private ApkProjectWorkspace project;
    private File originalCopy;
    private String originalPackage = "(unknown)", originalVersion = "(unknown)";
    private long originalVersionCode = -1;
    private List<String> originalSigners = Collections.emptyList();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(0xFF101318);

        TextView title = new TextView(this);
        title.setText("APK Edit → Rebuild → Sign Pipeline");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        root.addView(title);

        selectButton = new Button(this);
        selectButton.setText("1. SELECT ORIGINAL APK");
        root.addView(selectButton);

        sourceSummary = new TextView(this);
        sourceSummary.setTextColor(0xFFE5EAF0);
        sourceSummary.setTextSize(12);
        sourceSummary.setPadding(0, dp(6), 0, dp(6));
        root.addView(sourceSummary);

        editButton = new Button(this);
        editButton.setText("2. EDIT MANIFEST / RESOURCES AND EXPORT NEW APK");
        editButton.setEnabled(false);
        root.addView(editButton);

        signButton = new Button(this);
        signButton.setText("3. SIGN AND VERIFY EDITED APK");
        signButton.setEnabled(false);
        root.addView(signButton);

        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        status = new TextView(this);
        status.setTextColor(0xFF80CBC4);
        status.setPadding(0, dp(6), 0, dp(6));
        status.setText("Select an APK to begin. Every stage exports a separate file.");
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        report = new TextView(this);
        report.setTextColor(0xFFE5EAF0);
        report.setTextSize(12);
        report.setTypeface(android.graphics.Typeface.MONOSPACE);
        report.setTextIsSelectable(true);
        report.setPadding(dp(8), dp(8), dp(8), dp(8));
        scroll.addView(report);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        selectButton.setOnClickListener(v -> pickSource());
        editButton.setOnClickListener(v -> launchEditor());
        signButton.setOnClickListener(v -> launchSigner());

        String projectId = getIntent().getStringExtra("apk_project_id");
        String sourcePath = getIntent().getStringExtra("apk_file_path");
        if (projectId != null) {
            setBusy(true, "Opening persistent APK project…");
            Context appContext = getApplicationContext();
            Handler ui = new Handler(Looper.getMainLooper());
            WeakReference<ApkBuildPipelineActivity> owner = new WeakReference<>(this);
            worker.execute(() -> {
                ApkProjectWorkspace opened = null;
                String failure = null;
                try { opened = ApkProjectWorkspace.open(appContext, projectId); }
                catch (Exception error) { failure = message(error); }
                ApkProjectWorkspace ready = opened;
                String errorText = failure;
                postToActivity(ui, owner, activity -> {
                    if (errorText != null) {
                        activity.setBusy(false, "Project could not be opened.");
                        activity.showError(errorText);
                        return;
                    }
                    activity.project = ready;
                    activity.sourceFile = ready.getWorkingApk();
                    activity.sourceSummary.setText("Project: " + ready.getName()
                            + " • revision " + ready.getRevision());
                    activity.inspectOriginal();
                });
            });
        } else if (sourcePath != null) {
            sourceFile = new File(sourcePath);
            if (sourceFile.isFile()) inspectOriginal();
            else status.setText("Project working APK file is unavailable.");
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void pickSource() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.android.package-archive");
        startActivityForResult(intent, PICK_SOURCE);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_SOURCE) {
            if (result != RESULT_OK || data == null || data.getData() == null) return;
            project = null;
            sourceFile = null;
            sourceUri = data.getData();
            editedUri = null;
            signedUri = null;
            editButton.setEnabled(false);
            signButton.setEnabled(false);
            try {
                getContentResolver().takePersistableUriPermission(sourceUri,
                        data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            } catch (SecurityException ignored) { }
            inspectOriginal();
        } else if (request == EDIT_APK) {
            if (result == RESULT_OK && data != null && data.getData() != null) {
                editedUri = data.getData();
                editButton.setEnabled(false);
                signButton.setEnabled(project == null);
                status.setText("Edited APK exported as a separate unsigned file. Original APK remains untouched.");
                appendReport("\nEDIT STAGE: PASS\nEdited output URI received. Continue to signing and verification.");
                if (project != null) {
                    setBusy(true, "Saving edited unsigned APK into project exports…");
                    ApkProjectWorkspace activeProject = project;
                    Uri activeEditedUri = editedUri;
                    String exportName = "edited-unsigned-revision-" + (activeProject.getRevision() + 1) + ".apk";
                    Handler ui = new Handler(Looper.getMainLooper());
                    WeakReference<ApkBuildPipelineActivity> owner = new WeakReference<>(this);
                    worker.execute(() -> {
                        String failure = null;
                        try { activeProject.saveExportFromUri(activeEditedUri, exportName); }
                        catch (Exception error) { failure = message(error); }
                        String errorText = failure;
                        postToActivity(ui, owner, activity -> {
                            activity.setBusy(false, errorText == null
                                    ? "Edited unsigned APK saved to project exports." : "Project export save failed.");
                            if (errorText != null) {
                                activity.editedUri = null;
                                activity.signButton.setEnabled(false);
                                activity.editButton.setEnabled(true);
                                activity.showError(errorText);
                            }
                        });
                    });
                }
            } else {
                status.setText("Editing/export was cancelled or failed. Original APK remains unchanged.");
            }
        } else if (request == SIGN_APK) {
            if (result == RESULT_OK && data != null && data.getData() != null) {
                signedUri = data.getData();
                verifyFinalOutput();
            } else {
                status.setText("Signing/export was cancelled or failed. Previous files remain unchanged.");
            }
        }
    }

    private void inspectOriginal() {
        setBusy(true, "Copying and inspecting original APK identity and signature…");
        Context appContext = getApplicationContext();
        ApkProjectWorkspace activeProject = project;
        Uri activeSourceUri = sourceUri;
        File activeSourceFile = sourceFile;
        Handler ui = new Handler(Looper.getMainLooper());
        WeakReference<ApkBuildPipelineActivity> owner = new WeakReference<>(this);
        worker.execute(() -> {
            File copy = null;
            String error = null;
            String pkg = "(unknown)", version = "(unknown)";
            long versionCode = -1;
            List<String> signers = Collections.emptyList();
            try {
                if (activeProject != null) copy = copyFileToCache(appContext, activeProject.getOriginalApk(), "original");
                else if (activeSourceUri != null) copy = copyUri(appContext, activeSourceUri, "original");
                else if (activeSourceFile != null) copy = copyFileToCache(appContext, activeSourceFile, "original");
                else throw new IllegalStateException("No APK source is available.");
                PackageInfo info = appContext.getPackageManager().getPackageArchiveInfo(copy.getAbsolutePath(), 0);
                if (info == null) throw new IllegalArgumentException("Selected file is not a parseable APK.");
                pkg = info.packageName == null ? "(unknown)" : info.packageName;
                version = info.versionName == null ? "(not declared)" : info.versionName;
                versionCode = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
                signers = signerFingerprints(copy);
            } catch (Exception | Error e) {
                error = e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "No details." : e.getMessage());
                for (Throwable cleanup : e.getSuppressed())
                    error += "\nCleanup: " + cleanup.getMessage();
                if (copy != null) {
                    try { deleteTemporaryFile(copy, e); }
                    catch (Exception ignored) { }
                }
                copy = null;
            }
            final File ready = copy;
            final String failure = error, finalPkg = pkg, finalVersion = version;
            final long finalVersionCode = versionCode;
            final List<String> finalSigners = signers;
            postToActivity(ui, owner, activity -> {
                if (failure != null) {
                    if (ready != null) deleteQuietly(ready);
                    activity.setBusy(false, "Original APK inspection failed.");
                    activity.showError(failure);
                    return;
                }
                if (activity.originalCopy != null) deleteQuietly(activity.originalCopy);
                activity.originalCopy = ready;
                activity.originalPackage = finalPkg;
                activity.originalVersion = finalVersion;
                activity.originalVersionCode = finalVersionCode;
                activity.originalSigners = finalSigners;
                activity.editedUri = null;
                activity.signedUri = null;
                activity.editButton.setEnabled(true);
                activity.signButton.setEnabled(false);
                activity.sourceSummary.setText("Original: " + activity.originalPackage + "  •  "
                        + activity.originalVersion + "  •  versionCode " + activity.originalVersionCode);
                StringBuilder text = new StringBuilder("ORIGINAL APK\nPackage: ").append(activity.originalPackage)
                        .append("\nVersion: ").append(activity.originalVersion)
                        .append("\nVersion code: ").append(activity.originalVersionCode)
                        .append("\nOriginal verified signer(s): ");
                text.append(activity.originalSigners.isEmpty()
                        ? "(not verified / unsigned / unsupported)" : activity.originalSigners);
                text.append("\n\nThe original file is preserved. Editing and signing will create separate output files.");
                activity.report.setText(text.toString());
                activity.setBusy(false, "Original APK inspection complete.");
            });
        });
    }

    private void launchEditor() {
        if (sourceUri == null && sourceFile == null) return;
        Intent intent = new Intent(this, ApkResourceActivity.class);
        if (sourceFile != null) intent.putExtra("apk_file_path", sourceFile.getAbsolutePath());
        else intent.putExtra("apk_uri", sourceUri.toString());
        intent.putExtra("return_output_uri", true);
        startActivityForResult(intent, EDIT_APK);
    }

    private void launchSigner() {
        if (editedUri == null) return;
        Intent intent = new Intent(this, ApkSigningActivity.class);
        intent.putExtra("apk_uri", editedUri.toString());
        intent.putExtra("return_signed_apk", true);
        startActivityForResult(intent, SIGN_APK);
    }

    private void verifyFinalOutput() {
        setBusy(true, "Reopening exported APK and verifying signature and update compatibility…");
        Context appContext = getApplicationContext();
        Uri activeSignedUri = signedUri;
        ApkProjectWorkspace activeProject = project;
        String baselinePackage = originalPackage;
        long baselineVersionCode = originalVersionCode;
        List<String> baselineSigners = originalSigners;
        Handler ui = new Handler(Looper.getMainLooper());
        WeakReference<ApkBuildPipelineActivity> owner = new WeakReference<>(this);
        worker.execute(() -> {
            File copy = null;
            String error = null;
            String finalPackage = "(unknown)", finalVersion = "(unknown)";
            long finalVersionCode = -1;
            List<String> finalSigners = Collections.emptyList();
            boolean verified = false;
            boolean projectUpdated = false;
            String projectUpdateError = null;
            Throwable primaryFailure = null;
            try {
                copy = copyUri(appContext, activeSignedUri, "signed");
                PackageInfo info = appContext.getPackageManager().getPackageArchiveInfo(copy.getAbsolutePath(), 0);
                if (info == null) throw new IllegalArgumentException("Signed output is not a parseable APK.");
                finalPackage = info.packageName == null ? "(unknown)" : info.packageName;
                finalVersion = info.versionName == null ? "(not declared)" : info.versionName;
                finalVersionCode = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
                ApkVerifier.Result result = new ApkVerifier.Builder(copy).build().verify();
                verified = result.isVerified();
                if (!verified) {
                    StringBuilder why = new StringBuilder("APK signature verification failed.");
                    for (Object issue : result.getErrors()) why.append("\n").append(issue);
                    throw new IllegalStateException(why.toString());
                }
                finalSigners = signerFingerprints(copy);
                if (finalSigners.isEmpty()) throw new IllegalStateException("Signature verifier returned no signer certificates.");
                if (activeProject != null) {
                    try {
                        activeProject.replaceWorkingFromUri(activeSignedUri);
                        projectUpdated = true;
                    } catch (Exception projectError) {
                        projectUpdateError = message(projectError);
                    }
                }
            } catch (Exception failureCause) {
                primaryFailure = failureCause;
                error = message(failureCause);
            } finally {
                if (copy != null) {
                    try {
                        deleteTemporaryFile(copy, primaryFailure);
                    } catch (IOException cleanupError) {
                        error = error == null ? message(cleanupError)
                                : error + "\nCleanup failed: " + message(cleanupError);
                    }
                    if (primaryFailure != null) {
                        for (Throwable cleanupError : primaryFailure.getSuppressed())
                            error = error == null ? message(new IOException(cleanupError.getMessage(), cleanupError))
                                    : error + "\nCleanup failed: " + cleanupError.getMessage();
                    }
                }
            }
            final String failure = error, pkg = finalPackage, version = finalVersion;
            final long versionCode = finalVersionCode;
            final List<String> signers = finalSigners;
            final boolean signaturePass = verified;
            final boolean projectUpdatedFinal = projectUpdated;
            final String projectUpdateFailure = projectUpdateError;
            postToActivity(ui, owner, activity -> {
                activity.setBusy(false, failure == null ? "FINAL OUTPUT VERIFIED." : "FINAL OUTPUT VERIFICATION FAILED.");
                if (failure != null) {
                    activity.signedUri = null;
                    activity.signButton.setEnabled(true);
                    activity.appendReport("\nFINAL OUTPUT: FAIL\n" + failure);
                    activity.showError(failure);
                    return;
                }
                ApkUpdateCompatibility.Assessment assessment = ApkUpdateCompatibility.assess(
                        baselinePackage, baselineVersionCode, baselineSigners, pkg, versionCode, signers);
                StringBuilder finalReport = new StringBuilder("\nFINAL SIGNED OUTPUT\n");
                finalReport.append("Signature verification: ").append(signaturePass ? "PASS" : "FAIL").append('\n');
                finalReport.append("Package: ").append(pkg).append('\n');
                finalReport.append("Version: ").append(version).append(" (code ").append(versionCode).append(")\n");
                finalReport.append("Original package: ").append(baselinePackage).append('\n');
                finalReport.append("Same package ID: ").append(assessment.samePackage ? "yes" : "no").append('\n');
                finalReport.append("Version code increased: ").append(assessment.higherVersion ? "yes" : "no / unknown").append('\n');
                finalReport.append("Original signer(s): ").append(baselineSigners.isEmpty() ? "(unverified)" : baselineSigners).append('\n');
                finalReport.append("New signer(s): ").append(signers).append('\n');
                finalReport.append("Signer identity match: ").append(assessment.signerMatch ? "yes" : "no / unavailable").append('\n');
                finalReport.append("Update assessment: ").append(assessment.explanation).append('\n');
                finalReport.append("Signed output URI: ").append(activeSignedUri).append('\n');
                finalReport.append("\nThe exported signed APK is a separate file. The original APK and edited unsigned APK were not overwritten.");
                if (activeProject != null) {
                    if (projectUpdatedFinal)
                        finalReport.append("\nProject working APK: updated to signed output; previous working revision was snapshotted.");
                    else
                        finalReport.append("\nProject working APK was NOT updated: ").append(projectUpdateFailure);
                    try { activeProject.writeReport("Build-Sign-Report-r" + activeProject.getRevision(), finalReport.toString()); }
                    catch (Exception reportError) { finalReport.append("\nProject report save failed: ").append(message(reportError)); }
                }
                activity.appendReport(finalReport.toString());
                activity.status.setText("Final APK signature verified. Compatibility assessment is shown below.");
            });
        });
    }

    private static List<String> signerFingerprints(File file) throws Exception {
        ApkVerifier.Result result = new ApkVerifier.Builder(file).build().verify();
        if (!result.isVerified()) return Collections.emptyList();
        ArrayList<String> fingerprints = new ArrayList<>();
        for (X509Certificate certificate : result.getSignerCertificates()) {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) hex.append(String.format(Locale.ROOT, "%02X", value & 255));
            fingerprints.add(hex.toString());
        }
        Collections.sort(fingerprints);
        return fingerprints;
    }

    private static File copyFileToCache(Context context, File source, String label) throws Exception {
        File out = File.createTempFile("droidinspect-pipeline-" + label + "-", ".apk", context.getCacheDir());
        long total = 0;
        try (InputStream in = new java.io.FileInputStream(source);
             FileOutputStream stream = new FileOutputStream(out)) {
            byte[] buffer = new byte[32768];
            int read;
            while ((read = in.read(buffer)) != -1) {
                checkCancelled();
                total += read;
                if (total > MAX_APK_BYTES) throw new IllegalArgumentException("APK exceeds the 512 MB pipeline limit.");
                stream.write(buffer, 0, read);
            }
            stream.flush();
            return out;
        } catch (Exception | Error error) {
            deleteTemporaryFile(out, error);
            throw error;
        }
    }

    private static File copyUri(Context context, Uri uri, String label) throws Exception {
        File out = File.createTempFile("droidinspect-pipeline-" + label + "-", ".apk", context.getCacheDir());
        long total = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream stream = new FileOutputStream(out)) {
            if (in == null) throw new IllegalStateException("Unable to open selected APK document.");
            byte[] buffer = new byte[32768];
            int read;
            while ((read = in.read(buffer)) != -1) {
                checkCancelled();
                total += read;
                if (total > MAX_APK_BYTES) throw new IllegalArgumentException("APK exceeds the 512 MB pipeline limit.");
                stream.write(buffer, 0, read);
            }
            stream.flush();
            return out;
        } catch (Exception | Error error) {
            deleteTemporaryFile(out, error);
            throw error;
        }
    }

    private static void deleteTemporaryFile(File file, Throwable primaryFailure) throws IOException {
        if (file == null) return;
        try {
            java.nio.file.Files.deleteIfExists(file.toPath());
        } catch (IOException | SecurityException cleanupError) {
            IOException reported = new IOException(
                    "Could not remove temporary APK pipeline file: " + file.getName(), cleanupError);
            if (primaryFailure != null) primaryFailure.addSuppressed(reported);
            else throw reported;
        }
    }

    private static void deleteQuietly(File file) {
        if (file == null) return;
        try { java.nio.file.Files.deleteIfExists(file.toPath()); }
        catch (IOException | SecurityException ignored) { }
    }

    private void appendReport(String text) {
        report.append("\n");
        report.append(text);
    }

    private void setBusy(boolean busy, String message) {
        selectButton.setEnabled(!busy);
        editButton.setEnabled(!busy && (sourceUri != null || sourceFile != null) && editedUri == null);
        signButton.setEnabled(!busy && editedUri != null && signedUri == null);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        status.setText(message);
    }

    private void showError(String error) {
        new android.app.AlertDialog.Builder(this).setTitle("APK pipeline")
                .setMessage(error).setPositiveButton("CLOSE", null).show();
    }

    private static String message(Exception error) {
        return error.getClass().getSimpleName() + ": "
                + (error.getMessage() == null ? "No details." : error.getMessage());
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("APK pipeline operation cancelled because the activity was closed.");
    }

    private interface ActivityCallback { void deliver(ApkBuildPipelineActivity activity); }

    private static void postToActivity(Handler handler,
                                       WeakReference<ApkBuildPipelineActivity> owner,
                                       ActivityCallback callback) {
        handler.post(() -> {
            ApkBuildPipelineActivity activity = owner.get();
            if (activity == null || activity.destroyed
                    || activity.isFinishing() || activity.isDestroyed()) return;
            callback.deliver(activity);
        });
    }

    @Override protected void onDestroy() {
        destroyed = true;
        worker.shutdownNow();
        final ExecutorService activeWorker = worker;
        final File original = originalCopy;
        originalCopy = null;
        super.onDestroy();
        Thread cleanup = new Thread(() -> {
            boolean interrupted = false;
            while (!activeWorker.isTerminated()) {
                try {
                    activeWorker.awaitTermination(1, TimeUnit.DAYS);
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            deleteQuietly(original);
            if (interrupted) Thread.currentThread().interrupt();
        }, "droidinspect-pipeline-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }
}
