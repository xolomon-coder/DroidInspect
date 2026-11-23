package com.tanvir.droidinspect;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class PagedSourceDocumentSelfTest {
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("droidinspect-paged-source").toFile();
        File source = new File(root, "Large.cs");
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            input.append("namespace Test").append(i).append(" {\n");
            for (int j = 0; j < 190; j++) input.append("    public int Value").append(j).append(" = ").append(i + j).append(";\n");
            input.append("}\n");
        }
        while (input.length() % 1024 != 1011) input.append(" ");
        input.append("BOUNDARY_LEFT").append("BOUNDARY_RIGHT");
        input.append(" emoji=\uD83D\uDE80 end");
        try (FileOutputStream out = new FileOutputStream(source)) {
            out.write(input.toString().getBytes(StandardCharsets.UTF_8));
        }

        PagedSourceDocument document = PagedSourceDocument.load(source, 1024);
        require(document.pageCount() > 5, "large source should be split into multiple bounded pages");
        require(document.toString().equals(input.toString()), "load must preserve every character and line ending");

        String boundary = "BOUNDARY_LEFTBOUNDARY_RIGHT";
        int leftBoundary = input.indexOf("BOUNDARY_LEFT");
        require(leftBoundary % 1024 == 1011, "fixture should position the token at a page boundary");
        int boundaryAt = document.find(boundary, 0, true, false);
        require(boundaryAt == leftBoundary, "whole-document search must find a token spanning page boundaries");
        require(document.find(boundary, leftBoundary + boundary.length(), false, false) == leftBoundary,
                "backward search must find a token spanning page boundaries without flattening the document");
        require(document.find("namespace Test0", 0, false, false) == input.indexOf("namespace Test0"),
                "backward search must wrap from the beginning to the final matching occurrence");
        String repeated = "public int Value0 =";
        require(document.find(repeated, 0, false, false) == input.lastIndexOf(repeated),
                "backward search wraparound must return the last complete-document match");
        require(document.replaceAll(boundary, "BOUNDARY_FIXED", false) == 1,
                "replace-all must also match text spanning page boundaries");

        String first = document.getPage(0);
        document.setPage(0, first + "// first page edit\n");
        int inserted = document.toString().indexOf("// first page edit");
        require(inserted >= 0, "page edit must persist in the document model");
        for (int i = 0; i < document.pageCount(); i++)
            require(document.pageLength(i) <= 1024, "edited pages must remain bounded");

        int count = document.replaceAll("public int Value0 =", "public int EditedValue0 =", false);
        require(count == 9, "replace-all must operate across all pages; expected 9, got " + count);
        require(document.toString().contains("public int EditedValue0 ="), "replacement text must be present");
        for (int i = 0; i < document.pageCount(); i++) {
            require(document.pageLength(i) <= 1024, "replace-all output pages must remain bounded");
            if (i + 1 < document.pageCount()) {
                String left = document.getPage(i);
                String right = document.getPage(i + 1);
                require(!(Character.isHighSurrogate(left.charAt(left.length() - 1))
                                && Character.isLowSurrogate(right.charAt(0))),
                        "replacement page boundaries must not split surrogate pairs");
            }
        }

        File replacementUnicodeBoundary = new File(root, "ReplacementUnicodeBoundary.cs");
        String replacementUnicodeInput = "a".repeat(1023) + "X";
        Files.write(replacementUnicodeBoundary.toPath(),
                replacementUnicodeInput.getBytes(StandardCharsets.UTF_8));
        PagedSourceDocument replacementUnicodeDocument =
                PagedSourceDocument.load(replacementUnicodeBoundary, 1024);
        require(replacementUnicodeDocument.replaceAll("X", "🚀", false) == 1,
                "replace-all must find the match immediately before a page boundary");
        require(replacementUnicodeDocument.toString().equals("a".repeat(1023) + "🚀"),
                "replacement must preserve a supplementary Unicode pair at the page boundary");
        for (int i = 0; i < replacementUnicodeDocument.pageCount(); i++) {
            require(replacementUnicodeDocument.pageLength(i) <= 1024,
                    "supplementary replacement must not exceed the configured page size");
            if (i + 1 < replacementUnicodeDocument.pageCount()) {
                String left = replacementUnicodeDocument.getPage(i);
                String right = replacementUnicodeDocument.getPage(i + 1);
                require(!(Character.isHighSurrogate(left.charAt(left.length() - 1))
                                && Character.isLowSurrogate(right.charAt(0))),
                        "supplementary replacement must not split its surrogate pair across pages");
            }
        }

        File deleteFixture = new File(root, "DeleteAll.cs");
        Files.write(deleteFixture.toPath(), "REMOVE_ME".getBytes(StandardCharsets.UTF_8));
        PagedSourceDocument deleteDocument = PagedSourceDocument.load(deleteFixture, 1024);
        require(deleteDocument.replaceAll("REMOVE_ME", "", false) == 1,
                "replace-all must support deletion to an empty document");
        require(deleteDocument.length() == 0 && deleteDocument.pageCount() == 1
                        && deleteDocument.getPage(0).isEmpty(),
                "deleting all source text must leave one valid empty page");

        require(document.find("namespace Test0", document.length(), true, false) == input.indexOf("namespace Test0"),
                "forward search must wrap from the end of the document to the first match");
        require(document.find("NAMESPACE TEST1", 0, true, true) == input.indexOf("namespace Test1"),
                "ignore-case search must match across the complete document");
        require(document.find("NAMESPACE TEST1", 0, true, false) == -1,
                "case-sensitive search must not match different letter casing");

        File pageEditFixture = new File(root, "PageEdit.cs");
        String pageEditOriginal = "A".repeat(1024) + "B".repeat(1024) + "C".repeat(1024);
        Files.write(pageEditFixture.toPath(), pageEditOriginal.getBytes(StandardCharsets.UTF_8));
        PagedSourceDocument pageEdit = PagedSourceDocument.load(pageEditFixture, 1024);
        String oversizedPage = "X".repeat(1023) + "🚀" + "Y".repeat(601);
        pageEdit.setPage(1, oversizedPage);
        String expandedExpected = "A".repeat(1024) + oversizedPage + "C".repeat(1024);
        require(pageEdit.toString().equals(expandedExpected),
                "oversized page edits must split while preserving following page content");
        require(pageEdit.pageCount() == 4,
                "oversized page edit must create the required additional bounded page");
        for (int i = 0; i < pageEdit.pageCount(); i++) {
            require(pageEdit.pageLength(i) <= 1024, "oversized page edit must keep every page bounded");
            String current = pageEdit.getPage(i);
            if (i + 1 < pageEdit.pageCount() && current.length() > 0) {
                String following = pageEdit.getPage(i + 1);
                require(following.length() == 0
                                || !(Character.isHighSurrogate(current.charAt(current.length() - 1))
                                && Character.isLowSurrogate(following.charAt(0))),
                        "oversized page edit must not split a surrogate pair");
            }
        }
        pageEdit.setPage(1, "tiny\r\n");
        String shrunkExpected = "A".repeat(1024) + "tiny\r\n"
                + oversizedPage.substring(1023) + "C".repeat(1024);
        require(pageEdit.toString().equals(shrunkExpected),
                "shrinking an edited page must preserve its overflow page and CRLF line endings");
        pageEdit.setPage(2, "");
        pageEdit.setPage(1, "");
        String deletedExpected = "A".repeat(1024) + "C".repeat(1024);
        require(pageEdit.toString().equals(deletedExpected),
                "deleting edited page content must preserve text on both sides");
        pageEdit.writeTo(pageEditFixture);
        require(PagedSourceDocument.load(pageEditFixture, 1024).toString().equals(deletedExpected),
                "page insertion, shrink and deletion must survive save/reopen");

        File protectedSave = new File(root, "Protected.cs");
        Files.write(protectedSave.toPath(), "KEEP ORIGINAL".getBytes(StandardCharsets.UTF_8));
        boolean sizeRejected = false;
        try {
            pageEdit.writeTo(protectedSave, 1);
        } catch (java.io.IOException expected) {
            sizeRejected = true;
        }
        require(sizeRejected, "save must reject a document above the caller's byte limit");
        require(new String(Files.readAllBytes(protectedSave.toPath()), StandardCharsets.UTF_8)
                        .equals("KEEP ORIGINAL"),
                "a rejected save must leave the existing destination untouched");

        boolean cancelledLoad = false;
        Thread.currentThread().interrupt();
        try {
            PagedSourceDocument.load(source, 1024);
        } catch (java.io.InterruptedIOException expected) {
            cancelledLoad = true;
        } finally {
            Thread.interrupted();
        }
        require(cancelledLoad, "paged source loading must stop when its background worker is interrupted");

        int oldLength = document.length();
        int rocket = document.toString().indexOf("\uD83D\uDE80");
        require(rocket >= 0, "supplementary Unicode character must survive page splitting");
        document.replaceRange(rocket, rocket + 2, "🚀");
        require(document.length() == oldLength, "same-width Unicode replacement must preserve length");

        int page = document.pageForOffset(boundaryAt);
        require(page >= 0 && page < document.pageCount(), "page lookup must resolve absolute offsets");
        document.writeTo(source);
        PagedSourceDocument reopened = PagedSourceDocument.load(source, 1024);
        require(reopened.toString().equals(document.toString()), "save/reopen must preserve the complete edited source");

        File rangeBoundary = new File(root, "RangeBoundary.cs");
        String rangeFixture = "x".repeat(1014) + "RANGE_LEFT" + "RANGE_RIGHT" + "tail";
        Files.write(rangeBoundary.toPath(), rangeFixture.getBytes(StandardCharsets.UTF_8));
        PagedSourceDocument rangeDocument = PagedSourceDocument.load(rangeBoundary, 1024);
        int rangeStart = rangeDocument.find("RANGE_LEFTRANGE_RIGHT", 0, true, false);
        require(rangeStart == 1014, "range replacement fixture must cross a page boundary");
        rangeDocument.replaceRange(rangeStart, rangeStart + "RANGE_LEFTRANGE_RIGHT".length(), "RANGE_REPLACED");
        require(rangeDocument.toString().equals("x".repeat(1014) + "RANGE_REPLACED" + "tail"),
                "range replacement must preserve text on both sides of a page boundary");

        File small = new File(root, "Small.cs");
        Files.write(small.toPath(), "class Small { int Value = 1; }".getBytes(StandardCharsets.UTF_8));
        PagedSourceDocument smallDocument = PagedSourceDocument.load(small, 1024);
        require(smallDocument.pageCount() == 1, "small source files should remain a single editor page");
        smallDocument.setAll("class Small { int Value = 2; }");
        smallDocument.writeTo(small);
        require(PagedSourceDocument.load(small, 1024).toString().contains("Value = 2"),
                "small-file edit/save/reopen regression must pass");

        File unicodeBoundary = new File(root, "UnicodeBoundary.cs");
        StringBuilder unicodeInput = new StringBuilder();
        for (int i = 0; i < 8191; i++) unicodeInput.append('a');
        unicodeInput.append("🚀");
        Files.write(unicodeBoundary.toPath(), unicodeInput.toString().getBytes(StandardCharsets.UTF_8));
        PagedSourceDocument unicodeDocument = PagedSourceDocument.load(unicodeBoundary, 8192);
        require(unicodeDocument.toString().equals(unicodeInput.toString()),
                "UTF-16 surrogate pair must survive a reader-buffer boundary");
        require(!Character.isHighSurrogate(unicodeDocument.getPage(0).charAt(8190)),
                "page split must not leave a high surrogate at the end of the previous page");

        File empty = new File(root, "Empty.cs");
        if (!empty.createNewFile()) throw new java.io.IOException("Could not create empty source fixture.");
        PagedSourceDocument emptyDocument = PagedSourceDocument.load(empty, 1024);
        require(emptyDocument.pageCount() == 1 && emptyDocument.length() == 0,
                "empty source files should have one empty page");

        deleteTree(root);
        System.out.println("Paged C# source document tests passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void deleteTree(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        file.delete();
    }
}
