package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Unified recent-project launcher for APK, C# source and offline dependency projects. */
public final class ProjectWorkspaceActivity extends Activity {
    private final ArrayList<ProjectEntry> entries = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private LinearLayout root;
    private TextView status;
    private ArrayAdapter<ProjectEntry> adapter;
    private volatile boolean destroyed;
    private boolean skipInitialResumeRefresh;
    private int loadGeneration;

    private static final class ProjectEntry {
        final String kind, id, name;
        final long updated;
        ProjectEntry(String kind, String id, String name, long updated) {
            this.kind=kind; this.id=id; this.name=name; this.updated=updated;
        }
        @Override public String toString() { return kind + "  •  " + name + "\n" + id; }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        showProjects();
    }

    @Override protected void onResume() {
        super.onResume();
        if (skipInitialResumeRefresh) {
            skipInitialResumeRefresh = false;
            return;
        }
        if (root != null) loadProjects();
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private void showProjects() {
        skipInitialResumeRefresh = true;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(0xFF101318);
        TextView title = new TextView(this);
        title.setText("DroidInspect — Project Workspace");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(21);
        root.addView(title);
        status = new TextView(this);
        status.setTextColor(0xFF80CBC4);
        status.setText("Project indexing runs in the background. Project data remains in private app storage.");
        root.addView(status);
        Button apk = new Button(this);
        apk.setText("OPEN APK PROJECTS");
        apk.setOnClickListener(v -> startActivity(new Intent(this, ApkProjectWorkspaceActivity.class)));
        root.addView(apk);
        Button source = new Button(this);
        source.setText("OPEN LAST C# SOURCE PROJECT");
        source.setEnabled(CSharpSourceWorkspace.getLastProjectId(this) != null);
        source.setOnClickListener(v -> openSource(CSharpSourceWorkspace.getLastProjectId(this)));
        root.addView(source);
        Button deps = new Button(this);
        deps.setText("OPEN LAST .NET DEPENDENCY PROJECT");
        deps.setEnabled(DotNetDependencyWorkspace.getLastProjectId(this) != null);
        deps.setOnClickListener(v -> openDependencies(DotNetDependencyWorkspace.getLastProjectId(this)));
        root.addView(deps);

        TextView heading = new TextView(this);
        heading.setText("RECENT PROJECTS");
        heading.setTextColor(0xFFFFFFFF);
        heading.setPadding(0, dp(12), 0, dp(6));
        root.addView(heading);
        ListView list = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, entries);
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        list.setOnItemClickListener((parent, view, position, row) -> open(entries.get(position)));
        list.setOnItemLongClickListener((parent, view, position, row) -> {
            manageProject(entries.get(position));
            return true;
        });
        Button tools = new Button(this);
        tools.setText("PROJECT NOTES / BOOKMARKS / SAVED SEARCHES / RECOVERY");
        tools.setOnClickListener(v -> {
            if (entries.isEmpty()) status.setText("No saved projects are available.");
            else manageProject(entries.get(0));
        });
        root.addView(tools);
        setContentView(root);
        loadProjects();
    }

    /** Scan persistent project folders away from the UI thread; stale results are discarded. */
    private void loadProjects() {
        if (destroyed || status == null || adapter == null) return;
        final int request = ++loadGeneration;
        status.setText("Scanning recent APK, C# source and dependency projects…");
        final android.content.Context appContext = getApplicationContext();
        final WeakReference<ProjectWorkspaceActivity> screenRef = new WeakReference<>(this);
        final Handler handler = mainHandler;
        worker.execute(() -> {
            ArrayList<ProjectEntry> discovered = new ArrayList<>();
            StringBuilder warnings = new StringBuilder();
            try {
                discovered = collectProjects(appContext, warnings);
            } catch (RuntimeException error) {
                warnings.append("PROJECT INDEX WARNING: ").append(error.getMessage());
            }
            final ArrayList<ProjectEntry> result = discovered;
            final String warning = warnings.toString();
            handler.post(() -> {
                ProjectWorkspaceActivity screen = screenRef.get();
                if (screen == null || screen.destroyed || screen.isFinishing()
                        || screen.isDestroyed() || request != screen.loadGeneration) return;
                screen.entries.clear();
                screen.entries.addAll(result);
                screen.adapter.notifyDataSetChanged();
                String message = "Recent projects: " + screen.entries.size();
                screen.status.setText(warning.isEmpty() ? message : message + "\\n" + warning);
            });
        });
    }

    private static ArrayList<ProjectEntry> collectProjects(
            android.content.Context context, StringBuilder warnings) {
        ArrayList<ProjectEntry> result = new ArrayList<>();
        for (ApkProjectWorkspace.ProjectSummary project : ApkProjectWorkspace.listRecentProjects(context))
            result.add(new ProjectEntry("APK", project.id, project.name, project.updated));
        addIndexedDirectory(result, new File(context.getFilesDir(), "csharp-projects"),
                ".droidinspect-project", "C# SOURCE",
                new String[]{"src", "original/OriginalAssembly.bin"}, warnings);
        addIndexedDirectory(result, new File(context.getFilesDir(), "dotnet-dependency-projects"),
                ".droidinspect-dependency-project", ".NET DEPENDENCIES",
                new String[]{"OriginalAssembly.bin", "local-dependencies"}, warnings);
        Collections.sort(result, (a,b) -> Long.compare(b.updated, a.updated));
        return result;
    }

    private static void addIndexedDirectory(List<ProjectEntry> target, File base,
                                            String markerName, String kind, String[] required,
                                            StringBuilder warnings) {
        try {
            for (ProjectWorkspaceIndex.Entry item : ProjectWorkspaceIndex.scan(base, markerName, kind, required))
                target.add(new ProjectEntry(item.kind, item.id,
                        item.name + "  [" + item.state + "]", item.updated));
        } catch (Exception error) {
            if (warnings.length() > 0) warnings.append('\n');
            warnings.append(kind).append(" index: ").append(error.getMessage());
        }
    }

    private File projectRoot(ProjectEntry entry) {
        String folder;
        if ("APK".equals(entry.kind)) folder = "apk-projects";
        else if ("C# SOURCE".equals(entry.kind)) folder = "csharp-projects";
        else folder = "dotnet-dependency-projects";
        return new File(new File(getFilesDir(), folder), entry.id);
    }

    private void manageProject(ProjectEntry entry) {
        final ProjectWorkspaceMetadata metadata;
        try {
            metadata = new ProjectWorkspaceMetadata(projectRoot(entry));
        } catch (Exception error) {
            status.setText("PROJECT METADATA ERROR: " + error.getMessage());
            return;
        }
        String[] actions = {"View project notes, bookmarks and history",
                "Add note", "Add bookmark", "Save search", "Record operation / recovery status",
                "Mark last operation finished"};
        new AlertDialog.Builder(this).setTitle(entry.kind + " — " + entry.name)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) showOverview(metadata);
                    else if (which == 1) promptMetadata(metadata, "Add project note", "Note text", value -> metadata.appendNote(value));
                    else if (which == 2) promptPair(metadata, "Add bookmark", "Label", "Target / token / file", (a,b) -> metadata.addBookmark(a,b));
                    else if (which == 3) promptPair(metadata, "Save search", "Search query", "Search scope", (a,b) -> metadata.saveSearch(a,b));
                    else if (which == 4) promptPair(metadata, "Record operation", "Operation", "Details", (a,b) -> {
                        metadata.beginOperation(a,b);
                    });
                    else promptPair(metadata, "Finish operation", "Operation", "Result / details", (a,b) -> {
                        metadata.finishOperation(a,b);
                    });
                }).show();
    }

    private interface MetadataWriter { void write(String value) throws Exception; }
    private interface MetadataPairWriter { void write(String first, String second) throws Exception; }

    private void promptMetadata(ProjectWorkspaceMetadata metadata, String title, String hint, MetadataWriter writer) {
        EditText input = new EditText(this);
        input.setHint(hint);
        new AlertDialog.Builder(this).setTitle(title).setView(input)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("SAVE", (d,w) -> {
                    try { writer.write(input.getText().toString()); status.setText("Project metadata saved privately."); }
                    catch (Exception error) { status.setText("SAVE FAILED: " + error.getMessage()); }
                }).show();
    }

    private void promptPair(ProjectWorkspaceMetadata metadata, String title, String firstHint,
                            String secondHint, MetadataPairWriter writer) {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        EditText first = new EditText(this); first.setHint(firstHint);
        EditText second = new EditText(this); second.setHint(secondHint);
        fields.addView(first); fields.addView(second);
        new AlertDialog.Builder(this).setTitle(title).setView(fields)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("SAVE", (d,w) -> {
                    try { writer.write(first.getText().toString(), second.getText().toString()); status.setText("Project metadata saved privately."); }
                    catch (Exception error) { status.setText("SAVE FAILED: " + error.getMessage()); }
                }).show();
    }

    private void showOverview(ProjectWorkspaceMetadata metadata) {
        try {
            TextView content = new TextView(this);
            content.setText(metadata.readOverview());
            content.setTextIsSelectable(true);
            ScrollView scroll = new ScrollView(this);
            scroll.addView(content);
            new AlertDialog.Builder(this).setTitle("Project records and recovery")
                    .setView(scroll).setPositiveButton("CLOSE", null).show();
        } catch (Exception error) {
            status.setText("CANNOT READ PROJECT RECORDS: " + error.getMessage());
        }
    }

    private void open(ProjectEntry entry) {
        if ("APK".equals(entry.kind)) {
            Intent intent = new Intent(this, ApkProjectWorkspaceActivity.class);
            intent.putExtra("project_id", entry.id);
            startActivity(intent);
        } else if ("C# SOURCE".equals(entry.kind)) {
            openSource(entry.id);
        } else {
            openDependencies(entry.id);
        }
    }

    private void openSource(String id) {
        if (id == null) return;
        Intent intent = new Intent(this, CSharpSourceEditorActivity.class);
        intent.putExtra("project_id", id);
        startActivity(intent);
    }

    private void openDependencies(String id) {
        if (id == null) return;
        Intent intent = new Intent(this, DotNetDependencyActivity.class);
        intent.putExtra("project_id", id);
        startActivity(intent);
    }

    @Override protected void onDestroy() {
        destroyed = true;
        loadGeneration++;
        worker.shutdownNow();
        super.onDestroy();
    }
}
