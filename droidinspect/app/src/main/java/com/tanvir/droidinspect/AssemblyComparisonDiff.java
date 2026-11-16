package com.tanvir.droidinspect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedHashMap;

/** Pure comparison formatting and raw-IL classification shared by production and host tests. */
final class AssemblyComparisonDiff {
    private AssemblyComparisonDiff() { }

    static String compareSets(String heading, Set<String> baseline, Set<String> candidate) {
        Set<String> added = new TreeSet<>(candidate);
        added.removeAll(baseline);
        Set<String> removed = new TreeSet<>(baseline);
        removed.removeAll(candidate);
        StringBuilder out = new StringBuilder();
        out.append(heading).append('\n');
        out.append("Baseline entries: ").append(baseline.size())
                .append("; candidate entries: ").append(candidate.size()).append('\n');
        out.append("Added in candidate: ").append(added.size()).append('\n');
        for (String item : added) out.append("  + ").append(item).append('\n');
        out.append("Removed from candidate: ").append(removed.size()).append('\n');
        for (String item : removed) out.append("  - ").append(item).append('\n');
        if (added.isEmpty() && removed.isEmpty()) out.append("No structural line differences detected.\n");
        out.append('\n');
        return out.toString();
    }

    static String compareMethods(DotNetIlReader.Result baseline, DotNetIlReader.Result candidate) {
        Map<String, List<String>> a = methodBodies(baseline);
        Map<String, List<String>> b = methodBodies(candidate);
        Set<String> keys = new TreeSet<>();
        keys.addAll(a.keySet());
        keys.addAll(b.keySet());
        int added = 0, removed = 0, changed = 0, same = 0, unknown = 0;
        List<String> details = new ArrayList<>();
        for (String key : keys) {
            List<String> x = a.get(key), y = b.get(key);
            if (x == null) {
                added += y.size();
                details.add("  + " + key + " (" + y.size() + " method record(s))");
            } else if (y == null) {
                removed += x.size();
                details.add("  - " + key + " (" + x.size() + " method record(s))");
            } else {
                int paired = Math.min(x.size(), y.size());
                if (containsUnavailable(x) || containsUnavailable(y)) {
                    unknown += paired;
                    details.add("  ? " + key + " — one or more IL bodies exceed the comparison byte cap or could not be decoded");
                } else if (x.equals(y)) {
                    same += paired;
                } else {
                    changed += paired;
                    details.add("  * " + key + " — raw IL body differs (" + x.size()
                            + " baseline / " + y.size() + " candidate)");
                }
                if (y.size() > x.size()) added += y.size() - x.size();
                if (x.size() > y.size()) removed += x.size() - y.size();
            }
        }
        StringBuilder out = new StringBuilder("METHOD IL COMPARISON\n");
        out.append("Baseline methods indexed: ").append(baseline.methods.size())
                .append("; candidate methods indexed: ").append(candidate.methods.size()).append('\n');
        out.append("Unchanged body records: ").append(same).append("; changed body records: ")
                .append(changed).append("; unavailable/unverified: ").append(unknown)
                .append("; added: ").append(added).append("; removed: ").append(removed).append('\n');
        for (String detail : details) out.append(detail).append('\n');
        out.append('\n');
        return out.toString();
    }

    private static boolean containsUnavailable(List<String> bodies) {
        for (String body : bodies) if (body.startsWith("UNAVAILABLE_IL:")) return true;
        return false;
    }

    private static Map<String, List<String>> methodBodies(DotNetIlReader.Result result) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        for (DotNetIlReader.MethodInfo method : result.methods) {
            String key = method.owner + " :: " + method.name;
            String body = method.codeHex == null ? "" : method.codeHex;
            if (method.codeSize == 0 && method.body != null && method.body.startsWith("IL body unavailable:"))
                body = "UNAVAILABLE_IL:decode";
            else if (method.codeSize == 0) body = "NO_BODY";
            else if (body.isEmpty()) body = "UNAVAILABLE_IL:" + method.codeSize;
            values.computeIfAbsent(key, ignored -> new ArrayList<>()).add(body);
        }
        for (List<String> bodies : values.values()) Collections.sort(bodies);
        return values;
    }
}
