package no.reconic.generator.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record CompanyDiscoveryResult(
        Instant generatedAt,
        int fetchedCount,
        int candidateCount,
        int filteredOutCount,
        List<CompanyCandidate> candidates,
        Map<IndustrySegment, Long> segmentCounts,
        int domainCount,
        int missingDomainCount,
        int websiteDomainCount,
        int emailDomainCount,
        int highConfidenceCount,
        int mediumConfidenceCount,
        int dnsAttemptedCount,
        int dnsSuccessCount,
        int dnsPartialCount,
        int dnsFailureCount,
        int mxCount,
        int spfCount,
        int dmarcCount,
        int nameServerCount,
        int microsoft365Count,
        int googleWorkspaceCount,
        int gatewayCount,
        int dmarcEnforcedCount,
        int dmarcMonitoringCount,
        int dmarcMissingCount,
        int providerSignalCount
) {
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public CompanyDiscoveryResult(
            Instant generatedAt,
            int fetchedCount,
            int candidateCount,
            int filteredOutCount,
            List<CompanyCandidate> candidates,
            Map<IndustrySegment, Long> segmentCounts,
            int domainCount,
            int missingDomainCount,
            int websiteDomainCount,
            int emailDomainCount,
            int highConfidenceCount,
            int mediumConfidenceCount,
            int dnsAttemptedCount,
            int dnsSuccessCount,
            int dnsPartialCount,
            int dnsFailureCount,
            int mxCount,
            int spfCount,
            int dmarcCount,
            int nameServerCount
    ) {
        this(
                generatedAt,
                fetchedCount,
                candidateCount,
                filteredOutCount,
                candidates,
                segmentCounts,
                domainCount,
                missingDomainCount,
                websiteDomainCount,
                emailDomainCount,
                highConfidenceCount,
                mediumConfidenceCount,
                dnsAttemptedCount,
                dnsSuccessCount,
                dnsPartialCount,
                dnsFailureCount,
                mxCount,
                spfCount,
                dmarcCount,
                nameServerCount,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );
    }

    public CompanyDiscoveryResult {
        candidates = List.copyOf(candidates);
        segmentCounts = Map.copyOf(segmentCounts);
    }

    public String generatedAtDisplay() {
        return DISPLAY_FORMAT.format(generatedAt.atZone(ZoneId.systemDefault()));
    }

    public double domainCoveragePercent() {
        if (candidateCount == 0) {
            return 0.0;
        }
        return 100.0 * domainCount / candidateCount;
    }

    public String domainCoverageDisplay() {
        return String.format(Locale.forLanguageTag("nb-NO"), "%.1f %%", domainCoveragePercent());
    }
}
