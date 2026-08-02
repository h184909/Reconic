package no.reconic.generator.dns;

import java.util.List;

public record DnsObservation(
        String domain,
        DnsLookupStatus status,
        List<String> mxRecords,
        List<String> spfRecords,
        String dmarcRecord,
        String dmarcPolicy,
        List<String> nameServers,
        List<String> lookupErrors
) {
    public DnsObservation {
        status = status == null ? DnsLookupStatus.SKIPPED : status;
        mxRecords = copy(mxRecords);
        spfRecords = copy(spfRecords);
        nameServers = copy(nameServers);
        lookupErrors = copy(lookupErrors);
        dmarcRecord = normalize(dmarcRecord);
        dmarcPolicy = normalize(dmarcPolicy);
    }

    public static DnsObservation skipped(String domain) {
        return new DnsObservation(
                domain,
                DnsLookupStatus.SKIPPED,
                List.of(),
                List.of(),
                null,
                null,
                List.of(),
                List.of()
        );
    }

    public static DnsObservation failed(String domain, String error) {
        return new DnsObservation(
                domain,
                DnsLookupStatus.FAILED,
                List.of(),
                List.of(),
                null,
                null,
                List.of(),
                error == null || error.isBlank() ? List.of("Ukjent DNS-feil") : List.of(error)
        );
    }

    public boolean hasMx() {
        return !mxRecords.isEmpty();
    }

    public boolean hasSpf() {
        return !spfRecords.isEmpty();
    }

    public boolean hasDmarc() {
        return dmarcRecord != null;
    }

    public boolean hasNameServers() {
        return !nameServers.isEmpty();
    }

    public boolean hasErrors() {
        return !lookupErrors.isEmpty();
    }

    public String mxDisplay() {
        return hasMx() ? String.join(" | ", mxRecords) : "Ikke funnet";
    }

    public String spfDisplay() {
        return hasSpf() ? String.join(" | ", spfRecords) : "Ikke funnet";
    }

    public String dmarcDisplay() {
        if (!hasDmarc()) {
            return "Ikke funnet";
        }
        return dmarcPolicy == null ? dmarcRecord : dmarcPolicy;
    }

    public String nameServersDisplay() {
        return hasNameServers() ? String.join(" | ", nameServers) : "Ikke funnet";
    }

    public String errorsDisplay() {
        return hasErrors() ? String.join(" | ", lookupErrors) : "";
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
