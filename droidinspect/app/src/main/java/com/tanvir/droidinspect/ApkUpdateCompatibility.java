package com.tanvir.droidinspect;

import java.util.List;

/** Conservative package/version/certificate comparison; never guarantees device installability. */
final class ApkUpdateCompatibility {
    static final class Assessment {
        final boolean samePackage;
        final boolean higherVersion;
        final boolean signerMatch;
        final boolean potentialUpdateCompatible;
        final String explanation;

        Assessment(boolean samePackage, boolean higherVersion, boolean signerMatch,
                   boolean potentialUpdateCompatible, String explanation) {
            this.samePackage = samePackage;
            this.higherVersion = higherVersion;
            this.signerMatch = signerMatch;
            this.potentialUpdateCompatible = potentialUpdateCompatible;
            this.explanation = explanation;
        }
    }

    private ApkUpdateCompatibility() { }

    static Assessment assess(String originalPackage, long originalVersionCode, List<String> originalSigners,
                             String outputPackage, long outputVersionCode, List<String> outputSigners) {
        boolean samePackage = known(originalPackage) && originalPackage.equals(outputPackage);
        boolean higherVersion = originalVersionCode >= 0 && outputVersionCode > originalVersionCode;
        boolean signerMatch = originalSigners != null && !originalSigners.isEmpty()
                && outputSigners != null && !outputSigners.isEmpty()
                && originalSigners.equals(outputSigners);
        boolean compatible = samePackage && higherVersion && signerMatch;
        String explanation;
        if (compatible) explanation = "Potential update-compatible package identity; Android/device/runtime checks are still required.";
        else if (!samePackage) explanation = "Not an update of the original package: package identity changed or is unknown.";
        else if (!signerMatch) explanation = "Not update-compatible with the original signing identity: signer certificate differs or original signer was unavailable.";
        else if (!higherVersion) explanation = "Not a version-code upgrade over the original APK.";
        else explanation = "Update compatibility is unresolved.";
        return new Assessment(samePackage, higherVersion, signerMatch, compatible, explanation);
    }

    private static boolean known(String value) {
        return value != null && !value.isEmpty() && !"(unknown)".equals(value);
    }
}
