package no.reconic.generator.intelligence;

import java.util.ArrayList;
import java.util.List;

public record TechnologyObservation(
        EmailPlatform emailPlatform,
        SignalConfidence emailPlatformConfidence,
        EmailGateway emailGateway,
        SignalConfidence emailGatewayConfidence,
        DmarcPosture dmarcPosture,
        SpfPosture spfPosture,
        String spfAllMechanism,
        String spfRedirectTarget,
        List<String> spfSignals,
        List<ProviderSignal> providerSignals,
        List<String> evidence,
        PublicInfrastructureObservation publicInfrastructure
) {
    public TechnologyObservation(
            EmailPlatform emailPlatform,
            SignalConfidence emailPlatformConfidence,
            EmailGateway emailGateway,
            SignalConfidence emailGatewayConfidence,
            DmarcPosture dmarcPosture,
            SpfPosture spfPosture,
            String spfAllMechanism,
            String spfRedirectTarget,
            List<String> spfSignals,
            List<ProviderSignal> providerSignals,
            List<String> evidence
    ) {
        this(
                emailPlatform,
                emailPlatformConfidence,
                emailGateway,
                emailGatewayConfidence,
                dmarcPosture,
                spfPosture,
                spfAllMechanism,
                spfRedirectTarget,
                spfSignals,
                providerSignals,
                evidence,
                PublicInfrastructureObservation.empty()
        );
    }

    public TechnologyObservation(
            EmailPlatform emailPlatform,
            SignalConfidence emailPlatformConfidence,
            EmailGateway emailGateway,
            SignalConfidence emailGatewayConfidence,
            DmarcPosture dmarcPosture,
            SpfPosture spfPosture,
            String spfAllMechanism,
            List<String> spfSignals,
            List<ProviderSignal> providerSignals,
            List<String> evidence
    ) {
        this(
                emailPlatform,
                emailPlatformConfidence,
                emailGateway,
                emailGatewayConfidence,
                dmarcPosture,
                spfPosture,
                spfAllMechanism,
                null,
                spfSignals,
                providerSignals,
                evidence,
                PublicInfrastructureObservation.empty()
        );
    }

    public TechnologyObservation {
        emailPlatform = emailPlatform == null ? EmailPlatform.UNKNOWN : emailPlatform;
        emailPlatformConfidence = emailPlatformConfidence == null ? SignalConfidence.NONE : emailPlatformConfidence;
        emailGateway = emailGateway == null ? EmailGateway.NONE : emailGateway;
        emailGatewayConfidence = emailGatewayConfidence == null ? SignalConfidence.NONE : emailGatewayConfidence;
        dmarcPosture = dmarcPosture == null ? DmarcPosture.UNKNOWN : dmarcPosture;
        spfPosture = spfPosture == null ? SpfPosture.UNKNOWN : spfPosture;
        spfAllMechanism = normalize(spfAllMechanism);
        spfRedirectTarget = normalize(spfRedirectTarget);
        spfSignals = spfSignals == null ? List.of() : List.copyOf(spfSignals);
        providerSignals = providerSignals == null ? List.of() : List.copyOf(providerSignals);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        publicInfrastructure = publicInfrastructure == null
                ? PublicInfrastructureObservation.empty()
                : publicInfrastructure;
    }

    public static TechnologyObservation empty() {
        return new TechnologyObservation(
                EmailPlatform.UNKNOWN,
                SignalConfidence.NONE,
                EmailGateway.NONE,
                SignalConfidence.NONE,
                DmarcPosture.UNKNOWN,
                SpfPosture.UNKNOWN,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                PublicInfrastructureObservation.empty()
        );
    }

    public TechnologyObservation withPublicInfrastructure(PublicInfrastructureObservation observation) {
        return new TechnologyObservation(
                emailPlatform,
                emailPlatformConfidence,
                emailGateway,
                emailGatewayConfidence,
                dmarcPosture,
                spfPosture,
                spfAllMechanism,
                spfRedirectTarget,
                spfSignals,
                providerSignals,
                evidence,
                observation
        );
    }

    public boolean hasGateway() {
        return emailGateway != EmailGateway.NONE;
    }

    public boolean hasProviderSignals() {
        return !providerSignals.isEmpty();
    }

    public boolean hasMspCandidateSignals() {
        return providerSignals.stream().anyMatch(ProviderSignal::isMspCandidate);
    }

    public List<ProviderSignal> mspCandidateSignals() {
        return providerSignals.stream().filter(ProviderSignal::isMspCandidate).toList();
    }

    public ProviderSignal primaryProviderSignal() {
        return providerSignals.isEmpty() ? null : providerSignals.getFirst();
    }

    public String spfSignalsDisplay() {
        return String.join(" | ", spfSignals);
    }

    public String evidenceDisplay() {
        List<String> combined = new ArrayList<>(evidence);
        combined.addAll(publicInfrastructure.evidence());
        return String.join(" | ", combined);
    }

    public String providerSignalsDisplay() {
        return String.join(" | ", providerSignals.stream()
                .map(signal -> signal.provider() + " (" + signal.role().getDisplayName()
                        + "; " + signal.sourcesDisplay() + ")")
                .toList());
    }

    public String providerEvidenceDisplay() {
        return String.join(" | ", providerSignals.stream()
                .flatMap(signal -> signal.evidence().stream())
                .toList());
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
