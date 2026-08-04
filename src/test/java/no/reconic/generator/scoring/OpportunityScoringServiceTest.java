package no.reconic.generator.scoring;

import no.reconic.generator.dns.DnsLookupStatus;
import no.reconic.generator.dns.DnsObservation;
import no.reconic.generator.domain.DomainCandidate;
import no.reconic.generator.domain.DomainConfidence;
import no.reconic.generator.domain.DomainSource;
import no.reconic.generator.intelligence.TechnologyAnalysisService;
import no.reconic.generator.model.CompanyCandidate;
import no.reconic.generator.model.EntityType;
import no.reconic.generator.model.IndustrySegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpportunityScoringServiceTest {

    private final TechnologyAnalysisService technologyService = new TechnologyAnalysisService();
    private final OpportunityScoringService scoringService = new OpportunityScoringService();

    @Test
    void prioritizesGoodMarketFitWithWeakDmarcAndSpfSignals() {
        CompanyCandidate candidate = candidate(
                55,
                IndustrySegment.LEGAL_ACCOUNTING,
                DomainConfidence.HIGH,
                dns(
                        List.of("0 example-no.mail.protection.outlook.com"),
                        List.of("v=spf1 include:spf.protection.outlook.com ~all"),
                        null,
                        null,
                        List.of("ns1.example.no")
                )
        );

        OpportunityAssessment assessment = scoringService.score(candidate);

        assertTrue(assessment.opportunityScore() >= 65);
        assertTrue(assessment.technicalOpportunityScore() >= 15);
        assertTrue(assessment.reasonsDisplay().contains("DMARC"));
        assertTrue(assessment.dataConfidenceScore() >= 80);
    }

    @Test
    void microsoft365AloneDoesNotCreateHighTechnicalScore() {
        CompanyCandidate candidate = candidate(
                50,
                IndustrySegment.INDUSTRY,
                DomainConfidence.HIGH,
                dns(
                        List.of("0 example-no.mail.protection.outlook.com"),
                        List.of("v=spf1 include:spf.protection.outlook.com -all"),
                        "v=DMARC1; p=reject",
                        "reject",
                        List.of("ns1.hjelseth.com", "ns2.hjelseth.com")
                )
        );

        OpportunityAssessment assessment = scoringService.score(candidate);

        assertTrue(assessment.technicalOpportunityScore() <= 3);
        assertTrue(assessment.opportunityScore() < 65);
    }

    @Test
    void mediumDomainConfidenceReducesDataConfidenceButNotOpportunity() {
        DnsObservation dns = dns(
                List.of("0 example-no.mail.protection.outlook.com"),
                List.of("v=spf1 include:spf.protection.outlook.com ~all"),
                "v=DMARC1; p=none",
                "none",
                List.of("ns1.example.no")
        );
        CompanyCandidate high = candidate(50, IndustrySegment.CONSTRUCTION, DomainConfidence.HIGH, dns);
        CompanyCandidate medium = candidate(50, IndustrySegment.CONSTRUCTION, DomainConfidence.MEDIUM, dns);

        OpportunityAssessment highAssessment = scoringService.score(high);
        OpportunityAssessment mediumAssessment = scoringService.score(medium);

        assertEquals(highAssessment.opportunityScore(), mediumAssessment.opportunityScore());
        assertTrue(highAssessment.dataConfidenceScore() > mediumAssessment.dataConfidenceScore());
        assertTrue(mediumAssessment.warningsDisplay().contains("verifiseres manuelt"));
    }

    @Test
    void dnsProviderIsNotTreatedAsMspRelationship() {
        CompanyCandidate candidate = candidate(
                50,
                IndustrySegment.CONSULTING_TECHNICAL,
                DomainConfidence.HIGH,
                dns(
                        List.of("0 example-no.mail.protection.outlook.com"),
                        List.of("v=spf1 include:spf.protection.outlook.com -all"),
                        "v=DMARC1; p=reject",
                        "reject",
                        List.of("ns1.hyp.net", "ns2.hyp.net")
                )
        );

        OpportunityAssessment assessment = scoringService.score(candidate);

        assertEquals(12, assessment.providerLandscapeScore());
        assertTrue(assessment.reasonsDisplay().contains("Bare infrastrukturspor"));
    }

    @Test
    void multipleSpfRecordsCreateStrongTechnicalReason() {
        CompanyCandidate candidate = candidate(
                50,
                IndustrySegment.INDUSTRY,
                DomainConfidence.HIGH,
                dns(
                        List.of("0 example-no.mail.protection.outlook.com"),
                        List.of("v=spf1 include:spf.protection.outlook.com -all", "v=spf1 include:amazonses.com ~all"),
                        "v=DMARC1; p=reject",
                        "reject",
                        List.of("ns1.example.no")
                )
        );

        OpportunityAssessment assessment = scoringService.score(candidate);

        assertTrue(assessment.technicalOpportunityScore() >= 14);
        assertTrue(assessment.reasonsDisplay().contains("Flere SPF-poster"));
    }

    @Test
    void missingDomainKeepsScoreExplainableAndConfidenceLow() {
        CompanyCandidate candidate = new CompanyCandidate(
                "999999999",
                "Eksempel AS",
                50,
                IndustrySegment.INDUSTRY,
                "25.110",
                "Produksjon",
                "1108",
                "SANDNES",
                "Testveien 1",
                null,
                null,
                "12345678",
                EntityType.MAIN_UNIT,
                null,
                DomainCandidate.none("Ingen domene"),
                DnsObservation.skipped(null)
        );

        OpportunityAssessment assessment = scoringService.score(candidate);

        assertEquals(0, assessment.technicalOpportunityScore());
        assertTrue(assessment.dataConfidenceScore() < 40);
        assertTrue(assessment.warningsDisplay().contains("domene"));
    }

    private CompanyCandidate candidate(
            int employees,
            IndustrySegment segment,
            DomainConfidence domainConfidence,
            DnsObservation dns
    ) {
        String domain = "example.no";
        CompanyCandidate base = new CompanyCandidate(
                "999999999",
                "Eksempel AS",
                employees,
                segment,
                "25.110",
                "Produksjon",
                "1108",
                "SANDNES",
                "Testveien 1",
                "https://example.no",
                "post@example.no",
                "12345678",
                EntityType.MAIN_UNIT,
                null,
                new DomainCandidate(
                        domain,
                        domainConfidence == DomainConfidence.HIGH
                                ? DomainSource.REGISTERED_WEBSITE
                                : DomainSource.REGISTERED_EMAIL,
                        domainConfidence,
                        domainConfidence != DomainConfidence.HIGH,
                        "Test"
                ),
                dns
        );
        return base.withTechnologyObservation(technologyService.analyze(dns));
    }

    private DnsObservation dns(
            List<String> mx,
            List<String> spf,
            String dmarcRecord,
            String dmarcPolicy,
            List<String> ns
    ) {
        return new DnsObservation(
                "example.no",
                DnsLookupStatus.SUCCESS,
                mx,
                spf,
                dmarcRecord,
                dmarcPolicy,
                ns,
                List.of()
        );
    }
}
