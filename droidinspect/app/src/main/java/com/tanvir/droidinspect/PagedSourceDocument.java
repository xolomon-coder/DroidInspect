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
import java.util.UUID;

/**
 * Paged source model split into bounded editable chunks.
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
                        // Keep a UTF-16 surrogate pair together even when the reader buffer ends here.
                        if (Character.isHighSurrogate(page.charAt(page.length() - 1))) {
                            if (offset < count && Character.isLowSurrogate(buffer[offset])) {
                                char pendingHigh = page.charAt(page.length() - 1);
                                page.setLength(page.length() - 1);
                                document.addPage(page.toString());
                                page.setLength(0);
                                page.append(pendingHigh).append(buffer[offset++]);
                                continue;
                            } else if (offset == count) {
                                char pendingHigh = page.charAt(page.length() - 1);
                                page.setLength(page.length() - 1);
                                document.addPage(page.toString());
                                page.setLength(0);
                                page.append(pendingHigh);
                                continue;
                            }
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

    void setAll(String text) {
        rebuild(text == null ? "" : text);
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
        if (query == null || query.isEmpty() || pages.isEmpty() || query.length() > totalLength) return -1;
        int start = Math.max(0, Math.min(fromOffset, totalLength));
        if (forward) {
            int found = findForward(query, start, ignoreCase);
            return found >= 0 ? found : (start == 0 ? -1 : findForward(query, 0, ignoreCase));
        }
        int found = findBackward(query, start, ignoreCase);
        return found >= 0 ? found : (start == totalLength ? -1 : findBackward(query, totalLength, ignoreCase));
    }

    /**
     * Searches from the end without joining every page into one document-sized String.
     * The carry contains at most query.length()-1 characters from the beginning of
     * the already-visited suffix so matches crossing a page boundary are retained.
     */
    private int findBackward(String query, int fromOffset, boolean ignoreCase) {
        String carry = "";
        int nextPageStart = totalLength;
        for (int i = pages.size() - 1; i >= 0; i--) {
            String page = pages.get(i);
            int pageStart = nextPageStart - page.length();
            String combined = page + carry;
            int allowedStart = Math.min(fromOffset - 1, pageStart + page.length() - 1);
            int localStart = allowedStart - pageStart;
            int found = localStart < 0 ? -1 : (ignoreCase
                    ? lastIndexOfIgnoreCase(combined, query, localStart)
                    : combined.lastIndexOf(query, localStart));
            if (found >= 0) return pageStart + found;
            int keep = Math.min(query.length() - 1, combined.length());
            carry = keep == 0 ? "" : combined.substring(0, keep);
            nextPageStart = pageStart;
        }
        return -1;
    }

    private int findForward(String query, int fromOffset, boolean ignoreCase) {
        String carry = "";
        int absolutePageStart = 0;
        for (String page : pages) {
            String combined = carry + page;
            int combinedStart = absolutePageStart - carry.length();
            int localStart = Math.max(0, fromOffset - combinedStart);
            int found = ignoreCase
                    ? indexOfIgnoreCase(combined, query, localStart)
                    : combined.indexOf(query, localStart);
            if (found >= 0) return combinedStart + found;
            absolutePageStart += page.length();
            int keep = Math.min(Math.max(0, query.length() - 1), combined.length());
            carry = keep == 0 ? "" : combined.substring(combined.length() - keep);
        }
        return -1;
    }

    int replaceAll(String query, String replacement, boolean ignoreCase) {
        if (query == null || query.isEmpty() || query.length() > totalLength) return 0;
        String value = replacement == null ? "" : replacement;
        StringBuilder pending = new StringBuilder();
        PageAccumulator output = new PageAccumulator();
        int count = 0;
        for (String page : pages) {
            pending.append(page);
            int safeEnd = Math.max(0, pending.length() - query.length() + 1);
            int cursor = 0;
            while (cursor < safeEnd) {
                int found = ignoreCase
                        ? indexOfIgnoreCase(pending, query, cursor)
                        : pending.indexOf(query, cursor);
                if (found < 0 || found >= safeEnd) break;
                output.append(pending, cursor, found);
                output.append(value, 0, value.length());
                cursor = found + query.length();
                count++;
            }
            int flushEnd = Math.max(cursor, safeEnd);
            output.append(pending, cursor, flushEnd);
            pending.delete(0, flushEnd);
        }
        int cursor = 0;
        while (cursor <= pending.length() - query.length()) {
            int found = ignoreCase
                    ? indexOfIgnoreCase(pending, query, cursor)
                    : pending.indexOf(query, cursor);
            if (found < 0) break;
            output.append(pending, cursor, found);
            output.append(value, 0, value.length());
            cursor = found + query.length();
            count++;
        }
        output.append(pending, cursor, pending.length());
        if (count > 0) replacePages(output.finish());
        return count;
    }

    void replaceRange(int start, int end, String replacement) {
        if (start < 0 || end <= start || end > totalLength)
            throw new IndexOutOfBoundsException("Invalid replacement range.");
        String value = replacement == null ? "" : replacement;
        PageAccumulator output = new PageAccumulator();
        int absolute = 0;
        boolean inserted = false;
        for (String page : pages) {
            int pageStart = absolute;
            int pageEnd = pageStart + page.length();
            if (pageEnd <= start) {
                output.append(page, 0, page.length());
            } else if (pageStart >= end) {
                if (!inserted) {
                    output.append(value, 0, value.length());
                    inserted = true;
                }
                output.append(page, 0, page.length());
            } else {
                int prefixLength = Math.max(0, Math.min(page.length(), start - pageStart));
                int suffixStart = Math.max(0, Math.min(page.length(), end - pageStart));
                output.append(page, 0, prefixLength);
                if (!inserted) {
                    output.append(value, 0, value.length());
                    inserted = true;
                }
                output.append(page, suffixStart, page.length());
            }
            absolute = pageEnd;
        }
        if (!inserted) output.append(value, 0, value.length());
        replacePages(output.finish());
    }

    long utf8Length() {
        long bytes = 0;
        for (String page : pages) bytes += page.getBytes(StandardCharsets.UTF_8).length;
        return bytes;
    }

    void writeTo(File target) throws IOException {
        writeTo(target, Long.MAX_VALUE);
    }

    void writeTo(File target, long maxBytes) throws IOException {
        if (target == null) throw new IOException("Save destination is unavailable.");
        if (utf8Length() > maxBytes) throw new IOException("C# source file exceeds the 32 MB safety limit.");
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Cannot create source directory.");
        File temporary = new File(parent, target.getName() + ".paged-tmp-" + UUID.randomUUID());
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

    private void replacePages(ArrayList<String> updated) {
        pages.clear();
        totalLength = 0;
        if (updated == null || updated.isEmpty()) {
            pages.add("");
            return;
        }
        for (String page : updated) addPage(page);
    }

    /**
     * Builds replacement pages incrementally. This avoids holding a second
     * full-document StringBuilder during Replace All or a cross-page Replace Next.
     */
    private final class PageAccumulator {
        private final ArrayList<String> result = new ArrayList<>();
        private final StringBuilder current = new StringBuilder(pageChars);

        void append(CharSequence text, int start, int end) {
            if (text == null || start >= end) return;
            int offset = start;
            while (offset < end) {
                if (current.length() >= pageChars) {
                    if (current.length() == pageChars
                            && Character.isHighSurrogate(current.charAt(current.length() - 1))
                            && Character.isLowSurrogate(text.charAt(offset))) {
                        char pendingHigh = current.charAt(current.length() - 1);
                        current.setLength(current.length() - 1);
                        flush();
                        current.append(pendingHigh);
                    } else {
                        flush();
                    }
                }
                int amount = Math.min(pageChars - current.length(), end - offset);
                if (amount > 0 && offset + amount < end
                        && Character.isHighSurrogate(text.charAt(offset + amount - 1))
                        && Character.isLowSurrogate(text.charAt(offset + amount))) {
                    // Do not let a two-char surrogate pair overrun the page limit.
                    // Flush this partial page and copy the pair together onto the next one.
                    if (amount > 1) amount--;
                    else {
                        flush();
                        continue;
                    }
                }
                if (amount <= 0) {
                    flush();
                    continue;
                }
                current.append(text, offset, offset + amount);
                offset += amount;
                if (current.length() >= pageChars
                        && !Character.isHighSurrogate(current.charAt(current.length() - 1))) {
                    flush();
                }
            }
        }

        ArrayList<String> finish() {
            flush();
            if (result.isEmpty()) result.add("");
            return result;
        }

        private void flush() {
            if (current.length() == 0) return;
            result.add(current.toString());
            current.setLength(0);
        }
    }

    private void addPage(String page) {
        pages.add(page);
        totalLength += page.length();
    }

    private void checkPage(int page) {
        if (page < 0 || page >= pages.size()) throw new IndexOutOfBoundsException("Invalid source page.");
    }

    private static int indexOfIgnoreCase(CharSequence text, String query, int from) {
        int last = text.length() - query.length();
        for (int i = Math.max(0, from); i <= last; i++) {
            boolean matches = true;
            for (int j = 0; j < query.length(); j++) {
                char left = text.charAt(i + j);
                char right = query.charAt(j);
                if (left == right) continue;
                char upperLeft = Character.toUpperCase(left);
                char upperRight = Character.toUpperCase(right);
                if (upperLeft != upperRight
                        && Character.toLowerCase(upperLeft) != Character.toLowerCase(upperRight)) {
                    matches = false;
                    break;
                }
            }
            if (matches) return i;
        }
        return -1;
    }

    private static int lastIndexOfIgnoreCase(String text, String query, int from) {
        for (int i = Math.min(from, text.length() - query.length()); i >= 0; i--)
            if (text.regionMatches(true, i, query, 0, query.length())) return i;
        return -1;
    }
}
