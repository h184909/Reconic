package no.reconic.generator.dns;

import no.reconic.generator.domain.DomainCandidate;
import no.reconic.generator.domain.DomainConfidence;
import no.reconic.generator.domain.DomainSource;
import no.reconic.generator.model.CompanyCandidate;
import no.reconic.generator.model.EntityType;
import no.reconic.generator.model.IndustrySegment;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DnsEnrichmentServiceTest {

    @Test
    void enrichesMxSpfDmarcAndNameServers() {
        FakeDnsResolver resolver = new FakeDnsResolver();
        resolver.put("example.no", "MX", List.of("20 mx2.example.no.", "10 mx1.example.no."));
        resolver.put("example.no", "TXT", List.of("\"google-site-verification=x\"", "\"v=spf1 include:spf.protection.outlook.com -all\""));
        resolver.put("_dmarc.example.no", "TXT", List.of("\"v=DMARC1; p=quarantine; rua=mailto:dmarc@example.no\""));
        resolver.put("example.no", "NS", List.of("NS2.EXAMPLE.NET.", "ns1.example.net."));

        DnsEnrichmentService service = new DnsEnrichmentService(resolver);
        CompanyCandidate enriched = service.enrich(candidate("example.no"));
        DnsObservation observation = enriched.dnsObservation();

        assertEquals(DnsLookupStatus.SUCCESS, observation.status());
        assertEquals(List.of("10 mx1.example.no", "20 mx2.example.no"), observation.mxRecords());
        assertEquals(List.of("v=spf1 include:spf.protection.outlook.com -all"), observation.spfRecords());
        assertEquals("quarantine", observation.dmarcPolicy());
        assertEquals(List.of("ns1.example.net", "ns2.example.net"), observation.nameServers());
        assertFalse(observation.hasErrors());
    }

    @Test
    void marksResultPartialWhenOneLookupFails() {
        FakeDnsResolver resolver = new FakeDnsResolver();
        resolver.put("example.no", "MX", List.of("10 mail.example.no."));
        resolver.fail("example.no", "TXT");
        resolver.put("_dmarc.example.no", "TXT", List.of());
        resolver.put("example.no", "NS", List.of("ns1.example.no."));

        DnsObservation observation = new DnsEnrichmentService(resolver)
                .enrich(candidate("example.no"))
                .dnsObservation();

        assertEquals(DnsLookupStatus.PARTIAL, observation.status());
        assertTrue(observation.hasMx());
        assertFalse(observation.hasSpf());
        assertTrue(observation.hasErrors());
    }

    @Test
    void skipsDnsWhenCandidateHasNoDomain() {
        CompanyCandidate candidate = candidate(null);
        CompanyCandidate enriched = new DnsEnrichmentService(new FakeDnsResolver()).enrich(candidate);

        assertEquals(DnsLookupStatus.SKIPPED, enriched.dnsObservation().status());
        assertFalse(enriched.dnsObservation().hasMx());
    }

    @Test
    void preservesCandidateOrderDuringParallelEnrichment() {
        FakeDnsResolver resolver = new FakeDnsResolver();
        for (String domain : List.of("a.no", "b.no", "c.no")) {
            resolver.put(domain, "MX", List.of());
            resolver.put(domain, "TXT", List.of());
            resolver.put("_dmarc." + domain, "TXT", List.of());
            resolver.put(domain, "NS", List.of());
        }

        List<CompanyCandidate> result = new DnsEnrichmentService(resolver).enrich(List.of(
                candidate("a.no"),
                candidate("b.no"),
                candidate("c.no")
        ));

        assertEquals(List.of("a.no", "b.no", "c.no"), result.stream()
                .map(value -> value.domainCandidate().domain())
                .toList());
    }

    private CompanyCandidate candidate(String domain) {
        DomainCandidate domainCandidate = domain == null
                ? DomainCandidate.none("Ikke funnet")
                : new DomainCandidate(
                        domain,
                        DomainSource.REGISTERED_WEBSITE,
                        DomainConfidence.HIGH,
                        false,
                        "Test"
                );

        return new CompanyCandidate(
                "999999999",
                "Eksempel AS",
                50,
                IndustrySegment.INDUSTRY,
                "25.110",
                "Produksjon",
                "1108",
                "SANDNES",
                "Testveien 1",
                domain,
                "post@" + (domain == null ? "example.no" : domain),
                "12345678",
                EntityType.MAIN_UNIT,
                null,
                domainCandidate,
                DnsObservation.skipped(domain)
        );
    }

    private static final class FakeDnsResolver implements DnsResolver {
        private final Map<String, List<String>> answers = new HashMap<>();
        private final Map<String, Boolean> failures = new HashMap<>();

        void put(String name, String type, List<String> values) {
            answers.put(key(name, type), values);
        }

        void fail(String name, String type) {
            failures.put(key(name, type), true);
        }

        @Override
        public List<String> lookup(String name, String recordType) {
            if (failures.containsKey(key(name, recordType))) {
                throw new DnsLookupException("Simulert feil", new RuntimeException("timeout"));
            }
            return answers.getOrDefault(key(name, recordType), List.of());
        }

        private String key(String name, String type) {
            return name + "|" + type;
        }
    }
}
