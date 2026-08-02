package no.reconic.generator.intelligence;

public enum SignalConfidence {
    HIGH("Høy", "high"),
    MEDIUM("Middels", "medium"),
    LOW("Lav", "low"),
    NONE("Ingen", "none");

    private final String displayName;
    private final String cssClass;

    SignalConfidence(String displayName, String cssClass) {
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
