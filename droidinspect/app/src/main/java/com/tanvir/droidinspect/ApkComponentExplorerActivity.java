package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Xml;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.reandroid.apk.ApkModule;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.xml.base.XmlSerializable;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Structured, searchable Android manifest component and permission explorer. */
public final class ApkComponentExplorerActivity extends Activity {
    private static final int PICK_APK = 6301;
    private static final long MAX_APK_BYTES = 512L * 1024L * 1024L;
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final String[] CATEGORIES = {
            "All", "Activities", "Services", "Receivers", "Providers", "Permissions", "Features", "SDK"
    };

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayList<ComponentEntry> allEntries = new ArrayList<>();
    private final ArrayList<ComponentEntry> shownEntries = new ArrayList<>();
    private final ArrayList<String> labels = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private EditText search;
    private Spinner category;
    private ListView list;
    private TextView status, summary;
    private ProgressBar progress;
    private Button chooseButton;
    private File apkCopy;
    private String selectedCategory = "All";

    private static final class ComponentEntry {
        final String category, name;
        final StringBuilder details = new StringBuilder();
        int intentFilters;
        ComponentEntry(String category, String name) { this.category = category; this.name = name; }
        String label() { return name + "  •  " + category; }
        String report() {
            return category + "\n" + name + "\n\n" + details
                    + (intentFilters > 0 ? "\nIntent filters: " + intentFilters : "");
        }
        boolean matches(String query) {
            return name.toLowerCase(Locale.ROOT).contains(query)
                    || category.toLowerCase(Locale.ROOT).contains(query)
                    || details.toString().toLowerCase(Locale.ROOT).contains(query);
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.setBackgroundColor(0xFF101318);

        TextView title = new TextView(this);
        title.setText("APK Components and Permissions");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(19);
        root.addView(title);

        chooseButton = new Button(this);
        chooseButton.setText("SELECT APK");
        root.addView(chooseButton);

        summary = new TextView(this);
        summary.setTextColor(0xFFE5EAF0);
        summary.setTextSize(12);
        summary.setPadding(0, dp(6), 0, dp(6));
        root.addView(summary);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search components, permissions, actions, authorities…");
        root.addView(search);

        category = new Spinner(this);
        ArrayAdapter<String> categories = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CATEGORIES);
        categories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        category.setAdapter(categories);
        root.addView(category);

        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        status = new TextView(this);
        status.setTextColor(0xFF80CBC4);
        status.setPadding(0, dp(5), 0, dp(5));
        root.addView(status);

        list = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        chooseButton.setOnClickListener(v -> pickApk());
        category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = CATEGORIES[position];
                filter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
            @Override public void afterTextChanged(android.text.Editable e) { }
        });
        list.setOnItemClickListener((parent, view, position, id) ->
                new AlertDialog.Builder(this).setTitle(shownEntries.get(position).name)
                        .setMessage(shownEntries.get(position).report())
                        .setPositiveButton("CLOSE", null).show());
        status.setText("Select an APK to inspect manifest components and permissions.");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void pickApk() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.android.package-archive");
        startActivityForResult(intent, PICK_APK);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_APK || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        } catch (SecurityException ignored) { }
        loadApk(uri);
    }

    private void loadApk(Uri uri) {
        setBusy(true, "Copying APK and decoding binary manifest…");
        worker.execute(() -> {
            File copy = null;
            try {
                copy = File.createTempFile("droidinspect-components-", ".apk", getCacheDir());
                long total = 0;
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(copy)) {
                    if (in == null) throw new IllegalStateException("Unable to open selected APK.");
                    byte[] buffer = new byte[32768];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        total += n;
                        if (total > MAX_APK_BYTES) throw new IllegalArgumentException("APK exceeds the 512 MB inspection limit.");
                        out.write(buffer, 0, n);
                    }
                }
                ApkModule module = ApkModule.loadApkFile(copy);
                module.initializeAndroidFramework((Integer) null);
                if (!module.hasAndroidManifest()) throw new IllegalArgumentException("AndroidManifest.xml is missing.");
                AndroidManifestBlock manifest = module.getAndroidManifest();
                if (manifest == null) throw new IllegalArgumentException("Binary Android manifest could not be decoded.");
                String xml = XmlSerializable.toXmlString(manifest, true);
                ArrayList<ComponentEntry> parsed = parseManifest(xml);
                String pkg = manifest.getPackageName() == null ? "(unknown)" : manifest.getPackageName();
                String version = manifest.getVersionName() == null ? "(not declared)" : manifest.getVersionName();
                StringBuilder overview = new StringBuilder();
                overview.append("Package: ").append(pkg).append('\n');
                overview.append("Version: ").append(version).append('\n');
                overview.append("Manifest entries: ").append(parsed.size()).append('\n');
                overview.append("Components and permissions are searchable below.\n");
                File previous = apkCopy;
                apkCopy = copy;
                copy = null;
                if (previous != null) previous.delete();
                runOnUiThread(() -> {
                    allEntries.clear();
                    allEntries.addAll(parsed);
                    summary.setText(overview.toString());
                    category.setSelection(0);
                    filter();
                    setBusy(false, "Manifest decoded. Original APK was not modified.");
                });
            } catch (Exception error) {
                if (copy != null) copy.delete();
                String message = "MANIFEST ANALYSIS FAILED: " + error.getClass().getSimpleName()
                        + ": " + (error.getMessage() == null ? "No details." : error.getMessage());
                runOnUiThread(() -> setBusy(false, message));
            }
        });
    }

    private ArrayList<ComponentEntry> parseManifest(String xml) throws Exception {
        ArrayList<ComponentEntry> result = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(xml));
        ComponentEntry current = null;
        int componentDepth = -1;
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if (isComponent(tag)) {
                    String name = attr(parser, "name");
                    if (name.isEmpty()) name = "(unnamed " + tag + ")";
                    current = new ComponentEntry(categoryFor(tag), name);
                    addAttribute(current, "exported", attr(parser, "exported"));
                    addAttribute(current, "enabled", attr(parser, "enabled"));
                    addAttribute(current, "permission", attr(parser, "permission"));
                    addAttribute(current, "process", attr(parser, "process"));
                    addAttribute(current, "authorities", attr(parser, "authorities"));
                    addAttribute(current, "targetActivity", attr(parser, "targetActivity"));
                    addAttribute(current, "launchMode", attr(parser, "launchMode"));
                    addAttribute(current, "screenOrientation", attr(parser, "screenOrientation"));
                    addAttribute(current, "taskAffinity", attr(parser, "taskAffinity"));
                    addAttribute(current, "readPermission", attr(parser, "readPermission"));
                    addAttribute(current, "writePermission", attr(parser, "writePermission"));
                    addAttribute(current, "grantUriPermissions", attr(parser, "grantUriPermissions"));
                    addAttribute(current, "directBootAware", attr(parser, "directBootAware"));
                    result.add(current);
                    componentDepth = parser.getDepth();
                } else if (current != null && "intent-filter".equals(tag)) {
                    current.intentFilters++;
                } else if (current != null && "action".equals(tag)) {
                    addAttribute(current, "action", attr(parser, "name"));
                } else if (current != null && "category".equals(tag)) {
                    addAttribute(current, "category", attr(parser, "name"));
                } else if (tag.startsWith("uses-permission")) {
                    String name = attr(parser, "name");
                    ComponentEntry permission = new ComponentEntry("Permissions", name.isEmpty() ? "(unnamed permission)" : name);
                    addAttribute(permission, "maxSdkVersion", attr(parser, "maxSdkVersion"));
                    result.add(permission);
                } else if ("uses-feature".equals(tag)) {
                    String name = attr(parser, "name");
                    ComponentEntry feature = new ComponentEntry("Features", name.isEmpty() ? "(unnamed feature)" : name);
                    addAttribute(feature, "required", attr(parser, "required"));
                    addAttribute(feature, "version", attr(parser, "version"));
                    result.add(feature);
                } else if ("uses-sdk".equals(tag)) {
                    ComponentEntry sdk = new ComponentEntry("SDK", "SDK requirements");
                    addAttribute(sdk, "minSdkVersion", attr(parser, "minSdkVersion"));
                    addAttribute(sdk, "targetSdkVersion", attr(parser, "targetSdkVersion"));
                    addAttribute(sdk, "maxSdkVersion", attr(parser, "maxSdkVersion"));
                    result.add(sdk);
                }
            } else if (event == XmlPullParser.END_TAG && current != null
                    && parser.getDepth() == componentDepth) {
                current = null;
                componentDepth = -1;
            }
        }
        return result;
    }

    private boolean isComponent(String tag) {
        return "activity".equals(tag) || "activity-alias".equals(tag)
                || "service".equals(tag) || "receiver".equals(tag) || "provider".equals(tag);
    }

    private String categoryFor(String tag) {
        if ("activity".equals(tag) || "activity-alias".equals(tag)) return "Activities";
        if ("service".equals(tag)) return "Services";
        if ("receiver".equals(tag)) return "Receivers";
        return "Providers";
    }

    private String attr(XmlPullParser parser, String name) {
        String value = parser.getAttributeValue(ANDROID_NS, name);
        return value == null ? "" : value;
    }

    private void addAttribute(ComponentEntry entry, String name, String value) {
        if (value != null && !value.isEmpty()) entry.details.append(name).append(": ").append(value).append('\n');
    }

    private void filter() {
        String query = search.getText() == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
        shownEntries.clear();
        labels.clear();
        for (ComponentEntry entry : allEntries) {
            boolean categoryMatch = "All".equals(selectedCategory) || selectedCategory.equals(entry.category);
            if (categoryMatch && (query.isEmpty() || entry.matches(query))) {
                shownEntries.add(entry);
                labels.add(entry.label() + (entry.intentFilters > 0 ? "  •  " + entry.intentFilters + " intent filter(s)" : ""));
            }
        }
        adapter.notifyDataSetChanged();
        status.setText("Showing " + shownEntries.size() + " of " + allEntries.size() + " manifest records");
    }

    private void setBusy(boolean busy, String message) {
        chooseButton.setEnabled(!busy);
        search.setEnabled(!busy);
        category.setEnabled(!busy && !allEntries.isEmpty());
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        status.setText(message);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
        if (apkCopy != null) apkCopy.delete();
    }
}
