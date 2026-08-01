package no.reconic.generator.domain;

import org.junit.jupiter.api.Test;

import java.net.IDN;

import static org.assertj.core.api.Assertions.assertThat;

class DomainDiscoveryServiceTest {

    private final DomainDiscoveryService service = new DomainDiscoveryService();

    @Test
    void usesRegisteredWebsiteWithHighConfidence() {
        DomainCandidate result = service.discover("https://www.example.no/kontakt?from=brreg", "post@other.no");

        assertThat(result.domain()).isEqualTo("example.no");
        assertThat(result.source()).isEqualTo(DomainSource.REGISTERED_WEBSITE);
        assertThat(result.confidence()).isEqualTo(DomainConfidence.HIGH);
        assertThat(result.requiresVerification()).isFalse();
    }

    @Test
    void addsSchemeWhenWebsiteHasNone() {
        DomainCandidate result = service.discover("www.example.no/kontakt", null);

        assertThat(result.domain()).isEqualTo("example.no");
    }

    @Test
    void normalizesUppercaseAndTrailingDot() {
        DomainCandidate result = service.discover("HTTPS://WWW.EXAMPLE.NO./", null);

        assertThat(result.domain()).isEqualTo("example.no");
    }

    @Test
    void reducesSubdomainToRegistrableDomain() {
        DomainCandidate result = service.discover("https://portal.customer.no/login", null);

        assertThat(result.domain()).isEqualTo("customer.no");
    }

    @Test
    void preservesRegistrableDomainWithMultiLabelPublicSuffix() {
        DomainCandidate result = service.discover("https://portal.customer.co.uk/login", null);

        assertThat(result.domain()).isEqualTo("customer.co.uk");
    }

    @Test
    void handlesInternationalizedDomainName() {
        DomainCandidate result = service.discover("https://www.blåbær.no/kontakt", null);

        assertThat(result.domain()).isEqualTo(IDN.toASCII("blåbær.no"));
    }

    @Test
    void fallsBackToRegisteredEmailWithMediumConfidence() {
        DomainCandidate result = service.discover(null, "post@example.no");

        assertThat(result.domain()).isEqualTo("example.no");
        assertThat(result.source()).isEqualTo(DomainSource.REGISTERED_EMAIL);
        assertThat(result.confidence()).isEqualTo(DomainConfidence.MEDIUM);
        assertThat(result.requiresVerification()).isTrue();
    }

    @Test
    void extractsEmailFromDisplayNameFormat() {
        DomainCandidate result = service.discover(null, "Example AS <post@example.no>");

        assertThat(result.domain()).isEqualTo("example.no");
    }

    @Test
    void fallsBackToEmailWhenWebsiteIsInvalid() {
        DomainCandidate result = service.discover("not a valid url", "post@example.no");

        assertThat(result.domain()).isEqualTo("example.no");
        assertThat(result.source()).isEqualTo(DomainSource.REGISTERED_EMAIL);
        assertThat(result.explanation()).contains("registrerte hjemmesiden kunne ikke brukes");
    }

    @Test
    void rejectsFreeEmailProvider() {
        DomainCandidate result = service.discover(null, "company@gmail.com");

        assertThat(result.hasDomain()).isFalse();
        assertThat(result.source()).isEqualTo(DomainSource.NONE);
        assertThat(result.explanation()).contains("gratis e-postleverandør");
    }

    @Test
    void rejectsSocialMediaAsRegisteredWebsite() {
        DomainCandidate result = service.discover("https://facebook.com/example", null);

        assertThat(result.hasDomain()).isFalse();
        assertThat(result.explanation()).contains("delt plattform eller katalog");
    }

    @Test
    void rejectsSharedWebsiteBuilderDomain() {
        DomainCandidate result = service.discover("https://example.wixsite.com/company", null);

        assertThat(result.hasDomain()).isFalse();
    }

    @Test
    void rejectsIpAddress() {
        DomainCandidate result = service.discover("https://192.168.1.20", null);

        assertThat(result.hasDomain()).isFalse();
    }

    @Test
    void handlesPortInWebsite() {
        DomainCandidate result = service.discover("https://www.example.no:8443/path", null);

        assertThat(result.domain()).isEqualTo("example.no");
    }

    @Test
    void returnsNoneWhenNoSourcesExist() {
        DomainCandidate result = service.discover(null, null);

        assertThat(result.hasDomain()).isFalse();
        assertThat(result.confidence()).isEqualTo(DomainConfidence.NONE);
        assertThat(result.explanation()).contains("verken registrert hjemmeside eller e-postadresse");
    }
}
