package no.reconic.generator.intelligence;

public enum DmarcPosture {
    MISSING("Mangler", "missing", false),
    MONITORING("Overvåking (p=none)", "monitoring", false),
    QUARANTINE("Quarantine", "enforced", true),
    REJECT("Reject", "enforced", true),
    INVALID("Ugyldig eller ukjent", "warning", false),
    UNKNOWN("Ukjent", "unknown", false);

    private final String displayName;
    private final String cssClass;
    private final boolean enforced;

    DmarcPosture(String displayName, String cssClass, boolean enforced) {
        this.displayName = displayName;
        this.cssClass = cssClass;
        this.enforced = enforced;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }

    public boolean isEnforced() {
        return enforced;
    }
}
