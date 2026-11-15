package com.tanvir.droidinspect;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Reconstructed C# viewer. The raw IL browser remains a separate feature. */
public class CSharpDecompileActivity extends Activity {
    private static final int PAGE_CHARS = 12000;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView title, status, pageInfo, source;
    private ProgressBar progress;
    private ScrollView sourceScroll;
    private EditText search;
    private Button previousPage, nextPage, findPrevious, findNext, copyPage, editSource;
    private String completeSource = "";
    private String lastQuery = "";
    private int currentPage, searchCursor, currentMatch = -1;
    private boolean loading = true;
    private Uri selectedAssemblyUri;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_csharp_decompile);
        title = findViewById(R.id.csharpTitle);
        status = findViewById(R.id.csharpStatus);
        pageInfo = findViewById(R.id.csharpPageInfo);
        source = findViewById(R.id.csharpSource);
        progress = findViewById(R.id.csharpProgress);
        sourceScroll = findViewById(R.id.csharpSourceScroll);
        search = findViewById(R.id.csharpSearch);
        previousPage = findViewById(R.id.csharpPreviousPage);
        nextPage = findViewById(R.id.csharpNextPage);
        findPrevious = findViewById(R.id.csharpFindPrevious);
        findNext = findViewById(R.id.csharpFindNext);
        copyPage = findViewById(R.id.csharpCopyPage);
        editSource = findViewById(R.id.csharpEditSource);

        previousPage.setOnClickListener(v -> showPage(currentPage - 1));
        nextPage.setOnClickListener(v -> showPage(currentPage + 1));
        findPrevious.setOnClickListener(v -> findMatch(false));
        findNext.setOnClickListener(v -> findMatch(true));
        copyPage.setOnClickListener(v -> copyCurrentPage());
        editSource.setOnClickListener(v -> openSourceEditor());

        Uri uri = getIntent().getData();
        selectedAssemblyUri = uri;
        String name = getIntent().getStringExtra("assembly_name");
        title.setText(name == null ? "C# source" : name);
        if (uri == null) {
            finishWithError("No selected assembly was supplied.");
            return;
        }
        if (!NativeBridge.isArm64Supported()) {
            finishWithError("C# decompilation requires ARM64 (arm64-v8a). Other DroidInspect features remain available.");
            return;
        }
        startDecompilation(uri);
    }

    private void startDecompilation(Uri uri) {
        setLoading(true, "Preparing private working copy and offline references…");
        Context app = getApplicationContext();
        WeakReference<CSharpDecompileActivity> reference = new WeakReference<>(this);
        Handler ui = handler;
        worker.execute(() -> {
            String result;
            try {
                if (!NativeBridge.ensureLoaded()) throw new IllegalStateException(NativeBridge.getLoadError());
                try (DecompilationWorkspace workspace = DecompilationWorkspace.prepare(app, uri)) {
                ui.post(() -> {
                    CSharpDecompileActivity activity = reference.get();
                    if (activity != null && !activity.isFinishing())
                        activity.status.setText("Running offline ILSpy decompilation…");
                });
                result = NativeBridge.decompileFile(
                        workspace.getAssembly().getAbsolutePath(),
                        workspace.getReferenceDirectory());
                    if (result == null) result = "M11 DECOMPILATION ERROR: Native engine returned no result.";
                }
            } catch (Throwable error) {
                result = "M11 DECOMPILATION ERROR: " + error.getClass().getSimpleName()
                        + ": " + (error.getMessage() == null ? "No additional details." : error.getMessage());
            }
            final String output = result;
            ui.post(() -> {
                CSharpDecompileActivity activity = reference.get();
                if (activity != null && !activity.isFinishing()) activity.showResult(output);
            });
        });
    }

    private void showResult(String result) {
        loading = false;
        completeSource = result;
        currentPage = 0;
        searchCursor = 0;
        currentMatch = -1;
        lastQuery = "";
        setLoading(false, "Decompilation finished.");
        renderPage();
        if (completeSource.startsWith("M11 DECOMPILATION ERROR:")) {
            status.setText("DECOMPILATION FAILED\n" + completeSource);
        } else if (completeSource.startsWith("// M11 DEPENDENCY DIAGNOSTICS:")) {
            status.setText("C# SOURCE RETURNED — DEPENDENCY WARNINGS ARE INCLUDED ABOVE THE SOURCE.");
        } else {
            status.setText("C# SOURCE RETURNED — OFFLINE DECOMPILATION.");
        }
    }

    private void finishWithError(String message) {
        loading = false;
        progress.setVisibility(View.GONE);
        status.setText(message);
        source.setText("Decompilation is unavailable.");
        pageInfo.setText("Source page 0 / 0");
        updateControls();
    }

    private void setLoading(boolean active, String message) {
        loading = active;
        progress.setVisibility(active ? View.VISIBLE : View.GONE);
        status.setText(message);
        updateControls();
    }

    private int pageCount() {
        return completeSource.isEmpty() ? 0 : (completeSource.length() + PAGE_CHARS - 1) / PAGE_CHARS;
    }

    private void showPage(int page) {
        if (loading || page < 0 || page >= pageCount()) return;
        currentPage = page;
        renderPage();
    }

    private void renderPage() {
        int start = currentPage * PAGE_CHARS;
        int end = Math.min(completeSource.length(), start + PAGE_CHARS);
        if (completeSource.isEmpty()) {
            source.setText("No C# source output is available.");
        } else {
            SpannableString visible = new SpannableString(completeSource.substring(start, end));
            String query = search.getText().toString();
            if (!query.isEmpty()) {
                int from = start;
                while (from < end) {
                    int found = indexOfIgnoreCase(completeSource, query, from, end);
                    if (found < 0) break;
                    int matchEnd = Math.min(end, found + query.length());
                    visible.setSpan(new BackgroundColorSpan(found == currentMatch
                                    ? Color.rgb(255, 152, 0) : Color.rgb(255, 235, 130)),
                            found - start, matchEnd - start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    visible.setSpan(new ForegroundColorSpan(Color.rgb(25, 25, 25)),
                            found - start, matchEnd - start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    from = found + Math.max(1, query.length());
                }
            }
            source.setText(visible);
        }
        pageInfo.setText("Source page " + (pageCount() == 0 ? 0 : currentPage + 1) + " / " + pageCount());
        updateControls();
        sourceScroll.post(() -> sourceScroll.scrollTo(0, 0));
    }

    private void findMatch(boolean forward) {
        if (loading || completeSource.isEmpty()) return;
        String query = search.getText().toString();
        if (query.trim().isEmpty()) {
            status.setText("Enter text to search in the reconstructed C# source.");
            return;
        }
        if (!query.equals(lastQuery)) {
            lastQuery = query;
            searchCursor = forward ? 0 : completeSource.length();
            currentMatch = -1;
        }
        int found = forward
                ? indexOfIgnoreCase(completeSource, query, Math.min(searchCursor, completeSource.length()), completeSource.length())
                : lastIndexOfIgnoreCase(completeSource, query,
                        Math.min(searchCursor - 1, completeSource.length() - query.length()));
        boolean wrapped = false;
        if (found < 0) {
            wrapped = true;
            found = forward
                    ? indexOfIgnoreCase(completeSource, query, 0, completeSource.length())
                    : lastIndexOfIgnoreCase(completeSource, query, completeSource.length() - query.length());
        }
        if (found < 0) {
            currentMatch = -1;
            status.setText("No matches found for: " + query);
            renderPage();
            return;
        }
        currentMatch = found;
        searchCursor = forward ? found + Math.max(1, query.length()) : found;
        currentPage = found / PAGE_CHARS;
        renderPage();
        status.setText("Found " + query + (wrapped ? " (wrapped)" : "")
                + " — character " + (found + 1) + ", page " + (currentPage + 1));
        int local = found - currentPage * PAGE_CHARS;
        sourceScroll.post(() -> {
            android.text.Layout layout = source.getLayout();
            if (layout == null || source.length() == 0) return;
            int safe = Math.max(0, Math.min(local, source.length() - 1));
            int line = layout.getLineForOffset(safe);
            sourceScroll.smoothScrollTo(0, Math.max(0, layout.getLineTop(line) - 12));
        });
    }

    private static int indexOfIgnoreCase(String text, String query, int from, int limit) {
        int last = Math.min(limit - query.length(), text.length() - query.length());
        for (int i = Math.max(0, from); i <= last; i++)
            if (text.regionMatches(true, i, query, 0, query.length())) return i;
        return -1;
    }

    private static int lastIndexOfIgnoreCase(String text, String query, int from) {
        for (int i = Math.min(from, text.length() - query.length()); i >= 0; i--)
            if (text.regionMatches(true, i, query, 0, query.length())) return i;
        return -1;
    }

    private void copyCurrentPage() {
        if (completeSource.isEmpty()) return;
        int start = currentPage * PAGE_CHARS;
        int end = Math.min(completeSource.length(), start + PAGE_CHARS);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("C# source page", completeSource.substring(start, end)));
            Toast.makeText(this, "Current source page copied.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateControls() {
        int count = pageCount();
        previousPage.setEnabled(!loading && currentPage > 0);
        nextPage.setEnabled(!loading && currentPage + 1 < count);
        findPrevious.setEnabled(!loading && count > 0);
        findNext.setEnabled(!loading && count > 0);
        copyPage.setEnabled(!loading && count > 0);
        editSource.setEnabled(!loading && count > 0 && !completeSource.startsWith("M11 DECOMPILATION ERROR:"));
    }


    private void openSourceEditor() {
        if (loading || completeSource.isEmpty() || completeSource.startsWith("M11 DECOMPILATION ERROR:")) return;
        try {
            CSharpSourceWorkspace project = CSharpSourceWorkspace.create(
                    this, title.getText().toString(), completeSource, selectedAssemblyUri);
            Intent intent = new Intent(this, CSharpSourceEditorActivity.class);
            intent.putExtra("project_id", project.getId());
            startActivity(intent);
        } catch (Exception error) {
            status.setText("Could not create editable source workspace: " + error.getMessage());
        }
    }

    @Override protected void onDestroy() {
        // Native decompilation is synchronous and cannot be safely interrupted.
        // The weak UI reference lets the worker finish and clean its workspace.
        worker.shutdown();
        super.onDestroy();
    }
}
