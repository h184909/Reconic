package no.reconic.generator.dns;

public enum DnsLookupStatus {
    SUCCESS("Fullført", "success"),
    PARTIAL("Delvis", "partial"),
    FAILED("Feilet", "failed"),
    SKIPPED("Ikke kjørt", "skipped");

    private final String displayName;
    private final String cssClass;

    DnsLookupStatus(String displayName, String cssClass) {
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
