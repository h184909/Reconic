package no.reconic.generator.domain;

import no.reconic.generator.dns.DnsLookupStatus;
import no.reconic.generator.dns.DnsObservation;
import no.reconic.generator.model.CompanyCandidate;
import no.reconic.generator.model.EntityType;
import no.reconic.generator.model.IndustrySegment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainOverrideServiceTest {

    @Test
    void appliesValidatedOverrideAndResetsDownstreamAnalysis() {
        DomainOverrideService service = new DomainOverrideService(Map.of(
                "984030460",
                new DomainOverrideService.DomainOverride(
                        "sandneshavneterminal.no",
                        "Bekreftet i benchmark."
                )
        ));
        CompanyCandidate candidate = candidate(
                "984030460",
                new DomainCandidate(
                        "aktivepost.no",
                        DomainSource.REGISTERED_EMAIL,
                        DomainConfidence.MEDIUM,
                        true,
                        "Fra e-post"
                )
        );

        CompanyCandidate overridden = service.apply(candidate);

        assertEquals("sandneshavneterminal.no", overridden.domainCandidate().domain());
        assertEquals(DomainSource.MANUAL_OVERRIDE, overridden.domainCandidate().source());
        assertEquals(DomainConfidence.HIGH, overridden.domainCandidate().confidence());
        assertFalse(overridden.domainCandidate().requiresVerification());
        assertTrue(overridden.domainCandidate().explanation().contains("aktivepost.no"));
        assertEquals(DnsLookupStatus.SKIPPED, overridden.dnsObservation().status());
    }

    @Test
    void leavesUnknownOrganizationUnchanged() {
        DomainOverrideService service = new DomainOverrideService(Map.of(
                "984030460",
                new DomainOverrideService.DomainOverride("sandneshavneterminal.no", "Test")
        ));
        CompanyCandidate candidate = candidate(
                "999999999",
                new DomainCandidate(
                        "example.no",
                        DomainSource.REGISTERED_WEBSITE,
                        DomainConfidence.HIGH,
                        false,
                        "Test"
                )
        );

        assertEquals(candidate, service.apply(candidate));
    }

    @Test
    void bundledOverrideFileContainsValidatedCorrections() {
        DomainOverrideService service = new DomainOverrideService();

        assertTrue(service.overrideCount() >= 2);
    }

    private CompanyCandidate candidate(String organizationNumber, DomainCandidate domain) {
        return new CompanyCandidate(
                organizationNumber,
                "Eksempel AS",
                50,
                IndustrySegment.INDUSTRY,
                "25.110",
                "Produksjon",
                "1108",
                "SANDNES",
                "Testveien 1",
                null,
                "post@example.no",
                "12345678",
                EntityType.MAIN_UNIT,
                null,
                domain,
                new DnsObservation(
                        domain.domain(),
                        DnsLookupStatus.SUCCESS,
                        List.of("0 mail.example.no"),
                        List.of("v=spf1 -all"),
                        "v=DMARC1; p=reject",
                        "reject",
                        List.of("ns1.example.no"),
                        List.of()
                )
        );
    }
}
