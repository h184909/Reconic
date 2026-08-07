package no.reconic.generator.intelligence;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PublicInfrastructureServiceTest {

    @Test
    void parsesDnssecPresentFromDoh() {
        String json = """
                {
                  "Status": 0,
                  "AD": true,
                  "Answer": [
                    {
                      "name": "example.no.",
                      "type": 43,
                      "TTL": 3600,
                      "data": "12345 13 2 ABCDEF0123456789"
                    }
                  ]
                }
                """;

        var result = PublicInfrastructureService.parseDnssecDoh(json);

        assertEquals(PublicSignalStatus.PRESENT, result.status());
        assertEquals(Boolean.TRUE, result.authenticatedData());
        assertEquals(1, result.dsRecords().size());
        assertTrue(result.dsRecords().getFirst().startsWith("12345 13 2"));
    }

    @Test
    void parsesDnssecMissingFromSuccessfulDohResponse() {
        String json = """
                {
                  "Status": 0,
                  "AD": true,
                  "Authority": [
                    {"name":"no.","type":6,"TTL":900,"data":"example"}
                  ]
                }
                """;

        var result = PublicInfrastructureService.parseDnssecDoh(json);

        assertEquals(PublicSignalStatus.MISSING, result.status());
        assertTrue(result.dsRecords().isEmpty());
    }

    @Test
    void parsesNxdomainAsMissingDnssec() {
        String json = """
                {"Status":3,"AD":true}
                """;

        var result = PublicInfrastructureService.parseDnssecDoh(json);

        assertEquals(PublicSignalStatus.MISSING, result.status());
    }

    @Test
    void parsesServerFailureAsUnknownDnssec() {
        String json = """
                {"Status":2,"AD":false}
                """;

        var result = PublicInfrastructureService.parseDnssecDoh(json);

        assertEquals(PublicSignalStatus.UNKNOWN, result.status());
        assertEquals(Boolean.FALSE, result.authenticatedData());
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

        List<String> names = PublicInfrastructureService.parseCertificateNames(
                json.toString(),
                "example.no"
        );

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
