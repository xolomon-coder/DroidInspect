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

        String first = document.getPage(0);
        document.setPage(0, first + "// first page edit\n");
        int inserted = document.toString().indexOf("// first page edit");
        require(inserted >= 0, "page edit must persist in the document model");

        String boundary = "BOUNDARY_LEFTBOUNDARY_RIGHT";
        int boundaryAt = document.find(boundary, 0, true, false);
        require(boundaryAt >= 0, "whole-document search must find a token spanning page boundaries");

        int count = document.replaceAll("public int Value0 =", "public int EditedValue0 =", false);
        require(count == 9, "replace-all must operate across all pages; expected 9, got " + count);
        require(document.toString().contains("public int EditedValue0 ="), "replacement text must be present");

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
        require(reopened.pageCount() == document.pageCount(), "save/reopen must retain bounded page model");

        File small = new File(root, "Small.cs");
        Files.write(small.toPath(), "class Small { int Value = 1; }".getBytes(StandardCharsets.UTF_8));
        PagedSourceDocument smallDocument = PagedSourceDocument.load(small, 1024);
        require(smallDocument.pageCount() == 1, "small source files should remain a single editor page");
        smallDocument.setAll("class Small { int Value = 2; }");
        smallDocument.writeTo(small);
        require(PagedSourceDocument.load(small, 1024).toString().contains("Value = 2"),
                "small-file edit/save/reopen regression must pass");

        File empty = new File(root, "Empty.cs");
        Files.createNewFile(empty.toPath());
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
