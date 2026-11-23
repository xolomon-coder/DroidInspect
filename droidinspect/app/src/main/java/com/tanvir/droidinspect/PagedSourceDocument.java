package com.tanvir.droidinspect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * File-backed editor model split into bounded editable pages.
 * Only the active page needs to be attached to an Android text widget.
 */
final class PagedSourceDocument {
    static final int DEFAULT_PAGE_CHARS = 16000;

    private final int pageChars;
    private final ArrayList<String> pages = new ArrayList<>();
    private int totalLength;

    private PagedSourceDocument(int pageChars) {
        if (pageChars < 1024) throw new IllegalArgumentException("Page size is too small.");
        this.pageChars = pageChars;
    }

    static PagedSourceDocument load(File file) throws IOException {
        return load(file, DEFAULT_PAGE_CHARS);
    }

    static PagedSourceDocument load(File file, int pageChars) throws IOException {
        if (file == null || !file.isFile()) throw new IOException("C# source file is unavailable.");
        PagedSourceDocument document = new PagedSourceDocument(pageChars);
        StringBuilder page = new StringBuilder(pageChars);
        char[] buffer = new char[8192];
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            int count;
            while ((count = reader.read(buffer)) != -1) {
                int offset = 0;
                while (offset < count) {
                    int amount = Math.min(pageChars - page.length(), count - offset);
                    page.append(buffer, offset, amount);
                    offset += amount;
                    if (page.length() >= pageChars) {
                        // Keep a UTF-16 surrogate pair together when a chunk boundary is reached.
                        if (Character.isHighSurrogate(page.charAt(page.length() - 1))
                                && offset < count && Character.isLowSurrogate(buffer[offset])) {
                            page.append(buffer[offset++]);
                        }
                        document.addPage(page.toString());
                        page.setLength(0);
                    }
                }
            }
        }
        if (page.length() > 0 || document.pages.isEmpty()) document.addPage(page.toString());
        return document;
    }

    int pageCount() { return pages.size(); }
    int length() { return totalLength; }
    int pageLength(int page) { return getPage(page).length(); }

    String getPage(int page) {
        checkPage(page);
        return pages.get(page);
    }

    int getPageStart(int page) {
        if (page < 0 || page >= pages.size()) throw new IndexOutOfBoundsException("Invalid page.");
        int start = 0;
        for (int i = 0; i < page; i++) start += pages.get(i).length();
        return start;
    }

    int pageForOffset(int offset) {
        if (offset < 0 || offset > totalLength) throw new IndexOutOfBoundsException("Invalid source offset.");
        int start = 0;
        for (int i = 0; i < pages.size(); i++) {
            int end = start + pages.get(i).length();
            if (offset < end || (offset == totalLength && i == pages.size() - 1)) return i;
            start = end;
        }
        return Math.max(0, pages.size() - 1);
    }

    void setPage(int page, String text) {
        checkPage(page);
        String value = text == null ? "" : text;
        int oldLength = pages.get(page).length();
        ArrayList<String> replacement = split(value);
        pages.remove(page);
        pages.addAll(page, replacement);
        totalLength += value.length() - oldLength;
        if (pages.isEmpty()) pages.add("");
    }

    int find(String query, int fromOffset, boolean forward, boolean ignoreCase) {
        if (query == null || query.isEmpty() || pages.isEmpty()) return -1;
        String all = toString();
        int start = Math.max(0, Math.min(fromOffset, all.length()));
        if (ignoreCase) {
            if (forward) {
                int found = indexOfIgnoreCase(all, query, start);
                return found >= 0 ? found : indexOfIgnoreCase(all, query, 0);
            }
            int found = lastIndexOfIgnoreCase(all, query, Math.min(start - 1, all.length() - query.length()));
            return found >= 0 ? found : lastIndexOfIgnoreCase(all, query, all.length() - query.length());
        }
        int found = forward ? all.indexOf(query, start) : all.lastIndexOf(query, Math.min(start - 1, all.length()));
        if (found >= 0) return found;
        return forward ? all.indexOf(query) : all.lastIndexOf(query);
    }

    int replaceAll(String query, String replacement, boolean ignoreCase) {
        if (query == null || query.isEmpty()) return 0;
        String source = toString();
        String value = replacement == null ? "" : replacement;
        StringBuilder output = new StringBuilder(source.length());
        int count = 0;
        int cursor = 0;
        while (cursor <= source.length() - query.length()) {
            int found = ignoreCase ? indexOfIgnoreCase(source, query, cursor) : source.indexOf(query, cursor);
            if (found < 0) break;
            output.append(source, cursor, found).append(value);
            cursor = found + query.length();
            count++;
        }
        if (count > 0) {
            output.append(source, cursor, source.length());
            rebuild(output);
        }
        return count;
    }

    void replaceRange(int start, int end, String replacement) {
        if (start < 0 || end < start || end > totalLength) throw new IndexOutOfBoundsException("Invalid replacement range.");
        String source = toString();
        String value = replacement == null ? "" : replacement;
        StringBuilder output = new StringBuilder(source.length() - (end - start) + value.length());
        output.append(source, 0, start).append(value).append(source, end, source.length());
        rebuild(output);
    }

    void writeTo(File target) throws IOException {
        if (target == null) throw new IOException("Save destination is unavailable.");
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Cannot create source directory.");
        File temporary = new File(parent, target.getName() + ".paged-tmp");
        Throwable failure = null;
        try {
            try (FileOutputStream stream = new FileOutputStream(temporary);
                 Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
                for (String page : pages) writer.write(page);
                writer.flush();
                stream.getFD().sync();
            }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException | Error error) {
            failure = error;
            throw error;
        } finally {
            try { Files.deleteIfExists(temporary.toPath()); }
            catch (IOException cleanup) {
                if (failure != null) failure.addSuppressed(cleanup);
                else throw cleanup;
            }
        }
    }

    @Override public String toString() {
        StringBuilder result = new StringBuilder(totalLength);
        for (String page : pages) result.append(page);
        return result.toString();
    }

    private void rebuild(CharSequence source) {
        pages.clear();
        totalLength = 0;
        int offset = 0;
        while (offset < source.length()) {
            int end = Math.min(source.length(), offset + pageChars);
            if (end < source.length() && end > offset
                    && Character.isHighSurrogate(source.charAt(end - 1))
                    && Character.isLowSurrogate(source.charAt(end))) end--;
            addPage(source.subSequence(offset, end).toString());
            offset = end;
        }
        if (pages.isEmpty()) pages.add("");
    }

    private ArrayList<String> split(String source) {
        ArrayList<String> result = new ArrayList<>();
        int offset = 0;
        while (offset < source.length()) {
            int end = Math.min(source.length(), offset + pageChars);
            if (end < source.length() && Character.isHighSurrogate(source.charAt(end - 1))
                    && Character.isLowSurrogate(source.charAt(end))) end--;
            result.add(source.substring(offset, end));
            offset = end;
        }
        if (result.isEmpty()) result.add("");
        return result;
    }

    private void addPage(String page) {
        pages.add(page);
        totalLength += page.length();
    }

    private void checkPage(int page) {
        if (page < 0 || page >= pages.size()) throw new IndexOutOfBoundsException("Invalid source page.");
    }

    private static int indexOfIgnoreCase(String text, String query, int from) {
        int last = text.length() - query.length();
        for (int i = Math.max(0, from); i <= last; i++)
            if (text.regionMatches(true, i, query, 0, query.length())) return i;
        return -1;
    }

    private static int lastIndexOfIgnoreCase(String text, String query, int from) {
        for (int i = Math.min(from, text.length() - query.length()); i >= 0; i--)
            if (text.regionMatches(true, i, query, 0, query.length())) return i;
        return -1;
    }
}
