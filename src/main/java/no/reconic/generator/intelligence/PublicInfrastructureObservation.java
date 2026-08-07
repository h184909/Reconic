package no.reconic.generator.intelligence;

import java.util.List;

public record PublicInfrastructureObservation(
        PublicSignalStatus mtaStsStatus,
        String mtaStsRecord,
        PublicSignalStatus tlsRptStatus,
        String tlsRptRecord,
        PublicSignalStatus dnssecStatus,
        String autodiscoverTarget,
        PublicSignalStatus noridStatus,
        Boolean noridDnssec,
        String noridCreatedAt,
        String noridUpdatedAt,
        PublicSignalStatus certificateTransparencyStatus,
        List<String> certificateNames,
        List<String> lookupWarnings,
        List<String> evidence
) {
    public PublicInfrastructureObservation {
        mtaStsStatus = mtaStsStatus == null ? PublicSignalStatus.UNKNOWN : mtaStsStatus;
        mtaStsRecord = normalize(mtaStsRecord);
        tlsRptStatus = tlsRptStatus == null ? PublicSignalStatus.UNKNOWN : tlsRptStatus;
        tlsRptRecord = normalize(tlsRptRecord);
        dnssecStatus = dnssecStatus == null ? PublicSignalStatus.UNKNOWN : dnssecStatus;
        autodiscoverTarget = normalize(autodiscoverTarget);
        noridStatus = noridStatus == null ? PublicSignalStatus.SKIPPED : noridStatus;
        noridCreatedAt = normalize(noridCreatedAt);
        noridUpdatedAt = normalize(noridUpdatedAt);
        certificateTransparencyStatus = certificateTransparencyStatus == null
                ? PublicSignalStatus.SKIPPED
                : certificateTransparencyStatus;
        certificateNames = certificateNames == null ? List.of() : List.copyOf(certificateNames);
        lookupWarnings = lookupWarnings == null ? List.of() : List.copyOf(lookupWarnings);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static PublicInfrastructureObservation empty() {
        return new PublicInfrastructureObservation(
                PublicSignalStatus.SKIPPED,
                null,
                PublicSignalStatus.SKIPPED,
                null,
                PublicSignalStatus.SKIPPED,
                null,
                PublicSignalStatus.SKIPPED,
                null,
                null,
                null,
                PublicSignalStatus.SKIPPED,
                List.of(),
                List.of(),
                List.of()
        );
    }

    public boolean hasMtaSts() {
        return mtaStsStatus == PublicSignalStatus.PRESENT;
    }

    public boolean hasTlsRpt() {
        return tlsRptStatus == PublicSignalStatus.PRESENT;
    }

    public boolean hasDnssec() {
        return dnssecStatus == PublicSignalStatus.PRESENT
                || Boolean.TRUE.equals(noridDnssec);
    }

    public boolean hasAutodiscover() {
        return autodiscoverTarget != null;
    }

    public boolean hasCertificateNames() {
        return !certificateNames.isEmpty();
    }

    public String certificateNamesDisplay() {
        return String.join(" | ", certificateNames);
    }

    public String warningsDisplay() {
        return String.join(" | ", lookupWarnings);
    }

    public String evidenceDisplay() {
        return String.join(" | ", evidence);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
