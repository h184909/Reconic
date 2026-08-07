package no.reconic.generator.intelligence;

public enum PublicSignalStatus {
    PRESENT("Funnet"),
    MISSING("Ikke funnet"),
    UNKNOWN("Ukjent"),
    SKIPPED("Ikke kjørt");

    private final String displayName;

    PublicSignalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
