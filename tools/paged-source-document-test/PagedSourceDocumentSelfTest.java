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

        File deleteFixture = new File(root, "DeleteAll.cs");
        Files.write(deleteFixture.toPath(), "REMOVE_ME".getBytes(StandardCharsets.UTF_8));
        PagedSourceDocument deleteDocument = PagedSourceDocument.load(deleteFixture, 1024);
        require(deleteDocument.replaceAll("REMOVE_ME", "", false) == 1,
                "replace-all must support deletion to an empty document");
        require(deleteDocument.length() == 0 && deleteDocument.pageCount() == 1
                        && deleteDocument.getPage(0).isEmpty(),
                "deleting all source text must leave one valid empty page");

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
