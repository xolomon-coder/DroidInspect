package com.tanvir.droidinspect;

import java.util.Arrays;
import java.util.Collections;

/** Host tests for conservative package/version/certificate update assessment. */
public final class ApkUpdateCompatibilitySelfTest {
    public static void main(String[] args) {
        ApkUpdateCompatibility.Assessment pass = ApkUpdateCompatibility.assess(
                "com.example.app", 10, Arrays.asList("AA", "BB"),
                "com.example.app", 11, Arrays.asList("AA", "BB"));
        check(pass.potentialUpdateCompatible, "same package, higher version and exact signer set should pass potential assessment");

        ApkUpdateCompatibility.Assessment signerMismatch = ApkUpdateCompatibility.assess(
                "com.example.app", 10, Collections.singletonList("AA"),
                "com.example.app", 11, Collections.singletonList("CC"));
        check(!signerMismatch.potentialUpdateCompatible && !signerMismatch.signerMatch,
                "different signer must never be marked update-compatible");

        ApkUpdateCompatibility.Assessment packageMismatch = ApkUpdateCompatibility.assess(
                "com.example.app", 10, Collections.singletonList("AA"),
                "com.other.app", 11, Collections.singletonList("AA"));
        check(!packageMismatch.potentialUpdateCompatible && !packageMismatch.samePackage,
                "different package ID must never be marked update-compatible");

        ApkUpdateCompatibility.Assessment downgrade = ApkUpdateCompatibility.assess(
                "com.example.app", 10, Collections.singletonList("AA"),
                "com.example.app", 9, Collections.singletonList("AA"));
        check(!downgrade.potentialUpdateCompatible && !downgrade.higherVersion,
                "lower version code must not be marked update-compatible");

        ApkUpdateCompatibility.Assessment unknownOriginalSigner = ApkUpdateCompatibility.assess(
                "com.example.app", 10, Collections.emptyList(),
                "com.example.app", 11, Collections.singletonList("AA"));
        check(!unknownOriginalSigner.potentialUpdateCompatible,
                "unknown original signer must not be treated as matching");

        System.out.println("PASS: update assessment requires same package, higher version code, and exact verified signer set.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
