package no.reconic.generator.intelligence;

import java.util.List;

public record ProviderSignal(
        String provider,
        ProviderRole role,
        List<SignalSource> sources,
        SignalConfidence confidence,
        List<String> evidence
) {
    public ProviderSignal(
            String provider,
            List<SignalSource> sources,
            SignalConfidence confidence,
            List<String> evidence
    ) {
        this(provider, ProviderRole.UNKNOWN_TECHNICAL_PROVIDER, sources, confidence, evidence);
    }

    public ProviderSignal {
        provider = normalize(provider);
        role = role == null ? ProviderRole.UNKNOWN_TECHNICAL_PROVIDER : role;
        sources = sources == null ? List.of() : List.copyOf(sources);
        confidence = confidence == null ? SignalConfidence.NONE : confidence;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public String sourcesDisplay() {
        return sources.isEmpty()
                ? ""
                : String.join(" + ", sources.stream().map(Enum::name).toList());
    }

    public String evidenceDisplay() {
        return String.join(" | ", evidence);
    }

    public boolean isMspCandidate() {
        return role == ProviderRole.MSP_CANDIDATE;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "Ukjent";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "Ukjent" : trimmed;
    }
}
