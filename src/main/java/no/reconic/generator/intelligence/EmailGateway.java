package no.reconic.generator.intelligence;

public enum EmailGateway {
    NONE("Ingen separat gateway", "none"),
    MIMECAST("Mimecast", "gateway"),
    PROOFPOINT("Proofpoint", "gateway"),
    CISCO_EMAIL_SECURITY("Cisco Email Security", "gateway"),
    TELENOR("Telenor", "gateway"),
    ALTIBOX("Altibox", "gateway"),
    OTHER("Annen gateway", "other");

    private final String displayName;
    private final String cssClass;

    EmailGateway(String displayName, String cssClass) {
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
