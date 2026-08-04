package no.reconic.generator.intelligence;

public enum ProviderRole {
    MSP_CANDIDATE("Mulig IT-/driftsleverandør", "msp", 0),
    EMAIL_SECURITY_PROVIDER("E-postsikkerhet/gateway", "email-security", 1),
    EMAIL_PROVIDER("E-postleverandør", "email", 2),
    OUTBOUND_EMAIL_PROVIDER("Utgående e-post", "outbound-email", 3),
    DNS_PROVIDER("DNS-/domeneleverandør", "dns", 4),
    CONNECTIVITY_PROVIDER("Nett-/konnektivitetsleverandør", "connectivity", 5),
    UNKNOWN_TECHNICAL_PROVIDER("Ukjent teknisk rolle", "unknown", 6);

    private final String displayName;
    private final String cssClass;
    private final int sortOrder;

    ProviderRole(String displayName, String cssClass, int sortOrder) {
        this.displayName = displayName;
        this.cssClass = cssClass;
        this.sortOrder = sortOrder;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
