package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.zip.ZipFile;

public class DexAnalysisActivity extends Activity {
    private static final long MAX_APK_COPY = 512L * 1024L * 1024L;
    private final ArrayList<DexFileParser.ClassInfo> allClasses = new ArrayList<>();
    private final ArrayList<DexFileParser.ClassInfo> shownClasses = new ArrayList<>();
    private final ArrayList<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView summary;
    private TextView status;
    private ProgressBar progress;
    private EditText search;
    private ListView list;
    private Spinner packageSpinner;
    private String selectedPackage = "All packages";
    private File apkCopy;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_dex_analysis);
        summary = findViewById(R.id.dexSummary);
        status = findViewById(R.id.dexStatus);
        progress = findViewById(R.id.dexProgress);
        search = findViewById(R.id.dexSearch);
        list = findViewById(R.id.dexClassList);
        packageSpinner = findViewById(R.id.dexPackageSpinner);
        packageSpinner.setEnabled(false);
        packageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPackage = parent.getItemAtPosition(position).toString();
                filter(search.getText().toString());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> showClass(shownClasses.get(position)));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable e) { }
        });
        String raw = getIntent().getStringExtra("apk_uri");
        if (raw == null) {
            status.setText("No APK document was supplied.");
            return;
        }
        analyze(Uri.parse(raw));
    }

    private void analyze(Uri uri) {
        setBusy(true, "Copying APK and parsing DEX files…");
        new Thread(() -> {
            String failure = null;
            DexFileParser.Result result = null;
            try {
                apkCopy = File.createTempFile("droidinspect-apk-", ".apk", getCacheDir());
                long total = 0;
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(apkCopy)) {
                    if (in == null) throw new IllegalStateException("Android could not open the selected APK.");
                    byte[] buffer = new byte[32768];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        total += n;
                        if (total > MAX_APK_COPY) throw new IllegalArgumentException("APK exceeds the 512 MB inspection limit.");
                        out.write(buffer, 0, n);
                    }
                }
                result = DexFileParser.analyzeApk(apkCopy);
            } catch (Exception e) {
                failure = e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "No details." : e.getMessage());
            }
            final DexFileParser.Result parsed = result;
            final String error = failure;
            runOnUiThread(() -> {
                setBusy(false, error == null ? "DEX analysis finished." : "DEX analysis failed.");
                if (error != null) {
                    summary.setText("DEX ANALYSIS FAILED\n\n" + error + "\n\nThe original APK was not modified.");
                    return;
                }
                allClasses.clear();
                allClasses.addAll(parsed.classes);
                LinkedHashSet<String> packages = new LinkedHashSet<>();
                packages.add("All packages");
                for (DexFileParser.ClassInfo info : allClasses) packages.add(info.packageName);
                ArrayAdapter<String> packageAdapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, new ArrayList<>(packages));
                packageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                packageSpinner.setAdapter(packageAdapter);
                packageSpinner.setEnabled(true);
                StringBuilder report = new StringBuilder();
                report.append("DEX files found: ").append(parsed.dexCount).append('\n');
                report.append("Classes parsed: ").append(parsed.totalClasses).append('\n');
                report.append("Members indexed: ").append(parsed.totalMembers).append('\n');
                report.append("Class limit: ").append(parsed.classLimitReached ? "REACHED" : "not reached").append('\n');
                report.append("Member limit: ").append(parsed.memberLimitReached ? "REACHED" : "not reached").append("\n\n");
                report.append("DEX FILE RESULTS\n");
                for (String line : parsed.dexReports) report.append("• ").append(line).append('\n');
                if (!parsed.warning.isEmpty()) report.append("\nNOTE: ").append(parsed.warning);
                report.append("\n\nSelect a class to inspect its fields and methods.\n");
                report.append("This view shows DEX metadata, not reconstructed Java source.");
                summary.setText(report.toString());
                filter(search.getText().toString());
            });
        }).start();
    }

    private void filter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        shownClasses.clear();
        rows.clear();
        for (DexFileParser.ClassInfo info : allClasses) {
            boolean packageMatch = "All packages".equals(selectedPackage) || selectedPackage.equals(info.packageName);
            boolean textMatch = q.isEmpty() || info.className.toLowerCase(Locale.ROOT).contains(q)
                    || info.packageName.toLowerCase(Locale.ROOT).contains(q)
                    || contains(info.fields, q) || contains(info.methods, q);
            boolean match = packageMatch && textMatch;
            if (match) {
                shownClasses.add(info);
                rows.add(info.className + "\n" + info.packageName + "  •  " + info.dexName
                        + "\nFields: " + info.fields.size() + "   Methods: " + info.methods.size());
            }
        }
        adapter.notifyDataSetChanged();
        status.setText("Showing " + shownClasses.size() + " of " + allClasses.size() + " classes");
    }

    private boolean contains(List<String> values, String q) {
        for (String value : values) if (value.toLowerCase(Locale.ROOT).contains(q)) return true;
        return false;
    }

    private void showClass(DexFileParser.ClassInfo info) {
        StringBuilder text = new StringBuilder();
        text.append(info.access).append(info.className).append('\n');
        text.append("DEX: ").append(info.dexName).append('\n');
        text.append("Superclass: ").append(info.superClass).append("\n\n");
        text.append("FIELDS (").append(info.fields.size()).append(")\n");
        if (info.fields.isEmpty()) text.append("(none)\n");
        for (String field : info.fields) text.append("• ").append(field).append('\n');
        text.append("\nMETHODS (").append(info.methods.size()).append(")\n");
        if (info.methods.isEmpty()) text.append("(none)\n");
        for (String method : info.methods) text.append("• ").append(method).append('\n');
        text.append("\nMethod bodies / Java decompilation: not implemented in this milestone build.");
        new AlertDialog.Builder(this).setTitle(info.className).setMessage(text.toString())
                .setPositiveButton("Close", null).show();
    }

    private void setBusy(boolean busy, String message) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        search.setEnabled(!busy);
        packageSpinner.setEnabled(!busy && !allClasses.isEmpty());
        status.setText(message);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (apkCopy != null) {
            //noinspection ResultOfMethodCallIgnored
            apkCopy.delete();
        }
    }
}
