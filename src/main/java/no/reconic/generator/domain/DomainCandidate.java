package no.reconic.generator.domain;

public record DomainCandidate(
        String domain,
        DomainSource source,
        DomainConfidence confidence,
        boolean requiresVerification,
        String explanation
) {
    public DomainCandidate {
        domain = normalizeNullable(domain);
        source = source == null ? DomainSource.NONE : source;
        confidence = confidence == null ? DomainConfidence.NONE : confidence;
        explanation = normalizeNullable(explanation);

        if (domain == null) {
            source = DomainSource.NONE;
            confidence = DomainConfidence.NONE;
            requiresVerification = false;
        }
    }

    public static DomainCandidate none(String explanation) {
        return new DomainCandidate(null, DomainSource.NONE, DomainConfidence.NONE, false, explanation);
    }

    public boolean hasDomain() {
        return domain != null;
    }

    public String url() {
        return hasDomain() ? "https://" + domain : null;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
