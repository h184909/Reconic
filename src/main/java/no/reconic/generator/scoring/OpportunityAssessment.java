package no.reconic.generator.scoring;

import java.util.List;

public record OpportunityAssessment(
        int opportunityScore,
        OpportunityPriority priority,
        int marketFitScore,
        int technicalOpportunityScore,
        int providerLandscapeScore,
        int dataConfidenceScore,
        List<String> reasonsToContact,
        List<String> uncertaintyWarnings,
        List<String> evidence
) {
    public OpportunityAssessment {
        opportunityScore = clamp(opportunityScore);
        priority = priority == null ? OpportunityPriority.fromScore(opportunityScore) : priority;
        marketFitScore = clampRange(marketFitScore, 0, 35);
        technicalOpportunityScore = clampRange(technicalOpportunityScore, 0, 45);
        providerLandscapeScore = clampRange(providerLandscapeScore, 0, 20);
        dataConfidenceScore = clamp(dataConfidenceScore);
        reasonsToContact = copy(reasonsToContact);
        uncertaintyWarnings = copy(uncertaintyWarnings);
        evidence = copy(evidence);
    }

    public static OpportunityAssessment empty() {
        return new OpportunityAssessment(
                0,
                OpportunityPriority.LOW,
                0,
                0,
                0,
                0,
                List.of(),
                List.of("Opportunity-score er ikke beregnet."),
                List.of()
        );
    }

    public boolean hasWarnings() {
        return !uncertaintyWarnings.isEmpty();
    }

    public String reasonsDisplay() {
        return String.join(" | ", reasonsToContact);
    }

    public String warningsDisplay() {
        return String.join(" | ", uncertaintyWarnings);
    }

    public String evidenceDisplay() {
        return String.join(" | ", evidence);
    }

    public String scoreBreakdownDisplay() {
        return "Marked " + marketFitScore + "/35 | Teknisk "
                + technicalOpportunityScore + "/45 | Leverandørbilde "
                + providerLandscapeScore + "/20";
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static int clamp(int value) {
        return clampRange(value, 0, 100);
    }

    private static int clampRange(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
