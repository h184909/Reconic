package no.reconic.generator.intelligence;

import no.reconic.generator.dns.DnsLookupStatus;
import no.reconic.generator.dns.DnsObservation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechnologyAnalysisServiceTest {

    private final TechnologyAnalysisService service = new TechnologyAnalysisService();

    @Test
    void detectsDirectMicrosoft365AndEnforcedDmarc() {
        TechnologyObservation observation = service.analyze(dns(
                List.of("0 example-no.mail.protection.outlook.com"),
                List.of("v=spf1 include:spf.protection.outlook.com -all"),
                "v=DMARC1; p=reject",
                "reject",
                List.of("ns1.example.no")
        ));

        assertEquals(EmailPlatform.MICROSOFT_365, observation.emailPlatform());
        assertEquals(SignalConfidence.HIGH, observation.emailPlatformConfidence());
        assertEquals(EmailGateway.NONE, observation.emailGateway());
        assertEquals(DmarcPosture.REJECT, observation.dmarcPosture());
        assertEquals(SpfPosture.HARD_FAIL, observation.spfPosture());
    }

    @Test
    void separatesProofpointGatewayFromMicrosoft365Platform() {
        TechnologyObservation observation = service.analyze(dns(
                List.of("1 mx1-eu1.ppe-hosted.com", "2 mx2-eu1.ppe-hosted.com"),
                List.of("v=spf1 include:spf.protection.outlook.com ~all"),
                "v=DMARC1; p=none",
                "none",
                List.of("ns1.hyp.net")
        ));

        assertEquals(EmailGateway.PROOFPOINT, observation.emailGateway());
        assertEquals(EmailPlatform.MICROSOFT_365, observation.emailPlatform());
        assertEquals(SignalConfidence.MEDIUM, observation.emailPlatformConfidence());
        assertEquals(DmarcPosture.MONITORING, observation.dmarcPosture());
    }

    @Test
    void detectsGoogleWorkspaceFromMx() {
        TechnologyObservation observation = service.analyze(dns(
                List.of("10 aspmx.l.google.com", "20 alt1.aspmx.l.google.com"),
                List.of("v=spf1 include:_spf.google.com ~all"),
                null,
                null,
                List.of("ns1.hyp.net")
        ));

        assertEquals(EmailPlatform.GOOGLE_WORKSPACE, observation.emailPlatform());
        assertEquals(SignalConfidence.HIGH, observation.emailPlatformConfidence());
        assertEquals(DmarcPosture.MISSING, observation.dmarcPosture());
    }

    @Test
    void flagsMultipleSpfRecords() {
        TechnologyObservation observation = service.analyze(dns(
                List.of("10 mail.example.no"),
                List.of("v=spf1 include:spf.protection.outlook.com -all", "v=spf1 include:amazonses.com ~all"),
                null,
                null,
                List.of("ns1.example.no")
        ));

        assertEquals(SpfPosture.MULTIPLE, observation.spfPosture());
        assertTrue(observation.spfSignals().contains("Microsoft 365"));
    }

    @Test
    void givesHighProviderConfidenceWhenHjelsethAppearsInTwoSources() {
        TechnologyObservation observation = service.analyze(dns(
                List.of("10 mail.hjelseth.com"),
                List.of("v=spf1 include:spf.hjelseth.com include:spf.protection.outlook.com -all"),
                "v=DMARC1; p=quarantine",
                "quarantine",
                List.of("ns1.hjelseth.com", "ns2.hjelseth.com")
        ));

        ProviderSignal signal = observation.primaryProviderSignal();
        assertEquals("Hjelseth", signal.provider());
        assertEquals(ProviderRole.MSP_CANDIDATE, signal.role());
        assertEquals(SignalConfidence.HIGH, signal.confidence());
        assertTrue(signal.sources().containsAll(List.of(SignalSource.MX, SignalSource.SPF, SignalSource.NS)));
    }

    @Test
    void doesNotInventSignalsWhenDnsIsEmpty() {
        TechnologyObservation observation = service.analyze(dns(
                List.of(),
                List.of(),
                null,
                null,
                List.of()
        ));

        assertEquals(EmailPlatform.NONE, observation.emailPlatform());
        assertEquals(DmarcPosture.MISSING, observation.dmarcPosture());
        assertEquals(SpfPosture.MISSING, observation.spfPosture());
        assertFalse(observation.hasProviderSignals());
    }


    @Test
    void keepsTechnicalLookupFailureSeparateFromMissingRecords() {
        TechnologyObservation observation = service.analyze(new DnsObservation(
                "example.no",
                DnsLookupStatus.PARTIAL,
                List.of(),
                List.of(),
                null,
                null,
                List.of("ns1.example.no"),
                List.of(
                        "MX for example.no: timeout",
                        "TXT for example.no: timeout",
                        "TXT for _dmarc.example.no: timeout"
                )
        ));

        assertEquals(EmailPlatform.UNKNOWN, observation.emailPlatform());
        assertEquals(SpfPosture.UNKNOWN, observation.spfPosture());
        assertEquals(DmarcPosture.UNKNOWN, observation.dmarcPosture());
    }

    @Test
    void classifiesDomeneshopAsDnsProviderInsteadOfMsp() {
        TechnologyObservation observation = service.analyze(dns(
                List.of("10 example-no.mail.protection.outlook.com"),
                List.of("v=spf1 include:spf.protection.outlook.com -all"),
                "v=DMARC1; p=reject",
                "reject",
                List.of("ns1.hyp.net", "ns2.hyp.net")
        ));

        ProviderSignal signal = observation.primaryProviderSignal();
        assertEquals("Domeneshop", signal.provider());
        assertEquals(ProviderRole.DNS_PROVIDER, signal.role());
        assertFalse(observation.hasMspCandidateSignals());
    }

    @Test
    void classifiesSpfRedirectAndKeepsTarget() {
        TechnologyObservation observation = service.analyze(dns(
                List.of("10 mail.example.no"),
                List.of("v=spf1 include:sender.example redirect=_spf.policy.example"),
                null,
                null,
                List.of("ns1.example.no")
        ));

        assertEquals(SpfPosture.REDIRECTED, observation.spfPosture());
        assertEquals("_spf.policy.example", observation.spfRedirectTarget());
        assertTrue(observation.spfSignals().contains("SPF redirect: _spf.policy.example"));
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
