package com.tanvir.droidinspect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Metadata-token Assembly Explorer layered over the existing bounded PE and IL readers. */
public final class AssemblyExplorerActivity extends Activity {
    private static final long MAX_INPUT = 512L * 1024L * 1024L;
    private static final String[] CATEGORIES = {
            "Tree", "All", "Methods", "Fields", "Properties", "Events", "References", "Module references", "Standalone signatures", "Type references", "Member references", "Type specifications", "Method specifications", "Bookmarks"
    };
    private static final String[] SORTS = {"Name A-Z", "Metadata token", "Owner"};

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayList<Entry> allEntries = new ArrayList<>();
    private final ArrayList<Entry> shownEntries = new ArrayList<>();
    private final ArrayList<String> labels = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private EditText search;
    private Spinner category, sort;
    private ListView list;
    private TextView title, status;
    private Button backButton;
    private File sourceCopy;
    private String currentOwner;

    private static final class Entry {
        final String category, token, owner, label, detail;
        final DotNetIlReader.MethodInfo method;
        Entry(String category, String token, String owner, String label, String detail,
              DotNetIlReader.MethodInfo method) {
            this.category = category;
            this.token = token;
            this.owner = owner == null ? "" : owner;
            this.label = label == null ? "" : label;
            this.detail = detail == null ? "" : detail;
            this.method = method;
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_assembly_explorer);
        search = findViewById(R.id.assemblyExplorerSearch);
        category = findViewById(R.id.assemblyExplorerCategory);
        sort = findViewById(R.id.assemblyExplorerSort);
        list = findViewById(R.id.assemblyExplorerList);
        title = findViewById(R.id.assemblyExplorerTitle);
        status = findViewById(R.id.assemblyExplorerStatus);
        backButton = findViewById(R.id.assemblyExplorerBack);
        Button goToken = findViewById(R.id.assemblyExplorerGoToken);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> selectEntry(shownEntries.get(position)));

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CATEGORIES);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        category.setAdapter(categoryAdapter);
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SORTS);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sort.setAdapter(sortAdapter);

        category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentOwner = null;
                backButton.setEnabled(false);
                filter();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        sort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { filter(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
            @Override public void afterTextChanged(Editable e) { }
        });
        backButton.setOnClickListener(v -> returnToTree());
        goToken.setOnClickListener(v -> promptToken());

        String raw = getIntent().getStringExtra("assembly_uri");
        if (raw == null) {
            status.setText("No managed assembly document was supplied.");
            return;
        }
        load(Uri.parse(raw));
    }

    private void load(Uri uri) {
        setBusy(true, "Copying assembly and indexing metadata, members and IL…");
        worker.execute(() -> {
            String failure = null;
            String metadata = null;
            DotNetIlReader.Result il = null;
            try {
                sourceCopy = File.createTempFile("droidinspect-explorer-", ".bin", getCacheDir());
                copyUri(uri, sourceCopy);
                metadata = FileInspectionEngine.inspectPe(sourceCopy, sourceCopy.getName());
                il = DotNetIlReader.read(sourceCopy);
            } catch (Exception error) {
                failure = error.getClass().getSimpleName() + ": " + safe(error.getMessage());
            }
            final String error = failure;
            final String meta = metadata;
            final DotNetIlReader.Result methods = il;
            runOnUiThread(() -> {
                setBusy(false, error == null ? "Assembly indexing complete." : "Assembly indexing failed.");
                if (error != null) {
                    status.setText("ASSEMBLY EXPLORER ERROR: " + error);
                    return;
                }
                title.setText(methods.assembly + " — Assembly Explorer");
                buildEntries(meta, methods);
                filter();
            });
        });
    }

    private void buildEntries(String metadata, DotNetIlReader.Result il) {
        allEntries.clear();
        allEntries.add(new Entry("Assembly", "0x20000001", "", il.assembly,
                section(metadata, "ASSEMBLY IDENTITY", "ASSEMBLY REFERENCES"), null));
        allEntries.add(new Entry("Module", "0x00000001", "", "Module identity",
                section(metadata, "MODULE IDENTITY", "ASSEMBLY IDENTITY"), null));

        String section = "";
        String owner = "";
        String[] lines = metadata == null ? new String[0] : metadata.split("\n");
        Pattern tokenPattern = Pattern.compile("token 0x([0-9A-Fa-f]{8})");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.equals("ASSEMBLY REFERENCES (") || line.startsWith("ASSEMBLY REFERENCES (")) {
                section = "References";
                owner = "";
                continue;
            }
            if (line.startsWith("MODULE REFERENCES (")) { section = "Module references"; owner = ""; continue; }
            if (line.startsWith("STANDALONE SIGNATURES (")) { section = "Standalone signatures"; owner = ""; continue; }
            if (line.equals("TYPE REFERENCES (") || line.startsWith("TYPE REFERENCES (")) { section = "Type references"; owner = ""; continue; }
            if (line.startsWith("MEMBER REFERENCES (")) { section = "Member references"; owner = ""; continue; }
            if (line.startsWith("TYPE SPECIFICATIONS (")) { section = "Type specifications"; owner = ""; continue; }
            if (line.startsWith("METHOD SPECIFICATIONS (")) { section = "Method specifications"; owner = ""; continue; }
            if (line.equals("TYPES AND MEMBERS")) { section = "Types"; owner = ""; continue; }
            if (line.equals("PROPERTIES")) { section = "Properties"; owner = ""; continue; }
            if (line.equals("EVENTS")) { section = "Events"; owner = ""; continue; }
            if (line.startsWith("Decode status:") || line.startsWith("PHASE 1 SCOPE")) { section = ""; continue; }

            if ("Module references".equals(section) && line.matches("^\\d+\\..*ModuleRef token 0x[0-9A-Fa-f]{8}.*$")) {
                allEntries.add(new Entry("Module references", token(line, tokenPattern), "", line, line, null));
                continue;
            }
            if ("Standalone signatures".equals(section) && line.matches("^\\d+\\..*StandAloneSig token 0x[0-9A-Fa-f]{8}.*$")) {
                allEntries.add(new Entry("Standalone signatures", token(line, tokenPattern), "", line, line, null));
                continue;
            }
            if ("Type references".equals(section) && line.matches("^\\d+\\..*TypeRef token 0x[0-9A-Fa-f]{8}.*$")) {
                allEntries.add(new Entry("Type references", token(line, tokenPattern), "", line, line, null));
                continue;
            }
            if ("Member references".equals(section) && line.matches("^\\d+\\..*MemberRef token 0x[0-9A-Fa-f]{8}.*$")) {
                allEntries.add(new Entry("Member references", token(line, tokenPattern), "", line, line, null));
                continue;
            }
            if ("Type specifications".equals(section) && line.matches("^\\d+\\..*TypeSpec token 0x[0-9A-Fa-f]{8}.*$")) {
                allEntries.add(new Entry("Type specifications", token(line, tokenPattern), "", line, line, null));
                continue;
            }
            if ("Method specifications".equals(section) && line.matches("^\\d+\\..*MethodSpec token 0x[0-9A-Fa-f]{8}.*$")) {
                allEntries.add(new Entry("Method specifications", token(line, tokenPattern), "", line, line, null));
                continue;
            }
            if ("References".equals(section)) {
                if (!line.matches("^\\d+\\..*AssemblyRef token 0x[0-9A-Fa-f]{8}.*$")) continue;
                String token = token(line, tokenPattern);
                allEntries.add(new Entry("References", token, "", line, line, null));
                continue;
            }

            if ("Types".equals(section)) {
                if (line.contains("(TypeDef token 0x")) {
                    String token = token(line, tokenPattern);
                    int dot = line.indexOf(". ");
                    int marker = line.indexOf(" (TypeDef token");
                    owner = dot >= 0 && marker > dot ? line.substring(dot + 2, marker) : line;
                    if (owner.endsWith(" [interface]")) owner = owner.substring(0, owner.length() - 12);
                    allEntries.add(new Entry("Types", token, owner, owner, line, null));
                    continue;
                }
                if (line.startsWith("Field:")) {
                    String token = token(line, tokenPattern);
                    allEntries.add(new Entry("Fields", token, owner, owner + " :: " + line, line, null));
                    continue;
                }
            }

            if ("Properties".equals(section)) {
                if (line.startsWith("Property:")) {
                    String token = token(line, tokenPattern);
                    allEntries.add(new Entry("Properties", token, owner, owner + " :: " + line, line, null));
                } else if (!line.isEmpty() && !line.startsWith("PropertyMap") && !line.startsWith("No property")) {
                    owner = line;
                }
                continue;
            }

            if ("Events".equals(section)) {
                if (line.startsWith("Event:")) {
                    String token = token(line, tokenPattern);
                    allEntries.add(new Entry("Events", token, owner, owner + " :: " + line, line, null));
                } else if (!line.isEmpty() && !line.startsWith("EventMap") && !line.startsWith("No event")) {
                    owner = line;
                }
            }
        }

        for (DotNetIlReader.MethodInfo method : il.methods) {
            String token = String.format(Locale.ROOT, "0x%08X", 0x06000000L | method.row);
            allEntries.add(new Entry("Methods", token, method.owner, method.label(), method.body, method));
        }
        status.setText("Indexed " + allEntries.size() + " explorer entries; " + il.methods.size()
                + " methods with IL bodies. Metadata display caps remain documented in the inspection report.");
    }

    private static String token(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? "0x" + matcher.group(1).toUpperCase(Locale.ROOT) : "";
    }

    private static String section(String source, String start, String end) {
        if (source == null) return "";
        int a = source.indexOf(start);
        if (a < 0) return "";
        int b = source.indexOf(end, a + start.length());
        return source.substring(a, b < 0 ? source.length() : b).trim();
    }

    private void filter() {
        if (adapter == null) return;
        String query = search.getText() == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
        String selected = category.getSelectedItem() == null ? "Tree" : category.getSelectedItem().toString();
        String sortMode = sort.getSelectedItem() == null ? SORTS[0] : sort.getSelectedItem().toString();
        Set<String> bookmarks = getBookmarks();
        shownEntries.clear();
        labels.clear();

        for (Entry entry : allEntries) {
            boolean include;
            if (currentOwner != null) {
                include = entry.owner.equals(currentOwner)
                        && ("Methods".equals(entry.category) || "Fields".equals(entry.category)
                        || "Properties".equals(entry.category) || "Events".equals(entry.category));
            } else if ("Tree".equals(selected)) {
                include = "Types".equals(entry.category) || "Assembly".equals(entry.category)
                        || "Module".equals(entry.category) || "References".equals(entry.category)
                        || "Module references".equals(entry.category) || "Standalone signatures".equals(entry.category)
                        || "Type references".equals(entry.category) || "Member references".equals(entry.category)
                        || "Type specifications".equals(entry.category) || "Method specifications".equals(entry.category);
            } else if ("All".equals(selected)) {
                include = true;
            } else if ("Bookmarks".equals(selected)) {
                include = bookmarks.contains(entry.token);
            } else {
                include = selected.equals(entry.category);
            }

            if (!include) continue;
            if (!query.isEmpty()) {
                String haystack = (entry.label + " " + entry.owner + " " + entry.token + " " + entry.detail)
                        .toLowerCase(Locale.ROOT);
                if (!haystack.contains(query)) continue;
            }
            shownEntries.add(entry);
        }

        Comparator<Entry> comparator;
        if ("Metadata token".equals(sortMode)) {
            comparator = Comparator.comparingLong(e -> parseToken(e.token));
        } else if ("Owner".equals(sortMode)) {
            comparator = Comparator.comparing((Entry e) -> e.owner.toLowerCase(Locale.ROOT))
                    .thenComparing(e -> e.label.toLowerCase(Locale.ROOT));
        } else {
            comparator = Comparator.comparing(e -> e.label.toLowerCase(Locale.ROOT));
        }
        Collections.sort(shownEntries, comparator);
        for (Entry entry : shownEntries) labels.add(entry.label);
        adapter.notifyDataSetChanged();
        backButton.setEnabled(currentOwner != null);
        status.setText((currentOwner == null ? selected : currentOwner)
                + " — showing " + shownEntries.size() + " of " + allEntries.size());
    }

    private void selectEntry(Entry entry) {
        if ("Types".equals(entry.category) && "Tree".equals(category.getSelectedItem())) {
            currentOwner = entry.owner;
            filter();
            return;
        }
        if (entry.method != null) {
            showMethodEntry(entry);
            return;
        }
        showEntry(entry);
    }

    private void showMethodEntry(Entry entry) {
        Set<String> referenced = referencedTokens(entry.method.body);
        String message = "Category: Methods\\nMetadata token: " + entry.token + "\\nOwner: " + entry.owner
                + "\\nRVA: 0x" + String.format(Locale.ROOT, "%08X", entry.method.rva)
                + "\\nReferenced metadata tokens: " + referenced.size() + "\\n\\n" + entry.detail;
        boolean bookmarked = getBookmarks().contains(entry.token);
        new AlertDialog.Builder(this)
                .setTitle(entry.label)
                .setMessage(message)
                .setNegativeButton("CLOSE", null)
                .setNeutralButton("REFERENCES", (dialog, which) -> showReferencedTokens(entry.method))
                .setPositiveButton(bookmarked ? "REMOVE BOOKMARK" : "BOOKMARK",
                        (dialog, which) -> toggleBookmark(entry.token))
                .show();
    }

    private Set<String> referencedTokens(String body) {
        Set<String> values = new java.util.LinkedHashSet<>();
        Matcher matcher = Pattern.compile("token 0x([0-9A-Fa-f]{8})").matcher(body == null ? "" : body);
        while (matcher.find()) values.add("0x" + matcher.group(1).toUpperCase(Locale.ROOT));
        return values;
    }

    private void showReferencedTokens(DotNetIlReader.MethodInfo method) {
        ArrayList<String> tokens = new ArrayList<>(referencedTokens(method.body));
        if (tokens.isEmpty()) {
            Toast.makeText(this, "This method has no decoded metadata-token operands.", Toast.LENGTH_LONG).show();
            return;
        }
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Entry> targets = new ArrayList<>();
        for (String token : tokens) {
            Entry found = null;
            for (Entry candidate : allEntries) if (token.equals(candidate.token)) { found = candidate; break; }
            targets.add(found);
            labels.add(found == null ? token + " — not present in the bounded index"
                    : token + " — " + found.label);
        }
        new AlertDialog.Builder(this)
                .setTitle("Metadata references — " + method.name)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    Entry target = targets.get(which);
                    if (target != null) showEntry(target);
                    else Toast.makeText(this, "Referenced token is outside the current index or display cap.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void showEntry(Entry entry) {
        String message = "Category: " + entry.category + "\nMetadata token: " + entry.token
                + (entry.owner.isEmpty() ? "" : "\nOwner: " + entry.owner)
                + "\n\n" + entry.detail;
        boolean bookmarked = getBookmarks().contains(entry.token);
        new AlertDialog.Builder(this)
                .setTitle(entry.label)
                .setMessage(message)
                .setNegativeButton("CLOSE", null)
                .setNeutralButton(bookmarked ? "REMOVE BOOKMARK" : "BOOKMARK",
                        (dialog, which) -> toggleBookmark(entry.token))
                .setPositiveButton("COPY TOKEN", (dialog, which) -> copyToken(entry.token))
                .show();
    }

    private void returnToTree() {
        currentOwner = null;
        category.setSelection(0);
        filter();
    }

    private void promptToken() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Example: 0x06000001");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
                .setTitle("Go to metadata token")
                .setView(input)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("GO", (dialog, which) -> {
                    long wanted = parseToken(input.getText().toString());
                    for (Entry entry : allEntries) {
                        if (parseToken(entry.token) == wanted && wanted != 0) {
                            currentOwner = null;
                            category.setSelection(1);
                            filter();
                            showEntry(entry);
                            return;
                        }
                    }
                    Toast.makeText(this, "Token is not in the current bounded index.", Toast.LENGTH_LONG).show();
                }).show();
    }

    private long parseToken(String token) {
        if (token == null) return 0;
        try {
            String value = token.trim().toLowerCase(Locale.ROOT);
            if (value.startsWith("0x")) value = value.substring(2);
            return Long.parseLong(value, 16);
        } catch (Exception ignored) { return 0; }
    }

    private Set<String> getBookmarks() {
        return new HashSet<>(getSharedPreferences("assembly_explorer_bookmarks", MODE_PRIVATE)
                .getStringSet("tokens", new HashSet<>()));
    }

    private void toggleBookmark(String token) {
        Set<String> bookmarks = getBookmarks();
        if (bookmarks.contains(token)) bookmarks.remove(token);
        else bookmarks.add(token);
        getSharedPreferences("assembly_explorer_bookmarks", MODE_PRIVATE)
                .edit().putStringSet("tokens", bookmarks).apply();
        filter();
    }

    private void copyToken(String token) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Metadata token", token));
        Toast.makeText(this, "Metadata token copied.", Toast.LENGTH_SHORT).show();
    }

    private void setBusy(boolean busy, String message) {
        status.setText(message);
        search.setEnabled(!busy);
        category.setEnabled(!busy);
        sort.setEnabled(!busy);
        list.setEnabled(!busy);
        backButton.setEnabled(!busy && currentOwner != null);
    }

    private void copyUri(Uri uri, File output) throws Exception {
        long total = 0;
        try (InputStream input = getContentResolver().openInputStream(uri);
             FileOutputStream stream = new FileOutputStream(output)) {
            if (input == null) throw new IllegalStateException("Android could not open the selected document.");
            byte[] buffer = new byte[32768];
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > MAX_INPUT) throw new IllegalArgumentException("Assembly exceeds the 512 MB safety limit.");
                stream.write(buffer, 0, count);
            }
            stream.flush();
        }
    }

    private static String safe(String value) { return value == null ? "unknown" : value; }

    @Override public void onBackPressed() {
        if (currentOwner != null) {
            returnToTree();
            return;
        }
        super.onBackPressed();
    }

    @Override protected void onDestroy() {
        worker.shutdown();
        if (sourceCopy != null) sourceCopy.delete();
        super.onDestroy();
    }
}
