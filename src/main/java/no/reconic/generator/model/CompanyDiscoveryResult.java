package no.reconic.generator.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import no.reconic.generator.scoring.OpportunityPriority;

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
    public int veryHighOpportunityCount() {
        return countPriority(OpportunityPriority.VERY_HIGH);
    }

    public int highOpportunityCount() {
        return countPriority(OpportunityPriority.HIGH);
    }

    public int actionableOpportunityCount() {
        return veryHighOpportunityCount() + highOpportunityCount();
    }

    public int mediumOpportunityCount() {
        return countPriority(OpportunityPriority.MEDIUM);
    }

    public int lowDataConfidenceCount() {
        return (int) candidates.stream()
                .filter(candidate -> candidate.opportunityAssessment().dataConfidenceScore() < 60)
                .count();
    }

    public double averageOpportunityScore() {
        return candidates.stream()
                .mapToInt(candidate -> candidate.opportunityAssessment().opportunityScore())
                .average()
                .orElse(0.0);
    }

    public String averageOpportunityScoreDisplay() {
        return String.format(Locale.forLanguageTag("nb-NO"), "%.1f", averageOpportunityScore());
    }

    public double averageDataConfidenceScore() {
        return candidates.stream()
                .mapToInt(candidate -> candidate.opportunityAssessment().dataConfidenceScore())
                .average()
                .orElse(0.0);
    }

    public String averageDataConfidenceDisplay() {
        return String.format(Locale.forLanguageTag("nb-NO"), "%.1f", averageDataConfidenceScore());
    }

    private int countPriority(OpportunityPriority priority) {
        return (int) candidates.stream()
                .filter(candidate -> candidate.opportunityAssessment().priority() == priority)
                .count();
    }

}
