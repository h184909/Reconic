package no.reconic.generator.finance;

public enum FinancialLookupStatus {
    SUCCESS("Regnskap funnet"),
    NOT_AVAILABLE("Ingen nøkkeltall"),
    FAILED("Oppslag feilet"),
    SKIPPED("Ikke hentet");

    private final String displayName;

    FinancialLookupStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
