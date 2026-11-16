package com.tanvir.droidinspect;

/** Host-JVM regression tests for added/removed/changed and unavailable IL comparisons. */
public final class AssemblyComparisonDiffSelfTest {
    public static void main(String[] args) {
        DotNetIlReader.Result baseline = new DotNetIlReader.Result();
        baseline.methods.add(method(1, "Example.Type", "Run", 1, "AA", ""));
        baseline.methods.add(method(2, "Example.Type", "Huge", 5000, "", ""));
        baseline.methods.add(method(3, "Example.Type", "Change", 1, "00", ""));
        baseline.methods.add(method(4, "Example.Type", "Gone", 1, "11", ""));

        DotNetIlReader.Result candidate = new DotNetIlReader.Result();
        candidate.methods.add(method(1, "Example.Type", "Run", 1, "AA", ""));
        candidate.methods.add(method(2, "Example.Type", "Huge", 5000, "", ""));
        candidate.methods.add(method(3, "Example.Type", "Change", 1, "01", ""));
        candidate.methods.add(method(4, "Example.Type", "New", 1, "22", ""));

        String methods = AssemblyComparisonDiff.compareMethods(baseline, candidate);
        require(methods, "Unchanged body records: 1");
        require(methods, "changed body records: 1");
        require(methods, "unavailable/unverified: 1");
        require(methods, "added: 1");
        require(methods, "removed: 1");
        require(methods, "Example.Type :: Huge");

        java.util.Set<String> left = new java.util.TreeSet<>();
        left.add("A"); left.add("B");
        java.util.Set<String> right = new java.util.TreeSet<>();
        right.add("B"); right.add("C");
        String diff = AssemblyComparisonDiff.compareSets("FIXTURE", left, right);
        require(diff, "+ C");
        require(diff, "- A");
        require(diff, "Added in candidate: 1");
        require(diff, "Removed from candidate: 1");

        System.out.println("PASS: structural additions/removals, raw IL changes, unchanged IL, and unavailable IL are classified distinctly.");
    }

    private static DotNetIlReader.MethodInfo method(int row, String owner, String name,
                                                     int size, String codeHex, String body) {
        return new DotNetIlReader.MethodInfo(row, 0, owner, name, 0, 0, 0, size, codeHex, body);
    }

    private static void require(String value, String expected) {
        if (!value.contains(expected)) throw new AssertionError("Missing [" + expected + "] in report:\n" + value);
    }
}
