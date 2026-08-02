package no.reconic.generator.intelligence;

public enum EmailPlatform {
    MICROSOFT_365("Microsoft 365", "microsoft"),
    GOOGLE_WORKSPACE("Google Workspace", "google"),
    OTHER("Annen plattform", "other"),
    NONE("Ingen MX", "none"),
    UNKNOWN("Ukjent", "unknown");

    private final String displayName;
    private final String cssClass;

    EmailPlatform(String displayName, String cssClass) {
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
