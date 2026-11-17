package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Recent APK projects, notes, bookmarks, searches, exports, file tree and revision recovery. */
public final class ApkProjectWorkspaceActivity extends Activity {
    private static final int PICK_CREATE = 6701, PICK_IMPORT = 6702, PICK_EXPORT = 6703, PICK_REPORT_EXPORT = 6704;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout root;
    private ProgressBar progress;
    private TextView status;
    private ApkProjectWorkspace project;
    private File pendingReport;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String requestedProject = getIntent().getStringExtra("project_id");
        if (requestedProject == null) showHome();
        else {
            initializeRoot("APK Project Workspace");
            openProject(requestedProject);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (project != null && status != null) refreshProjectStatus();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void initializeRoot(String titleText) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(0xFF101318);
        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        root.addView(title);
        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));
        status = new TextView(this);
        status.setTextColor(0xFF80CBC4);
        status.setPadding(0, dp(6), 0, dp(6));
        root.addView(status);
        setContentView(root);
    }

    private Button button(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(v -> action.run());
        root.addView(button);
        return button;
    }

    private void showHome() {
        initializeRoot("APK Project Workspace");
        status.setText("Create or reopen a persistent APK project. Original inputs are preserved separately from the working APK.");
        button("CREATE NEW APK PROJECT", this::pickCreate);
        List<ApkProjectWorkspace.ProjectSummary> projects = ApkProjectWorkspace.listRecentProjects(this);
        TextView recent = new TextView(this);
        recent.setText("RECENT PROJECTS (" + projects.size() + ")");
        recent.setTextColor(0xFFFFFFFF);
        recent.setPadding(0, dp(12), 0, dp(6));
        root.addView(recent);
        ListView list = new ListView(this);
        ArrayAdapter<ApkProjectWorkspace.ProjectSummary> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, projects);
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        list.setOnItemClickListener((parent, view, position, id) -> openProject(projects.get(position).id));
        String last = ApkProjectWorkspace.getLastProjectId(this);
        if (last != null) button("OPEN LAST APK PROJECT", () -> openProject(last));
    }

    private void pickCreate() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.android.package-archive");
        startActivityForResult(intent, PICK_CREATE);
    }

    private void pickImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.android.package-archive");
        startActivityForResult(intent, PICK_IMPORT);
    }

    private void openProject(String id) {
        setBusy(true, "Opening saved APK project…");
        Context appContext = getApplicationContext();
        WeakReference<ApkProjectWorkspaceActivity> owner = new WeakReference<>(this);
        worker.execute(() -> {
            ApkProjectWorkspace opened = null;
            String error = null;
            try { opened = ApkProjectWorkspace.open(appContext, id); }
            catch (Exception e) { error = e.getClass().getSimpleName() + ": " + safe(e.getMessage()); }
            ApkProjectWorkspace ready = opened;
            String failure = error;
            postToActivity(owner, activity -> {
                if (failure != null) {
                    activity.setBusy(false, "Project could not be opened.");
                    activity.showError(failure);
                    return;
                }
                activity.project = ready;
                activity.showProject();
            });
        });
    }

    private void showProject() {
        initializeRoot("APK Project — " + project.getName());
        status.setText("Project ID: " + project.getId() + "\nRevision: " + project.getRevision()
                + "\nOriginal APK: " + project.getOriginalApk().length() + " bytes (immutable)"
                + "\nWorking APK: " + project.getWorkingApk().length() + " bytes");
        button("OPEN GUIDED EDIT → REBUILD → SIGN PIPELINE", this::openPipeline);
        button("IMPORT REBUILT APK AS NEW WORKING REVISION", this::pickImport);
        button("EXPORT CURRENT WORKING APK", () -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/vnd.android.package-archive");
            intent.putExtra(Intent.EXTRA_TITLE, "DroidInspect-" + project.getName() + "-working.apk");
            startActivityForResult(intent, PICK_EXPORT);
        });
        button("EDIT PROJECT NOTES", this::editNotes);
        button("ADD BOOKMARK", this::addBookmark);
        button("ADD SAVED SEARCH", this::addSavedSearch);
        button("VIEW BOOKMARKS / SAVED SEARCHES", this::showSavedItems);
        button("VIEW PROJECT FILE TREE", this::showTree);
        button("REVISION HISTORY / RECOVERY", this::showHistory);
        button("EXPORT PROJECT REPORT", this::showReports);
        button("BACK TO RECENT PROJECTS", this::showHome);
    }

    private void openPipeline() {
        Intent intent = new Intent(this, ApkBuildPipelineActivity.class);
        intent.putExtra("apk_file_path", project.getWorkingApk().getAbsolutePath());
        intent.putExtra("apk_project_id", project.getId());
        startActivity(intent);
    }

    private void editNotes() {
        EditText editor = new EditText(this);
        editor.setMinLines(8);
        editor.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        try { editor.setText(project.readNotes()); }
        catch (Exception error) { showError(safe(error.getMessage())); return; }
        new AlertDialog.Builder(this).setTitle("Project notes").setView(editor)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("SAVE", (dialog, which) -> {
                    String notes = editor.getText().toString();
                    runProjectAction("Saving notes…", active -> {
                        active.saveNotes(notes);
                        return "Project notes saved.";
                    });
                }).show();
    }

    private void addBookmark() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        EditText target = new EditText(this);
        target.setHint("Bookmark target (class, resource ID, component, method…)"); fields.addView(target);
        EditText note = new EditText(this);
        note.setHint("Optional note"); fields.addView(note);
        new AlertDialog.Builder(this).setTitle("Add project bookmark").setView(fields)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("SAVE", (dialog, which) -> {
                    String value = target.getText().toString().trim();
                    String noteText = note.getText().toString();
                    runProjectAction("Saving bookmark…", active -> {
                        if (value.isEmpty()) throw new IllegalArgumentException("Enter a bookmark target.");
                        active.addBookmark("General", value, noteText);
                        return "Bookmark saved.";
                    });
                }).show();
    }

    private void addSavedSearch() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        EditText scope = new EditText(this);
        scope.setHint("Search scope (manifest, DEX, resources…)"); fields.addView(scope);
        EditText query = new EditText(this);
        query.setHint("Search query"); fields.addView(query);
        new AlertDialog.Builder(this).setTitle("Save project search").setView(fields)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("SAVE", (dialog, which) -> {
                    String scopeText = scope.getText().toString();
                    String value = query.getText().toString().trim();
                    runProjectAction("Saving search…", active -> {
                        if (value.isEmpty()) throw new IllegalArgumentException("Enter a search query.");
                        active.addSavedSearch(scopeText, value);
                        return "Search saved.";
                    });
                }).show();
    }

    private void showSavedItems() {
        try {
            List<String> bookmarks = project.readBookmarks();
            List<String> searches = project.readSavedSearches();
            StringBuilder text = new StringBuilder("BOOKMARKS\n");
            for (String item : bookmarks) text.append("• ").append(item).append('\n');
            text.append("\nSAVED SEARCHES\n");
            for (String item : searches) text.append("• ").append(item).append('\n');
            new AlertDialog.Builder(this).setTitle("Project references").setMessage(text.toString())
                    .setPositiveButton("CLOSE", null).show();
        } catch (Exception error) { showError(safe(error.getMessage())); }
    }

    private void showTree() {
        try {
            List<String> files = project.listProjectTree();
            StringBuilder text = new StringBuilder();
            for (String file : files) text.append(file).append('\n');
            new AlertDialog.Builder(this).setTitle("Project file tree").setMessage(text.toString())
                    .setPositiveButton("CLOSE", null).show();
        } catch (Exception error) {
            showError("Cannot list project tree: " + safe(error.getMessage()));
        }
    }

    private void showHistory() {
        List<File> history;
        try { history = project.listHistory(); }
        catch (Exception error) {
            showError("Cannot list revision history: " + safe(error.getMessage()));
            return;
        }
        if (history.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Revision history")
                    .setMessage("No recovery snapshots yet. A snapshot is created before each working APK replacement.")
                    .setPositiveButton("CLOSE", null).show();
            return;
        }
        ArrayList<String> labels = new ArrayList<>();
        for (File file : history) labels.add(file.getName() + "  •  " + file.length() + " bytes");
        new AlertDialog.Builder(this).setTitle("Select recovery revision")
                .setItems(labels.toArray(new String[0]), (dialog, which) ->
                        new AlertDialog.Builder(this).setTitle("Restore revision?")
                                .setMessage("The current working APK will first be saved as another recovery snapshot.")
                                .setNegativeButton("CANCEL", null)
                                .setPositiveButton("RESTORE", (d, w) ->
                                        runProjectAction("Restoring revision…", active -> {
                                            active.restoreRevision(history.get(which));
                                            return "Revision restored. Current working APK was snapshotted first.";
                                        })).show())
                .setNegativeButton("CLOSE", null).show();
    }

    private void showReports() {
        List<File> reports;
        try { reports = project.listReports(); }
        catch (Exception error) {
            showError("Cannot list project reports: " + safe(error.getMessage()));
            return;
        }
        if (reports.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Project reports")
                    .setMessage("No reports have been saved to this project yet.")
                    .setPositiveButton("CLOSE", null).show();
            return;
        }
        ArrayList<String> labels = new ArrayList<>();
        for (File file : reports) labels.add(file.getName() + "  •  " + file.length() + " bytes");
        new AlertDialog.Builder(this).setTitle("Select report to export")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    pendingReport = reports.get(which);
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TITLE, pendingReport.getName());
                    startActivityForResult(intent, PICK_REPORT_EXPORT);
                }).setNegativeButton("CLOSE", null).show();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        } catch (SecurityException ignored) { }
        if (request == PICK_CREATE) {
            setBusy(true, "Creating persistent APK project…");
            Context appContext = getApplicationContext();
            WeakReference<ApkProjectWorkspaceActivity> owner = new WeakReference<>(this);
            worker.execute(() -> {
                ApkProjectWorkspace created = null;
                String error = null;
                try { created = ApkProjectWorkspace.create(appContext, uri); }
                catch (Exception e) { error = e.getClass().getSimpleName() + ": " + safe(e.getMessage()); }
                ApkProjectWorkspace ready = created;
                String failure = error;
                postToActivity(owner, activity -> {
                    if (failure != null) { activity.setBusy(false, "Project creation failed."); activity.showError(failure); return; }
                    activity.project = ready;
                    activity.showProject();
                });
            });
        } else if (request == PICK_IMPORT) {
            runProjectAction("Importing new working APK revision…", active -> {
                active.replaceWorkingFromUri(uri);
                return "New working APK imported. Previous working APK was saved to history.";
            });
        } else if (request == PICK_EXPORT) {
            runProjectAction("Exporting working APK…", active -> {
                active.exportWorking(uri);
                return "Working APK exported. Project copy remains unchanged.";
            });
        } else if (request == PICK_REPORT_EXPORT && pendingReport != null) {
            File reportFile = pendingReport;
            runProjectAction("Exporting project report…", active -> {
                active.exportFile(reportFile, uri);
                return "Project report exported.";
            });
        }
    }

    private interface ProjectTask { String run(ApkProjectWorkspace activeProject) throws Exception; }
    private interface ActivityCallback { void deliver(ApkProjectWorkspaceActivity activity); }

    private static void postToActivity(WeakReference<ApkProjectWorkspaceActivity> owner,
                                       ActivityCallback callback) {
        new Handler(Looper.getMainLooper()).post(() -> {
            ApkProjectWorkspaceActivity activity = owner.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
            callback.deliver(activity);
        });
    }

    private void runProjectAction(String busyMessage, ProjectTask task) {
        setBusy(true, busyMessage);
        ApkProjectWorkspace activeProject = project;
        WeakReference<ApkProjectWorkspaceActivity> owner = new WeakReference<>(this);
        worker.execute(() -> {
            String message = null, error = null;
            try { message = task.run(activeProject); }
            catch (Exception e) { error = e.getClass().getSimpleName() + ": " + safe(e.getMessage()); }
            String finalMessage = message, failure = error;
            postToActivity(owner, activity -> {
                activity.setBusy(false, failure == null ? finalMessage : "Project operation failed.");
                if (failure != null) activity.showError(failure);
                activity.refreshProjectStatus();
            });
        });
    }

    private void refreshProjectStatus() {
        if (project == null || status == null) return;
        status.setText("Project ID: " + project.getId() + "\nRevision: " + project.getRevision()
                + "\nOriginal APK: " + project.getOriginalApk().length() + " bytes (immutable)"
                + "\nWorking APK: " + project.getWorkingApk().length() + " bytes");
    }

    private void setBusy(boolean busy, String message) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        status.setText(message);
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof Button) child.setEnabled(!busy);
        }
    }

    private void showError(String message) {
        new AlertDialog.Builder(this).setTitle("APK Project Workspace")
                .setMessage(message).setPositiveButton("CLOSE", null).show();
    }

    private static String safe(String value) { return value == null ? "No details." : value; }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
