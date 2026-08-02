package no.reconic.generator.export;

import no.reconic.generator.dns.DnsObservation;
import no.reconic.generator.domain.DomainCandidate;
import no.reconic.generator.domain.DomainConfidence;
import no.reconic.generator.domain.DomainSource;
import no.reconic.generator.model.CompanyCandidate;
import no.reconic.generator.model.EntityType;
import no.reconic.generator.model.IndustrySegment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationSampleServiceTest {

    @Test
    void createsFiftyDomainCandidatesWithThirtyHighAndTwentyMediumWhenAvailable() {
        List<CompanyCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < 40; index++) {
            candidates.add(candidate(index, DomainConfidence.HIGH));
        }
        for (int index = 40; index < 80; index++) {
            candidates.add(candidate(index, DomainConfidence.MEDIUM));
        }

        List<CompanyCandidate> sample = new ValidationSampleService().createSample(candidates);

        assertEquals(50, sample.size());
        assertEquals(30, sample.stream()
                .filter(candidate -> candidate.domainCandidate().confidence() == DomainConfidence.HIGH)
                .count());
        assertEquals(20, sample.stream()
                .filter(candidate -> candidate.domainCandidate().confidence() == DomainConfidence.MEDIUM)
                .count());
        assertEquals(50, sample.stream().distinct().count());
    }

    @Test
    void excludesCandidatesWithoutDomain() {
        List<CompanyCandidate> candidates = List.of(
                candidate(1, DomainConfidence.HIGH),
                withoutDomain(2)
        );

        List<CompanyCandidate> sample = new ValidationSampleService().createSample(candidates);

        assertEquals(1, sample.size());
        assertTrue(sample.getFirst().domainCandidate().hasDomain());
    }

    private CompanyCandidate candidate(int index, DomainConfidence confidence) {
        String domain = "company" + index + ".no";
        return new CompanyCandidate(
                String.format("%09d", index),
                "Company " + index,
                50,
                IndustrySegment.INDUSTRY,
                "25.110",
                "Produksjon",
                "1108",
                "SANDNES",
                null,
                domain,
                "post@" + domain,
                null,
                EntityType.MAIN_UNIT,
                null,
                new DomainCandidate(
                        domain,
                        confidence == DomainConfidence.HIGH
                                ? DomainSource.REGISTERED_WEBSITE
                                : DomainSource.REGISTERED_EMAIL,
                        confidence,
                        confidence != DomainConfidence.HIGH,
                        "Test"
                ),
                DnsObservation.skipped(domain)
        );
    }

    private CompanyCandidate withoutDomain(int index) {
        return new CompanyCandidate(
                String.format("%09d", index),
                "Company " + index,
                50,
                IndustrySegment.INDUSTRY,
                "25.110",
                "Produksjon",
                "1108",
                "SANDNES",
                null,
                null,
                null,
                null,
                EntityType.MAIN_UNIT,
                null,
                DomainCandidate.none("Ikke funnet"),
                DnsObservation.skipped(null)
        );
    }
}
