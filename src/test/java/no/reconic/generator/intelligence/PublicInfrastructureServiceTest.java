package no.reconic.generator.intelligence;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PublicInfrastructureServiceTest {

    @Test
    void parsesNoridDnssecAndEvents() {
        String json = """
                {
                  "secureDNS": {"delegationSigned": true},
                  "events": [
                    {"eventAction":"registration","eventDate":"2020-01-02T10:00:00Z"},
                    {"eventAction":"last changed","eventDate":"2026-07-01T12:30:00Z"}
                  ]
                }
                """;

        var result = PublicInfrastructureService.parseNoridRdap(json);

        assertEquals(PublicSignalStatus.PRESENT, result.status());
        assertEquals(Boolean.TRUE, result.dnssec());
        assertEquals("2020-01-02T10:00:00Z", result.createdAt());
        assertEquals("2026-07-01T12:30:00Z", result.updatedAt());
    }

    @Test
    void certificateTransparencyKeepsOnlyRequestedDomainAndSubdomains() {
        String json = """
                [
                  {"name_value":"example.no\\nwww.example.no\\n*.vpn.example.no"},
                  {"name_value":"unrelated.no"}
                ]
                """;

        List<String> names = PublicInfrastructureService.parseCertificateNames(json, "example.no");

        assertTrue(names.contains("example.no"));
        assertTrue(names.contains("www.example.no"));
        assertTrue(names.contains("vpn.example.no"));
        assertFalse(names.contains("unrelated.no"));
    }

    @Test
    void certificateTransparencyIsBounded() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < 40; i++) {
            if (i > 0) json.append(",");
            json.append("{\"name_value\":\"host").append(i).append(".example.no\"}");
        }
        json.append("]");

        List<String> names = PublicInfrastructureService.parseCertificateNames(json.toString(), "example.no");

        assertTrue(names.size() <= 25);
    }

    @Test
    void emptyPublicObservationIsSafe() {
        var observation = PublicInfrastructureObservation.empty();

        assertEquals(PublicSignalStatus.SKIPPED, observation.mtaStsStatus());
        assertFalse(observation.hasMtaSts());
        assertFalse(observation.hasTlsRpt());
        assertFalse(observation.hasDnssec());
        assertTrue(observation.certificateNames().isEmpty());
    }

    @Test
    void technologyObservationPreservesExistingFieldsWhenPublicDataIsAdded() {
        var technology = TechnologyObservation.empty();
        var publicData = new PublicInfrastructureObservation(
                PublicSignalStatus.PRESENT,
                "v=STSv1; id=abc",
                PublicSignalStatus.MISSING,
                null,
                PublicSignalStatus.PRESENT,
                "autodiscover.outlook.com",
                PublicSignalStatus.SKIPPED,
                null,
                null,
                null,
                PublicSignalStatus.SKIPPED,
                List.of(),
                List.of(),
                List.of("MTA-STS TXT funnet")
        );

        var updated = technology.withPublicInfrastructure(publicData);

        assertEquals(technology.emailPlatform(), updated.emailPlatform());
        assertTrue(updated.publicInfrastructure().hasMtaSts());
        assertTrue(updated.publicInfrastructure().hasDnssec());
        assertTrue(updated.evidenceDisplay().contains("MTA-STS"));
    }
}
