package no.reconic.generator.intelligence;

public enum SpfPosture {
    MISSING("Mangler", "missing"),
    MULTIPLE("Flere SPF-poster", "warning"),
    HARD_FAIL("Hardfail (-all)", "strong"),
    SOFT_FAIL("Softfail (~all)", "monitoring"),
    NEUTRAL("Neutral (?all)", "warning"),
    PASS_ALL("Tillater alle (+all)", "danger"),
    REDIRECTED("Redirect til annen SPF-policy", "redirected"),
    PRESENT("Funnet", "present"),
    UNKNOWN("Ukjent", "unknown");

    private final String displayName;
    private final String cssClass;

    SpfPosture(String displayName, String cssClass) {
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
