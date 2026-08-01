package no.reconic.generator.domain;

public enum DomainConfidence {
    HIGH("Høy", "high"),
    MEDIUM("Middels", "medium"),
    LOW("Lav", "low"),
    NONE("Ingen", "none");

    private final String displayName;
    private final String cssClass;

    DomainConfidence(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }
}
