package no.reconic.generator.scoring;

public enum OpportunityPriority {
    VERY_HIGH("Svært høy", "very-high"),
    HIGH("Høy", "high"),
    MEDIUM("Middels", "medium"),
    LOW("Lav", "low");

    private final String displayName;
    private final String cssClass;

    OpportunityPriority(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }

    public static OpportunityPriority fromScore(int score) {
        if (score >= 80) {
            return VERY_HIGH;
        }
        if (score >= 65) {
            return HIGH;
        }
        if (score >= 45) {
            return MEDIUM;
        }
        return LOW;
    }
}
