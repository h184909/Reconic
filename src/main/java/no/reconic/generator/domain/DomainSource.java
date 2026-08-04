package no.reconic.generator.domain;

public enum DomainSource {
    REGISTERED_WEBSITE("Registrert hjemmeside"),
    REGISTERED_EMAIL("Registrert e-post"),
    MANUAL_OVERRIDE("Manuell fasit"),
    NONE("Ikke funnet");

    private final String displayName;

    DomainSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
