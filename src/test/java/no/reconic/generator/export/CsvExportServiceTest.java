package no.reconic.generator.export;

import no.reconic.generator.dns.DnsLookupStatus;
import no.reconic.generator.dns.DnsObservation;
import no.reconic.generator.domain.DomainCandidate;
import no.reconic.generator.domain.DomainConfidence;
import no.reconic.generator.domain.DomainSource;
import no.reconic.generator.model.CompanyCandidate;
import no.reconic.generator.model.CompanyDiscoveryResult;
import no.reconic.generator.model.EntityType;
import no.reconic.generator.model.IndustrySegment;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvExportServiceTest {

    @Test
    void exportsUtf8BomDnsFieldsAndManualColumns() {
        CompanyCandidate candidate = candidate();
        CompanyDiscoveryResult result = result(candidate);
        CsvExportService service = new CsvExportService(new ValidationSampleService());

        byte[] bytes = service.exportAll(result);
        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
        assertTrue(csv.contains("generatedDomain;domainSource;domainConfidence"));
        assertTrue(csv.contains("mxRecords;spfRecords;dmarcRecord;dmarcPolicy;nameServers;dnsErrors"));
        assertTrue(csv.contains("manualDomain;isCorrect;comment"));
        assertTrue(csv.contains("example.no"));
        assertTrue(csv.contains("10 mail.example.no"));
        assertTrue(csv.contains("quarantine"));
    }

    @Test
    void escapesSemicolonsAndQuotes() {
        CsvExportService service = new CsvExportService(new ValidationSampleService());
        byte[] bytes = service.exportAll(result(candidate()));
        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Eksempel; \"\"Test\"\" AS\""));
    }

    private CompanyCandidate candidate() {
        String domain = "example.no";
        return new CompanyCandidate(
                "999999999",
                "Eksempel; \"Test\" AS",
                50,
                IndustrySegment.INDUSTRY,
                "25.110",
                "Produksjon",
                "1108",
                "SANDNES",
                "Testveien 1",
                domain,
                "post@example.no",
                "12345678",
                EntityType.MAIN_UNIT,
                null,
                new DomainCandidate(
                        domain,
                        DomainSource.REGISTERED_WEBSITE,
                        DomainConfidence.HIGH,
                        false,
                        "Test"
                ),
                new DnsObservation(
                        domain,
                        DnsLookupStatus.SUCCESS,
                        List.of("10 mail.example.no"),
                        List.of("v=spf1 -all"),
                        "v=DMARC1; p=quarantine",
                        "quarantine",
                        List.of("ns1.example.no"),
                        List.of()
                )
        );
    }

    private CompanyDiscoveryResult result(CompanyCandidate candidate) {
        return new CompanyDiscoveryResult(
                Instant.now(),
                1,
                1,
                0,
                List.of(candidate),
                Map.of(IndustrySegment.INDUSTRY, 1L),
                1,
                0,
                1,
                0,
                1,
                0,
                1,
                1,
                0,
                0,
                1,
                1,
                1,
                1
        );
    }
}
