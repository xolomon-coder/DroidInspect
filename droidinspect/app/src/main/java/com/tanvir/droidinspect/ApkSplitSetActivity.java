package com.tanvir.droidinspect;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.reandroid.apk.ApkModule;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.xml.base.XmlSerializable;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Offline inventory and consistency checks for a user-selected base/split APK set. */
public final class ApkSplitSetActivity extends Activity {
    private static final int PICK_SET = 6401;
    private static final int MAX_FILES = 20;
    private static final long MAX_TOTAL_BYTES = 1024L * 1024L * 1024L;
    private static final long MAX_FILE_BYTES = 512L * 1024L * 1024L;
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean busy;
    private volatile boolean destroyed;
    private final ArrayList<File> copies = new ArrayList<>();
    private Button choose;
    private ProgressBar progress;
    private TextView status, report;

    private static final class SplitInfo {
        String file, packageName, versionName, splitName, configForSplit, featureSplit;
        long versionCode, size;
        int dexCount, components, permissions, resourcePackages;
        final List<String> dependencies = new ArrayList<>();
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(0xFF101318);

        TextView title = new TextView(this);
        title.setText("Split APK Set Analyzer");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        root.addView(title);

        choose = new Button(this);
        choose.setText("SELECT BASE APK + SPLIT APK FILES");
        root.addView(choose);

        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        status = new TextView(this);
        status.setTextColor(0xFF80CBC4);
        status.setPadding(0, dp(6), 0, dp(6));
        status.setText("Select multiple APK files from one installed-app split set.");
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

        choose.setOnClickListener(v -> pickSet());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void pickSet() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.android.package-archive");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_SET);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_SET || result != RESULT_OK || data == null) return;
        ArrayList<Uri> uris = new ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null) uris.add(uri);
            }
        } else if (data.getData() != null) uris.add(data.getData());
        if (uris.isEmpty()) return;
        if (uris.size() > MAX_FILES) {
            status.setText("Too many files. Select no more than " + MAX_FILES + " APKs per set.");
            return;
        }
        for (Uri uri : uris) {
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            } catch (SecurityException ignored) { }
        }
        analyze(uris);
    }

    private void analyze(List<Uri> uris) {
        if (busy) return;
        setBusy(true, "Copying and analyzing " + uris.size() + " APK file(s)…");
        final ArrayList<Uri> sources = new ArrayList<>(uris);
        final Context app = getApplicationContext();
        final File cacheDir = app.getCacheDir();
        final WeakReference<ApkSplitSetActivity> owner = new WeakReference<>(this);
        final Handler ui = mainHandler;
        worker.execute(() -> {
            ArrayList<SplitInfo> infos = new ArrayList<>();
            ArrayList<File> newCopies = new ArrayList<>();
            long total = 0;
            Throwable problem = null;
            try {
                for (Uri uri : sources) {
                    checkCancelled();
                    File copy = File.createTempFile("droidinspect-split-", ".apk", cacheDir);
                    newCopies.add(copy);
                    long copied = 0;
                    try (InputStream in = app.getContentResolver().openInputStream(uri);
                         FileOutputStream out = new FileOutputStream(copy)) {
                        if (in == null) throw new IOException("Unable to open one selected APK.");
                        byte[] buffer = new byte[32768];
                        int n;
                        while ((n = in.read(buffer)) != -1) {
                            checkCancelled();
                            copied += n;
                            total += n;
                            if (copied > MAX_FILE_BYTES || total > MAX_TOTAL_BYTES)
                                throw new IOException("Selected APK set exceeds the 512 MB per-file or 1 GB total safety limit.");
                            out.write(buffer, 0, n);
                        }
                        out.flush();
                        out.getFD().sync();
                    }
                    infos.add(inspectSplit(copy, queryName(app, uri)));
                }
                checkCancelled();
            } catch (Exception | Error error) {
                problem = error;
                for (File file : newCopies) deleteOwnedFile(file, cacheDir, problem);
                newCopies.clear();
            }
            final ArrayList<File> readyCopies = new ArrayList<>(newCopies);
            final String output = problem == null ? buildReport(infos, total) : null;
            final String errorText = problem == null ? null : describe(problem);
            ui.post(() -> {
                ApkSplitSetActivity activity = owner.get();
                if (activity == null || activity.destroyed || activity.isFinishing() || activity.isDestroyed()) {
                    for (File file : readyCopies) deleteOwnedFileAsync(file, cacheDir);
                    return;
                }
                if (errorText != null) {
                    activity.setBusy(false, "Split APK set analysis failed.");
                    activity.status.setText("SPLIT APK SET ANALYSIS FAILED: " + errorText);
                    for (File file : readyCopies) deleteOwnedFileAsync(file, cacheDir);
                    return;
                }
                ArrayList<File> previous = new ArrayList<>(activity.copies);
                activity.copies.clear();
                activity.copies.addAll(readyCopies);
                for (File file : previous) deleteOwnedFileAsync(file, cacheDir);
                activity.report.setText(output);
                activity.setBusy(false, "Split APK set inventory finished. No APK was modified or installed.");
            });
        });
    }

    private static SplitInfo inspectSplit(File file, String displayName) throws Exception {
        SplitInfo info = new SplitInfo();
        info.file = displayName;
        info.size = file.length();
        ApkModule module = ApkModule.loadApkFile(file);
        module.initializeAndroidFramework((Integer) null);
        if (!module.hasAndroidManifest()) throw new IllegalArgumentException(info.file + " has no AndroidManifest.xml.");
        AndroidManifestBlock manifest = module.getAndroidManifest();
        if (manifest == null) throw new IllegalArgumentException(info.file + " has an undecodable binary manifest.");
        info.packageName = safe(manifest.getPackageName());
        info.versionName = safe(manifest.getVersionName());
        Integer version = manifest.getVersionCode();
        info.versionCode = version == null ? -1 : version;
        if (module.getTableBlock() != null) info.resourcePackages = module.getTableBlock().size();
        String xml = XmlSerializable.toXmlString(manifest, true);
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(xml));
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            checkCancelled();
            if (event != XmlPullParser.START_TAG) continue;
            String tag = parser.getName();
            if ("manifest".equals(tag)) {
                info.splitName = value(parser.getAttributeValue(null, "split"));
                info.featureSplit = value(parser.getAttributeValue(ANDROID_NS, "isFeatureSplit"));
                info.configForSplit = value(parser.getAttributeValue(ANDROID_NS, "configForSplit"));
            } else if (isComponent(tag)) {
                info.components++;
            } else if (tag.startsWith("uses-permission")) {
                info.permissions++;
            } else if ("uses-split".equals(tag)) {
                String dependency = parser.getAttributeValue(ANDROID_NS, "name");
                if (dependency != null) info.dependencies.add(dependency);
            }
        }
        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                checkCancelled();
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().matches("classes([0-9]+)?\\.dex")) info.dexCount++;
            }
        }
        return info;
    }

    private static String queryName(Context context, Uri uri) {
        android.database.Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (index >= 0 && cursor.getString(index) != null) return cursor.getString(index);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String fallback = uri.getLastPathSegment();
        return fallback == null ? "Selected APK" : fallback;
    }

    private static boolean isComponent(String tag) {
        return "activity".equals(tag) || "activity-alias".equals(tag) || "service".equals(tag)
                || "receiver".equals(tag) || "provider".equals(tag);
    }

    private static String buildReport(List<SplitInfo> infos, long totalBytes) {
        StringBuilder out = new StringBuilder("DROIDINSPECT — SPLIT APK SET INVENTORY\n\n");
        out.append("Files selected: ").append(infos.size()).append('\n');
        out.append("Total copied size: ").append(totalBytes).append(" bytes\n");
        Set<String> packageNames = new HashSet<>();
        Set<Long> versions = new HashSet<>();
        Set<String> splitNames = new HashSet<>();
        int bases = 0;
        for (SplitInfo info : infos) {
            packageNames.add(info.packageName);
            versions.add(info.versionCode);
            if (info.splitName.isEmpty()) bases++;
            else if (!splitNames.add(info.splitName)) out.append("WARNING: duplicate split name: ").append(info.splitName).append('\n');
        }
        boolean packageKnown = packageNames.size() == 1 && !packageNames.contains("(unknown)");
        boolean versionKnown = versions.size() == 1 && !versions.contains(-1L);
        out.append("Package identities: ").append(packageNames.size())
                .append(packageKnown ? " (consistent)" : " (MISMATCH / unknown)").append('\n');
        out.append("Version codes: ").append(versions.size())
                .append(versionKnown ? " (consistent)" : " (MISMATCH / unknown)").append('\n');
        out.append("Base APK candidates: ").append(bases).append('\n');
        if (bases != 1) out.append("WARNING: a conventional split set should contain exactly one base APK.\n");
        if (!packageKnown || !versionKnown)
            out.append("WARNING: these files may not belong to the same installable package version, or identity data is unavailable.\n");

        for (SplitInfo info : infos) {
            out.append("\n").append("==================================================\n");
            out.append(info.file).append('\n');
            out.append("Role: ").append(info.splitName.isEmpty() ? "BASE APK candidate" : "SPLIT APK").append('\n');
            out.append("Split name: ").append(info.splitName.isEmpty() ? "(base)" : info.splitName).append('\n');
            out.append("Feature split: ").append(info.featureSplit.isEmpty() ? "(not declared)" : info.featureSplit).append('\n');
            out.append("Config for split: ").append(info.configForSplit.isEmpty() ? "(not declared)" : info.configForSplit).append('\n');
            out.append("Package: ").append(info.packageName).append('\n');
            out.append("Version: ").append(info.versionName).append(" (code ").append(info.versionCode).append(")\n");
            out.append("Size: ").append(info.size).append(" bytes\n");
            out.append("Root DEX files: ").append(info.dexCount).append('\n');
            out.append("Manifest components: ").append(info.components).append('\n');
            out.append("Declared permissions: ").append(info.permissions).append('\n');
            out.append("Resource packages: ").append(info.resourcePackages).append('\n');
            if (!info.dependencies.isEmpty()) out.append("Uses-split dependencies: ").append(info.dependencies).append('\n');
        }
        out.append("\nLIMITS\n");
        out.append("This tool inventories and consistency-checks selected split APK manifests and archive entries. ");
        out.append("It does not merge APKs, install the set, resolve device-specific split selection, or prove installability.\n");
        out.append("All selected APKs are copied to cache for read-only analysis; original files remain unchanged.");
        return out.toString();
    }

    private static String value(String value) { return value == null ? "" : value; }
    private static String safe(String value) { return value == null ? "(unknown)" : value; }

    private void setBusy(boolean busyValue, String message) {
        busy = busyValue;
        choose.setEnabled(!busyValue);
        progress.setVisibility(busyValue ? View.VISIBLE : View.GONE);
        status.setText(message);
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedIOException("Split APK analysis cancelled because the activity was closed.");
    }

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
                    || !absolute.getName().startsWith("droidinspect-split-")
                    || Files.isSymbolicLink(absolute.toPath()))
                throw new IOException("Refusing to delete a split APK copy outside private cache.");
            Files.deleteIfExists(absolute.toPath());
            if (absolute.exists()) throw new IOException("Split APK copy remains after deletion.");
        } catch (IOException | RuntimeException cleanupError) {
            IOException reported = cleanupError instanceof IOException ? (IOException) cleanupError
                    : new IOException("Split APK temporary cleanup failed.", cleanupError);
            if (primary != null) primary.addSuppressed(reported);
            else Log.e("DroidInspectSplitSet", "Temporary file could not be removed: " + file, reported);
        }
    }

    private static void deleteOwnedFileAsync(File file, File cacheDir) {
        if (file == null) return;
        Thread cleanup = new Thread(() -> deleteOwnedFile(file, cacheDir, null), "droidinspect-split-file-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        final ExecutorService executor = worker;
        final File cacheDir = getCacheDir();
        final ArrayList<File> ownedFiles = new ArrayList<>(copies);
        copies.clear();
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
            for (File file : ownedFiles) deleteOwnedFile(file, cacheDir, null);
            if (interrupted) Thread.currentThread().interrupt();
        }, "droidinspect-split-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }
}
