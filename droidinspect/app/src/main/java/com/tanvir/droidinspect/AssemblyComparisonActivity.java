package com.tanvir.droidinspect;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Structural .NET assembly comparison. It deliberately does not claim semantic equivalence. */
public final class AssemblyComparisonActivity extends Activity {
    private static final int PICK_LEFT = 6101, PICK_RIGHT = 6102;
    private static final long MAX_INPUT = 512L * 1024L * 1024L;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Uri leftUri, rightUri;
    private String leftName = "Baseline assembly", rightName = "Candidate assembly";
    private Button leftButton, rightButton, compareButton;
    private TextView status, report;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(0xFF101318);

        TextView title = new TextView(this);
        title.setText("Assembly Comparison");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title);

        leftButton = button("SELECT BASELINE DLL / EXE");
        rightButton = button("SELECT CANDIDATE DLL / EXE");
        compareButton = button("COMPARE ASSEMBLIES");
        compareButton.setEnabled(false);
        root.addView(leftButton);
        root.addView(rightButton);
        root.addView(compareButton);

        status = new TextView(this);
        status.setText("Select two managed assemblies. Original files are read-only.");
        status.setTextColor(0xFF80CBC4);
        status.setPadding(0, dp(8), 0, dp(8));
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

        leftButton.setOnClickListener(v -> pick(PICK_LEFT));
        rightButton.setOnClickListener(v -> pick(PICK_RIGHT));
        compareButton.setOnClickListener(v -> compare());
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        return b;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void pick(int request) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, request);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String name = queryName(uri);
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        } catch (SecurityException ignored) { }
        if (request == PICK_LEFT) {
            leftUri = uri;
            leftName = name;
            leftButton.setText("BASELINE: " + name);
        } else if (request == PICK_RIGHT) {
            rightUri = uri;
            rightName = name;
            rightButton.setText("CANDIDATE: " + name);
        }
        compareButton.setEnabled(leftUri != null && rightUri != null);
    }

    private String queryName(Uri uri) {
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int i = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (i >= 0 && cursor.getString(i) != null) return cursor.getString(i);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String name = uri.getLastPathSegment();
        return name == null ? "Selected assembly" : name;
    }

    private void compare() {
        if (leftUri == null || rightUri == null) return;
        leftButton.setEnabled(false);
        rightButton.setEnabled(false);
        compareButton.setEnabled(false);
        status.setText("Copying and analyzing both assemblies…");
        worker.execute(() -> {
            File left = null, right = null;
            try {
                left = copy(leftUri, "droidinspect-compare-left-");
                right = copy(rightUri, "droidinspect-compare-right-");
                String leftMetadata = FileInspectionEngine.inspectPe(left, leftName);
                String rightMetadata = FileInspectionEngine.inspectPe(right, rightName);
                if (!leftMetadata.contains(".NET CLI header: PRESENT")
                        || !rightMetadata.contains(".NET CLI header: PRESENT"))
                    throw new IllegalArgumentException("Both selected files must contain a valid managed .NET CLI header.");
                DotNetIlReader.Result leftIl = DotNetIlReader.read(left);
                DotNetIlReader.Result rightIl = DotNetIlReader.read(right);
                String output = buildReport(leftMetadata, rightMetadata, leftIl, rightIl);
                runOnUiThread(() -> {
                    report.setText(output);
                    status.setText("Structural comparison finished. Original assemblies were not modified.");
                    setButtons(true);
                });
            } catch (Exception error) {
                String message = "COMPARISON FAILED\n\n" + error.getClass().getSimpleName()
                        + ": " + (error.getMessage() == null ? "No details." : error.getMessage())
                        + "\n\nNo source assembly was modified.";
                runOnUiThread(() -> {
                    report.setText(message);
                    status.setText("Comparison failed.");
                    setButtons(true);
                });
            } finally {
                if (left != null) left.delete();
                if (right != null) right.delete();
            }
        });
    }

    private File copy(Uri uri, String prefix) throws Exception {
        File out = File.createTempFile(prefix, ".dll", getCacheDir());
        long total = 0;
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream stream = new FileOutputStream(out)) {
            if (in == null) throw new IllegalStateException("Unable to open selected document.");
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > MAX_INPUT) throw new IllegalArgumentException("Assembly exceeds the 512 MB comparison limit.");
                stream.write(buffer, 0, n);
            }
            stream.flush();
            return out;
        } catch (Exception error) {
            out.delete();
            throw error;
        }
    }

    private void setButtons(boolean enabled) {
        leftButton.setEnabled(enabled);
        rightButton.setEnabled(enabled);
        compareButton.setEnabled(enabled && leftUri != null && rightUri != null);
    }

    private String buildReport(String leftMetadata, String rightMetadata,
                               DotNetIlReader.Result leftIl, DotNetIlReader.Result rightIl) {
        StringBuilder out = new StringBuilder();
        out.append("DROIDINSPECT — STRUCTURAL ASSEMBLY COMPARISON\n\n");
        out.append("BASELINE: ").append(leftName).append("\nCANDIDATE: ").append(rightName).append("\n\n");
        appendIdentity(out, leftMetadata, rightMetadata);
        compareSection(out, "ASSEMBLY REFERENCES", leftMetadata, rightMetadata);
        compareSection(out, "TYPES AND MEMBERS", leftMetadata, rightMetadata);
        compareSection(out, "PROPERTIES", leftMetadata, rightMetadata);
        compareSection(out, "EVENTS", leftMetadata, rightMetadata);
        compareSection(out, "MANIFEST RESOURCES", leftMetadata, rightMetadata);
        compareMethods(out, leftIl, rightIl);
        out.append("\nINTERPRETATION\n");
        out.append("This report compares decoded metadata records and available raw IL bodies. ");
        out.append("It does not prove semantic equivalence, behavioral equivalence, or binary compatibility.\n");
        out.append("Items omitted by parser display limits or unsupported metadata encodings may not appear.\n");
        return out.toString();
    }

    private void appendIdentity(StringBuilder out, String left, String right) {
        out.append("ASSEMBLY IDENTITY\n");
        String a = section(left, "ASSEMBLY IDENTITY", "ASSEMBLY REFERENCES");
        String b = section(right, "ASSEMBLY IDENTITY", "ASSEMBLY REFERENCES");
        if (a.equals(b)) out.append("Identity metadata: unchanged\n");
        else {
            out.append("Identity metadata: DIFFERENT\n");
            out.append("Baseline:\n").append(a).append("\nCandidate:\n").append(b).append('\n');
        }
        out.append('\n');
    }

    private void compareSection(StringBuilder out, String heading, String left, String right) {
        Set<String> a = sectionEntries(section(left, heading, nextHeading(heading)));
        Set<String> b = sectionEntries(section(right, heading, nextHeading(heading)));
        appendSetDiff(out, heading, a, b);
    }

    private String nextHeading(String heading) {
        if ("ASSEMBLY REFERENCES".equals(heading)) return "MODULE REFERENCES";
        if ("TYPES AND MEMBERS".equals(heading)) return "PROPERTIES";
        if ("PROPERTIES".equals(heading)) return "EVENTS";
        if ("EVENTS".equals(heading)) return "MANIFEST RESOURCES";
        return "Decode status:";
    }

    private Set<String> sectionEntries(String block) {
        Set<String> values = new TreeSet<>();
        if (block == null || block.isEmpty()) return values;
        for (String raw : block.split("\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("TypeDef rows:") || line.startsWith("PropertyMap rows:")
                    || line.startsWith("EventMap rows:") || line.startsWith("…")) continue;
            if (line.matches("^\\d+\\. .*")) line = line.replaceFirst("^\\d+\\. ", "");
            values.add(line);
        }
        return values;
    }

    private void appendSetDiff(StringBuilder out, String heading, Set<String> left, Set<String> right) {
        Set<String> added = new TreeSet<>(right);
        added.removeAll(left);
        Set<String> removed = new TreeSet<>(left);
        removed.removeAll(right);
        out.append(heading).append("\n");
        out.append("Baseline entries: ").append(left.size()).append("; candidate entries: ").append(right.size()).append('\n');
        out.append("Added in candidate: ").append(added.size()).append('\n');
        for (String item : added) out.append("  + ").append(item).append('\n');
        out.append("Removed from candidate: ").append(removed.size()).append('\n');
        for (String item : removed) out.append("  - ").append(item).append('\n');
        if (added.isEmpty() && removed.isEmpty()) out.append("No structural line differences detected.\n");
        out.append('\n');
    }

    private void compareMethods(StringBuilder out, DotNetIlReader.Result left, DotNetIlReader.Result right) {
        Map<String, List<String>> a = methodBodies(left);
        Map<String, List<String>> b = methodBodies(right);
        Set<String> keys = new TreeSet<>();
        keys.addAll(a.keySet());
        keys.addAll(b.keySet());
        int added = 0, removed = 0, changed = 0, same = 0;
        List<String> details = new ArrayList<>();
        for (String key : keys) {
            List<String> x = a.get(key), y = b.get(key);
            if (x == null) { added += y.size(); details.add("  + " + key + " (" + y.size() + " method body record(s))"); }
            else if (y == null) { removed += x.size(); details.add("  - " + key + " (" + x.size() + " method body record(s))"); }
            else if (x.equals(y)) same += Math.min(x.size(), y.size());
            else {
                int paired = Math.min(x.size(), y.size());
                changed += paired;
                if (y.size() > x.size()) added += y.size() - x.size();
                if (x.size() > y.size()) removed += x.size() - y.size();
                details.add("  * " + key + " — raw IL body differs (" + x.size() + " baseline / " + y.size() + " candidate)");
            }
        }
        out.append("METHOD IL COMPARISON\n");
        out.append("Baseline methods with bodies: ").append(left.methods.size())
                .append("; candidate methods with bodies: ").append(right.methods.size()).append('\n');
        out.append("Unchanged body records: ").append(same).append("; changed body records: ")
                .append(changed).append("; added: ").append(added).append("; removed: ").append(removed).append('\n');
        for (String detail : details) out.append(detail).append('\n');
        out.append('\n');
    }

    private Map<String, List<String>> methodBodies(DotNetIlReader.Result result) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        for (DotNetIlReader.MethodInfo method : result.methods) {
            String key = method.owner + " :: " + method.name;
            values.computeIfAbsent(key, ignored -> new ArrayList<>()).add(method.codeHex == null ? "" : method.codeHex);
        }
        for (List<String> bodies : values.values()) Collections.sort(bodies);
        return values;
    }

    private String section(String source, String start, String end) {
        if (source == null) return "";
        int from = source.indexOf(start);
        if (from < 0) return "";
        from = source.indexOf('\n', from);
        if (from < 0) return "";
        int to = source.indexOf(end, from + 1);
        return source.substring(from + 1, to < 0 ? source.length() : to).trim();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
    }
}
