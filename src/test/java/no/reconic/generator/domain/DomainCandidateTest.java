package no.reconic.generator.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainCandidateTest {

    @Test
    void noDomainForcesNoneState() {
        DomainCandidate candidate = new DomainCandidate(
                " ",
                DomainSource.REGISTERED_WEBSITE,
                DomainConfidence.HIGH,
                true,
                "Ikke funnet"
        );

        assertThat(candidate.domain()).isNull();
        assertThat(candidate.source()).isEqualTo(DomainSource.NONE);
        assertThat(candidate.confidence()).isEqualTo(DomainConfidence.NONE);
        assertThat(candidate.requiresVerification()).isFalse();
    }

    @Test
    void buildsHttpsUrlForDomain() {
        DomainCandidate candidate = new DomainCandidate(
                "example.no",
                DomainSource.REGISTERED_WEBSITE,
                DomainConfidence.HIGH,
                false,
                "Registrert hjemmeside"
        );

        assertThat(candidate.url()).isEqualTo("https://example.no");
    }
}
